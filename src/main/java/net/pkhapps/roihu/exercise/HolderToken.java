package net.pkhapps.roihu.exercise;

import java.io.Serial;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * The secret a crew member's browser keeps to show that it holds a position. Whoever has it
 * holds the position, so it is never stored as such: only its hash is.
 */
public final class HolderToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int BYTES = 32;

    private final byte[] value;

    private HolderToken(byte[] value) {
        this.value = value;
    }

    static HolderToken random(RandomGenerator random) {
        var value = new byte[BYTES];
        random.nextBytes(value);
        return new HolderToken(value);
    }

    /** Reads a token back from its {@link #toString()} form, as the browser returns it. */
    public static Optional<HolderToken> parse(String text) {
        try {
            var value = Base64.getUrlDecoder().decode(text);
            return value.length == BYTES ? Optional.of(new HolderToken(value)) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    /** The SHA-256 hash of the token, which is all that is stored. */
    byte[] hash() {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every JVM supports SHA-256", impossible);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HolderToken other && MessageDigest.isEqual(value, other.value);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(value);
    }

    /** The token in URL-safe Base64, fit for a cookie. */
    @Override
    public String toString() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
