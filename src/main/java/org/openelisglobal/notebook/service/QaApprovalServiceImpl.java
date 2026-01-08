package org.openelisglobal.notebook.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of QA approval workflow for Stage 4 (Reporting & Release).
 *
 * Persists QA approval decisions to JSONB data field, manages page status
 * transitions, and maintains audit trail of all approval-related events.
 */
@Service
public class QaApprovalServiceImpl implements QaApprovalService {

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Override
    @Transactional
    public Map<String, Object> submitQaApproval(Integer pageId, String approvalStatus, String analystComments,
            String userId) {

        Map<String, Object> result = new HashMap<>();

        if (pageId == null) {
            result.put("success", false);
            result.put("error", "Page ID is required");
            return result;
        }

        // Validate approval status
        if (!("APPROVED".equals(approvalStatus) || "REJECTED".equals(approvalStatus)
                || "CONDITIONAL".equals(approvalStatus))) {
            result.put("success", false);
            result.put("error", "Invalid approval status: " + approvalStatus);
            return result;
        }

        try {
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                result.put("success", false);
                result.put("error", "Page not found: " + pageId);
                return result;
            }

            LocalDateTime approvalTimestamp = LocalDateTime.now();

            // Get all samples on this page
            List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);

            // Build QA approval data structure
            Map<String, Object> qaApprovalData = new HashMap<>();
            qaApprovalData.put("approvalStatus", approvalStatus);
            qaApprovalData.put("approvalDate", approvalTimestamp.toString());
            qaApprovalData.put("analyst", userId);
            qaApprovalData.put("comments", analystComments != null ? analystComments : "");
            qaApprovalData.put("pageStatus", "APPROVED_FOR_RELEASE");

            // Update page status based on approval decision
            String newPageStatus = "APPROVED".equals(approvalStatus) ? "APPROVED_FOR_RELEASE"
                    : "CONDITIONAL".equals(approvalStatus) ? "APPROVED_WITH_CONDITIONS" : "REJECTED";

            // Persist QA approval data to all samples on the page
            for (NotebookPageSample sample : samples) {
                if (sample.getData() == null) {
                    sample.setData(new HashMap<>());
                }

                Map<String, Object> sampleData = sample.getData();
                sampleData.put("qaApprovalData", qaApprovalData);
                sampleData.put("pageStatus", newPageStatus);

                sample.setData(sampleData);
                notebookPageSampleService.save(sample);

                LogEvent.logDebug(this.getClass().getName(), "submitQaApproval",
                        "Updated sample " + sample.getId() + " with QA approval data");
            }

            // Log audit event
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("approvalStatus", approvalStatus);
            auditDetails.put("samplesAffected", samples.size());
            auditDetails.put("comments", analystComments);
            logQaAuditEvent(pageId, "SUBMITTED", auditDetails, userId);

            // Build response
            result.put("success", true);
            result.put("pageId", pageId);
            result.put("approvalStatus", approvalStatus);
            result.put("approvalDate", approvalTimestamp);
            result.put("analyst", userId);
            result.put("samplesAffected", samples.size());
            result.put("pageStatus", newPageStatus);
            result.put("message", approvalStatus + " approval submitted for " + samples.size() + " samples");

            LogEvent.logInfo(this.getClass().getName(), "submitQaApproval",
                    "QA approval submitted: status=" + approvalStatus + ", pageId=" + pageId + ", analyst=" + userId
                            + ", samplesAffected=" + samples.size());

            return result;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "submitQaApproval",
                    "Error submitting QA approval: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Error submitting QA approval: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getQaApprovalStatus(Integer pageId) {

        Map<String, Object> result = new HashMap<>();

        if (pageId == null) {
            result.put("error", "Page ID is required");
            return result;
        }

        try {
            List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);

            if (samples.isEmpty()) {
                result.put("pageId", pageId);
                result.put("hasApproval", false);
                result.put("message", "No samples found for page");
                return result;
            }

            // Check first sample for QA approval data (should be consistent across all)
            NotebookPageSample firstSample = samples.get(0);
            Map<String, Object> sampleData = firstSample.getData();

            if (sampleData == null || !sampleData.containsKey("qaApprovalData")) {
                result.put("pageId", pageId);
                result.put("hasApproval", false);
                result.put("message", "No QA approval found for this page");
                return result;
            }

            Map<String, Object> qaApprovalData = (Map<String, Object>) sampleData.get("qaApprovalData");

            result.put("pageId", pageId);
            result.put("hasApproval", true);
            result.put("approvalStatus", qaApprovalData.get("approvalStatus"));
            result.put("approvalDate", qaApprovalData.get("approvalDate"));
            result.put("analyst", qaApprovalData.get("analyst"));
            result.put("comments", qaApprovalData.get("comments"));
            result.put("pageStatus", qaApprovalData.get("pageStatus"));
            result.put("samplesReviewed", samples.size());

            LogEvent.logDebug(this.getClass().getName(), "getQaApprovalStatus",
                    "Retrieved QA approval status for page " + pageId);

            return result;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getQaApprovalStatus",
                    "Error retrieving QA approval status: " + e.getMessage());
            result.put("error", "Error retrieving QA approval status: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApprovedForExport(Integer pageId) {

        if (pageId == null) {
            return false;
        }

        try {
            Map<String, Object> approvalStatus = getQaApprovalStatus(pageId);

            if (!((boolean) approvalStatus.getOrDefault("hasApproval", false))) {
                return false;
            }

            String status = (String) approvalStatus.get("approvalStatus");
            return "APPROVED".equals(status);

        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "isApprovedForExport",
                    "Error checking export approval status: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> revokeQaApproval(Integer pageId, String revocationReason, String userId) {

        Map<String, Object> result = new HashMap<>();

        if (pageId == null) {
            result.put("success", false);
            result.put("error", "Page ID is required");
            return result;
        }

        try {
            List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);

            // Remove QA approval data from all samples
            for (NotebookPageSample sample : samples) {
                if (sample.getData() != null) {
                    Map<String, Object> sampleData = sample.getData();
                    sampleData.remove("qaApprovalData");
                    sampleData.put("pageStatus", "PENDING_QA_REVIEW");

                    sample.setData(sampleData);
                    notebookPageSampleService.save(sample);
                }
            }

            // Log audit event
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("revocationReason", revocationReason);
            auditDetails.put("samplesAffected", samples.size());
            logQaAuditEvent(pageId, "REVOKED", auditDetails, userId);

            result.put("success", true);
            result.put("pageId", pageId);
            result.put("samplesAffected", samples.size());
            result.put("message",
                    "QA approval revoked for " + samples.size() + " samples. Reason: " + revocationReason);

            LogEvent.logInfo(this.getClass().getName(), "revokeQaApproval",
                    "QA approval revoked: pageId=" + pageId + ", reason=" + revocationReason + ", userId=" + userId);

            return result;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "revokeQaApproval",
                    "Error revoking QA approval: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Error revoking QA approval: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public void logQaAuditEvent(Integer pageId, String eventType, Map<String, Object> eventDetails, String userId) {

        if (pageId == null || eventType == null || userId == null) {
            return;
        }

        try {
            List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);

            // Create audit event record
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", eventType);
            auditEvent.put("timestamp", LocalDateTime.now().toString());
            auditEvent.put("userId", userId);
            auditEvent.put("details", eventDetails);

            // Append to audit trail in each sample
            for (NotebookPageSample sample : samples) {
                if (sample.getData() == null) {
                    sample.setData(new HashMap<>());
                }

                Map<String, Object> sampleData = sample.getData();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> auditTrail = (List<Map<String, Object>>) sampleData
                        .computeIfAbsent("qaAuditTrail", k -> new ArrayList<>());

                auditTrail.add(auditEvent);
                sampleData.put("qaAuditTrail", auditTrail);

                sample.setData(sampleData);
                notebookPageSampleService.save(sample);
            }

            LogEvent.logDebug(this.getClass().getName(), "logQaAuditEvent",
                    "Logged QA audit event: type=" + eventType + ", pageId=" + pageId + ", userId=" + userId);

        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "logQaAuditEvent",
                    "Error logging QA audit event: " + e.getMessage());
            // Don't throw exception - audit logging should not block main operations
        }
    }
}
