package org.openelisglobal.common.services.historyservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class QaHistoryService extends AbstractHistoryService {

    protected SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    public QaHistoryService(Sample sample) {
        setUpForSample(sample);
    }

    @SuppressWarnings("unchecked")
    private void setUpForSample(Sample sample) {
        SampleService sampleSampleService = SpringContext.getBean(SampleService.class);
        List<SampleQaEvent> qaEventList = sampleSampleService.getSampleQAEventList(sample);

        History searchHistory = new History();
        searchHistory.setReferenceTable(QAService.TABLE_REFERENCE_ID);
        historyList = new ArrayList<>();

        for (SampleQaEvent event : qaEventList) {
            searchHistory.setReferenceId(event.getId());
            historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
        }

        newValueMap = new HashMap<>();
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        identifier = sampleQaEventService.getData(history.getReferenceId()).getQaEvent().getLocalizedName();
        items.add(getCoreTrail(history));
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("qaevent.browse.title");
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        changeMap.put(STATUS_ATTRIBUTE, "Gail");
    }
}
