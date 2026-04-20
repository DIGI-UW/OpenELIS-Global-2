import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  Grid,
  Column,
  Dropdown,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

const normalizeResponseList = (response) => {
  if (Array.isArray(response)) {
    return response;
  }
  if (Array.isArray(response?.items)) {
    return response.items;
  }
  return [];
};

const resolveEntityId = (item) => {
  if (!item || typeof item !== "object") {
    return null;
  }

  const candidate =
    item.id ??
    item.roomId ??
    item.deviceId ??
    item.shelfId ??
    item.rackId ??
    item.boxId;

  if (candidate === null || candidate === undefined || candidate === "") {
    return null;
  }

  return candidate;
};

const resolveEntityLabel = (item, preferredKeys = []) => {
  const keys = [
    ...preferredKeys,
    "label",
    "name",
    "roomName",
    "deviceName",
    "shelfLabel",
    "rackLabel",
    "code",
  ];

  for (const key of keys) {
    const value = item?.[key];
    if (value !== null && value !== undefined && String(value).trim() !== "") {
      return String(value).trim();
    }
  }

  return "";
};

const toOption = (item, preferredLabelKeys = []) => {
  const id = resolveEntityId(item);
  const label = resolveEntityLabel(item, preferredLabelKeys);

  return {
    ...item,
    id,
    label,
  };
};

const resolveErrorDetail = (error) => {
  if (!error) {
    return "";
  }

  if (typeof error === "string" && error.trim() !== "") {
    return error.trim();
  }

  if (error?.message && String(error.message).trim() !== "") {
    return String(error.message).trim();
  }

  return "";
};

/**
 * StorageHierarchySelector - Reusable component for hierarchical storage location selection.
 * Provides cascading dropdowns: Room → Device/Freezer → Shelf → Rack → Box
 *
 * Storage Hierarchy (all levels are optional - can assign to any level):
 * - Room: Physical room containing storage equipment (can assign here)
 * - Device/Freezer: Refrigerator, Freezer (-20°C, -80°C), LN2 Tank, Incubator (can assign here)
 * - Shelf: Physical shelf within the device (can assign here)
 * - Rack: Storage rack within the shelf (can assign here)
 * - Box: Storage box within the rack or directly on shelf (can assign here)
 * - Well: Individual position within the box (handled by BoxLayoutViewer)
 *
 * @param {Object} props
 * @param {function} props.onSelectionChange - Callback when selection changes, receives { room, device, shelf, rack, box }
 * @param {boolean} props.showPath - Whether to show the hierarchical path (default: true)
 * @param {number} props.entryId - Notebook entry ID for box layout loading (optional)
 * @param {function} props.onBoxLayoutLoaded - Callback when box layout is loaded (optional)
 */
function StorageHierarchySelector({
  onSelectionChange,
  showPath = true,
  entryId,
  onBoxLayoutLoaded,
  biorepositoryOnly = false,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const requestCountRef = useRef(0);

  // Hierarchical storage selection state
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [boxes, setBoxes] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [selectedBox, setSelectedBox] = useState(null);

  const [loadingHierarchy, setLoadingHierarchy] = useState(false);
  const [hierarchyNotice, setHierarchyNotice] = useState(null);

  const beginHierarchyLoad = useCallback(() => {
    requestCountRef.current += 1;
    setLoadingHierarchy(true);
  }, []);

  const endHierarchyLoad = useCallback(() => {
    requestCountRef.current = Math.max(0, requestCountRef.current - 1);
    if (componentMounted.current && requestCountRef.current === 0) {
      setLoadingHierarchy(false);
    }
  }, []);

  const logHierarchyError = useCallback((level, endpoint, error) => {
    const errorDetail = resolveErrorDetail(error);
    if (errorDetail) {
      console.warn(
        `[StorageHierarchySelector:${level}]`,
        endpoint,
        errorDetail,
      );
    }
  }, []);

  const clearHierarchyNotice = useCallback(() => {
    setHierarchyNotice(null);
  }, []);

  const setHierarchyErrorNotice = useCallback(
    (levelLabel, error) => {
      const errorDetail = resolveErrorDetail(error);
      const detailSuffix = errorDetail ? ` (${errorDetail})` : "";

      setHierarchyNotice({
        kind: "error",
        title: intl.formatMessage({
          id: "notebook.storage.hierarchy.error.title",
          defaultMessage: "Unable to load storage hierarchy",
        }),
        subtitle: intl.formatMessage(
          {
            id: "notebook.storage.hierarchy.error.subtitle",
            defaultMessage:
              "Could not load {levelLabel} from the server. Refresh and try again, then confirm storage setup exists.{detailSuffix}",
          },
          { levelLabel, detailSuffix },
        ),
      });
    },
    [intl],
  );

  const setHierarchyEmptyNotice = useCallback(
    (level, context = {}) => {
      if (level === "rooms") {
        setHierarchyNotice({
          kind: "warning",
          title: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.rooms.title",
            defaultMessage: "No active storage rooms found",
          }),
          subtitle: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.rooms.subtitle",
            defaultMessage:
              "Set up and activate at least one storage room and freezer/device in Storage Management before assigning samples.",
          }),
        });
        return;
      }

      if (level === "devices") {
        setHierarchyNotice({
          kind: "warning",
          title: intl.formatMessage(
            {
              id: "notebook.storage.hierarchy.empty.devices.title",
              defaultMessage: "No active devices in {roomLabel}",
            },
            {
              roomLabel:
                context.roomLabel ||
                intl.formatMessage({
                  id: "notebook.storage.hierarchy.unknownRoom",
                  defaultMessage: "the selected room",
                }),
            },
          ),
          subtitle: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.devices.subtitle",
            defaultMessage:
              "Add or activate a freezer/device for this room in Storage Management.",
          }),
        });
        return;
      }

      if (level === "shelves") {
        setHierarchyNotice({
          kind: "info",
          title: intl.formatMessage(
            {
              id: "notebook.storage.hierarchy.empty.shelves.title",
              defaultMessage: "No shelves found in {deviceLabel}",
            },
            {
              deviceLabel:
                context.deviceLabel ||
                intl.formatMessage({
                  id: "notebook.storage.hierarchy.unknownDevice",
                  defaultMessage: "the selected device",
                }),
            },
          ),
          subtitle: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.shelves.subtitle",
            defaultMessage:
              "You can assign at room/device level, or configure shelves for finer location tracking.",
          }),
        });
        return;
      }

      if (level === "shelfTargets") {
        setHierarchyNotice({
          kind: "info",
          title: intl.formatMessage(
            {
              id: "notebook.storage.hierarchy.empty.shelfTargets.title",
              defaultMessage: "No racks or boxes found under {shelfLabel}",
            },
            {
              shelfLabel:
                context.shelfLabel ||
                intl.formatMessage({
                  id: "notebook.storage.hierarchy.unknownShelf",
                  defaultMessage: "the selected shelf",
                }),
            },
          ),
          subtitle: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.shelfTargets.subtitle",
            defaultMessage:
              "Assign at shelf level or configure racks/boxes for position-level storage.",
          }),
        });
        return;
      }

      if (level === "rackBoxes") {
        setHierarchyNotice({
          kind: "info",
          title: intl.formatMessage(
            {
              id: "notebook.storage.hierarchy.empty.rackBoxes.title",
              defaultMessage: "No boxes found in {rackLabel}",
            },
            {
              rackLabel:
                context.rackLabel ||
                intl.formatMessage({
                  id: "notebook.storage.hierarchy.unknownRack",
                  defaultMessage: "the selected rack",
                }),
            },
          ),
          subtitle: intl.formatMessage({
            id: "notebook.storage.hierarchy.empty.rackBoxes.subtitle",
            defaultMessage:
              "Assign at rack/shelf level or create boxes to enable well-level placement.",
          }),
        });
      }
    },
    [intl],
  );

  // Load rooms on mount
  useEffect(() => {
    componentMounted.current = true;
    loadRooms();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [biorepositoryOnly]);

  // Notify parent of selection changes
  useEffect(() => {
    if (onSelectionChange) {
      onSelectionChange({
        room: selectedRoom,
        device: selectedDevice,
        shelf: selectedShelf,
        rack: selectedRack,
        box: selectedBox,
      });
    }
    // Note: onSelectionChange is intentionally not in dependencies to avoid infinite loops
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRoom, selectedDevice, selectedShelf, selectedRack, selectedBox]);

  const loadRooms = () => {
    clearHierarchyNotice();
    const endpoint = `/rest/storage/rooms?status=active${
      biorepositoryOnly ? "&biorepositoryOnly=true" : ""
    }`;
    beginHierarchyLoad();
    getFromOpenElisServer(endpoint, (response, error) => {
      endHierarchyLoad();
      if (!componentMounted.current) {
        return;
      }

      if (error) {
        setRooms([]);
        logHierarchyError("rooms", endpoint, error);
        setHierarchyErrorNotice(
          intl.formatMessage({
            id: "notebook.storage.hierarchy.level.rooms",
            defaultMessage: "rooms",
          }),
          error,
        );
        return;
      }

      const mappedRooms = normalizeResponseList(response)
        .map((room) => toOption(room, ["name", "label"]))
        .filter((room) => room.id);

      setRooms(mappedRooms);
      if (mappedRooms.length === 0) {
        setHierarchyEmptyNotice("rooms");
      }
    });
  };

  // Load devices when room changes
  const handleRoomChange = ({ selectedItem }) => {
    const selectedRoomOption = selectedItem ? toOption(selectedItem) : null;
    clearHierarchyNotice();
    setSelectedRoom(selectedRoomOption);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);

    if (selectedRoomOption?.id) {
      const endpoint = `/rest/storage/devices?roomId=${encodeURIComponent(selectedRoomOption.id)}&status=active${
        biorepositoryOnly ? "&biorepositoryOnly=true" : ""
      }`;
      beginHierarchyLoad();
      getFromOpenElisServer(endpoint, (response, error) => {
        endHierarchyLoad();
        if (!componentMounted.current) {
          return;
        }

        if (error) {
          setDevices([]);
          logHierarchyError("devices", endpoint, error);
          setHierarchyErrorNotice(
            intl.formatMessage({
              id: "notebook.storage.hierarchy.level.devices",
              defaultMessage: "devices",
            }),
            error,
          );
          return;
        }

        const mappedDevices = normalizeResponseList(response)
          .map((device) => toOption(device, ["name", "label", "deviceName"]))
          .filter((device) => device.id);

        setDevices(mappedDevices);
        if (mappedDevices.length === 0) {
          setHierarchyEmptyNotice("devices", {
            roomLabel: selectedRoomOption.label,
          });
        }
      });
    }
  };

  // Load shelves when device changes
  const handleDeviceChange = ({ selectedItem }) => {
    const selectedDeviceOption = selectedItem ? toOption(selectedItem) : null;
    clearHierarchyNotice();
    setSelectedDevice(selectedDeviceOption);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setShelves([]);
    setRacks([]);
    setBoxes([]);

    if (selectedDeviceOption?.id) {
      const endpoint = `/rest/storage/shelves?deviceId=${encodeURIComponent(selectedDeviceOption.id)}&status=active${
        biorepositoryOnly ? "&biorepositoryOnly=true" : ""
      }`;
      beginHierarchyLoad();
      getFromOpenElisServer(endpoint, (response, error) => {
        endHierarchyLoad();
        if (!componentMounted.current) {
          return;
        }

        if (error) {
          setShelves([]);
          logHierarchyError("shelves", endpoint, error);
          setHierarchyErrorNotice(
            intl.formatMessage({
              id: "notebook.storage.hierarchy.level.shelves",
              defaultMessage: "shelves",
            }),
            error,
          );
          return;
        }

        const mappedShelves = normalizeResponseList(response)
          .map((shelf) => toOption(shelf, ["label", "name", "shelfLabel"]))
          .filter((shelf) => shelf.id);

        setShelves(mappedShelves);
        if (mappedShelves.length === 0) {
          setHierarchyEmptyNotice("shelves", {
            deviceLabel: selectedDeviceOption.label,
          });
        }
      });
    }
  };

  // Load racks and boxes when shelf changes
  const handleShelfChange = ({ selectedItem }) => {
    const selectedShelfOption = selectedItem ? toOption(selectedItem) : null;
    clearHierarchyNotice();
    setSelectedShelf(selectedShelfOption);
    setSelectedRack(null);
    setSelectedBox(null);
    setRacks([]);
    setBoxes([]);

    if (selectedShelfOption?.id) {
      const racksEndpoint = `/rest/storage/racks?shelfId=${encodeURIComponent(selectedShelfOption.id)}&status=active${
        biorepositoryOnly ? "&biorepositoryOnly=true" : ""
      }`;
      const boxesEndpoint = `/rest/storage/boxes?shelfId=${encodeURIComponent(selectedShelfOption.id)}&active=true${
        biorepositoryOnly ? "&biorepositoryOnly=true" : ""
      }`;
      let rackRequestDone = false;
      let boxRequestDone = false;
      let rackRequestError = null;
      let boxRequestError = null;
      let rackCount = 0;
      let boxCount = 0;

      const finalizeShelfLoad = () => {
        if (!componentMounted.current || !rackRequestDone || !boxRequestDone) {
          return;
        }

        if (rackRequestError || boxRequestError) {
          setHierarchyErrorNotice(
            intl.formatMessage({
              id: "notebook.storage.hierarchy.level.shelfChildren",
              defaultMessage: "racks and boxes",
            }),
            rackRequestError || boxRequestError,
          );
          return;
        }

        if (rackCount === 0 && boxCount === 0) {
          setHierarchyEmptyNotice("shelfTargets", {
            shelfLabel: selectedShelfOption.label,
          });
        }
      };

      beginHierarchyLoad();
      getFromOpenElisServer(racksEndpoint, (response, error) => {
        endHierarchyLoad();
        rackRequestDone = true;

        if (!componentMounted.current) {
          return;
        }

        if (error) {
          rackRequestError = error;
          setRacks([]);
          logHierarchyError("racks", racksEndpoint, error);
          finalizeShelfLoad();
          return;
        }

        const mappedRacks = normalizeResponseList(response)
          .map((rack) => toOption(rack, ["label", "name", "rackLabel"]))
          .filter((rack) => rack.id);
        rackCount = mappedRacks.length;
        setRacks(mappedRacks);
        finalizeShelfLoad();
      });

      beginHierarchyLoad();
      getFromOpenElisServer(boxesEndpoint, (response, error) => {
        endHierarchyLoad();
        boxRequestDone = true;

        if (!componentMounted.current) {
          return;
        }

        if (error) {
          boxRequestError = error;
          setBoxes([]);
          logHierarchyError("boxesByShelf", boxesEndpoint, error);
          finalizeShelfLoad();
          return;
        }

        const mappedBoxes = normalizeResponseList(response)
          .map((box) => toOption(box, ["label", "name"]))
          .filter((box) => box.id)
          .map((box) => ({
            ...box,
            rows: box.rows || 8,
            columns: box.columns || 12,
          }));

        boxCount = mappedBoxes.length;
        setBoxes(mappedBoxes);
        finalizeShelfLoad();
      });
    }
  };

  // Load boxes when rack changes
  const handleRackChange = ({ selectedItem }) => {
    const selectedRackOption = selectedItem ? toOption(selectedItem) : null;
    clearHierarchyNotice();
    setSelectedRack(selectedRackOption);
    setSelectedBox(null);
    setBoxes([]);

    if (selectedRackOption?.id) {
      const endpoint = `/rest/storage/boxes?rackId=${encodeURIComponent(selectedRackOption.id)}&active=true${
        biorepositoryOnly ? "&biorepositoryOnly=true" : ""
      }`;
      beginHierarchyLoad();
      getFromOpenElisServer(endpoint, (response, error) => {
        endHierarchyLoad();
        if (!componentMounted.current) {
          return;
        }

        if (error) {
          setBoxes([]);
          logHierarchyError("boxesByRack", endpoint, error);
          setHierarchyErrorNotice(
            intl.formatMessage({
              id: "notebook.storage.hierarchy.level.boxes",
              defaultMessage: "boxes",
            }),
            error,
          );
          return;
        }

        const mappedBoxes = normalizeResponseList(response)
          .map((box) => toOption(box, ["label", "name"]))
          .filter((box) => box.id)
          .map((box) => ({
            ...box,
            rows: box.rows || 8,
            columns: box.columns || 12,
          }));

        setBoxes(mappedBoxes);
        if (mappedBoxes.length === 0) {
          setHierarchyEmptyNotice("rackBoxes", {
            rackLabel: selectedRackOption.label,
          });
        }
      });
    }
  };

  // Load box layout when box changes
  const handleBoxChange = ({ selectedItem }) => {
    const selectedBoxOption = selectedItem ? toOption(selectedItem) : null;
    clearHierarchyNotice();
    setSelectedBox(selectedBoxOption);

    if (selectedBoxOption?.id && entryId && onBoxLayoutLoaded) {
      const endpoint = `/rest/notebook/${entryId}/box/${selectedBoxOption.id}/layout`;
      beginHierarchyLoad();
      getFromOpenElisServer(endpoint, (response, error) => {
        endHierarchyLoad();
        if (!componentMounted.current) {
          return;
        }

        if (error) {
          logHierarchyError("boxLayout", endpoint, error);
          setHierarchyErrorNotice(
            intl.formatMessage({
              id: "notebook.storage.hierarchy.level.boxLayout",
              defaultMessage: "box layout",
            }),
            error,
          );
          return;
        }

        if (response) {
          onBoxLayoutLoaded(response.wells || {});
        }
      });
    }
  };

  // Build hierarchical path
  const getHierarchicalPath = () => {
    const parts = [];
    if (selectedRoom) parts.push(selectedRoom.label);
    if (selectedDevice) parts.push(selectedDevice.label);
    if (selectedShelf) parts.push(selectedShelf.label);
    if (selectedRack) parts.push(selectedRack.label);
    if (selectedBox) parts.push(selectedBox.label);
    return parts.join(" > ");
  };

  return (
    <div className="storage-hierarchy-selector">
      <Grid fullWidth narrow>
        <Column lg={8} md={4} sm={4}>
          <Dropdown
            id="room-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.room",
              defaultMessage: "Room",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRoom",
              defaultMessage: "Select room...",
            })}
            items={rooms}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              selectedRoom
                ? rooms.find(
                    (room) => String(room.id) === String(selectedRoom.id),
                  ) || selectedRoom
                : null
            }
            onChange={handleRoomChange}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Dropdown
            id="device-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.device",
              defaultMessage: "Freezer/Device",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectDevice",
              defaultMessage: "Select freezer/device...",
            })}
            items={devices}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              selectedDevice
                ? devices.find(
                    (device) => String(device.id) === String(selectedDevice.id),
                  ) || selectedDevice
                : null
            }
            onChange={handleDeviceChange}
            disabled={!selectedRoom}
            helperText={intl.formatMessage({
              id: "notebook.storage.device.optional",
              defaultMessage: "Optional - can assign to room level",
            })}
          />
        </Column>
      </Grid>

      <Grid fullWidth narrow style={{ marginTop: "0.5rem" }}>
        <Column lg={5} md={3} sm={4}>
          <Dropdown
            id="shelf-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.shelf",
              defaultMessage: "Shelf",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectShelf",
              defaultMessage: "Select shelf...",
            })}
            items={shelves}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              selectedShelf
                ? shelves.find(
                    (shelf) => String(shelf.id) === String(selectedShelf.id),
                  ) || selectedShelf
                : null
            }
            onChange={handleShelfChange}
            disabled={!selectedDevice}
            helperText={intl.formatMessage({
              id: "notebook.storage.shelf.optional",
              defaultMessage: "Optional - can assign to device level",
            })}
          />
        </Column>
        <Column lg={5} md={3} sm={4}>
          <Dropdown
            id="rack-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.rack",
              defaultMessage: "Rack",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRack",
              defaultMessage: "Select rack...",
            })}
            items={racks}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              selectedRack
                ? racks.find(
                    (rack) => String(rack.id) === String(selectedRack.id),
                  ) || selectedRack
                : null
            }
            onChange={handleRackChange}
            disabled={!selectedShelf}
            helperText={intl.formatMessage({
              id: "notebook.storage.rack.optional",
              defaultMessage: "Optional - can assign to shelf level",
            })}
          />
        </Column>
        <Column lg={6} md={2} sm={4}>
          <Dropdown
            id="box-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.box",
              defaultMessage: "Box",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectBox",
              defaultMessage: "Select box...",
            })}
            items={boxes}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={
              selectedBox
                ? boxes.find(
                    (box) => String(box.id) === String(selectedBox.id),
                  ) || selectedBox
                : null
            }
            onChange={handleBoxChange}
            disabled={!selectedShelf && !selectedRack}
            helperText={intl.formatMessage({
              id: "notebook.storage.box.optional",
              defaultMessage: "Optional - can assign to rack or shelf level",
            })}
          />
        </Column>
      </Grid>

      {/* Hierarchical Path Display */}
      {showPath && getHierarchicalPath() && (
        <div
          style={{
            marginTop: "0.5rem",
            fontSize: "0.875rem",
            color: "#525252",
            backgroundColor: "#f4f4f4",
            padding: "0.5rem",
            borderRadius: "4px",
          }}
        >
          <strong>
            <FormattedMessage
              id="notebook.storage.path"
              defaultMessage="Path:"
            />
          </strong>{" "}
          {getHierarchicalPath()}
        </div>
      )}

      {hierarchyNotice && (
        <div style={{ marginTop: "0.5rem" }}>
          <InlineNotification
            lowContrast
            hideCloseButton
            kind={hierarchyNotice.kind}
            title={hierarchyNotice.title}
            subtitle={hierarchyNotice.subtitle}
          />
        </div>
      )}

      {loadingHierarchy && (
        <div style={{ marginTop: "0.5rem" }}>
          <Loading withOverlay={false} small description="Loading..." />
        </div>
      )}
    </div>
  );
}

export default StorageHierarchySelector;
