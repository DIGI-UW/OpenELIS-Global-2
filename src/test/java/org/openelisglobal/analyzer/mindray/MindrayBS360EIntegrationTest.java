/**
 * Integration test for Mindray BS-360E HL7 (chemistry).
 *
 * <p>Task Reference: T125 [M5] Mindray BS-360E HL7 integration.
 */
package org.openelisglobal.analyzer.mindray;

import org.junit.Test;

public class MindrayBS360EIntegrationTest extends AbstractMindrayIntegrationTest {

    @Override
    protected String getAnalyzerIp() {
        return "192.168.1.101";
    }

    @Override
    protected String getFixturePath() {
        return "testdata/hl7/mindray/bs360e-chemistry-result.hl7";
    }

    @Test
    public void mindrayBs360eHl7Message_resultsStored() throws Exception {
        verifyHl7MessageResultsStored();
    }
}
