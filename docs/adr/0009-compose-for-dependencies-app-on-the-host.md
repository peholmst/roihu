# Docker Compose runs the dependencies; the application runs on the host

Local development runs PostgreSQL and Keycloak in Docker Compose, while the Vaadin
application itself runs directly on the developer's machine.

The application stays out of Compose because the dev loop daemon owns its process: it
launches the application on a JetBrains Runtime with a hot-swap agent attached, and
that is what makes an edit live in seconds. An application service in Compose would
fight the daemon for port 8080 and give up the loop entirely.

Keycloak is the identity provider for development only. ADR-0005 requires an external
OIDC provider; which one a production deployment uses is that deployment's business.

## Consequences

Start the dependencies with Compose and the application with `.vaadin/vaadin-dev start`
— never `mvn spring-boot:run`, and never by adding an application service to Compose.

Keycloak needs a realm, a client and at least one officer account before anyone can
sign in; that configuration is imported on startup so a fresh checkout works without
manual clicking.

How the system is deployed to production is deliberately undecided. Nothing here
constrains that choice, and this ADR should not be read as implying the application is
containerised in production, or that it is not.
