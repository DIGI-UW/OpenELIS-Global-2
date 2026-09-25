import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";
import {
  getAnalyzerTypeDraft,
  updateAnalyzerTypeDraft,
} from "../../../services/analyzerService";
import ControlRecognitionDraftEditor from "./ControlRecognitionDraftEditor";
import ProfileStringList from "./ProfileStringList";
import ProfileTestDefinitions from "./ProfileTestDefinitions";

// These choices describe the published Bridge v1 contract, never instrument defaults.
const FORMATS = ["CSV", "TSV", "XLS", "XLSX", "ODS", "XML"];
const COLUMN_FIELDS = [
  "sampleId",
  "testCode",
  "result",
  "units",
  "interpretation",
  "qcTask",
  "testDate",
  "ctValue",
  "position",
];
const failed = (response) =>
  !response ||
  Boolean(response.error) ||
  response.status >= 400 ||
  response.statusCode >= 400;
const encode = (value) => JSON.stringify(value);
const columnsOf = (profile) =>
  Object.entries(profile?.column_mapping || {}).map(([source, field]) => ({
    source,
    field,
  }));

const ProfileDraftEditor = ({ draft: initialDraft, onStateChange }) => {
  const intl = useIntl();
  const text = (key, values) => profileAuthoringMessage(intl, key, values);
  const [draft, setDraft] = useState(initialDraft);
  const [profile, setProfile] = useState(initialDraft.profile || {});
  const [columns, setColumns] = useState(columnsOf(initialDraft.profile));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [conflict, setConflict] = useState(false);
  const [saved, setSaved] = useState(false);
  const [recognition, setRecognition] = useState(null);
  const [recognitionVersion, setRecognitionVersion] = useState(0);
  const draftId = initialDraft.draftId;
  const columnsChanged = encode(columns) !== encode(columnsOf(draft.profile));
  const dirty =
    encode(profile) !== encode(draft.profile || {}) || columnsChanged;
  const columnKeys = columns.map((row) => row.source.trim());
  const columnsValid =
    columns.every((row) => row.source.trim() && row.field) &&
    new Set(columnKeys).size === columnKeys.length;
  const fieldsLocked =
    saving || conflict || Boolean(recognition?.dirty || recognition?.saving);
  const publishable =
    !dirty &&
    !saving &&
    !error &&
    !conflict &&
    recognition?.publishable === true;

  useEffect(() => {
    onStateChange?.({
      loaded: true,
      dirty,
      publishable,
      validationIssues:
        recognition?.validationIssues || draft.validationIssues || [],
    });
  }, [dirty, draft.validationIssues, onStateChange, publishable, recognition]);

  const accept = useCallback((response) => {
    setDraft(response);
    setProfile(response.profile);
    setColumns(columnsOf(response.profile));
    setRecognition(null);
    setRecognitionVersion((value) => value + 1);
  }, []);

  const reload = () => {
    setSaving(true);
    getAnalyzerTypeDraft(draftId, (response) => {
      setSaving(false);
      if (
        failed(response) ||
        response.draftId !== draftId ||
        !response.profile
      ) {
        setError(response?.error || text("loadError"));
        setConflict(true);
        return;
      }
      accept(response);
      setError(null);
      setConflict(false);
    });
  };

  const change = (path, value) => {
    setSaved(false);
    setError(null);
    setProfile((current) => {
      const next = JSON.parse(encode(current));
      let target = next;
      path.slice(0, -1).forEach((key) => {
        target = target[key] ||= {};
      });
      const key = path[path.length - 1];
      if (value === undefined) delete target[key];
      else target[key] = value;
      return next;
    });
  };
  const changeColumns = (next) => {
    setSaved(false);
    setError(null);
    setColumns(next);
  };
  const valueAt = (path) => path.reduce((value, key) => value?.[key], profile);
  const input = (path, label, kind = "text") => (
    <TextInput
      key={path.join(".")}
      id={`profile-${path.join("-")}`}
      labelText={text(label)}
      type={kind}
      value={valueAt(path) ?? ""}
      onChange={(event) => {
        const value = event.target.value;
        change(
          path,
          value === "" ? undefined : kind === "number" ? Number(value) : value,
        );
      }}
    />
  );
  const select = (path, label, choices, disabled = false) => (
    <Select
      key={path.join(".")}
      disabled={disabled}
      id={`profile-${path.join("-")}`}
      labelText={text(label)}
      aria-label={text(label)}
      value={String(valueAt(path) ?? "")}
      onChange={(event) => change(path, event.target.value || undefined)}
    >
      <SelectItem value="" text={text("choose")} />
      {choices.map((value) => (
        <SelectItem key={value} value={value} text={value} />
      ))}
    </Select>
  );
  const boolean = (path, label) => (
    <Select
      key={path.join(".")}
      id={`profile-${path.join("-")}`}
      labelText={text(label)}
      aria-label={text(label)}
      value={String(valueAt(path) ?? "")}
      onChange={(event) =>
        change(
          path,
          event.target.value === "" ? undefined : event.target.value === "true",
        )
      }
    >
      <SelectItem value="" text={text("choose")} />
      <SelectItem value="true" text={text("yes")} />
      <SelectItem value="false" text={text("no")} />
    </Select>
  );

  const save = () => {
    if (!dirty || !columnsValid || fieldsLocked) return;
    setSaving(true);
    setError(null);
    setSaved(false);
    // Detect changes already saved by another editor before replacement.
    // Atomic protection for simultaneous writes still requires a Bridge revision precondition.
    getAnalyzerTypeDraft(draftId, (latest) => {
      if (failed(latest) || latest.draftId !== draftId || !latest.profile) {
        setSaving(false);
        setError(latest?.error || text("loadError"));
        return;
      }
      if (encode(latest.profile) !== encode(draft.profile)) {
        setSaving(false);
        setConflict(true);
        setError(text("conflict"));
        return;
      }
      const updated = columnsChanged
        ? {
            ...profile,
            column_mapping: Object.fromEntries(
              columns.map(({ source, field }) => [source, field]),
            ),
          }
        : profile;
      updateAnalyzerTypeDraft(draftId, updated, (response) => {
        setSaving(false);
        if (
          failed(response) ||
          response.draftId !== draftId ||
          !response.profile
        ) {
          setError(response?.error || text("saveError"));
          return;
        }
        accept(response);
        setSaved(true);
      });
    });
  };

  const protocol = profile.protocol?.name;
  const file = protocol === "FILE";
  const socket = protocol === "ASTM" || protocol === "HL7";
  const connectionFields = profile.connectionFields || [];
  const updateField = (index, key, value) =>
    change(
      ["connectionFields"],
      connectionFields.map((field, current) =>
        current === index ? { ...field, [key]: value } : field,
      ),
    );

  return (
    <section
      className="analyzer-type-profile-editor"
      aria-label={text("heading")}
    >
      {!initialDraft.profile?.protocol?.name && <p>{text("newFileScope")}</p>}
      <fieldset disabled={fieldsLocked} className="analyzer-type-modal__form">
        <legend>{text("heading")}</legend>
        {input(["profileMeta", "displayName"], "name")}
        {input(["profileMeta", "version"], "version")}
        {select(["profileMeta", "confidence"], "confidence", [
          "VALIDATED",
          "HIGH",
          "MEDIUM_HIGH",
          "MEDIUM",
          "ESTIMATED",
          "LOW",
          "NA",
        ])}
        {input(["manufacturer"], "manufacturer")}
        {input(["model"], "model")}
        {select(["category"], "category", [
          "HEMATOLOGY",
          "CHEMISTRY",
          "IMMUNOLOGY",
          "MICROBIOLOGY",
          "MOLECULAR",
          "COAGULATION",
        ])}
        {select(
          ["protocol", "name"],
          "protocol",
          initialDraft.profile?.protocol?.name
            ? [initialDraft.profile.protocol.name]
            : ["FILE"],
          Boolean(initialDraft.profile?.protocol?.name),
        )}
        {boolean(["capabilities", "inboundResults"], "inboundResults")}
        {boolean(["capabilities", "outboundOrders"], "outboundOrders")}
        {boolean(["capabilities", "connectionTest"], "connectionTest")}
        {file && (
          <>
            <h4>{text("fileHeading")}</h4>
            {select(["protocol", "format"], "fileFormat", FORMATS)}
            {select(
              ["configDefaults", "fileFormat"],
              "defaultFileFormat",
              FORMATS,
            )}
            {input(["configDefaults", "filePattern"], "filePattern")}
            {boolean(["configDefaults", "hasHeader"], "hasHeader")}
            {input(["configDefaults", "delimiter"], "delimiter")}
            {input(["configDefaults", "encoding"], "encoding")}
            {input(["configDefaults", "sheetIndex"], "sheetIndex", "number")}
            {input(["configDefaults", "skipRows"], "skipRows", "number")}
            <ProfileStringList
              id="extensions"
              label={text("extensions")}
              values={profile.supported_extensions || []}
              onChange={(value) => change(["supported_extensions"], value)}
            />
            <h4>{text("columns")}</h4>
            <p>{text("columnsHelp")}</p>
            {columns.map((row, index) => (
              <div
                key={index}
                role="group"
                aria-label={text("columnNumber", { number: index + 1 })}
              >
                <TextInput
                  id={`profile-column-${index}`}
                  labelText={text("columnName")}
                  value={row.source}
                  onChange={(event) =>
                    changeColumns(
                      columns.map((item, current) =>
                        current === index
                          ? { ...item, source: event.target.value }
                          : item,
                      ),
                    )
                  }
                />
                <Select
                  id={`profile-column-field-${index}`}
                  labelText={text("columnField")}
                  value={row.field}
                  onChange={(event) =>
                    changeColumns(
                      columns.map((item, current) =>
                        current === index
                          ? { ...item, field: event.target.value }
                          : item,
                      ),
                    )
                  }
                >
                  <SelectItem value="" text={text("choose")} />
                  {COLUMN_FIELDS.map((value) => (
                    <SelectItem
                      key={value}
                      value={value}
                      text={text(`semantic.${value}`)}
                    />
                  ))}
                </Select>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    changeColumns(
                      columns.filter((_, current) => current !== index),
                    )
                  }
                >
                  {text("removeColumn")}
                </Button>
              </div>
            ))}
            {!columnsValid && <p role="alert">{text("columnsInvalid")}</p>}
            <Button
              kind="tertiary"
              size="sm"
              onClick={() =>
                changeColumns([...columns, { source: "", field: "" }])
              }
            >
              {text("addColumn")}
            </Button>
            <ProfileStringList
              id="result-order"
              label={text("resultOrder")}
              values={profile.result_value_order || []}
              choices={["result", "ctValue", "interpretation"]}
              onChange={(value) => change(["result_value_order"], value)}
            />
          </>
        )}
        {socket && (
          <>
            <h4>{text("communicationHeading")}</h4>
            {input(["analyzer_name"], "analyzerName")}
            {input(["identifier_pattern"], "identifierPattern")}
            {input(["protocol", "version"], "protocolVersion")}
            {protocol === "ASTM" &&
              select(["protocol", "lowerLayerVersion"], "lowerLayerVersion", [
                "LIS01_A",
                "E1381_95",
              ])}
            {protocol === "HL7" && input(["msh3_pattern"], "msh3Pattern")}
            {select(["communication", "mode"], "communicationMode", [
              "ANALYZER_INITIATED",
              "LIS_INITIATED",
              "BOTH",
            ])}
            {boolean(
              ["communication", "supports_lis_initiated"],
              "lisInitiated",
            )}
            {select(["configDefaults", "connectionRole"], "connectionRole", [
              "SERVER",
              "CLIENT",
            ])}
            {select(
              ["configDefaults", "transport"],
              "transport",
              profile.transport || [],
            )}
            {input(["configDefaults", "port"], "port", "number")}
            {select(["configDefaults", "aggregationMode"], "aggregationMode", [
              "PER_MESSAGE",
              "BY_SPECIMEN",
            ])}
            {input(
              ["configDefaults", "aggregationWindowSeconds"],
              "aggregationWindowSeconds",
              "number",
            )}
          </>
        )}
        <ProfileTestDefinitions
          rows={profile.default_test_mappings || []}
          onChange={(rows) => change(["default_test_mappings"], rows)}
        />
        <h4>{text("connectionFields")}</h4>
        <p>{text("connectionFieldsHelp")}</p>
        {connectionFields.map((field, index) => (
          <div
            key={index}
            role="group"
            aria-label={text("connectionFieldNumber", { number: index + 1 })}
          >
            <TextInput
              id={`profile-connection-key-${index}`}
              labelText={text("fieldKey")}
              value={field.key || ""}
              onChange={(event) =>
                updateField(index, "key", event.target.value)
              }
            />
            <TextInput
              id={`profile-connection-label-${index}`}
              labelText={text("fieldLabel")}
              value={field.labelKey || ""}
              onChange={(event) =>
                updateField(index, "labelKey", event.target.value)
              }
            />
            <Select
              id={`profile-connection-kind-${index}`}
              labelText={text("inputKind")}
              value={field.inputKind || ""}
              onChange={(event) =>
                updateField(index, "inputKind", event.target.value)
              }
            >
              <SelectItem value="" text={text("choose")} />
              {[
                "TEXT",
                "NUMBER",
                "SELECT",
                "BOOLEAN",
                "SECRET",
                "FILE_PATH",
              ].map((value) => (
                <SelectItem
                  key={value}
                  value={value}
                  text={text(`inputKind.${value}`)}
                />
              ))}
            </Select>
            <Select
              id={`profile-connection-required-${index}`}
              labelText={text("required")}
              value={String(field.required ?? "")}
              onChange={(event) =>
                updateField(index, "required", event.target.value === "true")
              }
            >
              <SelectItem value="" text={text("choose")} />
              <SelectItem value="true" text={text("yes")} />
              <SelectItem value="false" text={text("no")} />
            </Select>
            <Button
              kind="ghost"
              size="sm"
              onClick={() =>
                change(
                  ["connectionFields"],
                  connectionFields.filter((_, current) => current !== index),
                )
              }
            >
              {text("removeField")}
            </Button>
          </div>
        ))}
        <Button
          kind="tertiary"
          size="sm"
          onClick={() =>
            change(
              ["connectionFields"],
              [
                ...connectionFields,
                { key: "", labelKey: "", inputKind: "", choices: [] },
              ],
            )
          }
        >
          {text("addField")}
        </Button>
        <Button
          kind="secondary"
          disabled={!dirty || !columnsValid || fieldsLocked}
          onClick={save}
        >
          {text("save")}
        </Button>
      </fieldset>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={text("error")}
          subtitle={error}
        />
      )}
      {conflict && (
        <Button kind="tertiary" onClick={reload} disabled={saving}>
          {text("reload")}
        </Button>
      )}
      {saved && (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={text("saved")}
        />
      )}
      {dirty ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={text("saveBeforeRecognition")}
        />
      ) : (
        <>
          <ControlRecognitionDraftEditor
            key={`${draftId}-${recognitionVersion}`}
            draftId={draftId}
            onStateChange={setRecognition}
            onSaved={reload}
          />
        </>
      )}
      {!dirty &&
        !recognition?.loaded &&
        (draft.validationIssues || []).length > 0 && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={text("validation")}
            subtitle={draft.validationIssues.join(" ")}
          />
        )}
    </section>
  );
};

export default ProfileDraftEditor;
