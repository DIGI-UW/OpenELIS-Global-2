package org.openelisglobal.eqa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.eqa.dao.EQAParticipantFollowupDAO;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the target of provider follow-up rows scored before the register read
 * qualitative targets (OGC-1665). Only a row with no target whose result has
 * one is written; every other row is left as it was scored, so a second run
 * changes nothing. Sealed targets are immutable, so this does not change what
 * the participant was scored against, and nothing is sent to the participant.
 * Each follow-up it changes gets an audit row.
 */
@Service
public class EQAFollowupTargetBackfill {

    /**
     * Reads and writes every number exactly as it was stored, so a reported
     * 60.00000 beside the filled target is not rewritten as 60.0.
     */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES).build();
    private static final String SUMMARY_ROWS = "unacceptable";
    private static final String TABLE = "eqa_participant_followup";

    @Autowired
    private EQAParticipantFollowupDAO followupDAO;

    @Autowired
    private EQAProviderScoringService providerScoringService;

    @Autowired
    private AuditTrailService auditTrailService;

    /** The number of register rows that received a target. */
    @Transactional
    public int backfill(String sysUserId) {
        int filled = 0;
        for (EQAParticipantFollowup followup : followupDAO.getAll()) {
            filled += backfill(followup, sysUserId);
        }
        if (filled > 0) {
            LogEvent.logInfo(getClass().getSimpleName(), "backfill",
                    "Filled the target of " + filled + " EQA follow-up register rows");
        }
        return filled;
    }

    private int backfill(EQAParticipantFollowup followup, String sysUserId) {
        String json = followup.getParticipantResultSummaryJson();
        if (GenericValidator.isBlankOrNull(json) || followup.getCycle() == null) {
            return 0;
        }
        JsonNode summary;
        try {
            summary = JSON.readTree(json);
        } catch (JsonProcessingException e) {
            LogEvent.logError(getClass().getSimpleName(), "backfill",
                    "Follow-up " + followup.getId() + " has an unreadable result summary: " + e.getMessage());
            return 0;
        }
        JsonNode rows = summary == null ? null : summary.get(SUMMARY_ROWS);
        if (rows == null || !rows.isArray() || !hasProviderRowWithoutTarget(rows)) {
            return 0;
        }

        Long cycleId = followup.getCycle().getId();
        List<EQAResult> results = providerScoringService.reportedResultsFor(cycleId, followup.getParticipantOrgId());
        Map<Long, String> sealedTargets = providerScoringService.sealedTargetsByResult(cycleId);
        int filled = 0;
        for (JsonNode row : rows) {
            if (!isProviderRowWithoutTarget(row)) {
                continue;
            }
            EQAResult result = resultOf(row, results);
            String target = result == null ? null : EQAResultTarget.of(result, sealedTargets);
            if (target != null) {
                ((ObjectNode) row).put("target", target);
                filled++;
            }
        }
        if (filled == 0) {
            return 0;
        }

        EQAParticipantFollowup prior = new EQAParticipantFollowup();
        BeanUtils.copyProperties(followup, prior);
        try {
            followup.setParticipantResultSummaryJson(JSON.writeValueAsString(summary));
        } catch (JsonProcessingException e) {
            LogEvent.logError(getClass().getSimpleName(), "backfill",
                    "Could not write the result summary of follow-up " + followup.getId() + ": " + e.getMessage());
            return 0;
        }
        followup.setSysUserId(sysUserId);
        auditTrailService.saveHistory(followup, prior, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE, TABLE);
        followupDAO.update(followup);
        return filled;
    }

    private static boolean hasProviderRowWithoutTarget(JsonNode rows) {
        for (JsonNode row : rows) {
            if (isProviderRowWithoutTarget(row)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rows the provider's scoring wrote name the test they answer; a laboratory's
     * own queue rows name the participant result instead and are not touched.
     */
    private static boolean isProviderRowWithoutTarget(JsonNode row) {
        if (!row.isObject() || !row.hasNonNull("testId")) {
            return false;
        }
        JsonNode target = row.get("target");
        return target == null || target.isNull() || GenericValidator.isBlankOrNull(target.asText());
    }

    /**
     * The one result the row describes: its test and the panel sample it answers.
     */
    private static EQAResult resultOf(JsonNode row, List<EQAResult> results) {
        Long testId = row.get("testId").asLong();
        Long panelSampleId = row.hasNonNull("panelSampleId") ? row.get("panelSampleId").asLong() : null;
        List<EQAResult> matches = results.stream().filter(
                result -> testId.equals(result.getTestId()) && Objects.equals(panelSampleId, result.getPanelSampleId()))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }
}
