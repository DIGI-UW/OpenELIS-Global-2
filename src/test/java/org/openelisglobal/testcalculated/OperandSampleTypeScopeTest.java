package org.openelisglobal.testcalculated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.testcalculated.valueholder.Operation;

/**
 * Which specimen a calculation operand reads.
 *
 * <p>
 * The builder has always stored the operand's specimen in {@code sampleId} — it
 * is the picker the user chooses the test from. {@code sampleTypeId} was added
 * beside it for the scoping work and is never written by the form, so an
 * executor reading only that saw NULL on every operand, treated each one as
 * unscoped, and fed the calculation with a result from any specimen the test
 * runs on. A calculation configured against Urines would run on a DBS result of
 * the same test.
 */
public class OperandSampleTypeScopeTest {

    /** Mirrors the executor's resolution. */
    private String operandSampleTypeId(Operation operation) {
        if (operation.getSampleTypeId() != null) {
            return operation.getSampleTypeId().toString();
        }
        return operation.getSampleId() == null ? null : operation.getSampleId().toString();
    }

    private Operation operand(Integer sampleId, Integer sampleTypeId) {
        Operation operation = new Operation();
        operation.setType(Operation.OperationType.TEST_RESULT);
        operation.setValue("6");
        operation.setSampleId(sampleId);
        operation.setSampleTypeId(sampleTypeId);
        return operation;
    }

    @Test
    public void readsTheSpecimenTheBuilderStored() {
        // Every operand configured before this fix looks like this: a specimen
        // in sampleId and nothing in sampleTypeId.
        assertEquals("26", operandSampleTypeId(operand(26, null)));
        assertEquals("1", operandSampleTypeId(operand(1, null)));
    }

    @Test
    public void prefersAnExplicitScopeWhenOneIsSet() {
        assertEquals("30", operandSampleTypeId(operand(26, 30)));
    }

    @Test
    public void staysUnscopedOnlyWhenNeitherIsSet() {
        assertNull(operandSampleTypeId(operand(null, null)));
    }

    @Test
    public void anOperandOnUrinesDoesNotResolveToDbs() {
        // The reported failure: same test, same component, different specimen.
        // Resolving to "1" is what lets the scope matcher reject a DBS result.
        Operation urines = operand(1, null);

        assertEquals("1", operandSampleTypeId(urines));
        assertEquals("the operand must not answer with another specimen of the same test", "1",
                operandSampleTypeId(urines));
    }
}
