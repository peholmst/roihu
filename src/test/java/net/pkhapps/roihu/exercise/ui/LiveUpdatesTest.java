package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import net.pkhapps.roihu.IntegrationTest;
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

import static net.pkhapps.roihu.TestExercises.joinCodeOf;
import static net.pkhapps.roihu.TestOfficers.ANNA;
import static org.assertj.core.api.Assertions.assertThat;

/** Crew members' devices following what the others do, without reloading. */
@IntegrationTest
class LiveUpdatesTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

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
    void thePickerMarksAPositionTakenAsSoonAsSomeoneTakesIt() {
        var joinCode = anExercise();
        var watching = app.newUser().newWindow();
        var taking = app.newUser().newWindow();
        watching.navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        takePumpOperator(taking, joinCode);

        receivePush(watching);
        assertThat(watching.getCurrentView().getElement().getTextRecursively())
                .containsSubsequence("RVS911K · Pump operator", "Taken");
    }

    @Test
    void thePickerMarksAPositionFreeAsSoonAsItsHolderChangesPosition() {
        var joinCode = anExercise();
        var watching = app.newUser().newWindow();
        var leaving = app.newUser().newWindow();
        takePumpOperator(leaving, joinCode);
        watching.navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        leaving.test(leaving.find(Button.class).withText("Change position").single()).click();

        receivePush(watching);
        assertThat(watching.getCurrentView().getElement().getTextRecursively()).doesNotContain("Taken");
    }

    @Test
    void aDisplacedDeviceReturnsToThePickerAsSoonAsItsPositionIsTakenOver() {
        var joinCode = anExercise();
        var displaced = app.newUser().newWindow();
        var newDevice = app.newUser().newWindow();
        takePumpOperator(displaced, joinCode);

        newDevice.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        newDevice.test(newDevice.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        newDevice.test(newDevice.find(ConfirmDialog.class).single()).confirm();

        receivePush(displaced);
        assertThat(displaced.getCurrentView()).isInstanceOf(PositionPickerView.class);
        assertThat(displaced.getCurrentView().getElement().getTextRecursively())
                .contains("Your position was taken over on another device");
        assertThat(newDevice.getCurrentView()).isInstanceOf(PositionView.class);
    }

    @Test
    void aDisplacedDeviceKeepsItsMessageWhileThePickerUpdates() {
        var joinCode = anExercise();
        var displaced = app.newUser().newWindow();
        var newDevice = app.newUser().newWindow();
        var another = app.newUser().newWindow();
        takePumpOperator(displaced, joinCode);
        newDevice.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        newDevice.test(newDevice.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        newDevice.test(newDevice.find(ConfirmDialog.class).single()).confirm();
        receivePush(displaced);

        another.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        another.test(another.find(Button.class).withText("RVSP911 · Officer").single()).click();

        receivePush(displaced);
        assertThat(displaced.getCurrentView().getElement().getTextRecursively())
                .contains("Your position was taken over on another device")
                .containsSubsequence("RVSP911 · Officer", "Taken");
    }

    @Test
    void thePickerLeavesForTheJoinScreenWhenTheExerciseEnds() {
        var joinCode = anExercise();
        var watching = app.newUser().newWindow();
        watching.navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        exercises.end(joinCode);

        receivePush(watching);
        assertThat(watching.getCurrentView()).isInstanceOf(JoinView.class);
    }

    @Test
    void thePickerFollowsTheExerciseItShowsAfterMovingToAnother() {
        var first = anExercise();
        var second = anExercise();
        var watching = app.newUser().newWindow();
        var taking = app.newUser().newWindow();
        var picker = watching.navigate("join/" + first + "/positions", PositionPickerView.class);
        assertThat(watching.navigate("join/" + second + "/positions", PositionPickerView.class)).isSameAs(picker);
        receivePush(watching);

        takePumpOperator(taking, second);

        receivePush(watching);
        assertThat(watching.getCurrentView().getElement().getTextRecursively())
                .containsSubsequence("RVS911K · Pump operator", "Taken");
    }

    /** Runs what the server pushed to the window while another one was busy. */
    private static void receivePush(BrowserlessUIContext window) {
        window.activate();
        var session = window.getUser().getSession();
        session.lock();
        try {
            session.getService().runPendingAccessTasks(session);
        } finally {
            session.unlock();
        }
    }

    private static void takePumpOperator(BrowserlessUIContext window, JoinCode joinCode) {
        window.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        window.test(window.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        assertThat(window.getCurrentView()).isInstanceOf(PositionView.class);
    }

    private JoinCode anExercise() {
        return joinCodeOf(exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), ANNA), ANNA));
    }
}
