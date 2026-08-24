package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerActivationCandidateDAO extends BaseDAO<AnalyzerActivationCandidate, String> {

    List<AnalyzerActivationCandidate> findByAnalyzerId(String analyzerId);
}
