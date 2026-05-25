package org.openelisglobal.inventory.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class EquipmentUsageRestControllerRbacTest {

    @InjectMocks
    private EquipmentUsageRestController controller;

    @Mock
    private InventoryUsageService usageService;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private SystemUserService systemUserService;

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
    public void recordUsageReturnsForbiddenWithoutEquipmentPermission() throws Exception {
        EquipmentUsageRestController.EquipmentUsageRequest request = new EquipmentUsageRestController.EquipmentUsageRequest();
        request.setItemId(5L);
        request.setLotId(1L);
        request.setQuantity(1.0);

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        InventoryItem item = new InventoryItem();
        when(inventoryItemService.get(anyLong())).thenReturn(item);
        when(departmentIsolationService.canAccessInventoryItem(eq(item), any())).thenReturn(true);
        when(rbacPermissionService.hasPermission(any(), eq(RbacAction.MANAGE_EQUIPMENT))).thenReturn(false);

        mockMvc.perform(post("/rest/equipment/usage/record")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
