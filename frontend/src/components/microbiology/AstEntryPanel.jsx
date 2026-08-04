import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import AstAttemptTable from "./AstAttemptTable";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const METHOD_OPTIONS = ["MIC", "ZONE"];
const OVERRIDE_OPTIONS = ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"];

const AstEntryPanel = ({
  caseId,
  workflowType,
  isolates = [],
  service,
  saving: caseSaving,
  onAstUpdated,
  readOnly = false,
}) => {
  const intl = useIntl();
  const [selectedIsolateId, setSelectedIsolateId] = useState("");
  const [panels, setPanels] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  const [breakpointStandards, setBreakpointStandards] = useState([]);
  const [selectedPanelId, setSelectedPanelId] = useState("");
  const [selectedAntibioticId, setSelectedAntibioticId] = useState("");
  const [selectedStandardId, setSelectedStandardId] = useState("");
  const [method, setMethod] = useState("MIC");
  const [rawValue, setRawValue] = useState("4");
  const [overrideInterpretation, setOverrideInterpretation] =
    useState("RESISTANT");
  const [overrideReason, setOverrideReason] = useState("");
  const [runs, setRuns] = useState([]);
  const [selectedRunId, setSelectedRunId] = useState("");
  const [selectedReadingId, setSelectedReadingId] = useState("");
  const [attemptType, setAttemptType] = useState("REPEAT");
  const [attemptReason, setAttemptReason] = useState("");
  const [attemptMethod, setAttemptMethod] = useState("");
  const [readiness, setReadiness] = useState(null);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState("");

  const activeIsolateId = selectedIsolateId || isolates[0]?.id || "";

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
  const currentReading =
    currentReadings.find((reading) => reading.id === selectedReadingId) ||
    currentReadings[0];

  const antibioticLabelFor = (reading) =>
    reading?.antibioticLabel ||
    antibiotics.find((antibiotic) => antibiotic.id === reading?.antibioticId)
      ?.label ||
    reading?.antibioticId;
  const busy = saving || caseSaving;
  const isReviewed = currentRun?.status === "REVIEWED";
  const hasInProgressRun = runs.some((run) => run.status === "IN_PROGRESS");
  const effectiveAttemptMethod = attemptMethod || currentRun?.method || "MIC";

  const viewRun = (runId) => {
    setSelectedRunId(runId);
    setAttemptMethod("");
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
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
      }
    });

  const startRepeatRun = () =>
    runOperation(() =>
      service.startRepeatAstRun(currentRun.id, {
        attemptType,
        reason: attemptReason,
        method: effectiveAttemptMethod,
      }),
    ).then((run) => {
      if (run) {
        setSelectedRunId(run.id);
        setAttemptReason("");
      }
    });

  const recordReading = () =>
    runOperation(() =>
      service.recordAstReading(currentRun.id, {
        antibioticId: selectedAntibioticId,
        method: currentRun.method || method,
        rawValue,
      }),
    );

  const overrideReading = () =>
    runOperation(() =>
      service.overrideAstReading(currentReading.id, {
        overrideInterpretation,
        overrideReason,
      }),
    );

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
              <Select
                id="microbiology-ast-panel"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.panel",
                })}
                value={selectedPanelId}
                onChange={(event) => setSelectedPanelId(event.target.value)}
              >
                {panels.map((panel) => (
                  <SelectItem
                    key={panel.id}
                    value={panel.id}
                    text={panel.label}
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
              <div>
                <Button
                  onClick={startRun}
                  disabled={
                    busy ||
                    readOnly ||
                    !!currentRun ||
                    !activeIsolateId ||
                    !selectedPanelId
                  }
                >
                  {intl.formatMessage({ id: "microbiology.ast.startRun" })}
                </Button>
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
                    value={selectedAntibioticId}
                    onChange={(event) =>
                      setSelectedAntibioticId(event.target.value)
                    }
                  >
                    {antibiotics.map((antibiotic) => (
                      <SelectItem
                        key={antibiotic.id}
                        value={antibiotic.id}
                        text={antibiotic.label}
                      />
                    ))}
                  </Select>
                  <Select
                    id="microbiology-ast-method"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.method",
                    })}
                    value={currentRun.method || method}
                    onChange={(event) => setMethod(event.target.value)}
                    disabled={isReviewed || !!currentRun.method}
                  >
                    {METHOD_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option} text={option} />
                    ))}
                  </Select>
                  <TextInput
                    id="microbiology-ast-raw-value"
                    labelText={intl.formatMessage({
                      id: "microbiology.ast.rawValue",
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
                      !selectedAntibioticId ||
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
                    <table className="microbiology-table">
                      <thead>
                        <tr>
                          <th>
                            {intl.formatMessage({
                              id: "microbiology.ast.antibiotic",
                            })}
                          </th>
                          <th>
                            {intl.formatMessage({
                              id: "microbiology.ast.method",
                            })}
                          </th>
                          <th>
                            {intl.formatMessage({
                              id: "microbiology.ast.rawValue",
                            })}
                          </th>
                          <th>
                            {intl.formatMessage({
                              id: "microbiology.ast.interpretation",
                            })}
                          </th>
                          <th>
                            {intl.formatMessage({
                              id: "microbiology.ast.override",
                            })}
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {currentReadings.map((reading) => (
                          <tr
                            key={reading.id}
                            data-testid="microbiology-ast-reading-row"
                          >
                            <td>{antibioticLabelFor(reading)}</td>
                            <td>{reading.method}</td>
                            <td>{reading.rawValue ?? reading.rawText}</td>
                            <td data-testid="microbiology-ast-interpretation">
                              <strong>{reading.interpretation}</strong>
                              {reading.overrideInterpretation
                                ? ` (${reading.overrideInterpretation})`
                                : ""}
                            </td>
                            <td>
                              {reading.overrideReason ||
                                intl.formatMessage({
                                  id: "microbiology.ast.noOverride",
                                })}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
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
                        value={effectiveAttemptMethod}
                        onChange={(event) =>
                          setAttemptMethod(event.target.value)
                        }
                      >
                        {METHOD_OPTIONS.map((option) => (
                          <SelectItem
                            key={option}
                            value={option}
                            text={option}
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
          </>
        )}
      </div>
    </section>
  );
};

export default AstEntryPanel;
