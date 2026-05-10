package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.fhir.providers.DiagnosticReportProvider;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class DiagnosticReportFacadeTest extends BaseWebContextSensitiveTest {

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;
    @Autowired
    private PanelService panelService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private DiagnosticReportProvider diagnosticReportProvider;

    private MockServletContext servletContext;

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
    public void readDiagnosticReport_shouldReturnSuccess() throws Exception {

        String fhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("DiagnosticReport", jsonResponse.get("resourceType").asText());
        assertEquals("final", jsonResponse.get("status").asText());
        assertNotNull(jsonResponse.get("result"));
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
    public void readDiagnosticReport_withInvalidUuid_shouldReturn400() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport/not-a-uuid");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void searchDiagnosticReport_endpointExists_shouldNotReturn404() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport");
        request.setQueryString("subject=Patient/550e8400-e29b-41d4-a716-446655440001&status=final");
        request.addParameter("subject", "Patient/550e8400-e29b-41d4-a716-446655440001");
        request.addParameter("status", "final");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertNotNull(response);
        org.junit.Assert.assertTrue(response.getStatus() != 404);
    }

    @Test
    public void createDiagnosticReport_shouldReturnSuccess() throws Exception {
        Analysis analysis = analysisService.getAnalysisById("2");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationService.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);
        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);

        String payload = """
                {
                  "resourceType": "DiagnosticReport",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Test Report"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/550e8400-e29b-41d4-a716-446655440004"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02"
                  }],
                  "result": [{
                    "reference": "Observation/550e8400-e29b-41d4-a716-446655440004"
                  }]
                }
                """;

        MockHttpServletRequest request = buildFhirRequest("POST", "/DiagnosticReport");

        request.setContentType("application/fhir+json");
        request.setContent(payload.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        // Usually HAPI returns 201 for create
        assertEquals(201, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());

        assertEquals("DiagnosticReport", json.get("resourceType").asText());
        assertEquals("final", json.get("status").asText());
        assertNotNull(json.get("id"));
    }

    @Test
    public void updateDiagnosticReport_shouldReturnSuccess() throws Exception {

        String fhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
        Analysis analysis = analysisService.getAnalysisById("2");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationService.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);
        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);

        String payload = """
                {
                  "resourceType": "DiagnosticReport",
                  "id": "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Updated Report"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/550e8400-e29b-41d4-a716-446655440004"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02"
                  }]
                }
                """;

        MockHttpServletRequest request = buildFhirRequest("PUT", "/DiagnosticReport/" + fhirUuid);

        request.setContentType("application/fhir+json");
        request.setContent(payload.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());

        assertEquals("DiagnosticReport", json.get("resourceType").asText());
        assertEquals("final", json.get("status").asText());
    }

}
