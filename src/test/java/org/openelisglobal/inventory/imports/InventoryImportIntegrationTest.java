package org.openelisglobal.inventory.imports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

/**
 * Defining a catalogue from a file, twice.
 *
 * <p>
 * The cases that matter are the ones about doing it more than once: a corrected
 * file re-imported has to update rather than duplicate, and an unchanged file
 * re-imported has to say so rather than churn every row.
 */
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

    // --- the preview writes nothing ---

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

    // --- idempotency, which is the whole point ---

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

    /** With no UPC there is nothing else to match on but the name. */
    @Test
    public void anItemWithNoUpcIsMatchedByName() {
        apply(HEADER + row("A", "", "25"));

        InventoryImportPlan second = apply(HEADER + row("A", "", "50"));

        assertEquals(1, itemCount());
        assertEquals(1, second.updated());
        assertEquals(Integer.valueOf(50), named(PREFIX + "A").getLowStockThreshold());
    }

    /**
     * A renamed item still matches on its UPC, which is the point of preferring it:
     * the barcode identifies the product, the name is how people write it down.
     */
    @Test
    public void aRenamedItemIsStillMatchedByItsUpc() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryImportPlan second = apply(HEADER + row("A_RENAMED", "0001", "25"));

        assertEquals("the same item, renamed", 1, itemCount());
        assertEquals(1, second.updated());
        assertNotNull(named(PREFIX + "A_RENAMED"));
        assertNull(named(PREFIX + "A"));
    }

    /** A blank UPC must reach the column as null; the unique index is partial. */
    @Test
    public void severalItemsCanHaveNoUpcAtAll() {
        InventoryImportPlan plan = apply(HEADER + row("A", "", "25") + row("B", "", "30") + row("C", "", "35"));

        assertEquals(3, plan.created());
        assertEquals(3, itemCount());
        assertEquals("blank must be null, not an empty string", Integer.valueOf(0),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.inventory_item WHERE name LIKE ? AND upc = ''",
                        Integer.class, PREFIX + "%"));
    }

    // --- per-row errors, and the rows around them ---

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

    // --- the fields themselves ---

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

    /**
     * A file that does not carry a column must leave that field alone. Otherwise a
     * short file used to fix one typo would blank everything it omits.
     */
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

        // The corrected file a lab actually sends: the same columns, with the
        // ones it has nothing to say about left empty.
        apply(HEADER + PREFIX + "A,tests,,0001,,,,,\n");

        InventoryItem item = named(PREFIX + "A");
        assertEquals("an empty threshold cell must not switch low-stock detection off", Integer.valueOf(25),
                item.getLowStockThreshold());
        assertEquals("Acme", item.getManufacturer());
        assertEquals(Integer.valueOf(14), item.getLeadTimeDays());
        assertEquals(Set.of("RDT", "Malaria"), item.getTags());
    }

    /**
     * A row the database refuses used to take the whole file with it. The failure
     * lands at flush, which marks the transaction rollback-only; the loop went on
     * recording the rows after it as written, and the commit then threw and
     * discarded every one of them. Each row now commits on its own.
     */
    @Test
    public void aRowTheDatabaseRefusesIsRolledBackAloneAndTheRestAreWritten() {
        String tooLongForTheColumn = "C".repeat(150);
        String bad = PREFIX + "BAD,tests,,0009,Acme," + tooLongForTheColumn + ",5,14,Y\n";

        InventoryImportPlan plan = apply(HEADER + row("GOOD1", "0001", "25") + bad + row("GOOD2", "0002", "30"));

        assertEquals("both good rows were written", 2, plan.created());
        assertEquals(1, plan.skipped());
        assertNotNull(named(PREFIX + "GOOD1"));
        assertNotNull("a row after the bad one must still be there", named(PREFIX + "GOOD2"));
        assertNull(named(PREFIX + "BAD"));
    }

    /**
     * A catalogue is typed in Excel, and Excel writes a UTF-8 byte order mark at
     * the head of the file. It lands invisibly on the first header, so the file was
     * refused for missing the column it plainly has.
     */
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

    /**
     * Tags are canonicalised on write, so a file spelling one differently in case
     * is not a change. Reporting it as an update would make every re-import of a
     * hand-edited file look like it did something.
     */
    @Test
    public void tagsThatDifferOnlyInCaseAreNotAChange() {
        apply(HEADER + row("A", "0001", "25"));

        InventoryImportPlan second = apply(
                HEADER + PREFIX + "A,tests,\"rdt;malaria\",0001,Acme," + PREFIX + "A-CAT,25,14,Y\n");

        assertEquals("the same tags, differently typed", 0, second.updated());
        assertEquals(1, second.unchanged());
    }
}
