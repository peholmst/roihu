package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
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

import static org.assertj.core.api.Assertions.assertThat;

/** Two crew members' devices, one of which takes over the position the other holds. */
@IntegrationTest
class TakeOverTest {

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
    void theDisplacedDeviceReturnsToThePickerWhenItNextComesBack() {
        var joinCode = anExercise();
        var displaced = app.newUser().newWindow();
        var newDevice = app.newUser().newWindow();
        takePumpOperator(displaced, joinCode);

        takeOverPumpOperator(newDevice, joinCode);

        displaced.navigate("join/" + joinCode + "/position", PositionPickerView.class);
        assertThat(displaced.getCurrentView().getElement().getTextRecursively())
                .contains("Your position was taken over on another device");
        assertThat(newDevice.getCurrentView()).isInstanceOf(PositionView.class);
    }

    @Test
    void theMessageIsShownOnceAndTheDeviceCanTakeAPositionAgain() {
        var joinCode = anExercise();
        var displaced = app.newUser().newWindow();
        var newDevice = app.newUser().newWindow();
        takePumpOperator(displaced, joinCode);
        takeOverPumpOperator(newDevice, joinCode);
        displaced.navigate("join/" + joinCode + "/position", PositionPickerView.class);

        displaced.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        assertThat(displaced.getCurrentView().getElement().getTextRecursively())
                .doesNotContain("taken over");

        displaced.test(displaced.find(Button.class).withText("RVSP911 · Officer").single()).click();
        assertThat(displaced.getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(displaced.getCurrentView().getElement().getTextRecursively()).contains("RVSP911 · Officer");
    }

    private static void takePumpOperator(BrowserlessUIContext window, JoinCode joinCode) {
        window.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        window.test(window.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        assertThat(window.getCurrentView()).isInstanceOf(PositionView.class);
    }

    private static void takeOverPumpOperator(BrowserlessUIContext window, JoinCode joinCode) {
        window.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        window.test(window.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        window.test(window.find(ConfirmDialog.class).single()).confirm();
        assertThat(window.getCurrentView()).isInstanceOf(PositionView.class);
    }

    private JoinCode anExercise() {
        return exercises.createFrom(scenarios.create("Warehouse fire", PreparedLanguage.FINNISH, List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))));
    }
}
