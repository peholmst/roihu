package net.pkhapps.roihu.scenario;

/** What came of trying to save a scenario. */
public sealed interface SaveResult {

    /** The scenario now holds what was saved. */
    record Saved() implements SaveResult {
    }

    /**
     * Someone changed the scenario after the version the save was made from, and nothing was
     * saved: saving anyway would silently lose their work.
     */
    record Conflict(Change lastChanged) implements SaveResult {
    }
}
