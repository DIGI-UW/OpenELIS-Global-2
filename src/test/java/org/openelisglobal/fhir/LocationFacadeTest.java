package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.LocationProvider;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.service.StorageShelfService;
import org.openelisglobal.storage.valueholder.StorageBox;
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

    private static final String ROOM_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
    private static final String DEVICE_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d6";
    private static final String SHELF_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6a3";
    private static final String RACK_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6f3";
    private static final String BOX_FHIRID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b1a3";
    private static final String UNKNOWN_FHIRID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    private StorageRoomService storageRoomService;
    @Autowired
    private StorageDeviceService storageDeviceService;
    @Autowired
    private StorageShelfService storageShelfService;
    @Autowired
    private StorageRackService storageRackService;
    @Autowired
    private StorageBoxService storageBoxService;

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

    @Test
    public void updateLocation_shouldUpdateRoomGivenId() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Updated Main Laboratory",
                  "description": "Updated laboratory storage facility",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "ro",
                      "display": "Room"
                    }],
                    "text": "Storage Room"
                  }
                }
                """.formatted(ROOM_FHIRID);

        MockHttpServletResponse response = serve(put(ROOM_FHIRID, locationJson));

        assertEquals(200, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Updated Main Laboratory", json.get("name").asText());
        assertEquals("Updated laboratory storage facility", json.get("description").asText());
        assertEquals("MAIN", roomByUuid(ROOM_FHIRID).getCode());
    }

    @Test
    public void updateLocation_roomRoundTrip_shouldBeAcceptedAsRead() throws Exception {

        ObjectNode asRead = (ObjectNode) readJson(ROOM_FHIRID);
        asRead.put("name", "Round-tripped Laboratory");

        MockHttpServletResponse response = serve(put(ROOM_FHIRID, asRead.toString()));

        assertEquals("a Location read from the facade must be writable back unchanged", 200, response.getStatus());
        assertEquals("Round-tripped Laboratory", roomByUuid(ROOM_FHIRID).getName());
    }

    @Test
    public void updateLocation_boxRoundTrip_shouldPreserveContainerType() throws Exception {

        ObjectNode asRead = (ObjectNode) readJson(BOX_FHIRID);
        asRead.put("name", "Plate A renamed");

        MockHttpServletResponse response = serve(put(BOX_FHIRID, asRead.toString()));

        assertEquals(200, response.getStatus());
        StorageBox box = boxByUuid(BOX_FHIRID);
        assertEquals("Plate A renamed", box.getLabel());
        assertEquals("96-well", box.getType());
        assertEquals(RACK_FHIRID, box.getParentRack().getFhirUuidAsString());
    }

    @Test
    public void updateLocation_shouldUpdateDeviceGivenIdWithoutTouchingCode() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Updated Freezer Unit",
                  "mode": "instance",
                  "identifier": [{
                    "system": "http://openelis.org/storage-location-code",
                    "value": "MAIN-FRZ01"
                  }],
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "ve",
                      "display": "Vehicle"
                    }],
                    "text": "Storage Equipment"
                  },
                  "type": [{
                    "coding": [{
                      "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
                      "code": "freezer",
                      "display": "Freezer"
                    }]
                  }]
                }
                """.formatted(DEVICE_FHIRID, ROOM_FHIRID);

        MockHttpServletResponse response = serve(put(DEVICE_FHIRID, locationJson));

        assertEquals(200, response.getStatus());
        StorageDevice device = deviceByUuid(DEVICE_FHIRID);
        assertEquals("Updated Freezer Unit", device.getName());
        assertEquals("the hierarchical identifier must not overwrite the device code", "FRZ01", device.getCode());
    }

    @Test
    public void updateLocation_shelfWithoutPartOf_shouldKeepExistingParent() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Updated Shelf-A",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "co",
                      "display": "Container"
                    }],
                    "text": "Storage Shelf"
                  }
                }
                """.formatted(SHELF_FHIRID);

        MockHttpServletResponse response = serve(put(SHELF_FHIRID, locationJson));

        assertEquals(200, response.getStatus());
        StorageShelf shelf = shelfByUuid(SHELF_FHIRID);
        assertEquals("Updated Shelf-A", shelf.getLabel());
        assertEquals(DEVICE_FHIRID, shelf.getParentDevice().getFhirUuidAsString());
    }

    @Test
    public void updateLocation_shouldUpdateRackGivenId() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Updated Rack R1",
                  "mode": "instance",
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "co",
                      "display": "Container"
                    }],
                    "text": "Storage Rack"
                  }
                }
                """.formatted(RACK_FHIRID);

        MockHttpServletResponse response = serve(put(RACK_FHIRID, locationJson));

        assertEquals(200, response.getStatus());
        assertEquals("Updated Rack R1", objectMapper.readTree(response.getContentAsString()).get("name").asText());
    }

    @Test
    public void updateLocation_withUnknownId_shouldReturn404() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Ghost Room",
                  "physicalType": { "text": "Storage Room" }
                }
                """.formatted(UNKNOWN_FHIRID);

        int roomsBefore = storageRoomService.getAll().size();

        MockHttpServletResponse response = serve(put(UNKNOWN_FHIRID, locationJson));

        assertEquals(404, response.getStatus());
        assertEquals("update of an unknown id must not create a room", roomsBefore, storageRoomService.getAll().size());
    }

    @Test
    public void updateLocation_withMismatchedBodyId_shouldReturn400() throws Exception {

        String locationJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "status": "active",
                  "name": "Wrong Target",
                  "physicalType": { "text": "Storage Room" }
                }
                """.formatted("f2cdeff8-8d5b-4023-bd7c-932b4b98b6d4");

        MockHttpServletResponse response = serve(put(ROOM_FHIRID, locationJson));

        assertEquals(400, response.getStatus());
        assertEquals("Main Laboratory", roomByUuid(ROOM_FHIRID).getName());
    }

    @Test
    public void createRoom_shouldReturnSuccess() throws Exception {

        String roomJson = """
                {
                  "resourceType": "Location",
                  "name": "Cold Store",
                  "description": "New cold store",
                  "status": "active",
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "ro",
                      "display": "Room"
                    }],
                    "text": "Storage Room"
                  }
                }
                """;

        MockHttpServletResponse response = serve(post(roomJson));

        assertEquals(201, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Location", json.get("resourceType").asText());
        assertNotNull(json.get("id"));
        assertEquals("Cold Store", json.get("name").asText());
        assertFalse("the service must assign a room code", roomByUuid(json.get("id").asText()).getCode() == null
                || roomByUuid(json.get("id").asText()).getCode().isBlank());
    }

    @Test
    public void createRoom_withClientSuppliedExistingId_shouldCreateNewRoomNotUpdate() throws Exception {

        String roomJson = """
                {
                  "resourceType": "Location",
                  "id": "%s",
                  "name": "Impostor Room",
                  "status": "active",
                  "physicalType": { "text": "Storage Room" }
                }
                """.formatted(ROOM_FHIRID);

        int roomsBefore = storageRoomService.getAll().size();

        MockHttpServletResponse response = serve(post(roomJson));

        assertEquals(201, response.getStatus());
        String newId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
        assertFalse("server must assign its own id on create", ROOM_FHIRID.equals(newId));
        assertEquals(roomsBefore + 1, storageRoomService.getAll().size());
        assertEquals("Main Laboratory", roomByUuid(ROOM_FHIRID).getName());
    }

    @Test
    public void createDevice_shouldReturnSuccess() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Freezer Unit 2",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "type": [{
                    "coding": [{
                      "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
                      "code": "freezer",
                      "display": "Freezer"
                    }]
                  }],
                  "physicalType": {
                    "coding": [{
                      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
                      "code": "ve",
                      "display": "Vehicle"
                    }],
                    "text": "Storage Equipment"
                  }
                }
                """.formatted(ROOM_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(201, response.getStatus());
        String newId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
        StorageDevice device = deviceByUuid(newId);
        assertEquals("Freezer Unit 2", device.getName());
        assertEquals(ROOM_FHIRID, device.getParentRoom().getFhirUuidAsString());
        assertTrue("the service must assign a device code", device.getCode() != null && !device.getCode().isBlank());
    }

    @Test
    public void createDevice_usingHierarchyTagInsteadOfPhysicalTypeText_shouldReturnSuccess() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "meta": {
                    "tag": [{
                      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
                      "code": "device"
                    }]
                  },
                  "name": "Tagged Freezer",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "type": [{ "coding": [{ "code": "freezer" }] }]
                }
                """.formatted(ROOM_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(201, response.getStatus());
        assertEquals("Storage Equipment",
                objectMapper.readTree(response.getContentAsString()).get("physicalType").get("text").asText());
    }

    @Test
    public void createDevice_withoutType_shouldReturn400() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Untyped Equipment",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": { "text": "Storage Equipment" }
                }
                """.formatted(ROOM_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void createShelf_shouldReturnSuccess() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Shelf X",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": {
                    "coding": [{ "code": "co", "display": "Container" }],
                    "text": "Storage Shelf"
                  }
                }
                """.formatted(DEVICE_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createShelf_withoutPartOf_shouldReturn400() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Orphan Shelf",
                  "status": "active",
                  "physicalType": { "text": "Storage Shelf" }
                }
                """;

        int shelvesBefore = storageShelfService.getAll().size();

        MockHttpServletResponse response = serve(post(json));

        assertEquals(400, response.getStatus());
        assertEquals(shelvesBefore, storageShelfService.getAll().size());
    }

    @Test
    public void createShelf_withUnknownParent_shouldReturn400() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Lost Shelf",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": { "text": "Storage Shelf" }
                }
                """.formatted(UNKNOWN_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void createLocation_withoutStorageLevel_shouldReturn400() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Unclassified",
                  "status": "active"
                }
                """;

        MockHttpServletResponse response = serve(post(json));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void createRack_shouldReturnSuccess() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Rack Z",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": {
                    "coding": [{ "code": "co", "display": "Container" }],
                    "text": "Storage Rack"
                  }
                }
                """.formatted(SHELF_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createBox_shouldPersistGridAndContainerType() throws Exception {

        String json = """
                {
                  "resourceType": "Location",
                  "name": "Plate C",
                  "status": "active",
                  "partOf": { "reference": "Location/%s" },
                  "physicalType": {
                    "coding": [{ "code": "co", "display": "Container" }],
                    "text": "Storage Box"
                  },
                  "type": [{
                    "coding": [{
                      "system": "http://openelis.org/fhir/CodeSystem/storage-box-type",
                      "code": "384-well"
                    }]
                  }],
                  "extension": [
                    { "url": "http://openelis.org/fhir/extension/rack-grid-dimensions", "valueString": "16 × 24" },
                    { "url": "http://openelis.org/fhir/extension/rack-position-schema-hint", "valueString": "letter-number" }
                  ]
                }
                """
                .formatted(RACK_FHIRID);

        MockHttpServletResponse response = serve(post(json));

        assertEquals(201, response.getStatus());
        String newId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
        StorageBox box = boxByUuid(newId);
        assertEquals("384-well", box.getType());
        assertEquals(Integer.valueOf(16), box.getRows());
        assertEquals(Integer.valueOf(24), box.getColumns());
    }

    @Test
    public void deleteLocation_shouldDeactivateDevice() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + DEVICE_FHIRID));

        assertEquals(204, response.getStatus());
        assertFalse(deviceByUuid(DEVICE_FHIRID).getActive());
        assertEquals("inactive", readJson(DEVICE_FHIRID).get("status").asText());
    }

    @Test
    public void deleteLocation_shouldDeactivateRoom() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + ROOM_FHIRID));

        assertEquals(204, response.getStatus());
        assertFalse(roomByUuid(ROOM_FHIRID).getActive());
    }

    @Test
    public void deleteLocation_shouldDeactivateRack() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + RACK_FHIRID));

        assertEquals(204, response.getStatus());
        StorageRack rack = storageRackService.getAllMatching("fhirUuid", UUID.fromString(RACK_FHIRID)).getFirst();
        assertNotNull(rack);
        assertFalse(rack.getActive());
    }

    @Test
    public void deleteLocation_shouldDeactivateShelf() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + SHELF_FHIRID));

        assertEquals(204, response.getStatus());
        assertFalse(shelfByUuid(SHELF_FHIRID).getActive());
    }

    @Test
    public void deleteLocation_shouldDeactivateBox() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + BOX_FHIRID));

        assertEquals(204, response.getStatus());
        assertFalse(boxByUuid(BOX_FHIRID).getActive());
        assertEquals("inactive", readJson(BOX_FHIRID).get("status").asText());
    }

    @Test
    public void deleteLocation_withUnknownId_shouldReturn404() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/" + UNKNOWN_FHIRID));

        assertEquals(404, response.getStatus());
    }

    @Test
    public void deleteLocation_withInvalidId_shouldReturn400() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/Location/not-a-uuid"));

        assertEquals(400, response.getStatus());
    }

    private JsonNode readJson(String uuid) throws Exception {
        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Location/" + uuid));
        assertEquals(200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private StorageRoom roomByUuid(String uuid) {
        return storageRoomService.getAllMatching("fhirUuid", UUID.fromString(uuid)).getFirst();
    }

    private StorageDevice deviceByUuid(String uuid) {
        return storageDeviceService.getAllMatching("fhirUuid", UUID.fromString(uuid)).getFirst();
    }

    private StorageShelf shelfByUuid(String uuid) {
        return storageShelfService.getAllMatching("fhirUuid", UUID.fromString(uuid)).getFirst();
    }

    private StorageBox boxByUuid(String uuid) {
        return storageBoxService.getAllMatching("fhirUuid", UUID.fromString(uuid)).getFirst();
    }

    private MockHttpServletRequest post(String body) {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Location");
        request.setContent(body.getBytes());
        return request;
    }

    private MockHttpServletRequest put(String uuid, String body) {
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Location/" + uuid);
        request.setContent(body.getBytes());
        request.setContentType("application/fhir+json");
        return request;
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
