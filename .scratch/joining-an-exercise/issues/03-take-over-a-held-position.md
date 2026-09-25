# Take over a held position

Status: done

## Parent

[PRD](../PRD.md)

## What to build

Tapping a taken position asks "Already taken — take it over?". Confirming takes the position over: the previous holder's token stops resolving, and that device is returned to the picker with a message that its position was taken over, the next time it contacts the server. Two devices taking the same free position at the same moment end with exactly one holder, enforced by the database. The other device gets the take-over prompt, not an error.

## Acceptance criteria

- [x] Tapping a taken position asks for confirmation before taking it over
- [x] Take-over replaces the holding and issues a new token; the old token no longer resolves
- [x] The displaced device returns to the picker with a taken-over message on its next contact
- [x] At most one holding per exercise position is enforced by the database
- [x] Two concurrent takes of one position produce exactly one holder, and the other device is offered a take-over
- [x] Take-over is refused once the exercise has ended
- [x] Service tests cover concurrency and token invalidation; view tests cover the confirmation and the displaced device

## Blocked by

- [Take a free position and keep it](02-take-a-free-position-and-keep-it.md)

## Comments

Done in 0e36898. Found during implementation and review:

- The database already enforced one holder per position (issue 02), so the concurrency test
  passed on its first run; take-over is an upsert on the same key.
- "Change position" re-enters the route instead of reloading the page, so a displaced device
  that taps it lands on the picker with the taken-over message.
- Clearing a token leaves a marker in the session: a request that still carried the old cookie
  was otherwise mistaken for a take-over after a voluntary change of position (Codex review).
- A device displaced before the exercise ended that comes back after the end is sent to the
  join screen without the message, since an ended exercise shows no picker.
