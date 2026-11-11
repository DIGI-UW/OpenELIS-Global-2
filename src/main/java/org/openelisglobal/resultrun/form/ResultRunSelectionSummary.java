package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * What a bulk action on a selection would cover (FR-E11): the eligible rows by
 * flag, and every row it would skip with the reason.
 */
public record ResultRunSelectionSummary(int selected, int critical, int abnormal, int normal,
        List<SkippedRow> skipped) {

    public record SkippedRow(String id, String accessionNumber, String testName, ResultRunRowCategory reason) {
    }
}
