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
import org.openelisglobal.fhir.providers.LocationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Location search over the storage hierarchy (facade-location.xml: two rooms,
 * two freezers in room 1, two shelves, two racks, two boxes).
 */
public class LocationSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ROOM_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
    private static final String ROOM_2_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d4";
    private static final String DEVICE_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d6";
    private static final String DEVICE_2_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d7";
    private static final String SHELF_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6a3";
    private static final String BOX_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b1a3";

    @Autowired
    private LocationProvider locationProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-location.xml");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(locationProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_withoutParameters_returnsEveryLevel() throws Exception {
        JsonNode bundle = search();
        assertEquals(10, bundle.get("total").asInt());
        assertTrue(idsOf(bundle).containsAll(List.of(ROOM_FHIRID, DEVICE_FHIRID, SHELF_FHIRID, BOX_FHIRID)));
    }

    @Test
    public void search_byIdIdentifierAndName() throws Exception {
        assertEquals(List.of(SHELF_FHIRID), idsOf(search("_id", SHELF_FHIRID)));
        assertEquals(List.of(ROOM_FHIRID), idsOf(search("identifier", "MAIN")));
        assertEquals(List.of(DEVICE_FHIRID), idsOf(search("identifier", "MAIN-FRZ01")));
        assertEquals(List.of(ROOM_2_FHIRID), idsOf(search("name", "secondary")));
        assertEquals(2, search("name:contains", "Unit").get("total").asInt());
        assertEquals(0, search("name", "Nowhere").get("total").asInt());
    }

    @Test
    public void search_byTagStatusAndPartOf() throws Exception {
        assertEquals(2, search("_tag", "room").get("total").asInt());
        assertEquals(2, search("_tag", "http://openelis.org/fhir/tag/storage-hierarchy|box").get("total").asInt());
        assertEquals(0, search("_tag", "http://example.org/other|box").get("total").asInt());
        assertEquals(10, search("status", "active").get("total").asInt());
        assertEquals(0, search("status", "inactive").get("total").asInt());
        assertTrue(idsOf(search("partof", "Location/" + ROOM_FHIRID))
                .containsAll(List.of(DEVICE_FHIRID, DEVICE_2_FHIRID)));
        assertEquals(2, search("partof", ROOM_FHIRID).get("total").asInt());
    }

    @Test
    public void search_includes_walkTheHierarchy() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Location");
        request.setQueryString("_id=" + SHELF_FHIRID + "&_include=Location:partof");
        request.addParameter("_id", SHELF_FHIRID);
        request.addParameter("_include", "Location:partof");
        JsonNode bundle = serve(request);
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(SHELF_FHIRID, DEVICE_FHIRID), idsOf(bundle));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());

        request = buildFhirRequest("GET", "/Location");
        request.setQueryString("_id=" + ROOM_FHIRID + "&_revinclude=Location:partof");
        request.addParameter("_id", ROOM_FHIRID);
        request.addParameter("_revinclude", "Location:partof");
        bundle = serve(request);
        assertEquals(1, bundle.get("total").asInt());
        assertTrue(idsOf(bundle).containsAll(List.of(ROOM_FHIRID, DEVICE_FHIRID, DEVICE_2_FHIRID)));
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Location"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Location");
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
        assertEquals(10, search("_lastUpdated", "ge2025-01-01").get("total").asInt());
        assertEquals(10, search("_lastUpdated", "2025-01-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2025-01-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "gt2025-01-02").get("total").asInt());
        JsonNode resource = search("_id", SHELF_FHIRID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2025-01-01"));
    }
}
