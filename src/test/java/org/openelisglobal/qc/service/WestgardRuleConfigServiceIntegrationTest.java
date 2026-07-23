package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.dto.RuleConfigSummary;
import org.openelisglobal.qc.dto.UnconfiguredMapping;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring integration tests for WestgardRuleConfigService summary and
 * unconfigured mapping methods.
 *
 * Uses real database via BaseWebContextSensitiveTest (TestContainers
 * PostgreSQL).
 */
public class WestgardRuleConfigServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WestgardRuleConfigService ruleConfigService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbcTemplate;

    // Test IDs — use high values to avoid collisions with seeded data
    private static final String TEST_ID_A = "90001";
    private static final String INSTRUMENT_ID_A = "90001";
    private static final String TEST_ID_B = "90002";
    private static final String INSTRUMENT_ID_B = "90002";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        entityManager.clear();
        cleanTestData();
        seedParentRows();
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    /**
     * Insert minimal parent rows required by foreign keys on westgard_rule_config
     * and qc_control_lot (both have FK to test.id). The test table eagerly loads a
     * Localization entity via name_localization_id, so we must seed that too.
     */
    private void seedParentRows() {
        // Insert system_user with id=1 (required by FK on westgard_rule_config and
        // qc_control_lot)
        int userExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_user WHERE id = 1", Integer.class);
        if (userExists == 0) {
            jdbcTemplate.update(
                    "INSERT INTO system_user (id, login_name, first_name, last_name, is_active, is_employee, lastupdated) "
                            + "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    1, "testUser", "John", "Doe", "Y", "Y");
        }

        // Insert analyzer rows (required by FK on westgard_rule_config and
        // qc_control_lot). Note: testId/instrumentId are exposed as String
        // in Java via LIMSStringNumberUserType, but the SQL columns are
        // NUMERIC — JDBC binds primitives to NUMERIC natively, so we
        // convert at the boundary.
        for (String instrumentId : new String[] { INSTRUMENT_ID_A, INSTRUMENT_ID_B }) {
            long instrumentIdNum = Long.parseLong(instrumentId);
            int analyzerExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analyzer WHERE id = ?",
                    Integer.class, instrumentIdNum);
            if (analyzerExists == 0) {
                jdbcTemplate.update("INSERT INTO analyzer (id, name, is_active, last_updated) VALUES (?, ?, ?, NOW())",
                        instrumentIdNum, "TestAnalyzer-" + instrumentId, true);
            }
        }

        for (String testId : new String[] { TEST_ID_A, TEST_ID_B }) {
            long testIdNum = Long.parseLong(testId);
            int exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test WHERE id = ?", Integer.class,
                    testIdNum);
            if (exists == 0) {
                // Create localization + localization_value rows (required by Test.getName())
                long locId = testIdNum; // reuse same ID for simplicity
                long locValId = testIdNum; // reuse for value row
                jdbcTemplate.update("INSERT INTO localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                        locId, "test name");
                jdbcTemplate.update(
                        "INSERT INTO localization_value (id, localization_id, locale, value) VALUES (?, ?, ?, ?)",
                        locValId, locId, "en", "IntTest-" + testId);
                jdbcTemplate.update(
                        "INSERT INTO test (id, name, description, is_active, guid, "
                                + "name_localization_id, lastupdated) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                        testIdNum, "IntTest-" + testId, "IntegrationTest-" + testId, "Y", UUID.randomUUID().toString(),
                        locId);
            }
        }
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM westgard_rule_config WHERE test_id IN (90001, 90002)");
            jdbcTemplate.execute("DELETE FROM qc_control_lot WHERE test_id IN (90001, 90002)");
            jdbcTemplate.execute("DELETE FROM test WHERE id IN (90001, 90002)");
            jdbcTemplate.execute("DELETE FROM analyzer WHERE id IN (90001, 90002)");
            jdbcTemplate.execute("DELETE FROM localization_value WHERE localization_id IN (90001, 90002)");
            jdbcTemplate.execute("DELETE FROM localization WHERE id IN (90001, 90002)");
        } catch (Exception e) {
            System.out.println("Failed to clean rule config test data: " + e.getMessage());
        }
    }

    // ---- Helper: insert a rule config row directly via SQL ----
    private void insertRuleConfig(String testId, String instrumentId, String ruleCode, boolean enabled,
            String severity) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO westgard_rule_config " + "(id, test_id, instrument_id, rule_code, enabled, severity, "
                        + "requires_corrective_action, sys_user_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, Long.parseLong(testId), Long.parseLong(instrumentId), ruleCode, enabled, severity,
                "REJECTION".equals(severity), 1);
    }

    // ---- Helper: insert a control lot row directly via SQL ----
    private void insertControlLot(String testId, String instrumentId, String status) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO qc_control_lot " + "(id, test_id, instrument_id, product_name, lot_number, "
                        + "manufacturer, control_level, status, calculation_method, "
                        + "initial_runs_count, sys_user_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, Long.parseLong(testId), Long.parseLong(instrumentId), "Test Control", "LOT-" + id.substring(0, 8),
                "TestMfg", "Level 1", status, "MANUFACTURER_FIXED", 20, 1);
    }

    // ==================== getAllRuleConfigSummaries tests ====================

    @Test
    public void testGetAllRuleConfigSummaries_returnsGroupedByTestInstrumentPair() {
        // Arrange — create rule configs for two distinct (test, instrument) pairs
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "1\u2083\u209B", true, "REJECTION");
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "2\u2082\u209B", false, "WARNING");
        insertRuleConfig(TEST_ID_B, INSTRUMENT_ID_B, "1\u2083\u209B", true, "REJECTION");

        // Act
        List<RuleConfigSummary> summaries = ruleConfigService.getAllRuleConfigSummaries();

        // Assert — at least our two test pairs are present
        RuleConfigSummary summaryA = summaries.stream()
                .filter(s -> s.getTestId().equals(TEST_ID_A) && s.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);
        RuleConfigSummary summaryB = summaries.stream()
                .filter(s -> s.getTestId().equals(TEST_ID_B) && s.getInstrumentId().equals(INSTRUMENT_ID_B)).findFirst()
                .orElse(null);

        assertNotNull("Summary for pair A should exist", summaryA);
        assertNotNull("Summary for pair B should exist", summaryB);

        // Pair A: 2 total, 1 enabled, names resolved
        assertEquals("Pair A total rule count", 2, summaryA.getTotalRuleCount());
        assertEquals("Pair A enabled rule count", 1, summaryA.getEnabledRuleCount());
        assertEquals("Pair A test name", "IntegrationTest-" + TEST_ID_A, summaryA.getTestName());
        assertEquals("Pair A instrument name", "TestAnalyzer-" + INSTRUMENT_ID_A, summaryA.getInstrumentName());

        // Pair B: 1 total, 1 enabled, names resolved
        assertEquals("Pair B total rule count", 1, summaryB.getTotalRuleCount());
        assertEquals("Pair B enabled rule count", 1, summaryB.getEnabledRuleCount());
        assertEquals("Pair B test name", "IntegrationTest-" + TEST_ID_B, summaryB.getTestName());
    }

    @Test
    public void testGetAllRuleConfigSummaries_includesRuleDetails() {
        // Arrange — create 2 rules for one pair
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "1\u2083\u209B", true, "REJECTION");
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "R\u2084\u209B", false, "REJECTION");

        // Act
        List<RuleConfigSummary> summaries = ruleConfigService.getAllRuleConfigSummaries();
        RuleConfigSummary summary = summaries.stream()
                .filter(s -> s.getTestId().equals(TEST_ID_A) && s.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);

        // Assert — rules list is populated with correct details
        assertNotNull("Summary should exist", summary);
        assertNotNull("Rules list should not be null", summary.getRules());
        assertEquals("Should have 2 rule details", 2, summary.getRules().size());

        RuleConfigSummary.RuleConfigDetail rule13s = summary.getRules().stream()
                .filter(r -> r.getRuleCode().equals("1\u2083\u209B")).findFirst().orElse(null);
        assertNotNull("1₃ₛ detail should exist", rule13s);
        assertNotNull("1₃ₛ should have an ID", rule13s.getId());
        assertTrue("1₃ₛ should be enabled", rule13s.isEnabled());
        assertEquals("1₃ₛ severity should be REJECTION", "REJECTION", rule13s.getSeverity());

        RuleConfigSummary.RuleConfigDetail ruleR4s = summary.getRules().stream()
                .filter(r -> r.getRuleCode().equals("R\u2084\u209B")).findFirst().orElse(null);
        assertNotNull("R₄ₛ detail should exist", ruleR4s);
        assertFalse("R₄ₛ should be disabled", ruleR4s.isEnabled());
    }

    @Test
    public void testGetAllRuleConfigSummaries_emptyWhenNoConfigs() {
        // Arrange — no rule configs inserted for our test IDs

        // Act
        List<RuleConfigSummary> summaries = ruleConfigService.getAllRuleConfigSummaries();

        // Assert — our test pairs should NOT appear
        boolean hasTestPair = summaries.stream()
                .anyMatch(s -> s.getTestId().equals(TEST_ID_A) && s.getInstrumentId().equals(INSTRUMENT_ID_A));
        assertFalse("Test pair should not appear when no configs exist", hasTestPair);
    }

    // ==================== getUnconfiguredMappings tests ====================

    @Test
    public void testGetUnconfiguredMappings_returnsControlLotsWithoutRuleConfig() {
        // Arrange — control lot exists but NO rule config for this pair
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");

        // Act
        List<UnconfiguredMapping> mappings = ruleConfigService.getUnconfiguredMappings();

        // Assert — pair A should appear as unconfigured
        UnconfiguredMapping mappingA = mappings.stream()
                .filter(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);

        assertNotNull("Pair A should be unconfigured", mappingA);
        assertEquals("Pair A should have 1 active lot", 1, mappingA.getActiveControlLotCount());
        assertEquals("Instrument name should be resolved", "TestAnalyzer-" + INSTRUMENT_ID_A,
                mappingA.getInstrumentName());
        // Test 90001 was seeded with description "IntegrationTest-90001"
        assertEquals("Test name should be resolved from seeded data", "IntegrationTest-" + TEST_ID_A,
                mappingA.getTestName());
    }

    @Test
    public void testGetUnconfiguredMappings_excludesPairsWithRuleConfig() {
        // Arrange — control lot AND rule config exist for pair A
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "1\u2083\u209B", true, "REJECTION");

        // Act
        List<UnconfiguredMapping> mappings = ruleConfigService.getUnconfiguredMappings();

        // Assert — pair A should NOT appear (it has rule config)
        boolean hasTestPair = mappings.stream()
                .anyMatch(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A));
        assertFalse("Pair with rule config should not appear as unconfigured", hasTestPair);
    }

    @Test
    public void testGetUnconfiguredMappings_mixOfConfiguredAndUnconfigured() {
        // Arrange — pair A: control lot + rule config (configured)
        // pair B: control lot only (unconfigured)
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");
        insertRuleConfig(TEST_ID_A, INSTRUMENT_ID_A, "1\u2083\u209B", true, "REJECTION");
        insertControlLot(TEST_ID_B, INSTRUMENT_ID_B, "ACTIVE");

        // Act
        List<UnconfiguredMapping> mappings = ruleConfigService.getUnconfiguredMappings();

        // Assert — only pair B appears
        boolean hasPairA = mappings.stream()
                .anyMatch(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A));
        boolean hasPairB = mappings.stream()
                .anyMatch(m -> m.getTestId().equals(TEST_ID_B) && m.getInstrumentId().equals(INSTRUMENT_ID_B));

        assertFalse("Configured pair A should not appear", hasPairA);
        assertTrue("Unconfigured pair B should appear", hasPairB);

        // Verify pair B's field values
        UnconfiguredMapping mappingB = mappings.stream()
                .filter(m -> m.getTestId().equals(TEST_ID_B) && m.getInstrumentId().equals(INSTRUMENT_ID_B)).findFirst()
                .orElse(null);
        assertNotNull("Pair B mapping should exist", mappingB);
        assertEquals("Pair B active lot count", 1, mappingB.getActiveControlLotCount());
        assertEquals("Pair B test name", "IntegrationTest-" + TEST_ID_B, mappingB.getTestName());
    }

    @Test
    public void testGetUnconfiguredMappings_emptyWhenNoControlLots() {
        // Arrange — no control lots inserted

        // Act
        List<UnconfiguredMapping> mappings = ruleConfigService.getUnconfiguredMappings();

        // Assert — our test pairs should NOT appear
        boolean hasTestPair = mappings.stream().anyMatch(m -> m.getTestId().equals(TEST_ID_A));
        assertFalse("Test pair should not appear when no control lots exist", hasTestPair);
    }

    @Test
    public void testGetUnconfiguredMappings_activeLotCountScopedToTestAndInstrument() {
        // Arrange — two different tests on the SAME instrument, each with control lots
        // Pair A: 2 active lots for (TEST_ID_A, INSTRUMENT_ID_A)
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");
        // Pair B: 1 active lot for (TEST_ID_B, INSTRUMENT_ID_A) — same instrument!
        insertControlLot(TEST_ID_B, INSTRUMENT_ID_A, "ACTIVE");

        // Act
        List<UnconfiguredMapping> mappings = ruleConfigService.getUnconfiguredMappings();

        // Assert — each pair's count is scoped to its own (test, instrument)
        UnconfiguredMapping mappingA = mappings.stream()
                .filter(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);
        UnconfiguredMapping mappingB = mappings.stream()
                .filter(m -> m.getTestId().equals(TEST_ID_B) && m.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);

        assertNotNull("Pair A should appear", mappingA);
        assertNotNull("Pair B should appear", mappingB);
        assertEquals("Pair A should have 2 active lots (not 3)", 2, mappingA.getActiveControlLotCount());
        assertEquals("Pair B should have 1 active lot (not 3)", 1, mappingB.getActiveControlLotCount());
    }

    // ==================== createDefaultConfig integration test
    // ====================

    @Test
    public void testCreateDefaultConfig_thenAppearsInSummaries() {
        // Act — create default config via service
        List<WestgardRuleConfig> created = ruleConfigService.createDefaultConfig(TEST_ID_A, INSTRUMENT_ID_A);

        // Assert — 8 rules created
        assertEquals("Should create 8 rules", 8, created.size());

        // Assert — appears in summaries
        List<RuleConfigSummary> summaries = ruleConfigService.getAllRuleConfigSummaries();
        RuleConfigSummary summary = summaries.stream()
                .filter(s -> s.getTestId().equals(TEST_ID_A) && s.getInstrumentId().equals(INSTRUMENT_ID_A)).findFirst()
                .orElse(null);

        assertNotNull("Created config should appear in summaries", summary);
        assertEquals("Total rules should be 8", 8, summary.getTotalRuleCount());
        // STANDARD preset: 4 enabled (1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ)
        assertEquals("Enabled rules should match STANDARD preset", 4, summary.getEnabledRuleCount());
        assertEquals("Rules details should have 8 entries", 8, summary.getRules().size());
    }

    @Test
    public void testCreateDefaultConfig_removesFromUnconfigured() {
        // Arrange — control lot exists, no rule config yet
        insertControlLot(TEST_ID_A, INSTRUMENT_ID_A, "ACTIVE");

        // Verify it starts as unconfigured
        List<UnconfiguredMapping> before = ruleConfigService.getUnconfiguredMappings();
        boolean unconfiguredBefore = before.stream()
                .anyMatch(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A));
        assertTrue("Should be unconfigured before creating defaults", unconfiguredBefore);

        // Act — create default config
        ruleConfigService.createDefaultConfig(TEST_ID_A, INSTRUMENT_ID_A);

        // Assert — no longer unconfigured
        List<UnconfiguredMapping> after = ruleConfigService.getUnconfiguredMappings();
        boolean unconfiguredAfter = after.stream()
                .anyMatch(m -> m.getTestId().equals(TEST_ID_A) && m.getInstrumentId().equals(INSTRUMENT_ID_A));
        assertFalse("Should no longer be unconfigured after creating defaults", unconfiguredAfter);
    }
}
