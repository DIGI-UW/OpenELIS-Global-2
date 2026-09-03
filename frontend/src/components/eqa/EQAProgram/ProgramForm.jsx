import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Toggle,
  Select,
  SelectItem,
  FilterableMultiSelect,
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
  const [nameError, setNameError] = useState("");
  const [providerError, setProviderError] = useState("");
  const [saveError, setSaveError] = useState("");

  // An in-house scheme is run by this laboratory, so it has no external provider
  // to name; every other type must have one.
  const providerRequired = schemeType !== "IN_HOUSE";

  // The tests this programme collects. Provider intake reads exactly this map —
  // the results grid and its CSV import both iterate it — so a programme with
  // none of them cannot take in a single participant result.
  const [tests, setTests] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [assignedTestIds, setAssignedTestIds] = useState([]);
  // FilterableMultiSelect takes its selection on mount only, so it waits for
  // both reads rather than mounting empty and never catching up.
  const [testsReady, setTestsReady] = useState(false);

  useEffect(() => {
    // The unscoped catalog, as the participant enrollment form uses: /rest/test-list
    // is narrowed by the caller's Results lab-unit role, which a QA Officer has no
    // reason to hold.
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", (data) => {
      // A nameless entry would reach Carbon's sort as undefined and take the
      // dialog down with it, so it is dropped rather than offered.
      const items = (Array.isArray(data) ? data : [])
        .filter((t) => t && t.id != null && t.value)
        .map((t) => ({ id: String(t.id), text: String(t.value) }));
      setTests(items);

      if (!isEditing) {
        setTestsReady(true);
        return;
      }
      getFromOpenElisServer(
        `/rest/eqa/programs/${program.id}/tests`,
        (assignments) => {
          const ids = (Array.isArray(assignments) ? assignments : [])
            .filter((a) => a.isActive !== false)
            .map((a) => String(a.testId));
          setAssignedTestIds(ids);
          setSelectedTests(items.filter((t) => ids.includes(t.id)));
          setTestsReady(true);
        },
      );
    });
  }, []);

  const saveTestAssignments = (programId, done) => {
    const chosen = selectedTests.map((t) => String(t.id));
    const unchanged =
      chosen.length === assignedTestIds.length &&
      chosen.every((id) => assignedTestIds.includes(id));
    // Every save would otherwise delete and re-create the rows, so a rename would
    // churn assignments it never touched.
    if (unchanged) {
      done();
      return;
    }
    putToOpenElisServerFullResponse(
      `/rest/eqa/programs/${programId}/tests`,
      JSON.stringify({ testIds: chosen.map(Number) }),
      (response) => {
        if (response && response.ok) {
          done();
          return;
        }
        Promise.resolve(
          response ? response.json().catch(() => null) : null,
        ).then((body) =>
          setSaveError(
            resolveApiErrorMessage(intl, body, "eqa.program.tests.saveFailed"),
          ),
        );
      },
    );
  };

  // keep the modal open and show why the server refused, instead of closing as if saved
  const handleResponse = (response) => {
    if (response && response.ok) {
      Promise.resolve(response.json().catch(() => null)).then((body) => {
        const programId = isEditing ? program.id : body?.id;
        if (programId == null) {
          if (onClose) onClose();
          return;
        }
        saveTestAssignments(programId, () => {
          if (onClose) onClose();
        });
      });
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
        <TextArea
          id="program-description"
          labelText={intl.formatMessage({ id: "eqa.program.description" })}
          placeholder={intl.formatMessage({
            id: "eqa.admin.form.description.placeholder",
          })}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        {testsReady && (
          <FilterableMultiSelect
            id="program-tests"
            titleText={intl.formatMessage({ id: "eqa.program.tests" })}
            helperText={intl.formatMessage({ id: "eqa.program.tests.helper" })}
            items={tests}
            itemToString={(item) => (item ? item.text : "")}
            initialSelectedItems={selectedTests}
            onChange={(e) => setSelectedTests(e.selectedItems)}
            placeholder={intl.formatMessage({
              id: "eqa.program.tests.select",
            })}
          />
        )}
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
