package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class InventoryItemTypeDroppedIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "TYPEDROP_";

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

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
        jdbc.update("DELETE FROM clinlims.inventory_transaction WHERE lot_id IN"
                + " (SELECT l.id FROM clinlims.inventory_lot l JOIN clinlims.inventory_item i"
                + " ON i.id = l.inventory_item_id WHERE i.code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE inventory_item_id IN"
                + " (SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item_tag WHERE item_id IN"
                + " (SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    private InventoryItem item(String suffix, Integer stabilityAfterOpening, String... tags) {
        InventoryItem item = new InventoryItem();
        item.setCode(CODE_PREFIX + suffix);
        item.setName(CODE_PREFIX + suffix);
        item.setUnits("tests");
        item.setFhirUuid(UUID.randomUUID());
        item.setSysUserId("1");
        item.setStabilityAfterOpening(stabilityAfterOpening);
        item.setTags(new java.util.LinkedHashSet<>(List.of(tags)));
        Long id = inventoryItemService.insert(item);
        return inventoryItemService.get(id);
    }

    private Long lotFor(InventoryItem item) {
        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setLotNumber(CODE_PREFIX + "LOT" + System.nanoTime());
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setSysUserId("1");
        return inventoryLotService.insert(lot);
    }

    @Test
    public void theColumnIsGone() {
        Integer present = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns"
                + " WHERE table_schema = 'clinlims' AND table_name = 'inventory_item'"
                + " AND column_name = 'item_type'", Integer.class);

        assertEquals("changeset 112 should have dropped inventory_item.item_type", Integer.valueOf(0), present);
    }

    @Test
    public void theCheckConstraintAndIndexWentWithIt() {
        assertEquals("chk_item_type goes with the column", Integer.valueOf(0), jdbc
                .queryForObject("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_item_type'", Integer.class));
        assertEquals("idx_inventory_item_type goes with the column", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_inventory_item_type'", Integer.class));
    }

    @Test
    public void rollingBackRestoresEachItemsTypeFromItsTags() throws Exception {
        Long cartridge = item("RB_CARTRIDGE", null, "Cartridge", "TB").getId();
        Long hivKit = item("RB_HIV", null, "HIV kit").getId();
        Long untyped = item("RB_UNTYPED", null, "Consumable").getId();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute(rollbackSqlFromChangeset());

                assertEquals("CARTRIDGE", typeOf(statement, cartridge));
                assertEquals("HIV_KIT", typeOf(statement, hivKit));
                assertEquals("an item no tag maps falls back to REAGENT", "REAGENT", typeOf(statement, untyped));
                assertEquals("NO",
                        single(statement,
                                "SELECT is_nullable FROM information_schema.columns"
                                        + " WHERE table_schema = 'clinlims' AND table_name = 'inventory_item'"
                                        + " AND column_name = 'item_type'"));
                assertEquals("1",
                        single(statement, "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_item_type'"));
                assertEquals("1", single(statement,
                        "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_inventory_item_type'"));
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    private String typeOf(Statement statement, Long itemId) throws Exception {
        return single(statement, "SELECT item_type FROM clinlims.inventory_item WHERE id = " + itemId);
    }

    private String single(Statement statement, String query) throws Exception {
        try (ResultSet row = statement.executeQuery(query)) {
            assertTrue(query, row.next());
            return row.getString(1);
        }
    }

    private String rollbackSqlFromChangeset() throws Exception {
        String changeset = new String(
                getClass().getResourceAsStream("/liquibase/3.5.x.x/112-drop-inventory-item-type.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        int open = changeset.indexOf("<![CDATA[");
        int close = changeset.indexOf("]]>", open);
        assertTrue("the rollback must be one SQL block", open > 0 && close > open);
        return changeset.substring(open + "<![CDATA[".length(), close).trim();
    }

    @Test
    public void anItemIsCreatedWithoutATypeAndKeepsItsTags() {
        InventoryItem created = item("PLAIN", null, "Cartridge", "TB");

        assertNotNull(created);
        assertEquals(Set.of("Cartridge", "TB"), created.getTags());
    }

    @Test
    public void openingALotShortensExpiryWhenTheItemDeclaresAStabilityPeriod() {
        InventoryItem declared = item("DECLARED", 30, "Cartridge");
        Long lotId = lotFor(declared);

        inventoryLotService.openLot(lotId, new Timestamp(System.currentTimeMillis()), "1");

        InventoryLot opened = inventoryLotService.get(lotId);
        assertNotNull("a declared stability period shortens the expiry", opened.getCalculatedExpiryAfterOpening());
        assertTrue("and it lands in the future",
                opened.getCalculatedExpiryAfterOpening().after(new Timestamp(System.currentTimeMillis())));
    }

    @Test
    public void openingALotLeavesExpiryAloneWhenNoStabilityPeriodIsDeclared() {
        InventoryItem undeclared = item("UNDECLARED", null, "Cartridge");
        Long lotId = lotFor(undeclared);

        inventoryLotService.openLot(lotId, new Timestamp(System.currentTimeMillis()), "1");

        assertNull("nothing declared, nothing to shorten",
                inventoryLotService.get(lotId).getCalculatedExpiryAfterOpening());
    }

    @Test
    public void theTagDoesNotDecide() {
        Long cartridgeLot = lotFor(item("TAGGED_CARTRIDGE", 14, "Cartridge"));
        Long reagentLot = lotFor(item("TAGGED_REAGENT", 14, "Reagent"));
        Timestamp openedAt = new Timestamp(System.currentTimeMillis());

        inventoryLotService.openLot(cartridgeLot, openedAt, "1");
        inventoryLotService.openLot(reagentLot, openedAt, "1");

        assertNotNull("a cartridge that declares a stability period gets one",
                inventoryLotService.get(cartridgeLot).getCalculatedExpiryAfterOpening());
        assertEquals("and it is the same date a reagent would get",
                inventoryLotService.get(reagentLot).getCalculatedExpiryAfterOpening(),
                inventoryLotService.get(cartridgeLot).getCalculatedExpiryAfterOpening());
    }
}
