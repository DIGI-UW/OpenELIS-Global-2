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

package org.openelisglobal.sample.action.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.validation.Errors;

/** */
public class SamplePatientUpdateData {
    private boolean savePatient = false;
    private Person providerPerson;
    private Provider provider;
    private String patientId;
    private String accessionNumber;
    private String referringId;
    private OrderPriority priority;

    private Sample sample;
    private List<SampleAdditionalField> sampleFields = new ArrayList<>();
    private SampleHuman sampleHuman = new SampleHuman();
    private SampleRequester requesterSite;
    private SampleRequester requesterSiteDepartment;
    private List<SampleTestCollection> sampleItemsTests;
    private SampleAddService sampleAddService;
    private Errors patientErrors;
    private Organization newOrganization;
    private Organization currentOrganization;
    private ElectronicOrder electronicOrder = null;

    private boolean useReceiveDateForCollectionDate = !FormFields.getInstance().useField(Field.CollectionDate);
    private String collectionDateFromReceiveDate = null;

    private OrganizationService orgService = SpringContext.getBean(OrganizationService.class);
    private ElectronicOrderService electronicOrderService = SpringContext.getBean(ElectronicOrderService.class);
    private ProgramService programService = SpringContext.getBean(ProgramService.class);
    private ProgramSampleService programSampleService = SpringContext.getBean(ProgramSampleService.class);
    private List<ObservationHistory> observations = new ArrayList<>();
    private List<OrganizationAddress> orgAddressExtra = new ArrayList<>();
    private final String currentUserId;

    private ProgramSample programSample;
    private QuestionnaireResponse programQuestionnaireResponse;

    private boolean eqaSample;
    private String eqaProgramId;
    private String eqaProviderOrganizationId;
    private String eqaProviderSampleId;
    private String eqaParticipantId;
    private String eqaDeadline;
    private String eqaPriority;

    private boolean customNotificationLogic;
    private List<String> patientEmailNotificationTestIds;
    private List<String> patientSMSNotificationTestIds;
    private List<String> providerEmailNotificationTestIds;
    private List<String> providerSMSNotificationTestIds;

    public SamplePatientUpdateData(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public boolean isSavePatient() {
        return savePatient;
    }

    public void setSavePatient(boolean savePatient) {
        this.savePatient = savePatient;
    }

    public Person getProviderPerson() {
        return providerPerson;
    }

    public void setProviderPerson(Person providerPerson) {
        this.providerPerson = providerPerson;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getReferringId() {
        return referringId;
    }

    public void setReferringId(String referringId) {
        this.referringId = referringId;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public SampleHuman getSampleHuman() {
        return sampleHuman;
    }

    public void setSampleHuman(SampleHuman sampleHuman) {
        this.sampleHuman = sampleHuman;
    }

    public SampleRequester getRequesterSite() {
        return requesterSite;
    }

    public void setRequesterSite(SampleRequester requesterSite) {
        this.requesterSite = requesterSite;
    }

    public SampleRequester getRequesterSiteDepartment() {
        return requesterSiteDepartment;
    }

    private void setRequesterSiteDepartment(SampleRequester requesterSiteDepartment) {
        this.requesterSiteDepartment = requesterSiteDepartment;
    }

    public List<SampleTestCollection> getSampleItemsTests() {
        return sampleItemsTests;
    }

    public void setSampleItemsTests(List<SampleTestCollection> sampleItemsTests) {
        this.sampleItemsTests = sampleItemsTests;
    }

    public SampleAddService getSampleAddService() {
        return sampleAddService;
    }

    public void setSampleAddService(SampleAddService sampleAddService) {
        this.sampleAddService = sampleAddService;
    }

    public void setPatientErrors(Errors patientErrors) {
        this.patientErrors = patientErrors;
    }

    public Organization getNewOrganization() {
        return newOrganization;
    }

    public void setNewOrganization(Organization newOrganization) {
        this.newOrganization = newOrganization;
    }

    public void setCurrentOrganization(Organization currentOrganization) {
        this.currentOrganization = currentOrganization;

    }

    public Organization getCurrentOrganization() {
        return currentOrganization;
    }

    public ElectronicOrder getElectronicOrder() {
        return electronicOrder;
    }

    public void setCollectionDateFromRecieveDateIfNeeded(String collectionDateFromRecieveDate) {
        if (useReceiveDateForCollectionDate) {
            collectionDateFromReceiveDate = collectionDateFromRecieveDate;
        }
    }

    public List<ObservationHistory> getObservations() {
        return observations;
    }

    public List<OrganizationAddress> getOrgAddressExtra() {
        return orgAddressExtra;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void addOrgAddressExtra(String value, String type, String addressPart) {
        if (!GenericValidator.isBlankOrNull(value)) {
            OrganizationAddress orgAddress = new OrganizationAddress();
            orgAddress.setSysUserId(currentUserId);
            orgAddress.setType(type);
            orgAddress.setValue(value);
            orgAddress.setAddressPartId(addressPart);
            orgAddressExtra.add(orgAddress);
        }
    }

    public void createObservation(String observationData, String observationType,
            ObservationHistory.ValueType valueType) {
        if (!GenericValidator.isBlankOrNull(observationData) && !GenericValidator.isBlankOrNull(observationType)) {
            ObservationHistory observation = new ObservationHistory();
            observation.setObservationHistoryTypeId(observationType);
            observation.setSysUserId(currentUserId);
            observation.setValue(observationData);
            observation.setValueType(valueType);
            observations.add(observation);
        }
    }

    public void validateSample(Errors errors) {
        validateSample(errors, true);
    }

    public void validateSample(Errors errors, boolean requireSampleItems) {
        // assure accession number - skip validation for updates (sample already exists)
        // When updating, the accession number is already in the database for this
        // sample,
        // so checkAccessionNumberValidity would incorrectly return USED_FAIL
        if (sample == null || sample.getId() == null) {
            IAccessionNumberValidator.ValidationResults result = AccessionNumberUtil
                    .checkAccessionNumberValidity(accessionNumber, null, null, null);

            if (result != IAccessionNumberValidator.ValidationResults.SUCCESS) {
                String message = AccessionNumberUtil.getInvalidMessage(result);
                errors.reject(message);
            }
        }

        // assure that there is at least 1 sample (skip for order-entry-only mode)
        if (requireSampleItems && sampleItemsTests.isEmpty()) {
            errors.reject("errors.no.sample");
        }

        // assure that all samples have tests (skip for order-entry-only mode)
        if (requireSampleItems && !allSamplesHaveTests()) {
            errors.reject("errors.samples.with.no.tests");
        }

        // check patient errors
        if (patientErrors.hasErrors()) {
            errors.addAllErrors(patientErrors);
        }
    }

    private boolean allSamplesHaveTests() {

        for (SampleTestCollection sampleTest : sampleItemsTests) {
            if (sampleTest.tests.size() == 0) {
                return false;
            }
        }

        return true;
    }

    public void createPopulatedSample(String receivedDate, SampleOrderItem sampleOrder) {
        // Check if editing an existing sample
        if (!GenericValidator.isBlankOrNull(sampleOrder.getSampleId())) {
            // Load existing sample for update
            sample = SpringContext.getBean(SampleService.class).get(sampleOrder.getSampleId());
            if (sample != null) {
                sample.setSysUserId(currentUserId);
                // Update fields that can change during edit
                sample.setReceivedTimestamp(DateUtil.convertStringDateToTimestamp(receivedDate));
                sample.setReferringId(sampleOrder.getRequesterSampleID());
                if (useReceiveDateForCollectionDate) {
                    sample.setCollectionDateForDisplay(collectionDateFromReceiveDate);
                }
                // Update informed consent fields with audit logic
                updateConsentFieldsWithAudit(sample, sampleOrder);
                setElectronicOrderIfNeeded(sampleOrder);
                return;
            }
        }

        // Create new sample
        sample = new Sample();
        sample.setSysUserId(currentUserId);
        sample.setAccessionNumber(accessionNumber);
        sample.setReferringId(referringId);

        sample.setEnteredDate(DateUtil.getNowAsSqlDate());

        sample.setReceivedTimestamp(DateUtil.convertStringDateToTimestamp(receivedDate));
        sample.setReferringId(sampleOrder.getRequesterSampleID());

        if (useReceiveDateForCollectionDate) {
            sample.setCollectionDateForDisplay(collectionDateFromReceiveDate);
        }

        // Set domain based on workflow type (OGC-356)
        // Environmental samples use "E" domain, clinical/human samples use "H" domain
        String workflowType = sampleOrder.getEnvironmentalFieldAsString("workflowType");
        if ("environmental".equals(workflowType)) {
            sample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.environmental"));
        } else {
            sample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
        }
        sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));

        // Set informed consent fields with audit logic
        updateConsentFieldsWithAudit(sample, sampleOrder);

        setElectronicOrderIfNeeded(sampleOrder);
    }

    private void setElectronicOrderIfNeeded(SampleOrderItem sampleOrder) {
        electronicOrder = null;
        String externalOrderNumber = sampleOrder.getExternalOrderNumber();
        if (!GenericValidator.isBlankOrNull(externalOrderNumber)) {
            List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(externalOrderNumber);
            if (!orders.isEmpty()) {
                electronicOrder = orders.get(orders.size() - 1);
                electronicOrder.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.Realized));
                electronicOrder.setSysUserId(currentUserId);

                sample.setReferringId(externalOrderNumber);
                sample.setClinicalOrderId(electronicOrder.getId());
            }
        }
    }

    public void initProvider(SampleOrderItem sampleOrder) {

        providerPerson = null;
        if (noRequesterInformation(sampleOrder)) {
            provider = PatientUtil.getUnownProvider();
        } else if (!GenericValidator.isBlankOrNull(sampleOrder.getProviderPersonId())) {
            provider = SpringContext.getBean(ProviderService.class).getProviderByPerson(
                    SpringContext.getBean(PersonService.class).get(sampleOrder.getProviderPersonId()));
            providerPerson = provider.getPerson();
            providerPerson.setSysUserId(currentUserId);
        } else {
            providerPerson = new Person();
            provider = new Provider();
            provider.setFhirUuid(UUID.randomUUID());
            provider.setActive(true);
            providerPerson.setFirstName(sampleOrder.getProviderFirstName());
            providerPerson.setLastName(sampleOrder.getProviderLastName());
            providerPerson.setWorkPhone(sampleOrder.getProviderWorkPhone());
            providerPerson.setFax(sampleOrder.getProviderFax());
            providerPerson.setEmail(sampleOrder.getProviderEmail());
            providerPerson.setSysUserId(currentUserId);
            provider.setExternalId(sampleOrder.getRequesterSampleID());
        }

        provider.setSysUserId(currentUserId);
    }

    private boolean noRequesterInformation(SampleOrderItem sampleOrder) {
        return (GenericValidator.isBlankOrNull(sampleOrder.getProviderPersonId())
                && GenericValidator.isBlankOrNull(sampleOrder.getProviderFirstName())
                && GenericValidator.isBlankOrNull(sampleOrder.getProviderWorkPhone())
                && GenericValidator.isBlankOrNull(sampleOrder.getProviderLastName())
                && GenericValidator.isBlankOrNull(sampleOrder.getRequesterSampleID())
                && GenericValidator.isBlankOrNull(sampleOrder.getProviderFax())
                && GenericValidator.isBlankOrNull(sampleOrder.getProviderEmail()));
    }

    public void buildSampleHuman() {
        sampleHuman.setSysUserId(currentUserId);
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patientId);
        // Only link provider if it's a real provider, not the "unknown provider"
        // placeholder
        if (provider != null && Boolean.TRUE.equals(provider.getActive())) {
            sampleHuman.setProviderId(provider.getId());
        }
    }

    public void initializeNewOrganization(SampleOrderItem orderItem) {
        newOrganization.setFhirUuid(UUID.randomUUID());
        newOrganization.setCode(orderItem.getReferringSiteCode());

        newOrganization.setIsActive("Y");
        newOrganization.setOrganizationName(orderItem.getNewRequesterName());

        // this was left as a warning for copy and paste -- it causes a null
        // pointer exception in session.flush()
        // newOrganization.setOrganizationTypes(ORG_TYPE_SET);
        newOrganization.setSysUserId(currentUserId);
        newOrganization.setMlsSentinelLabFlag("N");
    }

    public void updateCurrentOrgIfNeeded(String code, String orgId) {
        currentOrganization = orgService.getOrganizationById(orgId);
        if (StringUtil.compareWithNulls(code, currentOrganization.getCode()) != 0) {
            currentOrganization.setCode(code);
            currentOrganization.setSysUserId(currentUserId);
        } else {
            currentOrganization = null;
        }
    }

    public void initializeRequester(SampleOrderItem sampleOrder) {
        if (FormFields.getInstance().useField(Field.RequesterSiteList)) {
            setRequesterSite(initSampleRequester(sampleOrder));
        }
        if (FormFields.getInstance().useField(Field.SITE_DEPARTMENT)) {
            setRequesterSiteDepartment(initSampleRequesterDepartment(sampleOrder));
        }
    }

    private SampleRequester initSampleRequesterDepartment(SampleOrderItem orderItem) {
        SampleRequester requester = null;

        String orgId = orderItem.getReferringSiteDepartmentId();

        if (!GenericValidator.isBlankOrNull(orgId)) {
            requester = createSiteRequester(orgId, TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
        }

        return requester;
    }

    private SampleRequester initSampleRequester(SampleOrderItem orderItem) {
        SampleRequester requester = null;
        if (!GenericValidator.isBlankOrNull(orderItem.getReferringSiteName())) {
            orderItem.setNewRequesterName(orderItem.getReferringSiteName());
        }
        String orgId = orderItem.getReferringSiteId();

        if (!GenericValidator.isBlankOrNull(orgId)) {
            requester = createSiteRequester(orgId, TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
            if (FormFields.getInstance().useField(Field.SampleEntryReferralSiteCode)) {
                updateCurrentOrgIfNeeded(orderItem.getReferringSiteCode(), orgId);
            }

        } else if (!GenericValidator.isBlankOrNull(orderItem.getNewRequesterName())) {

            if (confirmNewRequesterName(orderItem.getNewRequesterName())) {
                // will be corrected after newOrg is persisted
                requester = createSiteRequester("0", TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);

                setNewOrganization(new Organization());

                if (FormFields.getInstance().useField(Field.SampleEntryHealthFacilityAddress)) {
                    addOrgAddressExtra(orderItem.getFacilityPhone(), "T",
                            TableIdService.getInstance().ADDRESS_PHONE_ID);
                    addOrgAddressExtra(orderItem.getFacilityFax(), "T", TableIdService.getInstance().ADDRESS_FAX_ID);
                    addOrgAddressExtra(orderItem.getFacilityAddressCommune(), "T",
                            TableIdService.getInstance().ADDRESS_COMMUNE_ID);
                    addOrgAddressExtra(orderItem.getFacilityAddressStreet(), "T",
                            TableIdService.getInstance().ADDRESS_STREET_ID);
                }

                initializeNewOrganization(orderItem);
            } else {
                Organization organization = new Organization();
                organization.setOrganizationName(orderItem.getNewRequesterName());
                organization = orgService.getActiveOrganizationByName(organization, true);
                orgId = organization.getId();

                if (!GenericValidator.isBlankOrNull(orgId)) {
                    requester = createSiteRequester(orgId, TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
                }
            }
        }

        return requester;
    }

    /**
     * Check if new requester name is actually a new name
     *
     * @param requesterName The name to check
     * @return if the name is not stored in the database
     */
    private boolean confirmNewRequesterName(String requesterName) {
        boolean newName = true;
        Organization organization = new Organization();
        organization.setOrganizationName(requesterName);
        organization = orgService.getActiveOrganizationByName(organization, true);

        if (organization == null) {
            newName = true;
        } else {
            newName = false;
        }
        return newName;
    }

    private SampleRequester createSiteRequester(String orgId, long requesterTypeId) {
        SampleRequester requester;
        requester = new SampleRequester();
        requester.setRequesterId(orgId);
        requester.setRequesterTypeId(requesterTypeId);
        requester.setSysUserId(currentUserId);
        return requester;
    }

    public void initSampleData(String sampleXML, String receivedDate, boolean trackPayments,
            SampleOrderItem sampleOrder) {
        createPopulatedSample(receivedDate, sampleOrder);
        addObservations(sampleOrder, trackPayments);

        SampleAddService sampleAddService = new SampleAddService(sampleXML, currentUserId, getSample(), receivedDate);
        List<SampleTestCollection> sampleItems = sampleAddService.createSampleTestCollection();
        setSampleItemsTests(sampleItems);
        setSampleAddService(sampleAddService);
    }

    public void initProgramQuestions(String programId, QuestionnaireResponse additionalQuestions) {
        Program program = programService.get(programId);
        setProgramQuestionnaireResponse(additionalQuestions);

        // For updates (sample already exists), try to load existing ProgramSample
        ProgramSample existingProgramSample = null;
        if (sample != null && sample.getId() != null) {
            existingProgramSample = programSampleService.getProgrammeSampleBySample(Integer.valueOf(sample.getId()),
                    program.getProgramName());
        }

        if (existingProgramSample != null) {
            // Update existing ProgramSample
            setProgramSample(existingProgramSample);
            getProgramSample().setSysUserId(currentUserId);
        } else {
            // Create new ProgramSample
            if (program.getProgramName().toLowerCase().contains("pathology")) {
                setProgramSample(new PathologySample());
            } else if (program.getProgramName().toLowerCase().contains("immunohistochemistry")) {
                setProgramSample(new ImmunohistochemistrySample());
            } else if (program.getProgramName().toLowerCase().contains("cytology")) {
                setProgramSample(new CytologySample());
            } else {
                setProgramSample(new ProgramSample());
            }
            getProgramSample().setProgram(program);
            getProgramSample().setSysUserId(currentUserId);
        }
    }

    private void addObservations(SampleOrderItem sampleOrder, boolean trackPayments) {
        ObservationHistoryService observationHistoryService = SpringContext.getBean(ObservationHistoryService.class);
        if (trackPayments) {
            createObservation(sampleOrder.getPaymentOptionSelection(),
                    observationHistoryService.getObservationTypeIdForType(ObservationType.PAYMENT_STATUS),
                    ValueType.DICTIONARY);
        }

        createObservation(sampleOrder.getRequestDate(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE), ValueType.LITERAL);
        createObservation(sampleOrder.getNextVisitDate(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.NEXT_VISIT_DATE),
                ValueType.LITERAL);
        createObservation(sampleOrder.getTestLocationCode(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.TEST_LOCATION_CODE),
                ValueType.DICTIONARY);
        createObservation(sampleOrder.getOtherLocationCode(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.TEST_LOCATION_CODE_OTHER),
                ValueType.LITERAL);
        createObservation(sampleOrder.getReferringPatientNumber(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.REFERRERS_PATIENT_ID),
                ValueType.LITERAL);
        createObservation(sampleOrder.getProvisionalClinicalDiagnosis(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS),
                ValueType.LITERAL);
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.USE_BILLING_REFERENCE_NUMBER, "true")) {
            createObservation(sampleOrder.getBillingReferenceNumber(),
                    observationHistoryService.getObservationTypeIdForType(ObservationType.BILLING_REFERENCE_NUMBER),
                    ValueType.LITERAL);
        }

        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ORDER_PROGRAM, "true")) {
            createObservation(sampleOrder.getProgram(),
                    observationHistoryService.getObservationTypeIdForType(ObservationType.PROGRAM),
                    ValueType.DICTIONARY);
        }
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ORDER_PROGRAM, "true")) {
            if (!GenericValidator.isBlankOrNull(sampleOrder.getProgramId())) {
                createObservation(programService.get(sampleOrder.getProgramId()).getProgramName(),
                        observationHistoryService.getObservationTypeIdForType(ObservationType.PROGRAM),
                        ValueType.LITERAL);
            }
        }

        // Add environmental workflow observations (OGC-356)
        addEnvironmentalObservations(sampleOrder, observationHistoryService);
    }

    /**
     * Add environmental workflow observations from the environmentalFields map.
     * Maps frontend field keys to ObservationHistoryType names.
     */
    private void addEnvironmentalObservations(SampleOrderItem sampleOrder,
            ObservationHistoryService observationHistoryService) {
        if (sampleOrder.getEnvironmentalFields() == null || sampleOrder.getEnvironmentalFields().isEmpty()) {
            return;
        }

        java.util.Map<String, Object> envFields = sampleOrder.getEnvironmentalFields();

        // Collection site description
        createObservation(getStringValue(envFields, "collectionSiteDescription"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_COLLECTION_SITE_DESCRIPTION),
                ValueType.LITERAL);

        // Requester reference (e.g., EPA Method number)
        createObservation(getStringValue(envFields, "requesterReference"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_REQUESTER_REFERENCE),
                ValueType.LITERAL);

        // Environmental conditions at collection
        createObservation(getStringValue(envFields, "environmentalConditions"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_ENVIRONMENTAL_CONDITIONS),
                ValueType.LITERAL);

        // Location hierarchy IDs - supports both nested object and flat keys
        String locationRegionId = getNestedLocationValue(envFields, 1);
        String locationDistrictId = getNestedLocationValue(envFields, 2);
        String locationVillageId = getNestedLocationValue(envFields, 3);

        createObservation(locationRegionId,
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_LOCATION_REGION_ID),
                ValueType.LITERAL);
        createObservation(locationDistrictId,
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_LOCATION_DISTRICT_ID),
                ValueType.LITERAL);
        createObservation(locationVillageId,
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_LOCATION_VILLAGE_ID),
                ValueType.LITERAL);

        // Workflow type (clinical vs environmental)
        createObservation(getStringValue(envFields, "workflowType"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_WORKFLOW_TYPE),
                ValueType.LITERAL);

        // Sampling site fields
        createObservation(getStringValue(envFields, "samplingSiteId"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_SAMPLING_SITE_ID),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "samplingSiteName"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_SAMPLING_SITE_NAME),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "siteType"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_SITE_TYPE),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "siteSubtype"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_SITE_SUBTYPE),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "environmentalZone"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_ENVIRONMENTAL_ZONE),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "regulatoryReference"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_REGULATORY_REFERENCE),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "collectionMethod"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_COLLECTION_METHOD),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "contactPerson"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_CONTACT_PERSON),
                ValueType.LITERAL);
        createObservation(getStringValue(envFields, "contactPhone"),
                observationHistoryService.getObservationTypeIdForType(ObservationType.ENV_CONTACT_PHONE),
                ValueType.LITERAL);
    }

    /**
     * Safely get a String value from a Map that may contain Object values.
     */
    private String getStringValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    /**
     * Extract location ID from environmentalFields for a given hierarchy level.
     * Supports both: - New format: flat keys like "locationHierarchy.1",
     * "locationHierarchy.2" - Old format: nested object "locationHierarchy": {"1":
     * "id1", "2": "id2"}
     */
    @SuppressWarnings("unchecked")
    private String getNestedLocationValue(java.util.Map<String, Object> envFields, int level) {
        // Try new flat key format first: locationHierarchy.1, locationHierarchy.2, etc.
        String value = getStringValue(envFields, "locationHierarchy." + level);
        if (!GenericValidator.isBlankOrNull(value)) {
            return value;
        }

        // Try old nested object format: locationHierarchy: {1: "id", 2: "id"}
        Object locationHierarchy = envFields.get("locationHierarchy");
        if (locationHierarchy instanceof java.util.Map) {
            java.util.Map<String, Object> hierarchyMap = (java.util.Map<String, Object>) locationHierarchy;
            Object levelValue = hierarchyMap.get(String.valueOf(level));
            if (levelValue == null) {
                levelValue = hierarchyMap.get(level); // Try integer key
            }
            if (levelValue != null) {
                return levelValue.toString();
            }
        }

        // Try alternative formats: location_Region, location_District, location_Village
        switch (level) {
        case 1:
            value = getStringValue(envFields, "location_Region");
            break;
        case 2:
            value = getStringValue(envFields, "location_District");
            break;
        case 3:
            value = getStringValue(envFields, "location_Village");
            if (GenericValidator.isBlankOrNull(value)) {
                value = getStringValue(envFields, "location_Town");
            }
            break;
        }
        return value;
    }

    public boolean getCustomNotificationLogic() {
        return customNotificationLogic;
    }

    public void setCustomNotificationLogic(boolean customNotificationLogic) {
        this.customNotificationLogic = customNotificationLogic;
    }

    public List<String> getPatientEmailNotificationTestIds() {
        return patientEmailNotificationTestIds;
    }

    public void setPatientEmailNotificationTestIds(List<String> patientEmailNotificationTestIds) {
        this.patientEmailNotificationTestIds = patientEmailNotificationTestIds;
    }

    public List<String> getPatientSMSNotificationTestIds() {
        return patientSMSNotificationTestIds;
    }

    public void setPatientSMSNotificationTestIds(List<String> patientSMSNotificationTestIds) {
        this.patientSMSNotificationTestIds = patientSMSNotificationTestIds;
    }

    public List<String> getProviderEmailNotificationTestIds() {
        return providerEmailNotificationTestIds;
    }

    public void setProviderEmailNotificationTestIds(List<String> providerEmailNotificationTestIds) {
        this.providerEmailNotificationTestIds = providerEmailNotificationTestIds;
    }

    public List<String> getProviderSMSNotificationTestIds() {
        return providerSMSNotificationTestIds;
    }

    public void setProviderSMSNotificationTestIds(List<String> providerSMSNotificationTestIds) {
        this.providerSMSNotificationTestIds = providerSMSNotificationTestIds;
    }

    public List<SampleAdditionalField> getSampleFields() {
        return sampleFields;
    }

    public void setSampleFields(List<SampleAdditionalField> sampleFields) {
        this.sampleFields = sampleFields;
    }

    public void addSampleField(SampleAdditionalField sampleField) {
        if (sampleFields == null) {
            sampleFields = new ArrayList<>();
        }
        sampleFields.add(sampleField);
    }

    public void addAllSampleFields(List<SampleAdditionalField> sampleFields) {
        if (sampleFields == null) {
            sampleFields = new ArrayList<>();
        }
        this.sampleFields.addAll(sampleFields);
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public void setPriority(OrderPriority priority) {
        this.priority = priority;
    }

    public ProgramSample getProgramSample() {
        return programSample;
    }

    public void setProgramSample(ProgramSample programSample) {
        this.programSample = programSample;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public boolean isEqaSample() {
        return eqaSample;
    }

    public void setEqaSample(boolean eqaSample) {
        this.eqaSample = eqaSample;
    }

    public String getEqaProgramId() {
        return eqaProgramId;
    }

    public void setEqaProgramId(String eqaProgramId) {
        this.eqaProgramId = eqaProgramId;
    }

    public String getEqaProviderOrganizationId() {
        return eqaProviderOrganizationId;
    }

    public void setEqaProviderOrganizationId(String eqaProviderOrganizationId) {
        this.eqaProviderOrganizationId = eqaProviderOrganizationId;
    }

    public String getEqaProviderSampleId() {
        return eqaProviderSampleId;
    }

    public void setEqaProviderSampleId(String eqaProviderSampleId) {
        this.eqaProviderSampleId = eqaProviderSampleId;
    }

    public String getEqaParticipantId() {
        return eqaParticipantId;
    }

    public void setEqaParticipantId(String eqaParticipantId) {
        this.eqaParticipantId = eqaParticipantId;
    }

    public String getEqaDeadline() {
        return eqaDeadline;
    }

    public void setEqaDeadline(String eqaDeadline) {
        this.eqaDeadline = eqaDeadline;
    }

    public String getEqaPriority() {
        return eqaPriority;
    }

    public void setEqaPriority(String eqaPriority) {
        this.eqaPriority = eqaPriority;
    }

    /**
     * Update consent fields from form data. When consent is provided (true), use
     * the user-supplied audit fields from the form. When consent is explicitly
     * withdrawn (false), clear all consent fields. When the form omits the consent
     * section entirely (consentGiven == null) on an update, preserve the persisted
     * values rather than wiping the existing consent record.
     */
    private void updateConsentFieldsWithAudit(Sample sample, SampleOrderItem sampleOrder) {
        Boolean consentGiven = sampleOrder.getConsentGiven();
        String consentFormReference = sampleOrder.getConsentFormReference();
        String consentRecordedAt = sampleOrder.getConsentRecordedAt();
        String consentRecordedBy = sampleOrder.getConsentRecordedBy();

        // On update, null consentGiven means the form did not include the consent
        // section; leave the persisted values alone. On a new sample, fall through
        // and default to "no consent recorded".
        if (consentGiven == null && sample.getId() != null) {
            return;
        }

        if (Boolean.TRUE.equals(consentGiven)) {
            // Consent provided - set fields from form data
            sample.setConsentGiven(true);
            sample.setConsentFormReference(consentFormReference);

            // Use form-supplied audit fields
            if (consentRecordedAt != null && !consentRecordedAt.trim().isEmpty()) {
                java.sql.Date parsedDate = DateUtil.convertStringDateToSqlDate(consentRecordedAt);
                sample.setConsentRecordedAt(new java.sql.Timestamp(parsedDate.getTime()));
            } else {
                sample.setConsentRecordedAt(null);
            }

            sample.setConsentRecordedBy(consentRecordedBy);
        } else {
            // Consent withdrawn or not provided - clear all fields
            sample.setConsentGiven(false);
            sample.setConsentFormReference(null);
            sample.setConsentRecordedAt(null);
            sample.setConsentRecordedBy(null);
        }
    }
}
