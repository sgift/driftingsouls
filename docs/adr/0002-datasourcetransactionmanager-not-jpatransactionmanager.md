# DataSourceTransactionManager, not JpaTransactionManager

Hibernate currently owns the connection pool (`HikariConnectionProvider` is registered as
`hibernate.connection.provider_class`), and jOOQ borrows from it via
`DBUtil.getConnection(EntityManager)` — which calls `ds.getConnection()` and therefore hands jOOQ a
*different* connection than the one bound to the request's `EntityManager`. jOOQ queries consequently
run outside the request transaction. Ownership is being inverted: a standalone Hikari `DataSource`
bean, with Hibernate demoted to a consumer of it.

We coordinate that with Spring's `DataSourceTransactionManager` (`spring-jdbc`) rather than the
textbook `JpaTransactionManager` (`spring-orm`), because `spring-orm` 5.x requires Hibernate 5.2+.
Choosing `JpaTransactionManager` would drag the Hibernate 4→6 bootstrap rewrite onto the critical
path ahead of everything else — which is precisely what ADR-0001 avoids. JDBC-level coordination is
version-agnostic about Hibernate, so 4.1.12 can be frozen and starved entity-by-entity rather than
migrated.

This is deliberate and will look wrong to a reader who sees a JPA application: do not "fix" it by
switching to `JpaTransactionManager` while Hibernate 4.1 is still present.

## Consequences

Hibernate must stop driving its own transaction. `HibernateSessionRequestFilter` currently calls
`em.getTransaction().begin()/commit()` directly; Spring takes over the boundary and Hibernate flushes
into a connection it does not control. This is the delicate part of the change, and it alters
transaction semantics for every request at once.

Verified 2026-07-25: Hibernate 4.1.12 boots cleanly on JDK 17.0.2, so freezing it is viable rather
than merely hoped for.

It also unlocks `@Transactional` (currently used nowhere in the codebase), which is the prerequisite
for a retry interceptor addressing the known optimistic-locking gap on concurrent writes to the same
`users` row.
