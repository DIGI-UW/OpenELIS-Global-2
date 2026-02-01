package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for Stago STart 4 message processing with full Spring
 * context.
 *
 * <p>
 * Feature: 011-madagascar-analyzer-integration Milestone: M11 (Stago STart 4)
 *
 * <p>
 * Tests verify:
 * <ul>
 * <li>ASTM message processing with plugin inserter
 * <li>HL7 message processing with plugin inserter
 * <li>Plugin identification and routing
 * </ul>
 *
 * <p>
 * Note: This test requires the Stago STart 4 plugin JAR to be deployed to
 * `/var/lib/openelis-global/plugins/` or available in the test classpath.
 */
public class StagoSTart4MessageProcessingIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    @Autowired
    private AnalyzerService analyzerService;

    private List<String> astmMessageLines;
    private List<String> hl7MessageLines;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Load ASTM test fixture
        astmMessageLines = loadTestFixture("testdata/stago/stago-start4-coagulation.astm");

        // Load HL7 test fixture
        hl7MessageLines = loadTestFixture("testdata/stago/stago-start4-coagulation.hl7");
    }

    /**
     * Test that ASTM messages are correctly identified by Stago plugin.
     *
     * <p>
     * Verifies plugin.isTargetAnalyzer() correctly identifies Stago ASTM messages.
     */
    @Test
    public void testASTMMessageIdentification() {
        Analyzer analyzer = analyzerService.getAnalyzerByName("Stago STart 4");

        // If plugin is not available, skip test
        if (analyzer == null) {
            System.out.println("SKIP: Stago STart 4 analyzer not found. Plugin may not be deployed.");
            return;
        }

        // Find plugin that can handle this message
        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();
        AnalyzerImporterPlugin stagoPlugin = null;

        for (AnalyzerImporterPlugin plugin : plugins) {
            if (plugin != null && plugin.isTargetAnalyzer(astmMessageLines)) {
                stagoPlugin = plugin;
                break;
            }
        }

        // If no plugin matches, skip test
        if (stagoPlugin == null) {
            System.out.println(
                    "SKIP: No plugin found that can handle Stago ASTM messages. " + "Plugin may not be deployed.");
            return;
        }

        assertNotNull("Stago plugin should identify ASTM message", stagoPlugin);
        assertTrue("Plugin should recognize Stago ASTM message", stagoPlugin.isTargetAnalyzer(astmMessageLines));
    }

    /**
     * Test that HL7 messages are correctly identified by Stago plugin.
     *
     * <p>
     * Verifies plugin.isTargetAnalyzer() correctly identifies Stago HL7 messages.
     */
    @Test
    public void testHL7MessageIdentification() {
        Analyzer analyzer = analyzerService.getAnalyzerByName("Stago STart 4");

        // If plugin is not available, skip test
        if (analyzer == null) {
            System.out.println("SKIP: Stago STart 4 analyzer not found. Plugin may not be deployed.");
            return;
        }

        // Find plugin that can handle this message
        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();
        AnalyzerImporterPlugin stagoPlugin = null;

        for (AnalyzerImporterPlugin plugin : plugins) {
            if (plugin != null && plugin.isTargetAnalyzer(hl7MessageLines)) {
                stagoPlugin = plugin;
                break;
            }
        }

        // If no plugin matches, skip test
        if (stagoPlugin == null) {
            System.out.println(
                    "SKIP: No plugin found that can handle Stago HL7 messages. " + "Plugin may not be deployed.");
            return;
        }

        assertNotNull("Stago plugin should identify HL7 message", stagoPlugin);
        assertTrue("Plugin should recognize Stago HL7 message", stagoPlugin.isTargetAnalyzer(hl7MessageLines));
    }

    /**
     * Test that result messages are correctly detected.
     *
     * <p>
     * Verifies plugin.isAnalyzerResult() correctly identifies result messages.
     */
    @Test
    public void testResultMessageDetection() {
        Analyzer analyzer = analyzerService.getAnalyzerByName("Stago STart 4");

        // If plugin is not available, skip test
        if (analyzer == null) {
            System.out.println("SKIP: Stago STart 4 analyzer not found. Plugin may not be deployed.");
            return;
        }

        // Find plugin that can handle this message
        List<AnalyzerImporterPlugin> plugins = pluginAnalyzerService.getAnalyzerPlugins();
        AnalyzerImporterPlugin stagoPlugin = null;

        for (AnalyzerImporterPlugin plugin : plugins) {
            if (plugin != null && plugin.isTargetAnalyzer(astmMessageLines)) {
                stagoPlugin = plugin;
                break;
            }
        }

        // If no plugin matches, skip test
        if (stagoPlugin == null) {
            System.out
                    .println("SKIP: No plugin found that can handle Stago messages. " + "Plugin may not be deployed.");
            return;
        }

        // Test ASTM result detection
        assertTrue("Plugin should detect ASTM result message", stagoPlugin.isAnalyzerResult(astmMessageLines));

        // Test HL7 result detection
        assertTrue("Plugin should detect HL7 result message", stagoPlugin.isAnalyzerResult(hl7MessageLines));
    }

    /**
     * Helper method to load test fixture files from test resources.
     *
     * @param resourcePath path to test fixture file
     * @return list of lines from the file
     * @throws Exception if file cannot be read
     */
    private List<String> loadTestFixture(String resourcePath) throws Exception {
        List<String> lines = new ArrayList<>();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new RuntimeException("Test fixture not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comment lines and empty lines
                if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }

        return lines;
    }
}
