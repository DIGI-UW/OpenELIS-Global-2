package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class EQAFhirSubmissionServiceTest {

    @Mock
    private EQADistributionDAO distributionDAO;

    @Mock
    private EQAResultDAO resultDAO;

    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @Mock
    private FhirConfig fhirConfig;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private EQAFhirSubmissionServiceImpl submissionService;

    private EQADistribution distribution;
    private EQAResult result1;
    private EQAResult result2;

    @Before
    public void setUp() {
        when(fhirConfig.getOeFhirSystem()).thenReturn("http://openelis-global.org");

        EQAProgram program = new EQAProgram();
        program.setId(1L);
        program.setName("Chemistry PT");

        distribution = new EQADistribution();
        distribution.setId(10L);
        distribution.setDistributionName("Round 1");
        distribution.setEqaProgram(program);
        distribution.setFhirUuid(UUID.randomUUID());
        distribution.setDeadline(Timestamp.valueOf("2026-12-31 23:59:59"));

        result1 = new EQAResult();
        result1.setId(100L);
        result1.setFhirUuid(UUID.randomUUID());
        result1.setEqaDistribution(distribution);
        result1.setParticipantOrganizationId(50L);
        result1.setTestId(5L);
        result1.setResultValue(new BigDecimal("12.5"));
        result1.setSubmissionMethod(EQASubmissionMethod.MANUAL);
        result1.setZScore(new BigDecimal("0.75"));
        result1.setPerformanceStatus(EQAPerformanceStatus.ACCEPTABLE);

        result2 = new EQAResult();
        result2.setId(101L);
        result2.setFhirUuid(UUID.randomUUID());
        result2.setEqaDistribution(distribution);
        result2.setParticipantOrganizationId(50L);
        result2.setTestId(6L);
        result2.setResultValue(new BigDecimal("8.3"));
        result2.setSubmissionMethod(EQASubmissionMethod.MANUAL);
    }

    @Test
    public void testSubmitResultsViaFhir_Success() throws FhirLocalPersistingException {
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1, result2));
        when(fhirPersistanceService.createFhirResourcesInFhirStore(anyMap()))
                .thenReturn(new Bundle());

        Map<String, Object> response = submissionService.submitResultsViaFhir(10L, 50L);

        assertTrue((Boolean) response.get("success"));
        assertEquals(3, response.get("resourceCount")); // 1 DiagnosticReport + 2 Observations
        assertEquals(10L, response.get("distributionId"));
        assertEquals(50L, response.get("organizationId"));
        verify(fhirPersistanceService).createFhirResourcesInFhirStore(anyMap());
    }

    @Test
    public void testSubmitResultsViaFhir_FhirError() throws FhirLocalPersistingException {
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1));
        when(fhirPersistanceService.createFhirResourcesInFhirStore(anyMap()))
                .thenThrow(new FhirLocalPersistingException(new RuntimeException("FHIR error")));

        Map<String, Object> response = submissionService.submitResultsViaFhir(10L, 50L);

        assertFalse((Boolean) response.get("success"));
        assertNotNull(response.get("error"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitResultsViaFhir_DistributionNotFound() {
        when(distributionDAO.get(999L)).thenReturn(Optional.empty());
        submissionService.submitResultsViaFhir(999L, 50L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitResultsViaFhir_NoResultsForOrg() {
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1));

        submissionService.submitResultsViaFhir(10L, 999L);
    }

    @Test
    public void testIsSubmissionLate_NotLate() {
        distribution.setDeadline(Timestamp.valueOf("2027-12-31 23:59:59"));
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));

        assertFalse(submissionService.isSubmissionLate(10L));
    }

    @Test
    public void testIsSubmissionLate_PastDeadline() {
        distribution.setDeadline(Timestamp.valueOf("2020-01-01 00:00:00"));
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));

        assertTrue(submissionService.isSubmissionLate(10L));
    }

    @Test
    public void testIsSubmissionLate_NullDeadline() {
        distribution.setDeadline(null);
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));

        assertFalse(submissionService.isSubmissionLate(10L));
    }

    @Test
    public void testApproveLateSubmission_Success() throws FhirLocalPersistingException {
        distribution.setDeadline(Timestamp.valueOf("2020-01-01 00:00:00"));
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1));

        SystemUser supervisor = new SystemUser();
        supervisor.setId("supervisor1");
        when(systemUserService.get("supervisor1")).thenReturn(supervisor);

        when(fhirPersistanceService.createFhirResourcesInFhirStore(anyMap())).thenReturn(new Bundle());

        Map<String, Object> response = submissionService.approveLateSubmission(10L, 50L, "Emergency situation",
                "supervisor1");

        assertTrue((Boolean) response.get("approved"));
        assertEquals("supervisor1", response.get("approvedBy"));
        assertEquals("Emergency situation", response.get("justification"));

        verify(resultDAO).update(any(EQAResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testApproveLateSubmission_NotLate() {
        distribution.setDeadline(Timestamp.valueOf("2027-12-31 23:59:59"));
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));

        submissionService.approveLateSubmission(10L, 50L, "No reason", "supervisor1");
    }

    @Test
    public void testSubmitResultsViaFhir_BuildsCorrectResourceCount() throws FhirLocalPersistingException {
        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1));
        when(fhirPersistanceService.createFhirResourcesInFhirStore(anyMap()))
                .thenReturn(new Bundle());

        Map<String, Object> response = submissionService.submitResultsViaFhir(10L, 50L);

        assertEquals(2, response.get("resourceCount")); // 1 DiagnosticReport + 1 Observation
    }

    @Test
    public void testSubmitResultsViaFhir_FiltersResultsByOrg() throws FhirLocalPersistingException {
        EQAResult otherOrgResult = new EQAResult();
        otherOrgResult.setId(102L);
        otherOrgResult.setFhirUuid(UUID.randomUUID());
        otherOrgResult.setEqaDistribution(distribution);
        otherOrgResult.setParticipantOrganizationId(99L);
        otherOrgResult.setTestId(5L);
        otherOrgResult.setResultValue(new BigDecimal("10.0"));

        when(distributionDAO.get(10L)).thenReturn(Optional.of(distribution));
        when(resultDAO.findByDistributionId(10L)).thenReturn(List.of(result1, otherOrgResult));
        when(fhirPersistanceService.createFhirResourcesInFhirStore(anyMap())).thenReturn(new Bundle());

        Map<String, Object> response = submissionService.submitResultsViaFhir(10L, 50L);

        assertEquals(2, response.get("resourceCount")); // Only result1 for org 50
    }
}
