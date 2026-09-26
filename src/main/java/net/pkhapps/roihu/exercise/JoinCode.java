package net.pkhapps.roihu.exercise;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** The short code that admits someone to an exercise as a crew member. */
public final class JoinCode {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = 8;

    private final String value;

    private JoinCode(String value) {
        this.value = value;
    }

    static JoinCode random(RandomGenerator random) {
        var value = new StringBuilder(LENGTH);
        for (var i = 0; i < LENGTH; i++) {
            value.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new JoinCode(value.toString());
    }

    /**
     * Reads a code however it was typed: in any case, with or without the separator and spaces,
     * and with the letters O, I and L taken for the digits they are easily mistaken for.
     */
    public static Optional<JoinCode> parse(String typed) {
        var normalised = normalise(typed);
        if (normalised.length() != LENGTH || !isCodeCharacters(normalised)) {
            return Optional.empty();
        }
        return Optional.of(new JoinCode(normalised));
    }

    /**
     * Whether typing more could still turn {@code typed} into a code, read the same way as
     * {@link #parse(String)}. True for a complete code too.
     */
    public static boolean couldBecomeACode(String typed) {
        var normalised = normalise(typed);
        return normalised.length() <= LENGTH && isCodeCharacters(normalised);
    }

    private static String normalise(String typed) {
        return typed.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "")
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
    }

    private static boolean isCodeCharacters(String normalised) {
        return normalised.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
    }

    /** The code without its separator, as stored. */
    String value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof JoinCode other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /** The code as it is read out, in two groups of four. */
    @Override
    public String toString() {
        return value.substring(0, 4) + "-" + value.substring(4);
    }
}
