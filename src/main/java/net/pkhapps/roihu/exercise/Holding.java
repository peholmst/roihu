package net.pkhapps.roihu.exercise;

/** A position held by a crew member, and the state of the exercise it belongs to. */
public record Holding(JoinCode joinCode, ExercisePosition position, ExerciseState state) {
}
