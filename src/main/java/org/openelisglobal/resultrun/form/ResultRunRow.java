package org.openelisglobal.resultrun.form;

/**
 * One value of a run as the instrument reported it. The test and component name
 * the worklist row it is accepted into (FR-E10); a value staged without a
 * component is the test's main value and names its primary component.
 * {@code stagedResult} is the value the worklist row is prefilled with: for a
 * dictionary test the choice the import resolved, where {@code value} is the
 * instrument's own text.
 */
public record ResultRunRow(String id, String accessionNumber, String testName, String part, String value, String units,
        boolean control, boolean duplicate, String importIssueReason, String instrumentFlags, String completedAt,
        String flag, ResultRunRowCategory category, String testId, String componentId, String stagedResult) {
}
