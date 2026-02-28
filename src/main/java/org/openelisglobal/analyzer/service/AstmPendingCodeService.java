package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;

public interface AstmPendingCodeService {

    List<AstmPendingCode> findPendingByAnalyzerId(String analyzerId);

    void recordSeen(String analyzerId, String analyzerTestName, String samplePayload);

    void resolveByMapping(String pendingCodeId, String openelisTestId);

    void updateStatus(String pendingCodeId, AstmPendingCode.Status status);

    void purgeOlderThanDays(int days);
}
