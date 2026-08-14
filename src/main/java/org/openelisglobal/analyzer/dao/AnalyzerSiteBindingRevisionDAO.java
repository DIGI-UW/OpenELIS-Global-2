package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerSiteBindingRevisionDAO extends BaseDAO<AnalyzerSiteBindingRevision, String> {

    Optional<AnalyzerSiteBindingRevision> findLatestByBindingId(String bindingId);

    List<AnalyzerSiteBindingRevision> findLatestByProfileIds(List<String> profileIds);
}
