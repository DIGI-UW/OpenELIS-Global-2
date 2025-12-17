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
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.spring.util.SpringContext;

/**
 * History service for NoteBookPage entity. Tracks changes to page content,
 * including QC parameters and other configuration data.
 */
public class NoteBookPageHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private static String NOTEBOOK_PAGE_TABLE_ID;

    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String TITLE_ATTRIBUTE = "title";
    private static final String ORDER_ATTRIBUTE = "order";

    public NoteBookPageHistoryService(NoteBookPage page) {
        NOTEBOOK_PAGE_TABLE_ID = referenceTablesService.getReferenceTableByName("NOTEBOOK_PAGE").getId();
        setUpForPage(page);
    }

    private void setUpForPage(NoteBookPage page) {
        identifier = page.getTitle() != null ? page.getTitle() : "Page " + page.getId();

        attributeToIdentifierMap = new HashMap<>();
        attributeToIdentifierMap.put(CONTENT_ATTRIBUTE,
                MessageUtil.getMessageOrDefault("notebook.page.content", null, "Content"));
        attributeToIdentifierMap.put(TITLE_ATTRIBUTE,
                MessageUtil.getMessageOrDefault("notebook.page.title", null, "Title"));
        attributeToIdentifierMap.put(ORDER_ATTRIBUTE,
                MessageUtil.getMessageOrDefault("notebook.page.order", null, "Order"));

        History searchHistory = new History();
        searchHistory.setReferenceId(page.getId().toString());
        searchHistory.setReferenceTable(NOTEBOOK_PAGE_TABLE_ID);
        historyList = new ArrayList<>();
        List<History> retrievedHistory = historyService.getHistoryByRefIdAndRefTableId(searchHistory);
        historyList.addAll(retrievedHistory);

        newValueMap = new HashMap<>();
        newValueMap.put(CONTENT_ATTRIBUTE, page.getContent());
        newValueMap.put(TITLE_ATTRIBUTE, page.getTitle());
        newValueMap.put(ORDER_ATTRIBUTE, page.getOrder() != null ? page.getOrder().toString() : "");
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        AuditTrailItem item = getCoreTrail(history);
        item.setAction(MessageUtil.getMessageOrDefault("notebook.page.auditTrail.action.created", null, "Created"));
        item.setAttribute("");
        item.setNewValue("");
        item.setOldValue("");
        items.add(item);
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        // Track content changes (includes QC parameters)
        simpleChange(changeMap, changes, CONTENT_ATTRIBUTE);
        simpleChange(changeMap, changes, TITLE_ATTRIBUTE);
    }

    @Override
    protected void addItemsForKeys(List<AuditTrailItem> items, History history, Map<String, String> changeMaps) {
        // Add items for content changes
        if (changeMaps.containsKey(CONTENT_ATTRIBUTE)) {
            setIdentifierForKey(CONTENT_ATTRIBUTE);
            AuditTrailItem item = getCoreTrail(history);
            item.setAttribute(attributeToIdentifierMap.get(CONTENT_ATTRIBUTE));
            item.setOldValue(changeMaps.get(CONTENT_ATTRIBUTE));
            item.setNewValue(newValueMap.get(CONTENT_ATTRIBUTE));
            newValueMap.put(CONTENT_ATTRIBUTE, item.getOldValue());
            if (item.newOldDiffer()) {
                items.add(item);
            }
        }

        // Add items for title changes
        if (changeMaps.containsKey(TITLE_ATTRIBUTE)) {
            setIdentifierForKey(TITLE_ATTRIBUTE);
            AuditTrailItem item = getCoreTrail(history);
            item.setAttribute(attributeToIdentifierMap.get(TITLE_ATTRIBUTE));
            item.setOldValue(changeMaps.get(TITLE_ATTRIBUTE));
            item.setNewValue(newValueMap.get(TITLE_ATTRIBUTE));
            newValueMap.put(TITLE_ATTRIBUTE, item.getOldValue());
            if (item.newOldDiffer()) {
                items.add(item);
            }
        }
    }

    /**
     * Get content-only history items. Returns audit trail items filtered to only
     * include content changes (QC parameters, etc.).
     *
     * @return list of content change audit trail items
     */
    public List<AuditTrailItem> getContentHistoryItems() {
        List<AuditTrailItem> allItems = getAuditTrailItems();
        List<AuditTrailItem> contentItems = new ArrayList<>();

        for (AuditTrailItem item : allItems) {
            String attr = item.getAttribute();
            if (attr != null && (attr.equals(attributeToIdentifierMap.get(CONTENT_ATTRIBUTE))
                    || item.getAction().contains("Created"))) {
                contentItems.add(item);
            }
        }

        return contentItems;
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessageOrDefault("notebook.page.heading", null, "Notebook Page");
    }

    @Override
    protected boolean showAttribute() {
        return true;
    }
}
