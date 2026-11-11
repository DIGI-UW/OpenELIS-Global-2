package org.openelisglobal.batchworkplan.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanQcRelease;
import org.openelisglobal.common.dao.BaseDAO;

public interface BatchWorkplanQcReleaseDAO extends BaseDAO<BatchWorkplanQcRelease, Long> {

    /** The rows written by a batch's releases, newest first. */
    List<BatchWorkplanQcRelease> getForBatch(Long batchWorkplanId);

    /** When any batch was last released against a control lot, or null. */
    Timestamp getLastReleaseAt(String controlLotId);
}
