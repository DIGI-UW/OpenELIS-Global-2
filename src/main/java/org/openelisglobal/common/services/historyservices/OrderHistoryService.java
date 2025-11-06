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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class OrderHistoryService extends AbstractHistoryService {

    protected OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private final String SAMPLE_TABLE_ID;
    private final String SAMPLE_REQUESTER_TABLE_ID;

    private static final String ACCESSION_ATTRIBUTE = "accessionNumber";
    private static final String ORGANIZATION_ATTRIBUTE = "requestingOrganization";
    private static final String PROVIDER_ATTRIBUTE = "requestingProvider";

    private String currentProviderRequestLinkId;

    private String currentOrganizationRequesterLinkId;
    private SampleRequester requester;

    public OrderHistoryService(Sample sample) {
        SAMPLE_TABLE_ID = referenceTablesService.getReferenceTableByName("SAMPLE").getId();
        SAMPLE_REQUESTER_TABLE_ID = referenceTablesService.getReferenceTableByName("SAMPLE_REQUESTER").getId();
        setUpForOrder(sample);
    }

    @SuppressWarnings("unchecked")
    private void setUpForOrder(Sample sample) {
        identifier = sample.getAccessionNumber();

        attributeToIdentifierMap = new HashMap<>();
        attributeToIdentifierMap.put(ORGANIZATION_ATTRIBUTE, MessageUtil.getMessage("organization.organization"));
        attributeToIdentifierMap.put(PROVIDER_ATTRIBUTE,
                MessageUtil.getContextualMessage("nonconformity.provider.label"));
        attributeToIdentifierMap.put(ACCESSION_ATTRIBUTE,
                MessageUtil.getContextualMessage("quick.entry.accession.number"));

        History searchHistory = new History();
        searchHistory.setReferenceId(sample.getId());
        searchHistory.setReferenceTable(SAMPLE_TABLE_ID);
        historyList = historyService.getHistoryByRefIdAndRefTableId(searchHistory);

        newValueMap = new HashMap<>();
        addReferrerHistory(sample, searchHistory);

        newValueMap.put(STATUS_ATTRIBUTE,
                SpringContext.getBean(IStatusService.class).getStatusNameFromId(sample.getStatusId()));
        newValueMap.put(ACCESSION_ATTRIBUTE, sample.getAccessionNumber());
    }

    private void addReferrerHistory(Sample sample, History searchHistory) {
        RequesterService requesterService = new RequesterService(sample.getId());

        requester = requesterService.getOrganizationSampleRequesterByType(false,
                TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        if (requester != null) {
            searchHistory.setReferenceId(requester.getId());
            searchHistory.setReferenceTable(SAMPLE_REQUESTER_TABLE_ID);
            List<History> list = historyService.getHistoryByRefIdAndRefTableId(searchHistory);
            historyList.addAll(list);
            newValueMap.put(ORGANIZATION_ATTRIBUTE, requesterService.getReferringSiteName());
            currentOrganizationRequesterLinkId = requester.getId();
        }

        List<SampleRequester> sampleRequesters = requesterService
                .getSampleRequestersByType(RequesterService.Requester.PERSON, false);
        requester = null;
        if (sampleRequesters.size() != 0) {
            requester = sampleRequesters.get(0);
            searchHistory.setReferenceId(requester.getId());
            searchHistory.setReferenceTable(SAMPLE_REQUESTER_TABLE_ID);
            List<History> list = historyService.getHistoryByRefIdAndRefTableId(searchHistory);
            historyList.addAll(list);
            newValueMap.put(PROVIDER_ATTRIBUTE, requesterService.getRequesterLastFirstName());
            currentProviderRequestLinkId = requester.getId();
        }
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        AuditTrailItem item = getCoreTrail(history);
        if (history.getReferenceTable().equals(SAMPLE_REQUESTER_TABLE_ID)) {
            if (history.getReferenceId().equals(currentProviderRequestLinkId)) {
                item.setIdentifier("Provider");
                item.setNewValue(newValueMap.get(PROVIDER_ATTRIBUTE));
            } else if (history.getReferenceId().equals(currentOrganizationRequesterLinkId)) {
                item.setIdentifier(MessageUtil.getMessage("organization.organization"));
                item.setNewValue(newValueMap.get(ORGANIZATION_ATTRIBUTE));
            }
        }
        items.add(item);
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        String status = extractStatus(changes);
        if (status != null) {
            changeMap.put(STATUS_ATTRIBUTE, status);
        }
        simpleChange(changeMap, changes, ACCESSION_ATTRIBUTE);

        if (history.getReferenceTable().equals(SAMPLE_REQUESTER_TABLE_ID)) {
            if (history.getReferenceId().equals(currentProviderRequestLinkId)) {
                String value = extractSimple(changes, "requesterId");
                if (value != null) {
                    changeMap.put(PROVIDER_ATTRIBUTE,
                            personService.getLastFirstName(personService.getPersonById(value)));
                }
            } else if (history.getReferenceId().equals(currentOrganizationRequesterLinkId)) {
                String value = extractSimple(changes, "requesterId");

                if (value != null) {
                    changeMap.put(ORGANIZATION_ATTRIBUTE,
                            organizationService.getOrganizationById(value).getOrganizationName());
                }
            }
        }
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("auditTrail.order");
    }

    @Override
    protected boolean showAttribute() {
        return false;
    }
}
