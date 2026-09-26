package net.pkhapps.roihu.scenario;

import java.util.List;
import java.util.Optional;

/**
 * What an officer writes into a scenario, saved as one unit. The description is for other
 * officers and never reaches the crew. The positions are in the order the crew's picker lists
 * them, and there may be none yet.
 */
public record ScenarioContent(String name, PreparedLanguage preparedLanguage, Optional<String> description,
                              List<ScenarioPosition> positions) {

    public ScenarioContent {
        positions = List.copyOf(positions);
    }
}
