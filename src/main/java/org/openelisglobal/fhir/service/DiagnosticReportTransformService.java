package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openelisglobal.analysis.valueholder.Analysis;

/**
 * OpenELIS Analysis (finalized) to FHIR DiagnosticReport.
 */
public interface DiagnosticReportTransformService {

    DiagnosticReport transformResultToDiagnosticReport(String analysisId);

    DiagnosticReport transformResultToDiagnosticReport(Analysis analysis);
}
