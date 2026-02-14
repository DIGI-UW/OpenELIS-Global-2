package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface QualitativeResultMappingDAO extends BaseDAO<QualitativeResultMapping, String> {
    List<QualitativeResultMapping> findByAnalyzerFieldId(String analyzerFieldId);
}
