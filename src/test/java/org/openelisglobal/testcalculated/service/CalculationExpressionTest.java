package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.testcalculated.valueholder.Operation;

public class CalculationExpressionTest {
    private static Operation op(Operation.OperationType type, String value) {
        Operation operation = new Operation();
        operation.setType(type);
        operation.setValue(value);
        return operation;
    }

    private static Operation n(String value) {
        return op(Operation.OperationType.INTEGER, value);
    }

    private static Operation math(String value) {
        return op(Operation.OperationType.MATH_FUNCTION, value);
    }

    private static Operation result() {
        return op(Operation.OperationType.TEST_RESULT, "17");
    }

    private static String calculate(Operation... operations) {
        return CalculationExpression.compile(List.of(operations))
                .evaluate(o -> new CalculationExpression.Operand(4.5, 2.0, 8.0));
    }

    private static void rejected(Operation... operations) {
        assertThrows(IllegalArgumentException.class, () -> calculate(operations));
    }

    @Test
    public void precedenceParenthesesAndUnaryOperators() {
        assertEquals("14.0", calculate(n("2"), math("+"), n("3"), math("*"), n("4")));
        assertEquals("20.0", calculate(math("("), n("2"), math("+"), n("3"), math(")"), math("*"), n("4")));
        assertEquals("-1.5", calculate(math("-"), n("3"), math("/"), n("2")));
        assertEquals("3.0", calculate(math("+"), n("3")));
        assertEquals("0.5", calculate(n("1"), math("/"), n("2")));
    }

    @Test
    public void decimalsAndExponentsKeepFloatingPointArithmetic() {
        assertEquals("0.30000000000000004", calculate(n("0.1"), math("+"), n("0.2")));
        assertEquals("5.0", calculate(n("1e1"), math("*"), n(".5")));
        assertEquals("-2.0", calculate(n("-5"), math("+"), n("3")));
    }

    @Test
    public void comparisonsAndBooleanPrecedence() {
        assertEquals("true", calculate(n("2"), math("=="), n("2"), math("&&"), n("3"), math("!="), n("4")));
        assertEquals("true", calculate(n("2"), math("<="), n("2"), math("||"), n("3"), math(">="), n("4")));
        assertEquals("true", calculate(n("0"), math("=="), n("-0")));
        rejected(n("1"), math("&&"), n("2"));
    }

    @Test
    public void measuredResultsAndAgeAreNumericOperands() {
        assertEquals("9.0", calculate(result(), math("*"), n("2")));
        Operation age = op(Operation.OperationType.PATIENT_ATTRIBUTE, "AGE");
        assertEquals("40.0", CalculationExpression.compile(List.of(age, math("*"), n("2")))
                .evaluate(o -> new CalculationExpression.Operand(20)));
        rejected(op(Operation.OperationType.PATIENT_ATTRIBUTE, "WEIGHT"));
    }

    @Test
    public void normalRangeAndExistingInclusiveOutsideBoundaryArePreserved() {
        for (double value : new double[] { 2, 4.5, 8 }) {
            assertEquals("true", CalculationExpression.compile(List.of(result(), math(Operation.IN_NORMAL_RANGE)))
                    .evaluate(o -> new CalculationExpression.Operand(value, 2.0, 8.0)));
        }
        assertEquals("true", CalculationExpression.compile(List.of(result(), math(Operation.OUTSIDE_NORMAL_RANGE)))
                .evaluate(o -> new CalculationExpression.Operand(8, 2.0, 8.0)));
        assertEquals("false", calculate(result(), math(Operation.OUTSIDE_NORMAL_RANGE)));
    }

    @Test
    public void absentRangeBoundsStayUnbounded() {
        assertEquals("true", CalculationExpression.compile(List.of(result(), math(Operation.IN_NORMAL_RANGE)))
                .evaluate(o -> new CalculationExpression.Operand(2)));
        assertEquals("false", CalculationExpression.compile(List.of(result(), math(Operation.OUTSIDE_NORMAL_RANGE)))
                .evaluate(o -> new CalculationExpression.Operand(2)));
        assertEquals("true", CalculationExpression.compile(List.of(result(), math(Operation.IN_NORMAL_RANGE)))
                .evaluate(o -> new CalculationExpression.Operand(2, null, 3.0)));
    }

    @Test
    public void invalidSyntaxAndIncompleteRulesAreRejected() {
        rejected();
        rejected(n("1"), n("2"));
        rejected(n("1"), math("+"));
        rejected(math("("), n("1"));
        rejected(math(Operation.IN_NORMAL_RANGE));
        rejected(n("1"), math(Operation.IN_NORMAL_RANGE));
        rejected(new Operation());
    }

    @Test
    public void codeAndUnadvertisedOperatorsCannotEnterTheGrammar() {
        String[] inputs = { "Java.type('java.lang.System').setProperty('oe.calc.test','executed')",
                "T(java.lang.Runtime)", "new java.lang.String()", "#root", "1;2", "1.toString()", "1 + 2", "2**100",
                "true", "'text'" };
        for (String input : inputs) {
            rejected(n(input));
            rejected(math(input));
            rejected(op(Operation.OperationType.TEST_RESULT, input));
        }
        rejected(n("2"), math("^"), n("3"));
        rejected(n("1"), math("="), n("2"));
        assertNull(System.getProperty("oe.calc.test"));
    }

    @Test
    public void noIncrementAssignmentOrPropertySyntaxFromCombinedTokens() {
        rejected(n("1"), math("+"), math("+"));
        rejected(math("("), n("1"), math(")"), math("("), n("2"), math(")"));
    }

    @Test
    public void invalidArithmeticDoesNotBecomeAClinicalValue() {
        rejected(n("1"), math("/"), n("0"));
        rejected(n("0"), math("/"), n("0"));
        rejected(n("1e999"));
        rejected(n("NaN"));
        rejected(n("Infinity"));
        rejected(n("1e308"), math("*"), n("1e308"));
        rejected(n("1"), math("/"), n("0"), math(">="), n("1"));
        assertThrows(IllegalArgumentException.class,
                () -> CalculationExpression.compile(List.of(result())).evaluate(o -> null));
    }

    @Test
    public void validationDoesNotEvaluateFakePatientValues() {
        CalculationExpression expression = CalculationExpression
                .compile(List.of(n("1"), math("/"), math("("), result(), math("-"), n("1"), math(")")));
        assertEquals("0.5", expression.evaluate(o -> new CalculationExpression.Operand(3)));
    }

    @Test
    public void ruleSizeAndNestingAreBounded() {
        List<Operation> huge = new ArrayList<>();
        for (int i = 0; i < 257; i++)
            huge.add(n("1"));
        assertThrows(IllegalArgumentException.class, () -> CalculationExpression.compile(huge));
        List<Operation> deep = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            deep.add(n("1"));
            deep.add(math("+"));
        }
        deep.add(n("1"));
        assertThrows(IllegalArgumentException.class, () -> CalculationExpression.compile(deep));
    }

    @Test
    public void existingRangeExpansionPrecedenceDoesNotChange() {
        // The legacy expansion is false && value <= min || value >= max.
        assertEquals("true", CalculationExpression
                .compile(
                        List.of(n("1"), math("=="), n("2"), math("&&"), result(), math(Operation.OUTSIDE_NORMAL_RANGE)))
                .evaluate(o -> new CalculationExpression.Operand(10, 2.0, 8.0)));
    }
}
