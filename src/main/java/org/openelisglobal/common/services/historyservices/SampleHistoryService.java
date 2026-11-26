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
import org.apache.commons.validator.GenericValidator;
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

/**
 * The order audit trail's sample item rows: each item's status and remaining
 * quantity changes, the old value read from the history row and the new one
 * from the next row, or from the item as it is now for the latest change.
 */
public class SampleHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private final String sampleItemTableId;
    private final List<SampleItem> sampleItems;
    private final String statusLabel = MessageUtil.getMessage("auditTrail.sampleItem.status");
    private final String remainingQuantityLabel = MessageUtil.getMessage("auditTrail.sampleItem.remainingQuantity");

    public SampleHistoryService(Sample sample) {
        sampleItemTableId = referenceTablesService.getReferenceTableByName("SAMPLE_ITEM").getId();
        sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
    }

    @Override
    public List<AuditTrailItem> getAuditTrailItems() {
        List<AuditTrailItem> items = new ArrayList<>();
        for (SampleItem item : sampleItems) {
            History searchHistory = new History();
            searchHistory.setReferenceTable(sampleItemTableId);
            searchHistory.setReferenceId(item.getId());
            historyList = new ArrayList<>(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
            identifier = itemIdentifier(item);
            newValueMap = new HashMap<>();
            newValueMap.put(statusLabel, statusName(item.getStatusId()));
            newValueMap.put(remainingQuantityLabel,
                    item.getRemainingQuantity() == null ? "" : item.getRemainingQuantity().toPlainString());
            items.addAll(super.getAuditTrailItems());
        }
        return items;
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        items.add(getCoreTrail(history));
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        String statusId = extractSimple(changes, "statusId");
        if (statusId != null) {
            changeMap.put(statusLabel, statusName(statusId));
        }
        String remainingQuantity = extractSimple(changes, "remainingQuantity");
        if (remainingQuantity != null) {
            changeMap.put(remainingQuantityLabel, remainingQuantity);
        }
    }

    private static String itemIdentifier(SampleItem item) {
        String type = item.getTypeOfSample() == null ? "" : item.getTypeOfSample().getDescription();
        if (GenericValidator.isBlankOrNull(item.getExternalId())) {
            return type;
        }
        return GenericValidator.isBlankOrNull(type) ? item.getExternalId() : type + " (" + item.getExternalId() + ")";
    }

    private static String statusName(String statusId) {
        if (statusId == null) {
            return null;
        }
        String name = SpringContext.getBean(IStatusService.class).getStatusNameFromId(statusId);
        return name != null ? name : statusId;
    }

    @Override
    protected boolean showAttribute() {
        return true;
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("sample.entry.sampleList.label");
    }
}
