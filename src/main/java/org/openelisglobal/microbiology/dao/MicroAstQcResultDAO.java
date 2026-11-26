package org.openelisglobal.microbiology.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcResult;

public interface MicroAstQcResultDAO extends BaseDAO<MicroAstQcResult, String> {
    List<MicroAstQcResult> getByRunId(String runId);

    boolean hasUnresolvedRejection(Collection<String> controlIds);
}
