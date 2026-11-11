package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseStateServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroReagentLotService reagentLotService;

    private MicroCaseStateService service;
    private MicroCase microCase;

    @Before
    public void setUp() {
        service = new MicroCaseStateServiceImpl(caseDAO, activityDAO, reagentLotService);
        microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
    }

    @Test
    public void advanceStagePersistsAllowedTransitionAndActivity() {
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase updated = service.advanceStage("case-1", MicroCaseStage.SETUP_RECORDED, "1", "setup done");

        assertEquals(MicroCaseStage.SETUP_RECORDED.name(), updated.getStage());
        verify(caseDAO).update(microCase);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void positiveSignalRemainsDistinctFromObservedGrowth() {
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase positive = service.advanceStage("case-1", MicroCaseStage.POSITIVE_SIGNAL, "1",
                "Bottle flagged positive");
        assertEquals(MicroCaseStage.POSITIVE_SIGNAL.name(), positive.getStage());

        MicroCase growth = service.advanceStage("case-1", MicroCaseStage.GROWTH_DETECTED, "1",
                "Subculture growth observed");
        assertEquals(MicroCaseStage.GROWTH_DETECTED.name(), growth.getStage());
        verify(caseDAO, org.mockito.Mockito.times(2)).update(microCase);
    }

    @Test
    public void noGrowthCanBeRecordedDirectlyFromIncubation() {
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase updated = service.advanceStage("case-1", MicroCaseStage.NO_GROWTH_READY, "42",
                "Incubation complete with no growth");

        assertEquals(MicroCaseStage.NO_GROWTH_READY.name(), updated.getStage());
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.STAGE_CHANGED.name(), activity.getValue().getActivityType());
        assertEquals("42", activity.getValue().getPerformedBy());
        assertEquals("Incubation complete with no growth", activity.getValue().getNote());
        assertEquals("{\"from\":\"INCUBATING\",\"to\":\"NO_GROWTH_READY\"}", activity.getValue().getStructuredData());
        assertNotNull(activity.getValue().getOccurredAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void advanceStageRejectsInvalidTransition() {
        try {
            service.advanceStage("case-1", MicroCaseStage.FINAL_RELEASED, "1", "too soon");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
            verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void advanceStageRejectsChangesAfterFinalRelease() {
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());

        service.advanceStage("case-1", MicroCaseStage.AMENDED, "1", "not supported in MVP");
    }
}
