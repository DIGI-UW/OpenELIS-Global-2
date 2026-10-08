package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class InventoryItemTagsIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private static final String SYS_USER_ID = "1";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private InventoryItem newItem(String name, String... tags) {
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName(name + " " + UUID.randomUUID());
        item.setUnits("tests");
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        item.setTags(new LinkedHashSet<>(List.of(tags)));
        return item;
    }

    private Set<String> tagsOf(Long itemId) {
        return inventoryItemService.get(itemId).getTags();
    }

    @Test
    public void insert_persistsEveryTagAndReadsThemBack() {
        Long id = inventoryItemService.insert(newItem("Tagged item", "Cartridge", "TB"));

        assertEquals(Set.of("Cartridge", "TB"), tagsOf(id));
    }

    @Test
    public void anItemCarriesSeveralTagsWhereTheOldColumnAllowedOne() {
        Long id = inventoryItemService.insert(newItem("Multi tag item", "Cartridge", "TB", "Consumable"));

        assertEquals(3, tagsOf(id).size());
        assertEquals(Set.of("Cartridge", "TB", "Consumable"), tagsOf(id));
    }

    @Test
    public void update_replacesTheTagSetRatherThanAddingToIt() {
        Long id = inventoryItemService.insert(newItem("Retagged item", "Cartridge", "TB"));

        InventoryItem stored = inventoryItemService.get(id);
        stored.setTags(new LinkedHashSet<>(List.of("Cartridge", "Malaria")));
        stored.setSysUserId(SYS_USER_ID);
        inventoryItemService.update(stored);

        assertEquals(Set.of("Cartridge", "Malaria"), tagsOf(id));
    }

    @Test
    public void removingAnItemsLastTagLeavesItWithNone() {
        Long id = inventoryItemService.insert(newItem("Untagged item", "Cartridge"));

        InventoryItem stored = inventoryItemService.get(id);
        stored.setTags(new LinkedHashSet<>());
        stored.setSysUserId(SYS_USER_ID);
        inventoryItemService.update(stored);

        assertTrue(tagsOf(id).isEmpty());
    }

    @Test
    public void aTagIsSuggestedToTheNextItemOnceAnyItemCarriesIt() {
        String tag = "Fridge stock " + UUID.randomUUID().toString().substring(0, 8);
        assertFalse("tag must not already be in use for this case to mean anything",
                inventoryItemService.getAllTags().contains(tag));

        inventoryItemService.insert(newItem("Suggesting item", tag));

        assertTrue(inventoryItemService.getAllTags().contains(tag));
    }

    @Test
    public void aSecondSpellingAdoptsTheSpellingAlreadyInUse() {
        String tag = "Glove " + UUID.randomUUID().toString().substring(0, 8);
        inventoryItemService.insert(newItem("First glove item", tag));

        Long second = inventoryItemService.insert(newItem("Second glove item", tag.toUpperCase()));

        assertEquals("the established spelling wins", Set.of(tag), tagsOf(second));
        assertEquals("and no second variant joins the suggestion list", 1,
                inventoryItemService.getAllTags().stream().filter(known -> known.equalsIgnoreCase(tag)).count());
    }

    @Test
    public void twoSpellingsInOneSubmissionCollapseToOneTag() {
        Long id = inventoryItemService.insert(newItem("Doubled item", "Cartridge", "cartridge", "  Cartridge  "));

        assertEquals(1, tagsOf(id).size());
    }

    @Test
    public void blankAndWhitespaceOnlyTagsAreDropped() {
        Long id = inventoryItemService.insert(newItem("Blank tag item", "Cartridge", "   ", ""));

        assertEquals(Set.of("Cartridge"), tagsOf(id));
    }

    @Test
    public void theBoardRowCarriesTheItemsTags() {
        Long id = inventoryItemService.insert(newItem("Board tag item", "Cartridge", "TB"));

        InventoryProjection row = inventoryProjectionService.getBoard().stream()
                .filter(candidate -> candidate.getItemId().equals(id)).findFirst().orElse(null);

        assertNotNull("the item must reach the board", row);
        assertEquals(2, row.getTags().size());
        assertTrue(row.getTags().containsAll(List.of("Cartridge", "TB")));
    }

    @Test
    public void anItemCreatedWithoutATypeStillSatisfiesTheLegacyColumn() {
        Long id = inventoryItemService.insert(newItem("Typeless item", "Cartridge"));

        assertEquals(ItemType.REAGENT, inventoryItemService.get(id).getItemType());
    }

    @Test
    public void theMigrationCarriesAnExistingItemsTypeAcrossAsATag() throws Exception {
        assertEquals("the seed changeset must be wired into base.xml", Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog"
                        + " WHERE id = 'OGC-438-seed-inventory-item-tag-from-item-type'", Integer.class));

        Long legacyId = insertLegacyRow("CARTRIDGE");
        assertEquals("the row has to start untagged for this case to prove anything", Integer.valueOf(0),
                tagCount(legacyId));

        jdbcTemplate.execute(seedSqlFromChangeset());

        assertEquals(Set.of("Cartridge"), tagsOf(legacyId));

        jdbcTemplate.execute(seedSqlFromChangeset());
        assertEquals(Integer.valueOf(1), tagCount(legacyId));
    }

    private Long insertLegacyRow(String itemType) {
        Long id = jdbcTemplate.queryForObject("SELECT nextval('clinlims.inventory_item_seq')", Long.class);
        jdbcTemplate.update(
                "INSERT INTO clinlims.inventory_item (id, fhir_uuid, code, name, item_type, units, is_active)"
                        + " VALUES (?, ?, ?, ?, ?, 'tests', 'Y')",
                id, UUID.randomUUID(), "TAGTEST_" + id, "Legacy typed item " + id, itemType);
        return id;
    }

    private Integer tagCount(Long itemId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.inventory_item_tag WHERE item_id = ?",
                Integer.class, itemId);
    }

    private String seedSqlFromChangeset() throws Exception {
        String changeset = new String(
                getClass().getResourceAsStream("/liquibase/3.5.x.x/105-inventory-item-tags.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        int open = changeset.indexOf("<![CDATA[");
        int close = changeset.indexOf("]]>", open);
        assertTrue("the changeset must still carry its seed statement", open > 0 && close > open);
        return changeset.substring(open + "<![CDATA[".length(), close).trim();
    }
}
