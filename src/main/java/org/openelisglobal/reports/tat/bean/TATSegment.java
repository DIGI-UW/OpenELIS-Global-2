package org.openelisglobal.reports.tat.bean;

public enum TATSegment {
    ORDER_TO_COLLECTION("Order to Collection"), COLLECTION_TO_RECEIPT("Collection to Receipt"),
    RECEIPT_TO_TESTING("Receipt to Testing Started"), RECEIPT_TO_RESULT("Receipt to Result Entry"),
    RECEIPT_TO_VALIDATION("Receipt to Validation"), RESULT_TO_VALIDATION("Result Entry to Validation"),
    OVERALL("Overall TAT");

    private final String displayName;

    TATSegment(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Returns true for segments 1-3 (per-order), false for 4-7 (per-test) */
    public boolean isPerOrder() {
        return this == ORDER_TO_COLLECTION || this == COLLECTION_TO_RECEIPT || this == RECEIPT_TO_TESTING;
    }
}
