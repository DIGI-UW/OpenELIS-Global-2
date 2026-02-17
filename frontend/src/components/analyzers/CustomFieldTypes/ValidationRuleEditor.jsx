import React, { useState, useEffect } from "react";
import {
  Select,
  SelectItem,
  TextInput,
  NumberInput,
  Button,
  Form,
  FormGroup,
  InlineNotification,
  Grid,
  Column,
  Tag,
  TextArea,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getValidationRules,
  createValidationRule,
  updateValidationRule,
  deleteValidationRule,
} from "../../../services/analyzerService";
import "./ValidationRuleEditor.css";

/**
 * ValidationRuleEditor Component
 *
 * Component for creating and editing validation rules for custom field types.
 * Supports REGEX, RANGE, ENUM, and LENGTH rule types with dynamic form fields.
 *
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 *
 *
 * @param {Object} props - Component props
 * @param {String} props.customFieldTypeId - Custom field type ID
 * @param {Function} props.onSave - Callback when rule is saved (rule) => void
 * @param {Function} props.onCancel - Callback when editor is cancelled () => void
 * @param {Object} props.editingRule - Rule being edited (optional)
 */
const ValidationRuleEditor = ({
  customFieldTypeId,
  onSave,
  onCancel,
  editingRule = null,
}) => {
  const intl = useIntl();

  // State
  const [ruleType, setRuleType] = useState(editingRule?.ruleType || "REGEX");
  const [ruleName, setRuleName] = useState(editingRule?.ruleName || "");
  const [errorMessage, setErrorMessage] = useState(
    editingRule?.errorMessage || "",
  );
  const [isActive, setIsActive] = useState(
    editingRule?.isActive !== undefined ? editingRule.isActive : true,
  );

  // Rule type specific state
  const [regexPattern, setRegexPattern] = useState("");
  const [rangeMin, setRangeMin] = useState("");
  const [rangeMax, setRangeMax] = useState("");
  const [enumValues, setEnumValues] = useState([]);
  const [enumInput, setEnumInput] = useState("");
  const [lengthMin, setLengthMin] = useState("");
  const [lengthMax, setLengthMax] = useState("");

  // Test validation state
  const [testValue, setTestValue] = useState("");
  const [testResult, setTestResult] = useState(null);
  const [testError, setTestError] = useState(null);

  // Form errors
  const [formErrors, setFormErrors] = useState({});
  const [notification, setNotification] = useState(null);

  // Load existing rule data when editing
  useEffect(() => {
    if (editingRule) {
      setRuleType(editingRule.ruleType || "REGEX");
      setRuleName(editingRule.ruleName || "");
      setErrorMessage(editingRule.errorMessage || "");
      setIsActive(
        editingRule.isActive !== undefined ? editingRule.isActive : true,
      );

      // Parse rule expression based on type
      if (editingRule.ruleExpression) {
        try {
          switch (editingRule.ruleType) {
            case "REGEX":
              setRegexPattern(editingRule.ruleExpression);
              break;
            case "RANGE":
              const rangeData = JSON.parse(editingRule.ruleExpression);
              setRangeMin(rangeData.min?.toString() || "");
              setRangeMax(rangeData.max?.toString() || "");
              break;
            case "ENUM":
              const enumData = JSON.parse(editingRule.ruleExpression);
              setEnumValues(Array.isArray(enumData) ? enumData : []);
              break;
            case "LENGTH":
              const lengthData = JSON.parse(editingRule.ruleExpression);
              setLengthMin(lengthData.minLength?.toString() || "");
              setLengthMax(lengthData.maxLength?.toString() || "");
              break;
          }
        } catch (e) {
          // Parse error handled silently — form will show default values
        }
      }
    }
  }, [editingRule]);

  // Build rule expression from form data
  const buildRuleExpression = () => {
    switch (ruleType) {
      case "REGEX":
        return regexPattern;
      case "RANGE":
        return JSON.stringify({
          min: rangeMin ? parseFloat(rangeMin) : null,
          max: rangeMax ? parseFloat(rangeMax) : null,
        });
      case "ENUM":
        return JSON.stringify(enumValues);
      case "LENGTH":
        return JSON.stringify({
          minLength: lengthMin ? parseInt(lengthMin) : null,
          maxLength: lengthMax ? parseInt(lengthMax) : null,
        });
      default:
        return "";
    }
  };

  // Validate form
  const validateForm = () => {
    const errors = {};

    if (!ruleName.trim()) {
      errors.ruleName = intl.formatMessage({
        id: "validationRule.error.ruleNameRequired",
        defaultMessage: "Rule name is required",
      });
    }

    switch (ruleType) {
      case "REGEX":
        if (!regexPattern.trim()) {
          errors.regexPattern = intl.formatMessage({
            id: "validationRule.error.patternRequired",
            defaultMessage: "Regex pattern is required",
          });
        } else {
          // Validate regex pattern
          try {
            new RegExp(regexPattern);
          } catch (e) {
            errors.regexPattern = intl.formatMessage({
              id: "validationRule.error.invalidPattern",
              defaultMessage: "Invalid regex pattern",
            });
          }
        }
        break;
      case "RANGE":
        if (
          rangeMin &&
          rangeMax &&
          parseFloat(rangeMin) > parseFloat(rangeMax)
        ) {
          errors.range = intl.formatMessage({
            id: "validationRule.error.minGreaterThanMax",
            defaultMessage: "Minimum cannot be greater than maximum",
          });
        }
        break;
      case "ENUM":
        if (enumValues.length === 0) {
          errors.enum = intl.formatMessage({
            id: "validationRule.error.enumValuesRequired",
            defaultMessage: "At least one enum value is required",
          });
        }
        break;
      case "LENGTH":
        if (
          lengthMin &&
          lengthMax &&
          parseInt(lengthMin) > parseInt(lengthMax)
        ) {
          errors.length = intl.formatMessage({
            id: "validationRule.error.minLengthGreaterThanMax",
            defaultMessage:
              "Minimum length cannot be greater than maximum length",
          });
        }
        break;
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle save
  const handleSave = () => {
    if (!validateForm()) {
      return;
    }

    const ruleData = {
      ruleName: ruleName.trim(),
      ruleType: ruleType,
      ruleExpression: buildRuleExpression(),
      errorMessage: errorMessage.trim() || null,
      isActive: isActive,
    };

    const callback = (response, extraParams) => {
      if (response && !extraParams?.error) {
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "validationRule.success.saved",
            defaultMessage: "Validation rule saved successfully",
          }),
        });
        if (onSave) {
          onSave(response);
        }
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "validationRule.error.saveFailed",
            defaultMessage: "Failed to save validation rule",
          }),
          subtitle: extraParams?.error || "Unknown error",
        });
      }
    };

    if (editingRule) {
      updateValidationRule(
        customFieldTypeId,
        editingRule.id,
        ruleData,
        callback,
        null,
      );
    } else {
      createValidationRule(customFieldTypeId, ruleData, callback, null);
    }
  };

  // Handle test validation
  const handleTestValidation = () => {
    if (!testValue.trim()) {
      setTestError(
        intl.formatMessage({
          id: "validationRule.test.valueRequired",
          defaultMessage: "Please enter a test value",
        }),
      );
      setTestResult(null);
      return;
    }

    setTestError(null);
    setTestResult(null);

    // Build test request
    const testData = {
      value: testValue,
      ruleType: ruleType,
      ruleExpression: buildRuleExpression(),
    };

    // Call validation API (we'll need to add this endpoint)
    // For now, we'll do client-side validation
    try {
      let isValid = false;
      switch (ruleType) {
        case "REGEX":
          if (regexPattern) {
            const regex = new RegExp(regexPattern);
            isValid = regex.test(testValue);
          }
          break;
        case "RANGE":
          const numValue = parseFloat(testValue);
          if (!isNaN(numValue)) {
            const min = rangeMin ? parseFloat(rangeMin) : null;
            const max = rangeMax ? parseFloat(rangeMax) : null;
            isValid =
              (min === null || numValue >= min) &&
              (max === null || numValue <= max);
          }
          break;
        case "ENUM":
          isValid = enumValues.includes(testValue);
          break;
        case "LENGTH":
          const length = testValue.length;
          const minLen = lengthMin ? parseInt(lengthMin) : null;
          const maxLen = lengthMax ? parseInt(lengthMax) : null;
          isValid =
            (minLen === null || length >= minLen) &&
            (maxLen === null || length <= maxLen);
          break;
      }

      setTestResult({
        valid: isValid,
        message: isValid
          ? intl.formatMessage({
              id: "validationRule.test.valid",
              defaultMessage: "Value passes validation",
            })
          : errorMessage ||
            intl.formatMessage({
              id: "validationRule.test.invalid",
              defaultMessage: "Value fails validation",
            }),
      });
    } catch (e) {
      setTestError(e.message || "Validation error");
      setTestResult(null);
    }
  };

  // Handle enum value addition
  const handleAddEnumValue = () => {
    if (enumInput.trim() && !enumValues.includes(enumInput.trim())) {
      setEnumValues([...enumValues, enumInput.trim()]);
      setEnumInput("");
    }
  };

  // Handle enum value removal
  const handleRemoveEnumValue = (value) => {
    setEnumValues(enumValues.filter((v) => v !== value));
  };

  return (
    <div
      className="validation-rule-editor"
      data-testid="validation-rule-editor"
    >
      {/* Notification */}
      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.title}
          subtitle={notification.subtitle}
          onClose={() => setNotification(null)}
          lowContrast
        />
      )}

      <Form>
        {/* Rule Name */}
        <FormGroup legendText="">
          <TextInput
            id="ruleName"
            labelText={intl.formatMessage({
              id: "validationRule.form.ruleName",
              defaultMessage: "Rule Name",
            })}
            placeholder={intl.formatMessage({
              id: "validationRule.form.ruleNamePlaceholder",
              defaultMessage: "e.g., Email Pattern",
            })}
            value={ruleName}
            onChange={(e) => setRuleName(e.target.value)}
            invalid={!!formErrors.ruleName}
            invalidText={formErrors.ruleName}
            required
            data-testid="rule-name-input"
          />
        </FormGroup>

        {/* Rule Type */}
        <FormGroup legendText="">
          <Select
            id="ruleType"
            labelText={intl.formatMessage({
              id: "validationRule.form.ruleType",
              defaultMessage: "Rule Type",
            })}
            value={ruleType}
            onChange={(e) => {
              setRuleType(e.target.value);
              setFormErrors({});
              setTestResult(null);
              setTestError(null);
            }}
            data-testid="rule-type-select"
          >
            <SelectItem value="REGEX" text="REGEX" />
            <SelectItem value="RANGE" text="RANGE" />
            <SelectItem value="ENUM" text="ENUM" />
            <SelectItem value="LENGTH" text="LENGTH" />
          </Select>
        </FormGroup>

        {/* Dynamic form fields based on rule type */}
        {ruleType === "REGEX" && (
          <FormGroup legendText="">
            <TextInput
              id="regexPattern"
              labelText={intl.formatMessage({
                id: "validationRule.form.regexPattern",
                defaultMessage: "Regex Pattern",
              })}
              placeholder={intl.formatMessage({
                id: "validationRule.form.regexPatternPlaceholder",
                defaultMessage:
                  "e.g., ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]'{2,}'$",
              })}
              value={regexPattern}
              onChange={(e) => setRegexPattern(e.target.value)}
              invalid={!!formErrors.regexPattern}
              invalidText={formErrors.regexPattern}
              required
              data-testid="regex-pattern-input"
            />
          </FormGroup>
        )}

        {ruleType === "RANGE" && (
          <FormGroup legendText="">
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="rangeMin"
                  label={intl.formatMessage({
                    id: "validationRule.form.rangeMin",
                    defaultMessage: "Minimum",
                  })}
                  value={rangeMin}
                  onChange={(e) => setRangeMin(e.target.value)}
                  allowEmpty
                  data-testid="range-min-input"
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="rangeMax"
                  label={intl.formatMessage({
                    id: "validationRule.form.rangeMax",
                    defaultMessage: "Maximum",
                  })}
                  value={rangeMax}
                  onChange={(e) => setRangeMax(e.target.value)}
                  allowEmpty
                  data-testid="range-max-input"
                />
              </Column>
            </Grid>
            {formErrors.range && (
              <div className="validation-error" data-testid="range-error">
                {formErrors.range}
              </div>
            )}
          </FormGroup>
        )}

        {ruleType === "ENUM" && (
          <FormGroup legendText="">
            <div className="enum-input-group">
              <TextInput
                id="enumInput"
                labelText={intl.formatMessage({
                  id: "validationRule.form.enumValues",
                  defaultMessage: "Allowed Values",
                })}
                placeholder={intl.formatMessage({
                  id: "validationRule.form.enumInputPlaceholder",
                  defaultMessage: "Enter value and press Enter",
                })}
                value={enumInput}
                onChange={(e) => setEnumInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddEnumValue();
                  }
                }}
                data-testid="enum-input"
              />
              <Button
                kind="tertiary"
                size="sm"
                onClick={handleAddEnumValue}
                disabled={!enumInput.trim()}
                data-testid="add-enum-value-button"
              >
                {intl.formatMessage({
                  id: "validationRule.action.addValue",
                  defaultMessage: "Add",
                })}
              </Button>
            </div>
            {enumValues.length > 0 && (
              <div className="enum-values-list" data-testid="enum-values-list">
                {enumValues.map((value, index) => (
                  <Tag
                    key={index}
                    type="blue"
                    filter
                    onClose={() => handleRemoveEnumValue(value)}
                    data-testid={`enum-value-tag-${value}`}
                  >
                    {value}
                  </Tag>
                ))}
              </div>
            )}
            {formErrors.enum && (
              <div className="validation-error" data-testid="enum-error">
                {formErrors.enum}
              </div>
            )}
          </FormGroup>
        )}

        {ruleType === "LENGTH" && (
          <FormGroup legendText="">
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="lengthMin"
                  label={intl.formatMessage({
                    id: "validationRule.form.lengthMin",
                    defaultMessage: "Minimum Length",
                  })}
                  value={lengthMin}
                  onChange={(e) => setLengthMin(e.target.value)}
                  allowEmpty
                  min={0}
                  data-testid="length-min-input"
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="lengthMax"
                  label={intl.formatMessage({
                    id: "validationRule.form.lengthMax",
                    defaultMessage: "Maximum Length",
                  })}
                  value={lengthMax}
                  onChange={(e) => setLengthMax(e.target.value)}
                  allowEmpty
                  min={0}
                  data-testid="length-max-input"
                />
              </Column>
            </Grid>
            {formErrors.length && (
              <div className="validation-error" data-testid="length-error">
                {formErrors.length}
              </div>
            )}
          </FormGroup>
        )}

        {/* Error Message */}
        <FormGroup legendText="">
          <TextArea
            id="errorMessage"
            labelText={intl.formatMessage({
              id: "validationRule.form.errorMessage",
              defaultMessage: "Error Message (Optional)",
            })}
            placeholder={intl.formatMessage({
              id: "validationRule.form.errorMessagePlaceholder",
              defaultMessage:
                "Custom error message to display when validation fails",
            })}
            value={errorMessage}
            onChange={(e) => setErrorMessage(e.target.value)}
            maxCount={500}
            rows={3}
            data-testid="error-message-textarea"
          />
        </FormGroup>

        {/* Test Validation Section */}
        <FormGroup
          legendText={intl.formatMessage({
            id: "validationRule.test.section",
            defaultMessage: "Test Validation",
          })}
        >
          <div className="test-validation-group">
            <TextInput
              id="testValue"
              labelText={intl.formatMessage({
                id: "validationRule.test.testValue",
                defaultMessage: "Test Value",
              })}
              placeholder={intl.formatMessage({
                id: "validationRule.test.testValuePlaceholder",
                defaultMessage: "Enter a value to test",
              })}
              value={testValue}
              onChange={(e) => {
                setTestValue(e.target.value);
                setTestResult(null);
                setTestError(null);
              }}
              data-testid="test-value-input"
            />
            <Button
              kind="secondary"
              onClick={handleTestValidation}
              data-testid="test-rule-button"
            >
              {intl.formatMessage({
                id: "validationRule.test.testButton",
                defaultMessage: "Test Rule",
              })}
            </Button>
          </div>
          {testResult && (
            <InlineNotification
              kind={testResult.valid ? "success" : "error"}
              title={testResult.message}
              lowContrast
              data-testid="test-result-notification"
            />
          )}
          {testError && (
            <InlineNotification
              kind="error"
              title={testError}
              lowContrast
              data-testid="test-error-notification"
            />
          )}
        </FormGroup>

        {/* Action Buttons */}
        <div className="validation-rule-editor-actions">
          <Button
            kind="primary"
            onClick={handleSave}
            data-testid="save-rule-button"
          >
            {intl.formatMessage({
              id: "validationRule.action.save",
              defaultMessage: "Save Rule",
            })}
          </Button>
          <Button
            kind="secondary"
            onClick={onCancel}
            data-testid="cancel-button"
          >
            {intl.formatMessage({
              id: "validationRule.action.cancel",
              defaultMessage: "Cancel",
            })}
          </Button>
        </div>
      </Form>
    </div>
  );
};

export default ValidationRuleEditor;
