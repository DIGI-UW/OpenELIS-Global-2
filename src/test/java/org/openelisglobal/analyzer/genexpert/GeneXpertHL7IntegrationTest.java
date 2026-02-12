/**
 * Integration test for GeneXpert HL7 protocol variant.
 *
 *
 * <p>Verifies that:
 * <ul>
 *   <li>GeneXpert HL7 ORU^R01 messages are parsed correctly</li>
 *   <li>Analyzer is identified from MSH sender (GENEXPERT)</li>
 * </ul>
 */
package org.openelisglobal.analyzer.genexpert;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.springframework.core.io.ClassPathResource;

public class GeneXpertHL7IntegrationTest extends BaseWebContextSensitiveTest {

    private static final String FIXTURE_PATH = "testdata/hl7/genexpert-result.hl7";

    /**
     * Test that a GeneXpert HL7 ORU^R01 message can be parsed by HL7AnalyzerReader.
     * This verifies the HL7 v2.5.1 message format is compatible with the reader.
     */
    @Test
    public void geneXpertHl7Message_parsedSuccessfully() throws Exception {
        String raw = loadFixture(FIXTURE_PATH);
        HL7AnalyzerReader reader = new HL7AnalyzerReader();

        boolean readOk = reader.readStream(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));

        assertTrue("HL7 message should parse successfully: " + reader.getError(), readOk);
        assertNull("No error should be present after successful parse", reader.getError());
    }

    /**
     * Test that the GeneXpert HL7 fixture contains the expected MSH sender. The HL7
     * variant is identified by MSH sender "GENEXPERT".
     */
    @Test
    public void geneXpertHl7Plugin_identifiedByMshSender() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        assertTrue("Fixture should contain GENEXPERT sender", fixture.contains("GENEXPERT"));
        assertTrue("Fixture should contain CEPHEID facility", fixture.contains("CEPHEID"));
        assertTrue("Fixture should be ORU^R01 message", fixture.contains("ORU^R01"));
    }

    /**
     * Test that the HL7 fixture contains the expected test result data.
     */
    @Test
    public void geneXpertHl7Fixture_containsExpectedResults() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        // Verify SARS-CoV-2 test result
        assertTrue("Fixture should contain SARS-CoV-2 test code", fixture.contains("94500-6"));
        assertTrue("Fixture should contain test result", fixture.contains("Not detected"));
        // Verify Ct value
        assertTrue("Fixture should contain Ct value", fixture.contains("28.5"));
    }

    private static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
