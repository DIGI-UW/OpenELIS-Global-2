package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Control lots are audited, but they are keyed by a UUID while
 * history.reference_id is numeric: writing the history row threw inside the
 * save, so editing or retiring any control lot answered 500. The history row
 * now keeps a non-numeric key in reference_key.
 */
@org.springframework.transaction.annotation.Transactional
public class QCControlLotAuditTrailIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private org.openelisglobal.test.service.TestService tests;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private JdbcTemplate jdbc;
    private String lotId;
    private String refTableId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        ensureAuditSystemUser();
        refTableId = ensureReferenceTable("qc_control_lot");
        lotId = UUID.randomUUID().toString();
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setDescription("Control lot audit " + UUID.randomUUID());
        test.setIsActive("Y");
        test.setGuid(UUID.randomUUID().toString());
        test.setSysUserId(TEST_SYS_USER_ID);
        String testId = tests.insert(test);
        jdbc.update("INSERT INTO clinlims.qc_control_lot (id, fhir_uuid, product_name, lot_number, control_level,"
                + " test_id, calculation_method, manufacturer_mean, manufacturer_std_dev, status, sys_user_id,"
                + " last_updated) VALUES (?, gen_random_uuid(), 'Chem Control', 'QC-AUDIT-1', 'NORMAL', ?::integer,"
                + " 'MANUFACTURER_FIXED', 100, 5, 'ESTABLISHMENT', 1, NOW())", lotId, testId);
    }

    private QCControlLot edit(String productName, String status) {
        QCControlLot lot = controlLotService.get(lotId);
        entityManager.detach(lot);
        lot.setProductName(productName);
        lot.setStatus(status);
        lot.setSysUserId("1");
        QCControlLot updated = controlLotService.update(lot);
        entityManager.flush();
        return updated;
    }

    private List<Map<String, Object>> historyRows() {
        return jdbc.queryForList("SELECT reference_id, reference_key, activity, changes FROM clinlims.history"
                + " WHERE reference_table = ?::numeric AND reference_key = ?", refTableId, lotId);
    }

    @Test
    public void editingAControlLotSavesAndIsAudited() {
        edit("Chem Control (Analyzer A)", "ESTABLISHMENT");

        assertEquals("Chem Control (Analyzer A)", jdbc
                .queryForObject("SELECT product_name FROM clinlims.qc_control_lot WHERE id = ?", String.class, lotId));
        List<Map<String, Object>> rows = historyRows();
        assertEquals("one update row keyed by the lot's id", 1, rows.size());
        assertEquals("U", rows.get(0).get("activity"));
        assertNull("a UUID key is not written to the numeric column", rows.get(0).get("reference_id"));
        assertTrue("the old value is recorded",
                new String((byte[]) rows.get(0).get("changes")).contains("Chem Control"));
    }

    @Test
    public void retiringAControlLotSavesAndIsAudited() {
        edit("Chem Control", "EXPIRED");

        assertEquals("EXPIRED",
                jdbc.queryForObject("SELECT status FROM clinlims.qc_control_lot WHERE id = ?", String.class, lotId));
        assertEquals(1, historyRows().size());
    }
}
