package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1587: a calculation that reads its own result, two that read each other,
 * or two active ones writing the same result each made result entry overwrite a
 * value the technician typed. They are refused when saved or switched on, with
 * the rule or test that is in the way named.
 */
public class CalculationRuleValidationIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long UREA = 95481L;
    private static final long ALT = 95482L;
    private static final long AST = 95483L;
    private static final String SERUM = "95484";
    private static final String PLASMA = "95485";

    @Autowired
    private TestCalculationService calculationService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String ureaPrimary;
    private String ureaSecond;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        for (long test : new long[] { UREA, ALT, AST }) {
            long name = jdbc.queryForObject("SELECT nextval('clinlims.localization_seq')", Long.class);
            jdbc.update(
                    "INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'RuleCheck', NOW())",
                    name);
            jdbc.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                            + " VALUES (nextval('clinlims.localization_value_seq'), ?, 'en', ?, NOW())",
                    name, "RuleCheck" + test);
            jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated,"
                    + " name_localization_id, reporting_name_localization_id) VALUES (?, ?, ?, 'Y', ?, NOW(), ?, ?)",
                    test, "RuleCheck" + test, "rule check " + test, UUID.randomUUID().toString(), name, name);
        }
        ureaPrimary = UUID.randomUUID().toString();
        ureaSecond = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, result_type, is_primary,"
                + " display_order) VALUES (?, ?, 'P', 'Primary', 'N', true, 0), (?, ?, 'S', 'Second', 'N', false, 1)",
                ureaPrimary, UREA, ureaSecond, UREA);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'RuleCheck%')");
        jdbc.update("DELETE FROM clinlims.calculation WHERE name LIKE 'RuleCheck%'");
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (?, ?, ?)", UREA, ALT, AST);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?, ?)", UREA, ALT, AST);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id IN (SELECT id FROM"
                + " clinlims.localization WHERE description = 'RuleCheck')");
        jdbc.update("DELETE FROM clinlims.localization WHERE description = 'RuleCheck'");
    }

    private Calculation rule(String name, long writes, String writesComponent, String writesSpecimen, long reads,
            String readsComponent, String readsSpecimen) {
        Calculation calculation = new Calculation();
        calculation.setName("RuleCheck " + name);
        calculation.setTestId((int) writes);
        calculation.setComponentId(writesComponent);
        calculation.setSampleId(writesSpecimen == null ? null : Integer.valueOf(writesSpecimen));
        calculation.setActive(true);
        calculation.setToggled(true);
        List<Operation> operations = new ArrayList<>();
        Operation operand = new Operation();
        operand.setOrder(0);
        operand.setType(Operation.OperationType.TEST_RESULT);
        operand.setValue(String.valueOf(reads));
        operand.setComponentId(readsComponent);
        operand.setSampleId(readsSpecimen == null ? null : Integer.valueOf(readsSpecimen));
        operations.add(operand);
        Operation times = new Operation();
        times.setOrder(1);
        times.setType(Operation.OperationType.MATH_FUNCTION);
        times.setValue("*");
        operations.add(times);
        Operation two = new Operation();
        two.setOrder(2);
        two.setType(Operation.OperationType.INTEGER);
        two.setValue("2");
        operations.add(two);
        calculation.setOperations(operations);
        return calculation;
    }

    private Calculation saved(Calculation calculation) {
        return calculationService.save(calculation);
    }

    private String refusal(Calculation calculation, boolean willBeActive) {
        return assertThrows(CalculationRuleViolation.class,
                () -> calculationService.validateRule(calculation, willBeActive)).getMessageKey();
    }

    @Test
    public void aRuleReadingItsOwnResultIsRefusedActiveOrNot() {
        Calculation selfReading = rule("urea x2", UREA, null, SERUM, UREA, null, SERUM);

        assertEquals(TestCalculationServiceImpl.SELF_REFERENCE, refusal(selfReading, true));
        assertEquals("a rule that can never run safely is refused even switched off",
                TestCalculationServiceImpl.SELF_REFERENCE, refusal(selfReading, false));
    }

    /**
     * An operation that is not a bounded arithmetic or range step is refused
     * whether or not the rule is switched on.
     */
    @Test
    public void scriptTextInAnOperationIsRefusedActiveOrNot() {
        Calculation scripted = rule("scripted", AST, null, SERUM, ALT, null, null);
        scripted.getOperations().get(1).setValue("java.lang.System.exit(0)");

        assertEquals(TestCalculationServiceImpl.INVALID_LOGIC, refusal(scripted, true));
        assertEquals(TestCalculationServiceImpl.INVALID_LOGIC, refusal(scripted, false));
    }

    /** No component on one side means the primary, so the two spellings match. */
    @Test
    public void namingThePrimaryOnOneSideOnlyIsStillSelfReference() {
        assertEquals(TestCalculationServiceImpl.SELF_REFERENCE,
                refusal(rule("urea primary", UREA, ureaPrimary, SERUM, UREA, null, null), true));
    }

    /**
     * Writing one component from another component of the same test is a real
     * configuration.
     */
    @Test
    public void anotherComponentOfTheSameTestIsNotSelfReference() {
        calculationService.validateRule(rule("urea second", UREA, ureaSecond, SERUM, UREA, ureaPrimary, null), true);
    }

    /** The same test on another specimen is another measurement. */
    @Test
    public void theSameTestOnAnotherSpecimenIsNotSelfReference() {
        calculationService.validateRule(rule("urea plasma", UREA, null, PLASMA, UREA, null, SERUM), true);
    }

    @Test
    public void twoRulesReadingEachOtherAreRefusedAndTheLoopIsNamed() {
        saved(rule("alt from ast", ALT, null, SERUM, AST, null, null));
        Calculation closing = rule("ast from alt", AST, null, SERUM, ALT, null, null);

        CalculationRuleViolation violation = assertThrows(CalculationRuleViolation.class,
                () -> calculationService.validateRule(closing, true));

        assertEquals(TestCalculationServiceImpl.CYCLE, violation.getMessageKey());
        assertEquals("RuleCheck ast from alt → RuleCheck alt from ast → RuleCheck ast from alt",
                violation.getMessageArgs().get("cycle"));
    }

    @Test
    public void aLoopThroughThreeRulesIsRefused() {
        saved(rule("alt from ast", ALT, null, SERUM, AST, null, null));
        saved(rule("urea from alt", UREA, null, SERUM, ALT, null, null));

        assertEquals(TestCalculationServiceImpl.CYCLE,
                refusal(rule("ast from urea", AST, null, SERUM, UREA, null, null), true));
    }

    /** A rule that is switched off runs for nobody, so it closes no loop. */
    @Test
    public void aSwitchedOffRuleClosesNoLoop() {
        Calculation off = rule("alt from ast", ALT, null, SERUM, AST, null, null);
        off.setActive(false);
        saved(off);

        calculationService.validateRule(rule("ast from alt", AST, null, SERUM, ALT, null, null), true);
    }

    /** Both rules of a loop may exist as long as one of them stays switched off. */
    @Test
    public void theClosingRuleMaySaveSwitchedOff() {
        saved(rule("alt from ast", ALT, null, SERUM, AST, null, null));

        calculationService.validateRule(rule("ast from alt", AST, null, SERUM, ALT, null, null), false);
    }

    @Test
    public void aSecondActiveRuleForTheSameResultIsRefused() {
        saved(rule("urea from alt", UREA, null, SERUM, ALT, null, null));

        CalculationRuleViolation violation = assertThrows(CalculationRuleViolation.class,
                () -> calculationService.validateRule(rule("urea from ast", UREA, null, SERUM, AST, null, null), true));

        assertEquals(TestCalculationServiceImpl.DUPLICATE_DESTINATION, violation.getMessageKey());
        assertEquals("RuleCheck urea from alt", violation.getMessageArgs().get("other"));
        calculationService.validateRule(rule("urea from ast off", UREA, null, SERUM, AST, null, null), false);
    }

    /** Re-saving a rule is not a conflict with itself. */
    @Test
    public void aRuleIsNotItsOwnDuplicate() {
        Calculation existing = saved(rule("urea from alt", UREA, null, SERUM, ALT, null, null));

        calculationService.validateRule(existing, true);
    }

    @Test
    public void aNameInUseIsRefusedAndSaysWhetherThatRuleIsOff() {
        Calculation off = rule("taken", UREA, null, SERUM, ALT, null, null);
        off.setActive(false);
        saved(off);

        assertEquals(TestCalculationServiceImpl.DUPLICATE_NAME_INACTIVE,
                refusal(rule("taken", AST, null, SERUM, ALT, null, null), true));

        jdbc.update("UPDATE clinlims.calculation SET active = true WHERE name = 'RuleCheck taken'");
        assertEquals(TestCalculationServiceImpl.DUPLICATE_NAME,
                refusal(rule("taken", AST, null, SERUM, ALT, null, null), true));
    }
}
