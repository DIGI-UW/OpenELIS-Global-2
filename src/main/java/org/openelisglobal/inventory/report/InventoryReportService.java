package org.openelisglobal.inventory.report;

public interface InventoryReportService {

    /**
     * Builds the requested report's tabular data. Throws
     * {@link org.openelisglobal.common.exception.LIMSRuntimeException} (via
     * {@link org.openelisglobal.common.exception.LocalizedValidationException}) for
     * an unknown reportType or a date-range report missing startDate/endDate.
     */
    ReportTable generateReport(InventoryReportRequest request);
}
