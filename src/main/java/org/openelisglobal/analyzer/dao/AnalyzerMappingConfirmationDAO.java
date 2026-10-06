package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerMappingConfirmationDAO extends BaseDAO<AnalyzerMappingConfirmation, String> {

    Optional<AnalyzerMappingConfirmation> findByRevisionId(String revisionId);

    Optional<AnalyzerMappingConfirmation> findLatestByBindingId(String bindingId);
}
