package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

/**
 * OGC-658 Part C — {@code InventoryItemServiceImpl.insert()} fills in the
 * item's human-readable code. The code is a unique column, not the primary key:
 * the id stays a surrogate sequence value, so nothing here renames a key or
 * cascades.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemServiceCodeGenerationTest {

    @Mock
    private InventoryItemDAO inventoryItemDAO;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private UserContextHolder userContextHolder;

    @InjectMocks
    private InventoryItemServiceImpl inventoryItemService;

    private final Set<String> existingCodes = new HashSet<>();

    @Before
    public void setup() {
        existingCodes.clear();
        when(userContextHolder.getCurrentSysUserId()).thenReturn("1");
        when(inventoryItemDAO.insert(any(InventoryItem.class))).thenReturn(1L);
        when(inventoryItemDAO.getByCode(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            if (!existingCodes.contains(code)) {
                return null;
            }
            InventoryItem existing = new InventoryItem();
            existing.setCode(code);
            return existing;
        });
    }

    private InventoryItem newItem(String name) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setUnits("mL");
        item.setItemType(ItemType.REAGENT);
        item.setIsActive("Y");
        return item;
    }

    @Test
    public void insert_generatesCodeFromName_whenCodeBlank() {
        InventoryItem item = newItem("Blood Culture Bottle");

        inventoryItemService.insert(item);

        assertEquals("BLOOD_CULTURE_BOTTLE", item.getCode());
    }

    @Test
    public void insert_leavesSurrogateIdToTheSequence() {
        InventoryItem item = newItem("Blood Culture Bottle");

        inventoryItemService.insert(item);

        assertEquals(Long.valueOf(1L), item.getId());
        assertEquals("BLOOD_CULTURE_BOTTLE", item.getCode());
    }

    @Test
    public void insert_generatesCollisionSuffixedCode_whenBaseCodeTaken() {
        existingCodes.add("REAGENT_X");
        existingCodes.add("REAGENT_X_2");

        InventoryItem item = newItem("Reagent X");
        inventoryItemService.insert(item);

        assertEquals("REAGENT_X_3", item.getCode());
    }

    @Test
    public void insert_normalizesExplicitCode() {
        InventoryItem item = newItem("Reagent Y");
        item.setCode(" my-code! ");

        inventoryItemService.insert(item);

        assertEquals("MY_CODE", item.getCode());
    }

    @Test(expected = LocalizedValidationException.class)
    public void insert_throws_whenExplicitCodeAlreadyExists() {
        existingCodes.add("REAGENT_Z");
        InventoryItem item = newItem("Reagent Z");
        item.setCode("REAGENT_Z");

        inventoryItemService.insert(item);
    }

    @Test
    public void insert_truncatesGeneratedCode_toFitColumnLength() {
        InventoryItem item = newItem("A".repeat(100));

        inventoryItemService.insert(item);

        assertEquals(64, item.getCode().length());
        assertTrue(item.getCode().chars().allMatch(c -> c == 'A'));
    }

    @Test
    public void insert_truncatesAndSuffixes_whenLongNameCollides() {
        existingCodes.add("A".repeat(64));

        InventoryItem item = newItem("A".repeat(100));
        inventoryItemService.insert(item);

        assertEquals(64, item.getCode().length());
        assertTrue("Should end with a collision suffix", item.getCode().endsWith("_2"));
    }
}
