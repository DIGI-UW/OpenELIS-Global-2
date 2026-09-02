package org.openelisglobal.common.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class StatusServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private IStatusService statusService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/status_service.xml");
        // Required because StatusService initializes caches at @PostConstruct
        // which happens before DBUnit loads our dataset.
        statusService.refreshCache();
    }

    @Test
    public void testGetAllStatusLookups_shouldReturnCorrectIds() {
        Assert.assertEquals("101", statusService.getStatusID(OrderStatus.Entered));
        Assert.assertEquals("206", statusService.getStatusID(AnalysisStatus.Finalized));
        Assert.assertEquals("301", statusService.getStatusID(SampleStatus.Entered));
        Assert.assertEquals("401", statusService.getStatusID(ExternalOrderStatus.Entered));

        Assert.assertEquals("1", statusService.getDictionaryID(RecordStatus.NotRegistered));
        Assert.assertEquals("2", statusService.getDictionaryID(RecordStatus.InitialRegistration));
        Assert.assertEquals("3", statusService.getDictionaryID(RecordStatus.ValidationRegistration));
    }

    @Test
    public void testMatches_shouldReturnTrueForMatchingStatus() {
        Assert.assertTrue(statusService.matches("101", OrderStatus.Entered));
        Assert.assertFalse(statusService.matches("101", OrderStatus.Finished));

        Assert.assertTrue(statusService.matches("206", AnalysisStatus.Finalized));
        Assert.assertTrue(statusService.matches("301", SampleStatus.Entered));
        Assert.assertTrue(statusService.matches("401", ExternalOrderStatus.Entered));
    }

    @Test
    public void testGetStatusName_shouldReturnConfiguredNamesWhenNotLocalized() {
        Assert.assertEquals("Test Entered", statusService.getStatusName(OrderStatus.Entered));
        Assert.assertEquals("Finalized", statusService.getStatusName(AnalysisStatus.Finalized));
        Assert.assertEquals("SampleEntered", statusService.getStatusName(SampleStatus.Entered));
        Assert.assertEquals("Entered", statusService.getStatusName(ExternalOrderStatus.Entered));
        Assert.assertEquals("Not Start", statusService.getStatusName(RecordStatus.NotRegistered));
    }

    @Test
    public void testGetStatusForIDReverseLookup_shouldReturnCorrectEnums() {
        Assert.assertEquals(OrderStatus.Entered, statusService.getOrderStatusForID("101"));
        Assert.assertEquals(AnalysisStatus.Finalized, statusService.getAnalysisStatusForID("206"));
        Assert.assertEquals(SampleStatus.Entered, statusService.getSampleStatusForID("301"));
        Assert.assertEquals(ExternalOrderStatus.Entered, statusService.getExternalOrderStatusForID("401"));
        Assert.assertEquals(RecordStatus.NotRegistered, statusService.getRecordStatusForID("1"));

        Assert.assertEquals("Test Entered", statusService.getStatusNameFromId("101"));
        Assert.assertEquals("Finalized", statusService.getStatusNameFromId("206"));
    }

    @Test
    public void testStatusSetForSampleId_shouldBuildAggregatedSet() {
        StatusSet statusSet = statusService.getStatusSetForSampleId("6000");

        Assert.assertEquals("6000", statusSet.getSampleId());
        Assert.assertEquals("5000", statusSet.getPatientId());
        Assert.assertEquals(OrderStatus.Entered, statusSet.getSampleStatus());

        Map<Analysis, AnalysisStatus> analysisMap = statusSet.getAnalysisStatus();
        Assert.assertEquals(1, analysisMap.size());

        Map.Entry<Analysis, AnalysisStatus> entry = analysisMap.entrySet().iterator().next();
        Assert.assertEquals("9000", entry.getKey().getId());
        Assert.assertEquals(AnalysisStatus.Finalized, entry.getValue());
    }

    @Test
    public void testStatusSetForAccessionNumber_shouldBuildAggregatedSet() {
        StatusSet statusSet = statusService.getStatusSetForAccessionNumber("123456789");

        Assert.assertEquals("6000", statusSet.getSampleId());
        Assert.assertEquals("5000", statusSet.getPatientId());
        Assert.assertEquals(OrderStatus.Entered, statusSet.getSampleStatus());

        Map<Analysis, AnalysisStatus> analysisMap = statusSet.getAnalysisStatus();
        Assert.assertEquals(1, analysisMap.size());

        Map.Entry<Analysis, AnalysisStatus> entry = analysisMap.entrySet().iterator().next();
        Assert.assertEquals("9000", entry.getKey().getId());
        Assert.assertEquals(AnalysisStatus.Finalized, entry.getValue());
    }

    @Test
    public void testPersistRecordStatus_shouldCreateObservationHistoryRecords() {
        Sample sample = new Sample();
        sample.setId("6000");

        Patient patient = new Patient();
        patient.setId("5000");

        statusService.persistRecordStatusForSample(sample, RecordStatus.InitialRegistration, patient,
                RecordStatus.ValidationRegistration, "sys123");

        List<ObservationHistory> obsList = observationHistoryService.getAll(patient, sample);
        Assert.assertEquals(2, obsList.size());

        boolean foundSampleRec = false;
        boolean foundPatientRec = false;

        for (ObservationHistory obs : obsList) {
            if ("1".equals(obs.getObservationHistoryTypeId())) { // SampleRecordStatus
                Assert.assertEquals("2", obs.getValue()); // Init Ent dict ID
                foundSampleRec = true;
            } else if ("2".equals(obs.getObservationHistoryTypeId())) { // PatientRecordStatus
                Assert.assertEquals("3", obs.getValue()); // Valid Ent dict ID
                foundPatientRec = true;
            }
        }

        Assert.assertTrue("Sample record status not found", foundSampleRec);
        Assert.assertTrue("Patient record status not found", foundPatientRec);
    }

    @Test
    public void testDeleteRecordStatus_shouldRemoveObservationHistoryRecords() {
        Sample sample = new Sample();
        sample.setId("6000");

        Patient patient = new Patient();
        patient.setId("5000");

        statusService.persistRecordStatusForSample(sample, RecordStatus.InitialRegistration, patient,
                RecordStatus.ValidationRegistration, "sys123");

        List<ObservationHistory> beforeDelete = observationHistoryService.getAll(patient, sample);
        Assert.assertEquals(2, beforeDelete.size());

        statusService.deleteRecordStatus(sample, patient, "sys123");

        List<ObservationHistory> afterDelete = observationHistoryService.getAll(patient, sample);
        Assert.assertEquals(0, afterDelete.size());
    }

    @Test
    public void testRefreshCache_shouldRebuildMapsAfterClear() {
        // Clear the cache to ensure refreshCache actually rebuilds it
        ReflectionTestUtils.setField(statusService, "orderStatusToObjectMap", new HashMap<>());
        ReflectionTestUtils.setField(statusService, "idToOrderStatusMap", new HashMap<>());

        Assert.assertNull(statusService.getStatusNameFromId("101"));

        statusService.refreshCache();

        Assert.assertEquals("Test Entered", statusService.getStatusNameFromId("101"));
    }
}
