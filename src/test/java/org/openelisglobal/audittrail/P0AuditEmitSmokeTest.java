package org.openelisglobal.audittrail;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Smoke coverage proving the P0 audit opt-ins (the 14 services flipped from
 * auditTrailLog=false to true) actually emit history rows when used. The
 * separate PatientAuditTrailIntegrationTest covers Patient/Person/
 * PatientIdentity directly; this adds a single representative non-patient P0
 * service (NcEvent) to lock that the framework-level fix benefits the other
 * newly-opted-in entities too.
 */
public class P0AuditEmitSmokeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NCEventService nceEventService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private String ncEventRefTableId;

    @Before
    public void setUp() throws Exception {
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        Object target = AopTestUtils.getUltimateTargetObject(nceEventService);
        ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrailService);

        cleanRowsInCurrentConnection(new String[] { "nc_event", "history" });
        ncEventRefTableId = refTableId("nc_event");
    }

    private String refTableId(String name) {
        ReferenceTables rt = referenceTablesService.getReferenceTableByName(name);
        if (rt == null) {
            try (java.sql.Connection conn = dataSource.getConnection();
                    java.sql.PreparedStatement ps = conn
                            .prepareStatement("INSERT INTO clinlims.reference_tables (id, name, keep_history) "
                                    + "VALUES (nextval('clinlims.reference_tables_seq'), ?, 'Y')")) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("Failed to seed reference_tables row for " + name, e);
            }
            rt = referenceTablesService.getReferenceTableByName(name);
            assertNotNull("Re-seed failed for " + name, rt);
        }
        return rt.getId();
    }

    @Test
    public void nceEventInsert_emitsInsertHistoryRow() {
        NcEvent event = new NcEvent();
        event.setName("smoke-event");
        event.setTitle("smoke title");
        event.setDescription("smoke description");
        event.setSysUserId("1");

        nceEventService.insert(event);

        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(event.getId().toString(), ncEventRefTableId);
        boolean foundInsert = false;
        for (History h : rows) {
            if ("I".equals(h.getActivity())) {
                foundInsert = true;
                break;
            }
        }
        assertTrue("Expected one INSERT history row on nc_event after opt-in", foundInsert);
    }

    @Test
    public void nceEventUpdate_emitsUpdateHistoryRowWithOldValue() {
        NcEvent event = new NcEvent();
        event.setName("original-name");
        event.setDescription("original description");
        event.setSysUserId("1");
        nceEventService.insert(event);
        Integer id = event.getId();

        NcEvent reloaded = nceEventService.get(id);
        reloaded.setName("updated-name");
        reloaded.setDescription("updated description");
        reloaded.setSysUserId("1");
        nceEventService.update(reloaded);

        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(id.toString(), ncEventRefTableId);
        boolean foundUpdateWithOld = false;
        for (History h : rows) {
            if (!"U".equals(h.getActivity()) || h.getChanges() == null || h.getChanges().length == 0) {
                continue;
            }
            String xml = new String(h.getChanges());
            if (xml.contains("original-name") || xml.contains("original description")) {
                foundUpdateWithOld = true;
                break;
            }
        }
        assertTrue("Expected UPDATE history row containing old name/description in changes XML", foundUpdateWithOld);
    }
}
