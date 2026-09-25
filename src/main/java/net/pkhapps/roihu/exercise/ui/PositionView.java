package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
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
import net.pkhapps.roihu.exercise.Subscription;

/**
 * The screen a crew member keeps open while they hold a position. It will show the injects
 * revealed to that position; nothing else, not the join code nor the other positions.
 */
@Route(value = "join/:code/position", autoLayout = false)
@AnonymousAllowed
public class PositionView extends Composite<VerticalLayout>
        implements BeforeEnterObserver, AfterNavigationObserver {

    private final CrewJoining crewJoining;

    PositionView(CrewJoining crewJoining) {
        this.crewJoining = crewJoining;
    }

    private JoinCode joinCodeShown;
    private HolderToken token;
    private Subscription subscription;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var code = event.getRouteParameters().get("code").orElse("");
        var joinCode = JoinCode.parse(code);
        var holding = joinCode.flatMap(held -> HolderTokens.holding(held, crewJoining));
        if (holding.isEmpty()) {
            event.forwardTo(PositionPickerView.class, new RouteParameters("code", code));
            return;
        }
        joinCodeShown = joinCode.get();
        token = HolderTokens.read(joinCodeShown).orElseThrow();
        show(holding.get(), token);
    }

    /**
     * Follows the exercise shown. After every navigation here, not on attach: a navigation to
     * another exercise's screen reuses this one without attaching it again.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        var ui = getUI().orElseThrow();
        stopFollowing();
        subscription = crewJoining.subscribe(joinCodeShown, () -> ui.access(this::showAsItIsNow));
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

    /** The position may have been taken over, or the exercise may have changed state. */
    private void showAsItIsNow() {
        crewJoining.findHolding(token).ifPresentOrElse(
                holding -> show(holding, token),
                () -> getUI().ifPresent(ui -> ui.navigate(PositionPickerView.class,
                        new RouteParameters("code", joinCodeShown.toString()))));
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
            // The position was taken over, or the exercise ended, since this screen was shown.
            // Enter it again to find out which.
            getUI().ifPresent(ui -> ui.refreshCurrentRoute(false));
            return;
        }
        HolderTokens.clear(holding.joinCode());
        getUI().ifPresent(ui -> ui.navigate(PositionPickerView.class,
                new RouteParameters("code", holding.joinCode().toString())));
    }
}
