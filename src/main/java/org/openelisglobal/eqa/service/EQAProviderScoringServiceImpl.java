package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See {@link EQAProviderScoringService}. */
@Service
@Transactional
public class EQAProviderScoringServiceImpl implements EQAProviderScoringService {

    /** FR-V2.5-07: unacceptable in 2 of the last 3 cycles is persistent failure. */
    private static final int PERSISTENT_FAILURE_WINDOW = 3;
    private static final int PERSISTENT_FAILURE_THRESHOLD = 2;

    /**
     * The provider machine from submissions_open to scored, walked one edge at a
     * time.
     */
    private static final List<EQACycleStatus> SCORING_PATH = List.of(EQACycleStatus.SUBMISSIONS_OPEN,
            EQACycleStatus.SUBMISSIONS_CLOSED, EQACycleStatus.SCORING, EQACycleStatus.SCORED);

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQACycleService eqaCycleService;

    @Autowired
    private EQADistributionDAO eqaDistributionDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private EQAResultDAO eqaResultDAO;

    @Autowired
    private EQAStatisticsService eqaStatisticsService;

    @Autowired
    private EQAFhirSubmissionService eqaFhirSubmissionService;

    @Autowired
    private EQAParticipantFollowupService followupService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getScoreRows(Long cycleId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<EQAResult>> entry : byOrganization(distribution.getId()).entrySet()) {
            Organization participant = organizationService.getOrganizationById(String.valueOf(entry.getKey()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("organizationId", entry.getKey());
            row.put("organizationName", participant == null ? null : participant.getOrganizationName());
            row.put("resultCount", entry.getValue().size());
            row.put("acceptableCount", count(entry.getValue(), EQAPerformanceStatus.ACCEPTABLE));
            row.put("questionableCount", count(entry.getValue(), EQAPerformanceStatus.QUESTIONABLE));
            row.put("unacceptableCount", count(entry.getValue(), EQAPerformanceStatus.UNACCEPTABLE));
            row.put("worstZScore", worstZScore(entry.getValue()));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Map<String, Object> scoreCycle(Long cycleId, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        if (cycle.getStatus() != EQACycleStatus.SUBMISSIONS_OPEN
                && cycle.getStatus() != EQACycleStatus.SUBMISSIONS_CLOSED) {
            throw new IllegalStateException("A cycle in " + cycle.getStatus() + " is not open for scoring");
        }

        EQADistribution distribution = findOrOpenDistribution(cycle, sysUserId);
        long reported = eqaResultDAO.findByDistributionId(distribution.getId()).stream()
                .filter(result -> result.getResultValue() != null).count();
        // Refused rather than silently half-scored: below this the peer mean and SD
        // describe nothing, and the statistics service would leave every verdict null.
        if (reported < EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS) {
            throw new IllegalStateException("Scoring needs at least " + EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS
                    + " reported results; this cycle has " + reported);
        }

        eqaStatisticsService.calculateAndUpdateStatistics(distribution.getId());
        advanceToScored(cycle, sysUserId);

        int followups = 0;
        for (Map.Entry<Long, List<EQAResult>> entry : byOrganization(distribution.getId()).entrySet()) {
            if (count(entry.getValue(), EQAPerformanceStatus.UNACCEPTABLE) == 0) {
                continue;
            }
            followupService.enqueueForOrganization(cycle.getScheme(), cycle, entry.getKey(),
                    snapshotRows(entry.getValue()), isPersistentFailure(cycle, entry.getKey()), sysUserId);
            followups++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cycleId", cycleId);
        summary.put("distributionId", distribution.getId());
        summary.put("scoredCount", (int) reported);
        summary.put("followupCount", followups);
        summary.put("cycleStatus",
                eqaCycleDAO.get(cycleId).map(EQACycle::getStatus).map(EQACycleStatus::name).orElse(null));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public String buildScoreCsv(Long cycleId, Long organizationId) {
        StringBuilder csv = new StringBuilder("test,result_value,target_value,z_score,performance_status,scored_on\n");
        for (EQAResult result : resultsFor(cycleId, organizationId)) {
            // Only the test name is escaped: it is the one free-text cell. Running a
            // decimal through csvEscape would quote a negative Z as a formula and
            // print it as '-0.28 (found driving the download, 2026-08-24).
            csv.append(StringUtil.csvEscape(testName(result.getTestId()))).append(',')
                    .append(number(result.getResultValue())).append(',').append(number(result.getTargetValue()))
                    .append(',').append(number(result.getZScore())).append(',')
                    .append(result.getPerformanceStatus() == null ? "" : result.getPerformanceStatus().name())
                    .append(',').append(result.getSubmissionDate() == null ? "" : result.getSubmissionDate())
                    .append('\n');
        }
        return csv.toString();
    }

    @Override
    public Map<String, Object> distributeScores(Long cycleId, Long organizationId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            throw new IllegalArgumentException("This cycle has no distribution, so there are no scores to return");
        }
        return eqaFhirSubmissionService.submitResultsViaFhir(distribution.getId(), organizationId);
    }

    // ---- helpers ----

    private EQACycle cycle(Long cycleId) {
        return eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new ObjectNotFoundException(cycleId, EQACycle.class.getName()));
    }

    /** The cycle's distribution, or null when no results have been taken in yet. */
    private EQADistribution distributionOf(EQACycle cycle) {
        List<EQADistribution> existing = eqaDistributionDAO.getAllMatching("cycleId", cycle.getId());
        return existing.isEmpty() ? null : existing.get(0);
    }

    /**
     * The distribution is the cycle's per-participant score container: results,
     * z-scores and the FHIR return all hang off it, and its {@code cycle_id} is
     * what ties them to the V2 cycle. Opened on demand so a cycle scored straight
     * after an import does not need a separate setup step.
     */
    private EQADistribution findOrOpenDistribution(EQACycle cycle, String sysUserId) {
        EQADistribution existing = distributionOf(cycle);
        if (existing != null) {
            return existing;
        }
        List<EQARound> rounds = eqaRoundDAO.getAllMatchingOrdered("cycle.id", cycle.getId(), "roundNumber", false);
        EQARound round = rounds.isEmpty() ? null : rounds.get(0);
        Timestamp distributionDate = firstNonNull(round == null ? null : round.getDistributionDate(),
                timestamp(cycle.getPlannedStartDate()), new Timestamp(System.currentTimeMillis()));

        EQADistribution distribution = new EQADistribution();
        distribution.setFhirUuid(UUID.randomUUID());
        distribution.setEqaProgram(cycle.getScheme());
        distribution.setDistributionName(
                GenericValidator.isBlankOrNull(cycle.getCycleName()) ? "Cycle " + cycle.getCycleNumber()
                        : cycle.getCycleName());
        distribution.setDistributionDate(distributionDate);
        distribution.setDeadline(firstNonNull(round == null ? null : round.getSubmissionDeadline(),
                timestamp(cycle.getPlannedEndDate()), distributionDate));
        distribution.setStatus(EQADistributionStatus.SHIPPED);
        distribution.setCreatedBy(systemUserService.get(sysUserId));
        distribution.setCycleId(cycle.getId());
        distribution.setRoundId(round == null ? null : round.getId());
        distribution.setSysUserId(sysUserId);
        distribution.setId(eqaDistributionDAO.insert(distribution));
        return distribution;
    }

    /** Each edge keeps its own audit row, as the state machine requires (T-10). */
    private void advanceToScored(EQACycle cycle, String sysUserId) {
        int from = SCORING_PATH.indexOf(cycle.getStatus());
        for (int step = from + 1; step < SCORING_PATH.size(); step++) {
            eqaCycleService.transition(cycle.getId(), SCORING_PATH.get(step), EQAStateMachine.PROVIDER,
                    EQATriggerType.AUTO, EQATriggerEvent.SCORE_INTAKE, null, "Provider scoring run", sysUserId);
        }
    }

    /**
     * FR-V2.5-07: unacceptable in at least 2 of the participant's last 3 cycles in
     * this scheme, the current one included. Cycles the participant did not take
     * part in are not counted against it — only cycles that produced results.
     */
    private boolean isPersistentFailure(EQACycle cycle, Long organizationId) {
        List<EQACycle> cycles = eqaCycleDAO.getAllMatchingOrdered("scheme.id", cycle.getScheme().getId(), "cycleNumber",
                true);
        int considered = 0;
        int failures = 0;
        for (EQACycle candidate : cycles) {
            if (candidate.getCycleNumber() > cycle.getCycleNumber()) {
                continue;
            }
            List<EQAResult> results = resultsFor(candidate.getId(), organizationId);
            if (results.isEmpty()) {
                continue;
            }
            considered++;
            if (count(results, EQAPerformanceStatus.UNACCEPTABLE) > 0) {
                failures++;
            }
            if (considered == PERSISTENT_FAILURE_WINDOW) {
                break;
            }
        }
        return failures >= PERSISTENT_FAILURE_THRESHOLD;
    }

    private Map<Long, List<EQAResult>> byOrganization(Long distributionId) {
        Map<Long, List<EQAResult>> byOrganization = new LinkedHashMap<>();
        for (EQAResult result : eqaResultDAO.findByDistributionId(distributionId)) {
            byOrganization.computeIfAbsent(result.getParticipantOrganizationId(), key -> new ArrayList<>()).add(result);
        }
        return byOrganization;
    }

    private List<EQAResult> resultsFor(Long cycleId, Long organizationId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            return List.of();
        }
        return eqaResultDAO.findByDistributionId(distribution.getId()).stream()
                .filter(result -> organizationId.equals(result.getParticipantOrganizationId())).toList();
    }

    /**
     * The snapshot the register prints; it outlives later re-scoring of the row.
     */
    private List<Map<String, Object>> snapshotRows(List<EQAResult> results) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAResult result : results) {
            if (result.getPerformanceStatus() != EQAPerformanceStatus.UNACCEPTABLE) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("testId", result.getTestId());
            row.put("testName", testName(result.getTestId()));
            row.put("reported", result.getResultValue());
            row.put("target", result.getTargetValue());
            row.put("zScore", result.getZScore());
            row.put("performanceStatus", result.getPerformanceStatus().name());
            rows.add(row);
        }
        return rows;
    }

    /**
     * TestService is fetched rather than injected: injecting it into a service
     * pulls in a bean cycle through the test catalog's own dependencies.
     */
    private String testName(Long testId) {
        if (testId == null) {
            return null;
        }
        Test test = SpringContext.getBean(TestService.class).get(String.valueOf(testId));
        return test == null ? String.valueOf(testId) : test.getName();
    }

    private static int count(List<EQAResult> results, EQAPerformanceStatus status) {
        return (int) results.stream().filter(result -> result.getPerformanceStatus() == status).count();
    }

    private static BigDecimal worstZScore(List<EQAResult> results) {
        BigDecimal worst = null;
        for (EQAResult result : results) {
            BigDecimal z = result.getZScore();
            if (z != null && (worst == null || z.abs().compareTo(worst.abs()) > 0)) {
                worst = z;
            }
        }
        return worst;
    }

    /**
     * A decimal CSV cell: no separator or quote can occur in one, so no escaping.
     */
    private static String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static Timestamp timestamp(java.sql.Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    private static Timestamp firstNonNull(Timestamp... candidates) {
        for (Timestamp candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
