package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerDeliveryIssueService {

    List<AnalyzerDeliveryIssue> getOpenIssues();

    void retry(String outboxEntryId, String actor);

    void dismiss(String outboxEntryId, String actor);
}
