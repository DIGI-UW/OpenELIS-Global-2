package org.openelisglobal.microbiology.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroCaseDAO extends BaseDAO<MicroCase, String> {

    void lockOrder(String sampleId);

    MicroCase getForUpdate(String caseId);

    List<MicroCase> getRoutingCandidates(String sampleId, String sampleTypeId, String testSectionId,
            String collectedInSetsTestId, String sampleItemId);

    List<MicroCase> getByOrder(String sampleId);

    List<MicroCase> getBySampleItem(String sampleItemId);

    List<MicroCase> getBySampleItemIds(List<String> sampleItemIds);

    List<MicroCase> getOpenCases(boolean allUnits, java.util.Set<String> unitIds);

    List<MicroCase> getFinalizedForExportByCollectionDateRange(String exportKey, Timestamp fromInclusive,
            Timestamp toExclusive);
}
