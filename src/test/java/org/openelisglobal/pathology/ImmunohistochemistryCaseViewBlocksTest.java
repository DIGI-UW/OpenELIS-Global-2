package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.ImmunohistochemistryDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The immunohistochemistry case view reads a referred case's blocks and slides
 * straight from the pathology case, so it must keep working as those rows gain
 * designations, barcodes and an active flag.
 */
public class ImmunohistochemistryCaseViewBlocksTest extends BaseWebContextSensitiveTest {

    private static final int PATHOLOGY_CASE_ID = 1;
    private static final int LEGACY_BLOCK_ID = 101;
    private static final int LEGACY_SLIDE_ID = 201;
    private static final int IHC_SAMPLE_ID = 9701;

    @Autowired
    private ImmunohistochemistryDisplayService immunohistochemistryDisplayService;

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    /**
     * The immunohistochemistry row and the audit entries survive the fixture
     * reload, so both are cleared before each test as well as after it; otherwise
     * the insert below collides with the row a previous test left behind.
     */
    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        clearOwnRows();
        jdbcTemplate.update("INSERT INTO clinlims.immunohistochemistry_sample (id, program_id, sample_id, status,"
                + " pathology_sample_id, reffered, last_updated) VALUES (?, 1, 1, 'IN_PROGRESS', ?, true, now())",
                IHC_SAMPLE_ID, PATHOLOGY_CASE_ID);
    }

    @After
    public void tearDown() {
        clearOwnRows();
    }

    private void clearOwnRows() {
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE id = ?", IHC_SAMPLE_ID);
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table = ?",
                Integer.parseInt(referenceTablesService.getReferenceTableByName("PATHOLOGY_BLOCK").getId()));
    }

    @Test
    public void caseView_listsThePathologyCasesBlocksAndSlides() {
        ImmunohistochemistryCaseViewDisplayItem displayItem = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        assertEquals("the referred case's own block is read, not a copy", 1, displayItem.getBlocks().size());
        assertEquals(Integer.valueOf(LEGACY_BLOCK_ID), displayItem.getBlocks().get(0).getId());
        assertEquals("the block's legacy number is still there for a row with no designation", Integer.valueOf(11),
                displayItem.getBlocks().get(0).getBlockNumber());
        assertEquals(1, displayItem.getSlides().size());
        assertEquals(Integer.valueOf(LEGACY_SLIDE_ID), displayItem.getSlides().get(0).getId());
    }

    @Test
    public void caseView_stillListsABlockAfterItIsDeactivated() {
        pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, "reason", "1001");

        ImmunohistochemistryCaseViewDisplayItem displayItem = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        PathologyBlock block = displayItem.getBlocks().stream()
                .filter(b -> Integer.valueOf(LEGACY_BLOCK_ID).equals(b.getId())).findFirst().orElse(null);
        assertNotNull("a retired block is still on the case, not removed from the list", block);
        assertFalse("but it is marked retired, the same as the pathology case view sees it", block.isActive());
        PathologySlide slide = displayItem.getSlides().get(0);
        assertTrue("deactivating the block leaves its slide untouched", slide.isActive());
    }
}
