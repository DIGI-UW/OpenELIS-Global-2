package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1266 FR-F5, FR-A4: the order's progress and its cancellation are stored
 * through Hibernate in the columns the changeset created, with the types the
 * changeset gave them.
 */
@Transactional
public class OrderProgressPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private OrderProgressService orderProgressService;
    @PersistenceContext
    private EntityManager entityManager;

    private String userId;
    private Patient patient;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        fixtures.ensureSampleCanceledStatus();
        patient = fixtures.createPatient("PROG");
    }

    @Test
    public void theEntrySaveRecordsEnteredAndThePrepareSamplesSaveRecordsPrepared() {
        Sample sample = newSample();
        persist(sample, null);

        Map<String, Object> row = progressRow(sample);
        assertEquals("ENTERED", row.get("order_progress_status"));
        assertNotNull(row.get("order_entered_at"));
        assertNull(row.get("order_prepared_at"));

        persist(sample, "SAMPLES_PREPARED");

        row = progressRow(sample);
        assertEquals("SAMPLES_PREPARED", row.get("order_progress_status"));
        assertNotNull(row.get("order_prepared_at"));
    }

    // Found in review: a reopened order is saved through a detached copy, and
    // the skip-storage decision set on that copy never reached the row.
    @Test
    public void theStorageDecisionOfAReopenedOrderReachesTheRow() {
        Sample sample = newSample();
        persist(sample, null, null);
        entityManager.detach(sample);

        Sample reopened = new Sample();
        reopened.setId(sample.getId());
        reopened.setAccessionNumber(sample.getAccessionNumber());
        reopened.setEnteredDate(sample.getEnteredDate());
        reopened.setReceivedTimestamp(sample.getReceivedTimestamp());
        reopened.setStatusId(sample.getStatusId());
        reopened.setSysUserId(userId);
        reopened.setLastupdated(sample.getLastupdated());
        persist(reopened, "SAMPLES_PREPARED", Boolean.TRUE);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT order_progress_status, storage_skipped FROM clinlims.sample WHERE id = ?",
                Long.valueOf(sample.getId()));
        assertEquals("SAMPLES_PREPARED", row.get("order_progress_status"));
        assertEquals(Boolean.TRUE, row.get("storage_skipped"));
    }

    @Test
    public void cancellingStoresTheReasonAndWhoCancelledInTheirColumns() {
        Sample sample = newSample();
        persist(sample, null);

        orderProgressService.cancel(sample.getId(), "Duplicate order", userId);
        entityManager.flush();

        Map<String, Object> row = jdbcTemplate
                .queryForMap("SELECT order_progress_status, order_cancel_reason, order_cancelled_by, order_cancelled_at"
                        + " FROM clinlims.sample WHERE id = ?", Long.valueOf(sample.getId()));
        assertEquals("CANCELLED", row.get("order_progress_status"));
        assertEquals("Duplicate order", row.get("order_cancel_reason"));
        assertEquals(userId, String.valueOf(row.get("order_cancelled_by")));
        assertNotNull(row.get("order_cancelled_at"));
    }

    private Map<String, Object> progressRow(Sample sample) {
        return jdbcTemplate.queryForMap(
                "SELECT order_progress_status, order_entered_at, order_prepared_at FROM clinlims.sample WHERE id = ?",
                Long.valueOf(sample.getId()));
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("PRG" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private void persist(Sample sample, String progressStep) {
        persist(sample, progressStep, null);
    }

    private void persist(Sample sample, String progressStep, Boolean storageSkipped) {
        SampleAddService sampleAddService = new SampleAddService("<samples></samples>", userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());

        SamplePatientEntryForm form = new SamplePatientEntryForm();
        SampleOrderItem orderItem = new SampleOrderItem();
        orderItem.setProgressStep(progressStep);
        orderItem.setStorageSkipped(storageSkipped);
        form.setSampleOrderItems(orderItem);
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        samplePatientEntryService.persistData(updateData, SpringContext.getBean(PatientManagementUpdate.class),
                patientInfo, form, new MockHttpServletRequest());
        entityManager.flush();
    }
}
