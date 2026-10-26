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
import {
  getReferenceOptions,
  getReportingRuleHistory,
  getReportingRules,
  getReportingRuleSettings,
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

const displayValue = (value) => {
  if (value === null || value === undefined) return "";
  if (Array.isArray(value)) return value.join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
};

export default function AstRulesPage() {
  const intl = useIntl();
  const t = (key) =>
    intl.formatMessage({ id: `microbiology.admin.astRules.${key}` });
  const [rules, setRules] = useState([]);
  const [settings, setSettings] = useState({});
  const [options, setOptions] = useState({
    antibiotics: [],
    organisms: [],
    organismGroups: [],
    sampleTypes: [],
  });
  const [draft, setDraft] = useState(null);
  const [history, setHistory] = useState(null);
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      getReportingRules(controller.signal),
      getReportingRuleSettings(controller.signal),
      getReferenceOptions("antibiotics", controller.signal),
      getReferenceOptions("organisms", controller.signal),
      getReferenceOptions("organism-groups", controller.signal),
      getReferenceOptions("specimen-types", controller.signal),
    ])
      .then(
        ([
          nextRules,
          nextSettings,
          antibiotics,
          organisms,
          organismGroups,
          sampleTypes,
        ]) => {
          setRules(nextRules || []);
          setSettings(nextSettings || {});
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
        id: draft.id,
        antibioticId: draft.antibioticId || "",
        organismId: draft.organismId || "",
        organismGroup: draft.organismGroup || "",
        sampleTypeIds: draft.sampleTypeIds || [],
        clinicalContext: draft.clinicalContext || "",
        reportBehavior: draft.reportBehavior || "",
        cascadeCondition: draft.cascadeCondition || "",
        addAutomatically: Boolean(draft.addAutomatically),
        active: draft.active !== false,
      });
      setRules((await getReportingRules()) || []);
      setDraft(null);
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };

  const selector = (field, label, items, blank, reset = {}) => (
    <Select
      id={`reporting-rule-${field}`}
      labelText={t(label)}
      value={draft?.[field] || ""}
      onChange={(event) =>
        setDraft({
          ...draft,
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
                <TableCell colSpan={7}>{group.label}</TableCell>
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
    </Stack>
  );
}
