package org.openelisglobal.inventory.report;

import java.sql.Timestamp;

/**
 * Parameters for an inventory report, mirroring the fields
 * {@code InventoryReports.jsx} collects: report type and export format are
 * required; the rest are optional filters only some report types honor (see
 * each {@code InventoryReportServiceImpl.build*Report} method for which).
 */
public class InventoryReportRequest {

    private final String reportType;
    private final String exportFormat;
    private final Timestamp startDate;
    private final Timestamp endDate;
    private final boolean includeInactive;
    private final boolean includeExpired;
    private final boolean groupByType;
    private final boolean groupByLocation;

    public InventoryReportRequest(String reportType, String exportFormat, Timestamp startDate, Timestamp endDate,
            boolean includeInactive, boolean includeExpired, boolean groupByType, boolean groupByLocation) {
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.includeInactive = includeInactive;
        this.includeExpired = includeExpired;
        this.groupByType = groupByType;
        this.groupByLocation = groupByLocation;
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

    public boolean isGroupByType() {
        return groupByType;
    }

    public boolean isGroupByLocation() {
        return groupByLocation;
    }
}
