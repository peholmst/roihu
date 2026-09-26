package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.base.ui.ViewTitle;
import net.pkhapps.roihu.exercise.Exercise;
import net.pkhapps.roihu.exercise.ExerciseId;
import net.pkhapps.roihu.exercise.Exercises;

import java.util.Optional;
import java.util.UUID;

/**
 * Where an officer hands out an exercise's join code, read aloud or sent as a link. Any officer
 * may open any exercise.
 */
@Route("exercises/:id")
@PageTitle("Roihu")
@RolesAllowed(Roles.OFFICER)
public class ExerciseView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final String ID = "id";

    private final Exercises exercises;

    ExerciseView(Exercises exercises) {
        this.exercises = exercises;
    }

    public static RouteParameters parametersFor(ExerciseId exercise) {
        return new RouteParameters(ID, exercise.value().toString());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var exercise = event.getRouteParameters().get(ID).flatMap(ExerciseView::parse).flatMap(exercises::get);
        if (exercise.isEmpty()) {
            event.forwardTo(ExercisesView.class);
            return;
        }
        show(exercise.get());
    }

    private static Optional<ExerciseId> parse(String id) {
        try {
            return Optional.of(new ExerciseId(UUID.fromString(id)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private void show(Exercise exercise) {
        var joinCode = new Div(exercise.joinCode().toString());
        joinCode.addClassName("join-code");
        joinCode.getElement().setAttribute("aria-label", getTranslation("exercise.join-code"));
        var link = new TextField(getTranslation("exercise.join-link"));
        link.setValue(JoinLinks.of(exercise.joinCode()));
        link.setReadOnly(true);
        link.setWidthFull();
        var copy = new Button(getTranslation("exercise.copy-link"));
        // The browser may refuse, over plain HTTP or when the officer has blocked it; the field
        // still holds the link to copy by hand.
        Clipboard.onClick(copy).writeText(link.getValue(),
                copied -> Notification.show(getTranslation("exercise.link-copied")),
                refused -> Notification.show(getTranslation("exercise.link-not-copied")));
        var linkRow = new HorizontalLayout(link, copy);
        linkRow.setWidthFull();
        linkRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        getContent().removeAll();
        getContent().add(new ViewTitle(exercise.scenarioName()),
                new Paragraph(getTranslation("exercise.state." + exercise.state())),
                joinCode, linkRow);
    }
}
