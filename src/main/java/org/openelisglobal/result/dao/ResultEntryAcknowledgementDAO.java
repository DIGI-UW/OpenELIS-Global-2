package org.openelisglobal.result.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.result.valueholder.ResultEntryAcknowledgement;

public interface ResultEntryAcknowledgementDAO extends BaseDAO<ResultEntryAcknowledgement, String> {

    /** Every acknowledgement recorded against the analysis, oldest first. */
    List<ResultEntryAcknowledgement> getByAnalysisId(String analysisId);
}
