package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class SystemAuditTrailIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private String dictionaryRefTableId;

    @Before
    public void setUp() throws Exception {
        // Replace the mocked AuditTrailService with a real one for this test
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        Object target = AopTestUtils.getUltimateTargetObject(dictionaryService);
        ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrailService);

        executeDataSetWithStateManagement("testdata/dictionary.xml");
        ReferenceTables rt = referenceTablesService.getReferenceTableByName("DICTIONARY");
        assertNotNull("DICTIONARY must be in reference_tables", rt);
        dictionaryRefTableId = rt.getId();
    }

    @Test
    public void testInsert_shouldCreateHistoryRecord() {
        Dictionary dict = new Dictionary();
        dict.setSortOrder(10);
        dict.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
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
        Dictionary dict = dictionaryService.get("1");
        String originalEntry = dict.getDictEntry();
        dict.setDictEntry("Updated Audit Entry");
        dict.setSysUserId("1");

        dictionaryService.update(dict);

        List<History> historyRecords = historyService.getHistoryByRefIdAndRefTableId("1", dictionaryRefTableId);
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
        Dictionary dict = dictionaryService.get("1");
        dict.setSysUserId("1");

        dictionaryService.delete(dict);

        List<History> historyRecords = historyService.getHistoryByRefIdAndRefTableId("1", dictionaryRefTableId);
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
        dict.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
        dict.setDictEntry("Filter Test Entry");
        dict.setIsActive("Y");
        dict.setLocalAbbreviation("FTE");
        dict.setSysUserId("1");
        dictionaryService.insert(dict);

        List<String> dictTableIds = List.of(dictionaryRefTableId);
        List<History> results = historyService.getSystemEventHistory(null, null, null, dictTableIds, null, null, 1,
                100);
        long count = historyService.getSystemEventHistoryCount(null, null, null, dictTableIds, null, null);

        assertEquals("Count should match results size", count, results.size());

        for (History h : results) {
            assertEquals("All results should be for DICTIONARY table", dictionaryRefTableId, h.getReferenceTable());
        }
    }

    @Test
    public void testSystemEventsQuery_shouldPaginate() {
        for (int i = 0; i < 5; i++) {
            Dictionary dict = new Dictionary();
            dict.setSortOrder(30 + i);
            dict.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
            dict.setDictEntry("Page Test " + i);
            dict.setIsActive("Y");
            dict.setLocalAbbreviation("PT" + i);
            dict.setSysUserId("1");
            dictionaryService.insert(dict);
        }

        List<String> dictTableIds = List.of(dictionaryRefTableId);
        long totalCount = historyService.getSystemEventHistoryCount(null, null, null, dictTableIds, "I", null);
        assertTrue("Should have at least 5 insert records", totalCount >= 5);

        List<History> page1 = historyService.getSystemEventHistory(null, null, null, dictTableIds, "I", null, 1, 2);
        assertEquals("Page 1 should have 2 results", 2, page1.size());

        List<History> page2 = historyService.getSystemEventHistory(null, null, null, dictTableIds, "I", null, 2, 2);
        assertEquals("Page 2 should have 2 results", 2, page2.size());

        assertTrue("Pages should have different records", !page1.get(0).getId().equals(page2.get(0).getId()));
    }
}
