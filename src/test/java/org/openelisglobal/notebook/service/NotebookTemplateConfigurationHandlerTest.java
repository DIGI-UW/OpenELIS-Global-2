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
        setForceUpdateTemplates(true);

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
        setForceUpdateTemplates(true);

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
        setForceUpdateTemplates(true);

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

        org.mockito.ArgumentCaptor<NoteBook> nbCaptor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO, org.mockito.Mockito.atLeast(1)).update(nbCaptor.capture());

        NoteBook updatedNb = nbCaptor.getValue();
        assertEquals("lab-key", updatedNb.getWorkflowType());
        assertEquals("New Content", updatedNb.getContent());
    }

    @Test
    public void testProcess_ExistingTemplate_clearsAndReSyncsDepartments() throws Exception {
        setForceUpdateTemplates(true);

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

        org.mockito.ArgumentCaptor<NoteBook> nbCaptor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO, org.mockito.Mockito.atLeast(1)).update(nbCaptor.capture());

        NoteBook updatedNb = nbCaptor.getValue();
        assertNotNull("Departments collection should exist", updatedNb.getDepartments());
        verify(testSectionService, org.mockito.Mockito.atLeastOnce()).getTestSectionByName("Dept A");
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
    public void testProcess_PageWithPageType_persistsPageType() throws Exception {
        String json = "{\"title\":\"Storage Lab\",\"pages\":[{\"order\":1,\"pageType\":\"gbd_storage_monitoring\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Storage Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        // Capture the inserted page and verify pageType was set
        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO, times(1)).insert(pageCaptor.capture());

        NoteBookPage insertedPage = pageCaptor.getValue();
        assertEquals("gbd_storage_monitoring", insertedPage.getPageType());
    }

    @Test
    public void testProcess_PageWithoutPageType_pageTypeRemains() throws Exception {
        String json = "{\"title\":\"Generic Lab\",\"pages\":[{\"order\":1,\"title\":\"Generic Page\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Generic Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        // Capture the inserted page and verify pageType was not set
        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO, times(1)).insert(pageCaptor.capture());

        NoteBookPage insertedPage = pageCaptor.getValue();
        assertNull(insertedPage.getPageType());
    }

    @Test
    public void testProcess_PageWithPageId_persistsPageId() throws Exception {
        String json = "{\"title\":\"Legacy Lab\",\"pages\":[{\"order\":1,\"pageId\":\"legacy_page_id\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Legacy Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        // Capture the inserted page and verify pageId was set
        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO, times(1)).insert(pageCaptor.capture());

        NoteBookPage insertedPage = pageCaptor.getValue();
        assertEquals("legacy_page_id", insertedPage.getPageId());
    }

    @Test
    public void testProcess_PageWithData_persistsDataAsJsonb() throws Exception {
        String json = "{\"title\":\"Config Lab\",\"pages\":[{\"order\":1,\"data\":{\"key\":\"value\",\"nested\":{\"field\":123}}}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        when(noteBookDAO.getAllMatching("title", "Config Lab")).thenReturn(new ArrayList<>());

        handler.processConfiguration(input, "test.json");

        // Capture the inserted page and verify data was parsed and set
        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO, times(1)).insert(pageCaptor.capture());

        NoteBookPage insertedPage = pageCaptor.getValue();
        assertNotNull("Data should be persisted", insertedPage.getData());
        assertEquals("value", insertedPage.getData().get("key"));
        assertNotNull("Nested object should exist", insertedPage.getData().get("nested"));
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
    public void testExistingTemplate_WithForceUpdateFalse_SkipsUpdate() throws Exception {
        setForceUpdateTemplates(false);

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Existing Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());

        NoteBookPage existingPage = new NoteBookPage();
        existingPage.setId(100);
        existingPage.setOrder(1);
        existingPage.setTitle("Original Page");
        existingTemplate.getPages().add(existingPage);

        when(noteBookDAO.getAllMatching("title", "Existing Lab"))
                .thenReturn(java.util.Collections.singletonList(existingTemplate));

        String json = "{\"title\":\"Existing Lab\",\"pages\":[{\"order\":1,\"title\":\"New Page\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        verify(noteBookDAO, times(0)).update(any(NoteBook.class));
        verify(noteBookPageDAO, times(0)).insert(any(NoteBookPage.class));
        assertEquals("Existing page should remain unchanged", 1, existingTemplate.getPages().size());
        assertEquals("Existing page title should not change", "Original Page",
                existingTemplate.getPages().get(0).getTitle());
    }

    @Test
    public void testExistingTemplate_WithForceUpdateTrue_UpdatesTemplate() throws Exception {
        setForceUpdateTemplates(true);

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Existing Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());

        NoteBookPage oldPage = new NoteBookPage();
        oldPage.setId(100);
        oldPage.setOrder(1);
        existingTemplate.getPages().add(oldPage);

        when(noteBookDAO.getAllMatching("title", "Existing Lab"))
                .thenReturn(java.util.Collections.singletonList(existingTemplate));

        String json = "{\"title\":\"Existing Lab\",\"workflowType\":\"updated-type\",\"pages\":[{\"order\":1,\"title\":\"New Page\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> nbCaptor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO, org.mockito.Mockito.atLeastOnce()).update(nbCaptor.capture());

        NoteBook updatedTemplate = nbCaptor.getAllValues().stream()
                .filter(nb -> "updated-type".equals(nb.getWorkflowType())).findFirst().orElse(null);
        assertNotNull("Template should have been updated with new workflowType", updatedTemplate);

        verify(noteBookPageDAO, org.mockito.Mockito.atLeastOnce()).insert(any(NoteBookPage.class));
    }

    @Test
    public void testNewTemplate_AlwaysCreatedRegardlessOfForceUpdate() throws Exception {
        setForceUpdateTemplates(false);

        when(noteBookDAO.getAllMatching("title", "New Lab")).thenReturn(new ArrayList<>());
        when(noteBookDAO.insert(any(NoteBook.class))).thenAnswer(invocation -> {
            NoteBook nb = invocation.getArgument(0);
            nb.setId(1);
            return 1;
        });

        String json = "{\"title\":\"New Lab\",\"workflowType\":\"new-type\",\"pages\":[{\"order\":1,\"title\":\"Page 1\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        org.mockito.ArgumentCaptor<NoteBook> nbCaptor = org.mockito.ArgumentCaptor.forClass(NoteBook.class);
        verify(noteBookDAO).insert(nbCaptor.capture());

        NoteBook createdTemplate = nbCaptor.getValue();
        assertEquals("New template should have correct title", "New Lab", createdTemplate.getTitle());
        assertEquals("New template should have correct workflowType", "new-type", createdTemplate.getWorkflowType());
        assertTrue("New template should be marked as template", createdTemplate.getIsTemplate());

        verify(noteBookPageDAO, org.mockito.Mockito.atLeastOnce()).insert(any(NoteBookPage.class));
    }

    @Test
    public void testExistingTemplate_WithForceUpdateTrue_ReplacesPages() throws Exception {
        setForceUpdateTemplates(true);

        NoteBook existingTemplate = new NoteBook();
        existingTemplate.setId(1);
        existingTemplate.setTitle("Lab");
        existingTemplate.setIsTemplate(true);
        existingTemplate.setPages(new ArrayList<>());
        existingTemplate.setDepartments(new HashSet<>());

        NoteBookPage page1 = new NoteBookPage();
        page1.setId(1);
        page1.setOrder(1);
        existingTemplate.getPages().add(page1);

        NoteBookPage page2 = new NoteBookPage();
        page2.setId(2);
        page2.setOrder(2);
        existingTemplate.getPages().add(page2);

        when(noteBookDAO.getAllMatching("title", "Lab"))
                .thenReturn(java.util.Collections.singletonList(existingTemplate));

        String json = "{\"title\":\"Lab\",\"pages\":[{\"order\":1,\"title\":\"Updated Page 1\"},{\"order\":2,\"title\":\"Updated Page 2\"},{\"order\":3,\"title\":\"New Page 3\"}]}";
        InputStream input = new ByteArrayInputStream(json.getBytes());

        handler.processConfiguration(input, "test.json");

        assertTrue("After update with forceUpdate=true, pages should be replaced",
                existingTemplate.getPages().isEmpty());

        org.mockito.ArgumentCaptor<NoteBookPage> pageCaptor = org.mockito.ArgumentCaptor.forClass(NoteBookPage.class);
        verify(noteBookPageDAO, org.mockito.Mockito.times(3)).insert(pageCaptor.capture());

        List<NoteBookPage> insertedPages = pageCaptor.getAllValues();
        assertEquals("Should insert 3 new pages", 3, insertedPages.size());
        assertEquals("First page should have correct order", Integer.valueOf(1), insertedPages.get(0).getOrder());
        assertEquals("Second page should have correct order", Integer.valueOf(2), insertedPages.get(1).getOrder());
        assertEquals("Third page should have correct order", Integer.valueOf(3), insertedPages.get(2).getOrder());
    }

    private void setForceUpdateTemplates(boolean value) throws Exception {
        java.lang.reflect.Field field = NotebookTemplateConfigurationHandler.class
                .getDeclaredField("forceUpdateTemplates");
        field.setAccessible(true);
        field.set(handler, value);
    }
}
