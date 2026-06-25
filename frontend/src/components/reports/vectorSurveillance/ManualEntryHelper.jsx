import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  TextInput,
  Button,
  IconButton,
  Tag,
  Modal,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

const VIEW_URL = "/rest/reports/vector-surveillance/manual-entry";
const SUBMIT_URL = "/rest/reports/vector-surveillance/manual-entry/submit";

/** ISO week (Mon-Sun) bounds for a given ISO week-string value "YYYY-Www". */
function isoWeekToRange(isoWeek) {
  if (!isoWeek) {
    return { periodStart: null, periodEnd: null };
  }
  const match = /^(\d{4})-W(\d{2})$/.exec(isoWeek);
  if (!match) {
    return { periodStart: null, periodEnd: null };
  }
  const year = Number(match[1]);
  const week = Number(match[2]);
  // ISO-8601: week 1 is the week containing the first Thursday.
  const simple = new Date(Date.UTC(year, 0, 1 + (week - 1) * 7));
  const dayOfWeek = simple.getUTCDay();
  const isoMonday = new Date(simple);
  const offset = dayOfWeek <= 4 ? dayOfWeek - 1 : dayOfWeek - 8;
  isoMonday.setUTCDate(simple.getUTCDate() - offset);
  const isoSunday = new Date(isoMonday);
  isoSunday.setUTCDate(isoMonday.getUTCDate() + 6);
  const fmt = (d) => d.toISOString().slice(0, 10);
  return { periodStart: fmt(isoMonday), periodEnd: fmt(isoSunday) };
}

/** Current ISO week as "YYYY-Www". */
function currentIsoWeek() {
  const now = new Date();
  const target = new Date(
    Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()),
  );
  const dayNr = (target.getUTCDay() + 6) % 7;
  target.setUTCDate(target.getUTCDate() - dayNr + 3);
  const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
  const week =
    1 +
    Math.round(
      ((target - firstThursday) / 86400000 -
        3 +
        ((firstThursday.getUTCDay() + 6) % 7)) /
        7,
    );
  return `${target.getUTCFullYear()}-W${String(week).padStart(2, "0")}`;
}

function MetricTile({ row, onCopy }) {
  const intl = useIntl();
  const gated = row.gated;
  const value = gated ? "" : (row.value ?? "");

  return (
    <Tile style={{ marginBottom: "0.75rem" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: "0.5rem",
        }}
      >
        <div style={{ flex: 1 }}>
          <p style={{ fontWeight: 600, marginBottom: "0.25rem" }}>
            {row.label}
            {row.portalTag && (
              <Tag type="gray" size="sm" style={{ marginLeft: "0.5rem" }}>
                {row.portalTag}
              </Tag>
            )}
          </p>
          {gated ? (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "vectorReport.manualEntry.gated",
                defaultMessage: "Needs ≥ 95% coverage",
              })}
              subtitle=""
            />
          ) : (
            <p style={{ fontSize: "1.5rem", fontWeight: 400 }}>{value || "—"}</p>
          )}
        </div>
        {!gated && (
          <IconButton
            kind="ghost"
            label={intl.formatMessage({
              id: "vectorReport.manualEntry.copy",
              defaultMessage: "Copy value",
            })}
            onClick={() => onCopy(value)}
            disabled={!value}
          >
            <Copy />
          </IconButton>
        )}
      </div>
    </Tile>
  );
}

export default function ManualEntryHelper() {
  const intl = useIntl();
  const [isoWeek, setIsoWeek] = useState(currentIsoWeek());
  const [view, setView] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState(null);

  const { periodStart, periodEnd } = isoWeekToRange(isoWeek);

  const load = useCallback(() => {
    if (!periodStart || !periodEnd) {
      return;
    }
    setLoading(true);
    getFromOpenElisServer(
      `${VIEW_URL}?periodStart=${periodStart}&periodEnd=${periodEnd}`,
      (data) => {
        setView(data || { rows: [] });
        setLoading(false);
      },
    );
  }, [periodStart, periodEnd]);

  useEffect(() => {
    load();
  }, [load]);

  const handleCopy = (value) => {
    if (value && navigator.clipboard) {
      navigator.clipboard.writeText(String(value));
      setNotice({
        kind: "success",
        title: intl.formatMessage({
          id: "vectorReport.manualEntry.copied",
          defaultMessage: "Copied to clipboard",
        }),
      });
    }
  };

  const handleSubmit = () => {
    if (!view || !periodStart || !periodEnd) {
      return;
    }
    setSubmitting(true);
    const valueSnapshot = {};
    (view.rows || []).forEach((r) => {
      valueSnapshot[r.metricKey] = r.gated ? "" : (r.value ?? "");
    });
    postToOpenElisServerFullResponse(
      SUBMIT_URL,
      JSON.stringify({ periodStart, periodEnd, siteId: null, valueSnapshot }),
      (response) => {
        setSubmitting(false);
        setSubmitOpen(false);
        if (response && (response.status === 201 || response.status === 200)) {
          setNotice({
            kind: "success",
            title: intl.formatMessage({
              id: "vectorReport.manualEntry.submitted",
              defaultMessage: "Week marked submitted",
            }),
          });
        } else {
          setNotice({
            kind: "error",
            title: intl.formatMessage({
              id: "vectorReport.manualEntry.submitError",
              defaultMessage: "Could not record submission",
            }),
          });
        }
      },
    );
  };

  const rows = view ? view.rows || [] : [];

  return (
    <Grid narrow style={{ padding: "1rem 0" }}>
      <Column lg={16} md={8} sm={4}>
        <h2 style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="vectorReport.manualEntry.title"
            defaultMessage="Manual Entry Helper"
          />
        </h2>

        {notice && (
          <InlineNotification
            kind={notice.kind}
            lowContrast
            title={notice.title}
            onCloseButtonClick={() => setNotice(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <div
          style={{
            display: "flex",
            gap: "1rem",
            alignItems: "flex-end",
            marginBottom: "1.5rem",
          }}
        >
          <TextInput
            id="manual-entry-week"
            type="week"
            labelText={intl.formatMessage({
              id: "vectorReport.manualEntry.week",
              defaultMessage: "Reporting week",
            })}
            value={isoWeek}
            onChange={(e) => setIsoWeek(e.target.value)}
            style={{ maxWidth: "16rem" }}
          />
          <Button
            kind="primary"
            onClick={() => setSubmitOpen(true)}
            disabled={loading || rows.length === 0}
          >
            <FormattedMessage
              id="vectorReport.manualEntry.markSubmitted"
              defaultMessage="Mark week submitted"
            />
          </Button>
        </div>

        {loading ? (
          <InlineLoading
            description={intl.formatMessage({
              id: "vectorReport.manualEntry.loading",
              defaultMessage: "Loading week's numbers...",
            })}
          />
        ) : rows.length === 0 ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "vectorReport.manualEntry.empty",
              defaultMessage: "No metrics configured for this period",
            })}
            subtitle=""
          />
        ) : (
          rows.map((row) => (
            <MetricTile key={row.metricKey} row={row} onCopy={handleCopy} />
          ))
        )}

        <Modal
          open={submitOpen}
          modalHeading={intl.formatMessage({
            id: "vectorReport.manualEntry.markSubmitted",
            defaultMessage: "Mark week submitted",
          })}
          primaryButtonText={intl.formatMessage({
            id: "vectorReport.manualEntry.confirmSubmit",
            defaultMessage: "Confirm",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
            defaultMessage: "Cancel",
          })}
          primaryButtonDisabled={submitting}
          onRequestClose={() => setSubmitOpen(false)}
          onRequestSubmit={handleSubmit}
        >
          <p>
            <FormattedMessage
              id="vectorReport.manualEntry.confirmBody"
              defaultMessage="Record an audit snapshot of this week's figures ({periodStart} – {periodEnd})? Re-submitting a week is recorded as a separate entry."
              values={{ periodStart, periodEnd }}
            />
          </p>
        </Modal>
      </Column>
    </Grid>
  );
}
