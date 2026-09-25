package net.pkhapps.roihu.exercise;

import java.util.UUID;

/** Identifies one of an exercise's own positions, never a scenario's. */
public record PositionId(UUID value) {
}
