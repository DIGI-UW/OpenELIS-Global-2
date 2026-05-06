package org.openelisglobal.rbac;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * TR-04/TR-05: Verifies that a user active in department A cannot read or
 * mutate records belonging to department B, even with a direct API call.
 */
@RunWith(MockitoJUnitRunner.class)
public class CrossDepartmentDenyTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "42";

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private RbacPermissionService rbacPermissionService;

    @Mock
    private RbacAuditService rbacAuditService;

    @InjectMocks
    private InventoryItemRestController controller;

    private MockHttpServletRequest httpRequest;

    @Before
    public void setUp() {
        httpRequest = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.parseInt(USER_ID));
        usd.setLoginName("testuser");
        usd.setLoginLabUnit(Integer.parseInt(DEPT_A));
        httpRequest.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        // User is active in DEPT_A — restricted (not unrestricted)
        RbacContext.set(USER_ID, "testuser", "127.0.0.1", List.of(DEPT_A), List.of(), DEPT_A);

        // User has READ permission for INVENTORY in general
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "READ")).thenReturn(true);
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    // --- getById ---

    @Test
    public void getById_itemInOwnDepartment_returns200() {
        InventoryItem item = itemInDept(1L, DEPT_A);
        when(inventoryItemService.get(1L)).thenReturn(item);

        ResponseEntity<?> response = controller.getById("1", httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getById_itemInOtherDepartment_returns403() {
        InventoryItem item = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(item);

        ResponseEntity<?> response = controller.getById("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // --- update ---

    @Test
    public void update_itemInOtherDepartment_returns403() {
        InventoryItem existing = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(existing);

        InventoryItem payload = itemInDept(2L, DEPT_B);
        ResponseEntity<?> response = controller.update("2", payload, httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void update_itemInOwnDepartment_proceeds() {
        InventoryItem existing = itemInDept(1L, DEPT_A);
        when(inventoryItemService.get(1L)).thenReturn(existing);
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "UPDATE", DEPT_A)).thenReturn(true);
        InventoryItem updated = itemInDept(1L, DEPT_A);
        when(inventoryItemService.update(org.mockito.ArgumentMatchers.any())).thenReturn(updated);

        InventoryItem payload = itemInDept(1L, DEPT_A);
        ResponseEntity<?> response = controller.update("1", payload, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- activate / deactivate ---

    @Test
    public void deactivate_itemInOtherDepartment_returns403() {
        InventoryItem existing = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(existing);

        ResponseEntity<?> response = controller.deactivate("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void activate_itemInOtherDepartment_returns403() {
        InventoryItem existing = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(existing);

        ResponseEntity<?> response = controller.activate("2", httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // --- list filtering ---

    @Test
    public void getAllActive_returnsOnlyOwnDepartmentItems() {
        InventoryItem ownItem = itemInDept(1L, DEPT_A);
        InventoryItem otherItem = itemInDept(2L, DEPT_B);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(ownItem, otherItem));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<InventoryItem>> response = (ResponseEntity<List<InventoryItem>>) controller
                .getAllActive(httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(DEPT_A, response.getBody().get(0).getDepartmentId());
    }

    // --- create anchors department server-side ---

    @Test
    public void create_restrictedUser_departmentAnchoredToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "INVENTORY", "CREATE"))
                .thenReturn(true);
        InventoryItem payload = new InventoryItem();
        payload.setName("Test Item");
        payload.setDepartmentId(DEPT_B); // client tries to set dept B

        InventoryItem saved = itemInDept(99L, DEPT_A);
        when(inventoryItemService.insert(org.mockito.ArgumentMatchers.any())).thenReturn(99L);
        when(inventoryItemService.get(99L)).thenReturn(saved);

        ResponseEntity<?> response = controller.create(payload, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // The saved item must be in DEPT_A, not DEPT_B
        InventoryItem result = (InventoryItem) response.getBody();
        assertEquals("department_id must be anchored to active dept, not client payload",
                DEPT_A, result.getDepartmentId());
    }

    // --- helpers ---

    private InventoryItem itemInDept(Long id, String deptId) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Item-" + id);
        item.setDepartmentId(deptId);
        return item;
    }
}
