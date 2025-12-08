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

package org.openelisglobal.common.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** */
@Service
@Scope("prototype")
@DependsOn({ "springContext" })
public class SampleOrderService {

    private ProgramSampleService programSampleService = SpringContext.getBean(ProgramSampleService.class);
    private FhirUtil fhirUtil = SpringContext.getBean(FhirUtil.class);
    private static SampleService sampleService = SpringContext.getBean(SampleService.class);
    private static OrganizationService orgService = SpringContext.getBean(OrganizationService.class);
    private ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);

    boolean needRequesterList = FormFields.getInstance().useField(FormFields.Field.RequesterSiteList);
    private boolean needPaymentOptions = ConfigurationProperties.getInstance()
            .isPropertyValueEqual(ConfigurationProperties.Property.TRACK_PATIENT_PAYMENT, "true");
    private boolean needTestLocationCode = FormFields.getInstance().useField(FormFields.Field.TEST_LOCATION_CODE);
    private SampleOrderItem sampleOrder;
    private Sample sample;
    private boolean readOnly = false;

    public SampleOrderService() {
        sample = null;
    }

    public SampleOrderService(Sample sample) {
        this.sample = sample;
    }

    public SampleOrderService(SampleOrderItem sampleOrder) {
        this.sampleOrder = sampleOrder;
    }

    public SampleOrderService(String accessionNumber, boolean readOnly) {
        sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        this.readOnly = readOnly;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    private SampleOrderItem getBaseSampleOrderItem() {
        SampleOrderItem orderItems = new SampleOrderItem();

        String dateAsText = DateUtil.getCurrentDateAsText();
        orderItems.setReceivedDateForDisplay(dateAsText);
        orderItems.setRequestDate(dateAsText);
        orderItems.setReceivedTime(DateUtil.convertTimestampToStringHourTime(DateUtil.getNowAsTimestamp()));

        orderItems.setProvidersList(
                DisplayListService.getInstance().getFreshList(DisplayListService.ListType.PRACTITIONER_PERSONS));

        orderItems.setPriorityList(
                DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ORDER_PRIORITY));

        if (needRequesterList) {
            orderItems.setReferringSiteList(DisplayListService.getInstance()
                    .getFreshList(DisplayListService.ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
        }

        if (needPaymentOptions) {
            orderItems.setPaymentOptions(DisplayListService.getInstance()
                    .getList(DisplayListService.ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS));
        }

        if (needTestLocationCode) {
            orderItems.setTestLocationCodeList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_LOCATION_CODE));
        }

        if (ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.ORDER_PROGRAM,
                "true")) {
            orderItems.setProgramList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.DICTIONARY_PROGRAM));
        }

        return orderItems;
    }

    /**
     * If this service is created with the default no parameter constructor then
     * this will generate a SampleOrderItem populated with the appropriate lists but
     * no other values.
     *
     * @return The SampleOrderItem
     */
    public SampleOrderItem getSampleOrderItem() {
        sampleOrder = getBaseSampleOrderItem();

        if (sample != null) {
            SampleService sampleService = SpringContext.getBean(SampleService.class);
            sampleOrder.setSampleId(sample.getId());
            sampleOrder.setLabNo(sampleService.getAccessionNumber(sample));
            sampleOrder.setPriority(sample.getPriority());
            sampleOrder.setReceivedDateForDisplay(sampleService.getReceivedDateForDisplay(sample));
            sampleOrder.setReceivedTime(sampleService.getReceived24HourTimeForDisplay(sample));

            sampleOrder.setRequestDate(
                    observationHistoryService.getValueForSample(ObservationType.REQUEST_DATE, sample.getId()));
            sampleOrder.setReferringPatientNumber(
                    observationHistoryService.getValueForSample(ObservationType.REFERRERS_PATIENT_ID, sample.getId()));
            sampleOrder.setNextVisitDate(
                    observationHistoryService.getValueForSample(ObservationType.NEXT_VISIT_DATE, sample.getId()));
            sampleOrder.setPaymentOptionSelection(
                    observationHistoryService.getRawValueForSample(ObservationType.PAYMENT_STATUS, sample.getId()));
            sampleOrder.setTestLocationCode(
                    observationHistoryService.getRawValueForSample(ObservationType.TEST_LOCATION_CODE, sample.getId()));
            sampleOrder.setOtherLocationCode(observationHistoryService
                    .getValueForSample(ObservationType.TEST_LOCATION_CODE_OTHER, sample.getId()));
            sampleOrder.setBillingReferenceNumber(observationHistoryService
                    .getValueForSample(ObservationType.BILLING_REFERENCE_NUMBER, sample.getId()));
            sampleOrder.setProvisionalClinicalDiagnosis(observationHistoryService
                    .getValueForSample(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS, sample.getId()));
            sampleOrder.setProgram(
                    observationHistoryService.getRawValueForSample(ObservationType.PROGRAM, sample.getId()));
            String programName = observationHistoryService.getRawValueForSample(ObservationType.PROGRAM,
                    sample.getId());
            ProgramSample programSample = programSampleService
                    .getProgrammeSampleBySample(Integer.valueOf(sample.getId()), programName);
            if (programSample != null) {
                sampleOrder.setProgramId(programSample.getProgram().getId());
                if (programSample.getQuestionnaireResponseUuid() != null) {
                    sampleOrder.setAdditionalQuestions(
                            fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                                    .withId(programSample.getQuestionnaireResponseUuid().toString()).execute());
                }
            }

            RequesterService requesterService = new RequesterService(sample.getId());
            sampleOrder.setProviderPersonId(requesterService.getRequesterPersonId());
            if (requesterService.getPerson() != null) {
                Provider provider = SpringContext.getBean(ProviderService.class)
                        .getProviderByPerson(requesterService.getPerson());
                if (provider != null) {
                    sampleOrder.setProviderId(provider.getId());
                }
            }
            sampleOrder.setProviderFirstName(requesterService.getRequesterFirstName());
            sampleOrder.setProviderLastName(requesterService.getRequesterLastName());
            sampleOrder.setProviderWorkPhone(requesterService.getWorkPhone());
            sampleOrder.setProviderFax(requesterService.getFax());
            sampleOrder.setProviderEmail(requesterService.getEmail());
            sampleOrder.setReferringSiteId(requesterService.getReferringSiteId());
            sampleOrder.setReferringSiteDepartmentId(requesterService.getReferringDepartmentId());
            sampleOrder.setReferringSiteCode(requesterService.getReferringSiteCode());
            sampleOrder.setReferringSiteName(requesterService.getReferringSiteName());

            sampleOrder.setReadOnly(readOnly);
        }

        return sampleOrder;
    }

    public SampleOrderPersistenceArtifacts getPersistenceArtifacts(Sample sample, String currentUserId) {
        if (sampleOrder == null) {
            throw new IllegalStateException(
                    "SampleOrderService.getPersistenceArtifacts have used the SampleOrderItem constructor");
        }

        SampleOrderPersistenceArtifacts artifacts = new SampleOrderPersistenceArtifacts();

        if (sampleOrder.getModified()) {
            createSampleArtifacts(sample, currentUserId, artifacts);
            createProviderArtifacts(sampleOrder, currentUserId, artifacts);
        }

        return artifacts;
    }

    private void createSampleArtifacts(Sample sample, String currentUserId, SampleOrderPersistenceArtifacts artifacts) {
        if (sample == null) {
            sample = new Sample();
            sample.setId(sampleOrder.getSampleId());
            sampleService.getData(sample);
            sample.setSysUserId(currentUserId);
        }

        String receivedDate = sampleOrder.getReceivedDateForDisplay();

        if (!GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
            receivedDate += " " + sampleOrder.getReceivedTime();
        } else {
            receivedDate += " 00:00";
        }

        // TODO check if the receivedDate has changed, if it has then the change has to
        // propagate through the analysis
        // if (useReceiveDateForCollectionDate) {
        // collectionDateFromRecieveDate = receivedDateForDisplay;
        // }

        sample.setReceivedTimestamp(DateUtil.convertStringDateToTimestamp(receivedDate));
        artifacts.setSample(sample);
    }

    private void createProviderArtifacts(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts) {
        RequesterService requesterService = new RequesterService(sampleOrder.getSampleId());
        createPersonProviderArtifacts(sampleOrder, currentUserId, artifacts, requesterService);
        createObservationHistoryArtifacts(sampleOrder, currentUserId, artifacts);
        createOrganizationProviderArtifacts(sampleOrder, currentUserId, artifacts, requesterService);
        createOrganizationDepartProviderArtifacts(sampleOrder, currentUserId, artifacts, requesterService);
    }

    private void createPersonProviderArtifacts(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, RequesterService requesterService) {
        Person providerPerson = null;
        Provider provider = null;

        if (!namesDiffer(requesterService.getPerson(), sampleOrder)) {
            if (!GenericValidator.isBlankOrNull(sampleOrder.getProviderPersonId())) {
                provider = SpringContext.getBean(ProviderService.class).getProviderByPerson(
                        SpringContext.getBean(PersonService.class).get(sampleOrder.getProviderPersonId()));
                providerPerson = provider.getPerson();
                providerPerson.setSysUserId(currentUserId);
            }
        }

        if (providerPerson == null) {
            provider = new Provider();
            provider.setFhirUuid(UUID.randomUUID());
            provider.setActive(true);
            provider.setExternalId(sampleOrder.getRequesterSampleID());

            providerPerson = new Person();
            providerPerson.setFirstName(sampleOrder.getProviderFirstName());
            providerPerson.setLastName(sampleOrder.getProviderLastName());
            providerPerson.setWorkPhone(sampleOrder.getProviderWorkPhone());
            providerPerson.setFax(sampleOrder.getProviderFax());
            providerPerson.setEmail(sampleOrder.getProviderEmail());
        }

        List<SampleRequester> personSampleRequesters = requesterService
                .getSampleRequestersByType(RequesterService.Requester.PERSON, true);
        SampleRequester samplePersonRequester = personSampleRequesters.size() > 0 ? personSampleRequesters.get(0)
                : null;
        if (samplePersonRequester != null) {
            samplePersonRequester.setSysUserId(currentUserId);
            artifacts.setSamplePersonRequester(samplePersonRequester);
        }
        providerPerson.setSysUserId(currentUserId);
        provider.setSysUserId(currentUserId);

        artifacts.setProviderPerson(providerPerson);
        artifacts.setProvider(provider);
    }

    private boolean namesDiffer(Person providerPerson, SampleOrderItem sampleOrder) {
        if (providerPerson == null || sampleOrder.getProviderPersonId() == null) {
            return true;
        }
        if (providerPerson.getId().trim().equals(sampleOrder.getProviderPersonId().trim())) {
            return StringUtil.compareWithNulls(providerPerson.getFirstName(), sampleOrder.getProviderFirstName()) != 0
                    || StringUtil.compareWithNulls(providerPerson.getLastName(),
                            sampleOrder.getProviderLastName()) != 0;
        } else {
            return false;
        }
    }

    private void createObservationHistoryArtifacts(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts) {
        List<ObservationHistory> observations = new ArrayList<>();
        SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
        Patient patient = sampleHumanService.getPatientForSample(artifacts.getSample());
        String patientId = patient.getId();

        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.REFERRERS_PATIENT_ID,
                sampleOrder.getReferringPatientNumber(), ValueType.LITERAL);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.NEXT_VISIT_DATE,
                sampleOrder.getNextVisitDate(), ValueType.LITERAL);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.TEST_LOCATION_CODE,
                sampleOrder.getTestLocationCode(), ValueType.DICTIONARY);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.TEST_LOCATION_CODE_OTHER,
                sampleOrder.getOtherLocationCode(), ValueType.LITERAL);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.PAYMENT_STATUS,
                sampleOrder.getPaymentOptionSelection(), ValueType.DICTIONARY);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.REQUEST_DATE,
                sampleOrder.getRequestDate(), ValueType.LITERAL);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.PROGRAM,
                sampleOrder.getProgram(), ValueType.DICTIONARY);
        createOrUpdateObservation(currentUserId, observations, patientId, ObservationType.BILLING_REFERENCE_NUMBER,
                sampleOrder.getBillingReferenceNumber(), ValueType.LITERAL);
        createOrUpdateObservation(currentUserId, observations, patientId,
                ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS, sampleOrder.getProvisionalClinicalDiagnosis(),
                ValueType.LITERAL);

        artifacts.setObservations(observations);
    }

    private void createOrUpdateObservation(String currentUserId, List<ObservationHistory> observations,
            String patientId, ObservationType observationType, String value, ValueType valueType) {
        ObservationHistory observation = observationHistoryService.getObservationForSample(observationType,
                sampleOrder.getSampleId());
        if (observation == null && !GenericValidator.isBlankOrNull(value)) {
            observation = new ObservationHistory();
            observation.setSampleId(sampleOrder.getSampleId());
            observation.setPatientId(patientId);
            observation.setObservationHistoryTypeId(
                    observationHistoryService.getObservationTypeIdForType(observationType));
            observation.setValueType(valueType.getCode());
        }

        if (observation != null) {
            observation.setValue(value);
            observation.setSysUserId(currentUserId);
            observations.add(observation);
        }
    }

    private void createOrganizationProviderArtifacts(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, RequesterService requesterService) {
        /*
         * These are the possible cases 1. No organizationRequester a. No referring site
         * id or code or new organization name -- nothing to do b. New organization name
         * -- create a new sample requester and organization they will need to be bound
         * after the organization is saved b. A referring site id -- create a new
         * SampleRequester using the referring site id i. no referring code or --
         * nothing to do to organization. We don't want to delete an existing code ii
         * referring code differs from what is in the database -- update the
         * organization iii. referring code is the same as in the database -- nothing to
         * do with the organization 2. Existing organizationRequester a. No referring
         * site id or new organization name -- delete requester b. New Organization --
         * create a new organization they will need to be bound after the organization
         * is saved c. A referring site id which is the same as in organizationRequester
         * i. referring code is the same as in the organization or referring code is
         * blank -- nothing to do ii referring code is different from organization --
         * update organization d. A different site id for that in the requester --
         * update the requester i. referring code is the same as in the organization or
         * referring code is blank -- nothing to do ii referring code is different from
         * organization -- update organization
         */
        SampleRequester orgRequester = sampleService.getOrganizationSampleRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_TYPE_ID);

        // SampleRequester orgRequester = requesterService
        // .getSampleRequesterByType(RequesterService.Requester.ORGANIZATION, false);

        if (orgRequester == null) {
            handleNoExistingOrganizationRequester(sampleOrder, currentUserId, artifacts);
        } else {
            handleExistingOrganizationRequester(sampleOrder, currentUserId, artifacts, orgRequester);
        }
    }

    private void createOrganizationDepartProviderArtifacts(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, RequesterService requesterService) {

        SampleRequester orgDepartRequester = sampleService.getOrganizationSampleRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);

        if (orgDepartRequester == null) {
            handleNoExistingOrganizationDepartRequester(sampleOrder, currentUserId, artifacts);
        } else {
            handleExistingOrganizationDepartRequester(sampleOrder, currentUserId, artifacts, orgDepartRequester);
        }
    }

    private void handleNoExistingOrganizationRequester(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts) {
        SampleRequester orgRequester;
        if (GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteId())
                && GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteName())) {
            return;
        }

        orgRequester = new SampleRequester();
        orgRequester.setRequesterId(Long.parseLong(sampleOrder.getReferringSiteId())); // may be overridden latter
        orgRequester.setSampleId(Long.parseLong(sampleOrder.getSampleId()));
        orgRequester.setRequesterTypeId(RequesterService.Requester.ORGANIZATION.getId());
        orgRequester.setSysUserId(currentUserId);
        artifacts.setSampleOrganizationRequester(orgRequester);

        // Either there is an existing org else a new org
        Organization org;
        if (GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteName())) {
            org = orgService.getOrganizationById(sampleOrder.getReferringSiteId());
            // all of these are reasons to have nothing to do with the organization
            if (GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteCode()) || org == null
                    || sampleOrder.getReferringSiteCode().equals(org.getCode())) {
                return;
            }
        } else {
            org = new Organization();
            org.setIsActive("Y");
            org.setMlsSentinelLabFlag("N");
        }

        org.setOrganizationName(sampleOrder.getReferringSiteName());
        org.setCode(sampleOrder.getReferringSiteCode());
        org.setSysUserId(currentUserId);
        artifacts.setProviderOrganization(org);
    }

    private void handleNoExistingOrganizationDepartRequester(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts) {

        if (!GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteDepartmentId())) {
            Organization org = orgService.getOrganizationById(sampleOrder.getReferringSiteDepartmentId());
            if (org != null) {
                org.setSysUserId(currentUserId);
                artifacts.setProviderDepartmentOrganization(org);

                SampleRequester orgRequester = new SampleRequester();
                orgRequester.setRequesterId(Long.parseLong(sampleOrder.getReferringSiteDepartmentId()));
                orgRequester.setSampleId(Long.parseLong(sampleOrder.getSampleId()));
                orgRequester.setRequesterTypeId(RequesterService.Requester.ORGANIZATION.getId());
                orgRequester.setSysUserId(currentUserId);
                artifacts.setSampleOrganizationDepartRequester(orgRequester);
            }
        }
    }

    private void handleExistingOrganizationRequester(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, SampleRequester orgRequester) {
        if (GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteId())
                && GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteName())) {
            artifacts.setDeletableSampleOrganizationRequester(orgRequester);
            return;
        }

        Organization org = null;
        if (!GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteName())) {
            org = new Organization();
            org.setIsActive("Y");
            org.setMlsSentinelLabFlag("N");
            org.setOrganizationName(sampleOrder.getReferringSiteName());
            org.setCode(sampleOrder.getReferringSiteCode());
            org.setSysUserId(currentUserId);
            artifacts.setProviderOrganization(org);
        } else {
            if (String.valueOf(orgRequester.getRequesterId()).equals(sampleOrder.getReferringSiteId())) {
                org = orgService.getOrganizationById(String.valueOf(orgRequester.getRequesterId()));
                // if (org == null || sampleOrder.getReferringSiteCode() == null
                // || sampleOrder.getReferringSiteCode().equals(org.getCode())) {
                // return;
                // }
                if (org != null) {
                    orgRequester.setRequesterId(orgRequester.getRequesterId());
                    updateExistingOrganizationCode(sampleOrder, currentUserId, artifacts, org);
                }

            } else {
                org = orgService.getOrganizationById(String.valueOf(sampleOrder.getReferringSiteId()));
                if (org != null) {
                    orgRequester.setRequesterId(sampleOrder.getReferringSiteId());
                    updateExistingOrganizationCode(sampleOrder, currentUserId, artifacts, org);
                }
            }
        }
        if (org != null) {
            orgRequester.setSysUserId(currentUserId);
            artifacts.setSampleOrganizationRequester(orgRequester);
        }
    }

    private void handleExistingOrganizationDepartRequester(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, SampleRequester orgRequester) {
        if (GenericValidator.isBlankOrNull(sampleOrder.getReferringSiteDepartmentId())) {
            return;
        }

        Organization org;

        if (String.valueOf(orgRequester.getRequesterId()).equals(sampleOrder.getReferringSiteDepartmentId())) {
            org = orgService.getOrganizationById(String.valueOf(orgRequester.getRequesterId()));

        } else {
            org = orgService.getOrganizationById(String.valueOf(sampleOrder.getReferringSiteDepartmentId()));
        }
        if (org != null) {
            org.setSysUserId(currentUserId);
            artifacts.setProviderDepartmentOrganization(org);
            orgRequester.setSysUserId(currentUserId);
            artifacts.setSampleOrganizationDepartRequester(orgRequester);
        }
    }

    private void updateExistingOrganizationCode(SampleOrderItem sampleOrder, String currentUserId,
            SampleOrderPersistenceArtifacts artifacts, Organization org) {
        if (sampleOrder.getReferringSiteCode() != null && !sampleOrder.getReferringSiteCode().equals(org.getCode())) {
            org.setCode(sampleOrder.getReferringSiteCode());
            org.setSysUserId(currentUserId);
        }
        artifacts.setProviderOrganization(org);
    }

    public class SampleOrderPersistenceArtifacts {
        private Sample sample;
        private Person providerPerson;
        private Provider provider;
        private Organization providerOrganization;
        private Organization providerDepartmentOrganization;
        private SampleRequester deletableSampleOrganizationRequester;
        private List<ObservationHistory> observations = new ArrayList<>();
        private SampleRequester sampleOrganizationRequester;
        private SampleRequester samplePersonRequester;
        private SampleRequester sampleOrganizationDepartRequester;

        public Sample getSample() {
            return sample;
        }

        public void setSample(Sample sample) {
            this.sample = sample;
        }

        public Person getProviderPerson() {
            return providerPerson;
        }

        public void setProviderPerson(Person providerPerson) {
            this.providerPerson = providerPerson;
        }

        public Organization getProviderOrganization() {
            return providerOrganization;
        }

        public void setProviderOrganization(Organization providerOrganization) {
            this.providerOrganization = providerOrganization;
        }

        public List<ObservationHistory> getObservations() {
            return observations;
        }

        public void setObservations(List<ObservationHistory> observations) {
            this.observations = observations;
        }

        public SampleRequester getSampleOrganizationRequester() {
            return sampleOrganizationRequester;
        }

        public void setSampleOrganizationRequester(SampleRequester sampleRequester) {
            sampleOrganizationRequester = sampleRequester;
        }

        public SampleRequester getDeletableSampleOrganizationRequester() {
            return deletableSampleOrganizationRequester;
        }

        public void setDeletableSampleOrganizationRequester(SampleRequester deletableSampleOrganizationRequester) {
            this.deletableSampleOrganizationRequester = deletableSampleOrganizationRequester;
        }

        public SampleRequester getSamplePersonRequester() {
            return samplePersonRequester;
        }

        public void setSamplePersonRequester(SampleRequester samplePersonRequester) {
            this.samplePersonRequester = samplePersonRequester;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public Organization getProviderDepartmentOrganization() {
            return providerDepartmentOrganization;
        }

        public void setProviderDepartmentOrganization(Organization providerDepartmentOrganization) {
            this.providerDepartmentOrganization = providerDepartmentOrganization;
        }

        public SampleRequester getSampleOrganizationDepartRequester() {
            return sampleOrganizationDepartRequester;
        }

        public void setSampleOrganizationDepartRequester(SampleRequester sampleOrganizationDepartRequester) {
            this.sampleOrganizationDepartRequester = sampleOrganizationDepartRequester;
        }
    }
}
