package net.pkhapps.roihu;

import net.pkhapps.roihu.exercise.CreateResult;
import net.pkhapps.roihu.exercise.JoinCode;

/** For tests that need an exercise to exist and care only about joining it. */
public final class TestExercises {

    private TestExercises() {
    }

    public static JoinCode joinCodeOf(CreateResult result) {
        if (result instanceof CreateResult.Created created) {
            return created.joinCode();
        }
        throw new AssertionError("No exercise was created: " + result);
    }
}
