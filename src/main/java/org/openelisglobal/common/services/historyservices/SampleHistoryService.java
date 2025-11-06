/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services.historyservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;

public class SampleHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private static String SAMPLE_ITEM_TABLE_ID;

    public SampleHistoryService(Sample sample) {
        SAMPLE_ITEM_TABLE_ID = referenceTablesService.getReferenceTableByName("SAMPLE_ITEM").getId();
        setUpForSample(sample);
    }

    @SuppressWarnings("unchecked")
    private void setUpForSample(Sample sample) {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());

        History searchHistory = new History();
        searchHistory.setReferenceTable(SAMPLE_ITEM_TABLE_ID);
        historyList = new ArrayList<History>();

        for (SampleItem item : sampleItems) {
            searchHistory.setReferenceId(item.getId());
            historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
        }

        newValueMap = new HashMap<String, String>();
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        identifier = sampleItemService.getData(history.getReferenceId()).getTypeOfSample().getDescription();
        items.add(getCoreTrail(history));
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        SampleItem item = sampleItemService.getData(history.getReferenceId());
        String statusId = item.getStatusId();
        if (statusId != null) {
            identifier = item.getTypeOfSample().getDescription();
            newValueMap.put(STATUS_ATTRIBUTE,
                    SpringContext.getBean(IStatusService.class).getStatusNameFromId(statusId));
            changeMap.put(STATUS_ATTRIBUTE, "");
        }
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("sample.entry.sampleList.label");
    }
}
