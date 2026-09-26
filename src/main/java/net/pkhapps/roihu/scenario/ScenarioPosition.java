package net.pkhapps.roihu.scenario;

import java.util.Optional;

/**
 * A position as the scenario defines it. It always has a name, so that the crew's picker has
 * something to show. The call sign is written however the station writes it, and may be missing
 * or shared with another position.
 */
public record ScenarioPosition(String name, Optional<String> callSign) {

    public ScenarioPosition {
        if (name.isBlank()) {
            throw new IllegalArgumentException("A position needs a name");
        }
    }
}
