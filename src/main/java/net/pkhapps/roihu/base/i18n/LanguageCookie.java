package net.pkhapps.roihu.base.i18n;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/** Remembers on this device the language its viewer chose, over reloads and reopened browsers. */
final class LanguageCookie {

    private static final String NAME = "roihu-language";
    private static final Duration LIFETIME = Duration.ofDays(365);

    private LanguageCookie() {
    }

    static Optional<InterfaceLanguage> read(VaadinRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> cookie.getName().equals(NAME))
                .findFirst()
                .flatMap(cookie -> InterfaceLanguage.fromCode(cookie.getValue()));
    }

    /** Sets the cookie on the response being written, which a choice made by a click has. */
    static void write(InterfaceLanguage language) {
        var request = VaadinService.getCurrentRequest();
        var response = VaadinService.getCurrentResponse();
        if (request == null || response == null) {
            return;
        }
        var cookie = new Cookie(NAME, language.code());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) LIFETIME.toSeconds());
        response.addCookie(cookie);
    }
}
