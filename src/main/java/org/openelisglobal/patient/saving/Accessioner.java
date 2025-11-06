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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */

/**
 * Cote d'Ivoire
 *
 * @author pahill
 * @since 2010-06-15
 */
package org.openelisglobal.patient.saving;

import static org.openelisglobal.sample.util.CI.ProjectForm.EID;
import static org.openelisglobal.sample.util.CI.ProjectForm.SPECIAL_REQUEST;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.ObservationHistoryTypeMap;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper.TypeOfSampleTests;
import org.openelisglobal.sample.util.CI.IProjectFormMapper;
import org.openelisglobal.sample.util.CI.ProjectForm;
import org.openelisglobal.sample.util.CI.ProjectFormMapperFactory;
import org.openelisglobal.sample.util.CI.form.IProjectForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Update/Creates, as needed, a Sample and Patient and all associated parts in
 * order to enter that patient in the system. To use one of these to accession,
 * you should:
 * <nl>
 * <li>Provide a constructor (which probably includes whatever structure comes
 * from your UI form, so it can be used below).
 * <li>set the expected new RecordStatus for the patient and sample (maybe in
 * the constructor), but you can change that later.
 * <li>implement canAccession() to decide if this is the right time to try this
 * accession algorithm.
 * <li>implement validate* and match* methods to make sure all the entities
 * created and found hang together.
 * <li>implement the missing populate* methods (including providing any empty
 * implementation).
 * <li>possibly override the persist* methods, if there is more to do than
 * simply saving what has been built (delete any old ones? Update instead?).
 * </nl>
 * Use:<br>
 * if (myAccessioner1.canAccession()) { if (!myAccession1.accession(...)) {
 * errors = myAccession1.getMessages(); saveErrors(request, errors);
 * request.setAttribute(Globals.ERROR_KEY, errors); return
 * mapping.findForward(FWD_FAIL); } else { return
 * mapping.findForward(FWD_SUCCESS); } } else (myAccession2.canAccession() { ...
 * }
 *
 * <p>
 * PAH 07/2010 This object is still a work in progress. For example, we have a
 * member for projectFormMapper, but all its use in the subclasses at this time
 * . Maybe the projectFormMapper should just be injected after creation? It is
 * also the case that there are form field property names listed in these
 * various acccesioning classes when the formFieldMapper class is really the
 * class that should know the right place to find values on the submitted form.
 *
 * @author pahill
 */
public abstract class Accessioner implements IAccessioner {
    @Autowired
    private FhirTransformService fhirTransformService;

    /** a set of possible analysis status that means an analysis is done */
    private Set<String> analysisDone = new HashSet<>();

    {
        analysisDone
                .add(SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.Finalized));
        analysisDone.add(SpringContext.getBean(IStatusService.class)
                .getStatusID(StatusService.AnalysisStatus.NonConforming_depricated));
        analysisDone
                .add(SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.Canceled));
    }

    /**
     * Sample or Patient Entry always mark the type of an analysis as a MANUAL type.
     */
    private static final String DEFAULT_ANALYSIS_TYPE = IActionConstants.ANALYSIS_TYPE_MANUAL;
    // private static String OBSERVATION_HISTORY_YES_ID = null;
    private static String SAMPLE_TABLE_ID = null;

    static {
        SAMPLE_TABLE_ID = SpringContext.getBean(ReferenceTablesService.class).getReferenceTableByName("sample").getId();
        // OBSERVATION_HISTORY_YES_ID = new
        // DictionaryServiceImpl().getDictionaryByDictEntry("Demographic Response Yes
        // (in Yes or No)").getId();
    }

    /**
     * Find the projectFormName where ever we normally store it. This is for that
     * could which needs this information before creating a projectFormMapper
     *
     * @param form
     * @return the current projectFormName
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static String findProjectFormName(IAccessionerForm form) {
        ObservationData observations = form.getObservations();
        if (observations == null) {
            return null;
        } else {
            return observations.getProjectFormName();
        }
    }

    /**
     * @param dynaBean
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws LIMSRuntimeException
     */
    public static IProjectFormMapper getProjectFormMapper(IAccessionerForm form) throws LIMSRuntimeException {
        return new ProjectFormMapperFactory().getProjectInitializer(findProjectFormName(form), form);
    }

    /**
     * @param dynaBean
     * @return
     */
    public static IProjectFormMapper getProjectFormMapper(String projectFormName, IAccessionerForm form) {
        return new ProjectFormMapperFactory().getProjectInitializer(projectFormName, form);
    }

    protected String accessionNumber;
    protected String patientIdentifier;
    protected String patientSiteSubjectNo;
    protected StatusSet statusSet;

    @Autowired
    @Qualifier("defaultErrors")
    Errors messages;

    protected java.sql.Date today;
    protected String todayAsText;
    protected String sysUserId;
    protected ProjectForm projectForm;

    protected Patient patientInDB;
    private List<PatientIdentity> patientIdentities = new ArrayList<>();
    protected List<ObservationHistory> observationHistories = new ArrayList<>();

    Map<String, List<ObservationHistory>> observationHistoryLists = null;

    protected Sample sample;
    protected List<SampleItemAnalysisCollection> sampleItemsAnalysis = new ArrayList<>();
    protected List<SampleOrganization> sampleOrganizations = new ArrayList<>();

    protected SampleHuman sampleHuman;
    protected SampleProject sampleProject;

    protected RecordStatus newPatientStatus;
    protected RecordStatus newSampleStatus;

    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected PatientIdentityService identityService = SpringContext.getBean(PatientIdentityService.class);
    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    protected SampleProjectService sampleProjectService = SpringContext.getBean(SampleProjectService.class);
    protected ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);
    protected ProjectService projectService = SpringContext.getBean(ProjectService.class);
    protected OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    protected SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);
    protected SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private NoteService noteService = SpringContext.getBean(NoteService.class);

    protected boolean existingSample;
    protected boolean existingPatient;
    protected IProjectFormMapper projectFormMapper;

    protected Patient patientToDelete;
    protected List<Analysis> analysisToUpdate = new ArrayList<>();
    protected List<Analysis> analysisToDelete = new ArrayList<>();
    protected List<SampleItem> sampleItemsToDelete = new ArrayList<>();
    private boolean aDifferentPatientRecord = false;
    private ProjectData projectData;

    protected Accessioner(String sampleIdentifier, String patientIdentifier, String siteSubjectNo, String sysUserId) {
        this();
        setAccessionNumber(sampleIdentifier);
        setPatientIdentifier(patientIdentifier);
        setPatientSiteSubjectNo(siteSubjectNo);
        setSysUserId(sysUserId);
    }

    public Accessioner() {
        today = new java.sql.Date(System.currentTimeMillis());
        todayAsText = DateUtil.formatDateAsText(today);
    }

    public void setAccessionNumber(String sampleIdentifier) {
        accessionNumber = sampleIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public void setPatientSiteSubjectNo(String patientSiteSubjectNo) {
        this.patientSiteSubjectNo = patientSiteSubjectNo;
    }

    public final void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    /**
     * Primary entry point for processing a patient/sample combination Check the
     * messages, on true there may have been errors
     *
     * @return TRUE => errors FALSE => did not do it, had a problem doing it. @ @
     * @throws IllegalAccessException
     * @throws LIMSRuntimeException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws LIMSException
     */
    @Override
    @Transactional // only works if this class is autowired in
    public String save() throws IllegalAccessException, LIMSRuntimeException, InvocationTargetException,
            NoSuchMethodException, LIMSException {
        try {
            if (!canAccession()) {
                return null;
            }
            existingPatient = findPatient();
            if (existingPatient && !validateFoundPatient()) {
                return IActionConstants.FWD_FAIL_INSERT;
            }
            existingSample = findSample();
            if (existingSample && !validateFoundSample()) {
                return IActionConstants.FWD_FAIL_INSERT;
            }
            if (!matchPatientAndSample()) {
                return IActionConstants.FWD_FAIL_INSERT;
            }

            populatePatientData();
            populateSampleData();
            populateSampleHuman();
            populateObservationHistory();
            updateSampleWithElectronicEOrders();

            // all of the following methods are assumed to only write when
            // necessary
            persistPatient();
            persistIdentityTypes();

            persistSampleData();

            persistSampleHuman();

            persistObservationHistory();

            // persistObservationHistoryLists();// no more running name has been changed to
            // persistObservationHistoryLists2
            persistObservationHistoryLists2();
            persistRecordStatus();
            deleteOldPatient();
            populateAndPersistUnderInvestigationNote();
            // update fhir resources
            SamplePatientUpdateData updateData = new SamplePatientUpdateData(sysUserId);
            updateData.setSample(sample);
            updateData.setAccessionNumber(accessionNumber);
            updateData.setProvider(null);
            updateData.setSampleItemsTests(null);
            PatientManagementInfo patientInfo = new PatientManagementInfo();
            patientInfo.setPatientPK(patientInDB.getId());
            return IActionConstants.FWD_SUCCESS_INSERT;
        } catch (IllegalAccessException e) {
            logAndAddMessage("save()", "errors.InsertException", e);
            throw e;
        }
    }

    private void populateAndPersistUnderInvestigationNote() {
        // N.B. The notes is being attached to the Sample table rather than the
        // observation history because the observation history table
        // is being updated by inserting new observation histories and then
        // deleting the old ones. Any references to the old observation history
        // under investigation row are being lost. Until we fix that the notes
        // will be attached to the sample with note type of "UnderInvestigation"
        if ( // OBSERVATION_HISTORY_YES_ID.equals(observationData.getUnderInvestigation()) &&
        // <-- not sure of the business rules around this
        !GenericValidator.isBlankOrNull(projectData.getUnderInvestigationNote())) {

            Note note = new Note();
            note.setNoteType(Note.EXTERNAL);
            note.setReferenceId(sample.getId());
            note.setReferenceTableId(SAMPLE_TABLE_ID);

            List<Note> noteList = noteService.getNotesByNoteTypeRefIdRefTable(note);

            if (noteList != null && !noteList.isEmpty()) {
                note = noteList.get(0);
                note.setText(
                        note.getText() + "<br/>" + getActionLabel() + ": " + projectData.getUnderInvestigationNote());
            } else {
                note.setText(getActionLabel() + ": " + projectData.getUnderInvestigationNote());
                note.setSubject("UnderInvestigation");
            }

            note.setSysUserId(sysUserId);
            note.setSystemUser(NoteServiceImpl.createSystemUser(sysUserId));

            if (note.getId() == null) {
                noteService.insert(note);
            } else {
                noteService.update(note);
            }
        }
    }

    protected void persistSampleData() {
        persistSample();
        persistSampleProject();
        persistSampleOrganization();
        persisteSampleItemsChanged();
        persistSampleItemsAndAnalysis();
    }

    /**
     * This is probably the result of Sample(Second)Entry, so nothing to due by
     * default
     */
    private void persisteSampleItemsChanged() {
        // analysisService.delete(analysisToDelete);
        analysisService.deleteAll(analysisToDelete);
        sampleItemService.deleteAll(sampleItemsToDelete);
        for (Analysis analysis : analysisToUpdate) {
            analysisService.update(analysis);
        }
    }

    /**
     * This method is called when this object contains a patient from the database
     * found by ID. Sometimes we need to validate some of the fields with the values
     * from the input. Place an error in the messages list to return an error.
     *
     * @return TRUE => all is well with the patient we have found in the database
     *         vs. the data coming from the input.
     */
    protected boolean validateFoundPatient() {
        return true;
    }

    /**
     * This method is called when this object contains a sample from the database
     * found by ID. Sometimes we need to validate some of the fields with the values
     * from the input. Place an error in the messages list to return an error.
     *
     * @return TRUE => all is well with the SAMPLE we have found in the database vs.
     *         the data coming from the input.
     */
    protected boolean validateFoundSample() {
        return true;
    }

    /**
     * This method is called when there is something to check to make sure the
     * patient and sample go together correctly.
     *
     * @return TRUE => all looks good.
     */
    protected boolean matchPatientAndSample() {
        return true;
    }

    /**
     * Use appropriate means to come up with a patient and its person record.
     *
     * @return TRUE => patient was found in the database, FALSE => patient not
     *         found, simply created.
     */
    protected boolean findPatient() {
        String patientId = statusSet.getPatientId();
        String sampleId = statusSet.getSampleId();
        String unknownPatient = PatientUtil.getUnknownPatient().getId();
        if (sampleId == null) {
            // no sample => nothing to leverage so build up from what we have
            return createPatientByIdentifiers();
        }
        if (patientId.equals(unknownPatient)) {
            // the UNKNOWN patient, so we'll create a new one
            setADifferentPatient(true);
            return createNewPatient();
        }
        // the patient is already associated with the existing sample and its
        // not the unknown one
        patientInDB = patientService.readPatient(patientId);
        if (patientIdentifiersDoNotMatch()) {
            Patient otherPatient = findPatientByIndentifiers();
            if (otherPatient == null) { // unknown
                int otherSamplesOnCurrentPatient = sampleHumanService.getSamplesForPatient(patientId).size() - 1;
                if (otherSamplesOnCurrentPatient == 0) {
                    // stick with the existing patient of the sample, update it
                    return true;
                } else {
                    // it's a new patient, so we might have to move some old
                    // pointers around.
                    setADifferentPatient(true);
                    return createNewPatient();
                }
            } else {
                // it's another existing patient, so use that and don't forget
                // we're doing that.
                patientInDB = otherPatient;
                setADifferentPatient(false);
                return true;
            }
        }
        return true;
    }

    private boolean patientIdentifiersDoNotMatch() {

        String existingSubjectNo = StringUtil.replaceNullWithEmptyString(patientInDB.getNationalId()).trim();

        if (!GenericValidator.isBlankOrNull(patientIdentifier) && patientIdentifier.equals(existingSubjectNo)) {
            return false;
        }

        String existingSiteSubjectNo = StringUtil.replaceNullWithEmptyString(patientInDB.getExternalId()).trim();

        return !(!GenericValidator.isBlankOrNull(patientSiteSubjectNo)
                && patientSiteSubjectNo.equals(existingSiteSubjectNo));
    }

    /** Either find it by the primary or secondary identifier or return a new one */
    private boolean createPatientByIdentifiers() {
        patientInDB = findPatientByIndentifiers();
        return patientInDB != null || createNewPatient();
    }

    private Patient findPatientByIndentifiers() {
        Patient aPatient;
        if (patientIdentifier != "") {
            aPatient = patientService.getPatientByNationalId(patientIdentifier);
        } else {
            String externalId = projectFormMapper.getSiteSubjectNumber();
            aPatient = patientService.getPatientByExternalId(externalId);
        }
        return aPatient;
    }

    /**
     * @return FALSE since it doesn't already exist
     */
    protected boolean createNewPatient() {
        patientInDB = new Patient();
        patientInDB.setPerson(new Person());
        return false;
    }

    /**
     * @return true = existing sample
     * @throws LIMSRuntimeException
     * @throws LIMSInvalidConfigurationException
     */
    protected boolean findSample() throws LIMSRuntimeException, LIMSInvalidConfigurationException {
        sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        String sampleId = statusSet.getSampleId();
        // if there is sample for the given acc. number is an existing sample it
        // had better be same one we found when we loading the sampleSet
        if (sample != null && !isNewSample()) {
            if (!sample.getId().equals(sampleId)) {
                messages.reject("errors.may_not_reuse_accession_number", sample.getAccessionNumber());
                throw new RuntimeException(
                        "You can not re-use an existing accessionNumber " + sample.getAccessionNumber());
            }
            return true;
        } else {
            // the accession number is unknown, so load/build the sample using
            // the sampleStatus ID
            sample = new Sample();
            if (sampleId == null) {
                return false; // it brand new, no existing accessionNumber in
                // use, not known sampleId provided
            } else {
                sample = knownSampleTemplate();
                sampleService.getData(sample);
                sample.setAccessionNumber(accessionNumber);
                return true; // it existed previously, but we have a new
                // (edited) accesionNumber
            }
        }
    }

    private Sample knownSampleTemplate() {
        Sample sample = new Sample();
        sample.setId(statusSet.getSampleId());
        return sample;
    }

    /**
     * @return if the sample we are working with is NOT in the database this object
     *         is about to create it.
     */
    protected boolean isNewSample() {
        return sample.getId() == null;
    }

    /**
     * @return TRUE if the patient is not the known patient already associated with
     *         the known sample.
     */
    protected boolean isADifferentPatient() {
        return aDifferentPatientRecord;
    }

    /**
     * Call this to record that when we started, the patient record associated with
     * the original sample record is NOT the record we are now working with. We
     * record this because, once we updating records to write, we can longer compare
     * IDs to come up with the answer.
     */
    protected void setADifferentPatient(boolean force) {
        // TODO 'force' was added as a safety because aDifferentPatientRecourd was not
        // being set to true when it should
        // have. The original test for aDifferentPatientRecord may still be needed, in
        // the two places force is set
        // to true
        aDifferentPatientRecord = (force || patientInDB == null || statusSet.getPatientId() != patientInDB.getId());
    }

    /**
     * Test this object to see if we should even begin. This is intended for
     * checking existing patient/sample record status.
     *
     * @return TRUE => all is well; FALSE (Default) => this particular version of
     *         the accession process is not appropriate for the status combination.
     */
    public abstract boolean canAccession();

    /**
     * load up this object with any new observation history records, including lists
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    protected void populateObservationHistory()
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        projectFormMapper.getForm();
        ObservationData observationData = (ObservationData) (PropertyUtils.getProperty(projectFormMapper.getForm(),
                "observations"));
        observationHistories = projectFormMapper.readObservationHistories(observationData);
        observationHistoryLists = projectFormMapper.readObservationHistoryLists(observationData);
    }

    public StatusSet findStatusSet() {
        if (statusSet == null) {
            String sampleId = projectFormMapper.getSampleId();
            if (GenericValidator.isBlankOrNull(sampleId)) {
                statusSet = SpringContext.getBean(IStatusService.class).getStatusSetForAccessionNumber(accessionNumber);
            } else {
                statusSet = SpringContext.getBean(IStatusService.class).getStatusSetForSampleId(sampleId);
            }
        }
        return statusSet;
    }

    /**
     * Move data from the form to the sample and sample related objects, include
     * SampleOrganization but not to any of the entities which tie a patient to a
     * sample; don't include ObservationHistory and SampleHuman
     *
     * @throws LIMSException @ if things go wrong.
     */
    protected abstract void populateSampleData() throws LIMSException;

    /** Create any appropriate sample human entity @ */
    protected void populateSampleHuman() {
        if (isNewSample()) {
            sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
            sampleHuman = new SampleHuman();
        } else {
            if (isNewPatient() || isADifferentPatient()) {
                sampleHuman = new SampleHuman();
                sampleHuman.setSampleId(sample.getId());
                sampleHumanService.getDataBySample(sampleHuman);
            }
        }
        if (isADifferentPatient()) {
            int size = sampleHumanService.getSamplesForPatient(statusSet.getPatientId()).size();
            if (size == 1) {
                // if there is only one sample on the OLD patient of this sample
                // we can delete it.
                patientToDelete = knownPatientTemplate();
                patientService.getData(patientToDelete);
            }
        }
    }

    private boolean isNewPatient() {
        return patientInDB.getId() == null;
    }

    /**
     * Build an example Patient record with the ID only for the current patient of
     * the sample
     *
     * @return
     */
    private Patient knownPatientTemplate() {
        Patient patient = new Patient();
        patient.setId(statusSet.getPatientId());
        return patient;
    }

    protected void populateSample(Timestamp receivedDateForDisplay, Timestamp collectionDateForDisplay) {
        sample.setAccessionNumber(accessionNumber);
        sample.setReceivedTimestamp(receivedDateForDisplay);
        sample.setCollectionDate(collectionDateForDisplay);

        // and all the administration fields.
        if (isNewSample()) {
            sample.setEnteredDateForDisplay(todayAsText);
            sample.setEnteredDate(today);
        }
        sample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
    }

    protected void populateSampleProject() {
        if (!isSampleInProject()) {
            Project project = projectForm.getProject();
            sampleProject = new SampleProject();
            sampleProject.setProject(project);
        }
    }

    /**
     * @return TRUE only if the sample is already associated with a project.
     */
    private boolean isSampleInProject() {
        if (isNewSample()) {
            return false;
        }
        SampleProject oldSampleProject = sampleProjectService.getSampleProjectBySampleId(sample.getId());
        return oldSampleProject != null;
    }

    /**
     * create a sampleOrganization record, finding any old one to delete. While
     * there can be more than one organization per sample, this code only supports
     * one.
     *
     * @param organizationId if none null, use it; otherwise do nothing
     */
    protected void populateSampleOrganization(String organizationId) {
        if (GenericValidator.isBlankOrNull(organizationId)
                || organizationId.equals(BaseProjectFormMapper.ORGANIZATION_ID_NONE)) {
            return;
        }

        Organization org = new Organization();
        org.setId(organizationId);
        organizationService.getData(org);

        if (null == org.getId()) {
            throw new LIMSRuntimeException("Undefined organization name = " + organizationId
                    + ". Unable to create sample to organization mapping.");
        }

        SampleOrganization oldSampleOrg;
        if (!isNewSample()) {
            oldSampleOrg = new SampleOrganization();
            oldSampleOrg.setSample(sample);
            sampleOrganizationService.getDataBySample(oldSampleOrg);
            if (oldSampleOrg.getOrganization() != null) { // there may not be an
                // organiztion
                // attached yet
                String oldOrganizationId = oldSampleOrg.getOrganization().getId();
                if (oldOrganizationId.equals(organizationId)) {
                    return; // we have an existing sample with the right
                    // organization already, so don't bother with delete
                    // or insert
                }
                oldSampleOrg.setSysUserId(sysUserId);
                oldSampleOrg.setOrganization(org);
                sampleOrganizations.add(oldSampleOrg);
            }
        }
        SampleOrganization so = new SampleOrganization();
        so.setOrganization(org);
        // sampleOrganization.setSampleOrganizationType("?"); // nothing
        // important
        so.setSysUserId(sysUserId);
        sampleOrganizations.add(so);
    }

    /**
     * Add to the list of patient identities which will be saved.
     *
     * @param patientIdentityTypeName
     * @param paramValue              a value for a particular patient identity; if
     *                                null, do nothing.
     */
    protected void addPatientIdentity(String patientIdentityTypeName, String paramValue) {
        if (paramValue == null) {
            return;
        }
        String typeID = PatientIdentityTypeMap.getInstance().getIDForType(patientIdentityTypeName);
        PatientIdentity identity = new PatientIdentity();
        identity.setPatientId(patientInDB.getId());
        identity.setIdentityTypeId(typeID);
        identity.setIdentityData(paramValue);
        patientIdentities.add(identity);
    }

    /**
     * Assume all the fields on the dynaForm have the right names (see code) and
     *
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    protected void populatePatientData()
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IProjectForm form = projectFormMapper.getForm();
        Timestamp lastupdated1 = patientInDB.getLastupdated();
        PropertyUtils.copyProperties(patientInDB, form);
        patientInDB.setLastupdated(lastupdated1);
        Person person = patientInDB.getPerson();
        PropertyUtils.copyProperties(person, form);

        patientInDB.setNationalId(convertEmptyToNull(form.getSubjectNumber()));
        patientInDB.setExternalId(convertEmptyToNull(form.getSiteSubjectNumber()));
        if (ObjectUtils.isNotEmpty(form.getPatientFhirUuid())) {
            patientInDB.setFhirUuid(UUID.fromString(form.getPatientFhirUuid()));
        }
        populatePatientBirthDate(form.getBirthDateForDisplay());

        projectData = form.getProjectData();
        if (projectData != null) {
            person.setHomePhone(convertEmptyToNull(projectData.getPhoneNumber()));
            person.setFax(convertEmptyToNull(projectData.getFaxNumber()));
            person.setEmail(convertEmptyToNull(projectData.getEmail()));
            person.setStreetAddress(convertEmptyToNull(projectData.getAddress()));
        }
    }

    /**
     * @param value - string to test
     * @return if the string is null or empty "", then return null
     */
    private String convertEmptyToNull(String value) {
        return (GenericValidator.isBlankOrNull(value)) ? null : value;
    }

    /**
     * Given a possibly ambiguous birthday date string, get the right values into
     * both (1) the patient birthdate and (2) make sure we create a patient_identity
     * for any ambiguous value.
     */
    protected void populatePatientBirthDate(String birthDateForDisplay) {
        patientInDB.setBirthDateForDisplay(birthDateForDisplay);
    }

    protected void populateSampleItems(List<TypeOfSampleTests> typeofSampleTestList, Timestamp collectionDate) {
        sampleItemsAnalysis = new ArrayList<>();

        if (typeofSampleTestList.size() == 0) {
            messages.reject("errors.no.tests");
            throw new LIMSRuntimeException("No tests selected.");
        }

        for (TypeOfSampleTests typeofSampleTest : typeofSampleTestList) {
            TypeOfSample toSample = typeofSampleTest.typeOfSample;
            List<Test> tests = typeofSampleTest.tests;
            SampleItem item = buildSampleItem(sample, toSample, collectionDate);

            sampleItemsAnalysis.add(new SampleItemAnalysisCollection(item, tests));
        }
    }

    protected SampleItem buildSampleItem(Sample sample, TypeOfSample typeofsample, Timestamp collectionDate) {
        SampleItem item = new SampleItem();
        item.setTypeOfSample(typeofsample);
        item.setSortOrder(Integer.toString(0));
        item.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered));
        item.setCollectionDate(collectionDate);
        return item;
    }

    protected final class SampleItemAnalysisCollection {
        public SampleItem item;
        public List<Test> tests;
        public String collectionDate;

        public SampleItemAnalysisCollection(SampleItem item, List<Test> tests) {
            // Currently we allow a sampleItem w/o any tests requested,
            // elsewhere we check that at least one test is ordered somewhere.
            // if (tests.size() == 0) {
            // messages.add(ActionErrors.GLOBAL_MESSAGE, new
            // ActionError("errors.samples.with.no.tests"));
            // throw new
            // Exception("A least one sample was defined without any corresponding test
            // selected.");
            // }
            this.item = item;
            this.tests = tests;
        }
    }

    /** Delete any new ones and remove any old ones. */
    protected void persistSampleOrganization() {
        for (SampleOrganization so : sampleOrganizations) {
            // we are in the same transaction, we do not need to reload the sample
            // sampleService.getData(sample);
            so.setSample(sample);
            if (so.getId() == null) {
                sampleOrganizationService.insert(so);
            } else {
                sampleOrganizationService.update(so);
            }
        }
    }

    protected void persistSampleItemsAndAnalysis() {
        if (0 == sampleItemsAnalysis.size()) {
            return;
        }

        // Find all the already created sample items for this sample
        Map<String, SampleItem> itemsByType = findExistingSampleTypeItems();
        int nextSortOrder = calcLastSortOrder(itemsByType) + 1;

        String analysisRevision = ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision");
        boolean newAnalysis = false;
        for (SampleItemAnalysisCollection sampleTestPair : sampleItemsAnalysis) {

            // create new or find existing sample item for testing.
            SampleItem item = sampleTestPair.item;
            SampleItem existingSampleItem = itemsByType.get(item.getTypeOfSampleId());
            if (existingSampleItem == null) {
                item.setSample(sample);
                item.setSortOrder(Integer.toString(nextSortOrder++));
                item.setSysUserId(sysUserId);

                sampleItemService.insert(item);
            } else {
                sampleTestPair.item = item = existingSampleItem;
            }

            List<String> existingTests = findDefinedTestsForItem(item);
            // insert any missing analysis
            for (Test newTest : sampleTestPair.tests) {
                testService.getData(newTest);
                if (!existingTests.contains(newTest.getId())) {
                    Analysis analysis = buildAnalysis(analysisRevision, sampleTestPair, newTest);
                    analysisService.insert(analysis);
                    newAnalysis = true;
                }
            }
        }
        // if we didn't create any analysis, we need to check if the sample is
        // completely done.
        if (!newAnalysis) {
            completeSample();
        }
    }

    /**
     * @param itemsByType
     * @return the maximum value which has already been used in the set of
     *         SampleItems
     */
    private int calcLastSortOrder(Map<String, SampleItem> itemsByType) {
        int max = 0;
        for (Entry<String, SampleItem> entry : itemsByType.entrySet()) {
            SampleItem sampleItem = entry.getValue();
            int curr = Integer.valueOf(sampleItem.getSortOrder());
            if (max < curr) {
                max = curr;
            }
        }
        return max;
    }

    /**
     * Find all tests of the item which have had analysis already ordered (defined).
     *
     * @param item
     * @return a list of test IDs
     */
    private List<String> findDefinedTestsForItem(SampleItem item) {
        List<Analysis> analysesForItem = analysisService.getAnalysesBySampleItem(item);
        List<String> testIds = new ArrayList<>();
        for (Analysis analysis : analysesForItem) {
            Test test = analysis.getTest();
            testIds.add(test.getId());
        }
        return testIds;
    }

    private Map<String, SampleItem> findExistingSampleTypeItems() {
        List<SampleItem> itemsForSample = sampleItemService.getSampleItemsBySampleId(sample.getId());
        Map<String, SampleItem> itemsByType = new HashMap<>();
        for (SampleItem sampleItem : itemsForSample) {
            String id = sampleItem.getTypeOfSampleId();
            itemsByType.put(id, sampleItem);
        }
        return itemsByType;
    }

    /**
     * If during entry we find (1) all the analysis are finished, and (2) the sample
     * is not already been declared bad, then we're ready to mark the sample as
     * done. @
     */
    @Transactional
    public void completeSample() {
        if (isAllAnalysisDone() && !SpringContext.getBean(IStatusService.class)
                .getStatusID(OrderStatus.NonConforming_depricated).equals(sample.getStatus())) {
            sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
            sample.setSysUserId(sysUserId);
            sampleService.update(sample);
        }
    }

    /**
     * The question is whether we are ready to update the sample status. TODO Pahill
     * maybe we could move this to SpringContext.getBean(IStatusService.class)?
     */
    private boolean isAllAnalysisDone() {
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        for (Analysis analysis : analyses) {
            if (!analysisDone.contains(analysis.getStatusId())) {
                return false;
            }
        }
        return true;
    }

    private Analysis buildAnalysis(String analysisRevision, SampleItemAnalysisCollection sampleTestCollection,
            Test test) {
        java.sql.Date collectionDateTime = DateUtil.convertStringDateToSqlDate(sampleTestCollection.collectionDate);

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setSampleItem(sampleTestCollection.item);
        analysis.setRevision(analysisRevision);
        analysis.setStartedDate(collectionDateTime);
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        analysis.setTestSection(test.getTestSection());
        analysis.setSysUserId(sysUserId);
        return analysis;
    }

    /** save patient */
    protected void persistPatient() {
        if (patientInDB != null) {
            Person person = patientInDB.getPerson();
            person.setSysUserId(sysUserId);
            patientInDB.setSysUserId(sysUserId);
            if (patientInDB.getId() == null) {
                personService.insert(person);
                patientService.insert(patientInDB);
            } else {
                // The reason for the explicit person update is to capture the
                // history.
                personService.update(person);
                patientService.update(patientInDB);
            }
        }
    }

    /**
     * Save whatever patient identities have been created.
     *
     * @throws LIMSRuntimeException
     */
    public void persistIdentityTypes() throws LIMSRuntimeException {

        if (0 == patientIdentities.size()) {
            return;
        }
        for (PatientIdentity identity : patientIdentities) {
            identity.setPatientId(patientInDB.getId());
            identity.setSysUserId(sysUserId);
            identityService.insert(identity);
        }
    }

    protected void persistSample() throws LIMSRuntimeException {
        if (null != sample) {
            sample.setSysUserId(sysUserId);
            if (sample.getId() != null) {
                sampleService.update(sample);
                // HibernateUtil.getSession().evict(sample);
            } else {
                sampleService.insertDataWithAccessionNumber(sample);
            }
        }
    }

    protected void persistSampleProject() throws LIMSRuntimeException {
        if (null != sampleProject) {
            sampleProject.setSample(sample);
            sampleProject.setSysUserId(sysUserId);
            sampleProjectService.insert(sampleProject);
        }
    }

    /** Persist any old or new sampleHuman we're currently holding. */
    protected void persistSampleHuman() {
        if (sampleHuman != null) {
            SampleHuman otherSampleHuman = sampleHumanService.getMatch("sampleId", sample.getId()).orElse(null);
            if (ObjectUtils.isNotEmpty(otherSampleHuman)) {
                sampleHuman = otherSampleHuman;
            }
            sampleHuman.setPatientId(patientInDB.getId());
            sampleHuman.setSampleId(sample.getId());
            // we do not store any doctor name as a provider in SampleHuman
            sampleHuman.setSysUserId(sysUserId);
            if (null == sampleHuman.getId()) {
                sampleHumanService.insert(sampleHuman);
            } else {
                sampleHumanService.update(sampleHuman);
            }
        }
    }

    /** Persist any simple observations histories in this accession object. */
    protected void persistObservationHistory() {
        if (isADifferentPatient()) {
            List<ObservationHistory> oldOHes = observationHistoryService.getAll(knownPatientTemplate(),
                    knownSampleTemplate());
            for (ObservationHistory oldOh : oldOHes) {
                oldOh.setSampleId(sample.getId());
                oldOh.setPatientId(patientInDB.getId());
                oldOh.setSysUserId(sysUserId);
                observationHistoryService.update(oldOh);
            }
        }

        for (ObservationHistory newOh : observationHistories) {
            List<ObservationHistory> existingTypeOHes = observationHistoryService.getAll(patientInDB, sample,
                    newOh.getObservationHistoryTypeId());
            List<ObservationHistory> deleteTypeOHes = new ArrayList<>();
            // delete any matching old ones, but only when there are both same
            // Ob. History type AND the same value type. Why because the
            // database allows more than one of the same type,
            // and we store e.g. nationality twice as two types, D=dictionary
            // for dropdown value, L=value used when there the menu value is
            // other, and there is a literal with text for other.
            String newValueType = newOh.getValueType();
            for (ObservationHistory oh : existingTypeOHes) {
                if (oh.getValueType().equals(newValueType)) {
                    oh.setSysUserId(sysUserId);
                    deleteTypeOHes.add(oh);
                }
            }
            if (deleteTypeOHes.size() > 0) {
                observationHistoryService.deleteAll(deleteTypeOHes);
            }

            newOh.setSampleId(sample.getId());
            newOh.setPatientId(patientInDB.getId());
            newOh.setSysUserId(sysUserId);
            observationHistoryService.insert(newOh);
        }
    }

    // Note -- this version does not do the delete insert model of updates but we
    // are putting off
    // rolling it out because of the cost of testing
    /*
     * protected void persistObservationHistory() { if (isADifferentPatient()) {
     * List<ObservationHistory> oldOHes =
     * observationHistoryService.getAll(knownPatientTemplate(),
     * knownSampleTemplate()); for (ObservationHistory oldOh : oldOHes) {
     * oldOh.setSampleId(sample.getId()); oldOh.setPatientId(patientInDB.getId());
     * oldOh.setSysUserId(sysUserId); observationHistoryService.update(oldOh); } }
     *
     * for (ObservationHistory newOh : observationHistories) { boolean machedInDB =
     * false; List<ObservationHistory> existingTypeOHes =
     * observationHistoryService.getAll(patientInDB, sample,
     * newOh.getObservationHistoryTypeId()); List<ObservationHistory> deleteTypeOHes
     * = new ArrayList<ObservationHistory>(); // update any matching old ones, but
     * only when there are both same // Ob. History type AND the same value type.
     * Why because the // database allows more than one of the same type, // and we
     * store e.g. nationality twice as two types, D=dictionary // for dropdown
     * value, L=value used when there the menu value is // other, and there is a
     * literal with text for other. String newValueType = newOh.getValueType(); for
     * (ObservationHistory oh : existingTypeOHes) { if
     * (oh.getValueType().equals(newValueType)) { machedInDB = true; if
     * (oh.getValue() != null && !oh.getValue().equals(newOh.getValue())) {
     * oh.setSysUserId(sysUserId); oh.setValue(newOh.getValue());
     * observationHistoryService.update(oh);
     *
     * } } } // if (deleteTypeOHes.size() > 0) { //
     * observationHistoryService.delete(deleteTypeOHes); // }
     *
     * if (!machedInDB) { newOh.setSampleId(sample.getId());
     * newOh.setPatientId(patientInDB.getId()); newOh.setSysUserId(sysUserId);
     * observationHistoryService.insert(newOh); } } }
     */
    /** */
    protected void persistObservationHistoryLists() {
        LogEvent.logInfo(this.getClass().getSimpleName(), "persistObservationHistoryLists",
                "FUNCTION NAME PROHIBITED !");
    }

    protected void persistObservationHistoryLists2() {
        if (observationHistoryLists == null) {
            return;
        }

        for (String listType : observationHistoryLists.keySet()) {
            // throw away the old list
            Map<String, ObservationHistory> oldOHes = findExistingObservationHistories(listType);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", );
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", listType +
            // "
            // oldOHes.size = "
            // +oldOHes.size());
            for (ObservationHistory oh : oldOHes.values()) {
                oh.setSysUserId(sysUserId);
            }
            observationHistoryService.deleteAll(new ArrayList<>(oldOHes.values()));

            // insert the new
            List<ObservationHistory> newOHes = observationHistoryLists.get(listType);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", listType +
            // "
            // newOHes.size = "
            // +newOHes.size());
            for (ObservationHistory newOH : newOHes) {
                newOH.setSysUserId(sysUserId);
                newOH.setPatientId(patientInDB.getId());
                newOH.setSampleId(sample.getId());

                observationHistoryService.insert(newOH);
            }
        }
    }

    private Map<String, ObservationHistory> findExistingObservationHistories(String nameKey) {
        Map<String, ObservationHistory> existing = new HashMap<>();

        String ohTypeId = ObservationHistoryTypeMap.getInstance().getIDForType(nameKey);
        List<ObservationHistory> existingOHes = observationHistoryService.getAll(patientInDB, sample, ohTypeId);
        for (ObservationHistory oh : existingOHes) {
            existing.put(oh.getValue(), oh);
        }
        return existing;
    }

    protected void persistRecordStatus() {
        // Special Request and EID don't have a patient entry form, so we move
        // the patient record status when we move the sample record status.
        if (projectForm == SPECIAL_REQUEST || projectForm == EID) {
            newPatientStatus = newSampleStatus;
        }
        SpringContext.getBean(IStatusService.class).persistRecordStatusForSample(sample, newSampleStatus, patientInDB,
                newPatientStatus, sysUserId);
    }

    protected void deleteOldPatient() {
        if (patientToDelete != null) {
            try {

                List<PatientIdentity> oldIdentities = identityService
                        .getPatientIdentitiesForPatient(patientToDelete.getId());
                for (PatientIdentity listIdentity : oldIdentities) {
                    identityService.delete(listIdentity.getId(), sysUserId);
                }
                Person personToDelete = patientToDelete.getPerson();
                patientToDelete.setSysUserId(sysUserId);
                patientService.deleteAll(Arrays.asList(patientToDelete));
                personToDelete.setSysUserId(sysUserId);
                personService.deleteAll(Arrays.asList(personToDelete));

            } catch (Exception e) {
                LogEvent.logError(e);
            }
        }
    }

    protected List<ObservationHistory> getObservationHistories() {
        return observationHistories;
    }

    protected SampleService getSimpleSampleService() {
        return SpringContext.getBean(SampleService.class);
    }

    @Override
    public Errors getMessages() {
        return messages;
    }

    public void setMessages(Errors messages) {
        this.messages = messages;
    }

    /****
     * Record a thrown exception
     *
     * @param methodName where it happened.
     * @param messageKey default message if there is not already a error recorded.
     * @param e          the thrown exception of which to print the stack trace.
     */
    public void logAndAddMessage(String methodName, String messageKey, Exception e) {
        LogEvent.logError(e);
        if (!messages.hasErrors()) {
            messages.reject(messageKey);
        }
    }

    protected abstract String getActionLabel();

    // protected void persistInitialSampleConditions()
    // throws IllegalAccessException, InvocationTargetException,
    // NoSuchMethodException {
    // if (!FormFields.getInstance().useField(Field.InitialSampleCondition)) {
    // return;
    // }
    //
    // try {
    //
    // String xml = projectFormMapper.getForm().getSampleXML();
    // // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
    // "AMANI:"+xml);
    // Document sampleDom = DocumentHelper.parseText(xml);
    // for (Iterator i = sampleDom.getRootElement().elementIterator("sample");
    // i.hasNext();) {
    // Element sampleItem = (Element) i.next();
    // String initialSampleConditionIdString =
    // sampleItem.attributeValue("initialConditionIds");
    // String sampleItemId = sampleItem.attributeValue("sampleID");
    //
    // ObservationHistory observation = new ObservationHistory();
    //
    // if (!GenericValidator.isBlankOrNull(initialSampleConditionIdString)) {
    // String[] initialSampleConditionIds =
    // initialSampleConditionIdString.split(",");
    // for (int j = 0; j < initialSampleConditionIds.length; j++) {
    // observation = new ObservationHistory();
    // observation.setValue(initialSampleConditionIds[j]);
    // observation.setValueType(ObservationHistory.ValueType.DICTIONARY);
    // observation.setObservationHistoryTypeId(getObservationHistoryTypeId(
    // SpringContext.getBean(ObservationHistoryTypeService.class),
    // "initialSampleCondition"));
    // observation.setSampleId(sample.getId());
    // observation.setSampleItemId(sampleItemId);
    // observation.setPatientId(patientInDB.getId());
    // observation.setSysUserId(sysUserId);
    // observationHistoryService.insert(observation);
    // }
    // }
    // }
    //
    // } catch (DocumentException e) {
    // LogEvent.logDebug(e);
    // }
    // // dynaForm.set("orbservations", observations);
    //
    // }

    private static String getObservationHistoryTypeId(ObservationHistoryTypeService ohtService, String name) {
        ObservationHistoryType oht;
        oht = ohtService.getByName(name);
        if (oht != null) {
            return oht.getId();
        }

        return null;
    }

    private void updateSampleWithElectronicEOrders() {
        try {
            if (ObjectUtils.isNotEmpty(projectFormMapper.getForm().getElectronicOrder())) {
                sample.setReferringId(projectFormMapper.getForm().getElectronicOrder().getExternalId());
                sample.setClinicalOrderId(projectFormMapper.getForm().getElectronicOrder().getId());
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
    }
}
