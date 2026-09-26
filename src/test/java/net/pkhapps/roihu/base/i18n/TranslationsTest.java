package net.pkhapps.roihu.base.i18n;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Every string of the interface is there in every interface language, and nothing else is. */
class TranslationsTest {

    @ParameterizedTest
    @ValueSource(strings = {"fi", "sv"})
    void everyStringIsTranslated(String language) throws IOException {
        var english = load("translations.properties");
        var translated = load("translations_" + language + ".properties");

        assertThat(translated.stringPropertyNames()).isEqualTo(english.stringPropertyNames());
        assertThat(translated.values()).allSatisfy(text -> assertThat(text).asString().isNotBlank());
    }

    private static Properties load(String file) throws IOException {
        try (var in = TranslationsTest.class.getResourceAsStream("/vaadin-i18n/" + file)) {
            assertThat(in).as(file).isNotNull();
            var properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
