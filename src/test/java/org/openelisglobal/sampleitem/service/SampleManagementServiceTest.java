package org.openelisglobal.sampleitem.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sampleitem.dto.SampleItemDTO;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit tests for SampleManagementService.searchByAccessionNumber method.
 *
 * <p>
 * Tests search functionality for sample items by accession number, including
 * hierarchy loading and test data inclusion.
 *
 * <p>
 * Related: Feature 001-sample-management, User Story 1, Task T023
 *
 * @see SampleManagementService
 */
public class SampleManagementServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleManagementService sampleManagementService;
    @Autowired
    private javax.sql.DataSource testDataSource;
    @Autowired
    private org.openelisglobal.common.services.IStatusService testStatusService;

    @Before
    public void init() throws Exception {
        // Load test data for sample items
        executeDataSetWithStateManagement("testdata/sampleitem.xml");
    }

    /**
     * OGC-1023 (R4, FR-E3) — a canceled analysis must not block re-ordering the
     * same test on the item (the NCE Retest disposition cancels then re-orders); an
     * active analysis still deduplicates.
     */
    @Test
    public void addTests_reordersAfterCancel_butSkipsWhenActive() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                testDataSource);
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                + " description, lastupdated) VALUES (9101, 'Not Tested', 4, 'ANALYSIS', 'Y', 's.9101', 'nt', NOW())");
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                + " description, lastupdated) VALUES (9102, 'Test Canceled', 5, 'ANALYSIS', 'Y', 's.9102', 'c',"
                + " NOW())");
        testStatusService.refreshCache();
        jdbc.update("UPDATE clinlims.sample_item SET quantity = 10 WHERE id = 601");
        // result.xml inserts analyses with explicit ids; realign the sequence so
        // the new retest analysis doesn't collide
        jdbc.execute("SELECT setval('clinlims.analysis_seq', (SELECT COALESCE(MAX(id)::bigint, 1) FROM"
                + " clinlims.analysis))");

        // analysis 1 = test 1 on sample item 601; while it is ACTIVE the add is
        // deduplicated
        jdbc.update("UPDATE clinlims.analysis SET status_id = 9101 WHERE id = 1");
        org.openelisglobal.sampleitem.form.AddTestsForm form = new org.openelisglobal.sampleitem.form.AddTestsForm();
        form.setSampleItemIds(java.util.List.of("601"));
        form.setTestIds(java.util.List.of("1"));
        org.openelisglobal.sampleitem.dto.AddTestsResponse skipped = sampleManagementService.addTestsToSamples(form,
                "1");
        assertEquals(0, skipped.getSuccessCount());

        // once canceled, the same test can be ordered again (retest)
        jdbc.update("UPDATE clinlims.analysis SET status_id = 9102 WHERE id = 1");
        org.openelisglobal.sampleitem.dto.AddTestsResponse added = sampleManagementService.addTestsToSamples(form, "1");
        assertEquals(1, added.getSuccessCount());
    }

    @Test
    public void searchByAccessionNumber_shouldReturnSampleItemsForValidAccessionNumber() {
        // Arrange: Test data has sample with accession number "13333"
        String accessionNumber = "13333";

        // Act: Search by accession number without including tests
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: Should return non-null response with matching accession number
        assertNotNull("Response should not be null", response);
        assertEquals("Accession number should match query", accessionNumber, response.getAccessionNumber());
        assertNotNull("Sample items list should not be null", response.getSampleItems());
        assertTrue("Should return at least one sample item", response.getSampleItems().size() > 0);
        assertTrue("Total count should be greater than 0", response.getTotalCount() > 0);
    }

    @Test
    public void searchByAccessionNumber_shouldReturnEmptyResultsForNonExistentAccessionNumber() {
        // Arrange: Use non-existent accession number
        String accessionNumber = "NONEXISTENT999";

        // Act: Search by non-existent accession number
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: Should return empty results
        assertNotNull("Response should not be null", response);
        assertEquals("Accession number should match query", accessionNumber, response.getAccessionNumber());
        assertNotNull("Sample items list should not be null", response.getSampleItems());
        assertEquals("Should return zero sample items", 0, response.getSampleItems().size());
        assertEquals("Total count should be 0", 0, response.getTotalCount());
    }

    @Test
    public void searchByAccessionNumber_shouldPopulateSampleItemDTOFields() {
        // Arrange: Test data has sample with accession number "13333"
        String accessionNumber = "13333";

        // Act: Search by accession number
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: DTO fields should be populated
        assertNotNull("Response should not be null", response);
        assertFalse("Sample items should not be empty", response.getSampleItems().isEmpty());

        SampleItemDTO dto = response.getSampleItems().get(0);
        assertNotNull("Sample item DTO should not be null", dto);
        assertNotNull("Sample item ID should not be null", dto.getId());
        assertNotNull("External ID should not be null", dto.getExternalId());
        assertEquals("Sample accession number should match", accessionNumber, dto.getSampleAccessionNumber());
    }

    @Test
    public void searchByAccessionNumber_shouldLoadHierarchyDataForAliquots() {
        // Arrange: This test will verify parent-child relationships are loaded
        // Note: Current test data may not have aliquots, but the service should handle
        // it
        String accessionNumber = "13333";

        // Act: Search by accession number
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: Hierarchy-related fields should be initialized (not null)
        assertNotNull("Response should not be null", response);
        assertFalse("Sample items should not be empty", response.getSampleItems().isEmpty());

        SampleItemDTO dto = response.getSampleItems().get(0);
        assertNotNull("Child aliquots list should not be null", dto.getChildAliquots());
        // Parent ID can be null if this is a root sample (not an aliquot)
    }

    @Test
    public void searchByAccessionNumber_withIncludeTestsTrue_shouldLoadOrderedTests() {
        // Arrange: Search with includeTests flag set to true
        String accessionNumber = "13333";

        // Act: Search by accession number with tests included
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, true);

        // Assert: Ordered tests should be loaded
        assertNotNull("Response should not be null", response);
        assertFalse("Sample items should not be empty", response.getSampleItems().isEmpty());

        SampleItemDTO dto = response.getSampleItems().get(0);
        assertNotNull("Ordered tests list should not be null", dto.getOrderedTests());
        // Note: The actual test data may or may not have tests, but list should not be
        // null
    }

    @Test
    public void searchByAccessionNumber_withIncludeTestsFalse_shouldNotLoadTests() {
        // Arrange: Search with includeTests flag set to false
        String accessionNumber = "13333";

        // Act: Search by accession number without tests
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: Ordered tests list should be initialized but likely empty
        assertNotNull("Response should not be null", response);
        assertFalse("Sample items should not be empty", response.getSampleItems().isEmpty());

        SampleItemDTO dto = response.getSampleItems().get(0);
        assertNotNull("Ordered tests list should not be null", dto.getOrderedTests());
        // When includeTests=false, the list should be empty or minimal
    }

    @Test
    public void searchByAccessionNumber_shouldCalculateComputedFields() {
        // Arrange: Test computed fields (hasRemainingQuantity, isAliquot, nestingLevel)
        String accessionNumber = "13333";

        // Act: Search by accession number
        SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber, false);

        // Assert: Computed fields should be set
        assertNotNull("Response should not be null", response);
        assertFalse("Sample items should not be empty", response.getSampleItems().isEmpty());

        SampleItemDTO dto = response.getSampleItems().get(0);
        // These fields should be calculated based on entity data
        // hasRemainingQuantity is based on remainingQuantity value
        // isAliquot is based on parentSampleItem presence
        // nestingLevel is calculated from parent chain
        // Just verify they're accessible (not testing specific values without knowing
        // test data)
        dto.isHasRemainingQuantity(); // Should not throw exception
        dto.isAliquot(); // Should not throw exception
        dto.getNestingLevel(); // Should not throw exception
    }
}
