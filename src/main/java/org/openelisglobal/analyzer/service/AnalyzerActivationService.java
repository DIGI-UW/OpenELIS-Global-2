package org.openelisglobal.analyzer.service;

public interface AnalyzerActivationService {

    AnalyzerActivationResult readiness(String analyzerId);

    AnalyzerActivationResult activate(String analyzerId, String actor);
}
