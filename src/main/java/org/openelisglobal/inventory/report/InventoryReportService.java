package org.openelisglobal.inventory.report;

public interface InventoryReportService {

    /**
     * @throws org.openelisglobal.common.exception.LocalizedValidationException for
     *                                                                          an
     *                                                                          unknown
     *                                                                          reportType,
     *                                                                          or a
     *                                                                          date-range
     *                                                                          report
     *                                                                          missing
     *                                                                          its
     *                                                                          dates
     */
    ReportTable generateReport(InventoryReportRequest request);
}
