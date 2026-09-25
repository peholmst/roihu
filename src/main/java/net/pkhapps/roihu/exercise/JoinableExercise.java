package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.PreparedLanguage;

import java.util.List;

/**
 * What a join code shows a crew member before they take a position. Deliberately without the
 * scenario's name, which would give the incident away.
 */
public record JoinableExercise(ExerciseState state, PreparedLanguage preparedLanguage,
                               List<ExercisePosition> positions) {
}
