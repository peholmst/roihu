package net.pkhapps.roihu.exercise;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodeTest {

    @Test
    void lettersThatLookLikeDigitsAreReadAsThoseDigits() {
        assertThat(JoinCode.parse("OIL0-K7QX")).isEqualTo(JoinCode.parse("0110-K7QX"));
        assertThat(JoinCode.parse("oil0k7qx")).isEqualTo(JoinCode.parse("0110-K7QX"));
        assertThat(JoinCode.parse("0110-K7QX").orElseThrow()).hasToString("0110-K7QX");
    }

    @Test
    void anythingButEightCodeCharactersIsMalformed() {
        assertThat(JoinCode.parse("K7QX-M2P")).isEmpty();
        assertThat(JoinCode.parse("K7QX-M2P9Z")).isEmpty();
        assertThat(JoinCode.parse("K7QX-M2PU")).isEmpty();
        assertThat(JoinCode.parse("K7QX_M2P9")).isEmpty();
        assertThat(JoinCode.parse("")).isEmpty();
    }
}
