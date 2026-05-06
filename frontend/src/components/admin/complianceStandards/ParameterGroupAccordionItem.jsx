import React, { useContext, useEffect, useState } from "react";
import {
  AccordionItem,
  Stack,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Modal,
  TextInput,
  TextArea,
} from "@carbon/react";
import {
  Add,
  Edit,
  ChevronUp,
  ChevronDown,
  TrashCan,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
  deleteFromOpenElisServer,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import MultiLimitForm, { LIMIT_TYPES } from "./MultiLimitForm";
import SelectMapForm from "./SelectMapForm";
import LinkTestForm from "./LinkTestForm";

const TAG_BY_TYPE = {
  MAXIMUM: "red",
  MINIMUM: "blue",
  RANGE: "teal",
  BORDERLINE: "warm-gray",
  DESCRIPTIVE: "purple",
  EXACT: "cyan",
  SELECT_MAP: "purple",
};

const STATUS_TAG_BY_VALUE = {
  COMPLIANT: { tag: "green", icon: "✓" },
  BORDERLINE: { tag: "warm-gray", icon: "🚩" },
  NON_COMPLIANT: { tag: "red", icon: "✗" },
};

function formatLimitBadge(t) {
  const unit = t.units ? ` ${t.units}` : "";
  switch (t.thresholdType) {
    case "MAXIMUM":
      return `≤ ${t.maxValue ?? "—"}${unit}`;
    case "MINIMUM":
      return `≥ ${t.minValue ?? "—"}${unit}`;
    case "RANGE":
      return `${t.minValue ?? "—"}–${t.maxValue ?? "—"}${unit}`;
    case "BORDERLINE":
      return `${t.minValue ?? "—"}–${t.maxValue ?? "—"}${unit} 🚩`;
    case "EXACT":
      return `${t.targetValue ?? "—"}${unit}`;
    case "DESCRIPTIVE":
      return t.valueDescriptive || "—";
    default:
      return "—";
  }
}

function buildThresholdPayload(group, test, lt, vals) {
  return {
    group: { id: group.id },
    test: test ? { id: test.id } : null,
    parameterCode: test ? test.code || test.value : null,
    displayName: test ? test.value : null,
    thresholdType: lt.type,
    minValue: lt.lower !== "" && lt.lower !== undefined ? lt.lower : null,
    maxValue: lt.upper !== "" && lt.upper !== undefined ? lt.upper : null,
    units: lt.unit || null,
    notes: lt.note || null,
    valueDescriptive: lt.descriptive || null,
    isMandatory: false,
  };
}

function ParameterGroupAccordionItem({
  standardId,
  group,
  standardSampleTypes,
  onDelete,
  onEdit,
  onMoveUp,
  onMoveDown,
  canMoveUp,
  canMoveDown,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  // Toast on every threshold action (link/save/unlink for numeric and
  // select-map flavours). Per-row error attribution on the multi-limit
  // form is preserved via the returned `errors` map; the toast is the
  // overall confirmation/banner-level signal.
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
  const [thresholds, setThresholds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedTestKey, setExpandedTestKey] = useState(null);
  const [addingTest, setAddingTest] = useState(false);
  const [showUnlinkModal, setShowUnlinkModal] = useState(false);
  const [unlinkTarget, setUnlinkTarget] = useState(null);
  const [testSampleTypes, setTestSampleTypes] = useState({});
  const [showDeleteGroupModal, setShowDeleteGroupModal] = useState(false);
  // FR-2-003: inline rename + description edit for the group header. The
  // editor takes the same shape as the add-form below — local draft state,
  // confirm/cancel buttons.
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState(group.name || "");
  const [draftDescription, setDraftDescription] = useState(
    group.description || "",
  );

  const reload = () => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/compliance/thresholds?groupId=${encodeURIComponent(group.id)}`,
      (data) => {
        setThresholds(Array.isArray(data) ? data : []);
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [standardId, group.id]);

  // Resolve real per-test sample types from the compliance test-catalog
  // endpoint. Falls back to the standard's declared sampleTypes if the
  // entry has none.
  useEffect(() => {
    getFromOpenElisServer("/rest/compliance/test-catalog", (data) => {
      if (!Array.isArray(data)) return;
      const map = {};
      data.forEach((t) => {
        if (t && t.id != null) {
          map[String(t.id)] = Array.isArray(t.sampleTypes)
            ? t.sampleTypes
            : null;
        }
      });
      setTestSampleTypes(map);
    });
  }, []);

  const testsInGroup = (() => {
    const map = new Map();
    thresholds.forEach((t) => {
      const key = t.test?.id ? String(t.test.id) : t.parameterCode || "?";
      if (!map.has(key)) {
        map.set(key, {
          testId: t.test?.id ? String(t.test.id) : null,
          testName: t.displayName || t.parameterCode,
          parameterCode: t.parameterCode,
          rows: [],
        });
      }
      map.get(key).rows.push(t);
    });
    return Array.from(map.values());
  })();

  const handleSaveNumericForExisting = (testKey, limits) => {
    const existing = testsInGroup.find(
      (t) => (t.testId || t.parameterCode) === testKey,
    );
    if (!existing) return Promise.resolve({});
    const test = {
      id: existing.testId,
      code: existing.parameterCode,
      value: existing.testName,
    };
    const previousByType = {};
    existing.rows.forEach((r) => {
      previousByType[r.thresholdType] = r;
    });

    // Tag each promise with its threshold type so we can attribute errors
    // back to the right row in the form.
    const tasks = [];
    LIMIT_TYPES.forEach((lt) => {
      const incoming = limits.find((l) => l.type === lt.type);
      const prior = previousByType[lt.type];
      if (incoming && prior) {
        const payload = {
          ...prior,
          ...buildThresholdPayload(group, test, incoming, incoming),
          id: prior.id,
        };
        tasks.push({
          type: lt.type,
          op: "put",
          run: () =>
            new Promise((resolve) =>
              putToOpenElisServerFullResponse(
                `/rest/compliance/thresholds/${prior.id}`,
                JSON.stringify(payload),
                resolve,
              ),
            ),
        });
      } else if (incoming && !prior) {
        const payload = buildThresholdPayload(group, test, incoming, incoming);
        tasks.push({
          type: lt.type,
          op: "post",
          run: () =>
            new Promise((resolve) =>
              postToOpenElisServerJsonResponse(
                "/rest/compliance/thresholds",
                JSON.stringify(payload),
                resolve,
              ),
            ),
        });
      } else if (!incoming && prior) {
        tasks.push({
          type: lt.type,
          op: "delete",
          run: () =>
            new Promise((resolve) =>
              deleteFromOpenElisServer(
                `/rest/compliance/thresholds/${prior.id}`,
                resolve,
              ),
            ),
        });
      }
    });
    return Promise.all(tasks.map((t) => t.run().then((r) => [t, r]))).then(
      (results) => {
        const errors = {};
        results.forEach(([t, resp]) => {
          if (t.op === "post") {
            if (!resp || resp.error || !resp.id) {
              errors[t.type] =
                (resp && (resp.error || resp.message)) ||
                intl.formatMessage({
                  id: "compliance.threshold.saveError",
                  defaultMessage: "Could not save this row.",
                });
            }
          } else if (t.op === "put") {
            if (!resp || (resp.ok === false && resp.status >= 400)) {
              errors[t.type] = intl.formatMessage({
                id: "compliance.threshold.saveError",
                defaultMessage: "Could not save this row.",
              });
            }
          } else if (t.op === "delete") {
            if (!resp || (typeof resp === "number" && resp >= 400)) {
              errors[t.type] = intl.formatMessage({
                id: "compliance.threshold.deleteError",
                defaultMessage: "Could not remove this row.",
              });
            }
          }
        });
        if (Object.keys(errors).length === 0) {
          setExpandedTestKey(null);
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Saved",
            "compliance.threshold.savedThresholds",
            "Compliance thresholds linked to the test.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.threshold.savedPartialError",
            "Some thresholds could not be saved. Review the highlighted rows.",
          );
        }
        reload();
        return errors;
      },
    );
  };

  const handleSaveSelectForExisting = (testKey, valueMap) => {
    const existing = testsInGroup.find(
      (t) => (t.testId || t.parameterCode) === testKey,
    );
    if (!existing) return;
    const parent = existing.rows.find((r) => r.thresholdType === "SELECT_MAP");
    if (!parent) return;
    const updated = {
      ...parent,
      valueMappings: Object.entries(valueMap).map(([opt, status]) => ({
        optionValue: opt,
        complianceStatus: status,
      })),
    };
    putToOpenElisServerFullResponse(
      `/rest/compliance/thresholds/${parent.id}`,
      updated,
      (response) => {
        const ok = response && response.ok;
        if (ok) {
          setExpandedTestKey(null);
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Saved",
            "compliance.threshold.savedValueMap",
            "Value-mapping threshold saved.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.threshold.saveError",
            "Could not save this row.",
          );
        }
        reload();
      },
    );
  };

  const handleLinkNumeric = (test, limits) => {
    const tasks = limits.map((lt) => ({
      type: lt.type,
      run: () => {
        const payload = buildThresholdPayload(group, test, lt, lt);
        return new Promise((resolve) =>
          postToOpenElisServerJsonResponse(
            "/rest/compliance/thresholds",
            JSON.stringify(payload),
            resolve,
          ),
        );
      },
    }));
    return Promise.all(tasks.map((t) => t.run().then((r) => [t, r]))).then(
      (results) => {
        const errors = {};
        results.forEach(([t, resp]) => {
          if (!resp || resp.error || !resp.id) {
            errors[t.type] =
              (resp && (resp.error || resp.message)) ||
              intl.formatMessage({
                id: "compliance.threshold.saveError",
                defaultMessage: "Could not save this row.",
              });
          }
        });
        if (Object.keys(errors).length === 0) {
          setAddingTest(false);
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Linked",
            "compliance.threshold.testLinked",
            "Test linked to the parameter group.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.threshold.savedPartialError",
            "Some thresholds could not be saved. Review the highlighted rows.",
          );
        }
        reload();
        return errors;
      },
    );
  };

  const handleLinkSelect = (test, valueMap) => {
    const payload = {
      group: { id: group.id },
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
        if (resp && resp.id && !resp.error) {
          setAddingTest(false);
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Linked",
            "compliance.threshold.testLinked",
            "Test linked to the parameter group.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.threshold.saveError",
            "Could not save this row.",
          );
        }
        reload();
      },
    );
  };

  const handleUnlinkConfirm = () => {
    if (!unlinkTarget) return;
    const target = testsInGroup.find(
      (t) => (t.testId || t.parameterCode) === unlinkTarget,
    );
    if (!target) {
      setShowUnlinkModal(false);
      return;
    }
    const promises = target.rows.map(
      (r) =>
        new Promise((resolve) =>
          deleteFromOpenElisServer(
            `/rest/compliance/thresholds/${r.id}`,
            resolve,
          ),
        ),
    );
    Promise.all(promises).then((results) => {
      setShowUnlinkModal(false);
      setUnlinkTarget(null);
      if (expandedTestKey === (target.testId || target.parameterCode)) {
        setExpandedTestKey(null);
      }
      // deleteFromOpenElisServer fires its callback regardless of HTTP
      // status. Treat anything non-2xx as a failure so an audit-blocking
      // BR-002 / BR-003 conflict (409) doesn't silently disappear.
      const allOk = results.every(
        (r) => !r || r === true || (r.status && r.status < 300),
      );
      if (allOk) {
        toast(
          NotificationKinds.success,
          "notification.title.success",
          "Removed",
          "compliance.linkedTest.unlinked",
          "Test removed from the parameter group.",
        );
      } else {
        toast(
          NotificationKinds.error,
          "notification.title.error",
          "Error",
          "compliance.linkedTest.unlinkError",
          "Could not remove this test from the group.",
        );
      }
      reload();
    });
  };

  return (
    <>
      <AccordionItem
        title={
          <Stack
            orientation="horizontal"
            gap={3}
            style={{ alignItems: "center" }}
          >
            <span style={{ fontWeight: 500 }}>{group.name}</span>
            <Tag size="sm" type="gray">
              {testsInGroup.length}{" "}
              <FormattedMessage
                id="compliance.label.tests"
                defaultMessage="tests"
              />
            </Tag>
          </Stack>
        }
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            gap: "0.5rem",
            marginBottom: "0.75rem",
            flexWrap: "wrap",
          }}
        >
          {group.description ? (
            <p
              style={{
                margin: 0,
                color: "var(--cds-text-02)",
                fontSize: "0.875rem",
                flex: 1,
              }}
            >
              {group.description}
            </p>
          ) : (
            <span />
          )}
          {onMoveUp && (
            <Button
              kind="ghost"
              size="sm"
              hasIconOnly
              renderIcon={ChevronUp}
              iconDescription={intl.formatMessage({
                id: "compliance.parameterGroup.moveUp",
                defaultMessage: "Move up",
              })}
              disabled={!canMoveUp}
              onClick={() => onMoveUp()}
            />
          )}
          {onMoveDown && (
            <Button
              kind="ghost"
              size="sm"
              hasIconOnly
              renderIcon={ChevronDown}
              iconDescription={intl.formatMessage({
                id: "compliance.parameterGroup.moveDown",
                defaultMessage: "Move down",
              })}
              disabled={!canMoveDown}
              onClick={() => onMoveDown()}
            />
          )}
          {onEdit && (
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Edit}
              onClick={() => {
                setDraftName(group.name || "");
                setDraftDescription(group.description || "");
                setEditing(true);
              }}
            >
              <FormattedMessage
                id="compliance.parameterGroup.editBtn"
                defaultMessage="Edit"
              />
            </Button>
          )}
          {onDelete && (
            <Button
              kind="danger--ghost"
              size="sm"
              renderIcon={TrashCan}
              onClick={() => setShowDeleteGroupModal(true)}
            >
              <FormattedMessage
                id="compliance.parameterGroup.deleteBtn"
                defaultMessage="Delete group"
              />
            </Button>
          )}
        </div>

        {editing && onEdit && (
          <div
            style={{
              marginTop: "0.5rem",
              padding: "0.75rem",
              border: "1px solid var(--cds-border-subtle)",
              borderRadius: "4px",
              background: "var(--cds-layer-01)",
            }}
          >
            <Stack gap={3}>
              <TextInput
                id={`pg-edit-name-${group.id}`}
                labelText={intl.formatMessage({
                  id: "compliance.parameterGroup.name",
                  defaultMessage: "Group name",
                })}
                value={draftName}
                onChange={(e) => setDraftName(e.target.value)}
              />
              <TextArea
                id={`pg-edit-desc-${group.id}`}
                labelText={intl.formatMessage({
                  id: "compliance.parameterGroup.description",
                  defaultMessage: "Description",
                })}
                value={draftDescription}
                onChange={(e) => setDraftDescription(e.target.value)}
                rows={2}
              />
              <Stack orientation="horizontal" gap={2}>
                <Button
                  kind="primary"
                  size="sm"
                  onClick={() => {
                    if (!draftName.trim()) return;
                    onEdit({
                      ...group,
                      name: draftName.trim(),
                      description: draftDescription.trim(),
                    });
                    setEditing(false);
                  }}
                >
                  <FormattedMessage
                    id="label.button.save"
                    defaultMessage="Save"
                  />
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => setEditing(false)}
                >
                  <FormattedMessage
                    id="label.button.cancel"
                    defaultMessage="Cancel"
                  />
                </Button>
              </Stack>
            </Stack>
          </div>
        )}

        {loading ? (
          <p style={{ fontSize: "0.875rem", color: "var(--cds-text-02)" }}>
            <FormattedMessage
              id="label.loading.message"
              defaultMessage="Loading…"
            />
          </p>
        ) : testsInGroup.length > 0 ? (
          <Table size="sm" style={{ marginBottom: "1rem" }}>
            <TableHead>
              <TableRow>
                <TableHeader>
                  <FormattedMessage
                    id="compliance.linkedTest.name"
                    defaultMessage="Test"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="compliance.linkedTest.sampleTypes"
                    defaultMessage="Sample Types"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="compliance.linkedTest.limits"
                    defaultMessage="Limits Configured"
                  />
                </TableHeader>
                <TableHeader style={{ width: "5rem" }}> </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {testsInGroup.map((entry) => {
                const key = entry.testId || entry.parameterCode;
                const isSelectMap = entry.rows.some(
                  (r) => r.thresholdType === "SELECT_MAP",
                );
                const selectMapRow = entry.rows.find(
                  (r) => r.thresholdType === "SELECT_MAP",
                );
                return (
                  <React.Fragment key={key}>
                    <TableRow>
                      <TableCell>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            flexWrap: "wrap",
                          }}
                        >
                          <span style={{ fontWeight: 500 }}>
                            {entry.testName}
                          </span>
                          {isSelectMap && (
                            <Tag size="sm" type="purple">
                              <FormattedMessage
                                id="compliance.linkTest.selectListBadge"
                                defaultMessage="Select List"
                              />
                            </Tag>
                          )}
                        </div>
                        {entry.parameterCode && (
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "var(--cds-text-02)",
                            }}
                          >
                            {entry.parameterCode}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        {(() => {
                          const types =
                            (entry.testId && testSampleTypes[entry.testId]) ||
                            standardSampleTypes ||
                            [];
                          if (!types || types.length === 0) {
                            return (
                              <em
                                style={{
                                  fontSize: "0.75rem",
                                  color: "var(--cds-text-placeholder)",
                                }}
                              >
                                —
                              </em>
                            );
                          }
                          return (
                            <Stack
                              orientation="horizontal"
                              gap={1}
                              style={{ flexWrap: "wrap" }}
                            >
                              {types.map((st) => (
                                <Tag key={st} size="sm" type="blue">
                                  {st}
                                </Tag>
                              ))}
                            </Stack>
                          );
                        })()}
                      </TableCell>
                      <TableCell>
                        {isSelectMap ? (
                          <Stack
                            orientation="horizontal"
                            gap={2}
                            style={{ flexWrap: "wrap" }}
                          >
                            {(selectMapRow?.valueMappings || []).map((m) => {
                              const meta = STATUS_TAG_BY_VALUE[
                                m.complianceStatus
                              ] || {
                                tag: "gray",
                                icon: "?",
                              };
                              // Borderline gets the yellow-tint look from the
                              // v2.3 mockup (.smb-borderline). Carbon ships no
                              // yellow Tag type, so paint it inline.
                              const isBorderline =
                                m.complianceStatus === "BORDERLINE";
                              return (
                                <Tag
                                  key={m.optionValue}
                                  size="sm"
                                  type={meta.tag}
                                  style={
                                    isBorderline
                                      ? {
                                          background: "#fffbf0",
                                          color: "#744c00",
                                          border: "1px solid #f1c21b",
                                        }
                                      : undefined
                                  }
                                >
                                  {m.optionValue} {meta.icon}
                                </Tag>
                              );
                            })}
                          </Stack>
                        ) : entry.rows.length === 0 ? (
                          <span
                            style={{
                              fontSize: "0.75rem",
                              color: "var(--cds-text-placeholder)",
                              fontStyle: "italic",
                            }}
                          >
                            <FormattedMessage
                              id="compliance.linkedTest.noLimits"
                              defaultMessage="No limits configured"
                            />
                          </span>
                        ) : (
                          <Stack
                            orientation="horizontal"
                            gap={2}
                            style={{ flexWrap: "wrap" }}
                          >
                            {entry.rows.map((r) => (
                              <Tag
                                key={r.id}
                                size="sm"
                                type={TAG_BY_TYPE[r.thresholdType] || "gray"}
                              >
                                {formatLimitBadge(r)}
                              </Tag>
                            ))}
                          </Stack>
                        )}
                      </TableCell>
                      <TableCell>
                        <Stack orientation="horizontal" gap={1}>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            iconDescription={intl.formatMessage({
                              id: "compliance.linkedTest.editLimits",
                              defaultMessage: "Edit limits",
                            })}
                            renderIcon={
                              expandedTestKey === key ? ChevronUp : Edit
                            }
                            onClick={() =>
                              setExpandedTestKey((prev) =>
                                prev === key ? null : key,
                              )
                            }
                          />
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            iconDescription={intl.formatMessage({
                              id: "compliance.linkedTest.unlink",
                              defaultMessage: "Remove from group",
                            })}
                            renderIcon={TrashCan}
                            onClick={() => {
                              setUnlinkTarget(key);
                              setShowUnlinkModal(true);
                            }}
                          />
                        </Stack>
                      </TableCell>
                    </TableRow>

                    {expandedTestKey === key && (
                      <TableRow>
                        <TableCell
                          colSpan={4}
                          style={{
                            background: "#edf5ff",
                            borderLeft: "3px solid #0f62fe",
                            padding: "1rem 1.25rem",
                          }}
                        >
                          {/* Inline-edit header: matches the v2.3 mockup's
                              "Turbidity TURB-01 [Water] [editing all limits]"
                              chip row above the form fields. */}
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.5rem",
                              marginBottom: "0.75rem",
                              flexWrap: "wrap",
                            }}
                          >
                            <strong style={{ fontSize: "0.875rem" }}>
                              {entry.testName}
                            </strong>
                            {entry.parameterCode && (
                              <span
                                style={{
                                  fontSize: "0.75rem",
                                  color: "var(--cds-text-02)",
                                }}
                              >
                                {entry.parameterCode}
                              </span>
                            )}
                            {(() => {
                              const types =
                                (entry.testId &&
                                  testSampleTypes[entry.testId]) ||
                                standardSampleTypes ||
                                [];
                              return types.map((st) => (
                                <Tag key={st} size="sm" type="blue">
                                  {st}
                                </Tag>
                              ));
                            })()}
                            {isSelectMap && (
                              <Tag size="sm" type="purple">
                                <FormattedMessage
                                  id="compliance.linkTest.selectListBadge"
                                  defaultMessage="Select List"
                                />
                              </Tag>
                            )}
                            <span
                              style={{
                                fontSize: "0.75rem",
                                color: "var(--cds-text-02)",
                                background: "#fff",
                                padding: "0.125rem 0.5rem",
                                borderRadius: "2px",
                                border: "1px solid #0f62fe33",
                              }}
                            >
                              {isSelectMap ? (
                                <FormattedMessage
                                  id="compliance.inlineEdit.editingMapping"
                                  defaultMessage="editing compliance mapping"
                                />
                              ) : (
                                <FormattedMessage
                                  id="compliance.inlineEdit.editingLimits"
                                  defaultMessage="editing all limits"
                                />
                              )}
                            </span>
                          </div>

                          {isSelectMap ? (
                            <SelectMapForm
                              testCode={entry.parameterCode || key}
                              testName={entry.testName}
                              options={(selectMapRow?.valueMappings || []).map(
                                (m) => m.optionValue,
                              )}
                              existingMap={Object.fromEntries(
                                (selectMapRow?.valueMappings || []).map((m) => [
                                  m.optionValue,
                                  m.complianceStatus,
                                ]),
                              )}
                              onSave={(valueMap) =>
                                handleSaveSelectForExisting(key, valueMap)
                              }
                              onCancel={() => setExpandedTestKey(null)}
                            />
                          ) : (
                            <MultiLimitForm
                              testCode={entry.parameterCode || key}
                              testName={entry.testName}
                              unit={entry.rows[0]?.units || ""}
                              existingLimits={entry.rows.map((r) => ({
                                type: r.thresholdType,
                                lower: r.minValue ?? "",
                                upper: r.maxValue ?? "",
                                unit: r.units ?? "",
                                note: r.notes ?? "",
                                descriptive: r.valueDescriptive ?? "",
                              }))}
                              onSave={(limits) =>
                                handleSaveNumericForExisting(key, limits)
                              }
                              onCancel={() => setExpandedTestKey(null)}
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>
        ) : (
          <p
            style={{
              marginBottom: "1rem",
              color: "var(--cds-text-placeholder)",
              fontSize: "0.875rem",
              fontStyle: "italic",
            }}
          >
            <FormattedMessage
              id="compliance.group.noTests"
              defaultMessage="No tests linked to this group yet. Link tests to define compliance thresholds for evaluation."
            />
          </p>
        )}

        {addingTest ? (
          <LinkTestForm
            standardSampleTypes={standardSampleTypes}
            groupId={group.id}
            alreadyLinkedTestIds={testsInGroup
              .map((t) => t.testId)
              .filter(Boolean)}
            onLinkNumeric={handleLinkNumeric}
            onLinkSelect={handleLinkSelect}
            onCancel={() => setAddingTest(false)}
          />
        ) : (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Add}
            onClick={() => setAddingTest(true)}
          >
            <FormattedMessage
              id="compliance.button.linkTest"
              defaultMessage="Link Test to Group"
            />
          </Button>
        )}
      </AccordionItem>

      <Modal
        open={showUnlinkModal}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "compliance.linkedTest.unlinkHeading",
          defaultMessage: "Remove Test from Group",
        })}
        primaryButtonText={intl.formatMessage({
          id: "compliance.linkedTest.unlinkConfirm",
          defaultMessage: "Remove",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        onRequestClose={() => setShowUnlinkModal(false)}
        onRequestSubmit={handleUnlinkConfirm}
      >
        <p>
          <FormattedMessage
            id="compliance.linkedTest.unlinkPrompt"
            defaultMessage="Remove this test from the group? All threshold definitions for this test in this group will be deleted."
          />
        </p>
      </Modal>

      <Modal
        open={showDeleteGroupModal}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "compliance.parameterGroup.deleteHeading",
          defaultMessage: "Delete Parameter Group",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.delete",
          defaultMessage: "Delete",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        onRequestClose={() => setShowDeleteGroupModal(false)}
        onRequestSubmit={() => {
          setShowDeleteGroupModal(false);
          if (onDelete) onDelete();
        }}
      >
        <p>
          <FormattedMessage
            id="compliance.parameterGroup.deletePrompt"
            defaultMessage="Delete the parameter group {name} and all of its thresholds? This cannot be undone."
            values={{ name: <strong>{group.name}</strong> }}
          />
        </p>
        {testsInGroup.length > 0 && (
          <p style={{ marginTop: "0.5rem", color: "var(--cds-text-02)" }}>
            <FormattedMessage
              id="compliance.parameterGroup.deleteWarning"
              defaultMessage="{count, plural, one {# test is} other {# tests are}} currently linked. All linked thresholds will be removed."
              values={{ count: testsInGroup.length }}
            />
          </p>
        )}
      </Modal>
    </>
  );
}

export default ParameterGroupAccordionItem;
