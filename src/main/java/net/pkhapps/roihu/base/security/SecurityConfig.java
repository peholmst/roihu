package net.pkhapps.roihu.base.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;

/**
 * Training officers sign in through an external OpenID Connect provider and are admitted only if
 * this deployment's {@link OfficerAllowlist} also lists them. Crew members have no identity at all
 * and reach an exercise by its join code.
 */
@Configuration
@EnableConfigurationProperties(OfficerAllowlist.class)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer
                .oauth2LoginPage("/oauth2/authorization/keycloak", "{baseUrl}"));
        return http.build();
    }

    /**
     * Rejects an authenticated user this deployment does not list, and grants the rest
     * {@link Roles#OFFICER}. Spring Security picks this up by its generic type.
     */
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(OfficerAllowlist allowlist) {
        var delegate = new OidcUserService();
        return request -> {
            var user = delegate.loadUser(request);
            if (!allowlist.admits(user.getEmail())) {
                throw new OAuth2AuthenticationException(new OAuth2Error("not_a_training_officer",
                        "This deployment does not list %s as a training officer"
                                .formatted(user.getEmail()), null));
            }
            var authorities = new HashSet<GrantedAuthority>(user.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + Roles.OFFICER));
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo());
        };
    }
}
