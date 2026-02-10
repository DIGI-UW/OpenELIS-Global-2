package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

@RunWith(MockitoJUnitRunner.class)
public class EQAResultServiceTest {

    @Mock
    private EQAResultDAO eqaResultDAO;

    @Mock
    private EQADistributionDAO eqaDistributionDAO;

    @Mock
    private EQAStatisticsService eqaStatisticsService;

    @InjectMocks
    private EQAResultServiceImpl resultService;

    @Test
    public void testSubmitResult_NewResult_CreatesRecord() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000)); // tomorrow

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.empty());
        when(eqaResultDAO.insert(any(EQAResult.class))).thenReturn(1L);
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(Collections.emptyList());

        EQAResult result = resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);

        assertNotNull("Result should not be null", result);
        assertEquals("Organization ID should match", Long.valueOf(10L), result.getParticipantOrganizationId());
        assertEquals("Test ID should match", Long.valueOf(20L), result.getTestId());
        assertEquals("Result value should match", new BigDecimal("5.5"), result.getResultValue());
        assertFalse("Should not be late submission", result.getIsLateSubmission());
        verify(eqaResultDAO).insert(any(EQAResult.class));
    }

    @Test
    public void testSubmitResult_DuplicateResult_UpdatesExisting() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        Timestamp originalDate = new Timestamp(System.currentTimeMillis() - 3600000); // 1 hour ago
        EQAResult existing = new EQAResult();
        existing.setId(99L);
        existing.setParticipantOrganizationId(10L);
        existing.setTestId(20L);
        existing.setResultValue(new BigDecimal("3.0"));
        existing.setSubmissionDate(originalDate);
        existing.setSubmissionMethod(EQASubmissionMethod.FHIR);

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.of(existing));
        when(eqaResultDAO.update(any(EQAResult.class))).thenAnswer(i -> i.getArgument(0));
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(Collections.emptyList());

        EQAResult result = resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);

        assertEquals("Result value should be updated", new BigDecimal("5.5"), result.getResultValue());
        // T116: Verify audit trail fields
        assertEquals("Previous value should be preserved", new BigDecimal("3.0"), result.getPreviousResultValue());
        assertEquals("Previous date should be preserved", originalDate, result.getPreviousSubmissionDate());
        assertEquals("Previous method should be preserved", EQASubmissionMethod.FHIR,
                result.getPreviousSubmissionMethod());
        verify(eqaResultDAO).update(any(EQAResult.class));
        verify(eqaResultDAO, never()).insert(any(EQAResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testSubmitResult_LateSubmission_BlockedWithoutApproval() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() - 86400000)); // yesterday

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));

        resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);
    }

    @Test
    public void testSubmitResult_NoDeadline_AllowsSubmission() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(null); // no deadline

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.empty());
        when(eqaResultDAO.insert(any(EQAResult.class))).thenReturn(1L);
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(Collections.emptyList());

        EQAResult result = resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);

        assertNotNull("Result should not be null", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitResult_NegativeValue_Rejected() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));

        resultService.submitResult(1L, 10L, 20L, new BigDecimal("-1"), EQASubmissionMethod.MANUAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitResult_ExceedsMaxValue_Rejected() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));

        resultService.submitResult(1L, 10L, 20L, new BigDecimal("1000000"), EQASubmissionMethod.MANUAL);
    }

    @Test
    public void testSubmitResult_NullValue_Allowed() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.empty());
        when(eqaResultDAO.insert(any(EQAResult.class))).thenReturn(1L);
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(Collections.emptyList());

        EQAResult result = resultService.submitResult(1L, 10L, 20L, null, EQASubmissionMethod.MANUAL);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testSubmitResult_WithEnoughParticipants_TriggersStatistics() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.empty());
        when(eqaResultDAO.insert(any(EQAResult.class))).thenReturn(1L);

        // Return 5 results (enough for stats)
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(
                Arrays.asList(new EQAResult(), new EQAResult(), new EQAResult(), new EQAResult(), new EQAResult()));

        resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);

        verify(eqaStatisticsService).calculateAndUpdateStatistics(1L);
    }

    @Test
    public void testSubmitResult_WithTooFewParticipants_DoesNotTriggerStatistics() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaResultDAO.findByDistributionAndOrgAndTest(1L, 10L, 20L)).thenReturn(Optional.empty());
        when(eqaResultDAO.insert(any(EQAResult.class))).thenReturn(1L);
        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(Arrays.asList(new EQAResult(), new EQAResult()));

        resultService.submitResult(1L, 10L, 20L, new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);

        verify(eqaStatisticsService, never()).calculateAndUpdateStatistics(anyLong());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitResult_WithInvalidDistribution_ThrowsException() {
        when(eqaDistributionDAO.get(999L)).thenReturn(Optional.empty());
        resultService.submitResult(999L, 10L, 20L,
                new BigDecimal("5.5"), EQASubmissionMethod.MANUAL);
    }

    @Test
    public void testFindByDistributionId_DelegatesToDAO() {
        when(eqaResultDAO.findByDistributionId(1L))
                .thenReturn(Arrays.asList(new EQAResult(), new EQAResult()));

        assertEquals("Should return 2 results", 2,
                resultService.findByDistributionId(1L).size());
    }

    @Test
    public void testCountByDistributionId_DelegatesToDAO() {
        when(eqaResultDAO.countByDistributionId(1L)).thenReturn(5L);

        assertEquals("Should return count 5", 5L,
                resultService.countByDistributionId(1L));
    }
}
