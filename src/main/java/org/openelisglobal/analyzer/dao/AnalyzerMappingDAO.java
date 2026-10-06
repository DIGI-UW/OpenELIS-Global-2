package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerMappingDAO extends BaseDAO<AnalyzerMapping, String> {

    Optional<AnalyzerMapping> findLatestByBindingId(String bindingId);
}
