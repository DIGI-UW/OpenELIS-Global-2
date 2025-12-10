package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryItemTypeValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService itemService;

    @Before
    public void setup() {
        // No specific setup needed
    }

    @Test
    public void insertReagent_withValidFields_shouldSucceed() {
        InventoryItem reagent = createBaseItem(ItemType.REAGENT);
        reagent.setStabilityAfterOpening(30);
        reagent.setDilutionNotes("Dilute 1:10 with distilled water");

        Long id = itemService.insert(reagent);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Item type should be REAGENT", ItemType.REAGENT, saved.getItemType());
        assertEquals("Stability should be saved", Integer.valueOf(30), saved.getStabilityAfterOpening());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertReagent_withoutStabilityAfterOpening_shouldThrowException() {
        InventoryItem reagent = createBaseItem(ItemType.REAGENT);

        itemService.insert(reagent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertReagent_withZeroStability_shouldThrowException() {
        InventoryItem reagent = createBaseItem(ItemType.REAGENT);
        reagent.setStabilityAfterOpening(0);

        itemService.insert(reagent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertReagent_withNegativeStability_shouldThrowException() {
        InventoryItem reagent = createBaseItem(ItemType.REAGENT);
        reagent.setStabilityAfterOpening(-5);

        itemService.insert(reagent);
    }

    @Test
    public void insertCartridge_withValidFields_shouldSucceed() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCompatibleAnalyzers("Analyzer-X, Analyzer-Y");
        cartridge.setCalibrationRequired("Y");

        Long id = itemService.insert(cartridge);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Item type should be CARTRIDGE", ItemType.CARTRIDGE, saved.getItemType());
        assertEquals("Compatible analyzers should be saved", "Analyzer-X, Analyzer-Y", saved.getCompatibleAnalyzers());
        assertEquals("Calibration required should be saved", "Y", saved.getCalibrationRequired());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertCartridge_withoutCompatibleAnalyzers_shouldThrowException() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCalibrationRequired("Y");

        itemService.insert(cartridge);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertCartridge_withEmptyCompatibleAnalyzers_shouldThrowException() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCompatibleAnalyzers("   ");
        cartridge.setCalibrationRequired("Y");

        itemService.insert(cartridge);
    }

    @Test
    public void insertCartridge_withoutExplicitCalibrationRequired_shouldUseDefault() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCompatibleAnalyzers("Analyzer-X");

        Long id = itemService.insert(cartridge);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Should use default value N", "N", saved.getCalibrationRequired());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertCartridge_withInvalidCalibrationRequired_shouldThrowException() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCompatibleAnalyzers("Analyzer-X");
        cartridge.setCalibrationRequired("MAYBE");

        itemService.insert(cartridge);
    }

    @Test
    public void insertRDT_withValidFields_shouldSucceed() {
        InventoryItem rdt = createBaseItem(ItemType.RDT);
        rdt.setTestsPerKit(25);
        rdt.setIndividualTracking("N");

        Long id = itemService.insert(rdt);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Item type should be RDT", ItemType.RDT, saved.getItemType());
        assertEquals("Tests per kit should be saved", Integer.valueOf(25), saved.getTestsPerKit());
        assertEquals("Individual tracking should be saved", "N", saved.getIndividualTracking());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertRDT_withoutTestsPerKit_shouldThrowException() {
        InventoryItem rdt = createBaseItem(ItemType.RDT);
        rdt.setIndividualTracking("N");

        itemService.insert(rdt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertRDT_withZeroTestsPerKit_shouldThrowException() {
        InventoryItem rdt = createBaseItem(ItemType.RDT);
        rdt.setTestsPerKit(0);
        rdt.setIndividualTracking("N");

        itemService.insert(rdt);
    }

    @Test
    public void insertRDT_withoutExplicitIndividualTracking_shouldUseDefault() {
        InventoryItem rdt = createBaseItem(ItemType.RDT);
        rdt.setTestsPerKit(25);

        Long id = itemService.insert(rdt);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Should use default value N", "N", saved.getIndividualTracking());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertRDT_withInvalidIndividualTracking_shouldThrowException() {
        InventoryItem rdt = createBaseItem(ItemType.RDT);
        rdt.setTestsPerKit(25);
        rdt.setIndividualTracking("SOMETIMES");

        itemService.insert(rdt);
    }

    @Test
    public void insertHIVKit_withValidFields_shouldSucceed() {
        InventoryItem hivKit = createBaseItem(ItemType.HIV_KIT);
        hivKit.setSourceOrganization("WHO");
        hivKit.setKitTestType("HIV");
        hivKit.setTestsPerKit(100);

        Long id = itemService.insert(hivKit);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Item type should be HIV_KIT", ItemType.HIV_KIT, saved.getItemType());
        assertEquals("Source organization should be saved", "WHO", saved.getSourceOrganization());
        assertEquals("Kit test type should be saved", "HIV", saved.getKitTestType());
        assertEquals("Tests per kit should be saved", Integer.valueOf(100), saved.getTestsPerKit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertHIVKit_withoutSourceOrganization_shouldThrowException() {
        InventoryItem hivKit = createBaseItem(ItemType.HIV_KIT);
        hivKit.setKitTestType("HIV");
        hivKit.setTestsPerKit(100);

        itemService.insert(hivKit);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertHIVKit_withoutKitTestType_shouldThrowException() {
        InventoryItem hivKit = createBaseItem(ItemType.HIV_KIT);
        hivKit.setSourceOrganization("WHO");
        hivKit.setTestsPerKit(100);

        itemService.insert(hivKit);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertHIVKit_withoutTestsPerKit_shouldThrowException() {
        InventoryItem hivKit = createBaseItem(ItemType.HIV_KIT);
        hivKit.setSourceOrganization("WHO");
        hivKit.setKitTestType("HIV");

        itemService.insert(hivKit);
    }

    @Test
    public void insertSyphilisKit_withValidFields_shouldSucceed() {
        InventoryItem syphilisKit = createBaseItem(ItemType.SYPHILIS_KIT);
        syphilisKit.setSourceOrganization("CDC");
        syphilisKit.setKitTestType("SYPHILIS");
        syphilisKit.setTestsPerKit(50);

        Long id = itemService.insert(syphilisKit);

        assertNotNull("Item should be inserted", id);
        InventoryItem saved = itemService.get(id);
        assertEquals("Item type should be SYPHILIS_KIT", ItemType.SYPHILIS_KIT, saved.getItemType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateReagent_removingRequiredField_shouldThrowException() {
        InventoryItem reagent = createBaseItem(ItemType.REAGENT);
        reagent.setStabilityAfterOpening(30);
        Long id = itemService.insert(reagent);

        InventoryItem toUpdate = itemService.get(id);
        toUpdate.setStabilityAfterOpening(null);
        toUpdate.setSysUserId("1");

        itemService.update(toUpdate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateCartridge_changingToInvalidCalibrationValue_shouldThrowException() {
        InventoryItem cartridge = createBaseItem(ItemType.CARTRIDGE);
        cartridge.setCompatibleAnalyzers("Analyzer-X");
        cartridge.setCalibrationRequired("Y");
        Long id = itemService.insert(cartridge);

        InventoryItem toUpdate = itemService.get(id);
        toUpdate.setCalibrationRequired("INVALID");
        toUpdate.setSysUserId("1");

        itemService.update(toUpdate);
    }

    private InventoryItem createBaseItem(ItemType itemType) {
        InventoryItem item = new InventoryItem();
        item.setName("Test " + itemType.name() + " Item");
        item.setItemType(itemType);
        item.setUnits("units");
        item.setIsActive("Y");
        item.setFhirUuid(UUID.randomUUID());
        item.setSysUserId("1");
        return item;
    }
}
