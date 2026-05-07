package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.controller.rest.EquipmentUsageRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Proves department-scope isolation on EquipmentUsageRestController.
 * Restricted user active in dept A must not record or read dept B equipment usage.
 */
@RunWith(MockitoJUnitRunner.class)
public class CrossDepartmentEquipmentIsolationTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "3";

    @Mock private InventoryUsageService usageService;
    @Mock private InventoryItemService inventoryItemService;
    @Mock private RbacPermissionService rbacPermissionService;
    @Mock private RbacAuditService rbacAuditService;
    @Mock private SystemUserService systemUserService;
    @Mock private TestSectionService testSectionService;

    @InjectMocks
    private EquipmentUsageRestController controller;

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

    // --- recordEquipmentUsage ---

    @Test
    public void recordUsage_ownDeptEquipment_succeeds() throws Exception {
        when(inventoryItemService.get(1L)).thenReturn(item(1L, DEPT_A));
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "CREATE", DEPT_A)).thenReturn(true);
        InventoryUsage usage = usage(1L, item(1L, DEPT_A));
        when(usageService.recordEquipmentUsage(any(), eq(1L), any(), any(), any())).thenReturn(usage);

        EquipmentUsageRestController.EquipmentUsageRequest req = new EquipmentUsageRestController.EquipmentUsageRequest();
        req.itemId = 1L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    public void recordUsage_otherDeptEquipment_returns403() {
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "CREATE", DEPT_B)).thenReturn(true);
        // Permission passes but active dept check blocks it

        EquipmentUsageRestController.EquipmentUsageRequest req = new EquipmentUsageRestController.EquipmentUsageRequest();
        req.itemId = 2L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void recordUsage_otherDeptEquipment_permissionDenied_returns403() {
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "CREATE", DEPT_B)).thenReturn(false);

        EquipmentUsageRestController.EquipmentUsageRequest req = new EquipmentUsageRestController.EquipmentUsageRequest();
        req.itemId = 2L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // --- history ---

    @Test
    public void history_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);
        when(usageService.getAll()).thenReturn(Arrays.asList(
                usage(1L, item(1L, DEPT_A)),
                usage(2L, item(2L, DEPT_B))));

        ResponseEntity<?> resp = controller.getAllEquipmentUsageHistory(null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) resp.getBody();
        assertEquals(1, body.size());
    }

    @Test
    public void history_admin_seesAllDepts() {
        RbacContext.clear();
        RbacContext.set(USER_ID, "admin", "127.0.0.1", null, null, null);
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);
        when(usageService.getAll()).thenReturn(Arrays.asList(
                usage(1L, item(1L, DEPT_A)),
                usage(2L, item(2L, DEPT_B))));

        ResponseEntity<?> resp = controller.getAllEquipmentUsageHistory(null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) resp.getBody();
        assertEquals(2, body.size());
    }

    // --- submissions ---

    @Test
    public void submissions_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);
        when(usageService.getAll()).thenReturn(Arrays.asList(
                usage(1L, item(1L, DEPT_A)),
                usage(2L, item(2L, DEPT_B))));

        ResponseEntity<?> resp = controller.getEquipmentUsageSubmissions(null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) resp.getBody();
        assertEquals(1, body.size());
    }

    // --- item/{itemId} history ---

    @Test
    public void itemHistory_otherDeptItem_returns403() {
        when(inventoryItemService.get(2L)).thenReturn(item(2L, DEPT_B));
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ", DEPT_B)).thenReturn(true);
        // Active dept check blocks it

        ResponseEntity<?> resp = controller.getEquipmentUsageHistory(2L, null, null, request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void itemHistory_ownDeptItem_succeeds() {
        when(inventoryItemService.get(1L)).thenReturn(item(1L, DEPT_A));
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ", DEPT_A)).thenReturn(true);
        when(usageService.getByInventoryItemId(1L)).thenReturn(List.of(usage(1L, item(1L, DEPT_A))));

        ResponseEntity<?> resp = controller.getEquipmentUsageHistory(1L, null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // --- metrics ---

    @Test
    public void metrics_filtersToActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);
        when(usageService.getAll()).thenReturn(Arrays.asList(
                usage(1L, item(1L, DEPT_A)),
                usage(2L, item(2L, DEPT_B))));
        when(inventoryItemService.getAllActive()).thenReturn(List.of());

        ResponseEntity<?> resp = controller.getEquipmentUsageMetrics(null, null, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        EquipmentUsageRestController.EquipmentUsageMetricsDTO dto =
                (EquipmentUsageRestController.EquipmentUsageMetricsDTO) resp.getBody();
        // Only dept A usage counted
        assertEquals(1, (int) dto.getTotalUsageRecords());
    }

    // --- helpers ---

    private InventoryItem item(Long id, String deptId) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Item-" + id);
        item.setDepartmentId(deptId);
        item.setItemType(ItemType.CARTRIDGE);
        return item;
    }

    private InventoryUsage usage(Long id, InventoryItem item) {
        InventoryLot lot = new InventoryLot();
        lot.setId(1L);
        lot.setLotNumber("LOT-001");

        InventoryUsage u = new InventoryUsage();
        u.setId(id);
        u.setInventoryItem(item);
        u.setLot(lot);
        u.setQuantityUsed(1.0);
        u.setUsageDate(new Timestamp(System.currentTimeMillis()));
        u.setPerformedByUser(Integer.parseInt(USER_ID));
        return u;
    }
}
