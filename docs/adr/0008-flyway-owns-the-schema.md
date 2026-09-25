# Flyway owns the schema, and jOOQ's generated code is checked in

Flyway migrations under `src/main/resources/db/migration` are the single source of
truth for the database schema. The jOOQ classes generated from that schema are
committed to version control, in a dedicated package under `src/main/java`, and are
regenerated only by an explicit Maven invocation rather than as part of a normal
build.

The schema will change rarely once the domain settles, so paying for code generation
on every build — and requiring Docker to run one — buys little. Checking the generated
classes in means an ordinary build, an IDE import and a CI run need nothing but
Maven, and the diff of a schema change shows up in review alongside the migration
that caused it.

Generation cleans a disposable `tabletop_codegen` database in the Compose PostgreSQL,
re-applies every migration into it, and reads the real catalog — so every
PostgreSQL-specific type, constraint and index is reflected exactly rather than
through a SQL parser's approximation, and the generated classes reflect the migrations
as a whole rather than whatever a developer's database happens to hold. Docker is
therefore required to regenerate, but not to build, test or run.

Testcontainers was the first choice, through
`testcontainers-jooq-codegen-maven-plugin`, so that generation needed nothing running.
Its latest release (0.0.4) cannot negotiate with Docker 29, failing with an empty
`/info` response, so generation goes through the Compose PostgreSQL that local
development already runs. Maven never speaks to Docker, which is also why the Docker
version stops mattering.

## Consequences

Changing the schema is three deliberate steps: write the migration, run the generation
goal, commit both. Generated classes are never edited by hand — the package carries a
`package-info.java` saying so — and a regeneration that produces no diff means the
migration did not do what its author thought.

The generated sources live under `src/main/java` so that the dev loop, which scans
only `src/main/java` and `src/main/resources`, compiles them like any other source. A
separate generated-sources root would be invisible to `apply`.
