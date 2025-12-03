package org.openelisglobal.qc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qc.dao.WestgardRuleConfigDAO;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Westgard Rule Configuration management (T071)
 *
 * Supports User Story 5: Configure Westgard Rules Following Constitution IV.5:
 * 
 * @Transactional in services ONLY (NOT controllers)
 */
@Service
public class WestgardRuleConfigServiceImpl extends BaseObjectServiceImpl<WestgardRuleConfig, String>
        implements WestgardRuleConfigService {

    @Autowired
    private WestgardRuleConfigDAO ruleConfigDAO;

    // Valid Westgard rule codes (8 standard rules)
    private static final List<String> VALID_RULE_CODES = Arrays.asList("1₂ₛ", "1₃ₛ", "2₂ₛ", "R₄ₛ", "4₁ₛ", "10ₓ", "3₁ₛ",
            "7ₜ");

    // Preset configurations
    private static final List<String> BASIC_PRESET = Arrays.asList("1₃ₛ");
    private static final List<String> STANDARD_PRESET = Arrays.asList("1₃ₛ", "2₂ₛ", "R₄ₛ", "4₁ₛ");
    private static final List<String> COMPREHENSIVE_PRESET = VALID_RULE_CODES; // All 8 rules

    public WestgardRuleConfigServiceImpl() {
        super(WestgardRuleConfig.class);
    }

    @Override
    protected WestgardRuleConfigDAO getBaseObjectDAO() {
        return ruleConfigDAO;
    }

    /**
     * Find all rule configurations for a specific test and instrument.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WestgardRuleConfig> findByTestAndInstrument(Integer testId, Integer instrumentId) {
        return ruleConfigDAO.findByTestAndInstrument(testId, instrumentId);
    }

    /**
     * Find all enabled rules for a test and instrument.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WestgardRuleConfig> findEnabledByTestAndInstrument(Integer testId, Integer instrumentId) {
        return ruleConfigDAO.findEnabledByTestAndInstrument(testId, instrumentId);
    }

    /**
     * Update rule configuration (enable/disable, change parameters).
     */
    @Override
    @Transactional
    public WestgardRuleConfig updateRuleConfig(WestgardRuleConfig config) {
        // Validate that we're not disabling the last rejection rule
        if (!config.getEnabled() && "REJECTION".equals(config.getSeverity())) {
            List<WestgardRuleConfig> allRules = ruleConfigDAO.findByTestAndInstrument(config.getTestId(),
                    config.getInstrumentId());
            List<WestgardRuleConfig> remainingRejectionRules = allRules.stream().filter(
                    r -> !r.getId().equals(config.getId()) && r.getEnabled() && "REJECTION".equals(r.getSeverity()))
                    .collect(Collectors.toList());

            if (remainingRejectionRules.isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot disable last rejection rule. At least one REJECTION-level rule must be enabled (FR-021).");
            }
        }

        WestgardRuleConfig updated = ruleConfigDAO.update(config);
        LogEvent.logInfo(this.getClass().getName(), "updateRuleConfig",
                "Updated rule config: " + config.getId() + " (enabled=" + config.getEnabled() + ")");
        return updated;
    }

    /**
     * Apply preset rule configuration.
     *
     * Presets: - BASIC: 1₃ₛ only (single rejection rule for critical violations) -
     * STANDARD: 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ (recommended multi-rule approach) -
     * COMPREHENSIVE: All 8 rules enabled (maximum sensitivity)
     */
    @Override
    @Transactional
    public List<WestgardRuleConfig> applyPreset(Integer testId, Integer instrumentId, String preset)
            throws IllegalArgumentException {

        // Validate preset name
        List<String> enabledRules;
        switch (preset.toUpperCase()) {
        case "BASIC":
            enabledRules = BASIC_PRESET;
            break;
        case "STANDARD":
            enabledRules = STANDARD_PRESET;
            break;
        case "COMPREHENSIVE":
            enabledRules = COMPREHENSIVE_PRESET;
            break;
        default:
            throw new IllegalArgumentException(
                    "Invalid preset: " + preset + ". Valid presets: BASIC, STANDARD, COMPREHENSIVE");
        }

        // Get all rules for this test-instrument combination
        List<WestgardRuleConfig> allRules = ruleConfigDAO.findByTestAndInstrument(testId, instrumentId);

        // Update enabled status based on preset
        List<WestgardRuleConfig> updatedRules = new ArrayList<>();
        for (WestgardRuleConfig rule : allRules) {
            boolean shouldEnable = enabledRules.contains(rule.getRuleCode());
            rule.setEnabled(shouldEnable);
            WestgardRuleConfig updated = ruleConfigDAO.update(rule);
            updatedRules.add(updated);
        }

        LogEvent.logInfo(this.getClass().getName(), "applyPreset",
                "Applied " + preset + " preset to test " + testId + ", instrument " + instrumentId);

        return updatedRules;
    }

    /**
     * Validate rule configuration.
     *
     * Validation rules: - At least one REJECTION-level rule must be enabled (FR-021
     * from spec.md) - Rule codes must be valid (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ,
     * 7ₜ)
     */
    @Override
    public void validateRuleConfig(List<WestgardRuleConfig> configs) throws IllegalArgumentException {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("Rule configuration cannot be empty");
        }

        // Validate rule codes
        for (WestgardRuleConfig config : configs) {
            if (!VALID_RULE_CODES.contains(config.getRuleCode())) {
                throw new IllegalArgumentException("Invalid rule code: " + config.getRuleCode() + ". Valid codes: "
                        + String.join(", ", VALID_RULE_CODES));
            }
        }

        // Validate at least one REJECTION-level rule is enabled (FR-021)
        boolean hasEnabledRejectionRule = configs.stream()
                .anyMatch(c -> c.getEnabled() && "REJECTION".equals(c.getSeverity()));

        if (!hasEnabledRejectionRule) {
            throw new IllegalArgumentException(
                    "At least one REJECTION-level rule must be enabled (FR-021 from spec.md)");
        }
    }

    /**
     * Create default rule configurations for a new test-instrument combination.
     *
     * Creates all 8 rules with STANDARD preset applied.
     */
    @Override
    @Transactional
    public List<WestgardRuleConfig> createDefaultConfig(Integer testId, Integer instrumentId) {
        List<WestgardRuleConfig> defaultConfigs = new ArrayList<>();

        // Rule definitions: [code, severity]
        Object[][] ruleDefinitions = { { "1₂ₛ", "WARNING" }, { "1₃ₛ", "REJECTION" }, { "2₂ₛ", "WARNING" },
                { "R₄ₛ", "REJECTION" }, { "4₁ₛ", "REJECTION" }, { "10ₓ", "WARNING" }, { "3₁ₛ", "WARNING" },
                { "7ₜ", "WARNING" } };

        // Create all 8 rules
        for (Object[] ruleDef : ruleDefinitions) {
            String ruleCode = (String) ruleDef[0];
            String severity = (String) ruleDef[1];

            WestgardRuleConfig config = new WestgardRuleConfig();
            config.setId(UUID.randomUUID().toString());
            config.setTestId(testId);
            config.setInstrumentId(instrumentId);
            config.setRuleCode(ruleCode);
            config.setSeverity(severity);

            // Apply STANDARD preset: Enable 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ
            boolean enabled = STANDARD_PRESET.contains(ruleCode);
            config.setEnabled(enabled);

            // REJECTION rules require corrective action
            config.setRequiresCorrectiveAction("REJECTION".equals(severity));

            String id = ruleConfigDAO.insert(config);
            defaultConfigs.add(ruleConfigDAO.get(id).get());
        }

        LogEvent.logInfo(this.getClass().getName(), "createDefaultConfig", "Created default rule config for test "
                + testId + ", instrument " + instrumentId + " (STANDARD preset)");

        return defaultConfigs;
    }
}
