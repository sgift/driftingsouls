# Backlog

Small deferred items — things worth doing that are too narrow to justify an ADR and too easy to lose
otherwise. Unlike `docs/adr/`, this file is editable: entries are added, reworded and deleted freely.
An entry graduates by becoming a commit, or by growing enough context to become an ADR.

Each entry says what, why, and how confident we are that it is actually a problem. Items marked
**unverified** are reasoned from reading the code and have no test behind them yet.

## Pending destruction lives in `ships.status`

See ADR-0008. `destroy` is a durable token in a column whose other durable tokens (`tradepost`,
`pluenderbar`) are harmless, and it triggers deletion without re-checking the condition that set it.
The naive fix — stripping it in `Ship.getPersistentStatus()` — breaks INSTABIL plundering, because
`tickShip` recalculates the status earlier in the tick than `doDestroyStatus` reads it. Needs the
pending intent modelled with its own lifetime.

## `Ship.destroy()` can strand docked and landed ships

`destroy()` gates `undock()`/`start()` on `getTypeData().getADocks()/getJDocks()`, and
`getDockedShips()`/`getLandedShips()` re-check the same counts and return empty. A carrier whose
module configuration no longer advertises the docks its ships are sitting in therefore releases
nobody, leaving them pointing at a deleted row.

A bulk-update safety net now releases whatever is left and logs a warning naming the ship and its
dock counts. The root cause is untouched: dock counts can drop to zero while docks are occupied,
which is reachable outside the tick too — e.g. removing a docking module from a ship with fighters
aboard. Read the warnings from a few production ticks before deciding what to do about it.
**Unverified** that this has actually happened in production.

## `UnitOfWork` keeps using the `EntityManager` after a rollback

`tryWork` rolls back and returns, and `executeFor` immediately begins a new transaction on the same
`EntityManager` without clearing it — then flushes into it. Hibernate's own guidance is that a
session must be discarded after a rollback, since the persistence context may hold state that was
never committed. In practice a failed work object is followed by a flush of whatever survived in the
context. **Unverified**; would show up as inconsistent writes after an error, which is hard to
distinguish from the error itself.

## `setStatus("destroy")` discards the rest of the status field

`SchiffsTick.berechneVerfallWegenCrewmangel` assigns the whole field rather than appending, so a
tradepost destroyed for crew shortage loses its `tradepost` token on the way out.
`PluendernController` appends correctly. Harmless today because the ship is deleted moments later,
but it is the same field-used-as-two-things confusion that ADR-0008 is about, and it would bite if
destruction ever became cancellable.

## `EditPlugin8` may hand entities to a clearing unit of work

`processJobs` runs `new UnitOfWork<>(...).setFlushSize(10).setClearOnFlush(true).executeFor(jobData)`
where `jobData` comes from `updateTask1.supplier.apply(updatedEntity)` — a `Collection<Object>` whose
contents depend on the caller. If any supplier yields entities, this has the detached-work-object
problem the ticks had (see `UnitOfWorkClearOnFlushTest`). The other two non-tick call sites,
`AdminCommands` (RecalculateShipModules) and `CreateObjectsFromImage` (delete bases), were checked
and already drive the unit of work by id. **Unverified** — needs the suppliers enumerated.

## Ships reported missing after ticks — unexplained

A player report of normal, undocked ships disappearing after ticks. Two candidate explanations were
investigated and neither holds up: the carrier-orphan path above (the reported ships were not docked
or landed) and a supposed one-ship-per-tick drain in `doDestroyStatus` (disproven by
`SchiffsTickDestroyStatusTest` — all flagged ships were already destroyed in a single tick, because
`Ship.shiptype` is eagerly fetched and Hibernate 4.1 tolerates removing a detached entity).

Still open. Candidates not yet examined: crew starvation driving crew to zero and hull decay
destroying the ship a tick or two later, which looks unprovoked from the player's side; and
`berechneSoldUndWartung` consigning ships to the pirate when the account cannot cover maintenance,
which removes them from the owner's list without deleting anything. Both send or should send a PM —
worth confirming against what the reporting players actually received.
