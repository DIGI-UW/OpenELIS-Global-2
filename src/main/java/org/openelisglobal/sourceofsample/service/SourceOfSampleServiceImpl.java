package org.openelisglobal.sourceofsample.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sourceofsample.dao.SourceOfSampleDAO;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SourceOfSampleServiceImpl extends AuditableBaseObjectServiceImpl<SourceOfSample, String>
        implements SourceOfSampleService {
    @Autowired
    protected SourceOfSampleDAO baseObjectDAO;

    SourceOfSampleServiceImpl() {
        super(SourceOfSample.class);
    }

    @Override
    protected SourceOfSampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
