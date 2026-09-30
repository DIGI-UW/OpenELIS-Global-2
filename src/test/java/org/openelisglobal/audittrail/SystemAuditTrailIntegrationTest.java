package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.history.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemAuditTrailIntegrationTest extends AuditTrailIntegrationTestSupport {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private HistoryService historyService;

    private String dictionaryRefTableId;
    private DictionaryCategory category;
    private String originalDictionaryId;

    @Before
    public void setUp() throws Exception {
        dictionaryRefTableId = requiredReferenceTable("DICTIONARY");
        category = new DictionaryCategory();
        category.setCategoryName("Audit test category");
        category.setDescription("Owned by the current test transaction");
        category.setLocalAbbreviation("AUDIT");
        category.setSysUserId(TEST_SYS_USER_ID);
        dictionaryCategoryService.insert(category);
        Dictionary original = new Dictionary();
        original.setDictionaryCategory(category);
        original.setDictEntry("Original audit entry");
        original.setIsActive("Y");
        original.setSysUserId(TEST_SYS_USER_ID);
        originalDictionaryId = dictionaryService.insert(original);
        detachSavedRecords();
    }

    @Test
    public void testInsert_shouldCreateHistoryRecord() {
        Dictionary dict = new Dictionary();
        dict.setSortOrder(10);
        dict.setDictionaryCategory(category);
        dict.setDictEntry("Audit Test Entry");
        dict.setIsActive("Y");
        dict.setLocalAbbreviation("ATE");
        dict.setSysUserId("1");

        String id = dictionaryService.insert(dict);
        assertNotNull("Insert should return an ID", id);

        List<History> historyRecords = historyService.getHistoryByRefIdAndRefTableId(id, dictionaryRefTableId);
        assertEquals("Exactly one history record should exist for insert", 1, historyRecords.size());

        History record = historyRecords.get(0);
        assertEquals("I", record.getActivity());
        assertEquals(id, record.getReferenceId());
        assertEquals(dictionaryRefTableId, record.getReferenceTable());
        assertEquals("1", record.getSysUserId());
    }

    @Test
    public void testUpdate_shouldCreateHistoryRecordWithChanges() {
        Dictionary dict = dictionaryService.get(originalDictionaryId);
        detachSavedRecords();
        String originalEntry = dict.getDictEntry();
        dict.setDictEntry("Updated Audit Entry");
        dict.setSysUserId("1");

        dictionaryService.update(dict);

        List<History> historyRecords = historyService.getHistoryByRefIdAndRefTableId(originalDictionaryId,
                dictionaryRefTableId);
        boolean foundUpdate = false;
        for (History h : historyRecords) {
            if ("U".equals(h.getActivity())) {
                foundUpdate = true;
                assertEquals("1", h.getSysUserId());
                assertNotNull("Changes should be recorded for update", h.getChanges());
                assertTrue("Changes should contain original value", new String(h.getChanges()).contains(originalEntry));
                break;
            }
        }
        assertTrue("An update history record should exist", foundUpdate);
    }

    @Test
    public void testDelete_shouldCreateHistoryRecord() {
        Dictionary dict = dictionaryService.get(originalDictionaryId);
        dict.setSysUserId("1");

        dictionaryService.delete(dict);

        List<History> historyRecords = historyService.getHistoryByRefIdAndRefTableId(originalDictionaryId,
                dictionaryRefTableId);
        boolean foundDelete = false;
        for (History h : historyRecords) {
            if ("D".equals(h.getActivity())) {
                foundDelete = true;
                assertEquals("1", h.getSysUserId());
                break;
            }
        }
        assertTrue("A delete history record should exist", foundDelete);
    }

    @Test
    public void testSystemEventsQuery_shouldFilterByEntityType() {
        Dictionary dict = new Dictionary();
        dict.setSortOrder(20);
        dict.setDictionaryCategory(category);
        dict.setDictEntry("Filter Test Entry");
        dict.setIsActive("Y");
        dict.setLocalAbbreviation("FTE");
        dict.setSysUserId("1");
        dictionaryService.insert(dict);

        List<String> dictTableIds = List.of(dictionaryRefTableId);
        List<History> results = historyService.getSystemEventHistory(null, null, null, dictTableIds, null, null, null,
                1, 100);
        long count = historyService.getSystemEventHistoryCount(null, null, null, dictTableIds, null, null, null);

        assertEquals("Count should match results size", count, results.size());

        for (History h : results) {
            assertEquals("All results should be for DICTIONARY table", dictionaryRefTableId, h.getReferenceTable());
        }
    }

    @Test
    public void testSystemEventsQuery_shouldFilterBySysUserId() {
        Dictionary dict = new Dictionary();
        dict.setSortOrder(25);
        dict.setDictionaryCategory(category);
        dict.setDictEntry("User Filter Entry");
        dict.setIsActive("Y");
        dict.setLocalAbbreviation("UFE");
        dict.setSysUserId("1");
        dictionaryService.insert(dict);

        List<String> dictTableIds = List.of(dictionaryRefTableId);

        List<History> matchingResults = historyService.getSystemEventHistory(null, null, "1", dictTableIds, null, null,
                null, 1, 100);
        assertTrue("Should have at least 1 result for user 1", matchingResults.size() >= 1);
        for (History h : matchingResults) {
            assertEquals("All results should be for user 1", "1", h.getSysUserId());
        }

        long matchingCount = historyService.getSystemEventHistoryCount(null, null, "1", dictTableIds, null, null, null);
        assertEquals("Count should match results size", matchingCount, matchingResults.size());

        List<History> nonMatchingResults = historyService.getSystemEventHistory(null, null, "9999", dictTableIds, null,
                null, null, 1, 100);
        assertEquals("Should have 0 results for non-existent user", 0, nonMatchingResults.size());
    }

    @Test
    public void testSystemEventsQuery_shouldPaginate() {
        for (int i = 0; i < 5; i++) {
            Dictionary dict = new Dictionary();
            dict.setSortOrder(30 + i);
            dict.setDictionaryCategory(category);
            dict.setDictEntry("Page Test " + i);
            dict.setIsActive("Y");
            dict.setLocalAbbreviation("PT" + i);
            dict.setSysUserId("1");
            dictionaryService.insert(dict);
        }

        List<String> dictTableIds = List.of(dictionaryRefTableId);
        long totalCount = historyService.getSystemEventHistoryCount(null, null, null, dictTableIds, "I", null, null);
        assertTrue("Should have at least 5 insert records", totalCount >= 5);

        List<History> page1 = historyService.getSystemEventHistory(null, null, null, dictTableIds, "I", null, null, 1,
                2);
        assertEquals("Page 1 should have 2 results", 2, page1.size());

        List<History> page2 = historyService.getSystemEventHistory(null, null, null, dictTableIds, "I", null, null, 2,
                2);
        assertEquals("Page 2 should have 2 results", 2, page2.size());

        assertTrue("Pages should have different records", !page1.get(0).getId().equals(page2.get(0).getId()));
    }
}
