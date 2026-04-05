package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@Rollback
public class ObservationFacadeTest extends BaseWebContextSensitiveTest {

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationProvider observationProvider;

    @Autowired
    private DataSource dataSource;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
        // Reset result_seq past existing test data IDs to prevent PK conflicts
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT setval('clinlims.result_seq', 1000, true)");
        }
        servletContext = new MockServletContext();
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(observationProvider));
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void readObservation_shouldReturnSuccess() throws Exception {
        String fhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation/" + fhirUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Observation", jsonResponse.get("resourceType").asText());
        assertEquals("final", jsonResponse.get("status").asText());
        assertNotNull(jsonResponse.get("valueQuantity"));
    }

    @Test
    public void readObservation_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void createObservation_shouldReturn201() throws Exception {

        String analysisUuid = "dddddddd-1111-1111-1111-000000000051";

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");
        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "preliminary",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "basedOn": [{ "reference": "ServiceRequest/%s" }],
                  "valueQuantity": {
                    "value": 12.5,
                    "unit": "g/dL"
                  }
                }
                """.formatted(analysisUuid);
        request.setContent(createJson.getBytes());
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        request.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Observation", jsonResponse.get("resourceType").asText());
        assertNotNull(jsonResponse.get("id"));
    }

    @Test
    public void createObservation_withoutBasedOn_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");
        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "preliminary",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "valueQuantity": {
                    "value": 12.5,
                    "unit": "g/dL"
                  }
                }
                """;
        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void createObservation_withFinalizedAnalysis_shouldReturn400() throws Exception {

        String finalizedAnalysisUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");
        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "preliminary",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "basedOn": [{ "reference": "ServiceRequest/%s" }],
                  "valueQuantity": {
                    "value": 12.5,
                    "unit": "g/dL"
                  }
                }
                """.formatted(finalizedAnalysisUuid);
        request.setContent(createJson.getBytes());
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        request.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void createObservation_withDuplicateAnalysis_shouldReturn400() throws Exception {
        String analysisUuid = "c3d4e5f6-3456-7890-cdef-012345678901";
        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "preliminary",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "basedOn": [{ "reference": "ServiceRequest/%s" }],
                  "valueQuantity": { "value": 12.5, "unit": "g/dL" }
                }
                """.formatted(analysisUuid);

        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);

        MockHttpServletRequest request1 = buildFhirRequest("POST", "/Observation");
        request1.setContent(createJson.getBytes());
        request1.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        fhirServlet.service(request1, response1);
        assertEquals(201, response1.getStatus());

        MockHttpServletRequest request2 = buildFhirRequest("POST", "/Observation");
        request2.setContent(createJson.getBytes());
        request2.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        fhirServlet.service(request2, response2);
        assertEquals(400, response2.getStatus());
    }

    @Test
    public void updateObservation_shouldUpdateValue() throws Exception {
        String fhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + fhirUuid);
        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/dL"
                  }
                }
                """.formatted(fhirUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Observation", jsonResponse.get("resourceType").asText());

        Result updatedResult = resultService.getResultByFhirUuid(fhirUuid);
        assertEquals("99.0", updatedResult.getValue());
    }

    @Test
    public void updateObservation_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + nonExistentUuid);
        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{ "system": "http://loinc.org", "code": "718-7" }]
                  },
                  "valueQuantity": { "value": 99.0, "unit": "g/dL" }
                }
                """.formatted(nonExistentUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void deleteObservation_shouldReturn204() throws Exception {
        String fhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Observation/" + fhirUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        Result deletedResult = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result should still exist after soft-delete", deletedResult);
    }

    @Test
    public void deleteObservation_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Observation/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void searchObservation_endpointExists_shouldNotReturn404() throws Exception {
        String patientUuid = "550e8400-e29b-41d4-a716-446655440001";
        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");
        request.setQueryString("patient=" + patientUuid);
        request.addParameter("patient", patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertNotNull(response);
        assertTrue(response.getStatus() != 404);
    }
}