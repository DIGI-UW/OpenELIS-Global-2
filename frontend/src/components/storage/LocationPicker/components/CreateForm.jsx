import React, { useState, useEffect } from "react";
import { Dropdown, Button, Modal, TextInput } from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

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
  const [options, setOptions] = useState({
    room: [],
    device: [],
    shelf: [],
    rack: [],
    box: [],
  });
  const [inlineCreate, setInlineCreate] = useState(null); // { level, name } | null

  // Cascading fetch: when a parent's selection changes, refetch the
  // dependent level's options. The picker reducer guarantees the parent
  // selection only changes when a higher-level value is set/cleared.
  LEVELS.forEach(({ key, endpoint, parentLevel, listParam }) => {
    const parentValue = parentLevel ? selection[parentLevel] : null;
    const parentId = parentValue ? parentValue.id : null;
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useEffect(() => {
      if (parentLevel && parentId == null) {
        // No parent selected → no options for this level
        setOptions((prev) =>
          prev[key].length === 0 ? prev : { ...prev, [key]: [] },
        );
        return;
      }
      const url = parentLevel
        ? `/rest/storage/${endpoint}?${listParam}=${parentId}`
        : `/rest/storage/${endpoint}`;
      getFromOpenElisServer(url, (response) => {
        setOptions((prev) => ({
          ...prev,
          [key]: Array.isArray(response) ? response : [],
        }));
      });
      // Listing only the IDs that drive this level's fetch — selection
      // identity changes are debounced into a stable id-derived dep.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [parentId]);
  });

  const handleCreate = () => {
    if (!inlineCreate || !inlineCreate.name.trim()) return;
    const { level, name } = inlineCreate;
    const meta = LEVELS.find((l) => l.key === level);
    // Map to the right backend field — Room/Device use `name`,
    // Shelf/Rack/Box use `label` (see LEVELS.createField).
    const body = { [meta.createField]: name.trim(), active: true };
    // Devices/shelves/racks/boxes need their parent id; the picker
    // disables the "Add new" button until the parent is selected so we
    // can rely on it being present here. Backend uses `parent{Parent}Id`
    // for POST bodies (distinct from GET's `{parent}Id` query param).
    if (meta.parentLevel) {
      body[meta.createParam] = selection[meta.parentLevel].id;
    }
    postToOpenElisServerJsonResponse(
      `/rest/storage/${meta.endpoint}`,
      JSON.stringify(body),
      (response) => {
        if (response && response.id) {
          onLevelChange(level, {
            id: response.id,
            name: response.name || response.label,
          });
          setInlineCreate(null);
        }
      },
    );
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
              onClick={() => setInlineCreate({ level: key, name: "" })}
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
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
            defaultMessage: "Cancel",
          })}
          onRequestSubmit={handleCreate}
          onRequestClose={() => setInlineCreate(null)}
          onSecondarySubmit={() => setInlineCreate(null)}
        >
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
        </Modal>
      )}
    </div>
  );
}
