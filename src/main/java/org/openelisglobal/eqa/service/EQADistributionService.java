package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;

public interface EQADistributionService extends BaseObjectService<EQADistribution, Long> {

    List<EQADistribution> findByProgramId(Long programId);

    List<EQADistribution> findByStatus(EQADistributionStatus status);

    EQADistribution advanceStatus(Long distributionId);

    void validateMinParticipants(Long distributionId, int participantCount);
}
