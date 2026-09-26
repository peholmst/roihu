package net.pkhapps.roihu.base.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * The training officer signed in to the current request, as the scenario library and the
 * exercises record them. Only officer routes may ask, since only they require a signed-in officer.
 */
@Component
public class SignedInOfficer {

    private final AuthenticationContext authenticationContext;

    SignedInOfficer(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    /** The {@link OfficerAllowlist} admits nobody without an email, so every officer has one. */
    public Officer get() {
        return authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(user -> java.util.Optional.ofNullable(user.getEmail()))
                .map(Officer::new)
                .orElseThrow(() -> new IllegalStateException("No training officer is signed in"));
    }
}
