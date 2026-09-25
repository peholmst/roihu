package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithAnonymousUser
class PositionViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void takingAFreePositionOpensThePositionScreen() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);

        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();

        assertThat(getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(getCurrentView().getElement().getTextRecursively())
                .contains("RVS911K · Pump operator")
                .contains("Not started yet")
                .contains("Injects revealed to RVS911K · Pump operator will appear here")
                .doesNotContain(joinCode.toString())
                .doesNotContain("RVSP911")
                .doesNotContain("Warehouse fire");
    }

    @Test
    void comingBackToTheExerciseReturnsToThePosition() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();

        navigate("join/" + joinCode, PositionView.class);
        navigate("join/" + joinCode + "/positions", PositionView.class);
    }

    @Test
    void reopeningTheBrowserReturnsToThePosition() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();

        reopenTheBrowser();

        navigate("join/" + joinCode, PositionView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("RVS911K · Pump operator");
    }

    @Test
    void changingPositionReturnsToThePickerWithThePositionFree() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();

        test(find(Button.class).withText("Change position").single()).click();

        assertThat(getCurrentView()).isInstanceOf(PositionPickerView.class);
        assertThat(test(find(Button.class).withText("RVS911K · Pump operator").single()).isUsable()).isTrue();
        reopenTheBrowser();
        navigate("join/" + joinCode, JoinView.class);
    }

    @Test
    void changingPositionAfterReopeningTheBrowserIsNotMistakenForATakeOver() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();
        reopenTheBrowser();
        navigate("join/" + joinCode, PositionView.class);

        test(find(Button.class).withText("Change position").single()).click();

        assertThat(getCurrentView()).isInstanceOf(PositionPickerView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).doesNotContain("taken over");
    }

    @Test
    void onceTheExerciseHasEndedTheHolderKeepsThePositionButCannotChangeIt() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();

        exercises.end(joinCode);

        navigate("join/" + joinCode, PositionView.class);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Exercise ended");
        assertThat(find(Button.class).withText("Change position").all()).isEmpty();
    }

    @Test
    void aHolderFindsTheirPositionAfterTheExerciseHasEndedHoweverTheyComeBack() {
        var joinCode = anExercise();
        navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        test(find(Button.class).withText("RVS911K · Pump operator").single()).click();
        exercises.end(joinCode);

        navigate("join/" + joinCode + "/positions", PositionView.class);

        navigate(JoinView.class);
        test(find(TextField.class).single()).setValue(joinCode.toString());
        test(find(Button.class).withText("Join").single()).click();
        assertThat(getCurrentView()).isInstanceOf(PositionView.class);
    }

    /** Starts a new session that carries only the cookies the browser was given. */
    private void reopenTheBrowser() {
        var cookies = new LinkedHashMap<String, Cookie>();
        mockResponse().getCookies().forEach(cookie -> cookies.put(cookie.getName(), cookie));
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        cookies.values().stream().filter(cookie -> cookie.getMaxAge() != 0).forEach(mockRequest()::addCookie);
    }

    private static MockResponse mockResponse() {
        ServletResponse response = (VaadinServletResponse) VaadinService.getCurrentResponse();
        while (response instanceof ServletResponseWrapper wrapper) {
            response = wrapper.getResponse();
        }
        return (MockResponse) response;
    }

    private static MockRequest mockRequest() {
        ServletRequest request = (VaadinServletRequest) VaadinService.getCurrentRequest();
        while (request instanceof ServletRequestWrapper wrapper) {
            request = wrapper.getRequest();
        }
        return (MockRequest) request;
    }

    private JoinCode anExercise() {
        return exercises.createFrom(scenarios.create("Warehouse fire", PreparedLanguage.FINNISH, List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))));
    }
}
