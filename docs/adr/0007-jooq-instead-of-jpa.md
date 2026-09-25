# jOOQ instead of JPA

Persistence is PostgreSQL, accessed through jOOQ. JPA and Hibernate are not used, and
the `spring-boot-starter-data-jpa` dependency and `@Entity` classes that came with the
generated starter are removed.

The data this application keeps is a record of what happened: a timeline of reveals
that is appended to and then read back (ADR-0001). That is relational, query-shaped
work rather than an object graph to be navigated, and jOOQ expresses it as the SQL it
actually is — while remaining type-safe and checked against the real schema.

The trade-off is that JPA is the obvious path here: the starter ships with it, Spring
Data repositories are less code for simple cases, and most Vaadin and Spring examples
assume it. We give that up in exchange for SQL we can read, no lazy-loading or
session-boundary surprises in a long-lived UI, and no Hibernate metamodel fixed at
startup.

## Consequences

Do not reintroduce JPA, Spring Data JPA repositories or `@Entity` annotations; a
mixture of the two is worse than either alone. Loading and saving is written as
queries against generated jOOQ classes (ADR-0008 covers where those come from).

The dev loop restarts rather than hot-swaps on a changed JPA mapping; without JPA that
particular escalation no longer applies, though structural changes to Spring beans
still restart.
