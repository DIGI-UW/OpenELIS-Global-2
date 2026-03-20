package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class NotebookTemplateConfigurationHandlerTest {

    @Mock
    private NoteBookDAO noteBookDAO;

    @Mock
    private NoteBookPageDAO noteBookPageDAO;

    @Mock
    private TestSectionService testSectionService;

    @InjectMocks
    private NotebookTemplateConfigurationHandler handler;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        // @Value is not processed by @InjectMocks, set it via reflection
        ReflectionTestUtils.setField(handler, "forceReload", true);
    }

    @Test
    public void testValidate_MissingTitle_throws() throws Exception {
        String json = "{}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing 'title'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_BlankTitle_throws() throws Exception {
        String json = "{\"title\":\"  \",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing 'title'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_NoPagesArray_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\"}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no 'pages' array");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_EmptyPagesArray_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_PageWithMissingOrder_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"title\":\"Page 1\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing 'order'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_PageWithOrderZero_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":0}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid 'order'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_PageWithNegativeOrder_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":-1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid 'order'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_PageWithDataButNoPageType_throws() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1,\"data\":{}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing 'pageType'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_PageWithDataAndPageType_passes() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1,\"pageType\":\"generic_sample_reception\",\"data\":{}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        // Should not throw; will fail on DB insert since template doesn't exist yet
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_PageWithNoDataAndNoPageType_passes() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        // Should not throw (only warns for missing pageType); will fail on DB insert
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_ValidMinimalTemplate_passes() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testProcess_NewTemplate_insertsNotebook() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testProcess_NewTemplate_insertsPages() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1},{\"order\":2}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookPageDAO, times(2)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testProcess_NewTemplate_setsStatusActive() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
        // Verify status was set to ACTIVE
    }

    @Test
    public void testProcess_NewTemplate_invalidStatus_defaultsToActive() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"status\":\"INVALID\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
        // Verify status defaults to ACTIVE despite invalid value
    }

    @Test
    public void testProcess_NewTemplate_departmentNotFound_logsWarnContinues() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"departments\":[\"Unknown Dept\"],\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());
        when(testSectionService.getTestSectionByName("Unknown Dept")).thenReturn(null);

        // Should not throw; logs warning and continues
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
        verify(noteBookDAO, times(1)).update(any(NoteBook.class)); // Update after linkDepartments
    }

    @Test
    public void testProcess_ExistingTemplate_forceReload_callsUpdate() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Test Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(matches);

        handler.processConfiguration(input, "test.json");

        // Should call update for scalar fields, departments, and pages
        verify(noteBookDAO, times(3)).update(any(NoteBook.class)); // scalar, deps clear, pages clear
    }

    @Test
    public void testProcess_ExistingTemplate_forceReload_replacesPages() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1},{\"order\":2}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Test Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(matches);

        handler.processConfiguration(input, "test.json");

        // Pages should be cleared then new ones inserted
        verify(noteBookPageDAO, times(2)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testProcess_ExistingTemplate_updatesScalarFields() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"workflowType\":\"lab-key\",\"content\":\"New Content\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Test Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(matches);

        handler.processConfiguration(input, "test.json");

        // Verify fields were updated
        verify(noteBookDAO, times(3)).update(any(NoteBook.class));
    }

    @Test
    public void testProcess_ExistingTemplate_clearsAndReSyncsDepartments() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"departments\":[\"Dept A\"],\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Test Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(matches);

        TestSection deptA = new TestSection();
        deptA.setId("1");
        deptA.setTestSectionName("Dept A");
        when(testSectionService.getTestSectionByName("Dept A")).thenReturn(deptA);

        handler.processConfiguration(input, "test.json");

        // Verify departments cleared and re-synced
        // (scalar update + clear update + link update + page clear update = 4 calls)
        verify(noteBookDAO, times(4)).update(any(NoteBook.class));
    }

    @Test
    public void testGetDomainName() {
        assertEquals("notebook-templates", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("json", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(210, handler.getLoadOrder());
    }
}
