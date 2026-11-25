package org.openelisglobal.analyzer.service;

/**
 * Taking an analyzer out of use: retiring it with its history, or deleting one
 * that was never used.
 */
public interface AnalyzerLifecycleService {

    /** Retires an analyzer that is not running; its results and history stay. */
    void retire(String analyzerId, String actor);

    /**
     * Brings a retired analyzer back, inactive, to be set up and activated again.
     */
    void restore(String analyzerId, String actor);

    /**
     * Deletes an analyzer that never received a result and was never activated,
     * with its own mapping revisions. One that was used is refused: retire it.
     */
    void delete(String analyzerId, String actor);
}
