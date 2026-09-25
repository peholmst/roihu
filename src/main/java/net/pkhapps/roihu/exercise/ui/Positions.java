package net.pkhapps.roihu.exercise.ui;

import net.pkhapps.roihu.exercise.ExercisePosition;

/** How crew screens name a position: by call sign and name, as it is said on the radio. */
final class Positions {

    private Positions() {
    }

    static String describe(ExercisePosition position) {
        return position.callSign()
                .map(callSign -> callSign + " · " + position.name())
                .orElse(position.name());
    }
}
