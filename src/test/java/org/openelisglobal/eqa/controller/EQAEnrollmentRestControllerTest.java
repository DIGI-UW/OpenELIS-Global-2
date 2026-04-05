package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.openelisglobal.eqa.controller.rest.EQAEnrollmentRestController;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAEnrollmentRestControllerTest {

    @Mock
    private EQAProgramEnrollmentService enrollmentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private EQAEnrollmentRestController controller;

    private EQAProgram program;
    private EQAProgramEnrollment enrollment1;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);

        program = new EQAProgram();
        program.setId(1L);
        program.setName("Chemistry PT");

        enrollment1 = new EQAProgramEnrollment();
        enrollment1.setId(10L);
        enrollment1.setEqaProgram(program);
        enrollment1.setOrganizationId(100L);
        enrollment1.setEnrollmentDate(new Date());
        enrollment1.setStatus("Active");
        enrollment1.setSysUserId("1");
    }

    @Test
    public void testListEnrollments() {
        when(enrollmentService.findByProgramId(1L)).thenReturn(List.of(enrollment1));

        ResponseEntity<List<Map<String, Object>>> response = controller.listEnrollments(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(100L, response.getBody().get(0).get("organizationId"));
    }

    @Test
    public void testCreateEnrollments_Success() {
        when(enrollmentService.bulkEnroll(eq(1L), anyList(), eq("1")))
                .thenReturn(List.of(enrollment1));

        Map<String, Object> body = new HashMap<>();
        body.put("organizationIds", List.of(100));

        ResponseEntity<?> response = controller.createEnrollments(request, 1L, body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void testCreateEnrollments_MissingOrgIds() {
        Map<String, Object> body = new HashMap<>();

        ResponseEntity<?> response = controller.createEnrollments(request, 1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testCreateEnrollments_EmptyOrgIds() {
        Map<String, Object> body = new HashMap<>();
        body.put("organizationIds", List.of());

        ResponseEntity<?> response = controller.createEnrollments(request, 1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testUpdateEnrollmentStatus_Success() {
        when(enrollmentService.updateStatus(eq(10L), eq("Suspended"), any(), eq("1")))
                .thenReturn(enrollment1);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "Suspended");

        ResponseEntity<?> response = controller.updateEnrollmentStatus(request, 1L, 10L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testUpdateEnrollmentStatus_MissingStatus() {
        Map<String, Object> body = new HashMap<>();

        ResponseEntity<?> response = controller.updateEnrollmentStatus(request, 1L, 10L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testUpdateEnrollmentStatus_InvalidTransition() {
        when(enrollmentService.updateStatus(eq(10L), eq("Active"), any(), eq("1")))
                .thenThrow(new IllegalArgumentException("Invalid transition"));

        Map<String, Object> body = new HashMap<>();
        body.put("status", "Active");

        ResponseEntity<?> response = controller.updateEnrollmentStatus(request, 1L, 10L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testGetEligibleOrganizations() {
        List<Map<String, Object>> orgs = List.of(
                Map.of("id", "101", "organizationName", "Hospital A", "alreadyEnrolled", true),
                Map.of("id", "102", "organizationName", "Clinic B", "alreadyEnrolled", false));

        when(enrollmentService.getEligibleOrganizations(1L)).thenReturn(orgs);

        ResponseEntity<List<Map<String, Object>>> response = controller.getEligibleOrganizations(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}
