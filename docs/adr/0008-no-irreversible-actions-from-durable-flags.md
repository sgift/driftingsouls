# A durable flag must not trigger an irreversible action without re-validating its condition

State that drives destruction, deletion or any other unrecoverable action is re-checked at the moment
the action is taken, against the live condition — never taken on trust from a flag written in an
earlier tick. Where that is impractical, the pending intent is modelled explicitly, with its own
lifetime, rather than stashed as a token in a general-purpose string column.

The case that motivates this is `ships.status`. That column carries two kinds of token, and
`Ship.getPersistentStatus()` is where the distinction lives: it strips `mangel_reaktor`, `offizier`,
`nocrew`, `disable_iff` and `mangel_nahrung` so that `recalculateShipStatus()` can rebuild them from
current facts, and preserves everything else — `tradepost`, `pluenderbar`, and `destroy`. The first
group is derived and self-correcting. The second is durable, which is right for `tradepost` and
`pluenderbar` and wrong for `destroy`, because `destroy` is the only token that causes the row to be
deleted.

Two places write it: `SchiffsTick.berechneVerfallWegenCrewmangel`, when hull damage from insufficient
crew exceeds the remaining hull, and `PluendernController.aktualisiereSchiffNachWarentransfer`, when
a ship of an `INSTABIL` type is plundered. One place reads it: `SchiffsTick.doDestroyStatus`, which
runs last in the tick and destroys everything matching `locate('destroy',status)!=0`. Nothing between
the write and the read re-checks whether the ship still has too little crew. A player who re-crews a
marked ship still loses it, and — because the token survives every subsequent `recalculateShipStatus()`
— loses it at an arbitrary later tick, with no visible connection to the event that marked it.

That the field is being asked to be two things at once is visible in the writes themselves:
`berechneVerfallWegenCrewmangel` calls `setStatus("destroy")`, discarding `tradepost` and any other
durable token the ship carried, while `PluendernController` carefully appends. The codebase already
has the alternative shape: `Nebel.Typ.DAMAGE` destroys a ship whose hull reaches zero by calling
`Ship.destroy()` on the spot. The status-mediated path is the outlier, not the norm.

## Consequences

**The obvious fix is wrong.** Adding `destroy` to the strip list in `getPersistentStatus()` breaks
plundering. `SchiffsTick.tick()` runs `doDestroyStatus` last, but `tickShip` — reached earlier via
`doUsers` — ends by calling `recalculateShipStatus(true)` on every surviving ship. A flag set by
`PluendernController` between ticks would therefore be erased before the destruction pass ever sees
it. The crew-shortage path happens to be immune, since `tickShip` returns early once
`berechneVerfallWegenCrewmangel` reports the ship as gone, which is exactly the kind of asymmetry that
makes the strip look safe in testing. Any remediation has to give the pending-destruction intent a
representation that outlives a status recalculation — an explicit column, a task, or destruction at
the point of decision — not merely remove it from the durable set.

**Remediation is unscheduled and does not ride along with the tick fixes.** The one-per-tick drain in
`doDestroyStatus` (detached entities under `UnitOfWork.setClearOnFlush`) is a separate defect, fixed
independently; that fix removes the backlog mechanism but not the design flaw described here. Clearing
the accumulated flags is an operational step, not a design change.

**The rule generalises past this column.** It applies wherever intent is parked in shared, long-lived
state and acted on later: space-separated token columns, free-text status fields, and any flag whose
producer and consumer are separated by a tick boundary. Prefer re-deriving the condition; where the
intent genuinely must persist, give it a type and a lifetime.

Identified 2026-07-29 from a player report of ships disappearing after ticks. The mechanism is
established from the code; the size of the accumulated backlog on production is to be measured before
the `doDestroyStatus` fix is deployed, since that fix drains the whole backlog in a single tick.
