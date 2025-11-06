package org.openelisglobal.sampleqaevent.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.dao.SampleQaEventDAO;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleQaEventServiceImpl extends AuditableBaseObjectServiceImpl<SampleQaEvent, String>
        implements SampleQaEventService {
    @Autowired
    protected SampleQaEventDAO baseObjectDAO;

    SampleQaEventServiceImpl() {
        super(SampleQaEvent.class);
        this.auditTrailLog = true;
    }

    @Override
    protected SampleQaEventDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsBySample(Sample sample) {
        return baseObjectDAO.getAllMatching("sample.id", sample.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleQaEvent sampleQaEvent) {
        getBaseObjectDAO().getData(sampleQaEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaEvent getData(String sampleQaEventId) {
        return getBaseObjectDAO().getData(sampleQaEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getAllUncompleatedEvents() {
        return getBaseObjectDAO().getAllUncompleatedEvents();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsBySample(SampleQaEvent sampleQaEvent) {
        return getBaseObjectDAO().getSampleQaEventsBySample(sampleQaEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsByUpdatedDate(Date lowDate, Date highDate) {
        return getBaseObjectDAO().getSampleQaEventsByUpdatedDate(lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaEvent getSampleQaEventBySampleAndQaEvent(SampleQaEvent sampleQaEvent) {
        return getBaseObjectDAO().getSampleQaEventBySampleAndQaEvent(sampleQaEvent);
    }
}
