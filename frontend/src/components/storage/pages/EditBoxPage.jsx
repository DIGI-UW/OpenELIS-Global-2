import React, { useState, useEffect, useCallback } from "react";
import { useParams, useHistory } from "react-router-dom";
import {
  Button,
  TextInput,
  NumberInput,
  Checkbox,
  Dropdown,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";

/**
 * EditBoxPage — /Storage/boxes/:id/edit
 *
 * Boxes have grid-layout fields (rows × columns + capacityLimit) that
 * don't fit the generic EditLocationPage shell, so they get their own
 * page.
 *
 * Backend form is StorageBoxForm { label, code, parentRackId,
 * capacityLimit, rows, columns, active }.
 */
export default function EditBoxPage() {
  const { id } = useParams();
  const history = useHistory();
  const intl = useIntl();

  const [formData, setFormData] = useState(null);
  const [rackOptions, setRackOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    getFromOpenElisServer(`/rest/storage/boxes/${id}`, (response) => {
      if (response && !response.error) {
        setFormData({
          label: response.label || response.name || "",
          code: response.code || "",
          parentRackId: String(response.parentRackId || ""),
          capacityLimit:
            response.capacityLimit != null
              ? String(response.capacityLimit)
              : "",
          rows: response.rows != null ? String(response.rows) : "",
          columns: response.columns != null ? String(response.columns) : "",
          active: response.active !== false,
        });
      } else {
        setError(
          response?.error ||
            response?.message ||
            intl.formatMessage({
              id: "storage.editbox.error.loadBox",
              defaultMessage: "Failed to load box",
            }),
        );
      }
      setLoading(false);
    });
  }, [id]);

  useEffect(() => {
    getFromOpenElisServer(`/rest/storage/racks`, (response) => {
      if (Array.isArray(response)) setRackOptions(response);
    });
  }, []);

  const updateField = useCallback((key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }, []);

  const navigateBack = () => {
    history.push(`/Storage/boxes?t=${Date.now()}`);
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);

    const payload = {
      label: formData.label,
      code: formData.code || null,
      parentRackId: formData.parentRackId || null,
      capacityLimit: formData.capacityLimit
        ? parseInt(formData.capacityLimit, 10)
        : null,
      rows: formData.rows ? parseInt(formData.rows, 10) : null,
      columns: formData.columns ? parseInt(formData.columns, 10) : null,
      active: formData.active,
    };

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/storage/boxes/${id}`,
        {
          method: "PUT",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(payload),
        },
      );
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(
          body.message ||
            intl.formatMessage(
              {
                id: "storage.edit.error.saveHttp",
                defaultMessage: "Save failed (HTTP {status})",
              },
              { status: response.status },
            ),
        );
      }
      navigateBack();
    } catch (e) {
      setError(
        e.message ||
          intl.formatMessage({
            id: "storage.edit.error.saveFailed",
            defaultMessage: "Save failed",
          }),
      );
    } finally {
      setSaving(false);
    }
  };

  const crumbs = [
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.storage",
        defaultMessage: "Storage",
      }),
      href: "/Storage",
    },
    {
      label: intl.formatMessage({
        id: "storage.nav.boxes",
        defaultMessage: "Boxes",
      }),
      href: "/Storage/boxes",
    },
    {
      label: intl.formatMessage({
        id: "storage.edit.title",
        defaultMessage: "Edit",
      }),
      href: `/Storage/boxes/${id}/edit`,
    },
  ];

  if (loading) {
    return (
      <div className="storage-edit-page">
        <BreadcrumbNav crumbs={crumbs} />
        <InlineLoading
          description={intl.formatMessage({
            id: "label.loading",
            defaultMessage: "Loading...",
          })}
        />
      </div>
    );
  }

  if (error && !formData) {
    return (
      <div className="storage-edit-page">
        <BreadcrumbNav crumbs={crumbs} />
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
        />
      </div>
    );
  }

  return (
    <div className="storage-edit-page">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage
          id="storage.editbox.heading"
          defaultMessage="Edit Box"
        />
      </h1>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
        />
      )}

      <div className="storage-edit-page-form" style={{ maxWidth: "32rem" }}>
        <TextInput
          id="box-edit-label"
          labelText={intl.formatMessage({
            id: "label.label",
            defaultMessage: "Label",
          })}
          value={formData.label}
          onChange={(e) => updateField("label", e.target.value)}
        />
        <TextInput
          id="box-edit-code"
          labelText={intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          })}
          value={formData.code}
          onChange={(e) => updateField("code", e.target.value)}
        />
        <Dropdown
          id="box-edit-rack"
          titleText={intl.formatMessage({
            id: "storage.nav.rack",
            defaultMessage: "Rack",
          })}
          label={intl.formatMessage({
            id: "storage.editbox.selectRack",
            defaultMessage: "Select rack",
          })}
          items={rackOptions}
          itemToString={(item) => (item ? item.label || item.name || "" : "")}
          selectedItem={
            rackOptions.find(
              (r) => String(r.id) === String(formData.parentRackId),
            ) || null
          }
          onChange={({ selectedItem }) =>
            updateField(
              "parentRackId",
              selectedItem ? String(selectedItem.id) : "",
            )
          }
        />
        <NumberInput
          id="box-edit-capacity"
          label={intl.formatMessage({
            id: "storage.box.capacity",
            defaultMessage: "Capacity limit",
          })}
          value={formData.capacityLimit}
          onChange={(_e, { value }) =>
            updateField("capacityLimit", String(value ?? ""))
          }
          min={0}
        />
        <NumberInput
          id="box-edit-rows"
          label={intl.formatMessage({
            id: "storage.box.rows",
            defaultMessage: "Rows",
          })}
          value={formData.rows}
          onChange={(_e, { value }) => updateField("rows", String(value ?? ""))}
          min={0}
        />
        <NumberInput
          id="box-edit-columns"
          label={intl.formatMessage({
            id: "storage.box.columns",
            defaultMessage: "Columns",
          })}
          value={formData.columns}
          onChange={(_e, { value }) =>
            updateField("columns", String(value ?? ""))
          }
          min={0}
        />
        <Checkbox
          id="box-edit-active"
          labelText={intl.formatMessage({
            id: "label.active",
            defaultMessage: "Active",
          })}
          checked={formData.active}
          onChange={(e, { checked }) => updateField("active", checked)}
        />
      </div>

      <div className="storage-edit-page-actions" style={{ marginTop: "1rem" }}>
        <Button kind="secondary" onClick={navigateBack}>
          <FormattedMessage id="label.cancel" defaultMessage="Cancel" />
        </Button>
        <Button kind="primary" onClick={handleSave} disabled={saving}>
          <FormattedMessage id="label.save" defaultMessage="Save" />
        </Button>
      </div>
    </div>
  );
}
