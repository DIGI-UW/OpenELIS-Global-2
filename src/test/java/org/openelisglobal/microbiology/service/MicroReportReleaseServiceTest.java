package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
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
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

@RunWith(MockitoJUnitRunner.class)
public class MicroReportReleaseServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroCaseReadinessService readinessService;

    private MicroReportReleaseService service;

    @Before
    public void setUp() {
        service = new MicroReportReleaseServiceImpl(caseDAO, activityDAO, readinessService);
    }

    @Test
    public void finalReleaseIsBlockedUntilReadinessPasses() {
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = false;
        readiness.blockers.add("AST_REVIEW_REQUIRED");
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);

        try {
            service.releaseFinal("case-1", "1");
        } catch (IllegalStateException expected) {
            assertEquals("Final release is blocked: AST_REVIEW_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void finalReleaseUpdatesCaseAndRecordsHistory() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase released = service.releaseFinal("case-1", "1");

        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), released.getFinalReleaseState());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), released.getStage());
        verify(activityDAO).insert(org.mockito.ArgumentMatchers.any());
    }
}
