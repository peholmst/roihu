package net.pkhapps.roihu.scenario;

import java.util.Arrays;

/**
 * The language a scenario's prepared injects were written in. A hint, never a guarantee: an
 * improvised inject may be in any language. Unrelated to the language the interface is shown in
 * (ADR-0006).
 */
public enum PreparedLanguage {
    FINNISH("fi"), SWEDISH("sv"), ENGLISH("en");

    private final String code;

    PreparedLanguage(String code) {
        this.code = code;
    }

    /** The ISO 639-1 code, as stored. */
    public String code() {
        return code;
    }

    public static PreparedLanguage fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No prepared language " + code));
    }
}
