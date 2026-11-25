package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.fhir.providers.DeviceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class DeviceFacadeTest extends BaseWebContextSensitiveTest {

    /** analyzer id=1 (Cobas 6800) in facade-device.xml. */
    private static final String COBAS_UUID = "2d335c87-1def-42e9-a610-2748b9872a1c";
    /** analyzer id=2 (ABL800 FLEX) in facade-device.xml. */

    private static final String UNKNOWN_UUID = "2d335c87-1def-42e9-a610-2748b9872a8c";

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DeviceProvider deviceProvider;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/facade-device.xml");

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(deviceProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readDevice_shouldReturnSuccess() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Device/" + COBAS_UUID));

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Device", jsonResponse.get("resourceType").asText());
        assertEquals(COBAS_UUID, jsonResponse.get("id").asText());
        assertFalse(jsonResponse.has("serialNumber"));
        assertEquals("bridge-cobas-6800", identifierValue(jsonResponse, "/analyzer_bridge_connection"));
    }

    @Test
    public void readsDoNotChangeAnalyzerLastUpdated() throws Exception {
        String before = String.valueOf(analyzerByUuid(COBAS_UUID).getLastupdated());

        assertEquals(200, serve(buildFhirRequest("GET", "/Device")).getStatus());
        assertEquals(200, serve(buildFhirRequest("GET", "/Device/" + COBAS_UUID)).getStatus());

        assertEquals(before, String.valueOf(analyzerByUuid(COBAS_UUID).getLastupdated()));
    }

    @Test
    public void readDevice_withInvalidFhirId_shouldReturn400() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Device/00000000-000000-0000000-000000"));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void readDevice_withUnknownFhirId_shouldReturn404() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Device/" + UNKNOWN_UUID));

        assertEquals(404, response.getStatus());
    }

    private String identifierValue(JsonNode resource, String systemSuffix) {
        for (JsonNode identifier : resource.path("identifier")) {
            if (identifier.path("system").asText().endsWith(systemSuffix)) {
                return identifier.path("value").asText();
            }
        }
        return null;
    }

    private Analyzer analyzerByUuid(String uuid) {
        return analyzerService.getAllMatching("fhirUuid", UUID.fromString(uuid)).getFirst();
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
