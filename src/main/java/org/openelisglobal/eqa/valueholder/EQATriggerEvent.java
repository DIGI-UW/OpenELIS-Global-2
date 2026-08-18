package org.openelisglobal.eqa.valueholder;

/**
 * What caused a cycle state transition (FR-V2.1-21). PANEL_RECEIPT is a T-11
 * addition (qa/024 extends the DB CHECK): the receipt insert's automatic
 * planned → panel_received move on the participant machine (FR-V2.1-20).
 */
public enum EQATriggerEvent {
    LAST_VALIDATED_RESULT, FHIR_SUBMIT_SUCCESS, FHIR_SUBMIT_FAILURE_RETRY, SCORE_INTAKE, DEADLINE_TIMER,
    ALL_SHIPMENTS_DELIVERED, ALL_SUBMISSIONS_RECEIVED, PANEL_SEAL, PANEL_UNBLIND, HOMOGENEITY_QC_PASSED,
    MANUAL_OVERRIDE, SCHEDULED_JOB, PANEL_RECEIPT
}
