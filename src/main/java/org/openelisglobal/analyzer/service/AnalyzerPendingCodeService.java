package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerPendingCodeService extends BaseObjectService<AnalyzerPendingCode, String> {
    List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId);

    AnalyzerPendingCode track(String analyzerId, String analyzerTestName, String samplePayload, String sysUserId);

    AnalyzerPendingCode updateStatus(String analyzerId, String pendingCodeId, AnalyzerPendingCode.Status status,
            String sysUserId);

    AnalyzerPendingCode resolve(String analyzerId, String pendingCodeId, String openelisTestId, String sysUserId);

    List<Map<String, Object>> getMappingOptions();

    Map<String, String> getMappedTestIds(String analyzerId);

    int purgeExpired(String analyzerId);
}
