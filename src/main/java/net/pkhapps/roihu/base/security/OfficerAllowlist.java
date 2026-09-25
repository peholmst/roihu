package net.pkhapps.roihu.base.security;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The training officers this deployment admits, by email address.
 * <p>
 * The identity provider may be shared by several deployments, so authenticating against it proves
 * only who someone is. Whose officers they are is decided here. See
 * {@code docs/adr/0005-two-access-paths.md}.
 */
@ConfigurationProperties(prefix = "roihu.security")
public record OfficerAllowlist(Set<String> officers) {

    public OfficerAllowlist {
        officers = officers == null ? Set.of() : officers.stream()
                .map(OfficerAllowlist::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Whether this deployment admits the officer with the given email address. An officer the
     * identity provider gives no email address for is never admitted, because there is then nothing
     * to match against the list.
     */
    public boolean admits(@Nullable String email) {
        return email != null && officers.contains(normalize(email));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
