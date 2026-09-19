package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns audit test records in a rollback transaction; never truncates or repairs
 * shared seeds.
 */
@Transactional
public abstract class AuditTrailIntegrationTestSupport extends BaseWebContextSensitiveTest {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ReferenceTablesService referenceTablesService;

    private Map<String, Integer> initialCounts;
    private List<Map<String, Object>> initialReferenceTables;

    @BeforeTransaction
    public void captureSharedState() {
        initialCounts = tableCounts();
        initialReferenceTables = referenceRows();
    }

    @AfterTransaction
    public void verifyNoFixtureLeakOrSeedDamage() {
        assertEquals("Audit tests must roll back their rows without deleting shared records", initialCounts,
                tableCounts());
        assertEquals("Audit tests must preserve migration-owned history settings", initialReferenceTables,
                referenceRows());
    }

    private Map<String, Integer> tableCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : List.of("system_user", "site_information", "patient_identity_type", "dictionary_category",
                "dictionary", "person", "patient", "patient_identity", "nc_event", "history")) {
            counts.put(table, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims." + table, Integer.class));
        }
        return counts;
    }

    private List<Map<String, Object>> referenceRows() {
        return jdbcTemplate.queryForList("SELECT id, name, keep_history FROM clinlims.reference_tables ORDER BY id");
    }

    protected String requiredReferenceTable(String name) {
        ReferenceTables table = referenceTablesService.getReferenceTableByName(name);
        assertNotNull("Missing migration seed for " + name, table);
        assertEquals("History must be enabled for " + name, "Y", table.getKeepHistory());
        return table.getId();
    }

    protected void detachSavedRecords() {
        entityManager.flush();
        entityManager.clear();
    }
}
