package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;

public interface AnalyzerBridgeTransportService {

    String sendMessage(Analyzer analyzer, String message);
}
