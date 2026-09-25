package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithAnonymousUser
class PositionPickerViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void thePickerShowsThePositionsInScenarioOrderByCallSignAndName() {
        var joinCode = anExercise();

        var picker = navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        assertThat(picker.getElement().getTextRecursively())
                .containsSubsequence("RVSP911 · Officer", "RVS911K · Pump operator", "Safety officer")
                .doesNotContain("· Safety officer");
    }

    @Test
    void thePickerShowsThePreparedLanguageAndTheStateButNotTheScenario() {
        var joinCode = anExercise();

        var picker = navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        assertThat(picker.getElement().getTextRecursively())
                .contains("Injects are in Swedish")
                .contains("Not started yet")
                .doesNotContain("Warehouse fire");
    }

    @Test
    void aCodeThatFindsNoExerciseLeadsBackToTheJoinScreen() {
        navigate("join/ZZZZ-ZZZZ/positions", JoinView.class);
    }

    private JoinCode anExercise() {
        return exercises.createFrom(scenarios.create("Warehouse fire", PreparedLanguage.SWEDISH, List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty()))));
    }
}
