import React, { useEffect, useState } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  Checkbox,
  InlineLoading,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { storageLevel } from "../storageLevels";

/**
 * EditLocationModal — edits one storage location at any of the five levels.
 * Fields are the union of what each level carries; boxes add rows/columns.
 */
export default function EditLocationModal({
  level,
  id,
  open,
  onClose,
  onUpdated,
}) {
  const intl = useIntl();
  const meta = storageLevel(level);

  const [form, setForm] = useState(null);
  const [parents, setParents] = useState([]);
  const [deviceTypes, setDeviceTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Opening on a different row starts clean rather than flashing the previous
  // one. Adjusting during render rather than inside the effect avoids both that
  // frame and the cascading render a synchronous setState in an effect causes;
  // AddLocationModal resets itself the same way.
  const rowKey = open && meta && id != null ? `${level}:${id}` : null;
  const [loadedKey, setLoadedKey] = useState(null);
  if (rowKey !== loadedKey) {
    setLoadedKey(rowKey);
    setForm(null);
    setError(null);
    setLoading(rowKey !== null);
  }

  useEffect(() => {
    if (!open || !meta || id == null) return;
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/storage/${meta.endpoint}/${encodeURIComponent(String(id))}`,
      (response) => {
        // Closing and reopening on another row leaves this request in flight.
        // Its values belong to the row that is gone, and Save would write them
        // back under the id now open.
        if (controller.signal.aborted) return;
        if (response && !response.error) {
          setForm({
            name:
              response[meta.nameField] || response.name || response.label || "",
            code: response.code || "",
            description: response.description || "",
            // A shelf's GET names its device deviceId; the other levels spell
            // the parent out as parentRoomId, parentShelfId, parentRackId.
            parentId: meta.parentField
              ? String(response[meta.parentField] || response.deviceId || "")
              : "",
            type: response.type || "",
            // Fields this modal does not show. A column left out of the
            // payload is written back as null, so it has to be carried
            // back untouched.
            temperatureSetting: response.temperatureSetting ?? null,
            capacityLimit: response.capacityLimit ?? null,
            positionSchemaHint: response.positionSchemaHint ?? null,
            rows: response.rows != null ? String(response.rows) : "",
            columns: response.columns != null ? String(response.columns) : "",
            active: response.active !== false,
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
      controller.signal,
    );
    return () => controller.abort();
  }, [open, id, meta?.endpoint, meta?.nameField, meta?.parentField]);

  useEffect(() => {
    if (!open || !meta?.parentEndpoint) return;
    getFromOpenElisServer(`/rest/storage/${meta.parentEndpoint}`, (res) =>
      setParents(Array.isArray(res) ? res : []),
    );
  }, [open, meta?.parentEndpoint]);

  useEffect(() => {
    if (!open || level !== "device") return;
    getFromOpenElisServer("/rest/storage/devices/types", (res) =>
      setDeviceTypes(Array.isArray(res) ? res : []),
    );
  }, [open, level]);

  const update = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = async () => {
    if (!form) return;
    setSaving(true);
    setError(null);

    const payload = {
      [meta.nameField]: form.name,
      code: form.code || null,
      active: form.active,
    };
    if (meta.parentField) payload[meta.parentField] = form.parentId || null;
    if (level === "room") payload.description = form.description || null;
    if (level === "device") {
      payload.type = form.type || null;
      payload.temperatureSetting = form.temperatureSetting;
      payload.capacityLimit = form.capacityLimit;
    }
    if (level === "shelf") payload.capacityLimit = form.capacityLimit;
    if (level === "box") {
      payload.type = form.type || null;
      payload.positionSchemaHint = form.positionSchemaHint;
      payload.rows = form.rows ? parseInt(form.rows, 10) : null;
      payload.columns = form.columns ? parseInt(form.columns, 10) : null;
    }

    try {
      const response = await new Promise((resolve) => {
        putToOpenElisServerFullResponse(
          `/rest/storage/${meta.endpoint}/${encodeURIComponent(String(id))}`,
          JSON.stringify(payload),
          (res) => resolve(res),
        );
      });
      if (!response) {
        throw new Error(
          intl.formatMessage({
            id: "storage.edit.error.saveFailed",
            defaultMessage: "Save failed",
          }),
        );
      }
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(
          body.error ||
            body.message ||
            // Bean-validation failures arrive as {errors: {field: message}},
            // which carries neither an `error` nor a `message` key.
            (body.errors && Object.values(body.errors).join(", ")) ||
            intl.formatMessage(
              {
                id: "storage.edit.error.saveHttp",
                defaultMessage: "Save failed (HTTP {status})",
              },
              { status: response.status },
            ),
        );
      }
      onUpdated();
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

  if (!meta) return null;

  const levelLabel = intl.formatMessage({
    id: meta.labelId,
    defaultMessage: meta.label,
  });

  const nameLabel = intl.formatMessage(
    meta.nameField === "name"
      ? { id: "label.name", defaultMessage: "Name" }
      : { id: "label.label", defaultMessage: "Label" },
  );

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage(
        { id: "storage.edit.heading", defaultMessage: "Edit {type}" },
        { type: levelLabel },
      )}
      primaryButtonText={intl.formatMessage({
        id: "label.save",
        defaultMessage: "Save",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      primaryButtonDisabled={
        !form || saving || (level === "device" && !form.type)
      }
      onRequestSubmit={handleSubmit}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
    >
      <Stack gap={5}>
        {error && (
          <InlineNotification
            kind="error"
            role="alert"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "label.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
          />
        )}

        {loading && (
          <InlineLoading
            description={intl.formatMessage({
              id: "label.loading",
              defaultMessage: "Loading...",
            })}
          />
        )}

        {form && (
          <>
            <TextInput
              id="storage-edit-modal-name"
              labelText={nameLabel}
              value={form.name}
              onChange={(e) => update("name", e.target.value)}
            />

            <TextInput
              id="storage-edit-modal-code"
              labelText={intl.formatMessage({
                id: "label.code",
                defaultMessage: "Code",
              })}
              value={form.code}
              onChange={(e) => update("code", e.target.value)}
            />

            {level === "room" && (
              <TextInput
                id="storage-edit-modal-description"
                labelText={intl.formatMessage({
                  id: "label.description",
                  defaultMessage: "Description",
                })}
                value={form.description}
                onChange={(e) => update("description", e.target.value)}
              />
            )}

            {/* Boxes render their rack read-only: updateBox writes back the
                rack the stored row already has, so an editable picker here
                would report success and move nothing. */}
            {meta.parentEndpoint && (
              <Dropdown
                disabled={level === "box"}
                id="storage-edit-modal-parent"
                titleText={intl.formatMessage({
                  id: meta.parentLabelId,
                  defaultMessage: meta.parentLabel,
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
                        defaultMessage: meta.parentLabel,
                      })
                      .toLowerCase(),
                  },
                )}
                items={parents}
                itemToString={(item) => item?.name || item?.label || ""}
                selectedItem={
                  parents.find((p) => String(p.id) === String(form.parentId)) ||
                  null
                }
                onChange={({ selectedItem }) =>
                  update(
                    "parentId",
                    selectedItem ? String(selectedItem.id) : "",
                  )
                }
              />
            )}

            {level === "device" && (
              <Dropdown
                id="storage-edit-modal-device-type"
                titleText={intl.formatMessage({
                  id: "storage.device.type",
                  defaultMessage: "Device type",
                })}
                label={intl.formatMessage({
                  id: "storage.picker.selectDeviceType",
                  defaultMessage: "Select device type",
                })}
                items={deviceTypes}
                itemToString={(item) => item || ""}
                selectedItem={form.type || null}
                onChange={({ selectedItem }) =>
                  update("type", selectedItem || "")
                }
              />
            )}

            {level === "box" && (
              <>
                <NumberInput
                  id="storage-edit-modal-rows"
                  label={intl.formatMessage({
                    id: "storage.box.rows",
                    defaultMessage: "Rows",
                  })}
                  min={0}
                  value={form.rows}
                  onChange={(_e, { value }) =>
                    update("rows", String(value ?? ""))
                  }
                />
                <NumberInput
                  id="storage-edit-modal-columns"
                  label={intl.formatMessage({
                    id: "storage.box.columns",
                    defaultMessage: "Columns",
                  })}
                  min={0}
                  value={form.columns}
                  onChange={(_e, { value }) =>
                    update("columns", String(value ?? ""))
                  }
                />
              </>
            )}

            <Checkbox
              id="storage-edit-modal-active"
              labelText={intl.formatMessage({
                id: "label.active",
                defaultMessage: "Active",
              })}
              checked={form.active}
              onChange={(_e, { checked }) => update("active", checked)}
            />
          </>
        )}
      </Stack>
    </Modal>
  );
}
