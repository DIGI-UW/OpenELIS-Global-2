package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

public class MicroCaseAnalysisServiceTest {

    private final MicroCaseAnalysisDAO dao = mock(MicroCaseAnalysisDAO.class);
    private final MicroCaseAnalysisServiceImpl service = new MicroCaseAnalysisServiceImpl(dao);

    @Test
    public void releasedCaseReturnsExistingLinkWithoutWriting() {
        MicroCase microCase = releasedCase();
        Analysis analysis = analysis();
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setCaseId(microCase.getId());
        link.setAnalysisId(analysis.getId());
        link.setReportableTestAnalyteId("original-report-target");
        when(dao.getActiveByAnalysisId(analysis.getId())).thenReturn(link);
        when(dao.getByCaseAndAnalysis(microCase.getId(), analysis.getId())).thenReturn(link);

        assertSame(link, service.linkAnalysis(microCase, analysis, "replacement-report-target"));
        assertEquals("original-report-target", link.getReportableTestAnalyteId());

        verify(dao, never()).insert(any(MicroCaseAnalysis.class));
        verify(dao, never()).update(any(MicroCaseAnalysis.class));
    }

    @Test(expected = MicroCaseLockedException.class)
    public void releasedCaseRejectsNewLink() {
        try {
            service.linkAnalysis(releasedCase(), analysis(), null);
        } finally {
            verify(dao, never()).insert(any(MicroCaseAnalysis.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void analysisOwnedByAnotherCaseCannotBeLinked() {
        MicroCaseAnalysis owner = new MicroCaseAnalysis();
        owner.setCaseId("other-case");
        when(dao.getActiveByAnalysisId("42")).thenReturn(owner);

        try {
            service.linkAnalysis(releasedCase(), analysis(), null);
        } finally {
            verify(dao, never()).insert(any(MicroCaseAnalysis.class));
        }
    }

    private MicroCase releasedCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("released-case");
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        return microCase;
    }

    private Analysis analysis() {
        Analysis analysis = new Analysis();
        analysis.setId("42");
        return analysis;
    }
}
