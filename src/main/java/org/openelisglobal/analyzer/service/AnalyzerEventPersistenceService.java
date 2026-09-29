package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerEventPersistenceService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    AnalyzerEventRegistration createIfAbsent(AnalyzerEvent event);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    AnalyzerEvent markApplied(AnalyzerEvent event, String targetReference);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    AnalyzerEvent markFailed(AnalyzerEvent event, String failureReason);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    List<AnalyzerEvent> getFailed(int limit);
}
