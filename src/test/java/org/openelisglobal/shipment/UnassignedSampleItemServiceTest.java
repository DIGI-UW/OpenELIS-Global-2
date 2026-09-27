package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.service.UnassignedSampleItemService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;

public class UnassignedSampleItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnassignedSampleItemService unassignedSampleItemService;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Before
    public void init() throws Exception {
        // referral 1 -> sample item 1 (accession 12345) -> organization 1
        // referral 2 -> sample item 2 (accession 13333) -> organization 2
        // neither referral is assigned to a box, so both are unassigned
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    private SampleItemDTO findByAccession(List<SampleItemDTO> dtos, String accessionNumber) {
        return dtos.stream().filter(d -> accessionNumber.equals(d.getAccessionNumber())).findFirst().orElse(null);
    }

    @Test
    public void getAllUnassigned_shouldSetDestinationFacilityIdFromLinkedOrganization() {
        List<SampleItemDTO> dtos = unassignedSampleItemService.getAllUnassigned();

        SampleItemDTO item1 = findByAccession(dtos, "12345");
        SampleItemDTO item2 = findByAccession(dtos, "13333");

        assertNotNull("Sample item for accession 12345 should be unassigned", item1);
        assertNotNull("Sample item for accession 13333 should be unassigned", item2);

        // Each sample item must carry the id of its referral's linked organization
        assertEquals("1", item1.getDestinationFacilityId());
        assertEquals("2", item2.getDestinationFacilityId());
    }

    @Test
    public void getSampleItemById_shouldSetDestinationFacilityIdFromLinkedOrganization() {
        SampleItemDTO dto = unassignedSampleItemService.getSampleItemById("1");

        assertNotNull(dto);
        // getSampleItemById always suffixes accession with the sample item sort order
        assertEquals("12345-1", dto.getAccessionNumber());
        assertEquals("1", dto.getDestinationFacilityId());
    }

    @Test
    public void getAllUnassigned_shouldLeaveDestinationFacilityIdNullWhenReferralHasNoOrganization() {
        Referral referral = referralService.getReferralById("2");
        referral.setOrganization(null);
        referral.setSysUserId("1");
        referralService.update(referral);

        List<SampleItemDTO> dtos = unassignedSampleItemService.getAllUnassigned();
        SampleItemDTO item2 = findByAccession(dtos, "13333");

        assertNotNull("Sample item should still be listed when its referral has no organization", item2);
        assertNull("destinationFacilityId must be null when the referral has no linked organization",
                item2.getDestinationFacilityId());
    }

    /**
     * Assigned sample items are excluded with a NOT IN, and a null inside a NOT IN
     * is never true in SQL, so a single box row without a sample item empties this
     * list for the whole site and no sample can be put in a box at all.
     *
     * <p>
     * Whether that row can exist depends on the schema: this table was created with
     * a non-null sample item, and the EQA work drops that constraint so panel
     * material can be box contents, at which point every EQA box row is one of
     * these. So the test asserts nothing where the column still forbids a null, and
     * starts exercising the guard on the schema where it does not.
     */
    @Test
    public void getAllUnassigned_shouldSurviveABoxRowWithNoSampleItem() {
        Assume.assumeTrue("only meaningful where a box row may have no sample item", sampleItemIsNullable());

        Integer boxId = insertBox();
        // Inserted directly: the mapping forbids a null sample item, the column allows
        // one, and rows like this exist in the wild from the box_sample migration.
        jdbcTemplate.update("INSERT INTO clinlims.box_sample_item"
                + " (id, shipping_box_id, sample_item_id, added_date, sys_user_id)"
                + " SELECT COALESCE(MAX(id), 0) + 1, ?, NULL, now(), 1 FROM clinlims.box_sample_item", boxId);

        List<SampleItemDTO> dtos = unassignedSampleItemService.getAllUnassigned();

        assertNotNull("a box row with no sample item must not empty the unassigned list",
                findByAccession(dtos, "12345"));
        assertNotNull(findByAccession(dtos, "13333"));
    }

    private boolean sampleItemIsNullable() {
        String nullable = jdbcTemplate
                .queryForObject(
                        "SELECT is_nullable FROM information_schema.columns WHERE table_schema = 'clinlims'"
                                + " AND table_name = 'box_sample_item' AND column_name = 'sample_item_id'",
                        String.class);
        return "YES".equalsIgnoreCase(nullable);
    }

    private Integer insertBox() {
        ShippingBox box = new ShippingBox();
        box.setBoxId("BOX-NULLROW-0001");
        box.setFhirUuid(UUID.randomUUID());
        box.setDestinationFacility(organizationService.get("1"));
        box.setState(BoxState.DRAFT);
        box.setSystemUserId(1);
        box.setArchived(false);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        box.setCreatedDate(now);
        box.setLastupdated(now);
        shippingBoxDAO.insert(box);
        return box.getId();
    }
}
