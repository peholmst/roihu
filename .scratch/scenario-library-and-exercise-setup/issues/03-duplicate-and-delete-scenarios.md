# Duplicate and delete scenarios

Status: done

## Parent

[PRD](../PRD.md)

## What to build

An officer can duplicate any scenario, copying name, prepared language, description and positions into a new scenario created by that officer, with no link to the original. An officer can delete a scenario that has never had an exercise. Deleting one that has exercises is refused with a message, and the database enforces it with a restricting foreign key.

## Acceptance criteria

- [x] Duplicating copies name, prepared language, description and positions in order
- [x] The copy is created by the duplicating officer, and later changes to either scenario do not affect the other
- [x] Deleting a scenario without exercises works
- [x] Deleting a scenario with exercises is refused with a message, enforced by the database
- [x] Service and view tests cover the above

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)

## Comments

From the review of issue 02: once scenarios can be deleted, `Scenarios.save` must report a
scenario that is gone as a result of its own rather than throwing, including when it disappears
between the refused update and the lookup of who last changed it. The editor should then say the
scenario was deleted and keep the officer's draft on screen, and a reload of a deleted scenario
should say so rather than silently returning to the library.

Done in fe57ca0. Found during implementation and review:

- Duplicate and Delete are buttons in the editor of an existing scenario rather than in the
  library, as the PRD lists them: the library opens a scenario on a row click, and a button inside
  a row would open it as well.
- `Scenarios.delete` returns `Deleted`, `HasExercises` or `Gone`. The restricting foreign key
  decides; only its violation (SQLSTATE 23503) is reported as `HasExercises`. It refuses to run
  inside an enclosing transaction, since PostgreSQL leaves a transaction unusable after the
  refusal.
- The note above is done: saving, reloading, duplicating or deleting a deleted scenario says so,
  and saving or duplicating keeps the draft.
- Duplicating copies the saved version, so with unsaved changes it asks first.
- The refusal says the scenario has exercises, not that it has been run: an exercise in setup
  counts.
- Checked by hand: deleting the seeded scenario, which has an exercise, is refused with the
  message; duplicating it opens a copy created by the officer; deleting that copy works.
- Not covered by tests: a delete racing an exercise being created, which the foreign key and
  PostgreSQL's locking decide.
- Codex review found the unsaved-changes, enclosing-transaction and wording points above.
