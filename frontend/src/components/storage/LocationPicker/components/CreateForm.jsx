import React, { useState, useEffect } from "react";
import {
  Dropdown,
  Button,
  Modal,
  TextInput,
  NumberInput,
  InlineNotification,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl, FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import useCreateLocation from "../../pages/hooks/useCreateLocation";

/**
 * CreateForm — 5-level cascading dropdown UI for the LocationPicker.
 *
 * Local state:
 *   - options: { room, device, shelf, rack, box } — API-fetched lists per level
 *   - inlineCreate: { level, name } | null — one open inline-create dialog
 *
 * Selection state lives in the parent's reducer (props.selection +
 * props.onLevelChange). SET_LEVEL handles the cascading-clear invariant.
 *
 * All 5 dropdowns are always rendered; descendants are disabled until
 * their parent is selected. If a selected rack has no boxes, the Box
 * dropdown is simply empty.
 *
 * Endpoints:
 *   GET  /rest/storage/rooms
 *   GET  /rest/storage/devices?roomId={id}
 *   GET  /rest/storage/shelves?deviceId={id}
 *   GET  /rest/storage/racks?shelfId={id}
 *   GET  /rest/storage/boxes?rackId={id}
 *   POST /rest/storage/{rooms|devices|shelves|racks|boxes}
 */

// Backend naming inconsistency captured here so callers don't branch:
//   - GET list parent param: "{parent}Id"   (e.g. roomId, deviceId)
//   - POST body parent field: "parent{Parent}Id" (e.g. parentRoomId)
//   - Identifier field: "name" for Room/Device, "label" for Shelf/Rack/Box
const LEVELS = [
  {
    key: "room",
    label: "Room",
    labelId: "storage.nav.room",
    endpoint: "rooms",
    parentLevel: null,
    listParam: null,
    createParam: null,
    createField: "name",
  },
  {
    key: "device",
    label: "Device",
    labelId: "storage.nav.device",
    endpoint: "devices",
    parentLevel: "room",
    listParam: "roomId",
    createParam: "parentRoomId",
    createField: "name",
  },
  {
    key: "shelf",
    label: "Shelf",
    labelId: "storage.nav.shelf",
    endpoint: "shelves",
    parentLevel: "device",
    listParam: "deviceId",
    createParam: "parentDeviceId",
    createField: "label",
  },
  {
    key: "rack",
    label: "Rack",
    labelId: "storage.nav.rack",
    endpoint: "racks",
    parentLevel: "shelf",
    listParam: "shelfId",
    createParam: "parentShelfId",
    createField: "label",
  },
  {
    key: "box",
    label: "Box",
    labelId: "storage.nav.box",
    endpoint: "boxes",
    parentLevel: "rack",
    listParam: "rackId",
    createParam: "parentRackId",
    createField: "label",
  },
];

export default function CreateForm({ selection, onLevelChange }) {
  const intl = useIntl();
  const createLocation = useCreateLocation();
  const [options, setOptions] = useState({
    room: [],
    device: [],
    shelf: [],
    rack: [],
    box: [],
  });
  const [inlineCreate, setInlineCreate] = useState(null); // { level, name, type? } | null
  const [createError, setCreateError] = useState(null);
  // Device types are pulled from the backend (StorageDevice.DeviceType enum)
  // so adding/removing a value only touches the Java enum.
  const [deviceTypes, setDeviceTypes] = useState([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/storage/devices/types", (response) => {
      if (Array.isArray(response)) {
        setDeviceTypes(response);
      }
    });
  }, []);

  const openInlineCreate = (level) => {
    setCreateError(null);
    setInlineCreate({
      level,
      name: "",
      type: level === "device" ? "" : null,
      // Box requires a code (10 chars max) and grid dimensions (rows/cols, min 1)
      // per StorageBoxForm validation. Picker collects them inline.
      ...(level === "box"
        ? { code: "", rows: "", columns: "" }
        : { code: null, rows: null, columns: null }),
    });
  };

  const closeInlineCreate = () => {
    setCreateError(null);
    setInlineCreate(null);
  };

  const setLevelOptions = (key, values) => {
    setOptions((prev) => ({
      ...prev,
      [key]: Array.isArray(values) ? values : [],
    }));
  };

  // Room options have no parent dependency.
  useEffect(() => {
    getFromOpenElisServer("/rest/storage/rooms", (response) => {
      setLevelOptions("room", response);
    });
  }, []);

  const roomId = selection.room?.id || null;
  useEffect(() => {
    if (roomId == null) {
      setOptions((prev) =>
        prev.device.length === 0 ? prev : { ...prev, device: [] },
      );
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/devices?roomId=${roomId}`,
      (response) => {
        setLevelOptions("device", response);
      },
    );
  }, [roomId]);

  const deviceId = selection.device?.id || null;
  useEffect(() => {
    if (deviceId == null) {
      setOptions((prev) =>
        prev.shelf.length === 0 ? prev : { ...prev, shelf: [] },
      );
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/shelves?deviceId=${deviceId}`,
      (response) => {
        setLevelOptions("shelf", response);
      },
    );
  }, [deviceId]);

  const shelfId = selection.shelf?.id || null;
  useEffect(() => {
    if (shelfId == null) {
      setOptions((prev) =>
        prev.rack.length === 0 ? prev : { ...prev, rack: [] },
      );
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/racks?shelfId=${shelfId}`,
      (response) => {
        setLevelOptions("rack", response);
      },
    );
  }, [shelfId]);

  const rackId = selection.rack?.id || null;
  useEffect(() => {
    if (rackId == null) {
      setOptions((prev) =>
        prev.box.length === 0 ? prev : { ...prev, box: [] },
      );
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/boxes?rackId=${rackId}`,
      (response) => {
        setLevelOptions("box", response);
      },
    );
  }, [rackId]);

  const handleCreate = async () => {
    if (!inlineCreate || !inlineCreate.name.trim()) return;
    const { level, name, type, code, rows, columns } = inlineCreate;
    // Device requires a type per StorageDeviceForm validation; refuse to
    // submit without one (the Create button is also disabled in that state).
    if (level === "device" && !type) return;
    // Box requires a non-blank code and grid dimensions ≥ 1 per StorageBoxForm.
    if (
      level === "box" &&
      (!code?.trim() ||
        !Number.isInteger(Number(rows)) ||
        Number(rows) < 1 ||
        !Number.isInteger(Number(columns)) ||
        Number(columns) < 1)
    ) {
      return;
    }
    const meta = LEVELS.find((l) => l.key === level);
    // Map to the right backend field — Room/Device use `name`,
    // Shelf/Rack/Box use `label` (see LEVELS.createField).
    const body = { [meta.createField]: name.trim(), active: true };
    if (level === "device") {
      body.type = type;
    }
    if (level === "box") {
      body.code = code.trim();
      body.rows = Number(rows);
      body.columns = Number(columns);
    }
    if (level === "rack") {
      // storage_rack.code is NOT NULL on the DB. Derive a code from the
      // label here on the frontend (uppercase, alnum only, max 10 chars)
      // so the picker doesn't fail with a constraint error.
      body.code = name
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]/g, "")
        .slice(0, 10);
    }
    // Devices/shelves/racks/boxes need their parent id; the picker
    // disables the "Add new" button until the parent is selected so we
    // can rely on it being present here. Backend uses `parent{Parent}Id`
    // for POST bodies (distinct from GET's `{parent}Id` query param).
    if (meta.parentLevel) {
      body[meta.createParam] = selection[meta.parentLevel].id;
    }
    try {
      const response = await createLocation(meta.endpoint, body);
      onLevelChange(level, {
        id: response.id,
        name: response.name || response.label,
      });
      closeInlineCreate();
    } catch (error) {
      // Keep the modal open so the user can correct the input and retry. The
      // message is already user-facing (see useCreateLocation).
      setCreateError(
        error?.message ||
          intl.formatMessage({
            id: "storage.picker.inlineCreate.error",
            defaultMessage: "Failed to create location",
          }),
      );
    }
  };

  return (
    <div className="storage-location-picker-create-form">
      {LEVELS.map(({ key, label, labelId, parentLevel }) => {
        const parentValue = parentLevel ? selection[parentLevel] : true;
        const enabled = parentValue != null;
        const items = options[key] || [];
        const levelLabel = intl.formatMessage({
          id: labelId,
          defaultMessage: label,
        });
        return (
          <div key={key} className="storage-location-picker-create-row">
            <Dropdown
              id={`location-picker-${key}`}
              titleText={levelLabel}
              label={intl.formatMessage(
                {
                  id: "storage.picker.select",
                  defaultMessage: "Select {level}",
                },
                { level: levelLabel.toLowerCase() },
              )}
              items={items}
              itemToString={(item) =>
                item ? item.name || item.label || "" : ""
              }
              selectedItem={selection[key] || null}
              disabled={!enabled}
              onChange={({ selectedItem }) => {
                onLevelChange(
                  key,
                  selectedItem
                    ? {
                        id: selectedItem.id,
                        name: selectedItem.name || selectedItem.label,
                      }
                    : undefined,
                );
              }}
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Add}
              disabled={!enabled}
              onClick={() => openInlineCreate(key)}
            >
              <FormattedMessage
                id="storage.picker.addNew"
                defaultMessage="Add new"
              />
            </Button>
          </div>
        );
      })}

      {inlineCreate && (
        <Modal
          open
          modalHeading={intl.formatMessage(
            {
              id: "storage.picker.inlineCreate.heading",
              defaultMessage: "Create new {level}",
            },
            {
              level: intl
                .formatMessage({
                  id: LEVELS.find((l) => l.key === inlineCreate.level).labelId,
                  defaultMessage: LEVELS.find(
                    (l) => l.key === inlineCreate.level,
                  ).label,
                })
                .toLowerCase(),
            },
          )}
          primaryButtonText={intl.formatMessage({
            id: "label.button.create",
            defaultMessage: "Create",
          })}
          primaryButtonDisabled={
            !inlineCreate.name.trim() ||
            (inlineCreate.level === "device" && !inlineCreate.type) ||
            (inlineCreate.level === "box" &&
              (!inlineCreate.code?.trim() ||
                !(Number(inlineCreate.rows) >= 1) ||
                !(Number(inlineCreate.columns) >= 1)))
          }
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
            defaultMessage: "Cancel",
          })}
          onRequestSubmit={handleCreate}
          onRequestClose={closeInlineCreate}
          onSecondarySubmit={closeInlineCreate}
        >
          {createError && (
            <InlineNotification
              kind="error"
              role="alert"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "storage.picker.inlineCreate.error.title",
                defaultMessage: "Could not create location",
              })}
              subtitle={createError}
            />
          )}
          <TextInput
            id="location-picker-inline-create-name"
            labelText={intl.formatMessage({
              id: "label.name",
              defaultMessage: "Name",
            })}
            value={inlineCreate.name}
            onChange={(e) =>
              setInlineCreate({ ...inlineCreate, name: e.target.value })
            }
          />
          {inlineCreate.level === "device" && (
            <Dropdown
              id="location-picker-inline-create-device-type"
              titleText={intl.formatMessage({
                id: "storage.device.type",
                defaultMessage: "Device type",
              })}
              label={intl.formatMessage({
                id: "storage.picker.select",
                defaultMessage: "Select device type",
              })}
              items={deviceTypes}
              itemToString={(item) => item || ""}
              selectedItem={inlineCreate.type || null}
              onChange={({ selectedItem }) =>
                setInlineCreate({
                  ...inlineCreate,
                  type: selectedItem || "",
                })
              }
            />
          )}
          {inlineCreate.level === "box" && (
            <>
              <TextInput
                id="location-picker-inline-create-box-code"
                labelText={intl.formatMessage({
                  id: "label.code",
                  defaultMessage: "Code",
                })}
                helperText={intl.formatMessage({
                  id: "storage.box.code.helper",
                  defaultMessage: "Up to 10 characters",
                })}
                maxLength={10}
                value={inlineCreate.code || ""}
                onChange={(e) =>
                  setInlineCreate({ ...inlineCreate, code: e.target.value })
                }
              />
              <NumberInput
                id="location-picker-inline-create-box-rows"
                label={intl.formatMessage({
                  id: "storage.box.rows",
                  defaultMessage: "Rows",
                })}
                min={1}
                value={
                  inlineCreate.rows === "" ? "" : Number(inlineCreate.rows)
                }
                onChange={(_e, { value }) =>
                  setInlineCreate({ ...inlineCreate, rows: value })
                }
              />
              <NumberInput
                id="location-picker-inline-create-box-columns"
                label={intl.formatMessage({
                  id: "storage.box.columns",
                  defaultMessage: "Columns",
                })}
                min={1}
                value={
                  inlineCreate.columns === ""
                    ? ""
                    : Number(inlineCreate.columns)
                }
                onChange={(_e, { value }) =>
                  setInlineCreate({ ...inlineCreate, columns: value })
                }
              />
            </>
          )}
        </Modal>
      )}
    </div>
  );
}
