package org.openelisglobal.analyzer.service;

public interface AnalyzerActivationService {

    AnalyzerActivationResult readiness(String analyzerId);

    AnalyzerActivationResult activate(String analyzerId, String actor);

    AnalyzerActivationResult reactivate(String analyzerId, String actor);

    AnalyzerDeactivationResult deactivate(String analyzerId, String actor);

    /**
     * Returns an analyzer whose analyzer type the Analyzer Bridge no longer has to
     * the state the upgrade migration leaves one in: its name, lab units, Bridge
     * connection and activation history stay, its mapping is cleared, and it is
     * inactive until it is set up again on an available type.
     *
     * @throws AnalyzerRequestException when the analyzer's type is still available
     */
    void resetUnavailableProfile(String analyzerId, String actor);
}
