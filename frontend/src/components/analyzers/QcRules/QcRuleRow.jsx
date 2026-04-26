import React from "react";
import { Dropdown, TextInput, Toggle, IconButton } from "@carbon/react";
import { TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";

const RULE_TYPE_OPTIONS = [
  { id: "FIELD_EQUALS", text: "Field Equals" },
  { id: "SPECIMEN_ID_PREFIX", text: "Specimen ID Prefix" },
  { id: "SPECIMEN_ID_PATTERN", text: "Specimen ID Pattern" },
  { id: "FIELD_CONTAINS", text: "Field Contains" },
];

const NEEDS_TARGET_FIELD = ["FIELD_EQUALS", "FIELD_CONTAINS"];

const QcRuleRow = ({ rule, index, onChange, onDelete, disabled }) => {
  const intl = useIntl();

  const handleChange = (field, value) => {
    onChange(index, { ...rule, [field]: value });
  };

  const showTargetField = NEEDS_TARGET_FIELD.includes(rule.ruleType);

  return (
    <div
      style={{
        display: "flex",
        alignItems: "flex-end",
        gap: "0.75rem",
        marginBottom: "0.75rem",
        padding: "0.75rem",
        background: "var(--cds-layer-01, #f4f4f4)",
        borderRadius: "4px",
      }}
      data-testid={`qc-rule-row-${index}`}
    >
      <div style={{ flex: "0 0 200px" }}>
        <Dropdown
          id={`qc-rule-type-${index}`}
          titleText={
            index === 0
              ? intl.formatMessage({ id: "analyzer.qcRules.ruleType" })
              : ""
          }
          label={intl.formatMessage({ id: "analyzer.qcRules.ruleType" })}
          items={RULE_TYPE_OPTIONS}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={RULE_TYPE_OPTIONS.find((o) => o.id === rule.ruleType)}
          onChange={({ selectedItem }) =>
            handleChange("ruleType", selectedItem?.id || "")
          }
          disabled={disabled}
          size="sm"
        />
      </div>

      {showTargetField && (
        <div style={{ flex: "0 0 120px" }}>
          <TextInput
            id={`qc-rule-field-${index}`}
            labelText={
              index === 0
                ? intl.formatMessage({
                    id: "analyzer.qcRules.targetField",
                  })
                : ""
            }
            placeholder={intl.formatMessage({
              id: "analyzer.qcRules.targetField.placeholder",
            })}
            value={rule.targetField || ""}
            onChange={(e) => handleChange("targetField", e.target.value)}
            disabled={disabled}
            size="sm"
            style={{ fontFamily: "monospace" }}
          />
        </div>
      )}

      <div style={{ flex: "1 1 auto" }}>
        <TextInput
          id={`qc-rule-operand-${index}`}
          labelText={
            index === 0
              ? intl.formatMessage({ id: "analyzer.qcRules.operand" })
              : ""
          }
          placeholder={intl.formatMessage({
            id: "analyzer.qcRules.operand.placeholder",
          })}
          value={rule.operand || ""}
          onChange={(e) => handleChange("operand", e.target.value)}
          disabled={disabled}
          size="sm"
        />
      </div>

      <div style={{ flex: "0 0 auto" }}>
        <Toggle
          id={`qc-rule-active-${index}`}
          labelText=""
          labelA=""
          labelB=""
          toggled={rule.isActive !== false}
          onToggle={(checked) => handleChange("isActive", checked)}
          disabled={disabled}
          size="sm"
        />
      </div>

      <div style={{ flex: "0 0 auto" }}>
        <IconButton
          label={intl.formatMessage({ id: "analyzer.qcRules.deleteRule" })}
          kind="ghost"
          size="sm"
          onClick={() => onDelete(index)}
          disabled={disabled}
          data-testid={`qc-rule-delete-${index}`}
        >
          <TrashCan />
        </IconButton>
      </div>
    </div>
  );
};

QcRuleRow.propTypes = {
  rule: PropTypes.shape({
    ruleType: PropTypes.string,
    targetField: PropTypes.string,
    operand: PropTypes.string,
    isActive: PropTypes.bool,
  }).isRequired,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

export default QcRuleRow;
