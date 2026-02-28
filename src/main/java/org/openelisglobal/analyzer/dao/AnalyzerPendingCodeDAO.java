package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerPendingCodeDAO extends BaseDAO<AnalyzerPendingCode, String> {
    List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId);

    Optional<AnalyzerPendingCode> findByAnalyzerAndCode(String analyzerId, String analyzerTestName);
}
