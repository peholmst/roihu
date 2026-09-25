# Exercise lifecycle

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

An officer can start an exercise in setup and end a running one, each after confirming; ending warns that it cannot be undone. States move only forward, setup → running → ended, as atomic state-guarded updates that record started-at and ended-at. Starting does not require any position to be taken. An exercise can be deleted only while in setup, after confirming. When an action's precondition no longer holds because another officer already started, ended or deleted the exercise, the action is refused with a message and the screen refreshes. State changes go out through the per-exercise broadcaster, so crew screens and officer screens update live. Once ended, the exercise admits nobody.

## Acceptance criteria

- [ ] Start and end each need confirmation, and ending warns that it cannot be undone
- [ ] Backward and repeated moves are refused
- [ ] Starting works with free positions
- [ ] Deleting is allowed in setup only, after confirmation
- [ ] The second of two conflicting actions is refused with a message and a refresh
- [ ] Crew position screens and officer screens show state changes live
- [ ] After ending, the join code gives the unknown-code result and holders keep their position screen
- [ ] Service and view tests cover the above

## Blocked by

- [Create an exercise and hand out its code](04-create-an-exercise-and-hand-out-its-code.md)
- [Live updates](../../joining-an-exercise/issues/04-live-updates.md)
