package org.openelisglobal.biorepository.service;

import java.io.IOException;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;

/**
 * Service interface for exporting biorepository data in multiple formats.
 *
 * Provides methods to export dashboard metrics and audit trail data as CSV,
 * Excel, JSON, or PDF.
 */
public interface BiorepositoryExportService {

    /**
     * Export dashboard metrics to CSV format.
     *
     * @return CSV file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportDashboardToCSV() throws IOException;

    /**
     * Export dashboard metrics to Excel format.
     *
     * @return Excel file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportDashboardToExcel() throws IOException;

    /**
     * Export dashboard metrics to JSON format.
     *
     * @return JSON file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportDashboardToJSON() throws IOException;

    /**
     * Export dashboard metrics to PDF format.
     *
     * @return PDF file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportDashboardToPDF() throws IOException;

    /**
     * Export audit trail to CSV format.
     *
     * @param sampleExternalId sample barcode filter (optional)
     * @param action           custody action filter (optional)
     * @param custodianId      custodian user ID filter (optional)
     * @param startDate        start timestamp filter (optional)
     * @param endDate          end timestamp filter (optional)
     * @return CSV file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportAuditTrailToCSV(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate) throws IOException;

    /**
     * Export audit trail to Excel format.
     *
     * @param sampleExternalId sample barcode filter (optional)
     * @param action           custody action filter (optional)
     * @param custodianId      custodian user ID filter (optional)
     * @param startDate        start timestamp filter (optional)
     * @param endDate          end timestamp filter (optional)
     * @return Excel file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportAuditTrailToExcel(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate) throws IOException;

    /**
     * Export audit trail to JSON format.
     *
     * @param sampleExternalId sample barcode filter (optional)
     * @param action           custody action filter (optional)
     * @param custodianId      custodian user ID filter (optional)
     * @param startDate        start timestamp filter (optional)
     * @param endDate          end timestamp filter (optional)
     * @return JSON file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportAuditTrailToJSON(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate) throws IOException;

    /**
     * Export audit trail to PDF format.
     *
     * @param sampleExternalId sample barcode filter (optional)
     * @param action           custody action filter (optional)
     * @param custodianId      custodian user ID filter (optional)
     * @param startDate        start timestamp filter (optional)
     * @param endDate          end timestamp filter (optional)
     * @return PDF file content as byte array
     * @throws IOException if export fails
     */
    byte[] exportAuditTrailToPDF(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate) throws IOException;
}
