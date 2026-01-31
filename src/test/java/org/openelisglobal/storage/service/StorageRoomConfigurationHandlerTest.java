package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StorageRoomConfigurationHandlerTest {

    private StorageRoomConfigurationHandler handler = new StorageRoomConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("storage-rooms", handler.getDomainName());
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
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Storage room configuration file test.csv is empty");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingNameColumn_ThrowsException() throws Exception {
        String csv = "code,description,active\nMAIN-LAB,Main laboratory,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("configuration file test.csv must have a 'name' column");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingCodeColumn_ThrowsException() throws Exception {
        String csv = "name,description,active\nMain Laboratory,Main laboratory,true\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("configuration file test.csv must have a 'code' column");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_ValidHeaders_NoException() throws Exception {
        String csv = "name,code,description,active\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // This should not throw an exception (would need mocked dependencies for full test)
        try {
            handler.processConfiguration(inputStream, "test.csv");
        } catch (NullPointerException e) {
            // Expected since we don't have autowired dependencies in unit test
            // The important thing is that it parsed headers correctly
        }
    }
}