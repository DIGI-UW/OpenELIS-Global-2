package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;

/**
 * OGC-658 Part C — unit coverage for
 * {@code InventoryItemServiceImpl.insert()}'s server-side code
 * generation/collision/truncation, mirroring the Mockito pattern already used
 * for {@code InventoryItemTypeServiceTest}.
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
        when(inventoryItemDAO.insert(any(InventoryItem.class)))
                .thenAnswer(invocation -> ((InventoryItem) invocation.getArgument(0)).getId());
        when(inventoryItemDAO.getAllMatchingOrdered(anyString(), any(), anyList(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String code = (String) invocation.getArgument(1);
                    return existingCodes.contains(code) ? List.of(new InventoryItem()) : Collections.emptyList();
                });
    }

    private InventoryItem newItem(String name) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setUnits("mL");
        item.setItemType("REAGENT");
        item.setIsActive("Y");
        return item;
    }

    @Test
    public void insert_generatesCodeFromName_whenIdBlank() {
        InventoryItem item = newItem("Blood Culture Bottle");

        String code = inventoryItemService.insert(item);

        assertEquals("BLOOD_CULTURE_BOTTLE", code);
        assertEquals("BLOOD_CULTURE_BOTTLE", item.getId());
    }

    @Test
    public void insert_generatesCollisionSuffixedCode_whenBaseCodeTaken() {
        existingCodes.add("REAGENT_X");
        existingCodes.add("REAGENT_X_2");

        String code = inventoryItemService.insert(newItem("Reagent X"));

        assertEquals("REAGENT_X_3", code);
    }

    @Test
    public void insert_normalizesExplicitCode() {
        InventoryItem item = newItem("Reagent Y");
        item.setId(" my-code! ");

        String code = inventoryItemService.insert(item);

        assertEquals("MYCODE", code);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void insert_throws_whenExplicitCodeAlreadyExists() {
        existingCodes.add("REAGENT_Z");
        InventoryItem item = newItem("Reagent Z");
        item.setId("REAGENT_Z");

        inventoryItemService.insert(item);
    }

    @Test
    public void insert_truncatesGeneratedCode_toFitColumnLength() {
        String longName = "A".repeat(100);

        String code = inventoryItemService.insert(newItem(longName));

        assertEquals(64, code.length());
        assertTrue(code.chars().allMatch(c -> c == 'A'));
    }

    @Test
    public void insert_truncatesAndSuffixes_whenLongNameCollides() {
        String base = "A".repeat(64);
        existingCodes.add(base);

        String code = inventoryItemService.insert(newItem("A".repeat(100)));

        assertEquals(64, code.length());
        assertTrue("Should end with a collision suffix", code.endsWith("_2"));
    }
}
