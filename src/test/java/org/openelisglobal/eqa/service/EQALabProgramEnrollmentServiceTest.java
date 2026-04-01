package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.organization.valueholder.Organization;

@RunWith(MockitoJUnitRunner.class)
public class EQALabProgramEnrollmentServiceTest {

    @Mock
    private EQALabProgramEnrollmentDAO enrollmentDAO;

    @Mock
    private EQAProgramDAO eqaProgramDAO;

    @InjectMocks
    private EQALabProgramEnrollmentServiceImpl service;

    private EQALabProgramEnrollment enrollment;
    private EQAProgram chemistryProgram;

    @Before
    public void setUp() {
        chemistryProgram = new EQAProgram();
        chemistryProgram.setId(1L);
        chemistryProgram.setName("Chemistry PT");

        enrollment = new EQALabProgramEnrollment();
        enrollment.setId(1L);
        enrollment.setEqaProgram(chemistryProgram);
        enrollment.setProvider("WHO");
        enrollment.setDescription("Chemistry proficiency testing");
        enrollment.setIsActive(true);
        enrollment.setSysUserId("1");
    }

    @Test
    public void testFindAll() {
        when(enrollmentDAO.findAll()).thenReturn(List.of(enrollment));

        List<EQALabProgramEnrollment> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("Chemistry PT", result.get(0).getEqaProgram().getName());
    }

    @Test
    public void testFindActiveEnrollments() {
        when(enrollmentDAO.findByIsActive(true)).thenReturn(List.of(enrollment));

        List<EQALabProgramEnrollment> result = service.findActiveEnrollments();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    public void testCreateEnrollment_WithMappings() {
        List<Long> labUnitIds = List.of(10L, 20L);
        List<Long> testIds = List.of(100L, 101L);
        List<Long> panelIds = List.of(200L);

        when(enrollmentDAO.insert(any(EQALabProgramEnrollment.class))).thenReturn(1L);
        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(enrollment));

        EQALabProgramEnrollment input = new EQALabProgramEnrollment();
        input.setEqaProgram(chemistryProgram);
        input.setProvider("WHO");
        input.setSysUserId("1");

        EQALabProgramEnrollment result = service.createEnrollment(input, labUnitIds, testIds, panelIds);

        assertNotNull(result);
        assertEquals("Chemistry PT", result.getEqaProgram().getName());

        ArgumentCaptor<EQALabProgramEnrollment> captor = ArgumentCaptor.forClass(EQALabProgramEnrollment.class);
        verify(enrollmentDAO).insert(captor.capture());

        EQALabProgramEnrollment captured = captor.getValue();
        assertNotNull(captured.getCreatedDate());
        assertEquals(2, captured.getLabUnits().size());
        assertEquals(3, captured.getTestMaps().size());
    }

    @Test
    public void testCreateEnrollment_NullMappings() {
        when(enrollmentDAO.insert(any(EQALabProgramEnrollment.class))).thenReturn(1L);
        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(enrollment));

        EQAProgram hemaProgram = new EQAProgram();
        hemaProgram.setName("Hematology PT");

        EQALabProgramEnrollment input = new EQALabProgramEnrollment();
        input.setEqaProgram(hemaProgram);
        input.setProvider("CDC");
        input.setSysUserId("1");

        EQALabProgramEnrollment result = service.createEnrollment(input, null, null, null);

        assertNotNull(result);

        ArgumentCaptor<EQALabProgramEnrollment> captor =
                ArgumentCaptor.forClass(EQALabProgramEnrollment.class);
        verify(enrollmentDAO).insert(captor.capture());
        assertEquals(0, captor.getValue().getLabUnits().size());
        assertEquals(0, captor.getValue().getTestMaps().size());
    }

    @Test
    public void testCreateEnrollment_DefaultsIsActiveTrue() {
        when(enrollmentDAO.insert(any(EQALabProgramEnrollment.class))).thenReturn(1L);
        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(enrollment));

        EQALabProgramEnrollment input = new EQALabProgramEnrollment();
        input.setEqaProgram(chemistryProgram);
        input.setProvider("Provider");
        input.setIsActive(null);
        input.setSysUserId("1");

        service.createEnrollment(input, null, null, null);

        ArgumentCaptor<EQALabProgramEnrollment> captor =
                ArgumentCaptor.forClass(EQALabProgramEnrollment.class);
        verify(enrollmentDAO).insert(captor.capture());
        assertTrue(captor.getValue().getIsActive());
    }

    @Test
    public void testUpdateEnrollment_Success() {
        EQAProgram oldProgram = new EQAProgram();
        oldProgram.setName("Old Name");
        EQAProgram newProgram = new EQAProgram();
        newProgram.setName("New Name");

        EQALabProgramEnrollment existing = new EQALabProgramEnrollment();
        existing.setId(1L);
        existing.setEqaProgram(oldProgram);
        existing.setProvider("Old Provider");
        existing.setIsActive(true);
        existing.setSysUserId("1");
        existing.setLabUnits(new HashSet<>());
        existing.setTestMaps(new HashSet<>());

        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(existing));
        when(enrollmentDAO.update(any(EQALabProgramEnrollment.class))).thenReturn(existing);

        EQALabProgramEnrollment updated = new EQALabProgramEnrollment();
        updated.setEqaProgram(newProgram);
        updated.setProvider("New Provider");
        updated.setDescription("Updated description");
        updated.setIsActive(true);
        updated.setSysUserId("1");

        EQALabProgramEnrollment result = service.updateEnrollment(1L, updated, List.of(30L), List.of(300L), null);

        assertNotNull(result);
        assertEquals("New Name", result.getEqaProgram().getName());
        assertEquals("New Provider", result.getProvider());
        assertNotNull(result.getLastModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateEnrollment_NotFound() {
        when(enrollmentDAO.get(999L)).thenReturn(Optional.empty());

        EQALabProgramEnrollment updated = new EQALabProgramEnrollment();
        updated.setEqaProgram(chemistryProgram);
        updated.setProvider("Whatever");
        updated.setSysUserId("1");

        service.updateEnrollment(999L, updated, null, null, null);
    }

    @Test
    public void testSoftDelete_Success() {
        EQALabProgramEnrollment existing = new EQALabProgramEnrollment();
        existing.setId(1L);
        existing.setIsActive(true);
        existing.setSysUserId("1");

        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(existing));
        when(enrollmentDAO.update(any(EQALabProgramEnrollment.class))).thenReturn(existing);

        service.softDelete(1L);

        ArgumentCaptor<EQALabProgramEnrollment> captor = ArgumentCaptor.forClass(EQALabProgramEnrollment.class);
        verify(enrollmentDAO).update(captor.capture());
        assertFalse(captor.getValue().getIsActive());
        assertNotNull(captor.getValue().getLastModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSoftDelete_NotFound() {
        when(enrollmentDAO.get(999L)).thenReturn(Optional.empty());
        service.softDelete(999L);
    }

    @Test
    public void testGetDistinctProviders_UnionQuery() {
        when(enrollmentDAO.findDistinctProviders()).thenReturn(List.of("WHO", "CDC"));

        Organization whoOrg = new Organization();
        whoOrg.setOrganizationName("WHO");
        Organization pepfarOrg = new Organization();
        pepfarOrg.setOrganizationName("PEPFAR");

        EQAProgram prog1 = new EQAProgram();
        prog1.setOrganization(whoOrg);
        EQAProgram prog2 = new EQAProgram();
        prog2.setOrganization(pepfarOrg);

        when(eqaProgramDAO.findByIsActive(true)).thenReturn(List.of(prog1, prog2));

        List<String> result = service.getDistinctProviders();

        assertEquals(3, result.size());
        assertEquals("CDC", result.get(0));
        assertEquals("PEPFAR", result.get(1));
        assertEquals("WHO", result.get(2));
    }

    @Test
    public void testGetDistinctProviders_NullAndBlankFiltered() {
        when(enrollmentDAO.findDistinctProviders()).thenReturn(List.of("WHO"));

        Organization blankOrg = new Organization();
        blankOrg.setOrganizationName("  ");

        EQAProgram prog1 = new EQAProgram();
        prog1.setOrganization(null);
        EQAProgram prog2 = new EQAProgram();
        prog2.setOrganization(blankOrg);

        when(eqaProgramDAO.findByIsActive(true)).thenReturn(List.of(prog1, prog2));

        List<String> result = service.getDistinctProviders();

        assertEquals(1, result.size());
        assertEquals("WHO", result.get(0));
    }

    @Test
    public void testGetDistinctProviders_Empty() {
        when(enrollmentDAO.findDistinctProviders()).thenReturn(new ArrayList<>());
        when(eqaProgramDAO.findByIsActive(true)).thenReturn(new ArrayList<>());

        List<String> result = service.getDistinctProviders();

        assertTrue(result.isEmpty());
    }
}
