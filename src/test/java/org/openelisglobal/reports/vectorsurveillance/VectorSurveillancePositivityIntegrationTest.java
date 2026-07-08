package org.openelisglobal.reports.vectorsurveillance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.DensityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.EffortAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.PositivityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SporozoiteAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.DensityRow;
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

    @Autowired
    private VectorSurveillanceService service;

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

    /** Flags a sample_item as a lab QC control/blank (the exclusion trigger). */
    private void insertQcProfile(String id, long sampleItemId, String qcType) {
        jdbcTemplate.update("INSERT INTO clinlims.sample_item_qc_profile"
                + " (id, sample_item_id, qc_type, sys_user_id) VALUES (?, ?, ?, 1)", id, sampleItemId, qcType);
    }

    /**
     * Inserts a LITERAL observation on a sample, resolving (or seeding) the type id
     * by name.
     */
    private void insertObservation(long id, long sampleId, String typeName, String value) {
        Long typeId = ensureObservationHistoryType(typeName);
        jdbcTemplate.update("INSERT INTO clinlims.observation_history"
                + " (id, sample_id, observation_history_type_id, value_type, value, lastupdated)"
                + " VALUES (?, ?, ?, 'L', ?, now())", id, sampleId, typeId, value);
    }

    /**
     * Resolve the observation_history_type id by name, inserting the type row if
     * absent. The vecTrapCount/vecTrapNights types are Liquibase-seeded (053), but
     * a sibling test's {@code TRUNCATE ... CASCADE} wipes that seed for the rest of
     * the suite and Liquibase never re-seeds mid-run — so this mirrors the base
     * test's {@code ensureReferenceTable}/{@code ensureAuditSystemUser}
     * resolve-or-insert resilience rather than assuming the seed survives.
     */
    private Long ensureObservationHistoryType(String typeName) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM clinlims.observation_history_type WHERE type_name = ?", Long.class, typeName);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbcTemplate.update(
                "INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                        + " VALUES (nextval('clinlims.observation_history_type_seq'), ?, ?, now())",
                typeName, typeName);
        return jdbcTemplate.queryForObject("SELECT id FROM clinlims.observation_history_type WHERE type_name = ?",
                Long.class, typeName);
    }

    private DensityRow densityRowForSite(List<DensityRow> rows, int siteId) {
        return rows.stream().filter(d -> Integer.valueOf(siteId).equals(d.getSiteId())).findFirst()
                .orElseThrow(() -> new AssertionError("expected a density row for site " + siteId));
    }

    private PositivityAggregate malariaRow(List<PositivityAggregate> panel) {
        return panel.stream().filter(p -> PATHOGEN_MALARIA.equals(p.getPathogen())).findFirst()
                .orElseThrow(() -> new AssertionError("expected a Malaria positivity row"));
    }

    private SpeciesMirAggregate anophelesMalariaMir(List<SpeciesMirAggregate> mir) {
        return mir.stream()
                .filter(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && PATHOGEN_MALARIA.equals(a.getPathogen()))
                .findFirst().orElseThrow(() -> new AssertionError("expected the Anopheles x Malaria MIR row"));
    }

    private long siteADensitySpecimens(List<DensityAggregate> density) {
        return density.stream().filter(d -> Integer.valueOf(900).equals(d.getSiteId()))
                .mapToLong(DensityAggregate::getSpecimenCount).sum();
    }

    private long anophelesSpecimens(List<SpeciesAggregate> dist) {
        return dist.stream().filter(s -> "Anopheles".equals(s.getGenus())).mapToLong(SpeciesAggregate::getSpecimenCount)
                .sum();
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

    // ---- QC-sample exclusion (FR-002 / SC-005) --------------------------------

    // A pool whose member sample_item is flagged as a lab QC control/blank
    // (sample_item_qc_profile.qc_type) is not a field observation and must leave
    // BOTH the numerator and the denominator of every surveillance index. This is
    // the inversion test for SC-005: adding the QC flag to the one POSITIVE malaria
    // pool (901) drops it from tested AND positive counts.
    @Test
    public void qcControlPool_isExcludedFromPositivity_numeratorAndDenominator_SC005() {
        PositivityAggregate before = malariaRow(dao.getPathogenPositivity(FROM, TO, null));
        assertEquals("baseline: 3 malaria-tested pools", 3L, before.getPoolsTested());
        assertEquals("baseline: 1 positive malaria pool (901)", 1L, before.getPoolsPositive());

        insertQcProfile("qc-901", 901, "CONTROL");

        PositivityAggregate after = malariaRow(dao.getPathogenPositivity(FROM, TO, null));
        assertEquals("QC control pool excluded from the tested denominator", 2L, after.getPoolsTested());
        assertEquals("QC control pool's POSITIVE excluded from the numerator", 0L, after.getPoolsPositive());
    }

    // A DUPLICATE is a QC replicate of a real specimen (reproducibility check), not
    // a
    // second field observation, so it is a QC artifact too. Guards both the
    // collection-density path and the DUPLICATE qc_type; removing DUPLICATE from
    // the
    // exclusion predicate makes this test fail.
    @Test
    public void qcSample_isExcludedFromCollectionDensity_coversDuplicateQcType_SC005() {
        assertEquals("baseline: site A density sums its three pool members 10+10+8", 28L,
                siteADensitySpecimens(dao.getCollectionDensity(FROM, TO, null)));

        insertQcProfile("qc-dens-900", 900, "DUPLICATE");

        assertEquals("QC DUPLICATE member (qty 10) must leave the density specimen count", 18L,
                siteADensitySpecimens(dao.getCollectionDensity(FROM, TO, null)));
    }

    @Test
    public void qcSample_isExcludedFromMirNumerator_SC005() {
        assertNotNull("baseline: the POSITIVE malaria pool 901 yields an Anopheles x Malaria MIR row",
                anophelesMalariaMir(dao.getMirAggregates(FROM, TO, null)));

        insertQcProfile("qc-mir-901", 901, "CONTROL");

        boolean present = dao.getMirAggregates(FROM, TO, null).stream()
                .anyMatch(a -> SPECIES_ANOPHELES.equals(a.getSpeciesId()) && PATHOGEN_MALARIA.equals(a.getPathogen()));
        assertFalse("the only POSITIVE malaria pool is now a QC control; its MIR row must disappear", present);
    }

    // Leaf sample_item 903 is the deconvoluted individual positive counted by the
    // observed-organism numerator (an item-anchored analysis). Flagging the leaf
    // (not
    // its parent pool member 901) leaves the pool-level MIR row intact but must
    // drop
    // the individual from the observed count — guards the observedPositiveOrganisms
    // path.
    @Test
    public void qcSample_isExcludedFromObservedOrganismCount_SC005() {
        assertEquals("baseline: one observed positive individual (leaf 903)", 1L,
                anophelesMalariaMir(dao.getMirAggregates(FROM, TO, null)).getObservedPositiveOrganisms());

        insertQcProfile("qc-leaf-903", 903, "CONTROL");

        assertEquals("QC-flagged individual leaf must leave the observed-organism numerator", 0L,
                anophelesMalariaMir(dao.getMirAggregates(FROM, TO, null)).getObservedPositiveOrganisms());
    }

    @Test
    public void qcSample_isExcludedFromSporozoite_numeratorAndDenominator_SC005() {
        SporozoiteAggregate before = dao.getSporozoiteAggregate(FROM, TO, null);
        assertEquals("baseline: one CSP-ELISA POSITIVE Anopheles pool", 1L, before.getPositivePools());
        assertEquals("baseline: 29 Anopheles specimens in scope", 29L, before.getTotalSpecimens());

        insertQcProfile("qc-spo-902", 902, "CONTROL");

        SporozoiteAggregate after = dao.getSporozoiteAggregate(FROM, TO, null);
        assertEquals("QC pool must leave the sporozoite numerator", 0L, after.getPositivePools());
        assertEquals("QC specimens (qty 8) must leave the sporozoite denominator", 21L, after.getTotalSpecimens());
    }

    @Test
    public void qcSample_isExcludedFromSpeciesDistribution_SC005() {
        assertEquals("baseline: Anopheles specimens 10+10+8+1", 29L,
                anophelesSpecimens(dao.getSpeciesDistribution(FROM, TO, null)));

        insertQcProfile("qc-species-900", 900, "BLANK");

        assertEquals("QC blank (qty 10) must leave the species-distribution count", 19L,
                anophelesSpecimens(dao.getSpeciesDistribution(FROM, TO, null)));
    }

    @Test
    public void qcSample_isExcludedFromSitesWithPositives_SC005() {
        assertEquals("baseline: site A holds a positive pool", 1L, dao.countSitesWithPositives(FROM, TO, null));

        // Site A's only positives are pools 901 (malaria) and 902 (CSP); flag both
        // members.
        insertQcProfile("qc-site-901", 901, "CONTROL");
        insertQcProfile("qc-site-902", 902, "CONTROL");

        assertEquals("with both positive pools QC-flagged, no site has a countable positive", 0L,
                dao.countSitesWithPositives(FROM, TO, null));
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

    // Data-quality guard: a non-null significance outside the recognized set (a
    // typo or legacy value that can never be counted) must be detected so the
    // dashboard warns instead of silently trusting a mixed catalog.
    @Test
    public void unrecognizedClassification_isDetected_forDataQualityGuard() {
        assertFalse("baseline: every seeded significance is a recognized classification",
                dao.hasUnrecognizedPositivityClassification(FROM, TO, null));

        // INDETERMINATE is the third recognized classification; it must NOT be flagged
        // (guards against RECOGNIZED_SIGNIFICANCE dropping an enum value).
        jdbcTemplate.update("UPDATE clinlims.test_result SET significance = 'INDETERMINATE' WHERE id = 9002");
        assertFalse("INDETERMINATE is a recognized classification and must not be flagged",
                dao.hasUnrecognizedPositivityClassification(FROM, TO, null));

        // A non-null significance outside the recognized set (typo/legacy) must be
        // flagged. 9001 is the malaria POSITIVE result on pool 901, which collects at
        // site 900 (A); pool 903 at site 901 (B) carries only recognized values.
        jdbcTemplate.update("UPDATE clinlims.test_result SET significance = 'POSITIF' WHERE id = 9001");
        assertTrue("a non-null significance outside the recognized set must be flagged",
                dao.hasUnrecognizedPositivityClassification(FROM, TO, null));

        assertTrue("the unrecognized value is at site A and must be flagged when scoped there",
                dao.hasUnrecognizedPositivityClassification(FROM, TO, 900));
        assertFalse("site B carries only recognized values, so it must not be flagged",
                dao.hasUnrecognizedPositivityClassification(FROM, TO, 901));
    }

    // ---- Collection density: trap-night effort (organisms per trap-night) -----

    // The effort HQL joins vecTrapCount/vecTrapNights observations to each pool's
    // Sample; only a real Postgres exercises the id user-types + observation join.
    // Site A pools 900/901 record both effort values; pool 902 records none here.
    @Test
    public void getCollectionEffort_returnsRecordedTrapEffortPerPool() {
        insertObservation(9201, 900, "vecTrapCount", "2");
        insertObservation(9202, 900, "vecTrapNights", "1");
        insertObservation(9203, 901, "vecTrapCount", "2");
        insertObservation(9204, 901, "vecTrapNights", "2");

        List<EffortAggregate> effort = dao.getCollectionEffort(FROM, TO, null);

        long siteAEffort = effort.stream().filter(e -> Integer.valueOf(900).equals(e.getSiteId()))
                .mapToLong(e -> Long.parseLong(e.getTrapCount()) * Long.parseLong(e.getTrapNights())).sum();
        assertEquals("2x1 (pool 900) + 2x2 (pool 901) = 6 trap-nights at site A", 6L, siteAEffort);
        assertFalse("site B recorded no trap effort",
                effort.stream().anyMatch(e -> Integer.valueOf(901).equals(e.getSiteId())));
    }

    // End-to-end: the service divides abundance by the summed effort (organisms per
    // trap-night) and degrades to null when a site recorded no effort — never a
    // fabricated rate. Site A abundance 28 / (2x1 + 2x2 + 2x1)=8 -> 3.5.
    @Test
    public void collectionDensity_isEffortNormalized_andDegradesWithoutEffort() {
        insertObservation(9211, 900, "vecTrapCount", "2");
        insertObservation(9212, 900, "vecTrapNights", "1");
        insertObservation(9213, 901, "vecTrapCount", "2");
        insertObservation(9214, 901, "vecTrapNights", "2");
        insertObservation(9215, 902, "vecTrapCount", "2");
        insertObservation(9216, 902, "vecTrapNights", "1");
        // Site B pool 903 records no trap effort.

        List<DensityRow> density = service.getIndices(FROM, TO, null).getCollectionDensity();

        DensityRow siteA = densityRowForSite(density, 900);
        assertEquals("site A abundance (10+10+8)", 28L, siteA.getSpecimenCount());
        assertEquals("site A trap-nights (2+4+2)", Long.valueOf(8), siteA.getTrapNights());
        assertEquals("organisms per trap-night 28/8", 3.5, siteA.getDensity(), 0.001);

        DensityRow siteB = densityRowForSite(density, 901);
        assertEquals("site B abundance still present", 5L, siteB.getSpecimenCount());
        assertNull("site B recorded no effort -> density degrades to null, not a fabricated rate", siteB.getDensity());
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

    @Test
    public void collectionDensity_groupsPerSite_andSiteIdsResolveToCatalogNames() {
        // Integration guard for the two things the service unit test structurally can't
        // see: (1) the
        // HQL groups density per (period, collectionLocationId); (2) that grouped
        // collectionLocationId
        // shares a key space with VectorSamplingSite.id so the service can resolve a
        // name. If that key
        // space diverges, the name map returns null and the chart collapses to one "All
        // sites" series
        // (the zigzag) — this fails; the mocked-DAO unit test would not.
        List<DensityAggregate> density = dao.getCollectionDensity(FROM, TO, null);
        assertFalse("fixture has vector collection density in scope", density.isEmpty());

        Set<Integer> densitySites = density.stream().map(DensityAggregate::getSiteId).collect(Collectors.toSet());
        assertTrue("density is grouped per site (fixture has 2 collection locations, not collapsed)",
                densitySites.size() >= 2);

        Map<Integer, String> catalogNames = dao.getSites().stream().filter(s -> s.getId() != null)
                .collect(Collectors.toMap(SiteOption::getId, SiteOption::getName, (a, b) -> a));
        for (Integer sid : densitySites) {
            assertNotNull("density collectionLocationId " + sid + " must resolve to a catalog site name",
                    catalogNames.get(sid));
        }
    }
}
