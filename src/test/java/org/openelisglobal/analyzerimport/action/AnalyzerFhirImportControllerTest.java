package org.openelisglobal.analyzerimport.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportException;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportSummary;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Request parsing and response mapping only; this standalone suite does not
 * claim authentication or authorization coverage.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFhirImportControllerTest {

    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-known-test.fhir.json");

    @Mock
    private AnalyzerNormalizedResultImportService importService;

    private MockMvc mockMvc;
    private UserSessionData actor;

    @Before
    public void setUp() {
        AnalyzerFhirImportController controller = new AnalyzerFhirImportController();
        ReflectionTestUtils.setField(controller, "importService", importService);
        ReflectionTestUtils.setField(controller, "fhirContext", FhirContext.forR4());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        actor = new UserSessionData();
        actor.setSytemUserId(47);
    }

    @Test
    public void normalizedBundleDelegatesToTheOwningService() throws Exception {
        // Parsing creates a new Bundle; its contents are checked after delegation below.
        when(importService.importBundle(any(Bundle.class), eq("47")))
                .thenReturn(new AnalyzerNormalizedResultImportSummary("42", 1, 0, 0));

        mockMvc.perform(post("/analyzer/fhir").requestAttr(IActionConstants.USER_SESSION_DATA, actor).contentType("application/fhir+json")
                .content(Files.readString(FIXTURE))).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.analyzerId").value("42"))
                .andExpect(jsonPath("$.resultsStaged").value(1)).andExpect(jsonPath("$.resultsHeld").value(0));

        ArgumentCaptor<Bundle> received = ArgumentCaptor.forClass(Bundle.class);
        verify(importService).importBundle(received.capture(), eq("47"));
        Bundle bundle = received.getValue();
        assertEquals("known-astm-001", bundle.getIdentifier().getValue());
        org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) bundle.getEntry().get(2).getResource();
        assertEquals("WBC", observation.getCode().getCodingFirstRep().getCode());
        assertEquals("7.5", observation.getValueQuantity().getValue().toPlainString());
    }

    @Test
    public void unknownConnectionReturnsVisibleUnprocessableContractError() throws Exception {
        // Any parsed Bundle triggers this domain error; this test covers HTTP error translation.
        when(importService.importBundle(any(Bundle.class), eq("47")))
                .thenThrow(new AnalyzerNormalizedResultImportException(
                        "analyzer.fhirImport.error.unknownConnection", "Connection is not configured"));

        mockMvc.perform(post("/analyzer/fhir").requestAttr(IActionConstants.USER_SESSION_DATA, actor).contentType(MediaType.APPLICATION_JSON)
                .content(Files.readString(FIXTURE))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorKey").value("analyzer.fhirImport.error.unknownConnection"));
    }

    @Test
    public void emptyPayloadDoesNotCallTheDomainService() throws Exception {
        mockMvc.perform(post("/analyzer/fhir").contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest());
        verifyZeroInteractions(importService);
    }

    @Test
    public void malformedFhirReturnsBadRequestWithoutCallingTheDomainService() throws Exception {
        mockMvc.perform(post("/analyzer/fhir").requestAttr(IActionConstants.USER_SESSION_DATA, actor)
                .contentType(MediaType.APPLICATION_JSON).content("{not-fhir")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorKey").value("analyzer.fhirImport.error.invalidPayload"));

        verifyZeroInteractions(importService);
    }
}
