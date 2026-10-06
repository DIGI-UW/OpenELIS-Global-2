package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerMappingResultDAO extends BaseDAO<AnalyzerMappingResult, AnalyzerMappingResultPK> {

    List<AnalyzerMappingResult> findByRevisionId(String revisionId);
}
