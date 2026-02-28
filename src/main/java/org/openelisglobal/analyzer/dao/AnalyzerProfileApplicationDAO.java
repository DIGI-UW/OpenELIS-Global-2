package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileApplication;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerProfileApplicationDAO extends BaseDAO<AnalyzerProfileApplication, String> {

    List<AnalyzerProfileApplication> findByAnalyzerId(Integer analyzerId);

    AnalyzerProfileApplication findLatestByAnalyzerId(Integer analyzerId);
}
