package org.openelisglobal.storage.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleStorageAssignmentDAOIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private IStatusService statusService;

    private String disposedStatusId;

    @Before
    public void setUpData() throws Exception {
        executeDataSetWithStateManagement("testdata/StorageDashboardServiceImplTest.xml");
        cleanRowsInCurrentConnection(new String[] { "sample_storage_assignment", "sample_item", "sample" });
        disposedStatusId = statusService.getStatusID(SampleStatus.Disposed);
    }

    @After
    public void cleanUp() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample_storage_assignment", "sample_item", "sample" });
    }

    @Test
    public void countSampleItemAssignmentMetrics_returnsZerosWhenTableIsEmpty() {
        Map<String, Long> metrics = sampleStorageAssignmentDAO.countSampleItemAssignmentMetrics(disposedStatusId);

        assertNotNull(metrics);
        assertEquals(Long.valueOf(0L), metrics.get("totalSampleItems"));
        assertEquals(Long.valueOf(0L), metrics.get("active"));
        assertEquals(Long.valueOf(0L), metrics.get("disposed"));
    }

    @Test
    public void countSampleItemAssignmentMetrics_countsActiveItem() {
        insertSampleAndItem(1001, 2001, "ACC-INT-1", "1");
        insertAssignment(3001, 2001);

        Map<String, Long> metrics = sampleStorageAssignmentDAO.countSampleItemAssignmentMetrics(disposedStatusId);

        assertEquals(Long.valueOf(1L), metrics.get("totalSampleItems"));
        assertEquals(Long.valueOf(1L), metrics.get("active"));
        assertEquals(Long.valueOf(0L), metrics.get("disposed"));
    }

    @Test
    public void countSampleItemAssignmentMetrics_countsDisposedItem() {
        insertSampleAndItem(1002, 2002, "ACC-INT-2", disposedStatusId);
        insertAssignment(3002, 2002);

        Map<String, Long> metrics = sampleStorageAssignmentDAO.countSampleItemAssignmentMetrics(disposedStatusId);

        assertEquals(Long.valueOf(1L), metrics.get("totalSampleItems"));
        assertEquals(Long.valueOf(0L), metrics.get("active"));
        assertEquals(Long.valueOf(1L), metrics.get("disposed"));
    }

    @Test
    public void countSampleItemAssignmentMetrics_aggregatesMixedActiveAndDisposedItems() {
        insertSampleAndItem(1003, 2003, "ACC-INT-3", "1");
        insertAssignment(3003, 2003);
        insertSampleAndItem(1004, 2004, "ACC-INT-4", "1");
        insertAssignment(3004, 2004);
        insertSampleAndItem(1006, 2006, "ACC-INT-6", disposedStatusId);
        insertAssignment(3006, 2006);

        Map<String, Long> metrics = sampleStorageAssignmentDAO.countSampleItemAssignmentMetrics(disposedStatusId);

        assertEquals(Long.valueOf(3L), metrics.get("totalSampleItems"));
        assertEquals(Long.valueOf(2L), metrics.get("active"));
        assertEquals(Long.valueOf(1L), metrics.get("disposed"));
    }

    @Test
    public void countSampleItemAssignmentMetrics_nullDisposedStatusIdCountsAllAsActive() {
        insertSampleAndItem(1005, 2005, "ACC-INT-5", "1");
        insertAssignment(3005, 2005);

        Map<String, Long> metrics = sampleStorageAssignmentDAO.countSampleItemAssignmentMetrics(null);

        assertEquals(Long.valueOf(1L), metrics.get("totalSampleItems"));
        assertEquals(Long.valueOf(1L), metrics.get("active"));
        assertEquals(Long.valueOf(0L), metrics.get("disposed"));
    }

    private void insertSampleAndItem(int sampleId, int sampleItemId, String accessionNumber, String itemStatusId) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample (id, accession_number, status_id, entered_date, received_date, sys_user_id) "
                        + "VALUES (?, ?, 1, NOW(), NOW(), 1)",
                sampleId, accessionNumber);

        Object statusParam = itemStatusId != null ? Integer.valueOf(itemStatusId) : null;
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, external_id, typeosamp_id, "
                        + "collection_date, collector, quantity, status_id, lastupdated) "
                        + "VALUES (?, ?, 1, ?, 1, NOW(), 'Tech', 1.0, ?, NOW())",
                sampleItemId, sampleId, accessionNumber + "-1", statusParam);
    }

    private void insertAssignment(int assignmentId, int sampleItemId) {
        jdbcTemplate.update("INSERT INTO clinlims.sample_storage_assignment "
                + "(id, occupant_type, sample_item_id, location_id, location_type, "
                + "position_coordinate, assigned_by_user_id, assigned_date, last_updated) "
                + "VALUES (?, 'SAMPLE_ITEM', ?, 1, 'rack', 'A1', 1, NOW(), NOW())", assignmentId, sampleItemId);
    }
}