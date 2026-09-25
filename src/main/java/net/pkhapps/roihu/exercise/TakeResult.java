package net.pkhapps.roihu.exercise;

/** What came of trying to take a position. */
public sealed interface TakeResult {

    /** The position is now held, by whoever keeps {@code token}. */
    record Taken(HolderToken token) implements TakeResult {
    }

    /** Someone already holds the position, and still does. */
    record AlreadyTaken() implements TakeResult {
    }

    /**
     * The code admits to no exercise the position belongs to: it is unknown or malformed, the
     * exercise has ended, or the position is another exercise's.
     */
    record NotJoinable() implements TakeResult {
    }
}
