package net.pkhapps.roihu.exercise.ui;

import com.vaadin.flow.server.VaadinServletRequest;
import net.pkhapps.roihu.exercise.JoinCode;

/**
 * The link that opens an exercise's join screen with its code filled in. Addressed as the
 * officer's own browser reached the application, which behind a reverse proxy relies on its
 * forwarded headers being honoured.
 */
final class JoinLinks {

    private JoinLinks() {
    }

    static String of(JoinCode joinCode) {
        return address() + "/" + joinCode;
    }

    /** The join screen's own address, where a crew member types the code. */
    static String address() {
        var request = VaadinServletRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("A join link is made while answering a request");
        }
        var http = request.getHttpServletRequest();
        var defaultPort = http.getScheme().equals("https") ? 443 : 80;
        var port = http.getServerPort() == defaultPort ? "" : ":" + http.getServerPort();
        return http.getScheme() + "://" + http.getServerName() + port + http.getContextPath() + "/join";
    }
}
