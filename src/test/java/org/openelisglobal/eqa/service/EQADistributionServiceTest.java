package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;

@RunWith(MockitoJUnitRunner.class)
public class EQADistributionServiceTest {

    @Mock
    private EQADistributionDAO eqaDistributionDAO;

    @InjectMocks
    private EQADistributionServiceImpl distributionService;

    @Test
    public void testAdvanceStatus_DraftToPrepared() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setStatus(EQADistributionStatus.DRAFT);

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaDistributionDAO.update(any(EQADistribution.class))).thenAnswer(i -> i.getArgument(0));

        EQADistribution result = distributionService.advanceStatus(1L);

        assertEquals("Status should advance to PREPARED", EQADistributionStatus.PREPARED, result.getStatus());
    }

    @Test
    public void testAdvanceStatus_PreparedToShipped() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setStatus(EQADistributionStatus.PREPARED);

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaDistributionDAO.update(any(EQADistribution.class))).thenAnswer(i -> i.getArgument(0));

        EQADistribution result = distributionService.advanceStatus(1L);

        assertEquals("Status should advance to SHIPPED", EQADistributionStatus.SHIPPED, result.getStatus());
    }

    @Test
    public void testAdvanceStatus_ShippedToCompleted() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setStatus(EQADistributionStatus.SHIPPED);

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));
        when(eqaDistributionDAO.update(any(EQADistribution.class))).thenAnswer(i -> i.getArgument(0));

        EQADistribution result = distributionService.advanceStatus(1L);

        assertEquals("Status should advance to COMPLETED", EQADistributionStatus.COMPLETED, result.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testAdvanceStatus_CompletedThrowsException() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);
        distribution.setStatus(EQADistributionStatus.COMPLETED);

        when(eqaDistributionDAO.get(1L)).thenReturn(Optional.of(distribution));

        distributionService.advanceStatus(1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdvanceStatus_WithInvalidId_ThrowsException() {
        when(eqaDistributionDAO.get(999L)).thenReturn(Optional.empty());
        distributionService.advanceStatus(999L);
    }

    @Test
    public void testValidateMinParticipants_WithEnoughParticipants_NoException() {
        distributionService.validateMinParticipants(1L, 3);
        // No exception means success
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateMinParticipants_WithTooFew_ThrowsException() {
        distributionService.validateMinParticipants(1L, 1);
    }

    @Test
    public void testFindByProgramId_DelegatesToDAO() {
        EQADistribution d1 = new EQADistribution();
        d1.setId(1L);

        when(eqaDistributionDAO.findByProgramId(1L)).thenReturn(Arrays.asList(d1));

        List<EQADistribution> result = distributionService.findByProgramId(1L);

        assertEquals("Should return 1 distribution", 1, result.size());
        verify(eqaDistributionDAO).findByProgramId(1L);
    }

    @Test
    public void testFindByStatus_DelegatesToDAO() {
        when(eqaDistributionDAO.findByStatus(EQADistributionStatus.DRAFT))
                .thenReturn(Arrays.asList(new EQADistribution()));

        List<EQADistribution> result = distributionService.findByStatus(EQADistributionStatus.DRAFT);

        assertEquals("Should return 1 distribution", 1, result.size());
    }
}
