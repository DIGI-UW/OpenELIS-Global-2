package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerDeliveryActionDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The attribution is only useful if it is durable and reportable, so this
 * exercises the real service against Postgres. The audit assertion wires a real
 * AuditTrailServiceImpl because AppTestConfig supplies a mock, and without the
 * reference_tables registration in the changeset saveNewHistory resolves no
 * table and records nothing silently.
 */
public class AnalyzerDeliveryActionPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String AUDIT_TABLE = "analyzer_delivery_action";

    @Autowired
    private AnalyzerDeliveryActionService deliveryActionService;

    @Autowired
    private AnalyzerDeliveryActionDAO actionDAO;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String outboxEntryId;

    @After
    public void removeAction() {
        if (outboxEntryId != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> actionDAO.findByOutboxEntryId(outboxEntryId).forEach(action -> actionDAO.delete(action)));
        }
    }

    @Test
    public void retainsTheActorAndTimeForARetry() {
        outboxEntryId = "integration-" + UUID.randomUUID();

        AnalyzerDeliveryAction retained = deliveryActionService.retain(outboxEntryId,
                AnalyzerDeliveryActionService.RETRY, null, TEST_SYS_USER_ID);

        assertNotNull("The action must be persisted", retained.getId());
        List<AnalyzerDeliveryAction> stored = deliveryActionService.findByOutboxEntryId(outboxEntryId);
        assertEquals(1, stored.size());
        AnalyzerDeliveryAction action = stored.get(0);
        assertEquals(outboxEntryId, action.getOutboxEntryId());
        assertEquals(AnalyzerDeliveryActionService.RETRY, action.getAction());
        assertEquals(TEST_SYS_USER_ID, action.getActor());
        assertNotNull("The action time must be recorded", action.getActedAt());
        assertNull("An unregistered sender matches no analyzer", action.getAnalyzerId());
    }

    @Test
    public void writesAnAuditHistoryRowSoTheActionIsReportable() {
        outboxEntryId = "integration-" + UUID.randomUUID();
        ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(AUDIT_TABLE);
        assertNotNull("The changeset must register " + AUDIT_TABLE + " in reference_tables", referenceTable);
        assertEquals("History is only kept when keep_history is Y", "Y", referenceTable.getKeepHistory());

        // AppTestConfig supplies a mock AuditTrailService, so a real one is wired here
        // to prove the history row lands. The manual instance is not a Spring proxy,
        // hence the explicit transaction.
        AuditTrailServiceImpl auditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(auditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(auditTrailService, "historyService", historyService);
        AnalyzerDeliveryActionService auditedService = new AnalyzerDeliveryActionServiceImpl(actionDAO,
                auditTrailService);

        AnalyzerDeliveryAction retained = new TransactionTemplate(transactionManager).execute(status -> auditedService
                .retain(outboxEntryId, AnalyzerDeliveryActionService.DISMISS, null, TEST_SYS_USER_ID));

        assertNotNull("The retained action must carry an id to be referenced by", retained.getId());
        assertFalse("The action must appear in the audit history",
                historyService.getHistoryByRefIdAndRefTableId(retained.getId(), referenceTable.getId()).isEmpty());
    }

    @Test
    public void keepsEveryActionOnTheSameEntry() {
        outboxEntryId = "integration-" + UUID.randomUUID();

        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.RETRY, null, TEST_SYS_USER_ID);
        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.DISMISS, null, "2");

        List<AnalyzerDeliveryAction> stored = deliveryActionService.findByOutboxEntryId(outboxEntryId);
        assertEquals("Each action is retained, not overwritten", 2, stored.size());
        assertEquals(List.of("1", "2"), stored.stream().map(AnalyzerDeliveryAction::getActor).sorted().toList());
    }

    @Test
    public void rejectsAnActionOutsideTheKnownSet() {
        assertThrows(IllegalArgumentException.class, () -> deliveryActionService
                .retain("integration-" + UUID.randomUUID(), "PURGE", null, TEST_SYS_USER_ID));
    }

    @Test
    public void rejectsAMissingActor() {
        assertThrows(IllegalArgumentException.class, () -> deliveryActionService
                .retain("integration-" + UUID.randomUUID(), AnalyzerDeliveryActionService.RETRY, null, " "));
    }
}
