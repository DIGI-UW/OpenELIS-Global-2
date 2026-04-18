import React, { useState, useEffect, useCallback } from "react";
import { useParams, useHistory } from "react-router-dom";
import {
  Button,
  TextInput,
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
 * EditLocationPage — /Storage/{rooms|devices|shelves|racks}/:id/edit
 *
 * Generic edit page for the four container levels that share a simple
 * form shape. Boxes use EditBoxPage because they add grid-layout
 * fields. Per-type payload:
 *   Room:   { name, code, description, active }
 *   Device: { name, code, parentRoomId, active }
 *   Shelf:  { label, code, parentDeviceId, active }
 *   Rack:   { label, code, parentShelfId, active }
 */

const TYPE_META = {
  room: {
    nameField: "name",
    endpoint: "rooms",
    parentField: null,
    parentEndpoint: null,
    parentLabel: null,
  },
  device: {
    nameField: "name",
    endpoint: "devices",
    parentField: "parentRoomId",
    parentEndpoint: "rooms",
    parentLabel: "Room",
    parentLabelId: "storage.nav.room",
  },
  shelf: {
    nameField: "label",
    endpoint: "shelves",
    parentField: "parentDeviceId",
    parentEndpoint: "devices",
    parentLabel: "Device",
    parentLabelId: "storage.nav.device",
  },
  rack: {
    nameField: "label",
    endpoint: "racks",
    parentField: "parentShelfId",
    parentEndpoint: "shelves",
    parentLabel: "Shelf",
    parentLabelId: "storage.nav.shelf",
  },
};

export default function EditLocationPage({ type }) {
  const { id } = useParams();
  const history = useHistory();
  const intl = useIntl();
  const meta = TYPE_META[type];

  const [formData, setFormData] = useState(null);
  const [parentOptions, setParentOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Fetch current location
  useEffect(() => {
    getFromOpenElisServer(
      `/rest/storage/${meta.endpoint}/${id}`,
      (response) => {
        if (response && !response.error) {
          setFormData({
            [meta.nameField]:
              response[meta.nameField] || response.name || response.label || "",
            code: response.code || "",
            active: response.active !== false,
            ...(meta.parentField
              ? { [meta.parentField]: String(response[meta.parentField] || "") }
              : {}),
            description: response.description || "",
          });
        } else {
          setError(
            response?.error ||
              response?.message ||
              intl.formatMessage({
                id: "storage.edit.error.loadLocation",
                defaultMessage: "Failed to load location",
              }),
          );
        }
        setLoading(false);
      },
    );
  }, [id, meta.endpoint, meta.nameField, meta.parentField]);

  // Fetch parent options when relevant
  useEffect(() => {
    if (!meta.parentEndpoint) return;
    getFromOpenElisServer(
      `/rest/storage/${meta.parentEndpoint}`,
      (response) => {
        if (Array.isArray(response)) setParentOptions(response);
      },
    );
  }, [meta.parentEndpoint]);

  const updateField = useCallback((key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }, []);

  const navigateBack = () => {
    history.push(`/Storage/${meta.endpoint}?t=${Date.now()}`);
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);

    const payload = {
      [meta.nameField]: formData[meta.nameField],
      code: formData.code || null,
      active: formData.active,
    };
    if (meta.parentField && formData[meta.parentField]) {
      payload[meta.parentField] = formData[meta.parentField];
    }
    if (type === "room") {
      payload.description = formData.description || null;
    }

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/storage/${meta.endpoint}/${id}`,
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
        id: `storage.nav.${meta.endpoint}`,
        defaultMessage:
          meta.endpoint.charAt(0).toUpperCase() + meta.endpoint.slice(1),
      }),
      href: `/Storage/${meta.endpoint}`,
    },
    {
      label: intl.formatMessage({
        id: "storage.edit.title",
        defaultMessage: "Edit",
      }),
      href: `/Storage/${meta.endpoint}/${id}/edit`,
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
          id="storage.edit.heading"
          defaultMessage="Edit {type}"
          values={{ type: type.charAt(0).toUpperCase() + type.slice(1) }}
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
          id="storage-edit-name"
          labelText={
            meta.nameField === "label"
              ? intl.formatMessage({
                  id: "label.label",
                  defaultMessage: "Label",
                })
              : intl.formatMessage({ id: "label.name", defaultMessage: "Name" })
          }
          value={formData[meta.nameField]}
          onChange={(e) => updateField(meta.nameField, e.target.value)}
        />
        <TextInput
          id="storage-edit-code"
          labelText={intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          })}
          value={formData.code}
          onChange={(e) => updateField("code", e.target.value)}
        />
        {type === "room" && (
          <TextInput
            id="storage-edit-description"
            labelText={intl.formatMessage({
              id: "label.description",
              defaultMessage: "Description",
            })}
            value={formData.description}
            onChange={(e) => updateField("description", e.target.value)}
          />
        )}
        {meta.parentField && (
          <Dropdown
            id="storage-edit-parent"
            titleText={intl.formatMessage({
              id: meta.parentLabelId,
              defaultMessage: meta.parentLabel,
            })}
            label={intl.formatMessage(
              {
                id: "storage.edit.selectParent",
                defaultMessage: "Select {parent}",
              },
              { parent: meta.parentLabel?.toLowerCase() },
            )}
            items={parentOptions}
            itemToString={(item) => (item ? item.name || item.label || "" : "")}
            selectedItem={
              parentOptions.find(
                (p) => String(p.id) === String(formData[meta.parentField]),
              ) || null
            }
            onChange={({ selectedItem }) =>
              updateField(
                meta.parentField,
                selectedItem ? String(selectedItem.id) : "",
              )
            }
          />
        )}
        <Checkbox
          id="storage-edit-active"
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
