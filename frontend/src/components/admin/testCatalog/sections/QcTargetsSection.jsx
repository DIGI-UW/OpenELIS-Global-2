import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  ComboBox,
  InlineNotification,
  Loading,
  NumberInput,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Toggle,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { Link } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-1148 — QC Targets section.
 *
 * One row per control level (Low / Normal / High) for the test's primary
 * component: the expected value and its uncertainty (quantitative) or the
 * expected dictionary outcome (qualitative), plus per-lot overrides that link
 * existing control lots (never create them). Results Entry control capture
 * prefills from the lot override, else the level default. Targets are
 * deactivated, never deleted; the whole set is saved in one PUT.
 */
export const LEVELS = ["LOW", "NORMAL", "HIGH"];
export const QC_LOT_MANAGEMENT_PATH = "/analyzers/qc/control-lots";

const isBlank = (value) =>
  value === null || value === undefined || value === "";
const toNum = (value) => (isBlank(value) ? null : Number(value));

let keySeq = 0;
const nextKey = () => `new-${++keySeq}`;

const QcTargetsSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [meta, setMeta] = useState({
    quantitative: true,
    unit: "",
    lots: [],
    dictionaryOptions: [],
  });
  const [targets, setTargets] = useState([]);
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [expanded, setExpanded] = useState(null);
  const [overrideDraft, setOverrideDraft] = useState(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const load = () => {
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/qc-targets`,
      (res) => {
        if (!mounted.current) {
          return;
        }
        setLoading(false);
        if (!res || !Array.isArray(res.targets)) {
          setError(true);
          return;
        }
        setMeta({
          quantitative: res.quantitative !== false,
          unit: res.unit || "",
          lots: res.lots || [],
          dictionaryOptions: res.dictionaryOptions || [],
        });
        setTargets(res.targets.map((t) => ({ ...t, _key: t.id })));
      },
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  const isActive = (t) => t.active !== false;
  const visible = (t) => isActive(t) || showDeactivated;

  const levelDefault = (level) =>
    targets.find(
      (t) => t.controlLevel === level && !t.qcControlLotId && isActive(t),
    ) ||
    (showDeactivated
      ? targets.find((t) => t.controlLevel === level && !t.qcControlLotId)
      : undefined);

  const overridesOf = (level) =>
    targets.filter(
      (t) => t.controlLevel === level && !!t.qcControlLotId && visible(t),
    );

  const lotsFreeFor = (level) =>
    meta.lots.filter(
      (lot) =>
        !targets.some(
          (t) =>
            t.controlLevel === level &&
            t.qcControlLotId === lot.id &&
            isActive(t),
        ),
    );

  const patch = (key, changes) =>
    setTargets((prev) =>
      prev.map((t) => (t._key === key ? { ...t, ...changes } : t)),
    );

  const addDefault = (level) => {
    setTargets((prev) => [
      ...prev,
      {
        _key: nextKey(),
        controlLevel: level,
        qcControlLotId: null,
        expectedValue: "",
        uncertainty: "",
        expectedDictResultId: "",
        active: true,
      },
    ]);
    setExpanded(level);
  };

  const startOverride = (level) =>
    setOverrideDraft({
      level,
      lot: null,
      expectedValue: "",
      uncertainty: "",
      expectedDictResultId: "",
    });

  const commitOverride = () => {
    if (!overrideDraft || !overrideDraft.lot) {
      return;
    }
    setTargets((prev) => [
      ...prev,
      {
        _key: nextKey(),
        controlLevel: overrideDraft.level,
        qcControlLotId: overrideDraft.lot.id,
        lotLabel: overrideDraft.lot.label,
        expectedValue: overrideDraft.expectedValue,
        uncertainty: overrideDraft.uncertainty,
        expectedDictResultId: overrideDraft.expectedDictResultId,
        active: true,
      },
    ]);
    setOverrideDraft(null);
  };

  const validationError = () => {
    for (const t of targets) {
      if (!isActive(t)) {
        continue;
      }
      if (meta.quantitative) {
        if (isBlank(t.expectedValue) || isBlank(t.uncertainty)) {
          return "error.testCatalog.qcTargets.required";
        }
        if (Number(t.uncertainty) < 0) {
          return "error.testCatalog.qcTargets.uncertaintyNegative";
        }
      } else if (isBlank(t.expectedDictResultId)) {
        return "error.testCatalog.qcTargets.required";
      }
    }
    return null;
  };

  const notify = (kind, message) => {
    setNotificationVisible(true);
    addNotification({
      kind,
      title: intl.formatMessage({
        id: "admin.testCatalog.qcTargets.section.label",
      }),
      message,
    });
  };

  const handleSave = () => {
    const problem = validationError();
    if (problem) {
      notify("error", intl.formatMessage({ id: problem }));
      return;
    }
    setSaving(true);
    const payload = {
      testId,
      targets: targets.map((t) => ({
        id: t.id || null,
        componentId: t.componentId || null,
        controlLevel: t.controlLevel,
        qcControlLotId: t.qcControlLotId || null,
        expectedValue: meta.quantitative ? toNum(t.expectedValue) : null,
        uncertainty: meta.quantitative ? toNum(t.uncertainty) : null,
        expectedDictResultId: meta.quantitative
          ? null
          : t.expectedDictResultId || null,
        active: isActive(t),
      })),
    };
    putToOpenElisServerFullResponse(
      `/rest/test-catalog/tests/${testId}/qc-targets`,
      JSON.stringify(payload),
      (response) => {
        setSaving(false);
        if (response && response.status === 200) {
          notify(
            "success",
            intl.formatMessage({ id: "admin.testCatalog.qcTargets.saved" }),
          );
          setExpanded(null);
          setOverrideDraft(null);
          load();
        } else if (response && response.text) {
          response
            .text()
            .then((text) =>
              notify(
                "error",
                text || intl.formatMessage({ id: "server.error.msg" }),
              ),
            );
        } else {
          notify("error", intl.formatMessage({ id: "server.error.msg" }));
        }
      },
    );
  };

  const levelLabel = (level) =>
    intl.formatMessage({
      id: `admin.testCatalog.qcTargets.level.${level.toLowerCase()}`,
    });

  const outcomeName = (t) =>
    t.expectedDictResultName ||
    (meta.dictionaryOptions.find((o) => o.id === t.expectedDictResultId) || {})
      .name ||
    "";

  const targetSummary = (t) =>
    meta.quantitative
      ? `${isBlank(t.expectedValue) ? "—" : t.expectedValue} ± ${
          isBlank(t.uncertainty) ? "—" : t.uncertainty
        }`
      : outcomeName(t) || "—";

  const statusTag = (t) => (
    <Tag type={isActive(t) ? "green" : "cool-gray"} size="sm">
      <FormattedMessage
        id={
          isActive(t)
            ? "label.testCatalog.basicInfo.active"
            : "label.testCatalog.list.filter.inactive"
        }
      />
    </Tag>
  );

  const targetFields = (t, idPrefix) =>
    meta.quantitative ? (
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
        <NumberInput
          id={`${idPrefix}-expected`}
          data-testid={`${idPrefix}-expected`}
          label={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.column.expected",
          })}
          allowEmpty
          hideSteppers
          value={isBlank(t.expectedValue) ? "" : t.expectedValue}
          onChange={(_e, { value }) => patch(t._key, { expectedValue: value })}
        />
        <NumberInput
          id={`${idPrefix}-uncertainty`}
          data-testid={`${idPrefix}-uncertainty`}
          label={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.column.uncertainty",
          })}
          allowEmpty
          hideSteppers
          min={0}
          invalid={!isBlank(t.uncertainty) && Number(t.uncertainty) < 0}
          invalidText={intl.formatMessage({
            id: "error.testCatalog.qcTargets.uncertaintyNegative",
          })}
          value={isBlank(t.uncertainty) ? "" : t.uncertainty}
          onChange={(_e, { value }) => patch(t._key, { uncertainty: value })}
        />
      </div>
    ) : (
      <Select
        id={`${idPrefix}-outcome`}
        data-testid={`${idPrefix}-outcome`}
        labelText={intl.formatMessage({
          id: "admin.testCatalog.qcTargets.expectedOutcome.label",
        })}
        value={t.expectedDictResultId || ""}
        onChange={(e) =>
          patch(t._key, { expectedDictResultId: e.target.value })
        }
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.expectedOutcome.placeholder",
          })}
        />
        {meta.dictionaryOptions.map((o) => (
          <SelectItem key={o.id} value={o.id} text={o.name || o.id} />
        ))}
      </Select>
    );

  const draftFields = () =>
    meta.quantitative ? (
      <>
        <NumberInput
          id={`override-expected-${overrideDraft.level}`}
          data-testid={`qc-override-expected-${overrideDraft.level}`}
          label={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.column.expected",
          })}
          allowEmpty
          hideSteppers
          value={overrideDraft.expectedValue}
          onChange={(_e, { value }) =>
            setOverrideDraft((d) => ({ ...d, expectedValue: value }))
          }
        />
        <NumberInput
          id={`override-uncertainty-${overrideDraft.level}`}
          data-testid={`qc-override-uncertainty-${overrideDraft.level}`}
          label={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.column.uncertainty",
          })}
          allowEmpty
          hideSteppers
          min={0}
          value={overrideDraft.uncertainty}
          onChange={(_e, { value }) =>
            setOverrideDraft((d) => ({ ...d, uncertainty: value }))
          }
        />
      </>
    ) : (
      <Select
        id={`override-outcome-${overrideDraft.level}`}
        data-testid={`qc-override-outcome-${overrideDraft.level}`}
        labelText={intl.formatMessage({
          id: "admin.testCatalog.qcTargets.expectedOutcome.label",
        })}
        value={overrideDraft.expectedDictResultId}
        onChange={(e) =>
          setOverrideDraft((d) => ({
            ...d,
            expectedDictResultId: e.target.value,
          }))
        }
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "admin.testCatalog.qcTargets.expectedOutcome.placeholder",
          })}
        />
        {meta.dictionaryOptions.map((o) => (
          <SelectItem key={o.id} value={o.id} text={o.name || o.id} />
        ))}
      </Select>
    );

  const renderExpanded = (level) => {
    const current = levelDefault(level);
    const overrides = overridesOf(level);
    const freeLots = lotsFreeFor(level);
    return (
      <TableRow key={`${level}-editor`}>
        <TableCell colSpan={7}>
          <Stack gap={5} data-testid={`qc-target-editor-${level}`}>
            {current && targetFields(current, `qc-target-${level}`)}

            <div>
              <h6>
                <FormattedMessage id="admin.testCatalog.qcTargets.column.overrides" />
              </h6>
              {overrides.length === 0 ? (
                <p>
                  <FormattedMessage id="admin.testCatalog.qcTargets.overrides.none" />
                </p>
              ) : (
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="admin.testCatalog.qcTargets.lot.label" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="admin.testCatalog.qcTargets.column.expected" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="admin.testCatalog.qcTargets.column.status" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="admin.testCatalog.qcTargets.column.actions" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {overrides.map((o) => (
                      <TableRow
                        key={o._key}
                        data-testid={`qc-override-row-${level}-${o.qcControlLotId}`}
                      >
                        <TableCell>{o.lotLabel || o.qcControlLotId}</TableCell>
                        <TableCell>
                          {isActive(o)
                            ? targetFields(o, `qc-override-${level}-${o._key}`)
                            : targetSummary(o)}
                        </TableCell>
                        <TableCell>{statusTag(o)}</TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() =>
                              patch(o._key, { active: !isActive(o) })
                            }
                          >
                            <FormattedMessage
                              id={
                                isActive(o)
                                  ? "admin.testCatalog.qcTargets.deactivate"
                                  : "admin.testCatalog.qcTargets.reactivate"
                              }
                            />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}

              {overrideDraft && overrideDraft.level === level ? (
                <div
                  data-testid={`qc-override-draft-${level}`}
                  style={{
                    display: "flex",
                    gap: "1rem",
                    alignItems: "flex-end",
                    flexWrap: "wrap",
                    marginTop: "0.75rem",
                  }}
                >
                  <ComboBox
                    id={`override-lot-${level}`}
                    data-testid={`qc-override-lot-${level}`}
                    titleText={intl.formatMessage({
                      id: "admin.testCatalog.qcTargets.lot.label",
                    })}
                    placeholder={intl.formatMessage({
                      id: "admin.testCatalog.qcTargets.lot.placeholder",
                    })}
                    items={freeLots}
                    itemToString={(lot) => (lot ? lot.label : "")}
                    selectedItem={overrideDraft.lot}
                    onChange={({ selectedItem }) =>
                      setOverrideDraft((d) => ({ ...d, lot: selectedItem }))
                    }
                  />
                  {freeLots.length === 0 && (
                    <Link to={QC_LOT_MANAGEMENT_PATH}>
                      <FormattedMessage id="admin.testCatalog.qcTargets.lot.emptyLink" />
                    </Link>
                  )}
                  {draftFields()}
                  <Button
                    kind="tertiary"
                    size="sm"
                    data-testid={`qc-override-add-${level}`}
                    disabled={!overrideDraft.lot}
                    onClick={commitOverride}
                  >
                    <FormattedMessage id="label.button.add" />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => setOverrideDraft(null)}
                  >
                    <FormattedMessage id="label.button.cancel" />
                  </Button>
                </div>
              ) : (
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Add}
                  data-testid={`qc-override-start-${level}`}
                  onClick={() => startOverride(level)}
                >
                  <FormattedMessage id="admin.testCatalog.qcTargets.addOverride" />
                </Button>
              )}
            </div>

            <div>
              <Button
                kind="secondary"
                size="sm"
                data-testid={`qc-target-done-${level}`}
                onClick={() => {
                  setExpanded(null);
                  setOverrideDraft(null);
                }}
              >
                <FormattedMessage id="admin.testCatalog.qcTargets.done" />
              </Button>
            </div>
          </Stack>
        </TableCell>
      </TableRow>
    );
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
          id: "admin.testCatalog.qcTargets.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="qc-targets-section">
      <p>
        <FormattedMessage id="admin.testCatalog.qcTargets.intro" />
      </p>
      <p data-testid="qc-targets-precedence">
        <FormattedMessage id="admin.testCatalog.qcTargets.helper.precedence" />
      </p>

      <Toggle
        id="qc-targets-show-deactivated"
        data-testid="qc-targets-show-deactivated"
        size="sm"
        labelText={intl.formatMessage({
          id: "admin.testCatalog.qcTargets.showDeactivated",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={showDeactivated}
        onToggle={(checked) => setShowDeactivated(checked)}
      />

      <Table size="sm">
        <TableHead>
          <TableRow>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.level" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id={
                  meta.quantitative
                    ? "admin.testCatalog.qcTargets.column.expected"
                    : "admin.testCatalog.qcTargets.expectedOutcome.label"
                }
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.uncertainty" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.unit" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.overrides" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.status" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="admin.testCatalog.qcTargets.column.actions" />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {LEVELS.map((level) => {
            const current = levelDefault(level);
            const overrides = overridesOf(level);
            const rows = [
              <TableRow key={level} data-testid={`qc-target-row-${level}`}>
                <TableCell>{levelLabel(level)}</TableCell>
                {current ? (
                  <>
                    <TableCell data-testid={`qc-target-summary-${level}`}>
                      {meta.quantitative
                        ? isBlank(current.expectedValue)
                          ? "—"
                          : current.expectedValue
                        : outcomeName(current) || "—"}
                    </TableCell>
                    <TableCell>
                      {meta.quantitative && !isBlank(current.uncertainty)
                        ? `± ${current.uncertainty}`
                        : "—"}
                    </TableCell>
                  </>
                ) : (
                  <TableCell
                    colSpan={2}
                    data-testid={`qc-target-empty-${level}`}
                  >
                    <FormattedMessage id="admin.testCatalog.qcTargets.empty.level" />
                  </TableCell>
                )}
                <TableCell>{meta.unit || "—"}</TableCell>
                <TableCell data-testid={`qc-target-overrides-${level}`}>
                  {overrides.length === 0
                    ? intl.formatMessage({
                        id: "admin.testCatalog.qcTargets.overrides.none",
                      })
                    : `${intl.formatMessage(
                        { id: "admin.testCatalog.qcTargets.overrides.count" },
                        { count: overrides.length },
                      )}: ${overrides
                        .map((o) => o.lotLabel || o.qcControlLotId)
                        .join(", ")}`}
                </TableCell>
                <TableCell>{current ? statusTag(current) : "—"}</TableCell>
                <TableCell>
                  {current ? (
                    <>
                      <Button
                        kind="ghost"
                        size="sm"
                        data-testid={`qc-target-edit-${level}`}
                        onClick={() =>
                          setExpanded(expanded === level ? null : level)
                        }
                      >
                        <FormattedMessage id="admin.testCatalog.qcTargets.edit" />
                      </Button>
                      <Button
                        kind="ghost"
                        size="sm"
                        data-testid={`qc-target-toggle-${level}`}
                        onClick={() =>
                          patch(current._key, { active: !isActive(current) })
                        }
                      >
                        <FormattedMessage
                          id={
                            isActive(current)
                              ? "admin.testCatalog.qcTargets.deactivate"
                              : "admin.testCatalog.qcTargets.reactivate"
                          }
                        />
                      </Button>
                    </>
                  ) : (
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Add}
                      data-testid={`qc-target-add-${level}`}
                      onClick={() => addDefault(level)}
                    >
                      <FormattedMessage id="admin.testCatalog.qcTargets.addTarget" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>,
            ];
            if (expanded === level) {
              rows.push(renderExpanded(level));
            }
            return rows;
          })}
        </TableBody>
      </Table>

      <div>
        <Button
          kind="primary"
          data-testid="qc-targets-save"
          disabled={saving}
          onClick={handleSave}
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default QcTargetsSection;
