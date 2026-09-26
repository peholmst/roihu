# QR code and presentation mode

Status: done

## Parent

[PRD](../PRD.md)

## What to build

The exercise screen shows a QR code of the join link, generated on the server with ZXing and served as an image, and it shows positions as free or taken, updating live. A presentation mode opens a full-screen view for the training room's screen with only the join code, the QR code, the app's address, and the positions filling up live. It shows no scenario name and nothing else meant only for officers.

## Acceptance criteria

- [x] The QR code encodes the join link, verified in tests by decoding it
- [x] The exercise screen shows free/taken positions updating live
- [x] Presentation mode shows only the join code, the QR code, the app's address and the live positions
- [x] Presentation mode never shows the scenario name or the description
- [x] A view test with two UIs: a crew UI takes a position while an officer or presentation UI watches it change

## Blocked by

- [Create an exercise and hand out its code](04-create-an-exercise-and-hand-out-its-code.md)
- [Live updates](../../joining-an-exercise/issues/04-live-updates.md)

## Comments

Done in 9875aff. Found during implementation and review:

- ZXing `core` only: the PNG is written with ImageIO, so `javase` and its command-line
  dependencies stay out. Tests decode the served bytes (`QrCode.png()`) with ZXing's detector
  and `TRY_HARDER`, as a phone would, not as a pure barcode.
- `Exercise` now carries its positions, read by the same query as the crew's picker
  (`ExercisePositions`).
- Presentation mode is `exercises/:id/presentation`, officers only and outside the shell, opened
  from the exercise screen in a new tab. It shows the join screen's address, the code, the QR
  code and the positions. The browser's own full screen is left to the officer (F11).
- Codex review found that a deleted exercise sent the projected tab to the start screen, which
  lists other scenarios and officers. A deleted or unknown exercise now leaves it saying the
  exercise is no longer available.
- After ending, the room screen still shows the code, which admits nobody by then.
- Live-update tests now run the catch-up a screen queues on entering before the change they
  watch for; before, some passed without the screen following the exercise at all (including
  issue 05's).
- Checked by hand: the room screen's layout and the unavailable message. The live update was
  not, since the browser extension's clicks did not reach the app's buttons.
