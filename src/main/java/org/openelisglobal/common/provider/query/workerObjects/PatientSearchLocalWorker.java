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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.query.workerObjects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;

public class PatientSearchLocalWorker extends PatientSearchWorker {

    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);

    @Override
    public String createSearchResultXML(String lastName, String firstName, String STNumber, String subjectNumber,
            String nationalID, String patientID, String guid, String dateOfBirth, String gender, StringBuilder xml) {

        String success = IActionConstants.VALID;

        if (GenericValidator.isBlankOrNull(lastName) && GenericValidator.isBlankOrNull(firstName)
                && GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalID) && GenericValidator.isBlankOrNull(patientID)
                && GenericValidator.isBlankOrNull(guid) && GenericValidator.isBlankOrNull(dateOfBirth)
                && GenericValidator.isBlankOrNull(gender)) {

            xml.append("No search terms were entered");
            return IActionConstants.INVALID;
        }

        // N.B. results do not have the referrinngPatientId information but it is not
        // displayed so for now it will be left as null
        List<PatientSearchResults> results = searchResultsService.getSearchResults(lastName, firstName, STNumber,
                subjectNumber, nationalID, nationalID, patientID, guid, dateOfBirth, gender);
        if (!GenericValidator.isBlankOrNull(nationalID)) {
            List<PatientSearchResults> observationResults = getObservationsByReferringPatientId(nationalID);
            results.addAll(observationResults);
        }
        sortPatients(results);

        if (!results.isEmpty()) {
            for (PatientSearchResults singleResult : results) {
                singleResult.setDataSourceName(MessageUtil.getMessage("patient.local.source"));
                appendSearchResultRow(singleResult, xml);
            }
        } else {
            success = IActionConstants.INVALID;

            xml.append("No results were found for search.  Check spelling or remove some of the fields");
        }

        return success;
    }

    @Override
    public List<PatientSearchResults> getPatientSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String patientID, String guid, String dateOfBirth, String gender) {

        if (GenericValidator.isBlankOrNull(lastName) && GenericValidator.isBlankOrNull(firstName)
                && GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalID) && GenericValidator.isBlankOrNull(patientID)
                && GenericValidator.isBlankOrNull(guid) && GenericValidator.isBlankOrNull(dateOfBirth)
                && GenericValidator.isBlankOrNull(gender)) {

            return new ArrayList<>();
        }

        // N.B. results do not have the referrinngPatientId information but it is not
        // displayed so for now it will be left as null
        List<PatientSearchResults> results = searchResultsService.getSearchResults(lastName, firstName, STNumber,
                subjectNumber, nationalID, nationalID, patientID, guid, dateOfBirth, gender);
        if (!GenericValidator.isBlankOrNull(nationalID)) {
            List<PatientSearchResults> observationResults = getObservationsByReferringPatientId(nationalID);
            results.addAll(observationResults);
        }
        sortPatients(results);

        if (!results.isEmpty()) {
            for (PatientSearchResults singleResult : results) {
                singleResult.setDataSourceName(MessageUtil.getMessage("patient.local.source"));
            }
        }

        return results;
    }

    private List<PatientSearchResults> getObservationsByReferringPatientId(String referringId) {
        List<PatientSearchResults> resultList = new ArrayList<>();
        List<ObservationHistory> observationList = SpringContext.getBean(ObservationHistoryService.class)
                .getObservationsByTypeAndValue(ObservationType.REFERRERS_PATIENT_ID, referringId);

        if (observationList != null) {
            for (ObservationHistory observation : observationList) {
                Patient patient = patientService.getData(observation.getPatientId());
                if (patient != null) {
                    resultList.add(getSearchResultsForPatient(patient, referringId));
                }
            }
        }

        return resultList;
    }

    private PatientSearchResults getSearchResultsForPatient(Patient patient, String referringId) {
        PatientService patientPatientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());
        return new PatientSearchResults(BigDecimal.valueOf(Long.parseLong(patient.getId())),
                patientPatientService.getFirstName(patient), patientPatientService.getLastName(patient),
                patientPatientService.getGender(patient), patientPatientService.getEnteredDOB(patient),
                patientPatientService.getNationalId(patient), patient.getExternalId(),
                patientPatientService.getSTNumber(patient), patientPatientService.getSubjectNumber(patient),
                patientPatientService.getGUID(patient), referringId);
    }
}
