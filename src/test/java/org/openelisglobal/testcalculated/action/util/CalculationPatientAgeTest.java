package org.openelisglobal.testcalculated.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;

/**
 * A calculation that reads the patient's age is skipped for a patient with no
 * birth date instead of failing the result save on a null birth date.
 */
public class CalculationPatientAgeTest {

    private static Operation operation(Operation.OperationType type, String value) {
        Operation operation = new Operation();
        operation.setType(type);
        operation.setValue(value);
        return operation;
    }

    private static Calculation calculation(Operation... operations) {
        for (int i = 0; i < operations.length; i++) {
            operations[i].setOrder(i);
        }
        Calculation calculation = new Calculation();
        calculation.setOperations(Arrays.asList(operations));
        return calculation;
    }

    private static Patient patient(boolean withBirthDate) {
        Patient patient = new Patient();
        if (withBirthDate) {
            patient.setBirthDate(Timestamp.valueOf("1990-01-01 00:00:00"));
        }
        return patient;
    }

    private final Calculation usesAge = calculation(
            operation(Operation.OperationType.PATIENT_ATTRIBUTE, Operation.PatientAttribute.AGE.toString()),
            operation(Operation.OperationType.MATH_FUNCTION, "*"), operation(Operation.OperationType.INTEGER, "2"));

    private final Calculation noAge = calculation(operation(Operation.OperationType.INTEGER, "2"),
            operation(Operation.OperationType.MATH_FUNCTION, "*"), operation(Operation.OperationType.INTEGER, "3"));

    @Test
    public void ageCalculationIsSkippedWithoutABirthDate() {
        assertTrue(TestCalculatedUtil.usesAgeWithoutBirthDate(usesAge, patient(false)));
        assertTrue(TestCalculatedUtil.usesAgeWithoutBirthDate(usesAge, null));
    }

    @Test
    public void ageCalculationRunsWithABirthDate() {
        assertFalse(TestCalculatedUtil.usesAgeWithoutBirthDate(usesAge, patient(true)));
    }

    @Test
    public void calculationWithoutAgeRunsWithoutABirthDate() {
        assertFalse(TestCalculatedUtil.usesAgeWithoutBirthDate(noAge, patient(false)));
    }
}
