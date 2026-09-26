package net.pkhapps.roihu.scenario;

/** What the library shows of a scenario, enough to tell scenarios apart without opening them. */
public record ScenarioSummary(ScenarioId id, String name, PreparedLanguage preparedLanguage, int positionCount,
                              Change lastChanged) {
}
