package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.notebook.valueholder.WorkflowStageDefinition;

public class WorkflowRegistryServiceTest {

    private WorkflowRegistryService service;

    @Before
    public void setUp() {
        service = new WorkflowRegistryService();
        service.replaceRegistry(List.of(
                stage("pathology", 9, "microscopy", "Junior Researcher", "Senior Researcher"),
                stage("pathology", 10, "report_print", "Lab Manager", "Senior Researcher")));
    }

    @Test
    public void normalizeWorkflowType_mapsPathologySubtypesToPathology() {
        assertEquals("pathology", WorkflowRegistryService.normalizeWorkflowType("fnac"));
        assertEquals("pathology",
                WorkflowRegistryService.normalizeWorkflowType("cytology_liquid_based_pap_smear"));
        assertEquals("pathology",
                WorkflowRegistryService.normalizeWorkflowType("histopathology_biopsy_tissue"));
    }

    @Test
    public void getAllowedPersonas_resolvesPathologyRegistryForFnacSubtype() {
        List<String> personas = service.getAllowedPersonas("fnac", 9);
        assertTrue(personas.contains("Junior Researcher"));
        assertTrue(personas.contains("Senior Researcher"));
    }

    @Test
    public void isActionPermitted_resolvesPathologyRegistryForCytologySubtype() {
        assertTrue(service.isActionPermitted("cytology_liquid_based_pap_smear", "report_print", 10,
                NotebookStageAction.VIEW));
    }

    @Test
    public void isKnownWorkflowType_acceptsPathologySubtypes() {
        assertTrue(service.isKnownWorkflowType("fnac"));
        assertFalse(service.isKnownWorkflowType("unknown_lab"));
    }

    private static WorkflowStageDefinition stage(String workflowType, int order, String pageKey,
            String... personas) {
        return new WorkflowStageDefinition("Pathology Laboratory", workflowType, order, pageKey, pageKey,
                pageKey, List.of(personas), EnumSet.allOf(NotebookStageAction.class));
    }
}
