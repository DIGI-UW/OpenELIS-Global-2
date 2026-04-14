package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.ServiceRequestProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class ServiceRequestFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS1_FHIRID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private ServiceRequestProvider serviceRequestProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(serviceRequestProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/facade-servicerequest.xml");

    }

    @Test
    public void read_shouldReturnServiceRequestByFhirUUID() throws ServletException, IOException {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/" + ANALYSIS1_FHIRID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("ServiceRequest", jsonResponse.get("resourceType").asText());

    }

    @Test
    public void readPractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void readServiceRequest_withInvalidUuid_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void searchServiceRequest_endpointExists_shouldNotReturn404() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest");
        request.setQueryString("subject=Patient/550e8400-e29b-41d4-a716-446655440001");
        request.addParameter("subject", "Patient/550e8400-e29b-41d4-a716-446655440001");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertNotNull(response);
        org.junit.Assert.assertTrue(response.getStatus() != 404);
    }
}
