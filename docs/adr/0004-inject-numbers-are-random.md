# Inject numbers are random, not sequential

An inject's number is drawn at random from a wide range — #347, #812, #109 — rather
than counted up from 1.

A number is a permanent reference: an officer says it out loud during prep and
debrief, and writes it in notes that outlive the scenario. A sequential number would
be read as the inject's place in an order, and no such order exists — the officer
decides live what to reveal and when. Starting at #347 rather than #1 makes that
unmistakable at a glance.

## Consequences

Do not replace the allocator with a counter, and do not add a renumber action to
close the gaps left by deleted injects; both would invalidate references people have
already written down. Numbers are unique within a scenario and inherited by the
exercises snapshotted from it (ADR-0002), so an inject improvised during an exercise
simply draws another number rather than continuing a sequence.
