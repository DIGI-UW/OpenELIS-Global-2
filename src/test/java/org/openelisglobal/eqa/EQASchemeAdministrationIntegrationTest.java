package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    public void updateProgram_refusesATypeChangeWhileACycleIsStillRunning() {
        Long id = created(Map.of("name", "Has a live cycle " + System.nanoTime(), "schemeType", "REGIONAL_PT",
                "provider", "CPHL"));
        insertCycle(eqaProgramService.get(id), 4);

        ResponseEntity<?> response = controller.updateProgram(request(), id, Map.of("schemeType", "IN_HOUSE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue("the refusal names the cycle that blocks it: " + errorOf(response),
                errorOf(response).contains("cycle 4"));
        assertTrue("and offers the only route an operator actually has: " + errorOf(response),
                errorOf(response).toLowerCase().contains("create a new scheme"));
        // Nothing in the product closes a cycle: the SCORED to CLOSED edge is legal on
        // all three machines and no caller asks for it. Advice to close this one first
        // would be advice nobody can follow, so the message must not give it.
        assertFalse("and does not tell them to close it, which no screen can do: " + errorOf(response),
                errorOf(response).toLowerCase().contains("close"));
        assertEquals("and the stored type is untouched", EQASchemeType.REGIONAL_PT,
                eqaProgramService.get(id).getSchemeType());
    }

    @Test
    public void updateProgram_refusesATypeChangeWhenTheSchemeStaysManagedAcrossTheCall() {
        // Every other update test here runs its service calls in separate transactions,
        // so the scheme is already detached by the time update() sees it and the guard
        // reads the stored row without trying. A request holds one persistence context
        // across the read and the write, and there a plain read hands the guard back
        // the
        // very object the controller has just edited, so it compares the new type with
        // itself and waves the change through. Holding one transaction open around the
        // call reproduces that, which is the only shape in which this guard can fail.
        Long id = created(Map.of("name", "Managed across the call " + System.nanoTime(), "schemeType", "REGIONAL_PT",
                "provider", "CPHL"));
        insertCycle(eqaProgramService.get(id), 7);

        ResponseEntity<?> response = inOneTransaction(
                () -> controller.updateProgram(request(), id, Map.of("schemeType", "IN_HOUSE")));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue("the refusal names the cycle that blocks it: " + errorOf(response),
                errorOf(response).contains("cycle 7"));
        assertEquals("and the stored type is untouched", EQASchemeType.REGIONAL_PT,
                eqaProgramService.get(id).getSchemeType());
    }

    @Test
    public void updateProgram_allowsATypeChangeWhenEveryCycleIsClosed() {
        // The V1 upgrade path. Every completed legacy distribution was backfilled a
        // CLOSED cycle, and those schemes all took the INTERNATIONAL_PT default, so
        // refusing on any cycle at all would strand them outside in-house blinding.
        Long id = created(Map.of("name", "Legacy V1 scheme " + System.nanoTime(), "schemeType", "INTERNATIONAL_PT",
                "provider", "CPHL"));
        Long cycleId = insertCycle(eqaProgramService.get(id), 1);
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'CLOSED' WHERE id = ?", cycleId);

        ResponseEntity<?> response = controller.updateProgram(request(), id,
                Map.of("schemeType", "IN_HOUSE", "provider", ""));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EQASchemeType.IN_HOUSE, eqaProgramService.get(id).getSchemeType());
    }

    @Test
    public void updateProgram_letsAnEditResendTheSameTypeUnderALiveCycle() {
        // The Programs form sends every field it displays, so an edit to the name or
        // the review gate carries schemeType with it. That is not a type change.
        Long id = created(
                Map.of("name", "Unchanged type " + System.nanoTime(), "schemeType", "REGIONAL_PT", "provider", "CPHL"));
        insertCycle(eqaProgramService.get(id), 2);

        ResponseEntity<?> response = controller.updateProgram(request(), id,
                Map.of("schemeType", "REGIONAL_PT", "provider", "CPHL", "description", "Second round"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Second round", eqaProgramService.get(id).getDescription());
        assertEquals(EQASchemeType.REGIONAL_PT, eqaProgramService.get(id).getSchemeType());
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

    /**
     * Runs the call with one transaction, and so one persistence context, open
     * around it, the way a request does. The transaction is rolled back rather than
     * committed: refusing marks it rollback-only, and committing a rollback-only
     * transaction raises UnexpectedRollbackException over whichever assertion
     * should be reporting the real failure.
     */
    private ResponseEntity<?> inOneTransaction(Supplier<ResponseEntity<?>> call) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            ResponseEntity<?> response = call.get();
            status.setRollbackOnly();
            return response;
        });
    }

    private String errorOf(ResponseEntity<?> response) {
        Object error = ((Map<?, ?>) response.getBody()).get("error");
        return error == null ? "" : String.valueOf(error);
    }

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
