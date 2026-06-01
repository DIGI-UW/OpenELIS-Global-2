package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dto.LabelCell;
import org.openelisglobal.labelpreset.dto.LabelColumn;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestPayload;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestResponse;
import org.openelisglobal.labelpreset.dto.SourceType;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aggregation tests for {@link OrderEntryLabelRequestServiceImpl} (OGC-285 M5,
 * T132). Implements the FRS §4.4.1 conflict-resolution rules / data-model.md
 * §6.1 against a real DB ({@link BaseWebContextSensitiveTest}) — no mocks of
 * the service, DAOs, or DTOs (anti-mocking discipline, Constitution V.6).
 *
 * <p>
 * Scenario (per task T132): two real tests from {@code testdata/test.xml} —
 * Test id=1 ("Complete Blood Count" / CBC) and Test id=2 ("Urinalysis", playing
 * the "Tissue Biopsy" role). CBC links a custom per-sample "Specimen" preset
 * (default 1, max 5, allow_override true). The second test links the same
 * "Specimen" preset (default 2, max 6, allow_override true) AND a custom
 * "Slide" preset (default 4, max 12, allow_override false).
 *
 * <p>
 * The fixture seeds <em>custom</em> (non-system) presets so the assertions are
 * not coupled to the 5 Liquibase-seeded system presets that coexist in the test
 * DB. Per §6.1 step 2, per-sample columns appear ONLY for presets linked by a
 * test in the order, so the seeded system per-sample presets do not leak into
 * {@code sample_columns} here.
 *
 * <p>
 * Inversion target (T153):
 * {@link #ac17_highestDefaultWins_specimenPrePopulatesAtTwo()} pins
 * MAX(default_qty)=2 — mutating the impl to MIN must fail it.
 */
public class OrderEntryLabelRequestServiceAggregationTest extends BaseWebContextSensitiveTest {

    private static final String CBC_TEST_ID = "1";
    private static final String SECOND_TEST_ID = "2";
    private static final String CBC_TEST_NAME = "Complete Blood Count";
    private static final String SECOND_TEST_NAME = "Urinalysis";

    private static final String SPECIMEN_PRESET = "T132-AggSpecimen";
    private static final String SLIDE_PRESET = "T132-AggSlide";
    private static final String UNLINKED_PRESET = "T132-AggUnlinked";
    private static final String CUSTOM_A_PRESET = "T132-aaa-custom";
    private static final String CUSTOM_Z_PRESET = "T132-zzz-custom";

    @Autowired
    private OrderEntryLabelRequestService orderEntryLabelRequestService;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private TestService testService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private LabelPreset specimenPreset;
    private LabelPreset slidePreset;
    private LabelPreset unlinkedPreset;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanTestData();

        assertNotNull("test.xml must supply Test id=1", testService.getTestById(CBC_TEST_ID));
        assertNotNull("test.xml must supply Test id=2", testService.getTestById(SECOND_TEST_ID));

        // Custom per-sample presets (is_system=false) so columns are not coupled to
        // the seeded system presets.
        specimenPreset = savePerSamplePreset(SPECIMEN_PRESET, 1, 5);
        slidePreset = savePerSamplePreset(SLIDE_PRESET, 4, 12);
        unlinkedPreset = savePerSamplePreset(UNLINKED_PRESET, 3, 9);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    // ── AC-17: highest default wins ───────────────────────────────────────────

    @Test
    public void ac17_highestDefaultWins_specimenPrePopulatesAtTwo() {
        // CBC links Specimen @ default 1; second test links Specimen @ default 2.
        linkTestToPreset(CBC_TEST_ID, specimenPreset, 1, 5, true);
        linkTestToPreset(SECOND_TEST_ID, specimenPreset, 2, 6, true);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L, 2L), "S1"));

        LabelCell specimenCell = sampleCell(response, "S1", specimenPreset.getId());
        assertEquals("MAX(default_qty) across linking tests = 2", 2, specimenCell.getDefaultQty());
        assertEquals("MAX(max_qty) across linking tests = 6", 6, specimenCell.getMax());
        assertEquals(SourceType.TEST, specimenCell.getSource());
        assertEquals("source test is the one driving the highest default (id=2)", Long.valueOf(2L),
                specimenCell.getSourceTestId());
        assertEquals(SECOND_TEST_NAME, specimenCell.getSourceTestName());
    }

    // ── AC-16: most-restrictive allow_override (conflicting links lock) ─────────

    @Test
    public void ac16_mostRestrictive_conflictingAllowOverrideLocks() {
        // Two tests link the same preset; one allows override, one does not → locked.
        linkTestToPreset(CBC_TEST_ID, specimenPreset, 1, 5, true);
        linkTestToPreset(SECOND_TEST_ID, specimenPreset, 2, 6, false);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L, 2L), "S1"));

        LabelCell specimenCell = sampleCell(response, "S1", specimenPreset.getId());
        assertTrue("any allow_override=false among linking tests locks the cell", specimenCell.getLocked());
    }

    // ── AC-15: a single link with allow_override=false locks ───────────────────

    @Test
    public void ac15_allowOverrideFalse_locksCell() {
        linkTestToPreset(SECOND_TEST_ID, slidePreset, 4, 12, false);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(2L), "S1"));

        LabelCell slideCell = sampleCell(response, "S1", slidePreset.getId());
        assertTrue("allow_override=false locks the cell", slideCell.getLocked());
        assertEquals(4, slideCell.getDefaultQty());
        assertEquals(12, slideCell.getMax());
    }

    @Test
    public void ac16_configOverrideFalse_locksCellEvenWhenLinkAllows() {
        // Link allows override, but the test's config disables order-entry override.
        // replace() creates BOTH the link (allow_override=true) and the config
        // (allow_order_entry_override=false) atomically.
        setConfigAllowOverride(CBC_TEST_ID, specimenPreset, 1, 5, true, false);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L), "S1"));

        LabelCell specimenCell = sampleCell(response, "S1", specimenPreset.getId());
        assertTrue("config.allow_order_entry_override=false locks the cell", specimenCell.getLocked());
    }

    // ── FR-014: system-default fallback for an unlinked per-sample preset ──────

    @Test
    public void fr014_unlinkedPreset_usesPresetDefaultAndIsAbsentFromColumns() {
        // Only Specimen is linked; the unlinked preset must NOT appear as a column.
        linkTestToPreset(CBC_TEST_ID, specimenPreset, 1, 5, true);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L), "S1"));

        assertTrue("unlinked per-sample preset must not be a sample column",
                response.getSampleColumns().stream().noneMatch(c -> unlinkedPreset.getId().equals(c.getPresetId())));
        assertTrue("only-linked Specimen preset is a sample column",
                response.getSampleColumns().stream().anyMatch(c -> specimenPreset.getId().equals(c.getPresetId())));
    }

    @Test
    public void fr014_systemDefaultFallback_whenPresetLinkedButNoLinkForThisOrdersTests() {
        // Link Slide to the SECOND test only, but order CBC (test 1) only.
        // Slide is then NOT a column for a CBC-only order (no linking test in order).
        linkTestToPreset(CBC_TEST_ID, specimenPreset, 1, 5, true);
        linkTestToPreset(SECOND_TEST_ID, slidePreset, 4, 12, false);

        OrderEntryLabelRequestResponse cbcOnly = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L), "S1"));
        assertTrue("Slide preset (linked only to the second test) must be absent for a CBC-only order",
                cbcOnly.getSampleColumns().stream().noneMatch(c -> slidePreset.getId().equals(c.getPresetId())));
    }

    // ── AC-13: column ordering — system presets first (by id), then custom A→Z ──

    @Test
    public void ac13_columnOrdering_systemFirstThenCustomAlphabetical() {
        // Two custom presets named to bracket alphabetically; both linked.
        LabelPreset customZ = savePerSamplePreset(CUSTOM_Z_PRESET, 1, 5);
        LabelPreset customA = savePerSamplePreset(CUSTOM_A_PRESET, 1, 5);
        linkTestToPreset(CBC_TEST_ID, customZ, 1, 5, true);
        linkTestToPreset(CBC_TEST_ID, customA, 1, 5, true);

        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L), "S1"));

        List<LabelColumn> cols = response.getSampleColumns();
        // All system columns (if any) precede all custom columns.
        int lastSystemIdx = -1;
        int firstCustomIdx = Integer.MAX_VALUE;
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).getIsSystem()) {
                lastSystemIdx = i;
            } else if (firstCustomIdx == Integer.MAX_VALUE) {
                firstCustomIdx = i;
            }
        }
        if (lastSystemIdx >= 0 && firstCustomIdx != Integer.MAX_VALUE) {
            assertTrue("all system columns precede all custom columns", lastSystemIdx < firstCustomIdx);
        }

        // Among my custom presets, "aaa" sorts before "zzz".
        int idxA = indexOfPreset(cols, customA.getId());
        int idxZ = indexOfPreset(cols, customZ.getId());
        assertTrue("custom 'aaa' present", idxA >= 0);
        assertTrue("custom 'zzz' present", idxZ >= 0);
        assertTrue("custom presets sorted alphabetically (aaa before zzz)", idxA < idxZ);
    }

    // ── Per-order columns: active per-order presets appear regardless of tests ──

    @Test
    public void perOrderColumns_includeSeededSystemOrderPreset_withPresetDefaultSource() {
        // No tests, no samples — per-order columns still computed (step 1).
        OrderEntryLabelRequestResponse response = orderEntryLabelRequestService
                .computeLabelRequest(payload(new ArrayList<>(), null));

        assertFalse("at least the seeded 'Order Label' per-order preset is a column",
                response.getOrderColumns().isEmpty());
        assertFalse("per-order row has a cell per per-order preset", response.getOrderRow().getCells().isEmpty());
        for (LabelCell cell : response.getOrderRow().getCells()) {
            assertEquals("per-order cells are never test-driven", SourceType.PRESET_DEFAULT, cell.getSource());
            assertFalse("per-order cells are never order-entry-locked", cell.getLocked());
        }
    }

    @Test
    public void determinism_sameInputsProduceSameOutput() {
        linkTestToPreset(CBC_TEST_ID, specimenPreset, 1, 5, true);
        linkTestToPreset(SECOND_TEST_ID, specimenPreset, 2, 6, true);

        OrderEntryLabelRequestResponse first = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L, 2L), "S1"));
        OrderEntryLabelRequestResponse second = orderEntryLabelRequestService
                .computeLabelRequest(payload(List.of(1L, 2L), "S1"));

        LabelCell c1 = sampleCell(first, "S1", specimenPreset.getId());
        LabelCell c2 = sampleCell(second, "S1", specimenPreset.getId());
        assertEquals(c1.getDefaultQty(), c2.getDefaultQty());
        assertEquals(c1.getMax(), c2.getMax());
        assertEquals(c1.getLocked(), c2.getLocked());
        assertEquals(c1.getSourceTestId(), c2.getSourceTestId());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private OrderEntryLabelRequestPayload payload(List<Long> testIds, String sampleLocalId) {
        OrderEntryLabelRequestPayload payload = new OrderEntryLabelRequestPayload();
        payload.setTestIds(testIds);
        List<OrderEntryLabelRequestPayload.SampleRef> samples = new ArrayList<>();
        if (sampleLocalId != null) {
            OrderEntryLabelRequestPayload.SampleRef ref = new OrderEntryLabelRequestPayload.SampleRef();
            ref.setSampleIdLocal(sampleLocalId);
            ref.setSampleType("BLOOD");
            samples.add(ref);
        }
        payload.setSamples(samples);
        return payload;
    }

    private LabelCell sampleCell(OrderEntryLabelRequestResponse response, String localId, Integer presetId) {
        OrderEntryLabelRequestResponse.SampleRow row = response.getSampleRows().stream()
                .filter(r -> localId.equals(r.getSampleIdLocal())).findFirst()
                .orElseThrow(() -> new AssertionError("no sample row for " + localId));
        return row.getCells().stream().filter(c -> presetId.equals(c.getPresetId())).findFirst()
                .orElseThrow(() -> new AssertionError("no cell for preset " + presetId + " in row " + localId));
    }

    private int indexOfPreset(List<LabelColumn> cols, Integer presetId) {
        for (int i = 0; i < cols.size(); i++) {
            if (presetId.equals(cols.get(i).getPresetId())) {
                return i;
            }
        }
        return -1;
    }

    private LabelPreset savePerSamplePreset(String name, int defaultPerSample, int maxPerSample) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(25);
        preset.setWidthMm(50);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(false);
        preset.setPrintsPerSample(true);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(defaultPerSample);
        preset.setMaxPerSample(maxPerSample);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        labelPresetDAO.insert(preset);
        return preset;
    }

    private void linkTestToPreset(String testId, LabelPreset preset, int defaultQty, int maxQty,
            boolean allowOverride) {
        TestLabelPresetLink link = new TestLabelPresetLink();
        link.setTest(testService.getTestById(testId));
        link.setPreset(preset);
        link.setDefaultQty(defaultQty);
        link.setMaxQty(maxQty);
        link.setAllowOverride(allowOverride);
        link.setSysUserId(TEST_SYS_USER_ID);
        testLabelPresetLinkService.insert(link);
    }

    /**
     * Create a link (allow_override = {@code linkAllowOverride}) AND a config row
     * (allow_order_entry_override = {@code configAllowOverride}) for the test. The
     * link is inserted through the service with the managed preset entity (same as
     * {@link #linkTestToPreset}); the config row is inserted directly via JDBC to
     * keep the fixture independent of the M3 {@code replace()} link-rebuild path.
     */
    private void setConfigAllowOverride(String testId, LabelPreset preset, int defaultQty, int maxQty,
            boolean linkAllowOverride, boolean configAllowOverride) {
        linkTestToPreset(testId, preset, defaultQty, maxQty, linkAllowOverride);
        jdbcTemplate.update(
                "INSERT INTO test_label_config (id, test_id, allow_order_entry_override, last_updated) "
                        + "VALUES (nextval('test_label_config_seq'), ?, ?, CURRENT_TIMESTAMP)",
                Long.valueOf(testId), configAllowOverride);
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.update("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T132-%')");
            jdbcTemplate.update("DELETE FROM test_label_preset_link WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T132-%')");
            jdbcTemplate.update("DELETE FROM test_label_config WHERE test_id IN ('1','2')");
            jdbcTemplate.update("DELETE FROM label_preset WHERE name LIKE 'T132-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
