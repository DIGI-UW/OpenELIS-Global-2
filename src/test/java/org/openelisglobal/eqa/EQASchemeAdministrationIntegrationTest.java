package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQAProgramRestController;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Scheme administration from the application, against the real schema: the
 * arrangement type the user chose reaches the row instead of defaulting
 * silently, an in-house scheme may omit the provider every other type requires,
 * and the scheme's test map — which provider result intake and its CSV import
 * both read — can be written through the endpoint the Programs screen calls.
 */
public class EQASchemeAdministrationIntegrationTest extends EQASpineTestBase {

    private static final long TEST_SERO = 9971L;
    private static final long TEST_VL = 9972L;

    @Autowired
    private EQAProgramEnrollmentService enrollmentService;

    private EQAProgramRestController controller;

    @Before
    public void buildController() {
        controller = new EQAProgramRestController(eqaProgramService, enrollmentService, systemUserService);
    }

    @Test
    public void createProgram_writesTheChosenTypeRatherThanTheEntityDefault() {
        Long id = created(Map.of("name", "Regional serology " + System.nanoTime(), "schemeType", "REGIONAL_PT",
                "provider", "CPHL"));

        assertEquals(EQASchemeType.REGIONAL_PT, eqaProgramService.get(id).getSchemeType());
    }

    @Test
    public void createProgram_acceptsAnInHouseSchemeWithNoProvider() {
        Long id = created(Map.of("name", "In-house blinded " + System.nanoTime(), "schemeType", "IN_HOUSE"));

        EQAProgram scheme = eqaProgramService.get(id);
        assertEquals(EQASchemeType.IN_HOUSE, scheme.getSchemeType());
        assertNull("a blank provider is stored as NULL, not as an empty string", scheme.getProvider());
    }

    @Test
    public void createProgram_refusesAMissingType() {
        ResponseEntity<?> response = controller.createProgram(request(),
                Map.of("name", "No type " + System.nanoTime(), "provider", "CPHL"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Scheme type is required", error(response));
    }

    @Test
    public void createProgram_refusesAnUnknownType() {
        ResponseEntity<?> response = controller.createProgram(request(),
                Map.of("name", "Bad type " + System.nanoTime(), "schemeType", "NATIONAL_PT", "provider", "CPHL"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unknown scheme type: NATIONAL_PT", error(response));
    }

    @Test
    public void createProgram_stillEnforcesTheProviderRuleForExternalTypes() {
        ResponseEntity<?> response = controller.createProgram(request(),
                Map.of("name", "Provider-less international " + System.nanoTime(), "schemeType", "INTERNATIONAL_PT"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(error(response).contains("Provider is required"));
    }

    @Test
    public void updateProgram_movesAnExternalSchemeInHouseAndDropsItsProvider() {
        Long id = created(Map.of("name", "Becomes in-house " + System.nanoTime(), "schemeType", "REGIONAL_PT",
                "provider", "CPHL"));

        ResponseEntity<?> response = controller.updateProgram(request(), id,
                Map.of("schemeType", "IN_HOUSE", "provider", ""));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EQAProgram scheme = eqaProgramService.get(id);
        assertEquals(EQASchemeType.IN_HOUSE, scheme.getSchemeType());
        assertNull(scheme.getProvider());
    }

    @Test
    public void updateTestAssignments_writesTheMapProviderIntakeReads() {
        seedTest(TEST_SERO, "Scheme admin serology");
        seedTest(TEST_VL, "Scheme admin viral load");
        Long id = created(
                Map.of("name", "Mapped scheme " + System.nanoTime(), "schemeType", "REGIONAL_PT", "provider", "CPHL"));

        controller.updateTestAssignments(id, Map.of("testIds", List.of(TEST_SERO, TEST_VL)));

        assertEquals(List.of(TEST_SERO, TEST_VL), assignedTestIds(id));

        // Re-sending a narrowed selection replaces the map rather than adding to
        // it. Dropping a test deactivates its row instead of deleting it — the
        // UNIQUE(scheme, test) row is reused if the test comes back — so what the
        // intake readers see is the active set, not the row count.
        controller.updateTestAssignments(id, Map.of("testIds", List.of(TEST_VL)));

        assertEquals(List.of(TEST_VL), assignedTestIds(id));
        assertEquals(2, eqaProgramService.getTestAssignments(id).size());
    }

    // ---- helpers ----

    private Long created(Map<String, Object> body) {
        ResponseEntity<?> response = controller.createProgram(request(), body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return Long.valueOf(String.valueOf(dto(response).get("id")));
    }

    private List<Long> assignedTestIds(Long programId) {
        return eqaProgramService.getTestAssignments(programId).stream()
                .filter(assignment -> Boolean.TRUE.equals(assignment.getIsActive())).map(EQAProgramTest::getTestId)
                .sorted().toList();
    }

    private void seedTest(long id, String name) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                id, name, name, UUID.randomUUID().toString(), id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dto(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private String error(ResponseEntity<?> response) {
        return String.valueOf(dto(response).get("error"));
    }

    private HttpServletRequest request() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(USER));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
