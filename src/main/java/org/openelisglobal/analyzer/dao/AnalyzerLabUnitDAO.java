package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerLabUnitDAO
        extends BaseDAO<AnalyzerLabUnit, org.openelisglobal.analyzer.valueholder.AnalyzerLabUnitId> {

    List<AnalyzerLabUnit> findByAnalyzerId(Integer analyzerId);

    void replaceForAnalyzer(Integer analyzerId, List<String> labUnitIds);
}
