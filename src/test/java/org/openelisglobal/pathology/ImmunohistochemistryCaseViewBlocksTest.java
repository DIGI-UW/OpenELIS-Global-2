package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
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
import org.openelisglobal.systemuser.service.SystemUserService;
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

    /**
     * This class's own stain category id and entry, which tests seed themselves.
     */
    private static final int STAIN_CATEGORY_ID = 9710;
    private static final int STAIN = 9711;
    private static final String STAIN_NAME = "Masson trichrome";

    private boolean insertedTheStainCategory;

    @Autowired
    private ImmunohistochemistryDisplayService immunohistochemistryDisplayService;

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

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
        PathologyCaseRows.clearLabelStamp(jdbcTemplate, LEGACY_SLIDE_ID);
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = NULL WHERE stain_id = ?", STAIN);
        DictionaryRows.removeEntries(jdbcTemplate, STAIN);
        if (insertedTheStainCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, STAIN_CATEGORY_ID);
            insertedTheStainCategory = false;
        }
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

    /**
     * The referred view names each slide's stain as the pathology case view does.
     */
    @Test
    public void caseView_namesTheStainsOfTheReferredSlides() {
        insertedTheStainCategory = DictionaryRows.insertCategory(jdbcTemplate, STAIN_CATEGORY_ID,
                PathologySampleService.STAIN_CATEGORY, PathologySampleService.STAIN_CATEGORY, "PathStain");
        int categoryId = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.STAIN_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, STAIN, categoryId, STAIN_NAME, "PSMas", true);
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = ? WHERE id = ?", STAIN, LEGACY_SLIDE_ID);

        ImmunohistochemistryCaseViewDisplayItem displayItem = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        assertEquals("the referred slide's stain is named, not left as a dictionary id",
                Map.of(String.valueOf(STAIN), STAIN_NAME), displayItem.getStainNames());
    }

    /**
     * The referred view shows whether each slide's label was checked against its
     * block, read from the same map the pathology case view reads.
     */
    @Test
    public void caseView_namesWhoVerifiedTheReferredSlidesLabels() {
        ImmunohistochemistryCaseViewDisplayItem unverified = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);
        assertTrue("a slide never checked is absent", unverified.getLabelVerifications().isEmpty());

        PathologyCaseRows.stampLabel(jdbcTemplate, LEGACY_SLIDE_ID, "1001");
        ImmunohistochemistryCaseViewDisplayItem verified = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        assertEquals("the verified slide is named by its verifier", systemUserService.get("1001").getDisplayName(),
                verified.getLabelVerifications().get(LEGACY_SLIDE_ID).verifiedBy());
    }
}
