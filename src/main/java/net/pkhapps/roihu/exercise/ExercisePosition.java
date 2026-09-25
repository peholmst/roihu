package net.pkhapps.roihu.exercise;

import java.util.Optional;

/**
 * A position as the exercise holds it: its own copy, taken from the scenario (ADR-0002). Taken
 * when a crew member holds it, and never says who.
 */
public record ExercisePosition(PositionId id, String name, Optional<String> callSign, boolean taken) {
}
