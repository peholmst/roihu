package net.pkhapps.roihu.scenario.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.ui.JoinView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Officer routes need a signed-in officer; crew routes need nobody at all (ADR-0005). */
@IntegrationTest
@WithAnonymousUser
class OfficerRoutesTest extends SpringBrowserlessTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "scenarios", "scenarios/edit", "exercises/00000000-0000-0000-0000-000000000000"})
    void anAnonymousUserIsSentToSignInInsteadOfSeeingAnOfficerRoute(String route) {
        UI.getCurrent().navigate(route);

        assertThat(UI.getCurrent().getInternals().getActiveRouterTargetsChain()).isEmpty();
        assertThat(pendingJavaScript()).anySatisfy(js -> assertThat(js).contains("/oauth2/authorization/keycloak"));
    }

    @Test
    void anAnonymousUserStillReachesTheCrewsJoinScreen() {
        assertThat(navigate(JoinView.class)).isInstanceOf(JoinView.class);
    }

    private static List<String> pendingJavaScript() {
        return UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(PendingJavaScriptInvocation::getInvocation)
                .map(invocation -> invocation.getExpression() + " " + invocation.getParameters())
                .toList();
    }
}
