package org.openelisglobal.qachecklist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleQaChecklistServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleQaChecklistService sampleQaChecklistService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-qa-checklist.xml");
    }

    @Test
    public void findBySampleId_withExistingIntegerId_shouldReturnChecklist() {
        SampleQaChecklist checklist = sampleQaChecklistService.findBySampleId(100);

        assertEquals("SampleId should match 100", Integer.valueOf(100), checklist.getSampleId());
        assertTrue("All required items should be marked verified", checklist.getAllRequiredVerified());

        assertEquals("User ID should match exact DB state", Integer.valueOf(1), checklist.getVerifiedByUserId());
        assertEquals("Exact Timestamp should match DB", "2024-01-15 08:30:00.0",
                checklist.getVerifiedDate().toString());

        Map<String, Boolean> items = checklist.getVerifiedItems();
        assertEquals("Should have exactly 2 verified item entries", 2, items.size());
        assertTrue("Temperature Verified should be mapped to true", items.get("Temperature Verified"));
        assertTrue("Volume Sufficient should be mapped to true", items.get("Volume Sufficient"));
    }

    @Test
    public void findBySampleId_withExistingStringId_shouldReturnChecklist() {
        SampleQaChecklist checklist = sampleQaChecklistService.findBySampleId("101");

        assertEquals("SampleId should match 101", Integer.valueOf(101), checklist.getSampleId());
        assertFalse("All required items should NOT be verified", checklist.getAllRequiredVerified());

        Map<String, Boolean> items = checklist.getVerifiedItems();
        assertTrue("Temperature Verified is true", items.get("Temperature Verified"));
        assertFalse("Volume Sufficient is false", items.get("Volume Sufficient"));
    }

    @Test
    public void getActiveChecklistItems_shouldReturnOnlyActiveItemsSorted() {
        List<Dictionary> activeItems = sampleQaChecklistService.getActiveChecklistItems();

        assertEquals("Should only return exactly 2 active items", 2, activeItems.size());

        assertEquals("First item should be 'Temperature Verified' (order 1)", "Temperature Verified",
                activeItems.get(0).getDictEntry());
        assertEquals("Second item should be 'Volume Sufficient' (order 3)", "Volume Sufficient",
                activeItems.get(1).getDictEntry());
    }

    @Test
    public void saveOrUpdateChecklist_withNewChecklist_shouldSaveAndReturn() {
        Map<String, Boolean> verificationMap = new HashMap<>();
        verificationMap.put("Temperature Verified", true);

        sampleQaChecklistService.saveOrUpdateChecklist(102, verificationMap, 1);

        SampleQaChecklist freshChecklist = sampleQaChecklistService.findBySampleId(102);

        assertEquals("Should link to sample 102", Integer.valueOf(102), freshChecklist.getSampleId());
        assertEquals("User ID should be persisted as 1", Integer.valueOf(1), freshChecklist.getVerifiedByUserId());
        assertFalse("allRequiredVerified should be false because 'Volume Sufficient' is missing",
                freshChecklist.getAllRequiredVerified());

        assertTrue("Temperature Verified is mapped to true",
                freshChecklist.getVerifiedItems().get("Temperature Verified"));
    }

    @Test
    public void saveOrUpdateChecklist_withAllItemsVerified_shouldSetVerifiedDateAndAllRequiredVerified() {
        Map<String, Boolean> verificationMap = new HashMap<>();
        verificationMap.put("Temperature Verified", true);
        verificationMap.put("Volume Sufficient", true);

        sampleQaChecklistService.saveOrUpdateChecklist(101, verificationMap, 1);

        SampleQaChecklist freshChecklist = sampleQaChecklistService.findBySampleId(101);

        assertTrue("allRequiredVerified must be true when all active items are verified",
                freshChecklist.getAllRequiredVerified());
        assertEquals("Must capture the user ID of the verifier", Integer.valueOf(1),
                freshChecklist.getVerifiedByUserId());

        assertTrue("verifiedDate must be generated dynamically when checklist completes",
                freshChecklist.getVerifiedDate() != null);
    }

    @Test
    public void areAllItemsVerified_withAllItemsVerified_shouldReturnTrue() {
        boolean result = sampleQaChecklistService.areAllItemsVerified(100);
        assertTrue("Expected areAllItemsVerified to evaluate to true", result);
    }

    @Test
    public void areAllItemsVerified_withMissingItems_shouldReturnFalse() {
        boolean result = sampleQaChecklistService.areAllItemsVerified(101);
        assertFalse("Expected areAllItemsVerified to evaluate to false", result);

        boolean resultNoChecklist = sampleQaChecklistService.areAllItemsVerified(102);
        assertFalse("Expected areAllItemsVerified to evaluate to false for missing checklist", resultNoChecklist);
    }
}
