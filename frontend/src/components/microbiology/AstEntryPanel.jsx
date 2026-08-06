import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  DataTable,
  FilterableMultiSelect,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
  TextInput,
  Tooltip,
} from "@carbon/react";
import { useIntl } from "react-intl";
import AstAttemptTable from "./AstAttemptTable";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import ReagentLotPicker, { formatReagentLotConflict } from "./ReagentLotPicker";
import ReagentUsageHistory from "./ReagentUsageHistory";

const TECHNIQUE_OPTIONS = [
  "VITEK_2",
  "PHOENIX",
  "ETEST",
  "BROTH_MICRODILUTION",
  "DISK_DIFFUSION",
];
const OVERRIDE_OPTIONS = ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"];

const measurementTypeForTechnique = (technique) =>
  technique === "DISK_DIFFUSION" ? "ZONE" : "MIC";

const AstEntryPanel = ({
  caseId,
  workflowType,
  isolates = [],
  service,
  saving: caseSaving,
  onAstUpdated,
  readOnly = false,
  reagentRequirements = [],
  reagentUsages = [],
  initialIsolateId = "",
  initialRunId = "",
  initialAction = "",
  onAttemptStarted,
}) => {
  const intl = useIntl();
  const [selectedIsolateId, setSelectedIsolateId] = useState(initialIsolateId);
  const [panels, setPanels] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  const [breakpointStandards, setBreakpointStandards] = useState([]);
  const [analyzers, setAnalyzers] = useState([]);
  const [selectedPanelId, setSelectedPanelId] = useState("");
  const [astSetup, setAstSetup] = useState(null);
  const [adjustingPanel, setAdjustingPanel] = useState(false);
  const [panelAdjustmentReason, setPanelAdjustmentReason] = useState("");
  const [panelAntibioticIds, setPanelAntibioticIds] = useState([]);
  const [adjustedAntibioticIds, setAdjustedAntibioticIds] = useState([]);
  const [selectedAntibioticId, setSelectedAntibioticId] = useState("");
  const [selectedStandardId, setSelectedStandardId] = useState("");
  const [technique, setTechnique] = useState("VITEK_2");
  const [entryMode, setEntryMode] = useState("MANUAL");
  const [selectedAnalyzerId, setSelectedAnalyzerId] = useState("");
  const [analyzerCardId, setAnalyzerCardId] = useState("");
  const [rawValue, setRawValue] = useState("4");
  const [overrideInterpretation, setOverrideInterpretation] =
    useState("RESISTANT");
  const [overrideReason, setOverrideReason] = useState("");
  const [expandedHistoryReadingId, setExpandedHistoryReadingId] = useState("");
  const [revertReason, setRevertReason] = useState("");
  const [runs, setRuns] = useState([]);
  const [selectedRunId, setSelectedRunId] = useState(initialRunId);
  const [selectedReadingId, setSelectedReadingId] = useState("");
  const [attemptType, setAttemptType] = useState("REPEAT");
  const [attemptReason, setAttemptReason] = useState("");
  const [attemptTechnique, setAttemptTechnique] = useState("");
  const [attemptScope, setAttemptScope] = useState("WHOLE_PANEL");
  const [attemptAntibioticId, setAttemptAntibioticId] = useState("");
  const [readiness, setReadiness] = useState(null);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState("");
  const [selectedLots, setSelectedLots] = useState({});
  const [analyzerResolutionReason, setAnalyzerResolutionReason] = useState("");
  const [replacementCardId, setReplacementCardId] = useState("");

  const activeIsolateId = selectedIsolateId || isolates[0]?.id || "";
  const activeIsolate = isolates.find(
    (isolate) => isolate.id === activeIsolateId,
  );
  const isolateIdentified = Boolean(
    activeIsolate?.organismId &&
    activeIsolate?.identificationStatus === "CONFIRMED",
  );

  useEffect(() => {
    if (!workflowType) {
      return;
    }
    service.getAstPanels(workflowType).then((items = []) => {
      setPanels(items);
      if (items.length > 0) {
        setSelectedPanelId((current) => current || items[0].id);
      }
    });
    service.getAntibiotics().then((items = []) => {
      setAntibiotics(items);
      if (items.length > 0) {
        setSelectedAntibioticId((current) => current || items[0].id);
      }
    });
    service.getBreakpointStandards().then((items = []) => {
      setBreakpointStandards(items);
      if (items.length > 0) {
        setSelectedStandardId((current) => current || items[0].id);
      }
    });
    if (service.getAnalyzers) {
      service.getAnalyzers().then((items = []) => {
        setAnalyzers(items);
        if (items.length > 0) {
          setSelectedAnalyzerId((current) => current || items[0].id);
        }
      });
    }
  }, [service, workflowType]);

  useEffect(() => {
    if (
      !activeIsolateId ||
      !isolateIdentified ||
      !service.getAstSetupForIsolate
    ) {
      return;
    }
    let active = true;
    service
      .getAstSetupForIsolate(activeIsolateId)
      .then((setup) => {
        if (!active) {
          return;
        }
        if (!setup || setup.error || setup.statusCode >= 400) {
          setActionError(
            formatMicrobiologyEnum(
              setup?.message || setup?.error || "AST_SETUP_UNAVAILABLE",
            ),
          );
          return;
        }
        setAstSetup(setup);
        if (setup.orderedPanelId) {
          setSelectedPanelId(setup.orderedPanelId);
          setAdjustingPanel(false);
        } else {
          setAdjustingPanel(true);
        }
        setPanelAdjustmentReason("");
      })
      .catch(() => {
        if (active) {
          setActionError(formatMicrobiologyEnum("AST_SETUP_UNAVAILABLE"));
        }
      });
    return () => {
      active = false;
    };
  }, [activeIsolateId, isolateIdentified, service]);

  const loadAstState = useCallback(() => {
    if (!activeIsolateId) {
      return Promise.resolve().then(() => setRuns([]));
    }
    return Promise.all([
      service.getAstRunsForIsolate(activeIsolateId).then((items = []) => {
        setRuns(items);
      }),
      service.getCaseReadiness(caseId).then((value) => {
        setReadiness(value);
      }),
    ]);
  }, [activeIsolateId, caseId, service]);

  useEffect(() => {
    loadAstState();
  }, [loadAstState]);

  const currentRun = useMemo(() => {
    const selected = runs.find((run) => run.id === selectedRunId);
    return (
      selected ||
      runs.find((run) => run.status === "IN_PROGRESS") ||
      runs.find((run) => run.status === "RESULTS_IN") ||
      runs.find((run) => run.status === "QC_FAILED") ||
      runs.find((run) => run.status === "AWAITING_RESULTS") ||
      runs.find((run) => run.reportable) ||
      (runs.length > 0 ? runs[runs.length - 1] : null)
    );
  }, [runs, selectedRunId]);
  const currentReadings = currentRun?.readings || [];
  const orderedAntibiotics = useMemo(() => {
    if (!currentRun) {
      return [];
    }
    if (!Array.isArray(currentRun.orderedAntibiotics)) {
      return antibiotics;
    }
    return currentRun.orderedAntibiotics.map((ordered) => {
      const antibiotic = antibiotics.find(
        (candidate) => candidate.id === ordered.antibioticId,
      );
      return {
        ...ordered,
        id: ordered.antibioticId,
        label: antibiotic?.label || ordered.antibioticId,
      };
    });
  }, [antibiotics, currentRun]);
  const currentReading =
    currentReadings.find((reading) => reading.id === selectedReadingId) ||
    currentReadings[0];
  const activeAntibioticId = orderedAntibiotics.some(
    (antibiotic) => antibiotic.id === selectedAntibioticId,
  )
    ? selectedAntibioticId
    : orderedAntibiotics[0]?.id || "";
  const orderedResultsComplete =
    orderedAntibiotics.length > 0 &&
    orderedAntibiotics.every((ordered) =>
      currentReadings.some(
        (reading) => reading.antibioticId === ordered.antibioticId,
      ),
    );

  const readingHeaders = useMemo(
    () => [
      {
        key: "antibiotic",
        header: intl.formatMessage({ id: "microbiology.ast.antibiotic" }),
      },
      {
        key: "method",
        header: intl.formatMessage({ id: "microbiology.ast.method" }),
      },
      {
        key: "result",
        header: intl.formatMessage({ id: "microbiology.ast.rawValue" }),
      },
      {
        key: "source",
        header: intl.formatMessage({ id: "microbiology.ast.source" }),
      },
      {
        key: "matchedBy",
        header: intl.formatMessage({ id: "microbiology.ast.matchedBy" }),
      },
      {
        key: "interpretation",
        header: intl.formatMessage({ id: "microbiology.ast.interpretation" }),
      },
      {
        key: "override",
        header: intl.formatMessage({ id: "microbiology.ast.override" }),
      },
    ],
    [intl],
  );

  const antibioticLabelFor = (reading) =>
    reading?.antibioticLabel ||
    antibiotics.find((antibiotic) => antibiotic.id === reading?.antibioticId)
      ?.label ||
    reading?.antibioticId;
  const busy = saving || caseSaving;
  const isReviewed = currentRun?.status === "REVIEWED";
  const hasInProgressRun = runs.some((run) =>
    ["IN_PROGRESS", "AWAITING_RESULTS", "RESULTS_IN", "QC_FAILED"].includes(
      run.status,
    ),
  );
  const effectiveAttemptTechnique =
    attemptTechnique || currentRun?.technique || "VITEK_2";
  const lotSelections = Object.values(selectedLots);
  const panelAdjusted = Boolean(
    astSetup && selectedPanelId !== astSetup.orderedPanelId,
  );
  const drugSetAdjusted =
    adjustedAntibioticIds.join("|") !== panelAntibioticIds.join("|");
  const orderAdjusted = panelAdjusted || drugSetAdjusted;
  const setupUnavailable = Boolean(
    service.getAstSetupForIsolate &&
    isolateIdentified &&
    astSetup?.isolateId !== activeIsolateId,
  );
  const measurementMode =
    currentRun?.measurementType ||
    currentRun?.method ||
    measurementTypeForTechnique(technique);
  const readingRows = currentReadings.map((reading) => ({
    id: reading.id,
    antibiotic: antibioticLabelFor(reading),
    method: formatMicrobiologyEnum(
      reading.technique || currentRun?.technique || reading.method,
    ),
    result: `${reading.rawValue ?? reading.rawText ?? ""}${
      reading.units ? ` ${reading.units}` : ""
    }`,
    source: formatMicrobiologyEnum(
      reading.overrideInterpretation ? "OVERRIDE" : reading.source || "UNKNOWN",
    ),
    matchedBy: formatMicrobiologyEnum(reading.matchedBy || "NONE"),
    interpretation: reading.instrumentInterpretation
      ? `${reading.interpretation} (${intl.formatMessage(
          { id: "microbiology.ast.instrumentInterpretation" },
          {
            interpretation: formatMicrobiologyEnum(
              reading.instrumentInterpretation,
            ),
          },
        )})`
      : reading.interpretation,
    override:
      reading.overrideReason ||
      intl.formatMessage({ id: "microbiology.ast.noOverride" }),
  }));
  const analyzerName =
    analyzers.find(
      (analyzer) => analyzer.id === currentRun?.analyzerInstrumentId,
    )?.name || currentRun?.analyzerInstrumentId;
  const analyzerOrganismMismatch = Boolean(
    currentRun?.analyzerOrganismId &&
    activeIsolate?.organismId &&
    currentRun.analyzerOrganismId !== activeIsolate.organismId,
  );
  const unresolvedInstrumentMismatch = currentReadings.some(
    (reading) =>
      reading.instrumentInterpretation &&
      reading.instrumentInterpretation !== reading.interpretation &&
      !reading.overrideInterpretation,
  );
  const unresolvedNoBreakpoint = currentReadings.some(
    (reading) =>
      reading.matchedBy === "NONE" && !reading.overrideInterpretation,
  );
  const unresolvedExpertFlags = Boolean(
    currentRun?.analyzerExpertFlags && !currentRun?.analyzerFlagsAcknowledgedAt,
  );
  const analyzerAcceptanceBlocked =
    currentRun?.status === "QC_FAILED" ||
    unresolvedInstrumentMismatch ||
    unresolvedNoBreakpoint ||
    unresolvedExpertFlags;
  const astProgress = [];
  if (readiness) {
    astProgress.push(
      readiness.astRunsTotal > 0
        ? intl.formatMessage(
            { id: "microbiology.ast.runsComplete" },
            {
              complete: readiness.astRunsComplete,
              total: readiness.astRunsTotal,
            },
          )
        : intl.formatMessage({ id: "microbiology.ast.noRunsYet" }),
    );
    if (readiness.significantIsolatesAwaitingAstSetup > 0) {
      astProgress.push(
        intl.formatMessage(
          { id: "microbiology.ast.awaitingSetupCount" },
          { count: readiness.significantIsolatesAwaitingAstSetup },
        ),
      );
    }
    if (readiness.isolatesPendingIdentification > 0) {
      astProgress.push(
        intl.formatMessage(
          { id: "microbiology.ast.pendingIdentificationCount" },
          { count: readiness.isolatesPendingIdentification },
        ),
      );
    }
  }

  const selectLot = (selection) => {
    const selectionKey = `${selection.analysisId}:${selection.testReagentLinkId}`;
    setSelectedLots((current) => ({
      ...current,
      [selectionKey]: selection,
    }));
  };

  const viewRun = (runId) => {
    setSelectedRunId(runId);
    setAttemptTechnique("");
    setAttemptReason("");
    setAttemptScope("WHOLE_PANEL");
    setAttemptAntibioticId("");
  };

  const runOperation = (operation) => {
    setSaving(true);
    setActionError("");
    return Promise.resolve()
      .then(operation)
      .then((result) => {
        if (
          result?.error ||
          result?.statusCode >= 400 ||
          result?.status === 0
        ) {
          throw new Error(
            formatReagentLotConflict(
              result,
              reagentRequirements,
              selectedLots,
              intl,
            ) || formatMicrobiologyEnum(result.message || result.error),
          );
        }
        return loadAstState().then(() => result);
      })
      .then((result) => {
        if (onAstUpdated) {
          onAstUpdated();
        }
        return result;
      })
      .catch((error) => {
        setActionError(error?.message || String(error));
        return null;
      })
      .finally(() => setSaving(false));
  };

  const startRun = () =>
    runOperation(() =>
      service.startAstRun({
        isolateId: activeIsolateId,
        panelId: selectedPanelId,
        breakpointStandardId: selectedStandardId,
        technique,
        ...(adjustingPanel
          ? { orderedAntibioticIds: adjustedAntibioticIds }
          : {}),
        ...(orderAdjusted
          ? { panelAdjustmentReason: panelAdjustmentReason.trim() }
          : {}),
        ...(lotSelections.length > 0 ? { lotSelections } : {}),
        ...(entryMode === "ANALYZER"
          ? {
              awaitAnalyzerResults: true,
              analyzerInstrumentId: selectedAnalyzerId,
              analyzerCardId: analyzerCardId.trim(),
            }
          : {}),
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
        setSelectedLots({});
      }
    });

  const startRepeatRun = () =>
    runOperation(() =>
      service.startRepeatAstRun(currentRun.id, {
        attemptType,
        reason: attemptReason,
        technique: effectiveAttemptTechnique,
        ...(attemptScope === "SINGLE_ANTIBIOTIC"
          ? { orderedAntibioticIds: [attemptAntibioticId] }
          : {}),
        ...(lotSelections.length > 0 ? { lotSelections } : {}),
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
        setAttemptReason("");
        setAttemptScope("WHOLE_PANEL");
        setAttemptAntibioticId("");
        setSelectedLots({});
        onAttemptStarted?.(run);
      }
    });

  const recordReading = () =>
    runOperation(() =>
      service.recordAstReading(currentRun.id, {
        antibioticId: activeAntibioticId,
        rawValue,
      }),
    );

  const loadPanelAntibiotics = (panelId) => {
    setPanelAntibioticIds([]);
    setAdjustedAntibioticIds([]);
    return service
      .getAstPanelAntibiotics(panelId)
      .then((rows = []) => {
        const antibioticIds = rows.map((row) => row.antibioticId);
        setPanelAntibioticIds(antibioticIds);
        setAdjustedAntibioticIds(antibioticIds);
      })
      .catch(() =>
        setActionError(formatMicrobiologyEnum("AST_SETUP_UNAVAILABLE")),
      );
  };

  const beginPanelAdjustment = () => {
    setAdjustingPanel(true);
    setPanelAdjustmentReason("");
    loadPanelAntibiotics(selectedPanelId);
  };

  const changeAdjustedPanel = (panelId) => {
    setSelectedPanelId(panelId);
    setPanelAdjustmentReason("");
    loadPanelAntibiotics(panelId);
  };

  const overrideReading = () =>
    runOperation(() =>
      service.overrideAstReading(currentReading.id, {
        overrideInterpretation,
        overrideReason,
      }),
    );

  const revertOverride = (readingId) =>
    runOperation(() =>
      service.revertAstOverride(readingId, {
        overrideReason: revertReason.trim(),
      }),
    ).then((reading) => {
      if (reading) {
        setRevertReason("");
      }
    });

  const reviewRun = () =>
    runOperation(() => service.reviewAstRun(currentRun.id));

  const acknowledgeAnalyzerFlags = () =>
    runOperation(() =>
      service.acknowledgeAstAnalyzerFlags(currentRun.id, {
        reason: analyzerResolutionReason.trim(),
      }),
    ).then((run) => {
      if (run) {
        setAnalyzerResolutionReason("");
      }
    });

  const overrideQcFailure = () =>
    runOperation(() =>
      service.overrideAstQcFailure(currentRun.id, {
        reason: analyzerResolutionReason.trim(),
      }),
    ).then((run) => {
      if (run) {
        setAnalyzerResolutionReason("");
      }
    });

  const invalidateAndRepeat = () =>
    runOperation(() =>
      service.invalidateAndRepeatAstRun(currentRun.id, {
        reason: analyzerResolutionReason.trim(),
        analyzerCardId: replacementCardId.trim(),
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
        setAnalyzerResolutionReason("");
        setReplacementCardId("");
      }
    });

  const selectReportableRun = (runId) =>
    runOperation(() => service.selectReportableAstRun(runId)).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
      }
    });

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-ast-card"
      aria-labelledby="microbiology-ast-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-ast-heading">
            {intl.formatMessage({ id: "microbiology.ast.title" })}
            {astProgress.length > 0 ? (
              <span className="microbiology-ast-progress-count">
                {astProgress.join(" · ")}
              </span>
            ) : null}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.ast.hint" })}
          </p>
        </div>
        {currentRun && (
          <div data-testid="microbiology-ast-run-status">
            <Tag
              type={
                currentRun.status === "REVIEWED"
                  ? "green"
                  : currentRun.status === "QC_FAILED"
                    ? "red"
                    : "cyan"
              }
            >
              {formatMicrobiologyEnum(currentRun.status)}
            </Tag>
          </div>
        )}
      </div>
      <div>
        {isolates.length === 0 ? (
          <p>{intl.formatMessage({ id: "microbiology.ast.noIsolate" })}</p>
        ) : (
          <>
            {readiness ? (
              <InlineNotification
                kind={readiness.finalReleaseReady ? "success" : "warning"}
                title={intl.formatMessage({
                  id: readiness.finalReleaseReady
                    ? "microbiology.readiness.ready"
                    : "microbiology.readiness.blocked",
                })}
                subtitle={(readiness.blockers || [])
                  .map(formatMicrobiologyEnum)
                  .join(", ")}
                hideCloseButton
              />
            ) : null}
            {actionError ? (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "microbiology.ast.actionError",
                })}
                subtitle={actionError}
                hideCloseButton
              />
            ) : null}
            {currentRun?.status === "AWAITING_RESULTS" ? (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "microbiology.ast.awaitingAnalyzerResults",
                })}
                subtitle={intl.formatMessage({
                  id: "microbiology.ast.awaitingAnalyzerResultsDetail",
                })}
                hideCloseButton
              />
            ) : null}
            {currentRun?.status === "RESULTS_IN" ? (
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
            ) : null}
            {currentRun?.analyzerInstrumentId ? (
              <div className="microbiology-ast-analyzer-provenance">
                <strong>{analyzerName}</strong>
                <span>
                  {intl.formatMessage(
                    { id: "microbiology.ast.analyzerProvenance" },
                    {
                      card: currentRun.analyzerCardId || "-",
                      version: currentRun.analyzerSoftwareVersion || "-",
                    },
                  )}
                </span>
                {currentRun.analyzerOrganismName ? (
                  <span>
                    {intl.formatMessage(
                      { id: "microbiology.ast.analyzerOrganism" },
                      {
                        organism: currentRun.analyzerOrganismName,
                        confidence:
                          currentRun.analyzerOrganismConfidence ?? "-",
                      },
                    )}
                  </span>
                ) : null}
              </div>
            ) : null}
            {analyzerOrganismMismatch ? (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "microbiology.ast.organismMismatch",
                })}
                subtitle={intl.formatMessage({
                  id: "microbiology.ast.organismMismatchDetail",
                })}
                hideCloseButton
              />
            ) : null}
            {currentRun?.status === "QC_FAILED" ? (
              <div className="microbiology-ast-qc-recovery">
                <InlineNotification
                  kind="error"
                  title={intl.formatMessage({
                    id: "microbiology.ast.qcFailed",
                  })}
                  subtitle={intl.formatMessage(
                    { id: "microbiology.ast.qcFailedDetail" },
                    { reference: currentRun.instrumentQcReference || "-" },
                  )}
                  hideCloseButton
                />
                <TextArea
                  id="microbiology-ast-analyzer-resolution-reason"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.analyzerResolutionReason",
                  })}
                  value={analyzerResolutionReason}
                  onChange={(event) =>
                    setAnalyzerResolutionReason(event.target.value)
                  }
                />
                <TextInput
                  id="microbiology-ast-replacement-card"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.replacementCardId",
                  })}
                  value={replacementCardId}
                  onChange={(event) => setReplacementCardId(event.target.value)}
                />
                <div className="microbiology-action-row">
                  <Button
                    kind="danger"
                    onClick={invalidateAndRepeat}
                    disabled={
                      busy ||
                      readOnly ||
                      !analyzerResolutionReason.trim() ||
                      !replacementCardId.trim()
                    }
                  >
                    {intl.formatMessage({
                      id: "microbiology.ast.invalidateAndRepeat",
                    })}
                  </Button>
                  <Button
                    kind="secondary"
                    onClick={overrideQcFailure}
                    disabled={
                      busy || readOnly || !analyzerResolutionReason.trim()
                    }
                  >
                    {intl.formatMessage({
                      id: "microbiology.ast.overrideQc",
                    })}
                  </Button>
                </div>
              </div>
            ) : null}
            {unresolvedExpertFlags && currentRun?.status === "RESULTS_IN" ? (
              <div className="microbiology-ast-flag-review">
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "microbiology.ast.expertFlags",
                  })}
                  subtitle={currentRun.analyzerExpertFlags}
                  hideCloseButton
                />
                <TextArea
                  id="microbiology-ast-flag-acknowledgement"
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.analyzerResolutionReason",
                  })}
                  value={analyzerResolutionReason}
                  onChange={(event) =>
                    setAnalyzerResolutionReason(event.target.value)
                  }
                />
                <Button
                  kind="secondary"
                  onClick={acknowledgeAnalyzerFlags}
                  disabled={
                    busy || readOnly || !analyzerResolutionReason.trim()
                  }
                >
                  {intl.formatMessage({
                    id: "microbiology.ast.acknowledgeFlags",
                  })}
                </Button>
              </div>
            ) : null}
            <div className="microbiology-form-grid">
              <Select
                id="microbiology-ast-isolate"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.isolate",
                })}
                value={activeIsolateId}
                onChange={(event) => setSelectedIsolateId(event.target.value)}
              >
                {isolates.map((isolate) => (
                  <SelectItem
                    key={isolate.id}
                    value={isolate.id}
                    text={isolate.isolateLabel}
                  />
                ))}
              </Select>
              {astSetup?.orderedPanelId && !adjustingPanel ? (
                <div className="microbiology-ast-panel-confirmation">
                  <span className="microbiology-card__hint">
                    {intl.formatMessage({
                      id: "microbiology.ast.orderedPanel",
                    })}
                  </span>
                  <strong>
                    {astSetup.orderedPanelLabel}
                    {astSetup.orderedPanelVersion
                      ? ` v${astSetup.orderedPanelVersion}`
                      : ""}
                  </strong>
                  <span className="microbiology-card__hint">
                    {intl.formatMessage({
                      id: "microbiology.ast.panelProvenance.organismDefault",
                    })}
                  </span>
                  <Button
                    kind="ghost"
                    size="sm"
                    type="button"
                    onClick={beginPanelAdjustment}
                  >
                    {intl.formatMessage({
                      id: "microbiology.ast.adjustPanel",
                    })}
                  </Button>
                </div>
              ) : (
                <div className="microbiology-ast-panel-adjustment">
                  <Select
                    id="microbiology-ast-panel"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.panel",
                    })}
                    value={selectedPanelId}
                    onChange={(event) =>
                      changeAdjustedPanel(event.target.value)
                    }
                  >
                    {panels.map((panel) => (
                      <SelectItem
                        key={panel.id}
                        value={panel.id}
                        text={panel.label}
                      />
                    ))}
                  </Select>
                  <FilterableMultiSelect
                    id="microbiology-ast-ordered-antibiotics"
                    titleText={intl.formatMessage({
                      id: "microbiology.ast.orderedAntibiotics",
                    })}
                    items={antibiotics}
                    itemToString={(item) => item?.label || ""}
                    selectedItems={antibiotics.filter((antibiotic) =>
                      adjustedAntibioticIds.includes(antibiotic.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setAdjustedAntibioticIds(
                        selectedItems.map((antibiotic) => antibiotic.id),
                      )
                    }
                  />
                  {orderAdjusted && (
                    <TextArea
                      id="microbiology-ast-panel-adjustment-reason"
                      labelText={intl.formatMessage({
                        id: "microbiology.ast.panelAdjustmentReason",
                      })}
                      value={panelAdjustmentReason}
                      onChange={(event) =>
                        setPanelAdjustmentReason(event.target.value)
                      }
                    />
                  )}
                  {astSetup?.orderedPanelId && (
                    <Button
                      kind="ghost"
                      size="sm"
                      type="button"
                      onClick={() => {
                        setSelectedPanelId(astSetup.orderedPanelId);
                        setPanelAdjustmentReason("");
                        setPanelAntibioticIds([]);
                        setAdjustedAntibioticIds([]);
                        setAdjustingPanel(false);
                      }}
                    >
                      {intl.formatMessage({ id: "button.cancel" })}
                    </Button>
                  )}
                </div>
              )}
              <Select
                id="microbiology-ast-technique"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.method",
                })}
                value={technique}
                onChange={(event) => setTechnique(event.target.value)}
                disabled={Boolean(currentRun)}
              >
                {TECHNIQUE_OPTIONS.map((option) => (
                  <SelectItem
                    key={option}
                    value={option}
                    text={formatMicrobiologyEnum(option)}
                  />
                ))}
              </Select>
              <Select
                id="microbiology-ast-breakpoint-standard"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.breakpointStandard",
                })}
                value={selectedStandardId}
                onChange={(event) => setSelectedStandardId(event.target.value)}
              >
                {breakpointStandards.map((standard) => (
                  <SelectItem
                    key={standard.id}
                    value={standard.id}
                    text={standard.label}
                  />
                ))}
              </Select>
              <RadioButtonGroup
                name="microbiology-ast-entry-mode"
                legendText={intl.formatMessage({
                  id: "microbiology.ast.entryMode",
                })}
                valueSelected={entryMode}
                onChange={setEntryMode}
                orientation="horizontal"
                disabled={Boolean(currentRun)}
              >
                <RadioButton
                  id="microbiology-ast-entry-manual"
                  value="MANUAL"
                  disabled={Boolean(currentRun)}
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.entryMode.manual",
                  })}
                />
                <RadioButton
                  id="microbiology-ast-entry-analyzer"
                  value="ANALYZER"
                  disabled={Boolean(currentRun)}
                  labelText={intl.formatMessage({
                    id: "microbiology.ast.entryMode.analyzer",
                  })}
                />
              </RadioButtonGroup>
              {entryMode === "ANALYZER" && !currentRun ? (
                <>
                  <Select
                    id="microbiology-ast-analyzer"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.analyzer",
                    })}
                    value={selectedAnalyzerId}
                    onChange={(event) =>
                      setSelectedAnalyzerId(event.target.value)
                    }
                  >
                    {analyzers.map((analyzer) => (
                      <SelectItem
                        key={analyzer.id}
                        value={analyzer.id}
                        text={analyzer.name}
                      />
                    ))}
                  </Select>
                  <TextInput
                    id="microbiology-ast-card-id"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.cardId",
                    })}
                    value={analyzerCardId}
                    onChange={(event) => setAnalyzerCardId(event.target.value)}
                  />
                </>
              ) : null}
              <div className="microbiology-ast-start-action">
                {!isolateIdentified && (
                  <p className="microbiology-card__hint">
                    {intl.formatMessage({
                      id: "microbiology.ast.identificationRequired",
                    })}
                  </p>
                )}
                <Tooltip
                  align="top"
                  label={
                    isolateIdentified
                      ? intl.formatMessage({ id: "microbiology.ast.startRun" })
                      : intl.formatMessage({
                          id: "microbiology.ast.identificationRequired",
                        })
                  }
                >
                  <span>
                    <Button
                      onClick={startRun}
                      disabled={
                        busy ||
                        readOnly ||
                        !!currentRun ||
                        !activeIsolateId ||
                        !isolateIdentified ||
                        setupUnavailable ||
                        !selectedPanelId ||
                        (adjustingPanel &&
                          adjustedAntibioticIds.length === 0) ||
                        (orderAdjusted && !panelAdjustmentReason.trim()) ||
                        (entryMode === "ANALYZER" &&
                          (!selectedAnalyzerId || !analyzerCardId.trim()))
                      }
                    >
                      {intl.formatMessage({ id: "microbiology.ast.startRun" })}
                    </Button>
                  </span>
                </Tooltip>
              </div>
              <div className="microbiology-form-grid__wide">
                <ReagentLotPicker
                  id="microbiology-ast-lots"
                  requirements={reagentRequirements}
                  selectedLots={selectedLots}
                  onChange={selectLot}
                  disabled={busy || readOnly}
                />
              </div>
            </div>
            {runs.length > 0 ? (
              <AstAttemptTable
                runs={runs}
                selectedRunId={currentRun?.id || ""}
                disabled={busy || readOnly}
                onView={viewRun}
                onSelectReportable={selectReportableRun}
              />
            ) : null}
            {currentRun ? (
              <div className="microbiology-card__body">
                <div className="microbiology-form-grid">
                  <Select
                    id="microbiology-ast-antibiotic"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.antibiotic",
                    })}
                    value={activeAntibioticId}
                    onChange={(event) =>
                      setSelectedAntibioticId(event.target.value)
                    }
                  >
                    {orderedAntibiotics.map((antibiotic) => (
                      <SelectItem
                        key={antibiotic.id}
                        value={antibiotic.id}
                        text={antibiotic.label}
                      />
                    ))}
                  </Select>
                  <TextInput
                    id="microbiology-ast-raw-value"
                    labelText={intl.formatMessage({
                      id:
                        measurementMode === "ZONE"
                          ? "microbiology.ast.measurement.zone"
                          : "microbiology.ast.measurement.mic",
                    })}
                    value={rawValue}
                    onChange={(event) => setRawValue(event.target.value)}
                  />
                  <Button
                    onClick={recordReading}
                    disabled={
                      busy ||
                      readOnly ||
                      isReviewed ||
                      ["AWAITING_RESULTS", "QC_FAILED", "INVALIDATED"].includes(
                        currentRun.status,
                      ) ||
                      !activeAntibioticId ||
                      !rawValue.trim()
                    }
                  >
                    {intl.formatMessage({
                      id: "microbiology.ast.recordReading",
                    })}
                  </Button>
                </div>
                {currentReading ? (
                  <>
                    <DataTable rows={readingRows} headers={readingHeaders}>
                      {({ rows, headers, getHeaderProps, getRowProps }) => (
                        <TableContainer>
                          <Table>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => {
                                const reading = currentReadings.find(
                                  (item) => item.id === row.id,
                                );
                                const history = reading?.overrideHistory || [];
                                const historyExpanded =
                                  expandedHistoryReadingId === reading?.id;
                                return (
                                  <React.Fragment key={row.id}>
                                    <TableRow
                                      {...getRowProps({ row })}
                                      data-testid="microbiology-ast-reading-row"
                                    >
                                      {row.cells.map((cell) => (
                                        <TableCell
                                          key={cell.id}
                                          data-testid={
                                            cell.info.header ===
                                            "interpretation"
                                              ? "microbiology-ast-interpretation"
                                              : undefined
                                          }
                                        >
                                          {cell.info.header ===
                                          "interpretation" ? (
                                            <>
                                              <strong>{cell.value}</strong>
                                              {reading?.overrideInterpretation
                                                ? ` (${reading.overrideInterpretation})`
                                                : ""}
                                            </>
                                          ) : cell.info.header ===
                                            "override" ? (
                                            <div>
                                              <span>{cell.value}</span>
                                              {history.length > 0 ? (
                                                <Button
                                                  kind="ghost"
                                                  size="sm"
                                                  type="button"
                                                  onClick={() => {
                                                    setExpandedHistoryReadingId(
                                                      historyExpanded
                                                        ? ""
                                                        : reading.id,
                                                    );
                                                    setRevertReason("");
                                                  }}
                                                >
                                                  {intl.formatMessage({
                                                    id: historyExpanded
                                                      ? "microbiology.ast.hideOriginal"
                                                      : "microbiology.ast.showOriginal",
                                                  })}
                                                </Button>
                                              ) : null}
                                            </div>
                                          ) : (
                                            cell.value
                                          )}
                                        </TableCell>
                                      ))}
                                    </TableRow>
                                    {historyExpanded ? (
                                      <TableRow data-testid="microbiology-ast-override-history">
                                        <TableCell colSpan={headers.length}>
                                          <div className="microbiology-ast-override-history">
                                            <h4>
                                              {intl.formatMessage({
                                                id: "microbiology.ast.overrideHistory",
                                              })}
                                            </h4>
                                            {history.map((event) => (
                                              <p key={event.id}>
                                                {formatMicrobiologyEnum(
                                                  event.fromInterpretation,
                                                )}{" "}
                                                {intl.formatMessage({
                                                  id: "microbiology.ast.historyTo",
                                                })}{" "}
                                                {formatMicrobiologyEnum(
                                                  event.toInterpretation,
                                                )}
                                                {` - ${event.reason} - ${
                                                  event.performedByDisplay ||
                                                  event.performedBy
                                                }`}
                                                {event.performedAt
                                                  ? ` - ${intl.formatDate(
                                                      event.performedAt,
                                                    )} ${intl.formatTime(
                                                      event.performedAt,
                                                    )}`
                                                  : ""}
                                              </p>
                                            ))}
                                            {reading.overrideInterpretation ? (
                                              <div className="microbiology-form-grid">
                                                <TextArea
                                                  id={`microbiology-ast-revert-reason-${reading.id}`}
                                                  labelText={intl.formatMessage(
                                                    {
                                                      id: "microbiology.ast.revertReason",
                                                    },
                                                  )}
                                                  value={revertReason}
                                                  onChange={(event) =>
                                                    setRevertReason(
                                                      event.target.value,
                                                    )
                                                  }
                                                />
                                                <Button
                                                  kind="danger--tertiary"
                                                  type="button"
                                                  onClick={() =>
                                                    revertOverride(reading.id)
                                                  }
                                                  disabled={
                                                    busy ||
                                                    readOnly ||
                                                    isReviewed ||
                                                    !revertReason.trim()
                                                  }
                                                >
                                                  {intl.formatMessage({
                                                    id: "microbiology.ast.revertOverride",
                                                  })}
                                                </Button>
                                              </div>
                                            ) : null}
                                          </div>
                                        </TableCell>
                                      </TableRow>
                                    ) : null}
                                  </React.Fragment>
                                );
                              })}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                    {currentReadings.some(
                      (reading) => reading.matchedBy === "NONE",
                    ) ? (
                      <InlineNotification
                        kind="warning"
                        lowContrast
                        hideCloseButton
                        title={intl.formatMessage({
                          id: "microbiology.ast.noBreakpointTitle",
                        })}
                        subtitle={intl.formatMessage({
                          id: "microbiology.ast.noBreakpointGuidance",
                        })}
                      />
                    ) : null}
                    <div className="microbiology-form-grid">
                      <Select
                        id="microbiology-ast-override-reading"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.reading",
                        })}
                        value={currentReading?.id || ""}
                        onChange={(event) =>
                          setSelectedReadingId(event.target.value)
                        }
                      >
                        {currentReadings.map((reading) => (
                          <SelectItem
                            key={reading.id}
                            value={reading.id}
                            text={`${antibioticLabelFor(reading)}: ${
                              reading.overrideInterpretation ||
                              reading.interpretation
                            }`}
                          />
                        ))}
                      </Select>
                      <Select
                        id="microbiology-ast-override"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.override",
                        })}
                        value={overrideInterpretation}
                        onChange={(event) =>
                          setOverrideInterpretation(event.target.value)
                        }
                      >
                        {OVERRIDE_OPTIONS.map((option) => (
                          <SelectItem
                            key={option}
                            value={option}
                            text={option}
                          />
                        ))}
                      </Select>
                      <TextArea
                        id="microbiology-ast-override-reason"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.overrideReason",
                        })}
                        value={overrideReason}
                        onChange={(event) =>
                          setOverrideReason(event.target.value)
                        }
                      />
                      <div>
                        <Button
                          kind="secondary"
                          onClick={overrideReading}
                          disabled={
                            busy ||
                            readOnly ||
                            isReviewed ||
                            !overrideReason.trim()
                          }
                        >
                          {intl.formatMessage({
                            id: "microbiology.ast.applyOverride",
                          })}
                        </Button>
                      </div>
                    </div>
                  </>
                ) : null}
                <Button
                  kind="primary"
                  onClick={reviewRun}
                  disabled={
                    busy ||
                    readOnly ||
                    isReviewed ||
                    !orderedResultsComplete ||
                    analyzerAcceptanceBlocked
                  }
                >
                  {intl.formatMessage({
                    id:
                      currentRun.status === "RESULTS_IN"
                        ? "microbiology.ast.acceptResults"
                        : "microbiology.ast.reviewRun",
                  })}
                </Button>
                {isReviewed ? (
                  <div className="microbiology-ast-repeat-form">
                    <h4>
                      {intl.formatMessage({
                        id: "microbiology.ast.newAttempt",
                      })}
                    </h4>
                    <RadioButtonGroup
                      name={`microbiology-ast-attempt-type-${currentRun.id}`}
                      legendText={intl.formatMessage({
                        id: "microbiology.ast.attemptType",
                      })}
                      valueSelected={attemptType}
                      onChange={setAttemptType}
                      orientation="horizontal"
                    >
                      <RadioButton
                        id={`microbiology-ast-repeat-${currentRun.id}`}
                        value="REPEAT"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.repeat",
                        })}
                      />
                      <RadioButton
                        id={`microbiology-ast-retest-${currentRun.id}`}
                        value="RETEST"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.retest",
                        })}
                      />
                    </RadioButtonGroup>
                    <RadioButtonGroup
                      name={`microbiology-ast-attempt-scope-${currentRun.id}`}
                      legendText={intl.formatMessage({
                        id: "microbiology.ast.attemptScope",
                      })}
                      valueSelected={attemptScope}
                      onChange={(value) => {
                        setAttemptScope(value);
                        setAttemptAntibioticId("");
                      }}
                      orientation="horizontal"
                    >
                      <RadioButton
                        id={`microbiology-ast-whole-panel-${currentRun.id}`}
                        value="WHOLE_PANEL"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.wholePanel",
                        })}
                      />
                      <RadioButton
                        id={`microbiology-ast-single-antibiotic-${currentRun.id}`}
                        value="SINGLE_ANTIBIOTIC"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.singleAntibiotic",
                        })}
                      />
                    </RadioButtonGroup>
                    <div className="microbiology-form-grid">
                      <TextArea
                        id={`microbiology-ast-attempt-reason-${currentRun.id}`}
                        autoFocus={initialAction === "new-ast-attempt"}
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.reasonForAttempt",
                        })}
                        value={attemptReason}
                        onChange={(event) =>
                          setAttemptReason(event.target.value)
                        }
                      />
                      <Select
                        id={`microbiology-ast-attempt-method-${currentRun.id}`}
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.attemptMethod",
                        })}
                        value={effectiveAttemptTechnique}
                        onChange={(event) =>
                          setAttemptTechnique(event.target.value)
                        }
                      >
                        {TECHNIQUE_OPTIONS.map((option) => (
                          <SelectItem
                            key={option}
                            value={option}
                            text={formatMicrobiologyEnum(option)}
                          />
                        ))}
                      </Select>
                      {attemptScope === "SINGLE_ANTIBIOTIC" ? (
                        <Select
                          id={`microbiology-ast-attempt-antibiotic-${currentRun.id}`}
                          labelText={intl.formatMessage({
                            id: "microbiology.ast.antibioticToRepeat",
                          })}
                          value={attemptAntibioticId}
                          onChange={(event) =>
                            setAttemptAntibioticId(event.target.value)
                          }
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "microbiology.ast.selectAntibiotic",
                            })}
                          />
                          {orderedAntibiotics.map((antibiotic) => (
                            <SelectItem
                              key={antibiotic.id}
                              value={antibiotic.id}
                              text={antibiotic.label}
                            />
                          ))}
                        </Select>
                      ) : null}
                    </div>
                    <Button
                      kind="secondary"
                      onClick={startRepeatRun}
                      disabled={
                        busy ||
                        readOnly ||
                        hasInProgressRun ||
                        !attemptReason.trim() ||
                        (attemptScope === "SINGLE_ANTIBIOTIC" &&
                          !attemptAntibioticId)
                      }
                    >
                      {intl.formatMessage(
                        { id: "microbiology.ast.startAttempt" },
                        {
                          type: formatMicrobiologyEnum(
                            attemptType,
                          ).toLowerCase(),
                        },
                      )}
                    </Button>
                  </div>
                ) : null}
              </div>
            ) : null}
            <ReagentUsageHistory usages={reagentUsages} />
          </>
        )}
      </div>
    </section>
  );
};

export default AstEntryPanel;
