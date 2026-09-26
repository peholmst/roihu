# Scenario library and editor

Status: done

## Parent

[PRD](../PRD.md)

## What to build

An officer sees the deployment's shared scenario library, where each scenario shows its name, prepared language, number of positions and last change. The officer can create a scenario and edit any scenario: name, prepared language (defaulting to the officer's interface language), optional description, and an ordered list of positions, each with a name and an optional call sign, with add, edit, remove and drag to reorder. A scenario is saved as one unit and may have no positions yet. Created-by and last-changed-by, with times, are recorded by the officer's email and shown. The description and scenario name never reach crew screens. Editing a scenario never changes an exercise that already exists. Officer routes require the officer role.

## Acceptance criteria

- [x] The library lists scenarios with name, prepared language, position count and last changed
- [x] Officers can create and edit any scenario, including name, prepared language, description and positions
- [x] Positions can be added, edited, removed and reordered by drag, and their order survives saving
- [x] Call signs are free text and optional; call signs and scenario names may be duplicated
- [x] Created-by and changed-by are recorded and shown
- [x] Editing a scenario does not change existing exercises
- [x] Anonymous users cannot reach officer routes, and crew routes stay open
- [x] Service tests against Testcontainers; view tests with a mocked officer sign-in

## Blocked by

- [Join by code and see the positions](../../joining-an-exercise/issues/01-join-by-code-and-see-the-positions.md)

## Comments

Done in 795b699. Found during implementation and review:

- An exercise now copies its scenario's prepared language when it is created (V4), as it copies
  the positions; otherwise editing the scenario changed the language an existing exercise showed
  its crew. ADR-0002 records this.
- A position must have a name; the domain refuses a blank one and the editor flags the field.
- Flow reuses the editor when navigating between scenarios, so it fills the form afresh on every
  entry.
- Times are shown in the server's time zone, on the grounds that a deployment serves one station
  (ADR-0003). Revisit if production servers run in UTC.
- The library lists the most recently changed scenarios first.
- Drag to reorder is tested by firing the grid's own drag and drop events; checked by hand in the
  browser: the library, and the editor loading the seeded scenario. A real mouse drag was not
  tried by hand.
- Follow-up: officer screens have no language switcher yet (PRD story 44, in no issue).
- Not covered by tests: a signed-in user without the officer role, whom sign-in never admits.
- Codex review found the prepared language, editor reuse and unnamed position issues above, all
  fixed.
