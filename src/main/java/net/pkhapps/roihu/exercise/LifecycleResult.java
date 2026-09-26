package net.pkhapps.roihu.exercise;

/** What came of trying to start, end or delete an exercise. */
public sealed interface LifecycleResult {

    /** The exercise moved on, or is gone as asked. */
    record Done() implements LifecycleResult {
    }

    /** The exercise is not in the state the move is from, having been moved by someone else. */
    record Refused(ExerciseState state) implements LifecycleResult {
    }

    /** Someone deleted the exercise. */
    record Gone() implements LifecycleResult {
    }
}
