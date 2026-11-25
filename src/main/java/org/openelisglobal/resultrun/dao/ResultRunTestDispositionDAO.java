package org.openelisglobal.resultrun.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition;

public interface ResultRunTestDispositionDAO extends BaseDAO<ResultRunTestDisposition, Long> {

    /** The decisions taken on a run's held tests. */
    List<ResultRunTestDisposition> getDispositions(Long runId);
}
