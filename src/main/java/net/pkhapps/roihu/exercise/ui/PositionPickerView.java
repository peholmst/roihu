package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExercisePosition;
import net.pkhapps.roihu.exercise.JoinableExercise;

/**
 * Where a crew member who has entered an exercise chooses the position they will occupy. Keyed
 * by the join code alone, so that a reload keeps them here. Shows nothing about the incident: not
 * even the scenario's name.
 */
@Route(value = "join/:code/positions", autoLayout = false)
@AnonymousAllowed
public class PositionPickerView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final CrewJoining crewJoining;

    PositionPickerView(CrewJoining crewJoining) {
        this.crewJoining = crewJoining;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("code")
                .flatMap(crewJoining::findExercise)
                .ifPresentOrElse(this::show, () -> event.forwardTo(JoinView.class));
    }

    private void show(JoinableExercise exercise) {
        var positions = new UnorderedList();
        exercise.positions().forEach(position -> positions.add(new ListItem(describe(position))));
        getContent().removeAll();
        getContent().add(
                new H1(getTranslation("picker.title")),
                new Paragraph(getTranslation("exercise.state." + exercise.state())),
                new Paragraph(getTranslation("picker.prepared-language",
                        getTranslation("prepared-language." + exercise.preparedLanguage()))),
                positions);
    }

    private static String describe(ExercisePosition position) {
        return position.callSign()
                .map(callSign -> callSign + " · " + position.name())
                .orElse(position.name());
    }
}
