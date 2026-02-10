package org.openelisglobal.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for TestConfigurationHandler. Tests focus on validation logic that
 * doesn't require Spring context or static mocking.
 */
public class TestConfigurationHandlerTest {

    private TestConfigurationHandler handler = new TestConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("tests", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(200, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testSection,sampleType\n" + "Hematology,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv must have a 'testName' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestSectionColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv must have a 'testSection' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testParseCsvLine_BasicParsing() throws Exception {
        // Test the CSV parsing functionality
        Method parseCsvLineMethod = TestConfigurationHandler.class.getDeclaredMethod("parseCsvLine", String.class);
        parseCsvLineMethod.setAccessible(true);

        // Test simple CSV line
        String csvLine = "Stat PaK,Serology,Plasma|Serum|Whole Blood,12345,Y,Y,1,mg/dL,StatPaK,StatPaK";
        String[] result = (String[]) parseCsvLineMethod.invoke(handler, csvLine);

        assertEquals("Should parse 10 columns", 10, result.length);
        assertEquals("Test name should be parsed correctly", "Stat PaK", result[0]);
        assertEquals("Test section should be parsed correctly", "Serology", result[1]);
        assertEquals("Sample types should be parsed correctly", "Plasma|Serum|Whole Blood", result[2]);
        assertEquals("LOINC should be parsed correctly", "12345", result[3]);
    }

    @Test
    public void testParseCsvLine_WithQuotes() throws Exception {
        // Test CSV parsing with quoted values
        Method parseCsvLineMethod = TestConfigurationHandler.class.getDeclaredMethod("parseCsvLine", String.class);
        parseCsvLineMethod.setAccessible(true);

        String csvLine = "\"Test Name, With Comma\",Serology,\"Plasma|Serum, Special\",12345,Y,Y,1,mg/dL,\"English Name\",\"French Name\"";
        String[] result = (String[]) parseCsvLineMethod.invoke(handler, csvLine);

        assertEquals("Should parse 10 columns", 10, result.length);
        assertEquals("Quoted test name should be parsed correctly", "Test Name, With Comma", result[0]);
        assertEquals("Quoted sample types should be parsed correctly", "Plasma|Serum, Special", result[2]);
    }

    /**
     * Test to document the new behavior: multiple sample types should result in separate tests
     * Note: This test documents the expected behavior but doesn't test the full implementation
     * since it would require Spring context and database services.
     */
    @Test
    public void testMultipleSampleTypesLogic_DocumentedBehavior() {
        // This test documents the expected behavior for future reference
        String baseTestName = "Stat PaK";
        String sampleTypes = "Plasma|Serum|Whole Blood";
        String[] expectedTestNames = {
            "Stat PaK(Plasma)",
            "Stat PaK(Serum)",
            "Stat PaK(Whole Blood)"
        };

        String[] sampleTypeArray = sampleTypes.split("\\|");
        List<String> generatedTestNames = new ArrayList<>();

        for (String sampleType : sampleTypeArray) {
            sampleType = sampleType.trim();
            if (!sampleType.isEmpty()) {
                generatedTestNames.add(baseTestName + "(" + sampleType + ")");
            }
        }

        assertEquals("Should generate 3 test names", 3, generatedTestNames.size());
        for (String expectedName : expectedTestNames) {
            assertTrue("Should contain test name: " + expectedName,
                      generatedTestNames.contains(expectedName));
        }
    }

}
