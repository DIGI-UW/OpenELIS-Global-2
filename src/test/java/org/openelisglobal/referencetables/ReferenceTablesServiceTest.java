package org.openelisglobal.referencetables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service-level tests for {@link ReferenceTablesService}.
 *
 * <p>
 * Seeds 8 deterministic test rows directly via JDBC at high IDs (9001-9008) to
 * avoid PK collisions with the SQL-seeded {@code reference_tables} rows (~246
 * entries spanning SAMPLE, PERSON, DICTIONARY, etc.). Pre-fix this class loaded
 * {@code
 * testdata/referencetables.xml} via
 * {@link BaseWebContextSensitiveTest#executeDataSetWithStateManagement}, which
 * TRUNCATEd the table and re-inserted only the 8 fixture rows — that broke
 * every downstream audit-emitting test (see {@code PROTECTED_SEED_TABLES}
 * javadoc on the base class) and made the assertion
 * {@code getTotalReferenceTableCount() == 8} accidentally pass.
 *
 * <p>
 * The fixture-loader now skips {@code reference_tables}, so the seed survives
 * and this test owns its data explicitly.
 */
public class ReferenceTablesServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ReferenceTablesService referenceTablesService;

    @Autowired
    private DataSource dataSource;

    private static final long TEST_ID_BASE = 9000L;

    private static final String[][] TEST_ROWS = {
            // {id-offset, name, is_hl7_encoded}
            { "1", "TestTable", "N" }, { "2", "DuplicateTable", "N" }, { "3", "Table1", "N" }, { "4", "Table2", "N" },
            { "5", "CountTable1", "N" }, { "6", "CountTable2", "N" }, { "7", "Hl7Table", "Y" },
            { "8", "NonHl7Table", "N" }, };

    @Before
    public void seedTestRows() throws Exception {
        deleteTestRows();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement insert = conn.prepareStatement("INSERT INTO clinlims.reference_tables "
                        + "(id, name, keep_history, is_hl7_encoded, lastupdated) VALUES (?, ?, 'Y', ?, NOW())")) {
            for (String[] row : TEST_ROWS) {
                long id = TEST_ID_BASE + Long.parseLong(row[0]);
                insert.setLong(1, id);
                insert.setString(2, row[1]);
                insert.setString(3, row[2]);
                insert.executeUpdate();
            }
        }
    }

    @After
    public void cleanupTestRows() throws Exception {
        deleteTestRows();
    }

    private void deleteTestRows() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement del = conn.prepareStatement("DELETE FROM clinlims.reference_tables "
                        + "WHERE name IN ('TestTable','DuplicateTable','Table1','Table2',"
                        + "'CountTable1','CountTable2','Hl7Table','NonHl7Table','NewInsertTestTable')")) {
            del.executeUpdate();
        }
    }

    @Test
    public void getReferenceTableByName_shouldReturnCorrectReferenceTable() throws Exception {
        String tableName = "TestTable";
        ReferenceTables retrievedTable = referenceTablesService.getReferenceTableByName(tableName);
        assertNotNull(retrievedTable);
        assertEquals(tableName, retrievedTable.getName());
    }

    @Test
    public void duplicateReferenceTable_shouldThrowException() throws Exception {
        String tableName = "DuplicateTable";
        ReferenceTables refTable = createReferenceTable(tableName);

        try {
            referenceTablesService.insert(refTable);
            fail("Expected LIMSDuplicateRecordException to be thrown");
        } catch (LIMSDuplicateRecordException e) {
            assertEquals("Duplicate record exists for " + tableName, e.getMessage());
        }
    }

    private ReferenceTables createReferenceTable(String tableName) {
        ReferenceTables refTable = new ReferenceTables();
        refTable.setTableName(tableName);
        refTable.setIsHl7Encoded("N");
        refTable.setKeepHistory("Y");
        return refTable;
    }

    @Test
    public void insertReferenceTable_shouldInsertCorrectly() throws Exception {
        ReferenceTables refTable = createReferenceTable("NewInsertTestTable");
        referenceTablesService.insert(refTable);
        ReferenceTables retrievedTable = referenceTablesService.getReferenceTableByName("NewInsertTestTable");
        assertNotNull(retrievedTable);
        assertEquals("NewInsertTestTable", retrievedTable.getTableName());
    }

    @Test
    public void getAllReferenceTables_shouldReturnAllTables() throws Exception {
        int expectedCount = referenceTablesService.getTotalReferenceTableCount();
        List<ReferenceTables> allTables = referenceTablesService.getAllReferenceTables();
        assertEquals(expectedCount, allTables.size());
    }

    @Test
    public void getTotalReferenceTableCount_shouldIncludeAllTestSeededRows() throws Exception {
        // Pre-fix this asserted exactly-8 because the fixture loader truncated
        // reference_tables. Post-fix the SQL seed (~246 rows) survives, so the
        // contract is "at least our 8 test rows are present alongside the seed."
        int count = referenceTablesService.getTotalReferenceTableCount();
        assertTrue("Expected at least 8 reference_tables rows (the 8 seeded by this test); got " + count,
                count >= TEST_ROWS.length);
    }

    @Test
    public void getAllReferenceTablesForHl7Encoding_shouldReturnOnlyHl7EncodedTables() throws Exception {
        List<ReferenceTables> hl7EncodedTables = referenceTablesService.getAllReferenceTablesForHl7Encoding();
        assertTrue(hl7EncodedTables.stream().allMatch(t -> "Y".equals(t.getIsHl7Encoded())));
        assertTrue(hl7EncodedTables.stream().anyMatch(t -> "Hl7Table".equals(t.getName())));
        assertFalse(hl7EncodedTables.stream().anyMatch(t -> "NonHl7Table".equals(t.getName())));
    }

    @Test
    public void getTotalReferenceTablesCount_shouldReturnCorrectCount() throws Exception {
        int count = referenceTablesService.getTotalReferenceTablesCount();
        assertTrue(count > 0);
    }

    @Test
    public void getPageOfReferenceTables_shouldReturnSubset() throws Exception {
        List<ReferenceTables> page = referenceTablesService.getPageOfReferenceTables(1);
        assertNotNull(page);
        assertFalse(page.isEmpty());
    }

    @Test
    public void getReferenceTableByName_withObject_shouldReturnCorrectReferenceTable() throws Exception {
        ReferenceTables ref = new ReferenceTables();
        ref.setTableName("TestTable");
        ReferenceTables result = referenceTablesService.getReferenceTableByName(ref);
        assertNotNull(result);
        assertEquals("TestTable", result.getName());
    }

    @Test
    public void getData_shouldPopulateFields() throws Exception {
        ReferenceTables ref = new ReferenceTables();
        ref.setId("1");
        referenceTablesService.getData(ref);
        assertNotNull(ref.getName());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateDuplicateReferenceTable_shouldThrowException() throws Exception {
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName("DuplicateTable");
        refTable.setId(null);
        referenceTablesService.update(refTable);
    }
}
