# Tabletop Exercise

A web app for running small-scale tabletop exercises for fire crews. The training
officer drives the simulation by hand — there is no automation — and the app is
the channel through which information reaches the crew. All other interaction
happens over real radios, outside the software.

## Language

**Scenario**:
A reusable preparation for an exercise: the simulated incident and the injects that
describe it. Prepared ahead of time and run many times.
_Avoid_: Simulation, template, case

**Exercise**:
One run of a scenario with a particular crew at a particular time. Owns what
happened during that run and the record of it afterwards. Moves through setup,
running and ended; its join code stays live throughout, so a position can be taken
or retaken at any point.
_Avoid_: Session, simulation, run, game

**Position**:
A function on the fire ground that the scenario expects someone to occupy, such as
Incident Commander or Pump Operator. Defined by the scenario and filled by a
person for the duration of one exercise. Injects are revealed to positions, not to
people.
_Avoid_: Role (reserved for access control), seat, slot, assignment

**Crew Member**:
A firefighter taking part in an exercise, occupying exactly one position. Known to
the system only for the duration of that exercise. Reads the injects revealed
to their position and does everything else over the radio.
_Avoid_: Player, trainee, student, participant, user

**Training Officer**:
The person who runs the exercise. Sole author of scenarios and injects, and the
only one who can reveal one. Acts as the simulation engine; the app automates
none of their judgement.
_Avoid_: Instructor, facilitator, game master, admin

**Reveal**:
The act of giving one position access to one inject at a point in time. A reveal
always names exactly one position; revealing an inject to several positions at once —
all of them, or any subset the officer picks — fans out into one reveal each, sharing
the instant. Permanent: a reveal is never withdrawn, erased or edited, because a crew
member cannot unsee what they have read.
_Avoid_: Share, send, publish, unlock, push

**Timeline**:
The ordered record of every reveal in an exercise, each carrying when it was read,
and the exercise's only state. What a crew member can see is exactly the reveals
aimed at their position; what the debrief discusses is the same record end to end.
_Avoid_: Log, audit trail, history, event feed

**Inject**:
One piece of information about the simulated incident — text, pictures, or both,
carrying a number and a short label. A scenario's injects are living and editable;
an exercise holds frozen copies, taken when it starts.
_Avoid_: Info card, card, clue, message, note, item

**Number**:
The permanent identifier an inject carries within its scenario, written #347. Drawn
at random from a wide range rather than counted up, so that it can never be read as
the inject's place in an order: the first inject of a scenario is #347, not #1.
Assigned once, never reused, never renumbered, and what an officer says out loud to
refer to an inject.
_Avoid_: Index, sequence number, order, position

**Label**:
The short name an officer gives an inject. Always visible to the officer and in the
timeline; visible to a crew member, with its number, only once they have opened the
inject, so that an unopened inject announces nothing but its own arrival.
_Avoid_: Title, headline, subject, name

**Improvised Inject**:
An inject the officer writes during a running exercise, in response to what is
happening on the radio. Belongs to that exercise alone and has no scenario ancestor.
_Avoid_: Ad-hoc inject, custom inject, on-the-fly card, improvised card

**Debrief**:
The discussion after an exercise has ended, read off its timeline: who knew what,
and when they came to know it. The officer opens it deliberately, which is what puts
the whole timeline on the crew's phones; read times stay on her screen alone.
_Avoid_: Review, after-action report, retrospective, post-mortem

**Read**:
The moment a position first opens an inject revealed to it. Recorded once per reveal.
Separates "the crew was never told" from "the crew was told and did not look"; it is
not an acknowledgement, and the crew is never asked to confirm anything.
_Avoid_: Seen, viewed, acknowledged, delivered, receipt
