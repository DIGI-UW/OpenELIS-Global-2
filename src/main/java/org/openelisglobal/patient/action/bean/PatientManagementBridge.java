/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.patient.action.bean;

import java.time.Period;
import java.util.List;
import java.util.Map;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.spring.util.SpringContext;

/** */
public class PatientManagementBridge {

    public String ADDRESS_PART_VILLAGE_ID;
    public String ADDRESS_PART_COMMUNE_ID;
    public String ADDRESS_PART_DEPT_ID;

    AddressPartService addressPartService = SpringContext.getBean(AddressPartService.class);

    public PatientManagementBridge() {
        List<AddressPart> partList = addressPartService.getAll();

        for (AddressPart addressPart : partList) {
            if ("department".equals(addressPart.getPartName())) {
                ADDRESS_PART_DEPT_ID = addressPart.getId();
            } else if ("commune".equals(addressPart.getPartName())) {
                ADDRESS_PART_COMMUNE_ID = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                ADDRESS_PART_VILLAGE_ID = addressPart.getId();
            }
        }
    }

    public PatientManagementInfo getPatientManagementInfoFor(Patient patient, boolean readOnly) {
        PatientManagementInfo info = new PatientManagementInfo();
        info.setReadOnly(readOnly);

        if (patient != null) {
            PatientService patientService = SpringContext.getBean(PatientService.class);
            PersonService personService = SpringContext.getBean(PersonService.class);
            personService.getData(patient.getPerson());
            Map<String, String> addressComponents = patientService.getAddressComponents(patient);
            info.setFirstName(patientService.getFirstName(patient));
            info.setLastName(patientService.getLastName(patient));
            info.setAddressDepartment(addressComponents.get(PatientServiceImpl.ADDRESS_DEPT));
            info.setCommune(addressComponents.get(PatientServiceImpl.ADDRESS_COMMUNE));
            info.setCity(addressComponents.get(PatientServiceImpl.ADDRESS_CITY));
            info.setStreetAddress(addressComponents.get(PatientServiceImpl.ADDRESS_STREET));
            info.setGender(readOnly ? patientService.getLocalizedGender(patient) : patientService.getGender(patient));
            info.setBirthDateForDisplay(patientService.getBirthdayForDisplay(patient));
            info.setNationalId(patientService.getNationalId(patient));
            info.setSTnumber(patientService.getSTNumber(patient));
            info.setSubjectNumber(patientService.getSubjectNumber(patient));
            info.setEducation(patientService.getEducation(patient));

            info.setMaritialStatus(patientService.getMaritalStatus(patient));
            info.setEducation(patientService.getEducation(patient));
            info.setNationality(patientService.getNationality(patient));
            info.setOtherNationality(patientService.getOtherNationality(patient));
            info.setHealthDistrict(patientService.getHealthDistrict(patient));
            info.setHealthRegion(patientService.getHealthRegion(patient));
            info.setPrimaryPhone(patient.getPerson().getPrimaryPhone());
            info.setEmail(patient.getPerson().getEmail());

            info.setMothersInitial(patientService.getMothersInitial(patient));
            if (readOnly) {
                Period period = DateUtil.getPeriodBetweenDates(
                        DateUtil.convertStringDateStringTimeToTimestamp(patientService.getBirthdayForDisplay(patient),
                                null),
                        DateUtil.convertStringDateStringTimeToTimestamp(DateUtil.getCurrentDateAsText(), null));
                info.setAgeYears(String.valueOf(period.getYears()));
                info.setAgeMonths(String.valueOf(period.getMonths()));
                info.setAgeDays(String.valueOf(period.getDays()));
            }
        }

        return info;
    }
}
