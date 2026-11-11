package org.openelisglobal.eqa.valueholder;

/**
 * Whether a roster row still counts towards what the cycle needs. Mirrors the
 * qa/032 CHECK; a WITHDRAWN row stays on the roster so the audit reads straight
 * but is excluded from the prep gate's arithmetic and from the shipment
 * workbench.
 */
public enum EQACycleParticipantStatus {
    ACTIVE, WITHDRAWN
}
