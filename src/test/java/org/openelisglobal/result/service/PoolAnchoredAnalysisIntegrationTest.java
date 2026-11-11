package org.openelisglobal.result.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.fhir.service.ServiceRequestTransformService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A vector pool's analysis is anchored to the pool and has no sample item. Two
 * read paths assumed one: its History panel on the Results page answered 500
 * (the retest lookup threw inside a transactional call, which left the
 * timeline's transaction rollback-only even though the error was caught and
 * logged), and its FHIR ServiceRequest failed on validation.
 */
public class PoolAnchoredAnalysisIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long POOL_ANALYSIS = 9200;

    @Autowired
    private AnalysisTimelineService analysisTimelineService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ServiceRequestTransformService serviceRequestTransformService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/vector-surveillance-positivity.xml");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update("DELETE FROM clinlims.analysis WHERE id = ?", POOL_ANALYSIS);
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis"
                        + " (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision, status_id, status,"
                        + "  started_date, entry_date, analysis_type, reflex_trigger, referred_out, corrected,"
                        + "  result_calculated, type_of_sample_name, fhir_uuid, lastupdated)"
                        + " VALUES (?, NULL, 900, 900, 900, 1, 900, '1', ?, ?, 'MANUAL', false, false, false, false,"
                        + "  'Mosquito', '00000000-0000-4000-8000-000000009200'::uuid, ?)",
                POOL_ANALYSIS, now, now, now);
    }

    @Test
    public void aPoolAnalysisHasATimeline() {
        List<AnalysisTimelineService.AnalysisTimelineEvent> events = analysisTimelineService
                .getTimeline(analysisService.get(String.valueOf(POOL_ANALYSIS)));

        assertTrue("no retest is inferred without a sample item",
                events.stream().noneMatch(event -> "RETEST".equals(event.getType())));
    }

    @Test
    public void aPoolAnalysisIsPublishedAsAServiceRequest() {
        assertNotNull(serviceRequestTransformService
                .transformToServiceRequest(analysisService.get(String.valueOf(POOL_ANALYSIS))).getCode());
    }
}
