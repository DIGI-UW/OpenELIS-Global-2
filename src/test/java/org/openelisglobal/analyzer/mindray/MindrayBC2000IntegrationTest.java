/**
 * Integration test for Mindray BC-2000 HL7 (hematology/CBC).
 *
 * <p>Task Reference: T216 [M14] Mindray BC2000 HL7 integration - P2 analyzer validation.
 * BC2000 shares the same protocol as BC-5380 (HL7 over Network).
 */
package org.openelisglobal.analyzer.mindray;

import org.junit.Test;

public class MindrayBC2000IntegrationTest extends AbstractMindrayIntegrationTest {

    @Override
    protected String getAnalyzerIp() {
        return "192.168.1.102"; // BC2000 IP distinct from BC-5380
    }

    @Override
    protected String getFixturePath() {
        // BC2000 uses same HL7 format as BC-5380
        return "testdata/hl7/mindray/bc2000-cbc-result.hl7";
    }

    @Test
    public void mindrayBc2000Hl7Message_resultsStored() throws Exception {
        verifyHl7MessageResultsStored();
    }
}
