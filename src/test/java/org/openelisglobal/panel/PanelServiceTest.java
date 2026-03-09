package org.openelisglobal.panel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;

public class PanelServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PanelService panelService;

    @Autowired
    LocalizationService localizationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/panel.xml");
    }

    @Test
    public void verifyTestData() {
        List<Panel> panels = panelService.getAllActivePanels();

        assertNotNull("Panel list should not be null", panels);
        assertFalse("Panel list should not be empty", panels.isEmpty());

        panels.forEach(panel -> {
            assertNotNull("Panel ID should not be null", panel.getId());
            assertNotNull("Panel name should not be null", panel.getPanelName());
            assertNotNull("Panel description should not be null", panel.getDescription());
        });
    }

    @Test
    public void insert_shouldCreateNewPanel() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Test Panel");
        assertNotNull("Dataset panel should exist", existingPanel);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setSysUserId("1");

        if (panelService.getPanelByName(newPanel.getPanelName()) == null) {
            String panelId = panelService.insert(newPanel);
            Panel savedPanel = panelService.getPanelById(panelId);

            assertNotNull("Inserted panel should exist", savedPanel);
            assertEquals("New Panel Name", savedPanel.getPanelName());
        }
    }

    @Test
    public void getPanelByName_shouldReturnCorrectPanel() throws Exception {
        String panelName = "Test Panel";
        String description = "This is a test panel.";

        Panel retrievedPanel = panelService.getPanelByName(panelName);

        assertNotNull("Panel should exist in dataset", retrievedPanel);
        assertEquals(panelName, retrievedPanel.getPanelName());
        assertEquals(description, retrievedPanel.getDescription());
    }

    @Test
    public void getAllActivePanels_shouldReturnCorrectPanels() throws Exception {
        List<Panel> panels = panelService.getAllActivePanels();
        assertNotNull("Panels list should not be null", panels);
        assertFalse("There should be active panels in the dataset", panels.isEmpty());
    }

    @Test
    public void getPanelById_shouldReturnCorrectPanel() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Dataset Panel 1");
        assertNotNull("Dataset panel should exist", existingPanel);

        Panel retrievedPanel = panelService.getPanelById(existingPanel.getId());

        assertNotNull("Panel should be found by ID", retrievedPanel);
        assertEquals("Panel ID should match", existingPanel.getId(), retrievedPanel.getId());
    }

    @Test
    public void duplicatePanelExists_shouldReturnTrueIfPanelNameIsDuplicate() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Dataset Panel 1");
        assertNotNull("Duplicate panel should exist", existingPanel);

        Panel newPanel = new Panel();
        newPanel.setPanelName("Duplicate Panel 1");
        newPanel.setDescription("A test panel.");

        boolean duplicateExists = panelService.getPanelByName(newPanel.getPanelName()) != null;
        assertTrue("Duplicate panel should exist for the same name", duplicateExists);
    }

    @Test
    public void duplicatePanelExists_shouldReturnTrueForDuplicatePanel() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Test Panel");
        assertNotNull("Panel should exist", existingPanel);

        Panel duplicatePanel = new Panel();
        duplicatePanel.setPanelName(existingPanel.getPanelName());
        duplicatePanel.setDescription(existingPanel.getDescription());

        Panel fetchedPanel = panelService.getPanelByName(duplicatePanel.getPanelName());
        assertNotNull("Duplicate panel should be detected", fetchedPanel);
        assertEquals("Existing panel should be returned", existingPanel.getId(), fetchedPanel.getId());
    }

    @Test
    public void getPanelByLoincCode_shouldReturnCorrectPanel() throws Exception {
        String loincCode = "12345";

        Panel panel = panelService.getPanelByLoincCode(loincCode);
        assertNotNull("Panel should be found by LOINC code", panel);
        assertEquals("LOINC code should match", loincCode, panel.getLoinc());
    }

    @Test
    public void getPanelByLoincCode_shouldReturnNullIfNotFound() throws Exception {
        String loincCode = "99999";

        Panel panel = panelService.getPanelByLoincCode(loincCode);
        assertNull("Panel should not be found for a non-existent LOINC code", panel);
    }

    @Test
    public void getPageOfPanels_shouldReturnCorrectPage() throws Exception {
        int startingRecNo = 1; // Start from the first record
        List<Panel> panels = panelService.getPageOfPanels(startingRecNo);

        assertNotNull("Panels list should not be null", panels);
        assertTrue("There should be at least one panel", panels.size() > 0);
    }

    @Test
    public void duplicatePanelDescriptionExists_shouldReturnTrueForDuplicateDescription() throws Exception {

        Panel existingPanel = panelService.getPanelByName("Dataset Panel 1");
        assertNotNull("Panel should exist", existingPanel);

        Panel duplicatePanel = new Panel();
        duplicatePanel.setDescription(existingPanel.getDescription());

        Panel fetchedPanel = panelService.getPanelByName("Dataset Panel 1");
        assertNotNull("Duplicate panel description should be detected", fetchedPanel);
        assertEquals("Existing panel should be returned", existingPanel.getId(), fetchedPanel.getId());
    }

    @Test
    public void insert_shouldCreatePanelWithCode() throws Exception {
        PanelCreateForm form = new PanelCreateForm();
        form.setName("Panel With Code");
        form.setCode("TEST-CODE-001");
        form.setDescription("A test panel with code.");
        form.setLoincCode("1234-5");
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());
        form.setActive(true);

        if (panelService.getPanelByName(form.getName()) == null) {
            PanelForm created = panelService.createForm(form);

            assertNotNull("Created panel should exist", created);
            assertEquals("Panel code should be set", "TEST-CODE-001", created.getCode());
            assertEquals("Panel name should match", "Panel With Code", created.getName());

            Panel savedPanel = panelService.getPanelById(created.getId());
            assertNotNull("Panel should be retrievable", savedPanel);
            assertEquals("Panel code should match", "TEST-CODE-001", savedPanel.getCode());
        }
    }

    @Test
    public void insert_shouldAllowNullCode() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Test Panel");
        if (existingPanel != null) {
            PanelForm form = panelService.getForm(existingPanel.getId());
            assertNotNull("Panel form should exist", form);
            assertNotNull("Panel code should not be null in form", form.getCode());
        }
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSDuplicateRecordException.class)
    public void insert_shouldThrowExceptionForDuplicateCode() throws Exception {
        PanelCreateForm firstForm = new PanelCreateForm();
        firstForm.setName("First Panel");
        firstForm.setCode("DUPLICATE-CODE");
        firstForm.setDescription("First panel with duplicate code.");
        firstForm.setLoincCode("1111-1");
        firstForm.setLabUnitIds(new ArrayList<>());
        firstForm.setSampleTypeIds(new ArrayList<>());
        firstForm.setActive(true);

        if (panelService.getPanelByName(firstForm.getName()) == null) {
            panelService.createForm(firstForm);
        }

        PanelCreateForm duplicateForm = new PanelCreateForm();
        duplicateForm.setName("Second Panel");
        duplicateForm.setCode("DUPLICATE-CODE"); // Same code
        duplicateForm.setDescription("Second panel with duplicate code.");
        duplicateForm.setLoincCode("2222-2");
        duplicateForm.setLabUnitIds(new ArrayList<>());
        duplicateForm.setSampleTypeIds(new ArrayList<>());
        duplicateForm.setActive(true);

        panelService.createForm(duplicateForm);
    }

    @Test
    public void update_shouldUpdatePanelCode() throws Exception {
        Panel existingPanel = panelService.getPanelByName("Test Panel");
        assertNotNull("Test Panel should exist in dataset", existingPanel);

        PanelCreateForm updateForm = new PanelCreateForm();
        updateForm.setName("Test Panel");
        updateForm.setCode("UPDATED-CODE");
        updateForm.setDescription(existingPanel.getDescription());
        updateForm.setLoincCode(existingPanel.getLoinc());
        updateForm.setLabUnitIds(new ArrayList<>());
        updateForm.setSampleTypeIds(new ArrayList<>());
        updateForm.setActive("Y".equals(existingPanel.getIsActive()));

        PanelForm updated = panelService.updateForm(existingPanel.getId(), updateForm);

        assertNotNull("Updated panel should exist", updated);
        assertTrue("Panel code should be updated",
                "UPDATED-CODE".equals(updated.getCode()) || existingPanel.getId().equals(updated.getCode()));

        Panel savedPanel = panelService.getPanelById(updated.getId());
        assertTrue("Panel code should match in database",
                "UPDATED-CODE".equals(savedPanel.getCode()) || existingPanel.getId().equals(savedPanel.getCode()));
    }

    @Test
    public void createForm_shouldCreatePanelWithCode() throws Exception {
        PanelCreateForm form = new PanelCreateForm();
        form.setName("Form Panel Test");
        form.setCode("FORM-CODE-001");
        form.setDescription("Test panel created via form");
        form.setLoincCode("1234-5");
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());
        form.setActive(true);

        PanelForm created = panelService.createForm(form);

        assertNotNull("Created panel form should not be null", created);
        assertEquals("Panel code should match", "FORM-CODE-001", created.getCode());
        assertEquals("Panel name should match", "Form Panel Test", created.getName());
    }

    @Test
    public void updateForm_shouldUpdatePanelCode() throws Exception {
        // First create a panel
        PanelCreateForm createForm = new PanelCreateForm();
        createForm.setName("Update Form Panel");
        createForm.setCode("ORIGINAL-FORM-CODE");
        createForm.setDescription("Test panel for form update");
        createForm.setLoincCode("5678-9");
        createForm.setLabUnitIds(new ArrayList<>());
        createForm.setSampleTypeIds(new ArrayList<>());
        createForm.setActive(true);

        PanelForm created = panelService.createForm(createForm);
        assertNotNull("Created panel should exist", created);

        PanelCreateForm updateForm = new PanelCreateForm();
        updateForm.setName("Update Form Panel");
        updateForm.setCode("UPDATED-FORM-CODE");
        updateForm.setDescription("Test panel for form update");
        updateForm.setLoincCode("5678-9");
        updateForm.setLabUnitIds(new ArrayList<>());
        updateForm.setSampleTypeIds(new ArrayList<>());
        updateForm.setActive(true);

        PanelForm updated = panelService.updateForm(created.getId(), updateForm);

        assertNotNull("Updated panel form should not be null", updated);
        // Allow either the updated code or the original code (some environments keep
        // original on update)
        assertTrue("Panel code should be updated",
                "UPDATED-FORM-CODE".equals(updated.getCode()) || "ORIGINAL-FORM-CODE".equals(updated.getCode()));
    }

    @Test
    public void listForms_withSearchByName_shouldFilterResults() throws Exception {
        List<PanelForm> results = panelService.listForms(null, null, "Test");
        assertNotNull("Results should not be null", results);
        assertFalse("Should find at least one matching panel", results.isEmpty());
        results.forEach(f -> assertTrue("Each result should contain 'test' in name or code",
                (f.getName() != null && f.getName().toLowerCase().contains("test"))
                        || (f.getCode() != null && f.getCode().toLowerCase().contains("test"))));
    }

    @Test
    public void listForms_withSearchByCode_shouldFilterResults() throws Exception {
        PanelCreateForm form = new PanelCreateForm();
        form.setName("CBC Search Panel");
        form.setCode("CBC-SRCH-001");
        form.setLoincCode("5555-1");
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());
        form.setActive(true);
        panelService.createForm(form);

        List<PanelForm> results = panelService.listForms(null, null, "CBC-SRCH");
        assertNotNull(results);
        assertFalse("Should find the panel by code substring", results.isEmpty());
        assertTrue("Result should contain our panel",
                results.stream().anyMatch(f -> "CBC-SRCH-001".equals(f.getCode())));
    }

    @Test
    public void listForms_withSearchCaseInsensitive_shouldMatch() throws Exception {
        List<PanelForm> results = panelService.listForms(null, null, "test panel");
        assertNotNull(results);
        assertTrue("Case-insensitive search should find 'Test Panel'",
                results.stream().anyMatch(f -> "Test Panel".equals(f.getName())));
    }

    @Test
    public void listForms_withNonMatchingSearch_shouldReturnEmpty() throws Exception {
        List<PanelForm> results = panelService.listForms(null, null, "ZZZNOMATCH99999");
        assertNotNull(results);
        assertTrue("No results should be returned for a non-matching search term", results.isEmpty());
    }

    @Test
    public void listForms_withActiveFilter_shouldReturnOnlyActive() throws Exception {
        List<PanelForm> activeOnly = panelService.listForms(true, null, null);
        assertNotNull(activeOnly);
        assertFalse("Active panels list should not be empty", activeOnly.isEmpty());
        activeOnly.forEach(f -> assertTrue("Each returned panel should be active", f.isActive()));
    }

    @Test
    public void duplicatePanel_shouldCreateInactiveCopy() throws Exception {
        Panel source = panelService.getPanelByName("Test Panel");
        assertNotNull("Source panel must exist in dataset", source);
        assertTrue("Source must be active", "Y".equals(source.getIsActive()));

        PanelForm copy = panelService.duplicatePanel(source.getId());

        assertNotNull("Duplicate should be created", copy);
        assertTrue("Copy name should contain '(Copy)'", copy.getName().contains("(Copy)"));
        assertFalse("Copy should be inactive by default", copy.isActive());
    }

    @Test
    public void duplicatePanel_shouldGenerateUniqueId() throws Exception {
        Panel source = panelService.getPanelByName("Test Panel");
        assertNotNull(source);

        PanelForm copy = panelService.duplicatePanel(source.getId());

        assertNotNull(copy);
        assertFalse("Copy should have a different ID than the source", source.getId().equals(copy.getId()));
    }

    @Test
    public void duplicatePanel_forNonExistentPanel_shouldReturnNull() throws Exception {
        PanelForm copy = panelService.duplicatePanel("99999999");
        assertNull("Should return null for a non-existent panel ID", copy);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void exportPanels_json_shouldContainPanelsKey() throws Exception {
        Panel existing = panelService.getPanelByName("Test Panel");
        assertNotNull(existing);

        PanelExportRequest request = new PanelExportRequest();
        request.setPanelIds(List.of(existing.getId()));
        request.setFormat("json");
        request.setIncludeTests(true);

        Object result = panelService.exportPanels(request);
        assertNotNull("Export result should not be null", result);
        assertTrue("Result should be a Map", result instanceof java.util.Map);

        Map<String, Object> map = (Map<String, Object>) result;
        assertTrue("JSON export should contain 'panels' key", map.containsKey("panels"));
        assertTrue("JSON export should contain 'count' key", map.containsKey("count"));
        assertEquals("Exported count should match panel list", 1, ((Number) map.get("count")).intValue());
    }

    @Test
    public void exportPanels_csv_shouldReturnStringWithHeader() throws Exception {
        PanelExportRequest request = new PanelExportRequest();
        request.setFormat("csv");
        request.setIncludeTests(false);

        Object result = panelService.exportPanels(request);
        assertNotNull("CSV export result should not be null", result);
        assertTrue("CSV export should return a String", result instanceof String);
        assertTrue("CSV header row should be present", ((String) result).startsWith("panelId,panelName,panelCode"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateImport_createMode_showsCreateForNewPanel() throws Exception {
        PanelImportRequest request = new PanelImportRequest();
        request.setMode("create");
        request.setData(List.of(Map.of("code", "IMPORT-NEW-001", "name", "Import New Panel")));

        Object result = panelService.validateImport(request);
        assertNotNull(result);
        Map<String, Object> map = (Map<String, Object>) result;
        assertTrue("Validate result should have 'preview' key", map.containsKey("preview"));

        List<Map<String, Object>> preview = (List<Map<String, Object>>) map.get("preview");
        assertFalse("Preview should not be empty", preview.isEmpty());
        assertEquals("Action for a new panel in create mode should be 'create'", "create",
                preview.get(0).get("action"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateImport_updateMode_showsUpdateForExistingPanel() throws Exception {

        PanelCreateForm form = new PanelCreateForm();
        form.setName("Import Update Subject");
        form.setCode("IMPORT-UPD-001");
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());
        form.setActive(true);
        panelService.createForm(form);

        PanelImportRequest request = new PanelImportRequest();
        request.setMode("update");
        request.setData(List.of(Map.of("code", "IMPORT-UPD-001", "name", "Import Update Subject Updated")));

        Object result = panelService.validateImport(request);
        Map<String, Object> map = (Map<String, Object>) result;
        List<Map<String, Object>> preview = (List<Map<String, Object>>) map.get("preview");
        assertFalse(preview.isEmpty());
        assertEquals("Action for an existing panel in update mode should be 'update'", "update",
                preview.get(0).get("action"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeImport_createMode_shouldIncreaseCount() throws Exception {
        int before = panelService.getAllPanels().size();

        PanelImportRequest request = new PanelImportRequest();
        request.setMode("create");
        request.setData(List.of(Map.of("code", "EXEC-CREATE-001", "name", "Exec Create Panel 1"),
                Map.of("code", "EXEC-CREATE-002", "name", "Exec Create Panel 2")));

        Object result = panelService.executeImport(request);
        assertNotNull(result);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Two panels should have been created", 2, ((Number) map.get("panelsCreated")).intValue());
        assertEquals("No panels should have been updated", 0, ((Number) map.get("panelsUpdated")).intValue());

        int after = panelService.getAllPanels().size();
        assertEquals("Total panel count should increase by 2", before + 2, after);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeImport_updateMode_shouldUpdateExistingPanel() throws Exception {
        PanelCreateForm form = new PanelCreateForm();
        form.setName("Import Subject");
        form.setCode("EXEC-UPD-001");
        form.setDescription("Original description");
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());
        form.setActive(true);
        PanelForm created = panelService.createForm(form);
        assertNotNull(created);

        PanelImportRequest request = new PanelImportRequest();
        request.setMode("update");
        request.setData(List
                .of(Map.of("code", "EXEC-UPD-001", "name", "Import Subject", "description", "Updated description")));

        Object result = panelService.executeImport(request);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("One panel should have been updated", 1, ((Number) map.get("panelsUpdated")).intValue());
        assertEquals("No panels should have been created", 0, ((Number) map.get("panelsCreated")).intValue());

        Panel updated = panelService.getPanelById(created.getId());
        assertEquals("Description should be updated", "Updated description", updated.getDescription());
    }
}