package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExerciseState;
import net.pkhapps.roihu.exercise.HolderToken;
import net.pkhapps.roihu.exercise.Holding;
import net.pkhapps.roihu.exercise.JoinCode;

/**
 * The screen a crew member keeps open while they hold a position. It will show the injects
 * revealed to that position; nothing else, not the join code nor the other positions.
 */
@Route(value = "join/:code/position", autoLayout = false)
@AnonymousAllowed
public class PositionView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final CrewJoining crewJoining;

    PositionView(CrewJoining crewJoining) {
        this.crewJoining = crewJoining;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var code = event.getRouteParameters().get("code").orElse("");
        var joinCode = JoinCode.parse(code);
        var holding = joinCode.flatMap(held -> HolderTokens.holding(held, crewJoining));
        if (holding.isEmpty()) {
            event.forwardTo(PositionPickerView.class, new RouteParameters("code", code));
            return;
        }
        show(holding.get(), HolderTokens.read(joinCode.get()).orElseThrow());
    }

    private void show(Holding holding, HolderToken token) {
        var position = Positions.describe(holding.position());
        getContent().removeAll();
        getContent().add(
                new H1(position),
                new Paragraph(getTranslation("exercise.state." + holding.state())),
                new Paragraph(getTranslation("position.injects-empty", position)));
        if (holding.state() != ExerciseState.ENDED) {
            getContent().add(new Button(getTranslation("position.change"), event -> changePosition(holding, token)));
        }
    }

    private void changePosition(Holding holding, HolderToken token) {
        if (!crewJoining.changePosition(token)) {
            // The exercise ended since this screen was shown; show it as it is now.
            getUI().ifPresent(ui -> ui.getPage().reload());
            return;
        }
        HolderTokens.clear(holding.joinCode());
        getUI().ifPresent(ui -> ui.navigate(PositionPickerView.class,
                new RouteParameters("code", holding.joinCode().toString())));
    }
}
