package org.openelisglobal.report.service;

import org.openelisglobal.report.ReportingData;

/**
 * Service for generating patient-centered report data.
 *
 * <p>
 * This service encapsulates all business logic for compiling patient report
 * rows and columns so that controllers remain thin and comply with the layered
 * architecture.
 */
public interface PatientReportService {

    /**
     * Build a patient results report for the given patient id.
     *
     * @param patientId the patient identifier
     * @param sysUserId the authenticated system user id for audit/security
     * @return reporting data for the patient, or {@code null} if the patient does
     *         not exist
     */
    ReportingData buildPatientResultsReport(String patientId, String sysUserId);
}
