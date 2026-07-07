import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  Button,
  Loading,
  InlineNotification,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import RangeModal from "./RangeModal";
import CoverageValidationPanel from "./CoverageValidationPanel";
import { formatAgeDays } from "./rangeUtils";

/**
 * OGC-949 M7 / OGC-969..971 — Reference Ranges section.
 *
 * Lists a test's reference ranges in a table, edits them via {@link RangeModal},
 * and saves the whole set in one PUT to /rest/test-catalog/tests/{id}/ranges
 * (diff-reconciled server-side). The {@link CoverageValidationPanel} renders the
 * per-sex coverage the backend computes on every load — the same report the
 * activation gate (OGC-973, wired in Basic Info) enforces.
 */
const RangesSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [ranges, setRanges] = useState([]);
  const [coverage, setCoverage] = useState(null);
  const [components, setComponents] = useState([]);
  // null = modal closed; -1 = adding a new range; >=0 = editing ranges[idx].
  const [editingIndex, setEditingIndex] = useState(null);

  const load = () => {
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/ranges`,
      (res) => {
        setLoading(false);
        if (!res || !Array.isArray(res.ranges)) {
          setError(true);
          return;
        }
        setRanges(res.ranges);
        setCoverage(res.coverage);
      },
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    load();
    // The range→component association (FR-19) needs the test's components to
    // offer a picker and to default a single-component test's ranges.
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/sample-results`,
      (res) => {
        if (res && Array.isArray(res.components)) {
          setComponents(
            res.components.map((c) => ({
              id: c.id,
              label: c.label || c.code || c.id,
            })),
          );
        }
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  const handleSaveRange = (r) => {
    setRanges((prev) =>
      editingIndex === -1
        ? [...prev, r]
        : prev.map((row, i) => (i === editingIndex ? { ...row, ...r } : row)),
    );
    setEditingIndex(null);
  };

  const removeRange = (i) =>
    setRanges((prev) => prev.filter((_, idx) => idx !== i));

  const handleSave = () => {
    setSaving(true);
    const payload = {
      testId,
      ranges: ranges.map((r) => ({
        id: r.id,
        componentId: r.componentId,
        gender: r.gender,
        minAge: r.minAge,
        maxAge: r.maxAge,
        lowNormal: r.lowNormal,
        highNormal: r.highNormal,
        lowCritical: r.lowCritical,
        highCritical: r.highCritical,
      })),
    };
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/ranges`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.ranges",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.ranges.saved",
            }),
          });
          load(); // refresh with server-assigned ids + recomputed coverage
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  const sexLabel = (gender) => {
    if (gender === "M") {
      return intl.formatMessage({ id: "label.testCatalog.ranges.male" });
    }
    if (gender === "F") {
      return intl.formatMessage({ id: "label.testCatalog.ranges.female" });
    }
    return intl.formatMessage({ id: "label.testCatalog.ranges.both" });
  };

  const ageLabel = (r) => {
    const noMin = !r.minAge;
    const noMax = r.maxAge === null || r.maxAge === undefined;
    if (noMin && noMax) {
      return intl.formatMessage({ id: "label.testCatalog.ranges.anyAge" });
    }
    if (noMax) {
      return `≥ ${formatAgeDays(r.minAge || 0, intl)}`;
    }
    return intl.formatMessage(
      { id: "label.testCatalog.ranges.gapRange" },
      {
        from: formatAgeDays(r.minAge || 0, intl),
        to: formatAgeDays(r.maxAge, intl),
      },
    );
  };

  const numRange = (low, high) => {
    if (
      (low === null || low === undefined) &&
      (high === null || high === undefined)
    ) {
      return "—";
    }
    return `${low ?? "−∞"} – ${high ?? "∞"}`;
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }
  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.ranges.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="ranges-section">
      <CoverageValidationPanel coverage={coverage} />

      <div>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          data-testid="add-range"
          onClick={() => setEditingIndex(-1)}
        >
          <FormattedMessage id="label.testCatalog.ranges.add" />
        </Button>
      </div>

      {ranges.length === 0 ? (
        <p>
          <FormattedMessage id="label.testCatalog.ranges.empty" />
        </p>
      ) : (
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                {intl.formatMessage({ id: "label.testCatalog.ranges.col.sex" })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({ id: "label.testCatalog.ranges.col.age" })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({
                  id: "label.testCatalog.ranges.col.normal",
                })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({
                  id: "label.testCatalog.ranges.col.critical",
                })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({
                  id: "label.testCatalog.ranges.col.actions",
                })}
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {ranges.map((r, i) => (
              <TableRow key={r.id || `new-${i}`}>
                <TableCell>{sexLabel(r.gender)}</TableCell>
                <TableCell>{ageLabel(r)}</TableCell>
                <TableCell>{numRange(r.lowNormal, r.highNormal)}</TableCell>
                <TableCell>{numRange(r.lowCritical, r.highCritical)}</TableCell>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={Edit}
                    iconDescription={intl.formatMessage({
                      id: "label.button.edit",
                    })}
                    onClick={() => setEditingIndex(i)}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={TrashCan}
                    iconDescription={intl.formatMessage({
                      id: "label.button.delete",
                    })}
                    onClick={() => removeRange(i)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <div>
        <Button kind="primary" disabled={saving} onClick={handleSave}>
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>

      {editingIndex !== null && (
        <RangeModal
          range={editingIndex >= 0 ? ranges[editingIndex] : null}
          components={components}
          onSave={handleSaveRange}
          onCancel={() => setEditingIndex(null)}
        />
      )}
    </Stack>
  );
};

export default RangesSection;
