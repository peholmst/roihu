# Scenario library and editor

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

An officer sees the deployment's shared scenario library, where each scenario shows its name, prepared language, number of positions and last change. The officer can create a scenario and edit any scenario: name, prepared language (defaulting to the officer's interface language), optional description, and an ordered list of positions, each with a name and an optional call sign, with add, edit, remove and drag to reorder. A scenario is saved as one unit and may have no positions yet. Created-by and last-changed-by, with times, are recorded by the officer's email and shown. The description and scenario name never reach crew screens. Editing a scenario never changes an exercise that already exists. Officer routes require the officer role.

## Acceptance criteria

- [ ] The library lists scenarios with name, prepared language, position count and last changed
- [ ] Officers can create and edit any scenario, including name, prepared language, description and positions
- [ ] Positions can be added, edited, removed and reordered by drag, and their order survives saving
- [ ] Call signs are free text and optional; call signs and scenario names may be duplicated
- [ ] Created-by and changed-by are recorded and shown
- [ ] Editing a scenario does not change existing exercises
- [ ] Anonymous users cannot reach officer routes, and crew routes stay open
- [ ] Service tests against Testcontainers; view tests with a mocked officer sign-in

## Blocked by

- [Join by code and see the positions](../../joining-an-exercise/issues/01-join-by-code-and-see-the-positions.md)
