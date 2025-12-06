package org.openelisglobal.test.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for TestSectionConfigurationHandler. Tests focus on validation
 * logic that doesn't require Spring context or static mocking.
 */
public class TestSectionConfigurationHandlerTest {

    private TestSectionConfigurationHandler handler = new TestSectionConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("test-sections", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(100, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test section configuration file test-sections.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test-sections.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestSectionNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "description,isActive\n" + "Some Description,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test section configuration file test-sections.csv must have a 'testSectionName' column");

        // When
        handler.processConfiguration(inputStream, "test-sections.csv");
    }
}
