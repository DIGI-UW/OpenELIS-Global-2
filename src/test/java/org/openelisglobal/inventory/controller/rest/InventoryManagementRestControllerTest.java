package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementRestControllerTest {

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private InventoryManagementRestController controller;

    @Test
    public void receiveInventory_answers404WithBody_whenItemDoesNotExist() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);
        InventoryLot lot = new InventoryLot();
        when(inventoryManagementService.receiveInventory(lot, "7"))
                .thenThrow(new ObjectNotFoundException(999L, InventoryItem.class.getName()));

        ResponseEntity<?> response = controller.receiveInventory(lot, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("InventoryItem 999 not found", ((Map<?, ?>) response.getBody()).get("error"));
    }
}
