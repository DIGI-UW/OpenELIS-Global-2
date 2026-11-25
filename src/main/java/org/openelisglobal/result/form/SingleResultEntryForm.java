package org.openelisglobal.result.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * Save payload for the unified Results worklist (OGC-1020, FR-O1): exactly ONE
 * analysis per save. The shape itself enforces per-analysis save scoping — a
 * client cannot re-submit rows it did not edit, which is the root-cause fix for
 * the whole-page-save overwrite incident described in the FRS.
 */
public class SingleResultEntryForm {

    @Valid
    @NotNull
    private TestResultItem testResult;

    /**
     * OGC-1200 FR-E10: set when the value is accepted from an analyzer run, naming
     * the staged run row it came from.
     */
    private String runRowId;

    public String getRunRowId() {
        return runRowId;
    }

    public void setRunRowId(String runRowId) {
        this.runRowId = runRowId;
    }

    public TestResultItem getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResultItem testResult) {
        this.testResult = testResult;
    }
}
