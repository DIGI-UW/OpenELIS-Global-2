import React, { useState, useEffect } from "react";
import { Dropdown, Button, Modal, TextInput } from "@carbon/react";
import { Add } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * CreateForm — 5-level cascading dropdown UI for the LocationPicker.
 *
 * Replaces the 1837-line EnhancedCascadingMode.jsx (26 useState, 14
 * useEffect, custom downshift overrides, 4 boolean flag families × 4 levels).
 *
 * State held in this component:
 *   - options: { room, device, shelf, rack, box } — the API-fetched lists
 *   - inlineCreate: { level, name, isOpen } — small modal state for one
 *     inline-create dialog at a time
 *
 * Selection state lives in the picker's reducer (props.selection +
 * props.onLevelChange callback). The reducer's SET_LEVEL handles the
 * cascading-clear invariant (descendants drop when a parent changes).
 *
 * Per the locked-in design (plan §"Design decisions locked in" #5):
 *   - All 5 dropdowns ALWAYS rendered
 *   - Descendants disabled until parent is selected (UI feedback)
 *   - Box dropdown is enabled when Rack is selected; if the rack has no
 *     boxes, the dropdown is simply empty and the user moves on
 *
 * Endpoints (no backend changes per plan):
 *   GET  /rest/storage/rooms
 *   GET  /rest/storage/devices?roomId={id}
 *   GET  /rest/storage/shelves?deviceId={id}
 *   GET  /rest/storage/racks?shelfId={id}
 *   GET  /rest/storage/boxes?rackId={id}
 *   POST /rest/storage/{rooms|devices|shelves|racks|boxes}
 */

// Maps from the picker's level keys to the corresponding REST endpoint
// segments and the parent-id params the backend uses. Note the backend
// is inconsistent on naming:
//   - GET list params are "{parent}Id" (roomId, deviceId, …)
//   - POST body parent params are "parent{Parent}Id" (parentRoomId, …)
//   - Identifier field is "name" for Room/Device, "label" for Shelf/Rack/Box
// Both mappings are expressed in this table.
const LEVELS = [
  {
    key: "room",
    label: "Room",
    endpoint: "rooms",
    parentLevel: null,
    listParam: null,
    createParam: null,
    createField: "name",
  },
  {
    key: "device",
    label: "Device",
    endpoint: "devices",
    parentLevel: "room",
    listParam: "roomId",
    createParam: "parentRoomId",
    createField: "name",
  },
  {
    key: "shelf",
    label: "Shelf",
    endpoint: "shelves",
    parentLevel: "device",
    listParam: "deviceId",
    createParam: "parentDeviceId",
    createField: "label",
  },
  {
    key: "rack",
    label: "Rack",
    endpoint: "racks",
    parentLevel: "shelf",
    listParam: "shelfId",
    createParam: "parentShelfId",
    createField: "label",
  },
  {
    key: "box",
    label: "Box",
    endpoint: "boxes",
    parentLevel: "rack",
    listParam: "rackId",
    createParam: "parentRackId",
    createField: "label",
  },
];

export default function CreateForm({ selection, onLevelChange }) {
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
      {LEVELS.map(({ key, label, parentLevel }) => {
        const parentValue = parentLevel ? selection[parentLevel] : true;
        const enabled = parentValue != null;
        const items = options[key] || [];
        return (
          <div key={key} className="storage-location-picker-create-row">
            <Dropdown
              id={`location-picker-${key}`}
              titleText={label}
              label={`Select ${label.toLowerCase()}`}
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
              Add new
            </Button>
          </div>
        );
      })}

      {inlineCreate && (
        <Modal
          open
          modalHeading={`Create new ${LEVELS.find(
            (l) => l.key === inlineCreate.level,
          ).label.toLowerCase()}`}
          primaryButtonText="Create"
          secondaryButtonText="Cancel"
          onRequestSubmit={handleCreate}
          onRequestClose={() => setInlineCreate(null)}
          onSecondarySubmit={() => setInlineCreate(null)}
        >
          <TextInput
            id="location-picker-inline-create-name"
            labelText="Name"
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
