package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;

/**
 * Lot-resolution behavior tests for processQCResult.
 *
 * The Tier 2 controlLevel match must require exactly one ACTIVE lot at the
 * given level. When multiple ACTIVE lots share a controlLevel for the same
 * (test, instrument), schema permits it (no uniqueness constraint) but the
 * pipeline cannot pick one without silent QC mis-attribution — the resolver
 * must return null and log so the caller surfaces the disambiguation failure.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCResultProcessingServiceImplTest {

    @Mock
    private QCControlLotDAO controlLotDAO;

    @Mock
    private QCResultService qcResultService;

    @InjectMocks
    private QCResultProcessingServiceImpl service;

    private static final String ANALYZER_ID = "42";
    private static final String TEST_ID = "7";
    private static final String TEST_ID_INT = "7";
    private static final String INSTRUMENT_ID = "42";
    private static final String ACCESSION = "QC-20260505-001";
    private static final BigDecimal RESULT = new BigDecimal("12.5");
    private static final String UNIT = "mg/dL";
    private static final LocalDateTime TS = LocalDateTime.of(2026, 5, 5, 9, 0);

    private QCControlLot lpcLot;
    private QCControlLot hpcLot;
    private QCControlLot duplicateLpcLot;

    @Before
    public void setUp() {
        lpcLot = new QCControlLot();
        lpcLot.setId("100");
        lpcLot.setLotNumber("LOT-LPC-001");
        lpcLot.setControlLevel("LPC");
        lpcLot.setStatus("ACTIVE");

        hpcLot = new QCControlLot();
        hpcLot.setId("101");
        hpcLot.setLotNumber("LOT-HPC-001");
        hpcLot.setControlLevel("HPC");
        hpcLot.setStatus("ACTIVE");

        duplicateLpcLot = new QCControlLot();
        duplicateLpcLot.setId("102");
        duplicateLpcLot.setLotNumber("LOT-LPC-002");
        duplicateLpcLot.setControlLevel("LPC");
        duplicateLpcLot.setStatus("ACTIVE");
    }

    @Test
    public void tier2_singleControlLevelMatch_resolvesAndPersists() {
        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Arrays.asList(lpcLot, hpcLot));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "LPC", RESULT, UNIT, TS);

        verify(qcResultService).createQCResult(eq(ANALYZER_ID), eq(TEST_ID), eq("100"), eq("LPC"), eq(RESULT), eq(UNIT),
                eq(TS));
    }

    @Test
    public void tier2_ambiguousControlLevel_returnsNullAndDoesNotPersist() {
        // Two ACTIVE lots share controlLevel="LPC" — schema allows this; resolver
        // must refuse rather than pick one by DAO order.
        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Arrays.asList(lpcLot, duplicateLpcLot, hpcLot));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "LPC", RESULT, UNIT, TS);

        verify(qcResultService, never()).createQCResult(anyString(), anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyString(), any(LocalDateTime.class));
    }

    @Test
    public void tier2_controlLevelMatchesNoLot_fallsThroughToTier3SingleActive() {
        // controlLevel is provided but no lot has it. With exactly one ACTIVE
        // lot in the list overall, Tier 3 single-active fallback applies.
        QCControlLot soloActive = new QCControlLot();
        soloActive.setId("200");
        soloActive.setLotNumber("LOT-SOLO");
        soloActive.setControlLevel("MID");
        soloActive.setStatus("ACTIVE");

        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Collections.singletonList(soloActive));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "LPC", RESULT, UNIT, TS);

        verify(qcResultService).createQCResult(eq(ANALYZER_ID), eq(TEST_ID), eq("200"), eq("MID"), eq(RESULT), eq(UNIT),
                eq(TS));
    }

    @Test
    public void tier1_explicitLotNumberMatch_winsOverLevelMatch() {
        // Tier 1 short-circuits before Tier 2, so even if controlLevel is
        // ambiguous, an explicit lotNumber resolves cleanly.
        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Arrays.asList(lpcLot, duplicateLpcLot, hpcLot));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, "LOT-LPC-002", "LPC", RESULT, UNIT, TS);

        verify(qcResultService).createQCResult(eq(ANALYZER_ID), eq(TEST_ID), eq("102"), eq("LPC"), eq(RESULT), eq(UNIT),
                eq(TS));
    }

    @Test
    public void tier2_emptyLotsList_returnsNullAndDoesNotPersist() {
        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID)).thenReturn(Collections.emptyList());

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "LPC", RESULT, UNIT, TS);

        verify(qcResultService, never()).createQCResult(anyString(), anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyString(), any(LocalDateTime.class));
    }

    @Test
    public void tier2_establishmentLotMatch_resolves() {
        // Tier 2 must accept ESTABLISHMENT lots — `isUsable` treats them as
        // eligible, so when the bridge surfaces an explicit controlLevel and
        // the only matching lot is in establishment, it resolves cleanly
        // (consistent with Tier 1's lot-number match across both statuses).
        QCControlLot establishmentLot = new QCControlLot();
        establishmentLot.setId("300");
        establishmentLot.setLotNumber("LOT-EST-001");
        establishmentLot.setControlLevel("LPC");
        establishmentLot.setStatus("ESTABLISHMENT");

        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Collections.singletonList(establishmentLot));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "LPC", RESULT, UNIT, TS);

        verify(qcResultService).createQCResult(eq(ANALYZER_ID), eq(TEST_ID), eq("300"), eq("LPC"), eq(RESULT), eq(UNIT),
                eq(TS));
    }

    @Test
    public void tier2_controlLevelWithWhitespace_isTrimmedBeforeMatching() {
        when(controlLotDAO.getByTestAndInstrument(TEST_ID_INT, INSTRUMENT_ID))
                .thenReturn(Arrays.asList(lpcLot, hpcLot));

        service.processQCResult(ANALYZER_ID, TEST_ID, ACCESSION, null, "  LPC  ", RESULT, UNIT, TS);

        verify(qcResultService).createQCResult(eq(ANALYZER_ID), eq(TEST_ID), eq("100"), eq("LPC"), eq(RESULT), eq(UNIT),
                eq(TS));
    }
}
