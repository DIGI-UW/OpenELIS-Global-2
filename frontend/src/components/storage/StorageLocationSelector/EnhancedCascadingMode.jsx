import React, { useState, useEffect, useCallback, useRef } from "react";
import { ComboBox, TextInput, Button, ComposedModal, ModalHeader, ModalBody, ModalFooter } from "@carbon/react";
import { Add } from "@carbon/icons-react";
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

  // Track pending room creation (must be declared before useEffect that uses it)
  const [pendingRoomCreation, setPendingRoomCreation] = useState(null);
  const roomCreationTimeoutRef = useRef(null);
  const [showAddRoomLink, setShowAddRoomLink] = useState(false);
  const [showAddDeviceLink, setShowAddDeviceLink] = useState(false);
  const [showAddShelfLink, setShowAddShelfLink] = useState(false);
  const [showAddRackLink, setShowAddRackLink] = useState(false);
  
  // Confirmation dialogs for Enter key
  const [showConfirmRoom, setShowConfirmRoom] = useState(false);
  const [showConfirmDevice, setShowConfirmDevice] = useState(false);
  const [showConfirmShelf, setShowConfirmShelf] = useState(false);
  const [showConfirmRack, setShowConfirmRack] = useState(false);

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
    if (!selectedLocation) {
      // Reset all selections when location is cleared
      setSelectedRoom(null);
      setSelectedDevice(null);
      setSelectedShelf(null);
      setSelectedRack(null);
      setRoomInput("");
      setDeviceInput("");
      setShelfInput("");
      setRackInput("");
      setPositionInput("");
      return;
    }
    
    if (selectedLocation) {
      // Format 1: EnhancedCascadingMode format (has room/device/shelf/rack properties)
      if (selectedLocation.room && typeof selectedLocation.room === 'object') {
        // If room only has id, try to find full room object in rooms list
        if (selectedLocation.room.id && !selectedLocation.room.name && rooms.length > 0) {
          const fullRoom = rooms.find((r) => r.id === selectedLocation.room.id);
          if (fullRoom) {
            setSelectedRoom(fullRoom);
            setRoomInput(fullRoom.name || "");
          } else {
            // Room not loaded yet, set what we have (will be updated when rooms load)
            setSelectedRoom(selectedLocation.room);
            setRoomInput(selectedLocation.room.name || "");
          }
        } else {
          setSelectedRoom(selectedLocation.room);
          setRoomInput(selectedLocation.room.name || "");
        }
      }
      if (selectedLocation.device && typeof selectedLocation.device === 'object') {
        setSelectedDevice(selectedLocation.device);
        setDeviceInput(selectedLocation.device.name || "");
      }
      if (selectedLocation.shelf && typeof selectedLocation.shelf === 'object') {
        setSelectedShelf(selectedLocation.shelf);
        setShelfInput(selectedLocation.shelf.label || selectedLocation.shelf.name || "");
      }
      if (selectedLocation.rack && typeof selectedLocation.rack === 'object') {
        setSelectedRack(selectedLocation.rack);
        setRackInput(selectedLocation.rack.label || selectedLocation.rack.name || "");
      }
      if (selectedLocation.position && typeof selectedLocation.position === 'object') {
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

  // Load full room data if room only has id (from LocationSearchAndCreate conversion)
  useEffect(() => {
    if (
      selectedRoom &&
      selectedRoom.id &&
      !selectedRoom.name &&
      rooms.length > 0
    ) {
      const fullRoom = rooms.find((r) => r.id === selectedRoom.id);
      if (fullRoom) {
        setSelectedRoom(fullRoom);
        setRoomInput(fullRoom.name || "");
      } else {
        // Room not in list yet, fetch it directly
        getFromOpenElisServer(
          `/rest/storage/rooms/${selectedRoom.id}`,
          (room) => {
            if (room) {
              setSelectedRoom(room);
              setRoomInput(room.name || "");
              setRooms((prev) => {
                // Add to rooms list if not already there
                if (!prev.find((r) => r.id === room.id)) {
                  return [...prev, room];
                }
                return prev;
              });
            }
          },
          () => {},
        );
      }
    }
  }, [selectedRoom, rooms]);

  // Load devices when room is selected or created
  useEffect(() => {
    if (selectedRoom && selectedRoom.id) {
      // Room has id - load devices for this room
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
      // Reset child selections only if this is a new room selection (not restoring from selectedLocation)
      if (!selectedLocation?.device && !selectedLocation?.shelf && !selectedLocation?.rack) {
        setSelectedDevice(null);
        setSelectedShelf(null);
        setSelectedRack(null);
        setDeviceInput("");
        setShelfInput("");
        setRackInput("");
        setShelves([]);
        setRacks([]);
      }
    } else if (selectedRoom && !selectedRoom.id && (isCreatingRoom || pendingRoomCreation)) {
      // Room is being created (typed but not yet saved) - keep devices empty but enable device field
      // Device field will be enabled because of the disabled prop logic
      setDevices([]);
    } else if (!selectedRoom) {
      // No room selected - clear devices and disable device field
      setDevices([]);
      setSelectedDevice(null);
      setDeviceInput("");
    }
  }, [selectedRoom, isCreatingRoom, pendingRoomCreation, selectedLocation]);

  // Load shelves when device is selected or created
  useEffect(() => {
    if (selectedDevice && selectedDevice.id) {
      // Device has id - load shelves for this device
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
    } else if (selectedDevice && !selectedDevice.id && isCreatingDevice) {
      // Device is being created (typed but not yet saved) - keep shelves empty but enable shelf field
      // Shelf field will be enabled because of the disabled prop logic
      setShelves([]);
    } else if (!selectedDevice) {
      // No device selected - clear shelves and disable shelf field
      setShelves([]);
      setSelectedShelf(null);
      setShelfInput("");
    }
  }, [selectedDevice, isCreatingDevice]);

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

      // Clear any pending room creation
      if (roomCreationTimeoutRef.current) {
        clearTimeout(roomCreationTimeoutRef.current);
        roomCreationTimeoutRef.current = null;
      }

      if (selectedItem) {
        // User selected from dropdown - room object has id
        setSelectedRoom(selectedItem);
        setIsCreatingRoom(false);
        setPendingRoomCreation(null);
        setShowAddRoomLink(false);
        // Clear room input to show selected item name
        setRoomInput(selectedItem.name || "");
        
        // Update parent's internal state (LocationSearchAndCreate) so it can track current selection
        if (onLocationChange) {
          const currentLocation = {
            room: selectedItem,
            device: selectedDevice || null,
            shelf: selectedShelf || null,
            rack: selectedRack || null,
            position: positionInput ? { coordinate: positionInput } : null,
          };
          onLocationChange(currentLocation);
        }
      } else if (trimmedValue) {
        // User is typing - check if room exists
        const existing = rooms.find(
          (r) => r.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          // Found existing room - set it with id
          setSelectedRoom(existing);
          setIsCreatingRoom(false);
          setPendingRoomCreation(null);
          setShowAddRoomLink(false);
          
          // Update parent's internal state (LocationSearchAndCreate)
          if (onLocationChange) {
            const currentLocation = {
              room: existing,
              device: selectedDevice || null,
              shelf: selectedShelf || null,
              rack: selectedRack || null,
              position: positionInput ? { coordinate: positionInput } : null,
            };
            onLocationChange(currentLocation);
          }
        } else {
          // New room - show "add new" link
          setIsCreatingRoom(true);
          const newRoom = {
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
          };
          setSelectedRoom(newRoom);
          setPendingRoomCreation(newRoom);
          setShowAddRoomLink(true);
          
          // Clear any existing timeout
          if (roomCreationTimeoutRef.current) {
            clearTimeout(roomCreationTimeoutRef.current);
            roomCreationTimeoutRef.current = null;
          }
        }
      } else {
        // Empty input - clear selection
        setSelectedRoom(null);
        setIsCreatingRoom(false);
        setPendingRoomCreation(null);
        setShowAddRoomLink(false);
        // Also clear child selections
        setSelectedDevice(null);
        setSelectedShelf(null);
        setSelectedRack(null);
        setDeviceInput("");
        setShelfInput("");
        setRackInput("");
      }
    },
    [rooms],
  );


  // Ref to track latest selected device (for onInputChange timing)
  const selectedDeviceRef = useRef(selectedDevice);
  useEffect(() => {
    selectedDeviceRef.current = selectedDevice;
  }, [selectedDevice]);

  // Helper: Check if Add button should be enabled for room
  const canAddRoom = useCallback(() => {
    const trimmed = roomInput.trim();
    if (!trimmed) return false;
    const matches = rooms.find((r) => r.name?.toLowerCase() === trimmed.toLowerCase());
    return !matches && trimmed.length > 0;
  }, [roomInput, rooms]);

  // Helper: Check if Add button should be enabled for device
  const canAddDevice = useCallback(() => {
    const trimmed = deviceInput.trim();
    if (!trimmed || !selectedRoom || (!selectedRoom.id && !isCreatingRoom && !pendingRoomCreation)) {
      return false;
    }
    const matches = devices.find((d) => d.name?.toLowerCase() === trimmed.toLowerCase());
    return !matches && trimmed.length > 0;
  }, [deviceInput, devices, selectedRoom, isCreatingRoom, pendingRoomCreation]);

  // Helper: Check if Add button should be enabled for shelf
  const canAddShelf = useCallback(() => {
    const trimmed = shelfInput.trim();
    if (!trimmed || !selectedDevice || (!selectedDevice.id && !isCreatingDevice)) {
      return false;
    }
    const matches = shelves.find((s) => (s.label || s.name)?.toLowerCase() === trimmed.toLowerCase());
    return !matches && trimmed.length > 0;
  }, [shelfInput, shelves, selectedDevice, isCreatingDevice]);

  // Helper: Check if Add button should be enabled for rack
  const canAddRack = useCallback(() => {
    const trimmed = rackInput.trim();
    if (!trimmed || !selectedShelf || (!selectedShelf.id && !isCreatingShelf)) {
      return false;
    }
    const matches = racks.find((r) => (r.label || r.name)?.toLowerCase() === trimmed.toLowerCase());
    return !matches && trimmed.length > 0;
  }, [rackInput, racks, selectedShelf, isCreatingShelf]);

  // Create device if input doesn't match existing
  const handleDeviceChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";

      if (selectedItem) {
        // User selected an item from dropdown - set it directly
        if (process.env.NODE_ENV === 'development') {
          console.log('[EnhancedCascadingMode] handleDeviceChange: selectedItem', JSON.stringify({
            id: selectedItem.id,
            name: selectedItem.name,
            code: selectedItem.code,
          }, null, 2));
        }
        setSelectedDevice(selectedItem);
        setDeviceInput(selectedItem.name || "");
        setIsCreatingDevice(false);
        setShowAddDeviceLink(false);
        
        // Update parent's internal state (LocationSearchAndCreate) so it can track current selection
        // This does NOT notify the grandparent (MoveSampleModal) - that only happens when "Add" is clicked
        if (onLocationChange) {
          const currentLocation = {
            room: selectedRoom || null,
            device: selectedItem,
            shelf: selectedShelf || null,
            rack: selectedRack || null,
            position: positionInput ? { coordinate: positionInput } : null,
          };
          onLocationChange(currentLocation);
        }
      } else if (trimmedValue && selectedRoom && (selectedRoom.id || isCreatingRoom || pendingRoomCreation)) {
        // User is typing - check if it matches existing or needs creation
        setDeviceInput(trimmedValue);
        const existing = devices.find(
          (d) => d.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedDevice(existing);
          setIsCreatingDevice(false);
          setShowAddDeviceLink(false);
        } else {
          setIsCreatingDevice(true);
          setSelectedDevice({
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
            type: "other",
          });
          setShowAddDeviceLink(true);
        }
      } else {
        // No input and no selected item - clear selection
        setSelectedDevice(null);
        setDeviceInput("");
        setIsCreatingDevice(false);
        setShowAddDeviceLink(false);
      }
    },
    [devices, selectedRoom, isCreatingRoom, pendingRoomCreation, onLocationChange, selectedShelf, selectedRack, positionInput],
  );

  // Handle Enter key for device - show confirmation
  const handleDeviceKeyDown = useCallback((e) => {
    if (e.key === 'Enter' && canAddDevice()) {
      e.preventDefault();
      setShowConfirmDevice(true);
    }
  }, [canAddDevice]);

  // Create shelf if input doesn't match existing
  const handleShelfChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setShelfInput(inputValue || "");

      if (selectedItem) {
        setSelectedShelf(selectedItem);
        setIsCreatingShelf(false);
        setShowAddShelfLink(false);
        
        // Update parent's internal state (LocationSearchAndCreate) so it can track current selection
        if (onLocationChange) {
          const currentLocation = {
            room: selectedRoom || null,
            device: selectedDevice || null,
            shelf: selectedItem,
            rack: selectedRack || null,
            position: positionInput ? { coordinate: positionInput } : null,
          };
          onLocationChange(currentLocation);
        }
      } else if (trimmedValue && selectedDevice && selectedDevice.id) {
        const existing = shelves.find(
          (s) => s.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedShelf(existing);
          setIsCreatingShelf(false);
          setShowAddShelfLink(false);
          
          // Don't call onLocationChange here - only when "Add" is clicked
        } else {
          setIsCreatingShelf(true);
          setSelectedShelf({ label: trimmedValue });
          setShowAddShelfLink(true);
        }
      } else {
        setSelectedShelf(null);
        setIsCreatingShelf(false);
        setShowAddShelfLink(false);
      }
    },
    [shelves, selectedDevice, onLocationChange, selectedRoom, selectedRack, positionInput],
  );

  // Create rack if input doesn't match existing
  const handleRackChange = useCallback(
    async (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";
      setRackInput(inputValue || "");

      if (selectedItem) {
        setSelectedRack(selectedItem);
        setIsCreatingRack(false);
        setShowAddRackLink(false);
        
        // Update parent's internal state (LocationSearchAndCreate) so it can track current selection
        if (onLocationChange) {
          const currentLocation = {
            room: selectedRoom || null,
            device: selectedDevice || null,
            shelf: selectedShelf || null,
            rack: selectedItem,
            position: positionInput ? { coordinate: positionInput } : null,
          };
          onLocationChange(currentLocation);
        }
      } else if (trimmedValue && selectedShelf && selectedShelf.id) {
        const existing = racks.find(
          (r) => r.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          setSelectedRack(existing);
          setIsCreatingRack(false);
          setShowAddRackLink(false);
          
          // Update parent's internal state (LocationSearchAndCreate)
          if (onLocationChange) {
            const currentLocation = {
              room: selectedRoom || null,
              device: selectedDevice || null,
              shelf: selectedShelf || null,
              rack: existing,
              position: positionInput ? { coordinate: positionInput } : null,
            };
            onLocationChange(currentLocation);
          }
        } else {
          setIsCreatingRack(true);
          setSelectedRack({ label: trimmedValue, rows: 0, columns: 0 });
          setShowAddRackLink(true);
        }
      } else {
        setSelectedRack(null);
        setIsCreatingRack(false);
        setShowAddRackLink(false);
      }
    },
    [racks, selectedShelf, onLocationChange, selectedRoom, selectedDevice, positionInput],
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
          
          // CRITICAL FIX: Immediately notify parent when device is created
          if (onLocationChange) {
            const currentLocation = {
              room: selectedRoom || null,
              device: created,
              shelf: selectedShelf || null,
              rack: selectedRack || null,
              position: positionInput ? { coordinate: positionInput } : null,
            };
            onLocationChange(currentLocation);
          }
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
        
        // Don't call onLocationChange here - only when "Add" is clicked
      },
      (error) => {
        console.error("Failed to create shelf:", error);
      },
    );
  }, [selectedShelf, selectedDevice, onLocationChange, selectedRoom, selectedRack, positionInput]);

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
        
        // Update parent's internal state (LocationSearchAndCreate)
        if (onLocationChange) {
          const currentLocation = {
            room: selectedRoom || null,
            device: selectedDevice || null,
            shelf: selectedShelf || null,
            rack: created,
            position: positionInput ? { coordinate: positionInput } : null,
          };
          onLocationChange(currentLocation);
        }
      },
      (error) => {
        console.error("Failed to create rack:", error);
      },
    );
  }, [selectedRack, selectedShelf, onLocationChange, selectedRoom, selectedDevice, positionInput]);

  // Note: Items are now created manually via "add new" links, not automatically

  // SIMPLIFIED WORKFLOW: Don't notify parent on every change - only when "Add" is clicked
  // This prevents state conflicts and race conditions
  // The parent component (LocationSearchAndCreate) will handle calling onLocationChange
  // when the user clicks "Add" button, after validating at least 2 levels are selected

  return (
    <div className="enhanced-cascading-container">
      {/* Confirmation dialogs */}
      {showConfirmDevice && selectedDevice && (
        <ComposedModal open={showConfirmDevice} onClose={() => setShowConfirmDevice(false)}>
          <ModalHeader
            title={intl.formatMessage({
              id: "label.button.confirmTitle",
              defaultMessage: "Confirm",
            })}
          />
          <ModalBody>
            <FormattedMessage
              id="storage.confirm.add.device"
              defaultMessage="Do you want to create a new device '{name}'?"
              values={{ name: selectedDevice.name || deviceInput }}
            />
          </ModalBody>
          <ModalFooter>
            <Button kind="secondary" onClick={() => setShowConfirmDevice(false)}>
              <FormattedMessage id="label.button.cancel" />
            </Button>
            <Button
              kind="primary"
              onClick={() => {
                if (selectedDevice && !selectedDevice.id && selectedRoom && selectedRoom.id) {
                  createDevice();
                }
                setShowConfirmDevice(false);
              }}
            >
              <FormattedMessage id="label.button.confirm" />
            </Button>
          </ModalFooter>
        </ComposedModal>
      )}
      {/* Room - ComboBox with autocomplete */}
      <div className="enhanced-cascading-row">
        <div className="enhanced-cascading-column enhanced-cascading-column-input">
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
            onInputChange={({ inputValue }) => {
              setRoomInput(inputValue || "");
              if (inputValue !== selectedRoom?.name) {
                handleRoomChange(inputValue, null);
              }
            }}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                handleRoomChange(selectedItem.name || "", selectedItem);
                setRoomInput(selectedItem.name || "");
              } else {
                handleRoomChange("", null);
                setRoomInput("");
              }
            }}
            selectedItem={selectedRoom}
            placeholder={intl.formatMessage({
              id: "storage.room.placeholder",
              defaultMessage: "Select or create room...",
            })}
          />
        </div>
        <div className="enhanced-cascading-column enhanced-cascading-column-button">
          <div className="inline-add-button-wrapper">
            <Button
              kind="ghost"
              size="md"
              onClick={() => {
                if (canAddRoom() && selectedRoom && !selectedRoom.id) {
                  createRoom();
                }
              }}
              data-testid="add-new-room-button"
              disabled={!canAddRoom()}
            >
              <Add size={16} />
              <FormattedMessage
                id="storage.add.new"
                defaultMessage="Add new"
              />
            </Button>
          </div>
        </div>
      </div>

      {/* Device - ComboBox with autocomplete */}
      <div className="enhanced-cascading-row">
        <div className="enhanced-cascading-column enhanced-cascading-column-input">
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
            onChange={({ selectedItem }) => {
              if (process.env.NODE_ENV === 'development') {
                console.log('[EnhancedCascadingMode] Device ComboBox onChange:', selectedItem ? JSON.stringify({
                  id: selectedItem.id,
                  name: selectedItem.name,
                }, null, 2) : 'null');
              }
              if (selectedItem) {
                selectedDeviceRef.current = selectedItem;
                handleDeviceChange(selectedItem.name || "", selectedItem);
                setDeviceInput(selectedItem.name || "");
              } else {
                selectedDeviceRef.current = null;
                handleDeviceChange("", null);
                setDeviceInput("");
              }
            }}
            onInputChange={({ inputValue }) => {
              const currentSelected = selectedDeviceRef.current;
              if (!currentSelected || currentSelected.name?.toLowerCase() !== inputValue?.trim()?.toLowerCase()) {
                handleDeviceChange(inputValue, null);
              }
            }}
            onKeyDown={handleDeviceKeyDown}
            selectedItem={selectedDevice}
            disabled={
              !selectedRoom ||
              (!selectedRoom.id && !isCreatingRoom && !pendingRoomCreation)
            }
            placeholder={intl.formatMessage({
              id: "storage.device.placeholder",
              defaultMessage: "Select or create device...",
            })}
          />
        </div>
        <div className="enhanced-cascading-column enhanced-cascading-column-button">
          <div className="inline-add-button-wrapper">
            <Button
              kind="ghost"
              size="md"
              onClick={() => {
                if (canAddDevice() && selectedDevice && !selectedDevice.id && selectedRoom && selectedRoom.id) {
                  createDevice();
                }
              }}
              data-testid="add-new-device-button"
              disabled={!canAddDevice()}
            >
              <Add size={16} />
              <FormattedMessage
                id="storage.add.new"
                defaultMessage="Add new"
              />
            </Button>
          </div>
        </div>
      </div>

      {/* Shelf - ComboBox with autocomplete */}
      <div className="enhanced-cascading-row">
        <div className="enhanced-cascading-column enhanced-cascading-column-input">
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
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                handleShelfChange(selectedItem.label || selectedItem.name || "", selectedItem);
                setShelfInput(selectedItem.label || selectedItem.name || "");
              } else {
                handleShelfChange("", null);
                setShelfInput("");
              }
            }}
            selectedItem={selectedShelf}
            disabled={(() => {
              const isDisabled = !selectedDevice || (!selectedDevice.id && !isCreatingDevice);
              return isDisabled;
            })()}
            placeholder={intl.formatMessage({
              id: "storage.shelf.placeholder",
              defaultMessage: "Select or create shelf...",
            })}
          />
        </div>
        <div className="enhanced-cascading-column enhanced-cascading-column-button">
          <div className="inline-add-button-wrapper">
            <Button
              kind="ghost"
              size="md"
              onClick={() => {
                if (canAddShelf() && selectedShelf && !selectedShelf.id && selectedDevice && selectedDevice.id) {
                  createShelf();
                }
              }}
              data-testid="add-new-shelf-button"
              disabled={!canAddShelf()}
            >
              <Add size={16} />
              <FormattedMessage
                id="storage.add.new"
                defaultMessage="Add new"
              />
            </Button>
          </div>
        </div>
      </div>

      {/* Rack - ComboBox with autocomplete */}
      <div className="enhanced-cascading-row">
        <div className="enhanced-cascading-column enhanced-cascading-column-input">
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
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                handleRackChange(selectedItem.label || selectedItem.name || "", selectedItem);
                setRackInput(selectedItem.label || selectedItem.name || "");
              } else {
                handleRackChange("", null);
                setRackInput("");
              }
            }}
            selectedItem={selectedRack}
            disabled={!selectedShelf || (!selectedShelf.id && !isCreatingShelf)}
            placeholder={intl.formatMessage({
              id: "storage.rack.placeholder",
              defaultMessage: "Select or create rack...",
            })}
          />
        </div>
        <div className="enhanced-cascading-column enhanced-cascading-column-button">
          <div className="inline-add-button-wrapper">
            <Button
              kind="ghost"
              size="md"
              onClick={() => {
                if (canAddRack() && selectedRack && !selectedRack.id && selectedShelf && selectedShelf.id) {
                  createRack();
                }
              }}
              data-testid="add-new-rack-button"
              disabled={!canAddRack()}
            >
              <Add size={16} />
              <FormattedMessage
                id="storage.add.new"
                defaultMessage="Add new"
              />
            </Button>
          </div>
        </div>
      </div>

      {/* Position - Simple text input (optional) */}
      <div className="enhanced-cascading-row">
        <div className="enhanced-cascading-column enhanced-cascading-column-full">
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
      </div>
    </div>
  );
};

export default EnhancedCascadingMode;
