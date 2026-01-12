package org.openelisglobal.notebook.service;

import java.util.Map;

/**
 * Service for QA approval workflow in Stage 4 (Reporting & Release).
 *
 * Handles quality assurance approval decisions for bioanalytical study results,
 * including persistence of approval metadata, audit trail tracking, and status
 * management for external data export authorization.
 */
public interface QaApprovalService {

    /**
     * Submit QA approval for a notebook page. Records the analyst's decision,
     * timestamp, and comments. Sets page status to APPROVED_FOR_RELEASE.
     *
     * @param pageId          the notebook page ID
     * @param approvalStatus  the approval decision (APPROVED, REJECTED, or
     *                        CONDITIONAL)
     * @param analystComments optional analyst comments/observations
     * @param userId          the user (analyst) performing the approval
     * @return map containing approval confirmation with status, timestamp, and
     *         analyst info
     */
    Map<String, Object> submitQaApproval(Integer pageId, String approvalStatus, String analystComments, String userId);

    /**
     * Get QA approval status and history for a page.
     *
     * @param pageId the notebook page ID
     * @return map containing approval status, approval date, analyst info, and
     *         historical approvals
     */
    Map<String, Object> getQaApprovalStatus(Integer pageId);

    /**
     * Check if a page has been approved for external data export.
     *
     * @param pageId the notebook page ID
     * @return true if page has APPROVED QA status, false otherwise
     */
    boolean isApprovedForExport(Integer pageId);

    /**
     * Revoke a previous QA approval (for corrections or re-evaluation). Sets page
     * status back to PENDING_QA_REVIEW.
     *
     * @param pageId           the notebook page ID
     * @param revocationReason reason for revocation
     * @param userId           the user performing the revocation
     * @return map containing revocation confirmation
     */
    Map<String, Object> revokeQaApproval(Integer pageId, String revocationReason, String userId);

    /**
     * Log QA approval event to audit trail.
     *
     * @param pageId       the notebook page ID
     * @param eventType    type of event (SUBMITTED, APPROVED, REJECTED, REVOKED)
     * @param eventDetails details about the event
     * @param userId       the user who triggered the event
     */
    void logQaAuditEvent(Integer pageId, String eventType, Map<String, Object> eventDetails, String userId);
}
