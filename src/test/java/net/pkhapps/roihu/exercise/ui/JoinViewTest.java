package net.pkhapps.roihu.exercise.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.pkhapps.roihu.TestExercises.joinCodeOf;
import static net.pkhapps.roihu.TestOfficers.ANNA;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithAnonymousUser
class JoinViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void typingTheJoinCodeOpensThePositionPicker() {
        var joinCode = anExercise();
        navigate(JoinView.class);

        test(find(TextField.class).single()).setValue(joinCode.toString());
        test(find(Button.class).withText("Join").single()).click();

        assertThat(getCurrentView()).isInstanceOf(PositionPickerView.class);
    }

    @Test
    void aJoinLinkFillsInTheCode() {
        var joinCode = anExercise();

        navigate("join/" + joinCode, JoinView.class);

        assertThat(find(TextField.class).single().getValue()).isEqualTo(joinCode.toString());
    }

    @Test
    void aJoinLinkIsReadHoweverItWasTyped() {
        var joinCode = anExercise();
        var typedByHand = joinCode.toString().toLowerCase(Locale.ROOT).replace("-", "%20");

        navigate("join/" + typedByHand, JoinView.class);

        assertThat(find(TextField.class).single().getValue()).isEqualTo(joinCode.toString());
    }

    @Test
    void malformedInputIsFlaggedWhileTypingButAnIncompleteCodeIsNot() {
        navigate(JoinView.class);
        var code = find(TextField.class).single();

        test(code).setValue("K7QX");
        assertThat(code.isInvalid()).isFalse();

        test(code).setValue("K7QX-M2PU");
        assertThat(code.isInvalid()).isTrue();

        test(code).setValue("K7QX-M2P9Z");
        assertThat(code.isInvalid()).isTrue();

        test(code).setValue("k7qx m2p9");
        assertThat(code.isInvalid()).isFalse();
    }

    @Test
    void anIncompleteCodeIsFlaggedOnJoining() {
        navigate(JoinView.class);
        var code = find(TextField.class).single();

        test(code).setValue("K7QX");
        test(find(Button.class).withText("Join").single()).click();

        assertThat(getCurrentView()).isInstanceOf(JoinView.class);
        assertThat(code.isInvalid()).isTrue();
    }

    @Test
    void aCodeThatFindsNoExerciseSaysSo() {
        navigate(JoinView.class);
        var code = find(TextField.class).single();

        test(code).setValue("ZZZZ-ZZZZ");
        test(find(Button.class).withText("Join").single()).click();

        assertThat(getCurrentView()).isInstanceOf(JoinView.class);
        assertThat(code.isInvalid()).isTrue();
        assertThat(code.getErrorMessage()).isEqualTo("No exercise with this code");
    }

    private JoinCode anExercise() {
        return joinCodeOf(exercises.createFrom(scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA), ANNA));
    }
}
