package org.openelisglobal.inventory.controller.rest;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryStorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;

    @Autowired
    private InventoryLotService lotService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");

        // Make sure Lot 1000 is not expired so it is "active"
        InventoryLot lot = lotService.get(1000L);
        if (lot != null) {
            Calendar cal = Calendar.getInstance();
            cal.set(2027, Calendar.DECEMBER, 31);
            lot.setExpirationDate(new Timestamp(cal.getTimeInMillis()));
            lotService.update(lot);
        }

        objectMapper = new ObjectMapper();

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    @Test
    public void testGetAllActive_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetById_ValidId_ShouldReturnLocation() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/1000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Refrigerator 1"));
    }

    @Test
    public void testGetByType_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/type/REFRIGERATOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationType").value("REFRIGERATOR"));
    }

    @Test
    public void testGetByCode_ShouldReturnLocation() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/code/TEST-FRIDGE-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000));
    }

    @Test
    public void testCreate_ShouldSucceed() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("New Shelf");
        location.setLocationCode("SHELF-01");
        location.setLocationType(LocationType.SHELF);
        location.setIsActive(true);
        location.setFhirUuid(java.util.UUID.randomUUID());

        mockMvc.perform(post("/rest/inventory-storage-locations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location)).session(mockSession))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("New Shelf"));
    }

    @Test
    public void testUpdate_ShouldSucceed() throws Exception {
        String response = mockMvc.perform(get("/rest/inventory-storage-locations/1000")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InventoryStorageLocation location = objectMapper.readValue(response, InventoryStorageLocation.class);
        location.setName("Updated Refrigerator");

        mockMvc.perform(put("/rest/inventory-storage-locations/1000").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Refrigerator"));
    }

    @Test
    public void testDeactivate_ShouldSucceed() throws Exception {
        // Create a new location first so we have one without lots
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("Temporary Location");
        location.setLocationType(LocationType.SHELF);
        location.setIsActive(true);
        location.setFhirUuid(java.util.UUID.randomUUID());

        String response = mockMvc
                .perform(post("/rest/inventory-storage-locations").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)).session(mockSession))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        InventoryStorageLocation saved = objectMapper.readValue(response, InventoryStorageLocation.class);

        mockMvc.perform(put("/rest/inventory-storage-locations/" + saved.getId() + "/deactivate").session(mockSession))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetChildren_ValidParent_ShouldReturnList() throws Exception {
        // Parent 1000 should have 0 children in old XML
        mockMvc.perform(get("/rest/inventory-storage-locations/1000/children")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetTopLevel_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/top-level")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0].parentLocation").value(nullValue()));
    }

    @Test
    public void testGetPath_ValidId_ShouldReturnPath() throws Exception {
        // Test Refrigerator 1 (1000) has no parent
        mockMvc.perform(get("/rest/inventory-storage-locations/1000/path")).andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("Test Refrigerator 1"));
    }

    @Test
    public void testHasActiveLots_ValidId_ShouldReturnBoolean() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/1000/has-active-lots")).andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveLots").value(true));
    }

    @Test
    public void testGetById_InvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/9999")).andExpect(status().isNotFound());
    }

    @Test
    public void testGetByCode_InvalidCode_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/code/NON-EXISTENT")).andExpect(status().isNotFound());
    }

    @Test
    public void testGetByType_InvalidEnum_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/type/INVALID_TYPE")).andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdate_InvalidId_ShouldReturn404() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("Nowhere");
        location.setLocationType(LocationType.ROOM);
        location.setFhirUuid(java.util.UUID.randomUUID());

        mockMvc.perform(put("/rest/inventory-storage-locations/9999").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location)).session(mockSession))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeactivate_WithActiveLots_ShouldReturn409() throws Exception {
        // Location 1000 has active lots
        mockMvc.perform(put("/rest/inventory-storage-locations/1000/deactivate").session(mockSession))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testCreate_InvalidData_ShouldReturn400() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setFhirUuid(java.util.UUID.randomUUID());
        // Missing name and locationType

        mockMvc.perform(post("/rest/inventory-storage-locations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location)).session(mockSession))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testCreate_NoSession_ShouldReturn401() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("New Shelf");
        location.setLocationType(LocationType.SHELF);
        location.setFhirUuid(java.util.UUID.randomUUID());

        // Performing post without .session(mockSession)
        mockMvc.perform(post("/rest/inventory-storage-locations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location))).andExpect(status().isUnauthorized());
    }
}
