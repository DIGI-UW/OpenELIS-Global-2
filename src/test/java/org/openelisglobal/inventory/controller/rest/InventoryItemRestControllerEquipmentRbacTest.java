package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;

@RunWith(MockitoJUnitRunner.class)
public class InventoryItemRestControllerEquipmentRbacTest {

    @InjectMocks
    private InventoryItemRestController controller;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private RbacPermissionService rbacPermissionService;

    @Test
    public void equipmentItemUsesManageEquipmentPermission() throws Exception {
        InventoryItem equipment = new InventoryItem();
        equipment.setItemType(ItemType.EQUIPMENT);
        equipment.setCategory("General");

        RbacAction action = invokeInventoryActionFor(equipment);
        assertEquals(RbacAction.MANAGE_EQUIPMENT, action);
    }

    @Test
    public void reagentWithEquipmentCategoryUsesUpdateSamples() throws Exception {
        InventoryItem reagent = new InventoryItem();
        reagent.setItemType(ItemType.REAGENT);
        reagent.setCategory("Bacteriology Equipment");

        RbacAction action = invokeInventoryActionFor(reagent);
        assertEquals(RbacAction.UPDATE_SAMPLES, action);
    }

    private RbacAction invokeInventoryActionFor(InventoryItem item) throws Exception {
        Method method = InventoryItemRestController.class.getDeclaredMethod("inventoryActionFor", InventoryItem.class);
        method.setAccessible(true);
        return (RbacAction) method.invoke(controller, item);
    }
}
