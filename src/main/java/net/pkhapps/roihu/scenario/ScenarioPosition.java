package net.pkhapps.roihu.scenario;

import java.util.Optional;

/**
 * A position as the scenario defines it. The call sign is written however the station writes
 * it, and may be missing or shared with another position.
 */
public record ScenarioPosition(String name, Optional<String> callSign) {
}
