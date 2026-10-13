package org.openelisglobal.analyzerresults.service;

/**
 * Records a failed analyzer run on its order and takes it off the review page.
 */
public interface AnalyzerFailedRunService {

    /**
     * Writes the failed run's reported value and the instrument's note as an
     * INTERNAL note on the one test the run was for, removes the staged row and
     * leaves the test open for the repeat. No result is written.
     *
     * @return the analysis the note was written on
     * @throws IllegalStateException when the row is not a failed run, or the run
     *                               matches no single test
     */
    String dismissAsFailedRun(String stagedResultId, String actor);
}
