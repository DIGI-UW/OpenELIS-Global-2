/**
 * Integration test for Mindray BC-2000 HL7 (hematology/CBC).
 *
 * <p>Task Reference: T216 [M14] Mindray BC2000 HL7 integration - P2 analyzer validation.
 * BC2000 shares the same protocol as BC-5380 (HL7 over Network).
 */
package org.openelisglobal.analyzer.mindray;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @Test
    public void mindrayBc2000Hl7Message_parsesCorrectValues() throws Exception {
        // Given: HL7 fixture with specific CBC values
        String raw = loadFixture(getFixturePath());
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));

        // When: parse and insert
        boolean readOk = reader.readStream(in);
        assertTrue("readStream should succeed", readOk);
        boolean insertOk = reader.insertAnalyzerData("1");
        assertTrue("insertAnalyzerData should succeed", insertOk);

        // Then: verify specific values from fixture are stored correctly
        // Fixture has: WBC=6.8, RBC=4.35, HGB=13.0, HCT=39.5, PLT=225 (5 OBX segments)
        Integer totalResults = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class,
                Integer.parseInt(mindrayAnalyzer.getId()));
        assertTrue("Expected 5 results (one per OBX segment)", totalResults != null && totalResults == 5);

        // Verify WBC value (test_name stores OBX-3 CE-2 text: "WHITE BLOOD CELL")
        String wbcValue = jdbcTemplate.queryForObject(
                "SELECT result FROM clinlims.analyzer_results WHERE analyzer_id = ? AND test_name LIKE '%WHITE BLOOD%' LIMIT 1",
                String.class, Integer.parseInt(mindrayAnalyzer.getId()));
        assertNotNull("WBC result should be stored", wbcValue);
        assertTrue("WBC value should be 6.8", wbcValue.contains("6.8"));

        // Verify PLT value (test_name stores OBX-3 CE-2 text: "PLATELET")
        String pltValue = jdbcTemplate.queryForObject(
                "SELECT result FROM clinlims.analyzer_results WHERE analyzer_id = ? AND test_name LIKE '%PLATELET%' LIMIT 1",
                String.class, Integer.parseInt(mindrayAnalyzer.getId()));
        assertNotNull("PLT result should be stored", pltValue);
        assertTrue("PLT value should be 225", pltValue.contains("225"));
    }
}
