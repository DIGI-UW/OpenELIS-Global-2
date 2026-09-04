package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerSiteBindingConfirmationDAO extends BaseDAO<AnalyzerSiteBindingConfirmation, String> {

    Optional<AnalyzerSiteBindingConfirmation> findByRevisionId(String revisionId);

    Optional<AnalyzerSiteBindingConfirmation> findLatestByBindingId(String bindingId);
}
