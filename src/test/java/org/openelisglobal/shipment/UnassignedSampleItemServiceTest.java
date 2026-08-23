package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.service.UnassignedSampleItemService;
import org.springframework.beans.factory.annotation.Autowired;

public class UnassignedSampleItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnassignedSampleItemService unassignedSampleItemService;

    @Autowired
    private ReferralService referralService;

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
}
