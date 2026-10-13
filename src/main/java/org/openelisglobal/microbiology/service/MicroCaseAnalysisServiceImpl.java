package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseAnalysisServiceImpl implements MicroCaseAnalysisService {

    private final MicroCaseAnalysisDAO caseAnalysisDAO;

    public MicroCaseAnalysisServiceImpl(MicroCaseAnalysisDAO caseAnalysisDAO) {
        this.caseAnalysisDAO = caseAnalysisDAO;
    }

    @Override
    @Transactional
    public MicroCaseAnalysis linkAnalysis(MicroCase microCase, Analysis analysis, String reportableTestAnalyteId) {
        if (microCase == null || microCase.getId() == null || analysis == null || analysis.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted microbiology case and analysis are required for report linkage");
        }
        MicroCaseAnalysis owner = caseAnalysisDAO.getActiveByAnalysisId(analysis.getId());
        if (owner != null && !microCase.getId().equals(owner.getCaseId())) {
            throw new IllegalArgumentException("Analysis already belongs to another case");
        }
        MicroCaseAnalysis existing = caseAnalysisDAO.getByCaseAndAnalysis(microCase.getId(), analysis.getId());
        if (existing != null) {
            return existing;
        }
        MicroCaseMutationGuard.requireMutable(microCase);
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setCaseId(microCase.getId());
        link.setAnalysisId(analysis.getId());
        link.setReportableTestAnalyteId(reportableTestAnalyteId);
        caseAnalysisDAO.insert(link);
        return link;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseAnalysis> getCaseAnalyses(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return caseAnalysisDAO.getByCaseId(caseId);
    }
}
