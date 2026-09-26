# PRD: Joining an exercise and taking a position

Status: done

## Problem Statement

A crew member arriving for a tabletop exercise has no account and no way into the app.
Before any inject can reach them, they need to get from "the officer just read out a
code" to "my phone knows I am RVS911K, Pump Operator" — quickly, on a phone, in the
language they read best, and in a way that survives a locked screen, a dropped
connection or a dead battery.

## Solution

A crew member opens the app's address and types the exercise's join code, or opens a
link that already carries it. The app shows the exercise's positions in scenario order,
each as its call sign and name and marked free or taken, together with the exercise's
state and the language its injects are prepared in — but not the scenario's name, which
would give the incident away. They take a position and land on a position screen that
will later show their injects.

A position has at most one holder. Taking a position someone else holds is allowed
after a confirmation, and the previous holder's device is returned to the picker at
once with a message saying so. That is also how a crew member retakes their own
position from a new device. The device remembers what it holds, so reloading or
reopening the app returns straight to the position, and the crew member can change
position themselves at any time.

The picker and the position screen update live, and the whole interface is available
in Finnish, Swedish and English.

For this slice, exercises exist only as test data: a development-only seeder creates
one, and writes its join code and join link to the log.

## User Stories

1. As a crew member, I want to join an exercise with nothing but a join code, so that I do not need an account for a drill.
2. As a crew member, I want to type the join code on my phone, so that I can join from a code the officer reads out or writes on a whiteboard.
3. As a crew member, I want to open a join link that fills in the code for me, so that I can join with one tap when the officer shares a link.
4. As a crew member, I want the code to be accepted whatever case I type it in, so that I do not have to fight my phone's keyboard.
5. As a crew member, I want the hyphen and spaces in the code to be optional, so that I can type it however I heard it.
6. As a crew member, I want 0 and O, and 1, I and L, to be treated as the same character, so that a code I misread still works.
7. As a crew member, I want to be told as I type when the code is the wrong length, so that I notice a typo before submitting.
8. As a crew member, I want a clear message when no exercise has the code I typed, so that I know to ask the officer again.
9. As a training officer, I want an unknown code to give the same message whatever the reason, so that the join screen tells an outsider nothing about which codes exist.
10. As a training officer, I want join codes to be unguessable, so that nobody can walk into a running exercise by trying codes.
11. As a crew member, I want to see the exercise's positions in the order the scenario lists them, so that the list matches how my unit is organised.
12. As a crew member, I want each position shown as its call sign and name, so that I can find the one I say on the radio.
13. As a crew member, I want a position without a call sign shown by its name alone, so that every position is still recognisable.
14. As a crew member, I want to see which positions are free and which are taken, so that I pick one nobody holds.
15. As a crew member, I want to see which language the exercise's injects are prepared in, so that I know before I start whether I will be reading Finnish, Swedish or English.
16. As a crew member, I want to see whether the exercise has started, is running or has ended, so that I know what to expect.
17. As a training officer, I want the scenario's name kept off the crew's screens, so that the crew learns about the incident only through what I reveal.
18. As a crew member, I want to take a free position with one tap, so that joining is quick.
19. As a crew member, I want to be asked to confirm before taking a position someone already holds, so that I do not take over a colleague's position by accident.
20. As a crew member, I want to take over a position that is already held, so that I can get my position back on a new phone when mine dies.
21. As a crew member whose position was taken over, I want my device returned to the picker at once with a message saying so, so that I notice immediately and can sort it out over the radio.
22. As a crew member, I want the picker to update live as others take and leave positions, so that I do not pick one that was just taken.
23. As a crew member, I want a position someone took at the same moment as me to prompt a take-over instead of failing silently, so that exactly one of us ends up holding it and we both know.
24. As a crew member, I want my phone to remember my position through reloads, locked screens and closed tabs, so that I do not have to join again mid-exercise.
25. As a crew member, I want to keep my position when my phone has been idle for a long time, so that being busy on the radio does not cost me my position.
26. As a crew member, I want to change position myself, so that I can fix a wrong pick without the officer's help.
27. As a crew member, I want the position I leave to become free, so that someone else can take it.
28. As a crew member, I want to keep my position when the exercise ends, so that my phone does not suddenly throw me out.
29. As a training officer, I want an ended exercise to admit nobody, so that its crew is fixed once the exercise is over.
30. As a crew member, I want a position screen that tells me which position I hold, so that I know my phone is set up correctly.
31. As a crew member, I want the position screen to show the exercise's state and update it live, so that I can see when the exercise starts and ends.
32. As a crew member, I want the position screen to say where my injects will appear, so that I know what to watch.
33. As a crew member, I want the interface in my browser's language when it is Finnish, Swedish or English, so that it is right from the first screen.
34. As a crew member, I want the interface in the deployment's default language when my browser prefers another language, so that I get the station's language rather than something arbitrary.
35. As a crew member, I want to switch the interface language on the join screen and the position screen, so that I can read it in the language I prefer.
36. As a crew member, I want my language choice remembered on this device, so that I do not have to choose again after a reload.
37. As a crew member, I want position names and call signs shown exactly as the scenario wrote them, whatever my interface language, so that they match what is said on the radio.
38. As a developer, I want a development-only seeder that creates an exercise and logs its join code and join link, so that I can try joining without an officer side.
39. As a developer, I want the seeded exercise to use a real station's positions, so that the picker is tried against realistic call signs.

## Implementation Decisions

**Domain model and schema (Flyway migrations, jOOQ; ADR-0007, ADR-0008)**

- A scenario has a name, a prepared language (Finnish, Swedish or English) and an ordered list of positions. A position has a name, an optional call sign and its place in the scenario's order. There is no authoring UI in this slice.
- An exercise belongs to a scenario and has a state (setup, running, ended) and a join code that is unique across all exercises.
- An exercise takes its own copy of the scenario's positions when it is created, extending the snapshot reasoning of ADR-0002 to positions: a later scenario edit must not rename or remove a position a crew member already holds. The exercise's positions keep the scenario's order, names and call signs.
- A holding links one exercise position to one holder token. There is at most one holding per exercise position, enforced by the database, so concurrent takes resolve to exactly one winner. Only a hash of the holder token is stored.
- Call signs are free text with no format, uniqueness or presence constraints.

**Join codes**

- 8 characters from the Crockford base32 alphabet (0–9 and A–Z without I, L, O, U), shown as two groups of four, `K7QX-M2P9`, which gives about 40 bits of entropy.
- Generated with a cryptographically secure random source, and redrawn on collision.
- Input normalisation: upper-case, remove hyphens and whitespace, map O→0 and I/L→1. Anything that is not 8 valid characters after normalisation is rejected as malformed before any lookup.
- Unknown codes, malformed codes that reach the server, and codes of ended exercises give one identical result. No rate limiting in this slice; the entropy is the protection.
- Codes admit crew members while the exercise is in setup or running. Once it has ended, taking, taking over and changing position are all refused.

**Crew-joining application service**

This deep module owns every rule above the views. Its operations, in domain terms:

- Find an exercise by a typed join code. Returns the exercise's state, its prepared language, and its positions in order, each with call sign, name and whether it is taken. It never returns the scenario's name.
- Take a position. It returns a new holder token, or reports that the position is held and needs a take-over. Taking over is a separate, explicit operation that replaces the existing holding and returns a new token.
- Change position: release the holding identified by a token.
- Resolve a holder token back to its exercise and position, or report that it no longer holds anything (taken over or released).
- Publish changes to holdings and to exercise state to subscribers of an exercise, so that views can update live.

Only a take-over or a change of position releases a holding. There is no presence detection or timeout, and the exercise ending does not release one: holders stay on their position screen, but can no longer change position.

**Live updates**

- Server push is enabled application-wide (Vaadin push over WebSocket, falling back to long-polling). Reveals will need it later; this slice introduces it.
- Change notification is an in-process broadcaster keyed by exercise, which assumes a single application node. Every change is still decided by the database, never by the broadcaster.

**Views (Vaadin Flow; crew routes open to anonymous users, ADR-0005)**

- Join screen: a code field, pre-filled when the route carries a code. Malformed input is flagged while typing, and the unknown-code message appears on submit.
- Position picker: the prepared-language hint, the exercise state, and positions in scenario order shown as "call sign · name", or the name alone, each marked free or taken. Taking a taken position opens a take-over confirmation. It updates live.
- Position screen: the call sign and name as heading, the live exercise state, an empty area saying injects revealed to this position will appear there, and a change-position action. Nothing else: no join code, no other positions, no scenario name.
- The displaced device returns to the picker with a message that its position was taken over. This happens immediately when it is connected, and otherwise on its next contact.
- The holder token lives in a browser cookie for the exercise. Opening the app with a token that still resolves goes straight to the position screen.

**Interface language (ADR-0006)**

- The default is the browser's preferred language if it is Finnish, Swedish or English. Otherwise it is the deployment's default language, a new configuration property that defaults to Finnish.
- A compact fi/sv/en switcher appears on the join screen and the position screen. The choice is stored in a cookie.
- Every user-facing string in this slice is externalised and translated into all three languages. Position names and call signs are content and are never translated.

**Development seeder**

- Runs only under a development profile and never in production.
- Seeds one scenario in Finnish with positions in this order: RVSP911 (officer), RVS911K (pump operator/driver), RVS911S1–S4 (firefighters 1–4), and RVS903 (tanker driver). It also creates one exercise in setup state.
- Logs the join code and the full join link.

## Testing Decisions

- Good tests exercise external behaviour through the highest available seam: the public operations of the crew-joining service, and the views as a user sees them. They don't assert on tables, jOOQ queries or component internals.
- **Crew-joining service tests** run against a real PostgreSQL started by Testcontainers (a new test dependency), with the real Flyway migrations applied. They cover:
  - code normalisation through lookup: case, hyphens, spaces, O/0, I/L/1
  - the identical result for unknown, malformed and ended-exercise codes
  - positions returned in order with the correct free/taken state, and without the scenario name
  - taking a free position
  - taking a held position reporting a take-over instead of succeeding
  - take-over invalidating the previous token
  - change of position freeing the position
  - holdings surviving the exercise ending, while an ended exercise refuses lookup by code, taking, taking over and changing position
  - two concurrent takes of one position producing exactly one holder
- **View tests** use `browserless-test-spring`. They cover:
  - the join screen: pre-filled from the route, malformed and unknown codes
  - the picker's order, "call sign · name" display and free/taken marks
  - the take-over confirmation
  - the position screen's content
  - language defaults, the switcher and remembering the choice
  - take-over and live updates, with two UI instances in one test: one takes a position the other holds, and the other is observed returning to the picker with its message
- No browser end-to-end tests in this slice. Push over a real WebSocket is checked by hand against the seeded exercise.
- Prior art: none; these are the first tests in the repository and set the pattern for later slices.

## Out of Scope

- Everything on the training officer's side: authoring scenarios and positions, creating and starting exercises, showing the join code or a QR code, changing exercise state.
- Injects, reveals, reads, the timeline and the debrief. The position screen only reserves space for them.
- Crew member names or any roster of who holds which position.
- Rate limiting or other brute-force protection for join codes.
- Presence detection, idle timeouts, or officer-initiated release of a position.
- Running more than one application node.

## Further Notes

- Glossary updates made during design: **Position** now states that it has at most one holder and that taking a held position takes it over; **Call Sign** is a new term.
- Copying positions into the exercise extends ADR-0002 beyond injects. Worth recording in ADR-0002, or a follow-up ADR, once the officer side creates exercises for real.
- Push affects deployment: the proxy in front of the app must pass WebSocket traffic, or crew members fall back to long-polling.

## Comments

Done: all five issues are implemented.

- [01 Join by code and see the positions](issues/01-join-by-code-and-see-the-positions.md) — 4afbf3d
- [02 Take a free position and keep it](issues/02-take-a-free-position-and-keep-it.md) — c3465f2
- [03 Take over a held position](issues/03-take-over-a-held-position.md) — 0e36898
- [04 Live updates](issues/04-live-updates.md) — 0a3d518
- [05 Interface language](issues/05-interface-language.md) — 6c62908

Both follow-ups are closed. Copying positions into the exercise (Further Notes above) is
recorded in ADR-0002, and `<html lang>` now follows the interface language (05).
