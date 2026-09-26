package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.WithOfficer;
import net.pkhapps.roihu.exercise.CreateResult;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioId;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithOfficer
class ExercisesViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void theStartScreenListsExercisesUnderWayFirstWithTheirScenarioStateCreatorTimeAndPositionsTaken(
            @Autowired CrewJoining crewJoining) {
        var ended = created(scenario("Harbour fire"), ANNA);
        exercises.end(ended.joinCode());
        var inSetup = created(scenario("Quarry fire"), BERTIL);
        var officer = crewJoining.findExercise(inSetup.joinCode().toString()).orElseThrow().positions().getFirst();
        crewJoining.take(inSetup.joinCode().toString(), officer.id());

        navigate(ExercisesView.class);

        var setupRow = rowOf("Quarry fire");
        var endedRow = rowOf("Harbour fire");
        assertThat(setupRow).isLessThan(endedRow);
        assertThat(cell(setupRow, 1)).isEqualTo("Setup");
        assertThat(cell(setupRow, 2)).isEqualTo(BERTIL.email());
        assertThat(cell(setupRow, 3)).startsWith("Created ");
        assertThat(cell(setupRow, 4)).isEqualTo("1 of 2");
        assertThat(cell(endedRow, 1)).isEqualTo("Ended");
        assertThat(cell(endedRow, 3)).startsWith("Ended ");
        assertThat(cell(endedRow, 4)).isEqualTo("0 of 2");
    }

    @Test
    void anyOfficersExerciseOpensFromTheStartScreen() {
        created(scenario("Tower fire"), BERTIL);
        navigate(ExercisesView.class);

        test(grid()).clickRow(rowOf("Tower fire"));

        assertThat(getCurrentView()).isInstanceOf(ExerciseView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Tower fire");
    }

    @Test
    void anOfficerCanSignOut() {
        navigate(ExercisesView.class);

        assertThat(find(Button.class).withText("Sign out").all()).hasSize(1);
    }

    private ScenarioId scenario(String name) {
        return scenarios.create(new ScenarioContent(name, PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.empty()))), ANNA);
    }

    private CreateResult.Created created(ScenarioId scenario, net.pkhapps.roihu.base.security.Officer officer) {
        return (CreateResult.Created) exercises.createFrom(scenario, officer);
    }

    @SuppressWarnings("unchecked")
    private Grid<Object> grid() {
        return find(Grid.class).single();
    }

    private int rowOf(String scenarioName) {
        var grid = test(grid());
        return IntStream.range(0, grid.size())
                .filter(row -> grid.getCellText(row, 0).equals(scenarioName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No exercise of " + scenarioName + " is listed"));
    }

    private String cell(int row, int column) {
        return test(grid()).getCellText(row, column);
    }
}
