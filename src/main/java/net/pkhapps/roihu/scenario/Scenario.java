package net.pkhapps.roihu.scenario;

/**
 * A scenario in the library, with who created it and who last changed it. The version says which
 * state of the scenario this is, and a save names the version it was made from.
 */
public record Scenario(ScenarioId id, ScenarioContent content, Change created, Change lastChanged, int version) {
}
