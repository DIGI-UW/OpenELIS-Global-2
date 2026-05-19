package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.openelisglobal.storage.controller.StorageLocationRestController;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.form.StorageRoomForm;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.service.StorageSearchService;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Proves storage endpoints enforce active-department isolation. A user active
 * in DEPT_A must not list, read, or mutate DEPT_B storage.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageDepartmentIsolationTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "7";

    @Mock
    private StorageLocationService storageLocationService;
    @Mock
    private StorageDashboardService storageDashboardService;
    @Mock
    private StorageSearchService storageSearchService;
    @Mock
    private UserModuleService userModuleService;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private RbacPermissionService rbacPermissionService;
    @Mock
    private RbacAuditService rbacAuditService;
    @Mock
    private StorageRoomDAO storageRoomDAO;
    @Mock
    private StorageDeviceDAO storageDeviceDAO;
    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @InjectMocks
    private StorageLocationRestController controller;

    private MockHttpServletRequest httpRequest;

    @Before
    public void setUp() {
        httpRequest = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.parseInt(USER_ID));
        usd.setLoginName("labtech");
        usd.setLoginLabUnit(Integer.parseInt(DEPT_A));
        httpRequest.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        // Restricted user active in DEPT_A
        RbacContext.set(USER_ID, "labtech", "127.0.0.1", List.of(DEPT_A), List.of(), DEPT_A);

        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    // ---- list rooms ----

    @Test
    public void getRooms_returnsOnlyActiveDeptRooms() {
        Map<String, Object> roomA = roomMap("1", DEPT_A);
        Map<String, Object> roomB = roomMap("2", DEPT_B);
        when(storageLocationService.getRoomsForAPI()).thenReturn(List.of(roomA, roomB));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> resp = (ResponseEntity<List<Map<String, Object>>>) controller
                .getRooms(null, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Only DEPT_A room should be visible", 1, resp.getBody().size());
        assertEquals(DEPT_A, resp.getBody().get(0).get("departmentId"));
    }

    @Test
    public void getRooms_adminSeesAll() {
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null); // unrestricted

        Map<String, Object> roomA = roomMap("1", DEPT_A);
        Map<String, Object> roomB = roomMap("2", DEPT_B);
        when(storageLocationService.getRoomsForAPI()).thenReturn(List.of(roomA, roomB));
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);

        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> resp = (ResponseEntity<List<Map<String, Object>>>) controller
                .getRooms(null, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Admin must see all rooms", 2, resp.getBody().size());
    }

    // ---- getById ----

    @Test
    public void getRoomById_ownDept_returns200() {
        StorageRoom room = roomEntity(1, DEPT_A);
        when(storageLocationService.getRoom(1)).thenReturn(room);

        ResponseEntity<?> resp = controller.getRoomById("1", httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void getRoomById_otherDept_returns403() {
        StorageRoom room = roomEntity(2, DEPT_B);
        when(storageLocationService.getRoom(2)).thenReturn(room);

        ResponseEntity<?> resp = controller.getRoomById("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ---- createRoom ----

    @Test
    public void createRoom_restrictedUser_departmentAnchoredToActiveDept() {
        StorageRoomForm form = new StorageRoomForm();
        form.setName("New Room");
        form.setDepartmentId(DEPT_B); // client tries to set DEPT_B

        // After anchoring, controller calls hasPermission with DEPT_A
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "CREATE", DEPT_A)).thenReturn(true);

        StorageRoom created = roomEntity(99, DEPT_A);
        when(storageLocationService.isNameUniqueWithinParent(any(), any(), any(), any())).thenReturn(true);
        when(storageLocationService.createRoom(any())).thenReturn(created);

        ResponseEntity<?> resp = controller.createRoom(form, httpRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        // Verify the room passed to service had DEPT_A, not DEPT_B
        verify(storageLocationService).createRoom(argThat(r -> DEPT_A.equals(r.getDepartmentId())));
    }

    @Test
    public void createRoom_adminWithNullDept_returns400() {
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null); // unrestricted

        StorageRoomForm form = new StorageRoomForm();
        form.setName("New Room");
        form.setDepartmentId(null); // admin forgot to set dept

        ResponseEntity<?> resp = controller.createRoom(form, httpRequest);

        assertEquals("Admin must explicitly choose a department", HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ---- updateRoom ----

    @Test
    public void updateRoom_otherDept_returns403() {
        StorageRoom existing = roomEntity(2, DEPT_B);
        when(storageLocationService.getRoom(2)).thenReturn(existing);

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Updated");

        ResponseEntity<?> resp = controller.updateRoom("2", form, httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void updateRoom_ownDept_proceeds() {
        StorageRoom existing = roomEntity(1, DEPT_A);
        when(storageLocationService.getRoom(1)).thenReturn(existing);
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "UPDATE", DEPT_A)).thenReturn(true);
        when(storageLocationService.isNameUniqueWithinParent(any(), any(), any(), any())).thenReturn(true);
        StorageRoom updated = roomEntity(1, DEPT_A);
        when(storageLocationService.updateRoom(eq(1), any())).thenReturn(updated);

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Updated Name");

        ResponseEntity<?> resp = controller.updateRoom("1", form, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ---- helpers ----

    private Map<String, Object> roomMap(String id, String deptId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("departmentId", deptId);
        m.put("name", "Room-" + id);
        return m;
    }

    private StorageRoom roomEntity(int id, String deptId) {
        StorageRoom r = new StorageRoom();
        r.setId(id);
        r.setName("Room-" + id);
        r.setDepartmentId(deptId);
        return r;
    }
}
