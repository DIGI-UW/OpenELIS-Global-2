package org.openelisglobal.inventory.report;

import org.openelisglobal.common.exception.LocalizedValidationException;

public interface InventoryReportService {

    /**
     * @throws LocalizedValidationException for an unknown reportType, or a
     *                                      date-range report missing its dates
     */
    ReportTable generateReport(InventoryReportRequest request);
}
