package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Generated codes come from the real {@code inventory_item_code_sequence} row,
 * so two blank-code inserts read back as -001 and -002.
 */
public class InventoryItemCodeSequenceTest extends BaseWebContextSensitiveTest {

    // A name no other suite uses, so this prefix's counter is this test's alone.
    private static final String NAME = "Codeseq Widget 6103";
    private static final String PREFIX = "COD-6103";
    private static final String TYPED_CODE = "CODESEQ-TYPED-6103";

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ? OR code = ?", PREFIX + "-%", TYPED_CODE);
        jdbc.update("DELETE FROM clinlims.inventory_item_code_sequence WHERE prefix = ?", PREFIX);
    }

    private Long insertItem(String code) {
        InventoryItem item = new InventoryItem();
        item.setName(NAME);
        item.setCode(code);
        item.setFhirUuid(UUID.randomUUID());
        item.setUnits("mL");
        item.setItemType(ItemType.REAGENT);
        item.setIsActive("Y");
        item.setSysUserId("1");
        return inventoryItemService.insert(item);
    }

    private String storedCode(Long id) {
        return jdbc.queryForObject("SELECT code FROM clinlims.inventory_item WHERE id = ?", String.class, id);
    }

    private long nextValue() {
        return jdbc.queryForObject("SELECT next_value FROM clinlims.inventory_item_code_sequence WHERE prefix = ?",
                Long.class, PREFIX);
    }

    @Test
    public void insert_numbersBlankCodes_fromThePrefixCounter() {
        Long first = insertItem(null);
        Long second = insertItem("  ");

        assertEquals(PREFIX + "-001", storedCode(first));
        assertEquals(PREFIX + "-002", storedCode(second));
        assertEquals("The counter row must sit on the value the third item would take", 3L, nextValue());
    }

    @Test
    public void insert_skipsTheCounterValue_aTypedCodeAlreadyHolds() {
        insertItem(PREFIX + "-001");

        Long generated = insertItem(null);

        assertEquals(PREFIX + "-002", storedCode(generated));
    }

    @Test
    public void insert_keepsATypedCode_andNeverStartsACounterForIt() {
        Long id = insertItem("codeseq typed 6103");

        assertEquals(TYPED_CODE, storedCode(id));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.inventory_item_code_sequence WHERE prefix = ?", Integer.class, PREFIX));
    }
}
