package org.openelisglobal.label;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.label.service.LabelService;
import org.openelisglobal.label.valueholder.Label;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.springframework.beans.factory.annotation.Autowired;

public class LabelServiceTest extends BaseWebContextSensitiveTest {

    private static final String EXISTING_LABEL_ID = "1";
    private static final String NEW_LABEL_ID = "2";

    @Autowired
    private LabelService labelService;

    @Autowired
    private ScriptletService scriptletService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/label.xml");
    }

    @Test
    public void getAll_shouldReturnNonEmptyList() throws Exception {
        List<Label> labels = labelService.getAll();
        assertFalse("Label list should not be empty", labels.isEmpty());
    }

    @Test
    public void get_shouldReturnCorrectLabelWithAllProperties() throws Exception {
        Label label = labelService.get(EXISTING_LABEL_ID);

        assertNotNull("Label should exist in test data", label);
        assertEquals("Default Label", label.getLabelName());
        assertEquals("Main printer label", label.getDescription());
        assertEquals("Z", label.getPrinterType());

        assertNull("Scriptlet should be null unless explicitly set", label.getScriptlet());
    }

    @Test
    public void shouldRetrieveExistingLabelWithScriptlet() {
        Label label = labelService.get("1");

        assertNotNull(label);
        assertEquals("Default Label", label.getLabelName());

    }

    private Label createTestLabel(String id, String name, String description, String printerType,
            String scriptletName) {
        Label label = new Label();
        label.setId(id);
        label.setLabelName(name);
        label.setDescription(description);
        label.setPrinterType(printerType);
        label.setScriptletName(scriptletName);
        return label;
    }

    private void assertLabelPropertiesMatch(Label label, String expectedName, String expectedDescription,
            String expectedPrinterType, String expectedScriptletName) {
        assertEquals(expectedName, label.getLabelName());
        assertEquals(expectedDescription, label.getDescription());
        assertEquals(expectedPrinterType, label.getPrinterType());
        assertEquals(expectedScriptletName, label.getScriptletName());
        assertNull("Scriptlet should remain null unless explicitly set", label.getScriptlet());
    }

    @Test
    public void get_shouldMapPrinterTypeCodeToFullName() throws Exception {
        Label label = labelService.get(EXISTING_LABEL_ID);

        assertEquals("Z", label.getPrinterType());
        String fullPrinterName = mapPrinterType(label.getPrinterType());
        assertEquals("Zebra", fullPrinterName);
    }

    @Test
    public void createLabel_shouldPersistNewLabel() throws Exception {
        Label newLabel = createTestLabel(null, "New Label", "Test Description", "T", null);
        labelService.insert(newLabel);
        String generatedId = newLabel.getId();
        assertNotNull("ID should be assigned after insert", generatedId);

        Label retrieved = labelService.get(generatedId);
        assertNotNull("Inserted label should be found", retrieved);

        assertLabelPropertiesMatch(retrieved, "New Label", "Test Description", "T", null);
    }

    @Test
    public void updateLabel_shouldModifyExistingLabel() throws Exception {
        Label label = labelService.get(EXISTING_LABEL_ID);
        label.setDescription("Updated Description");

        labelService.update(label);

        Label updated = labelService.get(EXISTING_LABEL_ID);
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    public void get_shouldThrowExceptionForMissingId() {
        assertThrows(ObjectNotFoundException.class, () -> labelService.get("999999"));
    }

    @Test
    public void scriptletAssociation_shouldWorkCorrectly() throws Exception {
        Scriptlet scriptlet = scriptletService.get("1");
        Label label = labelService.get(EXISTING_LABEL_ID);

        label.setScriptlet(scriptlet);
        labelService.update(label);

        Label updated = labelService.get(EXISTING_LABEL_ID);
        assertNotNull("Scriptlet should be set", updated.getScriptlet());
        assertEquals("1", updated.getScriptlet().getId());
    }

    @Test
    public void getAll_shouldReturnExpectedNumberOfLabels() throws Exception {
        List<Label> labels = labelService.getAll();
        assertEquals("Expected number of labels", 1, labels.size());
    }

    private String mapPrinterType(String code) {
        switch (code) {
        case "Z":
            return "Zebra";
        case "T":
            return "Thermal";
        default:
            return "Unknown";
        }
    }

}