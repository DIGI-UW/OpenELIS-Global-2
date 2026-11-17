package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.ImmunohistochemistryDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
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

    /** The fixture's open text request on the pathology case. */
    private static final int FIXTURE_REQUEST_ID = 301;

    /** This class's own requests on the pathology case. */
    private static final int COMPLETED_REQUEST = 9721;
    private static final int CANCELLED_REQUEST = 9722;

    /**
     * This class's own stain category id and entry, which tests seed themselves.
     */
    private static final int STAIN_CATEGORY_ID = 9710;
    private static final int STAIN = 9711;
    private static final String STAIN_NAME = "Masson trichrome";

    private DictionaryRows.StainEntries stains;

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
        jdbcTemplate.update("DELETE FROM clinlims.pathology_request WHERE id IN (?, ?)", COMPLETED_REQUEST,
                CANCELLED_REQUEST);
        PathologyCaseRows.clearLabelStamp(jdbcTemplate, LEGACY_SLIDE_ID);
        if (stains != null) {
            stains.close();
            stains = null;
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
        stains = DictionaryRows.stainEntries(jdbcTemplate, STAIN_CATEGORY_ID).with(STAIN, STAIN_NAME, true);
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

    /**
     * The referred view shows what staining and the coverslip check found on each
     * slide, read from the slide rows themselves, which carry the values by name.
     */
    @Test
    public void caseView_carriesTheReferredSlidesStainStatusAndCoverslipCheck() {
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_slide SET stain_status = 'STAINED', coverslip_qc = 'PASS'" + " WHERE id = ?",
                LEGACY_SLIDE_ID);

        ImmunohistochemistryCaseViewDisplayItem displayItem = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        PathologySlide slide = displayItem.getSlides().get(0);
        assertEquals("the stain status rides along on the slide", StainStatus.STAINED, slide.getStainStatus());
        assertEquals("and so does the coverslip check", CoverslipQc.PASS, slide.getCoverslipQc());
    }

    /**
     * The referred view lists what the pathologist asked of the bench: every
     * request that is not cancelled, text ones included, by its own id and
     * instruction, newest first, so two requests saying the same thing are still
     * two.
     */
    @Test
    public void caseView_listsEveryRequestNotCancelled_byIdAndInstruction() {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_request (id, pathology_sample_id, last_updated, status,"
                + " type, value, request_kind, priority) VALUES (?, ?, now(), 'COMPLETED', 'TEXT', 'Iron stain',"
                + " 'BLOCK', 'NORMAL'), (?, ?, now(), 'CANCELLED', 'TEXT', 'Asked twice', 'BLOCK', 'NORMAL')",
                COMPLETED_REQUEST, PATHOLOGY_CASE_ID, CANCELLED_REQUEST, PATHOLOGY_CASE_ID);

        ImmunohistochemistryCaseViewDisplayItem displayItem = immunohistochemistryDisplayService
                .convertToCaseDisplayItem(IHC_SAMPLE_ID);

        assertEquals("the completed request, then the fixture's open one; the cancelled one is left out",
                List.of(String.valueOf(COMPLETED_REQUEST) + "=Iron stain",
                        String.valueOf(FIXTURE_REQUEST_ID) + "=Histology review"),
                displayItem.getRequests().stream().map(request -> request.getId() + "=" + request.getValue()).toList());
    }
}
