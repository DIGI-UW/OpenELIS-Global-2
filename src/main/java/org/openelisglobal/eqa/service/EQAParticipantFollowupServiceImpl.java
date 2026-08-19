package org.openelisglobal.eqa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.eqa.dao.EQAParticipantFollowupDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAFollowupStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAParticipantFollowupServiceImpl extends BaseObjectServiceImpl<EQAParticipantFollowup, Long>
        implements EQAParticipantFollowupService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SUMMARY_ROWS = "unacceptable";
    private static final String SUMMARY_SOURCE = "source";
    private static final String ROW_RESULT_ID = "participantResultId";

    @Autowired
    private EQAParticipantFollowupDAO followupDAO;

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private EQAAnalystCompetencyService competencyService;

    @Autowired
    private OrganizationService organizationService;

    public EQAParticipantFollowupServiceImpl() {
        super(EQAParticipantFollowup.class);
    }

    @Override
    protected EQAParticipantFollowupDAO getBaseObjectDAO() {
        return followupDAO;
    }

    @Override
    public EQAParticipantFollowup enqueueForThisLab(EQAProgram scheme, EQACycle cycle, List<Map<String, Object>> rows,
            String sysUserId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Long orgId = Long.parseLong(selfOrganization(sysUserId).getId());
        String source = sourceLabel(scheme);

        for (EQAParticipantFollowup existing : followupDAO.getAllMatching("cycle.id", cycle.getId())) {
            if (orgId.equals(existing.getParticipantOrgId())) {
                // The register is unique on cycle + org, and one cycle can score
                // several panels and analytes. Merge rather than return, or the
                // later failures would be dropped silently.
                existing.setParticipantResultSummaryJson(
                        mergeSummary(existing.getParticipantResultSummaryJson(), rows, source));
                existing.setSysUserId(sysUserId);
                return followupDAO.update(existing);
            }
        }

        EQAParticipantFollowup followup = new EQAParticipantFollowup();
        followup.setScheme(scheme);
        followup.setCycle(cycle);
        followup.setParticipantOrgId(orgId);
        followup.setFollowupStatus(EQAFollowupStatus.NOTIFIED);
        followup.setNotifiedAt(DateUtil.getNowAsTimestamp());
        followup.setParticipantResultSummaryJson(mergeSummary(null, rows, source));
        followup.setSysUserId(sysUserId);
        followup.setId(followupDAO.insert(followup));
        return followup;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQueueRows() {
        List<EQAParticipantFollowup> open = new ArrayList<>();
        for (EQAParticipantFollowup followup : followupDAO.getAll()) {
            EQAFollowupStatus status = followup.getFollowupStatus();
            if (status != EQAFollowupStatus.ESCALATED && status != EQAFollowupStatus.RESOLVED) {
                open.add(followup);
            }
        }
        open.sort(Comparator.comparing(EQAParticipantFollowup::getNotifiedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAParticipantFollowup followup : open) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", followup.getId());
            dto.put("schemeId", followup.getScheme() == null ? null : followup.getScheme().getId());
            dto.put("schemeName", followup.getScheme() == null ? null : followup.getScheme().getName());
            dto.put("cycleId", followup.getCycle() == null ? null : followup.getCycle().getId());
            dto.put("cycleNumber", followup.getCycle() == null ? null : followup.getCycle().getCycleNumber());
            dto.put("cycleName", followup.getCycle() == null ? null : followup.getCycle().getCycleName());
            dto.put("source", sourceLabel(followup.getScheme()));
            dto.put("followupStatus",
                    followup.getFollowupStatus() == null ? null : followup.getFollowupStatus().name());
            dto.put("notifiedAt", followup.getNotifiedAt() == null ? null : followup.getNotifiedAt().toString());
            dto.put("persistentFailureFlag", followup.getPersistentFailureFlag());
            dto.put("results", summaryRows(followup));
            rows.add(dto);
        }
        return rows;
    }

    @Override
    public EQAParticipantFollowup markEscalated(Long followupId, String sysUserId) {
        EQAParticipantFollowup followup = get(followupId);
        followup.setFollowupStatus(EQAFollowupStatus.ESCALATED);
        followup.setResponseReceivedAt(DateUtil.getNowAsTimestamp());
        followup.setSysUserId(sysUserId);
        return followupDAO.update(followup);
    }

    @Override
    public EQAParticipantFollowup dismiss(Long followupId, EQADismissalCategory category, String notes,
            String sysUserId) {
        if (category == null) {
            throw new IllegalArgumentException("A dismissal needs a category");
        }
        EQAParticipantFollowup followup = get(followupId);
        if (followup.getFollowupStatus() == EQAFollowupStatus.ESCALATED
                || followup.getFollowupStatus() == EQAFollowupStatus.RESOLVED) {
            throw new IllegalStateException(
                    "This follow-up is already " + followup.getFollowupStatus() + " and cannot be dismissed");
        }

        EQACompetencyEventType eventType = competencyEventTypeFor(category);
        for (Long resultId : resultIdsFor(followup)) {
            participantResultDAO.get(resultId)
                    .ifPresent(result -> competencyService.record(result, eventType, null, category, notes, sysUserId));
        }

        followup.setFollowupStatus(EQAFollowupStatus.RESOLVED);
        followup.setResponseReceivedAt(DateUtil.getNowAsTimestamp());
        followup.setResolutionNotes(notes);
        followup.setSysUserId(sysUserId);
        return followupDAO.update(followup);
    }

    @Override
    public List<Long> resultIdsFor(EQAParticipantFollowup followup) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : summaryRows(followup)) {
            Object id = row.get(ROW_RESULT_ID);
            if (id != null) {
                Long parsed = Long.valueOf(String.valueOf(id));
                if (!ids.contains(parsed)) {
                    ids.add(parsed);
                }
            }
        }
        return ids;
    }

    @Override
    public String sourceLabel(EQAProgram scheme) {
        EQASchemeType type = scheme == null ? null : scheme.getSchemeType();
        if (type == EQASchemeType.IN_HOUSE) {
            return "In-house";
        }
        if (type == EQASchemeType.INTER_LAB_SPLIT) {
            return "Inter-lab split";
        }
        return "External provider";
    }

    /**
     * FR-V2.3-02 maps five triage categories onto four competency event types; the
     * "counts against the analyst" split lives in the FR-V2.3-06 rollup, not here.
     */
    private EQACompetencyEventType competencyEventTypeFor(EQADismissalCategory category) {
        switch (category) {
        case KNOWN_EQUIPMENT_ISSUE:
        case PENDING_RE_TEST:
            return EQACompetencyEventType.DISMISSED_EQUIPMENT;
        case TRANSCRIPTION_ERROR:
            return EQACompetencyEventType.DISMISSED_TRANSCRIPTION;
        case ACCEPTABLE_ON_REVIEW:
            return EQACompetencyEventType.DISMISSED_ACCEPTABLE_ON_REVIEW;
        default:
            return EQACompetencyEventType.DISMISSED_OTHER;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> summaryRows(EQAParticipantFollowup followup) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String json = followup.getParticipantResultSummaryJson();
        if (GenericValidator.isBlankOrNull(json)) {
            return rows;
        }
        try {
            JsonNode parsed = JSON.readTree(json).get(SUMMARY_ROWS);
            if (parsed != null && parsed.isArray()) {
                for (JsonNode node : parsed) {
                    rows.add(JSON.convertValue(node, Map.class));
                }
            }
        } catch (JsonProcessingException e) {
            LogEvent.logError(e);
        }
        return rows;
    }

    private String mergeSummary(String existingJson, List<Map<String, Object>> rows, String source) {
        List<Object> merged = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(existingJson)) {
            try {
                JsonNode parsed = JSON.readTree(existingJson).get(SUMMARY_ROWS);
                if (parsed != null && parsed.isArray()) {
                    for (JsonNode node : parsed) {
                        merged.add(JSON.convertValue(node, Map.class));
                    }
                }
            } catch (JsonProcessingException e) {
                LogEvent.logError(e);
            }
        }
        merged.addAll(rows);
        try {
            return JSON.writeValueAsString(Map.of(SUMMARY_SOURCE, source, SUMMARY_ROWS, merged));
        } catch (JsonProcessingException e) {
            LogEvent.logError(e);
            return existingJson;
        }
    }

    /**
     * The participant_org_id FK demands a real organization. For this lab's own
     * queue rows the "participant" is the lab itself, materialized once from the
     * configured site name.
     */
    private Organization selfOrganization(String sysUserId) {
        String siteName = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.SiteName);
        if (GenericValidator.isBlankOrNull(siteName)) {
            siteName = "This laboratory";
        }
        Organization lookup = new Organization();
        lookup.setOrganizationName(siteName);
        Organization existing = organizationService.getOrganizationByName(lookup, true);
        if (existing != null) {
            return existing;
        }
        Organization self = new Organization();
        self.setOrganizationName(siteName);
        self.setIsActive("Y");
        self.setMlsSentinelLabFlag("N");
        self.setSysUserId(sysUserId);
        organizationService.insert(self);
        return self;
    }

    /** Row builder shared by both enqueue paths (FR-V2.3-01, FR-V2.4-08). */
    public static Map<String, Object> summaryRow(EQAParticipantResult result, String targetValue) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(ROW_RESULT_ID, result.getId());
        row.put("analyteId", result.getAnalyteId());
        row.put("reported", result.getResultValue());
        row.put("target", targetValue);
        row.put("analystId", result.getAssignedAnalystId());
        row.put("zScore", result.getZScore());
        row.put("performanceStatus",
                result.getPerformanceStatus() == null ? null : result.getPerformanceStatus().name());
        return row;
    }
}
