import React, { useEffect, useState } from "react";
import { Checkbox, Modal, TextArea, TextInput, Toggle } from "@carbon/react";
import { useIntl } from "react-intl";
import { TEXT_MACRO_CONTEXTS } from "./textMacroConfig";

const MacroEditorModal = ({ open, mode, value, saving, onClose, onSave }) => {
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
    draft.contexts.length === 0;

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
        <TextInput
          id="text-macro-code"
          labelText={intl.formatMessage({ id: "textMacro.code" })}
          value={draft.code}
          onChange={(event) =>
            setDraft((current) => ({ ...current, code: event.target.value }))
          }
        />
        <TextArea
          id="text-macro-expansion"
          labelText={intl.formatMessage({ id: "textMacro.text" })}
          aria-label={intl.formatMessage({ id: "textMacro.text" })}
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
