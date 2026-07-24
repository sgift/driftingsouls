# Hibernate/jOOQ coexistence: reads mixed, writes owned per table

While the entity layer is migrated to jOOQ (ADR-0004), both technologies access the same schema. The
rule is: **reads may be mixed freely; writes to a given table belong to exactly one technology at a
time.** The migration unit is therefore "all write sites for one table", moved together — after which
that table is jOOQ-owned. Read sites port lazily, page by page.

This is what makes incremental migration possible. Every coexistence hazard below is a write-side
hazard, so owning writes per table neutralises all of them without ever requiring a big-bang switch of
every caller of a table at once.

## The hazards this rule addresses

**First-level cache (the dangerous one).** The persistence context is always on and cannot be
disabled. If Hibernate has loaded an entity and jOOQ then updates that row, Hibernate still holds the
original snapshot; when anything touches the entity, flush writes the whole stale snapshot back and
silently reverts the jOOQ write. This is data loss without an exception. Note it becomes *reachable*
only after ADR-0002's connection inversion — before that, jOOQ wrote on a separate connection and
transaction.

**Optimistic locking.** 57 entities carry `@Version`. A jOOQ update that does not bump `version`
silently disables optimistic locking for that row; one that does bump it makes any already-loaded
entity throw `StaleObjectStateException` at flush. Neither behaviour is right by default.

**Deferred writes.** Hibernate defers inserts and updates until flush, so a jOOQ `SELECT` mid-request
does not see pending Hibernate changes, and FK ordering between a jOOQ insert and a deferred Hibernate
insert can violate constraints.

**Cascades and ID generation.** A jOOQ `DELETE` bypasses Hibernate cascades and orphans child rows. A
jOOQ `INSERT` must respect `hibernate_sequences`, `ShipIdGenerator` and `ItemIdGenerator`.

The second-level cache is *not* a hazard here: no region factory is configured, no cache provider is on
the classpath, and no entity carries `@Cache`/`@Cacheable`. The `cache.use_minimal_puts` property in
`hibernate.xml` is vestigial and has no effect.

## Consequences

Two guards apply while a table is in transition: call `em.flush()` before a jOOQ read that may depend
on pending Hibernate writes in the same request, and do not `em.find()` an entity in a request that
also jOOQ-writes its table.

`em.flush()` only works as a synchronisation point after ADR-0002 — once Hibernate and jOOQ share the
request's connection, jOOQ sees flushed-but-uncommitted rows. This is a third reason the connection
inversion is the first persistence change.

The discipline this demands: before migrating a table, every writer of it must be found. This is why
migration runs leaves-first (ADR-0004) — root entities are written implicitly via cascade from across
the codebase, so their write sites cannot be enumerated with confidence.
