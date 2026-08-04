import React, { useEffect, useMemo, useState } from "react";
import {
  Checkbox,
  ComposedModal,
  InlineNotification,
  ModalBody,
  ModalFooter,
  ModalHeader,
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
      [field.key]: field.type === "checkbox" ? true : field.defaultValue || "",
    }),
    {},
  );

const ReferenceEditModal = ({
  open,
  titleId,
  fields,
  value,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const initialValue = useMemo(
    () => ({ ...emptyValue(fields), ...(value || {}) }),
    [fields, value],
  );
  const [draft, setDraft] = useState(initialValue);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) {
      setDraft(initialValue);
      setError("");
    }
  }, [initialValue, open]);

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
    <ComposedModal open={open} onClose={onClose} size="sm">
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
