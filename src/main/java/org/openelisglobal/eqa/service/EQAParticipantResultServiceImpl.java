package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
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
    private EQAAnalystCompetencyEventDAO eqaAnalystCompetencyEventDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

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
        EQAParticipantResult result = get(resultId);
        if (result.getSubmissionStatus() != EQASubmissionStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a SUBMITTED result can be scored; this one is " + result.getSubmissionStatus());
        }
        if (performance == null) {
            throw new IllegalArgumentException("A score needs a performance verdict");
        }

        result.setSubmissionStatus(EQASubmissionStatus.SCORED);
        result.setScoreReceivedAt(now());
        result.setEqaResultId(eqaResultId);
        result.setSysUserId(sysUserId);
        EQAParticipantResult scored = eqaParticipantResultDAO.update(result);

        if (performance != EQAPerformanceStatus.ACCEPTABLE) {
            recordCompetencyEvent(scored,
                    performance == EQAPerformanceStatus.UNACCEPTABLE ? EQACompetencyEventType.UNACCEPTABLE_SCORE
                            : EQACompetencyEventType.QUESTIONABLE_SCORE,
                    sysUserId);
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
        recordCompetencyEvent(missed, inHouse ? EQACompetencyEventType.IN_HOUSE_MISSED_DEADLINE
                : EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE, sysUserId);

        return missed;
    }

    /**
     * eqa_analyst_competency_event.analyst_id is NOT NULL by design — competency is
     * an analyst log, so a result with nobody assigned produces no event.
     */
    private void recordCompetencyEvent(EQAParticipantResult result, EQACompetencyEventType type, String sysUserId) {
        if (result.getAssignedAnalystId() == null) {
            return;
        }
        EQAAnalystCompetencyEvent event = new EQAAnalystCompetencyEvent();
        event.setAnalystId(result.getAssignedAnalystId());
        event.setEventType(type);
        event.setEventDate(new Date(System.currentTimeMillis()));
        event.setScheme(result.getCycle() == null ? null : result.getCycle().getScheme());
        event.setCycleId(result.getCycle() == null ? null : result.getCycle().getId());
        event.setParticipantResultId(result.getId());
        event.setAnalyteId(result.getAnalyteId());
        event.setSysUserId(sysUserId);
        eqaAnalystCompetencyEventDAO.insert(event);
    }

    /**
     * Hook for the tiered EQA→NCE trigger adapter (OGC-609): the adapter replaces
     * this body. Fired after the score and any competency event are committed to
     * the session, inside the same transaction.
     */
    protected void onResultScored(EQAParticipantResult result, EQAPerformanceStatus performance) {
        // Intentionally empty until the NCE adapter is wired (OGC-609).
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
            rows.add(dto);
        }
        return rows;
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
