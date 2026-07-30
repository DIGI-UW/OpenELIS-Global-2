import React, { useCallback, useEffect, useState } from "react";
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
  InlineNotification,
} from "@carbon/react";
import { Add, Checkmark, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

// Mirrors the Test Catalog Editor's Terminology UX (Source/Code/Relationship,
// inline edit, draft-row add). Persists through
// `GET/PUT /rest/sample-types/{id}/terminology`.
const SOURCES = ["LOINC", "SNOMED", "CIEL", "OCL", "WHONET"];
const RELATIONSHIPS = ["SAME_AS", "BROADER_THAN", "NARROWER_THAN"];
const SOURCE_TAG = {
  LOINC: "blue",
  SNOMED: "teal",
  CIEL: "purple",
  OCL: "cyan",
  WHONET: "magenta",
};

const emptyDraft = () => ({ source: "", code: "", relationship: "" });

const TerminologySection = ({ sampleTypeId }) => {
  const intl = useIntl();

  const [mappings, setMappings] = useState([]);
  const [draft, setDraft] = useState(emptyDraft);
  const [editingRows, setEditingRows] = useState(() => new Set());
  const [savedNotice, setSavedNotice] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadMappings = useCallback(() => {
    if (!sampleTypeId) {
      return;
    }
    getFromOpenElisServer(
      `/rest/sample-types/${encodeURIComponent(sampleTypeId)}/terminology`,
      (res) => {
        if (res && Array.isArray(res.mappings)) {
          setMappings(res.mappings);
        } else {
          setMappings([]);
        }
      },
    );
  }, [sampleTypeId]);

  useEffect(() => {
    setDraft(emptyDraft());
    setEditingRows(new Set());
    setSavedNotice(false);
    setSaveError(null);
    loadMappings();
  }, [sampleTypeId, loadMappings]);

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

  const updateMapping = (index, patch) =>
    setMappings((prev) =>
      prev.map((m, i) => (i === index ? { ...m, ...patch } : m)),
    );

  const addMapping = () => {
    if (!draft.source || !draft.code) {
      return;
    }
    setMappings((prev) => [
      ...prev,
      {
        id: null,
        source: draft.source,
        code: draft.code,
        relationship: draft.relationship || null,
      },
    ]);
    setDraft(emptyDraft());
  };

  const removeMapping = (index) => {
    setMappings((prev) => prev.filter((_, i) => i !== index));
    setEditingRows(new Set());
  };

  const handleSave = () => {
    if (!sampleTypeId) {
      return;
    }
    // Fold in a filled-but-not-yet-added draft so the admin can just type a row
    // and hit Save without first clicking "Add mapping".
    const all = [...mappings];
    if (draft.source && draft.code) {
      all.push({
        id: null,
        source: draft.source,
        code: draft.code,
        relationship: draft.relationship || null,
      });
    }
    const complete = all.filter((m) => m.source && m.code);
    const payload = {
      sampleTypeId,
      mappings: complete.map((m) => ({
        id: m.id || null,
        source: m.source,
        code: m.code,
        relationship: m.relationship || null,
      })),
    };
    setSaving(true);
    setSaveError(null);
    putToOpenElisServer(
      `/rest/sample-types/${encodeURIComponent(sampleTypeId)}/terminology`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        if (status === 200) {
          setDraft(emptyDraft());
          setEditingRows(new Set());
          setSavedNotice(true);
          window.setTimeout(() => setSavedNotice(false), 3000);
          // Reload with server-assigned ids so subsequent edits update in place.
          loadMappings();
        } else if (status === 422) {
          setSaveError(
            intl.formatMessage({
              id: "label.sampleType.terminology.saveInvalid",
              defaultMessage:
                "Some mappings are invalid. Check source, code, and duplicates.",
            }),
          );
        } else {
          setSaveError(
            intl.formatMessage({
              id: "label.sampleType.terminology.saveError",
              defaultMessage: "Could not save terminology mappings.",
            }),
          );
        }
      },
    );
  };

  return (
    <Stack gap={6} data-testid="sampleType-terminology-section">
      <p>
        <FormattedMessage id="label.sampleType.terminology.intro" />
      </p>

      {savedNotice && (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.sampleType.section.terminology",
          })}
          subtitle={intl.formatMessage({
            id: "label.sampleType.terminology.saved",
          })}
        />
      )}

      {saveError && (
        <InlineNotification
          kind="error"
          lowContrast
          onCloseButtonClick={() => setSaveError(null)}
          title={intl.formatMessage({
            id: "label.sampleType.section.terminology",
          })}
          subtitle={saveError}
        />
      )}

      {mappings.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.sampleType.terminology.empty",
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
                <FormattedMessage id="label.testCatalog.terminology.col.relationship" />
              </TableHeader>
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
                  data-testid={`sampleType-mapping-row-${m.id || i}`}
                >
                  <TableCell>
                    {editing ? (
                      <Select
                        id={`sampleType-mapping-source-${i}`}
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
                        id={`sampleType-mapping-code-${i}`}
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
                      <Select
                        id={`sampleType-mapping-rel-${i}`}
                        labelText=""
                        value={m.relationship || ""}
                        onChange={(e) =>
                          updateMapping(i, {
                            relationship: e.target.value || null,
                          })
                        }
                      >
                        <SelectItem
                          value=""
                          text={intl.formatMessage({
                            id: "label.testCatalog.terminology.rel.none",
                          })}
                        />
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
                    <Button
                      kind="ghost"
                      size="sm"
                      hasIconOnly
                      renderIcon={editing ? Checkmark : Edit}
                      data-testid={`sampleType-edit-mapping-${i}`}
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
          id="sampleType-terminology-source"
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
          id="sampleType-terminology-code"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.code",
          })}
          value={draft.code}
          onChange={(e) => setDraft({ ...draft, code: e.target.value })}
        />
        <Select
          id="sampleType-terminology-relationship"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.relationship",
          })}
          value={draft.relationship}
          onChange={(e) => setDraft({ ...draft, relationship: e.target.value })}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.testCatalog.terminology.rel.none",
            })}
          />
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
        <Button kind="tertiary" renderIcon={Add} onClick={addMapping}>
          <FormattedMessage id="label.testCatalog.terminology.addMapping" />
        </Button>
      </Stack>

      <Button kind="primary" onClick={handleSave} disabled={saving}>
        <FormattedMessage id="label.button.save" />
      </Button>
    </Stack>
  );
};

export default TerminologySection;
