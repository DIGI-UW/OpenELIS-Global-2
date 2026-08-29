package org.openelisglobal.sample.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;

public interface SampleComplianceStandardDAO extends BaseDAO<SampleComplianceStandard, Long> {

    List<SampleComplianceStandard> getAllForSample(String sampleId);

    void deleteAllForSample(String sampleId);
}
