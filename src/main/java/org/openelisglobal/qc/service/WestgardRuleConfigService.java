package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qc.dto.RuleConfigSummary;
import org.openelisglobal.qc.dto.UnconfiguredMapping;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Service interface for Westgard Rule Configuration management. Supports User
 * Story 5: Configure Westgard Rules
 */
public interface WestgardRuleConfigService extends BaseObjectService<WestgardRuleConfig, String> {

    /**
     * Find all rule configurations for a specific test and instrument.
     *
     * @param testId       The test ID
     * @param instrumentId The instrument ID
     * @return List of all 8 rule configurations
     */
    List<WestgardRuleConfig> findByTestAndInstrument(String testId, String instrumentId);

    /**
     * Find all enabled rules for a test and instrument.
     *
     * @param testId       The test ID
     * @param instrumentId The instrument ID
     * @return List of enabled rule configurations
     */
    List<WestgardRuleConfig> findEnabledByTestAndInstrument(String testId, String instrumentId);

    /**
     * Update rule configuration (enable/disable, change parameters).
     *
     * @param config The updated rule configuration
     * @return The updated configuration
     */
    WestgardRuleConfig updateRuleConfig(WestgardRuleConfig config);

    /**
     * Apply preset rule configuration.
     *
     * Presets: - BASIC: 1₃ₛ only (single rejection rule for critical violations) -
     * STANDARD: 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ (recommended multi-rule approach) -
     * COMPREHENSIVE: All 8 rules enabled (maximum sensitivity)
     *
     * @param testId       The test ID
     * @param instrumentId The instrument ID
     * @param preset       The preset name (BASIC, STANDARD, COMPREHENSIVE)
     * @return List of updated configurations
     * @throws IllegalArgumentException if preset name is invalid
     */
    List<WestgardRuleConfig> applyPreset(String testId, String instrumentId, String preset)
            throws IllegalArgumentException;

    /**
     * Validate rule configuration.
     *
     * Validation rules: - At least one REJECTION-level rule must be enabled (FR-021
     * from spec.md) - Rule codes must be valid (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ,
     * 7ₜ)
     *
     * @param configs List of rule configurations to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateRuleConfig(List<WestgardRuleConfig> configs) throws IllegalArgumentException;

    /**
     * Create default rule configurations for a new test-instrument combination.
     *
     * Creates all 8 rules with STANDARD preset applied.
     *
     * @param testId       The test ID
     * @param instrumentId The instrument ID
     * @return List of created configurations
     */
    List<WestgardRuleConfig> createDefaultConfig(String testId, String instrumentId);

    /**
     * Get summaries of all rule configurations grouped by (test, instrument) pair.
     *
     * @return List of summaries with resolved names and rule details
     */
    List<RuleConfigSummary> getAllRuleConfigSummaries();

    /**
     * Get control-lot (test, instrument) mappings that have no rule configurations.
     *
     * @return List of unconfigured mappings with resolved names
     */
    List<UnconfiguredMapping> getUnconfiguredMappings();
}
