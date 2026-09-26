package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.WithOfficer;
import net.pkhapps.roihu.exercise.CreateResult;
import net.pkhapps.roihu.exercise.ExerciseState;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithOfficer
class ExerciseViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void anyOfficersExerciseShowsItsStateScenarioAndJoinCodeWithALinkToCopy() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(new ScenarioContent(
                "Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), BERTIL), BERTIL);

        var view = navigate("exercises/" + created.id().value(), ExerciseView.class);

        assertThat(view.getElement().getTextRecursively())
                .contains("Warehouse fire")
                .contains("Not started yet")
                .contains(created.joinCode().toString());
        var link = find(TextField.class).withCaption("Join link").single();
        // The address the officer's browser reached the application at, which the test's is.
        assertThat(link.getValue()).isEqualTo("http://127.0.0.1:8080/join/" + created.joinCode());
        assertThat(link.isReadOnly()).isTrue();
        assertThat(find(Button.class).withText("Copy link").all()).hasSize(1);
    }

    @Test
    void startingAnExerciseInSetupAsksFirst() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);
        assertThat(find(Button.class).withText("End exercise").all()).isEmpty();

        test(find(Button.class).withText("Start exercise").single()).click();
        var confirmation = test(find(ConfirmDialog.class).single());
        assertThat(confirmation.getHeader()).isEqualTo("Start the exercise?");
        confirmation.cancel();
        assertThat(exercises.get(created.id()).orElseThrow().state()).isEqualTo(ExerciseState.SETUP);

        test(find(Button.class).withText("Start exercise").single()).click();
        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(exercises.get(created.id()).orElseThrow().state()).isEqualTo(ExerciseState.RUNNING);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Exercise running");
        assertThat(find(Button.class).withText("Start exercise").all()).isEmpty();
        assertThat(find(Button.class).withText("Delete exercise").all()).isEmpty();
        assertThat(find(Button.class).withText("End exercise").all()).hasSize(1);
    }

    @Test
    void endingARunningExerciseWarnsThatItCannotBeUndone() {
        var created = anExercise();
        exercises.start(created.id());
        navigate("exercises/" + created.id().value(), ExerciseView.class);

        test(find(Button.class).withText("End exercise").single()).click();
        var confirmation = test(find(ConfirmDialog.class).single());
        assertThat(confirmation.getHeader()).isEqualTo("End the exercise?");
        assertThat(confirmation.getText()).contains("cannot be undone");
        confirmation.confirm();

        assertThat(exercises.get(created.id()).orElseThrow().state()).isEqualTo(ExerciseState.ENDED);
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Exercise ended");
        assertThat(find(Button.class).all()).extracting(Button::getText)
                .doesNotContain("Start exercise", "End exercise", "Delete exercise");
    }

    @Test
    void anExerciseInSetupCanBeDeletedAfterConfirming() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);

        test(find(Button.class).withText("Delete exercise").single()).click();
        var confirmation = test(find(ConfirmDialog.class).single());
        assertThat(confirmation.getHeader()).isEqualTo("Delete the exercise?");
        confirmation.cancel();
        assertThat(exercises.get(created.id())).isPresent();

        test(find(Button.class).withText("Delete exercise").single()).click();
        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(exercises.get(created.id())).isEmpty();
        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
        assertThat(find(Notification.class).all()).isEmpty();
    }

    @Test
    void startingAnExerciseAnotherOfficerHasAlreadyStartedSaysSoAndShowsItRunning() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);
        test(find(Button.class).withText("Start exercise").single()).click();

        exercises.start(created.id());
        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Another officer has already started this exercise");
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Exercise running");
        assertThat(find(Button.class).withText("End exercise").all()).hasSize(1);
    }

    @Test
    void deletingAnExerciseAnotherOfficerHasAlreadyStartedIsRefused() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);
        test(find(Button.class).withText("Delete exercise").single()).click();

        exercises.start(created.id());
        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(exercises.get(created.id())).isPresent();
        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Another officer has already started this exercise");
        assertThat(getCurrentView()).isInstanceOf(ExerciseView.class);
    }

    @Test
    void startingAnExerciseAnotherOfficerHasDeletedSaysSoAndLeadsBackToTheStartScreen() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);
        test(find(Button.class).withText("Start exercise").single()).click();

        exercises.delete(created.id());
        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Another officer has deleted this exercise");
        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
    }

    @Test
    void theExerciseScreenShowsAnotherOfficerStartingAndEndingItAsSoonAsTheyDo() {
        var created = anExercise();
        var view = navigate("exercises/" + created.id().value(), ExerciseView.class);

        exercises.start(created.id());

        receivePush(view);
        assertThat(view.getElement().getTextRecursively()).contains("Exercise running");
        assertThat(find(Button.class).withText("End exercise").all()).hasSize(1);

        exercises.end(created.id());

        receivePush(view);
        assertThat(view.getElement().getTextRecursively()).contains("Exercise ended");
        assertThat(find(Button.class).withText("End exercise").all()).isEmpty();
    }

    @Test
    void theExerciseScreenLeadsBackToTheStartScreenAsSoonAsAnotherOfficerDeletesIt() {
        var created = anExercise();
        var view = navigate("exercises/" + created.id().value(), ExerciseView.class);

        exercises.delete(created.id());

        receivePush(view);
        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Another officer has deleted this exercise");
    }

    @Test
    void deletingAnExerciseYourselfSaysNothingOfAnotherOfficer() {
        var created = anExercise();
        navigate("exercises/" + created.id().value(), ExerciseView.class);

        test(find(Button.class).withText("Delete exercise").single()).click();
        test(find(ConfirmDialog.class).single()).confirm();

        receivePush((Component) getCurrentView());
        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
        assertThat(find(Notification.class).all()).isEmpty();
    }

    @Test
    void anUnknownExerciseLeadsBackToTheStartScreen() {
        navigate("exercises/00000000-0000-0000-0000-000000000000", ExercisesView.class);

        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
    }

    private CreateResult.Created anExercise() {
        return (CreateResult.Created) exercises.createFrom(scenarios.create(new ScenarioContent(
                "Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), BERTIL), BERTIL);
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
}
