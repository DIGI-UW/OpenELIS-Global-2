package org.openelisglobal.analyzerimport.service;

import org.hl7.fhir.r4.model.Bundle;

public interface AnalyzerNormalizedResultImportService {

    AnalyzerNormalizedResultImportSummary importBundle(Bundle bundle, String actor);

    AnalyzerNormalizedResultImportSummary reprocessHeldResult(String analyzerId, String resultId, String actor);
}
