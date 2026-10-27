import React, { useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  Form,
  InlineNotification,
  Modal,
  MultiSelect,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import config from "../../../config.json";
import {
  getInterpretationRules,
  getReferenceOptions,
  getReportingRuleHistory,
  getReportingRules,
  getReportingRuleSettings,
  previewReportingRules,
  saveInterpretationRule,
  saveReportingRule,
  saveReportingRuleSettings,
} from "./api";

const emptyRule = {
  antibioticId: "",
  organismId: "",
  organismGroup: "",
  sampleTypeIds: [],
  clinicalContext: "",
  reportBehavior: "ALWAYS",
  cascadeCondition: "",
  note: "",
  noteWhen: "ALWAYS",
  addAutomatically: false,
  active: true,
};

const behaviors = {
  ALWAYS: "behaviorAlways",
  SUPPRESS_UNLESS_RESISTANT: "behaviorSuppress",
  CASCADE: "behaviorCascade",
  DONT_REPORT: "behaviorDontReport",
};

const standards = {
  CLSI: "standardClsi",
  EUCAST: "standardEucast",
  LOCAL: "standardLocal",
};

const results = {
  SUSCEPTIBLE: "resultS",
  INTERMEDIATE: "resultI",
  RESISTANT: "resultR",
};

const noteWhens = {
  ALWAYS: "noteAlways",
  WHEN_R: "noteWhenR",
  WHEN_S: "noteWhenS",
};

const displayValue = (value) => {
  if (value === null || value === undefined) return "";
  if (Array.isArray(value)) return value.join(", ");
  return String(value);
};

export default function AstRulesPage() {
  const intl = useIntl();
  const t = (key) =>
    intl.formatMessage({ id: `microbiology.admin.astRules.${key}` });
  const [rules, setRules] = useState([]);
  const [interpretationRules, setInterpretationRules] = useState([]);
  const [settings, setSettings] = useState({});
  const [options, setOptions] = useState({
    antibiotics: [],
    organisms: [],
    organismGroups: [],
    sampleTypes: [],
  });
  const [draft, setDraft] = useState(null);
  const [interpretationDraft, setInterpretationDraft] = useState(null);
  const [history, setHistory] = useState(null);
  const [preview, setPreview] = useState({
    organismId: "",
    sampleTypeId: "",
    clinicalContext: "",
    readings: [],
  });
  const [previewResult, setPreviewResult] = useState(null);
  const [previewError, setPreviewError] = useState(false);
  const [error, setError] = useState(false);
  const [interpretationError, setInterpretationError] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      getReportingRules(controller.signal),
      getReportingRuleSettings(controller.signal),
      getInterpretationRules(controller.signal),
      getReferenceOptions("antibiotics", controller.signal),
      getReferenceOptions("organisms", controller.signal),
      getReferenceOptions("organism-groups", controller.signal),
      getReferenceOptions("specimen-types", controller.signal),
    ])
      .then(
        ([
          nextRules,
          nextSettings,
          nextInterpretationRules,
          antibiotics,
          organisms,
          organismGroups,
          sampleTypes,
        ]) => {
          setRules(nextRules || []);
          setSettings(nextSettings || {});
          setInterpretationRules(nextInterpretationRules || []);
          setOptions({
            antibiotics: antibiotics || [],
            organisms: organisms || [],
            organismGroups: organismGroups || [],
            sampleTypes: sampleTypes || [],
          });
        },
      )
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(true);
      });
    return () => controller.abort();
  }, []);

  const changeStandard = async (event) => {
    const writtenFor = event.target.value;
    setError(false);
    try {
      const next = await saveReportingRuleSettings({ writtenFor });
      setSettings(next || { ...settings, writtenFor });
    } catch {
      setError(true);
    }
  };

  const runPreview = async () => {
    setPreviewError(false);
    try {
      const result = await previewReportingRules(preview);
      setPreviewResult(result);
    } catch {
      setPreviewError(true);
      setPreviewResult(null);
    }
  };

  const changePreview = (next) => {
    setPreview(next);
    setPreviewResult(null);
  };

  const changePreviewReading = (index, field, value) =>
    changePreview({
      ...preview,
      readings: preview.readings.map((reading, position) =>
        position === index ? { ...reading, [field]: value } : reading,
      ),
    });

  const openHistory = async (rule) => {
    setError(false);
    try {
      const entries = await getReportingRuleHistory(rule.id);
      setHistory({ rule, entries: entries || [] });
    } catch {
      setError(true);
    }
  };

  const save = async () => {
    setBusy(true);
    setError(false);
    try {
      await saveReportingRule({
        ...draft,
        noteWhen: draft.noteWhen || "ALWAYS",
      });
      setRules((await getReportingRules()) || []);
      setDraft(null);
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };

  const saveInterpretation = async () => {
    setBusy(true);
    setInterpretationError(false);
    try {
      await saveInterpretationRule({
        id: interpretationDraft.id,
        antibioticId: interpretationDraft.antibioticId || "",
        organismId: interpretationDraft.organismId || "",
        organismGroup: interpretationDraft.organismGroup || "",
        interpretation: "RESISTANT",
        source: interpretationDraft.source || "",
        active: interpretationDraft.active !== false,
      });
      setInterpretationRules((await getInterpretationRules()) || []);
      setInterpretationDraft(null);
    } catch {
      setInterpretationError(true);
    } finally {
      setBusy(false);
    }
  };

  const selectorFor =
    (idPrefix, value, setValue) =>
    (field, label, items, blank, reset = {}) => (
      <Select
        id={`${idPrefix}-${field}`}
        labelText={t(label)}
        value={value?.[field] || ""}
        onChange={(event) =>
          setValue({
            ...value,
            ...reset,
            [field]: event.target.value,
          })
        }
        disabled={busy}
      >
        {blank && <SelectItem value="" text={t(blank)} />}
        {items.map((item) => (
          <SelectItem key={item.id} value={item.id} text={item.label} />
        ))}
      </Select>
    );

  const selector = selectorFor("reporting-rule", draft, setDraft);
  const interpretationSelector = selectorFor(
    "interpretation-rule",
    interpretationDraft,
    setInterpretationDraft,
  );

  const groups = [];
  const groupByKey = new Map();
  for (const rule of rules) {
    const key = rule.organismId
      ? `organism:${rule.organismId}`
      : rule.organismGroup
        ? `group:${rule.organismGroup}`
        : "any";
    if (!groupByKey.has(key)) {
      const group = {
        key,
        label: rule.organismName || rule.organismGroup || t("anyOrganism"),
        rules: [],
      };
      groupByKey.set(key, group);
      groups.push(group);
    }
    groupByKey.get(key).rules.push(rule);
  }

  const changes = (entry) => {
    const before = JSON.parse(entry.before || "{}");
    const after = JSON.parse(entry.after);
    const keys = Array.from(
      new Set([...Object.keys(before), ...Object.keys(after)]),
    );
    return keys
      .filter((key) => displayValue(before[key]) !== displayValue(after[key]))
      .map((key) => ({
        key,
        message: intl.formatMessage(
          { id: "microbiology.admin.astRules.change" },
          {
            field: key,
            before: displayValue(before[key]),
            after: displayValue(after[key]),
          },
        ),
      }));
  };

  return (
    <Stack gap={5}>
      <Select
        id="reporting-rules-written-for"
        labelText={t("writtenFor")}
        value={settings.writtenFor || ""}
        onChange={changeStandard}
      >
        <SelectItem value="" text={t("choose")} />
        <SelectItem value="CLSI" text={t("standardClsi")} />
        <SelectItem value="EUCAST" text={t("standardEucast")} />
        <SelectItem value="LOCAL" text={t("standardLocal")} />
      </Select>
      {settings.writtenFor &&
        settings.activeAuthority &&
        settings.writtenFor !== settings.activeAuthority && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage(
              { id: "microbiology.admin.astRules.standardWarning" },
              {
                writtenFor: t(standards[settings.writtenFor]),
                activeAuthority: settings.activeAuthority,
              },
            )}
          />
        )}
      {error && !draft && (
        <InlineNotification kind="error" title={t("error")} hideCloseButton />
      )}
      <Button onClick={() => setDraft({ ...emptyRule })}>{t("addRule")}</Button>
      <Button
        kind="tertiary"
        href={
          config.serverBaseUrl +
          "/rest/microbiology/admin/reporting-rules/export"
        }
      >
        {t("export")}
      </Button>
      <Table aria-label={t("rulesTable")}>
        <TableHead>
          <TableRow>
            {[
              "antibiotic",
              "sampleTypes",
              "indication",
              "reportDefault",
              "condition",
              "addAutomatically",
              "note",
              "actions",
            ].map((header) => (
              <TableHeader key={header}>{t(header)}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {groups.map((group) => (
            <React.Fragment key={group.key}>
              <TableRow>
                <TableCell colSpan={8}>{group.label}</TableCell>
              </TableRow>
              {group.rules.map((rule) => (
                <TableRow key={rule.id}>
                  <TableCell>
                    {rule.antibioticName}
                    {rule.active === false ? ` ${t("inactive")}` : ""}
                  </TableCell>
                  <TableCell>
                    {rule.sampleTypeNames?.length
                      ? rule.sampleTypeNames.join(", ")
                      : t("anySampleType")}
                  </TableCell>
                  <TableCell>
                    {rule.clinicalContext === "NON_MENINGITIS"
                      ? t("nonMeningitis")
                      : t("any")}
                  </TableCell>
                  <TableCell>{t(behaviors[rule.reportBehavior])}</TableCell>
                  <TableCell>{rule.cascadeCondition || ""}</TableCell>
                  <TableCell>
                    {rule.reportBehavior === "CASCADE"
                      ? rule.addAutomatically
                        ? t("yes")
                        : t("no")
                      : ""}
                  </TableCell>
                  <TableCell>
                    {rule.note
                      ? intl.formatMessage(
                          { id: "microbiology.admin.astRules.noteSummary" },
                          {
                            when: t(noteWhens[rule.noteWhen]),
                            note: rule.note,
                          },
                        )
                      : ""}
                  </TableCell>
                  <TableCell>
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() =>
                        setDraft({
                          ...rule,
                          sampleTypeIds: [...(rule.sampleTypeIds || [])],
                        })
                      }
                    >
                      {t("edit")}
                    </Button>
                    <Button
                      kind="ghost"
                      size="sm"
                      aria-label={intl.formatMessage(
                        { id: "microbiology.admin.astRules.historyFor" },
                        { antibiotic: rule.antibioticName },
                      )}
                      onClick={() => openHistory(rule)}
                    >
                      {t("history")}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </React.Fragment>
          ))}
        </TableBody>
      </Table>
      <h2>{t("interpretationRules")}</h2>
      <Button onClick={() => setInterpretationDraft({ ...emptyRule })}>
        {t("addInterpretationRule")}
      </Button>
      <Table aria-label={t("interpretationRules")}>
        <TableHead>
          <TableRow>
            {[
              "organism",
              "antibiotic",
              "then",
              "source",
              "status",
              "actions",
            ].map((header) => (
              <TableHeader key={header}>{t(header)}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {interpretationRules.map((rule) => (
            <TableRow key={rule.id}>
              <TableCell>{rule.organismName || rule.organismGroup}</TableCell>
              <TableCell>{rule.antibioticName}</TableCell>
              <TableCell>
                {rule.interpretation === "RESISTANT" ? t("alwaysR") : ""}
              </TableCell>
              <TableCell>{rule.source || ""}</TableCell>
              <TableCell>
                {rule.active === false ? t("inactive") : t("active")}
              </TableCell>
              <TableCell>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    setInterpretationDraft({
                      ...rule,
                      source: rule.source || "",
                    })
                  }
                >
                  {t("edit")}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {interpretationDraft && (
        <Modal
          open
          modalHeading={t("interpretationRule")}
          primaryButtonText={t("save")}
          secondaryButtonText={t("cancel")}
          onRequestClose={() => {
            if (!busy) {
              setInterpretationDraft(null);
              setInterpretationError(false);
            }
          }}
          onRequestSubmit={saveInterpretation}
          primaryButtonDisabled={busy}
        >
          <Form>
            <Stack gap={5}>
              {interpretationError && (
                <InlineNotification
                  kind="error"
                  title={t("interpretationError")}
                  hideCloseButton
                />
              )}
              {interpretationSelector(
                "antibioticId",
                "antibiotic",
                options.antibiotics,
                "choose",
              )}
              {interpretationSelector(
                "organismId",
                "organism",
                options.organisms,
                "chooseOrganism",
                { organismGroup: "" },
              )}
              {interpretationSelector(
                "organismGroup",
                "organismGroup",
                options.organismGroups,
                "chooseGroup",
                { organismId: "" },
              )}
              <TextInput
                id="interpretation-rule-source"
                labelText={t("source")}
                value={interpretationDraft.source || ""}
                onChange={(event) =>
                  setInterpretationDraft({
                    ...interpretationDraft,
                    source: event.target.value,
                  })
                }
                disabled={busy}
              />
              <Checkbox
                id="interpretation-rule-active"
                labelText={t("active")}
                checked={interpretationDraft.active !== false}
                onChange={(_, { checked }) =>
                  setInterpretationDraft({
                    ...interpretationDraft,
                    active: checked,
                  })
                }
                disabled={busy}
              />
            </Stack>
          </Form>
        </Modal>
      )}
      {draft && (
        <Modal
          open
          modalHeading={draft.id ? t("editRule") : t("addRule")}
          primaryButtonText={t("save")}
          secondaryButtonText={t("cancel")}
          onRequestClose={() => !busy && setDraft(null)}
          onRequestSubmit={save}
          primaryButtonDisabled={busy}
        >
          <Form>
            <Stack gap={5}>
              {error && (
                <InlineNotification
                  kind="error"
                  title={t("error")}
                  hideCloseButton
                />
              )}
              {selector(
                "antibioticId",
                "antibiotic",
                options.antibiotics,
                "choose",
              )}
              {selector(
                "organismId",
                "organism",
                options.organisms,
                "anyOrganism",
                { organismGroup: "" },
              )}
              {selector(
                "organismGroup",
                "organismGroup",
                options.organismGroups,
                "anyGroup",
                { organismId: "" },
              )}
              <MultiSelect
                id="reporting-rule-sample-types"
                titleText={t("sampleTypes")}
                label={t("chooseSampleTypes")}
                items={options.sampleTypes}
                itemToString={(item) => item.label}
                selectedItems={options.sampleTypes.filter((item) =>
                  (draft.sampleTypeIds || []).includes(item.id),
                )}
                onChange={({ selectedItems }) =>
                  setDraft({
                    ...draft,
                    sampleTypeIds: selectedItems.map((item) => item.id),
                  })
                }
                disabled={busy}
              />
              <Select
                id="reporting-rule-clinical-context"
                labelText={t("indication")}
                value={draft.clinicalContext || ""}
                onChange={(event) =>
                  setDraft({ ...draft, clinicalContext: event.target.value })
                }
                disabled={busy}
              >
                <SelectItem value="" text={t("any")} />
                <SelectItem value="NON_MENINGITIS" text={t("nonMeningitis")} />
              </Select>
              <Select
                id="reporting-rule-report-behavior"
                labelText={t("reportDefault")}
                value={draft.reportBehavior || ""}
                onChange={(event) =>
                  setDraft({ ...draft, reportBehavior: event.target.value })
                }
                disabled={busy}
              >
                {Object.entries(behaviors).map(([value, label]) => (
                  <SelectItem key={value} value={value} text={t(label)} />
                ))}
              </Select>
              {draft.reportBehavior === "CASCADE" && (
                <>
                  <TextInput
                    id="reporting-rule-cascade-condition"
                    labelText={t("cascadeCondition")}
                    value={draft.cascadeCondition || ""}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        cascadeCondition: event.target.value,
                      })
                    }
                    disabled={busy}
                  />
                  <Checkbox
                    id="reporting-rule-add-automatically"
                    labelText={t("addAutomatically")}
                    checked={Boolean(draft.addAutomatically)}
                    onChange={(_, { checked }) =>
                      setDraft({ ...draft, addAutomatically: checked })
                    }
                    disabled={busy}
                  />
                </>
              )}
              <TextInput
                id="reporting-rule-note"
                labelText={t("note")}
                maxCount={1000}
                enableCounter
                value={draft.note || ""}
                onChange={(event) =>
                  setDraft({ ...draft, note: event.target.value })
                }
                disabled={busy}
              />
              <Select
                id="reporting-rule-note-when"
                labelText={t("noteWhen")}
                value={draft.noteWhen || "ALWAYS"}
                onChange={(event) =>
                  setDraft({ ...draft, noteWhen: event.target.value })
                }
                disabled={busy}
              >
                {Object.entries(noteWhens).map(([value, label]) => (
                  <SelectItem key={value} value={value} text={t(label)} />
                ))}
              </Select>
              <Checkbox
                id="reporting-rule-active"
                labelText={t("active")}
                checked={draft.active !== false}
                onChange={(_, { checked }) =>
                  setDraft({ ...draft, active: checked })
                }
                disabled={busy}
              />
            </Stack>
          </Form>
        </Modal>
      )}
      {history && (
        <Modal
          open
          passiveModal
          modalHeading={t("historyTitle")}
          onRequestClose={() => setHistory(null)}
        >
          <Stack gap={5}>
            {history.entries.map((entry, index) => (
              <div key={entry.revision || index}>
                <p>
                  {intl.formatDate(new Date(entry.changedAt), {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                </p>
                <p>{entry.changedByName}</p>
                {changes(entry).map((change) => (
                  <p key={change.key}>{change.message}</p>
                ))}
              </div>
            ))}
          </Stack>
        </Modal>
      )}
      <h2>{t("preview")}</h2>
      <Stack gap={5}>
        <Select
          id="reporting-rule-preview-organism"
          labelText={t("previewOrganism")}
          value={preview.organismId}
          onChange={(event) =>
            changePreview({ ...preview, organismId: event.target.value })
          }
        >
          <SelectItem value="" text={t("choose")} />
          {options.organisms.map((item) => (
            <SelectItem key={item.id} value={item.id} text={item.label} />
          ))}
        </Select>
        <Select
          id="reporting-rule-preview-sample-type"
          labelText={t("previewSampleType")}
          value={preview.sampleTypeId}
          onChange={(event) =>
            changePreview({ ...preview, sampleTypeId: event.target.value })
          }
        >
          <SelectItem value="" text={t("anySampleType")} />
          {options.sampleTypes.map((item) => (
            <SelectItem key={item.id} value={item.id} text={item.label} />
          ))}
        </Select>
        <Select
          id="reporting-rule-preview-clinical-context"
          labelText={t("previewIndication")}
          value={preview.clinicalContext}
          onChange={(event) =>
            changePreview({ ...preview, clinicalContext: event.target.value })
          }
        >
          <SelectItem value="" text={t("any")} />
          <SelectItem value="NON_MENINGITIS" text={t("nonMeningitis")} />
        </Select>
        {preview.readings.map((reading, index) => (
          <div key={index}>
            <Select
              id={`reporting-rule-preview-antibiotic-${index}`}
              labelText={intl.formatMessage(
                { id: "microbiology.admin.astRules.previewAntibiotic" },
                { n: index + 1 },
              )}
              value={reading.antibioticId}
              onChange={(event) =>
                changePreviewReading(index, "antibioticId", event.target.value)
              }
            >
              <SelectItem value="" text={t("choose")} />
              {options.antibiotics.map((item) => (
                <SelectItem key={item.id} value={item.id} text={item.label} />
              ))}
            </Select>
            <Select
              id={`reporting-rule-preview-result-${index}`}
              labelText={intl.formatMessage(
                { id: "microbiology.admin.astRules.previewResult" },
                { n: index + 1 },
              )}
              value={reading.interpretation}
              onChange={(event) =>
                changePreviewReading(
                  index,
                  "interpretation",
                  event.target.value,
                )
              }
            >
              {Object.entries(results).map(([value, label]) => (
                <SelectItem key={value} value={value} text={t(label)} />
              ))}
            </Select>
          </div>
        ))}
        <Button
          kind="ghost"
          onClick={() =>
            changePreview({
              ...preview,
              readings: [
                ...preview.readings,
                { antibioticId: "", interpretation: "SUSCEPTIBLE" },
              ],
            })
          }
        >
          {t("addAntibiotic")}
        </Button>
        <Button onClick={runPreview} disabled={!preview.organismId}>
          {t("preview")}
        </Button>
        {previewError && (
          <InlineNotification
            kind="error"
            title={t("previewError")}
            hideCloseButton
          />
        )}
        {previewResult?.defaultPanelName && (
          <p>
            {intl.formatMessage(
              { id: "microbiology.admin.astRules.previewDefaultPanel" },
              { name: previewResult.defaultPanelName },
            )}
          </p>
        )}
        {previewResult && (
          <Table aria-label={t("previewResultTable")}>
            <TableHead>
              <TableRow>
                {[
                  "antibiotic",
                  "previewMeasured",
                  "previewInterpretedAs",
                  "previewReport",
                  "previewWhy",
                  "note",
                ].map((header) => (
                  <TableHeader key={header}>{t(header)}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {previewResult.rows.map((row, index) => (
                <TableRow key={index}>
                  <TableCell>{row.antibioticName}</TableCell>
                  <TableCell>{t(results[row.measured])}</TableCell>
                  <TableCell>
                    {t(results[row.interpretation])}
                    {row.expertRule && (
                      <div>
                        {intl.formatMessage(
                          { id: "microbiology.m6.expertRuleLine" },
                          {
                            organism: options.organisms.find(
                              (item) => item.id === preview.organismId,
                            )?.label,
                            antibiotic: row.antibioticName,
                          },
                        )}
                      </div>
                    )}
                  </TableCell>
                  <TableCell>{row.reported ? t("yes") : t("no")}</TableCell>
                  <TableCell>
                    {intl.formatMessage(
                      {
                        id: row.reportRuleId
                          ? "microbiology.admin.astRules.previewRule"
                          : "microbiology.admin.astRules.previewPanelOrDefault",
                      },
                      { behavior: t(behaviors[row.reportBehavior]) },
                    )}
                  </TableCell>
                  <TableCell>{row.note || ""}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Stack>
    </Stack>
  );
}
