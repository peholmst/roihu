# Take a free position and keep it

Status: done

## Parent

[PRD](../PRD.md)

## What to build

A crew member taps a free position in the picker and lands on the position screen. The screen shows the call sign and name as its heading, the exercise state, and an empty area saying that injects revealed to this position will appear there. The device keeps the position through reloads, locked screens and closed tabs: opening the app again goes straight back to the position screen. "Change position" frees the position and returns to the picker. The picker marks positions as free or taken.

A holding links one exercise position to one holder token. The token is stored in a browser cookie, and only a hash of it is kept in the database. Only changing position releases a holding (take-over comes in issue 03). The exercise ending does not release it, but an ended exercise refuses taking and changing position.

## Acceptance criteria

- [x] Taking a free position creates a holding and shows the position screen
- [x] The picker marks each position free or taken
- [x] Reopening the app with a valid holder token goes straight to the position screen, without the join code
- [x] Only a hash of the holder token is stored
- [x] Change position frees the position and returns to the picker
- [x] A holding survives the exercise ending; an ended exercise refuses taking and changing position
- [x] The position screen shows no join code, no other positions and no scenario name
- [x] Service and view tests cover the above

## Blocked by

- [Join by code and see the positions](01-join-by-code-and-see-the-positions.md)

## Comments

Done in c3465f2. Found during implementation and review:

- The cookie alone is not enough: the round trip that takes a position cannot read the cookie
  it is setting, so the token is also kept in the Vaadin session. The cookie is what survives
  a new session.
- Push is pinned to `WEBSOCKET_XHR`, because only an HTTP response can set a cookie.
- A browser holds one position per exercise. Another window of it that tries to take a second
  one is sent to the first instead (Codex review).
- Holders are sent to their position before joinability is checked, so they find it after
  the exercise has ended whichever way they come back (Codex review).
- Not covered by tests: that only the token's hash is stored, and a change of position
  refused because the exercise ended while the screen was open.
