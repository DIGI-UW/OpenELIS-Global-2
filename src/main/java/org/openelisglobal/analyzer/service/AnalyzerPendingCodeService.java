package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;

@CrossDomainService(callers = "analyzer import pipeline — internal infrastructure")
public interface AnalyzerPendingCodeService extends BaseObjectService<AnalyzerPendingCode, String> {
    List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId);

    AnalyzerPendingCode track(String analyzerId, String analyzerTestName, String samplePayload, String sysUserId);

    AnalyzerPendingCode updateStatus(String pendingCodeId, AnalyzerPendingCode.Status status, String sysUserId);

    int purgeExpired(String analyzerId);
}
