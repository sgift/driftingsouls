# Freeze Hibernate at 4.1 and starve it, rather than upgrading

Hibernate is being removed in favour of jOOQ, so upgrading it is work spent modernising something we
intend to delete. We freeze at 4.1.12.Final and migrate persistence code to jOOQ table by table until
nothing uses Hibernate, at which point it is dropped. There is no Hibernate 5 or 6 step.

This is deliberate. A reader seeing a 2013 ORM pinned in `pom.xml` should not "fix" it by bumping the
version: the 4→5 hop alone requires rewriting the `HibernateUtil` bootstrap (`ServiceRegistryBuilder`,
`org.hibernate.ejb.EntityManagerFactoryImpl`, `Configuration.addAnnotatedClass` and
`generateSchemaUpdateScript` are all removed in 5) and re-expressing `DsNamingStrategy` — the
camelCase→snake_case table naming — as a `PhysicalNamingStrategy`, where a subtle mistake silently
repoints 128 entities at the wrong tables. That risk buys nothing we keep.

## Consequences

Verified 2026-07-25 on JDK 17.0.2: Hibernate 4.1.12 boots cleanly (`HHH000412`, javassist bytecode
provider, `HikariConnectionProvider`, SessionFactory built). It is not an obstacle to the Java 17
upgrade — Spring 4.3 is (see ADR-0001). Freezing is therefore a verified option, not a gamble.

ADR-0002's choice of `DataSourceTransactionManager` over `JpaTransactionManager` is what makes this
possible: JDBC-level transaction coordination has no opinion about the Hibernate version, whereas
`spring-orm` 5.x would have required Hibernate 5.2+.

Accepted costs: no Hibernate security patches for the remaining lifetime of the entity layer (a
server-side game with no untrusted ORM input, so the exposure is low), and no access to Hibernate 5/6
features — which is moot, since new persistence code is written in jOOQ.

Migration proceeds in reverse dependency order, leaves first: static config tables (`Weapon`, `Rasse`,
`Rang`, `Medal`, `ModuleSlot`, `Forschung`), then append-mostly tables (`entities/statistik/*`,
`LogEntry`, `ShipHistory`, `SchlachtLog*`), then mid-tier (`ComNet*`, `PM`, `Handel`), and finally the
deep graphs (`Ship`, `Base`, `User`, `Battle`, `Werft*`). Root entities are last because cascades mean
their write sites cannot be enumerated confidently.
