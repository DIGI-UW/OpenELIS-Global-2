package org.openelisglobal.sample;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.dbunit.DatabaseUnitException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleServiceTest extends BaseWebContextSensitiveTest {

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
}
