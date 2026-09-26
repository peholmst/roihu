package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.exercise.Exercise;
import net.pkhapps.roihu.exercise.ExerciseId;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.Subscription;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * An exercise on the training room's screen, for the crew to join from: the join code, its QR
 * code, where to type it, and the positions filling up. The screen is the officer's, but the room
 * sees it, so it shows nothing meant only for officers — not even the scenario's name, which would
 * give the incident away.
 */
@Route(value = "exercises/:id/presentation", autoLayout = false)
@PageTitle("Roihu")
@RolesAllowed(Roles.OFFICER)
public class PresentationView extends Composite<VerticalLayout> implements BeforeEnterObserver, AfterNavigationObserver {

    private static final String ID = "id";

    private final Exercises exercises;
    /** Known once the screen has been entered with an exercise to show. */
    private @Nullable Exercise shown;
    private @Nullable Subscription subscription;
    /** Made on entering, from the request: showing a change pushed to the screen has none. */
    private String address = "";
    private String joinLink = "";

    PresentationView(Exercises exercises) {
        this.exercises = exercises;
        getContent().addClassName("presentation");
        getContent().setSizeFull();
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);
    }

    public static RouteParameters parametersFor(ExerciseId exercise) {
        return new RouteParameters(ID, exercise.value().toString());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var exercise = event.getRouteParameters().get(ID).flatMap(ExerciseIds::parse).flatMap(exercises::get);
        if (exercise.isEmpty()) {
            showUnavailable();
            return;
        }
        address = JoinLinks.address();
        joinLink = JoinLinks.of(exercise.get().joinCode());
        show(exercise.get());
    }

    /** Follows the exercise, so that the room sees positions being taken as they are. */
    @Override
    // What goes wrong in an access task reaches the session's error handler, not only its future.
    @SuppressWarnings("FutureReturnValueIgnored")
    public void afterNavigation(AfterNavigationEvent event) {
        var ui = getUI().orElseThrow();
        stopFollowing();
        var exercise = shown;
        if (exercise == null) {
            return;
        }
        subscription = exercises.subscribe(exercise.joinCode(), () -> ui.access(() -> {
            if (isAttached()) {
                showAsItIsNow();
            }
        }));
        // A change committed after the screen was read but before this subscription was not heard.
        ui.access(this::showAsItIsNow);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopFollowing();
    }

    private void stopFollowing() {
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    private void showAsItIsNow() {
        Optional.ofNullable(shown).flatMap(exercise -> exercises.get(exercise.id()))
                .ifPresentOrElse(this::show, this::showUnavailable);
    }

    /**
     * A deleted or unknown exercise has nothing to join. The screen says so and stays: anywhere
     * else in the application would show the room what only officers should see.
     */
    private void showUnavailable() {
        shown = null;
        stopFollowing();
        getContent().removeAll();
        getContent().add(new Paragraph(getTranslation("presentation.unavailable")));
    }

    private void show(Exercise exercise) {
        shown = exercise;
        var joinCode = new Div(exercise.joinCode().toString());
        joinCode.addClassName("join-code");
        joinCode.getElement().setAttribute("aria-label", getTranslation("exercise.join-code"));
        getContent().removeAll();
        getContent().add(new Paragraph(getTranslation("presentation.join-at", address)), joinCode,
                new QrCode(joinLink, getTranslation("exercise.qr-code")), new PositionList(exercise.positions()));
    }
}
