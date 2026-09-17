package org.openelisglobal.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
 * <li>item 1 — collected client specimen (kept)
 * <li>item 2 — uncollected client specimen, collection_date NULL (also kept:
 * SampleAddService only records a collection date when the user supplies one,
 * so gating the tile on it emptied it for deployments that leave the field
 * blank)
 * <li>item 3 — collected QC specimen (dropped, via its QC profile)
 * </ul>
 *
 * Both client analyses survive and only the QC one is dropped, so this query
 * differs from the plain excluding-Qc pair solely in admitting pool-anchored
 * vector analyses, and the count stays in step with the list.
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
    public void collectedExcludingQc_dropsQcOnly_keepsCollectedAndUncollectedClient() {
        // Baseline: all three NotStarted analyses, regardless of collection or QC.
        assertEquals("baseline must see all three NotStarted analyses", 3,
                analysisService.getAnalysesForStatusId(STATUS_NOT_STARTED).size());

        // Excluding QC only: both client analyses (collected + uncollected).
        List<Analysis> excludingQc = analysisService.getAnalysesForStatusIdExcludingQc(STATUS_NOT_STARTED);
        assertEquals("excluding-Qc must keep both client analyses, drop only the QC one", 2, excludingQc.size());

        // Same query used by the tile: both client analyses, QC one dropped. The
        // uncollected specimen must survive — dropping it emptied the tile for any
        // deployment that does not record collection dates.
        List<Analysis> inProgress = analysisService.getCollectedAnalysesForStatusIdExcludingQc(STATUS_NOT_STARTED);
        assertNotNull(inProgress);
        assertEquals("must keep both client analyses regardless of collection date", 2, inProgress.size());
        List<String> ids = inProgress.stream().map(Analysis::getId).sorted().collect(Collectors.toList());
        assertEquals("surviving analyses must be the two client ones", Arrays.asList("1", "2"), ids);
        assertTrue("the uncollected client specimen must be included", inProgress.stream()
                .anyMatch(a -> a.getSampleItem() != null && a.getSampleItem().getCollectionDate() == null));
    }

    @Test
    public void countCollectedExcludingQc_staysInStepWithTheList() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);
        // The tile count must match its drill-down list: both client analyses.
        assertEquals(2, analysisService.getCountOfAnalysesForStatusIdsExcludingQc(statuses));
        assertEquals(2, analysisService.getCountOfCollectedAnalysesForStatusIdsExcludingQc(statuses));
    }
}
