import React, { useState } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Toggle,
  Grid,
  Column,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";

const PROVIDERS = ["WHO", "CDC", "CAP", "UKNEQAS", "EQAS", "RIQAS"];

const CATEGORIES = [
  "Microbiology",
  "Serology",
  "Chemistry",
  "Hematology",
  "Immunology",
  "Parasitology",
];

const FREQUENCIES = ["Monthly", "Quarterly", "Biannual", "Annual"];

const ProgramForm = ({ program, onClose }) => {
  const intl = useIntl();
  const isEditing = !!program;

  const [name, setName] = useState(program?.name || "");
  const [description, setDescription] = useState(program?.description || "");
  const [providerName, setProviderName] = useState(program?.providerName || "");
  const [category, setCategory] = useState(program?.category || "");
  const [frequency, setFrequency] = useState(program?.frequency || "");
  const [isActive, setIsActive] = useState(program?.isActive !== false);
  const [nameError, setNameError] = useState("");

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError(intl.formatMessage({ id: "eqa.program.name.required" }));
      return;
    }

    const payload = { name, description, providerName, category, frequency };

    if (isEditing) {
      putToOpenElisServer(
        `/rest/eqa/programs/${program.id}`,
        JSON.stringify({ ...payload, isActive }),
        () => {
          if (onClose) onClose();
        },
      );
    } else {
      postToOpenElisServerJsonResponse(
        "/rest/eqa/programs",
        JSON.stringify(payload),
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
        <Grid condensed>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="program-provider"
              labelText={intl.formatMessage({ id: "eqa.admin.col.provider" })}
              value={providerName}
              onChange={(e) => setProviderName(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "eqa.admin.form.provider.placeholder",
                })}
              />
              {PROVIDERS.map((p) => (
                <SelectItem key={p} value={p} text={p} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="program-category"
              labelText={intl.formatMessage({ id: "eqa.admin.col.category" })}
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "eqa.admin.form.category.placeholder",
                })}
              />
              {CATEGORIES.map((cat) => (
                <SelectItem key={cat} value={cat} text={cat} />
              ))}
            </Select>
          </Column>
        </Grid>
        <Select
          id="program-frequency"
          labelText={intl.formatMessage({ id: "eqa.admin.col.frequency" })}
          value={frequency}
          onChange={(e) => setFrequency(e.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "eqa.admin.form.frequency.placeholder",
            })}
          />
          {FREQUENCIES.map((freq) => (
            <SelectItem key={freq} value={freq} text={freq} />
          ))}
        </Select>
        <TextArea
          id="program-description"
          labelText={intl.formatMessage({ id: "eqa.program.description" })}
          placeholder={intl.formatMessage({
            id: "eqa.admin.form.description.placeholder",
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
