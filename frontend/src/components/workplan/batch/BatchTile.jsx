import React, { useEffect, useRef, useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  InlineLoading,
  InlineNotification,
  Link,
  OverflowMenu,
  OverflowMenuItem,
  ProgressIndicator,
  ProgressStep,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import {
  ChevronDown,
  ChevronUp,
  Printer,
  Renew,
  Save,
  TrashCan,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import QcEntryForm from "./QcEntryForm";
import QcOverrideModal from "./QcOverrideModal";
import {
  BATCH_ENDPOINT,
  formatDate,
  formatDateTime,
  parseJson,
  priorityTagType,
  qcNotificationKind,
  qcSubjects,
  qcTagType,
  statusTagType,
} from "./batchWorkplanUtils";

const POLL_INTERVAL_MS = 1000;
const POLL_TIMEOUT_MS = 30000;

/**
 * One batch on the Batches tab (OGC-427 FR-BL-005/008, FR-RA, FR-QC, FR-QCE,
 * FR-NCE, FR-WG). Collapsed it shows the batch's reagent, QC and NCE tags;
 * expanded it walks the two setup phases and the release.
 */
export default function BatchTile({
  batch,
  expanded,
  onToggle,
  onChanged,
  notify,
  canOverride,
}) {
  const intl = useIntl();
  const draft = batch.status === "DRAFT";
  const active = batch.status === "ACTIVE";
  const qc = batch.qc;
  const latestNce = (batch.releases || []).find((release) => release.nceNumber);

  const [options, setOptions] = useState(null);
  const [reagentId, setReagentId] = useState(batch.reagentId || "");
  const [lotId, setLotId] = useState(batch.reagentLotId || "");
  const [instrumentId, setInstrumentId] = useState(batch.instrumentId || "");
  const [busy, setBusy] = useState(false);
  const [entryOpen, setEntryOpen] = useState(false);
  const [evaluating, setEvaluating] = useState(false);
  const [evaluationTimedOut, setEvaluationTimedOut] = useState(false);
  const [override, setOverride] = useState(null);
  const pollTimer = useRef(null);

  useEffect(() => {
    setReagentId(batch.reagentId || "");
    setLotId(batch.reagentLotId || "");
    setInstrumentId(batch.instrumentId || "");
  }, [batch.reagentId, batch.reagentLotId, batch.instrumentId]);

  useEffect(() => {
    if (expanded && draft && options === null) {
      getFromOpenElisServer(
        `${BATCH_ENDPOINT}/batches/${batch.id}/setup-options`,
        (body) =>
          setOptions(
            body && Array.isArray(body.reagents)
              ? body
              : { reagents: [], instruments: [] },
          ),
      );
    }
  }, [expanded, draft, options, batch.id]);

  useEffect(() => () => clearTimeout(pollTimer.current), []);

  const fail = async (response, fallbackId) => {
    const body = await parseJson(response);
    notify("error", body?.message || intl.formatMessage({ id: fallbackId }));
    return body;
  };

  const updateTests = (analysisIds) => {
    setBusy(true);
    putToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batch.id}`,
      JSON.stringify({ analysisIds }),
      async (response) => {
        setBusy(false);
        if (response?.ok) {
          notify(
            "success",
            intl.formatMessage({ id: "workplan.batch.tests.updated" }),
          );
          onChanged();
          return;
        }
        await fail(response, "workplan.batch.tests.updateFailed");
      },
    );
  };

  const transition = (status, successId) => {
    setBusy(true);
    putToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batch.id}/status`,
      JSON.stringify({ status }),
      async (response) => {
        setBusy(false);
        if (response?.ok) {
          notify(
            "success",
            intl.formatMessage({ id: successId }, { name: batch.name }),
          );
          onChanged();
          return;
        }
        await fail(response, "workplan.batch.status.error");
      },
    );
  };

  const saveReagent = () => {
    setBusy(true);
    putToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batch.id}/reagent`,
      JSON.stringify({
        reagentId: Number(reagentId),
        reagentLotId: Number(lotId),
        instrumentId: instrumentId || null,
      }),
      async (response) => {
        setBusy(false);
        if (response?.ok) {
          notify(
            "success",
            intl.formatMessage({ id: "workplan.batch.reagent.saved" }),
          );
          onChanged();
          return;
        }
        await fail(response, "workplan.batch.reagent.saveFailed");
      },
    );
  };

  const pollQc = (startedAt) => {
    getFromOpenElisServer(
      `${BATCH_ENDPOINT}/batches/${batch.id}/qc-status`,
      (body) => {
        if (body && body.evaluationPending === false) {
          setEvaluating(false);
          notify(
            body.status === "PASS" ? "success" : "warning",
            intl.formatMessage(
              { id: "workplan.batch.qc.evaluated" },
              {
                status: intl.formatMessage({
                  id: `workplan.batch.qc.status.${body.status}`,
                }),
              },
            ),
          );
          onChanged();
          return;
        }
        if (Date.now() - startedAt >= POLL_TIMEOUT_MS) {
          setEvaluating(false);
          setEvaluationTimedOut(true);
          onChanged();
          return;
        }
        pollTimer.current = setTimeout(
          () => pollQc(startedAt),
          POLL_INTERVAL_MS,
        );
      },
    );
  };

  const qcSaved = () => {
    setEntryOpen(false);
    setEvaluationTimedOut(false);
    setEvaluating(true);
    notify("info", intl.formatMessage({ id: "workplan.batch.qc.saved" }));
    pollQc(Date.now());
  };

  const generate = (withOverride) => {
    setBusy(true);
    postToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batch.id}/generate`,
      JSON.stringify({ override: withOverride }),
      async (response) => {
        setBusy(false);
        if (response?.ok) {
          const released = await parseJson(response);
          const nce = (released?.releases || []).find(
            (release) => release.nceNumber,
          );
          setOverride(null);
          notify(
            "success",
            nce && withOverride
              ? intl.formatMessage(
                  { id: "workplan.batch.generated.nce" },
                  { name: batch.name, nce: nce.nceNumber },
                )
              : intl.formatMessage(
                  { id: "workplan.batch.generated" },
                  { name: batch.name },
                ),
          );
          onChanged();
          return;
        }
        const body = await parseJson(response);
        if (response?.status === 409 && body?.qc) {
          setOverride(body.qc);
          return;
        }
        setOverride(null);
        notify(
          "error",
          response?.status === 403
            ? intl.formatMessage({ id: "workplan.batch.override.noPermission" })
            : body?.message ||
                intl.formatMessage({ id: "workplan.batch.generate.failed" }),
        );
      },
    );
  };

  const reagentOptions = options?.reagents || [];
  const selectedReagent = reagentOptions.find(
    (reagent) => String(reagent.id) === String(reagentId),
  );
  const lots = selectedReagent?.lots || [];
  const setupChanged =
    String(reagentId) !== String(batch.reagentId || "") ||
    String(lotId) !== String(batch.reagentLotId || "") ||
    String(instrumentId) !== String(batch.instrumentId || "");
  const setupDone = Boolean(batch.reagentLotId);
  const recordableLots = (qc?.lots || []).filter((lot) => lot.canRecordInline);
  const analyzerLotsNeedingQc = (qc?.lots || []).filter(
    (lot) => !lot.bench && lot.status !== "PASS",
  );
  const generateDisabled =
    busy || !setupDone || !qc || qc.blocking || setupChanged;
  const generateHint = !setupDone
    ? "workplan.batch.generate.needsReagent"
    : qc?.blocking
      ? "workplan.batch.generate.blocked"
      : setupChanged
        ? "workplan.batch.generate.saveFirst"
        : null;
  const currentStep = !setupDone ? 1 : active ? 3 : 2;
  const subjects = qc ? qcSubjects(qc) : null;
  const qcValues = subjects && {
    test: subjects.test,
    instrument:
      subjects.instrument ||
      intl.formatMessage({ id: "workplan.batch.instrument.bench" }),
    lastPass: subjects.lot?.lastPassingAt
      ? formatDateTime(intl, subjects.lot.lastPassingAt)
      : intl.formatMessage({ id: "workplan.batch.qc.never" }),
    rules: (subjects.lot?.ruleCodes || []).join(", ") || "-",
  };

  return (
    <div
      className={`batch-workplan__tile${
        qc && !["PASS", "NOT_RUN", "ESTABLISHMENT"].includes(qc.status)
          ? " batch-workplan__tile--alert"
          : ""
      }`}
      data-testid={`batch-tile-${batch.id}`}
    >
      <div className="batch-workplan__tile-header">
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={expanded ? ChevronUp : ChevronDown}
          iconDescription={intl.formatMessage({
            id: expanded ? "workplan.batch.collapse" : "workplan.batch.expand",
          })}
          tooltipPosition="right"
          onClick={() => onToggle(batch.id)}
          aria-expanded={expanded}
        />
        <div className="batch-workplan__tile-title">
          <strong>{batch.name}</strong>
          <span className="batch-workplan__muted">
            {batch.reagentName ||
              intl.formatMessage({ id: "workplan.batch.reagent.notAssigned" })}
            {batch.reagentLotNumber ? ` · ${batch.reagentLotNumber}` : ""}
            {" · "}
            <FormattedMessage
              id="workplan.batch.testCount"
              values={{ count: batch.itemCount }}
            />
            {" · "}
            {formatDate(intl, batch.createdAt)}
          </span>
        </div>
        <div className="batch-workplan__tile-tags">
          <Tag type={statusTagType(batch.status)} size="sm">
            <FormattedMessage id={`workplan.batch.status.${batch.status}`} />
          </Tag>
          {!setupDone && draft && (
            <Tag type="cool-gray" size="sm">
              <FormattedMessage id="workplan.batch.needsSetup" />
            </Tag>
          )}
          {qc && (
            <Tag
              type={qcTagType(qc.status)}
              size="sm"
              data-testid={`batch-${batch.id}-qc-tag`}
            >
              <FormattedMessage id={`workplan.batch.qc.status.${qc.status}`} />
            </Tag>
          )}
          {latestNce && (
            <Link
              href={`/ViewNonConformingEvent?nceNumber=${encodeURIComponent(latestNce.nceNumber)}`}
              className="batch-workplan__nce-link"
            >
              <Tag type="red" size="sm">
                <FormattedMessage
                  id="workplan.batch.nceTag"
                  values={{ nce: latestNce.nceNumber }}
                />
              </Tag>
            </Link>
          )}
          <OverflowMenu
            size="sm"
            flipped
            aria-label={intl.formatMessage({ id: "workplan.batch.actions" })}
            iconDescription={intl.formatMessage({
              id: "workplan.batch.actions",
            })}
          >
            {active && (
              <OverflowMenuItem
                itemText={intl.formatMessage({
                  id: "workplan.batch.action.edit",
                })}
                onClick={() =>
                  transition("DRAFT", "workplan.batch.edit.success")
                }
              />
            )}
            {active && (
              <OverflowMenuItem
                itemText={intl.formatMessage({
                  id: "workplan.batch.action.complete",
                })}
                onClick={() =>
                  transition("COMPLETED", "workplan.batch.status.success")
                }
              />
            )}
            <OverflowMenuItem
              isDelete
              itemText={intl.formatMessage({
                id: "workplan.batch.action.archive",
              })}
              onClick={() => transition("ARCHIVED", "workplan.batch.archived")}
            />
          </OverflowMenu>
        </div>
      </div>

      {qc && qc.status !== "PASS" && (
        <InlineNotification
          kind={qcNotificationKind(qc.status)}
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: `workplan.batch.qc.status.${qc.status}`,
          })}
          subtitle={intl.formatMessage(
            { id: `workplan.batch.qc.message.${qc.status}` },
            qcValues,
          )}
        />
      )}
      {qc && qc.status === "VIOLATION" && (
        <div className="batch-workplan__notification-link">
          <Link href="/qa/qc/alerts">
            <FormattedMessage id="workplan.batch.qc.viewViolation" />
          </Link>
        </div>
      )}

      {expanded && (
        <div className="batch-workplan__tile-body">
          <ProgressIndicator currentIndex={currentStep - 1} spaceEqually>
            <ProgressStep
              label={intl.formatMessage({ id: "workplan.batch.step.tests" })}
              complete
            />
            <ProgressStep
              label={intl.formatMessage({ id: "workplan.batch.step.reagent" })}
              complete={setupDone && qc?.status === "PASS"}
            />
            <ProgressStep
              label={intl.formatMessage({ id: "workplan.batch.step.generate" })}
              complete={active}
            />
          </ProgressIndicator>

          <h5 className="batch-workplan__section-title">
            <FormattedMessage
              id="workplan.batch.testsInBatch"
              values={{ count: batch.itemCount }}
            />
          </h5>
          <Table size="sm" useZebraStyles>
            <TableHead>
              <TableRow>
                <TableHeader>
                  <FormattedMessage id="workplan.batch.column.labNumber" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="workplan.batch.column.patient" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="workplan.batch.column.test" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="workplan.batch.column.sampleType" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="workplan.batch.column.priority" />
                </TableHeader>
                {draft && <TableHeader />}
              </TableRow>
            </TableHead>
            <TableBody>
              {(batch.items || []).map((item) => (
                <TableRow key={item.analysisId}>
                  <TableCell>
                    {convertAlphaNumLabNumForDisplay(item.accessionNumber)}
                  </TableCell>
                  <TableCell>{item.patientName}</TableCell>
                  <TableCell>{item.testName}</TableCell>
                  <TableCell>{item.sampleType}</TableCell>
                  <TableCell>
                    {item.priority && (
                      <Tag type={priorityTagType(item.priority)} size="sm">
                        <FormattedMessage
                          id={`sample.priority.${item.priority}`}
                        />
                      </Tag>
                    )}
                  </TableCell>
                  {draft && (
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        disabled={busy || batch.items.length <= 1}
                        iconDescription={intl.formatMessage(
                          { id: "workplan.batch.removeTest" },
                          { labNumber: item.accessionNumber },
                        )}
                        onClick={() =>
                          updateTests(
                            batch.items
                              .map((other) => other.analysisId)
                              .filter((id) => id !== item.analysisId),
                          )
                        }
                      />
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="batch-workplan__phase">
            <h5 className="batch-workplan__section-title">
              <FormattedMessage id="workplan.batch.step.reagent" />
            </h5>
            {draft && options === null && (
              <InlineLoading
                description={intl.formatMessage({ id: "loading.description" })}
              />
            )}
            {draft && options !== null && reagentOptions.length === 0 && (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "workplan.batch.reagent.noneLinked.title",
                })}
                subtitle={intl.formatMessage({
                  id: "workplan.batch.reagent.noneLinked",
                })}
              />
            )}
            {draft && reagentOptions.length > 0 && (
              <>
                <div className="batch-workplan__qc-entry-row">
                  <Select
                    id={`batch-${batch.id}-reagent`}
                    size="sm"
                    labelText={intl.formatMessage({
                      id: "workplan.batch.reagent",
                    })}
                    value={reagentId}
                    onChange={(event) => {
                      setReagentId(event.target.value);
                      setLotId("");
                    }}
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "workplan.batch.reagent.select",
                      })}
                    />
                    {reagentOptions.map((reagent) => (
                      <SelectItem
                        key={reagent.id}
                        value={String(reagent.id)}
                        text={`${reagent.name} (${reagent.testNames.join(", ")})`}
                      />
                    ))}
                  </Select>
                  <Select
                    id={`batch-${batch.id}-instrument`}
                    size="sm"
                    labelText={intl.formatMessage({
                      id: "workplan.batch.instrument",
                    })}
                    value={instrumentId}
                    onChange={(event) => setInstrumentId(event.target.value)}
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "workplan.batch.instrument.bench",
                      })}
                    />
                    {(options?.instruments || []).map((instrument) => (
                      <SelectItem
                        key={instrument.id}
                        value={String(instrument.id)}
                        text={instrument.name}
                      />
                    ))}
                  </Select>
                </div>
                {reagentId && lots.length === 0 && (
                  <p className="batch-workplan__muted">
                    <FormattedMessage id="workplan.batch.reagent.noLots" />
                  </p>
                )}
                {lots.length > 0 && (
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "workplan.batch.reagentLot",
                    })}
                    name={`batch-${batch.id}-lot`}
                    orientation="vertical"
                    valueSelected={String(lotId)}
                    onChange={(value) => setLotId(value)}
                  >
                    {lots.map((lot) => (
                      <RadioButton
                        key={lot.id}
                        id={`batch-${batch.id}-lot-${lot.id}`}
                        value={String(lot.id)}
                        labelText={
                          <span className="batch-workplan__lot">
                            <span className="batch-workplan__mono">
                              {lot.lotNumber}
                            </span>
                            {lot.suggested && (
                              <Tag type="teal" size="sm">
                                <FormattedMessage id="label.results.reagents.fefo" />
                              </Tag>
                            )}
                            <span className="batch-workplan__muted">
                              <FormattedMessage
                                id="workplan.batch.reagentLot.detail"
                                values={{
                                  expires: lot.expirationDate
                                    ? formatDate(intl, lot.expirationDate)
                                    : "-",
                                  remaining: lot.currentQuantity ?? "-",
                                  units: lot.units || "",
                                }}
                              />
                            </span>
                          </span>
                        }
                      />
                    ))}
                  </RadioButtonGroup>
                )}
                {setupChanged && reagentId && lotId && (
                  <Button
                    size="sm"
                    renderIcon={Save}
                    onClick={saveReagent}
                    disabled={busy}
                  >
                    <FormattedMessage id="workplan.batch.reagent.save" />
                  </Button>
                )}
              </>
            )}
            {!draft && (
              <p>
                <FormattedMessage
                  id="workplan.batch.reagent.summary"
                  values={{
                    reagent: batch.reagentName || "-",
                    lot: batch.reagentLotNumber || "-",
                    instrument:
                      batch.instrumentName ||
                      intl.formatMessage({
                        id: "workplan.batch.instrument.bench",
                      }),
                  }}
                />
              </p>
            )}
          </div>

          {qc && (
            <div className="batch-workplan__phase">
              <h5 className="batch-workplan__section-title">
                <FormattedMessage id="workplan.batch.qc.lastRun" />
              </h5>
              {qc.status !== "NO_CONTROL_LOT" &&
                (qc.testsWithoutControlLot || []).length > 0 && (
                  <InlineNotification
                    kind="warning"
                    lowContrast
                    hideCloseButton
                    title={intl.formatMessage({
                      id: "workplan.batch.qc.status.NO_CONTROL_LOT",
                    })}
                    subtitle={intl.formatMessage(
                      { id: "workplan.batch.qc.noControlLot" },
                      {
                        test: qc.testsWithoutControlLot.join(", "),
                        instrument:
                          qc.instrumentName ||
                          intl.formatMessage({
                            id: "workplan.batch.instrument.bench",
                          }),
                      },
                    )}
                  />
                )}
              {(qc.lots || []).length > 0 && (
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.test" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.controlLot" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.statusColumn" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.lastDate" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.performedBy" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.value" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.zScore" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.result" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.qc.nextDue" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {qc.lots.map((lot) => (
                      <TableRow key={lot.controlLotId}>
                        <TableCell>{lot.testName}</TableCell>
                        <TableCell>
                          {`${lot.lotNumber} (${lot.controlLevel})`}
                          <div className="batch-workplan__muted">
                            <FormattedMessage
                              id={`workplan.batch.qc.frequency.${lot.frequencyType}`}
                              values={{
                                hours:
                                  lot.frequencyType === "PER_SHIFT"
                                    ? lot.shiftDefinitionHours || 8
                                    : lot.frequencyHours || 24,
                              }}
                            />
                          </div>
                        </TableCell>
                        <TableCell>
                          <Tag type={qcTagType(lot.status)} size="sm">
                            <FormattedMessage
                              id={`workplan.batch.qc.status.${lot.status}`}
                            />
                          </Tag>
                        </TableCell>
                        <TableCell>
                          {lot.lastRun
                            ? formatDateTime(intl, lot.lastRun.runDateTime)
                            : intl.formatMessage({
                                id: "workplan.batch.qc.never",
                              })}
                        </TableCell>
                        <TableCell>{lot.lastRun?.performedBy || ""}</TableCell>
                        <TableCell>{lot.lastRun?.value ?? ""}</TableCell>
                        <TableCell>{lot.lastRun?.zScore ?? ""}</TableCell>
                        <TableCell>
                          {lot.lastRun?.resultStatus && (
                            <FormattedMessage
                              id={`workplan.batch.qc.resultStatus.${lot.lastRun.resultStatus}`}
                            />
                          )}
                          {(lot.lastRun?.ruleCodes || []).length > 0 &&
                            ` (${lot.lastRun.ruleCodes.join(", ")})`}
                        </TableCell>
                        <TableCell>
                          {lot.nextDueAt
                            ? formatDateTime(intl, lot.nextDueAt)
                            : ""}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
              {(qc.lots || []).length > 0 && (
                <Accordion size="sm">
                  <AccordionItem
                    title={intl.formatMessage({
                      id: "workplan.batch.qc.history",
                    })}
                  >
                    {qc.lots.map((lot) => (
                      <div key={lot.controlLotId}>
                        <strong>{`${lot.testName} - ${lot.lotNumber}`}</strong>
                        {(lot.recentRuns || []).length === 0 && (
                          <p className="batch-workplan__muted">
                            <FormattedMessage id="workplan.batch.qc.noHistory" />
                          </p>
                        )}
                        <ul className="batch-workplan__history">
                          {(lot.recentRuns || []).map((run) => (
                            <li key={run.id}>
                              {formatDateTime(intl, run.runDateTime)}
                              {" · "}
                              {run.performedBy || ""}
                              {" · "}
                              {run.value ?? "-"}
                              {run.zScore !== null && run.zScore !== undefined
                                ? ` (z ${run.zScore})`
                                : ""}
                              {" · "}
                              <FormattedMessage
                                id={`workplan.batch.qc.resultStatus.${run.resultStatus}`}
                              />
                              {(run.ruleCodes || []).length > 0 &&
                                ` (${run.ruleCodes.join(", ")})`}
                            </li>
                          ))}
                        </ul>
                        <Link
                          href={
                            lot.bench
                              ? "/qa/qc/dashboard"
                              : `/analyzers/qc/charts/${lot.instrumentId}`
                          }
                        >
                          <FormattedMessage id="workplan.batch.qc.viewChart" />
                        </Link>
                      </div>
                    ))}
                  </AccordionItem>
                </Accordion>
              )}
              {evaluating && (
                <InlineLoading
                  description={intl.formatMessage({
                    id: "workplan.batch.qc.evaluating",
                  })}
                />
              )}
              {evaluationTimedOut && (
                <InlineNotification
                  kind="info"
                  lowContrast
                  title={intl.formatMessage({
                    id: "workplan.batch.qc.evaluationPending",
                  })}
                  onCloseButtonClick={() => setEvaluationTimedOut(false)}
                />
              )}
              {draft && analyzerLotsNeedingQc.length > 0 && (
                <p className="batch-workplan__muted">
                  <FormattedMessage
                    id="workplan.batch.qc.analyzerFed"
                    values={{
                      instrument:
                        qc.instrumentName ||
                        intl.formatMessage({
                          id: "workplan.batch.instrument.bench",
                        }),
                    }}
                  />{" "}
                  <Link href={`/analyzers/qc/instruments/${qc.instrumentId}`}>
                    <FormattedMessage id="workplan.batch.qc.viewInstrument" />
                  </Link>
                </p>
              )}
              {draft &&
                recordableLots.length > 0 &&
                !entryOpen &&
                !evaluating && (
                  <Button
                    kind="tertiary"
                    size="sm"
                    renderIcon={Renew}
                    onClick={() => setEntryOpen(true)}
                  >
                    <FormattedMessage id="workplan.batch.qc.runQc" />
                  </Button>
                )}
              {entryOpen && (
                <QcEntryForm
                  batchId={batch.id}
                  lots={recordableLots}
                  onSaved={qcSaved}
                  onCancel={() => setEntryOpen(false)}
                />
              )}
            </div>
          )}

          <div className="batch-workplan__generate">
            {draft && generateHint && (
              <span className="batch-workplan__muted">
                <FormattedMessage id={generateHint} />
              </span>
            )}
            {draft && (
              <Button
                size="sm"
                renderIcon={Printer}
                disabled={generateDisabled}
                onClick={() =>
                  qc && qc.status !== "PASS" ? setOverride(qc) : generate(false)
                }
              >
                <FormattedMessage id="workplan.batch.generate" />
              </Button>
            )}
            {(active || batch.status === "COMPLETED") && (
              <Button
                size="sm"
                kind="secondary"
                renderIcon={Printer}
                href={`/Workplan/batch/${batch.id}/print`}
                target="_blank"
                rel="noopener noreferrer"
              >
                <FormattedMessage id="workplan.batch.print" />
              </Button>
            )}
          </div>
        </div>
      )}

      <QcOverrideModal
        open={Boolean(override)}
        batch={batch}
        qc={override}
        canOverride={canOverride}
        busy={busy}
        onProceed={() => generate(true)}
        onClose={() => setOverride(null)}
      />
    </div>
  );
}
