package net.pkhapps.roihu.base.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * A language the application's own text is translated into. Unrelated to the language a
 * scenario's injects are prepared in (ADR-0006).
 */
public enum InterfaceLanguage {
    FINNISH("fi", "suomi"), SWEDISH("sv", "svenska"), ENGLISH("en", "English");

    private final String code;
    private final String ownName;

    InterfaceLanguage(String code, String ownName) {
        this.code = code;
        this.ownName = ownName;
    }

    /** The ISO 639-1 code. */
    public String code() {
        return code;
    }

    /** The language's name for itself, which is the same whatever the interface speaks. */
    public String ownName() {
        return ownName;
    }

    public Locale locale() {
        return Locale.of(code);
    }

    public static Optional<InterfaceLanguage> fromCode(String code) {
        return Arrays.stream(values()).filter(language -> language.code.equals(code)).findFirst();
    }

    /** The language of {@code locale}, whatever its country: Swedish for sv-FI as for sv-SE. */
    public static Optional<InterfaceLanguage> of(Locale locale) {
        return fromCode(locale.getLanguage());
    }
}
