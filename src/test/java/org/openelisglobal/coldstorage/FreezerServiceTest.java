package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.springframework.beans.factory.annotation.Autowired;

public class FreezerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    FreezerService freezerService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer.xml");
    }

    @Test
    public void verifyTestData() {
        List<Freezer> freezers = freezerService.getActiveFreezers();

        assertNotNull("Freezer list should not be null", freezers);
        assertFalse("Freezer list should not be empty", freezers.isEmpty());

        freezers.forEach(freezer -> {
            assertNotNull("Freezer ID should not be null", freezer.getId());
            assertNotNull("Freezer name should not be null", freezer.getName());
            assertNotNull("Protocol should not be null", freezer.getProtocol());
            assertTrue("Active freezers should be active", freezer.getActive());
        });
    }

    @Test
    public void getActiveFreezers_shouldReturnOnlyActiveFreezers() {
        List<Freezer> activeFreezers = freezerService.getActiveFreezers();

        assertNotNull("Active freezers list should not be null", activeFreezers);
        assertEquals("Should have 2 active freezers", 2, activeFreezers.size());

        activeFreezers.forEach(freezer -> {
            assertTrue("All returned freezers should be active", freezer.getActive());
        });
    }

    @Test
    public void getAllFreezers_shouldReturnAllFreezersWhenSearchIsEmpty() {
        List<Freezer> allFreezers = freezerService.getAllFreezers("");

        assertNotNull("Freezers list should not be null", allFreezers);
        assertEquals("Should return all 3 freezers", 3, allFreezers.size());
    }

    @Test
    public void getAllFreezers_shouldReturnFilteredFreezersWhenSearchProvided() {
        List<Freezer> filteredFreezers = freezerService.getAllFreezers("Test Freezer 1");

        assertNotNull("Filtered freezers list should not be null", filteredFreezers);
        assertFalse("Should find matching freezers", filteredFreezers.isEmpty());

        filteredFreezers.forEach(freezer -> {
            assertTrue("Freezer name should contain search term", freezer.getName().contains("Test Freezer 1"));
        });
    }

    @Test
    public void findByName_shouldReturnFreezerWhenNameExists() {
        String freezerName = "Test Freezer 1";

        Optional<Freezer> freezerOpt = freezerService.findByName(freezerName);

        assertTrue("Freezer should be found", freezerOpt.isPresent());
        assertEquals("Freezer name should match", freezerName, freezerOpt.get().getName());
    }

    @Test
    public void findByName_shouldReturnEmptyWhenNameDoesNotExist() {
        String freezerName = "Non Existent Freezer";

        Optional<Freezer> freezerOpt = freezerService.findByName(freezerName);

        assertFalse("Freezer should not be found", freezerOpt.isPresent());
    }

    @Test
    public void findById_shouldReturnFreezerWhenIdExists() {
        Long freezerId = 100L;

        Optional<Freezer> freezerOpt = freezerService.findById(freezerId);

        assertTrue("Freezer should be found", freezerOpt.isPresent());
        assertEquals("Freezer ID should match", freezerId, freezerOpt.get().getId());
    }

    @Test
    public void findById_shouldReturnEmptyWhenIdDoesNotExist() {
        Long freezerId = 999L;

        Optional<Freezer> freezerOpt = freezerService.findById(freezerId);

        assertFalse("Freezer should not be found", freezerOpt.isPresent());
    }

    @Test
    public void createFreezer_shouldCreateNewFreezer() {
        Freezer newFreezer = new Freezer();
        newFreezer.setName("New Test Freezer");
        newFreezer.setProtocol(Freezer.Protocol.TCP);
        newFreezer.setHost("192.168.1.200");
        newFreezer.setPort(502);
        newFreezer.setSlaveId(10);
        newFreezer.setTemperatureRegister(0);
        newFreezer.setTemperatureScale(BigDecimal.ONE);
        newFreezer.setTemperatureOffset(new BigDecimal("-80"));
        newFreezer.setHumidityRegister(1);
        newFreezer.setHumidityScale(BigDecimal.ONE);
        newFreezer.setHumidityOffset(BigDecimal.ZERO);

        Freezer createdFreezer = freezerService.createFreezer(newFreezer, 1L, "1");

        assertNotNull("Created freezer should not be null", createdFreezer);
        assertNotNull("Created freezer should have ID", createdFreezer.getId());
        assertEquals("New Test Freezer", createdFreezer.getName());
        assertTrue("New freezer should be active by default", createdFreezer.getActive());
    }

    @Test
    public void updateFreezer_shouldUpdateExistingFreezer() {
        Long freezerId = 100L;
        Freezer existingFreezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", existingFreezer);

        Freezer updatedData = new Freezer();
        updatedData.setName("Updated Freezer Name");
        updatedData.setProtocol(Freezer.Protocol.TCP);
        updatedData.setHost("192.168.1.150");
        updatedData.setPort(502);
        updatedData.setSlaveId(5);
        updatedData.setTemperatureRegister(0);
        updatedData.setTemperatureScale(BigDecimal.ONE);
        updatedData.setTemperatureOffset(new BigDecimal("-80"));
        updatedData.setHumidityRegister(1);
        updatedData.setHumidityScale(BigDecimal.ONE);
        updatedData.setHumidityOffset(BigDecimal.ZERO);

        Freezer updated = freezerService.updateFreezer(freezerId, updatedData, 1L, "1");

        assertNotNull("Updated freezer should not be null", updated);
        assertEquals("Updated Freezer Name", updated.getName());
        assertEquals("192.168.1.150", updated.getHost());
    }

    @Test
    public void setDeviceStatus_shouldActivateFreezer() {
        Long freezerId = 102L; // Inactive freezer
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);
        assertFalse("Freezer should be inactive initially", freezer.getActive());

        freezerService.setDeviceStatus(freezerId, true);

        Freezer updatedFreezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should still exist", updatedFreezer);
        assertTrue("Freezer should now be active", updatedFreezer.getActive());
    }

    @Test
    public void setDeviceStatus_shouldDeactivateFreezer() {
        Long freezerId = 100L; // Active freezer
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);
        assertTrue("Freezer should be active initially", freezer.getActive());

        freezerService.setDeviceStatus(freezerId, false);

        Freezer updatedFreezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should still exist", updatedFreezer);
        assertFalse("Freezer should now be inactive", updatedFreezer.getActive());
    }

    @Test
    public void deleteFreezer_shouldMarkDeletedAndExcludeFromListings() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist before deletion", freezer);
        assertFalse("Freezer should not be deleted initially", Boolean.TRUE.equals(freezer.getDeleted()));

        freezerService.deleteFreezer(freezerId);

        // Soft delete: the row still exists but is flagged deleted (so history is
        // kept).
        Freezer deletedFreezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer row should still exist after soft delete", deletedFreezer);
        assertTrue("Freezer should be flagged deleted", deletedFreezer.getDeleted());

        // #3743: a deleted device must no longer appear in the management list...
        assertTrue("Deleted freezer should not appear in getAllFreezers",
                freezerService.getAllFreezers("").stream().noneMatch(f -> freezerId.equals(f.getId())));
        // ...nor in the active/status listing.
        assertTrue("Deleted freezer should not appear in active list",
                freezerService.getActiveFreezers().stream().noneMatch(f -> freezerId.equals(f.getId())));
    }

    @Test
    public void setDeviceStatus_shouldRejectReactivatingDeletedFreezer() {
        Long freezerId = 100L;
        freezerService.deleteFreezer(freezerId);

        // #3743: pressing the enable/power toggle must not resurrect a deleted device.
        try {
            freezerService.setDeviceStatus(freezerId, true);
            fail("Toggling status on a deleted freezer should throw");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void updateThresholds_shouldUpdateFreezerThresholds() {
        Long freezerId = 100L;
        BigDecimal targetTemp = new BigDecimal("-80.0");
        BigDecimal warningThreshold = new BigDecimal("-75.0");
        BigDecimal criticalThreshold = new BigDecimal("-70.0");
        Integer pollingInterval = 120;

        Freezer updated = freezerService.updateThresholds(freezerId, targetTemp, warningThreshold, criticalThreshold,
                pollingInterval, "1");

        assertNotNull("Updated freezer should not be null", updated);
        assertEquals("Target temperature should match", targetTemp, updated.getTargetTemperature());
        assertEquals("Warning threshold should match", warningThreshold, updated.getWarningThreshold());
        assertEquals("Critical threshold should match", criticalThreshold, updated.getCriticalThreshold());
        assertEquals("Polling interval should match", pollingInterval, updated.getPollingIntervalSeconds());
    }

    @Test
    public void updateThresholds_shouldUpdateOnlyProvidedThresholds() {
        Long freezerId = 100L;
        BigDecimal targetTemp = new BigDecimal("-85.0");

        Freezer updated = freezerService.updateThresholds(freezerId, targetTemp, null, null, null, "1");

        assertNotNull("Updated freezer should not be null", updated);
        assertEquals("Target temperature should be updated", targetTemp, updated.getTargetTemperature());
    }

    @Test
    public void updateThresholds_shouldUpdatePollingIntervalOnly() {
        Long freezerId = 100L;
        Integer pollingInterval = 300;

        Freezer updated = freezerService.updateThresholds(freezerId, null, null, null, pollingInterval, "1");

        assertNotNull("Updated freezer should not be null", updated);
        assertEquals("Polling interval should be updated", pollingInterval, updated.getPollingIntervalSeconds());
    }
}
