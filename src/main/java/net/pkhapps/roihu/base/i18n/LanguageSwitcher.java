package net.pkhapps.roihu.base.i18n;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lets a viewer change the language of the interface. Screens that show it follow the change by
 * observing the locale; the language's own names are shown, since they read the same in every
 * interface language.
 */
public class LanguageSwitcher extends Composite<HorizontalLayout> implements LocaleChangeObserver {

    private final Map<InterfaceLanguage, Button> buttons = new EnumMap<>(InterfaceLanguage.class);

    public LanguageSwitcher() {
        for (var language : InterfaceLanguage.values()) {
            var button = new Button(language.code().toUpperCase(), event -> choose(language));
            button.addThemeVariants(ButtonVariant.SMALL);
            button.setAriaLabel(language.ownName());
            buttons.put(language, button);
            getContent().add(button);
        }
        getContent().setSpacing(false);
    }

    private void choose(InterfaceLanguage language) {
        LanguageCookie.write(language);
        getUI().ifPresent(ui -> ui.setLocale(language.locale()));
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        var current = InterfaceLanguage.of(event.getLocale());
        buttons.forEach((language, button) -> {
            var chosen = current.filter(language::equals).isPresent();
            button.getElement().setAttribute("aria-pressed", String.valueOf(chosen));
            if (chosen) {
                button.addThemeVariants(ButtonVariant.PRIMARY);
            } else {
                button.removeThemeVariants(ButtonVariant.PRIMARY);
            }
        });
    }
}
