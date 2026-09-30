package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.inventory.dao.InventoryItemCodeSequenceDAO;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

/** {@code InventoryItemServiceImpl.insert()} fills in the item's code. */
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemServiceCodeGenerationTest {

    @Mock
    private InventoryItemDAO inventoryItemDAO;

    @Mock
    private InventoryItemCodeSequenceDAO codeSequenceDAO;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private UserContextHolder userContextHolder;

    @InjectMocks
    private InventoryItemServiceImpl inventoryItemService;

    private final Set<String> existingCodes = new HashSet<>();

    // Stands in for the per-prefix counter row: every call hands out the next
    // value.
    private final AtomicLong counter = new AtomicLong(1);

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
    public void insert_generatesPrefixAndPaddedCounter_whenCodeBlank() {
        when(codeSequenceDAO.nextValue("PAR-500MG")).thenAnswer(invocation -> counter.getAndIncrement());

        InventoryItem first = newItem("Paracetamol 500mg Tablets");
        InventoryItem second = newItem("Paracetamol 500mg Tablets");
        inventoryItemService.insert(first);
        inventoryItemService.insert(second);

        assertEquals("PAR-500MG-001", first.getCode());
        assertEquals("PAR-500MG-002", second.getCode());
    }

    @Test
    public void insert_padsToThreeDigits_butLetsTheCounterGrowPastThem() {
        when(codeSequenceDAO.nextValue("HIV-12")).thenReturn(7L, 1000L);

        InventoryItem seventh = newItem("HIV 1/2 Rapid Test Kit");
        InventoryItem thousandth = newItem("HIV 1/2 Rapid Test Kit");
        inventoryItemService.insert(seventh);
        inventoryItemService.insert(thousandth);

        assertEquals("HIV-12-007", seventh.getCode());
        assertEquals("HIV-12-1000", thousandth.getCode());
    }

    @Test
    public void insert_takesTheNextCounterValue_whenAStoredCodeHoldsTheSlot() {
        existingCodes.add("SOD-09-500ML-001");
        when(codeSequenceDAO.nextValue("SOD-09-500ML")).thenAnswer(invocation -> counter.getAndIncrement());

        InventoryItem item = newItem("Sodium Chloride 0.9% 500mL");
        inventoryItemService.insert(item);

        assertEquals("SOD-09-500ML-002", item.getCode());
    }

    @Test
    public void insert_givesUp_whenEveryCounterValueIsTaken() {
        when(codeSequenceDAO.nextValue("ITEM")).thenReturn(1L);
        existingCodes.add("ITEM-001");

        try {
            inventoryItemService.insert(newItem("   "));
            fail("Expected a LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("inventory.item.error.codeGenerationExhausted", e.getErrorCode());
        }
        verify(inventoryItemDAO, never()).insert(any(InventoryItem.class));
    }

    @Test
    public void insert_normalizesExplicitCode_withoutTouchingTheCounter() {
        InventoryItem item = newItem("Reagent Y");
        item.setCode(" my-code! ");

        inventoryItemService.insert(item);

        assertEquals("MY-CODE", item.getCode());
        verify(codeSequenceDAO, never()).nextValue(anyString());
    }

    @Test
    public void insert_rejectsExplicitCode_thatNormalizesToNothing() {
        InventoryItem item = newItem("Reagent W");
        item.setCode("___");

        try {
            inventoryItemService.insert(item);
            fail("Expected a LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("common.codeGenerator.error.invalidCode", e.getErrorCode());
            assertEquals("Code must contain at least one letter or number", e.getMessage());
        }
    }

    @Test(expected = LocalizedValidationException.class)
    public void insert_throws_whenExplicitCodeAlreadyExists() {
        existingCodes.add("REAGENT-Z");
        InventoryItem item = newItem("Reagent Z");
        item.setCode("REAGENT-Z");

        inventoryItemService.insert(item);
    }

    @Test
    public void insert_truncatesExplicitCode_toFitColumnLength() {
        InventoryItem item = newItem("Reagent V");
        item.setCode("A".repeat(100));

        inventoryItemService.insert(item);

        assertEquals("A".repeat(64), item.getCode());
    }
}
