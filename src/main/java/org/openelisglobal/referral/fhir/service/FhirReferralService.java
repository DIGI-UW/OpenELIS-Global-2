package org.openelisglobal.referral.fhir.service;

import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.referral.valueholder.Referral;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FhirReferralService {

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    void setReferralResult(ReferralResultsImportObjects resultsImport);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    Bundle referAnalysisesToOrganization(Referral referral) throws FhirLocalPersistingException;

    /**
     * Publish the Manual Entry completion of a referral to the FHIR store: PUTs the
     * Task to status=completed (with an output reference to a fresh
     * DiagnosticReport), the ServiceRequest to status=completed, and publishes the
     * DiagnosticReport + Observations carrying the values typed in at Result Entry.
     *
     * <p>
     * Best-effort: skips silently when the referral has no fhir_uuid (legacy data);
     * throws {@link FhirLocalPersistingException} on FHIR-store errors so the
     * caller can log + continue without rolling back the local DB transition.
     */
    void publishManualEntryCompletion(Referral referral, String actorUserId) throws FhirLocalPersistingException;

    /**
     * Publish a "lost in transit" cancellation to the FHIR store: PUTs the Task to
     * status=cancelled (with statusReason "lost in transit" and a note carrying the
     * user-supplied reason) and the ServiceRequest to status=revoked so the
     * receiving lab sees the referral is no longer expected to complete.
     *
     * <p>
     * Best-effort: skips silently when the referral or its analysis has no
     * fhir_uuid (legacy data); throws {@link FhirLocalPersistingException} on
     * FHIR-store errors so the caller can log + continue without rolling back the
     * local DB transition.
     */
    void publishReferralLost(Referral referral, String reason, String actorUserId) throws FhirLocalPersistingException;

    /**
     * Publish an OGC-804 rejection to the FHIR store: PUTs the Task to
     * status=rejected (with statusReason "rejected by reference lab" and a note
     * carrying the rejection reason) and the ServiceRequest to status=revoked so
     * the receiving lab sees the returned result was declined.
     *
     * <p>
     * Best-effort: skips silently when the referral or its analysis has no
     * fhir_uuid; throws {@link FhirLocalPersistingException} on FHIR-store errors
     * so the caller can log + continue without rolling back the local DB
     * transition.
     */
    void publishReferralRejected(Referral referral, String reasonText, String actorUserId)
            throws FhirLocalPersistingException;

    // Bundle cancelReferralToOrganization(String organizationId, String sampleId,
    // List<String>
    // analysisIds)
    // throws FhirLocalPersistingException;

}
