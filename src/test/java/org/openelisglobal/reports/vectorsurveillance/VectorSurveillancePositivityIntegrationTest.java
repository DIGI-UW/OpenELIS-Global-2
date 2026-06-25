package org.openelisglobal.reports.vectorsurveillance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.PositivityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SporozoiteAggregate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test (full Spring + Liquibase via Testcontainers) for the
 * catalog-driven positivity rewrite in {@link VectorSurveillanceDAO}. The HQL
 * positivity join is a DATABASE concern: it can only be exercised against a
 * real Postgres with real {@code result -> test_result.significance} rows, so
 * this is the only level that can catch the positivity inversion that shipped.
 *
 * <p>
 * THE INVERSION GUARD ({@code negativePool_isNotCountedPositive}): pool 900 is
 * a CONFIRMED-NEGATIVE Anopheles pool — {@code deconvolution_status = COMPLETE}
 * but its catalog result carries {@code significance = NEGATIVE}. The OLD code
 * marked a pool positive whenever
 * {@code deconvolutionStatus <> NOT_APPLICABLE}, so it counted this common
 * confirmed-negative case as POSITIVE in the MIR numerator, the positivity
 * panel, and the resolution %. The new HQL keys off
 * {@code tr.significance = 'POSITIVE'}, so the negative pool must NOT appear.
 * This test FAILS against the old proxy and PASSES against the fix.
 *
 * <p>
 * Reference rows (species, samples, sample_items, pools, members,
 * identifications, tests, sites) come from a dbunit fixture; the
 * analysis/result/test_result rows are inserted via {@code jdbcTemplate} so we
 * can mix a pool-anchored analysis ({@code vector_pool_id}) with an
 * item-anchored leaf analysis ({@code sampitem_id}) and set
 * {@code significance} per result — neither of which FlatXmlDataSet
 * column-sensing can express cleanly.
 */
public class VectorSurveillancePositivityIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private VectorSurveillanceDAO dao;

    // Scope covers the fixture's collection_date 2026-07-06.
    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 31);

    private static final Integer SPECIES_ANOPHELES = 900;
    private static final String PATHOGEN_MALARIA = "Malaria Parasite Detection";
    private static final String ASSAY_CSP = "Pan-Plasmodium CSP ELISA";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/vector-surveillance-positivity.xml");
        seedAnalysesAndResults();
    }

    // ---- Analysis / Result / TestResult seeding (jdbcTemplate) ----------------

    /**
     * test_result rows: one POSITIVE and one NEGATIVE classification per test, so
     * positivity is driven entirely by which classified result a pool's analysis
     * points at — not by the workflow status.
     */
    private void seedAnalysesAndResults() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Catalog classifications for the malaria pathogen test (900).
        insertTestResult(9001, 900, "POSITIVE", "POSITIVE");
        insertTestResult(9002, 900, "NEGATIVE", "NEGATIVE");
        // Catalog classifications for the CSP/sporozoite assay (901).
        insertTestResult(9011, 901, "POSITIVE", "POSITIVE");
        insertTestResult(9012, 901, "NEGATIVE", "NEGATIVE");

        // Pool 900 (CONFIRMED-NEGATIVE): pool-anchored analysis with a NEGATIVE
        // malaria result. deconvolution_status is COMPLETE in the fixture — the
        // exact shape the old code mis-counted as positive.
        insertPoolAnalysis(9100, 900, 900);
        insertResult(9100, 9100, 9002, "NEGATIVE");

        // Pool 901 (POSITIVE, resolved): pool-anchored analysis with a POSITIVE
        // malaria result ...
        insertPoolAnalysis(9101, 901, 900);
        insertResult(9101, 9101, 9001, "POSITIVE");
        // ... plus an item-anchored leaf analysis on the deconvoluted individual
        // (sample_item 903) carrying a POSITIVE malaria result — the exact
        // individual positive the deconvolution-aware count must find.
        insertItemAnalysis(9103, 903, 900);
        insertResult(9103, 9103, 9001, "POSITIVE");

        // Pool 902 (POSITIVE sporozoite): pool-anchored CSP-ELISA POSITIVE result.
        insertPoolAnalysis(9102, 902, 901);
        insertResult(9102, 9102, 9011, "POSITIVE");

        // Pool 901 also carries a POSITIVE confirmatory Plasmodium PCR (test 902) —
        // NOT the CSP-ELISA sporozoite assay, so it must not count toward sporozoite.
        insertTestResult(9021, 902, "POSITIVE", "POSITIVE");
        insertPoolAnalysis(9105, 901, 902);
        insertResult(9105, 9105, 9021, "POSITIVE");

        // Pool 903 (Culex, site B): a malaria result that is NEGATIVE — present
        // only to prove cross-species / cross-site rows do not leak in.
        insertPoolAnalysis(9104, 903, 900);
        insertResult(9104, 9104, 9002, "NEGATIVE");
    }

    private void insertTestResult(long id, long testId, String value, String significance) {
        jdbcTemplate.update("INSERT INTO clinlims.test_result"
                + " (id, test_id, tst_rslt_type, value, significance, is_active, sort_order, lastupdated)"
                + " VALUES (?, ?, 'D', ?, ?, true, 1, now())", id, testId, value, significance);
    }

    private void insertPoolAnalysis(long id, long vectorPoolId, long testId) {
        insertAnalysis(id, null, vectorPoolId, testId);
    }

    private void insertItemAnalysis(long id, long sampleItemId, long testId) {
        insertAnalysis(id, sampleItemId, null, testId);
    }

    private void insertAnalysis(long id, Long sampleItemId, Long vectorPoolId, long testId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update("INSERT INTO clinlims.analysis"
                + " (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision, status_id, status,"
                + "  started_date, entry_date, analysis_type, reflex_trigger, referred_out, corrected,"
                + "  result_calculated, type_of_sample_name, fhir_uuid, lastupdated)"
                + " VALUES (?, ?, ?, ?, 900, 1, 900, '1', ?, ?, 'MANUAL', false, false, false, false,"
                + "  'Mosquito', ?::uuid, ?)", id, sampleItemId, vectorPoolId, testId, now, now, uuid(id), now);
    }

    private void insertResult(long id, long analysisId, long testResultId, String value) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "INSERT INTO clinlims.result"
                        + " (id, analysis_id, analyte_id, test_result_id, sort_order, result_type, value,"
                        + "  grouping, fhir_uuid, lastupdated)" + " VALUES (?, ?, 900, ?, 1, 'D', ?, 0, ?::uuid, ?)",
                id, analysisId, testResultId, value, uuid(id), now);
    }

    private static String uuid(long id) {
        return String.format("00000000-0000-4000-8000-%012d", id);
    }

    // ---- THE INVERSION GUARD --------------------------------------------------

    @Test
    public void negativePool_isNotCountedPositive_perPathogenMir() {
        List<SpeciesMirAggregate> mir = dao.getMirAggregates(FROM, TO, null);

        SpeciesMirAggregate anophelesMalaria = mir.stream()
                .filter(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && PATHOGEN_MALARIA.equals(a.getPathogen()))
                .findFirst().orElseThrow(() -> new AssertionError("expected an Anopheles x Malaria MIR row"));

        // Two Anopheles intake pools (900, 901) carry a malaria result, but only
        // pool 901 is significance=POSITIVE, so the fix counts exactly 1. The old
        // deconvolutionStatus proxy fails this test too — it produced no per-pathogen
        // row at all (pathogen was null), dying at the orElseThrow above — so this
        // assertion genuinely guards the fix rather than passing on the old code.
        assertEquals("only the catalog-POSITIVE pool counts; the confirmed-NEGATIVE pool"
                + " (decon COMPLETE) must NOT inflate the numerator", 1L, anophelesMalaria.getPositivePools());
    }

    @Test
    public void negativePool_isNotCountedPositive_positivityPanel() {
        List<PositivityAggregate> panel = dao.getPathogenPositivity(FROM, TO, null);

        PositivityAggregate malaria = panel.stream().filter(p -> PATHOGEN_MALARIA.equals(p.getPathogen())).findFirst()
                .orElseThrow(() -> new AssertionError("expected a Malaria positivity row"));

        // Three intake pools (900, 901, 903) were tested for malaria; only one
        // (901) is POSITIVE. Old proxy would have inflated poolsPositive.
        assertEquals("all malaria-tested intake pools are counted as tested", 3L, malaria.getPoolsTested());
        assertEquals("only the catalog-POSITIVE pool is positive; confirmed-negative pools must not count", 1L,
                malaria.getPoolsPositive());
    }

    // ---- Per-pathogen grouping ------------------------------------------------

    @Test
    public void mir_isGroupedPerSpeciesAndPathogenTest() {
        List<SpeciesMirAggregate> mir = dao.getMirAggregates(FROM, TO, null);

        // The malaria-pathogen row and the CSP-assay row are distinct grouping
        // rows for Anopheles (grouped by species x pathogen Test, not collapsed).
        boolean hasMalaria = mir.stream()
                .anyMatch(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && PATHOGEN_MALARIA.equals(a.getPathogen()));
        boolean hasCsp = mir.stream()
                .anyMatch(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && ASSAY_CSP.equals(a.getPathogen()));

        assertTrue("Anopheles x Malaria must be its own per-pathogen row", hasMalaria);
        assertTrue("Anopheles x CSP-ELISA must be its own per-pathogen row", hasCsp);
        // No Culex positive rows: its only malaria result is NEGATIVE.
        boolean anyCulexPositive = mir.stream().anyMatch(a -> !SPECIES_ANOPHELES.equals(a.getSpeciesId()));
        assertFalse("a Culex pool with only a NEGATIVE result must not produce a positive MIR row", anyCulexPositive);
    }

    @Test
    public void infectionRate_isDeconvolutionAware_countsIndividualPositive() {
        SpeciesMirAggregate row = dao.getMirAggregates(FROM, TO, null).stream()
                .filter(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && PATHOGEN_MALARIA.equals(a.getPathogen()))
                .findFirst().orElseThrow(() -> new AssertionError("expected the Anopheles x Malaria row"));

        // Pool 901 is fully resolved (decon COMPLETE) so there is no 1-per-pool
        // fallback; the observed count is exactly the individual positive leaves
        // (sample_item 903). The negative pool 900 contributes zero.
        assertEquals("resolved positive pool counts every malaria POSITIVE individual, not the pool", 1L,
                row.getObservedPositiveOrganisms());
        assertEquals("the one positive pool is fully resolved", 1L, row.getCompletelyResolvedPositivePools());
        assertEquals(1L, row.getTotalPositivePools());
    }

    // ---- Sporozoite rate ------------------------------------------------------

    @Test
    public void sporozoite_countsAnophelesCspPositivePoolsOverSpecimens() {
        SporozoiteAggregate spo = dao.getSporozoiteAggregate(FROM, TO, null);

        // Pool 902 is the only Anopheles CSP-ELISA POSITIVE pool. Pool 901 carries a
        // POSITIVE Plasmodium PCR (not the CSP/sporozoite assay) — a broad
        // "%plasmodium%" match would wrongly report 2.
        assertEquals("only the CSP-ELISA (LOINC 71712-2) positive pool counts; a"
                + " Plasmodium-PCR positive must not inflate the sporozoite rate", 1L, spo.getPositivePools());
        // Anopheles specimens in scope: items 900(10) + 901(10) + 902(8) + 903(1) = 29.
        // Culex item 904 (5) is excluded by genus.
        assertEquals("denominator is the Anopheles specimen total, excluding Culex", 29L, spo.getTotalSpecimens());
    }

    // ---- Degradation: positivity classification presence ----------------------

    @Test
    public void positivityClassificationPresent_trueWhenResultsCarrySignificance() {
        assertTrue("at least one in-scope pool result carries a significance tag",
                dao.isPositivityClassificationPresent(FROM, TO, null));
    }

    @Test
    public void positivityClassificationPresent_falseWhenNoSignificanceTags() {
        // Strip every significance tag: results still exist, but none is
        // classified — the "not configured" degradation state.
        jdbcTemplate.update(
                "UPDATE clinlims.test_result SET significance = NULL WHERE id IN (9001, 9002, 9011, 9012, 9021)");

        assertFalse("results without any significance classification must report 'not configured', not fake positives",
                dao.isPositivityClassificationPresent(FROM, TO, null));
    }

    // ---- Site filter (positivity scoped by collection location) ---------------

    @Test
    public void siteFilter_scopesPositivityToSelectedSite() {
        // Site 900 (A) holds the malaria pools 900/901; site 901 (B) holds only
        // the Culex pool 903. Filtering to site B yields no malaria positives.
        List<PositivityAggregate> siteB = dao.getPathogenPositivity(FROM, TO, 901);

        boolean malariaPositiveAtSiteB = siteB.stream()
                .anyMatch(p -> PATHOGEN_MALARIA.equals(p.getPathogen()) && p.getPoolsPositive() > 0);
        assertFalse("no malaria-positive pools belong to site B", malariaPositiveAtSiteB);

        List<PositivityAggregate> siteA = dao.getPathogenPositivity(FROM, TO, 900);
        PositivityAggregate malariaA = siteA.stream().filter(p -> PATHOGEN_MALARIA.equals(p.getPathogen())).findFirst()
                .orElse(null);
        assertNotNull("malaria positivity must be present for site A", malariaA);
        assertEquals("site A carries the one malaria-positive pool", 1L, malariaA.getPoolsPositive());
    }

    // ---- Scope guards (date / site) -------------------------------------------

    @Test
    public void qcPassRate_isScopedToTheDateRange() {
        // The seeded in-scope vector analyses count toward QC.
        QcAggregate inScope = dao.getQcPassRate(FROM, TO, null);
        assertTrue("seeded in-scope vector analyses count toward QC", inScope.getAnalysesTotal() > 0);

        // Out-of-range window: the old unscoped query counted ALL vector analyses in
        // the DB regardless of date; the scoped query must return nothing.
        QcAggregate outOfScope = dao.getQcPassRate(LocalDate.of(2099, 1, 1), LocalDate.of(2099, 12, 31), null);
        assertEquals("QC pass-rate must be scoped to the date range", 0L, outOfScope.getAnalysesTotal());
    }

    @Test
    public void sitesWithPositives_countsDistinctSitesWithAPositivePool() {
        // The positive pools (malaria-POSITIVE 901, CSP-POSITIVE 902) both collect at
        // site A — one distinct site with a positive pool; the Culex site B has none.
        assertEquals("one distinct site holds a positive pool", 1L, dao.countSitesWithPositives(FROM, TO, null));
    }
}
