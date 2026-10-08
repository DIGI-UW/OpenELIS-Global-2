package org.openelisglobal.inventory.imports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.imports.InventoryImportPlan.Outcome;
import org.openelisglobal.inventory.imports.InventoryImportPlan.RowPlan;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class InventoryImportIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "IMPORTTEST_";
    private static final String HEADER = "name,units,tags,upc,manufacturer,catalog number,"
            + "reorder threshold,lead time days,track lots\n";

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private InventoryImportService importService;

    @Autowired
    private InventoryItemService inventoryItemService;

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
        jdbc.update("DELETE FROM clinlims.inventory_item_tag WHERE item_id IN"
                + " (SELECT id FROM clinlims.inventory_item WHERE name LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE name LIKE ?", PREFIX + "%");
    }

    private InventoryImportPlan apply(String csv) {
        return importService.apply(csv, "1");
    }

    private int itemCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.inventory_item WHERE name LIKE ?", Integer.class,
                PREFIX + "%");
    }

    private InventoryItem named(String name) {
        return inventoryItemService.getByExactName(name);
    }

    private String row(String name, String upc, String threshold) {
        return PREFIX + name + ",tests,\"RDT;Malaria\"," + upc + ",Acme," + PREFIX + name + "-CAT," + threshold
                + ",14,Y\n";
    }

    @Test
    public void aPreviewReportsWhatWouldHappenAndWritesNothing() {
        InventoryImportPlan plan = importService.preview(HEADER + row("A", "0001", "25"));

        assertEquals(1, plan.created());
        assertEquals(0, plan.skipped());
        assertEquals("a preview must not define anything", 0, itemCount());
    }

    @Test
    public void applyingWhatWasPreviewedProducesTheSameOutcome() {
        String csv = HEADER + row("A", "0001", "25") + row("B", "0002", "30");

        InventoryImportPlan previewed = importService.preview(csv);
        InventoryImportPlan applied = apply(csv);

        assertEquals(previewed.created(), applied.created());
        assertEquals(previewed.updated(), applied.updated());
        assertEquals(previewed.skipped(), applied.skipped());
        assertEquals(2, itemCount());
    }

    @Test
    public void importingTheSameFileTwiceLeavesOneRowPerItem() {
        String csv = HEADER + row("A", "0001", "25") + row("B", "0002", "30");

        apply(csv);
        InventoryImportPlan second = apply(csv);

        assertEquals("no duplicates on the second run", 2, itemCount());
        assertEquals("and nothing is reported as created", 0, second.created());
        assertEquals("nor as updated, because nothing differs", 0, second.updated());
        assertEquals(2, second.unchanged());
    }

    @Test
    public void aCorrectedFileUpdatesTheItemRatherThanDuplicatingIt() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryImportPlan second = apply(HEADER + row("A", "0001", "80"));

        assertEquals(1, itemCount());
        assertEquals(1, second.updated());
        assertEquals(Integer.valueOf(80), named(PREFIX + "A").getLowStockThreshold());
    }

    @Test
    public void anItemWithNoUpcIsMatchedByName() {
        apply(HEADER + row("A", "", "25"));

        InventoryImportPlan second = apply(HEADER + row("A", "", "50"));

        assertEquals(1, itemCount());
        assertEquals(1, second.updated());
        assertEquals(Integer.valueOf(50), named(PREFIX + "A").getLowStockThreshold());
    }

    @Test
    public void aRenamedItemIsStillMatchedByItsUpc() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryImportPlan second = apply(HEADER + row("A_RENAMED", "0001", "25"));

        assertEquals("the same item, renamed", 1, itemCount());
        assertEquals(1, second.updated());
        assertNotNull(named(PREFIX + "A_RENAMED"));
        assertNull(named(PREFIX + "A"));
    }

    @Test
    public void severalItemsCanHaveNoUpcAtAll() {
        InventoryImportPlan plan = apply(HEADER + row("A", "", "25") + row("B", "", "30") + row("C", "", "35"));

        assertEquals(3, plan.created());
        assertEquals(3, itemCount());
        assertEquals("blank must be null, not an empty string", Integer.valueOf(0),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.inventory_item WHERE name LIKE ? AND upc = ''",
                        Integer.class, PREFIX + "%"));
    }

    @Test
    public void aBadRowIsSkippedWithItsReasonAndItsLineNumber() {
        String csv = HEADER + row("A", "0001", "25") + (PREFIX + "B,tests,,0002,Acme,CAT,not-a-number,14,Y\n")
                + row("C", "0003", "30");

        InventoryImportPlan plan = importService.preview(csv);

        assertEquals(2, plan.created());
        assertEquals(1, plan.skipped());
        RowPlan bad = plan.rows().stream().filter(r -> r.outcome() == Outcome.SKIP).findFirst().orElseThrow();
        assertEquals("the line in the file, so it can be found", 3, bad.lineNumber());
        assertTrue(bad.reason(), bad.reason().contains("reorder threshold"));
        assertTrue(bad.reason(), bad.reason().contains("not-a-number"));
    }

    @Test
    public void aRowWithNoNameOrNoUnitsIsSkippedRatherThanFailingAtTheDatabase() {
        String csv = HEADER + ",tests,,,,,,,\n" + (PREFIX + "NOUNITS,,,,,,,,\n");

        InventoryImportPlan plan = importService.preview(csv);

        assertEquals(2, plan.skipped());
        assertTrue(plan.rows().stream().anyMatch(r -> r.reason().contains("needs a name")));
        assertTrue(plan.rows().stream().anyMatch(r -> r.reason().contains("needs units")));
    }

    @Test
    public void aFileNamingTheSameItemTwiceTakesTheFirstAndSaysSo() {
        InventoryImportPlan plan = apply(HEADER + row("A", "0001", "25") + row("A", "0001", "99"));

        assertEquals(1, plan.created());
        assertEquals(1, plan.skipped());
        assertEquals(1, itemCount());
        assertEquals("the first row won", Integer.valueOf(25), named(PREFIX + "A").getLowStockThreshold());
    }

    @Test
    public void aFileMissingARequiredColumnIsRefusedWholeRatherThanRowByRow() {
        InventoryImportPlan plan = importService.preview("name,tags\n" + PREFIX + "A,RDT\n");

        assertEquals(0, plan.created());
        assertEquals(1, plan.skipped());
        assertTrue(plan.rows().get(0).reason(), plan.rows().get(0).reason().contains("units"));
    }

    @Test
    public void everyColumnInTheTemplateReachesTheItem() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryItem item = named(PREFIX + "A");
        assertNotNull(item);
        assertEquals("tests", item.getUnits());
        assertEquals("0001", item.getUpc());
        assertEquals("Acme", item.getManufacturer());
        assertEquals(PREFIX + "A-CAT", item.getCatalogNumber());
        assertEquals(Integer.valueOf(25), item.getLowStockThreshold());
        assertEquals(Integer.valueOf(14), item.getLeadTimeDays());
        assertEquals("Y", item.getTrackLots());
        assertEquals(Set.of("RDT", "Malaria"), item.getTags());
    }

    @Test
    public void aColumnTheFileOmitsIsLeftAloneRatherThanBlanked() {
        apply(HEADER + row("A", "0001", "25"));

        apply("name,units\n" + PREFIX + "A,tests\n");

        InventoryItem item = named(PREFIX + "A");
        assertEquals("the manufacturer survived a file that did not mention it", "Acme", item.getManufacturer());
        assertEquals(Integer.valueOf(25), item.getLowStockThreshold());
        assertEquals(Set.of("RDT", "Malaria"), item.getTags());
    }

    @Test
    public void anEmptyCellIsLeftAloneRatherThanBlankingTheStoredValue() {
        apply(HEADER + row("A", "0001", "25"));

        apply(HEADER + PREFIX + "A,tests,,0001,,,,,\n");

        InventoryItem item = named(PREFIX + "A");
        assertEquals("an empty threshold cell must not switch low-stock detection off", Integer.valueOf(25),
                item.getLowStockThreshold());
        assertEquals("Acme", item.getManufacturer());
        assertEquals(Integer.valueOf(14), item.getLeadTimeDays());
        assertEquals(Set.of("RDT", "Malaria"), item.getTags());
    }

    @Test
    public void aRowOverAFieldLimitIsSkippedInThePreviewWithAPlainReason() {
        String bad = PREFIX + "LONG,tests,,0009,Acme," + "C".repeat(150) + ",5,14,Y\n";
        String csv = HEADER + row("GOOD1", "0001", "25") + bad;

        for (InventoryImportPlan plan : List.of(importService.preview(csv), apply(csv))) {
            assertEquals(1, plan.created());
            RowPlan skipped = plan.rows().stream().filter(r -> r.outcome() == Outcome.SKIP).findFirst().orElseThrow();
            assertEquals(3, skipped.lineNumber());
            assertTrue(skipped.reason(), skipped.reason().startsWith("catalog number"));
            assertFalse(skipped.reason(), skipped.reason().contains("org.openelisglobal"));
        }
        assertNull(named(PREFIX + "LONG"));
    }

    @Test
    public void aRowTheDatabaseRefusesIsRolledBackAloneAndTheRestAreWritten() {
        jdbc.execute("ALTER TABLE clinlims.inventory_item ADD CONSTRAINT import_test_refuses_bad CHECK (name <> '"
                + PREFIX + "BAD')");
        try {
            InventoryImportPlan plan = apply(
                    HEADER + row("GOOD1", "0001", "25") + row("BAD", "0009", "5") + row("GOOD2", "0002", "30"));

            assertEquals("both good rows were written", 2, plan.created());
            assertEquals(1, plan.skipped());
            assertNotNull(named(PREFIX + "GOOD1"));
            assertNotNull("a row after the bad one must still be there", named(PREFIX + "GOOD2"));
            assertNull(named(PREFIX + "BAD"));
        } finally {
            jdbc.execute("ALTER TABLE clinlims.inventory_item DROP CONSTRAINT IF EXISTS import_test_refuses_bad");
        }
    }

    @Test
    public void aFileSavedWithAByteOrderMarkIsReadRatherThanRefused() {
        InventoryImportPlan plan = apply("\uFEFF" + HEADER + row("BOM", "0007", "25"));

        assertEquals(plan.rows().isEmpty() ? "" : plan.rows().get(0).reason(), 1, plan.created());
        assertEquals(0, plan.skipped());
        assertNotNull(named(PREFIX + "BOM"));
    }

    @Test
    public void theTemplateIsAFileThisImporterAccepts() {
        InventoryImportPlan plan = importService.preview(importService.template());

        assertEquals("the template's own example row must import", 1, plan.created());
        assertEquals(0, plan.skipped());
    }

    @Test
    public void anEmptyFileIsRefusedRatherThanReportedAsNothingToDo() {
        InventoryImportPlan plan = importService.preview("");

        assertEquals(1, plan.skipped());
        assertTrue(plan.rows().get(0).reason(), plan.rows().get(0).reason().contains("header"));
    }

    @Test
    public void tagsThatDifferOnlyInCaseAreNotAChange() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryImportPlan second = apply(
                HEADER + PREFIX + "A,tests,\"rdt;malaria\",0001,Acme," + PREFIX + "A-CAT,25,14,Y\n");

        assertEquals("the same tags, differently typed", 0, second.updated());
        assertEquals(1, second.unchanged());
    }
}
