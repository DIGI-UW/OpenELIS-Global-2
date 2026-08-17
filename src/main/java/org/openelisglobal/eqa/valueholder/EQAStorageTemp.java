package org.openelisglobal.eqa.valueholder;

/**
 * Storage temperature requirement for a panel (FR-V2.1-17).
 *
 * <p>
 * The FRS spells the cold values {@code frozen_-20C} and
 * {@code ultra_frozen_-80C}. A hyphen cannot appear in a Java identifier and
 * this codebase persists enum names verbatim, so those two become
 * FROZEN_MINUS_20C and ULTRA_FROZEN_MINUS_80C. Same set, same order.
 */
public enum EQAStorageTemp {
    AMBIENT, REFRIGERATED_2_8C, FROZEN_MINUS_20C, ULTRA_FROZEN_MINUS_80C, DRY_ICE
}
