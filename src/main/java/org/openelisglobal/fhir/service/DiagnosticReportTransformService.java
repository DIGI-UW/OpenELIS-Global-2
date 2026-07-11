package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openelisglobal.analysis.valueholder.Analysis;

public interface DiagnosticReportTransformService {

    DiagnosticReport transformResultToDiagnosticReport(Analysis analysis);

}
