package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQAResultService extends BaseObjectService<EQAResult, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQAResult submitResult(Long distributionId, Long organizationId, Long testId, java.math.BigDecimal resultValue,
            EQASubmissionMethod method);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQAResult> findByDistributionId(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    long countByDistributionId(Long distributionId);
}
