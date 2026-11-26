package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * The run header's QC block (OGC-1200 FR-E2): the run's overall verdict, then
 * one line per covered test with its verdict, the controls covering it and the
 * blanks and duplicates recorded for it (FR-C1a).
 */
public record RunQcBlock(String overall, List<TestLine> tests) {

    /**
     * One test the run covers, with what was decided for it when it was held
     * (FR-C5) and the non-conformity that decision filed; the earlier runs this run
     * is confirmed or suggested as the retest of, and the later runs that
     * superseded it (FR-A3).
     *
     * @param holdCause   why the test's rows are held now, null when they are not
     * @param whenMissing what the test's policy does about what it lacks (HOLD when
     *                    any missing kind holds, else WARN), null when it lacks
     *                    nothing (FR-H1)
     * @param missing     the kinds a test whose verdict is No control lacks:
     *                    CONTROL, BLANK, DUPLICATE
     * @param items       the blanks and duplicates recorded for the test
     */
    public record TestLine(String testId, String testName, String verdict, List<ControlLine> controls, String decision,
            String nceNumber, List<RunLink> retestOf, List<RunLink> retestSuggestions, List<RunLink> supersededBy,
            RunHoldCause holdCause, String whenMissing, List<String> missing, List<ItemLine> items) {
    }

    /**
     * One blank or duplicate on the run (FR-C10, FR-C11): what was observed, for a
     * duplicate the sample it repeats, what it came to and why, who recorded it and
     * when.
     */
    public record ItemLine(Long id, String kind, String observed, String parentAccession, String evaluation,
            String detail, String operator, String recordedAt) {
    }

    /**
     * One control on the run: observed against expected, level, lot, verdict, where
     * the verdict came from, who recorded it and when.
     */
    public record ControlLine(String id, String level, String lotNumber, String observed, String expected,
            String verdict, String verdictSource, String operator, String recordedAt) {
    }
}
