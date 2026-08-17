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

/**
 * OGC-224 — Panel editor · Terminology (FRS v2.2).
 *
 * The same multi-source mapper as tests/sample types (Source / Code /
 * Relationship; LOINC / SNOMED / CIEL / OCL / WHONET), persisted through
 * GET/PUT /rest/test-catalog/panels/{id}/terminology. The LOINC mapping is
 * the panel's PRIMARY identifier (tagged) — there is no separate panel code —
 * and it stays denormalized on panel.loinc, which FHIR intake uses as a live
 * e-order routing key.
 */
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

/** index of the mapping that is the panel's primary identifier. */
export const primaryLoincIndex = (mappings) =>
  mappings.findIndex(
    (m) =>
      m.source === "LOINC" && (!m.relationship || m.relationship === "SAME_AS"),
  );

const PanelTerminologySection = ({ panel, onSaved }) => {
  const intl = useIntl();
  const panelId = panel?.id;

  const [mappings, setMappings] = useState([]);
  const [draft, setDraft] = useState(emptyDraft);
  const [editingRows, setEditingRows] = useState(() => new Set());
  const [savedNotice, setSavedNotice] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadMappings = useCallback(() => {
    if (!panelId) {
      return;
    }
    getFromOpenElisServer(
      `/rest/test-catalog/panels/${encodeURIComponent(panelId)}/terminology`,
      (res) => {
        setMappings(res && Array.isArray(res.mappings) ? res.mappings : []);
      },
    );
  }, [panelId]);

  useEffect(() => {
    setDraft(emptyDraft());
    setEditingRows(new Set());
    setSavedNotice(false);
    setSaveError(null);
    loadMappings();
  }, [panelId, loadMappings]);

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
    if (!panelId) {
      return;
    }
    // Fold in a filled-but-not-yet-added draft so the admin can just type a
    // row and hit Save without first clicking "Add mapping".
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
      panelId,
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
      `/rest/test-catalog/panels/${encodeURIComponent(panelId)}/terminology`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        if (status === 200) {
          setDraft(emptyDraft());
          setEditingRows(new Set());
          setSavedNotice(true);
          window.setTimeout(() => setSavedNotice(false), 3000);
          // Reload with server-assigned ids; the envelope refresh surfaces the
          // denormalized panel.loinc in the editor header + list.
          loadMappings();
          getFromOpenElisServer(
            `/rest/test-catalog/panels/${encodeURIComponent(panelId)}`,
            (fresh) => {
              if (fresh && fresh.id) {
                onSaved(fresh);
              }
            },
          );
        } else if (status === 422) {
          setSaveError(
            intl.formatMessage({ id: "label.panel.terminology.saveInvalid" }),
          );
        } else {
          setSaveError(
            intl.formatMessage({ id: "label.panel.terminology.saveError" }),
          );
        }
      },
    );
  };

  const primaryIndex = primaryLoincIndex(mappings);

  return (
    <Stack gap={6} data-testid="panel-terminology-section">
      <InlineNotification
        kind="info"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "helper.panel.loincIsIdentifier" })}
        subtitle={intl.formatMessage({ id: "helper.panel.loincRouting" })}
      />

      {savedNotice && (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.panel.section.terminology" })}
          subtitle={intl.formatMessage({
            id: "label.panel.terminology.saved",
          })}
        />
      )}

      {saveError && (
        <InlineNotification
          kind="error"
          lowContrast
          onCloseButtonClick={() => setSaveError(null)}
          title={intl.formatMessage({ id: "label.panel.section.terminology" })}
          subtitle={saveError}
        />
      )}

      {mappings.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.panel.terminology.empty" })}
        />
      ) : (
        <Table size="lg" aria-label="panel-terminology">
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
                  data-testid={`panel-mapping-row-${m.id || i}`}
                >
                  <TableCell>
                    {editing ? (
                      <Select
                        id={`panel-mapping-source-${i}`}
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
                      <>
                        <Tag type={SOURCE_TAG[m.source] || "gray"}>
                          {m.source}
                        </Tag>
                        {i === primaryIndex && (
                          <Tag type="blue" size="sm" data-testid="primary-tag">
                            <FormattedMessage id="label.panel.terminology.primary" />
                          </Tag>
                        )}
                      </>
                    )}
                  </TableCell>
                  <TableCell>
                    {editing ? (
                      <TextInput
                        id={`panel-mapping-code-${i}`}
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
                        id={`panel-mapping-rel-${i}`}
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
                      data-testid={`panel-edit-mapping-${i}`}
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
                      data-testid={`panel-remove-mapping-${i}`}
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
          id="panel-terminology-source"
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
          id="panel-terminology-code"
          labelText={intl.formatMessage({
            id: "label.testCatalog.terminology.code",
          })}
          value={draft.code}
          onChange={(e) => setDraft({ ...draft, code: e.target.value })}
        />
        <Select
          id="panel-terminology-relationship"
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

export default PanelTerminologySection;
