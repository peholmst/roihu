package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
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
    void anUnknownExerciseLeadsBackToTheStartScreen() {
        navigate("exercises/00000000-0000-0000-0000-000000000000", ExercisesView.class);

        assertThat(getCurrentView()).isInstanceOf(ExercisesView.class);
    }
}
