package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence + eager-fetch coverage for the S-14 {@link ReferralSubcontract}
 * child entity. Reads go through {@link ReferralService} — subcontract is a
 * cascade-managed child of Referral, so no dedicated service is needed.
 */
public class ReferralSubcontractServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    @Transactional
    public void referralFetch_loadsSubcontractWithAllFieldsPopulated() {
        Referral referral = referralService.getReferralById("1");

        ReferralSubcontract subcontract = referral.getSubcontract();
        assertNotNull(subcontract);
        assertEquals("10", subcontract.getId());
        assertEquals(SubcontractStatus.DRAFT, subcontract.getSubcontractStatus());
        assertEquals("AGR-2026-017", subcontract.getAgreementReference());
        assertEquals(Timestamp.valueOf("2026-04-25 09:30:00"), subcontract.getHandoffDatetime());
        assertEquals(Date.valueOf("2026-05-20"), subcontract.getExpectedReturnDate());
        assertEquals("Jane Doe", subcontract.getCocContactName());
        assertEquals("+1-555-0142", subcontract.getCocContactPhone());
        assertEquals("Initial handoff for Q2 subcontract batch.", subcontract.getSubcontractNotes());
    }

    @Test
    @Transactional
    public void referralWithoutSubcontract_returnsNullFromGetter() {
        // Referral id=2 in the fixture has no subcontract_id; covers the historical
        // pre-S-14 case (existing rows on the table when the schema landed).
        Referral referral = referralService.getReferralById("2");

        assertNull(referral.getSubcontract());
    }
}
