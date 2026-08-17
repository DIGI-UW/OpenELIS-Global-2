package org.openelisglobal.eqa.valueholder;

/**
 * Where a panel's material came from (FR-V2.1-17). Null is permitted for
 * in-house schemes, which use the V2.4 blinding flow instead. VENDOR_SOURCED
 * and MIXED require the vendor fields.
 */
public enum EQAPanelSourceType {
    IN_HOUSE_ALIQUOTED, VENDOR_SOURCED, MIXED
}
