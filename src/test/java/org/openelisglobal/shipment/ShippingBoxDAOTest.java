package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ShippingBoxDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Before
    public void init() throws Exception {
        // provides organization id "1" (African Health Org)
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    @Transactional
    public void findByDestinationFacilityId_shouldReturnBoxesForThatOrganization() {
        Organization org = organizationService.get("1");

        ShippingBox box = new ShippingBox();
        box.setBoxId("BOX-TEST-0001");
        box.setFhirUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        box.setDestinationFacility(org);
        box.setState(BoxState.DRAFT);
        box.setSystemUserId(1);
        box.setArchived(false);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        box.setCreatedDate(now);
        box.setLastupdated(now);
        shippingBoxDAO.insert(box);

        // Organization.id is a String; the facility id arrives as an Integer from the
        // controller. This call previously threw a parameter type-mismatch exception.
        List<ShippingBox> boxes = shippingBoxDAO.findByDestinationFacilityId(1);

        assertEquals(1, boxes.size());
        assertEquals("BOX-TEST-0001", boxes.get(0).getBoxId());
        assertEquals(BoxState.DRAFT, boxes.get(0).getState());
        assertEquals("1", boxes.get(0).getDestinationFacility().getId());
    }

    @Test
    @Transactional
    public void findByDestinationFacilityId_shouldReturnEmptyForFacilityWithNoBoxes() {
        List<ShippingBox> boxes = shippingBoxDAO.findByDestinationFacilityId(2);

        assertEquals(0, boxes.size());
    }
}
