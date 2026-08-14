package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.form.QCControlLotForm;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1147 gap B1 — a bench control lot (no analyzer) must be creatable,
 * findable and usable end to end.
 *
 * <p>
 * The database has allowed {@code qc_control_lot.instrument_id IS NULL} since
 * changeset qc-024, but four layers above it demanded an analyzer, so such a
 * lot could neither be created nor discovered. These tests pin each layer and,
 * just as importantly, pin that the analyzer path is not loosened: an analyzer
 * lot still validates as before, is still found by the analyzer lookups, and is
 * never returned by the bench lookup.
 *
 * <p>
 * Fixture {@code qc-initial-runs.xml} supplies test 1, analyzer 1 and user 1.
 */
public class QCBenchControlLotTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private QCStatisticsService statisticsService;

    @Autowired
    private QCResultService qcResultService;

    private static final String TEST_ID = "1";
    private static final String ANALYZER_ID = "1";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-initial-runs.xml");
        authenticateAs("testUser");
    }

    // ---------------- layer 2 + validator: creation ----------------

    @Test
    public void createControlLot_withNoAnalyzer_persistsABenchLotWithFixedStatistics() {
        QCControlLot created = controlLotService.createControlLot(benchLot("LOT-BENCH-CREATE", 100.0, 5.0));

        assertNull("a bench lot must persist with no analyzer at all", created.getInstrumentId());
        assertEquals(TEST_ID, created.getTestId());
        assertEquals("MANUFACTURER_FIXED", created.getCalculationMethod());
        // MANUFACTURER_FIXED lots are activated on create once their stats are seeded,
        // which is what makes the lot visible to the bench lookup immediately.
        assertEquals("ACTIVE", created.getStatus());

        QCStatistics stats = statisticsService.getLatestStatistics(created.getId());
        assertEquals(created.getId(), stats.getControlLotId());
        assertEquals(0, new BigDecimal("100.00000").compareTo(stats.getMean()));
        assertEquals(0, new BigDecimal("5.00000").compareTo(stats.getStandardDeviation()));
        assertEquals("MANUFACTURER_FIXED", stats.getCalculationMethod());

        // Reading it back proves the null survived the round trip through
        // LIMSStringNumberUserType rather than only living in the flushed entity.
        QCControlLot reloaded = controlLotService.get(created.getId());
        assertNull(reloaded.getInstrumentId());
        assertEquals("LOT-BENCH-CREATE", reloaded.getLotNumber());
    }

    @Test
    public void createControlLot_benchLotOnInitialRuns_isRefused() {
        QCControlLot lot = benchLot("LOT-BENCH-INITIAL", null, null);
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);

        try {
            controlLotService.createControlLot(lot);
            fail("expected a bench lot on INITIAL_RUNS to be refused — nothing accumulates runs for it (D3)");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("must use the MANUFACTURER_FIXED"));
        }

        // And it must not have been written before the refusal.
        assertEquals(0, controlLotService.getActiveBenchControlLots(TEST_ID).size());
        assertNull(controlLotService.getControlLotByLotNumber("LOT-BENCH-INITIAL"));
    }

    @Test
    public void createControlLot_benchLotOnRolling_isRefused() {
        QCControlLot lot = benchLot("LOT-BENCH-ROLLING", null, null);
        lot.setCalculationMethod("ROLLING");

        try {
            controlLotService.createControlLot(lot);
            fail("expected a bench lot on ROLLING to be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("must use the MANUFACTURER_FIXED"));
        }
    }

    @Test
    public void update_switchingABenchLotOffFixedTargets_isRefused() {
        QCControlLot created = controlLotService.createControlLot(benchLot("LOT-BENCH-SWITCH", 12.0, 1.0));
        created.setCalculationMethod("ROLLING");

        try {
            controlLotService.update(created);
            fail("expected the rule to hold on update, not only on create");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("must use the MANUFACTURER_FIXED"));
        }
    }

    // ---------------- layer 2: lookup ----------------

    @Test
    public void getActiveBenchControlLots_returnsOnlyLotsWithNoAnalyzer() {
        QCControlLot bench = controlLotService.createControlLot(benchLot("LOT-BENCH-FIND", 100.0, 5.0));
        QCControlLot analyzerLot = controlLotService
                .createControlLot(analyzerLot("LOT-ANALYZER-FIND", "MANUFACTURER_FIXED", 100.0, 5.0));

        List<String> benchLotNumbers = lotNumbers(controlLotService.getActiveBenchControlLots(TEST_ID));
        assertEquals("exactly the bench lot, and nothing keyed to an analyzer", List.of("LOT-BENCH-FIND"),
                benchLotNumbers);
        assertEquals(bench.getId(), controlLotService.getActiveBenchControlLots(TEST_ID).get(0).getId());

        // The converse: the analyzer lookups still see the analyzer lot and must not
        // start seeing the bench one now that instrument_id can be null.
        List<String> byTestAndInstrument = lotNumbers(controlLotService.getActiveControlLots(TEST_ID, ANALYZER_ID));
        assertEquals(List.of("LOT-ANALYZER-FIND"), byTestAndInstrument);
        assertEquals(analyzerLot.getId(), controlLotService.getActiveControlLots(TEST_ID, ANALYZER_ID).get(0).getId());
        assertEquals(List.of("LOT-ANALYZER-FIND"),
                lotNumbers(controlLotService.getActiveControlLotsByInstrument(ANALYZER_ID)));
    }

    @Test
    public void getActiveBenchControlLots_excludesLotsForOtherTestsAndInactiveLots() {
        QCControlLot active = controlLotService.createControlLot(benchLot("LOT-BENCH-ACTIVE", 8.0, 0.5));
        QCControlLot retired = controlLotService.createControlLot(benchLot("LOT-BENCH-RETIRED", 8.0, 0.5));
        controlLotService.deactivateControlLot(retired.getId());

        List<QCControlLot> found = controlLotService.getActiveBenchControlLots(TEST_ID);
        assertEquals(List.of("LOT-BENCH-ACTIVE"), lotNumbers(found));
        assertEquals(active.getId(), found.get(0).getId());

        // A different test id shares the bench-ness but must not share the lot.
        assertEquals(List.of(), lotNumbers(controlLotService.getActiveBenchControlLots("999999")));
    }

    // ---------------- analyzer lots are not loosened ----------------

    @Test
    public void analyzerLotOnInitialRuns_stillValidatesAndIsUnaffectedByTheBenchRule() {
        QCControlLot created = controlLotService
                .createControlLot(analyzerLot("LOT-ANALYZER-INITIAL", "INITIAL_RUNS", null, null));

        assertEquals(ANALYZER_ID, created.getInstrumentId());
        assertEquals("INITIAL_RUNS", created.getCalculationMethod());
        // Run-derived methods remain legal for an analyzer lot; it just starts
        // unestablished.
        assertEquals("ESTABLISHMENT", created.getStatus());
        assertNull(statisticsService.getLatestStatistics(created.getId()));
        assertEquals(List.of(), lotNumbers(controlLotService.getActiveBenchControlLots(TEST_ID)));
    }

    /**
     * Layer 1. The form had {@code @NotBlank} plus a regex that rejects the empty
     * string a cleared dropdown posts; both had to go for a bench lot to be
     * postable. A non-numeric or zero analyzer must still be rejected, which is the
     * half of the change that is easy to lose.
     */
    @Test
    public void form_acceptsAnOmittedAnalyzerButStillRejectsAMalformedOne() {
        // ParameterMessageInterpolator: the default one needs a jakarta.el provider,
        // which the container supplies at runtime but the test classpath does not.
        Validator validator = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

        assertEquals("an omitted analyzer must not be a binding error", 0,
                instrumentIdViolations(validator, null).size());
        assertEquals("a cleared dropdown posts \"\" and must bind as a bench lot", 0,
                instrumentIdViolations(validator, "").size());
        assertEquals(ANALYZER_ID, acceptedInstrumentId(validator, ANALYZER_ID));

        assertEquals("a non-numeric analyzer id is still refused", 1, instrumentIdViolations(validator, "abc").size());
        assertEquals("a zero analyzer id is still refused", 1, instrumentIdViolations(validator, "0").size());
    }

    // ---------------- end to end: a bench lot is usable ----------------

    @Test
    public void manualQuantitativeCapture_againstABenchLot_computesAZScore() {
        QCControlLot bench = controlLotService.createControlLot(benchLot("LOT-BENCH-CAPTURE", 100.0, 5.0));

        BenchQCCaptureForm capture = new BenchQCCaptureForm();
        capture.setSource(QCSource.MANUAL);
        capture.setTestId(TEST_ID);
        capture.setControlLotId(bench.getId());
        capture.setResultValue(new BigDecimal("112.50000"));
        capture.setUnitOfMeasure("mg/dL");
        capture.setQualitativeOutcome(QCQualitativeOutcome.PASS);
        capture.setRunDateTime(LocalDateTime.now());

        QCResult saved = qcResultService.createBenchQCResult(capture, 1);

        assertEquals(QCSource.MANUAL, saved.getSource());
        assertEquals(bench.getId(), saved.getControlLotId());
        // (112.5 - 100) / 5 = 2.5000 — from the bench lot's seeded fixed statistics,
        // which is the whole point of making the lot reachable.
        assertEquals(0, new BigDecimal("2.5000").compareTo(saved.getZScore()));
        assertEquals(0, new BigDecimal("112.50000").compareTo(saved.getResultValue()));
        assertEquals("ACCEPTED", saved.getResultStatus());
        assertNull("neither the lot nor the run has an analyzer", saved.getInstrumentId());
    }

    // ---------------- helpers ----------------

    private QCControlLot benchLot(String lotNumber, Double mean, Double stdDev) {
        QCControlLot lot = baseLot(lotNumber);
        lot.setInstrumentId(null);
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(mean);
        lot.setManufacturerStdDev(stdDev);
        return lot;
    }

    private QCControlLot analyzerLot(String lotNumber, String calculationMethod, Double mean, Double stdDev) {
        QCControlLot lot = baseLot(lotNumber);
        lot.setInstrumentId(ANALYZER_ID);
        lot.setCalculationMethod(calculationMethod);
        lot.setInitialRunsCount(20);
        lot.setManufacturerMean(mean);
        lot.setManufacturerStdDev(stdDev);
        return lot;
    }

    private QCControlLot baseLot(String lotNumber) {
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber(lotNumber);
        lot.setProductName("Bench Glucose Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId(TEST_ID);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);
        return lot;
    }

    private List<String> lotNumbers(List<QCControlLot> lots) {
        return lots.stream().map(QCControlLot::getLotNumber).sorted().collect(Collectors.toList());
    }

    private Set<?> instrumentIdViolations(Validator validator, String instrumentId) {
        QCControlLotForm form = new QCControlLotForm();
        form.setInstrumentId(instrumentId);
        return validator.validateProperty(form, "instrumentId");
    }

    private String acceptedInstrumentId(Validator validator, String instrumentId) {
        assertEquals(0, instrumentIdViolations(validator, instrumentId).size());
        QCControlLotForm form = new QCControlLotForm();
        form.setInstrumentId(instrumentId);
        return form.getInstrumentId();
    }
}
