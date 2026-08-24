import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Modal,
  Section,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
  Tag,
  Tile,
} from "@carbon/react";
import { ChevronDown, ChevronUp } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { formatDateOnly, hasQaPermission } from "../../utils/Utils";
import { csvCell, downloadCsv, hintStyle } from "../eqaCommon";
import {
  fetchProviderRegister,
  notifyParticipant,
  requestRepeatPanel,
  triageFollowUp,
} from "./followUpApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  {
    label: "eqa.provider.followups.title",
    link: "/qa/eqa/provider/follow-ups",
  },
];

/** Two of these states already have wording in the catalogue (Principle VII). */
const STATUS_KEY = {
  RESOLVED: "alerts.status.resolved",
};

const STATUS_TAG = {
  NOTIFIED: "blue",
  RESPONSE_RECEIVED: "cyan",
  UNDER_INVESTIGATION: "purple",
  ESCALATED: "red",
  RESOLVED: "green",
  REMOVED_FROM_PROGRAM: "gray",
};

/**
 * FR-V2.5-06's triage moves, in the order a reviewer reaches for them. The
 * button says what the reviewer is about to do; the tag says what the row is —
 * two different wordings for the same enum, so they carry separate keys.
 */
const TRIAGE = [
  { target: "RESPONSE_RECEIVED", label: "Record response" },
  { target: "UNDER_INVESTIGATION", label: "Investigate" },
  { target: "RESOLVED", label: "Resolve", needsNotes: true },
  {
    target: "REMOVED_FROM_PROGRAM",
    label: "Remove from programme",
    needsNotes: true,
  },
];

const CLOSED = ["RESOLVED", "REMOVED_FROM_PROGRAM"];

const dateCell = (value) =>
  value ? formatDateOnly(value.substring(0, 10)) : "—";

/**
 * The notification a reviewer sends by hand when the participant has no
 * contact email, or mail is not configured for this installation (FR-V2.5-08's
 * CSV fallback).
 */
const notificationCsv = (row) => {
  const header = [
    "Participant",
    "Scheme",
    "Cycle",
    "Test",
    "Reported",
    "Target",
    "Z-score",
  ];
  const lines = (row.results.length ? row.results : [{}]).map((result) =>
    [
      row.organizationName,
      row.schemeName,
      row.cycleName || row.cycleNumber || row.cycleId,
      result.testName || result.analyteName,
      result.reported,
      result.target,
      result.zScore,
    ]
      .map(csvCell)
      .join(","),
  );
  return [header.map(csvCell).join(","), ...lines].join("\n");
};

/**
 * Provider-side participant follow-up register (T-27, FR-V2.5-05..08): the
 * laboratories this lab provides PT to that returned unacceptable results, the
 * triage they are in, and the two actions that leave this page — notifying the
 * lab, and reprovisioning its panel (which is T-26's repeat shipment).
 *
 * Escalation here is a register state, never a local non-conformity: the NCE
 * path belongs to this lab's own Follow-Up Queue (AC-V2.5-10).
 */
const ProviderFollowupRegister = () => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const canTriage = hasQaPermission(userSessionDetails, "qa.eqa.provider");

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [notice, setNotice] = useState(null);
  const [prompt, setPrompt] = useState(null);
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    fetchProviderRegister((data) => {
      setRows(data);
      setLoading(false);
    });
  }, []);

  useEffect(load, [load]);

  const report = ({ ok, body }, successText) => {
    setBusy(false);
    setNotice(
      ok
        ? { kind: "success", text: successText }
        : {
            kind: "error",
            text:
              body?.error ||
              t("eqa.provider.followups.failed", "The action was refused."),
          },
    );
    if (ok) {
      load();
    }
  };

  const onTriage = (row, action, withNotes) => {
    setBusy(true);
    triageFollowUp(row.id, action.target, withNotes, (response) => {
      setPrompt(null);
      setNotes("");
      report(
        response,
        t("eqa.provider.followups.moved", "Follow-up moved to {status}.", {
          // The notice reports the state the row landed in, not the verb clicked.
          status: t(
            STATUS_KEY[action.target] ||
              `eqa.provider.followups.status.${action.target}`,
            action.target.toLowerCase().replace(/_/g, " "),
          ),
        }),
      );
    });
  };

  const onNotify = (row) => {
    setBusy(true);
    notifyParticipant(row.id, ({ ok, body }) => {
      if (ok && body && !body.emailed) {
        // No contact email, or mail is not configured: the reviewer sends it.
        downloadCsv(
          notificationCsv(row),
          `eqa-followup-${row.id}-notification.csv`,
        );
      }
      report(
        { ok, body },
        body?.emailed
          ? t(
              "eqa.provider.followups.emailed",
              "Notification emailed to {to}.",
              {
                to: body?.recipient || "",
              },
            )
          : t(
              "eqa.provider.followups.csvFallback",
              "No contact email on file — the notification was downloaded as CSV to send by hand.",
            ),
      );
    });
  };

  const onRepeat = (row, overrideNote) => {
    setBusy(true);
    requestRepeatPanel(row.id, overrideNote, (response) => {
      setPrompt(null);
      setNotes("");
      report(
        response,
        t(
          "eqa.provider.followups.repeatSent",
          "Repeat panel dispatched to {name}.",
          { name: row.organizationName },
        ),
      );
    });
  };

  const submitPrompt = () => {
    if (prompt.kind === "repeat") {
      onRepeat(prompt.row, notes);
      return;
    }
    onTriage(prompt.row, prompt.action, notes);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.provider.followups.title", "Participant follow-up")}
            </Heading>
            <p style={{ ...hintStyle, marginBottom: "1rem" }}>
              {t(
                "eqa.provider.followups.subtitle",
                "Laboratories participating in the schemes this lab provides that returned unacceptable results. Follow-up here is correspondence with another lab — it never opens a non-conformity in this lab's own register.",
              )}
            </p>
          </Section>

          {notice && (
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.text}
              onCloseButtonClick={() => setNotice(null)}
              style={{ marginBottom: "1rem", maxWidth: "100%" }}
            />
          )}

          {loading ? (
            <Loading withOverlay={false} small />
          ) : rows.length === 0 ? (
            <Tile>
              {t(
                "eqa.provider.followups.empty",
                "No participant follow-ups. Entries appear here as provider cycles are scored.",
              )}
            </Tile>
          ) : (
            <Table useZebraStyles>
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {t("eqa.shipment.participant", "Participant")}
                  </TableHeader>
                  <TableHeader>{t("eqa.queue.cycle", "Cycle")}</TableHeader>
                  <TableHeader>{t("label.status", "Status")}</TableHeader>
                  <TableHeader>
                    {t("eqa.provider.followups.status.NOTIFIED", "Notified")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.provider.followups.response", "Response")}
                  </TableHeader>
                  <TableHeader />
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableRow>
                      <TableCell>
                        {row.organizationName ||
                          t("eqa.provider.followups.unknownOrg", "Unknown lab")}
                        {row.persistentFailureFlag && (
                          <div>
                            <Tag type="red" size="sm">
                              {t(
                                "eqa.provider.followups.persistent",
                                "Persistent failure",
                              )}
                            </Tag>
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        {row.cycleName ||
                          t("eqa.queue.cycleNumber", "Cycle {number}", {
                            number: row.cycleNumber ?? row.cycleId,
                          })}
                        <div style={hintStyle}>{row.schemeName}</div>
                      </TableCell>
                      <TableCell>
                        <Tag
                          type={STATUS_TAG[row.followupStatus] || "gray"}
                          size="sm"
                        >
                          {t(
                            STATUS_KEY[row.followupStatus] ||
                              `eqa.provider.followups.status.${row.followupStatus}`,
                            (row.followupStatus || "").replace(/_/g, " "),
                          )}
                        </Tag>
                      </TableCell>
                      <TableCell>{dateCell(row.notifiedAt)}</TableCell>
                      <TableCell>{dateCell(row.responseReceivedAt)}</TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={
                            expanded === row.id ? ChevronUp : ChevronDown
                          }
                          iconDescription={t("eqa.queue.triage", "Triage")}
                          onClick={() =>
                            setExpanded(expanded === row.id ? null : row.id)
                          }
                        />
                      </TableCell>
                    </TableRow>
                    {expanded === row.id && (
                      <TableRow data-testid={`register-triage-${row.id}`}>
                        <TableCell colSpan={6}>
                          <Table size="sm" style={{ marginBottom: "0.75rem" }}>
                            <TableHead>
                              <TableRow>
                                <TableHeader>
                                  {t("eqa.results.test", "Test")}
                                </TableHeader>
                                <TableHeader>
                                  {t("eqa.queue.reported", "Reported")}
                                </TableHeader>
                                <TableHeader>
                                  {t("eqa.queue.target", "Target")}
                                </TableHeader>
                                <TableHeader>
                                  {t("eqa.queue.zScore", "Z-score")}
                                </TableHeader>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {row.results.map((result, index) => (
                                <TableRow key={result.testId || index}>
                                  <TableCell>
                                    {result.testName ||
                                      result.analyteName ||
                                      "—"}
                                  </TableCell>
                                  <TableCell>
                                    {result.reported ?? "—"}
                                  </TableCell>
                                  <TableCell>{result.target ?? "—"}</TableCell>
                                  <TableCell>{result.zScore ?? "—"}</TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                          {row.resolutionNotes && (
                            <p style={{ ...hintStyle, marginBottom: "0.5rem" }}>
                              {row.resolutionNotes}
                            </p>
                          )}
                          {canTriage &&
                            !CLOSED.includes(row.followupStatus) && (
                              <div
                                style={{
                                  display: "flex",
                                  gap: "0.5rem",
                                  flexWrap: "wrap",
                                }}
                              >
                                <Button
                                  kind="secondary"
                                  size="sm"
                                  disabled={busy}
                                  onClick={() => onNotify(row)}
                                >
                                  {t(
                                    "eqa.provider.followups.notify",
                                    "Notify lab",
                                  )}
                                </Button>
                                {TRIAGE.filter(
                                  // The row is already there; the server would
                                  // refuse the move, so it is not offered.
                                  (action) =>
                                    action.target !== row.followupStatus,
                                ).map((action) => (
                                  <Button
                                    key={action.target}
                                    kind={
                                      action.target === "REMOVED_FROM_PROGRAM"
                                        ? "danger"
                                        : "tertiary"
                                    }
                                    size="sm"
                                    disabled={busy}
                                    onClick={() =>
                                      action.needsNotes
                                        ? setPrompt({
                                            kind: "triage",
                                            row,
                                            action,
                                          })
                                        : onTriage(row, action, null)
                                    }
                                  >
                                    {t(
                                      `eqa.provider.followups.action.${action.target}`,
                                      action.label,
                                    )}
                                  </Button>
                                ))}
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  disabled={busy}
                                  onClick={() =>
                                    setPrompt({ kind: "repeat", row })
                                  }
                                >
                                  {t(
                                    "eqa.provider.followups.flagRepeat",
                                    "Flag for repeat",
                                  )}
                                </Button>
                              </div>
                            )}
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          )}
        </Column>
      </Grid>

      {prompt && (
        <Modal
          open
          modalHeading={
            prompt.kind === "repeat"
              ? t("eqa.receipt.repeatHeading", "Send a repeat panel")
              : t(
                  `eqa.provider.followups.action.${prompt.action.target}`,
                  prompt.action.label,
                )
          }
          primaryButtonText={t("label.confirm", "Confirm")}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy}
          onRequestClose={() => setPrompt(null)}
          onSecondarySubmit={() => setPrompt(null)}
          onRequestSubmit={submitPrompt}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {prompt.kind === "repeat"
              ? t(
                  "eqa.receipt.repeatHelp",
                  "The repeat comes out of the panel's reserve. If the reserve cannot cover it, a written justification is required before unreserved material is used.",
                )
              : t(
                  "eqa.provider.followups.notesHelp",
                  "What was agreed with the laboratory. Recorded against the register entry for accreditation trace.",
                )}
          </p>
          <TextArea
            id="eqa-provider-followup-notes"
            labelText={
              prompt.kind === "repeat"
                ? t("eqa.receipt.overrideNote", "Override note")
                : t("eqa.queue.notes", "Notes")
            }
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            rows={3}
          />
        </Modal>
      )}
    </>
  );
};

export default ProviderFollowupRegister;
