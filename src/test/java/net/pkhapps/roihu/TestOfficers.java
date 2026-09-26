package net.pkhapps.roihu;

import net.pkhapps.roihu.base.security.Officer;
import net.pkhapps.roihu.base.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Training officers for tests, by the emails the development realm gives them. */
public final class TestOfficers {

    public static final Officer ANNA = new Officer("anna@example.invalid");
    public static final Officer BERTIL = new Officer("bertil@example.invalid");

    private TestOfficers() {
    }

    /**
     * An officer signed in through the identity provider and admitted by the allowlist, as
     * {@code SecurityConfig} would leave them, without contacting the provider.
     */
    public static Authentication signedIn(String email, String name) {
        var idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", email, "email", email, "name", name));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OFFICER));
        return new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, idToken), authorities, "keycloak");
    }
}
