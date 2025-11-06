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
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public class AnalysisHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private String ANALYSIS_TABLE_ID;

    public AnalysisHistoryService(Analysis analysis) {
        ANALYSIS_TABLE_ID = referenceTablesService.getReferenceTableByName("ANALYSIS").getId();
        setUpForAnalysis(analysis);
    }

    @SuppressWarnings("unchecked")
    private void setUpForAnalysis(Analysis analysis) {
        if (analysis.getTest() != null) {
            History searchHistory = new History();
            searchHistory.setReferenceId(analysis.getId());
            searchHistory.setReferenceTable(ANALYSIS_TABLE_ID);
            historyList = historyService.getHistoryByRefIdAndRefTableId(searchHistory);

            newValueMap = new HashMap<String, String>();
            newValueMap.put(STATUS_ATTRIBUTE,
                    SpringContext.getBean(IStatusService.class).getStatusNameFromId(analysis.getStatusId()));

            identifier = TestServiceImpl.getLocalizedTestNameWithType(analysis.getTest()) + " - "
                    + analysis.getAnalysisType();
        } else {
            historyList = new ArrayList<History>();
        }
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        items.add(getCoreTrail(history));
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        String status = extractStatus(changes);
        if (status != null) {
            changeMap.put(STATUS_ATTRIBUTE, status);
        }
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getContextualMessage("sample.entry.test");
    }

    @Override
    protected boolean showAttribute() {
        return true;
    }
}
