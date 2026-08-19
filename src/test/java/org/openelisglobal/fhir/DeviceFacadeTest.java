package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
import org.openelisglobal.fhir.providers.DeviceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class DeviceFacadeTest extends BaseWebContextSensitiveTest {
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

        String fhirUuid = "2d335c87-1def-42e9-a610-2748b9872a1c";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Device/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Device", jsonResponse.get("resourceType").asText());

    }

    @Test
    public void readDevice_shouldWithInvalidFhirIdShouldReturn400() throws Exception {

        String fhirUuid = "00000000-000000-0000000-000000";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Device/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());

    }

    @Test
    public void readDevice_shouldWithValidFhirIdWithoutItemShouldReturn404() throws Exception {

        String fhirUuid = "d335c87-1def-42e9-a610-2748b9872a8c";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Device/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

    }

    @Test
    public void deleteDevice_shouldSetAnalyzerInactive() throws Exception {

        String fhirUuid = "2d335c87-1def-42e9-a610-2748b9872a1c";

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Device/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = analyzerService
                .getAllMatching("fhirUuid", UUID.fromString(fhirUuid)).getFirst();
        assertFalse(analyzer.isActive());

    }

    @Test
    public void createDevice_shouldPersistAnalyzerAndReturnSuccess() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer", "analyzer_test_map" });

        String deviceJson = """
                {
                  "resourceType": "Device",
                  "identifier": [{
                    "system": "test/system",
                    "value": "NEW-DEVICE-001"
                  }],
                  "serialNumber": "NEW-DEVICE-001",
                  "deviceName": [{
                    "name": "Test Device",
                    "type": "user-friendly-name"
                  }],
                  "type": {
                    "text": "Cobas 6800 Type"
                  }
                }
                """;

        MockHttpServletRequest request = buildFhirRequest("POST", "/Device");
        request.setContent(deviceJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Device", jsonResponse.get("resourceType").asText());
        assertNotNull(jsonResponse.get("id"));
        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = analyzerService.getAll().getFirst();
        assertEquals("Test Device", analyzer.getName());
    }

    @Test
    public void updateDevice_shouldModifyAnalyzer() throws Exception {

        String fhirUuid = "2d335c87-1def-42e9-a610-2748b9872a1c";

        String updateJson = """
                {
                  "resourceType": "Device",
                  "id": "2d335c87-1def-42e9-a610-2748b9872a1c",
                  "serialNumber": "UPDATED-SERIAL-123",
                  "deviceName": [{
                    "name": "Updated Device",
                    "type": "user-friendly-name"
                  }],
                  "type": {
                    "text": "MOLECULAR"
                  }
                }
                """;

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Device/" + fhirUuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = analyzerService
                .getAllMatching("fhirUuid", UUID.fromString(fhirUuid)).getFirst();
        assertEquals("Updated Device", analyzer.getName());

    }

}
