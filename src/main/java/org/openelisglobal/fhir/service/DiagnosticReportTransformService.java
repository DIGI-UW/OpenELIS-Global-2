package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.CrossDomainService;

/**
 * OpenELIS Analysis (finalized) to FHIR DiagnosticReport.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface DiagnosticReportTransformService {

    DiagnosticReport transformResultToDiagnosticReport(String analysisId);

    DiagnosticReport transformResultToDiagnosticReport(Analysis analysis);
}
