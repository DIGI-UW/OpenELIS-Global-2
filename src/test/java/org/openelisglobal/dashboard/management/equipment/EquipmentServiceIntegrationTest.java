package org.openelisglobal.dashboard.management.equipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class EquipmentServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        tearDown();
        seedSection(89741, "Equip Chemistry");
        seedSection(89742, "Equip Molecular");
        testSectionService.refreshNames();
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description, lastupdated)"
                + " VALUES (89740, 897, 'ANALYSIS', 'Equip Done', 'Equip Done', now())");
        seedAnalyzer(89741, "Equip Busy", true);
        seedAnalyzer(89742, "Equip Slow", true);
        seedAnalyzer(89743, "Equip Idle", true);
        seedAnalyzer(89744, "Equip Retired", false);
        seedAnalyzer(89745, "Equip New", true);
        seedAnalyzer(89746, "Equip Bridge", true);
        seedAnalyzer(89747, "Equip Staged", true);

        seedAnalysis(89741, 89741, 89741, 89741, 89741, Duration.ofMinutes(30));
        seedAnalysis(89742, 89741, 89741, 89741, 89741, Duration.ofMinutes(50));
        seedAnalysis(89743, 89742, 89741, 89742, 89742, Duration.ofHours(2));
        seedAnalysis(89744, 89743, 89742, null, 89742, Duration.ofHours(3));
        seedAnalysis(89745, 89744, 89743, 89741, 89741, Duration.ofDays(10));
        seedAnalysis(89746, 89745, 89744, 89741, 89741, Duration.ofMinutes(5));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_event (id, external_event_id, event_type, analyzer_id, payload,"
                        + " received_at) VALUES ('equip-89743', 'equip-89743', 'RESULT', '89743', '{}', ?)",
                Timestamp.valueOf(NOW.minusDays(2)));
        seedAnalysis(89747, 89746, 89746, 89741, 89741, Duration.ofMinutes(20));
        jdbc.update("UPDATE clinlims.analysis SET completed_date = date_trunc('day', completed_date) WHERE id = 89747");
        jdbc.update("INSERT INTO clinlims.analyzer_delivery_receipt (id, connection_id, message_id, analyzer_id,"
                + " profile_id, profile_revision, results_staged, results_held, controls_processed, accepted_by,"
                + " accepted_at) VALUES ('equip-89746', 'equip', 'equip-89746', 89746, 'equip', 1, 1, 0, 0, '1', ?)",
                Timestamp.valueOf(NOW.minusMinutes(10)));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                        + " last_updated) VALUES (89747, 89747, 'EQUIP-STAGED', 'Equip', '1', ?)",
                Timestamp.valueOf(NOW.minusHours(3)));
    }

    @After
    public void tearDown() {
        jdbc.update("DELETE FROM clinlims.analyzer_event WHERE id = 'equip-89743'");
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE id = 'equip-89746'");
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE id = 89747");
        jdbc.update("DELETE FROM clinlims.analysis WHERE id BETWEEN 89741 AND 89747");
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id BETWEEN 89741 AND 89746");
        jdbc.update("DELETE FROM clinlims.sample WHERE id BETWEEN 89741 AND 89746");
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id BETWEEN 89741 AND 89747");
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (89741, 89742)");
        jdbc.update("DELETE FROM clinlims.test_section WHERE id IN (89741, 89742)");
        jdbc.update("DELETE FROM clinlims.localization_value WHERE id IN (89741, 89742)");
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (89741, 89742)");
        jdbc.update("DELETE FROM clinlims.status_of_sample WHERE id = 89740");
    }

    private void seedSection(int id, String name) {
        jdbc.update("INSERT INTO clinlims.localization (id, description) VALUES (?, ?)", id, name);
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value)"
                + " VALUES (?, ?, 'en', ?)", id, id, name);
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                + " name_localization_id) VALUES (?, ?, ?, 'N', ?, ?)", id, name, name, id, id);
        jdbc.update("INSERT INTO clinlims.test (id, name, description, test_section_id, is_active, orderable,"
                + " sort_order, lastupdated, name_localization_id, guid)"
                + " VALUES (?, ?, ?, ?, 'Y', true, ?, now(), ?, ?)", id, name, name, id, id, id, "equip-" + id);
    }

    private void seedAnalyzer(int id, String name, boolean active) {
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated) VALUES (?, ?, ?, now())", id,
                name, active);
    }

    private void seedAnalysis(int id, int sampleId, int analyzerId, Integer sectionId, int testId, Duration ago) {
        Timestamp completed = Timestamp.valueOf(NOW.minus(ago));
        jdbc.update(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                        + " VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
                sampleId, "EQUIP-" + sampleId, completed, completed);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id) VALUES (?, ?, 1, 89740)"
                + " ON CONFLICT DO NOTHING", sampleId, sampleId);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, test_sect_id, analysis_type, status_id,"
                        + " analyzer_id, completed_date) VALUES (?, ?, ?, ?, 'MANUAL', 89740, ?, ?)",
                id, sampleId, testId, sectionId, analyzerId, completed);
    }

    private Map<String, AnalyzerActivity> lastSevenDays() {
        ScopeRange scope = ScopeRange.of("7d", LocalDate.now());
        return equipmentService.getEquipment(scope).stream()
                .collect(Collectors.toMap(AnalyzerActivity::id, Function.identity()));
    }

    @Test
    public void countsDistinctSamplesPerAnalyzerAndNamesItsBusiestSection() {
        AnalyzerActivity busy = lastSevenDays().get("89741");

        assertEquals("Equip Busy", busy.name());
        assertEquals(2, busy.samples());
        assertEquals("Equip Chemistry", busy.section());
    }

    @Test
    public void usesTheTestsSectionWhenTheAnalysisHasNone() {
        AnalyzerActivity slow = lastSevenDays().get("89742");

        assertEquals(1, slow.samples());
        assertEquals("Equip Molecular", slow.section());
    }

    @Test
    public void anAnalyzerIdleInScopeShowsItsLatestActivityFromAnySource() {
        AnalyzerActivity idle = lastSevenDays().get("89743");

        assertEquals(0, idle.samples());
        assertNull(idle.section());
        assertEquals(Timestamp.valueOf(NOW.minusDays(2)).toInstant().toString(), idle.lastActivityAt());
    }

    @Test
    public void aDeliveryReceiptOutranksTheDateOnlyCompletedDate() {
        AnalyzerActivity bridge = lastSevenDays().get("89746");

        assertEquals(1, bridge.samples());
        assertEquals(Timestamp.valueOf(NOW.minusMinutes(10)).toInstant().toString(), bridge.lastActivityAt());
    }

    @Test
    public void aStagedResultCountsAsActivityBeforeItIsAccepted() {
        AnalyzerActivity staged = lastSevenDays().get("89747");

        assertEquals(0, staged.samples());
        assertEquals(Timestamp.valueOf(NOW.minusHours(3)).toInstant().toString(), staged.lastActivityAt());
    }

    @Test
    public void listsOnlyActiveAnalyzersAndOneWithNoHistoryHasNoActivity() {
        Map<String, AnalyzerActivity> analyzers = lastSevenDays();

        assertFalse(analyzers.containsKey("89744"));
        assertNull(analyzers.get("89745").lastActivityAt());
    }
}
