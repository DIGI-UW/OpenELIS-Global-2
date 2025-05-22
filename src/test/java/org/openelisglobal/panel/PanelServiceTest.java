package org.openelisglobal.panel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;

public class PanelServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PanelService panelService;

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
}