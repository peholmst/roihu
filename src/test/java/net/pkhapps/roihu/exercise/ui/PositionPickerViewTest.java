package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.exercise.TakeResult;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.TestExercises.joinCodeOf;
import static net.pkhapps.roihu.TestOfficers.ANNA;
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
    void neitherThePickerNorThePositionScreenShowsTheScenariosDescription() {
        var joinCode = joinCodeOf(exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.SWEDISH, Optional.of("The fire starts in the paint store"),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA), ANNA));

        var picker = navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        assertThat(picker.getElement().getTextRecursively()).doesNotContain("paint store");

        test(find(Button.class).withText("RVSP911 · Officer").single()).click();
        assertThat(getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).doesNotContain("paint store");
    }

    @Test
    void takenPositionsAreMarked(@Autowired CrewJoining crewJoining) {
        var joinCode = anExercise();
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        crewJoining.take(joinCode.toString(), officer.id());

        var picker = navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        assertThat(picker.getElement().getTextRecursively())
                .containsSubsequence("RVSP911 · Officer", "Taken", "RVS911K · Pump operator")
                .doesNotContain("Pump operatorTaken");
    }

    @Test
    void takingATakenPositionAsksBeforeTakingItOver(@Autowired CrewJoining crewJoining) {
        var joinCode = anExercise();
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        crewJoining.take(joinCode.toString(), officer.id());
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        test(find(Button.class).withText("RVSP911 · Officer").single()).click();

        var confirmation = test(find(ConfirmDialog.class).single());
        assertThat(confirmation.getHeader()).isEqualTo("Already taken — take it over?");
        assertThat(getCurrentView()).isInstanceOf(PositionPickerView.class);
    }

    @Test
    void confirmingTheTakeOverOpensThePositionScreen(@Autowired CrewJoining crewJoining) {
        var joinCode = anExercise();
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        crewJoining.take(joinCode.toString(), officer.id());
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVSP911 · Officer").single()).click();

        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("RVSP911 · Officer");
    }

    @Test
    void cancellingTheTakeOverLeavesTheHolderInPlace(@Autowired CrewJoining crewJoining) {
        var joinCode = anExercise();
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        var holder = ((TakeResult.Taken) crewJoining.take(joinCode.toString(), officer.id())).token();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVSP911 · Officer").single()).click();

        test(find(ConfirmDialog.class).single()).cancel();

        assertThat(getCurrentView()).isInstanceOf(PositionPickerView.class);
        assertThat(crewJoining.findHolding(holder)).isPresent();
    }

    @Test
    void aPositionTakenSinceThePickerWasShownIsOfferedForTakeOver(@Autowired CrewJoining crewJoining) {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        crewJoining.take(joinCode.toString(), officer.id());

        test(find(Button.class).withText("RVSP911 · Officer").single()).click();

        assertThat(test(find(ConfirmDialog.class).single()).getHeader()).isEqualTo("Already taken — take it over?");
    }

    @Test
    void aCodeThatFindsNoExerciseLeadsBackToTheJoinScreen() {
        navigate("join/ZZZZ-ZZZZ/positions", JoinView.class);
    }

    private JoinCode anExercise() {
        return joinCodeOf(exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.SWEDISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty()))), ANNA), ANNA));
    }
}
