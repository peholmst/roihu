package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.base.ui.ViewTitle;
import net.pkhapps.roihu.exercise.Exercise;
import net.pkhapps.roihu.exercise.ExerciseId;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.LifecycleResult;
import net.pkhapps.roihu.exercise.Subscription;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Where an officer hands out an exercise's join code, read aloud or sent as a link. Any officer
 * may open any exercise.
 */
@Route("exercises/:id")
@PageTitle("Roihu")
@RolesAllowed(Roles.OFFICER)
public class ExerciseView extends Composite<VerticalLayout> implements BeforeEnterObserver, AfterNavigationObserver {

    private static final String ID = "id";

    private final Exercises exercises;
    /** Known once the screen has been entered with an exercise to show. */
    private @Nullable Exercise shown;
    private @Nullable Subscription subscription;
    /** Made on entering, from the request: showing a change pushed to the screen has none. */
    private String joinLink = "";

    ExerciseView(Exercises exercises) {
        this.exercises = exercises;
    }

    public static RouteParameters parametersFor(ExerciseId exercise) {
        return new RouteParameters(ID, exercise.value().toString());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var exercise = event.getRouteParameters().get(ID).flatMap(ExerciseIds::parse).flatMap(exercises::get);
        if (exercise.isEmpty()) {
            event.forwardTo(ExercisesView.class);
            return;
        }
        joinLink = JoinLinks.of(exercise.get().joinCode());
        show(exercise.get());
    }

    /**
     * Follows the exercise shown, so that what other officers do to it shows at once. After every
     * navigation here, not on attach: a navigation to another exercise reuses this screen.
     */
    @Override
    // What goes wrong in an access task reaches the session's error handler, not only its future.
    @SuppressWarnings("FutureReturnValueIgnored")
    public void afterNavigation(AfterNavigationEvent event) {
        var ui = getUI().orElseThrow();
        stopFollowing();
        subscription = exercises.subscribe(entered().joinCode(), () -> ui.access(() -> {
            // This officer may have left the screen, by deleting the exercise, before hearing of it.
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

    /** What is shown, which only a screen that has been entered and not forwarded away has. */
    private Exercise entered() {
        return Objects.requireNonNull(shown, "The exercise screen shows nothing before it is entered");
    }

    private void show(Exercise exercise) {
        shown = exercise;
        var joinCode = new Div(exercise.joinCode().toString());
        joinCode.addClassName("join-code");
        joinCode.getElement().setAttribute("aria-label", getTranslation("exercise.join-code"));
        var link = new TextField(getTranslation("exercise.join-link"));
        link.setValue(joinLink);
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
        // In a tab of its own, for the training room's screen, while the officer keeps this one.
        var presentation = new Anchor(RouteConfiguration.forSessionScope().getUrl(PresentationView.class,
                PresentationView.parametersFor(exercise.id())), getTranslation("exercise.presentation"));
        presentation.setTarget(AnchorTarget.BLANK);
        var actions = new HorizontalLayout();
        switch (exercise.state()) {
            case SETUP -> actions.add(
                    action("exercise.start", ButtonVariant.PRIMARY, () -> exercises.start(exercise.id()),
                            this::showAsItIsNow),
                    action("exercise.delete", ButtonVariant.ERROR, () -> exercises.delete(exercise.id()),
                            this::showStart));
            case RUNNING -> actions.add(action("exercise.end", ButtonVariant.ERROR,
                    () -> exercises.end(exercise.id()), this::showAsItIsNow));
            case ENDED -> {
                // Nothing more happens to an ended exercise.
            }
        }
        getContent().removeAll();
        getContent().add(new ViewTitle(exercise.scenarioName()),
                new Paragraph(getTranslation("exercise.state." + exercise.state())),
                actions, presentation, joinCode, linkRow, new QrCode(joinLink, getTranslation("exercise.qr-code")),
                new H2(getTranslation("exercise.positions")), new PositionList(exercise.positions()));
    }

    /** A button that does what it says after the officer confirms it, and then {@code onDone}. */
    private Button action(String action, ButtonVariant variant, Supplier<LifecycleResult> operation,
                          Runnable onDone) {
        var button = new Button(getTranslation(action), event -> confirm(action, operation, onDone));
        button.addThemeVariants(variant);
        return button;
    }

    private void confirm(String action, Supplier<LifecycleResult> onConfirm, Runnable onDone) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation(action + ".title"));
        dialog.setText(getTranslation(action + ".text"));
        dialog.setConfirmText(getTranslation(action + ".confirm"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation(action + ".cancel"));
        dialog.addConfirmListener(event -> showOutcome(onConfirm.get(), onDone));
        dialog.open();
    }

    /**
     * Another officer may have moved the exercise on, or deleted it, since this screen showed it.
     * Says so, and shows it as it is now.
     */
    private void showOutcome(LifecycleResult result, Runnable onDone) {
        switch (result) {
            case LifecycleResult.Done done -> onDone.run();
            case LifecycleResult.Refused refused -> {
                Notification.show(getTranslation("exercise.refused." + refused.state()));
                showAsItIsNow();
            }
            case LifecycleResult.Gone gone -> showGone();
        }
    }

    private void showAsItIsNow() {
        exercises.get(entered().id()).ifPresentOrElse(this::show, this::showGone);
    }

    private void showGone() {
        Notification.show(getTranslation("exercise.gone"));
        showStart();
    }

    private void showStart() {
        getUI().ifPresent(ui -> ui.navigate(ExercisesView.class));
    }
}
