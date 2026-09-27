package org.openelisglobal.analyzerimport.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportException;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportSummary;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerFhirImportControllerTest extends BaseWebContextSensitiveTest {

    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-known-test.fhir.json");

    @Mock
    private AnalyzerNormalizedResultImportService importService;

    private AnalyzerFhirImportController controller;
    private Object originalImportService;
    private Object originalFhirContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityContextHolder.clearContext();
        MockitoAnnotations.initMocks(this);
        controller = webApplicationContext.getBean(AnalyzerFhirImportController.class);
        originalImportService = ReflectionTestUtils.getField(controller, "importService");
        originalFhirContext = ReflectionTestUtils.getField(controller, "fhirContext");
        ReflectionTestUtils.setField(controller, "importService", importService);
        ReflectionTestUtils.setField(controller, "fhirContext", FhirContext.forR4());
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(controller, "importService", originalImportService);
        ReflectionTestUtils.setField(controller, "fhirContext", originalFhirContext);
    }

    @Test
    public void normalizedBundleDelegatesToTheOwningService() throws Exception {
        when(importService.importBundle(any(Bundle.class), eq("1")))
                .thenReturn(new AnalyzerNormalizedResultImportSummary("42", 1, 0, 0));

        mockMvc.perform(post("/analyzer/fhir").contentType("application/fhir+json")
                .content(Files.readString(FIXTURE))).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.analyzerId").value("42"))
                .andExpect(jsonPath("$.resultsStaged").value(1)).andExpect(jsonPath("$.resultsHeld").value(0));

        verify(importService).importBundle(any(Bundle.class), eq("1"));
    }

    @Test
    public void unknownConnectionReturnsVisibleUnprocessableContractError() throws Exception {
        when(importService.importBundle(any(Bundle.class), eq("1")))
                .thenThrow(new AnalyzerNormalizedResultImportException(
                        "analyzer.fhirImport.error.unknownConnection", "Connection is not configured"));

        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON)
                .content(Files.readString(FIXTURE))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorKey").value("analyzer.fhirImport.error.unknownConnection"));
    }

    @Test
    public void malformedFhirReturnsBadRequestWithoutCallingTheDomainService() throws Exception {
        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content("{not-fhir"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorKey").value("analyzer.fhirImport.error.invalidPayload"));

        verify(importService, never()).importBundle(any(Bundle.class), eq("1"));
    }
}
