package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.fhir.providers.DiagnosticReportProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class DiagnosticReportFacadeTest extends BaseWebContextSensitiveTest {

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosticReportProvider diagnosticReportProvider;

    @Autowired
    private AnalysisService analysisService;

    private MockServletContext servletContext;

    // analysis id="2" in result-facade.xml: status_id="6" (Finalized),
    // fhir_uuid="f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02"
    private static final String FINALIZED_ANALYSIS_FHIR_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";

    // analysis id="1" in result-facade.xml: status_id="1" (Technical Acceptance)
    private static final String PRELIMINARY_ANALYSIS_FHIR_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(diagnosticReportProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");

        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readDiagnosticReport_finalizedAnalysis_shouldReturnFinalStatus() throws Exception {
        List<Analysis> matches = analysisService.getAllMatching("fhirUuid",
                java.util.UUID.fromString(FINALIZED_ANALYSIS_FHIR_UUID));
        assertFalse("Analysis not found in test data", matches.isEmpty());
        Analysis analysis = matches.get(0);
        assertNotNull("Analysis not found in test data", analysis);

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + FINALIZED_ANALYSIS_FHIR_UUID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("DiagnosticReport", jsonResponse.get("resourceType").asText());
        assertEquals("final", jsonResponse.get("status").asText());
    }

    @Test
    public void readDiagnosticReport_shouldIncludeCorrectSubjectReference() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + FINALIZED_ANALYSIS_FHIR_UUID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        String subjectRef = jsonResponse.get("subject").get("reference").asText();
        // patient id="2" linked via sample_human id="2" -> sample id="2" -> sample_item
        // id="602"
        assertNotNull("Subject reference should be present", subjectRef);
        assertEquals("Patient/550e8400-e29b-41d4-a716-446655440004", subjectRef);
    }

    @Test
    public void readDiagnosticReport_preliminaryAnalysis_shouldReturnPreliminaryStatus() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + PRELIMINARY_ANALYSIS_FHIR_UUID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("DiagnosticReport", jsonResponse.get("resourceType").asText());
        // status_id="1" maps to TechnicalAcceptance → PRELIMINARY
        assertEquals("preliminary", jsonResponse.get("status").asText());
    }

    @Test
    public void readDiagnosticReport_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void readDiagnosticReport_withInvalidUuidFormat_shouldReturn404() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/not-a-valid-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void readDiagnosticReport_shouldContainSpecimenReference() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + FINALIZED_ANALYSIS_FHIR_UUID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        // specimen[0] should reference the sample_item
        // fhir_uuid="68438220-5cef-44c4-9e6f-9f88e6b93271"
        JsonNode specimenArray = jsonResponse.get("specimen");
        assertNotNull("Specimen array should be present", specimenArray);
        assertEquals(1, specimenArray.size());
        String specimenRef = specimenArray.get(0).get("reference").asText();
        assertEquals("Specimen/68438220-5cef-44c4-9e6f-9f88e6b93271", specimenRef);
    }
}
