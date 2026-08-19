package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerSiteBindingResultDAO extends BaseDAO<AnalyzerSiteBindingResult, AnalyzerSiteBindingResultPK> {

    List<AnalyzerSiteBindingResult> findByRevisionId(String revisionId);
}
