package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.Change;

import java.time.Instant;
import java.util.Optional;

/** What the officers' list shows of an exercise, enough to tell runs of one scenario apart. */
public record ExerciseSummary(ExerciseId id, String scenarioName, ExerciseState state, Change created,
                              Optional<Instant> started, Optional<Instant> ended, int positionsTaken,
                              int positionCount) {
}
