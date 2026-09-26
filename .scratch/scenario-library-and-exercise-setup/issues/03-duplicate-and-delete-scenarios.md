# Duplicate and delete scenarios

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

An officer can duplicate any scenario, copying name, prepared language, description and positions into a new scenario created by that officer, with no link to the original. An officer can delete a scenario that has never had an exercise. Deleting one that has exercises is refused with a message, and the database enforces it with a restricting foreign key.

## Acceptance criteria

- [ ] Duplicating copies name, prepared language, description and positions in order
- [ ] The copy is created by the duplicating officer, and later changes to either scenario do not affect the other
- [ ] Deleting a scenario without exercises works
- [ ] Deleting a scenario with exercises is refused with a message, enforced by the database
- [ ] Service and view tests cover the above

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)

## Comments

From the review of issue 02: once scenarios can be deleted, `Scenarios.save` must report a
scenario that is gone as a result of its own rather than throwing, including when it disappears
between the refused update and the lookup of who last changed it. The editor should then say the
scenario was deleted and keep the officer's draft on screen, and a reload of a deleted scenario
should say so rather than silently returning to the library.
