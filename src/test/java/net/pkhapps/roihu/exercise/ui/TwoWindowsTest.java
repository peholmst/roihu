package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.button.Button;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExercisePosition;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** One crew member's browser with the exercise open in two windows at once. */
@IntegrationTest
class TwoWindowsTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Autowired
    CrewJoining crewJoining;

    private SecuredBrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        app = SpringBrowserlessApplicationContext.createSecured(applicationContext, PositionPickerView.class);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void aBrowserThatAlreadyHoldsAPositionCannotTakeASecondOne() {
        var joinCode = anExercise();
        var browser = app.newUser();
        var first = browser.newWindow();
        var second = browser.newWindow();
        first.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        second.navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        first.test(first.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        second.test(second.find(Button.class).withText("RVSP911 · Officer").single()).click();

        assertThat(second.getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(second.getCurrentView().getElement().getTextRecursively()).contains("RVS911K · Pump operator");
        assertThat(crewJoining.findExercise(joinCode.toString()).orElseThrow().positions())
                .extracting(ExercisePosition::name, ExercisePosition::taken)
                .containsExactly(tuple("Officer", false), tuple("Pump operator", true));
    }

    private JoinCode anExercise() {
        return exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), ANNA));
    }
}
