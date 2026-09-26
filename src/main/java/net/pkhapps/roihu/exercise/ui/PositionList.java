package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import net.pkhapps.roihu.exercise.ExercisePosition;

import java.util.List;

/** An exercise's positions, each marked free or taken, for the officers and the training room. */
final class PositionList extends Composite<UnorderedList> {

    PositionList(List<ExercisePosition> positions) {
        getContent().addClassName("position-list");
        positions.forEach(position -> {
            var state = new Span(getTranslation(position.taken() ? "exercise.position.taken" : "exercise.position.free"));
            state.addClassName(position.taken() ? "taken" : "free");
            getContent().add(new ListItem(new Span(Positions.describe(position)), state));
        });
    }
}
