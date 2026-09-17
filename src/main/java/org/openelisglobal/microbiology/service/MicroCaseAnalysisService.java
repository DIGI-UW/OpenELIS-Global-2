package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseAnalysisService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCaseAnalysis linkAnalysis(MicroCase microCase, Analysis analysis, MicroCultureSetup cultureSetup);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCaseAnalysis> getCaseAnalyses(String caseId);
}
