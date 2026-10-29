package org.openelisglobal.sample.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class SampleDAOImplTest extends BaseWebContextSensitiveTest {

    private static final String UNASSIGNED_ACCESSION_NUMBER = "12345";

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    public void getUnassignedSampleByAccessionNumber_shouldReturnEligibleSample() {
        Sample sample = sampleDAO.getUnassignedSampleByAccessionNumber(UNASSIGNED_ACCESSION_NUMBER);

        assertEquals("1", sample.getId());
    }

    @Test
    @Transactional
    public void getUnassignedSampleByAccessionNumber_shouldExcludeCancelledReferralStatuses() {
        Referral referral = referralService.getReferralById("1");

        referral.setStatus(ReferralStatus.CANCELLED);
        referralService.update(referral);
        assertNull(sampleDAO.getUnassignedSampleByAccessionNumber(UNASSIGNED_ACCESSION_NUMBER));

        referral.setStatus(ReferralStatus.CANCELED);
        referralService.update(referral);
        assertNull(sampleDAO.getUnassignedSampleByAccessionNumber(UNASSIGNED_ACCESSION_NUMBER));
    }

    @Test
    @Transactional
    public void getUnassignedSampleByAccessionNumber_shouldExcludeAssignedReferral() {
        Referral referral = referralService.getReferralById("1");
        ShippingBox box = new ShippingBox();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        box.setBoxId("BOX-DAO-TEST-001");
        box.setFhirUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        box.setDestinationFacility(organizationService.get("1"));
        box.setState(BoxState.DRAFT);
        box.setSystemUserId(1);
        box.setCreatedDate(now);
        box.setLastupdated(now);
        shippingBoxDAO.insert(box);

        referral.setAssignedBox(box);
        referralService.update(referral);

        assertNull(sampleDAO.getUnassignedSampleByAccessionNumber(UNASSIGNED_ACCESSION_NUMBER));
    }

    @Test
    public void getUnassignedSampleByAccessionNumber_shouldReturnNullWhenSampleDoesNotExist() {
        assertNull(sampleDAO.getUnassignedSampleByAccessionNumber("missing-accession-number"));
    }
}
