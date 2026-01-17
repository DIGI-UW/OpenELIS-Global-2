/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO integration tests for OrderSampleLinkDAO.
 *
 * <p>
 * These tests exercise actual Hibernate operations against a test database to
 * catch:
 * <ul>
 * <li>Hibernate mapping errors (e.g., missing columns like last_updated)
 * <li>HQL query compilation errors
 * <li>Foreign key constraint violations
 * <li>Data type mismatches
 * </ul>
 *
 * <p>
 * This test would have caught the missing last_updated column issue that caused
 * transaction abortion in production.
 *
 * <p>
 * TODO: Requires proper test data setup with Liquibase seed data.
 *
 * <p>
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 */
public class OrderSampleLinkDAOIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderSampleLinkDAO orderSampleLinkDAO;

    private JdbcTemplate jdbcTemplate;

    private Integer testOrderId1 = 9001;
    private Integer testOrderId2 = 9002;
    private Integer testSampleId1 = 9101;
    private Integer testSampleId2 = 9102;
    private Integer testSampleItemId1 = 9201;
    private Integer testSampleItemId2 = 9202;
    private Integer testLinkId1 = 9301;
    private Integer testLinkId2 = 9302;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);

        executeDataSetWithStateManagement("testdata/status-of-sample.xml");
        executeDataSetWithStateManagement("testdata/typeofsample.xml");
        // Load test data using DBUnit - this will FAIL if last_updated column is
        // missing
        executeDataSetWithStateManagement("testdata/order-sample-link-dao-test-data.xml");
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Clean in reverse dependency order
            jdbcTemplate.execute("DELETE FROM clinlims.order_sample_link WHERE id IN (9301, 9302)");
            jdbcTemplate.execute("DELETE FROM clinlims.sample_item WHERE id IN (9201, 9202)");
            jdbcTemplate.execute("DELETE FROM clinlims.sample WHERE id IN (9101, 9102)");
            jdbcTemplate.execute("DELETE FROM clinlims.electronic_order WHERE id IN (9001, 9002)");
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    // ============================================================
    // Tests for basic CRUD operations - these exercise Hibernate
    // ============================================================

    /**
     * Test: get() - exercises Hibernate entity loading This test will FAIL if
     * OrderSampleLink entity has mapping errors (e.g., missing last_updated column)
     */
    @Test
    public void testGet_WithValidId_LoadsEntity() {
        // Act: This exercises Hibernate's SELECT query with all mapped columns
        OrderSampleLink result = orderSampleLinkDAO.get(testLinkId1).orElse(null);

        // Assert
        assertNotNull("Should load entity from database", result);
        assertEquals("ID should match", testLinkId1, result.getId());
        assertEquals("Order ID should match", testOrderId1, result.getElectronicOrderId());
        assertEquals("Sample ID should match", testSampleId1, result.getSampleId());
        assertEquals("Sample Item ID should match", testSampleItemId1, result.getSampleItemId());
        assertEquals("Container type should match", "EDTA Tube", result.getContainerTypeRequired());
        assertEquals("Volume should match", 0, new BigDecimal("5.0").compareTo(result.getVolumeRequiredMl()));
        assertEquals("Handling requirements should match", "Keep refrigerated", result.getHandlingRequirements());
        assertFalse("Should not be validated", result.isValidated());
        assertNotNull("Created at should be set", result.getCreatedAt());
        assertNotNull("Last updated should be set", result.getLastupdated()); // THIS LINE WOULD FAIL without
                                                                              // last_updated
                                                                              // column
    }

    /**
     * Test: insert() - exercises Hibernate entity insertion This test will FAIL if
     * Hibernate generates INSERT with missing columns
     */
    @Test
    public void testInsert_CreatesNewEntity() {
        // Arrange
        OrderSampleLink newLink = new OrderSampleLink();
        newLink.setElectronicOrderId(testOrderId1);
        newLink.setSampleId(testSampleId2);
        newLink.setSampleItemId(testSampleItemId2);
        newLink.setValidated(false);
        newLink.setCreatedBy(1);
        newLink.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        // Note: Do NOT manually set lastUpdated - Hibernate manages it via @Version

        // Act: This exercises Hibernate's INSERT query
        Integer newId = orderSampleLinkDAO.insert(newLink);

        // Assert
        assertNotNull("Should return generated ID", newId);

        // Verify entity was persisted
        OrderSampleLink persisted = orderSampleLinkDAO.get(newId).orElse(null);
        assertNotNull("Should retrieve persisted entity", persisted);
        assertEquals("Order ID should match", testOrderId1, persisted.getElectronicOrderId());

        // Cleanup
        jdbcTemplate.execute("DELETE FROM clinlims.order_sample_link WHERE id = " + newId);
    }

    /**
     * Test: update() - exercises Hibernate entity update This test will FAIL if
     * Hibernate generates UPDATE with incorrect column mappings
     */
    @Test
    public void testUpdate_ModifiesEntity() {
        // Arrange
        OrderSampleLink link = orderSampleLinkDAO.get(testLinkId1).orElse(null);
        assertNotNull("Test link should exist", link);

        // Modify entity
        link.setValidated(true);
        link.setValidatedBy(5);
        link.setValidatedAt(new Timestamp(System.currentTimeMillis()));
        // Note: Do NOT manually set lastUpdated - Hibernate manages it via @Version

        // Act: This exercises Hibernate's UPDATE query with @Version optimistic locking
        OrderSampleLink updated = orderSampleLinkDAO.update(link);

        // Assert
        assertNotNull("Should return updated entity", updated);
        assertTrue("Should be marked validated", updated.isValidated());
        assertEquals("Validator ID should be set", Integer.valueOf(5), updated.getValidatedBy());
        assertNotNull("Validated at should be set", updated.getValidatedAt());
    }

    // ============================================================
    // Tests for custom query methods - these exercise HQL queries
    // ============================================================

    /**
     * Test: hasOrderForSample - CRITICAL QC validation query This test will FAIL if
     * HQL query references non-existent properties
     */
    @Test
    public void testHasOrderForSample_WithLinkedSample_ReturnsTrue() {
        // Act: Exercises HQL COUNT query
        boolean result = orderSampleLinkDAO.hasOrderForSample(testSampleId1);

        // Assert
        assertTrue("Sample with order link should return true", result);
    }

    @Test
    public void testHasOrderForSample_WithUnlinkedSample_ReturnsFalse() {
        // Act
        boolean result = orderSampleLinkDAO.hasOrderForSample(9999);

        // Assert
        assertFalse("Sample without order link should return false", result);
    }

    /**
     * Test: getLinksBySampleId - exercises HQL query with WHERE clause
     */
    @Test
    public void testGetLinksBySampleId_ReturnsMatchingLinks() {
        // Act: Exercises HQL SELECT query
        List<OrderSampleLink> results = orderSampleLinkDAO.getLinksBySampleId(testSampleId1);

        // Assert
        assertNotNull("Should return results", results);
        assertEquals("Should find 1 link", 1, results.size());
        assertEquals("Link ID should match", testLinkId1, results.get(0).getId());
    }

    /**
     * Test: getLinksByOrderId - exercises HQL query by order ID
     */
    @Test
    public void testGetLinksByOrderId_ReturnsMatchingLinks() {
        // Act
        List<OrderSampleLink> results = orderSampleLinkDAO.getLinksByOrderId(testOrderId1);

        // Assert
        assertNotNull("Should return results", results);
        assertEquals("Should find 1 link", 1, results.size());
        assertEquals("Order ID should match", testOrderId1, results.get(0).getElectronicOrderId());
    }

    /**
     * Test: getLinksBySampleItemId - exercises HQL query by sample item ID
     */
    @Test
    public void testGetLinksBySampleItemId_ReturnsMatchingLinks() {
        // Act
        List<OrderSampleLink> results = orderSampleLinkDAO.getLinksBySampleItemId(testSampleItemId1);

        // Assert
        assertNotNull("Should return results", results);
        assertEquals("Should find 1 link", 1, results.size());
        assertEquals("Sample Item ID should match", testSampleItemId1, results.get(0).getSampleItemId());
    }

    /**
     * Test: getValidatedLinksByOrderId - exercises HQL query with multiple WHERE
     * conditions
     */
    @Test
    public void testGetValidatedLinksByOrderId_ReturnsOnlyValidatedLinks() {
        // Act
        List<OrderSampleLink> results = orderSampleLinkDAO.getValidatedLinksByOrderId(testOrderId2);

        // Assert
        assertNotNull("Should return results", results);
        assertEquals("Should find 1 validated link", 1, results.size());
        assertTrue("Link should be validated", results.get(0).isValidated());
    }

    /**
     * Test: getUnvalidatedLinks - exercises HQL query filtering by validated status
     */
    @Test
    public void testGetUnvalidatedLinks_ReturnsUnvalidatedLinks() {
        // Act
        List<OrderSampleLink> results = orderSampleLinkDAO.getUnvalidatedLinks();

        // Assert
        assertNotNull("Should return results", results);
        assertTrue("Should find at least 1 unvalidated link", results.size() >= 1);

        // Find our test link
        OrderSampleLink ourLink = results.stream().filter(l -> l.getId().equals(testLinkId1)).findFirst().orElse(null);
        assertNotNull("Should find test link", ourLink);
        assertFalse("Link should not be validated", ourLink.isValidated());
    }

    /**
     * Test: getLinkByOrderSampleTest - exercises HQL query with unique combination
     */
    @Test
    public void testGetLinkByOrderSampleTest_WithMatchingCombination_ReturnsLink() {
        // Act
        OrderSampleLink result = orderSampleLinkDAO.getLinkByOrderSampleTest(testOrderId1, testSampleId1, null);

        // Assert
        assertNotNull("Should find matching link", result);
        assertEquals("Link ID should match", testLinkId1, result.getId());
    }

    @Test
    public void testGetLinkByOrderSampleTest_WithNonMatchingCombination_ReturnsNull() {
        // Act
        OrderSampleLink result = orderSampleLinkDAO.getLinkByOrderSampleTest(9999, 9999, null);

        // Assert
        assertNull("Should not find link for non-existent combination", result);
    }

    // ============================================================
    // Tests for edge cases and data integrity
    // ============================================================

    /**
     * Test: Verify last_updated is populated by BaseObject This test specifically
     * validates the fix for the missing last_updated column issue
     */
    @Test
    public void testLastUpdated_IsPopulatedByHibernate() {
        // Arrange
        OrderSampleLink link = orderSampleLinkDAO.get(testLinkId1).orElse(null);
        assertNotNull("Test link should exist", link);
        Timestamp originalTimestamp = link.getLastupdated();
        assertNotNull("Last updated should be set on load", originalTimestamp);

        // Modify and save
        link.setValidated(true);
        // Note: Do NOT manually set lastUpdated - Hibernate manages it via @Version
        OrderSampleLink updated = orderSampleLinkDAO.update(link);

        // Assert: Hibernate should update last_updated via @Version
        assertNotNull("Last updated should be set after update", updated.getLastupdated());
    }

    /**
     * Test: Verify foreign key constraints are enforced This test ensures database
     * integrity is maintained
     */
    @Test(expected = Exception.class)
    public void testInsert_WithInvalidOrderId_ThrowsException() {
        // Arrange
        OrderSampleLink invalidLink = new OrderSampleLink();
        invalidLink.setElectronicOrderId(99999); // Non-existent order
        invalidLink.setSampleId(testSampleId1);
        invalidLink.setValidated(false);
        invalidLink.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        // Note: Do NOT manually set lastUpdated - Hibernate manages it via @Version

        // Act: Should throw exception due to foreign key constraint
        orderSampleLinkDAO.insert(invalidLink);
    }
}
