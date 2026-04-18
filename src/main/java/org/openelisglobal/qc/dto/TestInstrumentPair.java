package org.openelisglobal.qc.dto;

import java.util.Objects;

/**
 * Typed pair representing a (testId, instrumentId) combination. Used by DAO
 * methods that return distinct test-instrument groupings.
 */
public class TestInstrumentPair {

    private final Integer testId;
    private final Integer instrumentId;

    public TestInstrumentPair(Integer testId, Integer instrumentId) {
        this.testId = testId;
        this.instrumentId = instrumentId;
    }

    public Integer getTestId() {
        return testId;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestInstrumentPair that = (TestInstrumentPair) o;
        return Objects.equals(testId, that.testId) && Objects.equals(instrumentId, that.instrumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId, instrumentId);
    }
}
