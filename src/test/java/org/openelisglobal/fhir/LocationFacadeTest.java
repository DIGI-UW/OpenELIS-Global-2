package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.LocationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class LocationFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ROOM_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
    private static final String DEVICE_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d6";
    private static final String SHELF_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6a3";
    private static final String RACK_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6f3";
    private static final String BOX_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b1a3";
    private static final String UNKNOWN_FHIRID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    private LocationProvider locationProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;
    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/facade-location.xml");
        // The fixture seeds ids 1-2 at every level without advancing the sequences.
        for (String table : new String[] { "storage_room", "storage_device", "storage_shelf", "storage_rack",
                "storage_box" }) {
            resyncSequence("clinlims." + table + "_seq", "clinlims." + table);
        }

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(locationProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readLocation_shouldReturnRoomGivenId() throws Exception {

        JsonNode json = readJson(ROOM_FHIRID);

        assertEquals("Location", json.get("resourceType").asText());
        assertEquals("Main Laboratory", json.get("name").asText());
        assertEquals("Storage Room", json.get("physicalType").get("text").asText());
        assertEquals("room", json.get("meta").get("tag").get(0).get("code").asText());
    }

    /**
     * OGC-1399 (IG Known Issue 9): ServiceRequest.locationReference and the subject
     * of a patientless order point at Location/{organization fhir id}, which did
     * not resolve.
     */
    @Test
    public void readLocation_shouldReturnTheSiteLocationOfAnOrganization() throws Exception {

        JsonNode json = readJson("20e61b21-1399-42cc-b708-1a1194888781");

        assertEquals("Location", json.get("resourceType").asText());
        assertEquals("20e61b21-1399-42cc-b708-1a1194888781", json.get("id").asText());
        assertEquals("Bekasi Riverside", json.get("name").asText());
        assertEquals("active", json.get("status").asText());
        assertEquals("si", json.get("physicalType").get("coding").get(0).get("code").asText());
        assertEquals("Organization/20e61b21-1399-42cc-b708-1a1194888781",
                json.get("managingOrganization").get("reference").asText());
    }

    @Test
    public void readLocation_shouldMarkAnInactiveOrganizationsSiteInactive() throws Exception {

        JsonNode json = readJson("20e61b21-1399-42cc-b708-1a1194888782");

        assertEquals("inactive", json.get("status").asText());
    }

    @Test
    public void readLocation_shouldReturn400GivenInvalidId() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Location/not-a-uuid"));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void readLocation_shouldReturn404GivenNonExistingId() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Location/" + UNKNOWN_FHIRID));

        assertEquals(404, response.getStatus());
    }

    @Test
    public void readLocation_shouldReturnDeviceGivenId() throws Exception {

        JsonNode json = readJson(DEVICE_FHIRID);

        assertEquals("Freezer Unit 1", json.get("name").asText());
        assertEquals("Location/" + ROOM_FHIRID, json.get("partOf").get("reference").asText());
    }

    @Test
    public void readLocation_shouldReturnShelfGivenId() throws Exception {

        assertEquals("Shelf-A", readJson(SHELF_FHIRID).get("name").asText());
    }

    @Test
    public void readLocation_shouldReturnRackGivenId() throws Exception {

        assertEquals("Rack R1", readJson(RACK_FHIRID).get("name").asText());
    }

    @Test
    public void readLocation_shouldReturnBoxWithContainerTypeSeparateFromLevel() throws Exception {

        JsonNode json = readJson(BOX_FHIRID);

        assertEquals("Plate A", json.get("name").asText());
        assertEquals("Storage Box", json.get("physicalType").get("text").asText());
        assertEquals("96-well", json.get("type").get(0).get("coding").get(0).get("code").asText());
        assertEquals("active", json.get("status").asText());
    }

    private JsonNode readJson(String uuid) throws Exception {
        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Location/" + uuid));
        assertEquals(200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
