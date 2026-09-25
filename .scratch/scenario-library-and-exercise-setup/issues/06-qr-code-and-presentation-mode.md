# QR code and presentation mode

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

The exercise screen shows a QR code of the join link, generated on the server with ZXing and served as an image, and it shows positions as free or taken, updating live. A presentation mode opens a full-screen view for the training room's screen with only the join code, the QR code, the app's address, and the positions filling up live. It shows no scenario name and nothing else meant only for officers.

## Acceptance criteria

- [ ] The QR code encodes the join link, verified in tests by decoding it
- [ ] The exercise screen shows free/taken positions updating live
- [ ] Presentation mode shows only the join code, the QR code, the app's address and the live positions
- [ ] Presentation mode never shows the scenario name or the description
- [ ] A view test with two UIs: a crew UI takes a position while an officer or presentation UI watches it change

## Blocked by

- [Create an exercise and hand out its code](04-create-an-exercise-and-hand-out-its-code.md)
- [Live updates](../../joining-an-exercise/issues/04-live-updates.md)
