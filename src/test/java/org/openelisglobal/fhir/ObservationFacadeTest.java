package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ObservationFacadeTest extends BaseWebContextSensitiveTest {

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationProvider observationProvider;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
        servletContext = new MockServletContext();
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(observationProvider));
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    private MockHttpServletRequest buildRequest(String method, String pathInfo, String queryString) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("");
        request.setServletPath("/fhir");
        request.setPathInfo(pathInfo);
        request.setRequestURI("/fhir" + pathInfo);
        if (queryString != null) {
            request.setQueryString(queryString);
            for (String param : queryString.split("&")) {
                String[] kv = param.split("=");
                request.addParameter(kv[0], kv[1]);
            }
        }
        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");
        return request;
    }

    @Test
    public void readObservation_shouldReturnSuccess() throws Exception {
        String fhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildRequest("GET", "/Observation/" + fhirUuid, null);
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
        MockHttpServletRequest request = buildRequest("GET", "/Observation/" + nonExistentUuid, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void searchObservation_endpointExists_shouldNotReturn404() throws Exception {
        String patientUuid = "550e8400-e29b-41d4-a716-446655440001";
        MockHttpServletRequest request = buildRequest("GET", "/Observation", "patient=" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // Search forwards to HAPI store
        assertNotNull(response);
        assertTrue(response.getStatus() != 404);
    }
}