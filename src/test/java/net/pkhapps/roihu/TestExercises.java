package net.pkhapps.roihu;

import net.pkhapps.roihu.exercise.CreateResult;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.exercise.LifecycleResult;

/** For tests that need an exercise to exist and care only about joining it. */
public final class TestExercises {

    private TestExercises() {
    }

    public static CreateResult.Created created(CreateResult result) {
        if (result instanceof CreateResult.Created created) {
            return created;
        }
        throw new AssertionError("No exercise was created: " + result);
    }

    public static JoinCode joinCodeOf(CreateResult result) {
        return created(result).joinCode();
    }

    /** Runs the exercise from setup to its end, as an officer would. */
    public static void startAndEnd(Exercises exercises, CreateResult.Created exercise) {
        if (!(exercises.start(exercise.id()) instanceof LifecycleResult.Done)
                || !(exercises.end(exercise.id()) instanceof LifecycleResult.Done)) {
            throw new AssertionError("The exercise could not be run to its end: " + exercise);
        }
    }
}
