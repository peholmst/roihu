package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.Cookie;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.HolderToken;
import net.pkhapps.roihu.exercise.Holding;
import net.pkhapps.roihu.exercise.JoinCode;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Keeps a crew member's holder token, one per exercise, so that reloading, reopening the tab or
 * coming back to a locked phone returns them to their position. The cookie is what survives;
 * the session copy is what the rest of the round trip that took the position sees, since a
 * cookie set on a response is not on the request that is still being handled.
 */
final class HolderTokens {

    private static final Duration LIFETIME = Duration.ofDays(30);

    private HolderTokens() {
    }

    static Optional<HolderToken> read(JoinCode joinCode) {
        var session = VaadinSession.getCurrent();
        var kept = session == null ? null : session.getAttribute(name(joinCode));
        if (kept instanceof HolderToken token) {
            return Optional.of(token);
        }
        // Cleared in this session: the request may still carry the cookie it had before.
        if (kept == Released.RELEASED) {
            return Optional.empty();
        }
        var request = VaadinService.getCurrentRequest();
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name(joinCode)))
                .findFirst()
                .flatMap(cookie -> HolderToken.parse(cookie.getValue()));
    }

    /** The position this browser holds in the exercise, if it still holds one. */
    static Optional<Holding> holding(JoinCode joinCode, CrewJoining crewJoining) {
        return read(joinCode)
                .flatMap(crewJoining::findHolding)
                .filter(holding -> holding.joinCode().equals(joinCode));
    }

    static void write(JoinCode joinCode, HolderToken token) {
        VaadinSession.getCurrent().setAttribute(name(joinCode), token);
        setCookie(joinCode, token.toString(), LIFETIME);
    }

    static void clear(JoinCode joinCode) {
        VaadinSession.getCurrent().setAttribute(name(joinCode), Released.RELEASED);
        setCookie(joinCode, "", Duration.ZERO);
    }

    private static void setCookie(JoinCode joinCode, String value, Duration lifetime) {
        var cookie = new Cookie(name(joinCode), value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(VaadinService.getCurrentRequest().isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) lifetime.toSeconds());
        VaadinService.getCurrentResponse().addCookie(cookie);
    }

    /** What the session keeps for an exercise once this browser holds no position in it. */
    private enum Released {
        RELEASED
    }

    private static String name(JoinCode joinCode) {
        return "roihu-holder-" + joinCode;
    }
}
