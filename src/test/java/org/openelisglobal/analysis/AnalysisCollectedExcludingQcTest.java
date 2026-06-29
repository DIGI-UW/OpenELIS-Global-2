package org.openelisglobal.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@code getCollectedAnalysesForStatusIdExcludingQc} and
 * its count counterpart, which back the "In Progress / Awaiting Result Entry"
 * dashboard tile (PatientDashBoardProvider, ORDERS_IN_PROGRESS).
 *
 * <p>
 * Fixture (testdata/analysis-collected-excluding-qc.xml) seeds one accession
 * with three NotStarted analyses:
 *
 * <ul>
 * <li>item 1 — collected client specimen (the only row the collected query
 * keeps)
 * <li>item 2 — uncollected client specimen (collection_date NULL; dropped by
 * the collected query, kept by the plain excluding-Qc query)
 * <li>item 3 — collected QC specimen (dropped by both, via its QC profile)
 * </ul>
 *
 * The three-way split (baseline 3, excluding-Qc 2, collected-excluding-Qc 1)
 * proves the collected query composes the collection guard on top of the QC
 * exclusion, and that the count stays in step with the list.
 */
public class AnalysisCollectedExcludingQcTest extends BaseWebContextSensitiveTest {

    private static final String STATUS_NOT_STARTED = "1";

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis-collected-excluding-qc.xml");
    }

    @Test
    public void collectedExcludingQc_dropsUncollectedAndQc_keepsOnlyCollectedClient() {
        // Baseline: all three NotStarted analyses, regardless of collection or QC.
        assertEquals("baseline must see all three NotStarted analyses", 3,
                analysisService.getAnalysesForStatusId(STATUS_NOT_STARTED).size());

        // Excluding QC only: both client analyses (collected + uncollected).
        List<Analysis> excludingQc = analysisService.getAnalysesForStatusIdExcludingQc(STATUS_NOT_STARTED);
        assertEquals("excluding-Qc must keep both client analyses, drop only the QC one", 2, excludingQc.size());

        // Collected + excluding QC: only the collected client analysis (item 1).
        List<Analysis> collected = analysisService.getCollectedAnalysesForStatusIdExcludingQc(STATUS_NOT_STARTED);
        assertNotNull(collected);
        assertEquals("collected-excluding-Qc must keep only the collected client analysis", 1, collected.size());
        Analysis surviving = collected.get(0);
        assertEquals("surviving analysis must be analysis 1", "1", surviving.getId());
        assertEquals("surviving analysis must be on sample item 1", "1", surviving.getSampleItem().getId());
        assertNotNull("surviving analysis's specimen must be collected", surviving.getSampleItem().getCollectionDate());
    }

    @Test
    public void countCollectedExcludingQc_staysInStepWithTheList() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);
        // The tile count must match its drill-down list: 2 excluding-Qc, 1 collected.
        assertEquals(2, analysisService.getCountOfAnalysesForStatusIdsExcludingQc(statuses));
        assertEquals(1, analysisService.getCountOfCollectedAnalysesForStatusIdsExcludingQc(statuses));
    }
}
