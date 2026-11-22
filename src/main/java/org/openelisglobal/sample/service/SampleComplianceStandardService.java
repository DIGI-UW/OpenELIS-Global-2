package org.openelisglobal.sample.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;

public interface SampleComplianceStandardService extends BaseObjectService<SampleComplianceStandard, Long> {

    List<SampleComplianceStandard> getAllForSample(String sampleId);

    void replaceAllForSample(String sampleId, List<SampleComplianceStandard> standards);
}
