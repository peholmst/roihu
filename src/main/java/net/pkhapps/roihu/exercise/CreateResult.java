package net.pkhapps.roihu.exercise;

/** What came of trying to create an exercise from a scenario. */
public sealed interface CreateResult {

    /** The exercise is in setup, and its join code admits crew members. */
    record Created(ExerciseId id, JoinCode joinCode) implements CreateResult {
    }

    /** The scenario has no positions, so nobody could join the exercise. */
    record NoPositions() implements CreateResult {
    }

    /** Someone deleted the scenario. */
    record ScenarioGone() implements CreateResult {
    }
}
