package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQACycleStateTransitionDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionChannel;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-609 [EQA V2.1 / T-08] — the cycle spine against a real DB.
 *
 * <p>
 * The test DB is Liquibase-provisioned (BaseTestConfig runs base-changelog.xml
 * against a testcontainers Postgres), so qa/015 and qa/016 shape these tables:
 * CHECK constraints, foreign keys and unique constraints are all live here, and
 * the tests assert them rather than deferring to manual UAT.
 *
 * <p>
 * BaseWebContextSensitiveTest runs with transactions NOT_SUPPORTED, so each DAO
 * call commits on its own — a read-back is a genuinely fresh session, not the
 * same instance handed back from a first-level cache.
 */
public class EQACycleSpineIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER = "1";
    private static final long ADMIN_USER_ID = 1L;

    private static final long ENROLLMENT_ID = 9901L;
    private static final long ANALYTE_HIV_SEROLOGY = 9801L;
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long TEST_HIV_SEROLOGY = 9701L;

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private EQAParticipantResultDAO eqaParticipantResultDAO;

    @Autowired
    private EQACycleStateTransitionDAO eqaCycleStateTransitionDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/eqa-cycle-spine.xml");
        clean();
        seedLabEnrollment();
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_cycle_state_transition");
        jdbc.update("DELETE FROM clinlims.eqa_round");
        jdbc.update("DELETE FROM clinlims.eqa_cycle");
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id = ?", ENROLLMENT_ID);
        jdbc.update("DELETE FROM clinlims.eqa_program_test");
        jdbc.update("DELETE FROM clinlims.eqa_program");
    }

    // ---- FR-V2.1-01/02/05/21: the spine persists and reads back intact ----

    @Test
    public void cycleRoundResultAndTransitionRoundTripWithExactValues() {
        EQAProgram scheme = insertScheme("Spine round-trip", EQASchemeType.INTERNATIONAL_PT, "NHLS");

        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(1);
        cycle.setCycleName("2026 Cycle 1");
        cycle.setStatus(EQACycleStatus.TESTING);
        cycle.setCreatedBy(systemUser(ADMIN_USER_ID));
        cycle.setSysUserId(USER);
        Long cycleId = eqaCycleDAO.insert(cycle);

        EQACycle readCycle = eqaCycleDAO.get(cycleId).orElseThrow(AssertionError::new);
        assertEquals("2026 Cycle 1", readCycle.getCycleName());
        assertEquals(Integer.valueOf(1), readCycle.getCycleNumber());
        assertEquals(EQACycleStatus.TESTING, readCycle.getStatus());
        assertEquals(scheme.getId(), readCycle.getScheme().getId());
        assertEquals(Long.valueOf(ADMIN_USER_ID), Long.valueOf(readCycle.getCreatedBy().getId()));
        assertNotNull("fhirUuid is assigned on persist", readCycle.getFhirUuid());
        assertNotNull("createdAt is assigned on persist", readCycle.getCreatedAt());

        Long roundId = insertRound(readCycle, 1, "OPEN");
        EQARound readRound = eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(1), readRound.getRoundNumber());
        assertEquals("OPEN", readRound.getStatus());
        assertEquals(cycleId, readRound.getCycle().getId());

        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(readCycle);
        result.setRound(readRound);
        result.setLabEnrollmentId(ENROLLMENT_ID);
        result.setAnalyteId(ANALYTE_HIV_VL);
        result.setResultValue("4.52");
        result.setResultUnit("log10 c/mL");
        result.setSubmissionStatus(EQASubmissionStatus.SUBMITTED);
        result.setSubmissionChannel(EQASubmissionChannel.MANUAL);
        result.setManualSubmissionReference("PT-2026-001");
        result.setEnteredBy(ADMIN_USER_ID);
        result.setEnteredAt(new Timestamp(System.currentTimeMillis()));
        result.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
        result.setSysUserId(USER);
        Long resultId = eqaParticipantResultDAO.insert(result);

        EQAParticipantResult readResult = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        assertEquals("4.52", readResult.getResultValue());
        assertEquals("log10 c/mL", readResult.getResultUnit());
        assertEquals(EQASubmissionStatus.SUBMITTED, readResult.getSubmissionStatus());
        assertEquals(EQASubmissionChannel.MANUAL, readResult.getSubmissionChannel());
        assertEquals("PT-2026-001", readResult.getManualSubmissionReference());
        assertEquals(Long.valueOf(ENROLLMENT_ID), readResult.getLabEnrollmentId());
        assertEquals(Long.valueOf(ANALYTE_HIV_VL), readResult.getAnalyteId());
        assertEquals(cycleId, readResult.getCycle().getId());
        assertEquals(roundId, readResult.getRound().getId());
        assertNull("no analysis row behind a manually keyed result", readResult.getAnalysisId());
        assertNull("scoring has not happened yet", readResult.getScoreReceivedAt());

        EQACycleStateTransition transition = new EQACycleStateTransition();
        transition.setCycle(readCycle);
        transition.setPriorState(EQACycleStatus.PANEL_RECEIVED.name());
        transition.setNewState(EQACycleStatus.TESTING.name());
        transition.setStateMachine(EQAStateMachine.PARTICIPANT);
        transition.setTriggerType(EQATriggerType.MANUAL);
        transition.setTriggerEvent(EQATriggerEvent.MANUAL_OVERRIDE);
        transition.setTriggeredBy(ADMIN_USER_ID);
        transition.setReason("Panel opened early with QA officer approval");
        transition.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        transition.setSysUserId(USER);
        Long transitionId = eqaCycleStateTransitionDAO.insert(transition);

        EQACycleStateTransition readTransition = eqaCycleStateTransitionDAO.get(transitionId)
                .orElseThrow(AssertionError::new);
        assertEquals("PANEL_RECEIVED", readTransition.getPriorState());
        assertEquals("TESTING", readTransition.getNewState());
        assertEquals(EQAStateMachine.PARTICIPANT, readTransition.getStateMachine());
        assertEquals(EQATriggerType.MANUAL, readTransition.getTriggerType());
        assertEquals(EQATriggerEvent.MANUAL_OVERRIDE, readTransition.getTriggerEvent());
        assertEquals(Long.valueOf(ADMIN_USER_ID), readTransition.getTriggeredBy());
        assertEquals("Panel opened early with QA officer approval", readTransition.getReason());
        assertEquals(cycleId, readTransition.getCycle().getId());
    }

    // ---- FR-V2.1-03: existing order/distribution rows gain optional links ----

    @Test
    public void v1DistributionCanBeLinkedToACycleAndRound() {
        EQAProgram scheme = insertScheme("Link check", EQASchemeType.REGIONAL_PT, "AFRO");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");

        jdbc.update("INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                + " distribution_date, deadline, status, created_by, cycle_id, round_id, sys_user_id, last_updated)"
                + " VALUES (9951, gen_random_uuid(), ?, 'Linked distribution', now(), now(), 'DRAFT', ?, ?, ?, ?, now())",
                scheme.getId(), ADMIN_USER_ID, cycle.getId(), roundId, USER);

        Map<String, Object> row = jdbc
                .queryForMap("SELECT cycle_id, round_id FROM clinlims.eqa_distribution WHERE id = 9951");
        assertEquals(cycle.getId().longValue(), ((Number) row.get("cycle_id")).longValue());
        assertEquals(roundId.longValue(), ((Number) row.get("round_id")).longValue());

        jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE id = 9951");
    }

    @Test
    public void eqaOrdersMayStayUncycled() {
        // Gate G2: the cycle link is optional; uncycled orders surface in a
        // bucket rather than being rejected at insert.
        Integer nullableCycleId = jdbc.queryForObject(
                "SELECT CASE WHEN is_nullable = 'YES' THEN 1 ELSE 0 END FROM information_schema.columns"
                        + " WHERE table_schema = 'clinlims' AND table_name = 'sample_eqa' AND column_name = 'cycle_id'",
                Integer.class);
        assertEquals("sample_eqa.cycle_id must stay nullable (gate G2)", Integer.valueOf(1), nullableCycleId);
    }

    // ---- FR-V2.1-01/02/05: uniqueness invariants ----

    @Test
    public void cycleNumberIsUniquePerScheme() {
        EQAProgram scheme = insertScheme("Dup cycle", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        insertCycle(scheme, 7);
        try {
            insertCycle(scheme, 7);
            fail("a scheme cannot have two cycle 7s");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_cycle_scheme_number");
        }
    }

    @Test
    public void roundNumberIsUniquePerCycle() {
        EQAProgram scheme = insertScheme("Dup round", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        insertRound(cycle, 2, "OPEN");
        try {
            insertRound(cycle, 2, "OPEN");
            fail("a cycle cannot have two round 2s");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_round_cycle_number");
        }
    }

    @Test
    public void oneResultPerRoundEnrollmentAndAnalyte() {
        EQAProgram scheme = insertScheme("Dup result", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQARound round = eqaRoundDAO.get(insertRound(cycle, 1, "OPEN")).orElseThrow(AssertionError::new);
        insertResult(cycle, round, ANALYTE_HIV_SEROLOGY, "Reactive");
        try {
            insertResult(cycle, round, ANALYTE_HIV_SEROLOGY, "Non-reactive");
            fail("a lab reports one result per analyte per round");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_participant_result_round_lab_analyte");
        }
    }

    // ---- CHECK constraints from qa/015 + qa/016 ----

    @Test
    public void unknownCycleStatusIsRejectedByTheDatabase() {
        EQAProgram scheme = insertScheme("Bad status", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_cycle (id, fhir_uuid, scheme_id, cycle_number, status,"
                            + " created_at, created_by, sys_user_id, last_updated)"
                            + " VALUES (9961, gen_random_uuid(), ?, 99, 'BOGUS', now(), ?, ?, now())",
                    scheme.getId(), ADMIN_USER_ID, USER);
            fail("the status CHECK constraint should reject an unknown state");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_cycle_status_chk");
        }
    }

    @Test
    public void unknownSubmissionStatusIsRejectedByTheDatabase() {
        EQAProgram scheme = insertScheme("Bad submission", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_participant_result (id, fhir_uuid, cycle_id, round_id,"
                            + " lab_enrollment_id, analyte_id, submission_status, sys_user_id, last_updated)"
                            + " VALUES (9962, gen_random_uuid(), ?, ?, ?, ?, 'PARTIALLY_SUBMITTED', ?, now())",
                    cycle.getId(), roundId, ENROLLMENT_ID, ANALYTE_HIV_SEROLOGY, USER);
            fail("the submission_status CHECK constraint should reject an unknown state");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_participant_result_status_chk");
        }
    }

    // ---- FR-V2.1-06 + BR-004 ----

    @Test
    public void v1SchemesBackfillToInternationalPt() {
        // The scheme_type column carries a DEFAULT, so a legacy insert that
        // predates V2 still lands on a valid arrangement type.
        jdbc.update("INSERT INTO clinlims.eqa_program (id, fhir_uuid, name, is_active, sys_user_id, last_updated)"
                + " VALUES (9971, gen_random_uuid(), 'Legacy scheme', true, ?, now())", USER);
        String schemeType = jdbc.queryForObject("SELECT scheme_type FROM clinlims.eqa_program WHERE id = 9971",
                String.class);
        assertEquals("INTERNATIONAL_PT", schemeType);
    }

    @Test
    public void externalSchemeWithoutProviderIsRejectedOnInsert() {
        try {
            insertScheme("No provider", EQASchemeType.INTERNATIONAL_PT, null);
            fail("BR-004: an international PT scheme needs a provider");
        } catch (Exception expected) {
            assertTrue("message should name the rule, got: " + expected.getMessage(),
                    rootMessage(expected).contains("Provider is required"));
        }
    }

    @Test
    public void blankProviderCountsAsMissing() {
        try {
            insertScheme("Blank provider", EQASchemeType.REGIONAL_PT, "   ");
            fail("BR-004: whitespace is not a provider");
        } catch (Exception expected) {
            assertTrue(rootMessage(expected).contains("Provider is required"));
        }
    }

    @Test
    public void inHouseSchemeMayOmitTheProvider() {
        EQAProgram scheme = insertScheme("In-house HIV VL", EQASchemeType.IN_HOUSE, null);
        EQAProgram read = eqaProgramService.get(scheme.getId());
        assertEquals(EQASchemeType.IN_HOUSE, read.getSchemeType());
        assertNull("an in-house scheme has no external provider", read.getProvider());
    }

    @Test
    public void switchingAnInHouseSchemeToExternalRequiresAProvider() {
        EQAProgram scheme = insertScheme("Promoted scheme", EQASchemeType.IN_HOUSE, null);

        scheme.setSchemeType(EQASchemeType.INTER_LAB_SPLIT);
        try {
            eqaProgramService.update(scheme);
            fail("BR-004 must hold on update, not only on insert");
        } catch (Exception expected) {
            assertTrue(rootMessage(expected).contains("Provider is required"));
        }

        scheme.setProvider("Partner lab network");
        EQAProgram updated = eqaProgramService.update(scheme);
        assertEquals(EQASchemeType.INTER_LAB_SPLIT, updated.getSchemeType());
        assertEquals("Partner lab network", updated.getProvider());
    }

    @Test
    public void reassigningATestRevivesTheExistingAssignment() {
        // removeTestAssignment only clears is_active, but UNIQUE(program, test)
        // still holds the row, so a re-save of an overlapping test list would
        // collide if assignTest inserted blindly.
        EQAProgram scheme = insertScheme("Reassignment", EQASchemeType.INTERNATIONAL_PT, "NHLS");

        EQAProgramTest first = eqaProgramService.assignTest(scheme.getId(), TEST_HIV_SEROLOGY);
        eqaProgramService.removeTestAssignment(first.getId());
        EQAProgramTest second = eqaProgramService.assignTest(scheme.getId(), TEST_HIV_SEROLOGY);

        assertEquals("re-assignment must revive the soft-deleted row", first.getId(), second.getId());
        assertTrue("the revived assignment is active again", second.getIsActive());
        assertEquals("exactly one row may exist per (scheme, test) pair", Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_program_test" + " WHERE eqa_program_id = ? AND test_id = ?",
                        Integer.class, scheme.getId(), TEST_HIV_SEROLOGY));
    }

    // ---- helpers ----

    private void seedLabEnrollment() {
        jdbc.update("INSERT INTO clinlims.eqa_lab_program_enrollment"
                + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                + " VALUES (?, 'Spine test enrollment', 'NHLS', true, now(), ?, now())", ENROLLMENT_ID, USER);
    }

    private EQAProgram insertScheme(String name, EQASchemeType type, String provider) {
        EQAProgram scheme = new EQAProgram();
        scheme.setName(name);
        scheme.setSchemeType(type);
        scheme.setProvider(provider);
        scheme.setSysUserId(USER);
        Long id = eqaProgramService.insert(scheme);
        scheme.setId(id);
        return scheme;
    }

    private Long insertCycle(EQAProgram scheme, int cycleNumber) {
        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(cycleNumber);
        cycle.setCreatedBy(systemUser(ADMIN_USER_ID));
        cycle.setSysUserId(USER);
        return eqaCycleDAO.insert(cycle);
    }

    private EQACycle readBack(Long cycleId) {
        return eqaCycleDAO.get(cycleId).orElseThrow(AssertionError::new);
    }

    private Long insertRound(EQACycle cycle, int roundNumber, String status) {
        EQARound round = new EQARound();
        round.setCycle(cycle);
        round.setRoundNumber(roundNumber);
        round.setStatus(status);
        round.setSysUserId(USER);
        return eqaRoundDAO.insert(round);
    }

    private Long insertResult(EQACycle cycle, EQARound round, long analyteId, String value) {
        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(cycle);
        result.setRound(round);
        result.setLabEnrollmentId(ENROLLMENT_ID);
        result.setAnalyteId(analyteId);
        result.setResultValue(value);
        result.setSysUserId(USER);
        return eqaParticipantResultDAO.insert(result);
    }

    private SystemUser systemUser(long id) {
        return systemUserService.get(String.valueOf(id));
    }

    private void assertConstraintViolation(Exception e, String constraintName) {
        assertTrue("expected " + constraintName + " to be the failing constraint, got: " + rootMessage(e),
                rootMessage(e).contains(constraintName));
    }

    private String rootMessage(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = t; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(' ');
        }
        return messages.toString();
    }
}
