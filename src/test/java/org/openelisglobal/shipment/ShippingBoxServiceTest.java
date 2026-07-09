package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ShippingBoxServiceTest extends BaseWebContextSensitiveTest {

    private static final String SENDER = "42";

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private OrganizationService organizationService;

    @Before
    public void init() throws Exception {
        // referral 1 = DRAFT (+subcontract id 10); referral 2 = REQUESTED (no
        // subcontract). organization id 1 = African Health Org.
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    @Transactional
    public void sendBox_dispatchesDraftReferralsAndLeavesNonDraftUntouched() {
        Organization org = organizationService.get("1");

        ShippingBox box = new ShippingBox();
        box.setBoxId("BOX-SEND-0001");
        box.setFhirUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        box.setDestinationFacility(org);
        box.setState(BoxState.READY_TO_SEND);
        box.setSystemUserId(1);
        box.setArchived(false);
        Timestamp createdNow = new Timestamp(System.currentTimeMillis());
        box.setCreatedDate(createdNow);
        box.setLastupdated(createdNow);
        shippingBoxDAO.insert(box);
        Integer boxId = box.getId();

        // Both referrals ride in the same box; only the DRAFT one should dispatch.
        assignToBox("1", box);
        assignToBox("2", box);

        Timestamp beforeSend = new Timestamp(System.currentTimeMillis());
        ShippingBox sent = shippingBoxService.changeBoxState(boxId, BoxState.SENT, Integer.valueOf(SENDER));

        assertEquals(BoxState.SENT, sent.getState());

        // DRAFT referral dispatched to REQUESTED with handoff stamped at send time.
        Referral dispatched = referralService.getReferralById("1");
        assertEquals(ReferralStatus.REQUESTED, dispatched.getStatus());
        assertNotNull(dispatched.getSubcontract());
        assertNotNull(dispatched.getSubcontract().getHandoffDatetime());
        assertTrue("handoff must be stamped at send time, not the stale fixture value",
                !dispatched.getSubcontract().getHandoffDatetime().before(beforeSend));

        // Dispatch writes the DRAFT -> REQUESTED audit row attributed to the sender.
        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        ReferralStatusHistory latest = history.get(history.size() - 1);
        assertEquals(ReferralStatus.DRAFT, latest.getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, latest.getToStatus());
        assertEquals(SENDER, latest.getChangedByUserId());

        // Non-DRAFT referral in the same box is skipped — the guard never throws and
        // no spurious transition is recorded.
        Referral untouched = referralService.getReferralById("2");
        assertEquals(ReferralStatus.REQUESTED, untouched.getStatus());
        assertEquals(0, referralService.getSubcontractStatusHistory("2").size());
    }

    private void assignToBox(String referralId, ShippingBox box) {
        Referral referral = referralService.getReferralById(referralId);
        referral.setAssignedBox(box);
        referral.setSysUserId("1");
        referralService.update(referral);
    }

    // ---- OGC-807: Box.Reconciled gate ----------------------------------------

    private ShippingBox insertReceivedBox(String boxNumber, String uuid) {
        Organization org = organizationService.get("1");
        ShippingBox box = new ShippingBox();
        box.setBoxId(boxNumber);
        box.setFhirUuid(UUID.fromString(uuid));
        box.setDestinationFacility(org);
        box.setState(BoxState.RECEIVED);
        box.setSystemUserId(1);
        box.setArchived(false);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        box.setCreatedDate(now);
        box.setLastupdated(now);
        shippingBoxDAO.insert(box);
        return box;
    }

    private void setReferralStatus(String referralId, ReferralStatus status) {
        Referral referral = referralService.getReferralById(referralId);
        referral.setStatus(status);
        referral.setSysUserId("1");
        referralService.update(referral);
    }

    @Test
    @Transactional
    public void reconcile_blockedWhenAnyReferralNonTerminal() {
        ShippingBox box = insertReceivedBox("BOX-RECON-0001", "33333333-3333-3333-3333-333333333333");
        // referral 1 = DRAFT (non-terminal), referral 2 = REQUESTED (non-terminal).
        assignToBox("1", box);
        assignToBox("2", box);

        try {
            shippingBoxService.changeBoxState(box.getId(), BoxState.RECONCILED, Integer.valueOf(SENDER));
            org.junit.Assert.fail("expected IllegalStateException — box has non-terminal referrals");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertEquals(2L, referralService.countReferralsBlockingReconcile(box.getId()));
        assertEquals(BoxState.RECEIVED, shippingBoxDAO.get(box.getId()).orElseThrow().getState());
    }

    @Test
    @Transactional
    public void reconcile_proceedsWhenAllReferralsTerminal() {
        ShippingBox box = insertReceivedBox("BOX-RECON-0002", "44444444-4444-4444-4444-444444444444");
        assignToBox("1", box);
        assignToBox("2", box);
        setReferralStatus("1", ReferralStatus.COMPLETED);
        setReferralStatus("2", ReferralStatus.REJECTED);

        assertEquals(0L, referralService.countReferralsBlockingReconcile(box.getId()));

        ShippingBox reconciled = shippingBoxService.changeBoxState(box.getId(), BoxState.RECONCILED,
                Integer.valueOf(SENDER));
        assertEquals(BoxState.RECONCILED, reconciled.getState());
    }

    @Test
    @Transactional
    public void reconcile_lostReferralDoesNotBlock() {
        ShippingBox box = insertReceivedBox("BOX-RECON-0003", "55555555-5555-5555-5555-555555555555");
        // A non-terminal (DRAFT) referral that was marked lost must not gate the box.
        Referral lost = referralService.getReferralById("1");
        lost.setAssignedBox(box);
        lost.setLostStatus(Boolean.TRUE);
        lost.setSysUserId("1");
        referralService.update(lost);

        assertEquals(0L, referralService.countReferralsBlockingReconcile(box.getId()));

        ShippingBox reconciled = shippingBoxService.changeBoxState(box.getId(), BoxState.RECONCILED,
                Integer.valueOf(SENDER));
        assertEquals(BoxState.RECONCILED, reconciled.getState());
    }
}
