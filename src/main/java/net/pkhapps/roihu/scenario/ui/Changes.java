package net.pkhapps.roihu.scenario.ui;

import net.pkhapps.roihu.scenario.Change;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * How officer screens say who changed something and when. In the server's time zone: a
 * deployment serves one station (ADR-0003), and its server runs where the station is.
 */
final class Changes {

    private Changes() {
    }

    static String describe(Change change, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(change.at()) + " · " + change.by().email();
    }
}
