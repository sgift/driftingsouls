# Spring 5.3 on javax, not Spring 6

Spring 6 requires Java 17, the `jakarta.*` namespace and Tomcat 10 simultaneously, which forces the
Hibernate migration to happen at the same time as the namespace flip across ~250 files. We target
Spring 5.3 instead — the last `javax`-based line — keeping Tomcat 9, and spend the saved effort on
removing Hibernate (see ADR-0004 if written, and the Hibernate/jOOQ direction generally). Once the
entity layer is gone, most of the `javax.persistence` surface disappears with it and Spring 6 becomes
a much smaller step.

Spring 5.3 is a waypoint, not a destination: its OSS support has also ended. It is chosen because it
unblocks Java 17 at near-zero cost, not because it is current.

## Considered Options

Finishing `feature/jakarta` (single commit `e1327d6d1`, 2024-07-07, 212 files changed) was evaluated
and rejected. That branch raised Spring to 6.1.2, Hibernate to 6.4.1, jOOQ to 3.19.1 and the servlet
API to Tomcat 11, and mechanically renamed `javax.*` to `jakarta.*` — but it did not perform the
Hibernate API migration. `HibernateUtil` there still imports `EJB3NamingStrategy`,
`org.hibernate.ejb.EntityManagerFactoryImpl`, `ServiceRegistryBuilder`, `DatabaseMetadata`,
`MySQL5InnoDBDialect` and `org.hibernate.type.*`, all removed in Hibernate 5 or 6. The branch stopped
exactly at the bootstrap rewrite, which is the expensive part. Its base is now 124 commits stale,
behind the request-scoping pass that reworked `EntityManager` lifecycle — the most conflict-prone
possible overlap.

The branch is kept as a reference for dependency-version research. It should not be merged.

The failure mode was attempting everything at once — Spring, Hibernate, jOOQ, servlet API and the
namespace flip in a single commit. The decision recorded here is as much about *sequencing* as about
version numbers: the flip happens later, on its own, once Hibernate is gone.

## Consequences

Java 17 cannot precede the Spring upgrade. Verified 2026-07-25 on JDK 17.0.2: Hibernate 4.1.12 boots
cleanly, but Spring 4.3's repackaged cglib fails with
`InaccessibleObjectException: module java.base does not "opens java.lang" to unnamed module`
(JEP 396 strong encapsulation, default since Java 16). Spring 5.3 is therefore a hard prerequisite
for Java 17, not merely desirable.
