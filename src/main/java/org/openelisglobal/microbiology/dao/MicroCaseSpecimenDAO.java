package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;

public interface MicroCaseSpecimenDAO extends BaseDAO<MicroCaseSpecimen, String> {
    List<MicroCaseSpecimen> getByCaseId(String caseId);

    List<MicroCaseSpecimen> getByCaseIds(List<String> caseIds);

    MicroCaseSpecimen getByCaseAndSampleItem(String caseId, String sampleItemId);

    boolean hasRecordedResults(String caseId, String sampleItemId);
}
