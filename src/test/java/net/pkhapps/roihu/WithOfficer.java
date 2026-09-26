package net.pkhapps.roihu;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(TestOfficers.signedIn(officer.email(), officer.name()));
            return context;
        }
    }
}
