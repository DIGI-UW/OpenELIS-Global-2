package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerSiteBindingTestDAO extends BaseDAO<AnalyzerSiteBindingTest, AnalyzerSiteBindingTestPK> {

    List<AnalyzerSiteBindingTest> findByRevisionId(String revisionId);
}
