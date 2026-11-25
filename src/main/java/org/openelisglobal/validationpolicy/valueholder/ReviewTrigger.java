package org.openelisglobal.validationpolicy.valueholder;

/** OGC-1364: which saved results wait for a validator. */
public enum ReviewTrigger {
    /** Every result waits for review. */
    ALL,
    /**
     * Only results the clearance rule does not clear (the Validation page's Clear
     * lane is released at entry).
     */
    NEEDS_REVIEW,
    /** Results are released at entry. */
    NONE
}
