# Create an exercise and hand out its code

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

From the library, an officer creates an exercise from a scenario. The exercise copies the scenario's positions (name, call sign, order), gets a join code, and records who created it and when. Creating from a scenario with no positions is refused. The officer lands on the exercise screen, which shows the state, the scenario name, the join code large and grouped, and the join link with a copy button. The officer's start screen replaces the placeholder `StartView` and lists exercises: those in setup and running first, then ended ones by most recent. Each row shows scenario name, state, creator, start or end time, and positions taken out of the total. Any officer can open any exercise, and there are no limits on how many exist.

## Acceptance criteria

- [ ] Creating an exercise copies the scenario's positions and generates a join code
- [ ] Creating from a scenario without positions is refused
- [ ] A crew member can join an exercise created this way with its code
- [ ] The exercise screen shows the code large and grouped, and the join link with copy
- [ ] The start screen lists exercises in the specified order and with the specified row content
- [ ] Any officer sees and opens every exercise
- [ ] Service and view tests cover the above

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)
