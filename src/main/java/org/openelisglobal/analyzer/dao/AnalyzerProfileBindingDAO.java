package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerProfileBindingDAO extends BaseDAO<AnalyzerProfileBinding, String> {

    Optional<AnalyzerProfileBinding> findByProfileIdAndRevision(String profileId, int profileRevision);

    long countAnalyzersByBindingId(String bindingId);
}
