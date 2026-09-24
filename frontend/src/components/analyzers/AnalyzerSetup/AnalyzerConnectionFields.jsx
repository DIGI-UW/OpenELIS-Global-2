import React from "react";
import {
  Checkbox,
  NumberInput,
  PasswordInput,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";

const hasMessage = (intl, id) =>
  Boolean(id) && Object.prototype.hasOwnProperty.call(intl.messages, id);

const translated = (intl, id, fallbackId) =>
  intl.formatMessage({ id: hasMessage(intl, id) ? id : fallbackId });

const initialValue = (field) => {
  if (field.inputKind === "SECRET") {
    return "";
  }
  if (Object.prototype.hasOwnProperty.call(field, "currentValue")) {
    return field.currentValue;
  }
  if (field.defaultValue !== null && field.defaultValue !== undefined) {
    return field.defaultValue;
  }
  return field.inputKind === "BOOLEAN" ? false : "";
};

export const initializeConnectionValues = (fields = []) =>
  fields.reduce(
    (values, field) => ({ ...values, [field.key]: initialValue(field) }),
    {},
  );

const conditionIncludes = (expected, actual) =>
  Array.isArray(expected) && expected.some((value) => value === actual);

export const isConnectionFieldVisible = (
  field,
  values,
  fields = [],
  visiting = new Set(),
) => {
  if (visiting.has(field.key)) {
    return false;
  }
  const condition = field.visibleWhen;
  if (!condition) {
    return true;
  }
  const nextVisiting = new Set(visiting);
  nextVisiting.add(field.key);
  const controllingField = fields.find(
    (candidate) => candidate.key === condition.fieldKey,
  );
  if (
    controllingField &&
    !isConnectionFieldVisible(controllingField, values, fields, nextVisiting)
  ) {
    return false;
  }
  const actual = values[condition.fieldKey];
  switch (condition.operator) {
    case "EQUALS":
      return actual === condition.value;
    case "NOT_EQUALS":
      return actual !== condition.value;
    case "IN":
      return conditionIncludes(condition.value, actual);
    case "NOT_IN":
      return !conditionIncludes(condition.value, actual);
    default:
      return false;
  }
};

const isMissing = (field, value, changedSecrets) => {
  if (!field.required) {
    return false;
  }
  if (field.inputKind === "SECRET") {
    return (
      !field.isSet &&
      (!changedSecrets.has(field.key) || String(value).trim() === "")
    );
  }
  return value === null || value === undefined || String(value).trim() === "";
};

export const invalidConnectionFields = (
  fields = [],
  values = {},
  changedSecrets = new Set(),
) =>
  fields.filter(
    (field) =>
      isConnectionFieldVisible(field, values, fields) &&
      isMissing(field, values[field.key], changedSecrets),
  );

export const serializeConnectionValues = (
  fields = [],
  values = {},
  changedSecrets = new Set(),
) => {
  const serialized = {};
  fields
    .filter((field) => isConnectionFieldVisible(field, values, fields))
    .forEach((field) => {
      const value = values[field.key];
      if (field.inputKind === "SECRET" && !changedSecrets.has(field.key)) {
        return;
      }
      if (value === null || value === undefined || value === "") {
        return;
      }
      if (field.inputKind === "NUMBER") {
        const number = Number(value);
        if (Number.isFinite(number)) {
          serialized[field.key] = number;
        }
        return;
      }
      serialized[field.key] = typeof value === "string" ? value.trim() : value;
    });
  return serialized;
};

const fieldError = (intl, field, value, submitAttempted, changedSecrets) => {
  if (submitAttempted && isMissing(field, value, changedSecrets)) {
    return intl.formatMessage({
      id: "analyzer.connection.validation.required",
    });
  }
  const errorKey = field.validationErrors?.[0];
  return errorKey
    ? translated(intl, errorKey, "analyzer.connection.validation.invalid")
    : null;
};

const AnalyzerConnectionFields = ({
  fields,
  values,
  changedSecrets,
  submitAttempted,
  onChange,
}) => {
  const intl = useIntl();

  return (
    <div className="analyzer-setup__connect-fields">
      {fields
        .filter((field) => isConnectionFieldVisible(field, values, fields))
        .map((field) => {
          const id = `analyzer-connection-${field.key}`;
          const label = translated(
            intl,
            field.labelKey,
            "analyzer.connection.field.unavailable",
          );
          const helperText = hasMessage(intl, field.helpTextKey)
            ? intl.formatMessage({ id: field.helpTextKey })
            : undefined;
          const error = fieldError(
            intl,
            field,
            values[field.key],
            submitAttempted,
            changedSecrets,
          );
          const common = {
            id,
            invalid: Boolean(error),
            invalidText: error || "",
          };

          if (field.inputKind === "SELECT") {
            return (
              <Select
                {...common}
                key={field.key}
                labelText={label}
                helperText={helperText}
                value={values[field.key] ?? ""}
                onChange={(event) => onChange(field, event.target.value)}
              >
                {(values[field.key] === "" ||
                  values[field.key] === null ||
                  values[field.key] === undefined) && (
                  <SelectItem
                    disabled
                    value=""
                    text={intl.formatMessage({
                      id: "analyzer.connection.choice.select",
                    })}
                  />
                )}
                {(field.choices || []).map((choice) => (
                  <SelectItem
                    key={choice.value}
                    value={choice.value}
                    text={
                      hasMessage(intl, choice.labelKey)
                        ? intl.formatMessage({ id: choice.labelKey })
                        : String(choice.value)
                    }
                  />
                ))}
              </Select>
            );
          }

          if (field.inputKind === "NUMBER") {
            return (
              <NumberInput
                {...common}
                key={field.key}
                label={label}
                helperText={helperText}
                value={values[field.key] ?? ""}
                allowEmpty
                onChange={(event) => onChange(field, event.target.value)}
              />
            );
          }

          if (field.inputKind === "BOOLEAN") {
            return (
              <Checkbox
                id={id}
                key={field.key}
                labelText={label}
                checked={Boolean(values[field.key])}
                onChange={(_, { checked }) => onChange(field, checked)}
              />
            );
          }

          if (field.inputKind === "SECRET") {
            return (
              <PasswordInput
                {...common}
                key={field.key}
                labelText={label}
                helperText={helperText}
                value={values[field.key] ?? ""}
                placeholder={field.isSet ? field.maskedValue : undefined}
                autoComplete="new-password"
                onChange={(event) => onChange(field, event.target.value)}
              />
            );
          }

          return (
            <TextInput
              {...common}
              key={field.key}
              labelText={label}
              helperText={helperText}
              value={values[field.key] ?? ""}
              onChange={(event) => onChange(field, event.target.value)}
            />
          );
        })}
    </div>
  );
};

export default AnalyzerConnectionFields;
