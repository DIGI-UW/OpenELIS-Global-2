package org.openelisglobal.inventory.report;

import java.sql.Timestamp;
import java.util.List;

/**
 * Parameters for an inventory report. Report type and export format are
 * required; the rest are optional filters only some report types honor.
 *
 * <p>
 * The two dates mean different things per report, which is why they are not
 * named for one of them: for {@code RECEIVED} and {@code CONSUMED} they bound
 * the period, for {@code EXPIRING} they bound the expiry window, and for
 * {@code STOCK_ON_HAND} only {@code endDate} is read, as the instant to report
 * stock at. Both bounds are half-open, so {@code endDate} is the start of the
 * day after the one the user picked.
 */
public class InventoryReportRequest {

    private final String reportType;
    private final String exportFormat;
    private final Timestamp startDate;
    private final Timestamp endDate;
    private final boolean includeInactive;
    private final boolean includeExpired;
    private final List<String> tags;

    public InventoryReportRequest(String reportType, String exportFormat, Timestamp startDate, Timestamp endDate,
            boolean includeInactive, boolean includeExpired, List<String> tags) {
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.includeInactive = includeInactive;
        this.includeExpired = includeExpired;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public String getReportType() {
        return reportType;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public boolean isIncludeInactive() {
        return includeInactive;
    }

    public boolean isIncludeExpired() {
        return includeExpired;
    }

    /**
     * Item tags to restrict the report to. Empty means no tag filter. An item
     * matches when it carries <i>any</i> of them, the same rule the board's filter
     * uses — requiring every one would make a second selection an intersection.
     */
    public List<String> getTags() {
        return tags;
    }
}
