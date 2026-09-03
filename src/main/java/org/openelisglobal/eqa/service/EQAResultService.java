package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

public interface EQAResultService extends BaseObjectService<EQAResult, Long> {

    EQAResult submitResult(Long distributionId, Long organizationId, Long testId, java.math.BigDecimal resultValue,
            EQASubmissionMethod method, String sysUserId);

    /**
     * Provider-side intake of a value as the participant reported it: a number
     * lands in result_value, anything else ("Reactive", "Scanty") in result_text.
     * The provider is the authority on what arrived, so a value after the deadline
     * is recorded as late rather than refused.
     */
    EQAResult submitReportedValue(Long distributionId, Long organizationId, Long testId, String reported,
            EQASubmissionMethod method, String sysUserId);

    List<EQAResult> findByDistributionId(Long distributionId);

    long countByDistributionId(Long distributionId);
}
