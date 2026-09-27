package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;

/**
 * Unit Test Suite for ComplianceThresholdServiceImpl
 *
 * Tests service implementation with proper mocking to isolate business logic.
 * Follows OpenELIS testing patterns for service layer testing with transaction
 * boundary verification and business rule validation.
 *
 * Constitutional compliance: Tests service business logic independently of DAO
 * implementation.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComplianceThresholdServiceImplTest {

    @Mock
    private ComplianceThresholdDAO mockThresholdDAO;

    @Mock
    private ParameterGroupService mockParameterGroupService;

    @InjectMocks
    private ComplianceThresholdServiceImpl complianceThresholdService;

    private ComplianceThreshold testThreshold;
    private ParameterGroup testGroup;
    private String testGroupId = "GROUP_001";
    private String testThresholdId = "THRESHOLD_001";

    @Before
    public void setUp() {
        testThreshold = createTestThreshold();
        testThreshold.setId(testThresholdId);
        testGroup = new ParameterGroup();
        testGroup.setId(testGroupId);
    }

    @Test
    public void testSave_shouldValidateThresholdBusinessRules() throws LIMSRuntimeException {
        ComplianceThreshold validThreshold = createValidRangeThreshold();
        when(mockThresholdDAO.insert(validThreshold)).thenReturn("1");
        when(mockThresholdDAO.get("1")).thenReturn(java.util.Optional.of(validThreshold));

        ComplianceThreshold saved = complianceThresholdService.save(validThreshold);

        assertNotNull("Saved threshold should not be null", saved);
        verify(mockThresholdDAO).insert(validThreshold);
    }

    @Test
    public void testSave_shouldRejectInvalidRangeThreshold() {
        // Create threshold where min > max (invalid)
        ComplianceThreshold invalidThreshold = createTestThreshold();
        invalidThreshold.setThresholdType(ThresholdType.RANGE);
        invalidThreshold.setMinValue(new BigDecimal("10.0")); // min > max
        invalidThreshold.setMaxValue(new BigDecimal("5.0"));

        try {
            complianceThresholdService.save(invalidThreshold);
            fail("Should throw exception for invalid range");
        } catch (LIMSRuntimeException e) {
            assertTrue("Exception should mention min/max validation",
                    e.getMessage().contains("minimum value cannot exceed maximum value"));
            verify(mockThresholdDAO, never()).insert(any(ComplianceThreshold.class));
        }
    }

    @Test
    public void testSave_shouldRejectMinimumThresholdWithoutMinValue() {
        ComplianceThreshold invalidThreshold = createTestThreshold();
        invalidThreshold.setThresholdType(ThresholdType.MINIMUM);
        invalidThreshold.setMinValue(null); // Required for MINIMUM type

        try {
            complianceThresholdService.save(invalidThreshold);
            fail("Should throw exception for MINIMUM threshold without min value");
        } catch (LIMSRuntimeException e) {
            assertTrue("Exception should mention min value requirement",
                    e.getMessage().contains("Minimum value is required for MINIMUM threshold type"));
        }
    }

    @Test
    public void testSave_shouldRejectMaximumThresholdWithoutMaxValue() {
        ComplianceThreshold invalidThreshold = createTestThreshold();
        invalidThreshold.setThresholdType(ThresholdType.MAXIMUM);
        invalidThreshold.setMaxValue(null); // Required for MAXIMUM type

        try {
            complianceThresholdService.save(invalidThreshold);
            fail("Should throw exception for MAXIMUM threshold without max value");
        } catch (LIMSRuntimeException e) {
            assertTrue("Exception should mention max value requirement",
                    e.getMessage().contains("Maximum value is required for MAXIMUM threshold type"));
        }
    }

    @Test
    public void testSave_shouldRejectExactThresholdWithoutTargetValue() {
        ComplianceThreshold invalidThreshold = createTestThreshold();
        invalidThreshold.setThresholdType(ThresholdType.EXACT);
        invalidThreshold.setTargetValue(null); // Required for EXACT type

        try {
            complianceThresholdService.save(invalidThreshold);
            fail("Should throw exception for EXACT threshold without target value");
        } catch (LIMSRuntimeException e) {
            assertTrue("Exception should mention target value requirement",
                    e.getMessage().contains("Target value is required for EXACT threshold type"));
        }
    }

    @Test
    public void testUpdate_shouldValidateThresholdBeforeUpdate() throws LIMSRuntimeException {
        ComplianceThreshold validThreshold = createValidRangeThreshold();
        when(mockThresholdDAO.update(validThreshold)).thenReturn(validThreshold);

        complianceThresholdService.update(validThreshold);

        verify(mockThresholdDAO).update(validThreshold);
    }

    // Bulk Operations Tests

    // Delegation Tests (Constitutional Requirement)

    @Test
    public void testGetThresholdsByGroupId_shouldDelegateToDAO() throws LIMSRuntimeException {
        List<ComplianceThreshold> expectedThresholds = Arrays.asList(createTestThreshold());
        when(mockThresholdDAO.getThresholdsByGroupId(testGroupId)).thenReturn(expectedThresholds);

        List<ComplianceThreshold> result = complianceThresholdService.getThresholdsByGroupId(testGroupId);

        assertEquals("Should return DAO results", expectedThresholds, result);
        verify(mockThresholdDAO).getThresholdsByGroupId(testGroupId);
    }

    // Error Handling Tests

    @Test
    public void testSave_shouldPropagateDAOExceptions() throws LIMSRuntimeException {
        ComplianceThreshold validThreshold = createValidRangeThreshold();
        LIMSRuntimeException daoException = new LIMSRuntimeException("Database error");
        when(mockThresholdDAO.insert(validThreshold)).thenThrow(daoException);

        try {
            complianceThresholdService.save(validThreshold);
            fail("Should propagate DAO exception");
        } catch (LIMSRuntimeException e) {
            assertEquals("Should propagate original exception", daoException, e);
        }
    }

    private ComplianceThreshold createTestThreshold() {
        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setParameterCode("TEST_PARAM");
        threshold.setDisplayName("Test Parameter");
        threshold.setThresholdType(ThresholdType.RANGE);
        threshold.setMinValue(new BigDecimal("5.0"));
        threshold.setMaxValue(new BigDecimal("10.0"));
        threshold.setUnits("mg/L");
        threshold.setSortOrder(1);
        threshold.setSysUserId("1");
        return threshold;
    }

    private ComplianceThreshold createValidRangeThreshold() {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setThresholdType(ThresholdType.RANGE);
        threshold.setMinValue(new BigDecimal("6.5"));
        threshold.setMaxValue(new BigDecimal("8.5"));
        return threshold;
    }

    private ComplianceThreshold createMinimumThreshold() {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setThresholdType(ThresholdType.MINIMUM);
        threshold.setMinValue(new BigDecimal("5.0"));
        threshold.setMaxValue(null);
        threshold.setTargetValue(null);
        return threshold;
    }

    private ComplianceThreshold createMaximumThreshold() {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setThresholdType(ThresholdType.MAXIMUM);
        threshold.setMinValue(null);
        threshold.setMaxValue(new BigDecimal("10.0"));
        threshold.setTargetValue(null);
        return threshold;
    }

    private ComplianceThreshold createExactThreshold() {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setThresholdType(ThresholdType.EXACT);
        threshold.setMinValue(null);
        threshold.setMaxValue(null);
        threshold.setTargetValue(new BigDecimal("7.0"));
        return threshold;
    }

}