import React, { useEffect, useMemo, useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
  DataTable,
  InlineNotification,
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
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const TECHNIQUE_OPTIONS = ["ETEST", "BROTH_MICRODILUTION", "DISK_DIFFUSION"];
const OVERRIDE_OPTIONS = ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"];

const measurementForTechnique = (technique) =>
  technique === "DISK_DIFFUSION" ? "ZONE" : technique ? "MIC" : "";

const sameOrder = (left, right) =>
  left.length === right.length &&
  left.every((value, index) => value === right[index]);

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
  const [breakpointStandards, setBreakpointStandards] = useState([]);
  const [setup, setSetup] = useState(null);
  const [selectedPanelId, setSelectedPanelId] = useState("");
  const [panelAntibiotics, setPanelAntibiotics] = useState([]);
  const [orderedAntibioticIds, setOrderedAntibioticIds] = useState([]);
  const [customizeOrder, setCustomizeOrder] = useState(false);
  const [panelAdjustmentReason, setPanelAdjustmentReason] = useState("");
  const [selectedStandardId, setSelectedStandardId] = useState("");
  const [technique, setTechnique] = useState("");
  const [selectedAntibioticId, setSelectedAntibioticId] = useState("");
  const [rawValue, setRawValue] = useState("4");
  const [overrideInterpretation, setOverrideInterpretation] =
    useState("RESISTANT");
  const [overrideReason, setOverrideReason] = useState("");
  const [revertReason, setRevertReason] = useState("");
  const [runs, setRuns] = useState([]);
  const [selectedReadingId, setSelectedReadingId] = useState("");
  const [readiness, setReadiness] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

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
    service.getAntibiotics().then(setAntibiotics);
    service.getBreakpointStandards().then((items = []) => {
      setBreakpointStandards(items);
      if (items.length > 0) {
        setSelectedStandardId((current) => current || items[0].id);
      }
    });
  }, [service, workflowType]);

  useEffect(() => {
    if (!selectedIsolateId) {
      setSetup(null);
      return;
    }
    service.getAstSetupForIsolate(selectedIsolateId).then((value) => {
      setSetup(value);
      if (value?.orderedPanelId) {
        setSelectedPanelId(value.orderedPanelId);
      }
    });
  }, [selectedIsolateId, service]);

  useEffect(() => {
    if (!selectedPanelId) {
      setPanelAntibiotics([]);
      setOrderedAntibioticIds([]);
      return;
    }
    service.getAstPanelAntibiotics(selectedPanelId).then((items = []) => {
      setPanelAntibiotics(items);
      setOrderedAntibioticIds(items.map((item) => item.antibioticId));
      setCustomizeOrder(false);
      setPanelAdjustmentReason("");
    });
  }, [selectedPanelId, service]);

  const loadAstState = () => {
    if (!selectedIsolateId) {
      setRuns([]);
      return Promise.resolve();
    }
    return Promise.all([
      service.getAstRunsForIsolate(selectedIsolateId).then(setRuns),
      service.getCaseReadiness(caseId).then(setReadiness),
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
  const currentReadings = currentRun?.readings || [];
  const currentReading =
    currentReadings.find((reading) => reading.id === selectedReadingId) ||
    currentReadings[0];

  const antibioticById = useMemo(
    () => new Map(antibiotics.map((antibiotic) => [antibiotic.id, antibiotic])),
    [antibiotics],
  );
  const currentOrderedAntibiotics = useMemo(
    () =>
      (currentRun?.orderedAntibiotics || []).map((ordered) => ({
        ...ordered,
        label:
          antibioticById.get(ordered.antibioticId)?.label ||
          ordered.antibioticId,
      })),
    [antibioticById, currentRun],
  );

  useEffect(() => {
    if (
      currentReadings.length > 0 &&
      !currentReadings.some((reading) => reading.id === selectedReadingId)
    ) {
      setSelectedReadingId(currentReadings[0].id);
    }
  }, [currentReadings, selectedReadingId]);

  useEffect(() => {
    if (
      currentOrderedAntibiotics.length > 0 &&
      !currentOrderedAntibiotics.some(
        (ordered) => ordered.antibioticId === selectedAntibioticId,
      )
    ) {
      setSelectedAntibioticId(currentOrderedAntibiotics[0].antibioticId);
    }
  }, [currentOrderedAntibiotics, selectedAntibioticId]);

  const baselineAntibioticIds = panelAntibiotics.map(
    (item) => item.antibioticId,
  );
  const adjusted =
    (Boolean(setup?.orderedPanelId) &&
      selectedPanelId !== setup.orderedPanelId) ||
    !sameOrder(orderedAntibioticIds, baselineAntibioticIds);
  const measurementType = measurementForTechnique(technique);
  const busy = saving || caseSaving;
  const isReviewed = currentRun?.status === "REVIEWED";
  const reviewComplete =
    currentOrderedAntibiotics.length > 0 &&
    currentOrderedAntibiotics.every((ordered) =>
      currentReadings.some(
        (reading) => reading.antibioticId === ordered.antibioticId,
      ),
    );

  const runOperation = (operation) => {
    setError("");
    setSaving(true);
    return Promise.resolve(operation())
      .then(loadAstState)
      .then(() => onAstUpdated?.())
      .catch((operationError) => {
        setError(operationError?.message || "AST_OPERATION_FAILED");
      })
      .finally(() => setSaving(false));
  };

  const startRun = () => {
    const payload = {
      isolateId: selectedIsolateId,
      panelId: selectedPanelId,
      breakpointStandardId: selectedStandardId,
      technique,
      orderedAntibioticIds,
    };
    if (adjusted) {
      payload.panelAdjustmentReason = panelAdjustmentReason.trim();
    }
    return runOperation(() => service.startAstRun(payload));
  };

  const recordReading = () =>
    runOperation(() =>
      service.recordAstReading(currentRun.id, {
        antibioticId: selectedAntibioticId,
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

  const revertOverride = () =>
    runOperation(() =>
      service.revertAstOverride(currentReading.id, {
        overrideReason: revertReason,
      }),
    );

  const reviewRun = () =>
    runOperation(() => service.reviewAstRun(currentRun.id));

  const toggleOrderedAntibiotic = (antibioticId, checked) => {
    setOrderedAntibioticIds((current) =>
      checked
        ? current.includes(antibioticId)
          ? current
          : [...current, antibioticId]
        : current.filter((value) => value !== antibioticId),
    );
  };

  const readingRows = currentReadings.map((reading) => ({
    id: reading.id,
    antibiotic:
      antibioticById.get(reading.antibioticId)?.label || reading.antibioticId,
    value: `${reading.rawValue ?? reading.rawText ?? "-"}${
      reading.units ? ` ${reading.units}` : ""
    }`,
    interpretation: formatMicrobiologyEnum(
      reading.overrideInterpretation || reading.interpretation,
      intl,
    ),
    matchedBy: formatMicrobiologyEnum(reading.matchedBy || "NONE", intl),
    source: formatMicrobiologyEnum(reading.source || "MANUAL_ENTRY", intl),
  }));
  const readingHeaders = [
    {
      key: "antibiotic",
      header: intl.formatMessage({ id: "microbiology.ast.antibiotic" }),
    },
    {
      key: "value",
      header: intl.formatMessage({ id: "microbiology.ast.rawValue" }),
    },
    {
      key: "interpretation",
      header: intl.formatMessage({ id: "microbiology.ast.interpretation" }),
    },
    {
      key: "matchedBy",
      header: intl.formatMessage({ id: "microbiology.ast.matchedBy" }),
    },
    {
      key: "source",
      header: intl.formatMessage({ id: "microbiology.ast.source" }),
    },
  ];

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-ast-card"
      aria-labelledby="microbiology-ast-heading"
    >
      <Stack gap={5}>
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
            <Tag type={currentRun.status === "REVIEWED" ? "green" : "cyan"}>
              {formatMicrobiologyEnum(currentRun.status, intl)}
            </Tag>
          )}
        </div>

        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.ast.operationFailed",
            })}
            subtitle={formatMicrobiologyEnum(error, intl)}
          />
        )}

        {isolates.length === 0 ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.ast.noIsolate" })}
          />
        ) : (
          <>
            {readiness && (
              <InlineNotification
                kind={readiness.finalReleaseReady ? "success" : "warning"}
                title={intl.formatMessage({
                  id: readiness.finalReleaseReady
                    ? "microbiology.readiness.ready"
                    : "microbiology.readiness.blocked",
                })}
                subtitle={(readiness.blockers || [])
                  .map((blocker) => formatMicrobiologyEnum(blocker, intl))
                  .join(", ")}
                hideCloseButton
              />
            )}

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
                labelText={intl.formatMessage({ id: "microbiology.ast.panel" })}
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
              <Select
                id="microbiology-ast-technique"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.technique",
                })}
                value={technique}
                onChange={(event) => setTechnique(event.target.value)}
              >
                <SelectItem value="" text="" />
                {TECHNIQUE_OPTIONS.map((option) => (
                  <SelectItem
                    key={option}
                    value={option}
                    text={formatMicrobiologyEnum(option, intl)}
                  />
                ))}
              </Select>
            </div>

            {setup?.orderedPanelId && (
              <div>
                <strong>
                  {`${setup.orderedPanelLabel} v${setup.orderedPanelVersion}`}
                </strong>{" "}
                <Tag type="blue">
                  {formatMicrobiologyEnum(setup.panelProvenance, intl)}
                </Tag>
              </div>
            )}

            {measurementType && (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id:
                    measurementType === "ZONE"
                      ? "microbiology.ast.zoneMeasurement"
                      : "microbiology.ast.micMeasurement",
                })}
              />
            )}

            <Checkbox
              id="microbiology-ast-customize-order"
              aria-label={intl.formatMessage({
                id: "microbiology.ast.customizeOrderedAntibiotics",
              })}
              labelText={intl.formatMessage({
                id: "microbiology.ast.customizeOrderedAntibiotics",
              })}
              checked={customizeOrder}
              onChange={(_event, { checked }) => {
                setCustomizeOrder(checked);
                if (!checked) {
                  setOrderedAntibioticIds(baselineAntibioticIds);
                }
              }}
            />

            {customizeOrder && (
              <fieldset>
                <legend>
                  {intl.formatMessage({
                    id: "microbiology.ast.orderedAntibiotics",
                  })}
                </legend>
                <Stack gap={3}>
                  {antibiotics.map((antibiotic) => (
                    <Checkbox
                      key={antibiotic.id}
                      id={`microbiology-ast-ordered-${antibiotic.id}`}
                      aria-label={antibiotic.label}
                      labelText={antibiotic.label}
                      checked={orderedAntibioticIds.includes(antibiotic.id)}
                      onChange={(_event, { checked }) =>
                        toggleOrderedAntibiotic(antibiotic.id, checked)
                      }
                    />
                  ))}
                </Stack>
              </fieldset>
            )}

            {adjusted && (
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

            <Button
              onClick={startRun}
              disabled={
                busy ||
                runs.some((run) => run.status === "IN_PROGRESS") ||
                !selectedIsolateId ||
                !selectedPanelId ||
                !selectedStandardId ||
                !technique ||
                orderedAntibioticIds.length === 0 ||
                (adjusted && !panelAdjustmentReason.trim())
              }
            >
              {intl.formatMessage({ id: "microbiology.ast.startRun" })}
            </Button>

            {currentRun && (
              <Stack gap={5}>
                <div>
                  <Tag type="outline">
                    {formatMicrobiologyEnum(currentRun.technique, intl)}
                  </Tag>{" "}
                  <Tag type="gray">
                    {formatMicrobiologyEnum(currentRun.measurementType, intl)}
                  </Tag>
                </div>

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
                    {currentOrderedAntibiotics.map((ordered) => (
                      <SelectItem
                        key={ordered.antibioticId}
                        value={ordered.antibioticId}
                        text={ordered.label}
                      />
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

                {readingRows.length > 0 && (
                  <DataTable rows={readingRows} headers={readingHeaders}>
                    {({
                      rows: tableRows,
                      headers,
                      getTableProps,
                      getHeaderProps,
                      getRowProps,
                    }) => (
                      <TableContainer
                        title={intl.formatMessage({
                          id: "microbiology.ast.recordedReadings",
                        })}
                      >
                        <Table {...getTableProps()} tabIndex={0}>
                          <TableHead>
                            <TableRow>
                              {headers.map((header) => (
                                <TableHeader
                                  {...getHeaderProps({ header })}
                                  key={header.key}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {tableRows.map((row) => (
                              <TableRow {...getRowProps({ row })} key={row.id}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                )}

                {currentReading && (
                  <Stack gap={4}>
                    <div className="microbiology-form-grid">
                      <Select
                        id="microbiology-ast-override-reading"
                        labelText={intl.formatMessage({
                          id: "microbiology.ast.reading",
                        })}
                        value={currentReading.id}
                        onChange={(event) =>
                          setSelectedReadingId(event.target.value)
                        }
                      >
                        {currentReadings.map((item) => (
                          <SelectItem
                            key={item.id}
                            value={item.id}
                            text={
                              antibioticById.get(item.antibioticId)?.label ||
                              item.antibioticId
                            }
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
                            text={formatMicrobiologyEnum(option, intl)}
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
                      <Button
                        kind="secondary"
                        onClick={overrideReading}
                        disabled={busy || isReviewed || !overrideReason.trim()}
                      >
                        {intl.formatMessage({
                          id: "microbiology.ast.applyOverride",
                        })}
                      </Button>
                    </div>

                    {(currentReading.overrideHistory || []).length > 0 && (
                      <Accordion align="start">
                        <AccordionItem
                          open
                          title={intl.formatMessage({
                            id: "microbiology.ast.overrideHistory",
                          })}
                        >
                          <ol
                            aria-label={intl.formatMessage({
                              id: "microbiology.ast.overrideHistory",
                            })}
                          >
                            {currentReading.overrideHistory.map((event) => (
                              <li key={event.id}>
                                <strong>
                                  {event.performedByDisplay ||
                                    event.performedBy}
                                </strong>{" "}
                                {formatMicrobiologyEnum(event.action, intl)}:{" "}
                                {formatMicrobiologyEnum(
                                  event.fromInterpretation,
                                  intl,
                                )}{" "}
                                to{" "}
                                {formatMicrobiologyEnum(
                                  event.toInterpretation,
                                  intl,
                                )}
                                <div>{event.reason}</div>
                              </li>
                            ))}
                          </ol>
                        </AccordionItem>
                      </Accordion>
                    )}

                    {currentReading.overrideInterpretation && (
                      <div className="microbiology-form-grid">
                        <TextArea
                          id="microbiology-ast-revert-reason"
                          labelText={intl.formatMessage({
                            id: "microbiology.ast.revertReason",
                          })}
                          value={revertReason}
                          onChange={(event) =>
                            setRevertReason(event.target.value)
                          }
                        />
                        <Button
                          kind="danger--tertiary"
                          onClick={revertOverride}
                          disabled={busy || isReviewed || !revertReason.trim()}
                        >
                          {intl.formatMessage({
                            id: "microbiology.ast.revertOverride",
                          })}
                        </Button>
                      </div>
                    )}
                  </Stack>
                )}

                <Button
                  kind="primary"
                  onClick={reviewRun}
                  disabled={busy || isReviewed || !reviewComplete}
                >
                  {intl.formatMessage({ id: "microbiology.ast.reviewRun" })}
                </Button>
              </Stack>
            )}
          </>
        )}
      </Stack>
    </section>
  );
};

export default AstEntryPanel;
