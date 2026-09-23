package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerEventDAO extends BaseDAO<AnalyzerEvent, String> {

    Optional<AnalyzerEvent> getByExternalEventId(String externalEventId);

    List<AnalyzerEvent> getFailed(int limit);
}
