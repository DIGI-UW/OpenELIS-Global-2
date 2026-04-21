package org.openelisglobal.inventory.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryStorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");

        objectMapper = new ObjectMapper();

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    @Test
    public void testGetAllActive_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetById_ValidId_ShouldReturnLocation() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Refrigerator 1"));
    }

    @Test
    public void testGetByType_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/type/REFRIGERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationType").value("REFRIGERATOR"));
    }

    @Test
    public void testGetByCode_ShouldReturnLocation() throws Exception {
        mockMvc.perform(get("/rest/inventory-storage-locations/code/TEST-FRIDGE-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000));
    }

    @Test
    public void testCreate_ShouldSucceed() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("New Shelf");
        location.setLocationCode("SHELF-01");
        location.setLocationType(LocationType.SHELF);
        location.setIsActive(true);

        mockMvc.perform(post("/rest/inventory-storage-locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location))
                .session(mockSession))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Shelf"));
    }

    @Test
    public void testUpdate_ShouldSucceed() throws Exception {
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setName("Updated Refrigerator");
        location.setLocationCode("TEST-FRIDGE-01");
        location.setLocationType(LocationType.REFRIGERATOR);
        location.setIsActive(true);

        mockMvc.perform(put("/rest/inventory-storage-locations/1000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location))
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Refrigerator"));
    }

    @Test
    public void testDeactivate_ShouldSucceed() throws Exception {
        mockMvc.perform(put("/rest/inventory-storage-locations/1001/deactivate")
                .session(mockSession))
                .andExpect(status().isOk());
    }
}
