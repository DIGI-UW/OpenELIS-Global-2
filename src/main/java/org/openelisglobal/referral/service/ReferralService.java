package org.openelisglobal.referral.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;

public interface ReferralService extends BaseObjectService<Referral, String> {

    Referral getReferralById(String referralId);

    Referral getReferralByAnalysisId(String analysisId);

    List<Referral> getReferralsBySampleId(String id);

    List<Referral> getReferralsByBoxId(Integer boxId);

    long countReferralsBlockingReconcile(Integer boxId);

    List<Referral> getUncanceledOpenReferrals();

    List<Referral> getSentReferrals();

    List<UUID> getSentReferralUuids();

    List<Referral> getReferralsByOrganization(String organizationId, Date lowDate, Date highDate);

    List<Referral> getReferralsByAccessionNumber(String labNumber);

    List<Referral> getReferralByPatientId(String selPatient);

    ReferralDisplayItem convertToDisplayItem(Referral referral);

    List<ReferralDisplayItem> getReferralItems(ReferredOutTestsForm form);

    List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp, Timestamp endTimestamp,
            List<String> testUnitIds, List<String> testIds);

    // FHIR Task-aligned referral lifecycle transitions. Each call mutates
    // Referral.status and appends a row to referral_status_history. Transitions
    // are guarded by ReferralStatus.canTransitionTo; illegal transitions throw
    // IllegalStateException. Missing per-transition required fields throw
    // IllegalArgumentException. No-op (with debug log) when the referral has no
    // subcontract row (historical pre-S-14 data without subcontract metadata).

    void dispatchReferral(String referralId, Timestamp handoffDatetime, String actorUserId, String notes);

    void markReferralReceived(String referralId, String actorUserId, String notes);

    void markReferralCompleted(String referralId, String actorUserId, String notes);

    /**
     * OGC-799 Manual Entry path: advance an Outstanding referral to COMPLETED and
     * set the {@code manually_entered} flag so the row routes from Outstanding
     * straight to History (skipping Returned — needs action, since the validator
     * just entered the result themselves). No-op when the referral is already
     * terminal or has no subcontract (legacy data).
     */
    void markReferralCompletedFromManualEntry(String referralId, String actorUserId);

    /**
     * Mark a Referral as lost in transit. Unlike
     * {@link org.openelisglobal.shipment.service.UnassignedSampleService#markSampleAsLost
     * UnassignedSampleService.markSampleAsLost}, this method accepts referrals
     * already assigned to a shipment box — that's the OGC-799 Outstanding case
     * where the box was dispatched but the sample never reached the reference lab.
     */
    void markReferralAsLost(String referralId, String reason, String actorUserId);

    /**
     * OGC-803 Accept: acknowledge a returned reference-lab result into the patient
     * record. The result is already posted (by the inbound FHIR path), so this is a
     * reconciliation flag — not a re-post. Sets reconciled/at/by, writes a
     * status-history note row (status stays COMPLETED), and routes the row from
     * Returned to History. No-op when the referral is missing, not COMPLETED, or
     * already reconciled.
     */
    void markReferralReconciled(String referralId, String actorUserId);

    /**
     * OGC-804 Reject: terminally reject a returned reference-lab result the lab
     * declines to reconcile. Transitions COMPLETED -> REJECTED, persists the
     * reason, closes the originating Analysis to RejectedByReferenceLab, and fires
     * the recollection notification. Refuses if already reconciled.
     */
    void markReferralRejected(String referralId, String reasonCode, String reasonText, String actorUserId);

    /**
     * OGC-810 Notify reference lab: fire the configurable REFERRAL_NUDGE trigger to
     * nudge a slow reference lab about an outstanding referral, and record a
     * REFERRAL_NUDGE_SENT audit note. {@code freeFormMessage} is optional.
     */
    void nudgeReferenceLab(String referralId, String freeFormMessage, String actorUserId);

    List<ReferralStatusHistory> getSubcontractStatusHistory(String referralId);
}
