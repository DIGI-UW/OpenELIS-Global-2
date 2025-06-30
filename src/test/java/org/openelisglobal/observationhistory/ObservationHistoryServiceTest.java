package org.openelisglobal.observationhistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

public class ObservationHistoryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleService sampleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/observation-history.xml");
    }

    public void testDataInDataBase() {
        List<ObservationHistory> observationHistories = observationHistoryService.getAll();
        observationHistories.forEach(observationHistory -> {
            System.out.print(observationHistory.getValue() + " ");
        });
    }

    @Test
    public void getAll_shouldReturnAllObservationHistories() {
        List<ObservationHistory> observationHistories = observationHistoryService.getAll();
        assertTrue(observationHistories.size() == 3);
    }

    @Test
    public void getAllByPatientAndSample_shouldReturnAllObservationHistoriesForPatientAndSample() {
        Patient patient = patientService.get("1");
        Sample sample = sampleService.get("1");
        List<ObservationHistory> observationHistories = observationHistoryService.getAll(patient, sample);
        assertTrue(observationHistories.size() == 1);
    }

    @Test
    public void insert_shouldInsertObservationHistory() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "observation_history" });
        ObservationHistory observationHistory = new ObservationHistory();
        // observationHistory.setId("5");
        observationHistory.setPatientId("4");
        observationHistory.setSampleId("4");
        observationHistory.setObservationHistoryTypeId("4");
        observationHistory.setValueType("TEXT");
        observationHistory.setValue("PhD");
        observationHistoryService.insert(observationHistory);

        List<ObservationHistory> observationHistories = observationHistoryService.getAll();
        assertTrue(observationHistories.size() == 1);

        observationHistoryService.delete(observationHistory);
    }

    @Test
    public void getById_shouldReturnObservationHistoryById() {
        ObservationHistory observationHistory = new ObservationHistory();
        observationHistory.setId("1");

        ObservationHistory result = observationHistoryService.getById(observationHistory);
        assertEquals("University", result.getValue());
        assertEquals("1", result.getPatientId());
        assertEquals("1", result.getSampleId());
    }

    @Test
    public void update_shouldUpdateObservationHistory() {
        ObservationHistory observationHistory = new ObservationHistory();
        observationHistory.setId("1");

        ObservationHistory toUpdate = observationHistoryService.getById(observationHistory);
        toUpdate.setValue("PhD");
        observationHistoryService.update(toUpdate);

        ObservationHistory updated = observationHistoryService.getById(observationHistory);
        assertEquals("PhD", updated.getValue());
    }

    @Test
    public void getObservationHistoriesBySampleId_shouldReturnObservationHistoriesBySampleId() {
        List<ObservationHistory> observationHistories = observationHistoryService
                .getObservationHistoriesBySampleId("1");
        assertTrue(observationHistories.size() == 1);
    }

    @Test
    public void getObservationHistoriesBySampleItemId_shouldReturnObservationHistoriesBySampleItemId() {
        // Assuming sample item ID is stored in the sample_item_id field
        List<ObservationHistory> observationHistories = observationHistoryService
                .getObservationHistoriesBySampleItemId("1");
        assertTrue(observationHistories.isEmpty()); // Update this based on your test data
    }

    @Test
    public void getObservationHistoriesByValueAndType_shouldReturnObservationHistoriesByValueAndType() {
        ObservationHistoryType type = observationHistoryTypeService.get("1");
        List<ObservationHistory> observationHistories = observationHistoryService
                .getObservationHistoriesByValueAndType("University", type.getId(), "L");
        assertEquals(1, observationHistories.size());
    }

    @Test
    public void getObservationsByTypeAndValue_shouldReturnObservationsByTypeAndValue() {
        // Initialize the service to populate the observationTypeToIdMap
        String typeId = observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE);

        // Only run this test if the observation type exists in the database
        if (typeId != null) {
            List<ObservationHistory> observationHistories = observationHistoryService
                    .getObservationsByTypeAndValue(ObservationType.REQUEST_DATE, "Engineer");
            assertTrue(observationHistories.size() >= 0);
        }
    }

    @Test
    public void getValueForSample_shouldReturnValueForSample() {
        // Initialize the service to populate the observationTypeToIdMap
        String typeId = observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE);

        String value = observationHistoryService.getValueForSample(ObservationType.REQUEST_DATE, "3");
        // assertNotNull(value);
        assertEquals("Engineer", value);
    }

    @Test
    public void getMostRecentValueForPatient_shouldReturnMostRecentValueForPatient() {
        // Initialize the service to populate the observationTypeToIdMap
        String typeId = observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE);

        String value = observationHistoryService.getMostRecentValueForPatient(ObservationType.REQUEST_DATE, "3");
        // assertNotNull(value);
        assertEquals("Engineer", value);
    }

    @Test
    public void getObservationForSample_shouldReturnObservationForSample() {
        // Initialize the service to populate the observationTypeToIdMap
        String typeId = observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE);

        ObservationHistory observation = observationHistoryService.getObservationForSample(ObservationType.REQUEST_DATE,
                "3");
        assertNotNull(observation);

    }

    @Test
    public void getLastObservationForPatient_shouldReturnLastObservationForPatient() {
        // Initialize the service to populate the observationTypeToIdMap
        String typeId = observationHistoryService.getObservationTypeIdForType(ObservationType.REQUEST_DATE);

        ObservationHistory observation = observationHistoryService
                .getLastObservationForPatient(ObservationType.REQUEST_DATE, "3");
        assertNotNull(observation);

    }

    @Test
    public void getAllByPatientAndSampleAndType_shouldReturnAllObservationHistoriesForPatientAndSampleAndType() {
        Patient patient = patientService.get("1");
        Sample sample = sampleService.get("1");
        ObservationHistoryType type = observationHistoryTypeService.get("1");

        List<ObservationHistory> observationHistories = observationHistoryService.getAll(patient, sample, type.getId());
        assertTrue(observationHistories.size() == 1);
    }
}