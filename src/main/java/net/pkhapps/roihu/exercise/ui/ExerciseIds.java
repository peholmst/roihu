package net.pkhapps.roihu.exercise.ui;

import net.pkhapps.roihu.exercise.ExerciseId;

import java.util.Optional;
import java.util.UUID;

/** Exercise ids as they come in route parameters, which anyone can type. */
final class ExerciseIds {

    private ExerciseIds() {
    }

    static Optional<ExerciseId> parse(String id) {
        try {
            return Optional.of(new ExerciseId(UUID.fromString(id)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }
}
