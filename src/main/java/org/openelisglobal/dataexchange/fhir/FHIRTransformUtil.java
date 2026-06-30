package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestPriority;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FHIRTransformUtil {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private TestService testService;

    @Autowired
    private AddressPartService addressPartService;

    @Autowired
    private FhirFacilityOrganizationService facilityOrganizationService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private OrganizationService organizationService;

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    @PostConstruct
    public void initializeGlobalVariables() {
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

    public void addTelecomToPerson(List<ContactPoint> telecoms, Person person) {
        for (ContactPoint contact : telecoms) {
            String contactValue = contact.getValue();
            if (ContactPointSystem.EMAIL.equals(contact.getSystem())) {
                person.setEmail(contactValue);
            } else if (ContactPointSystem.FAX.equals(contact.getSystem())) {
                person.setFax(contactValue);
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.MOBILE.equals(contact.getUse())) {
                person.setCellPhone(contactValue);
                person.setPrimaryPhone(contactValue);
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.HOME.equals(contact.getUse())) {
                person.setHomePhone(contactValue);
                if (GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
                    person.setPrimaryPhone(contactValue);
                }
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.WORK.equals(contact.getUse())) {
                person.setWorkPhone(contactValue);
                if (GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
                    person.setPrimaryPhone(contactValue);
                }
            }
        }
    }

    public void addHumanNameToPerson(HumanName humanName, Person person) {
        person.setFirstName(
                humanName.getGivenAsSingleString() == null ? "" : humanName.getGivenAsSingleString().strip());
        person.setLastName(humanName.getFamily() == null ? "" : humanName.getFamily().strip());
    }

    public Identifier createIdentifier(String system, String value) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createIdentifier", "createIdentifier called");

        Identifier identifier = new Identifier();
        identifier.setValue(value);

        if (Objects.equals(system, fhirConfig.getOeFhirSystem() + "/pat_nationalId")) {
            identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        } else {
            identifier.setUse(Identifier.IdentifierUse.USUAL);
        }

        identifier.setSystem(system);
        return identifier;
    }

    /**
     * Creates a facility identifier that links a FHIR resource to this OpenELIS
     * facility. This identifier uses the facility ID and includes the facility
     * Organization as the assigner.
     *
     * @return the facility identifier, or null if facility is not initialized
     */
    public Identifier createFacilityIdentifier() {
        String facilityId = facilityOrganizationService.getFacilityId();
        String identifierSystem = facilityOrganizationService.getFacilityIdentifierSystem();
        Reference assignerRef = facilityOrganizationService.getFacilityOrganizationReference();

        if (facilityId == null) {
            return null;
        }

        Identifier identifier = new Identifier();
        identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        identifier.setSystem(identifierSystem);
        identifier.setValue(facilityId);

        if (assignerRef != null) {
            identifier.setAssigner(assignerRef);
        }

        return identifier;
    }

    public List<ContactPoint> transformToTelecom(Person person) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToTelecom", "transformToTelecom called");

        List<ContactPoint> contactPoints = new ArrayList<>();
        if (person.getPrimaryPhone() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue(person.getPrimaryPhone())
                    .setUse(ContactPointUse.MOBILE));
        }

        if (person.getEmail() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue(person.getEmail()));
        }

        if (person.getFax() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.FAX).setValue(person.getFax()));
        }

        return contactPoints;
    }

    public DateType transformToDateElement(String strDate) throws ParseException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToDateElement", "transformToDateElement called");

        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToDateElement", "transforming date " + strDate);
        if (GenericValidator.isBlankOrNull(strDate)) {
            return null;
        }
        boolean dayAmbiguous = false;
        boolean monthAmbiguous = false;
        // TODO look at this logic for detecting ambiguity
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            dayAmbiguous = true;
        }
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            monthAmbiguous = true;
        }
        Date birthDate = new SimpleDateFormat(DateUtil.getDateFormat()).parse(strDate);

        DateType dateType = new DateType();
        if (monthAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.YEAR);
        } else if (dayAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.MONTH);
        } else {
            dateType.setValue(birthDate, TemporalPrecisionEnum.DAY);
        }
        return dateType;
    }

    public Address transformToAddress(Person person) {
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

    public List<Identifier> createPatientIdentifiers(String subjectNumber, String nationalId, String stNumber,
            String guid, String fhirUuid) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToAddress", "transformToAddress called");

        List<Identifier> identifierList = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(subjectNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_subjectNumber", subjectNumber));
        }
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_nationalId", nationalId));
        }
        if (!GenericValidator.isBlankOrNull(stNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_stNumber", stNumber));
        }
        if (!GenericValidator.isBlankOrNull(guid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_guid", guid));
        }
        if (!GenericValidator.isBlankOrNull(fhirUuid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_uuid", fhirUuid));
        }
        return identifierList;
    }

    public Reference createReferenceFor(Resource resource) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createReferenceFor", "createReferenceFor called");

        if (resource == null) {
            return null;
        }
        Reference reference = new Reference(resource);
        reference.setReference(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
        return reference;
    }

    public Reference createReferenceFor(ResourceType resourceType, String id) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createReferenceFor", "createReferenceFor called");

        if (GenericValidator.isBlankOrNull(id)) {
            LogEvent.logWarn(this.getClass().getName(), "createReferenceFor",
                    "null or empty id used in resource:" + resourceType + "/" + id);
        }
        Reference reference = new Reference();
        reference.setReference(resourceType + "/" + id);
        return reference;
    }

    public CodeableConcept transformSampleProgramToCodeableConcept(ObservationHistory program) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformSampleProgramToCodeableConcept",
                "transformSampleProgramToCodeableConcept called");

        CodeableConcept codeableConcept = new CodeableConcept();
        String programDisplay = "";
        String programCode = "";
        if ("D".equals(program.getValueType())) {
            Dictionary dictionary = dictionaryService.get(program.getValue());
            if (dictionary != null) {
                programCode = dictionary.getDictEntry();
                programDisplay = dictionary.getDictEntry();
            }
        } else {
            programCode = program.getValue();
            programDisplay = program.getValue();
        }
        codeableConcept
                .addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sample_program", programCode, programDisplay));
        return codeableConcept;
    }

    public CodeableConcept transformTestToCodeableConcept(String testId) {
        return transformTestToCodeableConcept(testService.get(testId));
    }

    public CodeableConcept transformTestToCodeableConcept(Test test) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTestToCodeableConcept",
                "transformTestToCodeableConcept test called");

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept
                .addCoding(new Coding("http://loinc.org", test.getLoinc(), test.getLocalizedTestName().getEnglish()));
        return codeableConcept;
    }

    public Annotation transformNoteToAnnotation(Note note) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformNoteToAnnotation",
                "transformNoteToAnnotation called");

        Annotation annotation = new Annotation();
        annotation.setText(note.getText());
        return annotation;
    }

    public ServiceRequestPriority convertToServiceRequestPriority(OrderPriority orderPriority) {
        if (orderPriority == null) {
            return ServiceRequestPriority.ROUTINE;
        }
        switch (orderPriority) {
        case ROUTINE:
            return ServiceRequestPriority.ROUTINE;
        case ASAP:
            return ServiceRequestPriority.ASAP;
        case STAT:
        case FUTURE_STAT:
            return ServiceRequestPriority.STAT;
        case TIMED:
            return ServiceRequestPriority.URGENT;
        default:
            return ServiceRequestPriority.ROUTINE;
        }
    }

    public List<Test> resolveTestsFromServiceRequest(ServiceRequest serviceRequest) {
        List<Test> resolvedTests = new ArrayList<>();

        if (serviceRequest == null || !serviceRequest.hasCode() || !serviceRequest.getCode().hasCoding()) {
            return resolvedTests;
        }

        serviceRequest.getCode().getCoding().forEach(coding -> {

            if ("http://loinc.org".equalsIgnoreCase(coding.getSystem()) && coding.hasCode()) {

                List<Test> loincTests = testService.getTestsByLoincCode(coding.getCode());

                if (loincTests != null && !loincTests.isEmpty()) {
                    resolvedTests.addAll(loincTests);
                }
            }
            if (coding.hasDisplay() && !GenericValidator.isBlankOrNull(coding.getDisplay())) {

                List<Test> nameTests = testService.getTestsByName(coding.getDisplay());

                if (nameTests != null && !nameTests.isEmpty()) {
                    resolvedTests.addAll(nameTests.stream().filter(t -> "Y".equals(t.getIsActive())).toList());
                }
            }
        });

        return resolvedTests.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(Test::getId, t -> t, (a, b) -> a), m -> new ArrayList<>(m.values())));
    }

    public <T extends BaseObject<?>> T getItemByFhirId(String fhirUuid, BaseObjectService<T, ?> service) {

        if (fhirUuid == null) {
            return null;
        }

        try {
            List<T> matches = service.getAllMatching("fhirUuid", UUID.fromString(fhirUuid));
            return matches.isEmpty() ? null : matches.get(0);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(getClass().getSimpleName(), "getItemByFhirId", "Invalid UUID: " + fhirUuid);
            return null;
        }
    }

    public SampleStatus mapSpecimenStatus(Specimen.SpecimenStatus status) {
        if (status == null) {
            return SampleStatus.Entered;
        }

        switch (status) {
        case AVAILABLE:
            return SampleStatus.Entered;

        case UNAVAILABLE:
            return SampleStatus.Disposed;

        case UNSATISFACTORY:
            return SampleStatus.SampleRejected;

        case ENTEREDINERROR:
            return SampleStatus.Canceled;

        default:
            return SampleStatus.Entered;
        }
    }

    public Specimen.SpecimenStatus mapSampleItemStatusToSpecimenStatus(String statusId) {

        SampleStatus status = statusService.getSampleStatusForID(statusId);

        if (status == null)
            return Specimen.SpecimenStatus.AVAILABLE;

        switch (status) {
        case Canceled:
            return Specimen.SpecimenStatus.UNSATISFACTORY;

        case Disposed:
            return Specimen.SpecimenStatus.UNAVAILABLE;

        case Entered:
        default:
            return Specimen.SpecimenStatus.AVAILABLE;
        }
    }

    public SpecimenCollectionComponent transformToCollection(Timestamp collectionDate, String collector,
            Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToCollection", "transformToCollection called");

        SpecimenCollectionComponent specimenCollectionComponent = new SpecimenCollectionComponent();
        specimenCollectionComponent.setCollected(new DateTimeType(collectionDate));

        // Add GPS coordinates extension if available
        if (sample != null && sample.hasGpsCoordinates()) {
            Extension gpsExtension = createGpsExtension(sample);
            specimenCollectionComponent.addExtension(gpsExtension);
        }

        return specimenCollectionComponent;
    }

    public CodeableConcept transformSampleConditionToCodeableConcept(ObservationHistory initialSampleCondition) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformSampleConditionToCodeableConcept",
                "transformSampleConditionToCodeableConcept called");

        String observationValue;
        String observationDisplay;
        if (ValueType.DICTIONARY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = dictionaryService.get(initialSampleCondition.getValue()).getDictEntry();
            observationDisplay = dictionaryService.get(initialSampleCondition.getValue()).getDictEntryDisplayValue();
        } else if (ValueType.KEY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = localizationService.get(initialSampleCondition.getValue()).getEnglish();
            observationDisplay = "";
        } else {
            observationValue = initialSampleCondition.getValue();
            observationDisplay = "";
        }

        CodeableConcept condition = new CodeableConcept();
        condition.addCoding(
                new Coding(fhirConfig.getOeFhirSystem() + "/sample_condition", observationValue, observationDisplay));
        return condition;
    }

    /**
     * Creates a FHIR extension for GPS coordinates according to FHIR R4 standards.
     * Extension URL:
     * http://openelis-global.org/fhir/StructureDefinition/collection-location-gps
     *
     * @param sample Sample with GPS coordinates
     * @return Extension containing latitude, longitude, accuracy, method, and
     *         timestamp
     */
    public Extension createGpsExtension(Sample sample) {
        Extension gpsExtension = new Extension();
        gpsExtension.setUrl("http://openelis-global.org/fhir/StructureDefinition/collection-location-gps");

        // Latitude sub-extension (required if GPS data exists)
        if (sample.getGpsLatitude() != null) {
            Extension latitudeExt = new Extension("latitude", new DecimalType(sample.getGpsLatitude()));
            gpsExtension.addExtension(latitudeExt);
        }

        // Longitude sub-extension (required if GPS data exists)
        if (sample.getGpsLongitude() != null) {
            Extension longitudeExt = new Extension("longitude", new DecimalType(sample.getGpsLongitude()));
            gpsExtension.addExtension(longitudeExt);
        }

        // Accuracy sub-extension (optional)
        if (sample.getGpsAccuracyMeters() != null) {
            Extension accuracyExt = new Extension("accuracy", new IntegerType(sample.getGpsAccuracyMeters()));
            gpsExtension.addExtension(accuracyExt);
        }

        // Capture method sub-extension (optional)
        if (sample.getGpsCaptureMethod() != null) {
            Extension methodExt = new Extension("method", new CodeType(sample.getGpsCaptureMethod()));
            gpsExtension.addExtension(methodExt);
        }

        // Capture timestamp sub-extension (optional)
        if (sample.getGpsCaptureTimestamp() != null) {
            Extension timestampExt = new Extension("captureTimestamp",
                    new DateTimeType(sample.getGpsCaptureTimestamp()));
            gpsExtension.addExtension(timestampExt);
        }

        return gpsExtension;
    }

    public CodeableConcept transformTypeOfSampleToCodeableConcept(TypeOfSample typeOfSample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTypeOfSampleToCodeableConcept",
                "transformTypeOfSampleToCodeableConcept called");

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sampleType",
                typeOfSample.getLocalAbbreviation(), typeOfSample.getLocalizedName()));
        return codeableConcept;
    }

    public void setOeOrganizationIdentifiers(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationIdentifiers",
                "setOeOrganizationIdentifiers called");

        organization.setFhirUuid(UUID.fromString(fhirOrganization.getIdElement().getIdPart()));
        for (Identifier identifier : fhirOrganization.getIdentifier()) {
            if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_cliaNum")) {
                organization.setCliaNum(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_shortName")) {
                organization.setShortName(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_code")) {
                organization.setCode(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_uuid")) {
                organization.setFhirUuid(UUID.fromString(identifier.getValue()));
            }
        }
    }

    public void setFhirOrganizationIdentifiers(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationIdentifiers",
                "setFhirOrganizationIdentifiers called");

        if (!GenericValidator.isBlankOrNull(organization.getCliaNum())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_cliaNum")
                    .setValue(organization.getCliaNum()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getShortName())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_shortName")
                    .setValue(organization.getShortName()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_code")
                    .setValue(organization.getCode()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_uuid")
                    .setValue(organization.getFhirUuidAsString()));
        }
        Identifier facilityId = createFacilityIdentifier();
        if (facilityId != null) {
            fhirOrganization.addIdentifier(facilityId);
        }
    }

    public void setOeOrganizationTypes(Organization organization, org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationTypes", "setOeOrganizationTypes called");

        Set<OrganizationType> orgTypes = new HashSet<>();
        OrganizationType orgType = null;
        for (CodeableConcept type : fhirOrganization.getType()) {
            for (Coding coding : type.getCoding()) {
                if (coding.getSystem() != null
                        && coding.getSystem().equals(fhirConfig.getOeFhirSystem() + "/orgType")) {
                    orgType = new OrganizationType();
                    orgType.setName(coding.getCode());
                    orgType.setDescription(type.getText());
                    orgType.setNameKey("org_type." + coding.getCode() + ".name");
                    orgType.setOrganizations(new HashSet<>());
                    orgType.getOrganizations().add(organization);
                    orgTypes.add(orgType);
                }
            }
        }
        organization.setOrganizationTypes(orgTypes);
    }

    public void setFhirOrganizationTypes(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationTypes",
                "setFhirOrganizationTypes called");

        Set<OrganizationType> orgTypes = organizationService.get(organization.getId()).getOrganizationTypes();
        for (OrganizationType orgType : orgTypes) {
            fhirOrganization.addType(new CodeableConcept() //
                    .setText(orgType.getDescription()) //
                    .addCoding(new Coding() //
                            .setSystem(fhirConfig.getOeFhirSystem() + "/orgType") //
                            .setCode(orgType.getName())));
        }
    }

    public void setOeOrganizationAddressInfo(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationAddressInfo",
                "setOeOrganizationAddressInfo called");

        organization.setStreetAddress(fhirOrganization.getAddressFirstRep().getLine().stream()
                .map(e -> e.asStringValue()).collect(Collectors.joining("\\n")));
        organization.setCity(fhirOrganization.getAddressFirstRep().getCity());
        organization.setState(fhirOrganization.getAddressFirstRep().getState());
        organization.setZipCode(fhirOrganization.getAddressFirstRep().getPostalCode());
    }

    public void setFhirAddressInfo(org.hl7.fhir.r4.model.Organization fhirOrganization, Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirAddressInfo", "setFhirAddressInfo called");

        if (!GenericValidator.isBlankOrNull(organization.getStreetAddress())) {
            fhirOrganization.getAddressFirstRep().addLine(organization.getStreetAddress());
        }
        if (!GenericValidator.isBlankOrNull(organization.getCity())) {
            fhirOrganization.getAddressFirstRep().setCity(organization.getCity());
        }
        if (!GenericValidator.isBlankOrNull(organization.getState())) {
            fhirOrganization.getAddressFirstRep().setState(organization.getState());
        }
        if (!GenericValidator.isBlankOrNull(organization.getZipCode())) {
            fhirOrganization.getAddressFirstRep().setPostalCode(organization.getZipCode());
        }
    }

    public DiagnosticReport genNewDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "genNewDiagnosticReport", "genNewDiagnosticReport called");

        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(analysis.getFhirUuidAsString());
        diagnosticReport.addIdentifier(createIdentifier(fhirConfig.getOeFhirSystem() + "/analysisResult_uuid",
                analysis.getFhirUuidAsString()));
        Identifier facilityId = createFacilityIdentifier();
        if (facilityId != null) {
            diagnosticReport.addIdentifier(facilityId);
        }
        return diagnosticReport;
    }

    public TaskPriority convertToTaskPriority(OrderPriority orderPriority) {
        if (orderPriority == null) {
            return TaskPriority.ROUTINE;
        }
        switch (orderPriority) {
        case ROUTINE:
            return TaskPriority.ROUTINE;
        case ASAP:
            return TaskPriority.ASAP;
        case STAT:
        case FUTURE_STAT:
            return TaskPriority.STAT;
        case TIMED:
            return TaskPriority.URGENT;
        default:
            return TaskPriority.ROUTINE;
        }
    }

}
