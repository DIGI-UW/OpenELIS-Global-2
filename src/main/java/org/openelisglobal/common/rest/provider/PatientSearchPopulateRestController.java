package org.openelisglobal.common.rest.provider;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.rest.provider.bean.PatientInfoBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class PatientSearchPopulateRestController {

    @Autowired
    PatientService patientService;

    @Autowired
    PatientContactService patientContactService;

    @Autowired
    AddressPartService addressPartService;

    @Autowired
    PersonAddressService personAddressService;

    @Autowired
    PatientPatientTypeService patientPatientTypeService;

    private String ADDRESS_PART_VILLAGE_ID;

    private String ADDRESS_PART_COMMUNE_ID;

    private String ADDRESS_PART_DEPT_ID;

    @GetMapping(value = "patient-details", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PatientInfoBean getPatientResults(@RequestParam String patientID) {

        if (!GenericValidator.isBlankOrNull(patientID)) {
            return getPatientDetails(getPatientForID(patientID));
        } else {
            return new PatientInfoBean();
        }
    }

    private Patient getPatientForID(String patientID) {

        Patient patient = new Patient();
        patient.setId(patientID);

        patientService.getData(patient);
        if (patient.getId() == null) {
            return null;
        } else {
            return patient;
        }
    }

    private PatientInfoBean getPatientDetails(Patient patient) {
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

        Person person = patient.getPerson();

        PatientIdentityTypeMap identityMap = PatientIdentityTypeMap.getInstance();

        List<PatientIdentity> identityList = PatientUtil.getIdentityListForPatient(patient.getId());
        List<PatientContact> patientContacts = patientContactService.getForPatient(patient.getId());

        String city = getAddress(person, ADDRESS_PART_VILLAGE_ID);
        if (GenericValidator.isBlankOrNull(city)) {
            city = person.getCity();
        }
        String commune = getAddress(person, ADDRESS_PART_COMMUNE_ID);
        String dept = getAddress(person, ADDRESS_PART_DEPT_ID);

        PatientInfoBean patientInfo = new PatientInfoBean();
        patientInfo.setPatientPK(patient.getId());
        patientInfo.setNationalId(patient.getNationalId());
        patientInfo.setSTnumber(identityMap.getIdentityValue(identityList, "ST"));
        patientInfo.setSubjectNumber(identityMap.getIdentityValue(identityList, "SUBJECT"));
        patientInfo.setLastName(getLastNameForResponse(person));
        patientInfo.setFirstName(person.getFirstName());
        patientInfo.setMothersName(identityMap.getIdentityValue(identityList, "MOTHER"));
        patientInfo.setAka(identityMap.getIdentityValue(identityList, "AKA"));
        patientInfo.setStreetAddress(person.getStreetAddress());
        patientInfo.setCity(city);
        patientInfo.setPrimaryPhone(person.getPrimaryPhone());
        patientInfo.setEmail(person.getEmail());
        patientInfo.setGender(patient.getGender());
        patientInfo.setPatientType(getPatientType(patient));
        patientInfo.setInsuranceNumber(identityMap.getIdentityValue(identityList, "INSURANCE"));
        patientInfo.setOccupation(identityMap.getIdentityValue(identityList, "OCCUPATION"));
        String format1 = "dd/MM/yyyy";
        String format2 = "MM/dd/yyyy";
        patientInfo.setBirthDateForDisplay(
                ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE).equals("fr-FR")
                        ? DateUtil.formatStringDate(patient.getBirthDateForDisplay(), format1)
                        : DateUtil.formatStringDate(patient.getBirthDateForDisplay(), format2));
        patientInfo.setCommune(commune);
        patientInfo.setAddressDepartment(dept);
        patientInfo.setMothersInitial(identityMap.getIdentityValue(identityList, "MOTHERS_INITIAL"));
        patientInfo.setEducation(identityMap.getIdentityValue(identityList, "EDUCATION"));
        patientInfo.setMaritialStatus(identityMap.getIdentityValue(identityList, "MARITIAL"));
        patientInfo.setNationality(identityMap.getIdentityValue(identityList, "NATIONALITY"));
        patientInfo.setOtherNationality(identityMap.getIdentityValue(identityList, "OTHER NATIONALITY"));
        patientInfo.setHealthDistrict(identityMap.getIdentityValue(identityList, "HEALTH DISTRICT"));
        patientInfo.setHealthRegion(identityMap.getIdentityValue(identityList, "HEALTH REGION"));
        patientInfo.setGuid(identityMap.getIdentityValue(identityList, "GUID"));
        if (patientContacts.size() >= 1) {
            PatientContact contact = patientContacts.get(0);
            patientInfo.setPatientContact(contact);
        }

        if (patient.getLastupdated() != null) {
            patientInfo.setPatientLastUpdated(patient.getLastupdated().toString());
        }
        if (person.getLastupdated() != null) {
            patientInfo.setPersonLastUpdated(person.getLastupdated().toString());
        }
        return patientInfo;
    }

    private String getAddress(Person person, String addressPartId) {
        if (GenericValidator.isBlankOrNull(addressPartId)) {
            return "";
        }
        PersonAddress address = personAddressService.getByPersonIdAndPartId(person.getId(), addressPartId);

        return address != null ? address.getValue() : "";
    }

    /**
     * Fake the unknown patient by never return whatever happens to be in last name
     * field.
     *
     * @param person
     * @return
     */
    private String getLastNameForResponse(Person person) {
        if (PatientUtil.getUnknownPerson().getId().equals(person.getId())) {
            return null;
        } else {
            return person.getLastName();
        }
    }

    private String getPatientType(Patient patient) {
        PatientType patientType = patientPatientTypeService.getPatientTypeForPatient(patient.getId());
        return patientType != null ? patientType.getType() : null;
    }
}
