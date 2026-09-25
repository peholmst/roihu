package net.pkhapps.roihu.exercise;

import java.util.Optional;

/** A position as the exercise holds it: its own copy, taken from the scenario (ADR-0002). */
public record ExercisePosition(String name, Optional<String> callSign) {
}
