package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExercisePosition;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.exercise.JoinableExercise;
import net.pkhapps.roihu.exercise.TakeResult;


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

    private JoinCode joinCode;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var parsed = event.getRouteParameters().get("code").flatMap(JoinCode::parse);
        // A holder goes back to their position first: an ended exercise admits nobody new, but
        // those who hold a position keep it.
        if (parsed.isPresent() && HolderTokens.holding(parsed.get(), crewJoining).isPresent()) {
            event.forwardTo(PositionView.class, new RouteParameters("code", parsed.get().toString()));
            return;
        }
        // A token that no longer holds anything was taken over: only a change of position,
        // which forgets the token, releases a holding otherwise. Say so once, then forget it.
        var takenOver = parsed.filter(joinCode -> HolderTokens.read(joinCode).isPresent());
        takenOver.ifPresent(HolderTokens::clear);
        var exercise = parsed.flatMap(joinCode -> crewJoining.findExercise(joinCode.toString()));
        if (exercise.isEmpty()) {
            event.forwardTo(JoinView.class);
            return;
        }
        joinCode = parsed.get();
        show(exercise.get(), takenOver.isPresent());
    }

    private void show(JoinableExercise exercise, boolean takenOver) {
        var positions = new UnorderedList();
        exercise.positions().forEach(position -> positions.add(positionItem(position)));
        getContent().removeAll();
        getContent().add(new H1(getTranslation("picker.title")));
        if (takenOver) {
            getContent().add(new Paragraph(getTranslation("picker.taken-over")));
        }
        getContent().add(
                new Paragraph(getTranslation("exercise.state." + exercise.state())),
                new Paragraph(getTranslation("picker.prepared-language",
                        getTranslation("prepared-language." + exercise.preparedLanguage()))),
                positions);
    }

    private ListItem positionItem(ExercisePosition position) {
        var take = new Button(Positions.describe(position),
                event -> {
                    if (position.taken()) {
                        confirmTakeOver(position);
                    } else {
                        take(position);
                    }
                });
        var item = new ListItem(take);
        if (position.taken()) {
            item.add(new Span(getTranslation("picker.taken")));
        }
        return item;
    }

    private void take(ExercisePosition position) {
        if (alreadyHoldsAPosition()) {
            return;
        }
        switch (crewJoining.take(joinCode.toString(), position.id())) {
            case TakeResult.Taken taken -> hold(taken);
            // Someone took it since the picker was shown, perhaps at the same moment.
            case TakeResult.AlreadyTaken alreadyTaken -> confirmTakeOver(position);
            case TakeResult.NotJoinable notJoinable -> getUI().ifPresent(ui -> ui.navigate(JoinView.class));
        }
    }

    private void confirmTakeOver(ExercisePosition position) {
        var confirmation = new ConfirmDialog();
        confirmation.setHeader(getTranslation("picker.take-over.title"));
        confirmation.setText(getTranslation("picker.take-over.text", Positions.describe(position)));
        confirmation.setConfirmText(getTranslation("picker.take-over.confirm"));
        confirmation.setCancelable(true);
        confirmation.setCancelText(getTranslation("picker.take-over.cancel"));
        confirmation.addConfirmListener(event -> takeOver(position));
        confirmation.open();
    }

    private void takeOver(ExercisePosition position) {
        if (alreadyHoldsAPosition()) {
            return;
        }
        switch (crewJoining.takeOver(joinCode.toString(), position.id())) {
            case TakeResult.Taken taken -> hold(taken);
            case TakeResult.AlreadyTaken alreadyTaken -> throw new IllegalStateException("A take-over always takes");
            case TakeResult.NotJoinable notJoinable -> getUI().ifPresent(ui -> ui.navigate(JoinView.class));
        }
    }

    /**
     * Another window of this browser may have taken a position since this one was shown. A
     * browser holds one position per exercise, so send it there rather than take a second one
     * and orphan the first behind a token nobody keeps.
     */
    private boolean alreadyHoldsAPosition() {
        if (HolderTokens.holding(joinCode, crewJoining).isPresent()) {
            navigate(PositionView.class);
            return true;
        }
        return false;
    }

    private void hold(TakeResult.Taken taken) {
        HolderTokens.write(joinCode, taken.token());
        navigate(PositionView.class);
    }

    private void navigate(Class<? extends com.vaadin.flow.component.Component> view) {
        getUI().ifPresent(ui -> ui.navigate(view, new RouteParameters("code", joinCode.toString())));
    }
}
