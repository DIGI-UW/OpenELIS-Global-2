package org.openelisglobal.fhir.serviceImpl;

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
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.fhir.service.PatientTransformService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientTransformServiceImpl implements PatientTransformService {
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;
    @Autowired
    private PatientService patientService;

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
        fhirPatient.setIdentifier(
                fhirTransformUtil.createPatientIdentifiers(subjectNumber, nationalId, stNumber, guid, uuid));
        Identifier facilityId = fhirTransformUtil.createFacilityIdentifier();
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
                fhirPatient.setBirthDateElement(
                        fhirTransformUtil.transformToDateElement(patient.getBirthDateForDisplay()));
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
        fhirPatient.setTelecom(fhirTransformUtil.transformToTelecom(patient.getPerson()));

        fhirPatient.addAddress(fhirTransformUtil.transformToAddress(patient.getPerson()));

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

}
