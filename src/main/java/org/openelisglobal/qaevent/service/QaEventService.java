package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.QaEvent;

public interface QaEventService extends BaseObjectService<QaEvent, String> {
    void getData(QaEvent qaEvent);

    QaEvent getQaEventByName(QaEvent qaEvent);

    List<QaEvent> getQaEvents(String filter);

    List<QaEvent> getAllQaEvents();

    Integer getTotalQaEventCount();

    List<QaEvent> getPageOfQaEvents(int startingRecNo);
}
