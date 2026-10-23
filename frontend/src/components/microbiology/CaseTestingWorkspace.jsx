import CaseAttachmentNotes from "./CaseAttachmentNotes";
import { labTimestamp } from "./MicrobiologyTime";
import AmendmentHistoryPanel from "./AmendmentHistoryPanel";
import CriticalCommunicationPanel from "./CriticalCommunicationPanel";
import CaseNonconformancePanel from "./CaseNonconformancePanel";
import CaseHistory from "./CaseHistory";
import CaseIncomingWorkspace from "./CaseIncomingWorkspace";
import CaseAstWorkspace from "./CaseAstWorkspace";
import CaseReportWorkspace from "./CaseReportWorkspace";
import React, { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Stack,
  Tile,
  Select,
  SelectItem,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeader,
  TableCell,
  TableContainer,
  Tag,
  TextArea,
  TextInput,
  Checkbox,
  ComboBox,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import CollectTestPickerSection from "../order/steps/sections/CollectTestPickerSection";
import PolymorphicResultCell, {
  blocksSaveOnPrecision,
} from "../resultPage/unified/PolymorphicResultCell";
import ESignatureButton from "../esignature/ESignatureButton";
import ResultAlertModal, {
  acknowledgementRefusal,
} from "../resultPage/ResultAlertModal";
import { FlagChip } from "../resultPage/unified/flags";
import { normalizeScientificNotation } from "../resultPage/scientificNotation";
import { getFromOpenElisServer } from "../utils/Utils";

function valid(row) {
  if (blocksSaveOnPrecision(row)) return false;
  if (!row.resultValue) return true;
  if (row.resultType === "N")
    return Number.isFinite(
      Number(
        normalizeScientificNotation(row.resultValue.replace(/^[<>]=?\s*/, "")),
      ),
    );
  if (row.resultType === "D")
    return (row.dictionaryResults || []).some(
      (option) => String(option.id) === row.resultValue,
    );
  return true;
}

function hasResult(row) {
  if (row.resultType === "M" || row.resultType === "C") {
    try {
      const groups = JSON.parse(row.multiSelectResultValues || "{}");
      return (
        groups !== null &&
        !Array.isArray(groups) &&
        typeof groups === "object" &&
        Object.values(groups).some(
          (group) =>
            typeof group === "string" &&
            group.split(",").some((id) => id.trim()),
        )
      );
    } catch {
      return false;
    }
  }
  return (
    Boolean(row.resultValue?.trim()) &&
    !(row.resultType === "D" && row.resultValue === "0")
  );
}

function ResultEditor({ test, service, caseId, onSaved, onCancel }) {
  const intl = useIntl();
  const t = (id) => intl.formatMessage({ id });
  const mounted = useRef(true);
  useLayoutEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );
  const [rows, setRows] = useState(() =>
    test.components.map((row) => ({
      ...row,
      resultValue: row.rawResultValue ?? row.resultValue,
    })),
  );
  const [external, setExternal] = useState(test.testedElsewhere);
  const [performerType, setPerformerType] = useState(
    test.performingLabId ? "lab" : "user",
  );
  const [performer, setPerformer] = useState(() =>
    test.performingLabId || test.performingUserId
      ? {
          id: test.performingLabId || test.performingUserId,
          value: test.performedByDisplay,
        }
      : null,
  );
  const [performers, setPerformers] = useState([]);
  const [search, setSearch] = useState("");
  const [date, setDate] = useState(test.performedAt || "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [alerts, setAlerts] = useState(null);
  const [resultNote, setResultNote] = useState("");
  useEffect(() => {
    if (!external) return undefined;
    let live = true;
    const url =
      performerType === "lab"
        ? `/rest/organization/search?search=${encodeURIComponent(search)}`
        : "/rest/users";
    if (performerType === "lab" && search.trim().length < 2) return undefined;
    getFromOpenElisServer(url, (data) => {
      if (!live) return;
      setPerformers(
        performerType === "lab"
          ? (data?.organizations || []).map((o) => ({
              id: String(o.id),
              value: o.organizationName,
            }))
          : Array.isArray(data)
            ? data
            : [],
      );
    });
    return () => {
      live = false;
    };
  }, [external, performerType, search]);
  const submit = async (provenance, confirmedAlerts = []) => {
    setSaving(true);
    setError(false);
    try {
      const data = provenance
        ? await service.setTestedElsewhere(caseId, test.analysisId, {
            version: test.version,
            testedElsewhere: external,
            performingLabId:
              external && performerType === "lab" ? performer?.id : null,
            performingUserId:
              external && performerType === "user" ? performer?.id : null,
            performedAt: date,
          })
        : await service.saveResults(caseId, test.analysisId, {
            version: test.version,
            note: resultNote,
            components: rows.map((row) => ({
              componentId: row.testResultComponentId,
              value: row.resultValue || "",
              multiSelectResultValues: row.multiSelectResultValues,
              criticalAcknowledged: confirmedAlerts.some(
                (a) =>
                  a.kind === "CRITICAL" &&
                  (!a.componentId ||
                    a.componentId === row.testResultComponentId),
              ),
              invalidResultConfirmed: confirmedAlerts.some(
                (a) =>
                  a.kind === "INVALID" &&
                  (!a.componentId ||
                    a.componentId === row.testResultComponentId),
              ),
            })),
          });
      if (mounted.current) onSaved(data, !provenance);
    } catch (failure) {
      if (!mounted.current) return;
      const refusal = acknowledgementRefusal(failure.response);
      if (refusal) setAlerts(refusal);
      else setError(true);
    } finally {
      if (mounted.current) setSaving(false);
    }
  };
  return (
    <Stack gap={4}>
      {alerts && (
        <ResultAlertModal
          open
          alerts={alerts.alerts}
          customCriticalMessage={alerts.customCriticalMessage}
          mode="save"
          onCorrect={() => setAlerts(null)}
          onConfirm={() => {
            const confirmed = alerts.alerts;
            setAlerts(null);
            submit(false, confirmed);
          }}
        />
      )}
      <TextArea
        id={`result-note-${test.analysisId}`}
        labelText={t("microbiology.testing.resultNote")}
        value={resultNote}
        maxLength={4000}
        disabled={saving}
        onChange={(event) => setResultNote(event.target.value)}
      />
      {rows.map((row) => (
        <div key={row.testResultComponentId || row.analysisId}>
          <p>{row.testName}</p>
          <PolymorphicResultCell
            row={row}
            editable={!saving}
            onValueChange={(field, value) =>
              setRows((previous) =>
                previous.map((r) =>
                  r.testResultComponentId === row.testResultComponentId
                    ? { ...r, [field]: value }
                    : r,
                ),
              )
            }
          />
          {!valid(row) && (
            <InlineNotification
              kind="error"
              title={t("microbiology.testing.invalidValue")}
              hideCloseButton
            />
          )}
        </div>
      ))}
      <Button
        disabled={
          saving ||
          external !== test.testedElsewhere ||
          (external &&
            (performer?.id !==
              (test.performingLabId || test.performingUserId) ||
              date !== test.performedAt)) ||
          rows.length === 0 ||
          !rows.every(valid) ||
          !rows.some(hasResult)
        }
        onClick={() => submit(false)}
      >
        {t("microbiology.testing.saveResults")}
      </Button>
      <Checkbox
        id={`external-${test.analysisId}`}
        labelText={t("microbiology.testing.testedElsewhere")}
        checked={external}
        disabled={saving}
        onChange={(_, { checked }) => setExternal(checked)}
      />
      {external && (
        <>
          <Select
            id={`performer-type-${test.analysisId}`}
            labelText={t("microbiology.testing.performerType")}
            value={performerType}
            disabled={saving}
            onChange={(e) => {
              setPerformerType(e.target.value);
              setPerformer(null);
              setPerformers([]);
            }}
          >
            <SelectItem
              value="lab"
              text={t("microbiology.testing.laboratory")}
            />
            <SelectItem value="user" text={t("microbiology.testing.person")} />
          </Select>
          <ComboBox
            id={`performer-${test.analysisId}`}
            titleText={t("microbiology.testing.performedBy")}
            helperText={t("microbiology.testing.performerHelp")}
            items={performers}
            selectedItem={performer}
            itemToString={(item) => item?.value || ""}
            onInputChange={setSearch}
            onChange={({ selectedItem }) => setPerformer(selectedItem)}
            disabled={saving}
          />
          <TextInput
            id={`performed-date-${test.analysisId}`}
            type="date"
            labelText={t("microbiology.testing.performedDate")}
            value={date}
            onChange={(e) => setDate(e.target.value)}
            disabled={saving}
          />
        </>
      )}
      <Button
        kind="secondary"
        disabled={saving || (external && (!performer || !date))}
        onClick={() => submit(true)}
      >
        {t("microbiology.testing.saveProvenance")}
      </Button>
      <Button kind="ghost" onClick={onCancel} disabled={saving}>
        {t("button.cancel")}
      </Button>
      {saving && <InlineLoading description={t("common.loading")} />}
      {error && (
        <InlineNotification
          kind="error"
          title={t("microbiology.testing.saveError")}
          subtitle={t("microbiology.testing.preservedDraft")}
          hideCloseButton
        />
      )}
    </Stack>
  );
}

import CaseCultureWorkspace from "./CaseCultureWorkspace";
import CaseIsolateWorkspace from "./CaseIsolateWorkspace";

export default function CaseTestingWorkspace({
  detail,
  service,
  onCaseChanged,
  onScopeChange,
  scanSearch = "",
}) {
  const mounted = useRef(true);
  useLayoutEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const [tests, setTests] = useState([]);
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [cultureId, setCultureId] = useState("");
  const [pickCultureId, setPickCultureId] = useState("");
  const [cultureSource, setCultureSource] = useState(null);
  const scanParams = new URLSearchParams(scanSearch);
  const scanScope = scanParams.get("scope");
  const scanRow = scanParams.get("row") || scanParams.get("astRunId");
  const sectionScopes = {
    setup: "CULTURE",
    timeline: "HISTORY",
    isolates: "ISOLATES",
    ast: "AST",
    reports: "REPORT",
    amendment: "AMENDMENT",
    "critical-communication": "CRITICAL",
    nonconformance: "NONCONFORMANCE",
  };
  const routedScope =
    scanScope ||
    sectionScopes[scanParams.get("section")] ||
    { cultures: "CULTURE", ast: "AST" }[scanParams.get("grain")] ||
    "INITIAL";
  const scanTarget = scanRow
    ? `${routedScope === "CULTURE" ? "culture-row" : routedScope === "ISOLATES" ? "isolate" : "ast-run"}-${scanRow}`
    : routedScope === "AST" && scanParams.get("astIsolateId")
      ? `ast-isolate-${scanParams.get("astIsolateId")}`
      : null;
  const [placement, setPlacement] = useState(routedScope);
  useEffect(() => {
    setPlacement(routedScope);
    if (routedScope === "HISTORY")
      document.getElementById("case-history")?.focus();
    if (["case-info", "order-detail"].includes(scanParams.get("section"))) {
      document.getElementById("case-information")?.focus();
    }
  }, [routedScope, scanRow, scanSearch]);
  useEffect(() => {
    if (!scanTarget || placement !== routedScope) return undefined;
    const scroll = () => {
      const target = document.querySelector(
        `[data-testid="${CSS.escape(scanTarget)}"]`,
      );
      if (!target) return false;
      target.scrollIntoView?.({ block: "center" });
      target.focus({ preventScroll: true });
      return true;
    };
    if (scroll()) return undefined;
    const observer = new MutationObserver(() => {
      if (scroll()) observer.disconnect();
    });
    observer.observe(document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, [scanTarget, routedScope, placement]);
  const [auxiliaryIsolates, setAuxiliaryIsolates] = useState([]);
  useEffect(() => {
    if (
      !["CRITICAL", "NONCONFORMANCE"].includes(placement) ||
      !service.getIsolates
    )
      return undefined;
    let live = true;
    service
      .getIsolates(detail.id)
      .then((data) => {
        if (live)
          setAuxiliaryIsolates(
            data.isolates.map((i) => ({ ...i, isolateLabel: i.label })),
          );
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail.id, placement, service]);
  const [editor, setEditor] = useState(null);
  const [selection, setSelection] = useState([]);
  const [adding, setAdding] = useState(false);
  const [note, setNote] = useState("");
  const [sendWithResult, setSendWithResult] = useState(false);
  const [noting, setNoting] = useState(false);
  useEffect(() => {
    let live = true;
    setSelection(
      (detail.samples || []).map((s) => ({
        sampleItemId: s.sampleItemId,
        sampleRejected: s.sampleRejected,
        sampleTypeId: s.sampleTypeId,
        sampleTypeName: s.specimenType,
        tests: [],
        panels: [],
      })),
    );
    Promise.all([service.getTests(detail.id), service.getTimeline(detail.id)])
      .then(([data, history]) => {
        if (live) {
          setTests(data);
          setTimeline(history);
        }
      })
      .catch(() => {
        if (live) setError(true);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [detail.id, service]);
  // A transfer removes editing rights immediately, including any open editor.
  useEffect(() => {
    if (!detail.canWrite) setEditor(null);
  }, [detail.canWrite]);
  const renderNotes = (targetType, targetId) => (
    <CaseAttachmentNotes
      detail={detail}
      service={service}
      timeline={timeline}
      targetType={targetType}
      targetId={targetId}
      onSaved={() => service.getTimeline(detail.id).then(setTimeline)}
    />
  );
  const refreshTesting = () => {
    onCaseChanged?.();
    Promise.all([service.getTests(detail.id), service.getTimeline(detail.id)])
      .then(([data, history]) => {
        if (!mounted.current) return;
        setTests(data);
        setTimeline(history);
      })
      .catch(() => {
        if (mounted.current) setError(true);
      });
  };
  const addTests = async () => {
    setAdding(true);
    setError(false);
    try {
      for (const s of (placement === "CULTURE"
        ? [cultureSource].filter(Boolean)
        : selection
      ).filter((s) => s.tests.length || s.panels.length)) {
        const data = await service.addTests(detail.id, {
          placement,
          cultureId: placement === "CULTURE" ? cultureId : undefined,
          sampleItemId: s.sampleItemId,
          testIds: s.tests.map((t) => String(t.id)),
          panelIds: s.panels.map((p) => String(p.id)),
        });
        if (!mounted.current) return;
        setTests(data);
        if (placement === "CULTURE")
          setCultureSource((previous) =>
            previous ? { ...previous, tests: [], panels: [] } : null,
          );
        else
          setSelection((previous) =>
            previous.map((sample) =>
              sample.sampleItemId === s.sampleItemId
                ? { ...sample, tests: [], panels: [] }
                : sample,
            ),
          );
      }
    } catch {
      if (mounted.current) setError(true);
    } finally {
      if (mounted.current) setAdding(false);
    }
  };
  const addNote = async () => {
    setNoting(true);
    setError(false);
    try {
      const saved = await service.addNote(detail.id, note, sendWithResult);
      if (!mounted.current) return;
      setTimeline((previous) => [...previous, saved]);
      setNote("");
    } catch {
      if (mounted.current) setError(true);
    } finally {
      if (mounted.current) setNoting(false);
    }
  };
  const validate = async (test) => {
    setError(false);
    try {
      const saved = await service.validateResult(
        detail.id,
        test.analysisId,
        test.version,
      );
      if (mounted.current) setTests(saved);
    } catch {
      if (mounted.current) setError(true);
    }
  };
  const renderTests = (targetCultureId) => (
    <TableContainer>
      <Table aria-label={t("microbiology.testing.title")}>
        <TableHead>
          <TableRow>
            {[
              "test",
              "result",
              "flag",
              "performedBy",
              "status",
              "actions",
              "notes",
            ].map((key) => (
              <TableHeader key={key}>
                {t(`microbiology.testing.${key}`)}
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {tests
            .filter(
              (test) =>
                test.placement === placement &&
                (placement !== "CULTURE" || test.cultureId === targetCultureId),
            )
            .map((test) => (
              <React.Fragment key={test.analysisId}>
                <TableRow>
                  <TableCell>
                    {test.testName}
                    {test.testedElsewhere && (
                      <Tag type="purple">
                        {t("microbiology.testing.external")}
                      </Tag>
                    )}
                  </TableCell>
                  <TableCell>
                    <Stack gap={2}>
                      {test.components.map((row) => (
                        <div key={row.testResultComponentId || row.analysisId}>
                          <span>{row.testName}: </span>
                          <PolymorphicResultCell
                            row={row}
                            editable={false}
                            onValueChange={() => {}}
                          />
                        </div>
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    {test.components.map((row) => (
                      <FlagChip
                        key={row.testResultComponentId || row.analysisId}
                        flag={row.resultFlag}
                      />
                    ))}
                  </TableCell>
                  <TableCell>
                    {test.performedByDisplay || t("not.available")}
                    {test.performedAt && (
                      <p>
                        {intl.formatDate(
                          new Date(`${test.performedAt}T12:00:00`),
                        )}
                      </p>
                    )}
                  </TableCell>
                  <TableCell>
                    <Tag>{t(`microbiology.testing.status.${test.status}`)}</Tag>
                  </TableCell>
                  <TableCell>
                    {detail.canWrite && test.canEdit && (
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => setEditor(test.analysisId)}
                      >
                        {t("microbiology.testing.enterEdit")}
                      </Button>
                    )}
                    {detail.canValidate &&
                      (test.canValidate ||
                        (test.selfValidationBlocked &&
                          test.status === "TechnicalAcceptance")) && (
                        <ESignatureButton
                          size="sm"
                          kind="ghost"
                          meaning="VALIDATED_AND_RELEASED"
                          recordType="VALIDATION_BATCH"
                          recordId={Number(test.analysisId)}
                          context={t("microbiology.testing.validateContext", {
                            test: test.testName,
                          })}
                          disabled={
                            test.selfValidationBlocked || !test.canValidate
                          }
                          ariaDescribedBy={
                            test.selfValidationBlocked
                              ? `self-validation-${test.analysisId}`
                              : undefined
                          }
                          onSign={() => validate(test)}
                          label={t("microbiology.testing.validate")}
                        />
                      )}
                    {test.selfValidationBlocked && (
                      <p id={`self-validation-${test.analysisId}`}>
                        {t("microbiology.testing.selfValidationBlocked")}
                      </p>
                    )}
                  </TableCell>
                  <TableCell>
                    {renderNotes("RESULT", test.analysisId)}
                  </TableCell>
                </TableRow>
                {editor === test.analysisId &&
                  detail.canWrite &&
                  test.canEdit && (
                    <TableRow>
                      <TableCell colSpan={7}>
                        <ResultEditor
                          test={test}
                          caseId={detail.id}
                          service={service}
                          onCancel={() => setEditor(null)}
                          onSaved={(data, close) => {
                            setTests(data);
                            if (close) setEditor(null);
                          }}
                        />
                      </TableCell>
                    </TableRow>
                  )}
              </React.Fragment>
            ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
  // i18n-keys: microbiology.testing.status.*
  return (
    <Stack gap={5}>
      <h2>{t("microbiology.testing.title")}</h2>
      <Select
        id="case-testing-scope"
        labelText={t("microbiology.testing.scope")}
        value={placement}
        onChange={(e) => {
          setPlacement(e.target.value);
          onScopeChange?.(e.target.value);
          setEditor(null);
        }}
      >
        <SelectItem value="INITIAL" text={t("microbiology.testing.initial")} />
        <SelectItem value="CULTURE" text={t("microbiology.culture.title")} />
        <SelectItem value="ISOLATES" text={t("microbiology.case.isolates")} />
        <SelectItem value="AST" text={t("microbiology.m6.ast")} />
        <SelectItem value="INCOMING" text={t("microbiology.incoming.title")} />
        <SelectItem value="HISTORY" text={t("microbiology.case.timeline")} />
        <SelectItem value="REPORT" text={t("microbiology.m6.report")} />
        <SelectItem
          value="AMENDMENT"
          text={t("microbiology.amendment.title")}
        />
        <SelectItem value="CRITICAL" text={t("microbiology.critical.title")} />
        {detail.canWrite && (
          <SelectItem
            value="NONCONFORMANCE"
            text={t("microbiology.nce.sectionTitle")}
          />
        )}
        <SelectItem
          value="ADDITIONAL"
          text={t("microbiology.testing.additional")}
        />
      </Select>
      {placement === "AMENDMENT" && (
        <AmendmentHistoryPanel
          caseId={detail.id}
          finalReleaseState={detail.finalReleaseState}
          service={service}
          canAmend={detail.canAmend}
          onCaseUpdated={onCaseChanged}
        />
      )}
      {placement === "CRITICAL" && (
        <CriticalCommunicationPanel
          key={`${detail.id}-${scanParams.get("action")}-${scanParams.get("targetType")}-${scanParams.get("targetId")}`}
          onEntryComplete={() => onScopeChange?.("CRITICAL")}
          caseId={detail.id}
          sampleItemId={detail.samples?.[0]?.sampleItemId}
          isolates={auxiliaryIsolates}
          projectedResultIds={tests.flatMap((test) =>
            test.components
              .map((component) => component.resultId)
              .filter(Boolean),
          )}
          entryTargetType={
            scanParams.get("action") === "log-critical"
              ? scanParams.get("targetType")
              : ""
          }
          entryTargetId={
            scanParams.get("action") === "log-critical"
              ? scanParams.get("targetId")
              : ""
          }
          service={service}
          isReadOnly={!detail.canWrite}
          onCaseUpdated={onCaseChanged}
        />
      )}
      {placement === "NONCONFORMANCE" && detail.canWrite && (
        <CaseNonconformancePanel
          key={scanParams.get("action")}
          mode={
            scanParams.get("action") === "mark-lost"
              ? "mark-lost"
              : "report-nce"
          }
          isolates={auxiliaryIsolates}
          caseId={detail.id}
          service={service}
          onComplete={() => {
            refreshTesting();
            onCaseChanged?.();
            onScopeChange?.("HISTORY");
            setPlacement("HISTORY");
          }}
          onCancel={() => {
            onScopeChange?.("HISTORY");
            setPlacement("HISTORY");
          }}
        />
      )}
      {placement === "INCOMING" && (
        <CaseIncomingWorkspace
          detail={detail}
          service={service}
          onChanged={refreshTesting}
        />
      )}
      {placement === "AST" && (
        <CaseAstWorkspace
          entryAction={scanParams.get("action")}
          selectedRunId={scanRow}
          onEntryActionConsumed={() =>
            onScopeChange?.("AST", {
              ...(scanRow ? { row: scanRow } : {}),
              ...(scanParams.get("astView")
                ? { astView: scanParams.get("astView") }
                : {}),
            })
          }
          readOnly={scanParams.get("astView") === "reviewed"}
          renderNotes={renderNotes}
          detail={detail}
          service={service}
          onChanged={onCaseChanged}
        />
      )}
      {placement === "REPORT" && (
        <CaseReportWorkspace
          detail={detail}
          service={service}
          onChanged={onCaseChanged}
        />
      )}
      {placement === "CULTURE" && (
        <CaseCultureWorkspace
          entryAction={scanParams.get("action")}
          onEntryActionConsumed={() => onScopeChange?.("CULTURE")}
          onChanged={refreshTesting}
          renderNotes={renderNotes}
          detail={detail}
          service={service}
          selectedId={(routedScope === "CULTURE" && scanRow) || cultureId}
          renderTests={renderTests}
          onPickIsolate={(row) => {
            setPickCultureId(row.id);
            setPlacement("ISOLATES");
          }}
          onSelect={(row) => {
            setCultureId(row.id);
            setCultureSource({
              ...row.source,
              sampleTypeName: row.source?.specimenType,
              tests: [],
              panels: [],
            });
            setEditor(null);
          }}
          onChooseTests={(row, gramTest) => {
            setCultureId(row.id);
            setEditor(null);
            setCultureSource({
              ...row.source,
              sampleTypeName: row.source?.specimenType,
              tests: gramTest
                ? [{ id: gramTest.id, name: gramTest.value }]
                : [],
              panels: [],
            });
            document
              .getElementById("culture-test-chooser")
              ?.scrollIntoView?.({ block: "center" });
          }}
        />
      )}
      {placement === "ISOLATES" && (
        <CaseIsolateWorkspace
          renderNotes={renderNotes}
          onLogCritical={(isolate) =>
            onScopeChange?.("CRITICAL", {
              action: "log-critical",
              targetType: "ISOLATE",
              targetId: isolate.id,
            })
          }
          key={detail.id}
          detail={detail}
          service={service}
          initialCultureId={pickCultureId}
          onClosePick={() => setPickCultureId("")}
          onChanged={() => {
            service
              .getTests(detail.id)
              .then(setTests)
              .catch(() => setError(true));
            onCaseChanged?.();
          }}
        />
      )}
      {loading && <InlineLoading description={t("common.loading")} />}
      {error && (
        <InlineNotification
          kind="error"
          title={t("microbiology.testing.saveError")}
          hideCloseButton
        />
      )}
      {["INITIAL", "ADDITIONAL"].includes(placement) && renderTests(null)}
      {["INITIAL", "ADDITIONAL", "CULTURE"].includes(placement) &&
        !loading &&
        !tests.some((test) => test.placement === placement) && (
          <p>{t("microbiology.testing.noTests")}</p>
        )}
      {detail.canWrite &&
        ["INITIAL", "ADDITIONAL", "CULTURE"].includes(placement) &&
        selection.length > 0 &&
        (placement !== "CULTURE" || cultureId) && (
          <>
            <section id="culture-test-chooser">
              <CollectTestPickerSection
                samples={
                  placement === "CULTURE"
                    ? [cultureSource].filter(Boolean)
                    : selection
                }
                setSamples={
                  placement === "CULTURE"
                    ? (update) => {
                        const current = [cultureSource].filter(Boolean);
                        const next =
                          typeof update === "function"
                            ? update(current)
                            : update;
                        setCultureSource(next[0] || null);
                      }
                    : setSelection
                }
                isReadOnly={adding}
                titleId="microbiology.testing.addTests"
                helperId="microbiology.testing.chooserHelp"
                cataloguePath={`/rest/microbiology/cases/${detail.id}/analyses/catalog`}
                excludedTestIdsBySample={tests
                  .filter(
                    (test) =>
                      (test.cultureId || "") ===
                      (placement === "CULTURE" ? cultureId : ""),
                  )
                  .reduce((bySample, test) => {
                    (bySample[test.sampleItemId] ||= []).push(
                      String(test.testId),
                    );
                    return bySample;
                  }, {})}
              />
              <Button
                onClick={addTests}
                disabled={
                  adding ||
                  !(
                    placement === "CULTURE"
                      ? [cultureSource].filter(Boolean)
                      : selection
                  ).some((s) => s.tests.length || s.panels.length)
                }
              >
                {t("microbiology.testing.addSelected")}
              </Button>
            </section>
          </>
        )}
      <section
        id="case-history"
        tabIndex={-1}
        aria-label={t("microbiology.case.timeline")}
      >
        <CaseHistory activities={timeline} open={placement === "HISTORY"} />
      </section>
      <Tile>
        <Stack gap={4}>
          <h2>{t("microbiology.testing.notes")}</h2>
          <TableContainer>
            <Table aria-label={t("microbiology.testing.notes")}>
              <TableHead>
                <TableRow>
                  {["date", "author", "noteType", "note"].map((key) => (
                    <TableHeader key={key}>
                      {t(`microbiology.testing.${key}`)}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {timeline
                  .filter(
                    (item) =>
                      item.activityType === "MANUAL_NOTE" && !item.targetType,
                  )
                  .sort(
                    (a, b) => new Date(a.occurredAt) - new Date(b.occurredAt),
                  )
                  .map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        {labTimestamp(intl, item.occurredAt)}
                      </TableCell>
                      <TableCell>{item.performedByDisplay}</TableCell>
                      <TableCell>
                        {t(
                          item.structuredData === "SEND_RESULT"
                            ? "microbiology.m6.sendWithResult"
                            : "microbiology.testing.internalNote",
                        )}
                      </TableCell>
                      <TableCell>
                        {item.note}
                        {item.structuredData === "SEND_RESULT" && (
                          <Tag type="blue">
                            {t("microbiology.m6.sendWithResult")}
                          </Tag>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>
          {detail.canWrite && (
            <>
              <TextArea
                id="case-note-text"
                labelText={t("microbiology.testing.newNote")}
                value={note}
                maxLength={4000}
                disabled={noting}
                onChange={(e) => setNote(e.target.value)}
              />
              <Checkbox
                id="case-note-send"
                labelText={t("microbiology.m6.sendWithResult")}
                checked={sendWithResult}
                onChange={(_, { checked }) => setSendWithResult(checked)}
              />
              <Button disabled={noting || !note.trim()} onClick={addNote}>
                {t("microbiology.testing.saveNote")}
              </Button>
            </>
          )}
        </Stack>
      </Tile>
    </Stack>
  );
}
