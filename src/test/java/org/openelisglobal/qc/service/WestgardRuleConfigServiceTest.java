package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.WestgardRuleConfigDAO;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Unit tests for WestgardRuleConfigService (T066) Tests rule configuration
 * management: enable/disable, presets, validation
 *
 * Following Constitution V (TDD): Tests written FIRST, implementation follows
 */
@RunWith(MockitoJUnitRunner.class)
public class WestgardRuleConfigServiceTest {

    @Mock
    private WestgardRuleConfigDAO ruleConfigDAO;

    @InjectMocks
    private WestgardRuleConfigServiceImpl ruleConfigService;

    private WestgardRuleConfig rule13s;
    private WestgardRuleConfig rule22s;
    private WestgardRuleConfig ruleR4s;
    private WestgardRuleConfig rule41s;

    @Before
    public void setUp() {
        // Setup test rules for test ID 1, instrument ID 1
        rule13s = createRule("1", "1", "1", "1₃ₛ", true, "REJECTION");
        rule22s = createRule("2", "1", "1", "2₂ₛ", true, "WARNING");
        ruleR4s = createRule("3", "1", "1", "R₄ₛ", true, "REJECTION");
        rule41s = createRule("4", "1", "1", "4₁ₛ", false, "REJECTION");
    }

    private WestgardRuleConfig createRule(String id, String testId, String instrumentId, String ruleCode,
            boolean enabled, String severity) {
        WestgardRuleConfig rule = new WestgardRuleConfig();
        rule.setId(id);
        rule.setTestId(testId);
        rule.setInstrumentId(instrumentId);
        rule.setRuleCode(ruleCode);
        rule.setEnabled(enabled);
        rule.setSeverity(severity);
        rule.setRequiresCorrectiveAction(true);
        return rule;
    }

    /**
     * Test: Find all rule configurations for test and instrument Task Reference:
     * T066
     */
    @Test
    public void testFindByTestAndInstrument_ShouldReturnAllRules() {
        // Arrange
        List<WestgardRuleConfig> allRules = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(allRules);

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.findByTestAndInstrument("1", "1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return all 4 rules", 4, result.size());
        verify(ruleConfigDAO, times(1)).findByTestAndInstrument("1", "1");
    }

    /**
     * Test: Find only enabled rules Task Reference: T066
     */
    @Test
    public void testFindEnabledByTestAndInstrument_ShouldReturnOnlyEnabledRules() {
        // Arrange - 3 enabled rules (4₁ₛ is disabled)
        List<WestgardRuleConfig> enabledRules = Arrays.asList(rule13s, rule22s, ruleR4s);
        when(ruleConfigDAO.findEnabledByTestAndInstrument("1", "1")).thenReturn(enabledRules);

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.findEnabledByTestAndInstrument("1", "1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 3 enabled rules", 3, result.size());
        assertTrue("All returned rules should be enabled", result.stream().allMatch(WestgardRuleConfig::getEnabled));
        verify(ruleConfigDAO, times(1)).findEnabledByTestAndInstrument("1", "1");
    }

    /**
     * Test: Update rule configuration (enable/disable) Task Reference: T066
     */
    @Test
    public void testUpdateRuleConfig_ShouldUpdateAndReturnConfig() {
        // Arrange - Disable rule 1₃ₛ but keep R₄ₛ as another rejection rule
        rule13s.setEnabled(false);
        ruleR4s.setEnabled(true); // Another rejection rule still enabled

        // Mock: findByTestAndInstrument returns rules including another enabled
        // rejection
        List<WestgardRuleConfig> allRules = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(allRules);
        when(ruleConfigDAO.update(rule13s)).thenReturn(rule13s);

        // Act
        WestgardRuleConfig updated = ruleConfigService.updateRuleConfig(rule13s);

        // Assert
        assertNotNull("Updated config should not be null", updated);
        assertFalse("Rule should be disabled", updated.getEnabled());
        verify(ruleConfigDAO, times(1)).update(rule13s);
    }

    /**
     * Test: Apply BASIC preset (only 1₃ₛ enabled) Task Reference: T066
     *
     * BASIC preset: Single rejection rule for critical violations
     */
    @Test
    public void testApplyPreset_Basic_ShouldEnableOnly13s() {
        // Arrange
        List<WestgardRuleConfig> allRules = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(allRules);
        when(ruleConfigDAO.update(any(WestgardRuleConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.applyPreset("1", "1", "BASIC");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return all 4 rules", 4, result.size());

        // Verify only 1₃ₛ is enabled
        long enabledCount = result.stream().filter(WestgardRuleConfig::getEnabled).count();
        assertEquals("Only 1 rule should be enabled", 1, enabledCount);

        WestgardRuleConfig enabled13s = result.stream().filter(r -> r.getRuleCode().equals("1₃ₛ")).findFirst()
                .orElse(null);
        assertNotNull("1₃ₛ rule should exist", enabled13s);
        assertTrue("1₃ₛ should be enabled", enabled13s.getEnabled());

        verify(ruleConfigDAO, times(1)).findByTestAndInstrument("1", "1");
        verify(ruleConfigDAO, times(4)).update(any(WestgardRuleConfig.class));
    }

    /**
     * Test: Apply STANDARD preset (1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ enabled) Task Reference: T066
     *
     * STANDARD preset: Recommended multi-rule approach
     */
    @Test
    public void testApplyPreset_Standard_ShouldEnableRecommendedRules() {
        // Arrange
        List<WestgardRuleConfig> allRules = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(allRules);
        when(ruleConfigDAO.update(any(WestgardRuleConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.applyPreset("1", "1", "STANDARD");

        // Assert
        assertNotNull("Result should not be null", result);

        // Verify 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ are enabled
        long enabledCount = result.stream().filter(WestgardRuleConfig::getEnabled).count();
        assertEquals("4 rules should be enabled", 4, enabledCount);

        List<String> expectedEnabled = Arrays.asList("1₃ₛ", "2₂ₛ", "R₄ₛ", "4₁ₛ");
        for (String ruleCode : expectedEnabled) {
            WestgardRuleConfig rule = result.stream().filter(r -> r.getRuleCode().equals(ruleCode)).findFirst()
                    .orElse(null);
            assertNotNull(ruleCode + " rule should exist", rule);
            assertTrue(ruleCode + " should be enabled", rule.getEnabled());
        }

        verify(ruleConfigDAO, times(1)).findByTestAndInstrument("1", "1");
        verify(ruleConfigDAO, times(4)).update(any(WestgardRuleConfig.class));
    }

    /**
     * Test: Apply COMPREHENSIVE preset (all 8 rules enabled) Task Reference: T066
     *
     * COMPREHENSIVE preset: Maximum sensitivity
     */
    @Test
    public void testApplyPreset_Comprehensive_ShouldEnableAllRules() {
        // Arrange - Add remaining 4 rules (1₂ₛ, 10ₓ, 3₁ₛ, 7ₜ)
        WestgardRuleConfig rule12s = createRule("5", "1", "1", "1₂ₛ", false, "WARNING");
        WestgardRuleConfig rule10x = createRule("6", "1", "1", "10ₓ", false, "WARNING");
        WestgardRuleConfig rule31s = createRule("7", "1", "1", "3₁ₛ", false, "WARNING");
        WestgardRuleConfig rule7t = createRule("8", "1", "1", "7ₜ", false, "WARNING");

        List<WestgardRuleConfig> allRules = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s, rule12s, rule10x, rule31s,
                rule7t);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(allRules);
        when(ruleConfigDAO.update(any(WestgardRuleConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.applyPreset("1", "1", "COMPREHENSIVE");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return all 8 rules", 8, result.size());

        // Verify ALL rules are enabled
        long enabledCount = result.stream().filter(WestgardRuleConfig::getEnabled).count();
        assertEquals("All 8 rules should be enabled", 8, enabledCount);

        verify(ruleConfigDAO, times(1)).findByTestAndInstrument("1", "1");
        verify(ruleConfigDAO, times(8)).update(any(WestgardRuleConfig.class));
    }

    /**
     * Test: Invalid preset name Task Reference: T066
     */
    @Test(expected = IllegalArgumentException.class)
    public void testApplyPreset_WithInvalidPreset_ShouldThrowException() {
        // Act - should throw IllegalArgumentException before DAO call
        ruleConfigService.applyPreset("1", "1", "INVALID_PRESET");
    }

    /**
     * Test: Validate rule configuration - at least one rejection rule required Task
     * Reference: T066
     *
     * FR-021 from spec.md: At least one REJECTION-level rule must be enabled
     */
    @Test
    public void testValidateRuleConfig_WithAtLeastOneRejectionRule_ShouldPass() {
        // Arrange - 1₃ₛ and R₄ₛ are REJECTION rules
        List<WestgardRuleConfig> configs = Arrays.asList(rule13s, rule22s, ruleR4s);

        // Act & Assert - should not throw exception (implicit JUnit behavior)
        ruleConfigService.validateRuleConfig(configs);
    }

    /**
     * Test: Validate rule configuration - no rejection rules enabled Task
     * Reference: T066
     *
     * FR-021 violation: No REJECTION-level rules enabled
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateRuleConfig_WithNoRejectionRules_ShouldThrowException() {
        // Arrange - Only WARNING rules enabled
        rule22s.setEnabled(true);
        List<WestgardRuleConfig> configs = Arrays.asList(rule22s); // Only WARNING rule

        // Act - should throw IllegalArgumentException
        ruleConfigService.validateRuleConfig(configs);
    }

    /**
     * Test: Validate rule configuration - all rules disabled Task Reference: T066
     *
     * FR-021 violation: No rules enabled at all
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateRuleConfig_WithAllRulesDisabled_ShouldThrowException() {
        // Arrange - All rules disabled
        rule13s.setEnabled(false);
        rule22s.setEnabled(false);
        ruleR4s.setEnabled(false);
        rule41s.setEnabled(false);
        List<WestgardRuleConfig> configs = Arrays.asList(rule13s, rule22s, ruleR4s, rule41s);

        // Act - should throw IllegalArgumentException
        ruleConfigService.validateRuleConfig(configs);
    }

    /**
     * Test: Validate rule configuration - invalid rule code Task Reference: T066
     *
     * Valid codes: 1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateRuleConfig_WithInvalidRuleCode_ShouldThrowException() {
        // Arrange - Invalid rule code
        WestgardRuleConfig invalidRule = createRule("99", "1", "1", "INVALID_CODE", true, "REJECTION");
        List<WestgardRuleConfig> configs = Arrays.asList(invalidRule);

        // Act - should throw IllegalArgumentException
        ruleConfigService.validateRuleConfig(configs);
    }

    /**
     * Test: Create default rule configurations Task Reference: T066
     *
     * Should create all 8 rules with STANDARD preset applied
     */
    @Test
    public void testCreateDefaultConfig_ShouldCreateAllRulesWithStandardPreset() {
        // Arrange - capture inserted configs so we can verify their state
        List<WestgardRuleConfig> captured = new java.util.ArrayList<>();
        when(ruleConfigDAO.insert(any(WestgardRuleConfig.class))).thenAnswer(invocation -> {
            WestgardRuleConfig rule = invocation.getArgument(0);
            captured.add(rule);
            return rule.getId();
        });
        when(ruleConfigDAO.get(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return captured.stream().filter(r -> r.getId().equals(id)).findFirst();
        });

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.createDefaultConfig("1", "1");

        // Assert — all 8 rules created
        assertEquals("Should create 8 rules", 8, result.size());
        verify(ruleConfigDAO, times(8)).insert(any(WestgardRuleConfig.class));

        // Assert — correct test/instrument assignment
        for (WestgardRuleConfig config : result) {
            assertEquals("1", config.getTestId());
            assertEquals("1", config.getInstrumentId());
            assertNotNull("ID should be set", config.getId());
        }

        // Assert — all 8 rule codes present
        List<String> expectedCodes = Arrays.asList("1₂ₛ", "1₃ₛ", "2₂ₛ", "R₄ₛ", "4₁ₛ", "10ₓ", "3₁ₛ", "7ₜ");
        List<String> actualCodes = result.stream().map(WestgardRuleConfig::getRuleCode)
                .collect(java.util.stream.Collectors.toList());
        assertTrue("All 8 rule codes should be present", actualCodes.containsAll(expectedCodes));

        // Assert — STANDARD preset: only 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ enabled
        List<String> standardEnabled = Arrays.asList("1₃ₛ", "2₂ₛ", "R₄ₛ", "4₁ₛ");
        for (WestgardRuleConfig config : result) {
            boolean shouldBeEnabled = standardEnabled.contains(config.getRuleCode());
            assertEquals(config.getRuleCode() + " enabled state should match STANDARD preset", shouldBeEnabled,
                    config.getEnabled());
        }

        // Assert — correct severity per rule
        assertRuleSeverity(result, "1₂ₛ", "WARNING");
        assertRuleSeverity(result, "1₃ₛ", "REJECTION");
        assertRuleSeverity(result, "2₂ₛ", "WARNING");
        assertRuleSeverity(result, "R₄ₛ", "REJECTION");
        assertRuleSeverity(result, "4₁ₛ", "REJECTION");
        assertRuleSeverity(result, "10ₓ", "WARNING");
        assertRuleSeverity(result, "3₁ₛ", "WARNING");
        assertRuleSeverity(result, "7ₜ", "WARNING");

        // Assert — REJECTION rules require corrective action, WARNING rules do not
        for (WestgardRuleConfig config : result) {
            boolean expectCorrective = "REJECTION".equals(config.getSeverity());
            assertEquals(config.getRuleCode() + " requiresCorrectiveAction should match severity", expectCorrective,
                    config.getRequiresCorrectiveAction());
        }
    }

    @Test
    public void testCreateDefaultConfig_ShouldNotDuplicateExistingRuleCodes() {
        // Arrange — each call creates new UUIDs, so we verify uniqueness
        List<WestgardRuleConfig> captured = new java.util.ArrayList<>();
        when(ruleConfigDAO.insert(any(WestgardRuleConfig.class))).thenAnswer(invocation -> {
            WestgardRuleConfig rule = invocation.getArgument(0);
            captured.add(rule);
            return rule.getId();
        });
        when(ruleConfigDAO.get(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return captured.stream().filter(r -> r.getId().equals(id)).findFirst();
        });

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.createDefaultConfig("1", "1");

        // Assert — no duplicate rule codes
        long distinctCodes = result.stream().map(WestgardRuleConfig::getRuleCode).distinct().count();
        assertEquals("All 8 rule codes should be unique", 8, distinctCodes);

        // Assert — all IDs unique
        long distinctIds = result.stream().map(WestgardRuleConfig::getId).distinct().count();
        assertEquals("All 8 IDs should be unique", 8, distinctIds);
    }

    private void assertRuleSeverity(List<WestgardRuleConfig> configs, String ruleCode, String expectedSeverity) {
        WestgardRuleConfig config = configs.stream().filter(r -> r.getRuleCode().equals(ruleCode)).findFirst()
                .orElse(null);
        assertNotNull(ruleCode + " should exist in configs", config);
        assertEquals(ruleCode + " severity", expectedSeverity, config.getSeverity());
    }

    /**
     * Test: Update rule configuration with validation Task Reference: T066
     *
     * Should validate before updating
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRuleConfig_DisablingLastRejectionRule_ShouldThrowException() {
        // Arrange - Only one rejection rule enabled (1₃ₛ), try to disable it
        rule13s.setEnabled(false);
        rule22s.setEnabled(true); // Only WARNING rule would remain

        List<WestgardRuleConfig> remainingRules = Arrays.asList(rule22s);
        when(ruleConfigDAO.findByTestAndInstrument("1", "1")).thenReturn(remainingRules);

        // Act - should throw IllegalArgumentException during validation
        ruleConfigService.updateRuleConfig(rule13s);
    }

    /**
     * Test: Find rules by test and instrument - empty result Task Reference: T066
     */
    @Test
    public void testFindByTestAndInstrument_WithNoRules_ShouldReturnEmptyList() {
        // Arrange
        when(ruleConfigDAO.findByTestAndInstrument("99", "99")).thenReturn(Arrays.asList());

        // Act
        List<WestgardRuleConfig> result = ruleConfigService.findByTestAndInstrument("99", "99");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return empty list", 0, result.size());
        verify(ruleConfigDAO, times(1)).findByTestAndInstrument("99", "99");
    }
}
