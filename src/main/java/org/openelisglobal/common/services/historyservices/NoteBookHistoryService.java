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
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.spring.util.SpringContext;

public class NoteBookHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private static String NOTEBOOK_TABLE_ID;

    private static final String TITLE_ATTRIBUTE = "title";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String PROJECT_ATTRIBUTE = "project";
    private static final String OBJECTIVE_ATTRIBUTE = "objective";
    private static final String PROTOCOL_ATTRIBUTE = "protocol";
    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String STATUS_ATTRIBUTE = "status";

    public NoteBookHistoryService(NoteBook noteBook) {
        NOTEBOOK_TABLE_ID = referenceTablesService.getReferenceTableByName("NOTEBOOK").getId();
        setUpForNoteBook(noteBook);
    }

    private void setUpForNoteBook(NoteBook noteBook) {
        identifier = noteBook.getTitle() != null ? noteBook.getTitle() : String.valueOf(noteBook.getId());

        attributeToIdentifierMap = new HashMap<>();
        attributeToIdentifierMap.put(TITLE_ATTRIBUTE, MessageUtil.getMessage("notebook.label.title"));
        attributeToIdentifierMap.put(TYPE_ATTRIBUTE, MessageUtil.getMessage("notebook.label.experimentType"));
        attributeToIdentifierMap.put(PROJECT_ATTRIBUTE, MessageUtil.getMessage("notebook.label.project"));
        attributeToIdentifierMap.put(OBJECTIVE_ATTRIBUTE, MessageUtil.getMessage("notebook.label.objective"));
        attributeToIdentifierMap.put(PROTOCOL_ATTRIBUTE, MessageUtil.getMessage("notebook.label.protocol"));
        attributeToIdentifierMap.put(CONTENT_ATTRIBUTE, MessageUtil.getMessage("notebook.label.content"));
        attributeToIdentifierMap.put(STATUS_ATTRIBUTE, MessageUtil.getMessage("status"));

        History searchHistory = new History();
        searchHistory.setReferenceId(noteBook.getId().toString());
        searchHistory.setReferenceTable(NOTEBOOK_TABLE_ID);
        historyList = new ArrayList<>();
        List<History> retrievedHistory = historyService.getHistoryByRefIdAndRefTableId(searchHistory);
        historyList.addAll(retrievedHistory);

        // Log how many history records were retrieved
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "setUpForNoteBook",
                "Retrieved " + retrievedHistory.size() + " history records for notebook ID: " + noteBook.getId());
        for (History history : retrievedHistory) {
            org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "setUpForNoteBook",
                    "History record - ID: " + history.getId() + ", Activity: " + history.getActivity() + ", Timestamp: "
                            + history.getTimestamp());
        }

        newValueMap = new HashMap<>();
        newValueMap.put(TITLE_ATTRIBUTE, noteBook.getTitle());
        newValueMap.put(TYPE_ATTRIBUTE, noteBook.getType() != null ? noteBook.getType().getDictEntry() : "");
        newValueMap.put(PROJECT_ATTRIBUTE, noteBook.getProject());
        newValueMap.put(OBJECTIVE_ATTRIBUTE, noteBook.getObjective());
        newValueMap.put(PROTOCOL_ATTRIBUTE, noteBook.getProtocol());
        newValueMap.put(CONTENT_ATTRIBUTE, noteBook.getContent());
        newValueMap.put(STATUS_ATTRIBUTE, noteBook.getStatus() != null ? noteBook.getStatus().toString() : "");
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        // For INSERT, show as "created" action with message code
        AuditTrailItem item = getCoreTrail(history);
        item.setAction("notebook.auditTrail.action.created");
        item.setAttribute(""); // No attribute needed for status-only view
        item.setNewValue("");
        item.setOldValue("");
        items.add(item);
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        // Only track status changes - ignore all other field changes
        simpleChange(changeMap, changes, STATUS_ATTRIBUTE);
    }

    @Override
    public List<AuditTrailItem> getAuditTrailItems() {
        // Call parent to get all items, then filter to only status changes
        List<AuditTrailItem> allItems = super.getAuditTrailItems();
        List<AuditTrailItem> statusOnlyItems = new ArrayList<>();

        for (AuditTrailItem item : allItems) {
            // Only include items that are INSERT (created) or status changes
            if ((item.getAction() != null && item.getAction().startsWith("notebook.auditTrail.action."))
                    || STATUS_ATTRIBUTE.equals(item.getAttribute())) {
                // For status changes, map the action to a message code
                if (STATUS_ATTRIBUTE.equals(item.getAttribute())) {
                    String newStatus = item.getNewValue();
                    item.setAction(mapStatusToActionCode(newStatus));
                    item.setAttribute(""); // Clear attribute for status-only view
                    item.setNewValue(""); // Clear values for status-only view
                    item.setOldValue("");
                }
                statusOnlyItems.add(item);
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "getAuditTrailItems", "Filtered to " + statusOnlyItems.size()
                + " status-only audit trail items from " + allItems.size() + " total items");

        return statusOnlyItems;
    }

    @Override
    protected void addItemsForKeys(List<AuditTrailItem> items, History history, Map<String, String> changeMaps) {
        // Only add items for status changes
        if (changeMaps.containsKey(STATUS_ATTRIBUTE)) {
            setIdentifierForKey(STATUS_ATTRIBUTE);
            AuditTrailItem item = getCoreTrail(history);
            item.setAttribute(STATUS_ATTRIBUTE);
            item.setOldValue(changeMaps.get(STATUS_ATTRIBUTE));
            item.setNewValue(newValueMap.get(STATUS_ATTRIBUTE));
            newValueMap.put(STATUS_ATTRIBUTE, item.getOldValue());
            if (item.newOldDiffer()) {
                items.add(item);
            }
        }
        // Ignore all other field changes
    }

    /**
     * Maps NoteBook status to action message code for audit trail display
     */
    private String mapStatusToActionCode(String status) {
        if (status == null) {
            return "notebook.auditTrail.action.updated";
        }

        // Map status enum values to message codes
        String statusUpper = status.toUpperCase();
        if (statusUpper.contains("DRAFT")) {
            return "notebook.auditTrail.action.created";
        } else if (statusUpper.contains("SUBMITTED")) {
            return "notebook.auditTrail.action.submitted";
        } else if (statusUpper.contains("FINALIZED")) {
            return "notebook.auditTrail.action.finalized";
        } else if (statusUpper.contains("LOCKED")) {
            return "notebook.auditTrail.action.locked";
        } else if (statusUpper.contains("ARCHIVED")) {
            return "notebook.auditTrail.action.archived";
        }

        // Default to "updated" if status doesn't match known values
        return "notebook.auditTrail.action.updated";
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("notebook.heading.notebooks");
    }

    @Override
    protected boolean showAttribute() {
        return true;
    }
}
