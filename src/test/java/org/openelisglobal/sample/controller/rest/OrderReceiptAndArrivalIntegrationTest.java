package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1424 (FR-B23, FR-C9a): a step save stores who received each sample and
 * the condition it arrived in, the order reads them back with names, and a
 * later save of the same sample keeps every collection detail it sends.
 */
@Transactional
public class OrderReceiptAndArrivalIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private OrderSearchRestController orderSearchRestController;

    private String userId;
    private Patient patient;
    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("RCPT");
        sampleType = fixtures.createTypeOfSample();
    }

    @Test
    public void theReceiverAndTheArrivalConditionReadBackWithNames() {
        Sample sample = newSample();
        persist(sample, "<samples>" + sampleXml("", "", "refrigerated", "4,5", "VENIPUNCTURE") + "</samples>");

        Map<String, Object> saved = onlySample(sample.getAccessionNumber());

        assertEquals("a received sample with no receiver chosen is received by the saving user", userId,
                saved.get("receivedById"));
        assertFalse(String.valueOf(saved.get("receivedByName")).isBlank());
        assertEquals("REFRIGERATED", saved.get("arrivalCondition"));
        assertEquals("4.5", saved.get("arrivalTemperature"));
        assertFalse(String.valueOf(saved.get("arrivalRecordedAt")).isBlank());
    }

    @Test
    public void aLaterSaveOfTheSameSampleKeepsTheDetailsItSends() {
        Sample sample = newSample();
        persist(sample, "<samples>" + sampleXml("", "", "REFRIGERATED", "", "VENIPUNCTURE") + "</samples>");
        String sampleItemId = String.valueOf(onlySample(sample.getAccessionNumber()).get("sampleItemId"));

        persist(sample,
                "<samples>" + sampleXml(sampleItemId, userId, "ROOM_TEMPERATURE", "22", "CAPILLARY") + "</samples>");

        Map<String, Object> saved = onlySample(sample.getAccessionNumber());
        assertEquals(sampleItemId, String.valueOf(saved.get("sampleItemId")));
        assertEquals("ROOM_TEMPERATURE", saved.get("arrivalCondition"));
        assertEquals("22", saved.get("arrivalTemperature"));
        assertEquals("the collection method of a saved sample was silently dropped before OGC-1424", "CAPILLARY",
                saved.get("collectionMethod"));
    }

    @Test
    public void anUnknownArrivalConditionIsNotStored() {
        Sample sample = newSample();
        persist(sample, "<samples>" + sampleXml("", "", "SUNBATHED", "", "") + "</samples>");

        assertEquals("", onlySample(sample.getAccessionNumber()).get("arrivalCondition"));
    }

    private String sampleXml(String sampleItemId, String receivedById, String arrivalCondition,
            String arrivalTemperature, String collectionMethod) {
        return "<sample sampleID='1' typeId='" + sampleType.getId() + "' sampleItemId='" + sampleItemId
                + "' clientKey='' tests='' panels='' testSectionMap='' testSampleTypeMap='' rejected='false'"
                + " collectionMethod='" + collectionMethod + "' receivedDate='" + receivedDate()
                + "' receivedTime='09:30' receivedById='" + receivedById + "' arrivalCondition='" + arrivalCondition
                + "' arrivalTemperature='" + arrivalTemperature + "'/>";
    }

    private static String receivedDate() {
        return org.openelisglobal.common.util.DateUtil.getCurrentDateAsText();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> onlySample(String labNumber) {
        ResponseEntity<Map<String, Object>> response = orderSearchRestController.searchOrder(labNumber);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> samples = (List<Map<String, Object>>) response.getBody().get("samples");
        assertEquals(1, samples.size());
        return samples.get(0);
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("RCP" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private void persist(Sample sample, String sampleXml) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        SampleAddService sampleAddService = new SampleAddService(sampleXml, userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
