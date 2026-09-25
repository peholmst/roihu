# Take over a held position

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

Tapping a taken position asks "Already taken — take it over?". Confirming takes the position over: the previous holder's token stops resolving, and that device is returned to the picker with a message that its position was taken over, the next time it contacts the server. Two devices taking the same free position at the same moment end with exactly one holder, enforced by the database. The other device gets the take-over prompt, not an error.

## Acceptance criteria

- [ ] Tapping a taken position asks for confirmation before taking it over
- [ ] Take-over replaces the holding and issues a new token; the old token no longer resolves
- [ ] The displaced device returns to the picker with a taken-over message on its next contact
- [ ] At most one holding per exercise position is enforced by the database
- [ ] Two concurrent takes of one position produce exactly one holder, and the other device is offered a take-over
- [ ] Take-over is refused once the exercise has ended
- [ ] Service tests cover concurrency and token invalidation; view tests cover the confirmation and the displaced device

## Blocked by

- [Take a free position and keep it](02-take-a-free-position-and-keep-it.md)
