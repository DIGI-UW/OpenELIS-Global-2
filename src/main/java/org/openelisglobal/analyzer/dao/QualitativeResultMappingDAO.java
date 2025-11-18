package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;

public interface QualitativeResultMappingDAO extends BaseDAO<QualitativeResultMapping, String> {
    List<QualitativeResultMapping> findByAnalyzerFieldId(String analyzerFieldId);
}

