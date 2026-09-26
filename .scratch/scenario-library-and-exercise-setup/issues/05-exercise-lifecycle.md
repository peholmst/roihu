# Exercise lifecycle

Status: done

## Parent

[PRD](../PRD.md)

## What to build

An officer can start an exercise in setup and end a running one, each after confirming; ending warns that it cannot be undone. States move only forward, setup → running → ended, as atomic state-guarded updates that record started-at and ended-at. Starting does not require any position to be taken. An exercise can be deleted only while in setup, after confirming. When an action's precondition no longer holds because another officer already started, ended or deleted the exercise, the action is refused with a message and the screen refreshes. State changes go out through the per-exercise broadcaster, so crew screens and officer screens update live. Once ended, the exercise admits nobody.

## Acceptance criteria

- [x] Start and end each need confirmation, and ending warns that it cannot be undone
- [x] Backward and repeated moves are refused
- [x] Starting works with free positions
- [x] Deleting is allowed in setup only, after confirmation
- [x] The second of two conflicting actions is refused with a message and a refresh
- [x] Crew position screens and officer screens show state changes live
- [x] After ending, the join code gives the unknown-code result and holders keep their position screen
- [x] Service and view tests cover the above

## Blocked by

- [Create an exercise and hand out its code](04-create-an-exercise-and-hand-out-its-code.md)
- [Live updates](../../joining-an-exercise/issues/04-live-updates.md)

## Comments

Done in bdc170c. Found during implementation and review:

- `Exercises.start`, `end` and `delete` take the exercise's id and return `Done`,
  `Refused(state now)` or `Gone`. The old unguarded `end(JoinCode)` is gone; tests that ended an
  exercise straight from setup now start it first (`TestExercises.startAndEnd`).
- Deleting takes the exercise's positions and holdings with it. A holder's position screen hears
  of it and ends on the join screen.
- The exercise screen follows the exercise live, and says so when another officer started, ended
  or deleted it first. Deleting it yourself says nothing of another officer: the screen ignores
  the change it hears after leaving.
- The join link is made once on entering the exercise screen, since it needs the request and a
  pushed change has none.
- The start screen's list does not update live: changes are published per exercise. It reloads
  on every entry, as before.
- Codex review found that taking a position while the exercise was being deleted failed on the
  holding's foreign key. Taking now locks the position row (`for key share`), covered by a
  concurrency test.
- Not checked by hand: the browser extension's clicks did not reach the app's buttons.
