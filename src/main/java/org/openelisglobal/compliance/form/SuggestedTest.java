package org.openelisglobal.compliance.form;

import java.math.BigDecimal;
import java.util.List;

/**
 * OGC-768: a test the selected compliance standards ask for, once however many
 * standards cover it.
 *
 * @param coveredBy     the selected standards that cover it, in selection order
 * @param thresholds    each covering standard's active thresholds for it
 * @param conflicting   whether the covering standards set different limits
 * @param sampleTypeIds the sample types the test runs on
 */
public record SuggestedTest(String testId, String testName, String loinc, List<String> sampleTypeIds,
        List<Coverage> coveredBy, List<Threshold> thresholds, boolean conflicting) {

    public record Coverage(String standardId, String regulationNumber, String name) {
    }

    public record Threshold(String standardId, String regulationNumber, String type, BigDecimal min, BigDecimal max,
            BigDecimal target, String units) {
    }
}
