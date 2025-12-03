/**
 * RuleConfigPanel Component
 *
 * Panel for configuring Westgard rules per analyzer/test combination
 * Task Reference: T074
 * Specification: FR-015 to FR-021, User Story 7
 *
 * Features:
 * - Enable/disable individual Westgard rules
 * - Configure rule severity (Warning vs Rejection)
 * - Apply to specific analyzer/test combinations
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Tile,
  Toggle,
  Dropdown,
  RadioButtonGroup,
  RadioButton,
  Button,
  Loading,
  InlineNotification,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Save, Reset } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useParams, useHistory } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./RuleConfigPanel.css";

// Default Westgard rules configuration
const DEFAULT_RULES = [
  {
    code: "1_2s",
    name: "1:2s",
    description: "One control exceeds ±2SD",
    category: "warning",
    defaultEnabled: true,
    defaultSeverity: "WARNING",
  },
  {
    code: "1_3s",
    name: "1:3s",
    description: "One control exceeds ±3SD",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "2_2s",
    name: "2:2s",
    description: "Two consecutive controls exceed ±2SD in same direction",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "R_4s",
    name: "R:4s",
    description: "Range between two controls exceeds 4SD",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "4_1s",
    name: "4:1s",
    description: "Four consecutive controls exceed ±1SD in same direction",
    category: "warning",
    defaultEnabled: true,
    defaultSeverity: "WARNING",
  },
  {
    code: "10_x",
    name: "10:x",
    description: "Ten consecutive controls on same side of mean",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
  {
    code: "3_1s",
    name: "3:1s",
    description: "Three consecutive controls exceed ±1SD in same direction",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
  {
    code: "7_t",
    name: "7:T",
    description: "Seven consecutive controls trending in same direction",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
];

const RuleConfigPanel = () => {
  const intl = useIntl();
  const history = useHistory();
  const { analyzerId, testId } = useParams();

  // State
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [analyzers, setAnalyzers] = useState([]);
  const [tests, setTests] = useState([]);
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(analyzerId || null);
  const [selectedTest, setSelectedTest] = useState(testId || null);
  const [ruleConfigs, setRuleConfigs] = useState(
    DEFAULT_RULES.map((rule) => ({
      ...rule,
      enabled: rule.defaultEnabled,
      severity: rule.defaultSeverity,
    }))
  );
  const [hasChanges, setHasChanges] = useState(false);

  // Load analyzers
  useEffect(() => {
    getFromOpenElisServer("/rest/analyzers", (response) => {
      if (response && Array.isArray(response.data)) {
        setAnalyzers(response.data);
      } else if (Array.isArray(response)) {
        setAnalyzers(response);
      }
    });
  }, []);

  // Load tests when analyzer changes
  const loadTests = useCallback((analyzerId) => {
    if (!analyzerId) {
      setTests([]);
      return;
    }
    getFromOpenElisServer(`/rest/analyzers/${analyzerId}/tests`, (response) => {
      if (response && Array.isArray(response.data)) {
        setTests(response.data);
      } else if (Array.isArray(response)) {
        setTests(response);
      }
    });
  }, []);

  // Load rule configuration for analyzer/test
  const loadRuleConfig = useCallback(() => {
    if (!selectedAnalyzer || !selectedTest) {
      setLoading(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/qc/rule-configs?analyzerId=${selectedAnalyzer}&testId=${selectedTest}`,
      (response) => {
        if (response && response.data) {
          const configs = response.data.rules || response.data;
          if (Array.isArray(configs) && configs.length > 0) {
            // Merge API config with defaults
            setRuleConfigs(
              DEFAULT_RULES.map((defaultRule) => {
                const apiConfig = configs.find((c) => c.ruleCode === defaultRule.code);
                return {
                  ...defaultRule,
                  enabled: apiConfig ? apiConfig.enabled : defaultRule.defaultEnabled,
                  severity: apiConfig ? apiConfig.severity : defaultRule.defaultSeverity,
                };
              })
            );
          } else {
            // Use defaults
            setRuleConfigs(
              DEFAULT_RULES.map((rule) => ({
                ...rule,
                enabled: rule.defaultEnabled,
                severity: rule.defaultSeverity,
              }))
            );
          }
        }
        setLoading(false);
        setHasChanges(false);
      }
    );
  }, [selectedAnalyzer, selectedTest]);

  // Load tests and config when analyzer changes
  useEffect(() => {
    if (selectedAnalyzer) {
      loadTests(selectedAnalyzer);
    }
  }, [selectedAnalyzer, loadTests]);

  // Load config when test changes
  useEffect(() => {
    loadRuleConfig();
  }, [loadRuleConfig]);

  // Handle analyzer change
  const handleAnalyzerChange = ({ selectedItem }) => {
    setSelectedAnalyzer(selectedItem?.id || null);
    setSelectedTest(null);
    setHasChanges(false);
  };

  // Handle test change
  const handleTestChange = ({ selectedItem }) => {
    setSelectedTest(selectedItem?.id || null);
    setHasChanges(false);
  };

  // Handle rule toggle (FR-016)
  const handleRuleToggle = (ruleCode, enabled) => {
    setRuleConfigs((prev) =>
      prev.map((rule) =>
        rule.code === ruleCode ? { ...rule, enabled } : rule
      )
    );
    setHasChanges(true);
  };

  // Handle severity change (FR-018)
  const handleSeverityChange = (ruleCode, severity) => {
    setRuleConfigs((prev) =>
      prev.map((rule) =>
        rule.code === ruleCode ? { ...rule, severity } : rule
      )
    );
    setHasChanges(true);
  };

  // Handle save (FR-017)
  const handleSave = () => {
    if (!selectedAnalyzer || !selectedTest) {
      setError(intl.formatMessage({ id: "qc.ruleConfig.error.selectAnalyzerTest" }));
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);

    const payload = {
      analyzerId: selectedAnalyzer,
      testId: selectedTest,
      rules: ruleConfigs.map((rule) => ({
        ruleCode: rule.code,
        enabled: rule.enabled,
        severity: rule.severity,
      })),
    };

    postToOpenElisServerFullResponse(
      "/rest/qc/rule-configs",
      JSON.stringify(payload),
      (response) => {
        if (response.ok) {
          setSuccess(intl.formatMessage({ id: "qc.ruleConfig.success.saved" }));
          setHasChanges(false);
        } else {
          setError(intl.formatMessage({ id: "qc.ruleConfig.error.saveFailed" }));
        }
        setSaving(false);
      }
    );
  };

  // Handle reset to defaults (FR-019)
  const handleReset = () => {
    setRuleConfigs(
      DEFAULT_RULES.map((rule) => ({
        ...rule,
        enabled: rule.defaultEnabled,
        severity: rule.defaultSeverity,
      }))
    );
    setHasChanges(true);
  };

  // Group rules by category
  const rejectionRules = ruleConfigs.filter((r) => r.category === "rejection");
  const warningRules = ruleConfigs.filter((r) => r.category === "warning");

  return (
    <div className="rule-config-panel" data-testid="rule-config-panel">
      {/* Header */}
      <div className="rule-config-panel-header" data-testid="rule-config-panel-header">
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/analyzers/qc",
            },
            {
              label: intl.formatMessage({ id: "qc.ruleConfig.title" }),
            },
          ]}
          subtitle={intl.formatMessage({ id: "qc.ruleConfig.subtitle" })}
        />
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.ruleConfig.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="rule-config-error"
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "qc.ruleConfig.success.title" })}
          subtitle={success}
          onClose={() => setSuccess(null)}
          data-testid="rule-config-success"
        />
      )}

      {/* Analyzer/Test Selection */}
      <Grid className="rule-config-panel-selectors" data-testid="rule-config-selectors">
        <Column lg={6} md={4} sm={4}>
          <Dropdown
            id="analyzer-selector"
            titleText={intl.formatMessage({ id: "qc.ruleConfig.field.analyzer" })}
            label={intl.formatMessage({ id: "qc.ruleConfig.field.selectAnalyzer" })}
            items={analyzers}
            itemToString={(item) => item?.name || ""}
            selectedItem={analyzers.find((a) => a.id === selectedAnalyzer)}
            onChange={handleAnalyzerChange}
            data-testid="rule-config-analyzer-dropdown"
          />
        </Column>
        <Column lg={6} md={4} sm={4}>
          <Dropdown
            id="test-selector"
            titleText={intl.formatMessage({ id: "qc.ruleConfig.field.test" })}
            label={intl.formatMessage({ id: "qc.ruleConfig.field.selectTest" })}
            items={tests}
            itemToString={(item) => item?.name || ""}
            selectedItem={tests.find((t) => t.id === selectedTest)}
            onChange={handleTestChange}
            disabled={!selectedAnalyzer}
            data-testid="rule-config-test-dropdown"
          />
        </Column>
      </Grid>

      {/* Loading state */}
      {loading && selectedAnalyzer && selectedTest && (
        <div className="rule-config-panel-loading" data-testid="rule-config-loading">
          <Loading
            description={intl.formatMessage({ id: "qc.ruleConfig.loading" })}
            withOverlay={false}
          />
        </div>
      )}

      {/* Rule Configuration */}
      {!loading && selectedAnalyzer && selectedTest && (
        <>
          <Accordion className="rule-config-panel-accordion" data-testid="rule-config-accordion">
            {/* Rejection Rules */}
            <AccordionItem
              title={
                <div className="rule-config-category-title">
                  <span>{intl.formatMessage({ id: "qc.ruleConfig.category.rejection" })}</span>
                  <span className="rule-config-category-count">
                    ({rejectionRules.filter((r) => r.enabled).length}/{rejectionRules.length})
                  </span>
                </div>
              }
              open
              data-testid="rule-config-rejection-section"
            >
              {rejectionRules.map((rule) => (
                <Tile
                  key={rule.code}
                  className={`rule-config-item ${rule.enabled ? "rule-config-item--enabled" : ""}`}
                  data-testid={`rule-config-item-${rule.code}`}
                >
                  <div className="rule-config-item-header">
                    <div className="rule-config-item-info">
                      <span className="rule-config-item-name">{rule.name}</span>
                      <span className="rule-config-item-description">
                        {intl.formatMessage({ id: `qc.rules.${rule.code}.description` })}
                      </span>
                    </div>
                    <Toggle
                      id={`toggle-${rule.code}`}
                      labelA=""
                      labelB=""
                      toggled={rule.enabled}
                      onToggle={(checked) => handleRuleToggle(rule.code, checked)}
                      data-testid={`rule-toggle-${rule.code}`}
                    />
                  </div>
                  {rule.enabled && (
                    <div className="rule-config-item-severity">
                      <RadioButtonGroup
                        name={`severity-${rule.code}`}
                        legendText={intl.formatMessage({ id: "qc.ruleConfig.field.severity" })}
                        valueSelected={rule.severity}
                        onChange={(value) => handleSeverityChange(rule.code, value)}
                        orientation="horizontal"
                        data-testid={`rule-severity-${rule.code}`}
                      >
                        <RadioButton
                          id={`${rule.code}-warning`}
                          value="WARNING"
                          labelText={intl.formatMessage({ id: "qc.ruleConfig.severity.warning" })}
                        />
                        <RadioButton
                          id={`${rule.code}-rejection`}
                          value="REJECTION"
                          labelText={intl.formatMessage({ id: "qc.ruleConfig.severity.rejection" })}
                        />
                      </RadioButtonGroup>
                    </div>
                  )}
                </Tile>
              ))}
            </AccordionItem>

            {/* Warning Rules */}
            <AccordionItem
              title={
                <div className="rule-config-category-title">
                  <span>{intl.formatMessage({ id: "qc.ruleConfig.category.warning" })}</span>
                  <span className="rule-config-category-count">
                    ({warningRules.filter((r) => r.enabled).length}/{warningRules.length})
                  </span>
                </div>
              }
              data-testid="rule-config-warning-section"
            >
              {warningRules.map((rule) => (
                <Tile
                  key={rule.code}
                  className={`rule-config-item ${rule.enabled ? "rule-config-item--enabled" : ""}`}
                  data-testid={`rule-config-item-${rule.code}`}
                >
                  <div className="rule-config-item-header">
                    <div className="rule-config-item-info">
                      <span className="rule-config-item-name">{rule.name}</span>
                      <span className="rule-config-item-description">
                        {intl.formatMessage({ id: `qc.rules.${rule.code}.description` })}
                      </span>
                    </div>
                    <Toggle
                      id={`toggle-${rule.code}`}
                      labelA=""
                      labelB=""
                      toggled={rule.enabled}
                      onToggle={(checked) => handleRuleToggle(rule.code, checked)}
                      data-testid={`rule-toggle-${rule.code}`}
                    />
                  </div>
                  {rule.enabled && (
                    <div className="rule-config-item-severity">
                      <RadioButtonGroup
                        name={`severity-${rule.code}`}
                        legendText={intl.formatMessage({ id: "qc.ruleConfig.field.severity" })}
                        valueSelected={rule.severity}
                        onChange={(value) => handleSeverityChange(rule.code, value)}
                        orientation="horizontal"
                        data-testid={`rule-severity-${rule.code}`}
                      >
                        <RadioButton
                          id={`${rule.code}-warning`}
                          value="WARNING"
                          labelText={intl.formatMessage({ id: "qc.ruleConfig.severity.warning" })}
                        />
                        <RadioButton
                          id={`${rule.code}-rejection`}
                          value="REJECTION"
                          labelText={intl.formatMessage({ id: "qc.ruleConfig.severity.rejection" })}
                        />
                      </RadioButtonGroup>
                    </div>
                  )}
                </Tile>
              ))}
            </AccordionItem>
          </Accordion>

          {/* Actions */}
          <div className="rule-config-panel-actions" data-testid="rule-config-actions">
            <Button
              kind="secondary"
              renderIcon={Reset}
              onClick={handleReset}
              data-testid="rule-config-reset-button"
            >
              {intl.formatMessage({ id: "qc.ruleConfig.action.reset" })}
            </Button>
            <Button
              kind="primary"
              renderIcon={Save}
              onClick={handleSave}
              disabled={!hasChanges || saving}
              data-testid="rule-config-save-button"
            >
              {saving
                ? intl.formatMessage({ id: "button.saving" })
                : intl.formatMessage({ id: "button.save" })}
            </Button>
          </div>
        </>
      )}

      {/* Prompt to select analyzer/test */}
      {!loading && (!selectedAnalyzer || !selectedTest) && (
        <div className="rule-config-panel-prompt" data-testid="rule-config-prompt">
          {intl.formatMessage({ id: "qc.ruleConfig.prompt.selectAnalyzerTest" })}
        </div>
      )}
    </div>
  );
};

export default RuleConfigPanel;
