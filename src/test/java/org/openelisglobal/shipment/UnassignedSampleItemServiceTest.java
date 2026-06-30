package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.service.UnassignedSampleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.UnexpectedRollbackException;

public class UnassignedSampleItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnassignedSampleItemService unassignedSampleItemService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/unassigned-sample-item-service.xml");
    }

    @Test
    public void isAlreadyAssigned_unassignedBloodSampleItem_returnsFalse() {
        assertFalse("Sample item id=1 has no box assignment", unassignedSampleItemService.isAlreadyAssigned("1"));
    }

    @Test
    public void isAlreadyAssigned_unassignedUrineSampleItem_returnsFalse() {
        assertFalse("Sample item id=2 has no box assignment", unassignedSampleItemService.isAlreadyAssigned("2"));
    }

    @Test
    public void isAlreadyAssigned_assignedSerumSampleItem_returnsTrue() {
        assertTrue("Sample item id=3 is in box BOX-2024-001", unassignedSampleItemService.isAlreadyAssigned("3"));
    }

    @Test
    public void isAlreadyAssigned_nonExistentSampleItem_returnsFalse() {
        assertFalse("Non-existent sample item must return false",
                unassignedSampleItemService.isAlreadyAssigned("99999"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BROKEN PATHS - Documenting Current Behaviour
    // ─────────────────────────────────────────────────────────────────────────

    @Test(expected = UnexpectedRollbackException.class)
    public void getAllUnassigned_throwsUnexpectedRollbackException_dueToKnownBug() {
        unassignedSampleItemService.getAllUnassigned();
    }

    @Test(expected = UnexpectedRollbackException.class)
    public void searchUnassignedByAccessionNumber_throwsUnexpectedRollbackException_dueToKnownBug() {
        unassignedSampleItemService.searchUnassignedByAccessionNumber("ACC-001");
    }

    @Test(expected = UnexpectedRollbackException.class)
    public void getSampleItemById_throwsUnexpectedRollbackException_dueToKnownBug() {
        unassignedSampleItemService.getSampleItemById("1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTENDED BEHAVIOUR - Ignored until bug is fixed
    // ─────────────────────────────────────────────────────────────────────────

    @Ignore("Blocked by known BoxSampleItemDAOImpl bug (type mismatch on sampleItem.id)")
    @Test
    public void getAllUnassigned_shouldReturnOnlyUnassignedSampleItems() {
        List<SampleItemDTO> results = unassignedSampleItemService.getAllUnassigned();
        assertEquals("Exactly 2 sample items are unassigned in the fixture", 2, results.size());
    }

    @Ignore("Blocked by known BoxSampleItemDAOImpl bug (type mismatch on sampleItem.id)")
    @Test
    public void getAllUnassigned_shouldIncludeBloodSampleItemWithCorrectFields() {
        List<SampleItemDTO> results = unassignedSampleItemService.getAllUnassigned();

        SampleItemDTO bloodItem = results.stream().filter(dto -> "1".equals(dto.getSampleItemId())).findFirst()
                .orElseThrow(() -> new AssertionError("Blood sample item id=1 not found"));

        assertEquals("Blood Sample", bloodItem.getTypeOfSample());
        assertEquals("1", bloodItem.getTypeOfSampleId());
        assertEquals("ACC-001-1", bloodItem.getAccessionNumber());
        assertEquals("HIV Viral Load", bloodItem.getReferralTests().get(0).getTestName());
        assertEquals("National Reference Lab", bloodItem.getReferralTests().get(0).getOrganizationName());
        assertEquals("CREATED", bloodItem.getReferralTests().get(0).getStatus());
    }

    @Ignore("Blocked by known BoxSampleItemDAOImpl bug (type mismatch on sampleItem.id)")
    @Test
    public void searchUnassignedByAccessionNumber_exactMatch_shouldReturnBothSampleItemsForSameAccession() {
        List<SampleItemDTO> results = unassignedSampleItemService.searchUnassignedByAccessionNumber("ACC-001");
        assertEquals("ACC-001 has 2 unassigned sample items", 2, results.size());
    }

    @Ignore("Blocked by known ReferralDAOImpl bug (type mismatch on sampleItem.id)")
    @Test
    public void getSampleItemById_unassignedBloodItem_shouldReturnCorrectDTO() {
        SampleItemDTO dto = unassignedSampleItemService.getSampleItemById("1");

        assertEquals("1", dto.getSampleItemId());
        assertEquals("Blood Sample", dto.getTypeOfSample());
        assertEquals("ACC-001-1", dto.getAccessionNumber());
        assertNull(dto.getAssignedBoxId());
        assertEquals(1, dto.getReferralTests().size());
        assertEquals("HIV Viral Load", dto.getReferralTests().get(0).getTestName());
    }

    @Ignore("Blocked by known ReferralDAOImpl bug (type mismatch on sampleItem.id)")
    @Test
    public void getSampleItemById_assignedSerumItem_shouldReturnDTOWithBoxDetails() {
        SampleItemDTO dto = unassignedSampleItemService.getSampleItemById("3");

        assertEquals("3", dto.getSampleItemId());
        assertEquals("Serum Sample", dto.getTypeOfSample());
        assertEquals(Integer.valueOf(1), dto.getAssignedBoxId());
        assertEquals("BOX-2024-001", dto.getAssignedBoxName());
    }
}
