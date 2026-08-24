package org.openelisglobal.analyzer.service;

public interface AnalyzerActivationService {

    AnalyzerActivationResult activate(String analyzerId, String actor);
}
