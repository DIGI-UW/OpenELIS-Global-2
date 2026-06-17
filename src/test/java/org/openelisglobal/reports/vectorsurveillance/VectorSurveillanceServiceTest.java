package org.openelisglobal.reports.vectorsurveillance;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;

/**
 * Unit tests for the surveillance index math (mocked DAO). Inversion guard
 * (Constitution V.6): replacing the formula with a constant MUST fail these.
 */
@RunWith(MockitoJUnitRunner.class)
public class VectorSurveillanceServiceTest {

    @Mock
    private VectorSurveillanceDAO dao;

    private VectorSurveillanceService service;

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    @Before
    public void setUp() {
        service = new VectorSurveillanceServiceImpl(dao);
    }

    // T004 — classical MIR = (positive pools / total specimens) * 1000
    @Test
    public void classicalMir_isPositivePoolsOverTotalSpecimensTimes1000() {
        when(dao.getMirAggregates(any(), any(), any())).thenReturn(
                List.of(new SpeciesMirAggregate(1, "Aedes", "aegypti", "DENV", 3, 600, 4, 3, 0)));

        SurveillanceIndicesDTO dto = service.getIndices(FROM, TO, null);

        assertEquals(1, dto.getMirBySpecies().size());
        MirRow row = dto.getMirBySpecies().get(0);
        assertEquals("DENV", row.getPathogen());
        assertEquals(5.0, row.getMirClassic(), 0.001); // 3 / 600 * 1000
        assertEquals(3, row.getPositivePools());
        assertEquals(600, row.getTotalSpecimens());
    }

    // T004 — zero specimens must not divide-by-zero; MIR = 0
    @Test
    public void classicalMir_withNoSpecimens_isZeroNotNaN() {
        when(dao.getMirAggregates(any(), any(), any())).thenReturn(
                List.of(new SpeciesMirAggregate(1, "Aedes", "aegypti", "DENV", 0, 0, 0, 0, 0)));

        MirRow row = service.getIndices(FROM, TO, null).getMirBySpecies().get(0);

        assertEquals(0.0, row.getMirClassic(), 0.001);
    }

    // T004 — empty scope yields an empty (not null) MIR list (FR-012 shape)
    @Test
    public void noData_yieldsEmptyMirList() {
        when(dao.getMirAggregates(any(), any(), any())).thenReturn(Collections.emptyList());

        assertEquals(0, service.getIndices(FROM, TO, null).getMirBySpecies().size());
    }
}
