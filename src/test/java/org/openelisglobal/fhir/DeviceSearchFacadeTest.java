package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.DeviceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Device search answered from the analyzer table (facade-device.xml: Cobas
 * 6800, ABL800 FLEX, Sysmex XN-1000).
 */
public class DeviceSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String COBAS_UUID = "2d335c87-1def-42e9-a610-2748b9872a1c";
    private static final String ABL800_UUID = "2d335c87-1def-42e9-a610-2748b9872a2c";
    private static final String SYSMEX_UUID = "2d335c87-1def-42e9-a610-2748b9872a3c";

    @Autowired
    private DeviceProvider deviceProvider;

    @Autowired
    private javax.sql.DataSource dataSource;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-device.xml");
        resyncSequence("clinlims.analyzer_seq", "clinlims.analyzer");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(deviceProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_byIdAndIdentifiers() throws Exception {
        assertEquals(3, search().get("total").asInt());
        assertEquals(List.of(ABL800_UUID), idsOf(search("_id", ABL800_UUID)));
        assertEquals(List.of(COBAS_UUID), idsOf(search("identifier", "COBAS6800-001")));
        assertEquals(List.of(SYSMEX_UUID),
                idsOf(search("identifier", "http://openelis-global.org/analyzer_machineId|SYSMEX-XN1000-45")));
        assertEquals(List.of(ABL800_UUID),
                idsOf(search("identifier", "http://openelis-global.org/analyzer_uuid|" + ABL800_UUID)));
        assertEquals(0, search("identifier", "http://example.org/other|COBAS6800-001").get("total").asInt());
    }

    @Test
    public void search_byNameTypeAndStatus() throws Exception {
        assertEquals(List.of(COBAS_UUID), idsOf(search("device-name", "cobas")));
        assertEquals(List.of(ABL800_UUID), idsOf(search("type", "CHEMISTRY")));
        assertEquals(0, search("type", "SEROLOGY").get("total").asInt());

        new JdbcTemplate(dataSource).update("UPDATE clinlims.analyzer SET status = 'INACTIVE' WHERE id = 3");
        assertEquals(List.of(SYSMEX_UUID), idsOf(search("status", "inactive")));
        assertEquals(2, search("status", "active").get("total").asInt());
        assertEquals(0, search("status", "entered-in-error").get("total").asInt());
    }

    @Test
    public void search_returnsTransformedDevices() throws Exception {
        JsonNode resource = search("_id", COBAS_UUID).get("entry").get(0).get("resource");
        assertEquals("Device", resource.get("resourceType").asText());
        assertEquals("COBAS6800-001", resource.get("serialNumber").asText());
        assertTrue(resource.get("deviceName").size() >= 1);
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Device"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Device");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private static List<String> idsOf(JsonNode bundle) {
        List<String> ids = new ArrayList<>();
        JsonNode entries = bundle.get("entry");
        if (entries == null) {
            return ids;
        }
        for (JsonNode entry : entries) {
            ids.add(entry.get("resource").get("id").asText());
        }
        return ids;
    }

    @Test
    public void search_byLastUpdated_andExposesMetaLastUpdated() throws Exception {
        assertEquals(List.of(SYSMEX_UUID), idsOf(search("_lastUpdated", "ge2023-01-01T13:00:00")));
        assertEquals(List.of(COBAS_UUID), idsOf(search("_lastUpdated", "lt2023-01-01T12:30:00")));
        assertEquals(3, search("_lastUpdated", "2023-01-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2023-01-01").get("total").asInt());
        JsonNode resource = search("_id", ABL800_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2023-01-01"));
    }
}
