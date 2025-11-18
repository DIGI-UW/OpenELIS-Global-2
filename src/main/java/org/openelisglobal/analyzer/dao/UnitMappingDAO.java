package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.analyzer.valueholder.UnitMapping;

public interface UnitMappingDAO extends BaseDAO<UnitMapping, String> {
    List<UnitMapping> findByAnalyzerFieldId(String analyzerFieldId);
}

