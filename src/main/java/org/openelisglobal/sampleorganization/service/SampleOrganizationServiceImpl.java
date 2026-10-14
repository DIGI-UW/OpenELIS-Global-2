package org.openelisglobal.sampleorganization.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.dao.SampleOrganizationDAO;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleOrganizationServiceImpl extends AuditableBaseObjectServiceImpl<SampleOrganization, String>
        implements SampleOrganizationService {
    @Autowired
    protected SampleOrganizationDAO baseObjectDAO;

    SampleOrganizationServiceImpl() {
        super(SampleOrganization.class);
        this.auditTrailLog = true;
    }

    @Override
    protected SampleOrganizationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleOrganization sampleOrg) {
        getBaseObjectDAO().getData(sampleOrg);
    }

    @Override
    @Transactional(readOnly = true)
    public void getDataBySample(SampleOrganization sampleOrg) {
        getBaseObjectDAO().getDataBySample(sampleOrg);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleOrganization getDataBySample(Sample sample) {
        return getBaseObjectDAO().getDataBySample(sample);
    }
}
