package org.openelisglobal.resultrun.form;

/** Each chip's row count over the whole run the user can see (FR-E1a). */
public record ResultRunChipCounts(int all, int needsReview, int critical, int abnormal, int exceptions, int held,
        int normal) {
}
