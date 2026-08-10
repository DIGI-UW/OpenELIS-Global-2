package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.labeltype.InventoryLotLabel;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryLotLabelServiceIT extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryLotLabelService inventoryLotLabelService;

    @Autowired
    InventoryLotService inventoryLotService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void generateLabel_shouldRenderAPdfCarryingTheLotBarcode() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertEquals("Fixture lot should carry a barcode", "LOT-BC-1000", lot.getBarcode());

        ByteArrayOutputStream pdf = inventoryLotLabelService.generateLabel(lot);

        assertNotNull("Label stream should not be null", pdf);
        assertTrue("Label stream should carry content", pdf.size() > 0);
        String header = new String(pdf.toByteArray(), 0, 4, StandardCharsets.ISO_8859_1);
        assertEquals("Stream should be a PDF", "%PDF", header);
    }

    @Test
    public void label_shouldResolveEveryFieldNameToRealText() {
        // MessageUtil.getMessage falls back to the key itself, so a key that is
        // missing from message_en.properties prints "barcode.label.info.itemName"
        // on the label instead of "Item".
        InventoryLotLabel label = new InventoryLotLabel("Test Reagent A", "LOT-2025-001", "2099-12-31", "LOT-BC-1000");

        for (LabelField field : label.getAboveFields()) {
            assertFalse("Unresolved message key on label: " + field.getName(),
                    field.getName().startsWith("barcode.label."));
        }
        for (LabelField field : label.getBelowFields()) {
            assertFalse("Unresolved message key on label: " + field.getName(),
                    field.getName().startsWith("barcode.label."));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateLabel_shouldRejectANullLot() {
        inventoryLotLabelService.generateLabel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateLabel_shouldRejectALotWithNoBarcode() {
        // Lot 1002 is the fixture's barcode-less lot; there is nothing to encode.
        InventoryLot lot = inventoryLotService.get(1002L);
        String barcode = lot.getBarcode();
        assertTrue("Fixture lot 1002 should carry no usable barcode", barcode == null || barcode.trim().isEmpty());

        inventoryLotLabelService.generateLabel(lot);
    }
}
