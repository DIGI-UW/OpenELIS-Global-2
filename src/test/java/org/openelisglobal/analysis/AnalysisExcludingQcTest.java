package org.openelisglobal.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.qc.dao.SampleItemQcProfileDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the *ExcludingQc analysis query variants.
 *
 * <p>
 * Fixture (testdata/qc-evaluation.xml) seeds one accession with four analyses,
 * three of which belong to QC sample items (BLANK=2, DUPLICATE=3, CONTROL=4)
 * and one to a client sample (sampleItem=1). Each variant must return only the
 * client analysis while the non-Qc baseline returns all four.
 */
public class AnalysisExcludingQcTest extends BaseWebContextSensitiveTest {

    private static final String STATUS_NOT_STARTED = "1";
    private static final String TEST_SECTION_ENVIRONMENTAL = "1";
    private static final String SAMPLE_STATUS_RECEIVED = "1";
    private static final String ACCESSION = "QC-TEST-001";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleItemQcProfileDAO sampleItemQcProfileDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-evaluation.xml");
    }

    @Test
    public void fixturePreconditions_threeQcProfilesOneClientSample() {
        assertEquals(3, sampleItemQcProfileDAO.findBySampleItemIds(Arrays.asList(1, 2, 3, 4)).size());
        assertNull("Sample item 1 must be a client sample (no QC profile)",
                sampleItemQcProfileDAO.findBySampleItemId(1).orElse(null));
    }

    @Test
    public void getAnalysesForStatusIdExcludingQc_returnsOnlyClientAnalysis() {
        List<Analysis> all = analysisService.getAnalysesForStatusId(STATUS_NOT_STARTED);
        assertEquals("Baseline must return all four analyses (1 client + 3 QC)", 4, all.size());

        List<Analysis> clientOnly = analysisService.getAnalysesForStatusIdExcludingQc(STATUS_NOT_STARTED);
        assertNotNull(clientOnly);
        assertEquals(1, clientOnly.size());
        Analysis remaining = clientOnly.get(0);
        assertEquals("Should be the analysis on sample item 1", "1", remaining.getSampleItem().getId());
        assertEquals("Should be analysis id 1", "1", remaining.getId());
    }

    @Test
    public void getCountOfAnalysesForStatusIdsExcludingQc_returnsOne() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);
        assertEquals(4, analysisService.getCountOfAnalysesForStatusIds(statuses));
        assertEquals(1, analysisService.getCountOfAnalysesForStatusIdsExcludingQc(statuses));
    }

    @Test
    public void getAllAnalysisByTestSectionAndStatusExcludingQc_returnsOnlyClientAnalysis() {
        List<String> analysisStatuses = Arrays.asList(STATUS_NOT_STARTED);
        List<String> sampleStatuses = Arrays.asList(SAMPLE_STATUS_RECEIVED);

        List<Analysis> all = analysisService.getAllAnalysisByTestSectionAndStatus(TEST_SECTION_ENVIRONMENTAL,
                analysisStatuses, sampleStatuses);
        assertEquals("Baseline must return all four analyses", 4, all.size());

        List<Analysis> clientOnly = analysisService.getAllAnalysisByTestSectionAndStatusExcludingQc(
                TEST_SECTION_ENVIRONMENTAL, analysisStatuses, sampleStatuses);
        assertNotNull(clientOnly);
        assertEquals(1, clientOnly.size());
        assertEquals("1", clientOnly.get(0).getSampleItem().getId());
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatusExcludingQc_returnsOne() {
        List<String> analysisStatuses = Arrays.asList(STATUS_NOT_STARTED);
        List<String> sampleStatuses = Arrays.asList(SAMPLE_STATUS_RECEIVED);

        assertEquals(4, analysisService.getCountAnalysisByTestSectionAndStatus(TEST_SECTION_ENVIRONMENTAL,
                analysisStatuses, sampleStatuses));
        assertEquals(1, analysisService.getCountAnalysisByTestSectionAndStatusExcludingQc(TEST_SECTION_ENVIRONMENTAL,
                analysisStatuses, sampleStatuses));
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatusExcludingQc_returnsOnlyClientAnalysis() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);

        List<Analysis> all = analysisService.getPageAnalysisByTestSectionAndStatus(TEST_SECTION_ENVIRONMENTAL, statuses,
                false);
        assertEquals(4, all.size());

        List<Analysis> clientOnly = analysisService
                .getPageAnalysisByTestSectionAndStatusExcludingQc(TEST_SECTION_ENVIRONMENTAL, statuses, false);
        assertEquals(1, clientOnly.size());
        assertEquals("1", clientOnly.get(0).getSampleItem().getId());
    }

    @Test
    public void getPageAnalysisAtAccessionNumberAndStatusExcludingQc_returnsOnlyClientAnalysis() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);

        List<Analysis> all = analysisService.getPageAnalysisAtAccessionNumberAndStatus(ACCESSION, statuses, false);
        assertEquals(4, all.size());

        List<Analysis> clientOnly = analysisService.getPageAnalysisAtAccessionNumberAndStatusExcludingQc(ACCESSION,
                statuses, false);
        assertEquals(1, clientOnly.size());
        assertEquals("1", clientOnly.get(0).getSampleItem().getId());
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatusExcludingQc_twoArg_returnsOne() {
        List<String> statuses = Arrays.asList(STATUS_NOT_STARTED);

        assertEquals(4, analysisService.getCountAnalysisByTestSectionAndStatus(TEST_SECTION_ENVIRONMENTAL, statuses));
        assertEquals(1, analysisService.getCountAnalysisByTestSectionAndStatusExcludingQc(TEST_SECTION_ENVIRONMENTAL,
                statuses));
    }
}
