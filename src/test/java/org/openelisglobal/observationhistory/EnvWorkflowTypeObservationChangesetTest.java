package org.openelisglobal.observationhistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The order dashboards read each vector or environmental order's workflow type
 * from the {@code envWorkflowType} observation, which only the optional
 * observation-history-types CSV used to seed. Changeset 021 seeds the type and
 * copies the value of the {@code vecWorkflowType} marker, which every such
 * order carries, onto the orders entered before the type existed. The test runs
 * the backfill statement verbatim against an order shaped like those.
 */
public class EnvWorkflowTypeObservationChangesetTest extends BaseWebContextSensitiveTest {

    private static final String BACKFILL_SQL = "INSERT INTO clinlims.observation_history"
            + " (id, patient_id, sample_id, sample_item_id, observation_history_type_id, value_type, value, lastupdated)"
            + " SELECT nextval('clinlims.observation_history_seq'), v.patient_id, v.sample_id, v.sample_item_id,"
            + " env.id, v.value_type, v.value, now()" + " FROM clinlims.observation_history v"
            + " JOIN clinlims.observation_history_type vt ON vt.id = v.observation_history_type_id"
            + " AND vt.type_name = 'vecWorkflowType'"
            + " JOIN clinlims.observation_history_type env ON env.type_name = 'envWorkflowType'"
            + " WHERE v.sample_id IS NOT NULL AND v.value IN ('vector', 'environmental')"
            + " AND NOT EXISTS (SELECT 1 FROM clinlims.observation_history e"
            + " WHERE e.sample_id = v.sample_id AND e.observation_history_type_id = env.id)";

    private static final long VECTOR_SAMPLE = 97601L;
    private static final long ALREADY_MARKED_SAMPLE = 97602L;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        insertSample(VECTOR_SAMPLE, "ENV021A");
        insertSample(ALREADY_MARKED_SAMPLE, "ENV021B");
        insertObservation(VECTOR_SAMPLE, "vecWorkflowType", "vector");
        insertObservation(ALREADY_MARKED_SAMPLE, "vecWorkflowType", "environmental");
        insertObservation(ALREADY_MARKED_SAMPLE, "envWorkflowType", "environmental");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void theWorkflowTypeObservationTypeExists() {
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.observation_history_type WHERE type_name = 'envWorkflowType'",
                        Integer.class));
    }

    @Test
    public void backfillCopiesTheVectorMarkerOntoOrdersWithoutTheWorkflowTypeAndIsSafeToRunTwice() {
        jdbc.execute(BACKFILL_SQL);
        jdbc.execute(BACKFILL_SQL);

        assertEquals("vector", envWorkflowTypeOf(VECTOR_SAMPLE));
        assertEquals(Integer.valueOf(1), countEnvWorkflowType(VECTOR_SAMPLE));
        assertEquals(Integer.valueOf(1), countEnvWorkflowType(ALREADY_MARKED_SAMPLE));
    }

    @Test
    public void theDashboardLookupSeesTheBackfilledOrder() {
        jdbc.execute(BACKFILL_SQL);

        List<ObservationHistory> byType = observationHistoryService
                .getObservationHistoriesByType(ObservationType.ENV_WORKFLOW_TYPE);

        assertTrue(byType.stream()
                .anyMatch(o -> String.valueOf(VECTOR_SAMPLE).equals(o.getSampleId()) && "vector".equals(o.getValue())));
    }

    private String envWorkflowTypeOf(long sampleId) {
        return jdbc.queryForObject("SELECT value FROM clinlims.observation_history o"
                + " JOIN clinlims.observation_history_type t ON t.id = o.observation_history_type_id"
                + " WHERE t.type_name = 'envWorkflowType' AND o.sample_id = ?", String.class, sampleId);
    }

    private Integer countEnvWorkflowType(long sampleId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.observation_history o"
                + " JOIN clinlims.observation_history_type t ON t.id = o.observation_history_type_id"
                + " WHERE t.type_name = 'envWorkflowType' AND o.sample_id = ?", Integer.class, sampleId);
    }

    private void insertSample(long id, String accession) {
        jdbc.update(
                "INSERT INTO clinlims.sample (id, accession_number, domain, entered_date, received_date,"
                        + " is_confirmation, lastupdated) VALUES (?, ?, 'V', now(), now(), false, now())",
                id, accession);
    }

    private void insertObservation(long sampleId, String typeName, String value) {
        jdbc.update("INSERT INTO clinlims.observation_history"
                + " (id, sample_id, observation_history_type_id, value_type, value, lastupdated)"
                + " SELECT nextval('clinlims.observation_history_seq'), ?, t.id, 'L', ?, now()"
                + " FROM clinlims.observation_history_type t WHERE t.type_name = ?", sampleId, value, typeName);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.observation_history WHERE sample_id IN (?, ?)", VECTOR_SAMPLE,
                ALREADY_MARKED_SAMPLE);
        jdbc.update("DELETE FROM clinlims.sample WHERE id IN (?, ?)", VECTOR_SAMPLE, ALREADY_MARKED_SAMPLE);
    }
}
