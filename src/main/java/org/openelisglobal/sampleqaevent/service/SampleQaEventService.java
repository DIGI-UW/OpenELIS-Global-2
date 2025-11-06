package org.openelisglobal.sampleqaevent.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;

public interface SampleQaEventService extends BaseObjectService<SampleQaEvent, String> {
    void getData(SampleQaEvent sampleQaEvent);

    SampleQaEvent getData(String sampleQaEventId);

    List<SampleQaEvent> getAllUncompleatedEvents();

    List<SampleQaEvent> getSampleQaEventsBySample(Sample sample);

    List<SampleQaEvent> getSampleQaEventsBySample(SampleQaEvent sampleQaEvent);

    List<SampleQaEvent> getSampleQaEventsByUpdatedDate(Date lowDate, Date highDate);

    SampleQaEvent getSampleQaEventBySampleAndQaEvent(SampleQaEvent sampleQaEvent);
}
