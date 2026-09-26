# Conflicting scenario edits

Status: done

## Parent

[PRD](../PRD.md)

## What to build

A scenario carries a version. A save made from an out-of-date version is refused, and the editor shows which officer changed the scenario in the meantime and offers to reload. No edit is ever lost without anyone being told.

## Acceptance criteria

- [x] A save made from an out-of-date version is refused
- [x] The refusal names the officer who last changed the scenario
- [x] The editor offers a reload that shows the current version
- [x] Service and view tests cover the conflict

## Blocked by

- [Scenario library and editor](01-scenario-library-and-editor.md)

## Comments

Done in 5f0c8e1. Found during implementation and review:

- `Scenarios.save` returns `Saved` or `Conflict`, naming who last changed the scenario and
  when. The save is one update guarded by the version, so of officers saving from one version at
  once exactly one succeeds; the tests fail if the guard is removed.
- "Keep editing" leaves the officer's draft on screen, and saving it is still refused; only a
  reload, which says it discards the draft, gives them a version they can save.
- A scenario and its positions are now read from one repeatable-read snapshot, so a reload never
  mixes two versions. Not covered by a test: the race is not reproducible deterministically.
- Deferred to issue 03, noted there: saving or reloading a scenario that has been deleted.
- Follow-up, not in this issue: leaving the editor with unsaved edits discards them without asking.
- Codex review found the snapshot, deleted-scenario and unsaved-navigation points above.
