package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class BiorepositoryQcRoundGenerationServiceTest {

    private BiorepositoryQcRoundGenerationServiceImpl service;

    @Before
    public void setUp() {
        service = new BiorepositoryQcRoundGenerationServiceImpl();
    }

    @Test
    public void generateRound_selectsUniqueBioSampleIds() {
        Map<String, Object> overview = buildOverviewWithSamples(12, 2);
        Map<String, Object> result = service.generateRound(overview, 2, 3, 42L, "Freezer-A", false);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) result.get("samples");
        assertNotNull(samples);
        assertTrue(samples.size() <= 12);
        Set<Integer> ids = new HashSet<>();
        for (Map<String, Object> row : samples) {
            Integer id = ((Number) row.get("bioSampleId")).intValue();
            assertTrue("Duplicate bioSampleId in round: " + id, ids.add(id));
        }
    }

    @Test
    public void generateRound_requiresDeviceWhenMultipleDevices() {
        Map<String, Object> overview = buildOverviewWithSamples(6, 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) overview.get("filters");
        filters.put("freezers", List.of("Freezer-A", "Freezer-B"));

        try {
            service.generateRound(overview, 2, 2, null, null, false);
            fail("Expected device selection validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Select a device"));
        }
    }

    @Test
    public void generateRound_rejectsInsufficientSamplePool() {
        Map<String, Object> overview = buildOverviewWithSamples(5, 5);
        try {
            service.generateRound(overview, 2, 10, null, "Freezer-A", false);
            fail("Expected insufficient sample pool validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Not enough eligible samples"));
        }
    }

    @Test
    public void validateRoundParameters_rejectsZeroSamplesPerBox() {
        try {
            BiorepositoryQcRoundGenerationServiceImpl.validateRoundParameters(10, 0);
            fail("Expected validation error");
        } catch (IllegalArgumentException e) {
            assertEquals("samplesPerBox must be between 1 and 200", e.getMessage());
        }
    }

    private Map<String, Object> buildOverviewWithSamples(int sampleCount, int boxes) {
        Map<String, List<Map<String, Object>>> byBox = new HashMap<>();
        for (int i = 0; i < sampleCount; i++) {
            String boxKey = "Freezer-A > Shelf-1 > Rack-1 > Box-" + (i % boxes);
            Map<String, Object> row = new HashMap<>();
            row.put("bioSampleId", 1000 + i);
            row.put("boxKey", boxKey);
            row.put("shelfKey", "Freezer-A > Shelf-1");
            row.put("freezer", "Freezer-A");
            byBox.computeIfAbsent(boxKey, key -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> eligible = new ArrayList<>();
        byBox.values().forEach(eligible::addAll);

        Map<String, Object> counts = new HashMap<>();
        counts.put("boxes", byBox.size());
        counts.put("eligibleSamples", eligible.size());

        Map<String, Object> filters = new HashMap<>();
        filters.put("freezers", List.of("Freezer-A"));

        Map<String, Object> overview = new HashMap<>();
        overview.put("eligibleSamples", eligible);
        overview.put("counts", counts);
        overview.put("filters", filters);
        return overview;
    }
}
