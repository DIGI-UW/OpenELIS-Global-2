package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.shipment.service.UnassignedSampleService;
import org.springframework.beans.factory.annotation.Autowired;

public class UnassignedSampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnassignedSampleService unassignedSampleService;

    @Test
    public void getUnassignedSamplesForDashboard_shouldReturnUnassignedSamples() {
        List<Map<String, Object>> unassignedSamples = unassignedSampleService.getUnassignedSamplesForDashboard();

        assertNotNull("Unassigned samples list should not be null", unassignedSamples);
        // The list may be empty if no unassigned samples exist in test data
    }

    @Test
    public void getUnassignedSamplesByDestinationFacility_shouldFilterByFacility() {
        // Test with a specific facility ID
        Integer facilityId = 1;
        List<Map<String, Object>> samples = unassignedSampleService
                .getUnassignedSamplesByDestinationFacility(facilityId);

        assertNotNull("Samples list should not be null", samples);

        // Verify all returned samples have the correct destination facility
        for (Map<String, Object> sample : samples) {
            Integer destFacilityId = (Integer) sample.get("destinationFacilityId");
            if (destFacilityId != null) {
                assertEquals("All samples should have the requested facility ID", facilityId, destFacilityId);
            }
        }
    }

    @Test
    public void compileSampleData_shouldIncludeDaysUnassigned() {
        List<Map<String, Object>> unassignedSamples = unassignedSampleService.getUnassignedSamplesForDashboard();

        if (!unassignedSamples.isEmpty()) {
            Map<String, Object> firstSample = unassignedSamples.get(0);

            // Verify essential fields are present
            assertNotNull("Sample should have accession number", firstSample.get("accessionNumber"));
            assertNotNull("Sample should have days unassigned", firstSample.get("daysUnassigned"));

            // Verify daysUnassigned is a non-negative number
            Long daysUnassigned = (Long) firstSample.get("daysUnassigned");
            assertFalse("Days unassigned should be non-negative", daysUnassigned < 0);
        }
    }
}
