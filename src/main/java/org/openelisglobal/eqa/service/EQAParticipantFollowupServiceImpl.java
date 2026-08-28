package org.openelisglobal.eqa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.valueholder.AlertNotificationPayload;
import org.openelisglobal.analyte.dao.AnalyteDAO;
import org.openelisglobal.analyte.valueholder.Analyte;
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
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.notification.service.sender.EmailNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.openelisglobal.person.valueholder.Person;
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
    private static final String WITHDRAWN = "Withdrawn";

    /**
     * FR-V2.5-06's triage moves. Resolution and removal are terminal, and an
     * escalated row can still be investigated and closed — the escalation is a
     * priority, not an end state.
     */
    private static final Map<EQAFollowupStatus, Set<EQAFollowupStatus>> TRIAGE_EDGES = new EnumMap<>(
            EQAFollowupStatus.class);

    static {
        TRIAGE_EDGES.put(EQAFollowupStatus.NOTIFIED,
                EnumSet.of(EQAFollowupStatus.RESPONSE_RECEIVED, EQAFollowupStatus.UNDER_INVESTIGATION,
                        EQAFollowupStatus.RESOLVED, EQAFollowupStatus.REMOVED_FROM_PROGRAM));
        TRIAGE_EDGES.put(EQAFollowupStatus.RESPONSE_RECEIVED, EnumSet.of(EQAFollowupStatus.UNDER_INVESTIGATION,
                EQAFollowupStatus.RESOLVED, EQAFollowupStatus.REMOVED_FROM_PROGRAM));
        TRIAGE_EDGES.put(EQAFollowupStatus.UNDER_INVESTIGATION,
                EnumSet.of(EQAFollowupStatus.RESOLVED, EQAFollowupStatus.REMOVED_FROM_PROGRAM));
        TRIAGE_EDGES.put(EQAFollowupStatus.ESCALATED, EnumSet.of(EQAFollowupStatus.UNDER_INVESTIGATION,
                EQAFollowupStatus.RESOLVED, EQAFollowupStatus.REMOVED_FROM_PROGRAM));
    }

    @Autowired
    private EQAParticipantFollowupDAO followupDAO;

    @Autowired
    private EQAProgramEnrollmentService enrollmentService;

    @Autowired
    private OrganizationContactService organizationContactService;

    @Autowired
    private EmailNotificationSender emailSender;

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private EQAAnalystCompetencyService competencyService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AnalyteDAO analyteDAO;

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
        return enqueue(scheme, cycle, Long.parseLong(selfOrganization(sysUserId).getId()), rows, false, sysUserId);
    }

    @Override
    public EQAParticipantFollowup enqueueForOrganization(EQAProgram scheme, EQACycle cycle, Long participantOrgId,
            List<Map<String, Object>> rows, boolean persistentFailure, String sysUserId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return enqueue(scheme, cycle, participantOrgId, rows, persistentFailure, sysUserId);
    }

    private EQAParticipantFollowup enqueue(EQAProgram scheme, EQACycle cycle, Long orgId,
            List<Map<String, Object>> rows, boolean persistentFailure, String sysUserId) {
        String source = sourceLabel(scheme);

        for (EQAParticipantFollowup existing : followupDAO.getAllMatching("cycle.id", cycle.getId())) {
            if (orgId.equals(existing.getParticipantOrgId())) {
                // The register is unique on cycle + org, and one cycle can score
                // several panels and analytes. Merge rather than return, or the
                // later failures would be dropped silently.
                existing.setParticipantResultSummaryJson(
                        mergeSummary(existing.getParticipantResultSummaryJson(), rows, source));
                // A closed row merged into is a new failure arriving after triage
                // finished, and the queue only shows open rows — leaving it closed
                // hides that failure from the reviewer for good.
                if (existing.getFollowupStatus() == EQAFollowupStatus.ESCALATED
                        || existing.getFollowupStatus() == EQAFollowupStatus.RESOLVED) {
                    existing.setFollowupStatus(EQAFollowupStatus.NOTIFIED);
                    existing.setNotifiedAt(DateUtil.getNowAsTimestamp());
                    existing.setResponseReceivedAt(null);
                }
                existing.setSysUserId(sysUserId);
                return followupDAO.update(existing);
            }
        }

        EQAParticipantFollowup followup = new EQAParticipantFollowup();
        followup.setScheme(scheme);
        followup.setCycle(cycle);
        followup.setParticipantOrgId(orgId);
        followup.setFollowupStatus(persistentFailure ? EQAFollowupStatus.ESCALATED : EQAFollowupStatus.NOTIFIED);
        followup.setPersistentFailureFlag(persistentFailure);
        followup.setNotifiedAt(DateUtil.getNowAsTimestamp());
        followup.setParticipantResultSummaryJson(mergeSummary(null, rows, source));
        followup.setSysUserId(sysUserId);
        followup.setId(followupDAO.insert(followup));
        return followup;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQueueRows() {
        Long self = selfOrganizationId();
        List<EQAParticipantFollowup> open = new ArrayList<>();
        for (EQAParticipantFollowup followup : followupDAO.getAll()) {
            EQAFollowupStatus status = followup.getFollowupStatus();
            // Only this lab's own follow-ups: a row about another laboratory belongs to
            // the provider register, whose escalation is not a local non-conformity
            // (AC-V2.5-10). With no self organization yet, no row can be this lab's.
            boolean mine = self != null && self.equals(followup.getParticipantOrgId());
            if (mine && status != EQAFollowupStatus.ESCALATED && status != EQAFollowupStatus.RESOLVED) {
                open.add(followup);
            }
        }
        return toRowDtos(open);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpenProviderFollowups() {
        Long self = selfOrganizationId();
        long open = 0;
        for (EQAParticipantFollowup followup : followupDAO.getAll()) {
            EQAFollowupStatus status = followup.getFollowupStatus();
            // Same membership rule as the register below; terminal states drop out.
            if ((self == null || !self.equals(followup.getParticipantOrgId())) && status != EQAFollowupStatus.RESOLVED
                    && status != EQAFollowupStatus.REMOVED_FROM_PROGRAM) {
                open++;
            }
        }
        return open;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProviderRegisterRows() {
        Long self = selfOrganizationId();
        List<EQAParticipantFollowup> theirs = new ArrayList<>();
        for (EQAParticipantFollowup followup : followupDAO.getAll()) {
            if (self == null || !self.equals(followup.getParticipantOrgId())) {
                theirs.add(followup);
            }
        }
        List<Map<String, Object>> rows = toRowDtos(theirs);
        for (Map<String, Object> row : rows) {
            Organization participant = organizationService
                    .getOrganizationById(String.valueOf(row.get("participantOrgId")));
            row.put("organizationName", participant == null ? null : participant.getOrganizationName());
        }
        return rows;
    }

    /** Newest first, with the fields both registers render. */
    private List<Map<String, Object>> toRowDtos(List<EQAParticipantFollowup> followups) {
        followups.sort(Comparator.comparing(EQAParticipantFollowup::getNotifiedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAParticipantFollowup followup : followups) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", followup.getId());
            dto.put("schemeId", followup.getScheme() == null ? null : followup.getScheme().getId());
            dto.put("schemeName", followup.getScheme() == null ? null : followup.getScheme().getName());
            dto.put("cycleId", followup.getCycle() == null ? null : followup.getCycle().getId());
            dto.put("cycleNumber", followup.getCycle() == null ? null : followup.getCycle().getCycleNumber());
            dto.put("cycleName", followup.getCycle() == null ? null : followup.getCycle().getCycleName());
            dto.put("participantOrgId", followup.getParticipantOrgId());
            dto.put("source", sourceLabel(followup.getScheme()));
            // The queue tags the source through i18n, so it needs the enum as well
            // as the label the register and its CSV export print.
            dto.put("schemeType", followup.getScheme() == null || followup.getScheme().getSchemeType() == null ? null
                    : followup.getScheme().getSchemeType().name());
            dto.put("followupStatus",
                    followup.getFollowupStatus() == null ? null : followup.getFollowupStatus().name());
            dto.put("notifiedAt", followup.getNotifiedAt() == null ? null : followup.getNotifiedAt().toString());
            dto.put("responseReceivedAt",
                    followup.getResponseReceivedAt() == null ? null : followup.getResponseReceivedAt().toString());
            dto.put("resolutionNotes", followup.getResolutionNotes());
            dto.put("persistentFailureFlag", followup.getPersistentFailureFlag());
            dto.put("results", summaryRows(followup));
            rows.add(dto);
        }
        nameAnalytes(rows);
        return rows;
    }

    /**
     * The queue's Analyte column reads a name, and the snapshot only holds the id —
     * resolved here in one query for the whole queue rather than one per row. An id
     * that no longer resolves keeps the id as its label, so a data fault stays
     * visible instead of rendering as a blank cell.
     */
    @SuppressWarnings("unchecked")
    private void nameAnalytes(List<Map<String, Object>> queueRows) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> queueRow : queueRows) {
            for (Map<String, Object> result : (List<Map<String, Object>>) queueRow.get("results")) {
                Object analyteId = result.get("analyteId");
                if (analyteId != null && !ids.contains(String.valueOf(analyteId))) {
                    ids.add(String.valueOf(analyteId));
                }
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<String, String> names = new LinkedHashMap<>();
        for (Analyte analyte : analyteDAO.get(ids)) {
            names.put(analyte.getId(), analyte.getAnalyteName());
        }
        for (Map<String, Object> queueRow : queueRows) {
            for (Map<String, Object> result : (List<Map<String, Object>>) queueRow.get("results")) {
                String analyteId = result.get("analyteId") == null ? null : String.valueOf(result.get("analyteId"));
                result.put("analyteName", analyteId == null ? null : names.getOrDefault(analyteId, analyteId));
            }
        }
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
    public EQAParticipantFollowup transitionStatus(Long followupId, EQAFollowupStatus target, String notes,
            String sysUserId) {
        if (target == null) {
            throw new IllegalArgumentException("A triage action needs a target status");
        }
        EQAParticipantFollowup followup = get(followupId);
        EQAFollowupStatus from = followup.getFollowupStatus();
        if (!TRIAGE_EDGES.getOrDefault(from, EnumSet.noneOf(EQAFollowupStatus.class)).contains(target)) {
            throw new IllegalStateException("Cannot move a follow-up from " + from + " to " + target);
        }

        if (target == EQAFollowupStatus.REMOVED_FROM_PROGRAM) {
            withdrawEnrollment(followup, notes, sysUserId);
        }
        if (target == EQAFollowupStatus.RESPONSE_RECEIVED || followup.getResponseReceivedAt() == null) {
            followup.setResponseReceivedAt(DateUtil.getNowAsTimestamp());
        }
        if (!GenericValidator.isBlankOrNull(notes)) {
            followup.setResolutionNotes(notes);
        }
        followup.setFollowupStatus(target);
        followup.setSysUserId(sysUserId);
        return followupDAO.update(followup);
    }

    /**
     * BR-013: removing a participant from the programme is the enrollment's
     * withdrawal, not just a register state — otherwise the next cycle would size
     * itself to include a laboratory that was just removed. An enrollment already
     * withdrawn is left alone rather than transitioned again.
     */
    private void withdrawEnrollment(EQAParticipantFollowup followup, String reason, String sysUserId) {
        if (followup.getScheme() == null) {
            return;
        }
        for (EQAProgramEnrollment enrollment : enrollmentService.findByProgramId(followup.getScheme().getId())) {
            if (followup.getParticipantOrgId().equals(enrollment.getOrganizationId())
                    && !WITHDRAWN.equals(enrollment.getStatus())) {
                enrollmentService.updateStatus(enrollment.getId(), WITHDRAWN,
                        GenericValidator.isBlankOrNull(reason) ? "Removed after EQA follow-up" : reason, sysUserId);
            }
        }
    }

    @Override
    public Map<String, Object> notifyParticipant(Long followupId, String sysUserId) {
        EQAParticipantFollowup followup = get(followupId);
        String recipient = contactEmail(followup.getParticipantOrgId());
        String subject = "EQA follow-up: " + schemeName(followup) + ", cycle " + cycleLabel(followup);
        String message = notificationBody(followup);

        boolean emailed = false;
        if (!GenericValidator.isBlankOrNull(recipient)) {
            EmailNotification email = new EmailNotification();
            email.setRecipientEmailAddress(recipient);
            email.setPayload(new AlertNotificationPayload(subject, message));
            try {
                emailSender.send(email);
                emailed = true;
            } catch (RuntimeException e) {
                // A mail transport that is not configured must not roll back the
                // notification itself: the reviewer falls back to the CSV instead.
                LogEvent.logError(e);
            }
        }

        followup.setNotifiedAt(DateUtil.getNowAsTimestamp());
        followup.setSysUserId(sysUserId);
        followupDAO.update(followup);

        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("followupId", followupId);
        outcome.put("emailed", emailed);
        outcome.put("recipient", recipient);
        outcome.put("subject", subject);
        outcome.put("message", message);
        return outcome;
    }

    /**
     * ponytail: email plus the CSV fallback the FR names. A FHIR
     * CommunicationRequest is the third channel it allows — add it when a
     * deployment actually reads one; nothing in this codebase writes that resource
     * today.
     */
    private String notificationBody(EQAParticipantFollowup followup) {
        StringBuilder body = new StringBuilder("Your laboratory's results for ").append(schemeName(followup))
                .append(", cycle ").append(cycleLabel(followup))
                .append(", were assessed as unacceptable for the following tests:\n");
        for (Map<String, Object> row : summaryRows(followup)) {
            body.append("- ").append(row.getOrDefault("testName", row.getOrDefault("analyteName", "")))
                    .append(": reported ").append(row.getOrDefault("reported", "")).append(", target ")
                    .append(row.getOrDefault("target", "")).append('\n');
        }
        if (Boolean.TRUE.equals(followup.getPersistentFailureFlag())) {
            body.append("\nThis is a repeated failure across recent cycles and has been escalated.\n");
        }
        return body.append("\nPlease investigate and reply with your corrective action.").toString();
    }

    private String contactEmail(Long organizationId) {
        for (OrganizationContact contact : organizationContactService
                .getListForOrganizationId(String.valueOf(organizationId))) {
            Person person = contact.getPerson();
            if (person != null && !GenericValidator.isBlankOrNull(person.getEmail())) {
                return person.getEmail();
            }
        }
        return null;
    }

    private String schemeName(EQAParticipantFollowup followup) {
        return followup.getScheme() == null ? "" : followup.getScheme().getName();
    }

    private String cycleLabel(EQAParticipantFollowup followup) {
        EQACycle cycle = followup.getCycle();
        if (cycle == null) {
            return "";
        }
        return GenericValidator.isBlankOrNull(cycle.getCycleName()) ? String.valueOf(cycle.getCycleNumber())
                : cycle.getCycleName();
    }

    @Override
    @Transactional(readOnly = true)
    public Long selfOrganizationId() {
        String siteName = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.SiteName);
        Organization lookup = new Organization();
        lookup.setOrganizationName(GenericValidator.isBlankOrNull(siteName) ? "This laboratory" : siteName);
        Organization existing = organizationService.getOrganizationByName(lookup, true);
        return existing == null ? null : Long.valueOf(existing.getId());
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
