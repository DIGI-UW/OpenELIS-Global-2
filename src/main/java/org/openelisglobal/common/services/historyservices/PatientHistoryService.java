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
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.spring.util.SpringContext;

public class PatientHistoryService extends AbstractHistoryService {

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private final String PATIENT_TABLE_ID;
    private final String PERSON_TABLE_ID;

    private static final String GENDER_ATTRIBUTE = "gender";
    private static final String DOB_ATTRIBUTE = "birthDateForDisplay";
    private static final String NATIONAL_ID_ATTRIBUTE = "nationalId";
    private static final String EXTERNAL_ID_ATTRIBUTE = "externalId";
    private static final String FIRST_NAME_ATTRIBUTE = "firstName";
    private static final String LAST_NAME_ATTRIBUTE = "lastName";

    public PatientHistoryService(Patient patient) {
        PATIENT_TABLE_ID = referenceTablesService.getReferenceTableByName("PATIENT").getId();
        PERSON_TABLE_ID = referenceTablesService.getReferenceTableByName("PERSON").getId();
        if (patient != null) {
            setUpForPatient(patient);
        }
    }

    @SuppressWarnings("unchecked")
    private void setUpForPatient(Patient patient) {

        historyList = historyService.getHistoryByRefIdAndRefTableId(patient.getId(), PATIENT_TABLE_ID);

        attributeToIdentifierMap = new HashMap<String, String>();
        attributeToIdentifierMap.put(DOB_ATTRIBUTE, MessageUtil.getMessage("patient.birthDate"));
        attributeToIdentifierMap.put(GENDER_ATTRIBUTE, MessageUtil.getMessage("patient.gender"));
        attributeToIdentifierMap.put(NATIONAL_ID_ATTRIBUTE,
                MessageUtil.getMessage("sample.entry.project.subjectNumber"));
        attributeToIdentifierMap.put(FIRST_NAME_ATTRIBUTE, MessageUtil.getMessage("person.firstName"));
        attributeToIdentifierMap.put(LAST_NAME_ATTRIBUTE, MessageUtil.getMessage("person.lastName"));

        newValueMap = new HashMap<String, String>();
        newValueMap.put(GENDER_ATTRIBUTE, patient.getGender());
        newValueMap.put(NATIONAL_ID_ATTRIBUTE, patient.getNationalId());
        newValueMap.put(EXTERNAL_ID_ATTRIBUTE, patient.getExternalId());
        newValueMap.put(DOB_ATTRIBUTE, patient.getBirthDateForDisplay());

        if (patient.getPerson() != null) {
            List<History> personHistory = historyService.getHistoryByRefIdAndRefTableId(patient.getPerson().getId(),
                    PERSON_TABLE_ID);
            historyList.addAll(personHistory);

            newValueMap.put(FIRST_NAME_ATTRIBUTE, patient.getPerson().getFirstName());
            newValueMap.put(LAST_NAME_ATTRIBUTE, patient.getPerson().getLastName());
        }
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        setAndAddIfValueNotNull(items, history, DOB_ATTRIBUTE);
        setAndAddIfValueNotNull(items, history, GENDER_ATTRIBUTE);
        identifier = "Subject No.";
        setAndAddIfValueNotNull(items, history, NATIONAL_ID_ATTRIBUTE);

        identifier = "Site Subject No.";
        setAndAddIfValueNotNull(items, history, EXTERNAL_ID_ATTRIBUTE);
        setAndAddIfValueNotNull(items, history, FIRST_NAME_ATTRIBUTE);
        setAndAddIfValueNotNull(items, history, LAST_NAME_ATTRIBUTE);
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", changes );
        // this may get more complicated

        simpleChange(changeMap, changes, DOB_ATTRIBUTE);
        simpleChange(changeMap, changes, GENDER_ATTRIBUTE);
        simpleChange(changeMap, changes, NATIONAL_ID_ATTRIBUTE);
        simpleChange(changeMap, changes, FIRST_NAME_ATTRIBUTE);
        simpleChange(changeMap, changes, LAST_NAME_ATTRIBUTE);
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("sample.entry.patient");
    }
}
