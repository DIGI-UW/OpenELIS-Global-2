package org.openelisglobal.sample;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dbunit.DatabaseUnitException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleServiceTest extends BaseWebContextSensitiveTest {

    private static final String DEFAULT_ID = "1";
    @Autowired
    SampleService sampleService;

    @Autowired
    SampleHumanService sampleHumanService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/samplehuman.xml");
    }

    @Test
    public void createSample_shouldCreateNewSample() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "patient", "provider", "sample", "sample_human" });
        Date enteredDate = Date.valueOf("2024-06-13");
        String receivedTimestamp = "13/06/2024";
        String accessionNumber = "123";
        Sample samp = createSample(receivedTimestamp, accessionNumber);
        samp.setEnteredDate(enteredDate);

        Assert.assertEquals(0, sampleService.getAll().size());
        // save person to the DB
        String sampleId = sampleService.insert(samp);
        Sample savedSample = sampleService.get(sampleId);

        Assert.assertEquals(1, sampleService.getAll().size());
        Assert.assertEquals(accessionNumber, savedSample.getAccessionNumber());
        Assert.assertEquals("2024-06-13 00:00:00.0", savedSample.getReceivedTimestamp().toString());
    }

    @Test
    public void getAccessionNumber_shouldReturnAccessionNumber() throws Exception {
        Sample savedSample = sampleService.get("2");
        Assert.assertEquals("13333", savedSample.getAccessionNumber());
    }

    @Test
    public void getSampleByAccessionNumber_shouldReturnSampleByAccessionNumber() throws Exception {
        Sample savedSample = sampleService.getSampleByAccessionNumber("13333");
        Assert.assertEquals("2024-06-04 00:00:00.0", savedSample.getReceivedTimestamp().toString());
    }

    @Test
    public void insertDataWithAccessionNumber_shouldReturnsampleWithInsertedData() throws Exception {
        Sample savedSample = sampleService.getSampleByAccessionNumber("13333");
        savedSample.setEnumName("HIV4");
        sampleService.update(savedSample);

        Assert.assertEquals("HIV4", savedSample.getEnumName());
    }

    @Test
    public void getOrderedDate_shouldReturnOrderedDate() throws Exception {
        Sample savedSample = sampleService.get("1");
        Assert.assertEquals("2024-06-03 00:00:00.0", sampleService.getOrderedDate(savedSample).toString());
    }

    @Test
    public void getSamplesReceivedOn_shouldReturnSamplesOnDate() throws Exception {
        int receivedSamples = sampleService.getSamplesReceivedOn("04/06/2024").size();
        Assert.assertEquals(1, receivedSamples);
    }

    @Test
    public void getSamplesForPatient_shouldReturnSamplesForPatient() throws ParseException {
        Assert.assertEquals(1, sampleHumanService.getSamplesForPatient("1").size());
    }

    @Test
    public void getReceivedDateForDisplay_shouldReturnReceivedDateForDisplay() throws Exception {
        Sample savedSample = sampleService.get("2");
        Assert.assertEquals("04/06/2024", sampleService.getReceivedDateForDisplay(savedSample));
    }

    @Test
    public void getReceived24HourTimeForDisplay_shouldReturnReceived24HourTimeForDisplay() throws Exception {
        Sample savedSample = sampleService.get("2");
        Assert.assertEquals("00:00", sampleService.getReceived24HourTimeForDisplay(savedSample));
    }

    @Test
    public void getReceivedTimeForDisplay_shouldReturnReceivedTimeForDisplay() throws Exception {
        Sample savedSample = sampleService.get("2");
        Assert.assertEquals("00:00", sampleService.getReceivedTimeForDisplay(savedSample));
    }

    @Test
    public void isConfirmationSample_shouldReturnIsConfirmationSample() throws Exception {
        Sample savedSample = sampleService.get("2");
        savedSample.setIsConfirmation(true);
        assertFalse(sampleService.isConfirmationSample(null));
        assertTrue(sampleService.isConfirmationSample(savedSample));
    }

    @Test
    public void getReceivedDateWithTwoYearDisplay_shouldReturnReceivedDateWithTwoYearDisplay() throws Exception {
        Sample savedSample = sampleService.get("2");
        Assert.assertEquals("04/06/24", sampleService.getReceivedDateWithTwoYearDisplay(savedSample));
    }

    @Test
    public void getConfirmationSamplesReceivedInDateRange_shouldReturnConfirmationSamplesReceivedInDateRange()
            throws ParseException, SQLException, DatabaseUnitException {
        cleanRowsInCurrentConnection(new String[] { "person", "patient", "provider", "sample", "sample_human" });

        Date recievedDateStart = Date.valueOf("2024-06-03");
        Date recievedDateEnd = Date.valueOf("2024-06-04");
        Date enteredDate = Date.valueOf("2024-06-03");
        String receivedTimestamp = "03/06/2024";
        String accessionNumber = "12";
        Sample samp = createSample(receivedTimestamp, accessionNumber);
        samp.setEnteredDate(enteredDate);
        samp.setIsConfirmation(true);

        String sampleId = sampleService.insert(samp);
        Sample savedSample = sampleService.get(sampleId);
        Assert.assertEquals(1,
                sampleService.getConfirmationSamplesReceivedInDateRange(recievedDateStart, recievedDateEnd).size());
    }

    @Test
    public void getSamplesCollectedOn_shouldReturnSamplesCollected() {
        Assert.assertEquals(2, sampleService.getSamplesCollectedOn("03/06/2024").size());
    }

    @Test
    public void getLargestAccessionNumber_shouldReturnLargestAccessionNumber() {
        Assert.assertEquals("52541", sampleService.getLargestAccessionNumber());
    }

    @Test
    public void getSamplesReceivedInDateRange_shouldReturnSamplesReceivedInDateRange() {
        Assert.assertEquals(2, sampleService.getSamplesReceivedInDateRange("03/06/2024", "04/06/2024").size());
    }

    @Test
    public void getSamplesByAccessionRange_shouldReturnSamplesByAccessionRange() {
        Assert.assertEquals(2, sampleService.getSamplesByAccessionRange("12345", "13333").size());
    }

    @Test
    public void getId_shouldReturnId() {
        Sample savedSample = sampleService.get("1");
        Assert.assertEquals("1", sampleService.getId(savedSample));
    }

    private Sample createSample(String receivedTimestamp, String accessionNumber) throws ParseException {

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = dateFormat.parse(receivedTimestamp);
        long time = date.getTime();
        Timestamp doc = new Timestamp(time);

        Sample sample = new Sample();
        sample.setReceivedTimestamp(doc);
        sample.setAccessionNumber(accessionNumber);

        return sample;
    }

    @Test
    public void getAnalysis_shouldReturnAnalysesForSample() {
        Sample sample = sampleService.get(DEFAULT_ID);
        Assert.assertNotNull(sample);

        List<Analysis> analyses = sampleService.getAnalysis(sample);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysis_shouldReturnNull_whenSampleIsNotPersisted() {
        Sample transientSample = new Sample();
        List<Analysis> results = sampleService.getAnalysis(transientSample);
        Assert.assertNull(results);
    }

    @Test
    public void getPatient_shouldReturnPatient_whenSampleExists() {
        Sample sample = sampleService.get(DEFAULT_ID);
        Patient patient = sampleService.getPatient(sample);
        Assert.assertNotNull(patient.getPerson());
    }

    @Test
    public void getSamplesForPatient_shouldReturnList_whenPatientExists() {
        List<Sample> samples = sampleService.getSamplesForPatient("1");
        Assert.assertNotNull(samples);
        Assert.assertFalse("Patient 1 should have samples", samples.isEmpty());
    }

    @Test
    public void getData_shouldPopulateSampleDetails() {
        Sample sample = sampleService.get(DEFAULT_ID);
        sampleService.getData(sample);
        Assert.assertNotNull(sample.getAccessionNumber());
        Assert.assertNotNull(sample.getReceivedTimestamp());
    }

    @Test
    public void getSamplesByStatusAndDomain_shouldReturnList() {
        List<String> statuses = Arrays.asList("entered", "released");
        List<Sample> results = sampleService.getSamplesByStatusAndDomain(statuses, "clinical");
        Assert.assertTrue(results.stream().allMatch(s -> statuses.contains(s.getStatus())));
    }

    @Test
    public void getSampleByReferringId_shouldReturnNull_whenNotFound() {
        Sample result = sampleService.getSampleByReferringId("REF-99999");
        Assert.assertNull(result);
    }

    @Test
    public void getSamplesByAnalysisIds_shouldReturnList() {
        List<String> analysisIds = new ArrayList<>();
        List<Sample> results = sampleService.getSamplesByAnalysisIds(analysisIds);
        Assert.assertNotNull(results);
        Assert.assertTrue("Empty analysis IDs should return empty list", results.isEmpty());
    }

    @Test
    public void getLargestAccessionNumberMatchingPattern_shouldReturnResult() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample", "sample_human" });

        // Insert a known sample to find
        Sample sample = createSample("01/01/2024", "2024-999");
        sample.setEnteredDate(Date.valueOf(LocalDate.of(2024, 1, 1)));
        sample.setId(null);
        sampleService.insertDataWithAccessionNumber(sample);

        String result = sampleService.getLargestAccessionNumberMatchingPattern("2024", 8);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.startsWith("2024"));
    }

    @Test
    public void getLargestAccessionNumberWithPrefix_shouldReturnResult() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample", "sample_human" });

        Sample sample = createSample("01/01/2024", "2024-999");
        sample.setEnteredDate(Date.valueOf(LocalDate.of(2024, 1, 1)));
        sample.setId(null); // Ensure ID is null so it inserts as new

        sampleService.insertDataWithAccessionNumber(sample);

        String result = sampleService.getLargestAccessionNumberWithPrefix("2024");
        Assert.assertNotNull("Should find the sample we just inserted", result);
        Assert.assertEquals("The result should match the inserted accession number", "2024-999", result);
    }

    @Test
    public void getSamplesWithPendingQaEventsByService_shouldReturnList() {
        List<Sample> results = sampleService.getSamplesWithPendingQaEventsByService(DEFAULT_ID);
        Assert.assertNotNull(results);
    }

    @Test
    public void getAllMissingFhirUuid_shouldReturnList() {
        List<Sample> results = sampleService.getAllMissingFhirUuid();
        Assert.assertNotNull(results);
    }

    @Test
    public void getAccessionNumber_shouldReturnString() {
        Sample sample = new Sample();
        sample.setAccessionNumber("123");
        String result = sampleService.getAccessionNumber(sample);
        Assert.assertEquals("123", result);
    }

    @Test
    public void insertDataWithAccessionNumber_shouldRun() throws Exception {
        Sample sample = createSample("01/01/2024", "99999");
        sample.setEnteredDate(Date.valueOf(LocalDate.of(2024, 1, 1)));
        sample.setId(null); // ensure new insert

        sampleService.insertDataWithAccessionNumber(sample);

        Assert.assertNotNull(sample.getId());

        Sample persisted = sampleService.get(sample.getId());
        Assert.assertNotNull(persisted);
        Assert.assertEquals("99999", persisted.getAccessionNumber());
    }

    @Test
    public void generateAccessionNumberAndInsert_shouldRun() throws Exception {
        Sample sample = createSample("01/01/2024", "88888");
        sample.setEnteredDate(Date.valueOf(LocalDate.of(2024, 1, 1)));
        sample.setId(null);

        sampleService.generateAccessionNumberAndInsert(sample);

        Assert.assertNotNull(sample.getAccessionNumber());
        Assert.assertNotEquals("88888", sample.getAccessionNumber());

    }

    @Test
    public void getSamplesForSiteBetweenOrderDates_shouldReturnList() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        List<Sample> results = sampleService.getSamplesForSiteBetweenOrderDates(DEFAULT_ID, start, end);
        Assert.assertNotNull(results);
    }

    @Test
    public void getStudySamplesForSiteBetweenOrderDates_shouldReturnList() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        List<Sample> results = sampleService.getStudySamplesForSiteBetweenOrderDates(DEFAULT_ID, start, end);
        Assert.assertNotNull(results);
    }

    @Test
    public void getTableReferenceId_shouldReturnConstant() {
        String refId = org.openelisglobal.sample.service.SampleServiceImpl.getTableReferenceId();

        Assert.assertNotNull("Table Reference ID should not be null", refId);
        Assert.assertEquals("1", refId);
    }

    @Test
    public void sampleContainsTest_shouldCheckTests() {
        Sample sample = sampleService.get(DEFAULT_ID);
        List<Analysis> analyses = sampleService.getAnalysis(sample);

        // Case A: Test the "True" path (Loop finds a match)
        if (analyses != null && !analyses.isEmpty()) {
            String validTestId = analyses.get(0).getTest().getId();
            boolean result = sampleService.sampleContainsTest(DEFAULT_ID, validTestId);
            Assert.assertTrue("Should return true when test exists in sample", result);
        }

        // Case B: Test the "False" path (Loop finishes without match)
        boolean falseResult = sampleService.sampleContainsTest(DEFAULT_ID, "99999_NON_EXISTENT");
        Assert.assertFalse("Should return false when test does not exist", falseResult);
    }

    @Test
    public void sampleContainsTestWithLoinc_shouldCheckLoinc() {
        Sample sample = sampleService.get(DEFAULT_ID);
        List<Analysis> analyses = sampleService.getAnalysis(sample);

        // Case A: Test the "True" path
        if (analyses != null && !analyses.isEmpty()) {
            String validLoinc = analyses.get(0).getTest().getLoinc();
            // Only test true if the seed data actually has a LOINC code
            if (validLoinc != null) {
                boolean result = sampleService.sampleContainsTestWithLoinc(DEFAULT_ID, validLoinc);
                Assert.assertTrue("Should return true when LOINC matches", result);
            }
        }

        // Case B: Test the "False" path
        boolean falseResult = sampleService.sampleContainsTestWithLoinc(DEFAULT_ID, "INVALID_LOINC_CODE");
        Assert.assertFalse("Should return false when LOINC does not exist", falseResult);
    }
}
