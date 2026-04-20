import React, { useEffect, useMemo, useState } from "react";
import { useHistory } from "react-router-dom";
import {
  Button,
  Checkbox,
  Dropdown,
  InlineLoading,
  InlineNotification,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import { getFromOpenElisServer } from "../../utils/Utils";
import useCreateLocation from "./hooks/useCreateLocation";

const TYPE_META = {
  room: {
    endpoint: "rooms",
    nameField: "name",
    parentEndpoint: null,
    parentField: null,
    parentLabelId: null,
    addLabelId: "storage.add.room",
    addLabelDefault: "Add Room",
  },
  device: {
    endpoint: "devices",
    nameField: "name",
    parentEndpoint: "rooms",
    parentField: "parentRoomId",
    parentLabelId: "storage.nav.room",
    addLabelId: "storage.add.device",
    addLabelDefault: "Add Device",
  },
  shelf: {
    endpoint: "shelves",
    nameField: "label",
    parentEndpoint: "devices",
    parentField: "parentDeviceId",
    parentLabelId: "storage.nav.device",
    addLabelId: "storage.add.shelf",
    addLabelDefault: "Add Shelf",
  },
  rack: {
    endpoint: "racks",
    nameField: "label",
    parentEndpoint: "shelves",
    parentField: "parentShelfId",
    parentLabelId: "storage.nav.shelf",
    addLabelId: "storage.add.rack",
    addLabelDefault: "Add Rack",
  },
};

export default function AddLocationPage({ type }) {
  const intl = useIntl();
  const history = useHistory();
  const createLocation = useCreateLocation();
  const meta = TYPE_META[type];

  const [parentOptions, setParentOptions] = useState([]);
  const [loadingParents, setLoadingParents] = useState(
    Boolean(meta.parentField),
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [formData, setFormData] = useState({
    [meta.nameField]: "",
    code: "",
    active: true,
    description: "",
    ...(meta.parentField ? { [meta.parentField]: "" } : {}),
  });

  useEffect(() => {
    if (!meta.parentEndpoint) {
      return;
    }
    setLoadingParents(true);
    getFromOpenElisServer(
      `/rest/storage/${meta.parentEndpoint}`,
      (response) => {
        if (Array.isArray(response)) {
          setParentOptions(response);
        }
        setLoadingParents(false);
      },
    );
  }, [meta.parentEndpoint]);

  const crumbs = useMemo(
    () => [
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
          id: meta.addLabelId,
          defaultMessage: meta.addLabelDefault,
        }),
        href: `/Storage/${meta.endpoint}/new`,
      },
    ],
    [intl, meta.addLabelDefault, meta.addLabelId, meta.endpoint],
  );

  const navigateBack = () => {
    history.push(`/Storage/${meta.endpoint}?t=${Date.now()}`);
  };

  const updateField = (key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = async () => {
    setSaving(true);
    setError(null);

    const payload = {
      [meta.nameField]: formData[meta.nameField]?.trim(),
      code: formData.code?.trim() || null,
      active: formData.active,
    };
    if (type === "room") {
      payload.description = formData.description?.trim() || null;
    }
    if (meta.parentField) {
      payload[meta.parentField] = formData[meta.parentField] || null;
    }

    try {
      await createLocation(meta.endpoint, payload);
      navigateBack();
    } catch (e) {
      setError(
        e?.message ||
          intl.formatMessage({
            id: "storage.edit.error.saveFailed",
            defaultMessage: "Save failed",
          }),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="storage-add-page">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage
          id={meta.addLabelId}
          defaultMessage={meta.addLabelDefault}
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
          lowContrast
        />
      )}

      <div className="storage-edit-page-form" style={{ maxWidth: "32rem" }}>
        <TextInput
          id="storage-add-name"
          labelText={
            meta.nameField === "label"
              ? intl.formatMessage({
                  id: "label.label",
                  defaultMessage: "Label",
                })
              : intl.formatMessage({
                  id: "label.name",
                  defaultMessage: "Name",
                })
          }
          value={formData[meta.nameField]}
          onChange={(e) => updateField(meta.nameField, e.target.value)}
        />
        <TextInput
          id="storage-add-code"
          labelText={intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          })}
          value={formData.code}
          onChange={(e) => updateField("code", e.target.value)}
        />

        {type === "room" && (
          <TextInput
            id="storage-add-description"
            labelText={intl.formatMessage({
              id: "label.description",
              defaultMessage: "Description",
            })}
            value={formData.description}
            onChange={(e) => updateField("description", e.target.value)}
          />
        )}

        {meta.parentField && (
          <>
            {loadingParents ? (
              <InlineLoading
                description={intl.formatMessage({
                  id: "label.loading",
                  defaultMessage: "Loading...",
                })}
              />
            ) : (
              <Dropdown
                id="storage-add-parent"
                titleText={intl.formatMessage({
                  id: meta.parentLabelId,
                  defaultMessage: "Parent",
                })}
                label={intl.formatMessage(
                  {
                    id: "storage.edit.selectParent",
                    defaultMessage: "Select {parent}",
                  },
                  {
                    parent: intl
                      .formatMessage({
                        id: meta.parentLabelId,
                        defaultMessage: "parent",
                      })
                      .toLowerCase(),
                  },
                )}
                items={parentOptions}
                itemToString={(item) =>
                  item ? item.name || item.label || "" : ""
                }
                selectedItem={
                  parentOptions.find(
                    (option) =>
                      String(option.id) === String(formData[meta.parentField]),
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
          </>
        )}

        <Checkbox
          id="storage-add-active"
          labelText={intl.formatMessage({
            id: "label.active",
            defaultMessage: "Active",
          })}
          checked={formData.active}
          onChange={(_event, { checked }) => updateField("active", checked)}
        />
      </div>

      <div className="storage-edit-page-actions" style={{ marginTop: "1rem" }}>
        <Button kind="secondary" onClick={navigateBack}>
          <FormattedMessage id="label.cancel" defaultMessage="Cancel" />
        </Button>
        <Button kind="primary" onClick={handleSubmit} disabled={saving}>
          <FormattedMessage id="label.add" defaultMessage="Add" />
        </Button>
      </div>
    </div>
  );
}
