# Modernisation roadmap

Execution order. The ADRs in `docs/adr/` are numbered by when each decision was taken, not by when the
work happens — this file is the running order. Each step is sized to be picked up in its own session:
read the linked ADRs, do the step, stop.

Read `CONTEXT.md` for vocabulary before touching anything.

---

## 0. Foundations — zero risk ✅ done

No decisions, nothing can break, and both items protect every later step.

Done in `d0c731f07` (JS removal) and `29ab73362` (CI). Still to confirm by hand: every page loads on
the dev system without console errors.

**0.1 Delete dead JavaScript.** 17 files, ~7,856 lines in `game/src/main/webapp/data/javascript/`
(root level only): `prototype.js`, `scriptaculous.js`, `effects.js`, `dragdrop.js`, `controls.js`,
`slider.js`, `overlibmws.js`, `map.js`, `starmap.js`, `main.js`, `comm.js`, `builder.js`, `schiff.js`,
`tradepost.js`, `gfxpakversion.js`, `jquery-1.7.2.min.js`, `jquery-ui-1.8.20.min.js`.

`HtmlOutputHandler.appendJs()` emits every file in `libs/`, `common/` and `modules/{module}.js` by
directory enumeration — **root-level files are never emitted**, and none of these is referenced
explicitly. Nothing in `libs/` or `common/` is dead.
*Done when:* files removed, dev system serves every page without console errors.

**0.2 Make CI run the tests.** `.github/workflows/build.yml` runs `verify -DskipTests`. Turn that off
and let the 28 test files run. If they fail today, that is information worth having before Track Zero
churns the codebase.
*Done when:* CI executes the suite and is green.

---

## 1. Track Zero — rename to English

**ADR-0006**, vocabulary in `CONTEXT.md`.

Everything else waits on this, so that no later diff is buried in rename noise. IDE rename refactoring,
one concept per commit, no behavioural change in the same commit. Java identifiers only — the 896
`setVar("...")` string keys stay German until step 4.

Sequence within the step: framework and services first, then controllers and buildings, entities last
(they are scheduled for deletion in step 6). Includes `Fabrik` → `FactoryBuilding`, `Werft` →
`ShipyardBuilding`, `Kommandozentrale` → `CommandCenterBuilding`.
*Done when:* no German identifiers remain outside entity classes and template string keys; build green.

---

## 2. Framework upgrade

**ADR-0001.**

**2.1 Spring 4.3.30 → 5.3.** A hard gate for everything on a modern JDK. Watch `ScopeConfiguration` /
`ThreadScope` (custom scope SPI) and `spring-context-support`'s Quartz integration — Quartz 2.3.2 is
already new enough. `spring.xml` uses 2.5 XSDs, which 5.3 still accepts.

**2.2 Java 11 → 17.** Verified blocked until 2.1 lands: Spring 4.3's repackaged cglib fails on JDK 16+
with `InaccessibleObjectException` on `ClassLoader.defineClass`. Hibernate 4.1.12 was verified to boot
cleanly on 17, so it is not an obstacle.

**2.3 Dependency housekeeping.** Log4j 1.x → reload4j or logback, `reflections` 0.9.9-RC1 → current,
`commons-fileupload`, `mysql-connector-java` → `mysql-connector-j`, drop unused `spring-tx` and Derby.
`feature/jakarta` researched many of these versions and is worth reading — not merging.
*Done when:* build and tests green on JDK 17 with Spring 5.3.

---

## 3. Persistence foundation

**ADR-0002**, then **ADR-0005** before writing any jOOQ.

Ahead of the templating work because it fixes a correctness bug affecting players now: jOOQ queries run
on a different pool connection than the request's `EntityManager`, so they sit outside the request
transaction.

**3.1 Transaction-boundary tests.** Assert that a jOOQ read sees an uncommitted Hibernate write from
the same request, and that a failure rolls both back. These **fail on `master` today** — that is the
point; they encode the bug.

**3.2 Invert connection ownership.** Standalone Hikari `DataSource` bean; Hibernate demoted from owner
to consumer.

**3.3 `DataSourceTransactionManager`.** `HibernateSessionRequestFilter` stops calling
`em.getTransaction()`; Spring owns the boundary. Changes transaction semantics for every request at
once — verify on the dev system as well as in CI.

**3.4 `@Transactional` + retry interceptor.** Closes the known optimistic-locking gap on concurrent
writes to the same `users` row.

⚠️ After 3.2, the ADR-0005 first-level-cache hazard becomes reachable: Hibernate can flush a stale
snapshot over a jOOQ write, silently. Read ADR-0005 before step 6.
*Done when:* 3.1's tests pass, dev system exercised under real use.

---

## 4. Templating

**ADR-0003.**

**4.1 jte pilot** on `impressum.html` / `agb.html` — trivial pages where a mistake shows a wrong static
page, not a corrupted fleet.

**4.2 Port the 52 homegrown templates**, decomposed into **fragments from the start** — the sole
prerequisite for htmx in step 7, and free if done now rather than by re-splitting later. Translate the
`setVar` string keys to English here, where jte makes them typed parameters and the compiler checks it.

**4.3 Delete `TemplateCompiler`** (717 lines) once all 52 are ported.

**4.4 Port the 14 Thymeleaf pages** last; drop the Thymeleaf dependency. Single engine.

---

## 5. Presentation extraction

1022 inline-HTML call sites across 104 files, including domain classes: `Fabrik` (54),
`ForschungszentrumBuilding` (49), `SchiffFlugService` (31).

Runs interleaved with step 4 and ahead of step 6 for these classes. Presentation first, then
persistence: HTML extraction is verifiable by eye on the dev system, whereas persistence bugs are
silent — do the checkable operation first so the dangerous one happens in readable code. jte being
type-safe means a later data-shape change enumerates the templates to fix.

Reversible guidance, not an ADR — abandon it per-file if it fights you.

---

## 6. Hibernate → jOOQ

**ADR-0004** and **ADR-0005**. The long one; runs alongside 4 and 5.

Reverse dependency order, leaves first:

1. Static config — `Weapon`, `Rasse`, `Rang`, `Medal`, `ModuleSlot`, `Forschung`, `FactoryEntry`
2. Append-mostly — `entities/statistik/*`, `LogEntry`, `ShipHistory`, `SchlachtLog*`
3. Mid-tier — `ComNet*`, `PM`, `Ordner`, `Handel`, `GtuWarenKurse`
4. Deep graphs last — `Ship`, `Base`, `User`, `Battle`, `Offizier`, `Werft*`

Roots go last because cascades mean their write sites cannot be enumerated with confidence.

Migration unit is **all write sites for one table**, moved together (ADR-0005). Reads port lazily. No
big-bang switch is ever required.
*Done when:* Hibernate has no remaining users and the dependency is dropped.

---

## 7. Frontend

**7.1 AngularJS 1.0.1 → htmx.** EOL since December 2021; ~15.8k lines vendored plus ~3.5k application
lines serving 2 templates. `angular.service.ajax.js` alone is 1,622 lines of hand-rolled request
plumbing — the thing htmx replaces with attributes. Needs step 4.2's fragments.

**7.2 jQuery 2.1.1 → 3.x**, and resolve the third copy: `jquery.js` is jQuery **1.3.2** (2009) and is
still referenced somewhere.

Keep hand-written JS for the starmap (`starmapNew`, `starmapDrag`, `starmapVoronoi`) — pan, drag and
voronoi are client-side state that htmx is the wrong tool for.

---

## Deferred

The `javax` → `jakarta` flip and Spring 6. Deliberately last, once Hibernate is gone and most of the
`javax.persistence` surface has disappeared with it. See ADR-0001 — this was attempted all at once in
`feature/jakarta` (2024-07-07) and stalled.
