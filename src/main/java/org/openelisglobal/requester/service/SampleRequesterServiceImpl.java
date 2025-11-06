package org.openelisglobal.requester.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.requester.dao.SampleRequesterDAO;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleRequesterServiceImpl extends AuditableBaseObjectServiceImpl<SampleRequester, String>
        implements SampleRequesterService {
    @Autowired
    protected SampleRequesterDAO baseObjectDAO;

    SampleRequesterServiceImpl() {
        super(SampleRequester.class);
        this.auditTrailLog = true;
    }

    @Override
    protected SampleRequesterDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRequester> getRequestersForSampleId(String id) {
        return baseObjectDAO.getRequestersForSampleId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRequester> getRequestersForRequesterId(String requesterId, String requesterTypeId) {
        return baseObjectDAO.getRequestersForRequesterId(requesterId, requesterTypeId);
    }
}
