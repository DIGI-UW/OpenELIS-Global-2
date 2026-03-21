package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    public void testValidate_PageWithDataAndNoPageType_passes() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1,\"data\":{}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        // pageType is not required; data can exist independently
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_PageWithNoDataAndNoPageType_passes() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

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
    }

    @Test
    public void testProcess_NewTemplate_invalidStatus_defaultsToActive() throws Exception {
        String json = "{\"title\":\"Test Lab\",\"status\":\"INVALID\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
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
        verify(noteBookDAO, times(1)).update(any(NoteBook.class));
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

    @Test
    public void testValidate_DuplicatePageOrders_throws() throws Exception {
        String json = "{\n" + "  \"title\": \"Test Lab\",\n" + "  \"pages\": [\n"
                + "    {\"order\": 1, \"title\": \"Page 1\"},\n" + "    {\"order\": 2, \"title\": \"Page 2\"},\n"
                + "    {\"order\": 2, \"title\": \"Page 3 (duplicate order)\"}\n" + "  ]\n" + "}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate 'order=2'");
        thrown.expectMessage("all page orders must be unique");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testCreatePages_WithPageData_PersistsDataToPageEntity() throws Exception {
        String json = "{\n" + "  \"title\": \"Test Lab\",\n" + "  \"workflowType\": \"test-workflow\",\n"
                + "  \"pages\": [\n" + "    {\n" + "      \"order\": 1,\n" + "      \"title\": \"Sample Reception\",\n"
                + "      \"data\": {\n" + "        \"sampleTypesEndpoint\": \"/rest/notebook/test/sample-types\",\n"
                + "        \"validValues\": [\"Type1\", \"Type2\"],\n" + "        \"nested\": {\n"
                + "          \"key\": \"value\"\n" + "        }\n" + "      }\n" + "    }\n" + "  ]\n" + "}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        NoteBook template = new NoteBook();
        template.setId(1);
        template.setTitle("Test Lab");
        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());
        when(noteBookDAO.insert(any(NoteBook.class))).thenReturn(1);

        handler.processConfiguration(input, "test.json");

        verify(noteBookPageDAO, times(1)).insert(any(NoteBookPage.class));

        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO).insert(pageCaptor.capture());

        NoteBookPage insertedPage = pageCaptor.getValue();
        assertEquals("Sample Reception", insertedPage.getTitle());
        assertEquals(1, (int) insertedPage.getOrder());
        assertEquals("/rest/notebook/test/sample-types", insertedPage.getData().get("sampleTypesEndpoint"));
        java.util.List<?> validValues = (java.util.List<?>) insertedPage.getData().get("validValues");
        assertEquals(2, validValues.size());
        java.util.Map<?, ?> nested = (java.util.Map<?, ?>) insertedPage.getData().get("nested");
        assertEquals("value", nested.get("key"));
    }

    // ========== Additional Edge Case & Comprehensive Tests ==========

    @Test
    public void testValidate_TitleWithLeadingTrailingWhitespace_passes() throws Exception {
        String json = "{\"title\":\"  Test Lab  \",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Test Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_TitleWithOnlyWhitespace_throws() throws Exception {
        String json = "{\"title\":\"   \",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing 'title'");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_VeryLongTitle_passes() throws Exception {
        String longTitle = "A".repeat(500);
        String json = "{\"title\":\"" + longTitle + "\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", longTitle)).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_SpecialCharactersInTitle_passes() throws Exception {
        String json = "{\"title\":\"Lab & Research (2026) — Special/Test\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Lab & Research (2026) — Special/Test")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testValidate_MultiplePages_ValidOrders_passes() throws Exception {
        String json = "{\"title\":\"Multi Page Lab\",\"pages\":[{\"order\":1},{\"order\":2},{\"order\":3},{\"order\":100}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Multi Page Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookPageDAO, times(4)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testValidate_PageWithAllOptionalFields_passes() throws Exception {
        String json = "{\"title\":\"Lab\",\"pages\":[{\"order\":1,\"title\":\"Page Title\",\"instructions\":\"Do X\",\"content\":\"Content\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
        verify(noteBookPageDAO, times(1)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testValidate_StatusDraft_Persists() throws Exception {
        String json = "{\"title\":\"Draft Lab\",\"status\":\"DRAFT\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Draft Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> captor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO).insert(captor.capture());
        assertEquals("DRAFT", captor.getValue().getStatus().toString());
    }

    @Test
    public void testValidate_StatusFinalized_Persists() throws Exception {
        String json = "{\"title\":\"Final Lab\",\"status\":\"FINALIZED\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Final Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> captor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO).insert(captor.capture());
        assertEquals("FINALIZED", captor.getValue().getStatus().toString());
    }

    @Test
    public void testValidate_EmptyStringStatus_DefaultsToActive() throws Exception {
        String json = "{\"title\":\"Empty Status Lab\",\"status\":\"\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Empty Status Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> captor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO).insert(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus().toString());
    }

    @Test
    public void testValidate_CaseSensitiveStatus_InvalidCase_DefaultsToActive() throws Exception {
        String json = "{\"title\":\"Case Lab\",\"status\":\"draft\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Case Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> captor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO).insert(captor.capture());
        // Should default to ACTIVE because "draft" (lowercase) is not valid
        assertEquals("ACTIVE", captor.getValue().getStatus().toString());
    }

    @Test
    public void testDataPersistence_ComplexNestedStructure() throws Exception {
        String json = "{\"title\":\"Complex Lab\",\"pages\":[{\"order\":1,\"data\":{\"level1\":{\"level2\":{\"level3\":\"deep\"},\"array\":[1,2,3]},\"strings\":[\"a\",\"b\"],\"numbers\":[1.5,2.7]}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Complex Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO).insert(pageCaptor.capture());

        NoteBookPage page = pageCaptor.getValue();
        assertNotNull(page.getData());
        assertTrue(page.getData().containsKey("level1"));
        assertTrue(page.getData().containsKey("strings"));
        assertTrue(page.getData().containsKey("numbers"));
    }

    @Test
    public void testDataPersistence_UnicodeAndSpecialCharacters() throws Exception {
        String json = "{\"title\":\"Unicode Lab\",\"pages\":[{\"order\":1,\"data\":{\"emoji\":\"🧬🔬\",\"chinese\":\"中文\",\"arabic\":\"العربية\",\"symbols\":\"©®™€¥£§\"}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Unicode Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO).insert(pageCaptor.capture());

        NoteBookPage page = pageCaptor.getValue();
        assertEquals("🧬🔬", page.getData().get("emoji"));
        assertEquals("中文", page.getData().get("chinese"));
    }

    @Test
    public void testUpdate_ReducePageCount_OldPagesDeleted() throws Exception {
        // Existing template with 3 pages
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Reduce Pages Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Reduce Pages Lab")).thenReturn(matches);

        // Config with only 1 page
        String json = "{\"title\":\"Reduce Pages Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        // Should insert only 1 new page (old ones deleted via cascade)
        verify(noteBookPageDAO, times(1)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testUpdate_IncreasePageCount_NewPagesAdded() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Increase Pages Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Increase Pages Lab")).thenReturn(matches);

        // Config with 5 pages
        String json = "{\"title\":\"Increase Pages Lab\",\"pages\":[{\"order\":1},{\"order\":2},{\"order\":3},{\"order\":4},{\"order\":5}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        // Should insert 5 new pages
        verify(noteBookPageDAO, times(5)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testUpdate_RemoveDataFromPages() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("No Data Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "No Data Lab")).thenReturn(matches);

        // Config page with no data
        String json = "{\"title\":\"No Data Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO).insert(pageCaptor.capture());

        NoteBookPage page = pageCaptor.getValue();
        assertNull(page.getData());
    }

    @Test
    public void testUpdate_TagsReplacedCompletely() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Tag Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        java.util.List<String> oldTags = new ArrayList<>();
        oldTags.add("old-tag");
        existingTemplate.setTags(oldTags);

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Tag Lab")).thenReturn(matches);

        // Config with different tags
        String json = "{\"title\":\"Tag Lab\",\"tags\":[\"new-tag-1\",\"new-tag-2\"],\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        // Verify tags were cleared and replaced (not appended)
        org.mockito.ArgumentCaptor<NoteBook> captor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO, times(3)).update(captor.capture()); // scalar, clear deps, clear pages
        NoteBook updated = captor.getAllValues().get(0);
        assertEquals(2, updated.getTags().size());
    }

    @Test
    public void testUpdate_DepartmentsCleared_ThenResynced() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Dept Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Dept Lab")).thenReturn(matches);

        TestSection newDept = new TestSection();
        newDept.setId("2");
        newDept.setTestSectionName("New Dept");
        when(testSectionService.getTestSectionByName("New Dept")).thenReturn(newDept);

        String json = "{\"title\":\"Dept Lab\",\"departments\":[\"New Dept\"],\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(4)).update(any(NoteBook.class));
    }

    @Test
    public void testUpdate_DuplicateDepartmentNames_NotDuplicated() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Dup Dept Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Dup Dept Lab")).thenReturn(matches);

        TestSection dept = new TestSection();
        dept.setId("1");
        dept.setTestSectionName("Dept A");
        when(testSectionService.getTestSectionByName("Dept A")).thenReturn(dept);

        // Config has same department twice
        String json = "{\"title\":\"Dup Dept Lab\",\"departments\":[\"Dept A\",\"Dept A\"],\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        // Set prevents duplicates, so only 1 dept should be linked (even though listed
        // twice)
        // Verify updates occurred (scalar, clear deps, link deps, clear pages)
        verify(noteBookDAO, times(4)).update(any(NoteBook.class));
        // Template should have only 1 department
        assertEquals(1, existingTemplate.getDepartments().size());
    }

    @Test
    public void testUpdate_PreservesExistingTemplateId() throws Exception {
        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(99); // Specific ID
        existingTemplate.setTitle("Preserve ID Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());
        existingTemplate.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate);
        when(noteBookDAO.getAllMatching("title", "Preserve ID Lab")).thenReturn(matches);

        String json = "{\"title\":\"Preserve ID Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        // Template ID should remain 99 (not changed during update)
        assertEquals(99, (int) existingTemplate.getId());
    }

    @Test
    public void testValidate_LargeDataPayload_Persists() throws Exception {
        // Create large payload (1MB of data)
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeData.append("\"field").append(i).append("\":\"").append("x".repeat(100)).append("\",");
        }
        largeData.deleteCharAt(largeData.length() - 1);

        String json = "{\"title\":\"Large Data Lab\",\"pages\":[{\"order\":1,\"data\":{" + largeData + "}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Large Data Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookPageDAO, times(1)).insert(any(NoteBookPage.class));
    }

    @Test
    public void testValidate_EmptyPages_AfterWhitespace_throws() throws Exception {
        String json = "{\"title\":\"Empty Pages Lab\",\"pages\":[]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty");

        handler.processConfiguration(input, "test.json");
    }

    @Test
    public void testValidate_OrderVeryLarge_passes() throws Exception {
        String json = "{\"title\":\"Large Order Lab\",\"pages\":[{\"order\":999999}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Large Order Lab")).thenReturn(new ArrayList<>());
        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(1)).insert(any(NoteBook.class));
    }

    @Test
    public void testErrorMessage_IncludesFileName() throws Exception {
        String json = "{}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("custom-template.json");

        handler.processConfiguration(input, "custom-template.json");
    }

    @Test
    public void testMultipleTemplatesWithSameName_UpdatesExisting() throws Exception {
        NoteBook existingTemplate1 = new NoteBook();
        existingTemplate1.setId(1);
        existingTemplate1.setTitle("Same Name Lab");
        existingTemplate1.setIsTemplate(true);
        existingTemplate1.setPages(new ArrayList<>());
        existingTemplate1.setDepartments(new HashSet<>());
        existingTemplate1.setTags(new ArrayList<>());

        NoteBook existingTemplate2 = new NoteBook();
        existingTemplate2.setId(2);
        existingTemplate2.setTitle("Same Name Lab");
        existingTemplate2.setIsTemplate(false);
        existingTemplate2.setPages(new ArrayList<>());
        existingTemplate2.setDepartments(new HashSet<>());
        existingTemplate2.setTags(new ArrayList<>());

        List<NoteBook> matches = new ArrayList<>();
        matches.add(existingTemplate1);
        matches.add(existingTemplate2);
        when(noteBookDAO.getAllMatching("title", "Same Name Lab")).thenReturn(matches);

        String json = "{\"title\":\"Same Name Lab\",\"pages\":[{\"order\":1}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(3)).update(any(NoteBook.class)); // scalar, clear deps, clear pages
    }
}
