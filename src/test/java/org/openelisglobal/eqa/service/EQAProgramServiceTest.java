package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.dao.EQAProgramTestDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;

@RunWith(MockitoJUnitRunner.class)
public class EQAProgramServiceTest {

    @Mock
    private EQAProgramDAO eqaProgramDAO;

    @Mock
    private EQAProgramTestDAO eqaProgramTestDAO;

    @InjectMocks
    private EQAProgramServiceImpl programService;

    @Test
    public void testFindActivePrograms_ReturnsActiveOnly() {
        EQAProgram active1 = new EQAProgram();
        active1.setId(1L);
        active1.setName("Program A");
        active1.setIsActive(true);

        EQAProgram active2 = new EQAProgram();
        active2.setId(2L);
        active2.setName("Program B");
        active2.setIsActive(true);

        when(eqaProgramDAO.findByIsActive(true)).thenReturn(Arrays.asList(active1, active2));

        List<EQAProgram> result = programService.findActivePrograms();

        assertEquals("Should return 2 active programs", 2, result.size());
        verify(eqaProgramDAO).findByIsActive(true);
    }

    @Test
    public void testDeactivateProgram_SetsIsActiveToFalse() {
        EQAProgram program = new EQAProgram();
        program.setId(1L);
        program.setIsActive(true);

        when(eqaProgramDAO.get(1L)).thenReturn(Optional.of(program));
        when(eqaProgramDAO.update(any(EQAProgram.class))).thenAnswer(i -> i.getArgument(0));

        EQAProgram result = programService.deactivateProgram(1L);

        assertFalse("Program should be deactivated", result.getIsActive());
        verify(eqaProgramDAO).update(program);
    }

    @Test
    public void testActivateProgram_SetsIsActiveToTrue() {
        EQAProgram program = new EQAProgram();
        program.setId(1L);
        program.setIsActive(false);

        when(eqaProgramDAO.get(1L)).thenReturn(Optional.of(program));
        when(eqaProgramDAO.update(any(EQAProgram.class))).thenAnswer(i -> i.getArgument(0));

        EQAProgram result = programService.activateProgram(1L);

        assertTrue("Program should be activated", result.getIsActive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeactivateProgram_WithInvalidId_ThrowsException() {
        when(eqaProgramDAO.get(999L)).thenReturn(Optional.empty());
        programService.deactivateProgram(999L);
    }

    @Test
    public void testAssignTest_CreatesNewProgramTest() {
        EQAProgram program = new EQAProgram();
        program.setId(1L);

        when(eqaProgramDAO.get(1L)).thenReturn(Optional.of(program));
        when(eqaProgramTestDAO.insert(any(EQAProgramTest.class))).thenReturn(1L);

        EQAProgramTest result = programService.assignTest(1L, 10L);

        assertNotNull("Program test should not be null", result);
        assertEquals("Test ID should be 10", Long.valueOf(10L), result.getTestId());
        assertTrue("Should be active", result.getIsActive());
        verify(eqaProgramTestDAO).insert(any(EQAProgramTest.class));
    }

    @Test
    public void testRemoveTestAssignment_SetsIsActiveToFalse() {
        EQAProgramTest programTest = new EQAProgramTest();
        programTest.setId(1L);
        programTest.setIsActive(true);

        when(eqaProgramTestDAO.get(1L)).thenReturn(Optional.of(programTest));
        when(eqaProgramTestDAO.update(any(EQAProgramTest.class))).thenAnswer(i -> i.getArgument(0));

        programService.removeTestAssignment(1L);

        assertFalse("Program test should be deactivated", programTest.getIsActive());
        verify(eqaProgramTestDAO).update(programTest);
    }

    @Test
    public void testGetTestAssignments_DelegatesToDAO() {
        EQAProgramTest pt1 = new EQAProgramTest();
        pt1.setTestId(10L);
        EQAProgramTest pt2 = new EQAProgramTest();
        pt2.setTestId(20L);

        when(eqaProgramTestDAO.findByProgramId(1L)).thenReturn(Arrays.asList(pt1, pt2));

        List<EQAProgramTest> result = programService.getTestAssignments(1L);

        assertEquals("Should return 2 test assignments", 2, result.size());
    }
}
