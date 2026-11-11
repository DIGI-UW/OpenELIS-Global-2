package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface MicroCaseSampleDAO extends BaseDAO<MicroCaseSample, String> {

    MicroCaseSample getActiveByCaseAndSample(String caseId, String sampleItemId);

    List<MicroCaseSample> getByCaseId(String caseId);

    SampleItem lockSampleItem(String sampleItemId);

    List<String> getRelatedCaseIds(String caseId);
}
