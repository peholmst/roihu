package net.pkhapps.roihu.scenario.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.WithOfficer;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.ScenarioSummary;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithOfficer
class ScenarioLibraryViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Test
    void theLibraryShowsEachScenariosNameLanguagePositionCountAndLastChange() {
        scenarios.create(new ScenarioContent("Tunnel fire", PreparedLanguage.SWEDISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), BERTIL);

        navigate(ScenarioLibraryView.class);

        var row = rowOf("Tunnel fire");
        assertThat(cell(row, 1)).isEqualTo("Swedish");
        assertThat(cell(row, 2)).isEqualTo("2");
        assertThat(cell(row, 3)).contains(BERTIL.email());
    }

    @SuppressWarnings("unchecked")
    private Grid<ScenarioSummary> grid() {
        return find(Grid.class).single();
    }

    private int rowOf(String name) {
        var grid = test(grid());
        return IntStream.range(0, grid.size())
                .filter(row -> grid.getCellText(row, 0).equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No scenario named " + name + " in the library"));
    }

    private String cell(int row, int column) {
        return test(grid()).getCellText(row, column);
    }
}
