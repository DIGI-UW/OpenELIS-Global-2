package org.openelisglobal.resultrun.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;

public interface ResultRunEventDAO extends BaseDAO<ResultRunEvent, Long> {

    /** What happened on a run, oldest first. */
    List<ResultRunEvent> getEvents(Long runId);
}
