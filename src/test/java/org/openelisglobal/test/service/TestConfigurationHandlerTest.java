package org.openelisglobal.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for TestConfigurationHandler. Tests focus on validation and
 * identity logic that doesn't require Spring context or a database; the load
 * itself is covered by CatalogCsvLoaderIntegrationTest.
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

    @Test
    public void findTestByPlainName_prefersExactDescriptionOverNormalizedFallback() throws Exception {
        TestService testService = mock(TestService.class);
        org.openelisglobal.test.valueholder.Test expected = new org.openelisglobal.test.valueholder.Test();
        inject("testService", testService);
        when(testService.getTestByDescription("LYM%")).thenReturn(expected);

        org.openelisglobal.test.valueholder.Test result = findTestByPlainName("LYM%");

        assertSame(expected, result);
        verify(testService).getTestByDescription("LYM%");
        verify(testService, never()).getTestByNormalizedDescription("LYM%");
    }

    @Test
    public void findTestByPlainName_usesNormalizedFallbackWhenNoExactDescriptionExists() throws Exception {
        TestService testService = mock(TestService.class);
        org.openelisglobal.test.valueholder.Test expected = new org.openelisglobal.test.valueholder.Test();
        inject("testService", testService);
        when(testService.getTestByNormalizedDescription("Stat-Pak")).thenReturn(expected);

        org.openelisglobal.test.valueholder.Test result = findTestByPlainName("Stat-Pak");

        assertSame(expected, result);
        verify(testService).getTestByDescription("Stat-Pak");
        verify(testService).getTestByNormalizedDescription("Stat-Pak");
    }

    @Test
    public void findTestByPlainName_neverIdentifiesATestByItsTranslation() throws Exception {
        TestService testService = mock(TestService.class);
        inject("testService", testService);

        assertNull(findTestByPlainName("Glucose"));

        verify(testService, never()).getTestByLocalizedName(anyString(), any(Locale.class));
        verify(testService, never()).getTestByLocalizedName(anyString());
    }

    private org.openelisglobal.test.valueholder.Test findTestByPlainName(String testName) throws Exception {
        Method method = TestConfigurationHandler.class.getDeclaredMethod("findTestByPlainName", String.class);
        method.setAccessible(true);
        return (org.openelisglobal.test.valueholder.Test) method.invoke(handler, testName);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = TestConfigurationHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, value);
    }
}
