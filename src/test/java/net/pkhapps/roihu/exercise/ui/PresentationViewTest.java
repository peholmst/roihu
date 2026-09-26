package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.WithOfficer;
import net.pkhapps.roihu.exercise.CreateResult;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.TestExercises.created;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;

/** The training room's screen, where the crew finds how to join. */
@IntegrationTest
@WithOfficer
class PresentationViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void presentationModeShowsTheJoinCodeItsQrCodeTheAppsAddressAndThePositions() throws Exception {
        var exercise = anExercise();

        var view = navigate("exercises/" + exercise.id().value() + "/presentation", PresentationView.class);

        assertThat(view.getElement().getTextRecursively())
                .contains(exercise.joinCode().toString())
                .contains("http://127.0.0.1:8080/join")
                .containsSubsequence("RVSP911 · Officer", "Free", "RVS911K · Pump operator", "Free");
        assertThat(QrCodes.decode(find(QrCode.class).single().png()))
                .isEqualTo("http://127.0.0.1:8080/join/" + exercise.joinCode());
    }

    @Test
    void presentationModeNeverShowsTheScenarioNameOrDescription() {
        var exercise = anExercise();

        var view = navigate("exercises/" + exercise.id().value() + "/presentation", PresentationView.class);

        assertThat(view.getElement().getOuterHTML())
                .doesNotContain("Warehouse fire")
                .doesNotContain("gas cylinders");
    }

    @Test
    void aDeletedExerciseLeavesTheRoomsScreenOnPresentationModeShowingNothingOfOtherExercises() {
        var exercise = anExercise();
        var other = anExercise();
        var view = navigate("exercises/" + exercise.id().value() + "/presentation", PresentationView.class);
        receivePush(view);

        exercises.delete(exercise.id());

        receivePush(view);
        assertThat(getCurrentView()).isSameAs(view);
        assertThat(view.getElement().getTextRecursively())
                .contains("This exercise is no longer available")
                .doesNotContain(exercise.joinCode().toString())
                .doesNotContain(other.joinCode().toString());
        assertThat(find(QrCode.class).all()).isEmpty();
    }

    @Test
    void anUnknownExerciseStaysOnPresentationMode() {
        anExercise();

        var view = navigate("exercises/00000000-0000-0000-0000-000000000000/presentation", PresentationView.class);

        assertThat(view.getElement().getTextRecursively())
                .isEqualTo("This exercise is no longer available");
    }

    /** Runs what the server pushed while no request was being handled. */
    private static void receivePush(Component view) {
        var session = view.getUI().orElseThrow().getSession();
        var request = CurrentInstance.get(VaadinRequest.class);
        var response = CurrentInstance.get(VaadinResponse.class);
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinResponse.class, null);
        try {
            session.getService().runPendingAccessTasks(session);
        } finally {
            CurrentInstance.set(VaadinRequest.class, request);
            CurrentInstance.set(VaadinResponse.class, response);
        }
    }

    private CreateResult.Created anExercise() {
        return created(exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.of("Unmarked gas cylinders in the back room"), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), BERTIL), BERTIL));
    }
}
