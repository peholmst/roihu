# Take a free position and keep it

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

A crew member taps a free position in the picker and lands on the position screen. The screen shows the call sign and name as its heading, the exercise state, and an empty area saying that injects revealed to this position will appear there. The device keeps the position through reloads, locked screens and closed tabs: opening the app again goes straight back to the position screen. "Change position" frees the position and returns to the picker. The picker marks positions as free or taken.

A holding links one exercise position to one holder token. The token is stored in a browser cookie, and only a hash of it is kept in the database. Only changing position releases a holding (take-over comes in issue 03). The exercise ending does not release it, but an ended exercise refuses taking and changing position.

## Acceptance criteria

- [ ] Taking a free position creates a holding and shows the position screen
- [ ] The picker marks each position free or taken
- [ ] Reopening the app with a valid holder token goes straight to the position screen, without the join code
- [ ] Only a hash of the holder token is stored
- [ ] Change position frees the position and returns to the picker
- [ ] A holding survives the exercise ending; an ended exercise refuses taking and changing position
- [ ] The position screen shows no join code, no other positions and no scenario name
- [ ] Service and view tests cover the above

## Blocked by

- [Join by code and see the positions](01-join-by-code-and-see-the-positions.md)
