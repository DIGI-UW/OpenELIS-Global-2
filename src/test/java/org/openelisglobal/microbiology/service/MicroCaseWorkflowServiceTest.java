package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseWorkflowServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicrobiologyReferenceService referenceService;

    private MicroCaseWorkflowService service;
    private MicroCase microCase;

    @Before
    public void setUp() {
        service = new MicroCaseWorkflowServiceImpl(caseDAO, activityDAO, isolateDAO, referenceService,
                new ObjectMapper());
        microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("sample-item-1");
        microCase.setWorkflowType(MicroWorkflowType.UNASSIGNED.name());
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());
    }

    @Test
    public void classifyUnassignedCasePersistsCompatibleMethodAndAuditHistory() throws Exception {
        compatibleSetup("method-1", MicroWorkflowType.BACTERIOLOGY);
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase updated = service.changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "method-1",
                "Blood culture order", false, "42");

        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), updated.getWorkflowType());
        assertEquals("method-1", updated.getCultureMethodId());
        assertEquals(MicroCaseStage.RECEIVED.name(), updated.getStage());
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.WORKFLOW_CHANGED.name(), activity.getValue().getActivityType());
        assertEquals("42", activity.getValue().getPerformedBy());
        assertEquals("Blood culture order", activity.getValue().getNote());
        assertEquals("UNASSIGNED",
                new ObjectMapper().readTree(activity.getValue().getStructuredData()).get("fromWorkflow").asText());
        assertEquals("BACTERIOLOGY",
                new ObjectMapper().readTree(activity.getValue().getStructuredData()).get("toWorkflow").asText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeWorkflowRejectsIncompatibleMethod() {
        try {
            service.changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "tb-method", "Wrong method", false, "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test(expected = MicroCaseWorkflowConflictException.class)
    public void changeWorkflowRejectsSiblingCollision() {
        compatibleSetup("method-1", MicroWorkflowType.BACTERIOLOGY);
        MicroCase sibling = new MicroCase();
        sibling.setId("case-2");
        when(caseDAO.getBySampleItemAndWorkflow("sample-item-1", "BACTERIOLOGY")).thenReturn(sibling);

        try {
            service.changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "method-1", "Duplicate", false, "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test(expected = MicroCaseWorkflowConflictException.class)
    public void changeWorkflowRequiresConfirmationWhenClinicalWorkExists() {
        compatibleSetup("method-1", MicroWorkflowType.BACTERIOLOGY);
        microCase.setStage(MicroCaseStage.INCUBATING.name());

        try {
            service.changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "method-1", "Correct routing", false,
                    "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test
    public void confirmedChangePreservesExistingStageAndClinicalWork() {
        compatibleSetup("method-2", MicroWorkflowType.MYCOBACTERIOLOGY_TB);
        microCase.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("isolate-1");
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase updated = service.changeWorkflow("case-1", MicroWorkflowType.MYCOBACTERIOLOGY_TB, "method-2",
                "Corrected after setup", true, "42");

        assertEquals(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), updated.getWorkflowType());
        assertEquals(MicroCaseStage.INCUBATING.name(), updated.getStage());
        assertEquals("isolate-1", isolateDAO.getByCaseId("case-1").get(0).getId());
    }

    @Test(expected = MicroCaseLockedException.class)
    public void changeWorkflowRejectsFinalReleasedCase() {
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());

        service.changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "method-1", "Too late", true, "42");
    }

    @Test
    public void requiresPreservationConfirmationReflectsStageOrIsolates() {
        assertTrue(!service.requiresPreservationConfirmation("case-1"));
        microCase.setStage(MicroCaseStage.SETUP_RECORDED.name());
        assertTrue(service.requiresPreservationConfirmation("case-1"));
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(new MicroIsolate()));
        assertTrue(service.requiresPreservationConfirmation("case-1"));
    }

    private void compatibleSetup(String methodId, MicroWorkflowType workflowType) {
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(methodId);
        setup.setWorkflowType(workflowType.name());
        when(referenceService.getActiveCultureSetupForMethod(methodId, workflowType)).thenReturn(setup);
    }
}
