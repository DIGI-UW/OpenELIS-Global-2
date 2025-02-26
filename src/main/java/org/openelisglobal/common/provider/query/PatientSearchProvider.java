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
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchLocalAndExternalWorker;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchLocalWorker;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchWorker;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;

public class PatientSearchProvider extends BaseQueryProvider {

    protected AjaxServlet ajaxServlet = null;

    SampleService sampleService = SpringContext.getBean(SampleService.class);
    SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String lastName = request.getParameter("lastName");
        String firstName = request.getParameter("firstName");
        String STNumber = request.getParameter("STNumber");
        // N.B. This is a bad name, it is other than STnumber
        String subjectNumber = request.getParameter("subjectNumber");
        String nationalID = request.getParameter("nationalID");
        String labNumber = request.getParameter("labNumber");
        String guid = request.getParameter("guid");
        String dateOfBirth = request.getParameter("dateOfBirth");
        String gender = request.getParameter("gender");
        String suppressExternalSearch = request.getParameter("suppressExternalSearch");
        String patientID = null;

        String result = VALID;
        StringBuilder xml = new StringBuilder();
        // If we have a lab number then the patient is in the system and we just
        // have to get the patient and format the xml
        if (!GenericValidator.isBlankOrNull(labNumber)) {
            Patient patient = getPatientForLabNumber(labNumber);
            if (patient == null || GenericValidator.isBlankOrNull(patient.getId())) {
                result = IActionConstants.INVALID;
                xml.append("No results were found for search.  Check spelling or remove some of the fields");
            } else {
                PatientSearchResults searchResults = getSearchResultsForPatient(patient);
                PatientSearchWorker localWorker = new PatientSearchLocalWorker();
                localWorker.appendSearchResultRow(searchResults, xml);
            }
        } else {

            PatientSearchWorker worker = getAppropriateWorker(request, "true".equals(suppressExternalSearch));

            if (worker != null) {
                result = worker.createSearchResultXML(lastName, firstName, STNumber, subjectNumber, nationalID,
                        patientID, guid, dateOfBirth, gender, xml);
            } else {
                result = INVALID;
                xml.append("System is not configured correctly for searching for patients. Contact Administrator");
            }
        }
        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private PatientSearchResults getSearchResultsForPatient(Patient patient) {
        PatientService patientPatientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());
        return new PatientSearchResults(BigDecimal.valueOf(Long.parseLong(patient.getId())),
                patientPatientService.getFirstName(patient), patientPatientService.getLastName(patient),
                patientPatientService.getGender(patient), patientPatientService.getEnteredDOB(patient),
                patientPatientService.getNationalId(patient), patient.getExternalId(),
                patientPatientService.getSTNumber(patient), patientPatientService.getSubjectNumber(patient),
                patientPatientService.getGUID(patient),
                SpringContext.getBean(ObservationHistoryService.class).getMostRecentValueForPatient(
                        ObservationType.REFERRERS_PATIENT_ID, patientPatientService.getPatientId(patient)));
    }

    private Patient getPatientForLabNumber(String labNumber) {

        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);

        if (sample != null && !GenericValidator.isBlankOrNull(sample.getId())) {
            return sampleHumanService.getPatientForSample(sample);
        }

        return new Patient();
    }

    private PatientSearchWorker getAppropriateWorker(HttpServletRequest request, boolean suppressExternalSearch) {

        if (ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.UseExternalPatientInfo,
                "false") || suppressExternalSearch) {
            return new PatientSearchLocalWorker();
        } else {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            return new PatientSearchLocalAndExternalWorker(String.valueOf(usd.getSystemUserId()));
        }
    }

    @Override
    public void setServlet(AjaxServlet as) {
        ajaxServlet = as;
    }

    @Override
    public AjaxServlet getServlet() {
        return ajaxServlet;
    }
}
