package org.openelisglobal.reports.vectorsurveillance.manualentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntryFieldMapService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntryViewService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntryViewServiceImpl;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryMetricKeys;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryViewDTO;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;

/**
 * Unit tests for the Manual Entry view composition: the field map (order +
 * visibility) drives which rows appear and in what order, and the sporozoite
 * row is gated (value null) when resolution &lt; 95% (US4-3). Inversion guard
 * (Constitution V.6): replacing the gating predicate / ordering with a constant
 * MUST fail these.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManualEntryViewServiceTest {

    @Mock
    private ManualEntryFieldMapService fieldMapService;

    @Mock
    private VectorSurveillanceService surveillanceService;

    private ManualEntryViewService service;

    private static final LocalDate START = LocalDate.of(2026, 1, 5);
    private static final LocalDate END = LocalDate.of(2026, 1, 11);

    @Before
    public void setUp() {
        service = new ManualEntryViewServiceImpl(fieldMapService, surveillanceService);
    }

    private static ManualEntryFieldMap field(String key, int order, boolean visible) {
        ManualEntryFieldMap f = new ManualEntryFieldMap();
        f.setMetricKey(key);
        f.setFieldOrder(order);
        f.setLabel(key);
        f.setVisible(visible);
        return f;
    }

    private static MirRow mir(int positivePools, int totalSpecimens, double resolutionPct) {
        MirRow r = new MirRow();
        r.setSpeciesId(1);
        r.setPathogen("DENV");
        r.setPositivePools(positivePools);
        r.setTotalSpecimens(totalSpecimens);
        r.setPositiveResolutionPct(resolutionPct);
        return r;
    }

    private static SurveillanceIndicesDTO indicesWith(MirRow... rows) {
        SurveillanceIndicesDTO dto = new SurveillanceIndicesDTO();
        List<MirRow> list = new ArrayList<>();
        for (MirRow r : rows) {
            list.add(r);
        }
        dto.setMirBySpecies(list);
        return dto;
    }

    // The field map's fieldOrder determines row order; getVisibleOrdered() is the
    // source of truth (the service must not re-sort or invent its own order).
    @Test
    public void fieldMapOrder_drivesRowOrder() {
        when(fieldMapService.getVisibleOrdered()).thenReturn(List.of(
                field(ManualEntryMetricKeys.POOLS_POSITIVE, 1, true),
                field(ManualEntryMetricKeys.POOLS_TESTED, 2, true)));
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indicesWith(mir(2, 400, 100.0)));

        ManualEntryViewDTO view = service.getView(START, END, null);

        assertEquals(2, view.getRows().size());
        assertEquals(ManualEntryMetricKeys.POOLS_POSITIVE, view.getRows().get(0).getMetricKey());
        assertEquals(ManualEntryMetricKeys.POOLS_TESTED, view.getRows().get(1).getMetricKey());
    }

    // Hidden field-map rows (visible=false) are excluded — getVisibleOrdered()
    // already filters; the helper must surface exactly those rows.
    @Test
    public void hiddenFields_areExcludedFromView() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.POOLS_POSITIVE, 1, true)));
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indicesWith(mir(2, 400, 100.0)));

        ManualEntryViewDTO view = service.getView(START, END, null);

        assertEquals(1, view.getRows().size());
        assertEquals(ManualEntryMetricKeys.POOLS_POSITIVE, view.getRows().get(0).getMetricKey());
    }

    // US4-3: resolution < 95% → sporozoite row gated, value null.
    @Test
    public void sporozoiteRow_isGatedAndNull_whenResolutionBelow95() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.SPOROZOITE_RATE, 1, true)));
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indicesWith(mir(3, 600, 80.0)));

        ManualEntryViewDTO.Row sporozoite = service.getView(START, END, null).getRows().get(0);

        assertTrue("sporozoite must be gated when resolution < 95%", sporozoite.isGated());
        assertNull("gated sporozoite value must be null", sporozoite.getValue());
    }

    // US4-3 boundary: resolution >= 95% → sporozoite NOT gated (value present).
    @Test
    public void sporozoiteRow_isNotGated_whenResolutionAtOrAbove95() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.SPOROZOITE_RATE, 1, true)));
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indicesWith(mir(3, 600, 95.0)));

        ManualEntryViewDTO.Row sporozoite = service.getView(START, END, null).getRows().get(0);

        assertFalse("sporozoite must not be gated at resolution >= 95%", sporozoite.isGated());
        assertNotNull("non-gated sporozoite value must be present", sporozoite.getValue());
    }

    // Pools-positive value is derived from the indices, not the field map — proves
    // the view reads the real surveillance numbers, not a stubbed constant.
    @Test
    public void poolsPositiveValue_isDerivedFromIndices() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.POOLS_POSITIVE, 1, true)));
        when(surveillanceService.getIndices(any(), any(), any()))
                .thenReturn(indicesWith(mir(2, 400, 100.0), mir(5, 300, 100.0)));

        ManualEntryViewDTO.Row row = service.getView(START, END, null).getRows().get(0);

        assertEquals("7", row.getValue());
        assertFalse(row.isGated());
    }

    // A1: SITES_WITH_POSITIVES reflects the indices' real per-site count, not a
    // hardcoded "0" (red on the old fabricated value).
    @Test
    public void sitesWithPositives_isDerivedFromIndices() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.SITES_WITH_POSITIVES, 1, true)));
        SurveillanceIndicesDTO indices = indicesWith(mir(2, 400, 100.0));
        indices.setSitesWithPositives(3);
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indices);

        ManualEntryViewDTO.Row row = service.getView(START, END, null).getRows().get(0);

        assertEquals("3", row.getValue());
    }

    // A3: an ungated sporozoite row shows the computed rate, not a placeholder "—"
    // (red on the old hardcoded value).
    @Test
    public void sporozoiteValue_isTheComputedRate_whenNotGated() {
        when(fieldMapService.getVisibleOrdered())
                .thenReturn(List.of(field(ManualEntryMetricKeys.SPOROZOITE_RATE, 1, true)));
        SurveillanceIndicesDTO indices = indicesWith(mir(3, 600, 100.0));
        indices.setSporozoiteRatePct(1.5);
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indices);

        ManualEntryViewDTO.Row row = service.getView(START, END, null).getRows().get(0);

        assertFalse(row.isGated());
        assertEquals("1.50", row.getValue());
    }

    // C4: numeric metric values must be dot-decimal regardless of the JVM default
    // locale (red on the old String.format without Locale.ROOT, which emits
    // "2,50").
    @Test
    public void numericValue_usesDotDecimal_underACommaLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            when(fieldMapService.getVisibleOrdered())
                    .thenReturn(List.of(field(ManualEntryMetricKeys.MIR_CLASSIC, 1, true)));
            // 1 positive pool / 400 specimens * 1000 = 2.50.
            when(surveillanceService.getIndices(any(), any(), any())).thenReturn(indicesWith(mir(1, 400, 100.0)));

            ManualEntryViewDTO.Row row = service.getView(START, END, null).getRows().get(0);

            assertEquals("2.50", row.getValue());
        } finally {
            Locale.setDefault(original);
        }
    }

    // FR-012 shape: an empty field map yields an empty (non-null) row list.
    @Test
    public void emptyFieldMap_yieldsEmptyRows() {
        when(fieldMapService.getVisibleOrdered()).thenReturn(new ArrayList<>());
        when(surveillanceService.getIndices(any(), any(), any())).thenReturn(new SurveillanceIndicesDTO());

        ManualEntryViewDTO view = service.getView(START, END, null);

        assertNotNull(view.getRows());
        assertEquals(0, view.getRows().size());
    }
}
