package org.openelisglobal.storage.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
