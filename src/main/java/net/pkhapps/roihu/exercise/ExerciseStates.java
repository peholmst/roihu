package net.pkhapps.roihu.exercise;

/** Between {@link ExerciseState} and the stored state, whose generated name clashes with it. */
final class ExerciseStates {

    private ExerciseStates() {
    }

    static ExerciseState of(net.pkhapps.roihu.db.generated.enums.ExerciseState stored) {
        return switch (stored) {
            case setup -> ExerciseState.SETUP;
            case running -> ExerciseState.RUNNING;
            case ended -> ExerciseState.ENDED;
        };
    }
}
