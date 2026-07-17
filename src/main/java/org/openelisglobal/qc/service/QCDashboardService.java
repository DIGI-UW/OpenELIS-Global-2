package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.dto.InstrumentQCStatus;
import org.openelisglobal.qc.dto.QCDashboardSummary;

/**
 * Service interface for QC Dashboard (T120).
 *
 * Provides compliance status and dashboard data for the QC monitoring UI.
 */
public interface QCDashboardService {

    /**
     * Get compliance status for all instruments using default 1-month window.
     */
    List<InstrumentQCStatus> getAllInstrumentComplianceStatus();

    /**
     * Get compliance status for all instruments within a date range.
     */
    List<InstrumentQCStatus> getAllInstrumentComplianceStatus(Timestamp startDate, Timestamp endDate);

    /**
     * Get compliance status for a specific instrument using default 1-month window.
     */
    InstrumentQCStatus getInstrumentComplianceStatus(String instrumentId);

    /**
     * Get compliance status for a specific instrument within a date range.
     */
    InstrumentQCStatus getInstrumentComplianceStatus(String instrumentId, Timestamp startDate, Timestamp endDate);

    /**
     * Get dashboard summary using default 1-month window.
     */
    QCDashboardSummary getDashboardSummary();

    /**
     * Get dashboard summary within a date range.
     */
    QCDashboardSummary getDashboardSummary(Timestamp startDate, Timestamp endDate);
}