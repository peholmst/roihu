package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.base.ui.Changes;
import net.pkhapps.roihu.base.ui.ViewTitle;
import net.pkhapps.roihu.exercise.ExerciseSummary;
import net.pkhapps.roihu.exercise.Exercises;

/**
 * The training officer's start screen: every exercise of the deployment, what is under way
 * first.
 */
@Route("")
@PageTitle("Roihu")
@Menu(title = "menu.start", order = 0)
@RolesAllowed(Roles.OFFICER)
public class ExercisesView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final Exercises exercises;
    private final Grid<ExerciseSummary> grid = new Grid<>();

    ExercisesView(Exercises exercises) {
        this.exercises = exercises;
        grid.addColumn(ExerciseSummary::scenarioName).setHeader(getTranslation("exercises.scenario"));
        grid.addColumn(exercise -> getTranslation("exercises.state." + exercise.state()))
                .setHeader(getTranslation("exercises.state"));
        grid.addColumn(exercise -> exercise.created().by().email()).setHeader(getTranslation("exercises.created-by"));
        grid.addColumn(this::when).setHeader(getTranslation("exercises.when"));
        grid.addColumn(exercise -> getTranslation("exercises.positions-taken",
                        exercise.positionsTaken(), exercise.positionCount()))
                .setHeader(getTranslation("exercises.positions"));
        grid.addItemClickListener(event -> getUI().ifPresent(ui -> ui.navigate(ExerciseView.class,
                ExerciseView.parametersFor(event.getItem().id()))));
        getContent().add(new ViewTitle(getTranslation("exercises.title")), grid);
        getContent().setSizeFull();
    }

    /** Lists the exercises as they are now whenever the screen is entered, even from itself. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        grid.setItems(exercises.list());
    }

    /** When it ended, else when it started, else when it was created, so that runs are told apart. */
    private String when(ExerciseSummary exercise) {
        return exercise.ended().map(at -> getTranslation("exercises.ended-at", Changes.at(at, getLocale())))
                .or(() -> exercise.started().map(at -> getTranslation("exercises.started-at", Changes.at(at, getLocale()))))
                .orElseGet(() -> getTranslation("exercises.created-at", Changes.at(exercise.created(), getLocale())));
    }
}
