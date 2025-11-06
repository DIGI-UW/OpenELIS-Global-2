package org.openelisglobal.reports.service.send.sample;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.reports.send.sample.dao.SampleTransmissionSequenceDAO;
import org.openelisglobal.reports.send.sample.valueholder.SampleTransmissionSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleTransmissionSequenceServiceImpl
        extends AuditableBaseObjectServiceImpl<SampleTransmissionSequence, String>
        implements SampleTransmissionSequenceService {
    @Autowired
    protected SampleTransmissionSequenceDAO baseObjectDAO;

    SampleTransmissionSequenceServiceImpl() {
        super(SampleTransmissionSequence.class);
    }

    @Override
    protected SampleTransmissionSequenceDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
