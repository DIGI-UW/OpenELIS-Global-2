package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for AnalyzerQueryService.storeFields() method
 * 
 * This test verifies the exact functionality that stores parsed analyzer fields
 * to the database, ensuring: - ID is set manually (required for "assigned" ID
 * strategy) - All fields are persisted correctly - No null values in required
 * fields - Transaction management works correctly
 * 
 * This test prevents the loop of deployment failures by catching issues before
 * code is deployed.
 */
public class AnalyzerQueryServiceStoreFieldsIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerQueryService analyzerQueryService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Analyzer testAnalyzer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Clean up any leftover test data first
        cleanTestData();

        // Create test analyzer (same as analyzer 1000 in test data)
        testAnalyzer = new Analyzer();
        testAnalyzer.setName("TEST-STORE-FIELDS-ANALYZER");
        testAnalyzer.setActive(false);
        testAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(testAnalyzer);
        testAnalyzer.setId(analyzerId);
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    /**
     * Clean up test-created analyzer field data
     */
    private void cleanTestData() {
        try {
            // Delete fields for test analyzer
            if (testAnalyzer != null && testAnalyzer.getId() != null) {
                jdbcTemplate.update("DELETE FROM analyzer_field WHERE analyzer_id = ?", testAnalyzer.getId());
            }
            // Delete test analyzer
            if (testAnalyzer != null && testAnalyzer.getId() != null) {
                jdbcTemplate.update("DELETE FROM analyzer WHERE id = ?", testAnalyzer.getId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test that storeFields correctly persists fields with manually assigned IDs
     * 
     * This test mimics the exact data structure produced by parseFieldRecords() and
     * verifies it's stored correctly.
     */
    @Test
    public void testStoreFields_WithManuallyAssignedId_PersistsAllFields() throws Exception {
        // Arrange: Create field data in the exact format produced by
        // parseFieldRecords()
        List<Map<String, Object>> fields = new ArrayList<>();

        // Field 1: WBC
        Map<String, Object> field1 = new HashMap<>();
        field1.put("sequence", 1);
        field1.put("fieldName", "WBC");
        field1.put("astmRef", "^^^WBC");
        field1.put("unit", "10^3/μL");
        field1.put("fieldType", "NUMERIC");
        fields.add(field1);

        // Field 2: RBC
        Map<String, Object> field2 = new HashMap<>();
        field2.put("sequence", 2);
        field2.put("fieldName", "RBC");
        field2.put("astmRef", "^^^RBC");
        field2.put("unit", "10^6/μL");
        field2.put("fieldType", "NUMERIC");
        fields.add(field2);

        // Field 3: HGB
        Map<String, Object> field3 = new HashMap<>();
        field3.put("sequence", 3);
        field3.put("fieldName", "HGB");
        field3.put("astmRef", "^^^HGB");
        field3.put("unit", "g/dL");
        field3.put("fieldType", "NUMERIC");
        fields.add(field3);

        // Act: Store fields using the service (this calls storeFields internally)
        // We need to use reflection or make storeFields protected/public for testing
        // For now, we'll test via the public API that uses storeFields

        // Use reflection to access private storeFields method
        // Get target object from Spring proxy
        Object target = AopProxyUtils.getSingletonTarget(analyzerQueryService);
        if (target == null) {
            target = analyzerQueryService;
        }

        java.lang.reflect.Method storeFieldsMethod = AnalyzerQueryServiceImpl.class.getDeclaredMethod("storeFields",
                String.class, List.class);
        storeFieldsMethod.setAccessible(true);

        int storedCount = (Integer) storeFieldsMethod.invoke(target, testAnalyzer.getId(), fields);

        // Assert: Verify all fields were stored
        assertEquals("Should store all 3 fields", 3, storedCount);

        // Verify fields were persisted to database
        List<AnalyzerField> savedFields = analyzerFieldService.getFieldsByAnalyzerId(testAnalyzer.getId());
        assertEquals("Should have 3 fields in database", 3, savedFields.size());

        // Verify each field has all required values
        for (AnalyzerField field : savedFields) {
            assertNotNull("Field ID should not be null", field.getId());
            assertNotNull("Field name should not be null", field.getFieldName());
            assertNotNull("Field type should not be null", field.getFieldType());
            assertNotNull("Analyzer should not be null", field.getAnalyzer());
            assertNotNull("IsActive should not be null", field.getIsActive());

            // Verify field has correct values
            assertTrue("Field name should not be empty", !field.getFieldName().trim().isEmpty());
            assertEquals("Analyzer ID should match", testAnalyzer.getId(), field.getAnalyzer().getId());
            assertTrue("Field should be active", field.getIsActive());
        }

        // Verify specific field values
        AnalyzerField wbcField = savedFields.stream().filter(f -> "WBC".equals(f.getFieldName())).findFirst()
                .orElse(null);
        assertNotNull("WBC field should exist", wbcField);
        assertEquals("WBC astmRef should match", "^^^WBC", wbcField.getAstmRef());
        assertEquals("WBC unit should match", "10^3/μL", wbcField.getUnit());
        assertEquals("WBC type should be NUMERIC", AnalyzerField.FieldType.NUMERIC, wbcField.getFieldType());

        AnalyzerField rbcField = savedFields.stream().filter(f -> "RBC".equals(f.getFieldName())).findFirst()
                .orElse(null);
        assertNotNull("RBC field should exist", rbcField);
        assertEquals("RBC astmRef should match", "^^^RBC", rbcField.getAstmRef());
        assertEquals("RBC unit should match", "10^6/μL", rbcField.getUnit());
    }

    /**
     * Test that storeFields handles empty field list
     */
    @Test
    public void testStoreFields_WithEmptyList_ReturnsZero() throws Exception {
        // Arrange
        List<Map<String, Object>> emptyFields = new ArrayList<>();

        // Act - Get target object from Spring proxy
        Object target = AopProxyUtils.getSingletonTarget(analyzerQueryService);
        if (target == null) {
            target = analyzerQueryService;
        }

        java.lang.reflect.Method storeFieldsMethod = AnalyzerQueryServiceImpl.class.getDeclaredMethod("storeFields",
                String.class, List.class);
        storeFieldsMethod.setAccessible(true);

        int storedCount = (Integer) storeFieldsMethod.invoke(target, testAnalyzer.getId(), emptyFields);

        // Assert
        assertEquals("Should store 0 fields", 0, storedCount);
    }

    /**
     * Test that storeFields skips duplicate fields (by astmRef)
     */
    @Test
    public void testStoreFields_WithDuplicateAstmRef_SkipsDuplicates() throws Exception {
        // Arrange: Create fields with duplicate astmRef
        List<Map<String, Object>> fields = new ArrayList<>();

        Map<String, Object> field1 = new HashMap<>();
        field1.put("fieldName", "WBC");
        field1.put("astmRef", "^^^WBC");
        field1.put("unit", "10^3/μL");
        field1.put("fieldType", "NUMERIC");
        fields.add(field1);

        // Store first field - Get target object from Spring proxy
        Object target = AopProxyUtils.getSingletonTarget(analyzerQueryService);
        if (target == null) {
            target = analyzerQueryService;
        }

        java.lang.reflect.Method storeFieldsMethod = AnalyzerQueryServiceImpl.class.getDeclaredMethod("storeFields",
                String.class, List.class);
        storeFieldsMethod.setAccessible(true);

        int firstCount = (Integer) storeFieldsMethod.invoke(target, testAnalyzer.getId(), fields);

        // Act: Try to store same field again
        int secondCount = (Integer) storeFieldsMethod.invoke(target, testAnalyzer.getId(), fields);

        // Assert
        assertEquals("First store should save 1 field", 1, firstCount);
        assertEquals("Second store should skip duplicate", 0, secondCount);

        // Verify only one field exists
        List<AnalyzerField> savedFields = analyzerFieldService.getFieldsByAnalyzerId(testAnalyzer.getId());
        assertEquals("Should have only 1 field", 1, savedFields.size());
    }
}
