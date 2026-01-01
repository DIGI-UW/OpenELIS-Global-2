package org.openelisglobal.equipmentusage.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.equipmentusage.valueholder.Equipment;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class EquipmentRestControllerTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/equipment.xml");
    }

    private UserSessionData createMockUserSession() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        return usd;
    }

    @Test
    public void getAllActive_shouldReturnAllActiveEquipment() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        List<Equipment> equipmentList = objectMapper.readValue(result, new TypeReference<List<Equipment>>() {
        });

        assertNotNull("Equipment list should not be null", equipmentList);
        assertFalse("Equipment list should not be empty", equipmentList.isEmpty());

        for (Equipment equipment : equipmentList) {
            assertEquals("All equipment should be active", "Y", equipment.getIsActive());
        }
    }

    @Test
    public void getEquipmentForDropdown_shouldReturnActiveEquipment() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/dropdown")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        List<Equipment> equipmentList = objectMapper.readValue(result, new TypeReference<List<Equipment>>() {
        });

        assertNotNull("Equipment dropdown list should not be null", equipmentList);
        assertFalse("Equipment dropdown list should not be empty", equipmentList.isEmpty());

        for (Equipment equipment : equipmentList) {
            assertEquals("Dropdown equipment should be active", "Y", equipment.getIsActive());
        }
    }

    @Test
    public void getById_shouldReturnEquipmentGivenId() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/1")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        Equipment equipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Equipment should not be null", equipment);
        assertEquals("Equipment ID should match", Long.valueOf(1), equipment.getId());
        assertNotNull("Equipment name should not be null", equipment.getName());
    }

    @Test
    public void getById_shouldReturn404WhenEquipmentNotFound() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/9999")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void getBySerialNumber_shouldReturnEquipmentGivenSerialNumber() throws Exception {
        String serialNumber = "SN-001";
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/serial/" + serialNumber)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        Equipment equipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Equipment should not be null", equipment);
        assertEquals("Serial number should match", serialNumber, equipment.getSerialNumber());
    }

    @Test
    public void getBySerialNumber_shouldReturn404WhenSerialNumberNotFound() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/serial/INVALID-SN")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void searchByName_shouldReturnEquipmentMatchingSearchTerm() throws Exception {
        String searchTerm = "Freezer";
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/search")
                .param("q", searchTerm)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        List<Equipment> equipmentList = objectMapper.readValue(result, new TypeReference<List<Equipment>>() {
        });

        assertNotNull("Equipment search result should not be null", equipmentList);
        assertFalse("Equipment search result should not be empty", equipmentList.isEmpty());

        for (Equipment equipment : equipmentList) {
            assertTrue("Equipment name should contain search term",
                    equipment.getName().toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }

    @Test
    public void searchByName_shouldReturnEmptyListWhenNoMatchesFound() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/search")
                .param("q", "NonexistentEquipment12345")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        List<Equipment> equipmentList = objectMapper.readValue(result, new TypeReference<List<Equipment>>() {
        });

        assertNotNull("Equipment search result should not be null", equipmentList);
        assertTrue("Equipment search result should be empty", equipmentList.isEmpty());
    }

    @Test
    public void getByDepartment_shouldReturnEquipmentForGivenDepartment() throws Exception {
        String department = "Lab";
        MvcResult urlResult = super.mockMvc.perform(get("/rest/equipment/department/" + department)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        List<Equipment> equipmentList = objectMapper.readValue(result, new TypeReference<List<Equipment>>() {
        });

        assertNotNull("Equipment by department should not be null", equipmentList);
        assertFalse("Equipment by department should not be empty", equipmentList.isEmpty());

        for (Equipment equipment : equipmentList) {
            assertEquals("Department should match", department, equipment.getDepartment());
        }
    }

    @Test
    public void createEquipment_shouldCreateNewEquipment() throws Exception {
        Equipment newEquipment = new Equipment();
        newEquipment.setName("New Centrifuge");
        newEquipment.setSerialNumber("SN-NEW-001");
        newEquipment.setDepartment("Lab");
        newEquipment.setManufacturer("Eppendorf");
        newEquipment.setModelNumber("5810R");
        newEquipment.setIsActive("Y");
        newEquipment.setPurchaseDate(LocalDate.of(2024, 6, 15));
        newEquipment.setLastCalibrationDate(LocalDate.of(2024, 12, 1));
        newEquipment.setNextCalibrationDue(LocalDate.of(2025, 6, 1));

        String requestBody = objectMapper.writeValueAsString(newEquipment);

        MvcResult urlResult = super.mockMvc.perform(post("/rest/equipment")
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createMockUserSession()))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(201, status);

        String result = urlResult.getResponse().getContentAsString();
        Equipment createdEquipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Created equipment should not be null", createdEquipment);
        assertNotNull("Created equipment ID should not be null", createdEquipment.getId());
        assertEquals("Equipment name should match", "New Centrifuge", createdEquipment.getName());
        assertEquals("Equipment serial number should match", "SN-NEW-001", createdEquipment.getSerialNumber());
        assertEquals("Equipment department should match", "Lab", createdEquipment.getDepartment());
        assertEquals("Equipment manufacturer should match", "Eppendorf", createdEquipment.getManufacturer());
        assertEquals("Equipment model should match", "5810R", createdEquipment.getModelNumber());
    }

    @Test
    public void updateEquipment_shouldUpdateExistingEquipment() throws Exception {
        Equipment equipmentToUpdate = new Equipment();
        equipmentToUpdate.setId(1L);
        equipmentToUpdate.setName("Updated Equipment Name");
        equipmentToUpdate.setSerialNumber("SN-001");
        equipmentToUpdate.setDepartment("Updated Lab");
        equipmentToUpdate.setManufacturer("Updated Manufacturer");
        equipmentToUpdate.setModelNumber("Updated Model");
        equipmentToUpdate.setIsActive("Y");

        String requestBody = objectMapper.writeValueAsString(equipmentToUpdate);

        MvcResult urlResult = super.mockMvc.perform(put("/rest/equipment/1")
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, createMockUserSession()))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String result = urlResult.getResponse().getContentAsString();
        Equipment updatedEquipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Updated equipment should not be null", updatedEquipment);
        assertEquals("Equipment ID should remain the same", Long.valueOf(1), updatedEquipment.getId());
        assertEquals("Equipment name should be updated", "Updated Equipment Name", updatedEquipment.getName());
        assertEquals("Equipment department should be updated", "Updated Lab", updatedEquipment.getDepartment());
    }

    @Test
    public void deactivateEquipment_shouldSetEquipmentToInactive() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(put("/rest/equipment/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(204, status);

        // Verify the equipment is deactivated by fetching it
        MvcResult verifyResult = super.mockMvc.perform(get("/rest/equipment/1")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String result = verifyResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Equipment deactivatedEquipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Equipment should not be null", deactivatedEquipment);
        assertEquals("Equipment should be inactive", "N", deactivatedEquipment.getIsActive());
    }

    @Test
    public void activateEquipment_shouldSetEquipmentToActive() throws Exception {
        super.mockMvc.perform(put("/rest/equipment/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        MvcResult urlResult = super.mockMvc.perform(put("/rest/equipment/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(204, status);

        MvcResult verifyResult = super.mockMvc.perform(get("/rest/equipment/1")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String result = verifyResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Equipment activatedEquipment = objectMapper.readValue(result, Equipment.class);

        assertNotNull("Equipment should not be null", activatedEquipment);
        assertEquals("Equipment should be active", "Y", activatedEquipment.getIsActive());
    }
}
