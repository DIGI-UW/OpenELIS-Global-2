package org.openelisglobal.reports.action.implementation.reportBeans;

/**
 * One component of a multi-component test as the patient report prints it, in
 * the result columns under the test's name (FRS FR-A29).
 *
 * @param flagCode the component's own flag code from its own range, or empty
 */
public record ReportComponentLine(String label, String value, String flagCode, String range, String unit) {
}
