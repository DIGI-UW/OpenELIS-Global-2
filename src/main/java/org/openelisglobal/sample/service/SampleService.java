package org.openelisglobal.sample.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;

public interface SampleService extends BaseObjectService<Sample, String> {
    void getData(Sample sample);

    List<Sample> getConfirmationSamplesReceivedInDateRange(Date receivedDateStart, Date receivedDateEnd);

    List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(List<Integer> inclusiveProjectIdList,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession);

    List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(String projectId, List<Integer> inclusiveStatusIdList,
            String minAccession, String maxAccession);

    String getLargestAccessionNumberWithPrefix(String prefix);

    String getLargestAccessionNumberMatchingPattern(String startingWith, int size);

    List<Sample> getSamplesWithPendingQaEventsByService(String serviceId);

    List<Sample> getSamplesByStatusAndDomain(List<String> statuses, String domain);

    List<Sample> getSamplesReceivedOn(String recievedDate);

    List<Sample> getSamplesCollectedOn(String collectionDate);

    String getLargestAccessionNumber();

    List<Sample> getSamplesWithPendingQaEvents(Sample sample, boolean filterByCategory, String qaEventCategoryId,
            boolean filterByDomain);

    Sample getSampleByReferringId(String referringId);

    List<Sample> getSamplesReceivedInDateRange(String receivedDateStart, String receivedDateEnd);

    List<Sample> getSamplesByAccessionRange(String minAccession, String maxAccession);

    boolean insertDataWithAccessionNumber(Sample sample);

    void getSampleByAccessionNumber(Sample sample);

    Sample getSampleByAccessionNumber(String accessionNumber);

    List<Sample> getPageOfSamples(int startingRecNo);

    List<Sample> getSamplesForPatient(String patientID);

    String generateAccessionNumberAndInsert(Sample sample);

    Organization getOrganizationRequester(Sample sample, String orgTypeId);

    Person getPersonRequester(Sample sample);

    List<SampleQaEvent> getSampleQAEventList(Sample sample);

    List<Analysis> getAnalysis(Sample sample);

    Patient getPatient(Sample sample);

    String getId(Sample sample);

    boolean isConfirmationSample(Sample sample);

    String getReceived24HourTimeForDisplay(Sample sample);

    String getReceivedTimeForDisplay(Sample sample);

    String getReceivedDateWithTwoYearDisplay(Sample sample);

    String getReceivedDateForDisplay(Sample sample);

    String getAccessionNumber(Sample sample);

    Timestamp getOrderedDate(Sample sample);

    Date getCompletedDate(Sample sample);

    String getTwoYearReceivedDateForDisplay(Sample sample);

    List<SampleAdditionalField> getSampleAdditionalFieldsForSample(String sampleId);

    SampleAdditionalField getSampleAdditionalFieldForSample(String sampleId, AdditionalFieldName fieldName);

    SampleAdditionalField saveSampleAdditionalField(SampleAdditionalField sampleAdditionalField);

    String getSampleStatusForDisplay(Sample sample);

    boolean sampleContainsTest(String sampleId, String testId);

    SampleRequester getOrganizationSampleRequester(Sample sample, String orgTypeId);

    List<Sample> getAllMissingFhirUuid();

    List<Sample> getSamplesByAnalysisIds(List<String> analysisIds);

    List<Organization> getOrganizationRequesters(Sample sample);

    boolean sampleContainsTestWithLoinc(String id, String loinc);

    List<Sample> getSamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate, LocalDate upperDate);

    List<Sample> getStudySamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Sample> getSamplesByPriority(OrderPriority priority);
}
