package org.openelisglobal.analysis;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The section-scoped In Progress count, which is what the home-dashboard tile
 * reports to a user assigned to part of the lab. The fixture
 * (testdata/analysis-collected-sections.xml) seeds three NotStarted client
 * analyses — two in section 1, one in section 2 — plus a QC analysis in section
 * 1 that no count may include.
 */
public class AnalysisCollectedSectionScopedCountTest extends BaseWebContextSensitiveTest {

    private static final String STATUS_NOT_STARTED = "1";
    private static final List<String> NOT_STARTED = Arrays.asList(STATUS_NOT_STARTED);

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis-collected-sections.xml");
    }

    @Test
    public void sectionScopedCount_countsOnlyTheGivenSections() {
        assertEquals("unscoped: every client analysis, QC excluded", 3,
                analysisService.getCountOfCollectedAnalysesForStatusIdsExcludingQc(NOT_STARTED));

        assertEquals("section 1 holds two client analyses and one QC analysis", 2, analysisService
                .getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(NOT_STARTED, Arrays.asList("1")));

        assertEquals("section 2 holds one", 1, analysisService
                .getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(NOT_STARTED, Arrays.asList("2")));

        assertEquals("both sections together are the unscoped count", 3,
                analysisService.getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(NOT_STARTED,
                        Arrays.asList("1", "2")));
    }

    @Test
    public void sectionScopedCount_countsNothingForASectionWithNoAnalyses() {
        assertEquals("a section that holds nothing counts nothing", 0, analysisService
                .getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(NOT_STARTED, Arrays.asList("999")));

        assertEquals("no section in scope counts nothing", 0,
                analysisService.getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(NOT_STARTED,
                        Collections.emptyList()));
    }
}
