/**
 * Integration test for Mindray BC-5380 HL7 (hematology/CBC).
 *
 * <p>Task Reference: T124 [M5] Mindray BC-5380 HL7 integration.
 */
package org.openelisglobal.analyzer.mindray;

import org.junit.Test;

public class MindrayBC5380IntegrationTest extends AbstractMindrayIntegrationTest {

    @Override
    protected String getAnalyzerIp() {
        return "192.168.1.100";
    }

    @Override
    protected String getFixturePath() {
        return "testdata/hl7/mindray/bc5380-cbc-result.hl7";
    }

    @Test
    public void mindrayBc5380Hl7Message_resultsStored() throws Exception {
        verifyHl7MessageResultsStored();
    }
}
