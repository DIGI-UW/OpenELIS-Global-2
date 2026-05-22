package org.openelisglobal.inventory.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class InventoryItemRestControllerDepartmentIsolationTest {

    @InjectMocks
    private InventoryItemRestController controller;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private RbacPermissionService rbacPermissionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void createReturnsBadRequestWhenStrictDepartmentCannotBeResolved() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Test Reagent");
        item.setItemType(org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType.REAGENT);
        item.setProjectName("Other Department Project");

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(any())).thenReturn(false);
        when(departmentIsolationService.resolveDepartmentForStrictScopedCreate(any(), any(), any())).thenReturn(null);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(any())).thenReturn(java.util.Set.of(7));

        mockMvc.perform(post("/rest/inventory/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createReturnsBadRequestWhenProjectBelongsToDifferentDepartment() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Test Reagent");
        item.setItemType(org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType.REAGENT);
        item.setDepartmentTestSectionId(7);
        item.setProjectName("Other Department Project");

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(departmentIsolationService.resolveDepartmentForStrictScopedCreate(any(), any(), any())).thenReturn(7);
        when(departmentIsolationService.isInventoryProjectConsistent(7, "Other Department Project")).thenReturn(false);

        mockMvc.perform(post("/rest/inventory/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createReturnsForbiddenWhenRbacDenied() throws Exception {
        org.mockito.Mockito.when(rbacPermissionService.hasPermission(any(), any(RbacAction.class)))
                .thenReturn(false);

        InventoryItem item = new InventoryItem();
        item.setName("Test Reagent");
        item.setItemType(org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType.REAGENT);
        item.setDepartmentTestSectionId(7);

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(departmentIsolationService.resolveDepartmentForStrictScopedCreate(any(), any(), any())).thenReturn(7);
        when(departmentIsolationService.isInventoryProjectConsistent(7, null)).thenReturn(true);
        when(departmentIsolationService.canAccessInventoryItemStrictIntersection(any(), any())).thenReturn(true);

        mockMvc.perform(post("/rest/inventory/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isForbidden());
    }
}
