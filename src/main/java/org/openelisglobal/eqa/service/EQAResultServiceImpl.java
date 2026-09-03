package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAResultServiceImpl extends BaseObjectServiceImpl<EQAResult, Long> implements EQAResultService {

    @Autowired
    private EQAResultDAO eqaResultDAO;

    @Autowired
    private EQADistributionDAO eqaDistributionDAO;

    @Autowired
    private EQAStatisticsService eqaStatisticsService;

    public EQAResultServiceImpl() {
        super(EQAResult.class);
    }

    @Override
    protected EQAResultDAO getBaseObjectDAO() {
        return eqaResultDAO;
    }

    @Override
    public EQAResult submitResult(Long distributionId, Long organizationId, Long testId, BigDecimal resultValue,
            EQASubmissionMethod method, String sysUserId) {

        EQADistribution distribution = eqaDistributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));

        // T114: Deadline enforcement — block late submissions without supervisor
        // approval
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (distribution.getDeadline() != null && now.after(distribution.getDeadline())) {
            throw new IllegalStateException(
                    "Submission deadline has passed. Supervisor approval is required for late submissions.");
        }

        requirePlausible(resultValue);
        return upsert(distribution, organizationId, testId, resultValue, null, method, sysUserId);
    }

    @Override
    public EQAResult submitReportedValue(Long distributionId, Long organizationId, Long testId, String reported,
            EQASubmissionMethod method, String sysUserId) {
        if (reported == null || reported.isBlank()) {
            throw new IllegalArgumentException("A reported value is required");
        }
        EQADistribution distribution = eqaDistributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));
        String trimmed = reported.trim();
        BigDecimal numeric;
        try {
            numeric = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            numeric = null;
        }
        if (numeric != null) {
            requirePlausible(numeric);
        }
        return upsert(distribution, organizationId, testId, numeric, numeric == null ? trimmed : null, method,
                sysUserId);
    }

    // T115: Result value validation — reject biologically implausible values
    private static void requirePlausible(BigDecimal resultValue) {
        if (resultValue != null && (resultValue.compareTo(BigDecimal.ZERO) < 0
                || resultValue.compareTo(new BigDecimal("999999")) > 0)) {
            throw new IllegalArgumentException("Result value out of plausible range: " + resultValue);
        }
    }

    private EQAResult upsert(EQADistribution distribution, Long organizationId, Long testId, BigDecimal resultValue,
            String resultText, EQASubmissionMethod method, String sysUserId) {
        Long distributionId = distribution.getId();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Check for existing result (duplicate handling with overwrite)
        Optional<EQAResult> existing = eqaResultDAO.findByDistributionAndOrgAndTest(distributionId, organizationId,
                testId);

        EQAResult result;
        if (existing.isPresent()) {
            result = existing.get();
            // T116: Audit trail — capture original values before overwrite
            result.setPreviousResultValue(result.getResultValue());
            result.setPreviousSubmissionDate(result.getSubmissionDate());
            result.setPreviousSubmissionMethod(result.getSubmissionMethod());
            // Update with new values
            result.setResultValue(resultValue);
            result.setResultText(resultText);
            result.setSubmissionMethod(method);
            result.setSubmissionDate(now);
            result.setSysUserId(sysUserId);
            result = eqaResultDAO.update(result);
        } else {
            result = new EQAResult();
            result.setEqaDistribution(distribution);
            result.setParticipantOrganizationId(organizationId);
            result.setTestId(testId);
            result.setResultValue(resultValue);
            result.setResultText(resultText);
            result.setSubmissionMethod(method);
            result.setSubmissionDate(now);
            // sys_user_id is NOT NULL; the V1 path never set it, so no row had ever been
            // written through this entity (the oversight suites seeded results in SQL).
            result.setSysUserId(sysUserId);

            // Check if late submission (for approved late submissions that bypass deadline
            // check)
            if (distribution.getDeadline() != null && now.after(distribution.getDeadline())) {
                result.setIsLateSubmission(true);
            }

            eqaResultDAO.insert(result);
        }

        // Trigger statistics recalculation if enough participants
        List<EQAResult> allResults = eqaResultDAO.findByDistributionId(distributionId);
        if (allResults.size() >= EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS) {
            eqaStatisticsService.calculateAndUpdateStatistics(distributionId);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAResult> findByDistributionId(Long distributionId) {
        return eqaResultDAO.findByDistributionId(distributionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDistributionId(Long distributionId) {
        return eqaResultDAO.countByDistributionId(distributionId);
    }
}
