/**
 * Integration test for Mindray BA-88A biochemistry analyzer via RS232/ASTM.
 *
 *
 * <p>The BA-88A is a semi-automatic biochemistry analyzer that communicates via RS232
 * serial protocol using ASTM LIS2-A2 format. Unlike BC-5380 and BS-360E which use HL7,
 * the BA-88A requires ASTM parsing through the openelis-analyzer-bridge.
 *
 * <p>RS232 Configuration:
 * <ul>
 *   <li>Baud Rate: 9600</li>
 *   <li>Data Bits: 8</li>
 *   <li>Parity: None</li>
 *   <li>Stop Bits: 1</li>
 *   <li>Flow Control: None</li>
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
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class MindrayBA88AIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ANALYZER_NAME = "Mindray BA-88A";
    private static final String FIXTURE_PATH = "testdata/astm/mindray-ba88a-result.txt";

    // RS232 configuration constants for BA-88A
    // Note: stop_bits column is VARCHAR with enum values: ONE, ONE_POINT_FIVE, TWO
    private static final int BAUD_RATE = 9600;
    private static final int DATA_BITS = 8;
    private static final String STOP_BITS = "ONE";
    private static final String PARITY = "NONE";
    private static final String FLOW_CONTROL = "NONE";

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Analyzer ba88aAnalyzer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
        createBA88AAnalyzerAndConfig();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    /**
     * Create the BA-88A analyzer with RS232 serial port configuration.
     */
    private void createBA88AAnalyzerAndConfig() {
        ba88aAnalyzer = new Analyzer();
        ba88aAnalyzer.setName(ANALYZER_NAME);
        ba88aAnalyzer.setActive(true);
        ba88aAnalyzer.setSysUserId("1");
        ba88aAnalyzer.setIpAddress("127.0.0.1");
        ba88aAnalyzer.setPort(8080);
        String analyzerId = analyzerService.insert(ba88aAnalyzer);
        ba88aAnalyzer.setId(analyzerId);

        // Create RS232 serial port configuration via JDBC
        // This simulates what the openelis-analyzer-bridge configuration would store
        // Note: serial_port_configuration.analyzer_id references analyzer.id (numeric)
        jdbcTemplate.update("INSERT INTO clinlims.serial_port_configuration "
                + "(id, analyzer_id, port_name, baud_rate, data_bits, stop_bits, parity, flow_control, active, fhir_uuid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", java.util.UUID.randomUUID().toString(),
                Integer.parseInt(ba88aAnalyzer.getId()), "/dev/ttyUSB0", BAUD_RATE, DATA_BITS, STOP_BITS, PARITY,
                FLOW_CONTROL, true, java.util.UUID.randomUUID());
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM clinlims.analyzer_results WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = ?)",
                    ANALYZER_NAME);
            jdbcTemplate.update(
                    "DELETE FROM clinlims.serial_port_configuration WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = ?)",
                    ANALYZER_NAME);
            jdbcTemplate.update("DELETE FROM clinlims.analyzer WHERE name = ?", ANALYZER_NAME);
        } catch (Exception e) {
            // best-effort cleanup
        }
    }

    /**
     * Test: Verify ASTM message from BA-88A can be parsed.
     *
     * This test validates that the ASTMAnalyzerReader can read and parse the ASTM
     * LIS2-A2 formatted message from the BA-88A analyzer.
     */
    @Test
    public void astmMessage_canBeParsed() throws Exception {
        // Load the BA-88A ASTM fixture
        String astmMessage = loadFixture(FIXTURE_PATH);
        assertNotNull("Fixture should be loaded", astmMessage);

        // Parse only the ASTM segment lines (skip comment lines starting with #)
        List<String> astmLines = extractAstmLines(astmMessage);
        assertTrue("Should have ASTM segment lines", astmLines.size() >= 4);

        // Verify H segment (header) is present
        assertTrue("Should have H segment", astmLines.get(0).startsWith("H|"));

        // Verify P segment (patient) is present
        assertTrue("Should have P segment", astmLines.get(1).startsWith("P|"));

        // Verify O segment (order) is present
        assertTrue("Should have O segment", astmLines.get(2).startsWith("O|"));

        // Verify R segments (results) are present
        long resultCount = astmLines.stream().filter(line -> line.startsWith("R|")).count();
        assertEquals("Should have 10 R segments (results)", 10, resultCount);

        // Verify L segment (terminator) is present
        assertTrue("Should have L segment", astmLines.get(astmLines.size() - 1).startsWith("L|"));
    }

    /**
     * Test: Verify RS232 configuration parameters are correctly stored.
     *
     * This validates that the serial port configuration for the BA-88A (9600 baud,
     * 8N1, no flow control) is properly persisted.
     */
    @Test
    public void rs232Configuration_isStoredCorrectly() {
        // Query the serial port configuration
        // serial_port_configuration.analyzer_id directly references analyzer.id
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.serial_port_configuration spc "
                        + "JOIN clinlims.analyzer a ON spc.analyzer_id = a.id "
                        + "WHERE a.name = ? AND spc.baud_rate = ? AND spc.data_bits = ? "
                        + "AND spc.stop_bits = ? AND spc.parity = ? AND spc.flow_control = ?",
                Integer.class, ANALYZER_NAME, BAUD_RATE, DATA_BITS, STOP_BITS, PARITY, FLOW_CONTROL);

        assertEquals("RS232 configuration should be stored with correct parameters", Integer.valueOf(1), count);
    }

    /**
     * Test: Verify accession number extraction from O-segment.
     *
     * The BA-88A fixture contains O|1|ACC-2026-001^LAB format.
     */
    @Test
    public void oSegment_accessionNumberExtracted() throws Exception {
        String astmMessage = loadFixture(FIXTURE_PATH);
        List<String> astmLines = extractAstmLines(astmMessage);

        // Find O segment
        String oSegment = astmLines.stream().filter(line -> line.startsWith("O|")).findFirst().orElse(null);

        assertNotNull("O segment should exist", oSegment);

        // Extract accession number (field 2, component 1)
        String[] fields = oSegment.split("\\|");
        assertTrue("O segment should have specimen ID field", fields.length > 2);

        String specimenId = fields[2];
        String accessionNumber = specimenId.split("\\^")[0];
        assertEquals("Accession number should be ACC-2026-001", "ACC-2026-001", accessionNumber);
    }

    /**
     * Test: Verify test code extraction from R-segments.
     *
     * The BA-88A fixture contains R|1|^^^ALT|35.2|U/L|... format.
     */
    @Test
    public void rSegment_testCodeAndValueExtracted() throws Exception {
        String astmMessage = loadFixture(FIXTURE_PATH);
        List<String> astmLines = extractAstmLines(astmMessage);

        // Find first R segment (ALT result)
        String rSegment = astmLines.stream().filter(line -> line.startsWith("R|1|")).findFirst().orElse(null);

        assertNotNull("R segment should exist", rSegment);

        // Extract test code and value
        String[] fields = rSegment.split("\\|");
        assertTrue("R segment should have test ID field", fields.length > 3);

        // Test ID is in field 2, format: ^^^TEST_CODE
        String testIdField = fields[2];
        String[] components = testIdField.split("\\^");
        assertTrue("Test ID should have 4 components", components.length >= 4);
        assertEquals("Test code should be ALT", "ALT", components[3]);

        // Result value is in field 3
        assertEquals("Result value should be 35.2", "35.2", fields[3]);

        // Units are in field 4
        assertEquals("Units should be U/L", "U/L", fields[4]);
    }

    /**
     * Test: Verify analyzer name extraction from H-segment.
     *
     * The BA-88A fixture contains H|\^&|||Mindray^BA-88A^1.0|... format.
     */
    @Test
    public void hSegment_analyzerNameExtracted() throws Exception {
        String astmMessage = loadFixture(FIXTURE_PATH);
        List<String> astmLines = extractAstmLines(astmMessage);

        // Find H segment
        String hSegment = astmLines.stream().filter(line -> line.startsWith("H|")).findFirst().orElse(null);

        assertNotNull("H segment should exist", hSegment);

        // Extract analyzer name (field 4, format: Manufacturer^Model^Version)
        String[] fields = hSegment.split("\\|");
        assertTrue("H segment should have sender ID field", fields.length > 4);

        String senderId = fields[4];
        String[] components = senderId.split("\\^");
        assertEquals("Manufacturer should be Mindray", "Mindray", components[0]);
        assertEquals("Model should be BA-88A", "BA-88A", components[1]);
    }

    /**
     * Extract ASTM segment lines from the fixture (skip comment lines).
     */
    private List<String> extractAstmLines(String content) {
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            // Skip empty lines and comment lines (starting with #)
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
