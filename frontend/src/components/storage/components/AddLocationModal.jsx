import React, { useEffect, useMemo, useState } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  Checkbox,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import useCreateLocation from "../pages/hooks/useCreateLocation";

const LEVEL_META = {
  room: {
    endpoint: "rooms",
    nameField: "name",
    parentEndpoint: null,
    parentField: null,
    parentLabelId: null,
    titleId: "storage.add.room",
    titleDefault: "Add Room",
  },
  device: {
    endpoint: "devices",
    nameField: "name",
    parentEndpoint: "rooms",
    parentField: "parentRoomId",
    parentLabelId: "storage.nav.room",
    titleId: "storage.add.device",
    titleDefault: "Add Device",
  },
  shelf: {
    endpoint: "shelves",
    nameField: "label",
    parentEndpoint: "devices",
    parentField: "parentDeviceId",
    parentLabelId: "storage.nav.device",
    titleId: "storage.add.shelf",
    titleDefault: "Add Shelf",
  },
  rack: {
    endpoint: "racks",
    nameField: "label",
    parentEndpoint: "shelves",
    parentField: "parentShelfId",
    parentLabelId: "storage.nav.shelf",
    titleId: "storage.add.rack",
    titleDefault: "Add Rack",
  },
  box: {
    endpoint: "boxes",
    nameField: "label",
    parentEndpoint: "racks",
    parentField: "parentRackId",
    parentLabelId: "storage.nav.rack",
    titleId: "storage.add.box",
    titleDefault: "Add Box",
  },
};

// Same presets AddBoxPage offered; "custom" leaves rows/columns editable.
const GRID_PRESETS = [
  { id: "9x9", label: "9x9", rows: 9, columns: 9 },
  { id: "10x10", label: "10x10", rows: 10, columns: 10 },
  { id: "8x12", label: "8x12 (96-well plate)", rows: 8, columns: 12 },
  { id: "4x6", label: "4x6", rows: 4, columns: 6 },
  { id: "6x8", label: "6x8", rows: 6, columns: 8 },
  { id: "16x24", label: "16x24 (384-well plate)", rows: 16, columns: 24 },
  { id: "custom", label: "Custom", rows: null, columns: null },
];

const emptyForm = (level) => ({
  name: "",
  code: "",
  description: "",
  parentId: "",
  deviceType: "",
  preset: level === "box" ? "8x12" : null,
  rows: "8",
  columns: "12",
  active: true,
});

export default function AddLocationModal({ level, open, onClose, onCreated }) {
  const intl = useIntl();
  const createLocation = useCreateLocation();
  const meta = LEVEL_META[level];

  const [form, setForm] = useState(() => emptyForm(level));
  const [parents, setParents] = useState([]);
  const [deviceTypes, setDeviceTypes] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Reopening starts clean rather than showing the previous attempt. Adjusting
  // during render rather than in an effect avoids a frame where the modal shows
  // the last attempt's values.
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open) {
      setForm(emptyForm(level));
      setError(null);
    }
  }

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

  const nameLabel = intl.formatMessage(
    meta?.nameField === "name"
      ? { id: "label.name", defaultMessage: "Name" }
      : { id: "label.label", defaultMessage: "Label" },
  );

  const invalid = useMemo(() => {
    if (!form.name.trim()) return true;
    if (level === "device" && !form.deviceType) return true;
    if (level === "box") {
      if (!form.code.trim()) return true;
      if (!(Number(form.rows) >= 1) || !(Number(form.columns) >= 1))
        return true;
    }
    return false;
  }, [form, level]);

  const handleSubmit = async () => {
    if (invalid) return;
    setSaving(true);
    setError(null);

    const payload = {
      [meta.nameField]: form.name.trim(),
      code: form.code.trim() || null,
      active: form.active,
    };
    if (level === "room") payload.description = form.description.trim() || null;
    if (level === "device") payload.type = form.deviceType || null;
    if (level === "box") {
      payload.rows = Number(form.rows);
      payload.columns = Number(form.columns);
    }
    if (meta.parentField) payload[meta.parentField] = form.parentId || null;

    try {
      await createLocation(meta.endpoint, payload);
      onCreated();
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

  const parentItems = parents.map((p) => ({
    id: String(p.id),
    text: p.name || p.label || "",
  }));

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: meta.titleId,
        defaultMessage: meta.titleDefault,
      })}
      primaryButtonText={intl.formatMessage({
        id: "label.button.create",
        defaultMessage: "Create",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      primaryButtonDisabled={invalid || saving}
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

        <TextInput
          id="storage-add-modal-name"
          labelText={nameLabel}
          value={form.name}
          onChange={(e) => update("name", e.target.value)}
          required
        />

        <TextInput
          id="storage-add-modal-code"
          labelText={intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          })}
          value={form.code}
          onChange={(e) => update("code", e.target.value)}
        />

        {level === "room" && (
          <TextInput
            id="storage-add-modal-description"
            labelText={intl.formatMessage({
              id: "label.description",
              defaultMessage: "Description",
            })}
            value={form.description}
            onChange={(e) => update("description", e.target.value)}
          />
        )}

        {meta.parentEndpoint && (
          <Dropdown
            id="storage-add-modal-parent"
            titleText={intl.formatMessage({ id: meta.parentLabelId })}
            label={intl.formatMessage({
              id: "storage.picker.select",
              defaultMessage: "Select",
            })}
            items={parentItems}
            itemToString={(item) => item?.text || ""}
            selectedItem={
              parentItems.find((p) => p.id === String(form.parentId)) || null
            }
            onChange={({ selectedItem }) =>
              update("parentId", selectedItem?.id || "")
            }
          />
        )}

        {level === "device" && (
          <Dropdown
            id="storage-add-modal-device-type"
            titleText={intl.formatMessage({
              id: "storage.device.type",
              defaultMessage: "Device type",
            })}
            label={intl.formatMessage({
              id: "storage.picker.select",
              defaultMessage: "Select",
            })}
            items={deviceTypes}
            itemToString={(item) => item || ""}
            selectedItem={form.deviceType || null}
            onChange={({ selectedItem }) =>
              update("deviceType", selectedItem || "")
            }
          />
        )}

        {level === "box" && (
          <>
            <Dropdown
              id="storage-add-modal-grid"
              titleText={intl.formatMessage({
                id: "storage.box.gridSize",
                defaultMessage: "Grid size",
              })}
              label={intl.formatMessage({
                id: "storage.picker.select",
                defaultMessage: "Select",
              })}
              items={GRID_PRESETS}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                GRID_PRESETS.find((g) => g.id === form.preset) || null
              }
              onChange={({ selectedItem }) => {
                update("preset", selectedItem?.id || "custom");
                if (selectedItem?.rows) {
                  update("rows", String(selectedItem.rows));
                  update("columns", String(selectedItem.columns));
                }
              }}
            />
            <NumberInput
              id="storage-add-modal-rows"
              label={intl.formatMessage({
                id: "storage.box.rows",
                defaultMessage: "Rows",
              })}
              min={1}
              value={form.rows === "" ? "" : Number(form.rows)}
              disabled={form.preset !== "custom"}
              onChange={(_e, { value }) => update("rows", String(value))}
            />
            <NumberInput
              id="storage-add-modal-columns"
              label={intl.formatMessage({
                id: "storage.box.columns",
                defaultMessage: "Columns",
              })}
              min={1}
              value={form.columns === "" ? "" : Number(form.columns)}
              disabled={form.preset !== "custom"}
              onChange={(_e, { value }) => update("columns", String(value))}
            />
          </>
        )}

        <Checkbox
          id="storage-add-modal-active"
          labelText={intl.formatMessage({
            id: "label.active",
            defaultMessage: "Active",
          })}
          checked={form.active}
          onChange={(_e, { checked }) => update("active", checked)}
        />
      </Stack>
    </Modal>
  );
}
