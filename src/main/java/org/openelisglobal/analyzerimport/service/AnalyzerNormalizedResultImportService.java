package org.openelisglobal.analyzerimport.service;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerNormalizedResultImportService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
    AnalyzerNormalizedResultImportSummary importBundle(Bundle bundle, String actor);
}
