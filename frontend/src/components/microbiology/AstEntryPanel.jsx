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
import ReagentLotPicker from "./ReagentLotPicker";
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
}) => {
  const intl = useIntl();
  const [selectedIsolateId, setSelectedIsolateId] = useState("");
  const [panels, setPanels] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  const [breakpointStandards, setBreakpointStandards] = useState([]);
  const [selectedPanelId, setSelectedPanelId] = useState("");
  const [astSetup, setAstSetup] = useState(null);
  const [adjustingPanel, setAdjustingPanel] = useState(false);
  const [panelAdjustmentReason, setPanelAdjustmentReason] = useState("");
  const [panelAntibioticIds, setPanelAntibioticIds] = useState([]);
  const [adjustedAntibioticIds, setAdjustedAntibioticIds] = useState([]);
  const [selectedAntibioticId, setSelectedAntibioticId] = useState("");
  const [selectedStandardId, setSelectedStandardId] = useState("");
  const [technique, setTechnique] = useState("VITEK_2");
  const [rawValue, setRawValue] = useState("4");
  const [overrideInterpretation, setOverrideInterpretation] =
    useState("RESISTANT");
  const [overrideReason, setOverrideReason] = useState("");
  const [expandedHistoryReadingId, setExpandedHistoryReadingId] = useState("");
  const [revertReason, setRevertReason] = useState("");
  const [runs, setRuns] = useState([]);
  const [selectedRunId, setSelectedRunId] = useState("");
  const [selectedReadingId, setSelectedReadingId] = useState("");
  const [attemptType, setAttemptType] = useState("REPEAT");
  const [attemptReason, setAttemptReason] = useState("");
  const [attemptTechnique, setAttemptTechnique] = useState("");
  const [readiness, setReadiness] = useState(null);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState("");
  const [selectedLots, setSelectedLots] = useState({});

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
  const hasInProgressRun = runs.some((run) => run.status === "IN_PROGRESS");
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
    interpretation: reading.interpretation,
    override:
      reading.overrideReason ||
      intl.formatMessage({ id: "microbiology.ast.noOverride" }),
  }));

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
            formatMicrobiologyEnum(result.message || result.error),
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
        ...(lotSelections.length > 0 ? { lotSelections } : {}),
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
        setAttemptReason("");
        setSelectedLots({});
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
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.ast.hint" })}
          </p>
        </div>
        {currentRun && (
          <div data-testid="microbiology-ast-run-status">
            <Tag type={currentRun.status === "REVIEWED" ? "green" : "cyan"}>
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
                        (orderAdjusted && !panelAdjustmentReason.trim())
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
                    !currentRun.readings?.length
                  }
                >
                  {intl.formatMessage({ id: "microbiology.ast.reviewRun" })}
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
                    <div className="microbiology-form-grid">
                      <TextArea
                        id={`microbiology-ast-attempt-reason-${currentRun.id}`}
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
                    </div>
                    <Button
                      kind="secondary"
                      onClick={startRepeatRun}
                      disabled={
                        busy ||
                        readOnly ||
                        hasInProgressRun ||
                        !attemptReason.trim()
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
