package org.openelisglobal.batchworkplan.form;

import java.util.List;

/**
 * A page of pending tests, how many match in all, and the values each filter
 * can take across every pending test the user may work, not just this page.
 */
public record PendingTestsPageResponse(List<PendingBatchTestResponse> items, long total, List<FilterOption> tests,
        List<FilterOption> panels, List<FilterOption> units, List<String> priorities) {

    public record FilterOption(String id, String name) {
    }
}
