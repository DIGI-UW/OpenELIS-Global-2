package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Participant-result lifecycle enforcement + competency events (OGC-609,
 * FR-V2.1-05 / FR-V2.1-22).
 */
@Service
@Transactional
public class EQAParticipantResultServiceImpl extends BaseObjectServiceImpl<EQAParticipantResult, Long>
        implements EQAParticipantResultService {

    @Autowired
    private EQAParticipantResultDAO eqaParticipantResultDAO;

    @Autowired
    private EQAAnalystCompetencyService competencyService;

    @Autowired
    private EqaScoreNceService eqaScoreNceService;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private AnalysisDAO analysisDAO;

    public EQAParticipantResultServiceImpl() {
        super(EQAParticipantResult.class);
    }

    @Override
    protected EQAParticipantResultDAO getBaseObjectDAO() {
        return eqaParticipantResultDAO;
    }

    @Override
    public EQAParticipantResult saveDraft(EQAParticipantResult result) {
        if (result.getId() == null) {
            // eqa_participant_result.round_id is NOT NULL: every result lives in a
            // round. Resolve it here so a bad id is a 4xx, not a flush error.
            if (result.getRound() == null || result.getRound().getId() == null) {
                throw new IllegalArgumentException("A participant result requires its round");
            }
            result.setRound(eqaRoundDAO.get(result.getRound().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown round " + result.getRound().getId())));
            result.setSubmissionStatus(EQASubmissionStatus.DRAFT);
            if (result.getEnteredAt() == null) {
                result.setEnteredAt(now());
            }
            result.setId(eqaParticipantResultDAO.insert(result));
            return result;
        }

        EQAParticipantResult existing = get(result.getId());
        if (existing.getSubmissionStatus() != EQASubmissionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT result can be edited; this one is " + existing.getSubmissionStatus());
        }
        existing.setResultValue(result.getResultValue());
        existing.setResultUnit(result.getResultUnit());
        existing.setAssignedAnalystId(result.getAssignedAnalystId());
        existing.setAnalysisId(result.getAnalysisId());
        existing.setEnteredBy(result.getEnteredBy());
        existing.setSysUserId(result.getSysUserId());
        return eqaParticipantResultDAO.update(existing);
    }

    @Override
    public EQAParticipantResult transitionStatus(Long resultId, EQASubmissionStatus target, String sysUserId) {
        if (target == EQASubmissionStatus.SCORED || target == EQASubmissionStatus.MISSED_DEADLINE) {
            throw new IllegalStateException(target + " carries side-effects; use its dedicated operation");
        }

        EQAParticipantResult result = get(resultId);
        EQASubmissionStatus from = result.getSubmissionStatus();
        boolean legal = (from == EQASubmissionStatus.DRAFT && target == EQASubmissionStatus.VALIDATED_PARTIAL)
                || (from == EQASubmissionStatus.VALIDATED_PARTIAL && target == EQASubmissionStatus.SUBMITTED);
        if (!legal) {
            throw new IllegalStateException("Cannot move a participant result from " + from + " to " + target);
        }

        if (target == EQASubmissionStatus.SUBMITTED) {
            result.setSubmittedAt(now());
        }
        result.setSubmissionStatus(target);
        result.setSysUserId(sysUserId);
        return eqaParticipantResultDAO.update(result);
    }

    @Override
    public EQAParticipantResult recordScore(Long resultId, EQAPerformanceStatus performance, Long eqaResultId,
            String sysUserId) {
        return recordScore(resultId, performance, null, eqaResultId, sysUserId);
    }

    @Override
    public EQAParticipantResult recordScore(Long resultId, EQAPerformanceStatus performance, BigDecimal zScore,
            Long eqaResultId, String sysUserId) {
        EQAParticipantResult result = get(resultId);
        if (result.getSubmissionStatus() != EQASubmissionStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a SUBMITTED result can be scored; this one is " + result.getSubmissionStatus());
        }
        if (performance == null) {
            throw new IllegalArgumentException("A score needs a performance verdict");
        }

        result.setSubmissionStatus(EQASubmissionStatus.SCORED);
        result.setPerformanceStatus(performance);
        if (zScore != null) {
            result.setZScore(zScore);
        }
        result.setScoreReceivedAt(now());
        result.setEqaResultId(eqaResultId);
        result.setSysUserId(sysUserId);
        EQAParticipantResult scored = eqaParticipantResultDAO.update(result);

        if (performance != EQAPerformanceStatus.ACCEPTABLE) {
            competencyService.record(scored,
                    performance == EQAPerformanceStatus.UNACCEPTABLE ? EQACompetencyEventType.UNACCEPTABLE_SCORE
                            : EQACompetencyEventType.QUESTIONABLE_SCORE,
                    null, null, null, sysUserId);
        }

        onResultScored(scored, performance);
        return scored;
    }

    @Override
    public EQAParticipantResult markMissedDeadline(Long resultId, String sysUserId) {
        EQAParticipantResult result = get(resultId);
        EQASubmissionStatus from = result.getSubmissionStatus();
        if (from != EQASubmissionStatus.DRAFT && from != EQASubmissionStatus.VALIDATED_PARTIAL) {
            throw new IllegalStateException("Cannot mark a " + from + " result as missed");
        }

        result.setSubmissionStatus(EQASubmissionStatus.MISSED_DEADLINE);
        result.setSysUserId(sysUserId);
        EQAParticipantResult missed = eqaParticipantResultDAO.update(result);

        boolean inHouse = missed.getCycle() != null && missed.getCycle().getScheme() != null
                && missed.getCycle().getScheme().getSchemeType() == EQASchemeType.IN_HOUSE;
        competencyService.record(missed, inHouse ? EQACompetencyEventType.IN_HOUSE_MISSED_DEADLINE
                : EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE, null, null, null, sysUserId);

        return missed;
    }

    /**
     * Applies the tiered EQA to NCE rules (OGC-611, FR-V2.3-01) to a freshly scored
     * result: non-conformity, Follow-Up Queue entry, or nothing. Fired after the
     * score and any competency event are written to the session, inside the same
     * transaction, because the adapter stamps the NCE onto that competency event.
     */
    private void onResultScored(EQAParticipantResult result, EQAPerformanceStatus performance) {
        eqaScoreNceService.onResultScored(result, performance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getResultDtos(Long cycleId, Long labEnrollmentId) {
        List<EQAParticipantResult> results = labEnrollmentId == null
                ? eqaParticipantResultDAO.getAllMatching("cycle.id", cycleId)
                : eqaParticipantResultDAO
                        .getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAParticipantResult result : results) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", result.getId());
            dto.put("cycleId", cycleId);
            dto.put("roundId", result.getRound() == null ? null : result.getRound().getId());
            dto.put("labEnrollmentId", result.getLabEnrollmentId());
            dto.put("analyteId", result.getAnalyteId());
            dto.put("analysisId", result.getAnalysisId());
            dto.put("resultValue", result.getResultValue());
            dto.put("resultUnit", result.getResultUnit());
            dto.put("assignedAnalystId", result.getAssignedAnalystId());
            dto.put("submissionStatus",
                    result.getSubmissionStatus() == null ? null : result.getSubmissionStatus().name());
            dto.put("submittedAt", result.getSubmittedAt() == null ? null : result.getSubmittedAt().toString());
            dto.put("scoreReceivedAt",
                    result.getScoreReceivedAt() == null ? null : result.getScoreReceivedAt().toString());
            dto.put("eqaResultId", result.getEqaResultId());
            dto.put("panelSampleId", result.getPanelSampleId());
            dto.put("performanceStatus",
                    result.getPerformanceStatus() == null ? null : result.getPerformanceStatus().name());
            dto.put("zScore", result.getZScore());
            rows.add(dto);
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> sectionNamesByResultId(List<EQAParticipantResult> results) {
        List<String> analysisIds = results.stream().map(EQAParticipantResult::getAnalysisId).filter(Objects::nonNull)
                .map(String::valueOf).distinct().toList();
        Map<String, String> byAnalysis = new LinkedHashMap<>();
        for (Analysis analysis : analysisIds.isEmpty() ? List.<Analysis>of() : analysisDAO.get(analysisIds)) {
            if (analysis.getTest() != null && analysis.getTest().getTestSection() != null) {
                byAnalysis.put(analysis.getId(), analysis.getTest().getTestSection().getTestSectionName());
            }
        }

        Map<Long, String> byResult = new LinkedHashMap<>();
        for (EQAParticipantResult result : results) {
            String section = result.getAnalysisId() == null ? null
                    : byAnalysis.get(String.valueOf(result.getAnalysisId()));
            if (section == null) {
                EQAProgram scheme = result.getCycle() == null ? null : result.getCycle().getScheme();
                section = scheme == null || scheme.getTestSection() == null ? null
                        : scheme.getTestSection().getTestSectionName();
            }
            if (section != null) {
                byResult.put(result.getId(), section);
            }
        }
        return byResult;
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
