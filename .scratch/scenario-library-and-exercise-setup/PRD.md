# PRD: Scenario library and exercise setup

Status: ready-for-agent

## Problem Statement

A training officer cannot yet prepare anything in the app. Scenarios and exercises
exist only as test data from a development seeder, so there is no way to describe a
station's positions, run an exercise with a real crew, or get a join code in front of
that crew. The officer needs to prepare a scenario once, run it as often as they like,
and on the day go from "the crew is in the room" to "everyone holds a position" with
as little fuss as possible — and move the exercise from setup through running to
ended.

## Solution

Officers share one scenario library for the deployment. A scenario has a name, a
prepared language, an optional description and an ordered list of positions, each with
a name and an optional call sign. Any officer can create, edit, duplicate or delete any
scenario. Deleting is refused once the scenario has exercises, and two officers
editing at once can never silently overwrite each other.

From a scenario, an officer creates an exercise. The exercise copies the scenario's
positions and gets a join code. The exercise screen shows the code large, the join
link with a copy button and a QR code, and a presentation mode puts the code, the QR
code and the positions filling up live on the training room's screen. Any officer can
start the exercise, end it, or delete it while it is still in setup. States only move
forward, and an ended exercise admits nobody.

The officer's start screen lists exercises, those in setup and running first.

## User Stories

1. As a training officer, I want to see the deployment's scenario library, so that I can find a scenario to run.
2. As a training officer, I want each scenario in the library shown with its name, prepared language, number of positions and when it was last changed, so that I can tell scenarios apart without opening them.
3. As a training officer, I want to see who created a scenario and who last changed it, so that I know whom to ask about it.
4. As a training officer, I want to create a scenario, so that I can prepare an exercise ahead of time.
5. As a training officer, I want to give a scenario a name, so that officers can recognise it.
6. As a training officer, I want to be allowed a name another scenario already has, so that I am not forced to invent a distinction that does not matter.
7. As a training officer, I want to set a scenario's prepared language, so that crew members know which language its injects are written in.
8. As a training officer, I want the prepared language to default to my interface language, so that I usually do not have to set it.
9. As a training officer, I want to write an optional description of a scenario for other officers, so that they know what it trains and what it assumes without running it.
10. As a training officer, I want the description never shown to the crew, so that it cannot give the incident away.
11. As a training officer, I want to add positions to a scenario, so that the crew has something to take.
12. As a training officer, I want to give each position a name, so that everyone knows what function it is.
13. As a training officer, I want to give each position an optional call sign written however my station writes it, so that the app matches what is said on the radio.
14. As a training officer, I want to edit and remove positions, so that I can fix mistakes.
15. As a training officer, I want to drag positions into order, so that the crew's picker lists them as my unit is organised.
16. As a training officer, I want to save a scenario with no positions yet, so that I can prepare it in several sittings.
17. As a training officer, I want to edit any officer's scenario, so that the library does not depend on who wrote what.
18. As a training officer, I want my save refused when another officer has changed the scenario since I opened it, so that neither of us silently loses the other's work.
19. As a training officer, I want to duplicate a scenario, so that I can make a variant, or prepare the same scenario in another language, without changing the original.
20. As a training officer, I want to delete a scenario that has never been run, so that the library does not fill up with abandoned drafts.
21. As a training officer, I want deleting a scenario that has exercises to be refused, so that past exercises always keep their scenario.
22. As a training officer, I want editing a scenario never to change an existing exercise, so that an exercise stays exactly as it was when created.
23. As a training officer, I want to create an exercise from a scenario, so that I can run it with today's crew.
24. As a training officer, I want creating an exercise from a scenario without positions to be refused, so that I never hand the crew an exercise nobody can join.
25. As a training officer, I want to land on the new exercise's screen with its join code ready, so that I can hand it out straight away.
26. As a training officer, I want the join code shown large and grouped, so that I can read it aloud.
27. As a training officer, I want to copy the join link, so that I can paste it into the station's group chat.
28. As a training officer, I want a QR code of the join link, so that crew members can scan it from my screen.
29. As a training officer, I want a presentation mode with only the join code, the QR code and the app's address, so that I can put it on the training room's screen without showing the scenario name.
30. As a training officer, I want the presentation mode to show the positions filling up live, so that the room can see which positions are still free.
31. As a training officer, I want to see on the exercise screen which positions are free and which are taken, updating live, so that I know when the crew is ready.
32. As a training officer, I want to start an exercise after confirming, so that I do not start it by accident.
33. As a training officer, I want to start an exercise even when some positions are free, so that I decide when the crew is ready, not the app.
34. As a training officer, I want to end an exercise after confirming that it cannot be undone, so that I do not end it by accident.
35. As a training officer, I want an ended exercise to admit nobody, so that its crew is fixed once it is over.
36. As a training officer, I want states to move only forward, so that the record of an exercise can be trusted.
37. As a training officer, I want to delete an exercise that is still in setup, so that I can recreate it when I notice a mistake before it starts.
38. As a training officer, I want exercises that have started to be impossible to delete, so that they remain a record.
39. As a training officer, I want to see, start, end and delete any officer's exercise, so that an exercise is not stuck when the officer who created it is called away.
40. As a training officer, I want several exercises to exist and run at the same time, so that two crews can drill in parallel.
41. As a training officer, I want my start screen to list exercises in setup and running first and ended ones by most recent, so that what I am working on is at the top.
42. As a training officer, I want each exercise in the list shown with its scenario name, state, creator, start or end time and how many positions are taken, so that I can tell exercises of the same scenario apart.
43. As a training officer, I want an action another officer has just made impossible to be refused with a message, so that I understand why nothing happened.
44. As a training officer, I want the interface in Finnish, Swedish or English, chosen the same way as for the crew, so that I can work in my own language.

## Implementation Decisions

**Relationship to the joining slice**

- This PRD builds on `.scratch/joining-an-exercise/PRD.md`, which introduces the scenario, position, exercise and holding schema and the crew-joining service. Here the officer gets a UI for creating what that slice seeds. The development seeder stays as a convenience.

**Domain model and schema additions (Flyway, jOOQ)**

- A scenario gains an optional description, a created-by and created-at, a last-changed-by and last-changed-at, and a version for optimistic locking. Officers are recorded by their identity-provider email, as the allowlist knows them.
- An exercise gains created-by, created-at, started-at and ended-at.
- A scenario that has at least one exercise cannot be deleted. The database enforces this with a restricting foreign key, and the service reports it.
- An exercise copies its scenario's positions when it is created (name, call sign, order). This extends ADR-0002's snapshot beyond injects and moves the moment of copying for positions: injects are still copied at start (a later slice), positions at creation, because crew members join during setup. ADR-0002 records this.

**Scenario library service**

- Operations: list, get, create, save, duplicate, delete.
- A scenario is saved as one unit (name, prepared language, description, ordered positions) together with the version it was loaded at. A stale version is refused as a conflict naming the officer who last changed it.
- Duplicating copies name, prepared language, description and positions into a new scenario created by the duplicating officer. It records no link to the original.
- Positions within a scenario have no identity that exercises depend on, because exercises hold copies. A save may therefore replace a scenario's positions freely.
- No format, uniqueness or presence constraints on call signs, and no uniqueness constraint on scenario names.

**Exercise service**

- Operations: list, get, create from a scenario, start, end, delete.
- Creating requires at least one position. It generates the join code as specified in the joining PRD and copies the positions.
- The lifecycle moves forward only, setup → running → ended, and each move is an atomic state-guarded update. Deleting is allowed in setup only. An action whose precondition no longer holds (already started, already ended, deleted) is refused with a distinct result that the view turns into a message and a refresh.
- State changes are published through the joining slice's per-exercise broadcaster, so crew screens, officer screens and presentation mode all update live.
- Every officer may perform every action. No ownership, and no limit on exercises per officer or per scenario.

**Views (Vaadin Flow, officer routes require the officer role)**

- Start screen: replaces the placeholder `StartView` and lists exercises, those in setup and running first, then ended ones by most recent. Each row shows scenario name, state, creator, start or end time, and positions taken out of the total.
- Scenario library: the list with name, prepared language, position count and last changed. Actions: create, open, duplicate, delete, and create exercise.
- Scenario editor: name, prepared language, description, and a position list with add, edit, remove and drag to reorder. A conflict on save shows who changed the scenario and offers a reload.
- Exercise screen: state, scenario name, the join code large and grouped, the join link with copy, a QR code, live free/taken positions, and start/end/delete as the state allows, each with a confirmation.
- Presentation mode: a full-screen view of the join code, the QR code, the app's address and the positions filling up live. No scenario name and nothing else officer-only.
- The QR code encodes the join link. It is generated on the server with ZXing and served as an image. No add-on.
- Interface language: the same mechanism as the crew (browser preference, deployment default, a cookie-stored switcher). No per-officer preference is stored.

## Testing Decisions

- Good tests drive the public operations of the services and the views as an officer sees them. They don't assert on tables, queries or component internals. Same seams and prior art as the joining PRD, whose tests come first.
- **Scenario library service tests**, against Testcontainers PostgreSQL with the real migrations:
  - create, save and duplicate round-trips, including position order
  - optional call signs, duplicate call signs, and duplicate scenario names all accepted
  - a save at a stale version refused, naming the officer who last changed the scenario
  - created-by and changed-by recorded
  - deleting refused once an exercise exists, and allowed otherwise
- **Exercise service tests**, same setup:
  - creating copies positions, and a later scenario edit does not affect the exercise
  - creating from a scenario without positions refused
  - forward-only moves, with every backward or repeated move refused
  - deleting allowed in setup and refused afterwards
  - the second of two conflicting actions refused
  - the link to the joining slice: an exercise created here is found by its join code; once ended, its code gives the unknown-code result
- **View tests** with `browserless-test-spring` and a mocked officer sign-in (role `OFFICER`):
  - the start screen's order and row content
  - the library list and scenario editor, including the conflict message
  - the exercise screen's code, link and confirmations
  - presentation mode showing positions fill up live, with two UIs: a crew UI takes a position while an officer UI watches
  - the QR code decoded in the test and compared with the join link
  - officer routes rejecting anonymous users, and crew routes still open to them

## Out of Scope

- Injects in any form: authoring, numbers, labels, pictures, copying at start.
- Reveals, reads, improvised injects, the timeline and the debrief.
- Changing an exercise's positions after it is created.
- Officers releasing a crew member's position.
- An exercise title, or anything else about the exercise shown to the crew beyond what the joining PRD specifies.
- Searching or filtering the library and the exercise list.
- Deleting or archiving started exercises, and any retention policy.
- Managing the officer allowlist from the UI.
- Live co-editing of scenarios.

## Further Notes

- Glossary updates made during design: **Exercise** now says the join code admits crew members until the exercise ends, and that afterwards nobody new comes in and no position changes hands. ADR-0005 was amended to match, and so was the joining PRD.
- Open for the debrief PRD: whether the debrief is shown on the crew's phones at all, as the **Debrief** entry in `CONTEXT.md` currently says, or only on the officer's machine on the training room's screen, with the crew away from their phones. Presentation mode here is a first step toward the latter.
- Open for the reveal PRD: whether two officers revealing in the same running exercise is supported or an accident. This slice lets any officer open any exercise.
