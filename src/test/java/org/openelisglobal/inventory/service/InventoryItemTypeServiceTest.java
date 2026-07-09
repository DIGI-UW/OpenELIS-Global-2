package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryItemTypeDAO;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;

/**
 * OGC-658 Part A — unit coverage for the admin-managed inventory item type
 * service, in particular the code auto-generation/collision-suffixing rules
 * that replace the old hardcoded {@code InventoryEnums.ItemType} enum.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemTypeServiceTest {

    @Mock
    private InventoryItemTypeDAO inventoryItemTypeDAO;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private InventoryItemTypeServiceImpl inventoryItemTypeService;

    @Before
    public void setup() {
        when(inventoryItemTypeDAO.insert(any(InventoryItemType.class))).thenReturn(500L);
        when(inventoryItemTypeDAO.update(any(InventoryItemType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void create_generatesCodeFromName_whenCodeBlank() {
        when(inventoryItemTypeDAO.existsByCode(anyString())).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(null, "Blood Culture Bottle", "en", 10, null, "1");

        assertEquals("BLOOD_CULTURE_BOTTLE", created.getCode());
    }

    @Test
    public void create_generatesCollisionSuffixedCode_whenBaseCodeTaken() {
        when(inventoryItemTypeDAO.existsByCode("REAGENT")).thenReturn(true);
        when(inventoryItemTypeDAO.existsByCode("REAGENT_2")).thenReturn(true);
        when(inventoryItemTypeDAO.existsByCode("REAGENT_3")).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(null, "Reagent", "en", 10, null, "1");

        assertEquals("REAGENT_3", created.getCode());
    }

    @Test
    public void create_normalizesExplicitCode() {
        when(inventoryItemTypeDAO.existsByCode(anyString())).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(" my code! ", "My Code", "en", 10, null, "1");

        assertEquals("MYCODE", created.getCode());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void create_throws_whenNameBlank() {
        inventoryItemTypeService.create("CODE", "   ", "en", 10, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void create_throws_whenExplicitCodeAlreadyExists() {
        when(inventoryItemTypeDAO.existsByCode("REAGENT")).thenReturn(true);

        inventoryItemTypeService.create("REAGENT", "Reagent", "en", 10, null, "1");
    }

    @Test
    public void create_setsLocalizedNameForRequestedLocale() {
        when(inventoryItemTypeDAO.existsByCode(anyString())).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(null, "Stain", "fr", 70, null, "1");

        assertEquals("Stain", created.getNameLocalization().getLocalizedValue("fr"));
    }

    @Test
    public void updateNameAndSortOrder_updatesLocalizationAndSortOrder() {
        Localization localization = new Localization();
        localization.setLocalizedValue("en", "Old Name");
        InventoryItemType existing = new InventoryItemType();
        existing.setId(500L);
        existing.setCode("REAGENT");
        existing.setNameLocalization(localization);
        existing.setSortOrder(10);
        existing.setIsActive(true);
        when(inventoryItemTypeDAO.get(500L)).thenReturn(Optional.of(existing));

        InventoryItemType updated = inventoryItemTypeService.updateNameAndSortOrder(500L, "en", "New Name", 25, "1");

        assertEquals("New Name", updated.getNameLocalization().getLocalizedValue("en"));
        assertEquals(Integer.valueOf(25), updated.getSortOrder());
        verify(localizationService).update(localization);
    }

    @Test
    public void updateNameAndSortOrder_leavesNameUnchanged_whenBlank() {
        Localization localization = new Localization();
        localization.setLocalizedValue("en", "Kept Name");
        InventoryItemType existing = new InventoryItemType();
        existing.setId(500L);
        existing.setNameLocalization(localization);
        existing.setSortOrder(10);
        when(inventoryItemTypeDAO.get(500L)).thenReturn(Optional.of(existing));

        inventoryItemTypeService.updateNameAndSortOrder(500L, "en", "   ", null, "1");

        assertEquals("Kept Name", localization.getLocalizedValue("en"));
        verify(localizationService, never()).update(any());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateNameAndSortOrder_throws_whenTypeNotFound() {
        when(inventoryItemTypeDAO.get(999L)).thenReturn(Optional.empty());

        inventoryItemTypeService.updateNameAndSortOrder(999L, "en", "Name", 10, "1");
    }

    @Test
    public void deactivate_setsInactive() {
        InventoryItemType existing = new InventoryItemType();
        existing.setId(500L);
        existing.setIsActive(true);
        when(inventoryItemTypeDAO.get(500L)).thenReturn(Optional.of(existing));

        InventoryItemType deactivated = inventoryItemTypeService.deactivate(500L, "1");

        assertFalse(deactivated.getIsActive());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void deactivate_throws_whenTypeNotFound() {
        when(inventoryItemTypeDAO.get(999L)).thenReturn(Optional.empty());

        inventoryItemTypeService.deactivate(999L, "1");
    }

    @Test
    public void create_defaultsSortOrder_whenNull() {
        when(inventoryItemTypeDAO.existsByCode(anyString())).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(null, "Consumable", "en", null, null, "1");

        assertEquals(Integer.valueOf(0), created.getSortOrder());
        assertTrue(created.getIsActive());
        assertFalse(created.getIsSeeded());
    }

    @Test
    public void create_respectsExplicitInactiveFlag() {
        when(inventoryItemTypeDAO.existsByCode(anyString())).thenReturn(false);

        InventoryItemType created = inventoryItemTypeService.create(null, "Draft Type", "en", 10, false, "1");

        assertFalse(created.getIsActive());
    }

    @Test
    public void upsertSeeded_createsNewType_withAllLocalesAndSeededFlag() {
        when(inventoryItemTypeDAO.getByCode("CONTROL")).thenReturn(null);

        InventoryItemType created = inventoryItemTypeService.upsertSeeded("CONTROL", 60, true,
                Map.of("en", "Control Material", "fr", "Matériel de contrôle"), "1");

        assertEquals("CONTROL", created.getCode());
        assertEquals(Integer.valueOf(60), created.getSortOrder());
        assertTrue(created.getIsActive());
        assertTrue("Config-seeded rows are marked seeded", created.getIsSeeded());
        assertEquals("Control Material", created.getNameLocalization().getLocalizedValue("en"));
        assertEquals("Matériel de contrôle", created.getNameLocalization().getLocalizedValue("fr"));
    }

    @Test
    public void upsertSeeded_updatesExistingType_byCode() {
        Localization localization = new Localization();
        localization.setLocalizedValue("en", "Old Label");
        InventoryItemType existing = new InventoryItemType();
        existing.setId(500L);
        existing.setCode("CONTROL");
        existing.setNameLocalization(localization);
        existing.setSortOrder(60);
        existing.setIsActive(true);
        when(inventoryItemTypeDAO.getByCode("CONTROL")).thenReturn(existing);

        InventoryItemType updated = inventoryItemTypeService.upsertSeeded("CONTROL", 65, false,
                Map.of("en", "Updated Label"), "1");

        assertEquals(Integer.valueOf(65), updated.getSortOrder());
        assertFalse(updated.getIsActive());
        assertEquals("Updated Label", updated.getNameLocalization().getLocalizedValue("en"));
        verify(localizationService).update(localization);
    }

    @Test
    public void upsertSeeded_keepsExistingSortOrder_whenNullPassed() {
        Localization localization = new Localization();
        localization.setLocalizedValue("en", "Label");
        InventoryItemType existing = new InventoryItemType();
        existing.setId(500L);
        existing.setCode("CONTROL");
        existing.setNameLocalization(localization);
        existing.setSortOrder(60);
        existing.setIsActive(true);
        when(inventoryItemTypeDAO.getByCode("CONTROL")).thenReturn(existing);

        InventoryItemType updated = inventoryItemTypeService.upsertSeeded("CONTROL", null, true, Map.of("en", "Label"),
                "1");

        assertEquals(Integer.valueOf(60), updated.getSortOrder());
    }
}
