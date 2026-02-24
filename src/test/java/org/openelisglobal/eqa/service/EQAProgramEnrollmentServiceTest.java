package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;

@RunWith(MockitoJUnitRunner.class)
public class EQAProgramEnrollmentServiceTest {

    @Mock
    private EQAProgramEnrollmentDAO enrollmentDAO;

    @Mock
    private EQAProgramService programService;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private EQAProgramEnrollmentServiceImpl service;

    private EQAProgram program;
    private EQAProgramEnrollment enrollment;

    @Before
    public void setUp() {
        program = new EQAProgram();
        program.setId(1L);
        program.setName("Chemistry PT");

        enrollment = new EQAProgramEnrollment();
        enrollment.setId(10L);
        enrollment.setEqaProgram(program);
        enrollment.setOrganizationId(100L);
        enrollment.setStatus("Active");
        enrollment.setSysUserId("1");
    }

    @Test
    public void testFindByProgramId() {
        when(enrollmentDAO.findByProgramId(1L)).thenReturn(List.of(enrollment));

        List<EQAProgramEnrollment> result = service.findByProgramId(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getOrganizationId().longValue());
    }

    @Test
    public void testEnrollOrganization_Success() {
        when(enrollmentDAO.existsActiveEnrollment(1L, 200L)).thenReturn(false);
        when(programService.get(1L)).thenReturn(program);
        when(enrollmentDAO.insert(any(EQAProgramEnrollment.class))).thenReturn(10L);
        when(enrollmentDAO.get(10L)).thenReturn(Optional.of(enrollment));

        EQAProgramEnrollment result = service.enrollOrganization(1L, 200L, "1");

        assertNotNull(result);
        verify(enrollmentDAO).insert(any(EQAProgramEnrollment.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnrollOrganization_DuplicatePrevention() {
        when(enrollmentDAO.existsActiveEnrollment(1L, 100L)).thenReturn(true);

        service.enrollOrganization(1L, 100L, "1");
    }

    @Test
    public void testBulkEnroll_SkipsDuplicates() {
        when(enrollmentDAO.existsActiveEnrollment(1L, 100L)).thenReturn(true);
        when(enrollmentDAO.existsActiveEnrollment(1L, 200L)).thenReturn(false);
        when(programService.get(1L)).thenReturn(program);
        when(enrollmentDAO.insert(any(EQAProgramEnrollment.class))).thenReturn(11L);

        EQAProgramEnrollment newEnrollment = new EQAProgramEnrollment();
        newEnrollment.setId(11L);
        newEnrollment.setOrganizationId(200L);
        newEnrollment.setStatus("Active");
        when(enrollmentDAO.get(11L)).thenReturn(Optional.of(newEnrollment));

        List<EQAProgramEnrollment> result = service.bulkEnroll(1L, List.of(100L, 200L), "1");

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getOrganizationId().longValue());
    }

    @Test
    public void testUpdateStatus_ActiveToSuspended() {
        when(enrollmentDAO.get(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentDAO.update(any(EQAProgramEnrollment.class))).thenReturn(enrollment);

        assertNotNull(service.updateStatus(10L, "Suspended", null, "1"));

        ArgumentCaptor<EQAProgramEnrollment> captor = ArgumentCaptor.forClass(EQAProgramEnrollment.class);
        verify(enrollmentDAO).update(captor.capture());
        assertEquals("Suspended", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStatusChangedDate());
    }

    @Test
    public void testUpdateStatus_ActiveToWithdrawn_WithReason() {
        when(enrollmentDAO.get(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentDAO.update(any(EQAProgramEnrollment.class))).thenReturn(enrollment);

        assertNotNull(service.updateStatus(10L, "Withdrawn", "Non-compliance", "1"));

        ArgumentCaptor<EQAProgramEnrollment> captor = ArgumentCaptor.forClass(EQAProgramEnrollment.class);
        verify(enrollmentDAO).update(captor.capture());
        assertEquals("Withdrawn", captor.getValue().getStatus());
        assertEquals("Non-compliance", captor.getValue().getWithdrawalReason());
    }

    @Test
    public void testUpdateStatus_SuspendedToActive() {
        enrollment.setStatus("Suspended");
        when(enrollmentDAO.get(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentDAO.update(any(EQAProgramEnrollment.class))).thenReturn(enrollment);

        service.updateStatus(10L, "Active", null, "1");

        ArgumentCaptor<EQAProgramEnrollment> captor = ArgumentCaptor.forClass(EQAProgramEnrollment.class);
        verify(enrollmentDAO).update(captor.capture());
        assertEquals("Active", captor.getValue().getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateStatus_WithdrawnToActive_Invalid() {
        enrollment.setStatus("Withdrawn");
        when(enrollmentDAO.get(10L)).thenReturn(Optional.of(enrollment));

        service.updateStatus(10L, "Active", null, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateStatus_NotFound() {
        when(enrollmentDAO.get(999L)).thenReturn(Optional.empty());

        service.updateStatus(999L, "Suspended", null, "1");
    }

    @Test
    public void testGetEligibleOrganizations() {
        Organization org1 = new Organization();
        org1.setId("101");
        org1.setOrganizationName("Hospital A");
        org1.setIsActive("Y");

        Organization org2 = new Organization();
        org2.setId("102");
        org2.setOrganizationName("Clinic B");
        org2.setIsActive("Y");

        when(organizationService.getAll()).thenReturn(List.of(org1, org2));

        EQAProgramEnrollment activeEnrollment = new EQAProgramEnrollment();
        activeEnrollment.setOrganizationId(101L);
        when(enrollmentDAO.findByProgramIdAndStatus(1L, "Active")).thenReturn(List.of(activeEnrollment));

        List<Map<String, Object>> result = service.getEligibleOrganizations(1L);

        assertEquals(2, result.size());
        assertTrue((Boolean) result.get(0).get("alreadyEnrolled"));
        assertFalse((Boolean) result.get(1).get("alreadyEnrolled"));
    }

    @Test
    public void testCountActiveEnrollments() {
        when(enrollmentDAO.findByProgramIdAndStatus(1L, "Active")).thenReturn(List.of(enrollment));

        long count = service.countActiveEnrollments(1L);

        assertEquals(1L, count);
    }

    @Test
    public void testCountActiveEnrollments_Empty() {
        when(enrollmentDAO.findByProgramIdAndStatus(1L, "Active")).thenReturn(new ArrayList<>());

        long count = service.countActiveEnrollments(1L);

        assertEquals(0L, count);
    }
}
