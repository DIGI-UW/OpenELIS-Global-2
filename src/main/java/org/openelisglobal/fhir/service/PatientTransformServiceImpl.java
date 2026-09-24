package org.openelisglobal.fhir.service;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientTransformServiceImpl implements PatientTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private AddressPartService addressPartService;
    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;
    @Autowired
    private FhirCommonTransformService common;

    @PostConstruct
    private void initializeGlobalVariables() {
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

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId) {
        return transformToFhirPatient(patientService.get(patientId));
    }

    @Override
    public PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient) {
        PatientManagementInfo patient = new PatientManagementInfo();
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOePatientIdentifiers", "setOePatientIdentifiers called");
        for (Identifier identifier : fhirPatient.getIdentifier()) {
            if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/pat_nationalId")) {
                patient.setNationalId(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/pat_subjectNumber")) {
                patient.setSubjectNumber(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/pat_stNumber")) {
                patient.setSTnumber(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/pat_guid")) {
                patient.setGuid(identifier.getValue());
            }
        }
        PatientSearchResults results = transformToOpenElisPatientSearchResults(fhirPatient);
        patient.setFirstName(results.getFirstName());
        patient.setLastName(results.getLastName());
        patient.setGender(results.getGender());
        patient.setBirthDateForDisplay(results.getBirthdate());
        patient.setPatientContact(new PatientContact());

        if (fhirPatient.hasAddress()) {
            Address address = fhirPatient.getAddressFirstRep();
            if (address != null) {
                if (address.hasLine()) {
                    patient.setStreetAddress(
                            address.getLine().stream().map(StringType::getValue).collect(Collectors.joining(", ")));
                }
                if (address.hasCity()) {
                    patient.setCity(address.getCity());
                }
                if (address.hasDistrict()) {
                    patient.setCommune(address.getDistrict());
                }
                if (address.hasState()) {
                    patient.setAddressDepartment(address.getState());
                }
            }
        }

        return patient;

    }

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient", "transformToFhirPatient called");

        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient",
                "transforming patient with id: " + patient.getId());
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        String subjectNumber = patientService.getSubjectNumber(patient);
        String nationalId = patientService.getNationalId(patient);
        String guid = patientService.getGUID(patient);
        String stNumber = patientService.getSTNumber(patient);
        String uuid = patient.getFhirUuidAsString();
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient",
                "transforming patient with id: " + patient.getId() + " fhirUuid: " + uuid);

        fhirPatient.setId(uuid);
        fhirPatient.getMeta().setLastUpdated(patient.getLastupdated());
        fhirPatient.setIdentifier(createPatientIdentifiers(subjectNumber, nationalId, stNumber, guid, uuid));
        Identifier facilityId = common.createFacilityIdentifier();
        if (facilityId != null) {
            fhirPatient.addIdentifier(facilityId);
        }

        HumanName humanName = new HumanName();
        List<HumanName> humanNameList = new ArrayList<>();
        humanName.setFamily(patient.getPerson().getLastName());
        humanName.addGiven(patient.getPerson().getFirstName());
        humanNameList.add(humanName);
        fhirPatient.setName(humanNameList);
        fhirPatient.getNameFirstRep().setUse(HumanName.NameUse.OFFICIAL);

        try {
            if (patient.getBirthDateForDisplay() != null) {
                fhirPatient.setBirthDateElement(common.transformToDateElement(patient.getBirthDateForDisplay()));
            }
        } catch (ParseException e) {
            LogEvent.logError("patient date unparseable '" + patient.getBirthDateForDisplay() + "'", e);
        }
        if (GenericValidator.isBlankOrNull(patient.getGender())) {
            fhirPatient.setGender(AdministrativeGender.UNKNOWN);
        } else if (patient.getGender().equalsIgnoreCase("M")) {
            fhirPatient.setGender(AdministrativeGender.MALE);
        } else {
            fhirPatient.setGender(AdministrativeGender.FEMALE);
        }
        fhirPatient.setTelecom(common.transformToTelecom(patient.getPerson()));

        fhirPatient.addAddress(transformToAddress(patient.getPerson()));

        return fhirPatient;
    }

    @Override
    public PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient) {
        PatientSearchResults patientSearchResults = new PatientSearchResults();

        if (fhirPatient.hasId()) {
            patientSearchResults.setPatientID(fhirPatient.getIdElement().getIdPart());
        }

        for (Identifier identifier : fhirPatient.getIdentifier()) {
            String system = identifier.getSystem();
            String value = identifier.getValue();

            if ("http://openelis-global.org/pat_nationalId".equals(system)) {
                patientSearchResults.setNationalId(value);
            } else if ("http://openelis-global.org/pat_guid".equals(system)) {
                patientSearchResults.setExternalId(value);
            } else if ("http://openelis-global.org/pat_uuid".equals(system)) {
                patientSearchResults.setGUID(value);
            }
        }

        if (!fhirPatient.getName().isEmpty()) {
            HumanName name = fhirPatient.getNameFirstRep();
            patientSearchResults.setFirstName(name.getGivenAsSingleString());
            patientSearchResults.setLastName(name.getFamily());
        }

        switch (fhirPatient.getGender()) {
        case MALE:
            patientSearchResults.setGender("M");
            break;
        case FEMALE:
            patientSearchResults.setGender("F");
            break;
        default:
            patientSearchResults.setGender(null);
            break;
        }

        if (fhirPatient.getBirthDate() != null) {
            patientSearchResults.setBirthdate(
                    DateUtil.convertTimestampToStringDate(new Timestamp(fhirPatient.getBirthDate().getTime())));
        }

        if (!fhirPatient.getTelecom().isEmpty()) {
            ContactPoint telecom = fhirPatient.getTelecomFirstRep();
            if (ContactPointSystem.PHONE.equals(telecom.getSystem())) {
                patientSearchResults.setContactPhone(telecom.getValue());
            }

            if (ContactPointSystem.EMAIL.equals(telecom.getSystem())) {
                patientSearchResults.setContactEmail(telecom.getValue());
            }
        }

        return patientSearchResults;
    }

    private Address transformToAddress(Person person) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToAddress", "transformToAddress called");

        @SuppressWarnings("unused")
        PersonAddress village = null;
        PersonAddress commune = null;
        @SuppressWarnings("unused")
        PersonAddress dept = null;
        List<PersonAddress> personAddressList = personAddressService.getAddressPartsByPersonId(person.getId());

        for (PersonAddress address : personAddressList) {
            if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
                commune = address;
            } else if (address.getAddressPartId().equals(ADDRESS_PART_VILLAGE_ID)) {
                village = address;
            } else if (address.getAddressPartId().equals(ADDRESS_PART_DEPT_ID)) {
                dept = address;
            }
        }
        Address address = new Address() //
                .addLine(person.getStreetAddress()) //
                .setCity(person.getCity()) //
                // .setDistrict(value)
                .setState(person.getState()) //
                // .setPostalCode(value)
                .setCountry(person.getCountry()) //
        ;
        if (commune != null) {
            address.addLine("commune: " + commune.getValue());
        }
        return address;
    }

    private List<Identifier> createPatientIdentifiers(String subjectNumber, String nationalId, String stNumber,
            String guid, String fhirUuid) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToAddress", "transformToAddress called");

        List<Identifier> identifierList = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(subjectNumber)) {
            identifierList
                    .add(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_subjectNumber", subjectNumber));
        }
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            identifierList.add(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_nationalId", nationalId));
        }
        if (!GenericValidator.isBlankOrNull(stNumber)) {
            identifierList.add(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_stNumber", stNumber));
        }
        if (!GenericValidator.isBlankOrNull(guid)) {
            identifierList.add(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_guid", guid));
        }
        if (!GenericValidator.isBlankOrNull(fhirUuid)) {
            identifierList.add(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_uuid", fhirUuid));
        }
        return identifierList;
    }
}
