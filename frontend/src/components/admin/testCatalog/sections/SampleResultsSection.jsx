import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  Accordion,
  AccordionItem,
  TextInput,
  NumberInput,
  ComboBox,
  Select,
  SelectItem,
  TileGroup,
  RadioTile,
  Toggle,
  Checkbox,
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
import { Add, ArrowDown, ArrowUp, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M5 / OGC-749 — Sample & Results section.
 *
 * Edits a test's result components (OGC-962), each with its select-list options
 * (OGC-964) and interpretation rules (OGC-965). Components render in an
 * accordion (OGC-967). The whole tree is saved in one PUT to
 * /rest/test-catalog/tests/{id}/sample-results, which diff-reconciles
 * server-side (insert / update-by-id / soft-delete). Components can be reordered
 * (OGC-968), have a unit picked from the master list or created inline
 * (OGC-963, FR-29), and the whole config copied from another test (OGC-966).
 */
// The platform's full result-type set (TypeOfTestResultServiceImpl.ResultType):
// three common types, plus four advanced / legacy types kept available so a test
// saved as M/C/T/A stays editable without its type being downgraded (FR-28/FR-37).
const PRIMARY_RESULT_TYPES = ["N", "D", "R"];
const ADVANCED_RESULT_TYPES = ["M", "C", "T", "A"];

// A single component renders as one flat block (no accordion chrome); 2+
// components render as accordion panels (FR-34). PlainPanel is the flat wrapper —
// it ignores the accordion-only `open`/`title` props.
const PlainPanel = ({ children }) => <div>{children}</div>;

/**
 * Live result-entry preview (FR-35): renders a read-only representation of the
 * control a technician will see for this component's configuration, updating as
 * the admin edits. Disabled so nothing is entered/saved from it.
 */
const ResultEntryPreview = ({ component, uoms, intl }) => {
  const type = component.resultType || null;
  const label = component.label || component.code || "";
  const options = component.options || [];
  const unit = (uoms.find((u) => u.id === component.uomId) || {}).value || "";

  let control = null;
  if (type === null) {
    // FR-56 — the pre-seeded component carries no result type until the admin
    // explicitly picks one; say so instead of previewing a control that lies.
    control = (
      <p style={{ color: "var(--cds-text-secondary, #525252)" }}>
        {intl.formatMessage({
          id: "label.testCatalog.sampleResults.preview.noType",
        })}
      </p>
    );
  } else if (type === "N") {
    control = (
      <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
        <TextInput id="preview-n" labelText="" placeholder="0" disabled />
        {unit && <span>{unit}</span>}
      </div>
    );
  } else if (type === "T") {
    control = (
      <TextInput id="preview-t" labelText="" placeholder="1:10" disabled />
    );
  } else if (type === "R" || type === "A") {
    control = <TextInput id="preview-text" labelText="" disabled />;
  } else if (type === "D") {
    control = (
      <Select id="preview-d" labelText="" disabled>
        {options.map((o, i) => (
          <SelectItem key={i} value={String(i)} text={o.valueName || o.value} />
        ))}
      </Select>
    );
  } else if (type === "M" || type === "C") {
    control = (
      <div>
        {options.map((o, i) => (
          <Checkbox
            key={i}
            id={`preview-m-${i}`}
            labelText={o.valueName || o.value}
            disabled
          />
        ))}
      </div>
    );
  }

  return (
    <div
      style={{
        border: "1px solid var(--cds-border-subtle, #e0e0e0)",
        padding: "1rem",
        marginTop: "0.5rem",
      }}
      data-testid="result-entry-preview"
    >
      <strong>
        {intl.formatMessage({ id: "label.testCatalog.sampleResults.preview" })}
      </strong>
      <p style={{ color: "var(--cds-text-secondary, #525252)" }}>
        {intl.formatMessage({
          id: "label.testCatalog.sampleResults.previewHelper",
        })}
      </p>
      {label && <div className="cds--label">{label}</div>}
      {control}
    </div>
  );
};

const SampleResultsSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [components, setComponents] = useState([]);
  const [otherTests, setOtherTests] = useState([]);
  const [copyFromId, setCopyFromId] = useState("");
  const [uoms, setUoms] = useState([]);
  // Dictionary typeahead results + a reset counter (per component) so the ComboBox
  // clears its input after an option is added.
  const [optionSearch, setOptionSearch] = useState({});
  const [optionComboReset, setOptionComboReset] = useState({});
  // Which components have the "Advanced / legacy types" disclosure expanded
  // (FR-28). Keyed by component index; undefined falls back to "open iff the
  // component's current type is an advanced one" so an existing M/C/T/A test
  // shows its selected tile.
  const [advancedTypesOpen, setAdvancedTypesOpen] = useState({});
  // Inline "add new unit" form (FR-29): null = closed; otherwise
  // { ci, name, code, ucumCode, description } for the component at index ci.
  const [unitForm, setUnitForm] = useState(null);

  const load = () => {
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/sample-results`,
      (res) => {
        setLoading(false);
        if (!res || !Array.isArray(res.components)) {
          setError(true);
          return;
        }
        setComponents(res.components);
      },
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    load();
    getFromOpenElisServer("/rest/test-list", (res) => {
      if (Array.isArray(res)) {
        setOtherTests(res.filter((t) => t.id !== testId));
      }
    });
    getFromOpenElisServer("/rest/uom", (res) => {
      if (Array.isArray(res)) {
        setUoms(res);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  // FR-76 — changing the result type resets fields that don't apply to the new
  // type, so a component can't carry stale numeric units into a select-list type
  // (or leftover options into a numeric one). Returns the patch to apply.
  const typeAwareDefaults = (component, nextType) => {
    const patch = { resultType: nextType };
    const isSelectList = ["D", "M", "C"].includes(nextType);
    if (nextType !== "N") {
      // Numeric-only fields.
      patch.uomId = "";
      patch.significantDigits = null;
      patch.allowMultipleReadings = false;
    }
    if (!isSelectList) {
      // Select-list options are meaningless for non-list types.
      patch.options = [];
    }
    return patch;
  };

  // ── Immutable updaters ─────────────────────────────────────────────────────
  const patchComponent = (ci, patch) =>
    setComponents((prev) =>
      prev.map((c, i) => (i === ci ? { ...c, ...patch } : c)),
    );

  // FR-29: create a unit inline, then append it to the picker and select it on
  // the component whose form is open. Name is required.
  const saveUnit = () => {
    if (!unitForm || !unitForm.name || !unitForm.name.trim()) {
      return;
    }
    const ci = unitForm.ci;
    const payload = JSON.stringify({
      name: unitForm.name.trim(),
      code: unitForm.code,
      ucumCode: unitForm.ucumCode,
      description: unitForm.description,
    });
    postToOpenElisServerJsonResponse("/rest/uom", payload, (data) => {
      if (data && data.id) {
        setUoms((prev) => [...prev, { id: data.id, value: data.value }]);
        patchComponent(ci, { uomId: data.id });
        setUnitForm(null);
      } else {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "error.title" }),
          message: intl.formatMessage({ id: "server.error.msg" }),
        });
        setNotificationVisible(true);
      }
    });
  };

  // Accordion header for a component: label · code · result type (FR-34).
  const componentTitle = (c) => {
    const label =
      c.label ||
      c.code ||
      intl.formatMessage({
        id: "label.testCatalog.sampleResults.newComponent",
      });
    const parts = [label];
    if (c.code && c.code !== label) {
      parts.push(c.code);
    }
    parts.push(
      c.resultType
        ? intl.formatMessage({
            id: `label.testCatalog.sampleResults.resultType.${c.resultType}`,
          })
        : intl.formatMessage({
            id: "label.testCatalog.sampleResults.resultType.none",
          }),
    );
    return parts.join(" · ");
  };

  // FR-28: result type is chosen first, as three primary cards (Numeric,
  // Single-select, Free text) each with a one-line description; the four
  // specialised / legacy types (Multi-select, Cascading, Titer, Alpha) sit
  // behind an "Advanced / legacy types" disclosure so a test saved as one of
  // them stays editable without the type being silently dropped (FR-37).
  const renderTypeChooser = (c, ci) => {
    // No silent default (FR-56): an unset type shows no tile selected, and the
    // save requires an explicit choice.
    const current = c.resultType || null;
    const showAdvanced =
      advancedTypesOpen[ci] ?? ADVANCED_RESULT_TYPES.includes(current);
    const tile = (t) => (
      <RadioTile key={t} id={`comp-type-${ci}-${t}`} value={t}>
        <div className="cds--tile-content">
          <strong>
            {intl.formatMessage({
              id: `label.testCatalog.sampleResults.resultType.${t}`,
            })}
          </strong>
          <div style={{ fontSize: "0.75rem", marginTop: "0.25rem" }}>
            {intl.formatMessage({
              id: `label.testCatalog.sampleResults.resultTypeDesc.${t}`,
            })}
          </div>
        </div>
      </RadioTile>
    );
    return (
      <div>
        <TileGroup
          name={`comp-type-${ci}`}
          legend={intl.formatMessage({
            id: "label.testCatalog.sampleResults.resultType",
          })}
          valueSelected={current}
          onChange={(value) => patchComponent(ci, typeAwareDefaults(c, value))}
        >
          {PRIMARY_RESULT_TYPES.map(tile)}
          {showAdvanced && ADVANCED_RESULT_TYPES.map(tile)}
        </TileGroup>
        <Button
          kind="ghost"
          size="sm"
          onClick={() =>
            setAdvancedTypesOpen((prev) => ({ ...prev, [ci]: !showAdvanced }))
          }
        >
          {intl.formatMessage({
            id: showAdvanced
              ? "label.testCatalog.sampleResults.resultType.hideAdvanced"
              : "label.testCatalog.sampleResults.resultType.showAdvanced",
          })}
        </Button>
      </div>
    );
  };

  const addComponent = () =>
    setComponents((prev) => [
      ...prev,
      {
        // The first (only) component is the primary; its code is fixed to
        // PRIMARY (mirrored to the legacy test columns).
        code: prev.length === 0 ? "PRIMARY" : "",
        label: "",
        displayOrder: prev.length + 1,
        // The type is an explicit choice (FR-56/28) — no silent Numeric default.
        resultType: null,
        significantDigits: null,
        defaultResult: "",
        allowMultipleReadings: false,
        isPrimary: prev.length === 0,
        showOnReport: true,
        options: [],
        interpretations: [],
      },
    ]);

  // Exactly one component is primary. While one is marked, the other
  // components' Primary toggles are disabled — the current primary must be
  // unmarked first. Marking fixes the code to PRIMARY (and disables it);
  // unmarking frees the code back to the component's label.
  const togglePrimary = (ci, checked) =>
    setComponents((prev) =>
      prev.map((c, i) => {
        if (i !== ci) {
          return c;
        }
        if (checked) {
          return { ...c, isPrimary: true, code: "PRIMARY" };
        }
        return {
          ...c,
          isPrimary: false,
          code: c.code === "PRIMARY" ? c.label || "" : c.code,
        };
      }),
    );

  const removeComponent = (ci) =>
    setComponents((prev) => {
      const removed = prev[ci];
      const next = prev.filter((_, i) => i !== ci);
      // Removing the primary promotes the first remaining component.
      if (removed && removed.isPrimary && next.length > 0) {
        next[0] = { ...next[0], isPrimary: true, code: "PRIMARY" };
      }
      return next;
    });

  const moveComponent = (ci, dir) =>
    setComponents((prev) => {
      const ni = ci + dir;
      if (ni < 0 || ni >= prev.length) {
        return prev;
      }
      const next = [...prev];
      [next[ci], next[ni]] = [next[ni], next[ci]];
      // Renumber display order to match the new visual order.
      return next.map((comp, i) => ({ ...comp, displayOrder: i + 1 }));
    });

  const patchChild = (ci, key, ji, patch) =>
    setComponents((prev) =>
      prev.map((c, i) =>
        i === ci
          ? {
              ...c,
              [key]: c[key].map((row, j) =>
                j === ji ? { ...row, ...patch } : row,
              ),
            }
          : c,
      ),
    );

  // Live dictionary search for the "add option" typeahead, scoped per component.
  const searchDictionary = (ci, query) => {
    if (!query || !query.trim()) {
      setOptionSearch((prev) => ({ ...prev, [ci]: [] }));
      return;
    }
    getFromOpenElisServer(
      `/rest/test-catalog/dictionary?search=${encodeURIComponent(query.trim())}`,
      (res) => setOptionSearch((prev) => ({ ...prev, [ci]: res || [] })),
    );
  };

  // Add a dictionary-backed result option (stores the dictionary id in `value`,
  // its name in `valueName` for display) and reset the ComboBox input.
  const addDictionaryOption = (ci, item) => {
    if (!item || !item.id) {
      return;
    }
    setComponents((prev) =>
      prev.map((c, i) =>
        i === ci
          ? {
              ...c,
              options: [
                ...c.options,
                {
                  value: item.id,
                  valueName: item.name,
                  resultType: c.resultType,
                  sortOrder: c.options.length + 1,
                  normal: false,
                },
              ],
            }
          : c,
      ),
    );
    setOptionComboReset((prev) => ({ ...prev, [ci]: (prev[ci] || 0) + 1 }));
  };

  // FR-83 — add a blank, free-text option not backed by a dictionary entry. Its
  // `value` renders as an editable field (valueName stays null) so the admin can
  // type a one-off option without first curating a dictionary term.
  const addCustomOption = (ci) =>
    setComponents((prev) =>
      prev.map((c, i) =>
        i === ci
          ? {
              ...c,
              options: [
                ...c.options,
                {
                  value: "",
                  valueName: null,
                  resultType: c.resultType,
                  sortOrder: c.options.length + 1,
                  normal: false,
                },
              ],
            }
          : c,
      ),
    );

  const addInterpretation = (ci) =>
    setComponents((prev) =>
      prev.map((c, i) =>
        i === ci
          ? {
              ...c,
              interpretations: [
                ...c.interpretations,
                {
                  valueMatch: "",
                  text: "",
                  severity: "NORMAL",
                  displayOrder: c.interpretations.length + 1,
                },
              ],
            }
          : c,
      ),
    );

  const removeChild = (ci, key, ji) =>
    setComponents((prev) =>
      prev.map((c, i) =>
        i === ci ? { ...c, [key]: c[key].filter((_, j) => j !== ji) } : c,
      ),
    );

  // ── Save ───────────────────────────────────────────────────────────────────
  const toInt = (v) =>
    v === "" || v === null || v === undefined ? null : Number(v);

  const handleSave = () => {
    // Every component needs a label (FR-29); the code isn't a separate user field,
    // so default it to the label when left blank. Guide the user with a clear
    // message instead of surfacing the backend's 422.
    const normalized = components.map((c) => {
      const label = (c.label || "").trim();
      const code = (c.code || "").trim() || label;
      return { ...c, label, code };
    });
    if (normalized.some((c) => !c.label)) {
      setNotificationVisible(true);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "label.testCatalog.section.sample-results",
        }),
        message: intl.formatMessage({
          id: "label.testCatalog.sampleResults.labelRequired",
        }),
      });
      return;
    }
    // FR-56/59 — the result type is a required explicit choice; refuse the save
    // naming the component rather than persisting a typeless row.
    const untyped = normalized.find((c) => !c.resultType);
    if (untyped) {
      setNotificationVisible(true);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "label.testCatalog.section.sample-results",
        }),
        message: intl.formatMessage(
          { id: "label.testCatalog.sampleResults.resultTypeRequired" },
          { component: untyped.label || untyped.code },
        ),
      });
      return;
    }
    setSaving(true);
    const payload = {
      testId,
      components: normalized.map((c) => ({
        ...c,
        displayOrder: toInt(c.displayOrder),
        significantDigits: toInt(c.significantDigits),
        options: (c.options || []).map((o) => ({
          ...o,
          sortOrder: toInt(o.sortOrder),
        })),
        interpretations: (c.interpretations || []).map((it) => ({
          ...it,
          displayOrder: toInt(it.displayOrder),
        })),
      })),
    };
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/sample-results`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.sample-results",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.sampleResults.saved",
            }),
          });
          load(); // refresh with server-assigned ids
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

  const handleCopyFrom = () => {
    if (!copyFromId) {
      return;
    }
    postToOpenElisServerJsonResponse(
      `/rest/test-catalog/tests/${testId}/sample-results/copy-from/${copyFromId}`,
      JSON.stringify({}),
      (res) => {
        if (res) {
          setCopyFromId("");
          load();
          setNotificationVisible(true);
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.sample-results",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.sampleResults.copied",
            }),
          });
        }
      },
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
          id: "label.testCatalog.editor.loadError",
        })}
      />
    );
  }

  // One component → flat; several → accordion panels (FR-34).
  const multipleComponents = components.length > 1;
  const ListWrapper = multipleComponents ? Accordion : React.Fragment;
  const ItemWrapper = multipleComponents ? AccordionItem : PlainPanel;

  return (
    <Stack gap={6}>
      <p>
        <FormattedMessage id="label.testCatalog.sampleResults.purpose" />
      </p>
      <p style={{ color: "var(--cds-text-secondary, #525252)" }}>
        <FormattedMessage id="label.testCatalog.sampleResults.mostTestsOneResult" />
      </p>
      {components.length === 0 ? (
        <p>
          <FormattedMessage id="label.testCatalog.sampleResults.empty" />
        </p>
      ) : (
        <ListWrapper>
          {components.map((c, ci) => (
            <ItemWrapper
              key={c.id || `new-${ci}`}
              open
              title={componentTitle(c)}
            >
              <Stack gap={4}>
                <div style={{ display: "flex", gap: "0.5rem" }}>
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={ArrowUp}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.sampleResults.moveUp",
                    })}
                    disabled={ci === 0}
                    onClick={() => moveComponent(ci, -1)}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={ArrowDown}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.sampleResults.moveDown",
                    })}
                    disabled={ci === components.length - 1}
                    onClick={() => moveComponent(ci, 1)}
                  />
                </div>
                <Toggle
                  id={`comp-primary-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.isPrimary",
                  })}
                  labelA={intl.formatMessage({ id: "label.no" })}
                  labelB={intl.formatMessage({ id: "label.yes" })}
                  toggled={!!c.isPrimary}
                  disabled={!c.isPrimary && components.some((x) => x.isPrimary)}
                  onToggle={(checked) => togglePrimary(ci, checked)}
                />
                <Toggle
                  id={`comp-show-on-report-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.showOnReport",
                  })}
                  labelA={intl.formatMessage({ id: "label.no" })}
                  labelB={intl.formatMessage({ id: "label.yes" })}
                  toggled={c.isPrimary ? true : c.showOnReport !== false}
                  disabled={!!c.isPrimary}
                  onToggle={(checked) =>
                    patchComponent(ci, { showOnReport: checked })
                  }
                />
                <TextInput
                  id={`comp-code-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.code",
                  })}
                  value={c.isPrimary ? "PRIMARY" : c.code || ""}
                  disabled={!!c.isPrimary}
                  onChange={(e) => patchComponent(ci, { code: e.target.value })}
                />
                <TextInput
                  id={`comp-label-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.label",
                  })}
                  value={c.label || ""}
                  onChange={(e) =>
                    patchComponent(ci, { label: e.target.value })
                  }
                />
                {renderTypeChooser(c, ci)}
                {/* Unit + significant digits apply only to Numeric (FR-29). */}
                {c.resultType === "N" && (
                  <>
                    <ComboBox
                      id={`comp-uom-${ci}`}
                      titleText={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.uom",
                      })}
                      placeholder={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.uom.none",
                      })}
                      items={uoms}
                      itemToString={(u) => (u ? u.value : "")}
                      selectedItem={uoms.find((u) => u.id === c.uomId) || null}
                      onChange={({ selectedItem }) =>
                        patchComponent(ci, {
                          uomId: selectedItem ? selectedItem.id : "",
                        })
                      }
                    />
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Add}
                      data-testid={`add-unit-${ci}`}
                      onClick={() =>
                        setUnitForm({
                          ci,
                          name: "",
                          code: "",
                          ucumCode: "",
                          description: "",
                        })
                      }
                    >
                      <FormattedMessage id="label.testCatalog.sampleResults.uom.addNew" />
                    </Button>
                    {unitForm && unitForm.ci === ci && (
                      <div
                        data-testid={`add-unit-form-${ci}`}
                        style={{
                          border: "1px solid var(--cds-border-subtle, #e0e0e0)",
                          padding: "1rem",
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.75rem",
                        }}
                      >
                        <TextInput
                          id={`add-unit-name-${ci}`}
                          labelText={intl.formatMessage({
                            id: "label.testCatalog.sampleResults.uom.newName",
                          })}
                          value={unitForm.name}
                          onChange={(e) => {
                            const value = e.target.value;
                            setUnitForm((f) => ({ ...f, name: value }));
                          }}
                        />
                        <TextInput
                          id={`add-unit-code-${ci}`}
                          labelText={intl.formatMessage({
                            id: "label.testCatalog.sampleResults.uom.newCode",
                          })}
                          value={unitForm.code}
                          onChange={(e) => {
                            const value = e.target.value;
                            setUnitForm((f) => ({ ...f, code: value }));
                          }}
                        />
                        <TextInput
                          id={`add-unit-ucum-${ci}`}
                          labelText={intl.formatMessage({
                            id: "label.testCatalog.sampleResults.uom.newUcum",
                          })}
                          value={unitForm.ucumCode}
                          onChange={(e) => {
                            const value = e.target.value;
                            setUnitForm((f) => ({ ...f, ucumCode: value }));
                          }}
                        />
                        <TextInput
                          id={`add-unit-desc-${ci}`}
                          labelText={intl.formatMessage({
                            id: "label.testCatalog.sampleResults.uom.newDescription",
                          })}
                          value={unitForm.description}
                          onChange={(e) => {
                            const value = e.target.value;
                            setUnitForm((f) => ({ ...f, description: value }));
                          }}
                        />
                        <div style={{ display: "flex", gap: "0.5rem" }}>
                          <Button
                            kind="primary"
                            size="sm"
                            disabled={!unitForm.name.trim()}
                            onClick={saveUnit}
                          >
                            <FormattedMessage id="label.testCatalog.sampleResults.uom.saveNew" />
                          </Button>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => setUnitForm(null)}
                          >
                            <FormattedMessage id="label.button.cancel" />
                          </Button>
                        </div>
                      </div>
                    )}
                    <NumberInput
                      id={`comp-sigdig-${ci}`}
                      label={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.significantDigits",
                      })}
                      helperText={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.significantDigits.helper",
                      })}
                      min={0}
                      max={10}
                      allowEmpty
                      value={
                        c.significantDigits === null ||
                        c.significantDigits === undefined
                          ? ""
                          : c.significantDigits
                      }
                      onChange={(_e, { value }) =>
                        patchComponent(ci, {
                          significantDigits: value === "" ? null : value,
                        })
                      }
                    />
                  </>
                )}
                <TextInput
                  id={`comp-default-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.defaultResult",
                  })}
                  value={c.defaultResult || ""}
                  onChange={(e) =>
                    patchComponent(ci, { defaultResult: e.target.value })
                  }
                />
                <TextInput
                  id={`comp-order-${ci}`}
                  type="number"
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.displayOrder",
                  })}
                  value={c.displayOrder ?? ""}
                  onChange={(e) =>
                    patchComponent(ci, { displayOrder: e.target.value })
                  }
                />
                {c.resultType === "N" && (
                  <Toggle
                    id={`comp-multi-${ci}`}
                    labelText={intl.formatMessage({
                      id: "label.testCatalog.sampleResults.allowMultiple",
                    })}
                    labelA={intl.formatMessage({ id: "label.no" })}
                    labelB={intl.formatMessage({ id: "label.yes" })}
                    toggled={!!c.allowMultipleReadings}
                    onToggle={(checked) =>
                      patchComponent(ci, { allowMultipleReadings: checked })
                    }
                  />
                )}

                {/* Live result-entry preview (FR-35): the control a technician
                    will see for this component's type. Read-only. */}
                <ResultEntryPreview component={c} uoms={uoms} intl={intl} />

                {/* Select-list options apply only to select-list types (FR-30). */}
                {["D", "M", "C"].includes(c.resultType) && (
                  <>
                    {/* Select-list options (OGC-964) */}
                    <h6>
                      <FormattedMessage id="label.testCatalog.sampleResults.options" />
                    </h6>
                    <p
                      style={{
                        color: "var(--cds-text-secondary, #525252)",
                        fontSize: "0.75rem",
                      }}
                    >
                      <FormattedMessage id="label.testCatalog.sampleResults.option.sortOrder.helper" />
                    </p>
                    {(c.options || []).length === 0 && (
                      <InlineNotification
                        kind="info"
                        lowContrast
                        hideCloseButton
                        title={intl.formatMessage({
                          id: "label.testCatalog.sampleResults.options.empty",
                        })}
                      />
                    )}
                    <Table size="sm">
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.option.value" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.option.sortOrder" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.option.normal" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.actions" />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {(c.options || []).map((o, oi) => (
                          <TableRow key={o.id || `opt-${oi}`}>
                            <TableCell>
                              {o.valueName ? (
                                // Dictionary-backed option: show the entry name, not the
                                // raw dictionary id stored in `value`.
                                o.valueName
                              ) : (
                                <TextInput
                                  id={`opt-value-${ci}-${oi}`}
                                  labelText=""
                                  value={o.value || ""}
                                  onChange={(e) =>
                                    patchChild(ci, "options", oi, {
                                      value: e.target.value,
                                    })
                                  }
                                />
                              )}
                            </TableCell>
                            <TableCell>
                              <TextInput
                                id={`opt-order-${ci}-${oi}`}
                                type="number"
                                labelText=""
                                value={o.sortOrder ?? ""}
                                onChange={(e) =>
                                  patchChild(ci, "options", oi, {
                                    sortOrder: e.target.value,
                                  })
                                }
                              />
                            </TableCell>
                            <TableCell>
                              <Checkbox
                                id={`opt-normal-${ci}-${oi}`}
                                labelText=""
                                checked={!!o.normal}
                                onChange={(_e, { checked }) =>
                                  patchChild(ci, "options", oi, {
                                    normal: checked,
                                  })
                                }
                              />
                            </TableCell>
                            <TableCell>
                              <Button
                                kind="ghost"
                                size="sm"
                                hasIconOnly
                                renderIcon={TrashCan}
                                iconDescription={intl.formatMessage({
                                  id: "label.testCatalog.sampleResults.removeOption",
                                })}
                                onClick={() => removeChild(ci, "options", oi)}
                              />
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                    <ComboBox
                      key={`opt-add-${ci}-${optionComboReset[ci] || 0}`}
                      id={`opt-add-${ci}`}
                      titleText={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.addOption",
                      })}
                      placeholder={intl.formatMessage({
                        id: "label.testCatalog.sampleResults.searchDictionary",
                      })}
                      items={optionSearch[ci] || []}
                      itemToString={(item) => (item ? item.name : "")}
                      onInputChange={(text) => searchDictionary(ci, text)}
                      onChange={({ selectedItem }) =>
                        addDictionaryOption(ci, selectedItem)
                      }
                    />
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Add}
                      data-testid={`add-custom-option-${ci}`}
                      onClick={() => addCustomOption(ci)}
                    >
                      <FormattedMessage id="label.testCatalog.sampleResults.addCustomOption" />
                    </Button>
                  </>
                )}

                {/* Interpretations apply to Numeric & select-list types (FR-32). */}
                {["N", "D", "M", "C"].includes(c.resultType) && (
                  <>
                    <h6>
                      <FormattedMessage id="label.testCatalog.sampleResults.interpretations" />
                    </h6>
                    {(c.interpretations || []).length === 0 && (
                      <InlineNotification
                        kind="info"
                        lowContrast
                        hideCloseButton
                        title={intl.formatMessage({
                          id: "label.testCatalog.sampleResults.interpretations.empty",
                        })}
                      />
                    )}
                    <Table size="sm">
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.interp.valueMatch" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.interp.text" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.interp.severity" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="label.testCatalog.sampleResults.actions" />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {(c.interpretations || []).map((it, ii) => (
                          <TableRow key={it.id || `int-${ii}`}>
                            <TableCell>
                              {/* Value field adapts to the result type (FR-32):
                                  select-list components pick a configured option;
                                  numeric uses a free-text pattern (>N, N-M, exact). */}
                              {["D", "M", "C"].includes(c.resultType) ? (
                                <Select
                                  id={`int-match-${ci}-${ii}`}
                                  labelText=""
                                  value={it.valueMatch || ""}
                                  onChange={(e) =>
                                    patchChild(ci, "interpretations", ii, {
                                      valueMatch: e.target.value,
                                    })
                                  }
                                >
                                  <SelectItem
                                    value=""
                                    text={intl.formatMessage({
                                      id: "label.testCatalog.sampleResults.interp.selectValue",
                                    })}
                                  />
                                  {(c.options || []).map((o, oi) => (
                                    <SelectItem
                                      key={o.id || oi}
                                      value={o.value}
                                      text={o.valueName || o.value}
                                    />
                                  ))}
                                </Select>
                              ) : (
                                <TextInput
                                  id={`int-match-${ci}-${ii}`}
                                  labelText=""
                                  placeholder={intl.formatMessage({
                                    id: "label.testCatalog.sampleResults.interp.numericHint",
                                  })}
                                  value={it.valueMatch || ""}
                                  onChange={(e) =>
                                    patchChild(ci, "interpretations", ii, {
                                      valueMatch: e.target.value,
                                    })
                                  }
                                />
                              )}
                            </TableCell>
                            <TableCell>
                              <TextInput
                                id={`int-text-${ci}-${ii}`}
                                labelText=""
                                value={it.text || ""}
                                onChange={(e) =>
                                  patchChild(ci, "interpretations", ii, {
                                    text: e.target.value,
                                  })
                                }
                              />
                            </TableCell>
                            <TableCell>
                              <Select
                                id={`int-sev-${ci}-${ii}`}
                                labelText=""
                                value={it.severity || "NORMAL"}
                                onChange={(e) =>
                                  patchChild(ci, "interpretations", ii, {
                                    severity: e.target.value,
                                  })
                                }
                              >
                                {["NORMAL", "ABNORMAL", "CRITICAL"].map((s) => (
                                  <SelectItem
                                    key={s}
                                    value={s}
                                    text={intl.formatMessage({
                                      id: `label.testCatalog.sampleResults.severity.${s}`,
                                    })}
                                  />
                                ))}
                              </Select>
                            </TableCell>
                            <TableCell>
                              <Button
                                kind="ghost"
                                size="sm"
                                hasIconOnly
                                renderIcon={TrashCan}
                                iconDescription={intl.formatMessage({
                                  id: "label.testCatalog.sampleResults.removeInterpretation",
                                })}
                                onClick={() =>
                                  removeChild(ci, "interpretations", ii)
                                }
                              />
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Add}
                      onClick={() => addInterpretation(ci)}
                    >
                      <FormattedMessage id="label.testCatalog.sampleResults.addInterpretation" />
                    </Button>
                  </>
                )}

                <div>
                  <Button
                    kind="danger--tertiary"
                    size="sm"
                    renderIcon={TrashCan}
                    onClick={() => removeComponent(ci)}
                  >
                    <FormattedMessage id="label.testCatalog.sampleResults.removeComponent" />
                  </Button>
                </div>
              </Stack>
            </ItemWrapper>
          ))}
        </ListWrapper>
      )}

      <div style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}>
        <ComboBox
          id="copy-from-test"
          titleText={intl.formatMessage({
            id: "label.testCatalog.sampleResults.copyFrom",
          })}
          placeholder={intl.formatMessage({
            id: "label.testCatalog.sampleResults.copyFrom.placeholder",
          })}
          items={otherTests}
          itemToString={(t) => (t ? t.value : "")}
          selectedItem={otherTests.find((t) => t.id === copyFromId) || null}
          onChange={({ selectedItem }) =>
            setCopyFromId(selectedItem ? selectedItem.id : "")
          }
        />
        <Button
          kind="secondary"
          disabled={!copyFromId}
          onClick={handleCopyFrom}
        >
          <FormattedMessage id="label.testCatalog.sampleResults.copyFromButton" />
        </Button>
      </div>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button
          kind="tertiary"
          renderIcon={Add}
          onClick={addComponent}
          data-testid="add-component"
        >
          <FormattedMessage id="label.testCatalog.sampleResults.addComponent" />
        </Button>
        <Button kind="primary" disabled={saving} onClick={handleSave}>
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default SampleResultsSection;
