# Live updates

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

The crew's screens update without reloading. Server push is enabled application-wide (Vaadin push over WebSocket, with long-polling as the fallback). An in-process broadcaster keyed by exercise tells subscribers about changes to holdings and exercise state; the database still decides every change. The picker's free/taken marks update as others take and leave positions, a displaced device is returned to the picker immediately, and the position screen's exercise state updates live.

## Acceptance criteria

- [ ] Push is enabled application-wide
- [ ] The per-exercise broadcaster publishes holding and state changes; no change is decided by the broadcaster
- [ ] The picker's free/taken marks update live
- [ ] A connected displaced device returns to the picker immediately with its message
- [ ] The position screen's exercise state updates live
- [ ] View tests use two UI instances: one takes over a position the other holds, and the other is observed returning to the picker
- [ ] Push over a real WebSocket is checked by hand against the seeded exercise

## Blocked by

- [Take over a held position](03-take-over-a-held-position.md)
