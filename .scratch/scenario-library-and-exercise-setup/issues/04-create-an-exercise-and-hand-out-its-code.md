# Create an exercise and hand out its code

Status: done

## Parent

[PRD](../PRD.md)

## What to build

From the library, an officer creates an exercise from a scenario. The exercise copies the scenario's positions (name, call sign, order), gets a join code, and records who created it and when. Creating from a scenario with no positions is refused. The officer lands on the exercise screen, which shows the state, the scenario name, the join code large and grouped, and the join link with a copy button. The officer's start screen replaces the placeholder `StartView` and lists exercises: those in setup and running first, then ended ones by most recent. Each row shows scenario name, state, creator, start or end time, and positions taken out of the total. Any officer can open any exercise, and there are no limits on how many exist.

## Acceptance criteria

- [x] Creating an exercise copies the scenario's positions and generates a join code
- [x] Creating from a scenario without positions is refused
- [x] A crew member can join an exercise created this way with its code
- [x] The exercise screen shows the code large and grouped, and the join link with copy
- [x] The start screen lists exercises in the specified order and with the specified row content
- [x] Any officer sees and opens every exercise
- [x] Service and view tests cover the above

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)

## Comments

Done in 7823aba. Found during implementation and review:

- "Create exercise" is in the scenario editor, next to Duplicate and Delete, rather than in the
  library: the library opens a scenario on a row click. It uses the saved version and asks first
  when there are unsaved changes.
- The exercise also keeps its scenario's name (V6), so that renaming the scenario leaves existing
  exercises as they were (story 22, ADR-0002).
- `Exercises.createFrom` returns `Created`, `NoPositions` or `ScenarioGone`, and locks the
  scenario row while it copies, so a concurrent save or delete waits.
- Exercises in setup show when they were created, since they have not started; those not yet
  ended are listed most recently created first.
- The start screen reloads its list on every entry: it is the root route, and Flow reuses it.
- Join links use the request's address, with `server.forward-headers-strategy=native` so that
  a proxy on a private or loopback address can pass the public one. A proxy elsewhere needs
  `server.tomcat.remoteip.internal-proxies`; a path prefix is not supported.
- Copying the link says whether it worked. Not covered by tests, which cannot reach the
  browser's clipboard, and not checked by hand.
- Checked by hand: the start screen, the exercise screen with the code and link, sign-out in the
  drawer.
- Codex review found the proxy and silent-copy points above.
