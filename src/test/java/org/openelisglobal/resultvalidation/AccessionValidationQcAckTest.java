package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.bean.QcFailureItem;
import org.openelisglobal.resultvalidation.dao.ValidationQcAcknowledgmentDAO;
import org.openelisglobal.resultvalidation.exception.QcAcknowledgmentRequiredException;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.resultvalidation.valueholder.ValidationQcAcknowledgment;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the S-08 FR-04 validation QC acknowledgment flow: the
 * failure list lookup, the ack persistence DAO chain, and the service-level
 * release gate in {@link ResultValidationService#persistdata}.
 *
 * <p>
 * Fixture: {@code testdata/validation-qc-ack.xml} seeds one accession with a
 * client analysis (id=100) in TechnicalAcceptance and a BLANK QC analysis
 * (id=101) whose Result is pre-stamped {@code qc_evaluation=FAIL}.
 */
public class AccessionValidationQcAckTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-QC-ACK-001";
    private static final String CLIENT_ANALYSIS_ID = "100";
    private static final String QC_ANALYSIS_ID = "101";

    @Autowired
    private ResultsValidationUtility validationUtility;

    @Autowired
    private ResultValidationService resultValidationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ValidationQcAcknowledgmentDAO qcAckDAO;

    @Autowired
    private IStatusService statusService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/validation-qc-ack.xml");
    }

    @Test
    public void findFailedQcForAccession_returnsTheFailedBlank() {
        List<QcFailureItem> failures = validationUtility.findFailedQcForAccession(ACCESSION);
        assertEquals(1, failures.size());
        QcFailureItem item = failures.get(0);
        assertEquals(QC_ANALYSIS_ID, item.getAnalysisId());
        assertEquals(ACCESSION, item.getAccessionNumber());
        assertEquals("BLANK", item.getQcType());
        assertEquals("0.8", item.getResultValue());
        assertEquals("Result = 0.8 (threshold 0.5)", item.getQcEvaluationDetail());
    }

    @Test
    public void findFailedQcForAccession_unknownAccession_returnsEmpty() {
        assertTrue(validationUtility.findFailedQcForAccession("DOES-NOT-EXIST").isEmpty());
    }

    @Test
    public void getValidationAnalysisBySample_excludesQcAnalyses() {
        // This is the path the controller uses when doRange=false. It must
        // exclude QC analyses — they don't need per-row validation.
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION);
        assertNotNull(sample);
        java.util.List<org.openelisglobal.resultvalidation.bean.AnalysisItem> rows = validationUtility
                .getValidationAnalysisBySample(sample);
        assertEquals("Only the client analysis should appear; the failed BLANK is filtered out", 1, rows.size());
        assertEquals(CLIENT_ANALYSIS_ID, rows.get(0).getAnalysisId());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.sample.service.SampleService sampleService;

    @Test
    public void persistdata_withoutAck_throwsQcAcknowledgmentRequiredException() {
        Analysis clientAnalysis = analysisService.get(CLIENT_ANALYSIS_ID);
        assertNotNull(clientAnalysis);

        // Simulate the validation controller's transition to Finalized for the
        // client analysis. The gate must block this because the batch's BLANK QC
        // has a failure without an acknowledgment row.
        String finalizedStatusId = statusService.getStatusID(AnalysisStatus.Finalized);
        assertNotNull("Finalized status must be seeded in the fixture", finalizedStatusId);
        assertTrue("Finalized status must not be the missing-status sentinel", !"-1".equals(finalizedStatusId));
        clientAnalysis.setStatusId(finalizedStatusId);

        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(clientAnalysis);

        try {
            resultValidationService.persistdata(Collections.emptyList(), analysisUpdateList, new ArrayList<Result>(),
                    new ArrayList<AnalysisItem>(), new ArrayList<Sample>(), new ArrayList<Note>(), null,
                    Collections.emptyList(), "1");
            fail("Expected QcAcknowledgmentRequiredException because the QC BLANK has no ack row");
        } catch (QcAcknowledgmentRequiredException expected) {
            assertTrue("Exception message should reference the unacknowledged analysis id",
                    expected.getMessage().contains(QC_ANALYSIS_ID));
        }
    }

    @Test
    public void persistdata_withAck_doesNotThrow() {
        // Seed the ack row that the gate expects.
        ValidationQcAcknowledgment ack = new ValidationQcAcknowledgment();
        ack.setAnalysisId(Integer.valueOf(QC_ANALYSIS_ID));
        ack.setAcknowledgedBy(1);
        ack.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        ack.setJustification("Reviewed — repeat passed");
        resultValidationService.persistQcAcknowledgment(ack);

        // Confirm round-trip.
        List<ValidationQcAcknowledgment> persisted = qcAckDAO.findByAnalysisId(Integer.valueOf(QC_ANALYSIS_ID));
        assertEquals(1, persisted.size());
        assertEquals("Reviewed — repeat passed", persisted.get(0).getJustification());

        // Now the gate should let the release through.
        Analysis clientAnalysis = analysisService.get(CLIENT_ANALYSIS_ID);
        clientAnalysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(clientAnalysis);

        // Should not throw — the gate is satisfied. Any other downstream errors
        // (notification, sample finishing) would mask the gate result, so the
        // assertion is just "no QcAcknowledgmentRequiredException".
        try {
            resultValidationService.persistdata(Collections.emptyList(), analysisUpdateList, new ArrayList<Result>(),
                    new ArrayList<AnalysisItem>(), new ArrayList<Sample>(), new ArrayList<Note>(), null,
                    Collections.emptyList(), "1");
        } catch (QcAcknowledgmentRequiredException unexpected) {
            fail("Gate should pass once an ack row exists for the failed analysis");
        }
    }

    @Test
    public void persistdata_release_transitionsQcAnalysesToFinalizedWithNullReleasedDate() {
        // Seed the ack so the gate passes.
        ValidationQcAcknowledgment ack = new ValidationQcAcknowledgment();
        ack.setAnalysisId(Integer.valueOf(QC_ANALYSIS_ID));
        ack.setAcknowledgedBy(1);
        ack.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        ack.setJustification("ack");
        resultValidationService.persistQcAcknowledgment(ack);

        String finalizedId = statusService.getStatusID(AnalysisStatus.Finalized);
        Analysis clientAnalysis = analysisService.get(CLIENT_ANALYSIS_ID);
        clientAnalysis.setStatusId(finalizedId);
        clientAnalysis.setReleasedDate(new Timestamp(System.currentTimeMillis()));
        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(clientAnalysis);

        resultValidationService.persistdata(Collections.emptyList(), analysisUpdateList, new ArrayList<Result>(),
                new ArrayList<AnalysisItem>(), new ArrayList<Sample>(), new ArrayList<Note>(), null,
                Collections.emptyList(), "1");

        // QC analysis must have flipped to Finalized with released_date NULL.
        Analysis qcAfter = analysisService.get(QC_ANALYSIS_ID);
        assertEquals("QC analysis should transition to Finalized", finalizedId, qcAfter.getStatusId());
        assertNull("QC analysis released_date must stay null (not a patient release)", qcAfter.getReleasedDate());

        // Client analysis released_date should remain set (we set it above).
        Analysis clientAfter = analysisService.get(CLIENT_ANALYSIS_ID);
        assertEquals(finalizedId, clientAfter.getStatusId());
        assertNotNull("Client analysis released_date must persist", clientAfter.getReleasedDate());
    }

    /**
     * The actual {@code clinlims.history} INSERT cannot be asserted here because
     * {@code AuditTrailService} is mocked in the Spring test profile
     * ({@code AppTestConfig.auditTrailService}). What we can — and should — verify
     * is that our service correctly delegates to it with the right parameters when
     * persisting an ack. The real DB write is exercised in dev.
     */
    @Test
    public void persistQcAcknowledgment_delegatesToAuditTrailService() {
        org.openelisglobal.audittrail.dao.AuditTrailService auditMock = org.springframework.test.util.AopTestUtils
                .getTargetObject(auditTrailServiceMock);
        org.mockito.Mockito.reset(auditMock);

        ValidationQcAcknowledgment ack = new ValidationQcAcknowledgment();
        ack.setAnalysisId(Integer.valueOf(QC_ANALYSIS_ID));
        ack.setAcknowledgedBy(1);
        ack.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        ack.setJustification("auditable note");
        resultValidationService.persistQcAcknowledgment(ack);

        org.mockito.ArgumentCaptor<org.openelisglobal.common.valueholder.BaseObject> objectCaptor = org.mockito.ArgumentCaptor
                .forClass(org.openelisglobal.common.valueholder.BaseObject.class);
        org.mockito.Mockito.verify(auditMock).saveNewHistory(objectCaptor.capture(), org.mockito.Mockito.eq("1"),
                org.mockito.Mockito.eq("validation_qc_acknowledgment"));
        // Auto-generated id should be assigned by the time saveNewHistory is invoked.
        assertNotNull(objectCaptor.getValue().getStringId());
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("auditTrailService")
    private org.openelisglobal.audittrail.dao.AuditTrailService auditTrailServiceMock;
}
