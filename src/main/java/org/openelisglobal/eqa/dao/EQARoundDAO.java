package org.openelisglobal.eqa.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQARound;

public interface EQARoundDAO extends BaseDAO<EQARound, Long> {

    /**
     * Rounds whose submission deadline falls in [from, to), with the owning cycle
     * fetch-joined — callers include the scheduler, which runs outside a
     * transaction, so a lazy cycle proxy would blow up on first touch.
     */
    List<EQARound> findWithSubmissionDeadlineBetween(Timestamp from, Timestamp to);
}
