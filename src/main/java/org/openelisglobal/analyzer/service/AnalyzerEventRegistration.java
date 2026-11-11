package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;

public record AnalyzerEventRegistration(AnalyzerEvent event, boolean created) {
}
