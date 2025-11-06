package org.openelisglobal.sampletracking.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampletracking.dao.SampleTrackingDAO;
import org.openelisglobal.sampletracking.valueholder.SampleTracking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleTrackingServiceImpl extends AuditableBaseObjectServiceImpl<SampleTracking, String>
        implements SampleTrackingService {
    @Autowired
    protected SampleTrackingDAO baseObjectDAO;

    SampleTrackingServiceImpl() {
        super(SampleTracking.class);
    }

    @Override
    protected SampleTrackingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
