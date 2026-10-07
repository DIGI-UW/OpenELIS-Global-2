package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseAnalysisServiceImpl implements MicroCaseAnalysisService {

    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final TestService testService;

    public MicroCaseAnalysisServiceImpl(MicroCaseAnalysisDAO caseAnalysisDAO, TestService testService) {
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.testService = testService;
    }

    @Override
    @Transactional
    public MicroCaseAnalysis linkAnalysis(MicroCase microCase, Analysis analysis, String performedBy) {
        if (microCase == null || microCase.getId() == null || analysis == null || analysis.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted microbiology case and analysis are required for report linkage");
        }
        MicroCaseAnalysis existing = caseAnalysisDAO.getByAnalysis(analysis.getId());
        if (existing != null) {
            if (!microCase.getId().equals(existing.getCaseId())) {
                throw new IllegalArgumentException("An existing analysis cannot move to another case");
            }
            return existing;
        }
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        Test catalogTest = analysis.getTest() == null ? null : testService.get(analysis.getTest().getId());
        if (catalogTest == null || !catalogTest.isOpensMicrobiologyCase()) {
            throw new IllegalArgumentException("Only a catalog micro test can join a case");
        }
        link.setCaseId(microCase.getId());
        link.setAnalysisId(analysis.getId());
        link.setCaseRole(MicroCaseTestRole.valueOf(catalogTest.getMicrobiologyCaseRole()).name());
        link.setCollectedInSets(catalogTest.isCollectedInSets());
        link.setSysUserId(performedBy);
        caseAnalysisDAO.insert(link);
        return link;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAnalysis getAnalysisLink(String analysisId) {
        return caseAnalysisDAO.getByAnalysis(analysisId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseAnalysis> getCaseAnalyses(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return caseAnalysisDAO.getByCaseId(caseId);
    }
}
