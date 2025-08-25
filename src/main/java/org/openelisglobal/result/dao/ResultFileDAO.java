package org.openelisglobal.result.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.result.valueholder.ResultFile;

public interface ResultFileDAO extends BaseDAO<ResultFile, String> {
    ResultFile getFileByAnalysisId(String analysisId);

    void customInsert(ResultFile resultFile);

    void customUpdate(ResultFile resultFile);

    void customDelete(ResultFile resultFile);
}