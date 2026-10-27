import ReagentLotPicker, { formatReagentLotConflict } from "./ReagentLotPicker";
import ReagentUsageHistory from "./ReagentUsageHistory";
import { labTimestamp } from "./MicrobiologyTime";
import ContainerLabelButton from "./ContainerLabelButton";
import React, { useContext, useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { hasRole, Roles } from "../utils/Utils";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
  FilterableMultiSelect,
  InlineLoading,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";

const validationActions = [
  "review",
  "override",
  "revert",
  "select",
  "acknowledge",
  "qc-override",
];
const reasons = [
  "override",
  "revert",
  "select",
  "repeat",
  "retest",
  "invalidate",
  "acknowledge",
  "qc-override",
  "resolve",
];
const techniques = [
  "BROTH_MICRODILUTION",
  "DISK_DIFFUSION",
  "ETEST",
  "VITEK_2",
  "PHOENIX",
];
export default function CaseAstWorkspace({
  detail,
  service,
  onChanged,
  renderNotes,
  readOnly = false,
  entryAction,
  selectedRunId,
  onEntryActionConsumed,
}) {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const canListAnalyzers = [
    Roles.ANALYSER_IMPORT,
    Roles.GLOBAL_ADMIN,
    "Admin",
  ].some((role) => hasRole(userSessionDetails, role));
  const t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const [data, setData] = useState(null),
    [error, setError] = useState(false),
    [busy, setBusy] = useState(false),
    [draft, setDraft] = useState(null);
  const [lots, setLots] = useState({ requirements: [], usages: [] });
  const [selectedLots, setSelectedLots] = useState({});
  const [analyzers, setAnalyzers] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  useEffect(() => {
    if (draft?.action !== "start" || !draft.adjustPanel) return;
    let live = true;
    service
      .getAntibiotics()
      .then((rows) => {
        if (live)
          setAntibiotics(
            rows.map((a) => ({ id: a.id, label: a.name || a.label })),
          );
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [draft?.action, draft?.adjustPanel, service]);
  useEffect(() => {
    setDraft(null);
  }, [detail.id, readOnly]);
  const current = useRef(detail);
  current.current = detail;
  useEffect(() => {
    let live = true;
    service
      .getAstWorkspace(detail.id)
      .then((value) => {
        if (live) {
          setData(value);
          setError(false);
        }
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail.id, detail.canWrite, detail.canValidate, service]);
  useEffect(() => {
    let live = true;
    Promise.all([
      service.getReagentLotOverview?.(detail.id),
      canListAnalyzers ? service.getAnalyzers?.() : Promise.resolve([]),
    ])
      .then(([overview, instruments]) => {
        if (live) {
          setLots({
            requirements: (overview?.requirements || []).filter(
              (requirement) => requirement.astReagent,
            ),
            usages: overview?.usages || [],
          });
          setAnalyzers(instruments || []);
        }
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail.id, data, service, canListAnalyzers]);
  const canEnter = !readOnly && detail.canWrite && data?.canEnter;
  const canValidate = !readOnly && detail.canValidate && data?.canValidate;
  const actionLabel = (action, run) =>
    action === "acknowledge" &&
    run?.intrinsicContradiction &&
    !run?.analyzerExpertFlags
      ? "confirmIdentification"
      : action;
  const open = (action, isolate, run, reading, antibioticId) => {
    setError(false);
    setSelectedLots({});
    setDraft({
      action,
      label: actionLabel(action, run),
      isolateId: isolate?.id,
      runId: run?.id,
      readingId: reading?.id,
      antibioticId: antibioticId || reading?.antibioticId,
      panelId: isolate?.assignments?.[0]?.panelId || "",
      breakpointStandardId: data.defaultStandard?.configured
        ? data.defaultStandard.available
          ? data.defaultStandard.standardId
          : ""
        : data.standards?.length === 1
          ? data.standards[0].id
          : "",
      technique: run?.technique || techniques[0],
      clinicalContext: run?.clinicalContext || "",
      rawValue: "",
      orderedAntibioticIds: null,
      adjustPanel: false,
      analyzerInstrumentId: run?.analyzerInstrumentId,
      analyzerCardId: "",
      awaitAnalyzerResults: false,
      interpretation: "RESISTANT",
      reason: "",
    });
  };
  const consumedEntry = useRef(null);
  useEffect(() => {
    if (entryAction !== "new-ast-attempt") {
      consumedEntry.current = null;
      return;
    }
    if (!data) return;
    const key = `${detail.id}:${selectedRunId}`;
    if (consumedEntry.current === key) return;
    consumedEntry.current = key;
    const isolate = data.isolates.find((item) =>
      item.runs.some((run) => run.id === selectedRunId),
    );
    const run = isolate?.runs.find((item) => item.id === selectedRunId);
    if (canEnter && run?.status === "REVIEWED") open("repeat", isolate, run);
    onEntryActionConsumed?.();
  }, [
    entryAction,
    selectedRunId,
    detail.id,
    data,
    canEnter,
    onEntryActionConsumed,
  ]);
  const submit = async () => {
    if (readOnly || !draft) return;
    const validating =
      validationActions.includes(draft.action) || draft.action === "resolve";
    if (!(validating ? current.current.canValidate : current.current.canWrite))
      return;
    setBusy(true);
    setError(false);
    try {
      const { adjustPanel, ...request } = draft;
      if (draft.action === "resolve") {
        await service.resolveDst(detail.id, draft);
        setData(await service.getAstWorkspace(detail.id));
      } else
        setData(
          await service.astAction(
            detail.id,
            {
              ...request,
              lotSelections: Object.values(selectedLots),
              rawValue:
                draft.action === "reading" ? Number(draft.rawValue) : null,
            },
            validating,
          ),
        );
      setDraft(null);
      onChanged?.();
    } catch (failure) {
      const lotError = formatReagentLotConflict(
        failure.response,
        lots.requirements,
        selectedLots,
        intl,
      );
      setError(
        lotError
          ? { localized: lotError }
          : failure.response?.message || failure.response?.error || true,
      );
    } finally {
      setBusy(false);
    }
  };
  const reportAction = async (body) => {
    setBusy(true);
    setError(false);
    try {
      setData(await service.astAction(detail.id, body, !canEnter));
      onChanged?.();
    } catch (failure) {
      setError(failure.response?.message || failure.response?.error || true);
    } finally {
      setBusy(false);
    }
  };
  const button = (action, isolate, run, reading, antibioticId) => (
    <Button
      key={action}
      size="sm"
      kind="ghost"
      onClick={() => open(action, isolate, run, reading, antibioticId)}
    >
      {t(`action.${actionLabel(action, run)}`)}
    </Button>
  );
  const activeIsolate = data?.isolates?.find((i) => i.id === draft?.isolateId);
  const panelChanged =
    draft?.action === "start" &&
    (draft.adjustPanel ||
      activeIsolate?.assignments?.find((a) => a.panelId === draft.panelId)
        ?.provenance !== "ORGANISM_DEFAULT");
  const needsReason = draft && (reasons.includes(draft.action) || panelChanged);
  const invalid =
    draft &&
    ((draft.adjustPanel && !draft.orderedAntibioticIds?.length) ||
      (needsReason && !draft.reason.trim()) ||
      (draft.action === "reading" &&
        (draft.rawValue === "" ||
          !Number.isFinite(Number(draft.rawValue)) ||
          Number(draft.rawValue) < 0)) ||
      (draft.action === "start" &&
        (!draft.panelId || !draft.breakpointStandardId)) ||
      (draft.action === "assign" && !draft.panelId) ||
      (draft.action === "invalidate" &&
        draft.analyzerInstrumentId &&
        !draft.analyzerCardId.trim()));
  const errorKey =
    typeof error === "string" ? `microbiology.m6.error.${error}` : "";
  const errorMessage =
    errorKey && intl.messages[errorKey]
      ? intl.formatMessage({ id: errorKey })
      : error?.localized || t("saveError");
  return (
    <Stack gap={5}>
      <h3>{t("ast")}</h3>
      {error && (
        <InlineNotification kind="error" title={errorMessage} hideCloseButton />
      )}
      {data?.defaultStandard?.configured && !data.defaultStandard.available && (
        <InlineNotification
          kind="warning"
          hideCloseButton
          title={t("error.AST_BREAKPOINT_DEFAULT_NOT_AVAILABLE")}
        />
      )}
      {!data && !error && <InlineLoading description={t("loading")} />}
      {data?.isolates?.length === 0 && <p>{t("identifyFirst")}</p>}
      {data?.isolates?.map((isolate) => (
        <Tile
          key={isolate.id}
          data-testid={`ast-isolate-${isolate.id}`}
          tabIndex={-1}
        >
          <Stack gap={4}>
            <h4>
              {isolate.label}: {isolate.organism}
            </h4>
            {isolate.protocol === "NTM" && (
              <InlineNotification
                kind="info"
                title={t("ntm")}
                hideCloseButton
              />
            )}
            <p>
              {t("assignedPanels")}:{" "}
              {isolate.assignments.map((a) => a.label).join(", ") || t("none")}
            </p>
            {canEnter && (
              <div>
                {button("assign", isolate)}
                {button("start", isolate)}
              </div>
            )}
            <Accordion>
              {isolate.runs.map((run) => (
                <AccordionItem
                  key={run.id}
                  data-testid={`ast-run-${run.id}`}
                  title={`${t(`run.${run.status}`)} · ${t(`technique.${run.technique}`)} · ${labTimestamp(intl, run.startedAt)}`}
                  open
                >
                  <Stack gap={4}>
                    <p>
                      {t("clinicalContext")}:{" "}
                      {t(
                        run.clinicalContext === "NON_MENINGITIS"
                          ? "clinicalContext.nonMeningitis"
                          : "clinicalContext.unspecified",
                      )}
                    </p>
                    <ContainerLabelButton
                      detail={detail}
                      kind="AST"
                      rowId={run.id}
                      service={service}
                    />
                    {run.analyzerInstrumentId && (
                      <Stack gap={2}>
                        <strong>
                          {analyzers.find(
                            (a) => a.id === run.analyzerInstrumentId,
                          )?.name || run.analyzerInstrumentId}
                        </strong>
                        <p>
                          {intl.formatMessage(
                            { id: "microbiology.ast.analyzerProvenance" },
                            {
                              card: run.analyzerCardId || t("none"),
                              version: run.analyzerSoftwareVersion || t("none"),
                            },
                          )}
                        </p>
                      </Stack>
                    )}
                    {run.status === "QC_FAILED" && (
                      <InlineNotification
                        kind="error"
                        title={intl.formatMessage({
                          id: "microbiology.ast.qcFailed",
                        })}
                        subtitle={intl.formatMessage(
                          { id: "microbiology.ast.qcFailedDetail" },
                          {
                            reference: run.instrumentQcReference || t("none"),
                          },
                        )}
                        hideCloseButton
                      />
                    )}
                    {run.status === "RESULTS_IN" && (
                      <InlineNotification
                        kind="info"
                        title={intl.formatMessage({
                          id: "microbiology.ast.resultsReady",
                        })}
                        subtitle={intl.formatMessage({
                          id: "microbiology.ast.resultsReadyDetail",
                        })}
                        hideCloseButton
                      />
                    )}
                    {run.intrinsicContradiction &&
                      !run.analyzerFlagsAcknowledgedAt && (
                        <InlineNotification
                          kind="warning"
                          lowContrast
                          title={t("intrinsicContradiction")}
                          hideCloseButton
                        />
                      )}
                    <TableContainer>
                      <Table
                        size="sm"
                        aria-label={`${isolate.label} ${t("ast")}`}
                      >
                        <TableHead>
                          <TableRow>
                            {[
                              "agent",
                              "raw",
                              "interpretation",
                              "selected",
                              "reportTick",
                              "actions",
                            ].map((key) => (
                              <TableHeader key={key}>{t(key)}</TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {run.orderedAntibiotics.map((agent) => {
                            const entries = run.readings.filter(
                              (r) => r.antibioticId === agent.antibioticId,
                            );
                            const reading = entries[entries.length - 1];
                            const selected =
                              reading &&
                              isolate.choices.some(
                                (c) => c.readingId === reading.id,
                              );
                            const showTick =
                              selected ||
                              !isolate.choices.some(
                                (c) => c.antibioticId === agent.antibioticId,
                              );
                            const editable = ![
                              "REVIEWED",
                              "INVALIDATED",
                              "CANCELLED",
                              "RERUN_REQUIRED",
                            ].includes(run.status);
                            return (
                              <TableRow key={agent.antibioticId}>
                                <TableCell>
                                  {agent.antibioticName}
                                  {reading && renderNotes?.("DRUG", reading.id)}
                                </TableCell>
                                <TableCell>
                                  {reading
                                    ? `${reading.rawText ?? reading.rawValue ?? ""} ${reading.units || ""}`
                                    : t("none")}
                                </TableCell>
                                <TableCell>
                                  {reading ? (
                                    <>
                                      {t(
                                        `interpretation.${reading.overrideInterpretation || reading.interpretation}`,
                                      )}
                                      {reading.instrumentInterpretation && (
                                        <p>
                                          {intl.formatMessage(
                                            {
                                              id: "microbiology.ast.instrumentInterpretation",
                                            },
                                            {
                                              interpretation: t(
                                                `interpretation.${reading.instrumentInterpretation}`,
                                              ),
                                            },
                                          )}
                                        </p>
                                      )}
                                      {reading.overrideRuleId ? (
                                        <>
                                          <Tag type="teal">
                                            {t("expertRule")}
                                          </Tag>
                                          <p>
                                            {intl.formatMessage(
                                              {
                                                id: "microbiology.m6.expertRuleLine",
                                              },
                                              {
                                                organism: isolate.organism,
                                                antibiotic:
                                                  agent.antibioticName,
                                              },
                                            )}
                                          </p>
                                        </>
                                      ) : (
                                        reading.overrideInterpretation && (
                                          <Tag type="purple">{t("manual")}</Tag>
                                        )
                                      )}
                                      <br />
                                      {t("breakpoint")}: {run.breakpointVersion}{" "}
                                      {reading.matchedBy &&
                                        t(`match.${reading.matchedBy}`)}
                                    </>
                                  ) : (
                                    t("none")
                                  )}
                                </TableCell>
                                <TableCell>
                                  {selected ? (
                                    <Tag type="green">{t("selected")}</Tag>
                                  ) : (
                                    t("none")
                                  )}
                                </TableCell>
                                <TableCell>
                                  {showTick && (
                                    <>
                                      <Checkbox
                                        id={`report-${run.id}-${agent.antibioticId}`}
                                        labelText={intl.formatMessage(
                                          { id: "microbiology.m6.reportAgent" },
                                          { agent: agent.antibioticName },
                                        )}
                                        aria-label={intl.formatMessage(
                                          { id: "microbiology.m6.reportAgent" },
                                          { agent: agent.antibioticName },
                                        )}
                                        hideLabel
                                        checked={!!agent.reported}
                                        disabled={
                                          !(canEnter || canValidate) || busy
                                        }
                                        onChange={(_, { checked }) =>
                                          reportAction({
                                            action: "report",
                                            isolateId: isolate.id,
                                            runId: run.id,
                                            antibioticId: agent.antibioticId,
                                            reported: checked,
                                          })
                                        }
                                      />
                                      {agent.reportManual ? (
                                        <p>{t("reportReason.manual")}</p>
                                      ) : (
                                        agent.reportBehavior !== "ALWAYS" && (
                                          <p>
                                            {t(
                                              `reportReason.${agent.reportBehavior}`,
                                            )}
                                          </p>
                                        )
                                      )}
                                    </>
                                  )}
                                  {agent.ruleNote && (
                                    <>
                                      <Tag size="sm" type="teal">
                                        {t("fromRule")}
                                      </Tag>
                                      <p>{agent.ruleNote}</p>
                                    </>
                                  )}
                                </TableCell>
                                <TableCell>
                                  {canEnter &&
                                    editable &&
                                    button(
                                      "reading",
                                      isolate,
                                      run,
                                      reading,
                                      agent.antibioticId,
                                    )}
                                  {canValidate &&
                                    reading &&
                                    editable &&
                                    button("override", isolate, run, reading)}
                                  {canValidate &&
                                    reading?.overrideInterpretation &&
                                    editable &&
                                    button("revert", isolate, run, reading)}
                                  {canValidate &&
                                    reading &&
                                    run.status === "REVIEWED" &&
                                    !selected &&
                                    button("select", isolate, run, reading)}
                                </TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <div>
                      {canValidate &&
                        ["IN_PROGRESS", "RESULTS_IN"].includes(run.status) &&
                        button("review", isolate, run)}
                      {canEnter && run.status === "REVIEWED" && (
                        <>
                          {button("repeat", isolate, run)}
                          {button("retest", isolate, run)}
                        </>
                      )}
                      {canEnter &&
                        ["QC_FAILED", "RESULTS_IN"].includes(run.status) &&
                        button("invalidate", isolate, run)}
                      {canValidate &&
                        run.status === "QC_FAILED" &&
                        button("qc-override", isolate, run)}
                      {canValidate &&
                        (run.analyzerExpertFlags ||
                          run.intrinsicContradiction) &&
                        !run.analyzerFlagsAcknowledgedAt &&
                        button("acknowledge", isolate, run)}
                    </div>
                    <Accordion>
                      <AccordionItem title={t("history")}>
                        <p>
                          {intl.formatMessage(
                            { id: "microbiology.ast.attemptNumber" },
                            { number: isolate.runs.indexOf(run) + 1 },
                          )}
                        </p>
                        {run.attemptReason && <p>{run.attemptReason}</p>}
                        {run.readings.map((reading) => (
                          <div key={reading.id}>
                            <p>
                              {
                                run.orderedAntibiotics.find(
                                  (a) =>
                                    a.antibioticId === reading.antibioticId,
                                )?.antibioticName
                              }
                              : {reading.rawText ?? reading.rawValue}{" "}
                              {reading.units} ·{" "}
                              {t(`interpretation.${reading.interpretation}`)} ·{" "}
                              {labTimestamp(intl, reading.createdAt)}
                            </p>
                            {reading.overrideHistory.map((event) => (
                              <p key={event.id}>
                                {event.performedByDisplay} ·{" "}
                                {t(
                                  `interpretation.${event.fromInterpretation}`,
                                )}{" "}
                                →{" "}
                                {t(`interpretation.${event.toInterpretation}`)}:{" "}
                                {event.reason}
                              </p>
                            ))}
                          </div>
                        ))}
                      </AccordionItem>
                    </Accordion>
                  </Stack>
                </AccordionItem>
              ))}
            </Accordion>
            {(canEnter || canValidate) &&
              isolate.runs.some((run) =>
                run.orderedAntibiotics.some((agent) => agent.reportManual),
              ) && (
                <div>
                  <Button
                    size="sm"
                    kind="ghost"
                    disabled={busy}
                    onClick={() =>
                      reportAction({
                        action: "report-reset",
                        isolateId: isolate.id,
                      })
                    }
                  >
                    {t("action.report-reset")}
                  </Button>
                </div>
              )}
          </Stack>
        </Tile>
      ))}
      {data?.dst && data.dst.classification !== "NOT_APPLICABLE" && (
        <Tile>
          <h4>
            {t("dst")}: {t(`classification.${data.dst.classification}`)}
          </h4>
          {data.dst.drugs.map((drug) => (
            <Stack gap={3} key={drug.antibioticId}>
              <p>
                {drug.label}: {t(`interpretation.${drug.interpretation}`)}
              </p>
              {drug.evidence.map((item) => (
                <p key={`${item.source}-${item.sourceId}`}>
                  {t(`source.${item.source}`)}:{" "}
                  {t(`interpretation.${item.interpretation}`)} ·{" "}
                  {t(item.validated ? "validated" : "pending")}
                </p>
              ))}
              {drug.resolutionReason && (
                <p>
                  {t("resolution")}: {drug.resolutionReason}
                </p>
              )}
              {canValidate && drug.discordant && !drug.resolutionReason && (
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => {
                    setError(false);
                    setDraft({
                      action: "resolve",
                      antibioticId: drug.antibioticId,
                      evidenceFingerprint: drug.evidenceFingerprint,
                      interpretation: "RESISTANT",
                      reason: "",
                    });
                  }}
                >
                  {t("action.resolve")}
                </Button>
              )}
            </Stack>
          ))}
        </Tile>
      )}
      <ReagentUsageHistory usages={lots.usages} />
      {draft && (
        <Modal
          open
          modalHeading={t(`action.${draft.label || draft.action}`)}
          primaryButtonText={t("save")}
          secondaryButtonText={t("cancel")}
          primaryButtonDisabled={
            busy ||
            invalid ||
            (draft.action === "start" &&
              draft.awaitAnalyzerResults &&
              (!draft.analyzerInstrumentId || !draft.analyzerCardId.trim())) ||
            (validationActions.includes(draft.action) ||
            draft.action === "resolve"
              ? !canValidate
              : !canEnter)
          }
          onRequestClose={() => {
            if (!busy) {
              setDraft(null);
              setError(false);
            }
          }}
          onRequestSubmit={submit}
        >
          <Stack gap={4}>
            {draft.action === "start" && (
              <>
                <Select
                  id="ast-entry-mode"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.entryMode",
                  })}
                  value={draft.awaitAnalyzerResults ? "ANALYZER" : "MANUAL"}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      awaitAnalyzerResults: e.target.value === "ANALYZER",
                    })
                  }
                >
                  <SelectItem
                    value="MANUAL"
                    text={intl.formatMessage({
                      id: "microbiology.ast.entryMode.manual",
                    })}
                  />
                  {canListAnalyzers && (
                    <SelectItem
                      value="ANALYZER"
                      text={intl.formatMessage({
                        id: "microbiology.ast.entryMode.analyzer",
                      })}
                    />
                  )}
                </Select>
                {draft.awaitAnalyzerResults && (
                  <>
                    <Select
                      id="ast-instrument"
                      labelText={intl.formatMessage({
                        id: "microbiology.ast.analyzer",
                      })}
                      value={draft.analyzerInstrumentId || ""}
                      onChange={(e) =>
                        setDraft({
                          ...draft,
                          analyzerInstrumentId: e.target.value,
                        })
                      }
                    >
                      <SelectItem value="" text={t("choose")} />
                      {analyzers.map((a) => (
                        <SelectItem key={a.id} value={a.id} text={a.name} />
                      ))}
                    </Select>
                    <TextInput
                      id="ast-card-id"
                      labelText={intl.formatMessage({
                        id: "microbiology.ast.cardId",
                      })}
                      value={draft.analyzerCardId}
                      onChange={(e) =>
                        setDraft({ ...draft, analyzerCardId: e.target.value })
                      }
                    />
                  </>
                )}
                <Select
                  id="ast-clinical-context"
                  labelText={t("clinicalContext")}
                  helperText={t("clinicalContext.help")}
                  value={draft.clinicalContext}
                  onChange={(event) =>
                    setDraft({ ...draft, clinicalContext: event.target.value })
                  }
                >
                  <SelectItem
                    value=""
                    text={t("clinicalContext.unspecified")}
                  />
                  <SelectItem
                    value="NON_MENINGITIS"
                    text={t("clinicalContext.nonMeningitis")}
                  />
                </Select>
                <ReagentLotPicker
                  id="case-ast-lots"
                  requirements={lots.requirements}
                  selectedLots={selectedLots}
                  disabled={busy}
                  onChange={(lot) =>
                    setSelectedLots((previous) => ({
                      ...previous,
                      [`${lot.analysisId}:${lot.testReagentLinkId}`]: lot,
                    }))
                  }
                />
              </>
            )}
            {["start", "assign"].includes(draft.action) && (
              <Select
                id="ast-panel"
                labelText={t("panel")}
                value={draft.panelId}
                onChange={(e) =>
                  setDraft({ ...draft, panelId: e.target.value })
                }
              >
                <SelectItem value="" text={t("choose")} />
                {activeIsolate.panels.map((p) => (
                  <SelectItem key={p.id} value={p.id} text={p.label} />
                ))}
              </Select>
            )}
            {draft.action === "start" && (
              <>
                <Checkbox
                  id="ast-adjust-panel"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.adjustPanel",
                  })}
                  checked={draft.adjustPanel}
                  onChange={(_, { checked }) =>
                    setDraft({
                      ...draft,
                      adjustPanel: checked,
                      orderedAntibioticIds: checked ? [] : null,
                    })
                  }
                />
                {draft.adjustPanel && (
                  <FilterableMultiSelect
                    id="ast-ordered-agents"
                    titleText={intl.formatMessage({
                      id: "microbiology.ast.orderedAntibiotics",
                    })}
                    items={antibiotics}
                    itemToString={(a) => a?.label || ""}
                    selectedItems={antibiotics.filter((a) =>
                      draft.orderedAntibioticIds?.includes(a.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setDraft({
                        ...draft,
                        orderedAntibioticIds: selectedItems.map((a) => a.id),
                      })
                    }
                  />
                )}
              </>
            )}
            {["repeat", "retest"].includes(draft.action) && (
              <>
                <Select
                  id="ast-attempt-type"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.attemptType",
                  })}
                  value={draft.action}
                  onChange={(event) =>
                    setDraft({ ...draft, action: event.target.value })
                  }
                >
                  <SelectItem value="repeat" text={t("action.repeat")} />
                  <SelectItem value="retest" text={t("action.retest")} />
                </Select>
                <Select
                  id="ast-repeat-agent"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.attemptScope",
                  })}
                  value={draft.orderedAntibioticIds?.[0] || ""}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      orderedAntibioticIds: e.target.value
                        ? [e.target.value]
                        : null,
                    })
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "microbiology.ast.wholePanel",
                    })}
                  />
                  {activeIsolate.runs
                    .find((r) => r.id === draft.runId)
                    ?.orderedAntibiotics.map((a) => (
                      <SelectItem
                        key={a.antibioticId}
                        value={a.antibioticId}
                        text={a.antibioticName}
                      />
                    ))}
                </Select>
                <ReagentLotPicker
                  id="case-repeat-lots"
                  requirements={lots.requirements}
                  selectedLots={selectedLots}
                  disabled={busy}
                  onChange={(lot) =>
                    setSelectedLots((previous) => ({
                      ...previous,
                      [`${lot.analysisId}:${lot.testReagentLinkId}`]: lot,
                    }))
                  }
                />
              </>
            )}
            {["start", "repeat", "retest"].includes(draft.action) && (
              <Select
                id="ast-technique"
                labelText={t("technique")}
                value={draft.technique}
                onChange={(e) =>
                  setDraft({ ...draft, technique: e.target.value })
                }
              >
                {techniques.map((value) => (
                  <SelectItem
                    key={value}
                    value={value}
                    text={t(`technique.${value}`)}
                  />
                ))}
              </Select>
            )}
            {draft.action === "start" && (
              <Select
                id="ast-standard"
                labelText={t("breakpoint")}
                value={draft.breakpointStandardId}
                onChange={(e) =>
                  setDraft({ ...draft, breakpointStandardId: e.target.value })
                }
              >
                <SelectItem value="" text={t("choose")} />
                {data.standards.map((p) => (
                  <SelectItem key={p.id} value={p.id} text={p.label} />
                ))}
              </Select>
            )}
            {draft.action === "invalidate" && draft.analyzerInstrumentId && (
              <TextInput
                id="ast-replacement-card"
                labelText={t("replacementCard")}
                value={draft.analyzerCardId}
                onChange={(e) =>
                  setDraft({ ...draft, analyzerCardId: e.target.value })
                }
              />
            )}
            {draft.action === "reading" && (
              <TextInput
                id="ast-raw"
                type="number"
                min="0"
                step="any"
                labelText={t("raw")}
                value={draft.rawValue}
                onChange={(e) =>
                  setDraft({ ...draft, rawValue: e.target.value })
                }
              />
            )}
            {["override", "resolve"].includes(draft.action) && (
              <Select
                id="ast-interpretation"
                labelText={t("interpretation")}
                value={draft.interpretation}
                onChange={(e) =>
                  setDraft({ ...draft, interpretation: e.target.value })
                }
              >
                {(draft.action === "resolve"
                  ? ["SUSCEPTIBLE", "RESISTANT"]
                  : ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"]
                ).map((value) => (
                  <SelectItem
                    key={value}
                    value={value}
                    text={t(`interpretation.${value}`)}
                  />
                ))}
              </Select>
            )}
            {needsReason && (
              <TextArea
                id="ast-reason"
                labelText={
                  panelChanged
                    ? intl.formatMessage({
                        id: "microbiology.ast.panelAdjustmentReason",
                      })
                    : t("reason")
                }
                value={draft.reason}
                maxLength={4000}
                onChange={(e) => setDraft({ ...draft, reason: e.target.value })}
              />
            )}
            {error && (
              <InlineNotification
                kind="error"
                title={errorMessage}
                hideCloseButton
              />
            )}
          </Stack>
        </Modal>
      )}
    </Stack>
  );
}
