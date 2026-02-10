import React, { useState } from "react";
import { Modal, TextInput, TextArea, Toggle } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";

const ProgramForm = ({ program, onClose }) => {
  const intl = useIntl();
  const isEditing = !!program;

  const [name, setName] = useState(program?.name || "");
  const [description, setDescription] = useState(program?.description || "");
  const [isActive, setIsActive] = useState(program?.isActive !== false);
  const [nameError, setNameError] = useState("");

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError(intl.formatMessage({ id: "eqa.program.name.required" }));
      return;
    }

    if (isEditing) {
      putToOpenElisServer(
        `/rest/eqa/programs/${program.id}`,
        JSON.stringify({ name, description, isActive }),
        () => {
          if (onClose) onClose();
        },
      );
    } else {
      postToOpenElisServerJsonResponse(
        "/rest/eqa/programs",
        JSON.stringify({ name, description }),
        (response) => {
          if (response && !response.error) {
            if (onClose) onClose();
          }
        },
      );
    }
  };

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: isEditing ? "eqa.program.edit" : "eqa.program.create",
      })}
      primaryButtonText={intl.formatMessage({ id: "eqa.program.save" })}
      secondaryButtonText={intl.formatMessage({ id: "eqa.program.cancel" })}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      onSecondarySubmit={onClose}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
        <TextInput
          id="program-name"
          labelText={intl.formatMessage({ id: "eqa.program.name" })}
          placeholder={intl.formatMessage({
            id: "eqa.program.name.placeholder",
          })}
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            if (nameError) setNameError("");
          }}
          invalid={!!nameError}
          invalidText={nameError}
        />
        <TextArea
          id="program-description"
          labelText={intl.formatMessage({ id: "eqa.program.description" })}
          placeholder={intl.formatMessage({
            id: "eqa.program.description.placeholder",
          })}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
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
