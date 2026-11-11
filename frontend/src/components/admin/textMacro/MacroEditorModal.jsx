import React, { useEffect, useState } from "react";
import {
  Checkbox,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextArea,
  TextInput,
  Toggle,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { TEXT_MACRO_CATEGORIES, TEXT_MACRO_CONTEXTS } from "./textMacroConfig";

const CODE_ERRORS = [
  "INVALID_MACRO_CODE",
  "MACRO_CODE_EXISTS",
  "MACRO_CODE_RESERVED",
];

const MacroEditorModal = ({
  open,
  mode,
  value,
  saving,
  error,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const [draft, setDraft] = useState(value);

  useEffect(() => setDraft(value), [value]);

  if (!draft) return null;

  const toggleContext = (context, checked) =>
    setDraft((current) => ({
      ...current,
      contexts: checked
        ? [...new Set([...current.contexts, context])]
        : current.contexts.filter((item) => item !== context),
    }));

  const invalid =
    !draft.code.trim() ||
    !draft.expansionText.trim() ||
    !draft.category ||
    draft.contexts.length === 0;
  const codeError = CODE_ERRORS.includes(error?.code);

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: mode === "create" ? "textMacro.add" : "textMacro.edit",
      })}
      primaryButtonText={intl.formatMessage({ id: "textMacro.save" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving || invalid}
      onRequestClose={onClose}
      onRequestSubmit={() => onSave(draft)}
    >
      <div className="text-macro-admin__form">
        {error && !codeError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "textMacro.error" })}
            subtitle={error.message}
          />
        )}
        <TextInput
          id="text-macro-code"
          labelText={intl.formatMessage({ id: "textMacro.code" })}
          helperText={intl.formatMessage({ id: "textMacro.code.helper" })}
          maxLength={15}
          invalid={codeError}
          invalidText={codeError ? error.message : undefined}
          value={draft.code}
          onChange={(event) =>
            setDraft((current) => ({ ...current, code: event.target.value }))
          }
        />
        <Select
          id="text-macro-category"
          labelText={intl.formatMessage({ id: "textMacro.category" })}
          helperText={intl.formatMessage({
            id: draft.category
              ? `textMacro.category.${draft.category}.helper`
              : "textMacro.category.helper",
          })}
          value={draft.category}
          onChange={(event) =>
            setDraft((current) => ({
              ...current,
              category: event.target.value,
            }))
          }
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "textMacro.category.choose" })}
          />
          {TEXT_MACRO_CATEGORIES.map((category) => (
            <SelectItem
              key={category}
              value={category}
              text={intl.formatMessage({
                id: `textMacro.category.${category}`,
              })}
            />
          ))}
        </Select>
        <TextArea
          id="text-macro-expansion"
          labelText={intl.formatMessage({ id: "textMacro.text" })}
          maxLength={500}
          enableCounter
          value={draft.expansionText}
          onChange={(event) =>
            setDraft((current) => ({
              ...current,
              expansionText: event.target.value,
            }))
          }
        />
        <fieldset className="text-macro-admin__contexts">
          <legend>{intl.formatMessage({ id: "textMacro.contexts" })}</legend>
          {TEXT_MACRO_CONTEXTS.map((context) => (
            <Checkbox
              key={context}
              id={`text-macro-context-${context}`}
              labelText={intl.formatMessage({
                id: `textMacro.context.${context}`,
              })}
              checked={draft.contexts.includes(context)}
              onChange={(_, { checked }) => toggleContext(context, checked)}
            />
          ))}
        </fieldset>
        <Toggle
          id="text-macro-active"
          labelText={intl.formatMessage({ id: "textMacro.status" })}
          labelA={intl.formatMessage({ id: "textMacro.status.inactive" })}
          labelB={intl.formatMessage({ id: "textMacro.status.active" })}
          toggled={draft.active}
          onToggle={(active) => setDraft((current) => ({ ...current, active }))}
        />
      </div>
    </Modal>
  );
};

export default MacroEditorModal;
