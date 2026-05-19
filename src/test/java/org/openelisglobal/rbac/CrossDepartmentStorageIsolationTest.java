package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
 * Proves department-scope isolation on StorageLocationRestController.
 * Restricted user active in dept A must not read or mutate dept B storage.
 */
@RunWith(MockitoJUnitRunner.class)
public class CrossDepartmentStorageIsolationTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "3";

    @Mock private StorageLocationService storageLocationService;
    @Mock private StorageDashboardService storageDashboardService;
    @Mock private StorageSearchService storageSearchService;
    @Mock private StorageRoomDAO storageRoomDAO;
    @Mock private StorageDeviceDAO storageDeviceDAO;
    @Mock private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;
    @Mock private UserModuleService userModuleService;
    @Mock private UserRoleService userRoleService;
    @Mock private RbacPermissionService rbacPermissionService;
    @Mock private RbacAuditService rbacAuditService;

    @InjectMocks
    private StorageLocationRestController controller;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.parseInt(USER_ID));
        usd.setLoginName("alice");
        usd.setLoginLabUnit(Integer.parseInt(DEPT_A));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        // Restricted user active in dept A
        RbacContext.set(USER_ID, "alice", "127.0.0.1", List.of(DEPT_A), List.of(), DEPT_A);
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    // --- createRoom ---

    @Test
    public void createRoom_restrictedUser_deptAnchoredFromContext() {
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "CREATE", DEPT_A)).thenReturn(true);
        when(storageLocationService.isNameUniqueWithinParent("Lab A", null, "room", null)).thenReturn(true);
        StorageRoom created = room(1, DEPT_A);
        when(storageLocationService.createRoom(any())).thenReturn(created);
        // storageRoomDAO.findByCode used for uniqueness check — return null = unique
        when(storageRoomDAO.findByCode(any())).thenReturn(null);

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Lab A");
        form.setDepartmentId(DEPT_B); // client sends dept B — must be overridden

        ResponseEntity<?> resp = controller.createRoom(form, request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        // Room must be created with dept A (anchored), not dept B
        verify(storageLocationService).createRoom(argThat(r -> DEPT_A.equals(r.getDepartmentId())));
    }

    @Test
    public void createRoom_adminWithNullDept_returns400() {
        // Admin (unrestricted) must explicitly choose a department
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null);

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Lab X");
        form.setDepartmentId(null); // no dept chosen

        ResponseEntity<?> resp = controller.createRoom(form, request);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // --- getRooms ---

    @Test
    public void getRooms_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);
        when(storageLocationService.getRoomsForAPI()).thenReturn(Arrays.asList(
                roomMap(1, DEPT_A),
                roomMap(2, DEPT_B)));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> resp =
                (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) controller.getRooms(null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals(DEPT_A, resp.getBody().get(0).get("departmentId"));
    }

    @Test
    public void getRooms_admin_seesAllDepts() {
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null);
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);
        when(storageLocationService.getRoomsForAPI()).thenReturn(Arrays.asList(
                roomMap(1, DEPT_A),
                roomMap(2, DEPT_B)));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> resp =
                (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) controller.getRooms(null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().size());
    }

    // --- getRoomById ---

    @Test
    public void getRoomById_ownDept_returns200() {
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);
        when(storageLocationService.getRoom(1)).thenReturn(room(1, DEPT_A));

        ResponseEntity<?> resp = controller.getRoomById("1", request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void getRoomById_otherDept_returns403() {
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "READ")).thenReturn(true);
        when(storageLocationService.getRoom(2)).thenReturn(room(2, DEPT_B));

        ResponseEntity<?> resp = controller.getRoomById("2", request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // --- updateRoom ---

    @Test
    public void updateRoom_otherDept_returns403() {
        when(storageLocationService.getRoom(2)).thenReturn(room(2, DEPT_B));

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Updated");

        ResponseEntity<?> resp = controller.updateRoom("2", form, request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void updateRoom_ownDept_succeeds() {
        when(storageLocationService.getRoom(1)).thenReturn(room(1, DEPT_A));
        when(rbacPermissionService.hasPermission(USER_ID, "STORAGE", "UPDATE", DEPT_A)).thenReturn(true);
        when(storageLocationService.isNameUniqueWithinParent("Updated", null, "room", 1)).thenReturn(true);
        when(storageLocationService.updateRoom(eq(1), any())).thenReturn(room(1, DEPT_A));

        StorageRoomForm form = new StorageRoomForm();
        form.setName("Updated");

        ResponseEntity<?> resp = controller.updateRoom("1", form, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // --- helpers ---

    private StorageRoom room(Integer id, String deptId) {
        StorageRoom r = new StorageRoom();
        r.setId(id);
        r.setName("Room-" + id);
        r.setCode("R" + id);
        r.setActive(true);
        r.setDepartmentId(deptId);
        r.setSysUserId("1");
        r.setFhirUuid(java.util.UUID.randomUUID());
        return r;
    }

    private Map<String, Object> roomMap(Integer id, String deptId) {
        return Map.of("id", id, "name", "Room-" + id, "departmentId", deptId);
    }
}
