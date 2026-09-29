import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Modal,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import {
  formatDateOnly,
  hasQaPermission,
  resolveApiErrorMessage,
} from "../../../utils/Utils";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { hintStyle } from "../../eqaCommon";
import {
  distributeScores,
  fetchIntake,
  fetchReceiptRows,
  closeCycle,
  fetchScoreRows,
  importIntakeCsv,
  markDelivered,
  openSubmissions,
  saveIntake,
  scoreCycle,
  scoresCsvUrl,
  participantReportUrl,
  sendRepeat,
} from "./workbenchApi";

const RECEIPT_STATUS_TAG = {
  NOT_SHIPPED: "gray",
  IN_TRANSIT: "teal",
  OVERDUE: "red",
  DELIVERED: "green",
  EXCEPTION: "magenta",
};

/**
 * Two of these wordings already exist elsewhere in the catalogue, so the tag
 * reads off a key map rather than minting duplicates (constitution VII).
 */
const RECEIPT_STATUS_KEY = {
  NOT_SHIPPED: "eqa.receipt.status.NOT_SHIPPED",
  IN_TRANSIT: "eqa.receipt.status.IN_TRANSIT",
  OVERDUE: "eqa.status.overdue",
  DELIVERED: "eqa.cycle.status.delivered",
  EXCEPTION: "eqa.receipt.status.EXCEPTION",
};

/** Scoring is offered exactly where the provider machine allows it. */
const SCORABLE = ["SUBMISSIONS_OPEN", "SUBMISSIONS_CLOSED"];

const dateCell = (value) =>
  value ? formatDateOnly(value.substring(0, 10)) : "—";

/**
 * Receipt monitor (15) plus the scoring and score-return actions
 * (04). Delivery, overdue and repeat all read off the shipment the
 * participant's box carries, which is also what the participant's own receipt
 * marks delivered — so a receipt recorded by the lab shows up here on
 * the next load without a second source of truth.
 */
const ReceiptMonitor = ({ cycleId, cycleStatus, onChanged, onNotice }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  // Gate each action on the grant its own endpoint asks for, so what is on
  // offer is what this user can actually do. Provider covers reception,
  // repeats, provider-side result entry and score return; manage covers
  // scoring the cycle and the audited override that opens submissions.
  const isProvider = hasQaPermission(userSessionDetails, "qa.eqa.provider");
  const canManage = hasQaPermission(userSessionDetails, "qa.manage.eqa");
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [rows, setRows] = useState([]);
  const [scores, setScores] = useState([]);
  const [repeating, setRepeating] = useState(null);
  const [overrideNote, setOverrideNote] = useState("");
  const [busy, setBusy] = useState(null);
  const [openingSubmissions, setOpeningSubmissions] = useState(false);
  const [closing, setClosing] = useState(false);
  const [closeReason, setCloseReason] = useState("");
  const [openReason, setOpenReason] = useState("");

  // The cycle status is a dependency, not decoration: dispatching from the
  // sibling Shipments tab moves the cycle, and without it this tab kept
  // reporting "Not shipped" until the page was reloaded.
  const load = useCallback(() => {
    fetchReceiptRows(cycleId, setRows);
    fetchScoreRows(cycleId, setScores);
  }, [cycleId, cycleStatus]);

  useEffect(load, [load]);

  const refresh = () => {
    load();
    onChanged();
  };

  /**
   * A 200 is not the same as a success here: the FHIR return answers 200 with
   * {success: false, error} when the store refuses the bundle, and reporting
   * that as sent repeats an earlier defect: a success toast over a failed save.
   */
  const report = (
    { ok, status, body },
    successKey,
    successText,
    failKey,
    grant,
    successValues,
  ) => {
    setBusy(null);
    if (ok && body?.success !== false) {
      refresh();
      onNotice({
        kind: "success",
        text: t(successKey, successText, successValues),
      });
      return;
    }
    // A refusal names the action and the grant that carries it. The actions are
    // hidden without the grant, so this is reachable only when one is revoked
    // mid-session — which is exactly when "Forbidden" explains least.
    if (status === 403) {
      onNotice({
        kind: "error",
        text: t(
          "eqa.action.forbidden",
          "This action needs the {grant} permission, which your account does not have. Ask an administrator for it.",
          { grant },
        ),
      });
      return;
    }
    onNotice({
      kind: "error",
      text: resolveApiErrorMessage(intl, body, failKey),
    });
  };

  const handleDelivered = (row) => {
    setBusy(row.organizationId);
    markDelivered(cycleId, row.organizationId, (response) =>
      report(
        response,
        "eqa.receipt.delivered",
        "Delivery recorded.",
        "eqa.receipt.deliveredFailed",
        "qa.eqa.provider",
      ),
    );
  };

  const handleRepeat = () => {
    setBusy(repeating.organizationId);
    sendRepeat(cycleId, repeating.organizationId, overrideNote, (response) => {
      setRepeating(null);
      setOverrideNote("");
      report(
        response,
        "eqa.receipt.repeatSent",
        "Repeat panel dispatched.",
        "eqa.receipt.repeatFailed",
        "qa.eqa.provider",
      );
    });
  };

  /**
   * Open submissions on a partial roster. Auto-advance only fires when
   * every participant is delivered; one dormant lab must not keep submissions
   * closed for the labs that hold their panels. Audited MANUAL override —
   * reason required by the transition endpoint itself.
   */
  const handleOpenSubmissions = () => {
    setBusy("open-submissions");
    openSubmissions(cycleId, openReason, (response) => {
      setOpeningSubmissions(false);
      setOpenReason("");
      report(
        response,
        "eqa.receipt.submissionsOpened",
        "Submissions are open.",
        "eqa.receipt.submissionsOpenFailed",
        "qa.manage.eqa",
      );
    });
  };

  const handleCloseCycle = () => {
    setBusy("close");
    closeCycle(cycleId, closeReason, (response) => {
      setClosing(false);
      setCloseReason("");
      report(
        response,
        "eqa.cycle.closedCopy",
        "Cycle closed.",
        "eqa.cycle.closeFailed",
        "qa.manage.eqa",
      );
    });
  };

  // The provider keys what a participant phoned or emailed, or
  // pastes the participant's export bundle. Values are kept as reported —
  // numbers or words such as "Reactive" — and saving overwrites what is on file.
  const [intake, setIntake] = useState(null);

  const openIntake = (row) => {
    setBusy(row.organizationId);
    fetchIntake(cycleId, row.organizationId, (grid) => {
      setBusy(null);
      const tests = grid?.tests || [];
      setIntake({
        row,
        tests,
        values: Object.fromEntries(
          tests.map((test) => [
            test.testId,
            test.reported === null || test.reported === undefined
              ? ""
              : String(test.reported),
          ]),
        ),
        csv: "",
        error: null,
        imported: null,
      });
    });
  };

  const reportedRows = () =>
    intake.tests
      .map((test) => ({
        testId: test.testId,
        value: intake.values[test.testId] ?? "",
      }))
      .filter((entry) => String(entry.value).trim() !== "");

  const handleSaveIntake = () => {
    setBusy("intake");
    saveIntake(
      cycleId,
      intake.row.organizationId,
      reportedRows(),
      ({ ok, body }) => {
        setBusy(null);
        if (!ok) {
          setIntake({
            ...intake,
            error:
              body?.error ||
              t("eqa.intake.failed", "Could not record the results"),
          });
          return;
        }
        setIntake(null);
        refresh();
        onNotice({
          kind: "success",
          text: t("eqa.intake.saved", "Results recorded for {lab}.", {
            lab: intake.row.organizationName,
          }),
        });
      },
    );
  };

  const handleImportCsv = () => {
    setBusy("intake");
    importIntakeCsv(
      cycleId,
      intake.row.organizationId,
      intake.csv,
      ({ ok, body }) => {
        setBusy(null);
        if (!ok) {
          setIntake({
            ...intake,
            error:
              body?.error ||
              t("eqa.intake.failed", "Could not record the results"),
          });
          return;
        }
        const tests = body?.tests || intake.tests;
        setIntake({
          ...intake,
          tests,
          values: Object.fromEntries(
            tests.map((test) => [
              test.testId,
              test.reported === null || test.reported === undefined
                ? ""
                : String(test.reported),
            ]),
          ),
          csv: "",
          error: (body?.errors || []).length ? body.errors.join(" ") : null,
          imported: body?.imported ?? 0,
        });
        refresh();
      },
    );
  };

  const handleScore = () => {
    setBusy("score");
    scoreCycle(cycleId, (response) => {
      // A result whose analyte carries no sealed target and whose test has too few
      // peers to place it against reaches SCORED with no verdict on it. Saying so
      // is the difference between a gap and a silent null.
      const unjudged = response?.body?.unjudgedCount ?? 0;
      report(
        response,
        unjudged ? "eqa.score.scoredWithUnjudged" : "eqa.score.scored",
        unjudged
          ? "Cycle scored, but {count} result(s) got no verdict, on {tests}. Those analytes carry no sealed target and the cycle has too few peers to place them against."
          : "Cycle scored. Unacceptable participants are in the follow-up register.",
        "eqa.score.scoreFailed",
        "qa.manage.eqa",
        unjudged
          ? {
              count: unjudged,
              tests: (response?.body?.unjudgedTests ?? []).join(", "),
            }
          : undefined,
      );
    });
  };

  const handleDistribute = (row) => {
    setBusy(row.organizationId);
    distributeScores(cycleId, row.organizationId, (response) =>
      report(
        response,
        "eqa.score.distributed",
        "Scores placed in the FHIR store for the participant to collect.",
        "eqa.score.distributeFailed",
        "qa.eqa.provider",
      ),
    );
  };

  const scoreOf = (organizationId) =>
    scores.find((score) => score.organizationId === organizationId);

  return (
    <>
      {rows.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t("eqa.shipment.noParticipants.title", "No participants")}
          subtitle={t(
            "eqa.receipt.noParticipants.body",
            "Receipts appear here once panels have been dispatched to enrolled participant laboratories.",
          )}
        />
      ) : (
        <>
          {!isProvider && !canManage && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={t(
                "eqa.receipt.readOnly.title",
                "Read-only view of receipts and scores",
              )}
              subtitle={t(
                "eqa.receipt.readOnly.body",
                "Recording a delivery, dispatching a repeat, entering results and returning scores all need the provider grant; scoring the cycle needs the manage grant. The table below is the whole cycle either way.",
              )}
              style={{ marginBottom: "0.75rem" }}
            />
          )}
          <div
            style={{
              display: "flex",
              gap: "0.5rem",
              alignItems: "center",
              marginBottom: "0.75rem",
            }}
          >
            {canManage && SCORABLE.includes(cycleStatus) && (
              <Button size="sm" disabled={busy !== null} onClick={handleScore}>
                {t("eqa.score.scoreCycle", "Score cycle")}
              </Button>
            )}
            {canManage && cycleStatus === "SCORED" && (
              <Button
                size="sm"
                kind="tertiary"
                disabled={busy !== null}
                onClick={() => {
                  setClosing(true);
                  setCloseReason("");
                }}
              >
                {t("eqa.cycle.close", "Close cycle")}
              </Button>
            )}
            {canManage &&
              ["SHIPPED", "DELIVERED"].includes(cycleStatus) &&
              rows.some(
                (row) =>
                  row.receiptStatus === "DELIVERED" ||
                  row.receiptStatus === "EXCEPTION",
              ) && (
                <Button
                  size="sm"
                  kind="tertiary"
                  disabled={busy !== null}
                  onClick={() => {
                    setOpeningSubmissions(true);
                    setOpenReason("");
                  }}
                >
                  {t("eqa.receipt.openSubmissions", "Open submissions")}
                </Button>
              )}
            <Button
              kind="ghost"
              size="sm"
              as={RouterLink}
              to="/qa/eqa/provider/follow-ups"
            >
              {t("eqa.provider.followups.open", "Follow-up register")}
            </Button>
            <span style={hintStyle}>
              {t(
                "eqa.receipt.hint",
                "Submissions open on their own once every participant holds its panel. Overdue means two business days past the expected delivery.",
              )}
            </span>
          </div>
          <Table size="sm">
            <TableHead>
              <TableRow>
                <TableHeader>
                  {t("eqa.shipment.participant", "Participant")}
                </TableHeader>
                <TableHeader>{t("label.status", "Status")}</TableHeader>
                <TableHeader>
                  {t("eqa.shipment.expected", "Expected delivery")}
                </TableHeader>
                <TableHeader>
                  {t("shipment.state.received", "Received")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.receipt.condition", "Condition")}
                </TableHeader>
                <TableHeader>{t("eqa.score.verdicts", "Scores")}</TableHeader>
                <TableHeader>{t("column.actions", "Actions")}</TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => {
                const score = scoreOf(row.organizationId);
                const delivered =
                  row.receiptStatus === "DELIVERED" ||
                  row.receiptStatus === "EXCEPTION";
                return (
                  <TableRow key={row.organizationId}>
                    <TableCell>
                      {row.organizationName}
                      {row.repeatOfShipmentId && (
                        <div style={hintStyle}>
                          {t("eqa.receipt.isRepeat", "Repeat shipment")}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <Tag
                        type={RECEIPT_STATUS_TAG[row.receiptStatus] || "gray"}
                        size="sm"
                      >
                        {t(
                          RECEIPT_STATUS_KEY[row.receiptStatus] ||
                            `eqa.receipt.status.${row.receiptStatus}`,
                          row.receiptStatus.replace(/_/g, " "),
                        )}
                      </Tag>
                    </TableCell>
                    <TableCell>{dateCell(row.estimatedDeliveryDate)}</TableCell>
                    <TableCell>{dateCell(row.receivedDate)}</TableCell>
                    <TableCell>
                      {row.integrityOk === false
                        ? t("eqa.receipt.damaged", "Damaged: {notes}", {
                            notes: row.integrityNotes || "",
                          })
                        : row.receivedTempC !== null &&
                            row.receivedTempC !== undefined
                          ? t("eqa.receipt.tempOnArrival", "{temp} °C", {
                              temp: row.receivedTempC,
                            })
                          : "—"}
                    </TableCell>
                    <TableCell>
                      {score
                        ? t(
                            "eqa.score.counts",
                            "{unacceptable} unacceptable of {total}",
                            {
                              unacceptable: score.unacceptableCount,
                              total: score.resultCount,
                            },
                          )
                        : "—"}
                    </TableCell>
                    <TableCell>
                      {isProvider && !delivered && row.shipmentId && (
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={busy === row.organizationId}
                          onClick={() => handleDelivered(row)}
                        >
                          {t("eqa.receipt.markReceived", "Mark received")}
                        </Button>
                      )}
                      {isProvider && row.shipmentId && (
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={busy === row.organizationId}
                          onClick={() => {
                            setRepeating(row);
                            setOverrideNote("");
                          }}
                        >
                          {t("eqa.receipt.sendRepeat", "Send repeat")}
                        </Button>
                      )}
                      {isProvider && (
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={busy !== null}
                          onClick={() => openIntake(row)}
                        >
                          {t("eqa.intake.enterResults", "Enter results")}
                        </Button>
                      )}
                      {score && score.resultCount > 0 && (
                        <>
                          {isProvider && (
                            <Button
                              kind="ghost"
                              size="sm"
                              disabled={busy === row.organizationId}
                              onClick={() => handleDistribute(row)}
                            >
                              {t("eqa.score.sendScores", "Send scores")}
                            </Button>
                          )}
                          <Button
                            kind="ghost"
                            size="sm"
                            href={scoresCsvUrl(cycleId, row.organizationId)}
                          >
                            {t("eqa.score.downloadCsv", "Scores CSV")}
                          </Button>
                          <Button
                            kind="ghost"
                            size="sm"
                            href={participantReportUrl(
                              cycleId,
                              row.organizationId,
                            )}
                            target="_blank"
                          >
                            {t("eqa.score.downloadReport", "Report PDF")}
                          </Button>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </>
      )}

      {intake && (
        <Modal
          open
          size="md"
          modalHeading={t("eqa.intake.heading", "Results from {lab}", {
            lab: intake.row.organizationName,
          })}
          primaryButtonText={t("eqa.intake.save", "Save results")}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy !== null || reportedRows().length === 0}
          onRequestClose={() => setIntake(null)}
          onSecondarySubmit={() => setIntake(null)}
          onRequestSubmit={handleSaveIntake}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {t(
              "eqa.intake.help",
              "Key the values as the participant reported them — numbers, or words such as Reactive. Saving overwrites what is on file for that test.",
            )}
          </p>
          {intake.error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intake.error}
              style={{ marginBottom: "1rem" }}
            />
          )}
          {intake.imported !== null && (
            <InlineNotification
              kind="success"
              lowContrast
              hideCloseButton
              title={t("eqa.intake.imported", "{count} values imported.", {
                count: intake.imported,
              })}
              style={{ marginBottom: "1rem" }}
            />
          )}
          {intake.tests.length === 0 ? (
            <p style={hintStyle}>
              {t(
                "eqa.intake.noTests",
                "This scheme has no tests assigned yet.",
              )}
            </p>
          ) : (
            <Table size="sm" data-testid="intake-grid">
              <TableHead>
                <TableRow>
                  <TableHeader>{t("eqa.intake.test", "Test")}</TableHeader>
                  <TableHeader>
                    {t("eqa.intake.value", "Reported value")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {intake.tests.map((test) => (
                  <TableRow key={test.testId}>
                    <TableCell>
                      {test.testName}
                      {test.analyteName && (
                        <div style={hintStyle}>{test.analyteName}</div>
                      )}
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`intake-${test.testId}`}
                        labelText={test.testName}
                        hideLabel
                        size="sm"
                        value={intake.values[test.testId] ?? ""}
                        onChange={(event) =>
                          setIntake({
                            ...intake,
                            values: {
                              ...intake.values,
                              [test.testId]: event.target.value,
                            },
                          })
                        }
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          <TextArea
            id="intake-csv"
            labelText={t(
              "eqa.intake.csv",
              "Or paste the participant's export bundle (CSV)",
            )}
            value={intake.csv}
            onChange={(event) =>
              setIntake({ ...intake, csv: event.target.value })
            }
            rows={3}
            style={{ marginTop: "1rem" }}
          />
          <Button
            kind="tertiary"
            size="sm"
            disabled={busy !== null || !intake.csv.trim()}
            onClick={handleImportCsv}
            style={{ marginTop: "0.5rem" }}
          >
            {t("eqa.intake.import", "Import CSV")}
          </Button>
        </Modal>
      )}

      {closing && (
        <Modal
          open
          modalHeading={t("eqa.cycle.closeHeading", "Close this cycle")}
          primaryButtonText={t("eqa.cycle.close", "Close cycle")}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy !== null || !closeReason.trim()}
          onRequestClose={() => setClosing(false)}
          onSecondarySubmit={() => setClosing(false)}
          onRequestSubmit={handleCloseCycle}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {t(
              "eqa.cycle.closeHelp",
              "A closed cycle is final: participants see it as closed and no further verdicts are recorded against it. Closing is refused while a follow-up is open or a missed-deadline result is still waiting on an answer.",
            )}
          </p>
          <TextArea
            id="eqa-close-cycle-reason"
            labelText={t("eqa.receipt.openSubmissionsReason", "Reason")}
            value={closeReason}
            onChange={(event) => setCloseReason(event.target.value)}
            rows={3}
          />
        </Modal>
      )}

      {openingSubmissions && (
        <Modal
          open
          modalHeading={t(
            "eqa.receipt.openSubmissionsHeading",
            "Open submissions with undelivered panels",
          )}
          primaryButtonText={t(
            "eqa.receipt.openSubmissions",
            "Open submissions",
          )}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy !== null || !openReason.trim()}
          onRequestClose={() => setOpeningSubmissions(false)}
          onSecondarySubmit={() => setOpeningSubmissions(false)}
          onRequestSubmit={handleOpenSubmissions}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {t(
              "eqa.receipt.openSubmissionsHelp",
              "Participants that have not received their panel stay on the roster as late. This is a recorded manual override — give the reason.",
            )}
          </p>
          <TextArea
            id="eqa-open-submissions-reason"
            labelText={t("eqa.receipt.openSubmissionsReason", "Reason")}
            value={openReason}
            onChange={(event) => setOpenReason(event.target.value)}
            rows={3}
          />
        </Modal>
      )}

      {repeating && (
        <Modal
          open
          modalHeading={t("eqa.receipt.repeatHeading", "Send a repeat panel")}
          primaryButtonText={t("eqa.receipt.sendRepeat", "Send repeat")}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy !== null}
          onRequestClose={() => setRepeating(null)}
          onSecondarySubmit={() => setRepeating(null)}
          onRequestSubmit={handleRepeat}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {t(
              "eqa.receipt.repeatHelp",
              "The repeat comes out of the panel's reserve. If the reserve cannot cover it, a written justification is required before unreserved material is used.",
            )}
          </p>
          <TextArea
            id="eqa-repeat-override-note"
            labelText={t("eqa.receipt.overrideNote", "Override note")}
            value={overrideNote}
            onChange={(event) => setOverrideNote(event.target.value)}
            rows={3}
          />
        </Modal>
      )}
    </>
  );
};

export default ReceiptMonitor;
