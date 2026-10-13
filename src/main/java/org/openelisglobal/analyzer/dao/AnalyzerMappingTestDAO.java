package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerMappingTestDAO extends BaseDAO<AnalyzerMappingTest, AnalyzerMappingTestPK> {

    List<AnalyzerMappingTest> findByMappingId(String mappingId);
}
