package org.openelisglobal.sampleacceptance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;

public interface SampleAcceptanceRecordDAO extends BaseDAO<SampleAcceptanceRecord, Integer> {

    /**
     * The current (most recent) acceptance record for a specimen, or null if none.
     */
    SampleAcceptanceRecord findLatestBySampleItemId(Integer sampleItemId);

    /**
     * All acceptance records for a specimen, newest first (append-only audit
     * trail).
     */
    List<SampleAcceptanceRecord> findHistoryBySampleItemId(Integer sampleItemId);
}
