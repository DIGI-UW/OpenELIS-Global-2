package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQADistributionService extends BaseObjectService<EQADistribution, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQADistribution> findByProgramId(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQADistribution> findByStatus(EQADistributionStatus status);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQADistribution advanceStatus(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void validateMinParticipants(Long distributionId, int participantCount);
}
