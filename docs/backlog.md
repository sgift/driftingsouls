# Backlog

Small deferred items — things worth doing that are too narrow to justify an ADR and too easy to lose
otherwise. Unlike `docs/adr/`, this file is editable: entries are added, reworded and deleted freely.
An entry graduates by becoming a commit, or by growing enough context to become an ADR.

Each entry says what, why, and how confident we are that it is actually a problem. Items marked
**unverified** are reasoned from reading the code and have no test behind them yet.

## `TemplateCompiler` never cleans its output directory

It writes generated `.java` files into the output folder and deletes nothing, so a renamed or deleted
template leaves its old generated source and compiled class behind indefinitely. On an incremental
build `TemplateLoader` keeps resolving the stale class and the page keeps rendering from a template
that no longer exists in `src/main/templates`.

Found while writing `ControllerTemplateResolutionTest`: the first version resolved through
`TemplateLoader`, and stayed green after `basen.html` was deliberately renamed away, because both
`basen.class` and `basenbroken.class` were sitting in `target`. The test now asserts against the
template source file instead, which sidesteps the problem but does not fix it.

Harmless day to day — `mvn clean` clears it — but it makes any incremental build an unreliable
witness to template existence. Worth fixing before the jte migration (ADR-0003) starts deleting all
52 templates, where a stale artifact would mask a missed migration.

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

`ShipTick.calculateDecayFromCrewShortage` assigns the whole field rather than appending, so a
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

## Destruction for crew shortage notifies nobody

`ShipTick` sends a `PM` for mutiny, desertion and a low account balance, but
`calculateDecayFromCrewShortage` sets `destroy` without any message. The owner sees the ship simply
gone, one or more ticks later (see ADR-0008 for why the delay). Since the destroy backlog turned out
to be three ships owned by user -1, this is now the leading candidate for the player reports of
missing ships. A PM at the point the flag is set would make the loss explicable — and would be worth
having even after the flag itself is redesigned.

## Clear stale `destroy` flags before deploying the doDestroyStatus fix — OPERATIONAL

`doDestroyStatus` destroyed exactly one ship per tick before the fix: it runs with
`setFlushSize(1).setClearOnFlush(true)`, so every ship after the first was detached and
`Ship.destroy()` failed on `getTypeData().getADocks()` — `Ship.shiptype` is a LAZY `ManyToOne`.
`UnitOfWork` caught each failure per object, so the tick reported success while leaving the ship
alive with its flag intact. Confirmed by a production stack trace and reproduced by
`ShipTickDestroyStatusTest`.

This explains the missing-ships reports: because the flag is durable (ADR-0008), ships marked ticks
or weeks ago are still flying and were being picked off one per tick, with no visible connection to
whatever marked them.

The fix removes the drip — which means the first tick after deployment destroys the entire
accumulated backlog at once. Production was measured on 2026-07-30: **three ships, all owned by user
-1**, so the drain is harmless and this no longer blocks the deployment. It also means the backlog
does not explain the player reports — see the crew-shortage entry above. Re-measure before deploying
anyway, since the tick keeps adding to it:

```sql
SELECT COUNT(*) FROM ships WHERE id > 0 AND LOCATE('destroy', status) != 0;
```

then clear the token while preserving other status flags:

```sql
UPDATE ships
SET status = TRIM(BOTH ' ' FROM REPLACE(CONCAT(' ', status, ' '), ' destroy ', ' '))
WHERE id > 0 AND LOCATE('destroy', status) != 0;
```

Re-run until it reports 0 rows affected (MySQL's `REPLACE` will not match overlapping delimiters if
a row somehow carries the token twice), and keep a copy of the affected rows for answering player
tickets.

## 12 `System.out.println` debug statements in production code

`grep -rn "System\.out\.print" game/src/main --include=*.java` finds 12 live calls (a 13th, in
`thymeleaf/StarmapController`, is already commented out and is a deliberate timing probe worth
keeping as a comment). They are not logging — none goes through `Log`/`LogFactory` like the rest of
the codebase — they are leftover debugging that ships to stdout in production:

- `werften/WerftGUI.java` — **10 of the 12**, lines 823-843. A trace inside the module-slot loop of
  the shipyard fitting UI: `"else in usedSlots.containsKey()"`, `"continue caused by"`, plus raw
  slot names, item ids and access levels. It sits in a nested loop, so it prints once per item per
  slot on every render of that page. This is the noisy one.
- `modules/SchiffAjaxController.java:180` — `System.out.println("test1")`, in
  `fliegeSchiffAction`, between the destination calculation and the `ShipsMovement` call. Left over
  from the in-progress `SchiffFlugService` → `ShipsMovement` rewrite.
- `repositories/StarsystemRepository.java:152` — `System.out.println(select.getSQL())`, dumping
  generated jOOQ SQL.

Distinct from the 7 `System.err.println` calls (`AbstractTickExecuter`, `TickController`,
`SectorTemplateManager`, `jsstubs/JsServiceGenerator`), which are deliberate error/diagnostic
reporting on paths that may have no logger available. Those are a separate question and are not
included here.

Not urgent — stdout noise and a small amount of wasted work per render, no behavioural effect. Worth
clearing as its own commit rather than as a drive-by, since `WerftGUI`'s block is dense enough that
deleting it should be looked at rather than sed'd. **Verified** by grep; the per-render claim for
`WerftGUI` is read from the loop structure and has not been observed at runtime.
