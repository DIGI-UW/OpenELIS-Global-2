package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Controller integration tests for Storage Location CRUD endpoints.
 */
@RunWith(SpringRunner.class)
public class StorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private StorageLocationRestController storageLocationRestController;

    private UserModuleService userModuleServiceMock;
    private UserRoleService userRoleServiceMock;

    private ObjectMapper objectMapper;
    private UserSessionData usd;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        userModuleServiceMock = Mockito.mock(UserModuleService.class);
        userRoleServiceMock = Mockito.mock(UserRoleService.class);
        ReflectionTestUtils.setField(storageLocationRestController, "userModuleService", userModuleServiceMock);
        ReflectionTestUtils.setField(storageLocationRestController, "userRoleService", userRoleServiceMock);

        usd = new UserSessionData();
        usd.setSytemUserId(1);
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(true);
        executeDataSetWithStateManagement("testdata/storage-location-controller.xml");
    }

    @Test
    public void testDeleteRoom_WithValidId_Returns204() throws Exception {
        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/rooms/20000").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", usd)).andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteRoom_WithChildDevices_Returns409() throws Exception {
        // Act & Assert
        this.mockMvc
                .perform(delete("/rest/storage/rooms/20001").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.dependentCount").value(1));
    }

    @Test
    public void testDeleteDevice_WithValidId_Returns204() throws Exception {
        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/devices/20001").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", usd)).andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteShelf_WithValidId_Returns204() throws Exception {
        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/shelves/20000").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", usd)).andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteRack_WithValidId_Returns204() throws Exception {
        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/racks/20000").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", usd)).andExpect(status().isNoContent());
    }

    @Test
    public void testUpdateDevice_WithParentRoomChange_PersistsNewParent() throws Exception {
        // Arrange: Create a second room
        String roomJson = "{" + "\"name\":\"Secondary Lab\"," + "\"code\":\"SEC-LAB\","
                + "\"description\":\"Secondary laboratory\"," + "\"active\":true" + "}";
        this.mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(roomJson)
                .sessionAttr("userSessionData", usd)).andExpect(status().isCreated());

        // Get the new room ID from response (simplified - in real test would parse
        // response)
        // For this test, we'll use a known room ID from test data
        Integer newRoomId = 20002; // Assuming this exists in test data or was just created

        // Update device with new parent room
        String deviceUpdateJson = "{" + "\"name\":\"Updated Freezer\"," + "\"code\":\"FRZ01\","
                + "\"type\":\"freezer\"," + "\"parentRoomId\":\"" + newRoomId + "\"," + "\"active\":true" + "}";

        // Act & Assert: Update device
        this.mockMvc
                .perform(put("/rest/storage/devices/20001").contentType(MediaType.APPLICATION_JSON)
                        .content(deviceUpdateJson).sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(20001))
                .andExpect(jsonPath("$.parentRoomId").value(newRoomId));
    }

    @Test
    public void testCanMoveDevice_WithDownstreamSamples_ReturnsWarning() throws Exception {
        // Act & Assert: Check if device can be moved
        this.mockMvc
                .perform(get("/rest/storage/devices/20001/can-move?newParentRoomId=20002")
                        .contentType(MediaType.APPLICATION_JSON).sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canMove").value(true))
                .andExpect(jsonPath("$.hasDownstreamSamples").exists()).andExpect(jsonPath("$.sampleCount").exists());
    }

    @Test
    public void testCreateDevice_WithConnectivityFields_Returns201() throws Exception {
        String deviceJson = "{" + "\"name\":\"IoT Freezer\"," + "\"code\":\"IOT-FRZ-01\"," + "\"type\":\"freezer\","
                + "\"parentRoomId\":20005," + "\"ipAddress\":\"192.168.1.100\"," + "\"port\":502,"
                + "\"communicationProtocol\":\"BACnet\"" + "}";

        // Act & Assert
        this.mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON).content(deviceJson)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ipAddress").value("192.168.1.100"))
                .andExpect(jsonPath("$.port").value(502))
                .andExpect(jsonPath("$.communicationProtocol").value("BACnet"));
    }

    @Test
    public void testCreateDevice_WithBiorepositoryStorageFlag_Returns201WithFlag() throws Exception {
        String deviceJson = "{" + "\"name\":\"Bio Freezer\"," + "\"code\":\"BIO-FRZ-01\"," + "\"type\":\"freezer\","
                + "\"parentRoomId\":20005," + "\"biorepositoryStorage\":true" + "}";

        this.mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON).content(deviceJson)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.biorepositoryStorage").value(true));
    }

    @Test
    public void testGetRooms_WithBiorepositoryOnly_ReturnsOnlyBiorepositoryRooms() throws Exception {
        // Mark one room as biorepository-enabled by flagging one of its devices
        jdbcTemplate.execute("UPDATE clinlims.storage_device SET biorepository_storage = true WHERE id = 20001");

        MvcResult mvcResult = this.mockMvc
                .perform(get("/rest/storage/rooms?biorepositoryOnly=true").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, Object>> rooms = readMapList(mvcResult.getResponse().getContentAsString());
        assertFalse("Expected at least one biorepository room", rooms.isEmpty());
        assertTrue("All returned rooms must have biorepository devices",
                rooms.stream().allMatch(room -> Boolean.TRUE.equals(room.get("hasBiorepositoryDevices"))));
    }

    @Test
    public void testGetDevices_WithBiorepositoryOnly_ReturnsOnlyBiorepositoryDevices() throws Exception {
        jdbcTemplate.execute("UPDATE clinlims.storage_device SET biorepository_storage = true WHERE id = 20001");

        MvcResult mvcResult = this.mockMvc
                .perform(get("/rest/storage/devices?biorepositoryOnly=true").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, Object>> devices = readMapList(mvcResult.getResponse().getContentAsString());
        assertFalse("Expected at least one biorepository device", devices.isEmpty());
        assertTrue("All returned devices must be biorepository storage",
                devices.stream().allMatch(device -> Boolean.TRUE.equals(device.get("biorepositoryStorage"))));
    }

    @Test
    public void testGetShelvesAndRacks_WithBiorepositoryOnly_ReturnsOnlyBiorepositoryHierarchy() throws Exception {
        // Shelf 20001 and rack 20000 belong to device 20003
        jdbcTemplate.execute("UPDATE clinlims.storage_device SET biorepository_storage = true WHERE id = 20003");

        MvcResult shelvesResult = this.mockMvc
                .perform(get("/rest/storage/shelves?biorepositoryOnly=true").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, Object>> shelves = readMapList(shelvesResult.getResponse().getContentAsString());
        assertFalse("Expected at least one biorepository shelf", shelves.isEmpty());
        assertTrue("All returned shelves must inherit biorepository storage flag",
                shelves.stream().allMatch(shelf -> Boolean.TRUE.equals(shelf.get("biorepositoryStorage"))));

        MvcResult racksResult = this.mockMvc
                .perform(get("/rest/storage/racks?biorepositoryOnly=true").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, Object>> racks = readMapList(racksResult.getResponse().getContentAsString());
        assertFalse("Expected at least one biorepository rack", racks.isEmpty());
        assertTrue("All returned racks must inherit biorepository storage flag",
                racks.stream().allMatch(rack -> Boolean.TRUE.equals(rack.get("biorepositoryStorage"))));
    }

    @Test
    public void testGetRoomAssignableDepartments_UsesLinkedNotebookDepartments() throws Exception {
        NoteBook bacteriologyTemplate = new NoteBook();
        bacteriologyTemplate.setId(4);
        bacteriologyTemplate.setTitle("Bacteriology Laboratory");
        TestSection bacteriology = new TestSection();
        bacteriology.setId("168");
        bacteriology.setTestSectionName("Bacteriology");
        bacteriologyTemplate.setDepartments(new HashSet<>(List.of(bacteriology)));

        NoteBook immunologyTemplate = new NoteBook();
        immunologyTemplate.setId(1);
        immunologyTemplate.setTitle("Immunology Laboratory");
        TestSection immunology = new TestSection();
        immunology.setId("59");
        immunology.setTestSectionName("Immunology");
        immunologyTemplate.setDepartments(new HashSet<>(List.of(immunology)));

        NoteBook virologyTemplate = new NoteBook();
        virologyTemplate.setId(11);
        virologyTemplate.setTitle("Virology Laboratory");
        TestSection virology = new TestSection();
        virology.setId("76");
        virology.setTestSectionName("Virologie");
        virologyTemplate.setDepartments(new HashSet<>(List.of(virology)));

        NoteBookService noteBookServiceMock = Mockito.mock(NoteBookService.class);
        NotebookSecurityService notebookSecurityServiceMock = Mockito.mock(NotebookSecurityService.class);
        DepartmentIsolationService departmentIsolationServiceMock = Mockito.mock(DepartmentIsolationService.class);
        TestSectionService testSectionServiceMock = Mockito.mock(TestSectionService.class);

        when(noteBookServiceMock.getAllTemplateNoteBooks())
                .thenReturn(List.of(bacteriologyTemplate, immunologyTemplate, virologyTemplate));
        when(noteBookServiceMock.convertToDisplayBean(anyInt())).thenReturn(null);
        when(notebookSecurityServiceMock.canViewTemplate(anyInt(), anyString(), any())).thenReturn(true);
        when(departmentIsolationServiceMock.hasUnrestrictedDepartmentAccess(any())).thenReturn(true);
        when(testSectionServiceMock.getTestSectionById("168")).thenReturn(bacteriology);
        when(testSectionServiceMock.getTestSectionById("59")).thenReturn(immunology);
        when(testSectionServiceMock.getTestSectionById("76")).thenReturn(virology);

        ReflectionTestUtils.setField(storageLocationRestController, "noteBookService", noteBookServiceMock);
        ReflectionTestUtils.setField(storageLocationRestController, "notebookSecurityService",
                notebookSecurityServiceMock);
        ReflectionTestUtils.setField(storageLocationRestController, "departmentIsolationService",
                departmentIsolationServiceMock);
        ReflectionTestUtils.setField(storageLocationRestController, "testSectionService", testSectionServiceMock);

        MvcResult mvcResult = this.mockMvc
                .perform(get("/rest/storage/room-assignable-departments").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, String>> departments = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<List<Map<String, String>>>() {
                });

        assertTrue("Expected Bacteriology Laboratory in assignable departments",
                departments.stream().anyMatch(row -> "168".equals(row.get("id"))
                        && "Bacteriology Laboratory".equals(row.get("value"))));
        assertTrue("Expected Immunology Laboratory in assignable departments",
                departments.stream().anyMatch(row -> "59".equals(row.get("id"))
                        && "Immunology Laboratory".equals(row.get("value"))));
        assertTrue("Expected Virology Laboratory in assignable departments",
                departments.stream().anyMatch(row -> "76".equals(row.get("id"))
                        && "Virology Laboratory".equals(row.get("value"))));
    }

    @Test
    public void testGetBoxes_WithBiorepositoryOnly_ReturnsOnlyBoxesUnderBiorepositoryDevices() throws Exception {
        // Rack 20000 -> Shelf 20001 -> Device 20003
        jdbcTemplate.execute("UPDATE clinlims.storage_device SET biorepository_storage = true WHERE id = 20003");
        jdbcTemplate.execute("INSERT INTO clinlims.storage_box (id, label, code, type, rows, columns, parent_rack_id, active, fhir_uuid, sys_user_id, last_updated) "
                + "VALUES (20999, 'Bio Box', 'BIO-BOX-1', 'plate', 8, 12, 20000, true, '40000000-0000-0000-0000-000000020999', '1', CURRENT_TIMESTAMP)");

        MvcResult mvcResult = this.mockMvc
                .perform(get("/rest/storage/boxes?biorepositoryOnly=true").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk()).andReturn();

        List<Map<String, Object>> boxes = readMapList(mvcResult.getResponse().getContentAsString());
        assertFalse("Expected at least one biorepository box", boxes.isEmpty());
        assertTrue("All returned boxes must belong to the flagged biorepository device",
                boxes.stream().allMatch(box -> Integer.valueOf(20003).equals(asInteger(box.get("parentDeviceId")))));
    }

    private List<Map<String, Object>> readMapList(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    @Test
    public void testDeleteRoom_AsNonAdminUser_Returns403() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(false);

        this.mockMvc.perform(delete("/rest/storage/rooms/20006").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteRoom_AsAdminUser_Returns204() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(true);

        this.mockMvc.perform(delete("/rest/storage/rooms/20007").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteDevice_AsNonAdminUser_Returns403() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(false);

        this.mockMvc.perform(delete("/rest/storage/devices/20009").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteShelf_AsNonAdminUser_Returns403() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(false);

        this.mockMvc.perform(delete("/rest/storage/shelves/20012").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteRack_AsNonAdminUser_Returns403() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(false);

        this.mockMvc.perform(delete("/rest/storage/racks/20016").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isForbidden());
    }
}
