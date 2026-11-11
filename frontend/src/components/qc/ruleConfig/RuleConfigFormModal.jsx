/**
 * RuleConfigFormModal Component
 *
 * Shared modal form for creating and editing Westgard rule configurations.
 * Used by RuleConfigPanel for both the "Edit" action on existing configs
 * and the "Configure" action on unconfigured control-lot mappings.
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Toggle,
  RadioButtonGroup,
  RadioButton,
  Accordion,
  AccordionItem,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";

// Default Westgard rules — used as initial state for create mode
const DEFAULT_RULES = [
  {
    code: "1₂ₛ",
    name: "1:2s",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
  {
    code: "1₃ₛ",
    name: "1:3s",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "2₂ₛ",
    name: "2:2s",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "WARNING",
  },
  {
    code: "R₄ₛ",
    name: "R:4s",
    category: "rejection",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "4₁ₛ",
    name: "4:1s",
    category: "warning",
    defaultEnabled: true,
    defaultSeverity: "REJECTION",
  },
  {
    code: "10ₓ",
    name: "10:x",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
  {
    code: "3₁ₛ",
    name: "3:1s",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
  {
    code: "7ₜ",
    name: "7:T",
    category: "warning",
    defaultEnabled: false,
    defaultSeverity: "WARNING",
  },
];

const RuleConfigFormModal = ({
  open,
  testId,
  instrumentId,
  analyzerName,
  testName,
  isNew,
  onClose,
  onSave,
}) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [serverConfigIds, setServerConfigIds] = useState({});
  const [ruleConfigs, setRuleConfigs] = useState([]);
  const [hasChanges, setHasChanges] = useState(false);

  // Build initial defaults
  const buildDefaults = useCallback(
    () =>
      DEFAULT_RULES.map((rule) => ({
        ...rule,
        enabled: rule.defaultEnabled,
        severity: rule.defaultSeverity,
      })),
    [],
  );

  // Load existing config for edit mode
  const loadConfig = useCallback(() => {
    if (!open || !testId || !instrumentId) return;

    if (isNew) {
      setRuleConfigs(buildDefaults());
      setServerConfigIds({});
      setHasChanges(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/qc/ruleConfig?testId=${testId}&instrumentId=${instrumentId}`,
      (response) => {
        const configs = Array.isArray(response) ? response : [];
        if (configs.length > 0) {
          const idMap = {};
          configs.forEach((c) => {
            idMap[c.ruleCode] = c.id;
          });
          setServerConfigIds(idMap);

          setRuleConfigs(
            DEFAULT_RULES.map((defaultRule) => {
              const apiConfig = configs.find(
                (c) => c.ruleCode === defaultRule.code,
              );
              return {
                ...defaultRule,
                enabled: apiConfig
                  ? apiConfig.enabled
                  : defaultRule.defaultEnabled,
                severity: apiConfig
                  ? apiConfig.severity
                  : defaultRule.defaultSeverity,
              };
            }),
          );
        } else {
          setRuleConfigs(buildDefaults());
          setServerConfigIds({});
        }
        setLoading(false);
        setHasChanges(false);
      },
    );
  }, [open, testId, instrumentId, isNew, buildDefaults]);

  useEffect(() => {
    if (open) {
      setError(null);
      loadConfig();
    }
  }, [open, loadConfig]);

  const handleRuleToggle = (ruleCode, enabled) => {
    setRuleConfigs((prev) =>
      prev.map((rule) =>
        rule.code === ruleCode ? { ...rule, enabled } : rule,
      ),
    );
    setHasChanges(true);
  };

  const handleSeverityChange = (ruleCode, severity) => {
    setRuleConfigs((prev) =>
      prev.map((rule) =>
        rule.code === ruleCode ? { ...rule, severity } : rule,
      ),
    );
    setHasChanges(true);
  };

  const handleSave = () => {
    setSaving(true);
    setError(null);

    if (isNew && Object.keys(serverConfigIds).length === 0) {
      // Create mode: POST defaults first, then apply customisations
      postToOpenElisServerFullResponse(
        `/rest/qc/ruleConfig/defaults?testId=${testId}&instrumentId=${instrumentId}`,
        JSON.stringify({}),
        (response) => {
          if (response.ok) {
            response.json().then((created) => {
              if (!Array.isArray(created) || created.length === 0) {
                setSaving(false);
                onSave();
                return;
              }
              // Build ID map from created configs
              const idMap = {};
              created.forEach((c) => {
                idMap[c.ruleCode] = c.id;
              });
              // Apply any customisations the user made
              applyCustomisations(idMap);
            });
          } else {
            setError(
              intl.formatMessage({ id: "qc.ruleConfig.error.saveFailed" }),
            );
            setSaving(false);
          }
        },
      );
    } else {
      // Edit mode: PUT each changed rule
      applyCustomisations(serverConfigIds);
    }
  };

  const applyCustomisations = (idMap) => {
    const toUpdate = ruleConfigs.filter((rule) => idMap[rule.code]);

    if (toUpdate.length === 0) {
      setSaving(false);
      onSave();
      return;
    }

    let completed = 0;
    let failed = false;

    toUpdate.forEach((rule) => {
      const configId = idMap[rule.code];
      putToOpenElisServerFullResponse(
        `/rest/qc/ruleConfig/${configId}`,
        JSON.stringify({ enabled: rule.enabled, severity: rule.severity }),
        (response) => {
          completed++;
          if (!response.ok) {
            failed = true;
          }
          if (completed === toUpdate.length) {
            if (failed) {
              setError(
                intl.formatMessage({ id: "qc.ruleConfig.error.saveFailed" }),
              );
              setSaving(false);
            } else {
              setSaving(false);
              onSave();
            }
          }
        },
      );
    });
  };

  const rejectionRules = ruleConfigs.filter((r) => r.category === "rejection");
  const warningRules = ruleConfigs.filter((r) => r.category === "warning");

  const renderRuleItem = (rule) => (
    <div
      key={rule.code}
      className={`rule-config-item ${rule.enabled ? "rule-config-item--enabled" : ""}`}
      data-testid={`rule-config-item-${rule.code}`}
    >
      <div className="rule-config-item-header">
        <div className="rule-config-item-info">
          <span className="rule-config-item-name">{rule.name}</span>
          <span className="rule-config-item-description">
            {intl.formatMessage({
              id: `qc.rules.${rule.code}.description`,
            })}
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
            legendText={intl.formatMessage({
              id: "qc.ruleConfig.field.severity",
            })}
            valueSelected={rule.severity}
            onChange={(value) => handleSeverityChange(rule.code, value)}
            orientation="horizontal"
            data-testid={`rule-severity-${rule.code}`}
          >
            <RadioButton
              id={`${rule.code}-warning`}
              value="WARNING"
              labelText={intl.formatMessage({
                id: "qc.ruleConfig.severity.warning",
              })}
            />
            <RadioButton
              id={`${rule.code}-rejection`}
              value="REJECTION"
              labelText={intl.formatMessage({
                id: "qc.ruleConfig.severity.rejection",
              })}
            />
          </RadioButtonGroup>
        </div>
      )}
    </div>
  );

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="lg"
      data-testid="rule-config-form-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: isNew
            ? "qc.ruleConfig.modal.title.create"
            : "qc.ruleConfig.modal.title.edit",
        })}
        label={`${analyzerName || ""} — ${testName || ""}`}
        data-testid="rule-config-form-modal-header"
      />
      <ModalBody data-testid="rule-config-form-modal-body">
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "qc.ruleConfig.error.title" })}
            subtitle={error}
            onClose={() => setError(null)}
          />
        )}

        {loading ? (
          <Loading
            description={intl.formatMessage({ id: "qc.ruleConfig.loading" })}
            withOverlay={false}
          />
        ) : (
          <Accordion>
            <AccordionItem
              title={
                <div className="rule-config-category-title">
                  <span>
                    {intl.formatMessage({
                      id: "qc.ruleConfig.category.rejection",
                    })}
                  </span>
                  <span className="rule-config-category-count">
                    ({rejectionRules.filter((r) => r.enabled).length}/
                    {rejectionRules.length})
                  </span>
                </div>
              }
              open
              data-testid="rule-config-rejection-section"
            >
              {rejectionRules.map(renderRuleItem)}
            </AccordionItem>

            <AccordionItem
              title={
                <div className="rule-config-category-title">
                  <span>
                    {intl.formatMessage({
                      id: "qc.ruleConfig.category.warning",
                    })}
                  </span>
                  <span className="rule-config-category-count">
                    ({warningRules.filter((r) => r.enabled).length}/
                    {warningRules.length})
                  </span>
                </div>
              }
              data-testid="rule-config-warning-section"
            >
              {warningRules.map(renderRuleItem)}
            </AccordionItem>
          </Accordion>
        )}
      </ModalBody>
      <ModalFooter data-testid="rule-config-form-modal-footer">
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="rule-config-cancel-button"
        >
          {intl.formatMessage({ id: "button.cancel" })}
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={!hasChanges || saving || loading}
          data-testid="rule-config-save-button"
        >
          {saving
            ? intl.formatMessage({ id: "button.saving" })
            : intl.formatMessage({ id: "button.save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default RuleConfigFormModal;
