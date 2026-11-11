import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  Checkbox,
  NumberInput,
  Tag,
  InlineLoading,
} from "@carbon/react";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";

const FIELDS_URL = "/rest/admin/vector/manual-entry-fields";

function FieldRow({ field, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);
  const [form, setForm] = useState({
    fieldOrder: field.fieldOrder,
    label: field.label || "",
    portalTag: field.portalTag || "",
    visible: field.visible !== false,
  });

  useEffect(() => {
    setForm({
      fieldOrder: field.fieldOrder,
      label: field.label || "",
      portalTag: field.portalTag || "",
      visible: field.visible !== false,
    });
  }, [field]);

  const handleSave = () => {
    putToOpenElisServer(
      `${FIELDS_URL}/${field.id}`,
      JSON.stringify({
        id: field.id,
        metricKey: field.metricKey,
        fieldOrder: Number(form.fieldOrder),
        label: form.label,
        portalTag: form.portalTag || null,
        visible: form.visible,
        lastupdated: field.lastupdated,
      }),
      (status) => {
        if (status === 200) {
          setExpanded(false);
          onSaved();
        }
      },
    );
  };

  return (
    <>
      <tr style={{ borderBottom: "1px solid #e0e0e0" }}>
        <td style={{ padding: "0.75rem 1rem", fontWeight: 500 }}>
          {field.fieldOrder}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type="cool-gray" size="sm">
            {field.metricKey}
          </Tag>
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>{field.label}</td>
        <td style={{ padding: "0.75rem 1rem" }}>{field.portalTag || "—"}</td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type={field.visible !== false ? "green" : "gray"} size="sm">
            {field.visible !== false
              ? intl.formatMessage({
                  id: "vectorReport.fieldMap.visible",
                  defaultMessage: "Visible",
                })
              : intl.formatMessage({
                  id: "vectorReport.fieldMap.hidden",
                  defaultMessage: "Hidden",
                })}
          </Tag>
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Button kind="ghost" size="sm" onClick={() => setExpanded((v) => !v)}>
            <FormattedMessage id="label.button.edit" defaultMessage="Edit" />
          </Button>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={6} style={{ padding: "1rem", background: "#f9f9f9" }}>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "8rem 1fr 1fr",
                gap: "1rem",
                marginBottom: "0.75rem",
              }}
            >
              <NumberInput
                id={`field-order-${field.id}`}
                label={intl.formatMessage({
                  id: "vectorReport.fieldMap.order",
                  defaultMessage: "Order",
                })}
                value={form.fieldOrder}
                min={1}
                onChange={(e, { value }) =>
                  setForm((f) => ({ ...f, fieldOrder: value }))
                }
              />
              <TextInput
                id={`field-label-${field.id}`}
                labelText={intl.formatMessage({
                  id: "vectorReport.fieldMap.label",
                  defaultMessage: "Label",
                })}
                value={form.label}
                onChange={(e) =>
                  setForm((f) => ({ ...f, label: e.target.value }))
                }
              />
              <TextInput
                id={`field-tag-${field.id}`}
                labelText={intl.formatMessage({
                  id: "vectorReport.fieldMap.portalTag",
                  defaultMessage: "Portal tag",
                })}
                value={form.portalTag}
                onChange={(e) =>
                  setForm((f) => ({ ...f, portalTag: e.target.value }))
                }
              />
            </div>
            <div style={{ marginBottom: "0.75rem" }}>
              <Checkbox
                id={`field-visible-${field.id}`}
                labelText={intl.formatMessage({
                  id: "vectorReport.fieldMap.visibleLabel",
                  defaultMessage: "Visible in the Manual Entry Helper",
                })}
                checked={form.visible}
                onChange={(e, { checked }) =>
                  setForm((f) => ({ ...f, visible: checked }))
                }
              />
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button onClick={handleSave} disabled={!form.label}>
                <FormattedMessage
                  id="label.button.save"
                  defaultMessage="Save"
                />
              </Button>
              <Button kind="ghost" onClick={() => setExpanded(false)}>
                <FormattedMessage
                  id="label.button.cancel"
                  defaultMessage="Cancel"
                />
              </Button>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

export default function ManualEntryFieldMapPage() {
  const intl = useIntl();
  const [fields, setFields] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(FIELDS_URL, (data) => {
      setFields(data || []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="vectorReport.fieldMap.title"
          defaultMessage="Manual Entry Field Map"
        />
      </h3>
      <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
        <FormattedMessage
          id="vectorReport.fieldMap.description"
          defaultMessage="Reorder, hide, relabel, or portal-tag the metrics shown in the Manual Entry Helper."
        />
      </p>
      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          background: "#fff",
        }}
      >
        <thead>
          <tr
            style={{ background: "#f4f4f4", borderBottom: "2px solid #e0e0e0" }}
          >
            <th style={{ padding: "0.75rem 1rem", textAlign: "left" }}>
              <FormattedMessage
                id="vectorReport.fieldMap.order"
                defaultMessage="Order"
              />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left" }}>
              <FormattedMessage
                id="vectorReport.fieldMap.metric"
                defaultMessage="Metric"
              />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left" }}>
              <FormattedMessage
                id="vectorReport.fieldMap.label"
                defaultMessage="Label"
              />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left" }}>
              <FormattedMessage
                id="vectorReport.fieldMap.portalTag"
                defaultMessage="Portal tag"
              />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left" }}>
              <FormattedMessage
                id="vectorReport.fieldMap.visibility"
                defaultMessage="Visibility"
              />
            </th>
            <th />
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={6} style={{ padding: "2rem", textAlign: "center" }}>
                <InlineLoading />
              </td>
            </tr>
          ) : (
            fields.map((f) => <FieldRow key={f.id} field={f} onSaved={load} />)
          )}
        </tbody>
      </table>
    </>
  );
}
