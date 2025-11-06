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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.qaevent.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.QAService.QAObservationValueType;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qaevent.service.QaObservationService;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

@Service
@Scope("prototype")
public class NonConformityUpdateWorker implements INonConformityUpdateWorker {

    private Sample sample;
    private SampleHuman sampleHuman;
    private Patient patient;
    private ObservationHistory doctorObservation;
    private ObservationHistory serviceObservation;
    private Map<String, SampleItem> sampleItemsByType = new HashMap<>();

    private List<SampleQaEvent> sampleQAEventInsertList;
    private List<SampleQaEvent> sampleQAEventDeleteList;
    private Map<QaObservation, SampleQaEvent> qaObservationMap;
    private List<NoteSet> insertableNotes;
    private List<Note> updateableNotes;
    private List<Note> deleteableNotes;
    private SampleProject sampleProject;
    private List<PersonAddress> addressPartList;
    private PatientIdentity subjectNoPatientIdentity;
    private PatientIdentity STNoPatientIdentity;

    private boolean insertPatient = false;
    private boolean insertDoctorObservation = false;
    private boolean insertServiceObservation = false;
    private boolean insertSampleItems = false;
    private boolean insertSampleProject = false;
    private boolean insertSampleRequester = false;

    private boolean updatePatient = false;
    private boolean useFullProviderInfo;
    private boolean updateSampleHuman;

    public static final String NOTE_SUBJECT = "QaEvent Note";
    private static final boolean REJECT_IF_EMPTY = true;

    @Autowired
    private ObservationHistoryService observationService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private QaObservationService qaObservationService;
    @Autowired
    private OrganizationService orgService;
    // private final OrganizationOrganizationTypeService orgOrgTypeService ;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private SampleProjectService sampleProjectService;
    @Autowired
    private SystemUserService systemUserService;

    private Provider provider;
    private Person providerPerson;
    private boolean insertProvider = false;
    private Errors errors = new BaseErrors();
    private Organization newOrganization = null;
    private boolean insertNewOrganizaiton = false;
    private SampleRequester sampleRequester = null;

    private NonConformityUpdateData webData;

    public NonConformityUpdateWorker(NonConformityUpdateData data) {
        webData = data;
    }

    public NonConformityUpdateWorker() {
    }

    @Override
    public void setWebData(NonConformityUpdateData data) {
        webData = data;
    }

    @Override
    @Transactional
    public String update() {
        useFullProviderInfo = FormFields.getInstance().useField(FormFields.Field.QA_FULL_PROVIDER_INFO);

        createSystemUser();
        clearMembers();

        boolean isNewSample = GenericValidator.isBlankOrNull(webData.getSampleId());

        if (isNewSample) {
            createNewArtifacts();
        } else {
            updateArtifacts();
        }

        try {
            if (insertPatient) {
                patientService.insert(patient);
            } else if (updatePatient) {
                patientService.update(patient);
            }

            if (insertProvider) {
                persistProviderData();
            }

            if (subjectNoPatientIdentity != null) {
                subjectNoPatientIdentity.setPatientId(patient.getId());
                if (GenericValidator.isBlankOrNull(subjectNoPatientIdentity.getId())) {
                    patientIdentityService.insert(subjectNoPatientIdentity);
                } else {
                    patientIdentityService.update(subjectNoPatientIdentity);
                }
            }

            if (STNoPatientIdentity != null) {
                STNoPatientIdentity.setPatientId(patient.getId());
                if (GenericValidator.isBlankOrNull(STNoPatientIdentity.getId())) {
                    patientIdentityService.insert(STNoPatientIdentity);
                } else {
                    patientIdentityService.update(STNoPatientIdentity);
                }
            }

            if (isNewSample) {
                sampleService.insertDataWithAccessionNumber(sample);

                sampleHuman.setPatientId(patient.getId());
                sampleHuman.setSampleId(sample.getId());
                sampleHuman.setProviderId((provider == null) ? null : provider.getId());
                sampleHumanService.insert(sampleHuman);
            } else {
                sampleService.update(sample);
            }

            if (updateSampleHuman) {
                sampleHuman.setProviderId(provider.getId());
                sampleHuman.setSampleId(sample.getId());
                sampleHuman.setPatientId(patient.getId());
                sampleHuman.setSysUserId(webData.getCurrentSysUserId());
                sampleHumanService.update(sampleHuman);
            }

            if (insertSampleItems) {
                for (SampleItem si : sampleItemsByType.values()) {
                    if (si != null) {
                        sampleItemService.insert(si);
                    }
                }
            }

            if (insertSampleProject) {
                sampleProjectService.insert(sampleProject);
            }

            if (insertDoctorObservation) {
                doctorObservation.setPatientId(patient.getId());
                doctorObservation.setSampleId(sample.getId());
                observationService.insert(doctorObservation);
            }

            if (insertServiceObservation) {
                serviceObservation.setPatientId(patient.getId());
                serviceObservation.setSampleId(sample.getId());
                observationService.insert(serviceObservation);
            }

            if (insertNewOrganizaiton) {
                orgService.insert(newOrganization);
                orgService.linkOrganizationAndType(newOrganization, TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
            }

            if (insertSampleRequester) {
                if (insertNewOrganizaiton) {
                    sampleRequester.setRequesterId(newOrganization.getId());
                }
                sampleRequester.setSampleId(Long.parseLong(sample.getId()));
                sampleRequesterService.insert(sampleRequester);
            }

            for (SampleQaEvent event : sampleQAEventInsertList) {
                event.setSample(sample);
                sampleQaEventService.insert(event);
            }

            for (NoteSet noteSet : insertableNotes) {
                if (noteSet.referencedEvent != null) {
                    noteSet.note.setReferenceId(noteSet.referencedEvent.getId());
                } else if (noteSet.referencedSample != null) {
                    noteSet.note.setReferenceId(noteSet.referencedSample.getId());
                } else {
                    continue;
                }
                noteService.insert(noteSet.note);
            }

            for (Note note : updateableNotes) {
                noteService.update(note);
            }

            noteService.deleteAll(deleteableNotes);
            sampleQaEventService.deleteAll(sampleQAEventDeleteList);

            for (QaObservation qa : qaObservationMap.keySet()) {
                if (qa.getId() == null) {
                    qa.setObservedId(qaObservationMap.get(qa).getId());
                    qaObservationService.insert(qa);
                } else {
                    qaObservationService.update(qa);
                }
            }

        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof StaleObjectStateException) {
                errors.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                LogEvent.logDebug(e);
                errors.reject("errors.UpdateException", "errors.UpdateException");
            }
            throw e;
        }

        return IActionConstants.FWD_SUCCESS_INSERT;
    }

    @Override
    public Errors getErrors() {
        return errors;
    }

    private void createSystemUser() {
        SystemUser systemUser = new SystemUser();
        systemUser.setId(webData.getCurrentSysUserId());
        systemUserService.getData(systemUser);
    }

    private void clearMembers() {
        sample = null;
        sampleHuman = null;
        patient = null;
        doctorObservation = null;
        serviceObservation = null;
        sampleItemsByType = new HashMap<>();
        sampleQAEventInsertList = new ArrayList<>();
        sampleQAEventDeleteList = new ArrayList<>();
        qaObservationMap = new HashMap<>();
        insertableNotes = new ArrayList<>();
        updateableNotes = new ArrayList<>();
        deleteableNotes = new ArrayList<>();
        sampleProject = null;

        insertPatient = false;
        insertDoctorObservation = false;
        insertServiceObservation = false;
        insertSampleItems = false;
        insertSampleProject = false;
        insertSampleRequester = false;
        updateSampleHuman = false;
    }

    private void createNewArtifacts() {
        sample = new Sample();
        sample.setSysUserId(webData.getCurrentSysUserId());
        sample.setAccessionNumber(webData.getLabNo());
        sample.setDomain("H");
        sample.setEnteredDate(DateUtil.convertStringDateToSqlDate(getCompleteDateTime()));
        sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(getCompleteDateTime()));
        sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
        sample.setReferringId(webData.getRequesterSpecimanID());

        sampleHuman = new SampleHuman();
        sampleHuman.setSysUserId(webData.getCurrentSysUserId());

        if (!(GenericValidator.isBlankOrNull(webData.getProjectId()) || webData.getProjectId().equals("0"))) {
            insertSampleProject = true;
            sampleProject = new SampleProject();

            Project project = new Project();
            project.setId(webData.getProjectId());

            projectService.getData(project);
            sampleProject.setProject(project);
            sampleProject.setSample(sample);
            sampleProject.setSysUserId(webData.getCurrentSysUserId());
        }

        setPatient();
        addPatientIdentityIfNeeded();
        addDoctorIfNeeded();
        addServiceIfNeeded();
        addAllSampleQaEvents();
        addNoteToSampleIfNeeded();
    }

    private String getCompleteDateTime() {
        String receivedDateTime = webData.getReceivedDate();
        if (!GenericValidator.isBlankOrNull(webData.getReceivedTime())) {
            receivedDateTime += " " + webData.getReceivedTime();
        } else {
            receivedDateTime += " 00:00";
        }
        return receivedDateTime;
    }

    private void updateArtifacts() {
        sample = new Sample();
        sample.setId(webData.getSampleId());
        sampleService.getData(sample);
        sample.setSysUserId(webData.getCurrentSysUserId());
        sample.setReferringId(webData.getRequesterSpecimanID());

        sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        sampleHuman = sampleHumanService.getDataBySample(sampleHuman);

        String projectId = webData.getProjectId();
        if (!GenericValidator.isBlankOrNull(projectId)) {
            if (!(GenericValidator.isBlankOrNull(projectId) || projectId.equals("0"))) {
                insertSampleProject = true;
                sampleProject = new SampleProject();

                Project project = new Project();
                project.setId(projectId);

                projectService.getData(project);
                sampleProject.setProject(project);
                sampleProject.setSample(sample);
                sampleProject.setSysUserId(webData.getCurrentSysUserId());
            }
        } else {
            insertSampleProject = false;
            sampleProject = null;
        }

        if (webData.getNewNationalId() && !GenericValidator.isBlankOrNull(webData.getNationalId())) { // number
            updatePatient = true;
            patient = sampleHumanService.getPatientForSample(sample);
            patient.setSysUserId(webData.getCurrentSysUserId());
            patient.setNationalId(webData.getNationalId());
        }

        if (webData.getNewSubject() && !GenericValidator.isBlankOrNull(webData.getSubjectNo())) {
            if (patient == null) {
                patient = sampleHumanService.getPatientForSample(sample);
            }

            subjectNoPatientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType(patient.getId(),
                    TableIdService.getInstance().PATIENT_SUBJECT_IDENTITY);
            if (subjectNoPatientIdentity == null) {
                subjectNoPatientIdentity = new PatientIdentity();
            }

            subjectNoPatientIdentity.setSysUserId(webData.getCurrentSysUserId());
            subjectNoPatientIdentity.setIdentityData(webData.getSubjectNo());
            subjectNoPatientIdentity.setIdentityTypeId(TableIdService.getInstance().PATIENT_SUBJECT_IDENTITY);
        }

        if (webData.getNewSTNumber() && !GenericValidator.isBlankOrNull(webData.getSTNumber())) {
            if (patient == null) {
                patient = sampleHumanService.getPatientForSample(sample);
            }

            STNoPatientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType(patient.getId(),
                    TableIdService.getInstance().PATIENT_ST_IDENTITY);
            if (STNoPatientIdentity == null) {
                STNoPatientIdentity = new PatientIdentity();
            }

            STNoPatientIdentity.setSysUserId(webData.getCurrentSysUserId());
            STNoPatientIdentity.setIdentityData(webData.getSTNumber());
            STNoPatientIdentity.setIdentityTypeId(TableIdService.getInstance().PATIENT_ST_IDENTITY);
        }

        if (patient == null || GenericValidator.isBlankOrNull(patient.getId())) {
            setPatient();
        }

        addDoctorIfNeeded();
        addServiceIfNeeded();
        addAllSampleQaEvents();
        addNoteToSampleIfNeeded();
    }

    private void persistProviderData() {
        if (providerPerson != null) {
            providerPerson.setSysUserId(webData.getCurrentSysUserId());
            personService.insert(providerPerson);
        }
        if (provider != null) {
            provider.setSysUserId(webData.getCurrentSysUserId());
            if (providerPerson != null) {
                provider.setPerson(providerPerson);
            }
            providerService.insert(provider);
        }

        if (providerPerson != null) {
            for (PersonAddress addressPart : addressPartList) {
                addressPart.setPersonId(providerPerson.getId());
                personAddressService.insert(addressPart);
            }
        }
    }

    /** This is for when patients can not be added through the form */
    private void setPatient() {

        if (!GenericValidator.isBlankOrNull(webData.getPatientId())) {
            patient = patientService.readPatient(webData.getPatientId());
        } else if (!GenericValidator.isBlankOrNull(webData.getNationalId())) {
            patient = patientService.getPatientByNationalId(webData.getNationalId());
        }

        if (patient == null) {
            insertPatient = true;
            patient = new Patient();
            patient.setNationalId(webData.getNationalId());
            patient.setPerson(PatientUtil.getUnknownPerson());
        }

        patient.setSysUserId(webData.getCurrentSysUserId());
    }

    private void addPatientIdentityIfNeeded() {
        if (webData.getNewSubject() && !GenericValidator.isBlankOrNull(webData.getSubjectNo())) {
            subjectNoPatientIdentity = new PatientIdentity();
            subjectNoPatientIdentity.setSysUserId(webData.getCurrentSysUserId());
            subjectNoPatientIdentity.setIdentityData(webData.getSubjectNo());
            subjectNoPatientIdentity.setIdentityTypeId(TableIdService.getInstance().PATIENT_SUBJECT_IDENTITY);
        }

        if (webData.getNewSTNumber() && !GenericValidator.isBlankOrNull(webData.getSTNumber())) {
            STNoPatientIdentity = new PatientIdentity();
            STNoPatientIdentity.setSysUserId(webData.getCurrentSysUserId());
            STNoPatientIdentity.setIdentityData(webData.getSTNumber());
            STNoPatientIdentity.setIdentityTypeId(TableIdService.getInstance().PATIENT_ST_IDENTITY);
        }
    }

    private void addDoctorIfNeeded() {

        if (useFullProviderInfo) {
            insertProvider = true;
            initProvider();
        } else {
            String doctor = newOrBlankFieldValue(webData.getNewDoctor(), webData.getDoctor());
            if (GenericValidator.isBlankOrNull(doctor)) {
                return;
            }
            insertDoctorObservation = true;
            doctorObservation = new ObservationHistory();
            doctorObservation.setValue(doctor);
            doctorObservation.setValueType(ValueType.LITERAL);
            doctorObservation.setSysUserId(webData.getCurrentSysUserId());
            doctorObservation.setObservationHistoryTypeId(TableIdService.getInstance().DOCTOR_OBSERVATION_TYPE_ID);
        }
    }

    private void initProvider() {
        addressPartList = new ArrayList<>();

        if (webData.noRequesterInformation()) {
            Person person = PatientUtil.getUnknownPerson();
            provider = new Provider();
            provider.setPerson(person);
            providerPerson = null;
            updateSampleHuman = true;
            insertProvider = true;
        } else {
            providerPerson = new Person();
            provider = new Provider();

            providerPerson.setFirstName(webData.getRequesterFirstName());
            providerPerson.setLastName(webData.getRequesterLastName());
            providerPerson.setStreetAddress(webData.getRequesterStreetAddress());
            providerPerson.setWorkPhone(webData.getRequesterPhoneNumber());
            providerPerson.setSysUserId(webData.getCurrentSysUserId());

            // provider.setExternalId(webData.getRequesterSpecimanID());
            provider.setSysUserId(webData.getCurrentSysUserId());
            updateSampleHuman = true;
            sampleHuman.setSysUserId(webData.getCurrentSysUserId());
            insertProvider = true;

            addAddressPart(webData.getRequesterCommune(), TableIdService.getInstance().ADDRESS_COMMUNE_ID,
                    addressPartList, "T");
            addAddressPart(webData.getRequesterVillage(), TableIdService.getInstance().ADDRESS_VILLAGE_ID,
                    addressPartList, "T");
            addAddressPart(webData.getRequesterDepartment(), TableIdService.getInstance().ADDRESS_DEPARTMENT_ID,
                    addressPartList, "D");
        }
    }

    private void addAddressPart(String value, String partId, List<PersonAddress> addressPartList, String type) {
        if (!GenericValidator.isBlankOrNull(value) && !("D".equals(type) && "0".equals(value))) {
            PersonAddress addressPart = new PersonAddress();
            addressPart.setAddressPartId(partId);
            addressPart.setValue(value);
            addressPart.setType(type);
            addressPart.setSysUserId(webData.getCurrentSysUserId());
            addressPartList.add(addressPart);
        }
    }

    private void addServiceIfNeeded() {

        if (!webData.getNewService()) {
            return;
        }

        String service = webData.getService();
        if (GenericValidator.isBlankOrNull(service)) {
            service = webData.getNewServiceName();
            if (GenericValidator.isBlankOrNull(service)) {
                return;
            }

            if (useFullProviderInfo) {
                newOrganization = new Organization();
                newOrganization.setIsActive("Y");
                newOrganization.setOrganizationName(service);
                newOrganization.setSysUserId(webData.getCurrentSysUserId());
                newOrganization.setMlsSentinelLabFlag("N");
                insertNewOrganizaiton = true;
            }
        }

        if (useFullProviderInfo) {
            sampleRequester = new SampleRequester();
            if (!insertNewOrganizaiton) {
                sampleRequester.setRequesterId(service);
            }
            sampleRequester.setRequesterTypeId(TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
            sampleRequester.setSysUserId(webData.getCurrentSysUserId());
            insertSampleRequester = true;
        } else {
            serviceObservation = new ObservationHistory();
            serviceObservation.setValue(service);
            serviceObservation.setValueType(ValueType.LITERAL);
            serviceObservation.setSysUserId(webData.getCurrentSysUserId());
            serviceObservation.setObservationHistoryTypeId(TableIdService.getInstance().SERVICE_OBSERVATION_TYPE_ID);
            insertServiceObservation = true;
        }
    }

    private void addAllSampleQaEvents() {

        for (QaEventItem item : webData.getQaEvents()) {
            // New event
            if (isNonBlankNewEvent(item)) {
                String sampleType = item.getSampleType();
                // All samples has an id of -1
                if (sampleType.equals("-1")) {
                    addSampleQaEvent(item, null);
                } else {
                    insertSampleItems = true;
                    SampleItem sampleItem = sampleItemsByType.get(sampleType);
                    sampleItem = addSampleQaEventForItem(item, sampleItem, sampleType);
                    sampleItemsByType.put(sampleType, sampleItem);
                }
                // Marked for removal
            } else if (isOldForRemoval(item)) {
                SampleQaEvent sampleQaEvent = new SampleQaEvent();
                sampleQaEvent.setId(item.getId());
                sampleQaEvent.setSysUserId(webData.getCurrentSysUserId());
                sampleQAEventDeleteList.add(sampleQaEvent);
                Note existingNote = findExistingQANote(item.getId());
                if (existingNote != null) {
                    existingNote.setSysUserId(webData.getCurrentSysUserId());
                    deleteableNotes.add(existingNote);
                }
                // Updated note
            } else {
                SampleQaEvent sampleQaEvent = new SampleQaEvent();
                sampleQaEvent.setId(item.getId());
                Note existingNote = findExistingQANote(item.getId());
                if (existingNote == null) {
                    addNoteIfNeeded(item.getNote(), sampleQaEvent);
                } else {
                    if ((existingNote.getText() != null && !existingNote.getText().equals(item.getNote()))
                            || (existingNote.getText() == null && item.getNote() != null)) {
                        existingNote.setText(item.getNote());
                        existingNote.setSystemUserId(webData.getCurrentSysUserId());
                        existingNote.setSysUserId(webData.getCurrentSysUserId());
                        updateableNotes.add(existingNote);
                    }
                }
            }
        }
    }

    private void addNoteToSampleIfNeeded() {
        String noteText = webData.getNoteText(); // newOrBlankFieldValue(webData.getNewNoteText(),
        // webData.getNoteText());
        if (!GenericValidator.isBlankOrNull(noteText)) {
            Note note = findExistingSampleNote();
            if (note != null) {
                if (!note.getText().equals(noteText)) {
                    note.setText(noteText);
                    note.setSysUserId(webData.getCurrentSysUserId());
                    note.setSystemUserId(webData.getCurrentSysUserId());
                    updateableNotes.add(note);
                }
            } else {
                NoteSet noteSet = new NoteSet();
                noteSet.referencedSample = sample;
                NoteService noteService = SpringContext.getBean(NoteService.class);
                noteSet.note = noteService.createSavableNote(sample, NoteServiceImpl.NoteType.NON_CONFORMITY, noteText,
                        NOTE_SUBJECT, webData.getCurrentSysUserId());
                insertableNotes.add(noteSet);
            }
        }
    }

    private SampleQaEvent addSampleQaEvent(QaEventItem item, SampleItem sampleItem) {
        QAService qaService = new QAService(new SampleQaEvent());
        qaService.setCurrentUserId(webData.getCurrentSysUserId());
        qaService.setReportTime(getCompleteDateTime());
        qaService.setQaEventById(item.getQaEvent());
        qaService.setObservation(QAObservationType.SECTION, item.getSection(), QAObservationValueType.KEY,
                REJECT_IF_EMPTY);
        qaService.setObservation(QAObservationType.AUTHORIZER, item.getAuthorizer(), QAObservationValueType.LITERAL,
                REJECT_IF_EMPTY);
        qaService.setObservation(QAObservationType.DOC_NUMBER, item.getRecordNumber(), QAObservationValueType.LITERAL,
                REJECT_IF_EMPTY);
        qaService.setSampleItem(sampleItem);
        addNoteIfNeeded(item.getNote(), qaService.getSampleQaEvent());

        sampleQAEventInsertList.add(qaService.getSampleQaEvent());

        for (QaObservation observation : qaService.getUpdatedObservations()) {
            qaObservationMap.put(observation, qaService.getSampleQaEvent());
        }

        return qaService.getSampleQaEvent();
    }

    /**
     * @param item         The Item to be evaluated
     * @param sampleItem   The item being checked for existence
     * @param sampleTypeId The type of the sample item
     * @return if the return is null, the there was already a sampleItem on the
     *         Sample for the given sampleTypeId
     */
    private SampleItem addSampleQaEventForItem(QaEventItem item, SampleItem sampleItem, String sampleTypeId) {
        List<SampleItem> sampleItemsOfType = new ArrayList<>();
        if (sampleItem == null) {

            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeId);

            if (sample.getId() != null) {
                sampleItemsOfType = sampleItemService.getSampleItemsBySampleIdAndType(sample.getId(), typeOfSample);
            }
            if (sampleItemsOfType != null && sampleItemsOfType.size() > 0) {
                // ignoring any sample which actually has more than one
                // sampleItem of the same typeOfSample
                sampleItem = sampleItemsOfType.get(0);
            } else {
                sampleItem = new SampleItem();
                sampleItem.setSample(sample);
                sampleItem.setSysUserId(webData.getCurrentSysUserId());
                sampleItem.setTypeOfSample(typeOfSample);
                sampleItem.setSortOrder(getNextSampleItemSortOrder());
                sampleItem.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered));
            }
        }
        addSampleQaEvent(item, sampleItem);
        // if the DB already has this sample type don't bother returning it to
        // the caller (who will want to save it later), because this update
        // action never updates sampleItems
        return (sampleItemsOfType != null && sampleItemsOfType.size() > 0) ? null : sampleItem;
    }

    private Note findExistingSampleNote() {
        if (sample.getId() == null) {
            return null;
        }

        List<Note> notes = noteService.getNoteByRefIAndRefTableAndSubject(sample.getId(),
                NoteServiceImpl.getReferenceTableIdForNoteBinding(NoteServiceImpl.BoundTo.SAMPLE), NOTE_SUBJECT);
        return notes.isEmpty() ? null : notes.get(0);
    }

    private Note findExistingQANote(String sampleQAEventId) {
        if (sampleQAEventId == null || !sampleQAEventId.matches("\\d+")) {
            return null;
        }

        List<Note> notes = noteService.getNoteByRefIAndRefTableAndSubject(sampleQAEventId,
                NoteServiceImpl.getReferenceTableIdForNoteBinding(NoteServiceImpl.BoundTo.QA_EVENT), NOTE_SUBJECT);
        return notes.isEmpty() ? null : notes.get(0);
    }

    private void addNoteIfNeeded(String noteText, SampleQaEvent event) {
        if (!GenericValidator.isBlankOrNull(noteText)) {
            NoteSet noteSet = new NoteSet();
            noteSet.referencedEvent = event;
            NoteService noteService = SpringContext.getBean(NoteService.class);
            noteSet.note = noteService.createSavableNote(event, NoteServiceImpl.NoteType.NON_CONFORMITY, noteText,
                    NOTE_SUBJECT, webData.getCurrentSysUserId());
            insertableNotes.add(noteSet);
        }
    }

    private String getNextSampleItemSortOrder() {
        String sampleId = sample.getId();
        int max = 0;
        if (!GenericValidator.isBlankOrNull(sampleId)) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sampleId);
            for (SampleItem sampleItem : sampleItems) {
                Integer curr = Integer.valueOf(sampleItem.getSortOrder());
                max = (curr > max) ? curr : max;
            }
        }
        max++;
        return String.valueOf(max);
    }

    private String newOrBlankFieldValue(Boolean newValue, String value) {
        if (newValue) {
            return value == null ? "" : value;
        }
        return null;
    }

    /**
     * @param item The Item to be evaluated
     * @return TRUE if is new, contains some reason and isn't marked for delete
     */
    private boolean isNonBlankNewEvent(QaEventItem item) {
        return "NEW".equals(item.getId()) && !item.isRemove() && !"0".equals(item.getQaEvent());
    }

    /**
     * @param item The Item to be evaluated
     * @return TRUE if Is not new and is marked for removal.
     */
    private boolean isOldForRemoval(QaEventItem item) {
        return item.getId() != null && !"NEW".equals(item.getId()) && item.isRemove();
    }

    private class NoteSet {
        public SampleQaEvent referencedEvent;
        public Sample referencedSample;
        public Note note;
    }
}
