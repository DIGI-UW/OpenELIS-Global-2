package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstOverrideEvent;

public interface MicroAstOverrideEventDAO extends BaseDAO<MicroAstOverrideEvent, String> {

    List<MicroAstOverrideEvent> getByRunId(String runId);
}
