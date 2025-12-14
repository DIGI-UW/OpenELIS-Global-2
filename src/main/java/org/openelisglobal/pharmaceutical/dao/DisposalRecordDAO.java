package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;

public interface DisposalRecordDAO extends BaseDAO<DisposalRecord, Integer> {

    List<DisposalRecord> findBySampleId(Integer sampleId);

    List<DisposalRecord> findByStatus(DisposalRecord.DisposalStatus status);

    List<DisposalRecord> findPendingApprovals();
}
