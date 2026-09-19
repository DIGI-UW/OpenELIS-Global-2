package org.openelisglobal.audittrail;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Smoke coverage for an audited non-patient service: NcEvent insert and update
 * each produce a history row. Patient/Person/PatientIdentity are covered
 * separately by {@link PatientAuditTrailIntegrationTest}; NcEvent stands in
 * here for the wider set of audited services so a future regression that
 * affects audit emit in general — rather than the specific patient path — still
 * trips a database integration test.
 */
public class P0AuditEmitSmokeTest extends AuditTrailIntegrationTestSupport {

    @Autowired
    private NCEventService nceEventService;

    @Autowired
    private HistoryService historyService;

    private String ncEventRefTableId;

    @Before
    public void setUp() throws Exception {
        ncEventRefTableId = requiredReferenceTable("nc_event");
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
        detachSavedRecords();
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
