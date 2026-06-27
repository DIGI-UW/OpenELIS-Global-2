package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseReadinessServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroAstRunDAO astRunDAO;

    private MicroCaseReadinessService service;

    @Before
    public void setUp() {
        service = new MicroCaseReadinessServiceImpl(caseDAO, isolateDAO, astRunDAO);
    }

    @Test
    public void missingIsolateBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("ISOLATE_REQUIRED"));
    }

    @Test
    public void unreviewedAstBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("AST_REVIEW_REQUIRED"));
    }

    @Test
    public void reviewedAstAllowsFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertTrue(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.isEmpty());
    }

    private MicroIsolate significantIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        return isolate;
    }
}
