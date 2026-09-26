package net.pkhapps.roihu.scenario.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.WithOfficer;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioId;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.ScenarioSummary;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@WithOfficer
class ScenarioEditorViewTest extends SpringBrowserlessTest {

    @Autowired
    Scenarios scenarios;

    @Test
    void aNewScenarioIsPreparedInTheInterfaceLanguageUnlessTheOfficerSaysOtherwise() {
        UI.getCurrent().setLocale(Locale.of("sv"));

        navigate(ScenarioEditorView.class);

        assertThat(preparedLanguage().getValue()).isEqualTo(PreparedLanguage.SWEDISH);
    }

    @Test
    void aScenarioCreatedFromTheLibraryIsSavedIntoIt() {
        navigate(ScenarioLibraryView.class);
        test(find(Button.class).withText("New scenario").single()).click();

        test(field("Name")).setValue("Chimney fire");
        test(preparedLanguage()).selectItem("Finnish");
        test(find(TextArea.class).single()).setValue("Two-storey detached house, wood-burning stove");
        test(find(Button.class).withText("Save").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ScenarioLibraryView.class);
        var saved = scenarios.list().stream().filter(summary -> summary.name().equals("Chimney fire"))
                .map(ScenarioSummary::id).findFirst().flatMap(scenarios::get).orElseThrow();
        assertThat(saved.content()).isEqualTo(new ScenarioContent("Chimney fire", PreparedLanguage.FINNISH,
                Optional.of("Two-storey detached house, wood-burning stove"), List.of()));
        assertThat(saved.created().by()).isEqualTo(ANNA);
    }

    @Test
    void aScenarioWithoutANameIsNotSaved() {
        navigate(ScenarioEditorView.class);
        test(field("Name")).setValue("   ");

        test(find(Button.class).withText("Save").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ScenarioEditorView.class);
        assertThat(field("Name").isInvalid()).isTrue();
        assertThat(field("Name").getErrorMessage()).isEqualTo("Give the scenario a name");
    }

    @Test
    void aScenarioWithAnUnnamedPositionIsNotSaved() {
        navigate(ScenarioEditorView.class);
        test(field("Name")).setValue("Ship fire");
        test(find(Button.class).withText("Add position").single()).click();
        editPosition(0, " ", "RVS911");

        test(find(Button.class).withText("Save").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ScenarioEditorView.class);
        var positionName = (TextField) test(positions()).getCellComponent(0, "name");
        assertThat(positionName.isInvalid()).isTrue();
        assertThat(positionName.getErrorMessage()).isEqualTo("Give the position a name");
        assertThat(scenarios.list()).noneMatch(summary -> summary.name().equals("Ship fire"));
    }

    @Test
    void positionsAreAddedEditedAndRemovedAndACallSignMayBeLeftOut() {
        navigate(ScenarioEditorView.class);
        test(field("Name")).setValue("Car fire");
        var add = find(Button.class).withText("Add position").single();
        test(add).click();
        test(add).click();
        test(add).click();

        editPosition(0, "Officer", "RVSP911");
        editPosition(1, "Mistake", "XXX");
        editPosition(2, "Pump operator", "");
        test((Button) test(positions()).getCellComponent(1, "remove")).click();
        test(find(Button.class).withText("Save").single()).click();

        assertThat(savedNamed("Car fire").positions()).containsExactly(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.empty()));
    }

    @Test
    void anyOfficersScenarioOpensFromTheLibraryShowingWhoCreatedAndChangedItAndSavesAsAnEdit() {
        var id = scenarios.create(new ScenarioContent("Barn fire", PreparedLanguage.SWEDISH,
                Optional.of("Livestock inside"), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.empty()))), BERTIL);
        navigate(ScenarioLibraryView.class);

        test(find(Grid.class).single()).clickRow(rowOf("Barn fire"));

        assertThat(field("Name").getValue()).isEqualTo("Barn fire");
        assertThat(preparedLanguage().getValue()).isEqualTo(PreparedLanguage.SWEDISH);
        assertThat(find(TextArea.class).single().getValue()).isEqualTo("Livestock inside");
        assertThat(positionCell(0, "name")).isEqualTo("Officer");
        assertThat(positionCell(0, "call-sign")).isEqualTo("RVSP911");
        assertThat(positionCell(1, "name")).isEqualTo("Pump operator");
        assertThat(positionCell(1, "call-sign")).isEmpty();
        assertThat(getCurrentView().getElement().getTextRecursively())
                .containsPattern("Created .*" + BERTIL.email() + "\\. Last changed .*" + BERTIL.email());

        test(field("Name")).setValue("Barn fire with livestock");
        test(find(Button.class).withText("Save").single()).click();

        var saved = scenarios.get(id).orElseThrow();
        assertThat(saved.content().name()).isEqualTo("Barn fire with livestock");
        assertThat(saved.content().positions()).hasSize(2);
        assertThat(saved.created().by()).isEqualTo(BERTIL);
        assertThat(saved.lastChanged().by()).isEqualTo(ANNA);
        assertThat(scenarios.list()).filteredOn(summary -> summary.name().startsWith("Barn fire")).hasSize(1);
    }

    @Test
    void aSaveAfterAnotherOfficerChangedTheScenarioIsRefusedNamingThem() {
        var id = scenarios.create(new ScenarioContent("Mill fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
        navigate("scenarios/edit/" + id.value(), ScenarioEditorView.class);
        bertilRenames(id, "Mill fire, revised");

        test(field("Name")).setValue("Mill fire at night");
        test(find(Button.class).withText("Save").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ScenarioEditorView.class);
        var conflict = test(find(ConfirmDialog.class).single());
        assertThat(conflict.getHeader()).isEqualTo("Someone else changed this scenario");
        assertThat(conflict.getText()).contains(BERTIL.email());
        assertThat(scenarios.get(id).orElseThrow().content().name()).isEqualTo("Mill fire, revised");
    }

    @Test
    void reloadingAfterARefusedSaveShowsTheOtherOfficersVersionWhichThenSaves() {
        var id = scenarios.create(new ScenarioContent("Mill fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
        navigate("scenarios/edit/" + id.value(), ScenarioEditorView.class);
        bertilRenames(id, "Mill fire, revised");
        test(field("Name")).setValue("Mill fire at night");
        test(find(Button.class).withText("Save").single()).click();

        test(find(ConfirmDialog.class).single()).confirm();

        assertThat(field("Name").getValue()).isEqualTo("Mill fire, revised");
        assertThat(getCurrentView().getElement().getTextRecursively()).contains("Last changed").contains(BERTIL.email());
        test(field("Name")).setValue("Mill fire, revised twice");
        test(find(Button.class).withText("Save").single()).click();
        assertThat(getCurrentView()).isInstanceOf(ScenarioLibraryView.class);
        assertThat(scenarios.get(id).orElseThrow().lastChanged().by()).isEqualTo(ANNA);
        assertThat(scenarios.get(id).orElseThrow().content().name()).isEqualTo("Mill fire, revised twice");
    }

    @Test
    void keepingOnEditingAfterARefusedSaveKeepsTheEditsAndStillDoesNotOverwrite() {
        var id = scenarios.create(new ScenarioContent("Mill fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
        navigate("scenarios/edit/" + id.value(), ScenarioEditorView.class);
        bertilRenames(id, "Mill fire, revised");
        test(field("Name")).setValue("Mill fire at night");
        test(find(Button.class).withText("Save").single()).click();

        test(find(ConfirmDialog.class).single()).cancel();

        assertThat(field("Name").getValue()).isEqualTo("Mill fire at night");
        test(find(Button.class).withText("Save").single()).click();
        assertThat(find(ConfirmDialog.class).all()).isNotEmpty();
        assertThat(scenarios.get(id).orElseThrow().content().name()).isEqualTo("Mill fire, revised");
    }

    private void bertilRenames(ScenarioId id, String name) {
        var current = scenarios.get(id).orElseThrow();
        var content = current.content();
        scenarios.save(id, current.version(), new ScenarioContent(name, content.preparedLanguage(),
                content.description(), content.positions()), BERTIL);
    }

    @Test
    void goingFromOneScenarioToAnotherShowsOnlyTheOther() {
        var first = scenarios.create(new ScenarioContent("Bus fire", PreparedLanguage.FINNISH, Optional.of("Night"),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
        var second = scenarios.create(new ScenarioContent("Boat fire", PreparedLanguage.SWEDISH, Optional.empty(),
                List.of(new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), BERTIL);
        navigate("scenarios/edit/" + first.value(), ScenarioEditorView.class);

        navigate("scenarios/edit/" + second.value(), ScenarioEditorView.class);

        assertThat(field("Name").getValue()).isEqualTo("Boat fire");
        assertThat(find(TextArea.class).single().getValue()).isEmpty();
        assertThat(test(positions()).size()).isEqualTo(1);
        assertThat(positionCell(0, "name")).isEqualTo("Pump operator");
        test(find(Button.class).withText("Save").single()).click();
        assertThat(scenarios.get(second).orElseThrow().content().positions())
                .extracting(ScenarioPosition::name).containsExactly("Pump operator");
    }

    @Test
    void goingFromAScenarioToANewOneStartsBlankAndSavesAsANewScenario() {
        var existing = scenarios.create(new ScenarioContent("Train fire", PreparedLanguage.SWEDISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), BERTIL);
        navigate("scenarios/edit/" + existing.value(), ScenarioEditorView.class);

        navigate(ScenarioEditorView.class);

        assertThat(field("Name").getValue()).isEmpty();
        assertThat(preparedLanguage().getValue()).isEqualTo(PreparedLanguage.ENGLISH);
        assertThat(test(positions()).size()).isZero();
        assertThat(getCurrentView().getElement().getTextRecursively()).doesNotContain(BERTIL.email());
        test(field("Name")).setValue("Tram fire");
        test(find(Button.class).withText("Save").single()).click();
        assertThat(scenarios.get(existing).orElseThrow().content().name()).isEqualTo("Train fire");
        assertThat(savedNamed("Tram fire").positions()).isEmpty();
    }

    @Test
    void positionsAreDraggedIntoOrderAndTheOrderIsSaved() {
        var id = scenarios.create(new ScenarioContent("House fire", PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Smoke diver", Optional.of("RVS911S1")))), ANNA);
        navigate("scenarios/edit/" + id.value(), ScenarioEditorView.class);

        drag(2, 0, "above");
        drag(1, 2, "below");
        test(find(Button.class).withText("Save").single()).click();

        assertThat(scenarios.get(id).orElseThrow().content().positions())
                .extracting(ScenarioPosition::name)
                .containsExactly("Smoke diver", "Pump operator", "Officer");
    }

    /** Drags a row onto another, as the browser reports it: the drag starts, then the row lands. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void drag(int row, int onto, String dropLocation) {
        new DraggingTester<>((Grid) positions()).drag(row, onto, dropLocation);
    }

    private static final class DraggingTester<Y> extends GridTester<Grid<Y>, Y> {

        DraggingTester(Grid<Y> grid) {
            super(grid);
        }

        void drag(int row, int onto, String dropLocation) {
            var keys = getComponent().getDataCommunicator().getKeyMapper();
            var json = JsonNodeFactory.instance;
            var dragged = json.objectNode();
            dragged.putArray("draggedItems").addObject().put("key", keys.key(getRow(row)));
            fireDomEvent("grid-dragstart", json.objectNode().set("event.detail", dragged));
            var drop = json.objectNode();
            drop.putObject("event.detail.dropTargetItem").put("key", keys.key(getRow(onto)));
            drop.put("event.detail.dropLocation", dropLocation);
            drop.putArray("event.detail.dragData");
            fireDomEvent("grid-drop", drop);
            fireDomEvent("grid-dragend");
        }
    }

    private int rowOf(String name) {
        var library = test(find(Grid.class).single());
        return java.util.stream.IntStream.range(0, library.size())
                .filter(row -> library.getCellText(row, 0).equals(name))
                .findFirst().orElseThrow();
    }

    private String positionCell(int row, String column) {
        return ((TextField) test(positions()).getCellComponent(row, column)).getValue();
    }

    private void editPosition(int row, String name, String callSign) {
        test((TextField) test(positions()).getCellComponent(row, "name")).setValue(name);
        test((TextField) test(positions()).getCellComponent(row, "call-sign")).setValue(callSign);
    }

    private ScenarioContent savedNamed(String name) {
        return scenarios.list().stream().filter(summary -> summary.name().equals(name))
                .map(ScenarioSummary::id).findFirst().flatMap(scenarios::get).orElseThrow().content();
    }

    @SuppressWarnings("unchecked")
    private Grid<?> positions() {
        return find(Grid.class).single();
    }

    private TextField field(String label) {
        return find(TextField.class).withCaption(label).single();
    }

    @SuppressWarnings("unchecked")
    private Select<PreparedLanguage> preparedLanguage() {
        return find(Select.class).single();
    }
}
