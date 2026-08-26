package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;

public interface MicroCaseAnalysisDAO extends BaseDAO<MicroCaseAnalysis, String> {

    List<MicroCaseAnalysis> getByCaseId(String caseId);

    MicroCaseAnalysis getByCaseAndAnalysis(String caseId, String analysisId);
}
