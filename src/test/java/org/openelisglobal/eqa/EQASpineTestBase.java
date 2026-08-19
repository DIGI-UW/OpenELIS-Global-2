package org.openelisglobal.eqa;

import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared fixture for the EQA spine integration tests (T-08/T-09/T-10). Owns the
 * one authoritative clean-up order (children before parents) and the common
 * inserters, so adding a table means editing one list, not one per test class.
 *
 * <p>
 * The test DB is Liquibase-provisioned (BaseTestConfig runs base-changelog.xml
 * against a testcontainers Postgres), so CHECK/FK/unique constraints are live.
 * BaseWebContextSensitiveTest runs with transactions NOT_SUPPORTED, so each DAO
 * call commits on its own — a read-back is a genuinely fresh session.
 */
public abstract class EQASpineTestBase extends BaseWebContextSensitiveTest {

    protected static final String USER = "1";
    protected static final long ADMIN_USER_ID = 1L;

    @Autowired
    protected EQAProgramService eqaProgramService;

    @Autowired
    protected EQACycleDAO eqaCycleDAO;

    @Autowired
    protected EQARoundDAO eqaRoundDAO;

    @Autowired
    protected EQAParticipantResultDAO eqaParticipantResultDAO;

    @Autowired
    protected EQAPanelDAO eqaPanelDAO;

    @Autowired
    protected EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    protected SystemUserService systemUserService;

    @Autowired
    private DataSource dataSource;

    protected JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        ensureSystemUserOne();
        executeDataSetWithStateManagement("testdata/eqa-cycle-spine.xml");
        cleanEqaTables();
    }

    /**
     * {@link #ADMIN_USER_ID} has to resolve to a usable entity, not just to a
     * number: {@code eqa_cycle.created_by} and
     * {@code eqa_analyst_competency_event.analyst_id} both reference
     * {@code system_user}.
     *
     * <p>
     * Other suites truncate {@code system_user}, and the web base restores its
     * admin row from the sequence rather than at id 1, so in a full-suite run the
     * row can be missing entirely. The related null-version trap is repaired in the
     * web base, which every suite shares.
     */
    private void ensureSystemUserOne() {
        jdbc.update(
                "INSERT INTO clinlims.system_user (id, external_id, login_name, last_name, first_name,"
                        + " initials, is_active, is_employee, lastupdated)"
                        + " SELECT ?, 'EQA_TEST_SEED', 'eqa_test_seed', 'Seed', 'EQA', 'ES', 'Y', 'Y', now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_user WHERE id = ?)",
                ADMIN_USER_ID, ADMIN_USER_ID);
    }

    @After
    public void cleanUpEqaTables() {
        cleanEqaTables();
    }

    /**
     * Children first, parents last. Subclasses append their own rows via override.
     */
    protected void cleanEqaTables() {
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        jdbc.update("DELETE FROM clinlims.eqa_participant_followup");
        jdbc.update("DELETE FROM clinlims.eqa_panel_receipt");
        jdbc.update("DELETE FROM clinlims.eqa_panel_sample");
        jdbc.update("DELETE FROM clinlims.eqa_panel");
        jdbc.update("DELETE FROM clinlims.eqa_scheme_analyst");
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_cycle_state_transition");
        jdbc.update("DELETE FROM clinlims.eqa_round");
        jdbc.update("DELETE FROM clinlims.eqa_cycle");
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id BETWEEN 9900 AND 9999");
        jdbc.update("DELETE FROM clinlims.eqa_program_test");
        jdbc.update("DELETE FROM clinlims.eqa_program");
    }

    protected void seedEnrollment(long id, String name) {
        jdbc.update("INSERT INTO clinlims.eqa_lab_program_enrollment"
                + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                + " VALUES (?, ?, 'NHLS', true, now(), ?, now())", id, name, USER);
    }

    protected EQAProgram insertScheme(String name, EQASchemeType type, String provider) {
        EQAProgram scheme = new EQAProgram();
        scheme.setName(name);
        scheme.setSchemeType(type);
        scheme.setProvider(provider);
        scheme.setSysUserId(USER);
        scheme.setId(eqaProgramService.insert(scheme));
        return scheme;
    }

    protected Long insertCycle(EQAProgram scheme, int cycleNumber) {
        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(cycleNumber);
        cycle.setCreatedBy(systemUser(ADMIN_USER_ID));
        cycle.setSysUserId(USER);
        return eqaCycleDAO.insert(cycle);
    }

    protected EQACycle readBack(Long cycleId) {
        return eqaCycleDAO.get(cycleId).orElseThrow(AssertionError::new);
    }

    protected Long insertRound(EQACycle cycle, int roundNumber, String status) {
        EQARound round = new EQARound();
        round.setCycle(cycle);
        round.setRoundNumber(roundNumber);
        round.setStatus(status);
        round.setSysUserId(USER);
        return eqaRoundDAO.insert(round);
    }

    protected Long insertParticipantResult(EQACycle cycle, EQARound round, long enrollmentId, long analyteId,
            EQASubmissionStatus status, String value) {
        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(cycle);
        result.setRound(round);
        result.setLabEnrollmentId(enrollmentId);
        result.setAnalyteId(analyteId);
        if (status != null) {
            result.setSubmissionStatus(status);
        }
        result.setResultValue(value);
        result.setSysUserId(USER);
        return eqaParticipantResultDAO.insert(result);
    }

    protected EQAPanel insertPanel(EQAProgram scheme, Consumer<EQAPanel> customise) {
        EQAPanel panel = new EQAPanel();
        panel.setScheme(scheme);
        panel.setPanelName("Panel");
        panel.setSysUserId(USER);
        customise.accept(panel);
        panel.setId(eqaPanelDAO.insert(panel));
        return panel;
    }

    protected Long insertReceipt(EQACycle cycle, long enrollmentId) {
        EQAPanelReceipt receipt = new EQAPanelReceipt();
        receipt.setCycle(cycle);
        receipt.setLabEnrollmentId(enrollmentId);
        receipt.setReceivedDate(java.sql.Date.valueOf("2026-08-14"));
        receipt.setReceivedBy(ADMIN_USER_ID);
        receipt.setSysUserId(USER);
        return eqaPanelReceiptDAO.insert(receipt);
    }

    protected SystemUser systemUser(long id) {
        return systemUserService.get(String.valueOf(id));
    }

    protected void assertConstraintViolation(Exception e, String constraintName) {
        assertTrue("expected " + constraintName + " to be the failing constraint, got: " + rootMessage(e),
                rootMessage(e).contains(constraintName));
    }

    protected String rootMessage(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = t; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(' ');
        }
        return messages.toString();
    }
}
