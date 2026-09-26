package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.Cookie;
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
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Which language the crew's screens speak, and how a crew member changes it. */
@IntegrationTest
class InterfaceLanguageTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    private SecuredBrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        app = SpringBrowserlessApplicationContext.createSecured(applicationContext, JoinView.class);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void theInterfaceSpeaksTheBrowsersLanguageWhenItIsOneOfOurs() {
        var window = aBrowserPreferring(Locale.of("sv", "FI")).newWindow();

        window.navigate(JoinView.class);

        assertThat(window.find(H1.class).single().getText()).isEqualTo("Delta i en övning");
    }

    @Test
    void otherwiseTheInterfaceSpeaksTheDeploymentsDefaultWhichIsFinnish() {
        var window = aBrowserPreferring(Locale.GERMANY).newWindow();

        window.navigate(JoinView.class);

        assertThat(window.find(H1.class).single().getText()).isEqualTo("Liity harjoitukseen");
    }

    @Test
    void theSwitcherOnTheJoinScreenChangesTheLanguageAndKeepsWhatWasTyped() {
        var window = aBrowserPreferring(Locale.US).newWindow();
        window.navigate(JoinView.class);
        window.test(window.find(TextField.class).single()).setValue("K7QX");

        window.test(window.find(Button.class).withText("SV").single()).click();

        assertThat(window.find(H1.class).single().getText()).isEqualTo("Delta i en övning");
        assertThat(window.find(TextField.class).single().getLabel()).isEqualTo("Deltagarkod");
        assertThat(window.find(TextField.class).single().getValue()).isEqualTo("K7QX");
        assertThat(window.find(Button.class).withText("Delta").all()).hasSize(1);
    }

    @Test
    void theChoiceIsRememberedWhenTheBrowserComesBack() {
        var browser = aBrowserPreferring(Locale.US);
        var window = browser.newWindow();
        window.navigate(JoinView.class);
        window.test(window.find(Button.class).withText("SV").single()).click();
        var cookies = cookiesGivenTo(window);

        var reloaded = aBrowserPreferring(Locale.US, cookies).newWindow();
        reloaded.navigate(JoinView.class);

        assertThat(reloaded.find(H1.class).single().getText()).isEqualTo("Delta i en övning");
    }

    @Test
    void theSwitcherOnThePositionScreenChangesTheLanguageButNotThePosition() {
        var joinCode = anExercise();
        var window = aBrowserPreferring(Locale.of("fi", "FI")).newWindow();
        window.navigate("join/" + joinCode + "/positions", PositionPickerView.class);
        window.test(window.find(Button.class).withText("RVS911K · Pump operator").single()).click();
        assertThat(window.getCurrentView().getElement().getTextRecursively())
                .contains("RVS911K · Pump operator")
                .contains("Harjoitus ei ole vielä alkanut");

        window.test(window.find(Button.class).withText("SV").single()).click();

        assertThat(window.getCurrentView()).isInstanceOf(PositionView.class);
        assertThat(window.find(H1.class).single().getText()).isEqualTo("RVS911K · Pump operator");
        assertThat(window.getCurrentView().getElement().getTextRecursively())
                .contains("Övningen har inte börjat ännu")
                .contains("Inspel som visas för RVS911K · Pump operator kommer att synas här")
                .contains("Byt befattning");
    }

    @Test
    void thePageDeclaresTheLanguageItSpeaks() {
        var window = aBrowserPreferring(Locale.of("sv", "FI")).newWindow();
        window.navigate(JoinView.class);
        assertThat(pageLanguage(window)).contains("sv");

        window.test(window.find(Button.class).withText("FI").single()).click();

        assertThat(pageLanguage(window)).contains("fi");
    }

    /**
     * The language the page was last told it is in, so that a screen reader reads it with the
     * right voice, or nothing if it has not been told since it was last asked.
     */
    private static Optional<String> pageLanguage(BrowserlessUIContext window) {
        window.roundTrip();
        return window.getUI().getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(PendingJavaScriptInvocation::getInvocation)
                .filter(invocation -> invocation.getExpression().contains("document.documentElement.lang"))
                .map(invocation -> String.valueOf(invocation.getParameters().getFirst()))
                .reduce((earlier, later) -> later);
    }

    /** The cookies the browser holds after what {@code window} did: the ones it was given. */
    private static Cookie[] cookiesGivenTo(BrowserlessUIContext window) {
        window.activate();
        ServletResponse response = (VaadinServletResponse) VaadinService.getCurrentResponse();
        while (response instanceof ServletResponseWrapper wrapper) {
            response = wrapper.getResponse();
        }
        return ((MockResponse) response).getCookies().stream()
                .filter(cookie -> cookie.getMaxAge() != 0)
                .toArray(Cookie[]::new);
    }

    /**
     * A user whose browser asks for {@code locale}. Its requests are shared by all its windows, so
     * the window opened to reach them is closed again before any window the test uses.
     */
    private BrowserlessUserContext aBrowserPreferring(Locale locale, Cookie... cookies) {
        var browser = app.newUser();
        try (var probe = browser.newWindow()) {
            probe.activate();
            var request = mockRequest();
            request.setLocaleInt(locale);
            for (var cookie : cookies) {
                request.addCookie(cookie);
            }
        }
        return browser;
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
