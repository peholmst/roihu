package net.pkhapps.roihu;

import net.pkhapps.roihu.base.security.Roles;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Runs a test as a training officer signed in through the identity provider and admitted by the
 * allowlist, as {@code SecurityConfig} would leave them, without contacting the provider.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithOfficer.Factory.class)
public @interface WithOfficer {

    String email() default "anna@example.invalid";

    String name() default "Anna Officer";

    final class Factory implements WithSecurityContextFactory<WithOfficer> {

        @Override
        public SecurityContext createSecurityContext(WithOfficer officer) {
            var idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(3600),
                    Map.of("sub", officer.email(), "email", officer.email(), "name", officer.name()));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OFFICER));
            var user = new DefaultOidcUser(authorities, idToken);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new OAuth2AuthenticationToken(user, authorities, "keycloak"));
            return context;
        }
    }
}
