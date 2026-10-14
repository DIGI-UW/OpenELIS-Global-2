package org.openelisglobal.sampleorganization.service;

import java.util.Collection;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;

public interface SampleOrganizationService extends BaseObjectService<SampleOrganization, String> {
    void getData(SampleOrganization sampleOrg);

    void getDataBySample(SampleOrganization sampleOrg);

    SampleOrganization getDataBySample(Sample sample);

    Map<String, String> getOrganizationNamesBySampleIds(Collection<String> sampleIds);
}
