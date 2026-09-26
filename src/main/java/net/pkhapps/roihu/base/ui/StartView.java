package net.pkhapps.roihu.base.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.roihu.base.security.Roles;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Placeholder start view. It exists so that signing in can be exercised end to end, and should be
 * replaced by the training officer's real starting point once there is one.
 */
@Route("")
@PageTitle("Roihu")
@Menu(title = "menu.start", order = 0)
@RolesAllowed(Roles.OFFICER)
public class StartView extends Composite<VerticalLayout> {

    StartView(AuthenticationContext authenticationContext) {
        var officer = authenticationContext.getAuthenticatedUser(OidcUser.class).orElseThrow();
        getContent().add(new ViewTitle("Roihu"));
        getContent().add(new Span("Signed in as %s (%s)".formatted(officer.getFullName(), officer.getEmail())));
        getContent().add(new Button("Sign out", event -> authenticationContext.logout()));
    }
}
