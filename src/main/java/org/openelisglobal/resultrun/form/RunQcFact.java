package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * What a run says about the quality control of one analysis's test, for the
 * Validation row (OGC-1200 FR-G3): the run the result came from, the run's
 * verdict for the test, what was decided when its control failed, and each
 * control covering the test. The verdict reaches the row's quality-control slot
 * only as Pass or Fail; No control and Not required are no fact (D-056), unless
 * the results were accepted despite the missing control.
 */
public record RunQcFact(Long runId, String runLabel, String verdict, String decision, String reason, String nceNumber,
        List<RunQcBlock.ControlLine> controls) {

    /**
     * A failed control, or results accepted despite a hold of any cause (a missing
     * required control included): both are a known QC problem the validator sees.
     */
    public boolean failed() {
        return RunTestVerdict.FAIL.name().equals(verdict) || "ACCEPT_DESPITE".equals(decision);
    }

    public boolean passed() {
        return RunTestVerdict.PASS.name().equals(verdict);
    }
}
