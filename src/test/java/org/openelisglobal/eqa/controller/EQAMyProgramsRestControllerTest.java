package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQAMyProgramsRestController;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentLabUnit;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAMyProgramsRestControllerTest {

    @Mock
    private EQALabProgramEnrollmentService enrollmentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private EQAMyProgramsRestController controller;

    private EQALabProgramEnrollment enrollment1;
    private EQALabProgramEnrollment enrollment2;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);

        enrollment1 = new EQALabProgramEnrollment();
        enrollment1.setId(1L);
        enrollment1.setProgramName("Chemistry PT");
        enrollment1.setProvider("WHO");
        enrollment1.setDescription("Chemistry proficiency testing");
        enrollment1.setIsActive(true);
        enrollment1.setCreatedDate(new Date());
        enrollment1.setSysUserId("1");

        EQALabEnrollmentLabUnit lu = new EQALabEnrollmentLabUnit();
        lu.setId(10L);
        lu.setTestSectionId(100L);
        lu.setEnrollment(enrollment1);
        enrollment1.getLabUnits().add(lu);

        EQALabEnrollmentTestMap tm = new EQALabEnrollmentTestMap();
        tm.setId(20L);
        tm.setTestId(200L);
        tm.setEnrollment(enrollment1);
        enrollment1.getTestMaps().add(tm);

        enrollment2 = new EQALabProgramEnrollment();
        enrollment2.setId(2L);
        enrollment2.setProgramName("Hematology PT");
        enrollment2.setProvider("CDC");
        enrollment2.setIsActive(false);
        enrollment2.setCreatedDate(new Date());
        enrollment2.setSysUserId("1");
    }

    @Test
    public void testListMyPrograms() {
        when(enrollmentService.findAll()).thenReturn(List.of(enrollment1, enrollment2));

        ResponseEntity<List<Map<String, Object>>> response = controller.listMyPrograms();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("Chemistry PT", response.getBody().get(0).get("programName"));
        assertEquals("Hematology PT", response.getBody().get(1).get("programName"));
    }

    @Test
    public void testListMyPrograms_Empty() {
        when(enrollmentService.findAll()).thenReturn(new ArrayList<>());

        ResponseEntity<List<Map<String, Object>>> response = controller.listMyPrograms();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testGetMyProgram_Found() {
        when(enrollmentService.get(1L)).thenReturn(enrollment1);

        ResponseEntity<?> response = controller.getMyProgram(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> dto = (Map<String, Object>) response.getBody();
        assertEquals("Chemistry PT", dto.get("programName"));
        assertEquals("WHO", dto.get("provider"));
        assertEquals(true, dto.get("isActive"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetMyProgram_DtoContainsLabUnitsAndTests() {
        when(enrollmentService.get(1L)).thenReturn(enrollment1);

        ResponseEntity<?> response = controller.getMyProgram(1L);

        Map<String, Object> dto = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> labUnits = (List<Map<String, Object>>) dto.get("labUnits");
        List<Map<String, Object>> tests = (List<Map<String, Object>>) dto.get("tests");

        assertEquals(1, labUnits.size());
        assertEquals(100L, labUnits.get(0).get("id"));
        assertEquals(1, tests.size());
        assertEquals(200L, tests.get(0).get("id"));
    }

    @Test
    public void testGetMyProgram_NotFound() {
        when(enrollmentService.get(999L)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<?> response = controller.getMyProgram(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testCreateMyProgram_Success() {
        when(enrollmentService.createEnrollment(
                        any(EQALabProgramEnrollment.class), anyList(), anyList(), anyList()))
                .thenReturn(enrollment1);

        Map<String, Object> body = new HashMap<>();
        body.put("programName", "Chemistry PT");
        body.put("provider", "WHO");
        body.put("description", "Chemistry proficiency testing");
        body.put("labUnitIds", List.of(100));
        body.put("testIds", List.of(200));
        body.put("panelIds", List.of(300));

        ResponseEntity<?> response = controller.createMyProgram(request, body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> dto = (Map<String, Object>) response.getBody();
        assertEquals("Chemistry PT", dto.get("programName"));
    }

    @Test
    public void testCreateMyProgram_MissingProgramName() {
        Map<String, Object> body = new HashMap<>();
        body.put("provider", "WHO");

        ResponseEntity<?> response = controller.createMyProgram(request, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testCreateMyProgram_BlankProgramName() {
        Map<String, Object> body = new HashMap<>();
        body.put("programName", "   ");
        body.put("provider", "WHO");

        ResponseEntity<?> response = controller.createMyProgram(request, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testCreateMyProgram_MissingProvider() {
        Map<String, Object> body = new HashMap<>();
        body.put("programName", "Chemistry PT");

        ResponseEntity<?> response = controller.createMyProgram(request, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testUpdateMyProgram_Success() {
        when(enrollmentService.updateEnrollment(
                        eq(1L), any(EQALabProgramEnrollment.class), anyList(), anyList(), anyList()))
                .thenReturn(enrollment1);

        Map<String, Object> body = new HashMap<>();
        body.put("programName", "Updated Chemistry PT");
        body.put("provider", "WHO");
        body.put("labUnitIds", List.of(100));
        body.put("testIds", List.of(200));
        body.put("panelIds", List.of(300));

        ResponseEntity<?> response = controller.updateMyProgram(request, 1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testUpdateMyProgram_NotFound() {
        when(enrollmentService.updateEnrollment(
                        eq(999L), any(EQALabProgramEnrollment.class), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Not found"));

        Map<String, Object> body = new HashMap<>();
        body.put("programName", "Whatever");
        body.put("provider", "Whatever");

        ResponseEntity<?> response = controller.updateMyProgram(request, 999L, body);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testUpdateMyProgram_MissingProgramName() {
        Map<String, Object> body = new HashMap<>();
        body.put("provider", "WHO");

        ResponseEntity<?> response = controller.updateMyProgram(request, 1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testDeleteMyProgram_Success() {
        ResponseEntity<Void> response = controller.deleteMyProgram(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(enrollmentService).softDelete(1L);
    }

    @Test
    public void testDeleteMyProgram_NotFound() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Not found")).when(enrollmentService).softDelete(999L);

        ResponseEntity<Void> response = controller.deleteMyProgram(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetProviders() {
        when(enrollmentService.getDistinctProviders()).thenReturn(List.of("CDC", "PEPFAR", "WHO"));

        ResponseEntity<List<String>> response = controller.getProviders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertEquals("CDC", response.getBody().get(0));
    }
}
