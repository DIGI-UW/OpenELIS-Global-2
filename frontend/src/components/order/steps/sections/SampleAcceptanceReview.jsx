import React, { useState, useEffect, useCallback } from "react";
import {
  Tile,
  Table,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  Tag,
  InlineLoading,
} from "@carbon/react";
import { useIntl, FormattedMessage } from "react-intl";
import SampleAcceptanceChecklist, {
  computeTransitMinutes,
  formatTransit,
} from "./SampleAcceptanceChecklist";
import { getOrderItemEvaluations } from "../../api/sampleAcceptanceApi";
import "./SampleAcceptanceChecklist.css";

/**
 * SampleAcceptanceReview — the S-09 (OGC-580) intake-acceptance surface for the
 * QA step. A master/detail: a table of the order's acceptance units, and below it
 * the SampleAcceptanceChecklist for the selected unit.
 *
 * The acceptance unit differs by domain:
 *  - Clinical / Environmental / non-pooled: one row per {@code sample_item}.
 *  - Vector: one row per POOL (members grouped by {@code vectorPoolId}). A pool
 *    decision cascades to all member sample_items on the backend, so the per-item
 *    gate still works; the table just shows one row per catch.
 *
 * Props:
 *  - orderId:   the order's sample(id), for the per-order status fetch + gate
 *  - labNumber: accession, shown per row + handed to the detail
 *  - samples:   the order's samples array (from OrderContext)
 *  - onBlockedChange(blocked): lifts the aggregate Mandatory-gate state to the host
 */
const STATUS = { ACCEPTED: "ACCEPTED", REVIEW: "REVIEW", PENDING: "PENDING" };

const collectedReceived = (sample) => ({
  collectedAt: [sample.collectionDate, sample.collectionTime]
    .filter(Boolean)
    .join(" "),
  receivedAt: [sample.receivedDate, sample.receivedTime]
    .filter(Boolean)
    .join(" "),
});

/**
 * Collapse live specimens into acceptance-unit rows: vector pool members
 * (truthy vectorPoolId) group into one entry per pool; everything else is its
 * own per-specimen entry. Each entry exposes a `representative` sample_item used
 * to load the checklist + key the status/transit (pool members share these).
 */
const buildEntries = (liveSamples) => {
  const entries = [];
  const poolIndex = new Map();
  liveSamples.forEach((sample) => {
    const poolId = sample.vectorPoolId;
    if (poolId) {
      let entry = poolIndex.get(String(poolId));
      if (!entry) {
        entry = {
          key: `pool-${poolId}`,
          kind: "pool",
          poolId,
          representative: sample,
          count: sample.vectorPoolMemberCount || 0,
          members: [],
        };
        poolIndex.set(String(poolId), entry);
        entries.push(entry);
      }
      entry.members.push(sample);
    } else {
      entries.push({
        key: String(sample.sampleItemId),
        kind: "item",
        representative: sample,
      });
    }
  });
  entries.forEach((entry) => {
    if (entry.kind === "pool" && entry.count < entry.members.length) {
      entry.count = entry.members.length; // fallback when memberCount absent
    }
  });
  return entries;
};

const SampleAcceptanceReview = ({
  orderId,
  labNumber,
  samples,
  onBlockedChange,
}) => {
  const intl = useIntl();

  // Live physical specimens only: saved (have a sample_item id) and not QC.
  const liveSamples = (samples || []).filter(
    (s) => s.sampleItemId && !s.qcMetadata?.qcType,
  );
  const entries = buildEntries(liveSamples);

  const [statusById, setStatusById] = useState({});
  const [loading, setLoading] = useState(false);
  const [selectedKey, setSelectedKey] = useState(null);
  // Optimistic per-entry reject flags: a rejected pool/specimen drops out of
  // /order/{id}/items, so the table tag can't come from statusById — track it here.
  const [locallyRejected, setLocallyRejected] = useState(() => new Set());

  const isEntryRejected = (entry) =>
    !!entry.representative.sampleRejected || locallyRejected.has(entry.key);

  const loadStatuses = useCallback(() => {
    if (!orderId) return;
    setLoading(true);
    getOrderItemEvaluations(orderId)
      .then((items) => {
        const map = {};
        items.forEach((it) => {
          map[String(it.sampleItemId)] = it;
        });
        setStatusById(map);
      })
      .finally(() => setLoading(false));
  }, [orderId]);

  useEffect(() => {
    loadStatuses();
  }, [loadStatuses]);

  // Default the selection to the first actionable (non-rejected) unit once rows
  // are known; fall back to the first row if all rejected.
  useEffect(() => {
    if (!selectedKey && entries.length > 0) {
      const firstActive =
        entries.find((e) => !e.representative.sampleRejected) || entries[0];
      setSelectedKey(firstActive.key);
    }
  }, [entries, selectedKey]);

  // Lift the aggregate blocked state (any live member unsatisfied under
  // MANDATORY) to the host; the server /gate is the backstop. statusById holds an
  // entry per non-rejected member (from /order/{id}/items), so the pool cascade is
  // reflected once loadStatuses re-runs after an accept.
  useEffect(() => {
    const anyBlocked = Object.values(statusById).some((s) => s.blocked);
    onBlockedChange?.(anyBlocked);
  }, [statusById, onBlockedChange]);

  // The detail reports the server evaluation on load + after an Accept; refresh
  // that row's tag from it. STABLE reference (keyed off the evaluation's own
  // sampleItemId, not a closed-over selection) — an unstable callback re-runs the
  // checklist's load effect on every render and wipes in-progress answers.
  const handleEvaluation = useCallback((evaluation) => {
    if (!evaluation || evaluation.sampleItemId == null) {
      return;
    }
    setStatusById((prev) => ({
      ...prev,
      [String(evaluation.sampleItemId)]: {
        sampleItemId: evaluation.sampleItemId,
        overallStatus: evaluation.overallStatus,
        blocked: evaluation.blocked,
        domain: evaluation.domain,
      },
    }));
  }, []);

  const markRejected = useCallback((key) => {
    setLocallyRejected((prev) => {
      const next = new Set(prev);
      next.add(key);
      return next;
    });
  }, []);

  const statusTag = (rejected, status) => {
    if (rejected) {
      return (
        <Tag type="red">
          <FormattedMessage
            id="sampleAcceptance.qa.status.rejected"
            defaultMessage="Rejected"
          />
        </Tag>
      );
    }
    if (status === STATUS.ACCEPTED) {
      return (
        <Tag type="green">
          <FormattedMessage
            id="sampleAcceptance.qa.status.accepted"
            defaultMessage="Accepted"
          />
        </Tag>
      );
    }
    if (status === STATUS.REVIEW) {
      return (
        <Tag type="warm-gray">
          <FormattedMessage
            id="sampleAcceptance.qa.status.review"
            defaultMessage="Review"
          />
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="sampleAcceptance.qa.status.pending"
          defaultMessage="Pending"
        />
      </Tag>
    );
  };

  if (entries.length === 0) {
    return (
      <Tile className="sac-tile">
        <h4>
          <FormattedMessage
            id="sampleAcceptance.review.title"
            defaultMessage="Intake Acceptance"
          />
        </h4>
        <p className="sac-subtitle">
          <FormattedMessage
            id="sampleAcceptance.review.noSamples"
            defaultMessage="No collected specimens to review yet."
          />
        </p>
      </Tile>
    );
  }

  const selectedEntry = entries.find((e) => e.key === selectedKey);
  const selectedCR = selectedEntry
    ? collectedReceived(selectedEntry.representative)
    : {};

  return (
    <Tile className="sac-review">
      <div className="sac-review-header">
        <h4>
          <FormattedMessage
            id="sampleAcceptance.review.title"
            defaultMessage="Intake Acceptance"
          />
        </h4>
        <p className="sac-subtitle">
          <FormattedMessage
            id="sampleAcceptance.review.subtitle"
            defaultMessage="Select a sample to complete its acceptance checklist"
          />
          {loading && (
            <InlineLoading
              className="sac-review-loading"
              description={intl.formatMessage({
                id: "label.loading",
                defaultMessage: "Loading...",
              })}
            />
          )}
        </p>
      </div>

      <Table size="md" useZebraStyles className="sac-review-table">
        <TableHead>
          <TableRow>
            <TableHeader>
              <FormattedMessage
                id="sampleAcceptance.review.col.labNo"
                defaultMessage="Lab #"
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id="sampleAcceptance.review.col.sampleType"
                defaultMessage="Sample type"
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id="sampleAcceptance.review.col.transit"
                defaultMessage="Transit"
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id="sampleAcceptance.review.col.eligibility"
                defaultMessage="Eligibility"
              />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {entries.map((entry) => {
            const rep = entry.representative;
            const { collectedAt, receivedAt } = collectedReceived(rep);
            const mins = computeTransitMinutes(collectedAt, receivedAt);
            const typeLabel =
              entry.kind === "pool"
                ? `${rep.sampleTypeName || "—"} ×${entry.count}`
                : rep.sampleTypeName || "—";
            return (
              <TableRow
                key={entry.key}
                isSelected={entry.key === selectedKey}
                onClick={() => setSelectedKey(entry.key)}
                className="sac-review-row"
              >
                <TableCell>{labNumber || "—"}</TableCell>
                <TableCell>{typeLabel}</TableCell>
                <TableCell>
                  {mins !== null ? formatTransit(mins) : "—"}
                </TableCell>
                <TableCell>
                  {statusTag(
                    isEntryRejected(entry),
                    statusById[String(rep.sampleItemId)]?.overallStatus,
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>

      {selectedEntry && (
        <SampleAcceptanceChecklist
          key={selectedKey}
          sampleItemId={String(selectedEntry.representative.sampleItemId)}
          vectorPoolId={
            selectedEntry.kind === "pool" ? selectedEntry.poolId : undefined
          }
          labNumber={labNumber}
          collectedAt={selectedCR.collectedAt}
          receivedAt={selectedCR.receivedAt}
          rejected={isEntryRejected(selectedEntry)}
          // Don't feed a rejected unit's evaluation into the status/gate map —
          // rejected units are read-only and must not count toward the gate.
          onEvaluation={
            isEntryRejected(selectedEntry) ? undefined : handleEvaluation
          }
          // A pool accept cascades to every member server-side; re-pull all
          // per-member statuses so the gate + tags reflect the full cascade.
          onAccepted={loadStatuses}
          onRejected={() => {
            markRejected(selectedEntry.key);
            loadStatuses();
          }}
        />
      )}
    </Tile>
  );
};

export default SampleAcceptanceReview;
