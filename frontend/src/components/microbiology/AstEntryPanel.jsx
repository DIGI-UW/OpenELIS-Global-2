import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  InlineNotification,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";

const METHOD_OPTIONS = ["MIC", "ZONE"];
const OVERRIDE_OPTIONS = ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"];

const AstEntryPanel = ({
  caseId,
  workflowType,
  isolates = [],
  service,
  saving: caseSaving,
  onAstUpdated,
}) => {
  const intl = useIntl();
  const [selectedIsolateId, setSelectedIsolateId] = useState("");
  const [panels, setPanels] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  const [selectedPanelId, setSelectedPanelId] = useState("");
  const [selectedAntibioticId, setSelectedAntibioticId] = useState("");
  const [method, setMethod] = useState("MIC");
  const [rawValue, setRawValue] = useState("4");
  const [overrideInterpretation, setOverrideInterpretation] =
    useState("RESISTANT");
  const [overrideReason, setOverrideReason] = useState("");
  const [runs, setRuns] = useState([]);
  const [readiness, setReadiness] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!selectedIsolateId && isolates.length > 0) {
      setSelectedIsolateId(isolates[0].id);
    }
  }, [isolates, selectedIsolateId]);

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
  }, [service, workflowType]);

  const loadAstState = () => {
    if (!selectedIsolateId) {
      setRuns([]);
      return Promise.resolve();
    }
    return Promise.all([
      service.getAstRunsForIsolate(selectedIsolateId).then((items = []) => {
        setRuns(items);
      }),
      service.getCaseReadiness(caseId).then((value) => {
        setReadiness(value);
      }),
    ]);
  };

  useEffect(() => {
    loadAstState();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId, selectedIsolateId]);

  const currentRun = useMemo(
    () =>
      runs.find((run) => run.status === "IN_PROGRESS") ||
      (runs.length > 0 ? runs[runs.length - 1] : null),
    [runs],
  );
  const currentReading = currentRun?.readings?.[0];
  const currentAntibioticLabel =
    currentReading?.antibioticLabel ||
    antibiotics.find(
      (antibiotic) => antibiotic.id === currentReading?.antibioticId,
    )?.label ||
    antibiotics.find((antibiotic) => antibiotic.id === selectedAntibioticId)
      ?.label ||
    currentReading?.antibioticId;
  const currentReadingValue =
    currentReading?.rawValue ?? currentReading?.rawText ?? rawValue;
  const busy = saving || caseSaving;

  const runOperation = (operation) => {
    setSaving(true);
    return operation()
      .then(loadAstState)
      .then(() => {
        if (onAstUpdated) {
          onAstUpdated();
        }
      })
      .finally(() => setSaving(false));
  };

  const startRun = () =>
    runOperation(() =>
      service.startAstRun({
        isolateId: selectedIsolateId,
        panelId: selectedPanelId,
      }),
    );

  const recordReading = () =>
    runOperation(() =>
      service.recordAstReading(currentRun.id, {
        antibioticId: selectedAntibioticId,
        method,
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

  return (
    <section
      className="microbiology-card"
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
              {currentRun.status}
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
                subtitle={(readiness.blockers || []).join(", ")}
                hideCloseButton
              />
            ) : null}
            <div className="microbiology-form-grid">
              <Select
                id="microbiology-ast-isolate"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.isolate",
                })}
                value={selectedIsolateId}
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
              <div>
                <Button
                  onClick={startRun}
                  disabled={busy || !selectedIsolateId || !selectedPanelId}
                >
                  {intl.formatMessage({ id: "microbiology.ast.startRun" })}
                </Button>
              </div>
            </div>
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
                    value={method}
                    onChange={(event) => setMethod(event.target.value)}
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
                      currentRun.status === "REVIEWED" ||
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
                        <tr>
                          <td>{currentAntibioticLabel}</td>
                          <td>{currentReading.method}</td>
                          <td>{currentReadingValue}</td>
                          <td data-testid="microbiology-ast-interpretation">
                            <strong>{currentReading.interpretation}</strong>
                            {currentReading.overrideInterpretation
                              ? ` (${currentReading.overrideInterpretation})`
                              : ""}
                          </td>
                          <td>
                            {currentReading.overrideReason ||
                              intl.formatMessage({
                                id: "microbiology.ast.noOverride",
                              })}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                    <div className="microbiology-form-grid">
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
                          disabled={busy || !overrideReason.trim()}
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
                    currentRun.status === "REVIEWED" ||
                    !currentRun.readings?.length
                  }
                >
                  {intl.formatMessage({ id: "microbiology.ast.reviewRun" })}
                </Button>
              </div>
            ) : null}
          </>
        )}
      </div>
    </section>
  );
};

export default AstEntryPanel;
