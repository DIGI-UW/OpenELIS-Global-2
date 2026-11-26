package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerDeliveryActionDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The attribution is only useful if it is durable and reportable, so this
 * exercises the real service against Postgres. Without the reference_tables
 * registration in the changeset, saveNewHistory throws and the action insert
 * rolls back.
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
    private AnalyzerDAO analyzerDAO;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

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
        List<AnalyzerDeliveryAction> stored = stored();
        assertEquals(1, stored.size());
        AnalyzerDeliveryAction action = stored.get(0);
        assertEquals(outboxEntryId, action.getOutboxEntryId());
        assertEquals(AnalyzerDeliveryActionService.RETRY, action.getAction());
        assertEquals(TEST_SYS_USER_ID, action.getActor());
        assertNotNull("The action time must be recorded", action.getActedAt());
        assertNull("An unregistered sender matches no analyzer", action.getAnalyzerId());
    }

    @Test
    @Transactional
    public void attributesTheActingUserOnARegisteredAnalyzer() {
        ensureAuditSystemUser();
        outboxEntryId = "integration-" + UUID.randomUUID();
        String actor = anotherSystemUser();
        assertNotEquals(TEST_SYS_USER_ID, actor);
        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName("Delivery action " + outboxEntryId);
        analyzer.setActive(false);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setSysUserId(TEST_SYS_USER_ID);
        analyzerDAO.insert(analyzer);
        ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(AUDIT_TABLE);

        AnalyzerDeliveryAction retained = deliveryActionService.retain(outboxEntryId,
                AnalyzerDeliveryActionService.RETRY, analyzer.getId(), actor);
        entityManager.flush();
        entityManager.clear();

        List<AnalyzerDeliveryAction> stored = stored();
        assertEquals(1, stored.size());
        assertEquals(actor, stored.get(0).getActor());
        assertEquals(analyzer.getId(), stored.get(0).getAnalyzerId());
        List<History> history = historyService.getHistoryByRefIdAndRefTableId(retained.getId(), referenceTable.getId());
        assertEquals(1, history.size());
        assertEquals("The history row names the acting user", actor, history.get(0).getSysUserId());
    }

    @Test
    public void writesAnAuditHistoryRowSoTheActionIsReportable() {
        outboxEntryId = "integration-" + UUID.randomUUID();
        ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(AUDIT_TABLE);
        assertNotNull("The changeset must register " + AUDIT_TABLE + " in reference_tables", referenceTable);
        assertEquals("History is only kept when keep_history is Y", "Y", referenceTable.getKeepHistory());

        AnalyzerDeliveryAction retained = deliveryActionService.retain(outboxEntryId,
                AnalyzerDeliveryActionService.DISMISS, null, TEST_SYS_USER_ID);

        assertNotNull("The retained action must carry an id to be referenced by", retained.getId());
        assertFalse("The action must appear in the audit history",
                historyService.getHistoryByRefIdAndRefTableId(retained.getId(), referenceTable.getId()).isEmpty());
    }

    @Test
    public void keepsEveryActionOnTheSameEntry() {
        outboxEntryId = "integration-" + UUID.randomUUID();

        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.RETRY, null, TEST_SYS_USER_ID);
        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.DISMISS, null, TEST_SYS_USER_ID);

        List<AnalyzerDeliveryAction> stored = stored();
        assertEquals("Each action is retained, not overwritten", 2, stored.size());
        assertEquals(List.of(AnalyzerDeliveryActionService.DISMISS, AnalyzerDeliveryActionService.RETRY),
                stored.stream().map(AnalyzerDeliveryAction::getAction).sorted().toList());
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

    private List<AnalyzerDeliveryAction> stored() {
        return new TransactionTemplate(transactionManager)
                .execute(status -> actionDAO.findByOutboxEntryId(outboxEntryId));
    }

    private String anotherSystemUser() {
        return new JdbcTemplate(dataSource).queryForObject(
                "INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, is_active, is_employee,"
                        + " lastupdated) VALUES (nextval('clinlims.system_user_seq'), ?, 'Delivery', 'Actor', 'Y',"
                        + " 'Y', now()) RETURNING id",
                Long.class, "delivery-" + UUID.randomUUID().toString().substring(0, 8)).toString();
    }
}
