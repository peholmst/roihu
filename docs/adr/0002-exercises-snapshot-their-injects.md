# Exercises snapshot their injects

An exercise takes its own copy of the scenario's injects when it starts, rather
than referencing them. Editing a scenario therefore affects only exercises run after
the edit.

A debrief held months later has to show what that crew actually saw. A live
reference would rewrite the past every time an officer fixed a typo, and the
timeline is the evidence the debrief rests on — the same reason reveals are
append-only (ADR-0001).

## Consequences

The duplication between a scenario's injects and an exercise's is deliberate; do not
normalise it away into a foreign key. It also makes an improvised inject an ordinary
member of the exercise's inject set rather than a special case, since the exercise
owns its injects either way.

Positions are snapshotted the same way, for the same reason, but when the exercise is
created rather than when it starts: crew members take positions during setup, so the
exercise must own its positions before anyone can join it. The copy keeps the
scenario's order, names and call signs. It also protects the crew during the run: a
scenario edit must never rename or remove a position a crew member already holds.
