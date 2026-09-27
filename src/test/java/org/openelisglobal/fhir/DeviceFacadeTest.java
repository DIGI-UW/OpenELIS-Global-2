package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    private static final String ABL800_UUID = "2d335c87-1def-42e9-a610-2748b9872a2c";
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
        // The fixture seeds analyzer ids 1-3 without advancing analyzer_seq; resync so
        // a created Device does not collide on analyzer_pk.
        resyncSequence("clinlims.analyzer_seq", "clinlims.analyzer");

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

    @Test
    public void deleteDevice_shouldDeactivateAnalyzerAndReportInactive() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Device/" + COBAS_UUID));

        assertEquals(204, response.getStatus());

        Analyzer analyzer = analyzerByUuid(COBAS_UUID);
        assertFalse(analyzer.isActive());
        assertEquals(Analyzer.AnalyzerStatus.INACTIVE, analyzer.getStatus());

        JsonNode device = objectMapper
                .readTree(serve(buildFhirRequest("GET", "/Device/" + COBAS_UUID)).getContentAsString());
        assertEquals("deleted device must not read back as active", "inactive", device.get("status").asText());
    }

    @Test
    public void deleteDevice_withUnknownFhirId_shouldReturn404() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Device/" + UNKNOWN_UUID));

        assertEquals(404, response.getStatus());
    }

    @Test
    public void createDevice_shouldPersistAnalyzerAndReturnSuccess() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer" });

        String deviceJson = """
                {
                  "resourceType": "Device",
                  "identifier": [{
                    "system": "http://openelis.org/fhir/analyzer_bridge_connection",
                    "value": "bridge-new-device-001"
                  }],
                  "deviceName": [{
                    "name": "Test Device",
                    "type": "user-friendly-name"
                  }]
                }
                """;

        MockHttpServletResponse response = serve(post(deviceJson));

        assertEquals(201, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Device", jsonResponse.get("resourceType").asText());
        assertNotNull(jsonResponse.get("id"));
        Analyzer analyzer = analyzerService.getAll().getFirst();
        assertEquals("Test Device", analyzer.getName());
        assertEquals("bridge-new-device-001", analyzer.getBridgeConnectionId());
    }

    @Test
    public void createDevice_withClientSuppliedExistingId_shouldCreateNewAnalyzerNotUpdate() throws Exception {

        int before = analyzerService.getAll().size();

        String deviceJson = """
                {
                  "resourceType": "Device",
                  "id": "%s",
                  "serialNumber": "IMPOSTOR-001",
                  "deviceName": [{
                    "name": "Impostor Device",
                    "type": "user-friendly-name"
                  }]
                }
                """.formatted(COBAS_UUID);

        MockHttpServletResponse response = serve(post(deviceJson));

        assertEquals(201, response.getStatus());

        String newId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
        assertNotEquals("server must assign its own id on create", COBAS_UUID, newId);
        assertEquals(before + 1, analyzerService.getAll().size());
        assertEquals("existing analyzer must be untouched", "Cobas 6800", analyzerByUuid(COBAS_UUID).getName());
        assertEquals("Impostor Device", analyzerByUuid(newId).getName());
    }

    @Test
    public void updateDevice_shouldModifyAnalyzer() throws Exception {

        String updateJson = """
                {
                  "resourceType": "Device",
                  "id": "%s",
                  "identifier": [{
                    "system": "http://openelis.org/fhir/analyzer_bridge_connection",
                    "value": "bridge-updated-123"
                  }],
                  "deviceName": [{
                    "name": "Updated Device",
                    "type": "user-friendly-name"
                  }]
                }
                """.formatted(COBAS_UUID);

        MockHttpServletResponse response = serve(put(COBAS_UUID, updateJson));

        assertEquals(200, response.getStatus());

        Analyzer analyzer = analyzerByUuid(COBAS_UUID);
        assertEquals("Updated Device", analyzer.getName());
        assertEquals("bridge-updated-123", analyzer.getBridgeConnectionId());
    }

    /**
     * HAPI enforces the FHIR PUT contract before the provider runs: the body must
     * carry an id matching the URL. Nothing may be created or changed on the way to
     * that 400.
     */
    @Test
    public void updateDevice_withoutBodyId_shouldReturn400AndChangeNothing() throws Exception {

        String updateJson = """
                {
                  "resourceType": "Device",
                  "serialNumber": "ABL800-999",
                  "deviceName": [{
                    "name": "ABL800 renamed",
                    "type": "user-friendly-name"
                  }]
                }
                """;

        int before = analyzerService.getAll().size();

        MockHttpServletResponse response = serve(put(ABL800_UUID, updateJson));

        assertEquals(400, response.getStatus());
        assertEquals(before, analyzerService.getAll().size());
        assertEquals("ABL800 FLEX", analyzerByUuid(ABL800_UUID).getName());
    }

    @Test
    public void updateDevice_withMismatchedBodyId_shouldReturn400() throws Exception {

        String updateJson = """
                {
                  "resourceType": "Device",
                  "id": "%s",
                  "deviceName": [{
                    "name": "Wrong Target",
                    "type": "user-friendly-name"
                  }]
                }
                """.formatted(ABL800_UUID);

        MockHttpServletResponse response = serve(put(COBAS_UUID, updateJson));

        assertEquals(400, response.getStatus());
        assertEquals("Cobas 6800", analyzerByUuid(COBAS_UUID).getName());
        assertEquals("ABL800 FLEX", analyzerByUuid(ABL800_UUID).getName());
    }

    @Test
    public void updateDevice_withUnknownId_shouldReturn404() throws Exception {

        String updateJson = """
                {
                  "resourceType": "Device",
                  "id": "%s",
                  "deviceName": [{
                    "name": "Ghost",
                    "type": "user-friendly-name"
                  }]
                }
                """.formatted(UNKNOWN_UUID);

        int before = analyzerService.getAll().size();

        MockHttpServletResponse response = serve(put(UNKNOWN_UUID, updateJson));

        assertEquals(404, response.getStatus());
        assertEquals("update of an unknown id must not create an analyzer", before, analyzerService.getAll().size());
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

    private MockHttpServletRequest post(String body) {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Device");
        request.setContent(body.getBytes());
        return request;
    }

    private MockHttpServletRequest put(String uuid, String body) {
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Device/" + uuid);
        request.setContent(body.getBytes());
        return request;
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
