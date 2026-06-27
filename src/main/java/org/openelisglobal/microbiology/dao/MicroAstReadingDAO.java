package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;

public interface MicroAstReadingDAO extends BaseDAO<MicroAstReading, String> {

    List<MicroAstReading> getByRunId(String runId);
}
