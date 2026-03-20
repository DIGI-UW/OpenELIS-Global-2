package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQAProgramRestController;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAProgramRestControllerTest {

    @Mock
    private EQAProgramService programService;

    @Mock
    private EQAProgramEnrollmentService enrollmentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private EQAProgramRestController controller;

    private EQAProgram program1;
    private EQAProgram program2;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);

        program1 = new EQAProgram();
        program1.setId(1L);
        program1.setName("Chemistry PT");
        program1.setDescription("Chemistry proficiency testing");
        program1.setIsActive(true);
        program1.setFhirUuid(UUID.randomUUID());

        program2 = new EQAProgram();
        program2.setId(2L);
        program2.setName("Hematology PT");
        program2.setDescription("Hematology proficiency testing");
        program2.setIsActive(false);
        program2.setFhirUuid(UUID.randomUUID());
    }

    @Test
    public void testCreateProgram_Success() {
        when(programService.insert(any(EQAProgram.class))).thenReturn(1L);
        when(programService.get(1L)).thenReturn(program1);

        Map<String, Object> body = Map.of("name", "Chemistry PT", "description", "Chemistry proficiency testing");
        ResponseEntity<?> response = controller.createProgram(request, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> dto = (Map<String, Object>) response.getBody();
        assertEquals("Chemistry PT", dto.get("name"));
        assertEquals(true, dto.get("isActive"));
    }

    @Test
    public void testCreateProgram_MissingName() {
        Map<String, Object> body = Map.of("description", "No name");
        ResponseEntity<?> response = controller.createProgram(request, body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testCreateProgram_BlankName() {
        Map<String, Object> body = Map.of("name", "   ");
        ResponseEntity<?> response = controller.createProgram(request, body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testListPrograms_All() {
        when(programService.getAll()).thenReturn(List.of(program1, program2));

        ResponseEntity<List<Map<String, Object>>> response = controller.listPrograms(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testListPrograms_ActiveOnly() {
        when(programService.findActivePrograms()).thenReturn(List.of(program1));

        ResponseEntity<List<Map<String, Object>>> response = controller.listPrograms(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Chemistry PT", response.getBody().get(0).get("name"));
    }

    @Test
    public void testGetProgram_Found() {
        when(programService.get(1L)).thenReturn(program1);

        ResponseEntity<?> response = controller.getProgram(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> dto = (Map<String, Object>) response.getBody();
        assertEquals("Chemistry PT", dto.get("name"));
    }

    @Test
    public void testGetProgram_NotFound() {
        when(programService.get(999L)).thenThrow(new ObjectNotFoundException(999L, "EQAProgram"));

        ResponseEntity<?> response = controller.getProgram(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testUpdateProgram_Name() {
        when(programService.get(1L)).thenReturn(program1);
        when(programService.update(any(EQAProgram.class))).thenReturn(program1);

        Map<String, Object> body = Map.of("name", "Updated Chemistry PT");
        ResponseEntity<?> response = controller.updateProgram(request, 1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(programService).update(any(EQAProgram.class));
    }

    @Test
    public void testUpdateProgram_Deactivate() {
        when(programService.get(1L)).thenReturn(program1);
        when(programService.deactivateProgram(1L)).thenReturn(program1);

        Map<String, Object> body = Map.of("isActive", false);
        ResponseEntity<?> response = controller.updateProgram(request, 1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(programService).deactivateProgram(1L);
    }

    @Test
    public void testUpdateProgram_Activate() {
        when(programService.get(2L)).thenReturn(program2);
        when(programService.activateProgram(2L)).thenReturn(program2);

        Map<String, Object> body = Map.of("isActive", true);
        ResponseEntity<?> response = controller.updateProgram(request, 2L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(programService).activateProgram(2L);
    }

    @Test
    public void testUpdateProgram_NotFound() {
        when(programService.get(999L)).thenThrow(new ObjectNotFoundException(999L, "EQAProgram"));

        Map<String, Object> body = Map.of("name", "Whatever");
        ResponseEntity<?> response = controller.updateProgram(request, 999L, body);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testUpdateProgram_BlankName() {
        when(programService.get(1L)).thenReturn(program1);

        Map<String, Object> body = Map.of("name", "  ");
        ResponseEntity<?> response = controller.updateProgram(request, 1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testGetTestAssignments() {
        when(programService.get(1L)).thenReturn(program1);

        EQAProgramTest pt1 = new EQAProgramTest();
        pt1.setId(10L);
        pt1.setTestId(100L);
        pt1.setIsActive(true);

        EQAProgramTest pt2 = new EQAProgramTest();
        pt2.setId(11L);
        pt2.setTestId(101L);
        pt2.setIsActive(true);

        when(programService.getTestAssignments(1L)).thenReturn(List.of(pt1, pt2));

        ResponseEntity<?> response = controller.getTestAssignments(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dtos = (List<Map<String, Object>>) response.getBody();
        assertEquals(2, dtos.size());
        assertEquals(100L, dtos.get(0).get("testId"));
    }

    @Test
    public void testGetTestAssignments_ProgramNotFound() {
        when(programService.get(999L)).thenThrow(new ObjectNotFoundException(999L, "EQAProgram"));

        ResponseEntity<?> response = controller.getTestAssignments(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testUpdateTestAssignments_Success() {
        when(programService.get(1L)).thenReturn(program1);
        when(programService.getTestAssignments(1L)).thenReturn(List.of());

        EQAProgramTest newPt = new EQAProgramTest();
        newPt.setId(20L);
        newPt.setTestId(200L);
        newPt.setIsActive(true);

        when(programService.assignTest(eq(1L), eq(200L))).thenReturn(newPt);
        when(programService.getTestAssignments(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(newPt));

        Map<String, Object> body = Map.of("testIds", List.of(200));
        ResponseEntity<?> response = controller.updateTestAssignments(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(programService).assignTest(1L, 200L);
    }

    @Test
    public void testUpdateTestAssignments_MissingTestIds() {
        when(programService.get(1L)).thenReturn(program1);

        Map<String, Object> body = Map.of("name", "irrelevant");
        ResponseEntity<?> response = controller.updateTestAssignments(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
