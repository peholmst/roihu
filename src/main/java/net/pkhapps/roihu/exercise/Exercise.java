package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.Change;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * An exercise as the officers see it. The scenario name is the one the scenario had when the
 * exercise was created, and never reaches the crew.
 */
public record Exercise(ExerciseId id, String scenarioName, ExerciseState state, JoinCode joinCode,
                       Change created, Optional<Instant> started, Optional<Instant> ended,
                       List<ExercisePosition> positions) {
}
