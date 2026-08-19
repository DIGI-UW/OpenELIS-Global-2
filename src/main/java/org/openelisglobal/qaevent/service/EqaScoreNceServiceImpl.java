package org.openelisglobal.qaevent.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.service.EQAAnalystCompetencyService;
import org.openelisglobal.eqa.service.EQAParticipantFollowupService;
import org.openelisglobal.eqa.service.EQAParticipantFollowupServiceImpl;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAFollowupStatus;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EqaScoreNceServiceImpl implements EqaScoreNceService {

    /** |Z| above which an unacceptable external score is a non-conformity. */
    private static final BigDecimal NCE_THRESHOLD = new BigDecimal("3");

    /** |Z| above which a score needs a supervisor's eyes. */
    private static final BigDecimal QUEUE_THRESHOLD = new BigDecimal("2");

    private static final String NCE_SEVERITY_CRITICAL = "CRITICAL";
    private static final String NCE_STATUS_PENDING = "Pending";
    private static final String NCE_CATEGORY_ANALYSIS = "Analysis";

    @Autowired
    private NCEventService ncEventService;
    @Autowired
    private NceNumberGeneratorService nceNumberGeneratorService;
    @Autowired
    private NceCategoryService nceCategoryService;
    @Autowired
    private NceHistoryService nceHistoryService;
    @Autowired
    private EQAParticipantFollowupService followupService;
    @Autowired
    private EQAAnalystCompetencyService competencyService;
    @Autowired
    private EQAParticipantResultDAO participantResultDAO;
    @Autowired
    private EQAPanelSampleDAO panelSampleDAO;
    @Autowired
    private AnalyteService analyteService;

    @Override
    public void onResultScored(EQAParticipantResult result, EQAPerformanceStatus performance) {
        EQACycle cycle = result.getCycle();
        EQAProgram scheme = cycle == null ? null : cycle.getScheme();
        if (scheme == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "onResultScored",
                    "Scored result " + result.getId() + " has no scheme; no NCE or follow-up routing possible");
            return;
        }

        switch (tierFor(result, performance, scheme)) {
        case NCE:
            createNceForResult(result, scheme, cycle);
            break;
        case QUEUE:
            followupService.enqueueForThisLab(scheme, cycle,
                    List.of(EQAParticipantFollowupServiceImpl.summaryRow(result, sealedTarget(result))),
                    result.getSysUserId());
            break;
        default:
            break;
        }
    }

    /**
     * FR-V2.3-01. Z governs whenever the provider returned one; the categorical
     * path exists because HIV qualitative, TB smear grading and blood-film ID have
     * no Z by construction, and an external provider calling such a result
     * unacceptable warrants the same non-conformity as a numeric |Z| &gt; 3.
     *
     * <p>
     * Two readings are pinned down here. In-house unacceptable never auto-creates
     * an NCE (FR-V2.4-08: in-house is exploratory, so it is triaged first). And an
     * external unacceptable with |Z| &le; 2 is contradictory data rather than a
     * clean pass, so it is queued for a human instead of taking the FR's literal
     * "no action" branch — dropping an explicit unacceptable verdict would lose it.
     */
    private Tier tierFor(EQAParticipantResult result, EQAPerformanceStatus performance, EQAProgram scheme) {
        boolean external = scheme.getSchemeType() != EQASchemeType.IN_HOUSE;
        BigDecimal absZ = result.getZScore() == null ? null : result.getZScore().abs();

        if (performance == EQAPerformanceStatus.UNACCEPTABLE) {
            if (!external) {
                return Tier.QUEUE;
            }
            return absZ == null || absZ.compareTo(NCE_THRESHOLD) > 0 ? Tier.NCE : Tier.QUEUE;
        }
        if (performance == EQAPerformanceStatus.QUESTIONABLE) {
            return Tier.QUEUE;
        }
        return absZ != null && absZ.compareTo(QUEUE_THRESHOLD) > 0 ? Tier.QUEUE : Tier.NONE;
    }

    /**
     * Runs in the scoring transaction, unlike {@link QcViolationNceServiceImpl}'s
     * REQUIRES_NEW: this adapter stamps the NCE onto the competency event that
     * scoring wrote moments earlier, and a suspended transaction could not see that
     * uncommitted row. The trade-off is explicit — an NCE failure rolls the score
     * back with it, rather than leaving an orphan NCE behind.
     */
    private NcEvent createNceForResult(EQAParticipantResult result, EQAProgram scheme, EQACycle cycle) {
        String triggerId = String.valueOf(result.getId());
        NcEvent existing = ncEventService.findByTriggerSource(TRIGGER_SOURCE_EQA_UNACCEPTABLE, triggerId);
        if (existing != null) {
            return existing;
        }

        String analyte = analyteName(result.getAnalyteId());
        String sysUserId = result.getSysUserId();
        BigDecimal z = result.getZScore();
        String title = z == null
                ? "Unacceptable EQA score: " + analyte + " (reported " + result.getResultValue() + ") in "
                        + scheme.getName() + " cycle " + cycle.getCycleNumber()
                : "Unacceptable EQA score: " + analyte + " Z=" + z.stripTrailingZeros().toPlainString() + " in "
                        + scheme.getName() + " cycle " + cycle.getCycleNumber();

        NcEvent nce = new NcEvent();
        nce.setSysUserId(sysUserId);
        nce.setNceNumber(nceNumberGeneratorService.generateNceNumber());
        nce.setSeverity(NCE_SEVERITY_CRITICAL);
        nce.setStatus(NCE_STATUS_PENDING);
        nce.setAutoGenerated(Boolean.TRUE);
        nce.setTriggerSourceType(TRIGGER_SOURCE_EQA_UNACCEPTABLE);
        nce.setTriggerSourceId(triggerId);
        nce.setTitle(truncate(title, 200));
        nce.setDescription(describe(result, scheme, cycle, analyte));
        nce.setImmediateAction("Investigate the analytical process for this analyte before reporting patient results");
        nce.setNameOfReporter("System (EQA scoring)");
        nce.setReportDate(new Date(System.currentTimeMillis()));
        nce.setDateOfEvent(new Date(result.getScoreReceivedAt() == null ? System.currentTimeMillis()
                : result.getScoreReceivedAt().getTime()));
        resolveCategory(nce);

        nce = ncEventService.save(nce);
        competencyService.attachNce(result.getId(), nce.getId());
        logCreation(nce, "participant result " + result.getId(), sysUserId);
        return nce;
    }

    @Override
    public NcEvent escalateFollowup(Long followupId, String sysUserId) {
        EQAParticipantFollowup followup = followupService.get(followupId);
        List<Long> resultIds = followupService.resultIdsFor(followup);
        String triggerId = String.valueOf(followupId);

        NcEvent nce = ncEventService.findByTriggerSource(TRIGGER_SOURCE_EQA_FOLLOWUP, triggerId);
        if (followup.getFollowupStatus() == EQAFollowupStatus.ESCALATED && nce != null) {
            // Already escalated — a repeated click returns the same NCE rather than
            // duplicating its competency events.
            return nce;
        }
        if (nce == null) {
            EQAProgram scheme = followup.getScheme();
            EQACycle cycle = followup.getCycle();
            List<String> analytes = new ArrayList<>();
            for (Long resultId : resultIds) {
                participantResultDAO.get(resultId)
                        .ifPresent(result -> analytes.add(analyteName(result.getAnalyteId())));
            }

            nce = new NcEvent();
            nce.setSysUserId(sysUserId);
            nce.setNceNumber(nceNumberGeneratorService.generateNceNumber());
            nce.setSeverity(NCE_SEVERITY_CRITICAL);
            nce.setStatus(NCE_STATUS_PENDING);
            nce.setAutoGenerated(Boolean.FALSE);
            nce.setTriggerSourceType(TRIGGER_SOURCE_EQA_FOLLOWUP);
            nce.setTriggerSourceId(triggerId);
            nce.setTitle(truncate("Escalated EQA follow-up: " + String.join(", ", analytes) + " in "
                    + (scheme == null ? "unknown scheme" : scheme.getName()) + " cycle "
                    + (cycle == null ? "?" : cycle.getCycleNumber()), 200));
            nce.setDescription("Escalated from the EQA Follow-Up Queue (FR-V2.3-02). Covers participant result(s) "
                    + resultIds + ". Queue snapshot: " + followup.getParticipantResultSummaryJson());
            nce.setImmediateAction("Investigate the flagged analytes and record corrective action");
            nce.setNameOfReporter("EQA Follow-Up Queue");
            nce.setReportDate(new Date(System.currentTimeMillis()));
            nce.setDateOfEvent(new Date(followup.getNotifiedAt() == null ? System.currentTimeMillis()
                    : followup.getNotifiedAt().getTime()));
            resolveCategory(nce);

            nce = ncEventService.save(nce);
            logCreation(nce, "EQA follow-up " + followupId, sysUserId);
        }

        for (Long resultId : resultIds) {
            Integer nceId = nce.getId();
            participantResultDAO.get(resultId).ifPresent(result -> competencyService.record(result,
                    EQACompetencyEventType.ESCALATED_TO_NCE, nceId, null, null, sysUserId));
        }
        followupService.markEscalated(followupId, sysUserId);
        return nce;
    }

    private String describe(EQAParticipantResult result, EQAProgram scheme, EQACycle cycle, String analyte) {
        StringBuilder text = new StringBuilder("Auto-created from an unacceptable EQA score (FR-V2.3-01).");
        text.append(" Scheme: ").append(scheme.getName()).append(" (").append(scheme.getSchemeType()).append(").");
        text.append(" Cycle: ").append(cycle.getCycleNumber());
        if (cycle.getCycleName() != null) {
            text.append(" — ").append(cycle.getCycleName());
        }
        text.append(". Round: ").append(result.getRound() == null ? "n/a" : result.getRound().getId());
        text.append(". Analyte: ").append(analyte).append(" (id ").append(result.getAnalyteId()).append(").");
        text.append(" Reported: ").append(result.getResultValue());
        if (result.getResultUnit() != null) {
            text.append(" ").append(result.getResultUnit());
        }
        text.append(".");
        if (result.getZScore() != null) {
            text.append(" Z-score: ").append(result.getZScore().stripTrailingZeros().toPlainString()).append(".");
        } else {
            text.append(" No Z-score returned (categorical scheme).");
        }
        if (result.getAssignedAnalystId() != null) {
            text.append(" Assigned analyst id: ").append(result.getAssignedAnalystId()).append(".");
        }
        if (result.getAnalysisId() != null) {
            text.append(" Analysis id: ").append(result.getAnalysisId()).append(".");
        }
        return text.toString();
    }

    /**
     * Category "Analysis" is seeded by the nce-categories CSV config handler and
     * ids are sequence-assigned, so it is resolved by name. Optional context — a
     * missing seed never blocks NCE creation.
     */
    private void resolveCategory(NcEvent nce) {
        try {
            NceCategory category = nceCategoryService.getAllNceCategories().stream()
                    .filter(cat -> NCE_CATEGORY_ANALYSIS.equals(cat.getName())).findFirst().orElse(null);
            if (category != null) {
                nce.setNceCategoryId(category.getId());
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveCategory",
                    "Could not resolve NCE category: " + e.getMessage());
        }
    }

    private void logCreation(NcEvent nce, String source, String sysUserId) {
        Integer userId = 1;
        try {
            userId = Integer.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "logCreation",
                    "Non-numeric sys_user_id " + sysUserId + "; history attributed to the system user");
        }
        nceHistoryService.logActivity(nce.getId(), "AUTO_CREATED", "Created from " + source, null, null, userId);
        LogEvent.logInfo(this.getClass().getSimpleName(), "logCreation",
                "Created " + nce.getNceNumber() + " from " + source);
    }

    /**
     * The target an in-house result was scored against, for the register row. It is
     * already revealed by the time scoring runs — the unblind pass compared against
     * it — and external PT keeps its targets at the provider, so this is null
     * there.
     */
    private String sealedTarget(EQAParticipantResult result) {
        if (result.getPanelSampleId() == null) {
            return null;
        }
        return panelSampleDAO.get(result.getPanelSampleId()).map(EQAPanelSample::getTargetValue).orElse(null);
    }

    private String analyteName(Long analyteId) {
        if (analyteId != null) {
            try {
                Analyte analyte = analyteService.get(String.valueOf(analyteId));
                if (analyte != null && analyte.getAnalyteName() != null) {
                    return analyte.getAnalyteName();
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "analyteName",
                        "Could not resolve analyte name for id " + analyteId);
            }
        }
        return "analyte " + analyteId;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private enum Tier {
        NCE, QUEUE, NONE
    }
}
