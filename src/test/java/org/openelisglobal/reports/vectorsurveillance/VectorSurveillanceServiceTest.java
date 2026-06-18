package org.openelisglobal.reports.vectorsurveillance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceServiceImpl;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SporozoiteAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;

/**
 * Unit tests for the surveillance-index arithmetic in
 * {@link VectorSurveillanceServiceImpl} (mocked DAO).
 *
 * <p>
 * <b>Scope (what this level legitimately covers).</b> The service is a pure
 * compute layer: it takes raw aggregate counts from the DAO and turns them into
 * MIR / infection-rate / resolution / sporozoite figures, joining the
 * per-species specimen denominator from the species distribution. These tests
 * pin that arithmetic and the denominator-join wiring on known inputs, and pass
 * through the catalog-driven {@code positivityConfigured} degradation flag.
 *
 * <p>
 * <b>What this level explicitly does NOT and MUST NOT claim.</b> The DAO is
 * mocked, so these tests cannot exercise the positivity inversion fix (POSITIVE
 * significance vs the old {@code deconvolutionStatus <> NOT_APPLICABLE} proxy).
 * That bug lives entirely in HQL and is only catchable by an integration test
 * against a real database. Here, "positive pools" arrives as a number the test
 * supplies; asserting on it would be asserting on the mock's own input. We keep
 * the inputs as plain counts and only verify the math the service actually
 * owns.
 *
 * <p>
 * Inversion guard for THIS level (Constitution V.6): replacing any formula with
 * a constant, dropping the {@code *1000} / {@code *100} scaling, or mis-wiring
 * the denominator (e.g. reusing the aggregate's own zeroed
 * {@code totalSpecimens} instead of the species-distribution total) MUST fail
 * these tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class VectorSurveillanceServiceTest {

    @Mock
    private VectorSurveillanceDAO dao;

    private VectorSurveillanceService service;

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private static final int AEDES_ID = 1;

    @Before
    public void setUp() {
        service = new VectorSurveillanceServiceImpl(dao);
    }

    /**
     * Seed the species distribution so the service has a specimen denominator for
     * Aedes aegypti = {@code specimenTotal}. The MIR denominator comes from HERE
     * (the species distribution), not from the MIR aggregate's own totalSpecimens
     * (which the DAO deliberately leaves 0 — the service fills it). lenient() lets
     * an individual test omit the MIR stub without tripping strict-stub checks.
     */
    private void seedAedesSpecimens(long specimenTotal) {
        lenient().when(dao.getSpeciesDistribution(any(), any(), any()))
                .thenReturn(List.of(new SpeciesAggregate(AEDES_ID, "Aedes", "aegypti", specimenTotal)));
    }

    private MirRow firstMirRow(SurveillanceIndicesDTO dto) {
        assertEquals(1, dto.getMirBySpecies().size());
        return dto.getMirBySpecies().get(0);
    }

    // ---- classical MIR -----------------------------------------------------

    /**
     * BR-V04-001 classical MIR = positivePools / speciesSpecimens * 1000, with the
     * denominator joined from the species distribution. 3 / 600 * 1000 = 5.0.
     */
    @Test
    public void classicalMir_isPositivePoolsOverSpeciesSpecimensTimes1000() {
        seedAedesSpecimens(600);
        when(dao.getMirAggregates(any(), any(), any()))
                .thenReturn(List.of(new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 3, 0, 3, 3, 3)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals("DENV", row.getPathogen());
        assertEquals(5.0, row.getMirClassic(), 0.001); // 3 / 600 * 1000
        assertEquals(3, row.getPositivePools());
        // Denominator MUST come from the species distribution, not the (zeroed)
        // aggregate.totalSpecimens. If the service ever reads the aggregate field
        // this stays 0 and the assertion fails.
        assertEquals(600, row.getTotalSpecimens());
    }

    /**
     * Denominator-join discriminator: the aggregate carries a misleading non-zero
     * totalSpecimens (999), but the real denominator is the species-distribution
     * total (200). MIR must be 4/200*1000 = 20.0, NOT 4/999*1000 (~4.004). This
     * fails if anyone "simplifies" the service to read aggregate.totalSpecimens.
     */
    @Test
    public void classicalMir_usesSpeciesDistributionDenominator_notAggregateField() {
        seedAedesSpecimens(200);
        when(dao.getMirAggregates(any(), any(), any()))
                .thenReturn(List.of(new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 4, 999, 4, 4, 4)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals(200, row.getTotalSpecimens());
        assertEquals(20.0, row.getMirClassic(), 0.001);
    }

    /** Divide-by-zero guard: no specimens for the species → MIR = 0, not NaN. */
    @Test
    public void classicalMir_withNoSpeciesSpecimens_isZeroNotNaN() {
        // Species distribution empty → no denominator entry for AEDES_ID → total 0.
        when(dao.getSpeciesDistribution(any(), any(), any())).thenReturn(Collections.emptyList());
        when(dao.getMirAggregates(any(), any(), any())).thenReturn(List.of(
                new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 5, 0, 5, 5, 5)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals(0.0, row.getMirClassic(), 0.001);
        assertEquals(0.0, row.getInfectionRateObserved(), 0.001);
    }

    // ---- deconvolution-aware infection rate --------------------------------

    /**
     * infectionRateObserved = observedPositiveOrganisms / speciesSpecimens * 1000,
     * and is distinct from classical MIR when deconvolution resolved more than one
     * positive organism in a pool. positivePools=2 but observed=7 over 500
     * specimens → 7/500*1000 = 14.0, while classical MIR = 2/500*1000 = 4.0.
     */
    @Test
    public void infectionRateObserved_usesObservedOrganisms_distinctFromClassicalMir() {
        seedAedesSpecimens(500);
        when(dao.getMirAggregates(any(), any(), any()))
                .thenReturn(List.of(new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 2, 0, 7, 2, 2)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals(4.0, row.getMirClassic(), 0.001); // 2 / 500 * 1000
        assertEquals(14.0, row.getInfectionRateObserved(), 0.001); // 7 / 500 * 1000
    }

    // ---- positive-resolution percentage ------------------------------------

    /**
     * positiveResolutionPct = completelyResolvedPositivePools / totalPositivePools
     * * 100. 3 of 4 positive pools fully deconvoluted → 75%.
     */
    @Test
    public void positiveResolutionPct_isResolvedOverTotalPositivePoolsTimes100() {
        seedAedesSpecimens(600);
        when(dao.getMirAggregates(any(), any(), any()))
                .thenReturn(List.of(new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 4, 0, 6, 4, 3)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals(75.0, row.getPositiveResolutionPct(), 0.001); // 3 / 4 * 100
    }

    /**
     * No positive pools → resolution % = 0, not NaN.
     */
    @Test
    public void positiveResolutionPct_withNoPositivePools_isZero() {
        seedAedesSpecimens(600);
        when(dao.getMirAggregates(any(), any(), any()))
                .thenReturn(List.of(new SpeciesMirAggregate(AEDES_ID, "Aedes", "aegypti", "DENV", 0, 0, 0, 0, 0)));

        MirRow row = firstMirRow(service.getIndices(FROM, TO, null));

        assertEquals(0.0, row.getPositiveResolutionPct(), 0.001);
    }

    // ---- sporozoite rate (top-level, Anopheles) ----------------------------

    /**
     * Sporozoite rate is MIR-style: positivePools / totalSpecimens * 100.
     * 8 positive Anopheles pools / 2000 Anopheles specimens = 0.4%.
     */
    @Test
    public void sporozoiteRate_isPositivePoolsOverSpecimensTimes100() {
        when(dao.getSporozoiteAggregate(any(), any(), any()))
                .thenReturn(new SporozoiteAggregate(8, 2000));

        SurveillanceIndicesDTO dto = service.getIndices(FROM, TO, null);

        assertEquals(0.4, dto.getSporozoiteRatePct(), 0.0001); // 8 / 2000 * 100
    }

    /** No Anopheles specimens → sporozoite rate left null (not computed), not 0%. */
    @Test
    public void sporozoiteRate_withNoSpecimens_isNull() {
        when(dao.getSporozoiteAggregate(any(), any(), any()))
                .thenReturn(new SporozoiteAggregate(0, 0));

        assertNull(service.getIndices(FROM, TO, null).getSporozoiteRatePct());
    }

    /** A null sporozoite aggregate (DAO degradation) must not NPE; rate stays null. */
    @Test
    public void sporozoiteRate_withNullAggregate_isNullNotError() {
        when(dao.getSporozoiteAggregate(any(), any(), any())).thenReturn(null);

        assertNull(service.getIndices(FROM, TO, null).getSporozoiteRatePct());
    }

    // ---- degradation flag passthrough --------------------------------------

    /**
     * When the DAO reports no result carries a significance classification, the
     * service surfaces positivityConfigured=false so the frontend shows "not
     * configured" instead of fake zeros (the degradation contract). The service
     * must pass the flag through verbatim.
     */
    @Test
    public void positivityConfigured_falseWhenNoSignificanceClassificationPresent() {
        when(dao.isPositivityClassificationPresent(any(), any(), any())).thenReturn(false);

        assertEquals(false, service.getIndices(FROM, TO, null).isPositivityConfigured());
    }

    @Test
    public void positivityConfigured_trueWhenClassificationPresent() {
        when(dao.isPositivityClassificationPresent(any(), any(), any())).thenReturn(true);

        assertTrue(service.getIndices(FROM, TO, null).isPositivityConfigured());
    }

    // ---- shape -------------------------------------------------------------

    /** Empty scope yields empty (not null) MIR list (FR-012 shape). */
    @Test
    public void noData_yieldsEmptyMirList() {
        when(dao.getMirAggregates(any(), any(), any())).thenReturn(Collections.emptyList());

        assertTrue(service.getIndices(FROM, TO, null).getMirBySpecies().isEmpty());
    }
}
