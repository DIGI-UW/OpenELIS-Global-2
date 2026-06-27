package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseStateServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    private MicroCaseStateService service;
    private MicroCase microCase;

    @Before
    public void setUp() {
        service = new MicroCaseStateServiceImpl(caseDAO, activityDAO);
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

    @Test(expected = IllegalArgumentException.class)
    public void advanceStageRejectsInvalidTransition() {
        try {
            service.advanceStage("case-1", MicroCaseStage.FINAL_RELEASED, "1", "too soon");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
            verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
        }
    }
}
