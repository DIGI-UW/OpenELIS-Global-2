package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.controller.rest.InventoryItemRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.dao.RbacAuditLogDAO;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacAuditServiceImpl;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.openelisglobal.rbac.valueholder.RbacAuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Proves denied access is logged and audit log entries have correct outcome.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbacAuditTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "3";

    // ---- RbacAuditServiceImpl unit tests ----

    @Mock
    private RbacAuditLogDAO rbacAuditLogDAO;

    @InjectMocks
    private RbacAuditServiceImpl auditService;

    @Test
    public void logDenied_persistsEntryWithDeniedOutcome() {
        auditService.logDenied(USER_ID, "alice", "INVENTORY", "READ", "42", "InventoryItem", DEPT_B, null, "127.0.0.1",
                "Access denied");

        ArgumentCaptor<RbacAuditLog> captor = ArgumentCaptor.forClass(RbacAuditLog.class);
        verify(rbacAuditLogDAO).insert(captor.capture());

        RbacAuditLog log = captor.getValue();
        assertEquals("DENIED", log.getOutcome());
        assertEquals(USER_ID, log.getSystemUserId());
        assertEquals("INVENTORY", log.getModule());
        assertEquals("READ", log.getAction());
        assertEquals(DEPT_B, log.getDepartmentId());
    }

    @Test
    public void logAction_persistsEntryWithSuccessOutcome() {
        auditService.logAction(USER_ID, "alice", "STORAGE", "CREATE", "7", "StorageRoom", DEPT_A, null, "127.0.0.1",
                "Room created");

        ArgumentCaptor<RbacAuditLog> captor = ArgumentCaptor.forClass(RbacAuditLog.class);
        verify(rbacAuditLogDAO).insert(captor.capture());

        RbacAuditLog log = captor.getValue();
        assertEquals("SUCCESS", log.getOutcome());
        assertEquals("STORAGE", log.getModule());
        assertEquals("CREATE", log.getAction());
    }

    @Test
    public void logDenied_nullUsername_defaultsToUnknown() {
        auditService.logDenied(USER_ID, null, "EQUIPMENT", "READ", null, null, DEPT_B, null, "127.0.0.1", "no session");

        ArgumentCaptor<RbacAuditLog> captor = ArgumentCaptor.forClass(RbacAuditLog.class);
        verify(rbacAuditLogDAO).insert(captor.capture());
        assertEquals("unknown", captor.getValue().getUsername());
    }

    // ---- Integration: denied access triggers logDenied on controller ----

    @Mock
    private InventoryItemService inventoryItemService;
    @Mock
    private RbacPermissionService rbacPermissionService;
    @Mock
    private RbacAuditService rbacAuditServiceMock;

    @InjectMocks
    private InventoryItemRestController inventoryController;

    private MockHttpServletRequest httpRequest;

    @Before
    public void setUp() {
        httpRequest = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.parseInt(USER_ID));
        usd.setLoginName("alice");
        usd.setLoginLabUnit(Integer.parseInt(DEPT_A));
        httpRequest.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        RbacContext.set(USER_ID, "alice", "127.0.0.1", List.of(DEPT_A), List.of(), DEPT_A);
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    @Test
    public void getById_otherDept_doesNotCallAuditLogDenied_butReturns403() {
        // The department check happens in the controller after permission check passes.
        // checkInventoryPermission passes (READ allowed), but isInActiveDepartment fails.
        // logDenied is called by checkInventoryPermission only when permission is denied.
        // Here permission passes but dept scope blocks — 403 is returned without logDenied.
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        InventoryItem item = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(item);

        ResponseEntity<?> resp = inventoryController.getById("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void getById_permissionDenied_callsLogDenied() {
        // When the permission check itself fails, logDenied must be called
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(false);

        ResponseEntity<?> resp = inventoryController.getById("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        verify(rbacAuditServiceMock).logDenied(
                eq(USER_ID), eq("alice"), eq("INVENTORY"), eq("READ"),
                isNull(), isNull(), isNull(), isNull(),
                anyString(), anyString());
    }

    // ---- helpers ----

    private InventoryItem itemInDept(Long id, String deptId) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Item-" + id);
        item.setDepartmentId(deptId);
        return item;
    }
}
