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
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An item's classification is now a set of free-form tags rather than one value
 * from a fixed list of five.
 *
 * <p>
 * The cases that matter are the ones a service-level test would not catch on
 * its own: that a tag survives a round trip through the database at all, that
 * several tags fit on one item where the old column allowed one, that a second
 * spelling of a tag a lab already uses is folded into the first rather than
 * starting a duplicate, and that the migration carried every existing item's
 * type across so nothing was lost on upgrade.
 */
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

    /**
     * The point of folding case: a typeahead offers what exists, but nothing stops
     * a user typing past it, and two spellings of one tag split the lab's own
     * vocabulary in half.
     */
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

    /**
     * The board is where a tag has to arrive for anyone to search or see it, and
     * the row is built field by field rather than from the entity, so the copy is
     * easy to leave out.
     */
    @Test
    public void theBoardRowCarriesTheItemsTags() {
        Long id = inventoryItemService.insert(newItem("Board tag item", "Cartridge", "TB"));

        InventoryProjection row = inventoryProjectionService.getBoard().stream()
                .filter(candidate -> candidate.getItemId().equals(id)).findFirst().orElse(null);

        assertNotNull("the item must reach the board", row);
        assertEquals(2, row.getTags().size());
        assertTrue(row.getTags().containsAll(List.of("Cartridge", "TB")));
    }

    /**
     * The upgrade path, driven against the migration's own SQL rather than a
     * paraphrase of it: the statement is read out of the changeset file and run on
     * a row shaped like one that predates this change. A copy of the SQL in the
     * test would pass even if the changeset had a typo in it.
     *
     * <p>
     * The column this migration reads has since been dropped, so the case puts it
     * back for its own duration. That is not contrivance: an instance upgrading
     * from before 105 runs the seed and <i>then</i> the drop, so a broken seed
     * loses every item's classification moments before the evidence goes with it.
     * The order those two changesets run in is exactly what this proves.
     */
    @Test
    public void theMigrationCarriesAnExistingItemsTypeAcrossAsATag() throws Exception {
        assertEquals("the seed changeset must be wired into base.xml", Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog"
                        + " WHERE id = 'OGC-438-seed-inventory-item-tag-from-item-type'", Integer.class));
        assertEquals("and the drop must be wired in after it", Integer.valueOf(1),
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM databasechangelog" + " WHERE id = 'OGC-438-drop-inventory-item-type'",
                        Integer.class));

        restoreLegacyColumn();
        try {
            Long legacyId = insertLegacyRow("CARTRIDGE");
            assertEquals("the row has to start untagged for this case to prove anything", Integer.valueOf(0),
                    tagCount(legacyId));

            jdbcTemplate.execute(seedSqlFromChangeset());

            assertEquals(Set.of("Cartridge"), tagsOf(legacyId));

            // The seed is written to survive being replayed, which is what a rerun of a
            // partially applied changelog does.
            jdbcTemplate.execute(seedSqlFromChangeset());
            assertEquals(Integer.valueOf(1), tagCount(legacyId));
        } finally {
            jdbcTemplate.execute("ALTER TABLE clinlims.inventory_item DROP COLUMN IF EXISTS item_type");
        }
    }

    /** The table as it stood before the drop, so the seed has something to read. */
    private void restoreLegacyColumn() {
        jdbcTemplate.execute("ALTER TABLE clinlims.inventory_item ADD COLUMN IF NOT EXISTS item_type VARCHAR(50)");
    }

    /**
     * An item written straight to the table, the way one existed before this
     * change.
     */
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
