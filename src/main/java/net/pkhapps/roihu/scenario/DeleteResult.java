package net.pkhapps.roihu.scenario;

/** What came of trying to delete a scenario. */
public sealed interface DeleteResult {

    /** The scenario is no longer in the library. */
    record Deleted() implements DeleteResult {
    }

    /**
     * The scenario has exercises, in setup or run long ago, and stays: every exercise keeps its
     * scenario.
     */
    record HasExercises() implements DeleteResult {
    }

    /** Someone else deleted the scenario first. */
    record Gone() implements DeleteResult {
    }
}
