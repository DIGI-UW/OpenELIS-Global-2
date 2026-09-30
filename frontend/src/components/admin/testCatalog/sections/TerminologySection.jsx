import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  Select,
  SelectItem,
  TextInput,
  Button,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add, Checkmark, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M10 / OGC-957..958 — Terminology Mappings section.
 *
 * Lists a test's terminology mappings (Source / Code / Relationship). Rows are
 * read-only until the admin clicks Edit; a row's fields are then editable in
 * place. New rows are added via the bottom form (either "Add mapping" or just
 * filling it and hitting Save). The whole set persists with PUT
 * /rest/test-catalog/tests/{id}/terminology. Source ∈ LOINC / SNOMED / CIEL /
 * OCL; relationship ∈ SAME_AS / BROADER_THAN / NARROWER_THAN.
 */
const SOURCES = ["LOINC", "SNOMED", "CIEL", "OCL"];
const RELATIONSHIPS = ["SAME_AS", "BROADER_THAN", "NARROWER_THAN"];
const SOURCE_TAG = {
  LOINC: "blue",
  SNOMED: "teal",
  CIEL: "purple",
  OCL: "cyan",
};

const TerminologySection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [mappings, setMappings] = useState([]);
  // The test's result components (id, code, label), so a mapping can be scoped
  // to a single component ("Applies to") instead of the whole test.
  const [components, setComponents] = useState([]);
  // OGC-1145 FR-13: the test's associated sample types — a mapping may
  // override for one specimen (null = shared across all of them).
  const [sampleTypes, setSampleTypes] = useState([]);
  const [loincIntegrity, setLoincIntegrity] = useState(null);
  const [draft, setDraft] = useState({
    source: "",
    code: "",
    // Relationship is required (FR-70); default to SAME_AS, no "none" option.
    relationship: "SAME_AS",
    displayName: "",
    componentId: "",
    sampleTypeId: "",
  });

  const componentLabel = (id) => {
    const c = components.find((x) => x.id === id);
    if (!c) {
      return "";
    }
    return c.label || c.code || "";
  };

  const sampleTypeLabel = (id) => {
    const t = sampleTypes.find((x) => x.id === id);
    return t ? t.name : "";
  };
  // The override picker only matters once the test runs on several specimens.
  const showSampleTypeColumn = sampleTypes.length > 1;
  // Row indices currently in edit mode; a row's fields are editable only after
  // its Edit button is clicked.
  const [editingRows, setEditingRows] = useState(() => new Set());

  const toggleEdit = (index) =>
    setEditingRows((prev) => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });

  const loadLoincIntegrity = () => {
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/loinc-integrity`,
      (res) => setLoincIntegrity(res || null),
    );
  };

  const loadMappings = () => {
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/terminology`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setMappings(res.mappings || []);
        setComponents(res.components || []);
        setSampleTypes(res.sampleTypes || []);
      },
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    loadMappings();
    loadLoincIntegrity();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  // Edit an existing (or newly added) row in place.
  const updateMapping = (index, patch) =>
    setMappings((prev) =>
      prev.map((m, i) => (i === index ? { ...m, ...patch } : m)),
    );

  // A mapping's identity is (source, code, scope). Warn specifically on a
  // duplicate — the backend rejects it with a bodyless 422, which used to
  // surface as nothing more than a generic failure.
  const mappingKey = (m) =>
    `${m.source}|${(m.code || "").trim()}|${m.componentId || ""}|${
      m.sampleTypeId || ""
    }`;

  const notifyDuplicate = (m) => {
    setNotificationVisible(true);
    addNotification({
      kind: "error",
      title: intl.formatMessage({
        id: "label.testCatalog.section.terminology",
      }),
      message: intl.formatMessage(
        { id: "error.testCatalog.terminology.duplicate" },
        { source: m.source, code: (m.code || "").trim() },
      ),
    });
  };

  const addMapping = () => {
    if (!draft.source || !draft.code) {
      return;
    }
    if (mappings.some((m) => mappingKey(m) === mappingKey(draft))) {
      notifyDuplicate(draft);
      return;
    }
    setMappings((prev) => [
      ...prev,
      {
        id: null,
        source: draft.source,
        code: draft.code,
        relationship: draft.relationship || "SAME_AS",
        displayName: draft.displayName || null,
        componentId: draft.componentId || null,
        sampleTypeId: draft.sampleTypeId || null,
      },
    ]);
    setDraft({
      source: "",
      code: "",
      relationship: "SAME_AS",
      displayName: "",
      componentId: "",
      sampleTypeId: "",
    });
  };

  const removeMapping = (index) => {
    setMappings((prev) => prev.filter((_, i) => i !== index));
    // Row indices shift after a removal; clear edit state to avoid the wrong row
    // appearing editable.
    setEditingRows(new Set());
  };

  const handleSave = () => {
    // Fold in a filled-but-not-yet-added draft so the admin can just type a row
    // and hit Save without first clicking "Add mapping".
    const all = [...mappings];
    if (draft.source && draft.code) {
      all.push({
        id: null,
        source: draft.source,
        code: draft.code,
        relationship: draft.relationship || "SAME_AS",
        displayName: draft.displayName || null,
        componentId: draft.componentId || null,
        sampleTypeId: draft.sampleTypeId || null,
      });
    }
    // Persist only complete rows (source + code); drop blank/partial ones.
    const complete = all.filter((m) => m.source && m.code);

    // Duplicate (source, code, scope) — catch it here with a specific message
    // (in-place edits can collide too, not just the add form).
    const seen = new Set();
    for (const m of complete) {
      const key = mappingKey(m);
      if (seen.has(key)) {
        notifyDuplicate(m);
        return;
      }
      seen.add(key);
    }

    setSaving(true);
    const payload = {
      testId,
      mappings: complete.map((m) => ({
        id: m.id || null,
        source: m.source,
        code: m.code,
        relationship: m.relationship || "SAME_AS",
        displayName: m.displayName || null,
        componentId: m.componentId || null,
        sampleTypeId: m.sampleTypeId || null,
      })),
    };
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/terminology`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.terminology",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.terminology.saved",
            }),
          });
          // Refresh with server-assigned ids + recomputed LOINC integrity so the
          // next edit updates in place rather than inserting duplicates.
          setDraft({
            source: "",
            code: "",
            relationship: "SAME_AS",
            displayName: "",
            componentId: "",
          });
          setEditingRows(new Set());
          loadMappings();
          loadLoincIntegrity();
        } else if (status === 422) {
          // Server-side validation (duplicate mapping, unknown source/relationship,
          // stale component scope) — name the cause instead of a generic failure.
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "label.testCatalog.section.terminology",
            }),
            message: intl.formatMessage({
              id: "error.testCatalog.terminology.invalid",
            }),
          });
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
          id: "label.testCatalog.terminology.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="terminology-section">
      <p>
        <FormattedMessage id="label.testCatalog.terminology.intro" />
      </p>

      {loincIntegrity && loincIntegrity.noLoinc && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          data-testid="no-loinc-warning"
          title={intl.formatMessage({ id: "warning.testCatalog.noLoinc" })}
        />
      )}
      {loincIntegrity &&
        loincIntegrity.duplicates &&
        loincIntegrity.duplicates.length > 0 && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            data-testid="duplicate-loinc-warning"
            title={intl.formatMessage(
              { id: "warning.testCatalog.duplicateLoinc" },
              {
                code: loincIntegrity.loinc,
                testName: loincIntegrity.duplicates
                  .map((d) => d.name)
                  .join(", "),
              },
            )}
          />
        )}

      {mappings.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.terminology.empty",
          })}
        />
      ) : (
        <Table size="lg" aria-label="terminology">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.source" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.code" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.displayName" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.relationship" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.appliesTo" />
              </TableHeader>
              {showSampleTypeColumn && (
                <TableHeader>
                  <FormattedMessage id="label.testCatalog.override.col.sampleType" />
                </TableHeader>
              )}
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.actions" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {mappings.map((m, i) => {
              const editing = editingRows.has(i);
              return (
                <TableRow
                  key={m.id || `new-${i}`}
                  data-testid={`mapping-row-${m.id || i}`}
                >
                  <TableCell>
                    {editing ? (
                      <Select
                        id={`mapping-source-${i}`}
                        labelText=""
                        value={m.source || ""}
                        onChange={(e) =>
                          updateMapping(i, { source: e.target.value })
                        }
                      >
                        <SelectItem value="" text="" />
                        {SOURCES.map((s) => (
                          <SelectItem key={s} value={s} text={s} />
                        ))}
                      </Select>
                    ) : (
                      <Tag type={SOURCE_TAG[m.source] || "gray"}>
                        {m.source}
                      </Tag>
                    )}
                  </TableCell>
                  <TableCell>
                    {editing ? (
                      <TextInput
                        id={`mapping-code-${i}`}
                        labelText=""
                        value={m.code || ""}
                        onChange={(e) =>
                          updateMapping(i, { code: e.target.value })
                        }
                      />
                    ) : (
                      <code>{m.code}</code>
                    )}
                  </TableCell>
                  <TableCell>
                    {editing ? (
                      <TextInput
                        id={`mapping-display-${i}`}
                        labelText=""
                        value={m.displayName || ""}
                        placeholder={intl.formatMessage({
                          id: "label.testCatalog.terminology.displayName.placeholder",
                        })}
                        onChange={(e) =>
                          updateMapping(i, { displayName: e.target.value })
                        }
                      />
                    ) : (
                      m.displayName || ""
                    )}
                  </TableCell>
                  <TableCell>
                    {editing ? (
                      <Select
                        id={`mapping-rel-${i}`}
                        labelText=""
                        value={m.relationship || "SAME_AS"}
                        onChange={(e) =>
                          updateMapping(i, {
                            relationship: e.target.value,
                          })
                        }
                      >
                        {RELATIONSHIPS.map((r) => (
                          <SelectItem
                            key={r}
                            value={r}
                            text={intl.formatMessage({
                              id: `label.testCatalog.terminology.rel.${r}`,
                            })}
                          />
                        ))}
                      </Select>
                    ) : m.relationship ? (
                      <FormattedMessage
                        id={`label.testCatalog.terminology.rel.${m.relationship}`}
                      />
                    ) : (
                      ""
                    )}
                  </TableCell>
                  <TableCell>
                    {editing ? (
                      <Select
                        id={`mapping-component-${i}`}
                        labelText=""
                        value={m.componentId || ""}
                        onChange={(e) =>
                          updateMapping(i, {
                            componentId: e.target.value || null,
                          })
                        }
                      >
                        <SelectItem
                          value=""
                          text={intl.formatMessage({
                            id: "label.testCatalog.terminology.appliesTo.test",
                          })}
                        />
                        {components.map((c) => (
                          <SelectItem
                            key={c.id}
                            value={c.id}
                            text={c.label || c.code}
                          />
                        ))}
                      </Select>
                    ) : m.componentId ? (
                      <Tag type="green">{componentLabel(m.componentId)}</Tag>
                    ) : (
                      <FormattedMessage id="label.testCatalog.terminology.appliesTo.test" />
                    )}
                  </TableCell>
                  {showSampleTypeColumn && (
                    <TableCell>
                      {editing ? (
                        <Select
                          id={`mapping-sample-type-${i}`}
                          labelText=""
                          value={m.sampleTypeId || ""}
                          onChange={(e) =>
                            updateMapping(i, {
                              sampleTypeId: e.target.value || null,
                            })
                          }
                        >
                          <SelectItem
                            value=""
                            text={intl.formatMessage({
                              id: "label.testCatalog.override.shared",
                            })}
                          />
                          {sampleTypes.map((t) => (
                            <SelectItem key={t.id} value={t.id} text={t.name} />
                          ))}
                        </Select>
                      ) : m.sampleTypeId ? (
                        <Tag type="blue">{sampleTypeLabel(m.sampleTypeId)}</Tag>
                      ) : (
                        <FormattedMessage id="label.testCatalog.override.shared" />
                      )}
                    </TableCell>
                  )}
                  <TableCell>
                    <Button
                      kind="ghost"
                      size="sm"
                      hasIconOnly
                      renderIcon={editing ? Checkmark : Edit}
                      data-testid={`edit-mapping-${i}`}
                      iconDescription={intl.formatMessage({
                        id: editing
                          ? "label.button.close"
                          : "label.button.edit",
                      })}
                      onClick={() => toggleEdit(i)}
                    />
                    <Button
                      kind="ghost"
                      size="sm"
                      hasIconOnly
                      renderIcon={TrashCan}
                      iconDescription={intl.formatMessage({
                        id: "label.testCatalog.terminology.remove",
                      })}
                      onClick={() => removeMapping(i)}
                    />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}

      <Stack gap={4} orientation="horizontal">
        <Select
          id="terminology-source"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.source",
          })}
          value={draft.source}
          onChange={(e) => setDraft({ ...draft, source: e.target.value })}
        >
          <SelectItem value="" text="" />
          {SOURCES.map((s) => (
            <SelectItem key={s} value={s} text={s} />
          ))}
        </Select>
        <TextInput
          id="terminology-code"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.code",
          })}
          value={draft.code}
          onChange={(e) => setDraft({ ...draft, code: e.target.value })}
        />
        <TextInput
          id="terminology-display-name"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.col.displayName",
          })}
          value={draft.displayName}
          onChange={(e) => setDraft({ ...draft, displayName: e.target.value })}
        />
        <Select
          id="terminology-relationship"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.relationship",
          })}
          value={draft.relationship}
          onChange={(e) => setDraft({ ...draft, relationship: e.target.value })}
        >
          {RELATIONSHIPS.map((r) => (
            <SelectItem
              key={r}
              value={r}
              text={intl.formatMessage({
                id: `label.testCatalog.terminology.rel.${r}`,
              })}
            />
          ))}
        </Select>
        <Select
          id="terminology-component"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.col.appliesTo",
          })}
          value={draft.componentId}
          onChange={(e) => setDraft({ ...draft, componentId: e.target.value })}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.testCatalog.terminology.appliesTo.test",
            })}
          />
          {components.map((c) => (
            <SelectItem key={c.id} value={c.id} text={c.label || c.code} />
          ))}
        </Select>
        {showSampleTypeColumn && (
          <Select
            id="terminology-sample-type"
            labelText={intl.formatMessage({
              id: "label.testCatalog.override.col.sampleType",
            })}
            value={draft.sampleTypeId}
            onChange={(e) =>
              setDraft({ ...draft, sampleTypeId: e.target.value })
            }
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.testCatalog.override.shared",
              })}
            />
            {sampleTypes.map((t) => (
              <SelectItem key={t.id} value={t.id} text={t.name} />
            ))}
          </Select>
        )}
        <Button kind="tertiary" renderIcon={Add} onClick={addMapping}>
          <FormattedMessage id="label.testCatalog.terminology.addMapping" />
        </Button>
      </Stack>

      <Button kind="primary" disabled={saving} onClick={handleSave}>
        <FormattedMessage id="label.button.save" />
      </Button>
    </Stack>
  );
};

export default TerminologySection;
