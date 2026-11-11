package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for ComplianceThresholdService
 *
 * Tests threshold management functionality within compliance standards. Follows
 * constitutional TDD requirements and validates proper transaction boundaries.
 * Tests threshold value validation, parameter associations, and evaluation
 * logic.
 */
public class ComplianceThresholdServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    private String testStandardId;
    private String testGroupId;
    private ComplianceStandard savedStandard;
    private ComplianceThreshold testThreshold;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        savedStandard = complianceStandardService.save(createTestStandard());
        testStandardId = savedStandard.getId();

        ParameterGroup testGroup = createTestParameterGroup(savedStandard);
        testGroupId = parameterGroupService.save(testGroup).getId();

        testThreshold = createTestThreshold(testGroupId);
    }

    @Test
    public void testSaveThreshold_shouldCreateNewThreshold() {
        ComplianceThreshold newThreshold = createValidThreshold(testGroupId, "pH", "pH Level", ThresholdType.RANGE,
                new BigDecimal("6.5"), new BigDecimal("8.5"), "pH units");

        ComplianceThreshold savedThreshold = complianceThresholdService.save(newThreshold);
        String savedId = savedThreshold.getId();

        assertNotNull("Saved ID should not be null", savedId);
        assertEquals("Threshold ID should match", newThreshold.getId(), savedId);

        assertEquals("Group ID should be set", testGroupId, newThreshold.getGroupId());

        assertNotNull("Created date should be set", newThreshold.getLastupdated());
    }

    @Test
    public void testGetThresholdsByGroupId_shouldReturnOrderedThresholds() {
        // Create multiple thresholds with different sort orders
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);
        createAndSaveThreshold(testGroupId, "Temperature", ThresholdType.MAXIMUM, 2);
        createAndSaveThreshold(testGroupId, "Turbidity", ThresholdType.MAXIMUM, 3);

        List<ComplianceThreshold> thresholds = complianceThresholdService.getThresholdsByGroupId(testGroupId);

        assertNotNull("Thresholds list should not be null", thresholds);
        assertEquals("Should have 3 thresholds", 3, thresholds.size());

        assertEquals("First threshold should be pH", "pH", thresholds.get(0).getParameterCode());
        assertEquals("Second threshold should be Temperature", "Temperature", thresholds.get(1).getParameterCode());
        assertEquals("Third threshold should be Turbidity", "Turbidity", thresholds.get(2).getParameterCode());

        assertTrue("Sort orders should be ascending",
                thresholds.get(0).getSortOrder() < thresholds.get(1).getSortOrder());
    }

    @Test
    public void testUpdateThreshold_shouldUpdateExistingThreshold() {
        String thresholdId = complianceThresholdService.save(testThreshold).getId();
        ComplianceThreshold savedThreshold = complianceThresholdService.get(thresholdId);

        BigDecimal originalMinValue = savedThreshold.getMinValue();
        BigDecimal newMinValue = new BigDecimal("7.0");
        savedThreshold.setMinValue(newMinValue);

        complianceThresholdService.update(savedThreshold);

        ComplianceThreshold updatedThreshold = complianceThresholdService.get(thresholdId);
        // Use compareTo: BigDecimal#equals is scale-sensitive but Postgres rehydrates
        // numeric(15,6) as 7.000000 while the literal we set is "7.0" (scale 1).
        assertEquals("Min value should be updated", 0, newMinValue.compareTo(updatedThreshold.getMinValue()));

        assertNotEquals("Updated min value should differ from original", 0, originalMinValue.compareTo(newMinValue));
    }

    @Test
    public void testValidateThresholdValues_shouldEnforceBusinessRules() {
        ComplianceThreshold invalidRangeThreshold = createValidThreshold(testGroupId, "pH", "pH Level",
                ThresholdType.RANGE, new BigDecimal("8.5"), // min > max (invalid)
                new BigDecimal("6.5"), "pH units");

        try {
            complianceThresholdService.save(invalidRangeThreshold);
            fail("Should not allow min value greater than max value");
        } catch (Exception e) {
            assertTrue("Should throw validation error for invalid range",
                    e.getMessage().contains("minimum value cannot exceed maximum"));
        }
    }

    @Test
    public void testUniquenessConstraintPerGroup_shouldPreventDuplicateParameters() {
        // FRS S-01 v2.3 scopes uniqueness by (groupId, parameterCode,
        // thresholdType) so multi-limit saves (e.g. HIGH + BORDERLINE on
        // the same parameter) don't collide. The duplicate must therefore
        // share the SAME thresholdType to trip the constraint.
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);

        ComplianceThreshold duplicate = createValidThreshold(testGroupId, "pH", "Different display name",
                ThresholdType.RANGE, new BigDecimal("6.5"), new BigDecimal("8.0"), "pH units");

        try {
            complianceThresholdService.save(duplicate);
            fail("Should not allow duplicate parameter code with same threshold type within same group");
        } catch (Exception e) {
            assertTrue("Should throw uniqueness constraint violation", e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testUniquenessConstraintPerGroup_shouldAllowSameParameterDifferentType() {
        // Same parameterCode but different thresholdType is now valid (HIGH +
        // BORDERLINE for the same parameter is the canonical multi-limit case).
        createAndSaveThreshold(testGroupId, "pH", ThresholdType.RANGE, 1);

        ComplianceThreshold sibling = createValidThreshold(testGroupId, "pH", "pH advisory zone",
                ThresholdType.BORDERLINE, new BigDecimal("6.5"), new BigDecimal("7.0"), "pH units");

        ComplianceThreshold saved = complianceThresholdService.save(sibling);
        assertNotNull("Should accept different threshold type for same parameter", saved);
        assertNotNull("Saved threshold should have an id", saved.getId());
    }

    // Helper Methods

    private ComplianceStandard createTestStandard() {
        ComplianceStandard standard = new ComplianceStandard();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        standard.setName("Test Standard for Thresholds");
        standard.setIssuingBody("Test Authority");
        standard.setRegulationNumber("TT-" + uniqueSuffix);
        standard.setVersion("1.0");
        standard.setCountryRegion("Indonesia");
        standard.setApplicableSampleTypes("Water");
        standard.setEffectiveDate(LocalDate.now());
        standard.setStatus(ComplianceStandardStatus.DRAFT);
        standard.setIsPreSeeded(false);
        standard.setSysUserId("1");
        return standard;
    }

    private ParameterGroup createTestParameterGroup(ComplianceStandard standard) {
        ParameterGroup group = new ParameterGroup();
        group.setStandard(standard);
        group.setName("Test Parameter Group");
        group.setDescription("Test group for threshold management");
        group.setSortOrder(1);
        group.setSysUserId("1");
        return group;
    }

    private ComplianceThreshold createTestThreshold(String groupId) {
        return createValidThreshold(groupId, "pH", "pH Level", ThresholdType.RANGE, new BigDecimal("6.5"),
                new BigDecimal("8.5"), "pH units");
    }

    private ComplianceThreshold createValidThreshold(String groupId, String parameterCode, String displayName,
            ThresholdType thresholdType, BigDecimal minValue, BigDecimal maxValue, String units) {
        ComplianceThreshold threshold = new ComplianceThreshold();
        // Load the managed ParameterGroup to avoid TransientPropertyValueException
        threshold.setGroup(parameterGroupService.get(groupId));
        threshold.setParameterCode(parameterCode);
        threshold.setDisplayName(displayName);
        threshold.setThresholdType(thresholdType);
        threshold.setMinValue(minValue);
        threshold.setMaxValue(maxValue);
        threshold.setUnits(units);
        threshold.setSortOrder(1);
        threshold.setSysUserId("1");
        return threshold;
    }

    private String createAndSaveThreshold(String groupId, String parameterCode, ThresholdType thresholdType,
            int sortOrder) {
        ComplianceThreshold threshold = createValidThreshold(groupId, parameterCode, "Test " + parameterCode,
                thresholdType, new BigDecimal("1.0"), new BigDecimal("10.0"), "units");
        threshold.setSortOrder(sortOrder);
        return complianceThresholdService.save(threshold).getId();
    }
}