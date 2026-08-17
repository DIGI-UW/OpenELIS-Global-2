package org.openelisglobal.accreditation.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * OGC-686 — one accrediting body's EQA cover, the ISO 15189 §7.7 question
 * answered as a row: of the tests in this body's accredited scope, how many are
 * in a live EQA scheme, and which are not.
 *
 * <p>
 * Derived on read from data the lab already maintains in two places (test
 * accreditation and EQA lab enrollment). No schema, nothing to keep in sync,
 * and no way for the answer to go stale.
 */
public class EqaCoverageView {

    public Long accreditingBodyId;
    public String bodyCode;
    public String bodyName;

    /** The body's derived status chip — an expired body's gaps read differently. */
    public String status;

    public int enrolledTestCount;

    public int coveredTestCount;

    /** The tests in scope with no active EQA enrollment. */
    public List<GapTest> gaps = new ArrayList<>();

    public static class GapTest {
        public String testId;
        public String testName;

        public GapTest(String testId, String testName) {
            this.testId = testId;
            this.testName = testName;
        }
    }
}
