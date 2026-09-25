# Join by code and see the positions

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

A crew member opens the join screen, types a join code (or opens a join link that fills it in), and reaches a position picker for that exercise. The picker shows the exercise's prepared language, its state, and its positions in scenario order as "call sign · name" (the name alone when there is no call sign). It never shows the scenario's name. Positions cannot be taken yet.

This slice lays the ground for everything after it:
- The schema for scenarios, their ordered positions, and exercises. An exercise holds its own copy of the positions (ADR-0002) and a unique join code.
- Join code generation and normalisation, as specified in the PRD.
- A development-only seeder that creates the RVS911/RVS903 scenario and one exercise in setup, and logs its join code and join link.
- The test infrastructure: Testcontainers PostgreSQL for the crew-joining service, and `browserless-test-spring` for views.
- Externalised user-facing strings (ADR-0006). English is enough here; the other languages arrive in issue 05.

Crew routes are open to anonymous users (ADR-0005).

## Acceptance criteria

- [ ] Join codes are 8 Crockford base32 characters from a secure random source, shown as `XXXX-XXXX`, and unique across exercises
- [ ] Typed codes are accepted regardless of case, hyphens and spaces, with O read as 0 and I/L as 1
- [ ] Input that is not 8 valid characters is flagged on the join screen while typing
- [ ] Unknown codes, malformed codes that reach the server, and codes of ended exercises all give one identical "No exercise with this code" result
- [ ] A join link carrying a code opens the join screen with the code filled in
- [ ] The picker shows the prepared language, the exercise state, and positions in scenario order as "call sign · name", or the name alone
- [ ] The scenario's name appears nowhere on crew screens
- [ ] The dev seeder runs only under a development profile and logs the join code and the full join link
- [ ] Service tests run against Testcontainers PostgreSQL with the real Flyway migrations; view tests use browserless-test-spring
- [ ] No hard-coded user-facing strings

## Blocked by

None - can start immediately
