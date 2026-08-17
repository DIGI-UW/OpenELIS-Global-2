package org.openelisglobal.eqa.valueholder;

/**
 * Panel lifecycle (FR-V2.1-11). In-house panels go SEALED → DISTRIBUTED →
 * UNBLINDED; provider-side panels skip the unblind and go SEALED → DISTRIBUTED
 * → SCORED.
 */
public enum EQAPanelStatus {
    PREPARING, SEALED, DISTRIBUTED, UNBLINDED, SCORED, CLOSED
}
