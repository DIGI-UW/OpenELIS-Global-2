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

    @Test(expected = NumberFormatException.class)
    public void getHistoryByRefIdAndRefTableId_noRecordsFound() {
        historyService.getHistoryByRefIdAndRefTableId("nonexistent", "nonexistent");
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
}
