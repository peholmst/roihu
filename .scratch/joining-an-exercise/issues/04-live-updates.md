# Live updates

Status: done

## Parent

[PRD](../PRD.md)

## What to build

The crew's screens update without reloading. Server push is enabled application-wide (Vaadin push over WebSocket, with long-polling as the fallback). An in-process broadcaster keyed by exercise tells subscribers about changes to holdings and exercise state; the database still decides every change. The picker's free/taken marks update as others take and leave positions, a displaced device is returned to the picker immediately, and the position screen's exercise state updates live.

## Acceptance criteria

- [x] Push is enabled application-wide
- [x] The per-exercise broadcaster publishes holding and state changes; no change is decided by the broadcaster
- [x] The picker's free/taken marks update live
- [x] A connected displaced device returns to the picker immediately with its message
- [x] The position screen's exercise state updates live
- [x] View tests use two UI instances: one takes over a position the other holds, and the other is observed returning to the picker
- [x] Push over a real WebSocket is checked by hand against the seeded exercise

## Blocked by

- [Take over a held position](03-take-over-a-held-position.md)

## Comments

Done in 0a3d518. Found during implementation and review:

- Push was already enabled by issue 02 (`WEBSOCKET_XHR`).
- Checked by hand in two tabs against the seeded exercise: taking and leaving a position
  updated the other tab with no HTTP request of its own, so over the WebSocket. Displacement
  needs two browsers and ending needs the officer side, so both are covered only by the
  two-browser view tests.
- A token read from a cookie is kept in the session too: nothing pushed carries a cookie, and
  a cookie cannot be cleared then, so the session marks it released instead.
- `roundTrip()` does not run queued `ui.access` tasks in browserless; tests drain the queue
  with `runPendingAccessTasks`.
- The issue-03 test of a displaced device tapping "Change position" was removed: push returns
  it to the picker first, and browserless cannot simulate a disconnected device. That fallback
  is no longer covered.
- Codex review: isolate a failing follower from the others and from the committed operation;
  subscribe atomically with the last follower leaving; read again once subscribed; subscribe
  after every navigation, since a reused view is not attached again. Only the first and last
  have tests; the other two are races.
