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
  Select,
  SelectItem,
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
import { ChevronDown, ChevronUp, Download } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { formatDateOnly, hasQaPermission } from "../../utils/Utils";
import { hintStyle, kpiLabelStyle, kpiValueStyle } from "../eqaCommon";
import {
  dismissFollowUp,
  escalateFollowUp,
  fetchFollowUpQueue,
} from "./followUpApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "eqa.oversight.followUpQueue", link: "/qa/eqa/follow-up-queue" },
];

const SOURCE_TAG = {
  external: "blue",
  in_house: "purple",
  inter_lab_split: "cyan",
};

const REASON_TAG = {
  questionable: "warm-gray",
  inHouseFailure: "red",
};

// FR-V2.3-02's triage categories, in the order a reviewer reaches for them.
const DISMISSAL_CATEGORIES = [
  "TRANSCRIPTION_ERROR",
  "KNOWN_EQUIPMENT_ISSUE",
  "PENDING_RE_TEST",
  "ACCEPTABLE_ON_REVIEW",
  "OTHER",
];

const DAY_MS = 24 * 60 * 60 * 1000;

const daysSince = (value) => {
  if (!value) return null;
  const enqueued = new Date(value.replace(" ", "T"));
  if (isNaN(enqueued.getTime())) return null;
  return Math.max(0, Math.floor((Date.now() - enqueued.getTime()) / DAY_MS));
};

const zLabel = (value) =>
  value === null || value === undefined || value === "" ? "—" : value;

const csvCell = (value) => `"${String(value ?? "").replace(/"/g, '""')}"`;

/**
 * One CSV line per result, not per queue row: the register is cycle-grain but
 * the accreditation reader wants the analyte that failed.
 */
export const queueCsv = (rows, sourceLabelOf, reasonLabelOf) => {
  const header = [
    "Cycle",
    "Scheme",
    "Analyte",
    "Reported",
    "Target",
    "Z-score",
    "Reason",
    "Enqueued",
    "Source",
    "Status",
  ];
  const lines = rows.flatMap((row) => {
    const results = row.results.length ? row.results : [{}];
    return results.map((result) =>
      [
        row.cycleName || row.cycleNumber || row.cycleId,
        row.schemeName,
        result.analyteName,
        result.reported,
        result.target,
        result.zScore,
        reasonLabelOf(row.reason),
        row.notifiedAt,
        sourceLabelOf(row.sourceKey),
        row.followupStatus,
      ]
        .map(csvCell)
        .join(","),
    );
  });
  return [header.map(csvCell).join(","), ...lines].join("\n");
};

const downloadCsv = (content, filename) => {
  const url = URL.createObjectURL(
    new Blob([content], { type: "text/csv;charset=utf-8;" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
};

/**
 * FR-V2.3-02 — this lab's questionable EQA scores and in-house failures
 * awaiting corrective review, with the two triage actions the tiered NCE rules
 * leave to a human: escalate to an NCE, or dismiss with a category that writes
 * the matching competency event.
 */
const FollowUpQueuePage = () => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const canTriage = hasQaPermission(userSessionDetails, "qa.manage.eqa");

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [notice, setNotice] = useState(null);
  const [dismissing, setDismissing] = useState(null);
  const [category, setCategory] = useState(DISMISSAL_CATEGORIES[0]);
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    fetchFollowUpQueue((data) => {
      setRows(data);
      setLoading(false);
    });
  }, []);

  useEffect(load, [load]);

  const sourceLabelOf = (key) =>
    t(
      `eqa.queue.source.${key}`,
      key === "in_house"
        ? "In-house"
        : key === "inter_lab_split"
          ? "Inter-lab split"
          : "External provider",
    );

  const reasonLabelOf = (reason) =>
    t(
      `eqa.queue.reason.${reason}`,
      reason === "inHouseFailure" ? "In-house fail" : "Questionable",
    );

  const questionableCount = rows.filter(
    (row) => row.reason === "questionable",
  ).length;
  const inHouseCount = rows.filter(
    (row) => row.sourceKey === "in_house",
  ).length;
  const oldest = rows.reduce((max, row) => {
    const age = daysSince(row.notifiedAt);
    return age !== null && age > max ? age : max;
  }, 0);

  const onEscalate = (row) => {
    setBusy(true);
    escalateFollowUp(row.id, ({ ok, body }) => {
      setBusy(false);
      setNotice(
        ok
          ? {
              kind: "success",
              text: t(
                "eqa.queue.escalated",
                "Escalated — NCE {nce} raised for this cycle.",
                { nce: body?.nceNumber || "" },
              ),
            }
          : {
              kind: "error",
              text:
                body?.error ||
                t("eqa.queue.escalateFailed", "The escalation was refused."),
            },
      );
      if (ok) {
        load();
      }
    });
  };

  const onDismiss = () => {
    setBusy(true);
    dismissFollowUp(dismissing.id, category, notes, ({ ok, body }) => {
      setBusy(false);
      setNotice(
        ok
          ? {
              kind: "success",
              text: t(
                "eqa.queue.dismissed",
                "Dismissed. The competency event for this category is recorded.",
              ),
            }
          : {
              kind: "error",
              text:
                body?.error ||
                t("eqa.queue.dismissFailed", "The dismissal was refused."),
            },
      );
      setDismissing(null);
      setNotes("");
      setCategory(DISMISSAL_CATEGORIES[0]);
      if (ok) {
        load();
      }
    });
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.oversight.followUpQueue", "Follow-Up Queue")}
            </Heading>
            <p style={{ ...hintStyle, marginBottom: "1rem" }}>
              {t(
                "eqa.oversight.queueSubtitle",
                "This lab's questionable EQA scores awaiting corrective review, from external providers and our own in-house schemes. Provider-side follow-up on other labs' submissions lives in EQA Program Management.",
              )}
            </p>
          </Section>

          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={t("eqa.queue.policyTitle", "Auto-NCE policy active.")}
            subtitle={t(
              "eqa.queue.policyBody",
              "Unacceptable scores on any external scheme this lab participates in open an NCE automatically. This queue holds the questionable range (2 < |Z| ≤ 3) and in-house failures, which need human triage.",
            )}
            style={{ marginBottom: "1rem", maxWidth: "100%" }}
          />

          {notice && (
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.text}
              onCloseButtonClick={() => setNotice(null)}
              style={{ marginBottom: "1rem", maxWidth: "100%" }}
            />
          )}

          <Grid condensed style={{ marginBottom: "1.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-queued">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.queue.kpi.queued", "Items queued")}
                </h4>
                <p style={kpiValueStyle}>{rows.length}</p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-questionable">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.queue.kpi.questionable", "Questionable")}
                </h4>
                <p style={kpiValueStyle}>{questionableCount}</p>
                <span style={hintStyle}>
                  {t("eqa.queue.kpi.questionable.hint", "2 < |Z| ≤ 3")}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-inhouse">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.queue.kpi.inHouse", "In-house failures")}
                </h4>
                <p style={kpiValueStyle}>{inHouseCount}</p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-oldest">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.queue.kpi.oldest", "Oldest")}
                </h4>
                <p style={kpiValueStyle}>
                  {rows.length
                    ? t("eqa.queue.kpi.days", "{days} days", { days: oldest })
                    : "—"}
                </p>
              </Tile>
            </Column>
          </Grid>

          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "0.5rem",
            }}
          >
            <strong>
              {t("eqa.queue.count", "Queue · {count} items", {
                count: rows.length,
              })}
            </strong>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Download}
              disabled={rows.length === 0}
              onClick={() =>
                downloadCsv(
                  queueCsv(rows, sourceLabelOf, reasonLabelOf),
                  "eqa-follow-up-queue.csv",
                )
              }
            >
              {t("eqa.queue.exportCsv", "Export CSV")}
            </Button>
          </div>

          {loading ? (
            <Loading withOverlay={false} small />
          ) : rows.length === 0 ? (
            <Tile>
              {t(
                "eqa.queue.empty",
                "Nothing awaiting triage. Questionable scores and in-house failures land here as cycles are scored.",
              )}
            </Tile>
          ) : (
            <Table useZebraStyles>
              <TableHead>
                <TableRow>
                  <TableHeader>{t("eqa.queue.cycle", "Cycle")}</TableHeader>
                  <TableHeader>{t("eqa.queue.analyte", "Analyte")}</TableHeader>
                  <TableHeader>{t("eqa.queue.zScore", "Z-score")}</TableHeader>
                  <TableHeader>{t("eqa.queue.reason", "Reason")}</TableHeader>
                  <TableHeader>
                    {t("eqa.queue.enqueued", "Enqueued")}
                  </TableHeader>
                  <TableHeader>{t("eqa.queue.source", "Source")}</TableHeader>
                  <TableHeader />
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableRow>
                      <TableCell>
                        {row.cycleName ||
                          t("eqa.queue.cycleNumber", "Cycle {number}", {
                            number: row.cycleNumber ?? row.cycleId,
                          })}
                        <div style={hintStyle}>{row.schemeName}</div>
                      </TableCell>
                      <TableCell>
                        {row.analyteLabel.length === 0
                          ? "—"
                          : row.analyteLabel.length === 1
                            ? row.analyteLabel[0]
                            : t(
                                "eqa.queue.analytePlus",
                                "{first} +{more} more",
                                {
                                  first: row.analyteLabel[0],
                                  more: row.analyteLabel.length - 1,
                                },
                              )}
                      </TableCell>
                      <TableCell>
                        <strong>{zLabel(row.worstZScore)}</strong>
                      </TableCell>
                      <TableCell>
                        <Tag type={REASON_TAG[row.reason]} size="sm">
                          {reasonLabelOf(row.reason)}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        {row.notifiedAt
                          ? formatDateOnly(row.notifiedAt.substring(0, 10))
                          : "—"}
                      </TableCell>
                      <TableCell>
                        <Tag type={SOURCE_TAG[row.sourceKey]} size="sm">
                          {sourceLabelOf(row.sourceKey)}
                        </Tag>
                      </TableCell>
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
                      <TableRow data-testid={`triage-${row.id}`}>
                        <TableCell colSpan={7}>
                          <div style={{ fontWeight: 600, marginBottom: 6 }}>
                            {t("eqa.queue.triageTitle", "Triage this item")}
                          </div>
                          <p style={{ ...hintStyle, marginBottom: "0.75rem" }}>
                            {t(
                              "eqa.queue.triageHelp",
                              "Escalate if you suspect a real non-conformity. Dismiss with a reason if this is noise — a transcription error, or equipment trouble already documented. Dismissed entries stay linked to the cycle for accreditation trace.",
                            )}
                          </p>
                          <Table size="sm" style={{ marginBottom: "0.75rem" }}>
                            <TableHead>
                              <TableRow>
                                <TableHeader>
                                  {t("eqa.queue.analyte", "Analyte")}
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
                                <TableHeader>
                                  {t("eqa.queue.verdict", "Verdict")}
                                </TableHeader>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {row.results.map((result, i) => (
                                <TableRow key={result.participantResultId || i}>
                                  <TableCell>
                                    {result.analyteName || "—"}
                                  </TableCell>
                                  <TableCell>
                                    {result.reported || "—"}
                                  </TableCell>
                                  <TableCell>{result.target || "—"}</TableCell>
                                  <TableCell>{zLabel(result.zScore)}</TableCell>
                                  <TableCell>
                                    {result.performanceStatus
                                      ? t(
                                          `eqa.performance.${result.performanceStatus}`,
                                          result.performanceStatus,
                                        )
                                      : "—"}
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                          <div style={{ display: "flex", gap: "0.5rem" }}>
                            {canTriage && (
                              <>
                                <Button
                                  kind="danger"
                                  size="sm"
                                  disabled={busy}
                                  onClick={() => onEscalate(row)}
                                >
                                  {t("eqa.queue.escalate", "Escalate to NCE")}
                                </Button>
                                <Button
                                  kind="secondary"
                                  size="sm"
                                  disabled={busy}
                                  onClick={() => setDismissing(row)}
                                >
                                  {t(
                                    "eqa.queue.dismiss",
                                    "Dismiss with reason",
                                  )}
                                </Button>
                              </>
                            )}
                            <Button
                              kind="ghost"
                              size="sm"
                              as={RouterLink}
                              to="/qa/eqa/my-cycles"
                            >
                              {t("eqa.queue.viewCycle", "View cycle")}
                            </Button>
                          </div>
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

      {dismissing && (
        <Modal
          open
          modalHeading={t("eqa.queue.dismissHeading", "Dismiss with reason")}
          primaryButtonText={t("eqa.queue.dismissConfirm", "Dismiss")}
          secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
          primaryButtonDisabled={busy}
          onRequestClose={() => setDismissing(null)}
          onSecondarySubmit={() => setDismissing(null)}
          onRequestSubmit={onDismiss}
        >
          <p style={{ ...hintStyle, marginBottom: "1rem" }}>
            {t(
              "eqa.queue.dismissHelp",
              "The category decides which competency event is written against every result this entry covers, so pick the one that describes what happened.",
            )}
          </p>
          <Select
            id="eqa-dismissal-category"
            labelText={t("eqa.queue.category", "Category")}
            value={category}
            onChange={(event) => setCategory(event.target.value)}
          >
            {DISMISSAL_CATEGORIES.map((value) => (
              <SelectItem
                key={value}
                value={value}
                text={t(
                  `eqa.queue.category.${value}`,
                  value.toLowerCase().replace(/_/g, " "),
                )}
              />
            ))}
          </Select>
          <TextArea
            id="eqa-dismissal-notes"
            labelText={t("eqa.queue.notes", "Notes")}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            rows={3}
          />
        </Modal>
      )}
    </>
  );
};

export default FollowUpQueuePage;
