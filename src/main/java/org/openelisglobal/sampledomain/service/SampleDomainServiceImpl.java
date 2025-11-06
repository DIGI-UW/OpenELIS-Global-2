package org.openelisglobal.sampledomain.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampledomain.dao.SampleDomainDAO;
import org.openelisglobal.sampledomain.valueholder.SampleDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleDomainServiceImpl extends AuditableBaseObjectServiceImpl<SampleDomain, String>
        implements SampleDomainService {
    @Autowired
    protected SampleDomainDAO baseObjectDAO;

    SampleDomainServiceImpl() {
        super(SampleDomain.class);
    }

    @Override
    protected SampleDomainDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
