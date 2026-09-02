package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * TDD Test Suite for ParameterGroupService
 *
 * Tests the parameter group management functionality within compliance
 * standards. Follows constitutional TDD requirements and tests proper
 * transaction boundaries.
 */
public class ParameterGroupServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    private String testStandardId;
    private ComplianceStandard savedTestStandard;
    private ParameterGroup testParameterGroup;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        savedTestStandard = complianceStandardService.save(createTestStandard());
        testStandardId = savedTestStandard.getId();

        testParameterGroup = createTestParameterGroup(testStandardId);
    }

    @Test
    public void testSaveParameterGroup_shouldCreateNewGroup() {
        ParameterGroup newGroup = createValidParameterGroup(testStandardId, "Physical Parameters",
                "Temperature, pH, turbidity, color parameters", 1);

        ParameterGroup savedGroup = parameterGroupService.save(newGroup);
        String savedId = savedGroup.getId();

        assertNotNull("Saved ID should not be null", savedId);
        assertEquals("Group ID should match", newGroup.getId(), savedId);

        assertEquals("Standard ID should be set", testStandardId, newGroup.getStandardId());

        assertNotNull("Created date should be set", newGroup.getLastupdated());
    }

    @Test
    public void testGetGroupsByStandardId_shouldReturnOrderedGroups() {
        // Create multiple groups with different sort orders
        createAndSaveParameterGroup(testStandardId, "Chemical Parameters", 2);
        createAndSaveParameterGroup(testStandardId, "Physical Parameters", 1);
        createAndSaveParameterGroup(testStandardId, "Biological Parameters", 3);

        List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(testStandardId);

        assertNotNull("Groups list should not be null", groups);
        assertEquals("Should have 3 groups", 3, groups.size());

        assertEquals("First group should be Physical", "Physical Parameters", groups.get(0).getName());
        assertEquals("Second group should be Chemical", "Chemical Parameters", groups.get(1).getName());
        assertEquals("Third group should be Biological", "Biological Parameters", groups.get(2).getName());

        assertTrue("Sort orders should be ascending", groups.get(0).getSortOrder() < groups.get(1).getSortOrder());
    }

    @Test
    public void testUpdateParameterGroup_shouldUpdateExistingGroup() {
        String groupId = parameterGroupService.save(testParameterGroup).getId();
        ParameterGroup savedGroup = parameterGroupService.get(groupId);

        String originalName = savedGroup.getName();
        String newName = "Updated Parameter Group";
        savedGroup.setName(newName);

        parameterGroupService.update(savedGroup);

        ParameterGroup updatedGroup = parameterGroupService.get(groupId);
        assertEquals("Name should be updated", newName, updatedGroup.getName());

        assertFalse("Updated name should differ from original", originalName.equals(newName));
    }

    @Test
    public void testDeleteParameterGroup_shouldPreventDeletionWithThresholds() {
        // BR-003: a parameter group cannot be deleted while a ComplianceThreshold
        // references it. Create the group AND link a threshold, then assert that
        // delete refuses with the BR-003 message.
        String groupId = parameterGroupService.save(testParameterGroup).getId();

        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setGroup(parameterGroupService.get(groupId));
        threshold.setParameterCode("pH");
        threshold.setDisplayName("pH Level");
        threshold.setThresholdType(ThresholdType.RANGE);
        threshold.setMinValue(new BigDecimal("6.5"));
        threshold.setMaxValue(new BigDecimal("8.5"));
        threshold.setUnits("pH units");
        threshold.setSortOrder(1);
        threshold.setSysUserId("1");
        complianceThresholdService.save(threshold);

        ParameterGroup groupToDelete = parameterGroupService.get(groupId);
        try {
            parameterGroupService.delete(groupToDelete);
            fail("Should refuse to delete a parameter group that has linked thresholds (BR-003)");
        } catch (Exception e) {
            assertTrue("Exception should mention thresholds (BR-003 message)",
                    e.getMessage() != null && e.getMessage().toLowerCase().contains("threshold"));
        }
    }

    @Test
    public void testUniquenessConstraintPerStandard_shouldPreventDuplicateGroupNames() {
        createAndSaveParameterGroup(testStandardId, "Physical Parameters", 1);

        // Try to create duplicate group name in same standard
        ParameterGroup duplicate = createValidParameterGroup(testStandardId, "Physical Parameters", // Same name
                "Different description", 2);

        try {
            parameterGroupService.save(duplicate);
            fail("Should not allow duplicate group name within same standard");
        } catch (Exception e) {
            assertTrue("Should throw uniqueness constraint violation", e.getMessage().contains("already exists"));
        }
    }

    // Helper Methods

    private ComplianceStandard createTestStandard() {
        ComplianceStandard standard = new ComplianceStandard();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        standard.setName("Test Standard for Parameter Groups");
        standard.setIssuingBody("Test Authority");
        standard.setRegulationNumber("TG-" + uniqueSuffix);
        standard.setVersion("1.0");
        standard.setEffectiveDate(LocalDate.now());
        standard.setCountryRegion("Test Country");
        standard.setApplicableSampleTypes("Water");
        standard.setStatus(ComplianceStandardStatus.DRAFT);
        standard.setIsPreSeeded(false);
        standard.setSysUserId("1");
        return standard;
    }

    private ParameterGroup createTestParameterGroup(String standardId) {
        return createValidParameterGroup(standardId, "Test Parameter Group", "Test group for parameter management", 1);
    }

    private ParameterGroup createValidParameterGroup(String standardId, String name, String description,
            int sortOrder) {
        ParameterGroup group = new ParameterGroup();
        // Use the managed parent entity rather than a transient stub created via
        // setStandardId(...) — Hibernate refuses to persist children whose parent
        // reference is transient.
        group.setStandard(
                standardId.equals(testStandardId) ? savedTestStandard : complianceStandardService.get(standardId));
        group.setName(name);
        group.setDescription(description);
        group.setSortOrder(sortOrder);
        group.setSysUserId("1");
        return group;
    }

    private String createAndSaveParameterGroup(String standardId, String name, int sortOrder) {
        ParameterGroup group = createValidParameterGroup(standardId, name, "Test description", sortOrder);
        return parameterGroupService.save(group).getId();
    }
}