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

@RunWith(MockitoJUnitRunner.class)
public class EQALabProgramEnrollmentServiceTest {

    @Mock
    private EQALabProgramEnrollmentDAO enrollmentDAO;

    @Mock
    private EQAProgramDAO eqaProgramDAO;

    @InjectMocks
    private EQALabProgramEnrollmentServiceImpl service;

    private EQALabProgramEnrollment enrollment;

    @Before
    public void setUp() {
        enrollment = new EQALabProgramEnrollment();
        enrollment.setId(1L);
        enrollment.setProgramName("Chemistry PT");
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
        assertEquals("Chemistry PT", result.get(0).getProgramName());
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
        input.setProgramName("Chemistry PT");
        input.setProvider("WHO");
        input.setSysUserId("1");

        EQALabProgramEnrollment result = service.createEnrollment(input, labUnitIds, testIds, panelIds);

        assertNotNull(result);
        assertEquals("Chemistry PT", result.getProgramName());

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

        EQALabProgramEnrollment input = new EQALabProgramEnrollment();
        input.setProgramName("Hematology PT");
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
        input.setProgramName("Test");
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
        EQALabProgramEnrollment existing = new EQALabProgramEnrollment();
        existing.setId(1L);
        existing.setProgramName("Old Name");
        existing.setProvider("Old Provider");
        existing.setIsActive(true);
        existing.setSysUserId("1");
        existing.setLabUnits(new ArrayList<>());
        existing.setTestMaps(new ArrayList<>());

        when(enrollmentDAO.get(1L)).thenReturn(Optional.of(existing));
        when(enrollmentDAO.update(any(EQALabProgramEnrollment.class))).thenReturn(existing);

        EQALabProgramEnrollment updated = new EQALabProgramEnrollment();
        updated.setProgramName("New Name");
        updated.setProvider("New Provider");
        updated.setDescription("Updated description");
        updated.setIsActive(true);
        updated.setSysUserId("1");

        EQALabProgramEnrollment result = service.updateEnrollment(1L, updated, List.of(30L), List.of(300L), null);

        assertNotNull(result);
        assertEquals("New Name", result.getProgramName());
        assertEquals("New Provider", result.getProvider());
        assertNotNull(result.getLastModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateEnrollment_NotFound() {
        when(enrollmentDAO.get(999L)).thenReturn(Optional.empty());

        EQALabProgramEnrollment updated = new EQALabProgramEnrollment();
        updated.setProgramName("Whatever");
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

        EQAProgram prog1 = new EQAProgram();
        prog1.setProviderName("WHO");
        EQAProgram prog2 = new EQAProgram();
        prog2.setProviderName("PEPFAR");

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

        EQAProgram prog1 = new EQAProgram();
        prog1.setProviderName(null);
        EQAProgram prog2 = new EQAProgram();
        prog2.setProviderName("  ");

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
