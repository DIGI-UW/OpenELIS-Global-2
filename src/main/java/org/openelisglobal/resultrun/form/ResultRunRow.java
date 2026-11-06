package org.openelisglobal.resultrun.form;

/** One value of a run as the instrument reported it. */
public record ResultRunRow(String id, String accessionNumber, String testName, String part, String value, String units,
        boolean control, boolean duplicate, String importIssueReason, String instrumentFlags, String completedAt) {
}
