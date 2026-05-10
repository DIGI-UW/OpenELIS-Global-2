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
import org.openelisglobal.fhir.providers.LocationProvider;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class LocationFacadeTest extends BaseWebContextSensitiveTest {
    @Autowired
    private StorageRoomDAO storageRoomDAO;
    @Autowired
    private StorageShelfDAO storageShelfDao;
    @Autowired
    private StorageDeviceDAO storageDeviceDAO;
    @Autowired
    private StorageRackDAO storageRackDAO;
    @Autowired
    private StorageBoxDAO storageBoxDAO;

    private static final String ROOM_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
    private static final String RACK_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6f3";
    private static final String SHELF_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6a3";
    private static final String BOX_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b1a3";
    private static final String DEVICE_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d6";

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private LocationProvider locatiobProvider;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/facade-location.xml");

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(locatiobProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readLocation_shouldReturnRoomGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Location/" + ROOM_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", jsonResponse.get("resourceType").asText());
        assertEquals("Main Laboratory", jsonResponse.get("name").asText());

    }

    @Test
    public void readLocation_shouldReturndDeviceGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Location/" + DEVICE_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", jsonResponse.get("resourceType").asText());
        assertEquals("Freezer Unit 1", jsonResponse.get("name").asText());

    }

    @Test
    public void readLocation_shouldReturndShelfGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Location/" + SHELF_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", jsonResponse.get("resourceType").asText());
        assertEquals("Shelf-A", jsonResponse.get("name").asText());

    }

    @Test
    public void readLocation_shouldReturndRackGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Location/" + RACK_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", jsonResponse.get("resourceType").asText());
        assertEquals("Rack R1", jsonResponse.get("name").asText());

    }

    @Test
    public void readLocation_shouldReturndBoxGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Location/" + BOX_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", jsonResponse.get("resourceType").asText());
        assertEquals("Plate A", jsonResponse.get("name").asText());

    }

    @Test
    public void updateLocation_shouldUpdateRoomGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Location/" + ROOM_FHIRID);

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3",
                  "status": "active",
                  "name": "Updated Main Laboratory",
                  "description": "Updated laboratory storage facility",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "ro",
                        "display": "Room"
                      }
                    ],
                    "text": "Storage Room"
                  }
                }
                """;

        request.setContent(locationJson.getBytes());
        request.setContentType("application/fhir+json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Updated Main Laboratory", jsonResponse.get("name").asText());

        assertEquals("Updated laboratory storage facility", jsonResponse.get("description").asText());
    }

    @Test
    public void updateLocation_shouldUpdateDeviceGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Location/" + DEVICE_FHIRID);

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d6",
                  "status": "active",
                  "name": "Updated Freezer Unit",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "ve",
                        "display": "Vehicle"
                      }
                    ],
                    "text": "Storage Equipment"
                  },
                  "type": [
                    {
                      "coding": [
                        {
                          "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
                          "code": "freezer",
                          "display": "Freezer"
                        }
                      ]
                    }
                  ]
                }
                """;

        request.setContent(locationJson.getBytes());
        request.setContentType("application/fhir+json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Updated Freezer Unit", jsonResponse.get("name").asText());

    }

    @Test
    public void updateLocation_shouldUpdateShelfGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Location/" + SHELF_FHIRID);

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "f2cdeff8-8d5b-4023-bd7c-932b4b98b6a3",
                  "status": "active",
                  "name": "Updated Shelf-A",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "co",
                        "display": "Container"
                      }
                    ],
                    "text": "Storage Shelf"
                  }
                }
                """;

        request.setContent(locationJson.getBytes());
        request.setContentType("application/fhir+json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Updated Shelf-A", jsonResponse.get("name").asText());

    }

    @Test
    public void updateLocation_shouldUpdateRackGivenId() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Location/" + RACK_FHIRID);

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "f2cdeff8-8d5b-4023-bd7c-932b4b98b6f3",
                  "status": "active",
                  "name": "Updated Rack R1",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "co",
                        "display": "Container"
                      }
                    ],
                    "text": "Storage Rack"
                  }
                }
                """;

        request.setContent(locationJson.getBytes());
        request.setContentType("application/fhir+json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Updated Rack R1", jsonResponse.get("name").asText());

    }

    @Test
    public void createRoom_shouldReturnSuccess() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "storage_room" });

        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");

        String roomJson = """
                {
                  "resourceType": "Location",
                  "name": "Main Laboratory Updated",
                  "description": "Updated primary lab",
                  "status": "active",
                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "ro",
                        "display": "Room"
                      }
                    ],
                    "text": "Storage Room"
                  }
                }
                """;

        request.setContent(roomJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());

        assertEquals("Location", json.get("resourceType").asText());
        assertNotNull(json.get("id"));
        assertEquals("Main Laboratory Updated", json.get("name").asText());
    }

    @Test
    public void createDevice_shouldReturnSuccess() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "storage_device" });

        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Freezer Unit 2",
                  "status": "active",

                  "partOf": {
                    "reference": "Location/%s"
                  },

                  "type": [
                    {
                      "coding": [
                        {
                          "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
                          "code": "freezer",
                          "display": "Freezer"
                        }
                      ]
                    }
                  ],

                  "physicalType": {
                    "coding": [
                      {
                        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                        "code": "ve",
                        "display": "Vehicle"
                      }
                    ],
                    "text": "Storage Equipment"
                  }
                }
                """.formatted(ROOM_FHIRID);

        request.setContent(json.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createShelf_shouldReturnSuccess() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "storage_shelf" });

        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Shelf X",
                  "status": "active",
                  "partOf": {
                    "reference": "Location/%s"
                  },
                  "physicalType": {
                    "coding": [
                      {
                        "code": "co",
                        "display": "Container"
                      }
                    ],
                    "text": "Storage Shelf"
                  }
                }
                """.formatted(DEVICE_FHIRID);

        request.setContent(json.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createRack_shouldReturnSuccess() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "storage_rack" });

        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Rack Z",
                  "status": "active",
                  "partOf": {
                    "reference": "Location/%s"
                  },
                  "physicalType": {
                    "coding": [
                      {
                        "code": "co",
                        "display": "Container"
                      }
                    ],
                    "text": "Storage Rack"
                  }
                }
                """.formatted(SHELF_FHIRID);

        request.setContent(json.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createBox_shouldReturnSuccess() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "storage_box" });

        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Plate C",
                  "status": "active",

                  "partOf": {
                    "reference": "Location/%s"
                  },

                  "physicalType": {
                    "coding": [
                      {
                        "code": "co",
                        "display": "Container"
                      }
                    ],
                    "text": "Storage Box"
                  },

                  "extension": [
                    {
                      "url": "http://openelis.org/fhir/extension/rack-grid-dimensions",
                      "valueString": "8 × 12"
                    },
                    {
                      "url": "http://openelis.org/fhir/extension/storage-capacity",
                      "valueInteger": 96
                    },
                    {
                      "url": "http://openelis.org/fhir/extension/rack-position-schema-hint",
                      "valueString": "letter-number"
                    },
                    {
                      "url": "http://openelis.org/fhir/extension/position-occupancy",
                      "valueBoolean": false
                    }
                  ]
                }
                """.formatted(RACK_FHIRID);

        request.setContent(json.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void deleteLocation_shouldSetDeviceFalse() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Location/" + DEVICE_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        StorageDevice device = storageDeviceDAO.getAllMatching("fhirUuid", UUID.fromString(DEVICE_FHIRID)).getFirst();
        assertFalse(device.getActive());
    }

    @Test
    public void deleteLocation_shouldsetRoomFalse() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Location/" + ROOM_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        StorageRoom room = storageRoomDAO.getAllMatching("fhirUuid", UUID.fromString(ROOM_FHIRID)).getFirst();
        assertFalse(room.getActive());

    }

    @Test
    public void deleteLocation_shouldSetRackFalse() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Location/" + RACK_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        StorageRack storageRack = storageRackDAO.getAllMatching("fhirUuid", UUID.fromString(RACK_FHIRID)).getFirst();
        assertNotNull(storageRack);
        assertFalse(storageRack.getActive());

    }

    @Test
    public void deleteLocation_shouldSetShelfFalse() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Location/" + SHELF_FHIRID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        StorageShelf shelf = storageShelfDao.getAllMatching("fhirUuid", UUID.fromString(SHELF_FHIRID)).getFirst();
        assertFalse(shelf.getActive());

    }

}
