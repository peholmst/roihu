package net.pkhapps.roihu.scenario.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.i18n.InterfaceLanguage;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.base.security.SignedInOfficer;
import net.pkhapps.roihu.base.ui.ViewTitle;
import net.pkhapps.roihu.scenario.Change;
import net.pkhapps.roihu.scenario.DeleteResult;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.SaveResult;
import net.pkhapps.roihu.scenario.Scenario;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioId;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Where an officer writes a scenario: any scenario, whoever created it. */
@Route("scenarios/edit/:id?")
@PageTitle("Roihu")
@RolesAllowed(Roles.OFFICER)
public class ScenarioEditorView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final String ID = "id";

    private final Scenarios scenarios;
    private final SignedInOfficer officer;
    private final TextField name = new TextField();
    private final Select<PreparedLanguage> preparedLanguage = new Select<>();
    private final TextArea description = new TextArea();
    private final Grid<PositionRow> positions = new Grid<>();
    private final GridListDataView<PositionRow> positionRows = positions.setItems(new ArrayList<>());
    private final Paragraph provenance = new Paragraph();
    private final Button duplicate = new Button();
    private final Button delete = new Button();
    /** The position being dragged into a new place, while one is. */
    private @Nullable PositionRow dragged;
    /** The scenario being edited, or nothing while a new one is being written. */
    private @Nullable ScenarioId editing;
    /** The version of the scenario being edited that the form was filled from. */
    private int editingVersion;
    /** What the form was filled with, to tell whether the officer has changed it since. */
    private ScenarioContent filledWith = new ScenarioContent("", PreparedLanguage.FINNISH, Optional.empty(), List.of());

    ScenarioEditorView(Scenarios scenarios, SignedInOfficer officer) {
        this.scenarios = scenarios;
        this.officer = officer;
        name.setLabel(getTranslation("editor.name"));
        name.setRequiredIndicatorVisible(true);
        name.setErrorMessage(getTranslation("editor.name.missing"));
        name.addValueChangeListener(event -> name.setInvalid(false));
        preparedLanguage.setLabel(getTranslation("editor.prepared-language"));
        preparedLanguage.setItems(PreparedLanguage.values());
        preparedLanguage.setItemLabelGenerator(language -> getTranslation("prepared-language." + language));
        name.setWidthFull();
        description.setWidthFull();
        description.setLabel(getTranslation("editor.description"));
        description.setHelperText(getTranslation("editor.description.helper"));
        var addPosition = new Button(getTranslation("editor.positions.add"),
                event -> positionRows.addItem(new PositionRow()));
        var save = new Button(getTranslation("editor.save"), event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        duplicate.setText(getTranslation("editor.duplicate"));
        duplicate.addClickListener(event -> duplicate());
        delete.setText(getTranslation("editor.delete"));
        delete.addThemeVariants(ButtonVariant.ERROR);
        delete.addClickListener(event -> confirmDelete());
        var actions = new HorizontalLayout(duplicate, delete);
        getContent().add(new ViewTitle(getTranslation("editor.title")), actions, provenance, name, preparedLanguage, description,
                new H2(getTranslation("editor.positions")), createPositions(), addPosition, save);
    }

    static RouteParameters parametersFor(ScenarioId scenario) {
        return new RouteParameters(ID, scenario.value().toString());
    }

    /**
     * Fills the form afresh on every entry: navigating from one scenario to another, or to a new
     * one, reuses this view rather than building another.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var id = event.getRouteParameters().get(ID);
        if (id.isEmpty()) {
            showNew();
            return;
        }
        var scenario = parse(id.get()).flatMap(scenarios::get);
        if (scenario.isEmpty()) {
            event.forwardTo(ScenarioLibraryView.class);
            return;
        }
        show(scenario.get());
    }

    private static Optional<ScenarioId> parse(String id) {
        try {
            return Optional.of(new ScenarioId(UUID.fromString(id)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private void showNew() {
        editing = null;
        fill(new ScenarioContent("", defaultPreparedLanguage(), Optional.empty(), List.of()));
        provenance.setText("");
        provenance.setVisible(false);
        duplicate.setVisible(false);
        delete.setVisible(false);
    }

    private void show(Scenario scenario) {
        editing = scenario.id();
        editingVersion = scenario.version();
        fill(scenario.content());
        provenance.setText(getTranslation("editor.provenance",
                Changes.describe(scenario.created(), getLocale()),
                Changes.describe(scenario.lastChanged(), getLocale())));
        provenance.setVisible(true);
        duplicate.setVisible(true);
        delete.setVisible(true);
    }

    private void fill(ScenarioContent content) {
        filledWith = content;
        name.setValue(content.name());
        name.setInvalid(false);
        preparedLanguage.setValue(content.preparedLanguage());
        description.setValue(content.description().orElse(""));
        positionRows.removeItems(positionRows.getItems().toList());
        positionRows.addItems(content.positions().stream().map(PositionRow::new).toList());
    }

    private Grid<PositionRow> createPositions() {
        positions.addComponentColumn(row -> {
                    var field = positionField(row.name, "editor.positions.name", value -> {
                        row.name = value;
                        row.nameMissing = false;
                    });
                    field.setErrorMessage(getTranslation("editor.positions.name.missing"));
                    field.setInvalid(row.nameMissing);
                    return field;
                })
                .setKey("name").setHeader(getTranslation("editor.positions.name"));
        positions.addComponentColumn(row -> positionField(row.callSign, "editor.positions.call-sign",
                        value -> row.callSign = value))
                .setKey("call-sign").setHeader(getTranslation("editor.positions.call-sign"));
        positions.addComponentColumn(row -> {
            var remove = new Button(getTranslation("editor.positions.remove"),
                    event -> positionRows.removeItem(row));
            remove.addThemeVariants(ButtonVariant.TERTIARY);
            return remove;
        }).setKey("remove").setFlexGrow(0).setAutoWidth(true);
        positions.setAllRowsVisible(true);
        positions.setRowsDraggable(true);
        positions.setDropMode(GridDropMode.BETWEEN);
        positions.addDragStartListener(event -> dragged = event.getDraggedItems().getFirst());
        positions.addDragEndListener(event -> dragged = null);
        positions.addDropListener(event -> event.getDropTargetItem().ifPresent(target -> {
            var moved = dragged;
            if (moved == null || moved == target) {
                return;
            }
            positionRows.removeItem(moved);
            if (event.getDropLocation() == GridDropLocation.BELOW) {
                positionRows.addItemAfter(moved, target);
            } else {
                positionRows.addItemBefore(moved, target);
            }
        }));
        return positions;
    }

    private TextField positionField(String value, String label, Consumer<String> onChange) {
        var field = new TextField();
        field.setValue(value);
        field.setAriaLabel(getTranslation(label));
        field.setWidthFull();
        field.addValueChangeListener(event -> onChange.accept(event.getValue()));
        return field;
    }

    /** Officers usually prepare scenarios in the language they work in. */
    private PreparedLanguage defaultPreparedLanguage() {
        return InterfaceLanguage.of(getLocale())
                .map(language -> PreparedLanguage.fromCode(language.code()))
                .orElse(PreparedLanguage.FINNISH);
    }

    private void save() {
        var rows = positionRows.getItems().toList();
        rows.forEach(row -> row.nameMissing = row.name.isBlank());
        positionRows.refreshAll();
        name.setInvalid(name.getValue().isBlank());
        if (name.isInvalid() || rows.stream().anyMatch(row -> row.nameMissing)) {
            return;
        }
        var content = new ScenarioContent(name.getValue().strip(), preparedLanguage.getValue(),
                description(),
                rows.stream().map(PositionRow::toPosition).toList());
        if (editing == null) {
            scenarios.create(content, officer.get());
            showLibrary();
            return;
        }
        switch (scenarios.save(editing, editingVersion, content, officer.get())) {
            case SaveResult.Saved saved -> showLibrary();
            case SaveResult.Conflict conflict -> offerReload(conflict.lastChanged());
            case SaveResult.Gone gone -> Notification.show(getTranslation("editor.gone.not-saved"));
        }
    }

    /**
     * Another officer saved the scenario after this form was filled. Their work stays; this
     * officer may reload it, losing their own edits, or keep editing to copy them out first.
     */
    private void offerReload(Change lastChanged) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("editor.conflict.title"));
        dialog.setText(getTranslation("editor.conflict.text", lastChanged.by().email(),
                Changes.at(lastChanged, getLocale())));
        dialog.setConfirmText(getTranslation("editor.conflict.reload"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("editor.conflict.keep-editing"));
        dialog.addConfirmListener(event -> reload());
        dialog.open();
    }

    private void reload() {
        Optional.ofNullable(editing).flatMap(scenarios::get)
                .ifPresentOrElse(this::show, this::showGone);
    }

    /**
     * Copies the saved version of the scenario and opens the copy, which the officer may then make
     * into a variant. Asks first when that would leave unsaved changes behind.
     */
    private void duplicate() {
        if (!hasUnsavedChanges()) {
            duplicateSaved();
            return;
        }
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("editor.duplicate.title"));
        dialog.setText(getTranslation("editor.duplicate.text"));
        dialog.setConfirmText(getTranslation("editor.duplicate.confirm"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("editor.duplicate.cancel"));
        dialog.addConfirmListener(event -> duplicateSaved());
        dialog.open();
    }

    /** A copy of a deleted scenario cannot be made, so the officer's draft stays on screen. */
    private void duplicateSaved() {
        Optional.ofNullable(editing).flatMap(original -> scenarios.duplicate(original, officer.get()))
                .ifPresentOrElse(copy -> getUI().ifPresent(ui -> ui.navigate(ScenarioEditorView.class,
                        parametersFor(copy))), () -> Notification.show(getTranslation("editor.gone")));
    }

    private boolean hasUnsavedChanges() {
        return !name.getValue().strip().equals(filledWith.name())
                || preparedLanguage.getValue() != filledWith.preparedLanguage()
                || !description().equals(filledWith.description())
                || !positionRows.getItems().map(PositionRow::written).toList()
                .equals(filledWith.positions().stream().map(PositionRow::new).map(PositionRow::written).toList());
    }

    private Optional<String> description() {
        return Optional.of(description.getValue()).filter(text -> !text.isBlank());
    }

    private void confirmDelete() {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("editor.delete.title"));
        dialog.setText(getTranslation("editor.delete.text", name.getValue()));
        dialog.setConfirmText(getTranslation("editor.delete.confirm"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("editor.delete.cancel"));
        dialog.addConfirmListener(event -> delete());
        dialog.open();
    }

    private void delete() {
        var scenario = editing;
        if (scenario == null) {
            return;
        }
        switch (scenarios.delete(scenario)) {
            case DeleteResult.Deleted deleted -> showLibrary();
            case DeleteResult.HasExercises hasExercises ->
                    Notification.show(getTranslation("editor.delete.has-exercises"));
            case DeleteResult.Gone gone -> showGone();
        }
    }

    private void showGone() {
        Notification.show(getTranslation("editor.gone"));
        showLibrary();
    }

    private void showLibrary() {
        getUI().ifPresent(ui -> ui.navigate(ScenarioLibraryView.class));
    }

    /**
     * A position as it is being edited. Compared by identity, so that two positions written alike
     * are still two rows.
     */
    private static final class PositionRow {
        private String name = "";
        private String callSign = "";
        private boolean nameMissing;

        PositionRow() {
        }

        PositionRow(ScenarioPosition position) {
            name = position.name();
            callSign = position.callSign().orElse("");
        }

        /** The position as written, whether or not it could be saved yet. */
        List<String> written() {
            return List.of(name.strip(), callSign.strip());
        }

        ScenarioPosition toPosition() {
            return new ScenarioPosition(name.strip(), Optional.of(callSign.strip()).filter(text -> !text.isEmpty()));
        }
    }
}
