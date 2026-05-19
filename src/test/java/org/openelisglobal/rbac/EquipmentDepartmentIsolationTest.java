package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import org.openelisglobal.inventory.controller.rest.EquipmentUsageRestController.EquipmentUsageRequest;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryUsageService;
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
 * Proves equipment usage endpoints enforce active-department isolation.
 */
@RunWith(MockitoJUnitRunner.class)
public class EquipmentDepartmentIsolationTest {

    private static final String DEPT_A = "10";
    private static final String DEPT_B = "20";
    private static final String USER_ID = "9";

    @Mock
    private InventoryUsageService usageService;
    @Mock
    private InventoryItemService inventoryItemService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private RbacPermissionService rbacPermissionService;
    @Mock
    private RbacAuditService rbacAuditService;

    @InjectMocks
    private EquipmentUsageRestController controller;

    private MockHttpServletRequest httpRequest;

    @Before
    public void setUp() {
        httpRequest = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.parseInt(USER_ID));
        usd.setLoginName("biomedical");
        usd.setLoginLabUnit(Integer.parseInt(DEPT_A));
        httpRequest.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        RbacContext.set(USER_ID, "biomedical", "127.0.0.1", List.of(DEPT_A), List.of(), DEPT_A);
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    // ---- record usage ----

    @Test
    public void recordUsage_equipmentInOtherDept_returns403() {
        InventoryItem item = itemInDept(5L, DEPT_B);
        when(inventoryItemService.get(5L)).thenReturn(item);
        // permission check for DEPT_B returns false
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "CREATE", DEPT_B)).thenReturn(false);

        EquipmentUsageRequest req = new EquipmentUsageRequest();
        req.itemId = 5L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void recordUsage_equipmentInOwnDept_proceeds() {
        InventoryItem item = itemInDept(1L, DEPT_A);
        when(inventoryItemService.get(1L)).thenReturn(item);
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "CREATE", DEPT_A)).thenReturn(true);

        InventoryUsage usage = usageFor(item);
        when(usageService.recordEquipmentUsage(anyLong(), anyLong(), anyDouble(), any(), any())).thenReturn(usage);

        EquipmentUsageRequest req = new EquipmentUsageRequest();
        req.itemId = 1L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, httpRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    // ---- history filtered by dept ----

    @Test
    public void getHistory_returnsOnlyActiveDeptUsage() {
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);

        InventoryItem ownItem = itemInDept(1L, DEPT_A);
        InventoryItem otherItem = itemInDept(2L, DEPT_B);
        InventoryUsage ownUsage = usageFor(ownItem);
        InventoryUsage otherUsage = usageFor(otherItem);
        when(usageService.getAll()).thenReturn(List.of(ownUsage, otherUsage));

        @SuppressWarnings("unchecked")
        ResponseEntity<List<EquipmentUsageRestController.InventoryUsageDTO>> resp =
                (ResponseEntity<List<EquipmentUsageRestController.InventoryUsageDTO>>)
                        controller.getAllEquipmentUsageHistory(null, null, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Only DEPT_A usage should be returned", 1, resp.getBody().size());
    }

    // ---- item/{itemId} blocked for other dept ----

    @Test
    public void getItemHistory_otherDeptItem_returns403() {
        InventoryItem item = itemInDept(2L, DEPT_B);
        when(inventoryItemService.get(2L)).thenReturn(item);
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ", DEPT_B)).thenReturn(false);

        ResponseEntity<?> resp = controller.getEquipmentUsageHistory(2L, null, null, httpRequest);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void getItemHistory_ownDeptItem_returns200() {
        InventoryItem item = itemInDept(1L, DEPT_A);
        when(inventoryItemService.get(1L)).thenReturn(item);
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ", DEPT_A)).thenReturn(true);
        when(usageService.getByInventoryItemId(1L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.getEquipmentUsageHistory(1L, null, null, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // ---- metrics filtered ----

    @Test
    public void getMetrics_filteredByActiveDept() {
        when(rbacPermissionService.hasPermission(USER_ID, "EQUIPMENT", "READ")).thenReturn(true);

        InventoryItem ownItem = itemInDept(1L, DEPT_A);
        InventoryItem otherItem = itemInDept(2L, DEPT_B);
        when(usageService.getAll()).thenReturn(List.of(usageFor(ownItem), usageFor(otherItem)));
        when(inventoryItemService.getAllActive()).thenReturn(List.of());

        ResponseEntity<?> resp = controller.getEquipmentUsageMetrics(null, null, httpRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        EquipmentUsageRestController.EquipmentUsageMetricsDTO dto =
                (EquipmentUsageRestController.EquipmentUsageMetricsDTO) resp.getBody();
        assertEquals("Metrics must count only active-dept usage", 1, (int) dto.getTotalUsageRecords());
    }

    // ---- no session ----

    @Test
    public void recordUsage_noSession_returns401() {
        MockHttpServletRequest noSession = new MockHttpServletRequest();
        EquipmentUsageRequest req = new EquipmentUsageRequest();
        req.itemId = 1L;
        req.lotId = 1L;
        req.quantity = 1.0;

        ResponseEntity<?> resp = controller.recordEquipmentUsage(req, noSession);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ---- helpers ----

    private InventoryItem itemInDept(Long id, String deptId) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Item-" + id);
        item.setDepartmentId(deptId);
        return item;
    }

    private InventoryUsage usageFor(InventoryItem item) {
        InventoryLot lot = new InventoryLot();
        lot.setId(1L);
        lot.setLotNumber("LOT-001");

        InventoryUsage u = new InventoryUsage();
        u.setId(item.getId() * 100);
        u.setInventoryItem(item);
        u.setLot(lot);
        u.setQuantityUsed(1.0);
        return u;
    }
}
