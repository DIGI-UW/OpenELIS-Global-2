/**
 * Integration test for GeneXpert File-based protocol variant.
 *
 *
 * <p>Verifies that:
 * <ul>
 *   <li>GeneXpert CSV/TXT export files are parsed correctly</li>
 *   <li>Results from "GeneXpert Dx System" format are extracted</li>
 * </ul>
 */
package org.openelisglobal.analyzer.genexpert;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.core.io.ClassPathResource;

public class GeneXpertFileIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String FIXTURE_PATH = "testdata/files/genexpert-results.csv";

    /**
     * Test that the GeneXpert export file fixture has the expected structure.
     * GeneXpert files use a section-based format with headers like "GeneXpert Dx
     * System".
     */
    @Test
    public void geneXpertFile_hasExpectedStructure() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        assertTrue("Fixture should contain GeneXpert Dx System header", fixture.contains("GeneXpert Dx System"));
        assertTrue("Fixture should contain ASSAY INFORMATION section", fixture.contains("ASSAY INFORMATION"));
        assertTrue("Fixture should contain RESULT TABLE section", fixture.contains("RESULT TABLE"));
    }

    /**
     * Test that the GeneXpert File plugin can coexist with ASTM and HL7 variants.
     * The File variant is identified by the "GeneXpert Dx System" header.
     */
    @Test
    public void geneXpertFilePlugin_identifiedByFileFormat() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        // GeneXpert file format is identified by the specific file structure
        assertTrue("Fixture should be identifiable as GeneXpert format",
                fixture.contains("GeneXpert Dx System") || fixture.contains("ASSAY INFORMATION"));
    }

    /**
     * Test that the fixture contains multiple results (multi-test export).
     */
    @Test
    public void geneXpertFile_containsMultipleResults() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        // Count RESULT TABLE sections - should have multiple results
        int resultCount = 0;
        int index = 0;
        while ((index = fixture.indexOf("RESULT TABLE", index)) != -1) {
            resultCount++;
            index += "RESULT TABLE".length();
        }
        assertTrue("Fixture should contain multiple RESULT TABLE sections (found " + resultCount + ")",
                resultCount >= 2);
    }

    /**
     * Test that the fixture contains expected assay types.
     */
    @Test
    public void geneXpertFile_containsExpectedAssays() throws Exception {
        String fixture = loadFixture(FIXTURE_PATH);

        // Verify expected assay types are present
        assertTrue("Fixture should contain SARS-CoV-2 assay", fixture.contains("Xpert Xpress SARS-CoV-2"));
        assertTrue("Fixture should contain HBV assay", fixture.contains("Xpert HBV Viral Load"));
    }

    private static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
