package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * The run header's QC block (OGC-1200 FR-E2): the run's overall verdict, then
 * one line per covered test with its verdict and the controls covering it.
 */
public record RunQcBlock(String overall, List<TestLine> tests) {

    /**
     * One test the run covers, with what was decided for it when its control failed
     * (FR-C5) and the non-conformity that decision filed.
     */
    public record TestLine(String testId, String testName, String verdict, List<ControlLine> controls, String decision,
            String nceNumber) {
    }

    /**
     * One control on the run: observed against expected, level, lot, verdict, where
     * the verdict came from, who recorded it and when.
     */
    public record ControlLine(String id, String level, String lotNumber, String observed, String expected,
            String verdict, String verdictSource, String operator, String recordedAt) {
    }
}
