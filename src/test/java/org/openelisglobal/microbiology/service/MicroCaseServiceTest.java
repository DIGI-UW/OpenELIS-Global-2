package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Test
    public void createOrGetCaseReturnsExistingCaseWithoutDuplicateActivity() {
        MicroCase existing = new MicroCase();
        existing.setSampleItemId("1001");
        existing.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        when(caseDAO.getBySampleItemAndWorkflow("1001", MicroWorkflowType.BACTERIOLOGY.name())).thenReturn(existing);

        MicroCaseService service = new MicroCaseServiceImpl(caseDAO, activityDAO, isolateDAO);
        MicroCase result = service.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");

        assertEquals(existing, result);
        verify(caseDAO, never()).insert(any(MicroCase.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createOrGetCaseCreatesReceivedCaseAndTimelineActivity() {
        MicroCaseService service = new MicroCaseServiceImpl(caseDAO, activityDAO, isolateDAO);

        MicroCase result = service.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");

        assertEquals("1001", result.getSampleItemId());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), result.getWorkflowType());
        assertEquals(MicroCaseStage.RECEIVED.name(), result.getStage());
        assertNotNull(result.getCreatedAt());
        verify(caseDAO).insert(result);
        ArgumentCaptor<MicroCaseActivity> activityCaptor = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activityCaptor.capture());
        assertEquals(result.getId(), activityCaptor.getValue().getCaseId());
        assertEquals(MicroCaseActivityType.CASE_CREATED.name(), activityCaptor.getValue().getActivityType());
    }
}
