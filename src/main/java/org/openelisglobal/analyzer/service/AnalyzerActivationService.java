package org.openelisglobal.analyzer.service;

public interface AnalyzerActivationService {

    AnalyzerActivationResult readiness(String analyzerId);

    AnalyzerActivationResult activate(String analyzerId, String actor);

    AnalyzerActivationResult reactivate(String analyzerId, String actor);

    AnalyzerDeactivationResult deactivate(String analyzerId, String actor);
}
