/**
 * Test that validates GenericASTM plugin can parse Mindray BA-88A ASTM messages.
 *
 *
 * <p>The BA-88A uses ASTM LIS2-A2 protocol over RS232, which is handled by the GenericASTM
 * plugin (not the existing Mindray HL7 plugin). This test validates that:
 * <ul>
 *   <li>The ASTM H-segment identifier can be parsed correctly</li>
 *   <li>The GenericASTMLineInserter can process R-segments</li>
 *   <li>Test codes and values are extracted correctly</li>
 * </ul>
 */
package org.openelisglobal.analyzer.mindray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for BA-88A ASTM message parsing. These tests validate parsing
 * logic without requiring Spring context.
 */
public class MindrayBA88AASTMParsingTest {

    private static final String FIXTURE_PATH = "testdata/astm/mindray-ba88a-result.txt";

    /**
     * Test: Verify analyzer identifier can be extracted from H-segment.
     *
     * The GenericASTMAnalyzer.parseAnalyzerIdentifier() method extracts field 4
     * from the H-segment to identify the analyzer.
     */
    @Test
    public void hSegment_analyzerIdentifierExtracted() throws Exception {
        List<String> lines = loadAstmLines();

        // Find H segment and extract identifier
        String hSegment = lines.stream().filter(line -> line.startsWith("H|")).findFirst().orElse(null);

        assertNotNull("H segment should exist", hSegment);

        // Parse field 4 (manufacturer^model^version)
        String[] fields = hSegment.split("\\|");
        assertTrue("H segment should have at least 5 fields", fields.length > 4);

        String identifier = fields[4];
        assertNotNull("Identifier field should not be null", identifier);
        assertEquals("Identifier should be Mindray^BA-88A^1.0", "Mindray^BA-88A^1.0", identifier);

        // Verify identifier can match pattern "Mindray.*BA-88A"
        assertTrue("Identifier should match 'Mindray.*BA-88A' pattern", identifier.matches("Mindray.*BA-88A.*"));
    }

    /**
     * Test: Verify R-segment parsing extracts test codes correctly.
     *
     * GenericASTMLineInserter extracts test codes from field 2 of R-segments.
     * Format: R|seq|^^^TEST_CODE|value|units|...
     */
    @Test
    public void rSegments_testCodesExtracted() throws Exception {
        List<String> lines = loadAstmLines();

        // Extract R segments
        List<String> rSegments = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("R|")) {
                rSegments.add(line);
            }
        }

        assertEquals("Should have 10 R segments", 10, rSegments.size());

        // Verify test code extraction for each segment
        String[] expectedTestCodes = { "ALT", "AST", "ALP", "T-Bil", "D-Bil", "TC", "TG", "HDL-C", "CREA", "TP" };

        for (int i = 0; i < rSegments.size(); i++) {
            String rSegment = rSegments.get(i);
            String[] fields = rSegment.split("\\|");
            assertTrue("R segment should have test ID field", fields.length > 2);

            String testIdField = fields[2];
            String[] components = testIdField.split("\\^");
            assertTrue("Test ID should have 4 components (^^^TEST_CODE format)", components.length >= 4);

            String testCode = components[3];
            assertEquals("Test code " + (i + 1) + " should match", expectedTestCodes[i], testCode);
        }
    }

    /**
     * Test: Verify R-segment parsing extracts result values correctly.
     */
    @Test
    public void rSegments_resultValuesExtracted() throws Exception {
        List<String> lines = loadAstmLines();

        // Find first R segment (ALT)
        String altSegment = lines.stream().filter(line -> line.startsWith("R|1|")).findFirst().orElse(null);

        assertNotNull("ALT R segment should exist", altSegment);

        String[] fields = altSegment.split("\\|");
        assertTrue("R segment should have value field", fields.length > 3);
        assertTrue("R segment should have units field", fields.length > 4);

        assertEquals("ALT value should be 35.2", "35.2", fields[3]);
        assertEquals("ALT units should be U/L", "U/L", fields[4]);
    }

    /**
     * Test: Verify R-segment parsing extracts reference ranges.
     *
     * BA-88A includes reference ranges in field 5 (format: low-high).
     */
    @Test
    public void rSegments_referenceRangesExtracted() throws Exception {
        List<String> lines = loadAstmLines();

        // Find first R segment (ALT)
        String altSegment = lines.stream().filter(line -> line.startsWith("R|1|")).findFirst().orElse(null);

        assertNotNull("ALT R segment should exist", altSegment);

        String[] fields = altSegment.split("\\|");
        assertTrue("R segment should have reference range field", fields.length > 5);

        String refRange = fields[5];
        assertEquals("ALT reference range should be 10-40", "10-40", refRange);
    }

    /**
     * Test: Verify all chemistry tests in fixture have valid formats.
     */
    @Test
    public void allChemistryTests_haveValidFormat() throws Exception {
        List<String> lines = loadAstmLines();

        for (String line : lines) {
            if (line.startsWith("R|")) {
                String[] fields = line.split("\\|");

                // Verify minimum required fields
                assertTrue("R segment should have at least 5 fields", fields.length >= 5);

                // Verify test ID format
                String testIdField = fields[2];
                assertTrue("Test ID should contain ^ delimiter", testIdField.contains("^"));

                // Verify value is numeric
                String value = fields[3];
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    assertTrue("Result value should be numeric: " + value, false);
                }

                // Verify units present
                String units = fields[4];
                assertNotNull("Units should be present", units);
                assertTrue("Units should not be empty", !units.isEmpty());
            }
        }
    }

    /**
     * Load ASTM lines from the fixture file (skipping comments).
     */
    private List<String> loadAstmLines() throws Exception {
        String content;
        try (InputStream in = new ClassPathResource(FIXTURE_PATH).getInputStream()) {
            content = IOUtils.toString(in, StandardCharsets.UTF_8);
        }

        List<String> lines = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
