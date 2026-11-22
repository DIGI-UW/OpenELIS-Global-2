package org.openelisglobal.microbiology.service;

/**
 * Projects reviewed microbiology content into the existing standard Result
 * model. This service owns no report UI or report format; it supplies the
 * Result records consumed by the existing OpenELIS report path.
 */
public interface MicroReportProjectionService {

    MicroReportProjectionResult releasePreliminary(String caseId, String performedBy);

    MicroReportProjectionResult releaseFinal(String caseId, String performedBy);

    MicroReportProjectionResult releaseAmended(String caseId, String performedBy);

    MicroReportProjectionResult preview(String caseId);
}
