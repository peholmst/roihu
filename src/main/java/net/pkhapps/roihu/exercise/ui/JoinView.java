package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.JoinCode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Where a crew member enters an exercise. The join code is the only thing they need, since they
 * have no account (ADR-0005).
 */
@Route(value = "join/:code?", autoLayout = false)
@AnonymousAllowed
public class JoinView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final CrewJoining crewJoining;
    private final TextField code = new TextField();

    JoinView(CrewJoining crewJoining) {
        this.crewJoining = crewJoining;
        code.setLabel(getTranslation("join.code"));
        code.setValueChangeMode(ValueChangeMode.EAGER);
        code.addValueChangeListener(event -> {
            if (JoinCode.couldBecomeACode(event.getValue())) {
                code.setInvalid(false);
            } else {
                showError("join.malformed");
            }
        });
        var join = new Button(getTranslation("join.submit"), event -> join());
        join.addThemeVariants(ButtonVariant.PRIMARY);
        join.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
        getContent().add(new H1(getTranslation("join.title")), code, join);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var heldHere = event.getRouteParameters().get("code")
                .map(link -> URLDecoder.decode(link, StandardCharsets.UTF_8))
                .flatMap(JoinCode::parse)
                .filter(joinCode -> HolderTokens.holding(joinCode, crewJoining).isPresent());
        if (heldHere.isPresent()) {
            event.forwardTo(PositionView.class, new RouteParameters("code", heldHere.get().toString()));
            return;
        }
        event.getRouteParameters().get("code")
                .map(link -> URLDecoder.decode(link, StandardCharsets.UTF_8))
                .map(typed -> JoinCode.parse(typed).map(JoinCode::toString).orElse(typed))
                .ifPresent(code::setValue);
    }

    private void join() {
        var joinCode = JoinCode.parse(code.getValue());
        if (joinCode.isEmpty()) {
            showError("join.malformed");
        } else if (HolderTokens.holding(joinCode.get(), crewJoining).isPresent()) {
            getUI().ifPresent(ui -> ui.navigate(PositionView.class,
                    new RouteParameters("code", joinCode.get().toString())));
        } else if (crewJoining.findExercise(code.getValue()).isEmpty()) {
            showError("join.unknown");
        } else {
            getUI().ifPresent(ui -> ui.navigate(PositionPickerView.class,
                    new RouteParameters("code", joinCode.get().toString())));
        }
    }

    private void showError(String key) {
        code.setErrorMessage(getTranslation(key));
        code.setInvalid(true);
    }
}
