package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;

public interface EQAParticipantResultDAO extends BaseDAO<EQAParticipantResult, Long> {

    List<EQAParticipantResult> findByCycleAndEnrollment(Long cycleId, Long labEnrollmentId);
}
