# Roihu

A web app for running small-scale tabletop exercises for fire crews.

## Requirements

The user interface must be available in Finnish, Swedish and English. Inject content is
written in a single language by the training officer and is never translated — see
[ADR-0006](docs/adr/0006-the-ui-is-translated-content-is-not.md). The domain language is
defined in [CONTEXT.md](CONTEXT.md), and the decisions behind it in [docs/adr/](docs/adr/).

## Project Structure

This project has the following structure:

```
src
├── main/java
│   └── [application package]
│       ├── base
│       │   └── ui
│       │       ├── MainLayout.java
│       │       └── ViewTitle.java
│       └── Application.java
├── main/resources
│   ├── META-INF
│   │   └── resources
│   │       ├── icons
│   │       ├── styles.css
│   │       └── view-title.css
│   ├── db/migration          Flyway migrations; the source of truth for the schema
│   └── application.properties
└── test/java
    └── [application package]
```

The main entry point into the application is `Application.java`. This class contains the `main()` method that starts up 
the Spring Boot application.

The project follows a *feature-based package structure*, organizing code by *functional units* rather than traditional 
architectural layers. It currently has one such package, `base`.

* The `base` package contains classes meant for reuse across different features, either through composition or 
  inheritance. You can use them as-is, tweak them to your needs, or remove them.
* Feature packages sit beside `base`, each a *self-contained unit of functionality* including UI components,
  business logic, data access and tests. The generated `examplefeature` package has been removed; the domain this
  application is being built around is defined in [CONTEXT.md](CONTEXT.md).
* The jOOQ classes generated from the Flyway migrations live in `[application package].db.generated` and are
  committed to version control. They are never edited by hand.


## Starting in Development Mode

Start PostgreSQL and Keycloak first, then the application through the dev loop:

```bash
docker compose up -d
.vaadin/vaadin-dev start
```

The application serves on <http://localhost:8080> and Keycloak on <http://localhost:8081>
(admin/admin). Do **not** start the application with `./mvnw`, `mvn spring-boot:run` or an
IDE run configuration: the dev loop daemon owns the application's process and a second
launcher fights it for port 8080. See
[ADR-0009](docs/adr/0009-compose-for-dependencies-app-on-the-host.md).

Local runs use the `dev` Spring profile by default, which seeds an exercise and logs its join
code and join link (look for `Seeded exercise` in `target/devloop/app.log`). Tests run under
`test` and production under `prod`, so neither ever seeds anything.

After editing sources, make the change live with:

```bash
.vaadin/vaadin-dev apply
```

### Changing the database schema

The schema is owned by Flyway migrations in `src/main/resources/db/migration`, and the jOOQ
classes generated from them are committed. After adding or changing a migration, regenerate
and commit both:

```bash
docker compose up -d postgres
./mvnw -Pcodegen generate-sources
```

See [ADR-0008](docs/adr/0008-flyway-owns-the-schema.md).

## Building for Production

To build the application in production mode, run:

```bash
./mvnw package
```

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```

## Next Steps

The [Building Apps](https://vaadin.com/docs/v25/building-apps) guides contain hands-on advice for adding features to 
your application.
