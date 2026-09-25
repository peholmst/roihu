# Conflicting scenario edits

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

A scenario carries a version. A save made from an out-of-date version is refused, and the editor shows which officer changed the scenario in the meantime and offers to reload. No edit is ever lost without anyone being told.

## Acceptance criteria

- [ ] A save made from an out-of-date version is refused
- [ ] The refusal names the officer who last changed the scenario
- [ ] The editor offers a reload that shows the current version
- [ ] Service and view tests cover the conflict

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)
