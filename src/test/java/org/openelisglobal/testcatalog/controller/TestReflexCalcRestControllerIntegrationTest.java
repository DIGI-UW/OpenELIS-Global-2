package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestReflexCalcRestController;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService.ReflexCalcView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-764 — read-only Reflex &amp; Calc endpoint. A test with no
 * reflex rules or calculations returns empty cross-link lists (not an error),
 * and an unknown test 404s.
 */
public class TestReflexCalcRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95431L;
    private static final long DICT_ID = 954311L;
    private static final String DICT_ENTRY = "ReflexCalcIT Detected";
    private static final String COMPONENT_ID = "reflexcalcit-component";
    private static final String COMPONENT_LABEL = "ReflexCalcIT Ct";
    private static final long TEST_RESULT_ID = 954312L;
    private static final long TEST_REFLEX_ID = 954313L;
    private static final int REFLEX_RULE_ID = 954314;
    private static final int REFLEX_ACTION_ID = 954315;
    private static final int CALCULATION_ID = 954316;
    private static final int OPERATION_ID = 954317;
    private static final long ANALYTE_ID = 954318L;
    private static final long TEST_ANALYTE_ID = 954319L;
    /** Any specimen the instance already has; its name is what must appear. */
    private String specimenId;
    private String specimenName;

    @Autowired
    private ReflexCalcViewService reflexCalcViewService;

    @Autowired
    private TestService testService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestReflexCalcRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestReflexCalcRestController(reflexCalcViewService, testService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ReflexCalcIT", "ReflexCalcIT desc", UUID.randomUUID().toString());

        Map<String, Object> specimen = jdbc
                .queryForList("SELECT id, description FROM clinlims.type_of_sample ORDER BY id LIMIT 1").get(0);
        specimenId = String.valueOf(specimen.get("id"));
        specimenName = String.valueOf(specimen.get("description"));

        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated)"
                + " VALUES (?, ?, 'Y', NOW())", DICT_ID, DICT_ENTRY);
        jdbc.update(
                "INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order,"
                        + " result_type, allow_multiple_readings, is_active, is_primary, show_on_report, lastupdated)"
                        + " VALUES (?, ?, 'ITCODE', ?, 0, 'D', false, 'Y', true, true, NOW())",
                COMPONENT_ID, TEST_ID, COMPONENT_LABEL);
        jdbc.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_active,"
                        + " component_id, lastupdated) VALUES (?, ?, 'D', ?, 1, true, ?, NOW())",
                TEST_RESULT_ID, TEST_ID, String.valueOf(DICT_ID), COMPONENT_ID);
    }

    /**
     * A rule whose name and whose internal note differ, so the two cannot be
     * confused.
     */
    private void seedReflexRule(String ruleName, String internalNote) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', NOW())",
                ANALYTE_ID, ruleName);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, result_group, testalyt_type,"
                + " lastupdated) VALUES (?, ?, ?, '30', 'R', NOW())", TEST_ANALYTE_ID, TEST_ID, ANALYTE_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_reflex (id, test_id, tst_rslt_id, add_test_id, relation,"
                        + " component_id, add_sample_type_id, internal_note, test_analyte_id, lastupdated)"
                        + " VALUES (?, ?, ?, ?, 'EQUALS', ?, CAST(? AS NUMERIC), ?, ?, NOW())",
                TEST_REFLEX_ID, TEST_ID, TEST_RESULT_ID, TEST_ID, COMPONENT_ID, specimenId, internalNote,
                TEST_ANALYTE_ID);
        jdbc.update("INSERT INTO clinlims.reflex_rule (id, rule_name, overall, active) VALUES (?, ?, 'ANY', true)",
                REFLEX_RULE_ID, ruleName);
        jdbc.update(
                "INSERT INTO clinlims.reflex_rule_action (id, reflex_test_name, reflex_test_id, sample_id,"
                        + " internal_note, reflex_rule_id, test_reflex_id) VALUES (?, '', ?, ?, ?, ?, ?)",
                REFLEX_ACTION_ID, String.valueOf(TEST_ID), specimenId, internalNote, REFLEX_RULE_ID,
                (int) TEST_REFLEX_ID);
    }

    /** A calculation reading this test's component and writing back to it. */
    private void seedCalculation() {
        jdbc.update("INSERT INTO clinlims.calculation (id, name, sample_id, test_id, result, toggled, active,"
                + " component_id, last_updated) VALUES (?, 'ReflexCalcIT Calc', CAST(? AS INTEGER), ?, '', true, true,"
                + " ?, NOW())", CALCULATION_ID, specimenId, (int) TEST_ID, COMPONENT_ID);
        jdbc.update(
                "INSERT INTO clinlims.calculation_operation (id, type, sample_id, operation_order, value,"
                        + " calculation_id, component_id) VALUES (?, 'TEST_RESULT', CAST(? AS INTEGER), 0, ?, ?, ?)",
                OPERATION_ID, specimenId, String.valueOf(TEST_ID), CALCULATION_ID, COMPONENT_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE id = ?", OPERATION_ID);
        jdbc.update("DELETE FROM clinlims.calculation WHERE id = ?", CALCULATION_ID);
        jdbc.update("DELETE FROM clinlims.reflex_rule_action WHERE id = ?", REFLEX_ACTION_ID);
        jdbc.update("DELETE FROM clinlims.reflex_rule WHERE id = ?", REFLEX_RULE_ID);
        jdbc.update("DELETE FROM clinlims.test_reflex WHERE id = ?", TEST_REFLEX_ID);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_ID);
        jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", ANALYTE_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE id = ?", TEST_RESULT_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = ?", COMPONENT_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", DICT_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void get_noCrossLinks_returnsEmptyLists() {
        ReflexCalcView view = controller.get(String.valueOf(TEST_ID));
        assertNotNull(view);
        assertNotNull(view.reflexRules);
        assertNotNull(view.calculatedBy);
        assertNotNull(view.feedsInto);
        assertTrue(view.reflexRules.isEmpty());
    }

    /**
     * The summary names the rule the way its author named it.
     *
     * <p>
     * A reflex row carries notes meant for the report, and the summary printed the
     * internal one where the name belongs — so a rule configured as "Covid-Reflex"
     * with a note of "XXXX" read as "XXXX". The name is on the Reflex Rules record
     * the action points back at, which the summary already resolves in order to
     * link the row.
     */
    @Test
    public void reflexRow_isNamedByItsRule_notByItsInternalNote() {
        seedReflexRule("Covid-Reflex", "XXXX");

        ReflexCalcView view = controller.get(String.valueOf(TEST_ID));

        assertEquals(1, view.reflexRules.size());
        assertEquals("Covid-Reflex", view.reflexRules.get(0).ruleName);
    }

    /**
     * A coded trigger is stored as a dictionary id, and the summary printed the id
     * — "EQUALS 1578" — to a reader with no way to know what it means. The rule
     * builder's own option list resolves it the same way.
     */
    @Test
    public void reflexTrigger_readsACodedValueByName() {
        seedReflexRule("Covid-Reflex", "XXXX");

        String trigger = controller.get(String.valueOf(TEST_ID)).reflexRules.get(0).triggerCondition;

        assertTrue("the dictionary entry is named: " + trigger, trigger.contains(DICT_ENTRY));
        assertFalse("and its id is not shown: " + trigger, trigger.contains(String.valueOf(DICT_ID)));
    }

    /** The measurement the trigger reads, not just the test it belongs to. */
    @Test
    public void reflexTrigger_namesTheComponentItReads() {
        seedReflexRule("Covid-Reflex", "XXXX");

        String trigger = controller.get(String.valueOf(TEST_ID)).reflexRules.get(0).triggerCondition;

        assertTrue("names the component: " + trigger, trigger.contains(COMPONENT_LABEL));
    }

    /** The generated test on the specimen the rule reports it on. */
    @Test
    public void reflexRow_namesTheSpecimenItReportsOn() {
        seedReflexRule("Covid-Reflex", "XXXX");

        String generated = controller.get(String.valueOf(TEST_ID)).reflexRules.get(0).reflexTests;

        assertTrue("names the target specimen: " + generated, generated.contains(specimenName));
    }

    /**
     * A calculation parameter is stored as a test id, and the formula printed it —
     * "300 - 3" reads as arithmetic on the number 300. It names a measurement:
     * test, specimen and component, the three the builder makes its author choose.
     */
    @Test
    public void calculationFormula_namesTheMeasurementItReads_notATestId() {
        seedCalculation();

        String formula = controller.get(String.valueOf(TEST_ID)).calculatedBy.get(0).formula;

        assertTrue("names the test: " + formula, formula.contains("ReflexCalcIT"));
        assertTrue("names the specimen: " + formula, formula.contains(specimenName));
        assertTrue("names the component: " + formula, formula.contains(COMPONENT_LABEL));
        assertFalse("and does not read as arithmetic on a test id: " + formula,
                formula.startsWith(String.valueOf(TEST_ID)));
    }

    /** Where the calculation writes, to the same three axes. */
    @Test
    public void calculationOutput_carriesTestSpecimenAndComponent() {
        seedCalculation();

        String output = controller.get(String.valueOf(TEST_ID)).calculatedBy.get(0).outputTest;

        assertTrue("names the test: " + output, output.contains("ReflexCalcIT"));
        assertTrue("names the specimen: " + output, output.contains(specimenName));
        assertTrue("names the component: " + output, output.contains(COMPONENT_LABEL));
    }

    @Test
    public void get_unknownTest_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.get("99999999"));
        assertEquals(404, ex.getStatusCode().value());
    }
}
