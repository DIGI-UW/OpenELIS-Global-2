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
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M10 / OGC-957..958 — Terminology Mappings section.
 *
 * Lists a test's terminology mappings (Source / Code / Relationship) and lets
 * the admin add or remove them via an inline form, persisting the whole set
 * with PUT /rest/test-catalog/tests/{id}/terminology. Source ∈ LOINC / SNOMED /
 * CIEL / OCL; relationship ∈ SAME_AS / BROADER_THAN / NARROWER_THAN.
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
  const [loincIntegrity, setLoincIntegrity] = useState(null);
  const [draft, setDraft] = useState({
    source: "",
    code: "",
    relationship: "",
  });

  const loadLoincIntegrity = () => {
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/loinc-integrity`,
      (res) => setLoincIntegrity(res || null),
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/terminology`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setMappings(res.mappings || []);
      },
    );
    loadLoincIntegrity();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

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
    setDraft({ source: "", code: "", relationship: "" });
  };

  const removeMapping = (index) =>
    setMappings((prev) => prev.filter((_, i) => i !== index));

  const handleSave = () => {
    setSaving(true);
    const payload = {
      testId,
      mappings: mappings.map((m) => ({
        id: m.id || null,
        source: m.source,
        code: m.code,
        relationship: m.relationship || null,
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
                <FormattedMessage id="label.testCatalog.terminology.col.relationship" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.terminology.col.actions" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {mappings.map((m, i) => (
              <TableRow
                key={m.id || `${m.source}-${m.code}-${i}`}
                data-testid={`mapping-row-${m.id || i}`}
              >
                <TableCell>
                  <Tag type={SOURCE_TAG[m.source] || "gray"}>{m.source}</Tag>
                </TableCell>
                <TableCell>
                  <code>{m.code}</code>
                </TableCell>
                <TableCell>
                  {m.relationship ? (
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
                    renderIcon={TrashCan}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.terminology.remove",
                    })}
                    onClick={() => removeMapping(i)}
                  />
                </TableCell>
              </TableRow>
            ))}
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
        <Select
          id="terminology-relationship"
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

      <Button kind="primary" disabled={saving} onClick={handleSave}>
        <FormattedMessage id="label.button.save" />
      </Button>
    </Stack>
  );
};

export default TerminologySection;
