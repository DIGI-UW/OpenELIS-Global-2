import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  Accordion,
  AccordionItem,
  TextInput,
  ComboBox,
  Select,
  SelectItem,
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
 * (OGC-968), have a unit picked from the master list (OGC-963), and the whole
 * config copied from another test (OGC-966). Inline-add of a *new* unit (the
 * create half of OGC-963) and sample types (OGC-961) are follow-ups.
 */
const RESULT_TYPES = ["N", "D", "R"];

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

  // ── Immutable updaters ─────────────────────────────────────────────────────
  const patchComponent = (ci, patch) =>
    setComponents((prev) =>
      prev.map((c, i) => (i === ci ? { ...c, ...patch } : c)),
    );

  const addComponent = () =>
    setComponents((prev) => [
      ...prev,
      {
        code: "",
        label: "",
        displayOrder: prev.length + 1,
        resultType: "N",
        significantDigits: null,
        defaultResult: "",
        allowMultipleReadings: false,
        options: [],
        interpretations: [],
      },
    ]);

  const removeComponent = (ci) =>
    setComponents((prev) => prev.filter((_, i) => i !== ci));

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
                  resultType: "D",
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
    setSaving(true);
    const payload = {
      testId,
      components: components.map((c) => ({
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

  return (
    <Stack gap={6}>
      {components.length === 0 ? (
        <p>
          <FormattedMessage id="label.testCatalog.sampleResults.empty" />
        </p>
      ) : (
        <Accordion>
          {components.map((c, ci) => (
            <AccordionItem
              key={c.id || `new-${ci}`}
              open
              title={
                c.label ||
                c.code ||
                intl.formatMessage({
                  id: "label.testCatalog.sampleResults.newComponent",
                })
              }
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
                <TextInput
                  id={`comp-code-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.code",
                  })}
                  value={c.code || ""}
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
                <Select
                  id={`comp-type-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.resultType",
                  })}
                  value={c.resultType || "N"}
                  onChange={(e) =>
                    patchComponent(ci, { resultType: e.target.value })
                  }
                >
                  {RESULT_TYPES.map((t) => (
                    <SelectItem
                      key={t}
                      value={t}
                      text={intl.formatMessage({
                        id: `label.testCatalog.sampleResults.resultType.${t}`,
                      })}
                    />
                  ))}
                </Select>
                <Select
                  id={`comp-uom-${ci}`}
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.uom",
                  })}
                  value={c.uomId || ""}
                  onChange={(e) =>
                    patchComponent(ci, { uomId: e.target.value })
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "label.testCatalog.sampleResults.uom.none",
                    })}
                  />
                  {uoms.map((u) => (
                    <SelectItem key={u.id} value={u.id} text={u.value} />
                  ))}
                </Select>
                <TextInput
                  id={`comp-sigdig-${ci}`}
                  type="number"
                  labelText={intl.formatMessage({
                    id: "label.testCatalog.sampleResults.significantDigits",
                  })}
                  value={c.significantDigits ?? ""}
                  onChange={(e) =>
                    patchComponent(ci, { significantDigits: e.target.value })
                  }
                />
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

                {/* Select-list options (OGC-964) */}
                <h6>
                  <FormattedMessage id="label.testCatalog.sampleResults.options" />
                </h6>
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
                              patchChild(ci, "options", oi, { normal: checked })
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

                {/* Interpretations (OGC-965) */}
                <h6>
                  <FormattedMessage id="label.testCatalog.sampleResults.interpretations" />
                </h6>
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
                          <TextInput
                            id={`int-match-${ci}-${ii}`}
                            labelText=""
                            value={it.valueMatch || ""}
                            onChange={(e) =>
                              patchChild(ci, "interpretations", ii, {
                                valueMatch: e.target.value,
                              })
                            }
                          />
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
            </AccordionItem>
          ))}
        </Accordion>
      )}

      <div style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}>
        <Select
          id="copy-from-test"
          labelText={intl.formatMessage({
            id: "label.testCatalog.sampleResults.copyFrom",
          })}
          value={copyFromId}
          onChange={(e) => setCopyFromId(e.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.testCatalog.sampleResults.copyFrom.placeholder",
            })}
          />
          {otherTests.map((t) => (
            <SelectItem key={t.id} value={t.id} text={t.value} />
          ))}
        </Select>
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
