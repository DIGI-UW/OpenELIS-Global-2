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
package org.openelisglobal.patient.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.gender.valueholder.Gender;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.validation.Errors;

public class PatientUtil {

    private static Patient UNKNOWN_PATIENT;
    private static Person UNKNOWN_PERSON;
    private static Provider UNKNOWN_PROVIDER;
    private static PatientService patientService = SpringContext.getBean(PatientService.class);
    private static PatientIdentityService patientIdentityService = SpringContext.getBean(PatientIdentityService.class);

    private static void initializeUnknowns() {
        PersonService personService = SpringContext.getBean(PersonService.class);
        UNKNOWN_PERSON = personService.getPersonByLastName("UNKNOWN_");
        if (UNKNOWN_PERSON == null) {
            UNKNOWN_PERSON = new Person();
            UNKNOWN_PERSON.setSysUserId("1");
            UNKNOWN_PERSON.setLastName("UNKNOWN_");
            personService.insert(UNKNOWN_PERSON);
        }

        ProviderService providerService = SpringContext.getBean(ProviderService.class);
        UNKNOWN_PROVIDER = providerService.getProviderByPerson(UNKNOWN_PERSON);

        if (UNKNOWN_PROVIDER == null) {
            UNKNOWN_PROVIDER = new Provider();
            UNKNOWN_PROVIDER.setSysUserId("1");
            UNKNOWN_PROVIDER.setPerson(UNKNOWN_PERSON);
            providerService.insert(UNKNOWN_PROVIDER);
        }
        UNKNOWN_PROVIDER.setActive(false);

        UNKNOWN_PATIENT = patientService.getPatientByPerson(UNKNOWN_PERSON);

        if (UNKNOWN_PATIENT == null) {
            UNKNOWN_PATIENT = new Patient();
            UNKNOWN_PATIENT.setSysUserId("1");
            UNKNOWN_PATIENT.setPerson(UNKNOWN_PERSON);
            patientService.insert(UNKNOWN_PATIENT);
        }
    }

    public static String getDisplayDOBForPatient(String patientId, String defaultValue) {
        Patient patient = patientService.getData(patientId);
        if (patient != null) {
            return patient.getBirthDateForDisplay();
        }

        return defaultValue;
    }

    public static List<PatientIdentity> getIdentityListForPatient(String patientId) {
        PatientIdentityService identityService = SpringContext.getBean(PatientIdentityService.class);
        return identityService.getPatientIdentitiesForPatient(patientId);
    }

    public static List<Gender> findGenders() {
        return SpringContext.getBean(GenderService.class).getAll();
    }

    public static List<PatientIdentity> getIdentityListForPatient(Patient patient) {
        if (patient != null) {
            PatientIdentityService identityService = SpringContext.getBean(PatientIdentityService.class);
            return identityService.getPatientIdentitiesForPatient(patient.getId());
        } else {
            return new ArrayList<>();
        }
    }

    public static void invalidateUnknownPatients() {
        UNKNOWN_PATIENT = null;
        UNKNOWN_PERSON = null;
        UNKNOWN_PROVIDER = null;
    }

    public static Patient getUnknownPatient() {
        if (UNKNOWN_PATIENT == null) {
            initializeUnknowns();
        }
        return UNKNOWN_PATIENT;
    }

    public static Person getUnknownPerson() {
        if (UNKNOWN_PERSON == null) {
            initializeUnknowns();
        }
        return UNKNOWN_PERSON;
    }

    public static Provider getUnownProvider() {
        if (UNKNOWN_PROVIDER == null) {
            initializeUnknowns();
        }
        return UNKNOWN_PROVIDER;
    }

    public static Patient getPatientByIdentificationNumber(String id) {
        Patient patient = patientService.getPatientByNationalId(id);

        if (patient == null) {
            patient = patientService.getPatientByExternalId(id);
        }

        return patient;
    }

    public static Patient getPatientForSample(Sample sample) {
        return SpringContext.getBean(SampleHumanService.class).getPatientForSample(sample);
    }

    public static void preparePatientData(Errors errors, HttpServletRequest request, PatientManagementInfo patientInfo,
            Patient patient) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        ValidatePatientInfo.validatePatientInfo(errors, patientInfo);
        if (errors.hasErrors()) {
            return;
        }

        initMembers(patient);
        patientInfo.setPatientIdentities(new ArrayList<PatientIdentity>());

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE) {
            Patient dbPatient = loadForUpdate(patientInfo);
            PropertyUtils.copyProperties(patient, dbPatient);
        }

        copyFormBeanToValueHolders(patientInfo, patient);

        setSystemUserID(patientInfo, patient, request);

        setLastUpdatedTimeStamps(patientInfo, patient);
    }

    public static void copyFormBeanToValueHolders(PatientManagementInfo patientInfo, Patient patient)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PropertyUtils.copyProperties(patient, patientInfo);
        PropertyUtils.copyProperties(patient.getPerson(), patientInfo);
    }

    public static void setSystemUserID(PatientManagementInfo patientInfo, Patient patient, HttpServletRequest request) {
        patient.setSysUserId(ControllerUtills.getSysUserId(request));
        patient.getPerson().setSysUserId(ControllerUtills.getSysUserId(request));

        for (PatientIdentity identity : patientInfo.getPatientIdentities()) {
            identity.setSysUserId(ControllerUtills.getSysUserId(request));
        }
        patientInfo.getPatientContact().setSysUserId(ControllerUtills.getSysUserId(request));
    }

    public static void initMembers(Patient patient) {
        patient.setPerson(new Person());
    }

    public static void setLastUpdatedTimeStamps(PatientManagementInfo patientInfo, Patient patient) {
        String patientUpdate = patientInfo.getPatientLastUpdated();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(patientUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(patientUpdate);
            patient.setLastupdated(timeStamp);
        }

        String personUpdate = patientInfo.getPersonLastUpdated();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(personUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(personUpdate);
            patient.getPerson().setLastupdated(timeStamp);
        }
    }

    public static Patient loadForUpdate(PatientManagementInfo patientInfo) {
        Patient patient = patientService.get(patientInfo.getPatientPK());
        patientInfo.setPatientIdentities(patientIdentityService.getPatientIdentitiesForPatient(patient.getId()));
        return patient;
    }
}
