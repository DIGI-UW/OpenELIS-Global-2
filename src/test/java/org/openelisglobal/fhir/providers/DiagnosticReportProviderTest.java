package org.openelisglobal.fhir.providers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportProviderTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private FhirTransformService fhirTransformService;

    @InjectMocks
    private DiagnosticReportProvider diagnosticReportProvider;

    @Test(expected = InternalErrorException.class)
    public void readDiagnosticReport_whenDuplicateUuidMatches_shouldReturn500() throws Exception {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
        IdType id = new IdType("DiagnosticReport", uuid);

        Analysis first = new Analysis();
        Analysis second = new Analysis();
        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Arrays.asList(first, second));

        diagnosticReportProvider.readDiagnosticReport(id);
    }

    @Test(expected = InternalErrorException.class)
    public void readDiagnosticReport_whenTransformThrows_shouldReturn500() throws Exception {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
        IdType id = new IdType("DiagnosticReport", uuid);

        Analysis analysis = new Analysis();
        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Collections.singletonList(analysis));
        when(fhirTransformService.transformResultToDiagnosticReport(eq(analysis)))
                .thenThrow(new FhirTransformationException("transform failed"));

        diagnosticReportProvider.readDiagnosticReport(id);
    }

    @Test
    public void readDiagnosticReport_whenSingleMatchAndTransformSuccess_shouldReturnReport() throws Exception {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
        IdType id = new IdType("DiagnosticReport", uuid);

        Analysis analysis = new Analysis();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(uuid);

        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Collections.singletonList(analysis));
        when(fhirTransformService.transformResultToDiagnosticReport(eq(analysis))).thenReturn(report);

        DiagnosticReport result = diagnosticReportProvider.readDiagnosticReport(id);

        org.junit.Assert.assertEquals(uuid, result.getIdElement().getIdPart());
    }
}
