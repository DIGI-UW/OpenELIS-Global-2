import React, { useMemo, useState } from "react";
import {
  Checkbox,
  ComposedModal,
  InlineNotification,
  ModalBody,
  ModalFooter,
  ModalHeader,
  NumberInput,
  Select,
  SelectItem,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";

const emptyValue = (fields) =>
  fields.reduce(
    (value, field) => ({
      ...value,
      [field.key]:
        field.type === "checkbox"
          ? true
          : field.type === "number"
            ? (field.defaultValue ?? null)
            : field.defaultValue || "",
    }),
    {},
  );

const ReferenceEditModal = ({ titleId, fields, value, onClose, onSave }) => {
  const intl = useIntl();
  const initialValue = useMemo(
    () => ({ ...emptyValue(fields), ...(value || {}) }),
    [fields, value],
  );
  const [draft, setDraft] = useState(initialValue);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const update = (key, next) =>
    setDraft((current) => ({ ...current, [key]: next }));

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      await onSave(draft);
    } catch (requestError) {
      setError(
        requestError.message ||
          intl.formatMessage({ id: "microbiology.admin.error.save" }),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <ComposedModal open onClose={onClose} size="sm">
      <ModalHeader
        title={intl.formatMessage({ id: titleId })}
        closeModal={onClose}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.admin.error.title",
            })}
            subtitle={error}
          />
        )}
        <div className="microbiology-admin__form">
          {fields.map((field) => {
            const label = intl.formatMessage({ id: field.label });
            if (field.type === "dynamic-select") {
              return (
                <Select
                  key={field.key}
                  id={`microbiology-${field.key}`}
                  labelText={label}
                  value={draft[field.key] || ""}
                  disabled={field.disabledOnEdit && !!draft.id}
                  onChange={(event) => update(field.key, event.target.value)}
                >
                  <SelectItem value="" text="" />
                  {(field.options || []).map((option) => (
                    <SelectItem
                      key={option.id}
                      value={option.id}
                      text={
                        option.code
                          ? `${option.label} (${option.code})`
                          : option.label
                      }
                    />
                  ))}
                </Select>
              );
            }
            if (field.type === "select") {
              return (
                <Select
                  key={field.key}
                  id={`microbiology-${field.key}`}
                  labelText={label}
                  value={draft[field.key] || ""}
                  disabled={field.disabledOnEdit && !!draft.id}
                  onChange={(event) => update(field.key, event.target.value)}
                >
                  <SelectItem value="" text="" />
                  {field.options.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={intl.formatMessage({ id: option.label })}
                    />
                  ))}
                </Select>
              );
            }
            if (field.type === "textarea") {
              return (
                <TextArea
                  key={field.key}
                  id={`microbiology-${field.key}`}
                  labelText={label}
                  value={draft[field.key] || ""}
                  onChange={(event) => update(field.key, event.target.value)}
                />
              );
            }
            if (field.type === "number") {
              return (
                <NumberInput
                  key={field.key}
                  id={`microbiology-${field.key}`}
                  label={label}
                  value={draft[field.key] ?? ""}
                  min={field.min}
                  max={field.max}
                  step={field.step || 1}
                  allowEmpty
                  onChange={(event, state = {}) => {
                    const next = state.value ?? event.target.value;
                    update(field.key, next === "" ? null : Number(next));
                  }}
                />
              );
            }
            if (field.type === "checkbox") {
              return (
                <Checkbox
                  key={field.key}
                  id={`microbiology-${field.key}`}
                  labelText={label}
                  checked={!!draft[field.key]}
                  onChange={(_, detail) => update(field.key, detail.checked)}
                />
              );
            }
            return (
              <TextInput
                key={field.key}
                id={`microbiology-${field.key}`}
                labelText={label}
                value={draft[field.key] || ""}
                required={field.required}
                disabled={field.disabledOnEdit && !!draft.id}
                onChange={(event) => update(field.key, event.target.value)}
              />
            );
          })}
        </div>
      </ModalBody>
      <ModalFooter
        primaryButtonText={intl.formatMessage({ id: "button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestSubmit={save}
        onRequestClose={onClose}
        primaryButtonDisabled={saving}
      />
    </ComposedModal>
  );
};

export default ReferenceEditModal;
