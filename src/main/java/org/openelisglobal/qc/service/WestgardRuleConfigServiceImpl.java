package org.openelisglobal.qc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.WestgardRuleConfigDAO;
import org.openelisglobal.qc.dto.RuleConfigSummary;
import org.openelisglobal.qc.dto.TestInstrumentPair;
import org.openelisglobal.qc.dto.UnconfiguredMapping;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
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

    /**
     * System user ID used for automated default config creation (no user session).
     */
    private static final int SYSTEM_AUTOMATION_USER_ID = 1;

    @Autowired
    private WestgardRuleConfigDAO ruleConfigDAO;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private TestService testService;

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
    public List<WestgardRuleConfig> findByTestAndInstrument(String testId, String instrumentId) {
        return ruleConfigDAO.findByTestAndInstrument(testId, instrumentId);
    }

    /**
     * Find all enabled rules for a test and instrument.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WestgardRuleConfig> findEnabledByTestAndInstrument(String testId, String instrumentId) {
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
    public List<WestgardRuleConfig> applyPreset(String testId, String instrumentId, String preset)
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
    public List<WestgardRuleConfig> createDefaultConfig(String testId, String instrumentId) {
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

            // Set system user ID for audit trail (automated — no user session)
            config.setSysUserId(String.valueOf(SYSTEM_AUTOMATION_USER_ID));
            config.setSystemUserId(SYSTEM_AUTOMATION_USER_ID);

            String id = ruleConfigDAO.insert(config);
            defaultConfigs.add(ruleConfigDAO.get(id).get());
        }

        LogEvent.logInfo(this.getClass().getName(), "createDefaultConfig", "Created default rule config for test "
                + testId + ", instrument " + instrumentId + " (STANDARD preset)");

        return defaultConfigs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleConfigSummary> getAllRuleConfigSummaries() {
        List<TestInstrumentPair> pairs = ruleConfigDAO.findDistinctTestInstrumentPairs();

        // Pre-fetch analyzer and test names to avoid N+1
        Map<String, String> analyzerNames = new HashMap<>();
        Map<String, String> testNames = new HashMap<>();
        for (TestInstrumentPair pair : pairs) {
            analyzerNames.computeIfAbsent(pair.getInstrumentId(), this::resolveAnalyzerName);
            testNames.computeIfAbsent(pair.getTestId(), this::resolveTestName);
        }

        List<RuleConfigSummary> summaries = new ArrayList<>();
        for (TestInstrumentPair pair : pairs) {
            List<WestgardRuleConfig> configs = ruleConfigDAO.findByTestAndInstrument(pair.getTestId(),
                    pair.getInstrumentId());

            RuleConfigSummary summary = new RuleConfigSummary();
            summary.setTestId(pair.getTestId());
            summary.setInstrumentId(pair.getInstrumentId());
            summary.setTestName(testNames.get(pair.getTestId()));
            summary.setInstrumentName(analyzerNames.get(pair.getInstrumentId()));
            summary.setTotalRuleCount(configs.size());
            summary.setEnabledRuleCount((int) configs.stream().filter(WestgardRuleConfig::getEnabled).count());

            List<RuleConfigSummary.RuleConfigDetail> details = configs.stream().map(c -> {
                RuleConfigSummary.RuleConfigDetail detail = new RuleConfigSummary.RuleConfigDetail();
                detail.setId(c.getId());
                detail.setRuleCode(c.getRuleCode());
                detail.setEnabled(c.getEnabled());
                detail.setSeverity(c.getSeverity());
                return detail;
            }).collect(Collectors.toList());
            summary.setRules(details);

            summaries.add(summary);
        }
        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnconfiguredMapping> getUnconfiguredMappings() {
        Set<TestInstrumentPair> controlLotPairs = new HashSet<>(controlLotDAO.findDistinctTestInstrumentPairs());
        Set<TestInstrumentPair> configuredPairs = new HashSet<>(ruleConfigDAO.findDistinctTestInstrumentPairs());

        // Set difference: control lot pairs that have no rule configs
        controlLotPairs.removeAll(configuredPairs);

        List<UnconfiguredMapping> mappings = new ArrayList<>();
        for (TestInstrumentPair pair : controlLotPairs) {
            UnconfiguredMapping mapping = new UnconfiguredMapping();
            mapping.setTestId(pair.getTestId());
            mapping.setInstrumentId(pair.getInstrumentId());
            mapping.setTestName(resolveTestName(pair.getTestId()));
            mapping.setInstrumentName(resolveAnalyzerName(pair.getInstrumentId()));

            long activeCount = controlLotDAO.countActiveByTestAndInstrument(pair.getTestId(), pair.getInstrumentId());
            mapping.setActiveControlLotCount((int) activeCount);

            mappings.add(mapping);
        }
        return mappings;
    }

    private String resolveAnalyzerName(String instrumentId) {
        try {
            Optional<Analyzer> analyzer = analyzerService.getWithType(String.valueOf(instrumentId));
            return analyzer.map(Analyzer::getName).orElse("Analyzer " + instrumentId);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "resolveAnalyzerName",
                    "Could not load analyzer " + instrumentId + ": " + e.getMessage());
            return "Analyzer " + instrumentId;
        }
    }

    private String resolveTestName(String testId) {
        try {
            Test test = testService.getTestById(String.valueOf(testId));
            if (test != null && test.getDescription() != null) {
                return test.getDescription();
            }
            return "Test " + testId;
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "resolveTestName",
                    "Could not load test " + testId + ": " + e.getMessage());
            return "Test " + testId;
        }
    }
}
