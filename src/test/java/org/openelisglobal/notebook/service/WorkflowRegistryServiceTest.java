package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.notebook.valueholder.WorkflowStageDefinition;

public class WorkflowRegistryServiceTest {

    private WorkflowRegistryService service;

    @Before
    public void setUp() {
        service = new WorkflowRegistryService();
        service.replaceRegistry(List.of(
                new WorkflowStageDefinition("Biorepository Laboratory", "biorepository", 1, "intake", "intake",
                        "Sample Intake & Registration", List.of("Sample Collector", "Laboratory Technician"),
                        org.openelisglobal.notebook.valueholder.NotebookStageAction.DEFAULT_STAGE_ACTIONS),
                new WorkflowStageDefinition("Biorepository Laboratory", "biorepository", 6, "qc", "qc",
                        "QC Inspection", List.of("Laboratory Technician", "Lab Manager"),
                        org.openelisglobal.notebook.valueholder.NotebookStageAction.DEFAULT_STAGE_ACTIONS)));
    }

    @Test
    public void returnsPersonasForKnownStage() {
        List<String> personas = service.getAllowedPersonas("biorepository", 1);
        assertEquals(2, personas.size());
        assertTrue(personas.contains("Sample Collector"));
    }

    @Test
    public void resolveAllowedPersonasFailsClosedWhenUnknown() {
        List<String> resolved = service.resolveAllowedPersonas("unknown", 1, 1, List.of());
        assertTrue(resolved.isEmpty());
    }

    @Test
    public void filtersExplicitPageRolesToSrsPersonasOnly() {
        List<String> resolved = service.resolveAllowedPersonas("biorepository", 1, 1,
                List.of("Sample Collector", "Storage Manager"));
        assertEquals(1, resolved.size());
        assertEquals("Sample Collector", resolved.get(0));
    }

    @Test
    public void normalizesWorkflowTypeKeys() {
        assertTrue(service.getStagesForWorkflowType("").isEmpty());
        assertEquals(2, service.getStagesForWorkflowType("BIOREPOSITORY").size());
    }

    @Test
    public void resolvesPersonasByPageKey() {
        List<String> personas = service.resolveAllowedPersonas("biorepository", null, null, "intake", List.of());
        assertTrue(personas.contains("Sample Collector"));
    }

    @Test
    public void permitsViewEditCompleteForRegistryStage() {
        assertTrue(service.isActionPermitted("biorepository", "intake", 1,
                org.openelisglobal.notebook.valueholder.NotebookStageAction.VIEW));
        assertTrue(service.isActionPermitted("biorepository", "intake", 1,
                org.openelisglobal.notebook.valueholder.NotebookStageAction.COMPLETE));
    }
}
