package net.pkhapps.roihu.scenario.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import net.pkhapps.roihu.base.ui.Changes;
import net.pkhapps.roihu.base.ui.ViewTitle;
import net.pkhapps.roihu.scenario.ScenarioSummary;
import net.pkhapps.roihu.scenario.Scenarios;

/** The deployment's scenario library, shared by all its officers. */
@Route("scenarios")
@PageTitle("Roihu")
@Menu(title = "menu.scenarios", order = 1)
@RolesAllowed(Roles.OFFICER)
public class ScenarioLibraryView extends Composite<VerticalLayout> {

    ScenarioLibraryView(Scenarios scenarios) {
        var grid = new Grid<ScenarioSummary>();
        grid.addColumn(ScenarioSummary::name).setHeader(getTranslation("library.name"));
        grid.addColumn(summary -> getTranslation("prepared-language." + summary.preparedLanguage()))
                .setHeader(getTranslation("library.prepared-language"));
        grid.addColumn(ScenarioSummary::positionCount).setHeader(getTranslation("library.positions"));
        grid.addColumn(summary -> Changes.describe(summary.lastChanged(), getLocale()))
                .setHeader(getTranslation("library.last-changed"));
        grid.setItems(scenarios.list());
        grid.addItemClickListener(event -> getUI().ifPresent(ui -> ui.navigate(ScenarioEditorView.class,
                ScenarioEditorView.parametersFor(event.getItem().id()))));
        var create = new Button(getTranslation("library.new"),
                event -> getUI().ifPresent(ui -> ui.navigate(ScenarioEditorView.class)));
        create.addThemeVariants(ButtonVariant.PRIMARY);
        getContent().add(new ViewTitle(getTranslation("library.title")), create, grid);
        getContent().setSizeFull();
    }
}
