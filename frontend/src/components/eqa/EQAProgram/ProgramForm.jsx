import React, { useState } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
  resolveApiErrorMessage,
} from "../../utils/Utils";

const ProgramForm = ({ program, onClose }) => {
  const intl = useIntl();
  const isEditing = !!program;

  const [name, setName] = useState(program?.name || "");
  const [provider, setProvider] = useState(program?.provider || "");
  const [description, setDescription] = useState(program?.description || "");
  const [isActive, setIsActive] = useState(program?.isActive !== false);
  const [perAnalyst, setPerAnalyst] = useState(program?.perAnalyst === true);
  const [nameError, setNameError] = useState("");
  const [providerError, setProviderError] = useState("");
  const [saveError, setSaveError] = useState("");

  // keep the modal open and show why the server refused, instead of closing as if saved
  const handleResponse = (response) => {
    if (response && response.ok) {
      if (onClose) onClose();
      return;
    }
    Promise.resolve(response ? response.json().catch(() => null) : null).then(
      (body) =>
        setSaveError(resolveApiErrorMessage(intl, body, "error.save.failed")),
    );
  };

  const handleSubmit = () => {
    let valid = true;

    if (!name.trim()) {
      setNameError(intl.formatMessage({ id: "eqa.program.name.required" }));
      valid = false;
    }
    if (!provider.trim()) {
      setProviderError(
        intl.formatMessage({ id: "eqa.program.provider.required" }),
      );
      valid = false;
    }

    if (!valid) return;

    const payload = {
      name,
      provider,
      description,
      perAnalyst,
    };

    setSaveError("");
    if (isEditing) {
      putToOpenElisServerFullResponse(
        `/rest/eqa/programs/${program.id}`,
        JSON.stringify({ ...payload, isActive }),
        handleResponse,
      );
    } else {
      postToOpenElisServerFullResponse(
        "/rest/eqa/programs",
        JSON.stringify(payload),
        handleResponse,
      );
    }
  };

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: isEditing
          ? "eqa.admin.form.editHeading"
          : "eqa.admin.form.addHeading",
      })}
      modalLabel={intl.formatMessage({ id: "eqa.admin.form.subtitle" })}
      primaryButtonText={intl.formatMessage({
        id: isEditing ? "eqa.program.save" : "eqa.admin.addProgram",
      })}
      secondaryButtonText={intl.formatMessage({ id: "eqa.program.cancel" })}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      onSecondarySubmit={onClose}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
        {saveError && (
          <InlineNotification
            kind="error"
            title={saveError}
            onCloseButtonClick={() => setSaveError("")}
          />
        )}
        <TextInput
          id="program-name"
          labelText={intl.formatMessage({ id: "eqa.program.name" })}
          placeholder={intl.formatMessage({
            id: "eqa.admin.form.name.placeholder",
          })}
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            if (nameError) setNameError("");
          }}
          invalid={!!nameError}
          invalidText={nameError}
        />
        <TextInput
          id="program-provider"
          labelText={intl.formatMessage({ id: "eqa.admin.col.provider" })}
          placeholder={intl.formatMessage({
            id: "eqa.admin.form.provider.placeholder",
          })}
          value={provider}
          onChange={(e) => {
            setProvider(e.target.value);
            if (providerError) setProviderError("");
          }}
          invalid={!!providerError}
          invalidText={providerError}
        />
        <TextArea
          id="program-description"
          labelText={intl.formatMessage({ id: "eqa.program.description" })}
          placeholder={intl.formatMessage({
            id: "eqa.admin.form.description.placeholder",
          })}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <Toggle
          id="program-per-analyst"
          labelText={intl.formatMessage({
            id: "eqa.program.perAnalyst",
            defaultMessage: "Record the analyst on every result",
          })}
          labelA={intl.formatMessage({ id: "eqa.program.perAnalyst.off" })}
          labelB={intl.formatMessage({ id: "eqa.program.perAnalyst.on" })}
          toggled={perAnalyst}
          onToggle={(toggled) => setPerAnalyst(toggled)}
        />
        {isEditing && (
          <Toggle
            id="program-active"
            labelText={intl.formatMessage({ id: "eqa.program.status" })}
            labelA={intl.formatMessage({ id: "eqa.program.inactive" })}
            labelB={intl.formatMessage({ id: "eqa.program.active" })}
            toggled={isActive}
            onToggle={(toggled) => setIsActive(toggled)}
          />
        )}
      </div>
    </Modal>
  );
};

export default ProgramForm;
