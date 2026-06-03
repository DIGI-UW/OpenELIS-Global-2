package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for the S-14 FR-02 audit-trail DAO: round-trip
 * persistence, the chronological ordered finder, and the empty-result branches.
 */
public class ReferralStatusHistoryDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    @Transactional
    public void findByReferralIdOrderedByChangedAt_returnsFixtureRow() {
        List<ReferralStatusHistory> rows = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");

        assertEquals(1, rows.size());
        ReferralStatusHistory only = rows.get(0);
        assertEquals("100", only.getId());
        assertNull(only.getFromStatus());
        assertEquals(SubcontractStatus.DRAFT, only.getToStatus());
        assertEquals("1", only.getChangedByUserId());
        assertEquals(Timestamp.valueOf("2026-04-25 09:00:00"), only.getChangedAt());
        assertEquals("initial creation", only.getNotes());
    }

    @Test
    public void findByReferralIdOrderedByChangedAt_emptyForUnknownReferral() {
        assertTrue(statusHistoryDAO.findByReferralIdOrderedByChangedAt("9999").isEmpty());
    }

    @Test
    public void findByReferralIdOrderedByChangedAt_emptyForBlankInput() {
        assertTrue(statusHistoryDAO.findByReferralIdOrderedByChangedAt(null).isEmpty());
        assertTrue(statusHistoryDAO.findByReferralIdOrderedByChangedAt("").isEmpty());
    }

    @Test
    @Transactional
    public void insertAndFind_returnsRowsInChronologicalOrder() {
        ReferralStatusHistory later = new ReferralStatusHistory();
        later.setReferralId("1");
        later.setFromStatus(SubcontractStatus.DRAFT);
        later.setToStatus(SubcontractStatus.DISPATCHED);
        later.setChangedByUserId("1");
        later.setChangedAt(Timestamp.valueOf("2026-04-25 10:00:00"));
        later.setNotes("dispatched via courier");
        later.setSysUserId("1");
        statusHistoryDAO.insert(later);

        ReferralStatusHistory earlier = new ReferralStatusHistory();
        earlier.setReferralId("1");
        earlier.setFromStatus(null);
        earlier.setToStatus(SubcontractStatus.DRAFT);
        earlier.setChangedByUserId("1");
        earlier.setChangedAt(Timestamp.valueOf("2026-04-25 08:00:00"));
        earlier.setSysUserId("1");
        statusHistoryDAO.insert(earlier);

        List<ReferralStatusHistory> rows = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(3, rows.size());
        // Earlier-inserted-by-time but later-inserted-by-call comes first.
        assertEquals(Timestamp.valueOf("2026-04-25 08:00:00"), rows.get(0).getChangedAt());
        assertNull(rows.get(0).getFromStatus());
        assertEquals(SubcontractStatus.DRAFT, rows.get(0).getToStatus());
        // Fixture row in the middle (09:00).
        assertEquals("100", rows.get(1).getId());
        // The "dispatched" row last (10:00).
        assertEquals(Timestamp.valueOf("2026-04-25 10:00:00"), rows.get(2).getChangedAt());
        assertEquals(SubcontractStatus.DISPATCHED, rows.get(2).getToStatus());
        assertEquals("dispatched via courier", rows.get(2).getNotes());
    }

    @Test
    @Transactional
    public void roundTrip_preservesAllFields() {
        ReferralStatusHistory row = new ReferralStatusHistory();
        row.setReferralId("1");
        row.setFromStatus(SubcontractStatus.RECEIVED);
        row.setToStatus(SubcontractStatus.RESULTS_RETURNED);
        row.setChangedByUserId("42");
        row.setChangedAt(Timestamp.valueOf("2026-05-01 13:45:00"));
        row.setNotes("FHIR result import");
        row.setSysUserId("42");
        statusHistoryDAO.insert(row);

        ReferralStatusHistory fetched = statusHistoryDAO.get(row.getId()).orElseThrow();
        assertEquals(row.getId(), fetched.getId());
        assertEquals("1", fetched.getReferralId());
        assertEquals(SubcontractStatus.RECEIVED, fetched.getFromStatus());
        assertEquals(SubcontractStatus.RESULTS_RETURNED, fetched.getToStatus());
        assertEquals("42", fetched.getChangedByUserId());
        assertEquals(Timestamp.valueOf("2026-05-01 13:45:00"), fetched.getChangedAt());
        assertEquals("FHIR result import", fetched.getNotes());
    }
}
