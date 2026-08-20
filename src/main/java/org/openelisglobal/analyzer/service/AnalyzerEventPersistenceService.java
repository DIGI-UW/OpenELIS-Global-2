package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;

public interface AnalyzerEventPersistenceService {

    AnalyzerEvent createIfAbsent(AnalyzerEvent event);

    AnalyzerEvent markApplied(AnalyzerEvent event, String targetReference);

    AnalyzerEvent markFailed(AnalyzerEvent event, String failureReason);

    List<AnalyzerEvent> getFailed(int limit);
}
