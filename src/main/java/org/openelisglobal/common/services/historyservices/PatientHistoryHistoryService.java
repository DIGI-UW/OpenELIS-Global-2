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
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.ObservationHistoryTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;

public class PatientHistoryHistoryService extends AbstractHistoryService {

    protected ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);
    protected SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);

    private static String OBSERVATION_HISTORY_TABLE_ID;
    private static String SAMPLE_ORG_TABLE_ID;

    private static final String ORGANIZATION_ATTRIBUTE = "organization";
    private static final String REFERRING_PATIENT_ID_ATTRIBUTE = "referrersPatientId";

    public PatientHistoryHistoryService(Sample sample) {
        OBSERVATION_HISTORY_TABLE_ID = referenceTablesService.getReferenceTableByName("observation_history").getId();
        SAMPLE_ORG_TABLE_ID = referenceTablesService.getReferenceTableByName("SAMPLE_ORGANIZATION").getId();
        setUpForPatientHistory(sample);
    }

    @SuppressWarnings("unchecked")
    private void setUpForPatientHistory(Sample sample) {
        attributeToIdentifierMap = new HashMap<String, String>();
        attributeToIdentifierMap.put(ORGANIZATION_ATTRIBUTE, "Referring Organization");
        attributeToIdentifierMap.put(REFERRING_PATIENT_ID_ATTRIBUTE,
                MessageUtil.getMessage("sample.referring.patientNumber"));

        newValueMap = new HashMap<String, String>();
        historyList = new ArrayList<History>();

        List<ObservationHistory> observationList = observationHistoryService
                .getObservationHistoriesBySampleId(sample.getId());
        History history = new History();
        for (ObservationHistory observation : observationList) {
            newValueMap.put(observation.getId(), getObservationValue(observation));

            history.setReferenceId(observation.getId());
            history.setReferenceTable(OBSERVATION_HISTORY_TABLE_ID);
            historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(history));
        }

        identifier = sample.getAccessionNumber();

        SampleOrganization sampleOrg = sampleOrganizationService.getDataBySample(sample);
        if (sampleOrg != null) {
            newValueMap.put(ORGANIZATION_ATTRIBUTE, sampleOrg.getOrganization().getOrganizationName());
            history.setReferenceId(sampleOrg.getId());
            history.setReferenceTable(SAMPLE_ORG_TABLE_ID);
            List<History> orgHistory = historyService.getHistoryByRefIdAndRefTableId(history);
            historyList.addAll(orgHistory);
        }
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        ObservationHistory observation = new ObservationHistory();
        observation.setId(history.getReferenceId());
        observation = observationHistoryService.getById(observation);
        if (observation != null) {
            identifier = ObservationHistoryTypeMap.getInstance()
                    .getTypeFromId(observation.getObservationHistoryTypeId());
            setIdentifierForKey(identifier);
            AuditTrailItem item = getCoreTrail(history);
            item.setNewValue(getObservationValue(observation));
            items.add(item);
        } else {
            setAndAddIfValueNotNull(items, history, ORGANIZATION_ATTRIBUTE);
        }
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {

        String status = extractStatus(changes);
        if (status != null) {
            changeMap.put(STATUS_ATTRIBUTE, status);
        }
        String value = extractSimple(changes, "value");
        if (value != null) {
            value = getCorrectValueForHistory(history, value);

            changeMap.put(VALUE_ATTRIBUTE, value);
        }

        String orgString = extractSimple(changes, ORGANIZATION_ATTRIBUTE);
        if (orgString != null) {
            String[] orgParts = orgString.split(", ");
            for (String part : orgParts) {
                if (part.startsWith("organizationName")) {
                    changeMap.put(ORGANIZATION_ATTRIBUTE, part.split("=")[1]);
                }
            }
        }
    }

    private String getCorrectValueForHistory(History history, String value) {
        ObservationHistory obsHistory = new ObservationHistory();
        obsHistory.setId(history.getReferenceId());
        obsHistory = observationHistoryService.getById(obsHistory);
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // obsHistory.getObservationHistoryTypeId() + " : " + value);
        if ("D".equals(obsHistory.getValueType())) {
            return dictionaryService.getDataForId(value).getDictEntry();
        }

        return value;
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("patient.history");
    }

    protected void addItemsForKeys(List<AuditTrailItem> items, History history, Map<String, String> changeMaps) {
        for (String key : changeMaps.keySet()) {
            if (key == VALUE_ATTRIBUTE) {
                setIdentifierForObservation(history);
            } else {
                setIdentifierForKey(key);
            }

            AuditTrailItem item = getCoreTrail(history);
            if (showAttribute()) {
                item.setAttribute(key);
            }

            String observationKey = history.getReferenceId();

            item.setOldValue(changeMaps.get(key));
            item.setNewValue(newValueMap.get(observationKey));
            newValueMap.put(observationKey, item.getOldValue());
            item.setAttribute(showAttribute() && !GenericValidator.isBlankOrNull(key) ? key
                    : MessageUtil.getMessage("auditTrail.action.update"));
            if (item.newOldDiffer()) {
                items.add(item);
            }
        }
    }

    private void setIdentifierForObservation(History history) {
        ObservationHistory observation = new ObservationHistory();
        observation.setId(history.getReferenceId());
        observation = observationHistoryService.getById(observation);
        identifier = ObservationHistoryTypeMap.getInstance().getTypeFromId(observation.getObservationHistoryTypeId());
        setIdentifierForKey(identifier);
    }

    protected String getObservationValue(ObservationHistory observation) {
        if ("D".equals(observation.getValueType())) {
            Dictionary dict = dictionaryService.getDataForId(observation.getValue());
            return dict != null ? dict.getDictEntry() : observation.getValue();
        }

        return observation.getValue();
    }
}
