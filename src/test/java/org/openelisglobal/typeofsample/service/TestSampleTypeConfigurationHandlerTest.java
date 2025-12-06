package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for TestSampleTypeConfigurationHandler. Tests focus on validation
 * logic that doesn't require Spring context or static mocking.
 */
public class TestSampleTypeConfigurationHandlerTest {

    private TestSampleTypeConfigurationHandler handler = new TestSampleTypeConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("test-sample-types", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(210, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "sampleType\n" + "Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv must have a 'testName' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingSampleTypeColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testName\n" + "Complete Blood Count\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv must have a 'sampleType' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }
}
