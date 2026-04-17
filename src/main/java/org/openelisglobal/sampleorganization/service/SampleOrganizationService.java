package org.openelisglobal.sampleorganization.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SAMPLE_REQUESTER_VIEW')")
public interface SampleOrganizationService extends BaseObjectService<SampleOrganization, String> {
    void getData(SampleOrganization sampleOrg);

    void getDataBySample(SampleOrganization sampleOrg);

    SampleOrganization getDataBySample(Sample sample);
}
