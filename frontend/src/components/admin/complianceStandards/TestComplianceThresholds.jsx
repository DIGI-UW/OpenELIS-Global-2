import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Loading,
  Grid,
  Column,
  Stack,
  Tag,
  Select,
  SelectItem,
  TextInput,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  InlineNotification,
  Tile,
  Modal,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  deleteFromOpenElisServer,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import MultiLimitForm from "./MultiLimitForm";
import SelectMapForm from "./SelectMapForm";

const TAG_BY_TYPE = {
  MAXIMUM: "red",
  MINIMUM: "blue",
  RANGE: "teal",
  BORDERLINE: "warm-gray",
  DESCRIPTIVE: "purple",
  EXACT: "cyan",
  SELECT_MAP: "purple",
};

// English defaults for the limit-type tag labels. Tag rendering goes through
// limitLabel(intl, type) so the bundled key wins when locale ≠ en; the
// defaults here are the FRS-prescribed strings and back-compat fallbacks
// (e.g. "MAXIMUM" → "High Limit ≤").
const LIMIT_LABEL_DEFAULT = {
  MAXIMUM: "High Limit ≤",
  MINIMUM: "Low Limit ≥",
  RANGE: "Normal Range",
  BORDERLINE: "Borderline 🚩",
  DESCRIPTIVE: "Qualitative",
  EXACT: "Exact",
  SELECT_MAP: "Value Mapping",
};

function limitLabel(intl, type) {
  if (!type) return "";
  return intl.formatMessage({
    id: `compliance.limitType.${type}`,
    defaultMessage: LIMIT_LABEL_DEFAULT[type] || type,
  });
}

const STATUS_TAG = {
  ACTIVE: "green",
  DRAFT: "blue",
  SUPERSEDED: "warm-gray",
  ARCHIVED: "gray",
};

const STATUS_LABEL_DEFAULT = {
  ACTIVE: "Active",
  DRAFT: "Draft",
  SUPERSEDED: "Superseded",
  ARCHIVED: "Archived",
};

function statusLabel(intl, status) {
  if (!status) return "—";
  return intl.formatMessage({
    id: `compliance.status.${status}`,
    defaultMessage: STATUS_LABEL_DEFAULT[status] || status,
  });
}

function formatThresholdRange(t) {
  switch (t.thresholdType) {
    case "MAXIMUM":
      return `≤ ${t.maxValue ?? "—"}`;
    case "MINIMUM":
      return `≥ ${t.minValue ?? "—"}`;
    case "RANGE":
    case "BORDERLINE":
      return `${t.minValue ?? "—"} — ${t.maxValue ?? "—"}`;
    case "EXACT":
      return `${t.targetValue ?? "—"}`;
    case "DESCRIPTIVE":
      return t.valueDescriptive || "—";
    default:
      return "—";
  }
}

function buildThresholdPayload(groupId, test, lt) {
  return {
    group: { id: groupId },
    test: { id: test.id },
    parameterCode: test.code || test.value,
    displayName: test.value,
    thresholdType: lt.type,
    minValue: lt.lower !== "" && lt.lower !== undefined ? lt.lower : null,
    maxValue: lt.upper !== "" && lt.upper !== undefined ? lt.upper : null,
    units: lt.unit || null,
    notes: lt.note || null,
    valueDescriptive: lt.descriptive || null,
    isMandatory: false,
  };
}

function AddThresholdInline({ test, onSaved, onCancel }) {
  const intl = useIntl();
  // Toast on every save outcome so the admin gets visible confirmation
  // without scanning the table for an updated row count. Errors still
  // also surface via the inline banner because some callers (multi-limit
  // batch save) need per-row failure detail.
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [standards, setStandards] = useState([]);
  const [standardId, setStandardId] = useState("");
  const [groups, setGroups] = useState([]);
  const [groupId, setGroupId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const notifySuccess = (messageId, defaultMessage) => {
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({
        id: "notification.title.success",
        defaultMessage: "Saved",
      }),
      message: intl.formatMessage({ id: messageId, defaultMessage }),
    });
    setNotificationVisible(true);
  };

  const notifyError = (messageId, defaultMessage) => {
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({
        id: "notification.title.error",
        defaultMessage: "Error",
      }),
      message: intl.formatMessage({ id: messageId, defaultMessage }),
    });
    setNotificationVisible(true);
  };

  useEffect(() => {
    // FR-3-008 / v2.1 spec: only ACTIVE standards appear in the threshold
    // add form's standard selector. DRAFT and ARCHIVED standards are not
    // selectable here. Existing thresholds linked to non-ACTIVE standards
    // still render in the read-only threshold table.
    getFromOpenElisServer("/rest/compliance/standards", (data) => {
      if (Array.isArray(data)) {
        setStandards(data.filter((s) => s.status === "ACTIVE"));
      }
    });
  }, []);

  useEffect(() => {
    if (!standardId) {
      setGroups([]);
      setGroupId("");
      return;
    }
    getFromOpenElisServer(
      `/rest/compliance/standards/${standardId}/parameter-groups`,
      (data) => {
        setGroups(Array.isArray(data) ? data : []);
        setGroupId("");
      },
    );
  }, [standardId]);

  const handleSaveNumeric = (limits) => {
    setLoading(true);
    setError(null);
    const tasks = limits.map((lt) => {
      const payload = buildThresholdPayload(groupId, test, lt);
      return new Promise((resolve) =>
        postToOpenElisServerJsonResponse(
          "/rest/compliance/thresholds",
          JSON.stringify(payload),
          resolve,
        ),
      ).then((resp) => ({ type: lt.type, resp }));
    });
    return Promise.all(tasks).then((results) => {
      setLoading(false);
      const errs = {};
      results.forEach(({ type, resp }) => {
        if (!resp || resp.error || !resp.id) {
          errs[type] =
            (resp && (resp.error || resp.message)) ||
            intl.formatMessage({
              id: "compliance.threshold.saveError",
              defaultMessage: "Could not save this row.",
            });
        }
      });
      if (Object.keys(errs).length === 0) {
        notifySuccess(
          "compliance.threshold.savedThresholds",
          "Compliance thresholds linked to the test.",
        );
        onSaved();
      } else {
        notifyError(
          "compliance.threshold.savedPartialError",
          "Some thresholds could not be saved. Review the highlighted rows.",
        );
      }
      return errs;
    });
  };

  const handleSaveSelect = (valueMap) => {
    setLoading(true);
    setError(null);
    const payload = {
      group: { id: groupId },
      test: { id: test.id },
      parameterCode: test.code || test.value,
      displayName: test.value,
      thresholdType: "SELECT_MAP",
      isMandatory: false,
      valueMappings: Object.entries(valueMap).map(([opt, status]) => ({
        optionValue: opt,
        complianceStatus: status,
      })),
    };
    postToOpenElisServerJsonResponse(
      "/rest/compliance/thresholds",
      JSON.stringify(payload),
      (resp) => {
        setLoading(false);
        if (resp && resp.id && !resp.error) {
          notifySuccess(
            "compliance.threshold.savedValueMap",
            "Value-mapping threshold saved.",
          );
          onSaved();
        } else {
          const msg =
            (resp && (resp.error || resp.message)) ||
            intl.formatMessage({
              id: "compliance.threshold.saveError",
              defaultMessage: "Could not save this row.",
            });
          setError(msg);
          notifyError(
            "compliance.threshold.saveError",
            "Could not save this row.",
          );
        }
      },
    );
  };

  return (
    <Tile
      style={{
        padding: "1rem",
        background: "#edf5ff",
        borderLeft: "3px solid #0f62fe",
        marginBottom: "1rem",
      }}
    >
      <h5 style={{ margin: 0, marginBottom: "1rem" }}>
        <FormattedMessage
          id="compliance.testEditor.addThresholdHeading"
          defaultMessage="Link {name} to a compliance standard"
          values={{ name: <strong>{test?.value || ""}</strong> }}
        />
      </h5>

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title=""
          subtitle={error}
          style={{ marginBottom: "0.75rem", maxWidth: "100%" }}
        />
      )}

      <Grid style={{ marginBottom: "1rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="add-th-standard"
            labelText={intl.formatMessage({
              id: "compliance.testEditor.pickStandard",
              defaultMessage: "Compliance standard",
            })}
            value={standardId}
            onChange={(e) => setStandardId(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "compliance.testEditor.pickStandardPlaceholder",
                defaultMessage: "-- Select a standard --",
              })}
            />
            {standards.map((s) => (
              <SelectItem
                key={s.id}
                value={String(s.id)}
                text={`${s.name} (${s.regulationNumber || s.version || ""})`}
              />
            ))}
          </Select>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="add-th-group"
            labelText={intl.formatMessage({
              id: "compliance.testEditor.pickGroup",
              defaultMessage: "Parameter group",
            })}
            value={groupId}
            onChange={(e) => setGroupId(e.target.value)}
            disabled={!standardId}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "compliance.testEditor.pickGroupPlaceholder",
                defaultMessage: "-- Select a parameter group --",
              })}
            />
            {groups.map((g) => (
              <SelectItem key={g.id} value={String(g.id)} text={g.name} />
            ))}
          </Select>
        </Column>
      </Grid>

      {!groupId && (
        <p
          style={{
            fontSize: "0.875rem",
            color: "var(--cds-text-placeholder)",
            fontStyle: "italic",
            marginBottom: "0.5rem",
          }}
        >
          <FormattedMessage
            id="compliance.testEditor.pickGroupHint"
            defaultMessage="Pick a standard and parameter group above to configure the threshold."
          />
        </p>
      )}

      {groupId && test?.resultType === "select" ? (
        (test.selectOptions || []).length === 0 ? (
          <p
            style={{
              fontSize: "0.875rem",
              color: "var(--cds-text-placeholder)",
              fontStyle: "italic",
            }}
          >
            <FormattedMessage
              id="compliance.linkTest.selectNoOptions"
              defaultMessage="This test is select-list but has no options configured in the test catalog. Add result options in test management before linking it to a compliance standard."
            />
          </p>
        ) : (
          <SelectMapForm
            testCode={test.code || test.id}
            testName={test.value}
            options={test.selectOptions}
            existingMap={null}
            onSave={handleSaveSelect}
            onCancel={onCancel}
          />
        )
      ) : groupId ? (
        <MultiLimitForm
          testCode={test.code || test.id}
          testName={test.value}
          unit=""
          existingLimits={null}
          onSave={handleSaveNumeric}
          onCancel={onCancel}
        />
      ) : (
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="ghost" size="sm" onClick={onCancel}>
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
        </div>
      )}

      {loading && (
        <Loading
          small
          description={intl.formatMessage({
            id: "compliance.button.saving",
            defaultMessage: "Saving…",
          })}
          withOverlay={false}
        />
      )}
    </Tile>
  );
}

/**
 * Per-test compliance threshold editor.
 *
 * Two embedding modes:
 *  - Standalone admin tab (default): renders the search input + overview
 *    table + per-test editor in sequence. Empty state shows the table.
 *  - Embedded inside the Test Editor (FR-3-001): pass `embeddedTestId` to
 *    pre-select a test and skip the search/overview chrome — only the
 *    per-test editor + Add Threshold flow + threshold table render. The
 *    "Edit Test" header bar is also suppressed in this mode because the
 *    surrounding Test Editor already provides the test identity.
 */
function TestComplianceThresholds({ embeddedTestId, onCountChange } = {}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const isEmbedded = Boolean(embeddedTestId);

  const toast = (kind, titleKey, titleDefault, messageKey, messageDefault) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: titleKey, defaultMessage: titleDefault }),
      message: intl.formatMessage({
        id: messageKey,
        defaultMessage: messageDefault,
      }),
    });
    setNotificationVisible(true);
  };

  const [tests, setTests] = useState([]);
  const [selectedTestId, setSelectedTestId] = useState(
    embeddedTestId ? String(embeddedTestId) : "",
  );
  const [searchText, setSearchText] = useState("");
  const [thresholds, setThresholds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [adding, setAdding] = useState(false);
  const [unlinkTarget, setUnlinkTarget] = useState(null);
  // FR-3-012: per-test threshold table grouping toggle. STANDARD groups by
  // standard.id (default per spec), GROUP groups by parameter_group.id.
  const [groupBy, setGroupBy] = useState("STANDARD");
  // List of tests that already have at least one linked threshold, with
  // thresholdCount + standardCount pre-joined server-side. Backs the Tab 2
  // overview table on the landing page.
  const [linkedTests, setLinkedTests] = useState([]);

  const loadLinkedTests = () => {
    getFromOpenElisServer(
      "/rest/compliance/test-catalog/with-compliance",
      (data) => {
        if (!componentMounted.current) return;
        setLinkedTests(Array.isArray(data) ? data : []);
      },
    );
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/compliance/test-catalog", (data) => {
      if (componentMounted.current && Array.isArray(data)) {
        setTests(data);
      }
    });
    // Embedded mode skips the overview-table fetch — the parent Test Editor
    // already knows which test is being edited, so the "Tests linked to
    // standards" landing has nothing to do.
    if (!isEmbedded) {
      loadLinkedTests();
    }
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Re-sync when the parent passes a different testId (only meaningful in
  // embedded mode — parent component re-mounts on test switch otherwise).
  useEffect(() => {
    if (embeddedTestId) {
      setSelectedTestId(String(embeddedTestId));
    }
  }, [embeddedTestId]);

  const reload = () => {
    if (!selectedTestId) {
      setThresholds([]);
      return;
    }
    setLoading(true);
    getFromOpenElisServer(
      `/rest/tests/${selectedTestId}/compliance-thresholds`,
      (data) => {
        if (componentMounted.current) {
          setThresholds(Array.isArray(data) ? data : []);
          setLoading(false);
        }
      },
    );
    // Counts on the overview table change whenever a threshold is added,
    // updated, or unlinked — refresh so the landing view stays accurate
    // when the user navigates back.
    loadLinkedTests();
  };

  useEffect(() => {
    reload();
    setAdding(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedTestId]);

  // v2.1 spec — Compliance tab shows a count badge on its tab label.
  // Notify the parent Test Editor whenever the threshold count changes
  // (initial load, add, unlink) so the badge stays in sync.
  useEffect(() => {
    if (onCountChange) {
      onCountChange(thresholds.length);
    }
  }, [thresholds, onCountChange]);

  const selectedTest = useMemo(
    () => tests.find((t) => String(t.id) === String(selectedTestId)),
    [tests, selectedTestId],
  );

  // Inline-flow typeahead: when nothing is selected, the user types into
  // the input and a flow-layout results panel renders below — no absolute
  // positioning so we never have to fight z-index / overflow:hidden on
  // the surrounding Tabs container.
  const filteredTests = useMemo(() => {
    const q = searchText.trim().toLowerCase();
    if (!q) return [];
    return tests
      .filter(
        (t) =>
          (t.value || "").toLowerCase().includes(q) ||
          (t.code || "").toLowerCase().includes(q) ||
          (t.loinc || "").toLowerCase().includes(q),
      )
      .slice(0, 25);
  }, [tests, searchText]);

  const linkedStandardCount = useMemo(() => {
    const ids = new Set();
    thresholds.forEach((t) => {
      const sid = t.group?.standard?.id;
      if (sid != null) ids.add(String(sid));
    });
    return ids.size;
  }, [thresholds]);

  // FR-3-011: warn the admin about thresholds attached to a standard that is
  // no longer ACTIVE — those rows still display but stop being evaluated at
  // validation time. One banner per (standard, status) pair so multiple
  // affected standards each surface their own warning.
  const inactiveLinkedStandards = useMemo(() => {
    const seen = new Map();
    thresholds.forEach((t) => {
      const s = t.group?.standard;
      if (!s || !s.id) return;
      if (s.status !== "SUPERSEDED" && s.status !== "ARCHIVED") return;
      if (!seen.has(s.id)) {
        seen.set(s.id, { id: s.id, name: s.name || "—", status: s.status });
      }
    });
    return Array.from(seen.values());
  }, [thresholds]);

  const handleConfirmUnlink = () => {
    if (!unlinkTarget) return;
    const targetLabel = unlinkTarget.parameterCode || unlinkTarget.displayName;
    deleteFromOpenElisServer(
      `/rest/compliance/thresholds/${unlinkTarget.id}`,
      (resp) => {
        // deleteFromOpenElisServer fires the callback regardless of HTTP
        // outcome; treat anything non-2xx as a failure so the admin learns
        // the row is still linked. The conflict path (BR-002 / BR-003)
        // returns 409 and the row stays in place.
        const ok = !resp || resp === true || (resp.status && resp.status < 300);
        setUnlinkTarget(null);
        if (ok) {
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Unlinked",
            "compliance.threshold.unlinked",
            "Threshold unlinked from {label}.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.threshold.unlinkError",
            "Could not unlink this threshold.",
          );
        }
        // Re-render either way — reload picks up the latest state.
        reload();
        // Avoid unused-var warning when the message template doesn't reach
        // the runtime path (some locales may format without {label}).
        void targetLabel;
      },
    );
  };

  // Per-threshold table row. Extracted so the section-header injection in
  // the FR-3-012 group-by render can reuse it without duplicating the cell
  // markup.
  const renderThresholdRow = (t) => {
    const standard = t.group?.standard;
    const isBorderline = t.thresholdType === "BORDERLINE";
    return (
      <TableRow
        key={t.id}
        style={
          isBorderline
            ? {
                background: "rgba(255,193,7,0.08)",
                borderLeft: "3px solid #f1c21b",
              }
            : undefined
        }
      >
        <TableCell>{standard?.name || "—"}</TableCell>
        <TableCell>{t.group?.name || "—"}</TableCell>
        <TableCell>
          <Tag size="sm" type={TAG_BY_TYPE[t.thresholdType] || "gray"}>
            {limitLabel(intl, t.thresholdType)}
          </Tag>
        </TableCell>
        <TableCell>{formatThresholdRange(t)}</TableCell>
        <TableCell>{t.units || "—"}</TableCell>
        <TableCell>{standard?.effectiveDate || "—"}</TableCell>
        <TableCell>
          <Tag size="sm" type={STATUS_TAG[standard?.status] || "gray"}>
            {statusLabel(intl, standard?.status)}
          </Tag>
        </TableCell>
        <TableCell>
          <Button
            kind="ghost"
            size="sm"
            hasIconOnly
            renderIcon={TrashCan}
            iconDescription={intl.formatMessage({
              id: "compliance.testEditor.unlink",
              defaultMessage: "Unlink",
            })}
            onClick={() => setUnlinkTarget(t)}
          />
        </TableCell>
      </TableRow>
    );
  };

  return (
    <>
      {loading && <Loading />}
      <div>
        {!selectedTestId && (
          <>
            {/* Search input — does double duty: filters the overview table
                below AND surfaces unlinked tests from the catalog as
                "Add test to compliance" suggestions when there are no
                matches in the linked list. */}
            <div style={{ marginBottom: "1rem", maxWidth: "640px" }}>
              <TextInput
                id="ctt-test-search"
                labelText={intl.formatMessage({
                  id: "compliance.testThresholds.searchLabel",
                  defaultMessage: "Search test",
                })}
                placeholder={intl.formatMessage({
                  id: "compliance.testThresholds.comboboxPlaceholder",
                  defaultMessage: "Search by name, code, or LOINC…",
                })}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                size="lg"
              />
            </div>

            {(() => {
              const q = searchText.trim().toLowerCase();
              // linkedTests rows already carry test fields + counts inline
              // (server-assembled), so the brittle catalog/summary join
              // pattern from earlier is gone — what the API returns is what
              // we render.
              const linkedRows = linkedTests;
              const filteredLinked = q
                ? linkedRows.filter(
                    (t) =>
                      (t.value || "").toLowerCase().includes(q) ||
                      (t.code || "").toLowerCase().includes(q) ||
                      (t.loinc || "").toLowerCase().includes(q),
                  )
                : linkedRows;
              const linkedIdSet = new Set(linkedRows.map((t) => String(t.id)));
              const unlinkedMatches = q
                ? filteredTests
                    .filter((t) => !linkedIdSet.has(String(t.id)))
                    .slice(0, 8)
                : [];

              return (
                <>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "baseline",
                      marginBottom: "0.5rem",
                      flexWrap: "wrap",
                      gap: "0.5rem",
                    }}
                  >
                    <h5 style={{ margin: 0 }}>
                      <FormattedMessage
                        id="compliance.testThresholds.overviewHeading"
                        defaultMessage="Tests linked to compliance standards"
                      />
                    </h5>
                    {linkedRows.length > 0 && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--cds-text-02)",
                        }}
                      >
                        <FormattedMessage
                          id="compliance.testThresholds.overviewCount"
                          defaultMessage="{filtered} of {total} test{total, plural, one {} other {s}}"
                          values={{
                            filtered: filteredLinked.length,
                            total: linkedRows.length,
                          }}
                        />
                      </span>
                    )}
                  </div>

                  {/* Table is ALWAYS rendered on landing — matches the v2.3
                      mockup feel where the page is table-driven rather than
                      hidden behind a Tile placeholder. Empty / no-match
                      states render as a single full-width row inside the
                      table body. */}
                  <div
                    style={{
                      background: "#fff",
                      border: "1px solid var(--cds-border-subtle)",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <Table size="md">
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.test"
                              defaultMessage="Test"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.code"
                              defaultMessage="Code"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.loinc"
                              defaultMessage="LOINC"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.sampleTypes"
                              defaultMessage="Sample Types"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.thresholdCount"
                              defaultMessage="# Thresholds"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testThresholds.col.standardCount"
                              defaultMessage="# Standards"
                            />
                          </TableHeader>
                          <TableHeader style={{ width: "5rem" }}> </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {linkedRows.length === 0 ? (
                          <TableRow>
                            <TableCell
                              colSpan={7}
                              style={{
                                padding: "1.5rem 1rem",
                                textAlign: "center",
                                color: "var(--cds-text-02)",
                              }}
                            >
                              <div style={{ marginBottom: "0.25rem" }}>
                                <FormattedMessage
                                  id="compliance.testThresholds.overviewEmpty"
                                  defaultMessage="No tests are linked to compliance standards yet."
                                />
                              </div>
                              <div style={{ fontSize: "0.75rem" }}>
                                <FormattedMessage
                                  id="compliance.testThresholds.overviewEmptyHint"
                                  defaultMessage="CSV-seeded thresholds at parameter-group level appear in Tab 1 (Compliance Standards Admin), not here. Search above to pick a test from the catalog and link it to a standard."
                                />
                              </div>
                            </TableCell>
                          </TableRow>
                        ) : filteredLinked.length === 0 ? (
                          <TableRow>
                            <TableCell
                              colSpan={7}
                              style={{
                                padding: "1.5rem 1rem",
                                textAlign: "center",
                                color: "var(--cds-text-02)",
                              }}
                            >
                              <FormattedMessage
                                id="compliance.testThresholds.noLinkedMatches"
                                defaultMessage='No linked tests match "{q}".'
                                values={{ q: searchText }}
                              />
                            </TableCell>
                          </TableRow>
                        ) : (
                          filteredLinked.map((t) => (
                            <TableRow
                              key={t.id}
                              onClick={() => setSelectedTestId(String(t.id))}
                              style={{ cursor: "pointer" }}
                            >
                              <TableCell>
                                <strong>{t.value}</strong>
                              </TableCell>
                              <TableCell>{t.code || "—"}</TableCell>
                              <TableCell>{t.loinc || "—"}</TableCell>
                              <TableCell>
                                {t.sampleTypes && t.sampleTypes.length > 0 ? (
                                  <span style={{ fontSize: "0.8125rem" }}>
                                    {t.sampleTypes.join(", ")}
                                  </span>
                                ) : (
                                  "—"
                                )}
                              </TableCell>
                              <TableCell>
                                <Tag size="sm" type="blue">
                                  {t.thresholdCount}
                                </Tag>
                              </TableCell>
                              <TableCell>
                                <Tag size="sm" type="teal">
                                  {t.standardCount}
                                </Tag>
                              </TableCell>
                              <TableCell onClick={(e) => e.stopPropagation()}>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() =>
                                    setSelectedTestId(String(t.id))
                                  }
                                >
                                  <FormattedMessage
                                    id="compliance.testThresholds.openEditor"
                                    defaultMessage="Open"
                                  />
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))
                        )}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Suggestions: tests in the catalog matching the search
                      term that are NOT yet linked. Lets the admin add a new
                      test without a separate picker step. */}
                  {q && unlinkedMatches.length > 0 && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <h6
                        style={{
                          margin: "0 0 0.5rem 0",
                          fontSize: "0.75rem",
                          fontWeight: 700,
                          color: "var(--cds-text-02)",
                          textTransform: "uppercase",
                          letterSpacing: "0.04em",
                        }}
                      >
                        <FormattedMessage
                          id="compliance.testThresholds.addSection"
                          defaultMessage="Add a test to compliance"
                        />
                      </h6>
                      <div
                        style={{
                          border: "1px solid var(--cds-border-subtle)",
                          borderRadius: "4px",
                          background: "#fff",
                          maxWidth: "640px",
                        }}
                      >
                        {unlinkedMatches.map((t) => (
                          <button
                            key={t.id}
                            type="button"
                            onClick={() => {
                              setSelectedTestId(String(t.id));
                              setSearchText("");
                            }}
                            style={{
                              width: "100%",
                              display: "flex",
                              alignItems: "center",
                              gap: "0.5rem",
                              padding: "0.625rem 1rem",
                              fontSize: "0.875rem",
                              border: "none",
                              borderBottom:
                                "1px solid var(--cds-border-subtle)",
                              background: "transparent",
                              cursor: "pointer",
                              textAlign: "left",
                            }}
                          >
                            <Add size={16} />
                            <span style={{ fontWeight: 500 }}>{t.value}</span>
                            {t.code && (
                              <span
                                style={{
                                  fontSize: "0.75rem",
                                  color: "var(--cds-text-02)",
                                }}
                              >
                                {t.code}
                              </span>
                            )}
                            {t.resultType === "select" && (
                              <Tag size="sm" type="purple">
                                <FormattedMessage
                                  id="compliance.linkTest.selectListBadge"
                                  defaultMessage="Select List"
                                />
                              </Tag>
                            )}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              );
            })()}
          </>
        )}

        {selectedTestId && (
          <>
            {/* Stylized "Edit Test: …" header bar matching the v2.3 mockup —
                test name + LOINC / sample type / result type meta line on the
                left, a single Close button on the right. Each threshold
                add / unlink hits the API immediately, so there is no separate
                save action to surface here.

                Suppressed when embedded inside the Test Editor — the parent
                already shows the test name + LOINC, so a second identity
                strip is duplicate noise. */}
            {!isEmbedded && (
              <div
                style={{
                  background: "#fff",
                  border: "1px solid var(--cds-border-subtle)",
                  borderRadius: "4px",
                  padding: "1rem 1.25rem",
                  marginBottom: "1rem",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "1rem",
                  flexWrap: "wrap",
                }}
              >
                <div style={{ minWidth: 0 }}>
                  <h4
                    style={{
                      margin: 0,
                      fontSize: "1.125rem",
                      fontWeight: 600,
                    }}
                  >
                    <FormattedMessage
                      id="compliance.testEditor.editHeader"
                      defaultMessage="Edit Test:"
                    />{" "}
                    {selectedTest?.value || ""}
                  </h4>
                  <div
                    style={{
                      fontSize: "0.8125rem",
                      color: "var(--cds-text-02)",
                      marginTop: "2px",
                    }}
                  >
                    {selectedTest?.loinc && (
                      <>
                        <FormattedMessage
                          id="compliance.testEditor.meta.loinc"
                          defaultMessage="LOINC:"
                        />{" "}
                        {selectedTest.loinc}
                        {" | "}
                      </>
                    )}
                    {selectedTest?.sampleTypes &&
                      selectedTest.sampleTypes.length > 0 && (
                        <>
                          <FormattedMessage
                            id="compliance.testEditor.meta.sampleType"
                            defaultMessage="Sample Type:"
                          />{" "}
                          {selectedTest.sampleTypes.join(", ")}
                          {" | "}
                        </>
                      )}
                    <FormattedMessage
                      id="compliance.testEditor.meta.resultType"
                      defaultMessage="Result Type:"
                    />{" "}
                    {selectedTest?.resultType === "select"
                      ? intl.formatMessage({
                          id: "compliance.testEditor.meta.resultType.select",
                          defaultMessage: "Select List",
                        })
                      : intl.formatMessage({
                          id: "compliance.testEditor.meta.resultType.numeric",
                          defaultMessage: "Numeric",
                        })}
                  </div>
                </div>
                <Button
                  kind="secondary"
                  size="md"
                  onClick={() => setSelectedTestId("")}
                >
                  <FormattedMessage
                    id="label.button.close"
                    defaultMessage="Close"
                  />
                </Button>
              </div>
            )}

            {/* FR-3-001 mockup: vertical Test Editor sidebar grouping the
                non-Compliance tabs as visual placeholders. Mirrors the v2.3
                mockup structure (Configuration / Organization / Resources /
                Automation / Compliance section headers). The placeholder
                tabs are non-functional — they signal where the future Test
                Editor surface will live; only the Compliance tab is active.
                Hidden when embedded inside the actual Test Editor (the
                horizontal Tabs in TestModifyEntry already navigate). */}
            <div
              style={{
                display: "flex",
                gap: "1rem",
                alignItems: "flex-start",
              }}
            >
              {!isEmbedded && (
                <nav
                  aria-label="Test editor sections"
                  style={{
                    width: "14rem",
                    minWidth: "14rem",
                    background: "#fff",
                    border: "1px solid var(--cds-border-subtle)",
                    borderRadius: "4px",
                    padding: "0.5rem",
                  }}
                >
                  {[
                    {
                      label: intl.formatMessage({
                        id: "compliance.testEditor.section.configuration",
                        defaultMessage: "Configuration",
                      }),
                      tabs: [
                        "Basic Info",
                        "Sample & Results",
                        "Ranges",
                        "Sample Storage",
                      ],
                    },
                    {
                      label: intl.formatMessage({
                        id: "compliance.testEditor.section.organization",
                        defaultMessage: "Organization",
                      }),
                      tabs: ["Display Order", "Panels", "Labels"],
                    },
                    {
                      label: intl.formatMessage({
                        id: "compliance.testEditor.section.resources",
                        defaultMessage: "Resources",
                      }),
                      tabs: ["Terminology", "Reagents"],
                    },
                    {
                      label: intl.formatMessage({
                        id: "compliance.testEditor.section.automation",
                        defaultMessage: "Automation",
                      }),
                      tabs: ["Analyzers", "Methods", "Alerts", "Reflex & Calc"],
                    },
                  ].map((section) => (
                    <div key={section.label} style={{ marginBottom: "0.5rem" }}>
                      <div
                        style={{
                          padding: "0.375rem 0.75rem",
                          fontSize: "0.6875rem",
                          fontWeight: 700,
                          color: "var(--cds-text-placeholder)",
                          textTransform: "uppercase",
                          letterSpacing: "0.06em",
                        }}
                      >
                        {section.label}
                      </div>
                      {section.tabs.map((t) => (
                        <button
                          key={t}
                          type="button"
                          disabled
                          title={intl.formatMessage({
                            id: "compliance.testEditor.tabPlaceholder",
                            defaultMessage:
                              "Coming with the Test Editor surface — manage this from the Modify Test page for now.",
                          })}
                          style={{
                            width: "100%",
                            display: "block",
                            padding: "0.5rem 0.75rem",
                            fontSize: "0.875rem",
                            fontWeight: 500,
                            border: "none",
                            borderLeft: "3px solid transparent",
                            borderRadius: "3px",
                            cursor: "not-allowed",
                            textAlign: "left",
                            background: "transparent",
                            color: "var(--cds-text-placeholder)",
                            marginBottom: "2px",
                          }}
                        >
                          {t}
                        </button>
                      ))}
                    </div>
                  ))}
                  {/* Compliance section — the only active tab. */}
                  <div style={{ marginTop: "0.5rem" }}>
                    <div
                      style={{
                        padding: "0.375rem 0.75rem",
                        fontSize: "0.6875rem",
                        fontWeight: 700,
                        color: "var(--cds-text-placeholder)",
                        textTransform: "uppercase",
                        letterSpacing: "0.06em",
                      }}
                    >
                      <FormattedMessage
                        id="compliance.testEditor.section.compliance"
                        defaultMessage="Compliance"
                      />
                    </div>
                    <button
                      type="button"
                      style={{
                        width: "100%",
                        display: "block",
                        padding: "0.5rem 0.75rem",
                        fontSize: "0.875rem",
                        fontWeight: 600,
                        border: "none",
                        borderLeft: "3px solid #0e6027",
                        borderRadius: "3px",
                        textAlign: "left",
                        background: "#defbe6",
                        color: "#0e6027",
                        cursor: "default",
                      }}
                    >
                      <FormattedMessage
                        id="compliance.testEditor.tab.compliance"
                        defaultMessage="Compliance"
                      />
                    </button>
                  </div>
                </nav>
              )}

              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "1rem",
                    flexWrap: "wrap",
                    gap: "0.5rem",
                  }}
                >
                  <h5 style={{ margin: 0 }}>
                    <FormattedMessage
                      id="compliance.testEditor.thresholdsHeading"
                      defaultMessage="Compliance Thresholds for"
                    />{" "}
                    {selectedTest?.value || ""}
                  </h5>
                  <Stack
                    orientation="horizontal"
                    gap={3}
                    style={{ alignItems: "flex-end" }}
                  >
                    {/* FR-3-012: group-by toggle. Sorts threshold rows so all
                    rows from the same standard (or parameter group) sit
                    adjacent and section-header rows separate them. */}
                    {thresholds.length > 1 && (
                      <Select
                        id="ctt-group-by"
                        size="sm"
                        labelText={intl.formatMessage({
                          id: "compliance.testEditor.groupBy",
                          defaultMessage: "Group by",
                        })}
                        value={groupBy}
                        onChange={(e) => setGroupBy(e.target.value)}
                        style={{ minWidth: "11rem" }}
                      >
                        <SelectItem
                          value="STANDARD"
                          text={intl.formatMessage({
                            id: "compliance.testEditor.col.standard",
                            defaultMessage: "Standard",
                          })}
                        />
                        <SelectItem
                          value="GROUP"
                          text={intl.formatMessage({
                            id: "compliance.testEditor.col.parameterGroup",
                            defaultMessage: "Parameter Group",
                          })}
                        />
                      </Select>
                    )}
                    {!adding && (
                      <Button
                        kind="primary"
                        size="md"
                        renderIcon={Add}
                        onClick={() => setAdding(true)}
                      >
                        <FormattedMessage
                          id="compliance.testEditor.addThreshold"
                          defaultMessage="Add Threshold"
                        />
                      </Button>
                    )}
                  </Stack>
                </div>

                {linkedStandardCount > 0 && (
                  <InlineNotification
                    kind="info"
                    lowContrast
                    hideCloseButton
                    title=""
                    subtitle={intl.formatMessage(
                      {
                        id: "compliance.testEditor.linkedBanner",
                        defaultMessage:
                          "This test is linked to {count, plural, one {# compliance standard} other {# compliance standards}}. Thresholds are evaluated at validation time.",
                      },
                      { count: linkedStandardCount },
                    )}
                    style={{ marginBottom: "1rem", maxWidth: "100%" }}
                  />
                )}

                {/* FR-3-011: warn per affected standard whose status is no longer
                ACTIVE. The threshold rows still render so the admin can see
                what's at stake, but evaluations against this standard have
                stopped. */}
                {inactiveLinkedStandards.map((s) => (
                  <InlineNotification
                    key={`inactive-${s.id}`}
                    kind="warning"
                    lowContrast
                    hideCloseButton
                    title=""
                    subtitle={intl.formatMessage(
                      {
                        id: "compliance.testEditor.standardInactiveBanner",
                        defaultMessage:
                          "This test has thresholds linked to {name} which is now {status}. Results will no longer be evaluated against this standard.",
                      },
                      {
                        name: s.name,
                        status: statusLabel(intl, s.status),
                      },
                    )}
                    style={{ marginBottom: "1rem", maxWidth: "100%" }}
                  />
                ))}

                {/* Limit Types legend — visual key matching the v2.3 mockup. */}
                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "0.5rem",
                    padding: "0.625rem 1rem",
                    background: "#fff",
                    border: "1px solid var(--cds-border-subtle)",
                    borderRadius: "2px",
                    marginBottom: "1rem",
                    alignItems: "center",
                  }}
                >
                  <span
                    style={{
                      fontSize: "0.6875rem",
                      fontWeight: 700,
                      color: "var(--cds-text-02)",
                      textTransform: "uppercase",
                      letterSpacing: "0.04em",
                      marginRight: "0.25rem",
                    }}
                  >
                    <FormattedMessage
                      id="compliance.legend.limitTypes"
                      defaultMessage="Limit Types:"
                    />
                  </span>
                  <Tag size="sm" type="red">
                    {limitLabel(intl, "MAXIMUM")}
                  </Tag>
                  <Tag size="sm" type="blue">
                    {limitLabel(intl, "MINIMUM")}
                  </Tag>
                  <Tag size="sm" type="teal">
                    {limitLabel(intl, "RANGE")}
                  </Tag>
                  <Tag size="sm" type="warm-gray">
                    {limitLabel(intl, "BORDERLINE")}
                  </Tag>
                  <Tag size="sm" type="purple">
                    {limitLabel(intl, "DESCRIPTIVE")}
                  </Tag>
                </div>

                {adding && selectedTest && (
                  <AddThresholdInline
                    test={selectedTest}
                    onSaved={() => {
                      setAdding(false);
                      reload();
                    }}
                    onCancel={() => setAdding(false)}
                  />
                )}

                {thresholds.length === 0 && !loading && !adding ? (
                  <Tile>
                    <p>
                      <FormattedMessage
                        id="compliance.testThresholds.empty"
                        defaultMessage="No compliance thresholds are linked to this test yet. Click Add Threshold to link this test to a compliance standard's parameter group."
                      />
                    </p>
                  </Tile>
                ) : thresholds.length > 0 ? (
                  <div
                    style={{
                      background: "#fff",
                      border: "1px solid var(--cds-border-subtle)",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <Table size="md">
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.standard"
                              defaultMessage="Standard"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.parameterGroup"
                              defaultMessage="Parameter Group"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.limitType"
                              defaultMessage="Limit Type"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.thresholdRange"
                              defaultMessage="Threshold / Range"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.unit"
                              defaultMessage="Unit"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.effective"
                              defaultMessage="Effective"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="compliance.testEditor.col.status"
                              defaultMessage="Status"
                            />
                          </TableHeader>
                          <TableHeader style={{ width: "3rem" }}> </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {(() => {
                          // Sort + group thresholds by the active groupBy mode.
                          // A header row is inserted whenever the grouping key
                          // changes, so the table reads as sectioned even though
                          // it's a single <TableBody>.
                          const sectionKey = (t) =>
                            groupBy === "GROUP"
                              ? (t.group?.id ?? "?")
                              : (t.group?.standard?.id ?? "?");
                          const sectionLabel = (t) =>
                            groupBy === "GROUP"
                              ? t.group?.name || "—"
                              : t.group?.standard?.name || "—";
                          const sorted = [...thresholds].sort((a, b) => {
                            const ak = String(sectionKey(a));
                            const bk = String(sectionKey(b));
                            if (ak !== bk) return ak.localeCompare(bk);
                            return (a.sortOrder || 0) - (b.sortOrder || 0);
                          });
                          const out = [];
                          let lastKey = null;
                          sorted.forEach((t) => {
                            const k = sectionKey(t);
                            if (k !== lastKey) {
                              out.push(
                                <TableRow key={`section-${k}`}>
                                  <TableCell
                                    colSpan={8}
                                    style={{
                                      background: "var(--cds-layer-01)",
                                      fontSize: "0.75rem",
                                      fontWeight: 700,
                                      textTransform: "uppercase",
                                      letterSpacing: "0.04em",
                                      color: "var(--cds-text-02)",
                                      padding: "0.375rem 0.75rem",
                                    }}
                                  >
                                    {sectionLabel(t)}
                                  </TableCell>
                                </TableRow>,
                              );
                              lastKey = k;
                            }
                            out.push(renderThresholdRow(t));
                          });
                          return out;
                        })()}
                      </TableBody>
                    </Table>
                  </div>
                ) : null}
              </div>
            </div>
          </>
        )}
      </div>

      <Modal
        open={!!unlinkTarget}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "compliance.testEditor.unlinkHeading",
          defaultMessage: "Unlink Threshold",
        })}
        primaryButtonText={intl.formatMessage({
          id: "compliance.testEditor.unlink",
          defaultMessage: "Unlink",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        onRequestClose={() => setUnlinkTarget(null)}
        onRequestSubmit={handleConfirmUnlink}
      >
        <p>
          <FormattedMessage
            id="compliance.testEditor.unlinkPrompt"
            defaultMessage="Remove this threshold from {test}? The threshold itself stays on the standard's parameter group; only the link to this specific test is removed."
            values={{ test: <strong>{selectedTest?.value || ""}</strong> }}
          />
        </p>
      </Modal>
    </>
  );
}

export default TestComplianceThresholds;
