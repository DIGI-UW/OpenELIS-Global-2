package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for TypeOfSampleConfigurationHandler. Tests focus on validation
 * logic that doesn't require Spring context or static mocking.
 */
public class TypeOfSampleConfigurationHandlerTest {

    private TypeOfSampleConfigurationHandler handler = new TypeOfSampleConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("sample-types", handler.getDomainName());
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
        thrown.expectMessage("Sample type configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingDescriptionColumn_ThrowsException() throws Exception {
        // Given
        String csv = "localAbbreviation,domain\n" + "WB,H\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sample type configuration file test.csv must have a 'description' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingLocalAbbreviationColumn_ThrowsException() throws Exception {
        // Given
        String csv = "description,domain\n" + "Whole Blood,H\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sample type configuration file test.csv must have a 'localAbbreviation' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }
}
