package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coverage for the two interlocking S-14 / OGC-624 fixes:
 *
 * <ol>
 * <li>REQUIRES_NEW propagation on {@code markSubcontractReceived} /
 * {@code markSubcontractResultsReturned} so the strict-linear guard's
 * {@link IllegalStateException} no longer poisons a parent transaction (the
 * FHIR result-import path's outer @Transactional method).</li>
 * <li>The DISPATCHED → RECEIVED auto-trigger wired into
 * {@code FhirApiWorkFlowServiceImpl#beginTaskCheckIfAcceptedPath} when the
 * remote receiver flips its Task to ACCEPTED.</li>
 * </ol>
 *
 * <p>
 * The poisoning tests deliberately do <b>not</b> use {@code @Transactional} at
 * the method level: Spring's test-tx auto-rollback would mask a missing
 * REQUIRES_NEW (a poisoned outer tx would roll back anyway at test end and the
 * sentinel side-effect would vanish silently). Instead they drive an outer tx
 * via {@link TransactionTemplate}, commit it, then query post-commit.
 */
public class SubcontractAutoTransitionTest extends BaseWebContextSensitiveTest {

    private static final String SENTINEL_NOTE = "OUTER_TX_SENTINEL";
    private static final UUID REFERRAL_1_FHIR_UUID = UUID.fromString("436a353f-8ac9-4a30-86db-bb6eda979b57");
    private static final String REMOTE_STORE = "http://test-remote/fhir";

    @Autowired
    private ReferralService referralService;

    @Autowired
    private FhirApiWorkflowService fhirApiWorkflowService;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    private FhirPersistanceService fhirPersistanceServiceMock;
    private Object originalFhirPersistanceService;
    private Object originalStatusHistoryDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        fhirPersistanceServiceMock = mock(FhirPersistanceService.class);
        originalFhirPersistanceService = ReflectionTestUtils.getField(fhirApiWorkflowService, "fhirPersistanceService");
        ReflectionTestUtils.setField(fhirApiWorkflowService, "fhirPersistanceService", fhirPersistanceServiceMock);
        originalStatusHistoryDAO = ReflectionTestUtils.getField(referralService, "statusHistoryDAO");
        Mockito.reset(fhirUtil, fhirConfig);
        when(fhirConfig.getRemoteStoreIdentifier()).thenReturn(List.of("test-remote-id"));
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(fhirApiWorkflowService, "fhirPersistanceService", originalFhirPersistanceService);
        ReflectionTestUtils.setField(referralService, "statusHistoryDAO", originalStatusHistoryDAO);
        Mockito.reset(fhirUtil, fhirConfig);
        jdbcTemplate.update("DELETE FROM clinlims.referral_status_history WHERE notes = ?", SENTINEL_NOTE);
    }

    // ---- Transaction-poisoning tests ----------------------------------------

    @Test
    public void markSubcontractReceived_rejectsTransition_doesNotPoisonOuterTransaction() {
        // Walk fixture's referral 1 forward to RECEIVED so a second
        // markSubcontractReceived call hits the strict-linear guard.
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");
        referralService.markSubcontractReceived("1", "1", "prep received");

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            insertSentinelHistory("1");
            try {
                referralService.markSubcontractReceived("1", "1", "rejected — already received");
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // The fix: REQUIRES_NEW means the inner tx rolls back alone,
                // the outer tx is NOT marked rollback-only, and our sentinel
                // commits when this TransactionTemplate returns.
            }
            return null;
        });

        assertEquals(1L, countSentinelRows("1"));
    }

    @Test
    public void markSubcontractResultsReturned_rejectsTransition_doesNotPoisonOuterTransaction() {
        // DISPATCHED -> RESULTS_RETURNED is illegal (must pass through RECEIVED).
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            insertSentinelHistory("1");
            try {
                referralService.markSubcontractResultsReturned("1", "1", "rejected — skipping RECEIVED");
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // Same poisoning shape as the production FHIR result-import path.
            }
            return null;
        });

        assertEquals(1L, countSentinelRows("1"));
    }

    // ---- Auto-trigger tests -------------------------------------------------

    @Test
    public void autoReceived_firesWhenRemoteTaskAccepted() throws Exception {
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");
        // Sender-direction: remote Task is ACCEPTED by receiver, no local task-based-on
        // copy.
        stubRemoteFhirSearch(remoteTasksBundle(REFERRAL_1_FHIR_UUID.toString(), TaskStatus.ACCEPTED));
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString())).thenReturn(Optional.empty());

        invokeBeginTaskCheckIfAcceptedPath();

        assertEquals("RECEIVED", currentSubcontractStatus("1"));

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        ReferralStatusHistory latest = history.get(history.size() - 1);
        assertEquals(SubcontractStatus.DISPATCHED, latest.getFromStatus());
        assertEquals(SubcontractStatus.RECEIVED, latest.getToStatus());
        assertEquals("1", latest.getChangedByUserId());
        assertEquals("FHIR auto: receiver accepted Task " + REFERRAL_1_FHIR_UUID, latest.getNotes());
        assertNotNull(latest.getChangedAt());
    }

    @Test
    public void autoReceived_isIdempotentOnRepeatedObservations() throws Exception {
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");
        // Mirrors real HAPI: once the remote Task is ACCEPTED, the filter
        // (REQUESTED, RECEIVED) returns 0 entries for that referral on the
        // second poll cycle. Sender-direction: no local task-based-on copy.
        stubRemoteFhirSearch(remoteTasksBundle(REFERRAL_1_FHIR_UUID.toString(), TaskStatus.ACCEPTED), new Bundle());
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString())).thenReturn(Optional.empty());

        invokeBeginTaskCheckIfAcceptedPath();
        invokeBeginTaskCheckIfAcceptedPath();

        long autoRows = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").stream()
                .filter(h -> SubcontractStatus.DISPATCHED.equals(h.getFromStatus())
                        && SubcontractStatus.RECEIVED.equals(h.getToStatus()))
                .count();
        assertEquals(1L, autoRows);
    }

    @Test
    public void autoReceived_skipsWhenSubcontractAlreadyPastReceived() throws Exception {
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");
        referralService.markSubcontractReceived("1", "1", "prep received");
        long historyBefore = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size();

        stubRemoteFhirSearch(remoteTasksBundle(REFERRAL_1_FHIR_UUID.toString()));
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString()))
                .thenReturn(Optional.of(localTaskWithStatus(TaskStatus.ACCEPTED)));

        invokeBeginTaskCheckIfAcceptedPath();

        assertEquals("RECEIVED", currentSubcontractStatus("1"));
        assertEquals(historyBefore, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
    }

    @Test
    public void autoReceived_doesNotFireOnTaskRejected() throws Exception {
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "1", "prep dispatch");
        long historyBefore = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size();

        stubRemoteFhirSearch(remoteTasksBundle(REFERRAL_1_FHIR_UUID.toString()));
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString()))
                .thenReturn(Optional.of(localTaskWithStatus(TaskStatus.REJECTED)));

        invokeBeginTaskCheckIfAcceptedPath();

        assertEquals("DISPATCHED", currentSubcontractStatus("1"));
        assertEquals(historyBefore, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
    }

    @Test
    public void autoReceived_subcontractAtDraft_logsAndSwallows() throws Exception {
        // Fixture leaves referral 1 at DRAFT — DRAFT -> RECEIVED is illegal.
        long historyBefore = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size();
        stubRemoteFhirSearch(remoteTasksBundle(REFERRAL_1_FHIR_UUID.toString()));
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString()))
                .thenReturn(Optional.of(localTaskWithStatus(TaskStatus.ACCEPTED)));

        invokeBeginTaskCheckIfAcceptedPath();

        assertEquals("DRAFT", currentSubcontractStatus("1"));
        assertEquals(historyBefore, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
    }

    @Test
    public void autoReceived_legacyReferralWithoutSubcontract_doesNotBreakPoller() throws Exception {
        // Referral 2 in the fixture has no subcontract row (pre-S-14). The
        // poller normally skips it (status=RECEIVED, not SENT), so flip it to
        // SENT just for this test to exercise the iteration over a legacy row.
        UUID ref2Uuid = UUID.fromString("386e3e0c-013b-425b-ab5f-46f40278c5d3");
        jdbcTemplate.update("UPDATE clinlims.referral SET status = 'SENT' WHERE id = 2");

        stubRemoteFhirSearch(remoteTasksBundle(ref2Uuid.toString()));
        when(fhirPersistanceServiceMock.getTaskBasedOnTask(anyString()))
                .thenReturn(Optional.of(localTaskWithStatus(TaskStatus.ACCEPTED)));

        invokeBeginTaskCheckIfAcceptedPath();

        assertEquals(0, statusHistoryDAO.findByReferralIdOrderedByChangedAt("2").size());
        Referral ref2 = referralService.getReferralById("2");
        org.junit.Assert.assertNull(ref2.getSubcontract());
    }

    @Test
    public void transition_innerTxAtomicity_historyAndSubcontractCommitTogether() {
        ReferralStatusHistoryDAO mockDao = mock(ReferralStatusHistoryDAO.class);
        doThrow(new RuntimeException("simulated DAO failure")).when(mockDao).insert(any());
        ReflectionTestUtils.setField(referralService, "statusHistoryDAO", mockDao);

        assertThrows(RuntimeException.class, () -> referralService.dispatchSubcontract("1",
                Timestamp.valueOf("2026-05-15 10:30:00"), "1", "dispatch that fails mid-tx"));

        // History DAO was mocked, so we can only assert on referral_subcontract.
        // The atomicity assertion: subcontract did NOT advance to DISPATCHED.
        assertEquals("DRAFT", currentSubcontractStatus("1"));
    }

    // ---- Iterator contract regression ---------------------------------------

    @Test
    public void getSentReferrals_returnsInitializedScalarFields() {
        List<Referral> sent = referralService.getSentReferrals();
        assertEquals(1, sent.size());
        Referral ref = sent.get(0);
        assertEquals("1", ref.getId());
        assertEquals(REFERRAL_1_FHIR_UUID, ref.getFhirUuid());
    }

    // ---- Helpers ------------------------------------------------------------

    private void invokeBeginTaskCheckIfAcceptedPath() throws Exception {
        ReflectionTestUtils.invokeMethod(fhirApiWorkflowService, "beginTaskCheckIfAcceptedPath", REMOTE_STORE);
    }

    private Bundle remoteTasksBundle(String taskId) {
        return remoteTasksBundle(taskId, TaskStatus.REQUESTED);
    }

    private Bundle remoteTasksBundle(String taskId, TaskStatus status) {
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(status);
        Bundle bundle = new Bundle();
        bundle.addEntry(new BundleEntryComponent().setResource(task));
        return bundle;
    }

    private Task localTaskWithStatus(TaskStatus status) {
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setStatus(status);
        return task;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void stubRemoteFhirSearch(Bundle firstBundle, Bundle... subsequentBundles) {
        IGenericClient mockClient = mock(IGenericClient.class);
        IUntypedQuery untyped = mock(IUntypedQuery.class);
        IQuery query = mock(IQuery.class);

        when(fhirUtil.getFhirClient(anyString())).thenReturn(mockClient);
        when(mockClient.search()).thenReturn(untyped);
        when(untyped.forResource((Class) any())).thenReturn(query);
        when(query.returnBundle(any())).thenReturn(query);
        when(query.include(any())).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        if (subsequentBundles.length == 0) {
            when(query.execute()).thenReturn(firstBundle);
        } else {
            when(query.execute()).thenReturn(firstBundle, subsequentBundles);
        }
    }

    private void insertSentinelHistory(String referralId) {
        ReferralStatusHistory sentinel = new ReferralStatusHistory();
        sentinel.setReferralId(referralId);
        sentinel.setFromStatus(null);
        sentinel.setToStatus(SubcontractStatus.DRAFT);
        sentinel.setChangedByUserId("1");
        sentinel.setChangedAt(DateUtil.getNowAsTimestamp());
        sentinel.setNotes(SENTINEL_NOTE);
        sentinel.setSysUserId("1");
        statusHistoryDAO.insert(sentinel);
    }

    private long countSentinelRows(String referralId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.referral_status_history WHERE referral_id = ? AND notes = ?",
                Integer.class, Integer.valueOf(referralId), SENTINEL_NOTE);
        return count == null ? 0L : count.longValue();
    }

    private String currentSubcontractStatus(String referralId) {
        return jdbcTemplate.queryForObject(
                "SELECT s.subcontract_status FROM clinlims.referral_subcontract s "
                        + "JOIN clinlims.referral r ON r.subcontract_id = s.id WHERE r.id = ?",
                String.class, Integer.valueOf(referralId));
    }
}
