package net.pkhapps.roihu.base.ui;

import net.pkhapps.roihu.scenario.Change;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * How officer screens say who changed something and when. In the server's time zone: a
 * deployment serves one station (ADR-0003), and its server runs where the station is.
 */
public final class Changes {

    private Changes() {
    }

    public static String describe(Change change, Locale locale) {
        return at(change, locale) + " · " + change.by().email();
    }

    public static String at(Change change, Locale locale) {
        return at(change.at(), locale);
    }

    public static String at(Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
