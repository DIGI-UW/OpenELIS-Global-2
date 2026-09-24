package org.openelisglobal.eqa.valueholder;

/**
 * Panel lifecycle. In-house panels go SEALED → DISTRIBUTED → UNBLINDED;
 * provider-side panels skip the unblind and go SEALED → DISTRIBUTED → SCORED.
 */
public enum EQAPanelStatus {
    PREPARING, SEALED, DISTRIBUTED, UNBLINDED, SCORED, CLOSED
}
