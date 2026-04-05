package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
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
}
