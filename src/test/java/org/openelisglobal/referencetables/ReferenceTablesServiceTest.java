package org.openelisglobal.referencetables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferenceTablesServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ReferenceTablesService referenceTablesService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referencetables.xml");
    }

    @After
    public void tearDown() {

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
    public void getTotalReferenceTableCount_shouldReturnCorrectCount() throws Exception {
        int expectedCount = 8;
        int count = referenceTablesService.getTotalReferenceTableCount();
        assertEquals(expectedCount, count);
    }

    @Test
    public void getAllReferenceTablesForHl7Encoding_shouldReturnOnlyHl7EncodedTables() throws Exception {
        List<ReferenceTables> hl7EncodedTables = referenceTablesService.getAllReferenceTablesForHl7Encoding();
        assertTrue(hl7EncodedTables.stream().allMatch(t -> "Y".equals(t.getIsHl7Encoded())));
        assertTrue(hl7EncodedTables.stream().anyMatch(t -> "Hl7Table".equals(t.getName())));
        assertFalse(hl7EncodedTables.stream().anyMatch(t -> "NonHl7Table".equals(t.getName())));
    }
}
