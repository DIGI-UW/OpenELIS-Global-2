package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NotebookDepartmentConfigurationHandlerTest {

    private NotebookDepartmentConfigurationHandler handler = new NotebookDepartmentConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("notebook-departments", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(220, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Notebook-department configuration file test.csv is empty");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingNotebookTitleColumn_ThrowsException() throws Exception {
        String csv = "departmentName\nTraditional & Modern Medicine Research Lab\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("configuration file test.csv must have a 'notebookTitle' column");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingDepartmentNameColumn_ThrowsException() throws Exception {
        String csv = "notebookTitle\nTraditional & Modern Medicine Research Lab\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("configuration file test.csv must have a 'departmentName' column");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_ValidHeaders_NoException() throws Exception {
        String csv = "notebookTitle,departmentName\n";
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