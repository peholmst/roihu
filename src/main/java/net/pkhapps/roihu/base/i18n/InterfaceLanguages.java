package net.pkhapps.roihu.base.i18n;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

/**
 * Chooses the language each browser window opens in: the one its viewer chose on this device,
 * then the browser's own preference when it is one we speak, and otherwise the deployment's
 * default.
 */
@Component
class InterfaceLanguages implements VaadinServiceInitListener {

    private final InterfaceLanguage deploymentDefault;

    InterfaceLanguages(@Value("${roihu.default-language:fi}") String deploymentDefault) {
        this.deploymentDefault = InterfaceLanguage.fromCode(deploymentDefault).orElseThrow(
                () -> new IllegalArgumentException("roihu.default-language must be fi, sv or en"));
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInit -> uiInit.getUI()
                .setLocale(chooseFor(VaadinService.getCurrentRequest()).locale()));
    }

    private InterfaceLanguage chooseFor(@Nullable VaadinRequest request) {
        return Optional.ofNullable(request)
                .flatMap(from -> LanguageCookie.read(from).or(() -> preferredByBrowser(from)))
                .orElse(deploymentDefault);
    }

    /** The first language in the browser's order of preference that we speak. */
    private static Optional<InterfaceLanguage> preferredByBrowser(VaadinRequest request) {
        return Collections.list(request.getLocales()).stream()
                .flatMap(locale -> InterfaceLanguage.of(locale).stream())
                .findFirst();
    }
}
