package org.openelisglobal.accreditation.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * OGC-686: one accrediting body's EQA cover under ISO 15189:2022 §7.3.7.3, how
 * many accredited tests are in a live EQA scheme and which are not.
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
