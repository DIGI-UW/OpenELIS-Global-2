import React, { useState, useEffect, useCallback } from "react";
import { ComboBox, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import "./EnhancedCascadingMode.css";

/**
 * Enhanced cascading mode with autocomplete text boxes that allow inline creation
 * Each level (Room, Device, Shelf, Rack) uses ComboBox with autocomplete suggestions
 * Users can select existing items or type new names to create them
 * Position is a simple text input (optional)
 *
 * Props:
 * - onLocationChange: function - Callback when location is selected/created
 * - selectedLocation: object - Pre-selected location (optional)
 */
const EnhancedCascadingMode = ({ onLocationChange, selectedLocation }) => {
  const intl = useIntl();

  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);

  const [roomInput, setRoomInput] = useState("");
  const [deviceInput, setDeviceInput] = useState("");
  const [shelfInput, setShelfInput] = useState("");
  const [rackInput, setRackInput] = useState("");
  const [positionInput, setPositionInput] = useState("");

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);

  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [isCreatingDevice, setIsCreatingDevice] = useState(false);
  const [isCreatingShelf, setIsCreatingShelf] = useState(false);
  const [isCreatingRack, setIsCreatingRack] = useState(false);

  // Load rooms on mount
  useEffect(() => {
    getFromOpenElisServer(
      "/rest/storage/rooms",
      (response) => {
        // Filter out inactive rooms (only show active ones in creation form)
        const activeRooms = (response || []).filter((r) => r.active !== false);
        setRooms(activeRooms);
      },
      () => {
        setRooms([]);
      },
    );
  }, []); // Only run on mount

  // When rooms load and we have a selectedLocation with type='room',
  // update selectedRoom to use the proper room object from the list
  useEffect(() => {
    if (
      rooms.length > 0 &&
      selectedLocation &&
      selectedLocation.type === "room" &&
      selectedLocation.id
    ) {
      const room = rooms.find((r) => r.id === selectedLocation.id);
      if (room && (!selectedRoom || selectedRoom.id !== room.id)) {
        setSelectedRoom(room);
        setRoomInput(room.name || "");
      }
    }
  }, [rooms, selectedLocation, selectedRoom]);

  // Pre-populate from selectedLocation
  // Handle both formats:
  // 1. EnhancedCascadingMode format: { room: {...}, device: {...}, ... }
  // 2. LocationFilterDropdown format: { id, type: 'room', name: '...', ... }
  useEffect(() => {
    if (selectedLocation) {
      // Format 1: EnhancedCascadingMode format (has room/device/shelf/rack properties)
      if (selectedLocation.room) {
        setSelectedRoom(selectedLocation.room);
        setRoomInput(selectedLocation.room.name || "");
      }
      if (selectedLocation.device) {
        setSelectedDevice(selectedLocation.device);
        setDeviceInput(selectedLocation.device.name || "");
      }
      if (selectedLocation.shelf) {
        setSelectedShelf(selectedLocation.shelf);
        setShelfInput(selectedLocation.shelf.label || "");
      }
      if (selectedLocation.rack) {
        setSelectedRack(selectedLocation.rack);
        setRackInput(selectedLocation.rack.label || "");
      }
      if (selectedLocation.position) {
        setPositionInput(selectedLocation.position.coordinate || "");
      }

      // Format 2: LocationFilterDropdown format (has type and id)
      if (
        selectedLocation.type &&
        selectedLocation.id &&
        !selectedLocation.room
      ) {
        // Find the location in the appropriate list based on type
        if (selectedLocation.type === "room") {
          // Try to find in rooms list first
          const room = rooms.find((r) => r.id === selectedLocation.id);
          if (room) {
            setSelectedRoom(room);
            setRoomInput(room.name || "");
          } else if (selectedLocation.id && selectedLocation.name) {
            // Room not in list yet (might still be loading), use the selectedLocation data directly
            // This will work because the room data from LocationFilterDropdown has all needed fields
            // The room will be properly set once rooms load, but this allows the device field to be enabled
            setSelectedRoom({
              id: selectedLocation.id,
              name: selectedLocation.name,
              code:
                selectedLocation.code ||
                selectedLocation.name.substring(0, 50).toUpperCase(),
              active: selectedLocation.active !== false,
              ...selectedLocation,
            });
            setRoomInput(selectedLocation.name || "");
          }
        } else if (selectedLocation.type === "device") {
          // Device selected - need to find its parent room first
          // Check if we have the device in the current list
          const device = devices.find((d) => d.id === selectedLocation.id);
          if (device && device.parentRoomId) {
            const room = rooms.find((r) => r.id === device.parentRoomId);
            if (room) {
              setSelectedRoom(room);
              setRoomInput(room.name || "");
            }
            setSelectedDevice(device);
            setDeviceInput(device.name || "");
          } else if (selectedLocation.parentRoomId) {
            // Device has parentRoomId in the selectedLocation
            const room = rooms.find(
              (r) => r.id === selectedLocation.parentRoomId,
            );
            if (room) {
              setSelectedRoom(room);
              setRoomInput(room.name || "");
              // Load devices for this room
              getFromOpenElisServer(
                `/rest/storage/devices?roomId=${room.id}`,
                (response) => {
                  const activeDevices = (response || []).filter(
                    (d) => d.active !== false,
                  );
                  setDevices(activeDevices);
                  // Now set the device
                  const foundDevice = activeDevices.find(
                    (d) => d.id === selectedLocation.id,
                  );
                  if (foundDevice) {
                    setSelectedDevice(foundDevice);
                    setDeviceInput(foundDevice.name || "");
                  }
                },
                () => {},
              );
            }
          }
        }
        // Similar handling for shelf and rack could be added if needed
      }
    }
  }, [selectedLocation, rooms, devices]);

  // Load devices when room is selected or created
  useEffect(() => {
    if (selectedRoom && selectedRoom.id) {
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedRoom.id}`,
        (response) => {
          // Filter out inactive devices (only show active ones in creation form)
          const activeDevices = (response || []).filter(
            (d) => d.active !== false,
          );
          setDevices(activeDevices);
        },
        () => {
          setDevices([]);
        },
      );
      // Reset child selections
      setSelectedDevice(null);
      setSelectedShelf(null);
      setSelectedRack(null);
      setDeviceInput("");
      setShelfInput("");
      setRackInput("");
      setShelves([]);
      setRacks([]);
    }
  }, [selectedRoom]);

  // Load shelves when device is selected or created
  useEffect(() => {
    if (selectedDevice && selectedDevice.id) {
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedDevice.id}`,
        (response) => {
          // Filter out inactive shelves (only show active ones in creation form)
          const activeShelves = (response || []).filter(
            (s) => s.active !== false,
          );
          setShelves(activeShelves);
        },
        () => {
          setShelves([]);
        },
      );
      setSelectedShelf(null);
      setSelectedRack(null);
      setShelfInput("");
      setRackInput("");
      setRacks([]);
    }
  }, [selectedDevice]);

  // Load racks when shelf is selected or created
  useEffect(() => {
    if (selectedShelf && selectedShelf.id) {
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedShelf.id}`,
        (response) => {
          // Filter out inactive racks (only show active ones in creation form)
          const activeRacks = (response || []).filter(
            (r) => r.active !== false,
          );
          setRacks(activeRacks);
        },
        () => {
          setRacks([]);
        },
      );
      setSelectedRack(null);
      setRackInput("");
    }
  }, [selectedShelf]);

  // Create room if input doesn't match existing
  const handleRoomChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setRoomInput(inputValue || "");

      if (selectedItem) {
        // User selected from dropdown
        setSelectedRoom(selectedItem);
        setIsCreatingRoom(false);
      } else if (trimmedValue) {
        // User is typing - check if room exists
        const existing = rooms.find(
          (r) => r.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedRoom(existing);
          setIsCreatingRoom(false);
        } else {
          // New room - create immediately
          setIsCreatingRoom(true);
          const newRoom = {
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
          };
          setSelectedRoom(newRoom);
        }
      } else {
        // Empty input
        setSelectedRoom(null);
        setIsCreatingRoom(false);
      }
    },
    [rooms],
  );

  // Create device if input doesn't match existing
  const handleDeviceChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setDeviceInput(inputValue || "");

      if (selectedItem) {
        setSelectedDevice(selectedItem);
        setIsCreatingDevice(false);
      } else if (trimmedValue && selectedRoom && selectedRoom.id) {
        // Only allow if room is selected and has id (created)
        const existing = devices.find(
          (d) => d.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedDevice(existing);
          setIsCreatingDevice(false);
        } else {
          setIsCreatingDevice(true);
          setSelectedDevice({
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
            type: "other",
          });
        }
      } else {
        setSelectedDevice(null);
        setIsCreatingDevice(false);
      }
    },
    [devices, selectedRoom],
  );

  // Create shelf if input doesn't match existing
  const handleShelfChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setShelfInput(inputValue || "");

      if (selectedItem) {
        setSelectedShelf(selectedItem);
        setIsCreatingShelf(false);
      } else if (trimmedValue && selectedDevice && selectedDevice.id) {
        const existing = shelves.find(
          (s) => s.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedShelf(existing);
          setIsCreatingShelf(false);
        } else {
          setIsCreatingShelf(true);
          setSelectedShelf({ label: trimmedValue });
        }
      } else {
        setSelectedShelf(null);
        setIsCreatingShelf(false);
      }
    },
    [shelves, selectedDevice],
  );

  // Create rack if input doesn't match existing
  const handleRackChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setRackInput(inputValue || "");

      if (selectedItem) {
        setSelectedRack(selectedItem);
        setIsCreatingRack(false);
      } else if (trimmedValue && selectedShelf && selectedShelf.id) {
        const existing = racks.find(
          (r) => r.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedRack(existing);
          setIsCreatingRack(false);
        } else {
          setIsCreatingRack(true);
          setSelectedRack({ label: trimmedValue, rows: 0, columns: 0 });
        }
      } else {
        setSelectedRack(null);
        setIsCreatingRack(false);
      }
    },
    [racks, selectedShelf],
  );

  // Create room via API
  const createRoom = useCallback(async () => {
    if (!selectedRoom || !selectedRoom.name || selectedRoom.id) return;

    const formData = {
      name: selectedRoom.name,
      code:
        selectedRoom.code || selectedRoom.name.substring(0, 50).toUpperCase(),
      description: "",
      active: true,
    };

    postToOpenElisServer(
      "/rest/storage/rooms",
      formData,
      (created) => {
        setSelectedRoom(created);
        setRooms((prev) => [...prev, created]);
        setIsCreatingRoom(false);
      },
      (error) => {
        console.error("Failed to create room:", error);
      },
    );
  }, [selectedRoom]);

  // Create device via API
  const createDevice = useCallback(async () => {
    if (
      !selectedDevice ||
      !selectedDevice.name ||
      selectedDevice.id ||
      !selectedRoom?.id
    )
      return;

    const formData = {
      name: selectedDevice.name,
      code:
        selectedDevice.code ||
        selectedDevice.name.substring(0, 50).toUpperCase(),
      type: selectedDevice.type || "other",
      active: true,
      parentRoomId: String(selectedRoom.id),
    };

    postToOpenElisServer(
      "/rest/storage/devices",
      formData,
      (created) => {
        setSelectedDevice(created);
        setDevices((prev) => [...prev, created]);
        setIsCreatingDevice(false);
      },
      (error) => {
        console.error("Failed to create device:", error);
      },
    );
  }, [selectedDevice, selectedRoom]);

  // Create shelf via API
  const createShelf = useCallback(async () => {
    if (
      !selectedShelf ||
      !selectedShelf.label ||
      selectedShelf.id ||
      !selectedDevice?.id
    )
      return;

    const formData = {
      label: selectedShelf.label,
      active: true,
      parentDeviceId: String(selectedDevice.id),
    };

    postToOpenElisServer(
      "/rest/storage/shelves",
      formData,
      (created) => {
        setSelectedShelf(created);
        setShelves((prev) => [...prev, created]);
        setIsCreatingShelf(false);
      },
      (error) => {
        console.error("Failed to create shelf:", error);
      },
    );
  }, [selectedShelf, selectedDevice]);

  // Create rack via API
  const createRack = useCallback(async () => {
    if (
      !selectedRack ||
      !selectedRack.label ||
      selectedRack.id ||
      !selectedShelf?.id
    )
      return;

    const formData = {
      label: selectedRack.label,
      rows: selectedRack.rows || 0,
      columns: selectedRack.columns || 0,
      active: true,
      parentShelfId: String(selectedShelf.id),
    };

    postToOpenElisServer(
      "/rest/storage/racks",
      formData,
      (created) => {
        setSelectedRack(created);
        setRacks((prev) => [...prev, created]);
        setIsCreatingRack(false);
      },
      (error) => {
        console.error("Failed to create rack:", error);
      },
    );
  }, [selectedRack, selectedShelf]);

  // Auto-create items immediately when they're typed (not selected from dropdown)
  useEffect(() => {
    // Create room immediately when typed (not selected)
    if (isCreatingRoom && selectedRoom && !selectedRoom.id) {
      createRoom();
    }
  }, [isCreatingRoom, selectedRoom, createRoom]);

  useEffect(() => {
    // Create device when typed and room exists
    if (
      isCreatingDevice &&
      selectedDevice &&
      !selectedDevice.id &&
      selectedRoom?.id
    ) {
      createDevice();
    }
  }, [isCreatingDevice, selectedDevice, selectedRoom, createDevice]);

  useEffect(() => {
    // Create shelf when typed and device exists
    if (
      isCreatingShelf &&
      selectedShelf &&
      !selectedShelf.id &&
      selectedDevice?.id
    ) {
      createShelf();
    }
  }, [isCreatingShelf, selectedShelf, selectedDevice, createShelf]);

  useEffect(() => {
    // Create rack when typed and shelf exists
    if (
      isCreatingRack &&
      selectedRack &&
      !selectedRack.id &&
      selectedShelf?.id
    ) {
      createRack();
    }
  }, [isCreatingRack, selectedRack, selectedShelf, createRack]);

  // Notify parent when location is complete
  // Only notify when we have at least a rack (or shelf if no rack selected)
  useEffect(() => {
    if (selectedRack && selectedRack.id && onLocationChange) {
      // Full hierarchy with rack
      onLocationChange({
        room: selectedRoom,
        device: selectedDevice,
        shelf: selectedShelf,
        rack: selectedRack,
        position: positionInput ? { coordinate: positionInput } : null,
      });
    } else if (
      selectedShelf &&
      selectedShelf.id &&
      (!selectedRack || !selectedRack.id) &&
      onLocationChange
    ) {
      // Allow selection at shelf level (no rack yet)
      onLocationChange({
        room: selectedRoom,
        device: selectedDevice,
        shelf: selectedShelf,
        rack: null,
        position: null,
      });
    } else if (
      selectedRoom &&
      selectedRoom.id &&
      !selectedShelf &&
      onLocationChange
    ) {
      // Allow selection at room level (minimal)
      onLocationChange({
        room: selectedRoom,
        device: null,
        shelf: null,
        rack: null,
        position: null,
      });
    }
  }, [
    selectedRoom,
    selectedDevice,
    selectedShelf,
    selectedRack,
    positionInput,
    onLocationChange,
  ]);

  return (
    <div className="enhanced-cascading-container">
      {/* Room - ComboBox with autocomplete */}
      <ComboBox
        id="room-combobox"
        data-testid="room-combobox"
        titleText={intl.formatMessage({
          id: "storage.room.label",
          defaultMessage: "Room",
        })}
        label={intl.formatMessage({
          id: "storage.room.label",
          defaultMessage: "Room",
        })}
        items={rooms || []}
        itemToString={(item) => (item ? item.name : "")}
        onInputChange={({ inputValue }) => handleRoomChange(inputValue, null)}
        onChange={({ selectedItem }) =>
          handleRoomChange(selectedItem?.name || "", selectedItem)
        }
        selectedItem={selectedRoom}
        inputValue={roomInput}
        placeholder={intl.formatMessage({
          id: "storage.room.placeholder",
          defaultMessage: "Select or type to create room...",
        })}
      />

      {/* Device - ComboBox with autocomplete */}
      <ComboBox
        id="device-combobox"
        data-testid="device-combobox"
        titleText={intl.formatMessage({
          id: "storage.device.label",
          defaultMessage: "Device",
        })}
        label={intl.formatMessage({
          id: "storage.device.label",
          defaultMessage: "Device",
        })}
        items={devices || []}
        itemToString={(item) => (item ? item.name : "")}
        onInputChange={({ inputValue }) => handleDeviceChange(inputValue, null)}
        onChange={({ selectedItem }) =>
          handleDeviceChange(selectedItem?.name || "", selectedItem)
        }
        selectedItem={selectedDevice}
        inputValue={deviceInput}
        disabled={!selectedRoom || !selectedRoom.id}
        placeholder={intl.formatMessage({
          id: "storage.device.placeholder",
          defaultMessage: "Select or type to create device...",
        })}
      />

      {/* Shelf - ComboBox with autocomplete */}
      <ComboBox
        id="shelf-combobox"
        data-testid="shelf-combobox"
        titleText={intl.formatMessage({
          id: "storage.shelf.label",
          defaultMessage: "Shelf",
        })}
        label={intl.formatMessage({
          id: "storage.shelf.label",
          defaultMessage: "Shelf",
        })}
        items={shelves || []}
        itemToString={(item) => (item ? item.label : "")}
        onInputChange={({ inputValue }) => handleShelfChange(inputValue, null)}
        onChange={({ selectedItem }) =>
          handleShelfChange(selectedItem?.label || "", selectedItem)
        }
        selectedItem={selectedShelf}
        inputValue={shelfInput}
        disabled={!selectedDevice || !selectedDevice.id}
        placeholder={intl.formatMessage({
          id: "storage.shelf.placeholder",
          defaultMessage: "Select or type to create shelf...",
        })}
      />

      {/* Rack - ComboBox with autocomplete */}
      <ComboBox
        id="rack-combobox"
        data-testid="rack-combobox"
        titleText={intl.formatMessage({
          id: "storage.rack.label",
          defaultMessage: "Rack",
        })}
        label={intl.formatMessage({
          id: "storage.rack.label",
          defaultMessage: "Rack",
        })}
        items={racks || []}
        itemToString={(item) => (item ? item.label : "")}
        onInputChange={({ inputValue }) => handleRackChange(inputValue, null)}
        onChange={({ selectedItem }) =>
          handleRackChange(selectedItem?.label || "", selectedItem)
        }
        selectedItem={selectedRack}
        inputValue={rackInput}
        disabled={!selectedShelf || !selectedShelf.id}
        placeholder={intl.formatMessage({
          id: "storage.rack.placeholder",
          defaultMessage: "Select or type to create rack...",
        })}
      />

      {/* Position - Simple text input (optional) */}
      <TextInput
        id="position-input"
        data-testid="position-input"
        labelText={
          <>
            <FormattedMessage
              id="storage.position.label"
              defaultMessage="Position"
            />{" "}
            <span className="optional-text">
              (
              <FormattedMessage id="label.optional" defaultMessage="optional" />
              )
            </span>
          </>
        }
        value={positionInput}
        onChange={(e) => setPositionInput(e.target.value)}
        disabled={!selectedRack || !selectedRack.id}
        placeholder={intl.formatMessage({
          id: "storage.position.placeholder",
          defaultMessage: "e.g., A5, 1-1, RED-12",
        })}
      />
    </div>
  );
};

export default EnhancedCascadingMode;
