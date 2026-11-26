package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.service.LabelManagementService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class LabelManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final int TEST_STORAGE_ROOM_ID = 1000;
    private static final int TEST_STORAGE_DEVICE_ID = 1001;
    private static final int TEST_STORAGE_DEVICE_EMPTY_CODE_ID = 1002;
    private static final int TEST_STORAGE_DEVICE_SHORT_CODE_ID = 1003;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");

        org.openelisglobal.common.services.IStatusService statusService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.common.services.IStatusService.class);
        String statusId = statusService
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Entered);
        if ("-1".equals(statusId)) {
            throw new IllegalStateException(
                    "SampleStatus.Entered not found in database. Test data may not be loaded correctly.");
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void postPrintLabelEndpoint_ShouldGeneratePdf_ForDeviceWithCode() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));
        assertNotNull("Device should have parentRoom", device.getParentRoom());
        assertNotNull("ParentRoom should have code", device.getParentRoom().getCode());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    public void printValidation_ShouldReturnBadRequest_WhenCodeIsMissing() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_EMPTY_CODE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertTrue("Device should have empty code", device.getCode() == null || device.getCode().isEmpty());

        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists()).andReturn();

        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    @Test
    public void postPrintLabelEndpoint_ShouldGeneratePdf_WhenCodeIsLessThanOrEqualTo10Chars() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_SHORT_CODE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should be ≤ 10 chars", device.getCode().length() <= 10);
        assertNotNull("Device should have parent room", device.getParentRoom());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    public void ErrorResponseIfCodeMissing_ReturnsJsonError() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_EMPTY_CODE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertTrue("Device should have empty code", device.getCode() == null || device.getCode().isEmpty());

        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    @Test
    public void printHistory_ShouldBeRecorded_AfterLabelGeneration() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        Integer count = countRowsInTable("storage_location_print_history");
        assertNotNull("Print history should be recorded", count);
        assertTrue("Print history count should be > 0", count > 0);
    }

    @Test
    public void testPostPrintLabelEndpoint_InvalidType_Returns400() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);

        mockMvc.perform(post("/rest/storage/room/" + deviceId + "/print-label")).andExpect(status().isBadRequest());
    }

    @Test
    public void postPrintLabelEndpoint_ShouldReturnBadRequest_ForInvalidType() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        mockMvc.perform(get("/rest/storage/device/" + deviceId + "/print-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void pdfGeneration_ShouldUseCode_FromEntity() throws Exception {
        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    public void printLabel_ShouldRecordAuthenticatedUser_InPrintHistory() throws Exception {
        resyncSequence("clinlims.system_user_seq", "clinlims.system_user");
        jdbcTemplate.update("INSERT INTO system_user (id, external_id, login_name, last_name, first_name, initials, "
                + "is_active, is_employee, lastupdated) "
                + "SELECT nextval('system_user_seq'), 'TEST_PRINTER', 'printtester', 'Tester', 'Print', 'PT', 'Y', 'Y', now() "
                + "WHERE NOT EXISTS (SELECT 1 FROM system_user WHERE login_name = 'printtester')");
        String expectedUserId = jdbcTemplate
                .queryForObject("SELECT id FROM system_user WHERE login_name = 'printtester'", String.class);
        authenticateAs("printtester");

        String deviceId = String.valueOf(TEST_STORAGE_DEVICE_ID);
        jdbcTemplate.update("DELETE FROM storage_location_print_history WHERE location_id = ?", deviceId);

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        String printedBy = jdbcTemplate.queryForObject(
                "SELECT printed_by FROM storage_location_print_history WHERE location_id = ?", String.class, deviceId);
        assertEquals("Print history should record the acting user", expectedUserId, printedBy);
        assertNotEquals("Should not be the hardcoded system user", "1", printedBy);
    }

    @Test
    public void printLabel_ShouldReturn401_WhenNoAuthContext() throws Exception {
        SecurityContextHolder.clearContext();
        int before = countRowsInTable("storage_location_print_history");

        mockMvc.perform(post("/rest/storage/device/" + TEST_STORAGE_DEVICE_ID + "/print-label"))
                .andExpect(status().isUnauthorized());

        assertEquals("No print history should be written", before, countRowsInTable("storage_location_print_history"));
    }
}