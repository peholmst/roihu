package net.pkhapps.roihu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Stands in for the Keycloak registration, whose issuer URI would make Spring Security fetch the
 * provider's metadata on startup. The endpoints are never called: tests that need a signed-in
 * training officer mock the authentication instead.
 */
@TestConfiguration(proxyBeanMethods = false)
public class OfflineIdentityProviderConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
                .clientId("roihu")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("http://identity-provider.invalid/auth")
                .tokenUri("http://identity-provider.invalid/token")
                .jwkSetUri("http://identity-provider.invalid/certs")
                .userNameAttributeName("sub")
                .build());
    }
}
