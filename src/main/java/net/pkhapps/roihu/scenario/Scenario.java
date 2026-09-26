package net.pkhapps.roihu.scenario;

/** A scenario in the library, with who created it and who last changed it. */
public record Scenario(ScenarioId id, ScenarioContent content, Change created, Change lastChanged) {
}
