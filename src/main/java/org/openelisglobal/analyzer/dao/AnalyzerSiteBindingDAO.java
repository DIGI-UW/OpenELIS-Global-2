package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerSiteBindingDAO extends BaseDAO<AnalyzerSiteBinding, String> {

    Optional<AnalyzerSiteBinding> findByIdForUpdate(String id);
}
