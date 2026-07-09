package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

public interface EQAResultService extends BaseObjectService<EQAResult, Long> {

    EQAResult submitResult(Long distributionId, Long organizationId, Long testId, java.math.BigDecimal resultValue,
            EQASubmissionMethod method);

    List<EQAResult> findByDistributionId(Long distributionId);

    long countByDistributionId(Long distributionId);
}
