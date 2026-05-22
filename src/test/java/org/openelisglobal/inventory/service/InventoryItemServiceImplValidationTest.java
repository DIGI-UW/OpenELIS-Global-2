package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

@RunWith(MockitoJUnitRunner.class)
public class InventoryItemServiceImplValidationTest {

    @InjectMocks
    private InventoryItemServiceImpl inventoryItemService;

    @Mock
    private InventoryItemDAO inventoryItemDAO;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    private InventoryItem baseItem;

    @Before
    public void setUp() {
        baseItem = new InventoryItem();
        baseItem.setName("Test Item");
        baseItem.setUnits("units");
    }

    @Test
    public void reagentRequiresStabilityAfterOpening() throws Exception {
        baseItem.setItemType(ItemType.REAGENT);
        assertValidationMessage(
                "Stability after opening (in days) is required for reagents and must be greater than 0");
    }

    @Test
    public void equipmentRequiresModelNumber() throws Exception {
        baseItem.setItemType(ItemType.EQUIPMENT);
        baseItem.setEquipmentCondition("functional");
        assertValidationMessage("Model number is required for equipment");
    }

    @Test
    public void cartridgeRejectsEquipmentMetadata() throws Exception {
        baseItem.setItemType(ItemType.CARTRIDGE);
        baseItem.setCompatibleAnalyzers("GeneXpert");
        baseItem.setModelNumber("Model-X");
        assertValidationMessage(
                "Equipment metadata belongs on item type EQUIPMENT, not CARTRIDGE. Use Equipment for instruments.");
    }

    @Test
    public void cartridgeRequiresCompatibleAnalyzers() throws Exception {
        baseItem.setItemType(ItemType.CARTRIDGE);
        assertValidationMessage("Compatible analyzers are required for analyzer cartridges");
    }

    @Test
    public void consumableAllowsMinimalFields() throws Exception {
        baseItem.setItemType(ItemType.CONSUMABLE);
        invokeValidation(baseItem);
    }

    @Test
    public void equipmentAcceptsValidPayload() throws Exception {
        baseItem.setItemType(ItemType.EQUIPMENT);
        baseItem.setModelNumber("QS-3");
        baseItem.setEquipmentCondition("functional");
        invokeValidation(baseItem);
    }

    private void assertValidationMessage(String expected) throws Exception {
        try {
            invokeValidation(baseItem);
            fail("Expected validation failure");
        } catch (IllegalArgumentException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    private void invokeValidation(InventoryItem item) throws Exception {
        Method method = InventoryItemServiceImpl.class.getDeclaredMethod("validateItemTypeSpecificFields",
                InventoryItem.class);
        method.setAccessible(true);
        try {
            method.invoke(inventoryItemService, item);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            throw e;
        }
    }
}
