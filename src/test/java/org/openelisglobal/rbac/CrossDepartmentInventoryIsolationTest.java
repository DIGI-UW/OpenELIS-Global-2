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
import org.openelisglobal.inventory.controller.rest.InventoryItemRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Proves department-scope isolation on InventoryItemRestController.
 * User active in dept A must not read or mutate dept B inventory.
 */
@RunWith(MockitoJUnitRunner.class)
public class CrossDepartmentInventoryIsolationTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "3";

    @Mock
    private InventoryItemService inventoryItemService;
    @Mock
    private RbacPermissionService rbacPermissionService;
    @Mock
    private RbacAuditService rbacAuditService;

    @InjectMocks
    private InventoryItemRestController controller;

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

    // --- getById ---

    @Test
    public void getById_ownDept_returns200() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.get(1L)).thenReturn(item(1L, DEPT_A));

        ResponseEntity<?> resp = controller.getById("1", request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void getById_otherDept_returns403() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));

        ResponseEntity<?> resp = controller.getById("2", request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // --- getAll / getAllActive ---

    @Test
    public void getAllActive_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.getAllActive()).thenReturn(
                Arrays.asList(item(1L, DEPT_A), item(2L, DEPT_B)));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<InventoryItem>> resp =
                (ResponseEntity<List<InventoryItem>>) (ResponseEntity<?>) controller.getAllActive(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals(DEPT_A, resp.getBody().get(0).getDepartmentId());
    }

    @Test
    public void getAll_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.getAll()).thenReturn(
                Arrays.asList(item(1L, DEPT_A), item(2L, DEPT_B), item(3L, DEPT_B)));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<InventoryItem>> resp =
                (ResponseEntity<List<InventoryItem>>) (ResponseEntity<?>) controller.getAll(null, null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    // --- stock ---

    @Test
    public void getStock_otherDept_returns403() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));

        ResponseEntity<?> resp = controller.getTotalStock("2", request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // --- create ---

    @Test
    public void create_anchorsDeptFromContext_notClientPayload() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "CREATE")).thenReturn(true);
        InventoryItem toCreate = item(null, DEPT_B); // client sends dept B
        when(inventoryItemService.insert(any())).thenReturn(1L);
        when(inventoryItemService.get(1L)).thenReturn(item(1L, DEPT_A));

        controller.create(toCreate, request);

        // Verify the item saved has dept A (anchored server-side), not dept B
        verify(inventoryItemService).insert(argThat(i -> DEPT_A.equals(i.getDepartmentId())));
    }

    // --- update ---

    @Test
    public void update_otherDeptItem_returns403() {
        InventoryItem existing = item(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(existing);

        ResponseEntity<?> resp = controller.update("2", item(2L, DEPT_B), request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void update_ownDeptItem_preservesDept() {
        InventoryItem existing = item(1L, DEPT_A);
        when(inventoryItemService.get(1L)).thenReturn(existing);
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "UPDATE", DEPT_A)).thenReturn(true);
        InventoryItem updated = item(1L, DEPT_A);
        when(inventoryItemService.update(any())).thenReturn(updated);

        ResponseEntity<?> resp = controller.update("1", item(1L, DEPT_B), request); // client tries to move to B

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // dept must be preserved as A
        verify(inventoryItemService).update(argThat(i -> DEPT_A.equals(i.getDepartmentId())));
    }

    // --- deactivate / activate ---

    @Test
    public void deactivate_otherDeptItem_returns403() {
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));

        ResponseEntity<?> resp = controller.deactivate("2", request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void activate_otherDeptItem_returns403() {
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));

        ResponseEntity<?> resp = controller.activate("2", request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // --- admin is unrestricted ---

    @Test
    public void admin_seesAllDepts() {
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null); // unrestricted
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.getAllActive()).thenReturn(
                Arrays.asList(item(1L, DEPT_A), item(2L, DEPT_B)));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<InventoryItem>> resp =
                (ResponseEntity<List<InventoryItem>>) (ResponseEntity<?>) controller.getAllActive(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().size());
    }

    // --- paged endpoint passes activeDept to service ---

    @Test
    public void paged_passesActiveDeptToService() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
        when(inventoryItemService.getPagedItems(anyInt(), anyInt(), any(), any(), any(), any(), any(), eq(DEPT_A)))
                .thenReturn(List.of());
        when(inventoryItemService.getPagedItemsCount(any(), any(), any(), eq(DEPT_A))).thenReturn(0L);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> resp =
                (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) controller.getPagedItems(
                        20, 0, "name", "asc", null, null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // Both list and count queries received the same activeDept
        verify(inventoryItemService).getPagedItems(anyInt(), anyInt(), any(), any(), any(), any(), any(), eq(DEPT_A));
        verify(inventoryItemService).getPagedItemsCount(any(), any(), any(), eq(DEPT_A));
    }

    // --- helpers ---

    private InventoryItem item(Long id, String deptId) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Item-" + id);
        item.setDepartmentId(deptId);
        item.setItemType(ItemType.REAGENT);
        return item;
    }
}
