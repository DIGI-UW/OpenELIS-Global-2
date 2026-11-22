package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saving an existing order from the order entry page (OGC-1192). Emptying a
 * field the page shows and reloads, such as the provisional diagnosis or the
 * field notes, used to leave the old value in place because blank values were
 * skipped; a field the caller did not send at all is still left alone. The
 * order's referring id, which the page never shows, used to be wiped by every
 * edit.
 */
@Transactional
public class OrderEditClearsFieldsIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private ObservationHistoryService observationHistoryService;

    private String userId;
    private Patient patient;
    private Sample order;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("CLR");
        order = newOrder();
    }

    @Test
    public void emptyingTheProvisionalDiagnosisClearsIt() {
        edit(item -> item.setProvisionalClinicalDiagnosis("Suspected malaria"));
        assertEquals("Suspected malaria", stored(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS));

        edit(item -> item.setProvisionalClinicalDiagnosis(""));

        assertNull(stored(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS));
    }

    @Test
    public void aFieldTheSaveDoesNotSendIsLeftAlone() {
        edit(item -> item.setProvisionalClinicalDiagnosis("Suspected malaria"));

        edit(item -> item.setProvisionalClinicalDiagnosis(null));

        assertEquals("Suspected malaria", stored(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS));
    }

    @Test
    public void emptyingTheFieldNotesOfAnEnvironmentalOrderClearsThem() {
        edit(item -> item.setEnvironmentalFields(envFields("fieldNotes", "Turbid after rain")));
        assertEquals("Turbid after rain", stored(ObservationType.ENV_FIELD_NOTES));

        edit(item -> item.setEnvironmentalFields(envFields("fieldNotes", "")));

        assertNull(stored(ObservationType.ENV_FIELD_NOTES));
    }

    @Test
    public void anEditLeavesTheReferringIdTheFormDoesNotShow() {
        Sample stored = sampleService.get(order.getId());
        stored.setReferringId("EXT-ORDER-17");
        stored.setSysUserId(userId);
        sampleService.update(stored);

        edit(item -> item.setRequesterSampleID(""));

        assertEquals("EXT-ORDER-17", sampleService.get(order.getId()).getReferringId());
    }

    private Map<String, Object> envFields(String key, String value) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("workflowType", "environmental");
        fields.put(key, value);
        return fields;
    }

    private String stored(ObservationType type) {
        ObservationHistory observation = observationHistoryService.getObservationHistoriesBySampleIdAndType(
                order.getId(), observationHistoryService.getObservationTypeIdForType(type));
        return observation == null ? null : observation.getValue();
    }

    private Sample newOrder() {
        Sample sample = new Sample();
        sample.setAccessionNumber("CLR" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);

        SampleAddService sampleAddService = new SampleAddService("<samples></samples>", userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        save(updateData);
        return sample;
    }

    /**
     * One save of the order as the order entry page sends it for an existing order.
     */
    private void edit(Consumer<SampleOrderItem> fill) {
        SampleOrderItem item = new SampleOrderItem();
        item.setSampleId(order.getId());
        item.setLabNo(order.getAccessionNumber());
        fill.accept(item);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setAccessionNumber(order.getAccessionNumber());
        updateData.initSampleData("<samples></samples>", DateUtil.getCurrentDateAsText() + " 00:00", false, item);
        save(updateData);
    }

    private void save(SamplePatientUpdateData updateData) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setRequestedSampleTypes(List.of());
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);
        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
