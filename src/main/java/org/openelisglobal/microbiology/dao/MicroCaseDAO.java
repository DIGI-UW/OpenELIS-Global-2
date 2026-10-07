package org.openelisglobal.microbiology.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroCaseDAO extends BaseDAO<MicroCase, String> {

    List<MicroCase> getBySampleItem(String sampleItemId);

    List<MicroCase> getOpenCases();

    List<MicroCase> getFinalizedBacteriologyByCollectionDateRange(Timestamp fromInclusive, Timestamp toExclusive);
}
