package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;

public interface MicroCaseAnalysisService {

    MicroCaseAnalysis linkAnalysis(MicroCase microCase, Analysis analysis, String reportableTestAnalyteId);

    List<MicroCaseAnalysis> getCaseAnalyses(String caseId);
}
