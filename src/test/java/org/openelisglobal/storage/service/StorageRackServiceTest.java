package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class StorageRackServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageRackService storageRackService;

    @Autowired
    private StorageShelfService storageShelfService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        ensureAuditSystemUser();
        executeDataSetWithStateManagement("testdata/storage-location.xml");
    }

    @Test
    @Transactional
    public void findByParentShelfId_shouldReturnExactRacksAssociatedWithShelf() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        List<StorageRack> racks = storageRackService.findByParentShelfId(shelfA.getId());

        assertEquals("Shelf A must contain exactly 2 racks", 2, racks.size());

        StorageRack rack1 = racks.get(0);
        StorageRack expectedRack1 = storageRackService.findByLabel("Rack R1");
        assertEquals(expectedRack1.getId(), rack1.getId());
        assertEquals("Rack R1", rack1.getLabel());
        assertEquals("TEST-RR1", rack1.getCode());
        assertEquals(shelfA.getId(), rack1.getParentShelf().getId());
        assertTrue("Rack R1 must be active", rack1.getActive());

        StorageRack rack2 = racks.get(1);
        StorageRack expectedRack2 = storageRackService.findByLabel("Rack R2");
        assertEquals(expectedRack2.getId(), rack2.getId());
        assertEquals("Rack R2", rack2.getLabel());
        assertEquals("TEST-RR2", rack2.getCode());
        assertEquals(shelfA.getId(), rack2.getParentShelf().getId());
        assertTrue("Rack R2 must be active", rack2.getActive());
    }

    @Test
    @Transactional
    public void findByParentShelfId_shouldReturnEmptyList_whenShelfHasNoRacks() {
        Integer nonExistentShelfId = 99999;
        List<StorageRack> racks = storageRackService.findByParentShelfId(nonExistentShelfId);

        assertEquals("Racks collection must be empty for non-existent shelf", 0, racks.size());
        assertTrue("Racks list must be empty", racks.isEmpty());
    }

    @Test
    @Transactional
    public void countByShelfId_shouldReturnExactCount() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        StorageShelf shelfB = storageShelfService.findByLabel("Shelf B");

        assertEquals("Shelf A must count 2 racks", 2, storageRackService.countByShelfId(shelfA.getId()));
        assertEquals("Shelf B must count 1 rack", 1, storageRackService.countByShelfId(shelfB.getId()));
        assertEquals("Non-existent shelf must count 0 racks", 0, storageRackService.countByShelfId(99999));
    }

    @Test
    @Transactional
    public void findByLabel_shouldReturnExactMatchingRack() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        StorageRack rack = storageRackService.findByLabel("Rack R1");

        assertEquals("Rack R1", rack.getLabel());
        assertEquals("TEST-RR1", rack.getCode());
        assertEquals(shelfA.getId(), rack.getParentShelf().getId());
        assertTrue("Rack must be active", rack.getActive());
    }

    @Test
    @Transactional
    public void findByLabel_shouldReturnNull_whenLabelDoesNotExist() {
        StorageRack rack = storageRackService.findByLabel("NonExistentLabel");

        assertEquals("Non-existent label must return null", null, rack);
    }

    @Test
    @Transactional
    public void findByLabelAndParentShelf_shouldReturnExactMatchingRack() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        StorageRack rack = storageRackService.findByLabelAndParentShelf("Rack R1", shelfA);

        assertEquals("Rack R1", rack.getLabel());
        assertEquals("TEST-RR1", rack.getCode());
        assertEquals(shelfA.getId(), rack.getParentShelf().getId());
    }

    @Test
    @Transactional
    public void findByLabelAndParentShelf_shouldReturnNull_whenLabelOnDifferentShelf() {
        StorageShelf shelfB = storageShelfService.findByLabel("Shelf B");
        StorageRack rack = storageRackService.findByLabelAndParentShelf("Rack R1", shelfB);

        assertEquals("Rack R1 does not belong to Shelf B and must return null", null, rack);
    }

    @Test
    @Transactional
    public void findByCode_shouldReturnExactMatchingRack() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        StorageRack rack = storageRackService.findByCode("TEST-RR2");

        assertEquals("Rack R2", rack.getLabel());
        assertEquals("TEST-RR2", rack.getCode());
        assertEquals(shelfA.getId(), rack.getParentShelf().getId());
    }

    @Test
    @Transactional
    public void findByCode_shouldReturnNull_whenCodeDoesNotExist() {
        StorageRack rack = storageRackService.findByCode("NON-EXISTENT-CODE");

        assertEquals("Non-existent code must return null", null, rack);
    }

    @Test
    @Transactional
    public void findByLabelAndParentShelfId_shouldReturnSpecificRack() {
        StorageShelf shelfB = storageShelfService.findByLabel("Shelf B");
        StorageRack rack = storageRackService.findByLabelAndParentShelfId("Rack R3", shelfB.getId());

        assertEquals("Rack R3", rack.getLabel());
        assertEquals("TEST-RR3", rack.getCode());
        assertEquals(shelfB.getId(), rack.getParentShelf().getId());
    }

    @Test
    @Transactional
    public void findByLabelAndParentShelfId_shouldReturnNull_whenShelfIdDoesNotMatch() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");
        StorageRack rack = storageRackService.findByLabelAndParentShelfId("Rack R3", shelfA.getId());

        assertEquals("Rack R3 does not belong to Shelf A and must return null", null, rack);
    }

    @Test
    @Transactional
    public void getRack_shouldReturnOptionalWithExactStorageRack() {
        StorageRack expectedRack = storageRackService.findByLabel("Rack R1");
        Optional<StorageRack> rackOpt = storageRackService.getRack(expectedRack.getId());

        assertTrue("Rack R1 must be present in database", rackOpt.isPresent());
        StorageRack rack = rackOpt.orElseThrow();
        assertEquals(expectedRack.getId(), rack.getId());
        assertEquals("Rack R1", rack.getLabel());
        assertEquals("TEST-RR1", rack.getCode());
        assertTrue("Rack must be active", rack.getActive());
    }

    @Test
    @Transactional
    public void getRack_shouldReturnEmptyOptional_whenIdNotFound() {
        Optional<StorageRack> rackOpt = storageRackService.getRack(99999);

        assertTrue("Non-existent rack ID must return empty Optional", rackOpt.isEmpty());
    }

    @Test
    @Transactional
    public void save_shouldPersistNewRackInDatabaseWithExactFields() {
        StorageShelf shelfA = storageShelfService.findByLabel("Shelf A");

        StorageRack newRack = new StorageRack();
        newRack.setLabel("New Test Rack");
        newRack.setCode("TEST-RNEW");
        newRack.setParentShelf(shelfA);
        newRack.setActive(true);
        newRack.setSysUserIdValue(1);

        StorageRack savedRack = storageRackService.save(newRack);

        StorageRack fetchedRack = storageRackService.getRack(savedRack.getId()).orElseThrow();
        assertEquals("New Test Rack", fetchedRack.getLabel());
        assertEquals("TEST-RNEW", fetchedRack.getCode());
        assertEquals(shelfA.getId(), fetchedRack.getParentShelf().getId());
        assertTrue("Newly saved rack must be active", fetchedRack.getActive());
        assertEquals(Integer.valueOf(1), fetchedRack.getSysUserIdValue());
    }

    @Test
    @Transactional
    public void save_shouldUpdateExistingRackInDatabase() {
        StorageRack existingRack = storageRackService.findByLabel("Rack R1");
        Integer rackId = existingRack.getId();
        existingRack.setLabel("Updated Rack R1");
        existingRack.setCode("TEST-UPD1");

        storageRackService.save(existingRack);

        StorageRack fetchedRack = storageRackService.getRack(rackId).orElseThrow();
        assertEquals(rackId, fetchedRack.getId());
        assertEquals("Updated Rack R1", fetchedRack.getLabel());
        assertEquals("TEST-UPD1", fetchedRack.getCode());
        assertTrue("Updated rack must remain active", fetchedRack.getActive());
    }
}
