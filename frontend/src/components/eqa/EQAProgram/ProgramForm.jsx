import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Toggle,
  Select,
  SelectItem,
  MultiSelect,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
  resolveApiErrorMessage,
} from "../../utils/Utils";

// The four arrangement types a scheme can have. IN_HOUSE is the only one that
// may omit a provider, which is why the form branches on it.
const SCHEME_TYPES = [
  "INTERNATIONAL_PT",
  "REGIONAL_PT",
  "INTER_LAB_SPLIT",
  "IN_HOUSE",
];

const sameIds = (left, right) =>
  left.length === right.length &&
  [...left].sort().join(",") === [...right].sort().join(",");

const ProgramForm = ({ program, onClose }) => {
  const intl = useIntl();
  const isEditing = !!program;

  const [name, setName] = useState(program?.name || "");
  const [provider, setProvider] = useState(program?.provider || "");
  const [description, setDescription] = useState(program?.description || "");
  const [isActive, setIsActive] = useState(program?.isActive !== false);
  const [perAnalyst, setPerAnalyst] = useState(program?.perAnalyst === true);
  const [schemeType, setSchemeType] = useState(
    program?.schemeType || "INTERNATIONAL_PT",
  );
  const [testOptions, setTestOptions] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [assignedTestIds, setAssignedTestIds] = useState([]);
  const [nameError, setNameError] = useState("");
  const [providerError, setProviderError] = useState("");
  const [saveError, setSaveError] = useState("");

  const providerRequired = schemeType !== "IN_HOUSE";

  // ALL_TESTS, not the lab-unit-scoped test list: a scheme's test map is a
  // configuration decision, and the QA officer who makes it holds no bench role.
  useEffect(() => {
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", (data) => {
      const items = (Array.isArray(data) ? data : []).map((test) => ({
        id: String(test.id),
        text: test.value,
      }));
      setTestOptions(items);

      if (!program?.id) return;
      getFromOpenElisServer(
        `/rest/eqa/programs/${program.id}/tests`,
        (assignments) => {
          const ids = (Array.isArray(assignments) ? assignments : [])
            .filter((row) => row.isActive !== false)
            .map((row) => String(row.testId));
          setAssignedTestIds(ids);
          setSelectedTests(items.filter((item) => ids.includes(item.id)));
        },
      );
    });
  }, [program?.id]);

  const showRefusal = (response, fallbackKey) =>
    Promise.resolve(response ? response.json().catch(() => null) : null).then(
      (body) => setSaveError(resolveApiErrorMessage(intl, body, fallbackKey)),
    );

  // The scheme has to exist before its tests can be assigned, so a create
  // chains: POST the scheme, then PUT the test map against the id it answers.
  const saveTestMap = (programId) => {
    const testIds = selectedTests.map((test) => test.id);
    if (sameIds(testIds, assignedTestIds)) {
      if (onClose) onClose();
      return;
    }
    putToOpenElisServerFullResponse(
      `/rest/eqa/programs/${programId}/tests`,
      JSON.stringify({ testIds: testIds.map(Number) }),
      (response) => {
        if (response && response.ok) {
          if (onClose) onClose();
          return;
        }
        showRefusal(response, "eqa.program.tests.saveFailed");
      },
    );
  };

  // keep the modal open and show why the server refused, instead of closing as if saved
  const handleResponse = (response) => {
    if (response && response.ok) {
      Promise.resolve(response.json().catch(() => null)).then((body) => {
        const programId = program?.id || body?.id;
        if (programId) {
          saveTestMap(programId);
        } else if (onClose) {
          onClose();
        }
      });
      return;
    }
    showRefusal(response, "error.save.failed");
  };

  const handleSubmit = () => {
    let valid = true;

    if (!name.trim()) {
      setNameError(intl.formatMessage({ id: "eqa.program.name.required" }));
      valid = false;
    }
    if (providerRequired && !provider.trim()) {
      setProviderError(
        intl.formatMessage({ id: "eqa.program.provider.required" }),
      );
      valid = false;
    }

    if (!valid) return;

    const payload = {
      name,
      provider: providerRequired ? provider : "",
      description,
      perAnalyst,
      schemeType,
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
        <Select
          id="program-scheme-type"
          labelText={intl.formatMessage({
            id: "eqa.program.schemeType",
            defaultMessage: "Scheme type",
          })}
          helperText={intl.formatMessage({
            id: "eqa.program.schemeType.helper",
            defaultMessage:
              "In-house schemes are run by this laboratory and need no provider; every other type does.",
          })}
          value={schemeType}
          onChange={(e) => {
            setSchemeType(e.target.value);
            if (providerError) setProviderError("");
          }}
        >
          {SCHEME_TYPES.map((type) => (
            <SelectItem
              key={type}
              value={type}
              text={intl.formatMessage({
                id: `eqa.scheme.type.${type.toLowerCase()}`,
                defaultMessage: type.replace(/_/g, " "),
              })}
            />
          ))}
        </Select>
        {providerRequired && (
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
        )}
        <MultiSelect
          id="program-tests"
          titleText={intl.formatMessage({
            id: "eqa.program.tests",
            defaultMessage: "Tests in this scheme",
          })}
          helperText={intl.formatMessage({
            id: "eqa.program.tests.helper",
            defaultMessage:
              "The tests participants report on. Provider result entry and CSV import offer these, so a scheme with none cannot take in results.",
          })}
          label={intl.formatMessage({
            id: "eqa.program.tests.placeholder",
            defaultMessage: "Select tests",
          })}
          items={testOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItems={selectedTests}
          onChange={({ selectedItems }) =>
            setSelectedTests(selectedItems || [])
          }
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
