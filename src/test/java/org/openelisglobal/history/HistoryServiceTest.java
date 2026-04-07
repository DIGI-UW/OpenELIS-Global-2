package org.openelisglobal.history;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private HistoryService historyService;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/history.xml");
    }

    @Test
    public void insert_validHistory_shouldInsertRecord() {
        History newHistory = new History();
        newHistory.setSysUserId("1");
        newHistory.setReferenceId("11111");
        newHistory.setReferenceTable("5");
        newHistory.setTimestamp(Timestamp.from(Instant.now()));
        newHistory.setActivity("I");
        newHistory.setChanges("Test insert".getBytes());

        String insertedId = historyService.insert(newHistory);

        Assert.assertNotNull(insertedId);

        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("11111", "5");
        Assert.assertEquals(1, historyList.size());
    }

    @Test
    public void updateHistory_shouldModifyAndReturnUpdatedRecord() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        Assert.assertFalse(historyList.isEmpty());

        History history = historyList.get(0);
        Timestamp newTimestamp = Timestamp.from(Instant.now());
        history.setTimestamp(newTimestamp);

        historyService.update(history);

        List<History> updatedHistoryList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        History updatedHistory = updatedHistoryList.get(0);

        Assert.assertNotNull(updatedHistory);
    }

    @Test
    public void update_validHistory_shouldUpdateRecord() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        Assert.assertFalse(historyList.isEmpty());

        History history = historyList.get(0);
        Timestamp newTimestamp = Timestamp.from(Instant.now());
        history.setTimestamp(newTimestamp);

        History updatedHistory = historyService.update(history);

        Assert.assertNotNull(updatedHistory);
        Assert.assertEquals(newTimestamp, updatedHistory.getTimestamp());
    }

    @Test
    public void update_historyWithNullLastupdated_shouldSetTimestampAndUpdate() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        Assert.assertEquals(2, historyList.size());

        History history = historyList.get(0);
        history.setLastupdated(null);
        historyService.update(history);
    }

    @Test
    public void update_historyWithLastupdated_shouldUpdateNormally() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "2");
        Assert.assertEquals(1, historyList.size());

        History history = historyList.get(0);
        Timestamp timestamp = Timestamp.from(Instant.now());
        history.setLastupdated(timestamp);
        history.setChanges("Updated changes".getBytes());

        // This triggers the non-null lastupdated branch
        historyService.update(history);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteHistory_detachedEntity_shouldThrowException() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        History detachedHistory = historyList.get(0);

        entityManager.clear();
        historyService.delete(detachedHistory);
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_validInputs_shouldReturnRecords() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");

        Assert.assertFalse(historyList.isEmpty());
        Assert.assertEquals(2, historyList.size());
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_differentTableIds_shouldReturnCorrectRecords() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "2");

        Assert.assertFalse(historyList.isEmpty());
        Assert.assertEquals(1, historyList.size());
    }

    @Test
    public void getHistory_differentRefTables_shouldReturnCorrectCounts() {
        List<History> table1 = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        Assert.assertEquals(2, table1.size());

        List<History> table2 = historyService.getHistoryByRefIdAndRefTableId("67890", "2");
        Assert.assertEquals(1, table2.size());
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_shouldReturnSortedResults() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("67890", "1");
        Assert.assertFalse(historyList.isEmpty());

        for (int i = 1; i < historyList.size(); i++) {
            Timestamp previousTimestamp = historyList.get(i - 1).getTimestamp();
            Timestamp currentTimestamp = historyList.get(i).getTimestamp();
            Assert.assertTrue(previousTimestamp.compareTo(currentTimestamp) >= 0);
        }
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_existingData_shouldReturnRecords() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("99999", "99999");

        Assert.assertFalse(historyList.isEmpty());
        Assert.assertEquals(1, historyList.size());
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_noMatchingData_shouldReturnEmptyList() {
        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId("88888", "88888");

        Assert.assertTrue(historyList.isEmpty());
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_withHistoryObject_shouldReturnRecords() {
        History searchHistory = new History();
        searchHistory.setReferenceId("67890");
        searchHistory.setReferenceTable("1");

        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId(searchHistory);

        Assert.assertFalse(historyList.isEmpty());
        Assert.assertEquals(2, historyList.size());
    }

    @Test
    public void getHistoryByRefIdAndRefTableId_withHistoryObject_differentTable_shouldReturnCorrectRecords() {
        History searchHistory = new History();
        searchHistory.setReferenceId("67890");
        searchHistory.setReferenceTable("2");

        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId(searchHistory);

        Assert.assertFalse(historyList.isEmpty());
        Assert.assertEquals(1, historyList.size());
    }

    @Test(expected = NumberFormatException.class)
    public void getHistoryByRefIdAndRefTableId_noRecordsFound() {
        historyService.getHistoryByRefIdAndRefTableId("nonexistent", "nonexistent");
    }

    @Test(expected = NumberFormatException.class)
    public void getHistoryByRefIdAndRefTableId_withHistoryObject_nonNumericIds_shouldThrowException() {
        History searchHistory = new History();
        searchHistory.setReferenceId("notanumber");
        searchHistory.setReferenceTable("1");

        historyService.getHistoryByRefIdAndRefTableId(searchHistory);
    }
}