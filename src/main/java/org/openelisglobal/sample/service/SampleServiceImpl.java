package org.openelisglobal.sample.service;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.dao.SampleAdditionalFieldDAO;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class SampleServiceImpl extends AuditableBaseObjectServiceImpl<Sample, String> implements SampleService {

    private static String TABLE_REFERENCE_ID;

    private static Long PERSON_REQUESTER_TYPE_ID;
    private static Long ORGANIZATION_REQUESTER_TYPE_ID;

    @Autowired
    protected SampleDAO sampleDAO;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ReferenceTablesService referenceTableService;
    @Autowired
    private RequesterTypeService requesterTypeService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleAdditionalFieldDAO sampleAdditionalFieldDAO;
    @Autowired
    private StatusOfSampleService statusOfSampleService;

    @PostConstruct
    private void initializeGlobalVariables() {
        TABLE_REFERENCE_ID = referenceTableService.getReferenceTableByName("SAMPLE").getId();
        RequesterType type = requesterTypeService.getRequesterTypeByName("provider");
        PERSON_REQUESTER_TYPE_ID = type != null ? Long.parseLong(type.getId()) : Long.MIN_VALUE;
        type = requesterTypeService.getRequesterTypeByName("organization");
        ORGANIZATION_REQUESTER_TYPE_ID = type != null ? Long.parseLong(type.getId()) : Long.MIN_VALUE;
    }

    public SampleServiceImpl() {
        super(Sample.class);
        this.auditTrailLog = true;
    }

    @Override
    public String insert(Sample sample) {
        if (sample.getFhirUuid() == null) {
            sample.setFhirUuid(UUID.randomUUID());
        }
        return super.insert(sample);
    }

    @Override
    protected SampleDAO getBaseObjectDAO() {
        return sampleDAO;
    }

    public static String getTableReferenceId() {
        return TABLE_REFERENCE_ID;
    }

    @Override
    @Transactional(readOnly = true)
    public Sample getSampleByAccessionNumber(String labNumber) {
        if (labNumber != null && labNumber.contains(".")) {
            labNumber = labNumber.substring(0, labNumber.indexOf('.'));
        }
        return getMatch("accessionNumber", labNumber).orElse(null);
    }

    @Override
    @Transactional
    public boolean insertDataWithAccessionNumber(Sample sample) {
        insert(sample);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesReceivedOn(String recievedDate) {
        return sampleDAO.getSamplesReceivedOn(recievedDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesForPatient(String patientID) {
        return sampleHumanService.getSamplesForPatient(patientID);
    }

    /**
     * Gets the date of when the order was completed
     *
     * @return The date of when it was completed, null if it was not yet completed
     */
    @Override
    @Transactional(readOnly = true)
    public Date getCompletedDate(Sample sample) {
        Date date = null;
        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

        for (Analysis analysis : analysisList) {
            if (!isCanceled(analysis)) {
                if (analysis.getCompletedDate() == null) {
                    return null;
                } else if (date == null) {
                    date = analysis.getCompletedDate();
                } else if (analysis.getCompletedDate().after(date)) {
                    date = analysis.getCompletedDate();
                }
            }
        }
        return date;
    }

    private boolean isCanceled(Analysis analysis) {
        return SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.Canceled)
                .equals(analysis.getStatusId());
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getOrderedDate(Sample sample) {
        if (sample == null) {
            return null;
        }
        ObservationHistory observation = SpringContext.getBean(ObservationHistoryService.class)
                .getObservationForSample(ObservationHistoryServiceImpl.ObservationType.REQUEST_DATE, sample.getId());
        if (observation != null && observation.getValue() != null) {
            return DateUtil.convertStringDateToTruncatedTimestamp(observation.getValue());
        } else { // If ordered date is not given then use received date
            return sample.getReceivedTimestamp();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getAccessionNumber(Sample sample) {
        return sample.getAccessionNumber();
    }

    @Override
    @Transactional(readOnly = true)
    public String getReceivedDateForDisplay(Sample sample) {
        return sample.getReceivedDateForDisplay();
    }

    @Override
    @Transactional(readOnly = true)
    public String getTwoYearReceivedDateForDisplay(Sample sample) {
        String fourYearDate = getReceivedDateForDisplay(sample);
        int lastSlash = fourYearDate.lastIndexOf("/");
        return fourYearDate.substring(0, lastSlash + 1) + fourYearDate.substring(lastSlash + 3);
    }

    @Override
    @Transactional(readOnly = true)
    public String getReceivedDateWithTwoYearDisplay(Sample sample) {
        return DateUtil.convertTimestampToTwoYearStringDate(sample.getReceivedTimestamp());
    }

    @Override
    @Transactional(readOnly = true)
    public String getReceivedTimeForDisplay(Sample sample) {
        return sample.getReceivedTimeForDisplay();
    }

    @Override
    @Transactional(readOnly = true)
    public String getReceived24HourTimeForDisplay(Sample sample) {
        return sample.getReceived24HourTimeForDisplay();
    }

    @Override
    public boolean isConfirmationSample(Sample sample) {
        return sample != null && sample.getIsConfirmation();
    }

    @Override
    @Transactional(readOnly = true)
    public String getId(Sample sample) {
        return sample.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysis(Sample sample) {
        return sample == null ? new ArrayList<>() : analysisService.getAnalysesBySampleId(sample.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQAEventList(Sample sample) {
        return sample == null ? new ArrayList<>() : sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonRequester(Sample sample) {
        if (sample == null) {
            return null;
        }

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {
            if (PERSON_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                Person person = new Person();
                person.setId(String.valueOf(requester.getRequesterId()));
                personService.getData(person);
                return person.getId() != null ? person : null;
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationRequesters(Sample sample) {
        List<Organization> orgs = new ArrayList<>();
        if (sample == null) {
            return orgs;
        }

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {
            if (ORGANIZATION_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                Organization org = organizationService.getOrganizationById(String.valueOf(requester.getRequesterId()));
                if (org != null && org.getOrganizationTypes().stream()
                        .anyMatch(e -> e.getId().equals(TableIdService.getInstance().REFERRING_ORG_TYPE_ID))) {
                    orgs.add(org);
                }
            }
        }

        return orgs;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRequester getOrganizationSampleRequester(Sample sample, String orgTypeId) {
        if (sample == null) {
            return null;
        }

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {
            if (ORGANIZATION_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                Organization org = organizationService.getOrganizationById(String.valueOf(requester.getRequesterId()));
                if (org != null && org.getOrganizationTypes().stream().anyMatch(e -> e.getId().equals(orgTypeId))) {
                    return requester;
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationRequester(Sample sample, String orgTypeId) {
        if (sample == null) {
            return null;
        }

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {

            if (ORGANIZATION_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                Organization org = organizationService.getOrganizationById(String.valueOf(requester.getRequesterId()));
                if (org != null && org.getOrganizationTypes().stream().anyMatch(e -> e.getId().equals(orgTypeId))) {
                    return org;
                }
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public Sample getPatientPreviousSampleForTestName(Sample sample, Patient patient, String testName) {
        List<Sample> patientSampleList = sampleHumanService.getSamplesForPatient(patient.getId());
        Sample previousSample = null;
        List<Integer> sampIDList = new ArrayList<>();
        List<Integer> testIDList = new ArrayList<>();

        testIDList.add(Integer.parseInt(testService.getTestByLocalizedName(testName).getId()));

        if (patientSampleList.isEmpty()) {
            return previousSample;
        }

        for (Sample patientSample : patientSampleList) {
            sampIDList.add(Integer.parseInt(patientSample.getId()));
        }

        List<Integer> statusList = new ArrayList<>();
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));

        List<Analysis> analysisList = analysisService.getAnalysesBySampleIdTestIdAndStatusId(sampIDList, testIDList,
                statusList);

        if (analysisList.isEmpty()) {
            return previousSample;
        }

        for (int j = 0; j < analysisList.size(); j++) {
            if (j < analysisList.size() && sample.getAccessionNumber()
                    .equals(analysisList.get(j).getSampleItem().getSample().getAccessionNumber())) {
                previousSample = analysisList.get(j + 1).getSampleItem().getSample();
            }
        }

        /*
         * for(int j=0;j<analysisList.size();j++){
         *
         * if(j<analysisList.size() &&
         * sample.getAccessionNumber().equals(analysisList.get(j).getSampleItem().
         * getSample().getAccessionNumber())) return
         * analysisList.get(j+1).getSampleItem().getSample();
         *
         * }
         */
        return previousSample;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Sample sample) {
        getBaseObjectDAO().getData(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getConfirmationSamplesReceivedInDateRange(Date receivedDateStart, Date receivedDateEnd) {
        return getBaseObjectDAO().getConfirmationSamplesReceivedInDateRange(receivedDateStart, receivedDateEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(List<Integer> inclusiveProjectIdList,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession) {
        if (minAccession != null && minAccession.contains(".")) {
            minAccession = minAccession.substring(0, minAccession.indexOf('.'));
        }
        if (maxAccession != null && maxAccession.contains(".")) {
            maxAccession = maxAccession.substring(0, maxAccession.indexOf('.'));
        }
        return getBaseObjectDAO().getSamplesByProjectAndStatusIDAndAccessionRange(inclusiveProjectIdList,
                inclusiveStatusIdList, minAccession, maxAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(String projectId,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession) {
        if (minAccession != null && minAccession.contains(".")) {
            minAccession = minAccession.substring(0, minAccession.indexOf('.'));
        }
        if (maxAccession != null && maxAccession.contains(".")) {
            maxAccession = maxAccession.substring(0, maxAccession.indexOf('.'));
        }
        return getBaseObjectDAO().getSamplesByProjectAndStatusIDAndAccessionRange(projectId, inclusiveStatusIdList,
                minAccession, maxAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumberWithPrefix(String prefix) {
        return getBaseObjectDAO().getLargestAccessionNumberWithPrefix(prefix);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumberMatchingPattern(String startingWith, int size) {
        return getBaseObjectDAO().getLargestAccessionNumberMatchingPattern(startingWith, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesWithPendingQaEventsByService(String serviceId) {
        return getBaseObjectDAO().getSamplesWithPendingQaEventsByService(serviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByStatusAndDomain(List<String> statuses, String domain) {
        return getBaseObjectDAO().getSamplesByStatusAndDomain(statuses, domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesCollectedOn(String collectionDate) {
        return getBaseObjectDAO().getSamplesCollectedOn(collectionDate);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumber() {
        return getBaseObjectDAO().getLargestAccessionNumber();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesWithPendingQaEvents(Sample sample, boolean filterByCategory, String qaEventCategoryId,
            boolean filterByDomain) {
        return getBaseObjectDAO().getSamplesWithPendingQaEvents(sample, filterByCategory, qaEventCategoryId,
                filterByDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Sample getSampleByReferringId(String referringId) {
        return getBaseObjectDAO().getSampleByReferringId(referringId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesReceivedInDateRange(String receivedDateStart, String receivedDateEnd) {
        return getBaseObjectDAO().getSamplesReceivedInDateRange(receivedDateStart, receivedDateEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByAccessionRange(String minAccession, String maxAccession) {
        if (minAccession != null && minAccession.contains(".")) {
            minAccession = minAccession.substring(0, minAccession.indexOf('.'));
        }
        if (maxAccession != null && maxAccession.contains(".")) {
            maxAccession = maxAccession.substring(0, maxAccession.indexOf('.'));
        }
        return getBaseObjectDAO().getSamplesByAccessionRange(minAccession, maxAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public void getSampleByAccessionNumber(Sample sample) {
        if (sample.getAccessionNumber() != null && sample.getAccessionNumber().contains(".")) {
            sample.setAccessionNumber(
                    sample.getAccessionNumber().substring(0, sample.getAccessionNumber().indexOf('.')));
        }
        getBaseObjectDAO().getSampleByAccessionNumber(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getPageOfSamples(int startingRecNo) {
        return getBaseObjectDAO().getPageOfSamples(startingRecNo);
    }

    @Override
    public String generateAccessionNumberAndInsert(Sample sample) {
        sample.setAccessionNumber(getBaseObjectDAO().getNextAccessionNumber());
        return insert(sample);
    }

    @Override
    public String getSampleStatusForDisplay(Sample sample) {
        StatusOfSample statusOfSample = statusOfSampleService.get(sample.getStatusId());
        return statusOfSample.getStatusOfSampleName();
    }

    @Override
    public List<SampleAdditionalField> getSampleAdditionalFieldsForSample(String sampleId) {
        return sampleAdditionalFieldDAO.getAllForSample(sampleId);
    }

    @Override
    public SampleAdditionalField getSampleAdditionalFieldForSample(String sampleId, AdditionalFieldName fieldName) {
        return sampleAdditionalFieldDAO.getFieldForSample(fieldName, sampleId).orElse(new SampleAdditionalField());
    }

    @Override
    public SampleAdditionalField saveSampleAdditionalField(SampleAdditionalField sampleAdditionalField) {
        if (sampleAdditionalField.getLastupdated() == null) {
            return sampleAdditionalFieldDAO.get(sampleAdditionalFieldDAO.insert(sampleAdditionalField)).get();
        } else {
            return sampleAdditionalFieldDAO.update(sampleAdditionalField);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean sampleContainsTest(String sampleId, String testId) {
        for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sampleId)) {
            if (curAnalysis.getTest().getId().equals(testId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean sampleContainsTestWithLoinc(String sampleId, String loinc) {
        for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sampleId)) {
            if (curAnalysis.getTest().getLoinc().equals(loinc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getAllMissingFhirUuid() {
        return sampleDAO.getAllMissingFhirUuid();
    }

    @Override
    public List<Sample> getSamplesByAnalysisIds(List<String> analysisIds) {
        return sampleDAO.getSamplesByAnalysisIds(analysisIds);
    }

    @Override
    public List<Sample> getSamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        return sampleDAO.getSamplesForSiteBetweenOrderDates(referringSiteId, lowerDate, upperDate);
    }

    @Override
    public List<Sample> getStudySamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        return sampleDAO.getStudySamplesForSiteBetweenOrderDates(referringSiteId, lowerDate, upperDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByPriority(OrderPriority priority) {
        return sampleDAO.getSamplesByPriority(priority);
    }
}
