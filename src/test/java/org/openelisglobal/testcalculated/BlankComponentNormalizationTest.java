package org.openelisglobal.testcalculated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;

/**
 * A rule builder posts "" for a component picker it has not resolved yet.
 *
 * <p>
 * Postgres reads "" as a key that does not exist rather than as no key at all,
 * so the component foreign key rejects the insert and saving the rule fails
 * outright:
 *
 * <pre>
 * violates foreign key constraint "fk_test_reflex_component"
 *   Detail: Key (component_id)=() is not present in table "test_result_component"
 * </pre>
 *
 * <p>
 * "No component named" has to reach the column as NULL, which is also what the
 * execution side reads as unscoped.
 */
public class BlankComponentNormalizationTest {

    /** Mirrors the controller's normalisation. */
    private void normalize(Calculation calculation) {
        if (calculation.getComponentId() != null && calculation.getComponentId().isBlank()) {
            calculation.setComponentId(null);
        }
        if (calculation.getOperations() != null) {
            calculation.getOperations().forEach(operation -> {
                if (operation.getComponentId() != null && operation.getComponentId().isBlank()) {
                    operation.setComponentId(null);
                }
            });
        }
    }

    private int order = 0;

    private Operation operand(String componentId) {
        Operation operation = new Operation();
        operation.setType(Operation.OperationType.TEST_RESULT);
        operation.setValue("300");
        operation.setComponentId(componentId);
        // getOperations() sorts by order, so an operand needs one.
        operation.setOrder(order++);
        return operation;
    }

    @Test
    public void blankDestinationBecomesNull() {
        Calculation calculation = new Calculation();
        calculation.setOperations(new java.util.ArrayList<>());
        calculation.setComponentId("");

        normalize(calculation);

        assertNull("an empty destination must not reach the foreign key", calculation.getComponentId());
    }

    @Test
    public void blankOperandComponentBecomesNull() {
        Calculation calculation = new Calculation();
        calculation.setOperations(new java.util.ArrayList<>(Arrays.asList(operand(""), operand("   "))));

        normalize(calculation);

        calculation.getOperations().forEach(o -> assertNull(o.getComponentId()));
    }

    @Test
    public void aNamedComponentIsLeftAlone() {
        Calculation calculation = new Calculation();
        calculation.setComponentId("c-ct");
        calculation.setOperations(new java.util.ArrayList<>(Arrays.asList(operand("c-vl"))));

        normalize(calculation);

        assertEquals("c-ct", calculation.getComponentId());
        assertEquals("c-vl", calculation.getOperations().get(0).getComponentId());
    }

    @Test
    public void anAlreadyNullComponentSurvives() {
        Calculation calculation = new Calculation();
        calculation.setComponentId(null);
        calculation.setOperations(new java.util.ArrayList<>(Arrays.asList(operand(null))));

        normalize(calculation);

        assertNull(calculation.getComponentId());
        assertNull(calculation.getOperations().get(0).getComponentId());
    }
}
