package org.openelisglobal.reports.action.implementation.reportBeans;

/**
 * One sample of an order in the patient report's Samples block (FRS FR-A59).
 *
 * @param number          the sample's number within the order, as the results'
 *                        Sample column prints it
 * @param collected       the collection date and time, or null when not
 *                        recorded
 * @param rejectionReason the localized rejection reason when the sample was
 *                        rejected, otherwise null
 * @param conditions      the collection conditions entered for the sample, or
 *                        null
 */
public record ReportSampleLine(String number, String type, String collected, String received, String rejectionReason,
        String conditions) {

    public boolean rejected() {
        return rejectionReason != null;
    }
}
