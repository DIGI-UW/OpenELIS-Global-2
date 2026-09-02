package org.openelisglobal.eqa.valueholder;

/**
 * What caused a cycle state transition (FR-V2.1-21). PANEL_RECEIPT (added with
 * liquibase qa/024, which extends the DB CHECK) is the receipt insert's
 * automatic planned → panel_received move on the participant machine
 * (FR-V2.1-20). FIRST_SHIPMENT_SENT (qa/028) is its provider-side counterpart:
 * the first participant dispatch moves ready_to_ship → shipped (FR-V2.5-13).
 * V1_BACKFILL (qa/022) marks the audit row written when the V1-absorption
 * migration creates a synthetic cycle for a legacy distribution.
 */
public enum EQATriggerEvent {
    LAST_VALIDATED_RESULT, FHIR_SUBMIT_SUCCESS, FHIR_SUBMIT_FAILURE_RETRY, SCORE_INTAKE, DEADLINE_TIMER,
    ALL_SHIPMENTS_DELIVERED, ALL_SUBMISSIONS_RECEIVED, PANEL_SEAL, PANEL_UNBLIND, HOMOGENEITY_QC_PASSED,
    MANUAL_OVERRIDE, SCHEDULED_JOB, PANEL_RECEIPT, FIRST_SHIPMENT_SENT, V1_BACKFILL
}
