package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TbSampleServiceImpl implements TbSampleService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";

    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleOrganizationService sampleOrganizationService;

    private String sampleId;
    private String patientId;
    private String sampleItemId;

    @Transactional
    @Override
    public boolean persistTbData(SampleTbEntryForm form, HttpServletRequest request) {
        boolean isOK = false;
        try {
            persistPatientData(form);
            createPatientIdentity(form, patientId);
            sampleId = persistSampleData(form);
            persistSampleHumanData(form);
            sampleItemId = persistSampleItemData(form);
            persistAnalysisData(form);
            persistSampleOrganizationData(form);
            persistObservations(form);
            isOK = true;
        } catch (Exception e) {
            isOK = false;
            throw e;
        }
        return isOK;
    }

    private List<String> persistObservations(SampleTbEntryForm formData) {
        List<ObservationHistory> obervations = new ArrayList<ObservationHistory>();

        // tb order reason
        ObservationHistory orderReason = new ObservationHistory();
        orderReason.setSampleId(sampleId);
        orderReason.setPatientId(patientId);
        orderReason.setLastupdated(DateUtil.getNowAsTimestamp());
        orderReason.setSysUserId(formData.getSysUserId());
        orderReason.setValueType(ValueType.DICTIONARY);
        orderReason.setValue(formData.getTbOrderReason());
        orderReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbOrderReason"));
        obervations.add(orderReason);
        // tb diagnostic reason
        ObservationHistory diagnosticReason = new ObservationHistory();
        diagnosticReason.setSampleId(sampleId);
        diagnosticReason.setPatientId(patientId);
        diagnosticReason.setLastupdated(DateUtil.getNowAsTimestamp());
        diagnosticReason.setSysUserId(formData.getSysUserId());
        diagnosticReason.setValueType(ValueType.DICTIONARY);
        diagnosticReason.setValue(formData.getTbDiagnosticReason());
        diagnosticReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbDiagnosticReason"));
        obervations.add(diagnosticReason);
        // tb followup reason
        ObservationHistory tbFollowupReason = new ObservationHistory();
        tbFollowupReason.setSampleId(sampleId);
        tbFollowupReason.setPatientId(patientId);
        tbFollowupReason.setLastupdated(DateUtil.getNowAsTimestamp());
        tbFollowupReason.setSysUserId(formData.getSysUserId());
        tbFollowupReason.setValueType(ValueType.DICTIONARY);
        tbFollowupReason.setValue(formData.getTbFollowupReason());
        tbFollowupReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReason"));
        obervations.add(tbFollowupReason);
        // tb sample aspect
        ObservationHistory tbAspect = new ObservationHistory();
        tbAspect.setSampleId(sampleId);
        tbAspect.setPatientId(patientId);
        tbAspect.setLastupdated(DateUtil.getNowAsTimestamp());
        tbAspect.setSysUserId(formData.getSysUserId());
        tbAspect.setValueType(ValueType.DICTIONARY);
        tbAspect.setValue(formData.getTbAspect());
        tbAspect.setObservationHistoryTypeId(getObservationHistoryTypeId("TbSampleAspects"));
        obervations.add(tbAspect);
        // tb follwup period Line 1
        ObservationHistory tbFollowupReasonPeriodLine1 = new ObservationHistory();
        tbFollowupReasonPeriodLine1.setSampleId(sampleId);
        tbFollowupReasonPeriodLine1.setPatientId(patientId);
        tbFollowupReasonPeriodLine1.setLastupdated(DateUtil.getNowAsTimestamp());
        tbFollowupReasonPeriodLine1.setSysUserId(formData.getSysUserId());
        tbFollowupReasonPeriodLine1.setValueType(ValueType.LITERAL);
        tbFollowupReasonPeriodLine1.setValue(formData.getTbFollowupPeriodLine1());
        tbFollowupReasonPeriodLine1
                .setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine1"));
        obervations.add(tbFollowupReasonPeriodLine1);
        // tb follwup period Line 2
        ObservationHistory tbFollowupReasonPeriodLine2 = new ObservationHistory();
        tbFollowupReasonPeriodLine2.setSampleId(sampleId);
        tbFollowupReasonPeriodLine2.setPatientId(patientId);
        tbFollowupReasonPeriodLine2.setLastupdated(DateUtil.getNowAsTimestamp());
        tbFollowupReasonPeriodLine2.setSysUserId(formData.getSysUserId());
        tbFollowupReasonPeriodLine2.setValueType(ValueType.LITERAL);
        tbFollowupReasonPeriodLine2.setValue(formData.getTbFollowupPeriodLine2());
        tbFollowupReasonPeriodLine2
                .setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine2"));
        obervations.add(tbFollowupReasonPeriodLine2);
        // tb Analysis Method
        ObservationHistory analysisMethod = new ObservationHistory();
        analysisMethod.setSampleId(sampleId);
        analysisMethod.setPatientId(patientId);
        analysisMethod.setLastupdated(DateUtil.getNowAsTimestamp());
        analysisMethod.setSysUserId(formData.getSysUserId());
        analysisMethod.setValueType(ValueType.DICTIONARY);
        analysisMethod.setValue(formData.getSelectedTbMethod());
        analysisMethod.setObservationHistoryTypeId(getObservationHistoryTypeId("TbAnalysisMethod"));
        obervations.add(analysisMethod);
        return observationHistoryService.insertAll(obervations);
    }

    private Patient persistPatientData(SampleTbEntryForm formData) {
        Patient oldPatient = null;
        if (!GenericValidator.isBlankOrNull(formData.getTbSubjectNumber())) {
            oldPatient = patientService.getByExternalId(formData.getTbSubjectNumber());
        }
        if (ObjectUtils.isEmpty(oldPatient)) {
            Patient patient = new Patient();
            patient.setPerson(createPersonAndAddress(formData));
            patient.setExternalId(formData.getTbSubjectNumber());
            patient.setNationalId(formData.getTbSubjectNumber());
            patient.setBirthDateForDisplay(formData.getPatientBirthDate());
            patient.setBirthDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getPatientBirthDate()));
            patient.setGender(formData.getPatientGender());
            patient.setSysUserId(formData.getSysUserId());
            patientId = patientService.insert(patient);
            patient.setId(patientId);
            return patient;
        } else {
            // update
            oldPatient.setPerson(createPersonAndAddress(formData));
            oldPatient.setBirthDateForDisplay(formData.getPatientBirthDate());
            oldPatient.setBirthDate(DateUtil.convertStringDateToTimestamp(formData.getPatientBirthDate() + " 00:00"));
            oldPatient.setGender(formData.getPatientGender());
            oldPatient.setSysUserId(formData.getSysUserId());
            oldPatient = patientService.update(oldPatient);
            patientId = oldPatient.getId();
            return oldPatient;
        }
    }

    // create a new Person
    private Person createPersonAndAddress(SampleTbEntryForm formData) {
        Person person = new Person();
        person.setFirstName(formData.getPatientFirstName());
        person.setLastName(formData.getPatientLastName());
        person.setLastupdatedFields();
        person.setSysUserId(formData.getSysUserId());
        String personId = personService.insert(person);
        person.setId(personId);
        createPersonAddresses(formData, personId);
        return person;
    }

    // create a new Person
    private String createPatientIdentity(SampleTbEntryForm formData, String patientId) {
        String typeID = PatientIdentityTypeMap.getInstance().getIDForType("SUBJECT");
        PatientIdentity patientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType(patientId,
                typeID);
        if (ObjectUtils.isEmpty(patientIdentity)) {
            patientIdentity = new PatientIdentity();
            patientIdentity.setPatientId(patientId);
            patientIdentity.setIdentityData(formData.getTbSubjectNumber());
            patientIdentity.setLastupdated(DateUtil.getNowAsTimestamp());
            patientIdentity.setIdentityTypeId(typeID);
            return patientIdentityService.insert(patientIdentity);
        } else {
            return patientIdentity.getId();
        }
    }

    private String createPersonAndProvider(SampleTbEntryForm formData) {
        Person person = new Person();
        person.setFirstName(formData.getProviderFirstName());
        person.setLastName(formData.getProviderLastName());
        person.setLastupdatedFields();
        person.setSysUserId(formData.getSysUserId());
        String personId = personService.insert(person);
        person.setId(personId);
        Provider provider = new Provider();
        provider.setPerson(person);
        provider.setLastupdated(DateUtil.getNowAsTimestamp());
        return providerService.insert(provider);
    }

    // create a new Person
    private void createPersonAddresses(SampleTbEntryForm formData, String personId) {
        // define addresses
        List<PersonAddress> existingAddresses = personAddressService.getAddressPartsByPersonId(personId);
        if (ObjectUtils.isEmpty(existingAddresses)) {
            PersonAddress patientPhone = new PersonAddress();
            patientPhone.setPersonId(personId);
            patientPhone.setAddressPartId(addressPartService.getAddresPartByName("phone").getId());
            patientPhone.setType("T");
            patientPhone.setValue(formData.getPatientPhone());
            patientPhone.setSysUserId(formData.getSysUserId());
            patientPhone.setLastupdatedFields();
            personAddressService.insert(patientPhone);
            PersonAddress patientStreetAddress = new PersonAddress();
            patientStreetAddress.setPersonId(personId);
            patientStreetAddress.setAddressPartId(addressPartService.getAddresPartByName("street").getId());
            patientStreetAddress.setType("T");
            patientStreetAddress.setValue(formData.getPatientAddress());
            patientStreetAddress.setSysUserId(formData.getSysUserId());
            patientStreetAddress.setLastupdatedFields();
            personAddressService.insert(patientStreetAddress);
        } else {
            // update adresses
            existingAddresses.forEach(address -> {
                if (address.getAddressPartId().equals(addressPartService.getAddresPartByName("phone").getId())) {
                    address.setValue(formData.getPatientPhone());
                }
                if (address.getAddressPartId().equals(addressPartService.getAddresPartByName("street").getId())) {
                    address.setValue(formData.getPatientAddress());
                }
                personAddressService.update(address);
            });
        }
    }

    private String persistSampleData(SampleTbEntryForm formData) {
        Sample sample = new Sample();
        if (ObjectUtils.isEmpty(formData.getSampleId())) {
            // create a new Sample
            sample.setAccessionNumber(formData.getLabNo());
            sample.setCollectionDateForDisplay(formData.getRequestDate());
            sample.setCollectionDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getRequestDate()));
            sample.setReceivedDateForDisplay(formData.getReceivedDate());
            sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(formData.getReceivedDate()));
            sample.setEnteredDate(DateUtil.getNowAsSqlDate());
            sample.setDomain("H");
            sample.setSysUserId(formData.getSysUserId());
            sample.setLastupdated(DateUtil.getNowAsTimestamp());
            sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
            sample.setPriority(OrderPriority.ROUTINE);
            return sampleService.insert(sample);
        } else {
            // update
            sample.setCollectionDateForDisplay(formData.getRequestDate());
            sample.setCollectionDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getRequestDate()));
            sample.setReceivedDateForDisplay(formData.getReceivedDate());
            sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(formData.getReceivedDate()));
            sample.setEnteredDate(DateUtil.getNowAsSqlDate());
            sample.setSysUserId(formData.getSysUserId());
            sample.setLastupdated(DateUtil.getNowAsTimestamp());
            sample.setPriority(OrderPriority.ROUTINE);
            return sampleService.update(sample).getId();
        }
    }

    private String persistSampleHumanData(SampleTbEntryForm formData) {
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sampleId);
        sampleHuman.setPatientId(patientId);
        sampleHuman.setLastupdated(DateUtil.getNowAsTimestamp());
        sampleHuman.setSysUserId(formData.getSysUserId());
        sampleHuman.setProviderId(createPersonAndProvider(formData));
        return sampleHumanService.insert(sampleHuman);
    }

    private String persistSampleItemData(SampleTbEntryForm formData) {
        SampleItem item = new SampleItem();
        if (!ObjectUtils.isEmpty(sampleId)) {
            List<SampleItem> oldSampleItems = sampleItemService.getSampleItemsBySampleId(sampleId);
            if (!ObjectUtils.isEmpty(oldSampleItems)) {
                SampleItem oldSampleItem = oldSampleItems.get(0);
                oldSampleItem.setSample(sampleService.get(sampleId));
                oldSampleItem.setLastupdated(DateUtil.getNowAsTimestamp());
                oldSampleItem.setSysUserId(formData.getSysUserId());
                oldSampleItem.setTypeOfSample(typeOfSampleService.get(formData.getTbSpecimenNature()));
                return sampleItemService.update(oldSampleItem).getId();
            }
        }
        item.setSample(sampleService.get(sampleId));
        item.setLastupdated(DateUtil.getNowAsTimestamp());
        item.setTypeOfSample(typeOfSampleService.get(formData.getTbSpecimenNature()));
        item.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered));
        item.setSortOrder(Integer.toString(1));
        item.setSysUserId(formData.getSysUserId());
        return sampleItemService.insert(item);
    }

    private List<String> persistAnalysisData(SampleTbEntryForm formData) {
        List<Analysis> analysisItems = new ArrayList<Analysis>();
        for (String testId : formData.getNewSelectedTests()) {
            Analysis analysis = new Analysis();
            Test test = testService.get(testId);
            SampleItem sampleItem = sampleItemService.get(sampleItemId);
            analysis.setSampleItem(sampleItem);
            analysis.setTest(test);
            analysis.setRevision("0");
            analysis.setTestSection(testSectionService.getTestSectionByName("TB"));
            analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            analysis.setIsReportable(test.getIsReportable());
            analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
            analysis.setStartedDate(DateUtil.getNowAsSqlDate());
            analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
            analysis.setSysUserId(formData.getSysUserId());
            analysis.setFhirUuid(UUID.randomUUID());
            analysis.setSampleTypeName(typeOfSampleService.get(formData.getTbSpecimenNature()).getDescription());
            analysisItems.add(analysis);
        }
        return analysisService.insertAll(analysisItems);
    }

    private String persistSampleOrganizationData(SampleTbEntryForm formData) {
        SampleOrganization sampleOrganization = new SampleOrganization();
        if (ObjectUtils.isNotEmpty(formData.getSampleId())) {
            sampleOrganization = sampleOrganizationService.getDataBySample(sampleService.get(formData.getSampleId()));
            if (ObjectUtils.isNotEmpty(sampleOrganization)) {
                sampleOrganization.setLastupdated(DateUtil.getNowAsTimestamp());
                sampleOrganization.setSysUserId(formData.getSysUserId());
                sampleOrganization.setSample(sampleService.get(sampleId));
                sampleOrganization.setSysUserId(formData.getSysUserId());
                sampleOrganization.setOrganization(organizationService.get(formData.getReferringSiteCode()));
                return sampleOrganizationService.update(sampleOrganization).getId();
            }
        }
        sampleOrganization.setLastupdated(DateUtil.getNowAsTimestamp());
        sampleOrganization.setSysUserId(formData.getSysUserId());
        sampleOrganization.setSample(sampleService.get(sampleId));
        sampleOrganization.setSysUserId(formData.getSysUserId());
        sampleOrganization.setOrganization(organizationService.get(formData.getReferringSiteCode()));
        return sampleOrganizationService.insert(sampleOrganization);
    }

    private String getObservationHistoryTypeId(String name) {
        ObservationHistoryType oht;
        oht = observationHistoryTypeService.getByName(name);
        if (oht != null) {
            return oht.getId();
        }
        return null;
    }

    @Override
    public void getTBFormData(SampleTbEntryForm form) {
        String labnoForSearch = form.getLabnoForSearch();
        if (ObjectUtils.isNotEmpty(labnoForSearch)) {
            Sample searchSample = sampleService.getSampleByAccessionNumber(labnoForSearch);
        }
    }
}
