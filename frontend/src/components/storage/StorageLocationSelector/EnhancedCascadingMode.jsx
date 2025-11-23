import React, {
  useState,
  useEffect,
  useCallback,
  useRef,
  useContext,
} from "react";
import {
  ComboBox,
  TextInput,
  Button,
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
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
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);

  const [roomInput, setRoomInput] = useState("");
  const [deviceInput, setDeviceInput] = useState("");
  const [shelfInput, setShelfInput] = useState("");
  const [rackInput, setRackInput] = useState("");
  const [positionInput, setPositionInput] = useState("");

  // Refs to track current input values synchronously (for button validation)
  const roomInputRef = useRef("");
  const deviceInputRef = useRef("");
  const shelfInputRef = useRef("");
  const rackInputRef = useRef("");

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
  const lastRoomInputRef = useRef("");
  // Removed showAddRoomLink - using button only, no link
  const [showAddDeviceLink, setShowAddDeviceLink] = useState(false);
  const [showAddShelfLink, setShowAddShelfLink] = useState(false);
  const [showAddRackLink, setShowAddRackLink] = useState(false);

  // Confirmation dialogs for Enter key
  const [showConfirmRoom, setShowConfirmRoom] = useState(false);
  const [showConfirmDevice, setShowConfirmDevice] = useState(false);
  const [showConfirmShelf, setShowConfirmShelf] = useState(false);
  const [showConfirmRack, setShowConfirmRack] = useState(false);

  /**
   * Helper function to build location object with locationId, locationType, and positionCoordinate
   * for the new flexible assignment architecture
   * Priority: rack > shelf > device (lowest selected level wins)
   */
  const buildLocationWithFlexibleFields = useCallback((location) => {
    if (!location) return null;

    const result = { ...location };

    // Extract locationId and locationType based on lowest selected hierarchy level
    if (location.rack && location.rack.id) {
      result.locationId = location.rack.id;
      result.locationType = "rack";
    } else if (location.shelf && location.shelf.id) {
      result.locationId = location.shelf.id;
      result.locationType = "shelf";
    } else if (location.device && location.device.id) {
      result.locationId = location.device.id;
      result.locationType = "device";
    }

    // Extract positionCoordinate from position object or direct field
    result.positionCoordinate =
      location.position?.coordinate || location.positionCoordinate || null;

    return result;
  }, []);

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
      if (selectedLocation.room && typeof selectedLocation.room === "object") {
        // If room only has id, try to find full room object in rooms list
        if (
          selectedLocation.room.id &&
          !selectedLocation.room.name &&
          rooms.length > 0
        ) {
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
      if (
        selectedLocation.device &&
        typeof selectedLocation.device === "object"
      ) {
        setSelectedDevice(selectedLocation.device);
        setDeviceInput(selectedLocation.device.name || "");
      }
      if (
        selectedLocation.shelf &&
        typeof selectedLocation.shelf === "object"
      ) {
        setSelectedShelf(selectedLocation.shelf);
        setShelfInput(
          selectedLocation.shelf.label || selectedLocation.shelf.name || "",
        );
      }
      if (selectedLocation.rack && typeof selectedLocation.rack === "object") {
        setSelectedRack(selectedLocation.rack);
        setRackInput(
          selectedLocation.rack.label || selectedLocation.rack.name || "",
        );
      }
      if (
        selectedLocation.position &&
        typeof selectedLocation.position === "object"
      ) {
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
            const roomObj = {
              id: selectedLocation.id,
              name: selectedLocation.name,
              code:
                selectedLocation.code ||
                selectedLocation.name.substring(0, 50).toUpperCase(),
              active: selectedLocation.active !== false,
              ...selectedLocation,
            };
            setSelectedRoom(roomObj);

            const locationObj = {
              room: roomObj,
              device: null,
              shelf: null,
              rack: null,
              position: null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
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

              // Update parent with location
              const currentDevice = selectedDeviceRef.current || selectedDevice;
              const currentShelf = selectedShelfRef.current || selectedShelf;
              const currentRack = selectedRackRef.current || selectedRack;
              const currentPosition = positionInput;
              const locationObj = {
                room: room,
                device: currentDevice || null,
                shelf: currentShelf || null,
                rack: currentRack || null,
                position: currentPosition
                  ? { coordinate: currentPosition }
                  : null,
              };
              onLocationChange(buildLocationWithFlexibleFields(locationObj));
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
      if (
        !selectedLocation?.device &&
        !selectedLocation?.shelf &&
        !selectedLocation?.rack
      ) {
        setSelectedDevice(null);
        setSelectedShelf(null);
        setSelectedRack(null);
        setDeviceInput("");
        setShelfInput("");
        setRackInput("");
        setShelves([]);
        setRacks([]);
      }
    } else if (
      selectedRoom &&
      !selectedRoom.id &&
      (isCreatingRoom || pendingRoomCreation)
    ) {
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

  // Create room if input doesn't match existing - FULLY SYNCHRONOUS
  const handleRoomChange = useCallback(
    (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";

      // Clear any pending room creation
      if (roomCreationTimeoutRef.current) {
        clearTimeout(roomCreationTimeoutRef.current);
        roomCreationTimeoutRef.current = null;
      }

      // Get current values from refs for synchronous access
      const currentRoom = selectedRoomRef.current || selectedRoom;
      const currentDevice = selectedDeviceRef.current || selectedDevice;
      const currentShelf = selectedShelfRef.current || selectedShelf;
      const currentRack = selectedRackRef.current || selectedRack;
      const currentPosition = positionInput;

      if (selectedItem) {
        // User selected from dropdown - room object has id
        selectedRoomRef.current = selectedItem;
        setSelectedRoom(selectedItem);
        setIsCreatingRoom(false);
        setPendingRoomCreation(null);
        // Button state managed via canAddRoom() - no link state needed
        roomInputRef.current = selectedItem.name || "";
        setRoomInput(selectedItem.name || "");

        // Update parent immediately with current ref values
        if (onLocationChange) {
          const locationObj = {
            room: selectedItem,
            device: currentDevice || null,
            shelf: currentShelf || null,
            rack: currentRack || null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      } else if (trimmedValue) {
        // User is typing - check if room exists
        const existing = rooms.find(
          (r) => r.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          // Found existing room - set it with id
          selectedRoomRef.current = existing;
          setSelectedRoom(existing);
          setIsCreatingRoom(false);
          setPendingRoomCreation(null);
          // Button state managed via canAddRoom() - no link state needed
          roomInputRef.current = existing.name || "";
          setRoomInput(existing.name || "");

          // Update parent immediately
          if (onLocationChange) {
            const locationObj = {
              room: existing,
              device: currentDevice || null,
              shelf: currentShelf || null,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        } else {
          // New room - show "add new" link
          if (process.env.NODE_ENV === "development") {
            console.log(
              "[EnhancedCascadingMode] handleRoomChange: Setting new room creation mode",
              {
                trimmedValue,
                existingRooms: rooms.map((r) => r.name),
              },
            );
          }
          setIsCreatingRoom(true);
          const newRoom = {
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
          };
          selectedRoomRef.current = newRoom;
          setSelectedRoom(newRoom);
          setPendingRoomCreation(newRoom);
          // Button enabled via canAddRoom() - no link state needed
          // Keep input value - don't clear it
          // Track last input to prevent empty string cleanup from clearing state
          lastRoomInputRef.current = trimmedValue;

          // Update parent immediately with new room (no id yet)
          if (onLocationChange) {
            const locationObj = {
              room: newRoom,
              device: currentDevice || null,
              shelf: currentShelf || null,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        }
      } else {
        // Empty input - but check if we just had input (to prevent test cleanup from clearing state)
        // If we just set a room creation, ignore empty strings that come immediately after
        const hasInput = roomInputRef.current && roomInputRef.current.trim();
        const justHadInput =
          lastRoomInputRef.current && lastRoomInputRef.current.trim();
        if (hasInput || isCreatingRoom || justHadInput) {
          // Keep the input - user was typing, don't clear on empty events
          // Restore the ref from lastRoomInputRef if it was cleared
          if (!roomInputRef.current && lastRoomInputRef.current) {
            roomInputRef.current = lastRoomInputRef.current;
            setRoomInput(lastRoomInputRef.current);
          }
          // Restore selectedRoomRef from pendingRoomCreation if it was cleared
          if (!selectedRoomRef.current && pendingRoomCreation) {
            selectedRoomRef.current = pendingRoomCreation;
            setSelectedRoom(pendingRoomCreation);
          }
          return;
        }
        // Empty input - clear selection (user explicitly cleared)
        selectedRoomRef.current = null;
        setSelectedRoom(null);
        setIsCreatingRoom(false);
        setPendingRoomCreation(null);
        // Button state managed via canAddRoom() - no link state needed
        roomInputRef.current = "";
        setRoomInput("");
        // Also clear child selections
        selectedDeviceRef.current = null;
        selectedShelfRef.current = null;
        selectedRackRef.current = null;
        setSelectedDevice(null);
        setSelectedShelf(null);
        setSelectedRack(null);
        deviceInputRef.current = "";
        shelfInputRef.current = "";
        rackInputRef.current = "";
        setDeviceInput("");
        setShelfInput("");
        setRackInput("");

        // Update parent immediately
        if (onLocationChange) {
          onLocationChange(
            buildLocationWithFlexibleFields({
              room: null,
              device: null,
              shelf: null,
              rack: null,
              position: null,
            }),
          );
        }
      }
    },
    [
      rooms,
      onLocationChange,
      selectedDevice,
      selectedShelf,
      selectedRack,
      positionInput,
      buildLocationWithFlexibleFields,
    ],
  );

  // Refs to track latest selected items synchronously
  const selectedRoomRef = useRef(selectedRoom);
  const selectedDeviceRef = useRef(selectedDevice);
  const selectedShelfRef = useRef(selectedShelf);
  const selectedRackRef = useRef(selectedRack);

  useEffect(() => {
    selectedRoomRef.current = selectedRoom;
  }, [selectedRoom]);

  useEffect(() => {
    selectedDeviceRef.current = selectedDevice;
  }, [selectedDevice]);

  useEffect(() => {
    selectedShelfRef.current = selectedShelf;
  }, [selectedShelf]);

  useEffect(() => {
    selectedRackRef.current = selectedRack;
  }, [selectedRack]);

  // Helper: Check if Add button should be enabled for room
  const canAddRoom = useCallback(() => {
    // Use ref for synchronous access to current input value
    const currentInput = roomInputRef.current || roomInput;
    const trimmed = currentInput.trim();
    const result = (() => {
      if (!trimmed) return false;
      // Must be in creation mode (isCreatingRoom flag is true)
      if (!isCreatingRoom && !pendingRoomCreation) return false;
      const matches = rooms.find(
        (r) => r.name?.toLowerCase() === trimmed.toLowerCase(),
      );
      return !matches && trimmed.length > 0;
    })();
    if (process.env.NODE_ENV === "development") {
      console.log("[EnhancedCascadingMode] canAddRoom:", {
        currentInput,
        trimmed,
        isCreatingRoom,
        pendingRoomCreation: !!pendingRoomCreation,
        matchesExisting: !!rooms.find(
          (r) => r.name?.toLowerCase() === trimmed.toLowerCase(),
        ),
        result,
      });
    }
    return result;
  }, [roomInput, rooms, isCreatingRoom, pendingRoomCreation]);

  // Helper: Check if Add button should be enabled for device
  const canAddDevice = useCallback(() => {
    // Use ref for synchronous access to current input value
    const currentInput = deviceInputRef.current || deviceInput;
    const trimmed = currentInput.trim();
    const result = (() => {
      if (!trimmed) return false;
      // Must have a valid room (with id or being created)
      if (
        !selectedRoom ||
        (!selectedRoom.id && !isCreatingRoom && !pendingRoomCreation)
      ) {
        return false;
      }
      // Must be in creation mode (isCreatingDevice flag is true)
      if (!isCreatingDevice) return false;
      const matches = devices.find(
        (d) => d.name?.toLowerCase() === trimmed.toLowerCase(),
      );
      return !matches && trimmed.length > 0;
    })();
    if (process.env.NODE_ENV === "development") {
      console.log("[EnhancedCascadingMode] canAddDevice:", {
        currentInput,
        trimmed,
        selectedRoom: selectedRoom
          ? { id: selectedRoom.id, name: selectedRoom.name }
          : null,
        isCreatingRoom,
        pendingRoomCreation: !!pendingRoomCreation,
        isCreatingDevice,
        matchesExisting: !!devices.find(
          (d) => d.name?.toLowerCase() === trimmed.toLowerCase(),
        ),
        result,
      });
    }
    return result;
  }, [
    deviceInput,
    devices,
    selectedRoom,
    isCreatingRoom,
    pendingRoomCreation,
    isCreatingDevice,
  ]);

  // Helper: Check if Add button should be enabled for shelf
  const canAddShelf = useCallback(() => {
    // Use ref for synchronous access to current input value
    const currentInput = shelfInputRef.current || shelfInput;
    const trimmed = currentInput.trim();
    const result = (() => {
      if (!trimmed) return false;
      // Must have a valid device (with id or being created)
      if (!selectedDevice || (!selectedDevice.id && !isCreatingDevice)) {
        return false;
      }
      // Must be in creation mode (isCreatingShelf flag is true)
      if (!isCreatingShelf) return false;
      const matches = shelves.find(
        (s) => (s.label || s.name)?.toLowerCase() === trimmed.toLowerCase(),
      );
      return !matches && trimmed.length > 0;
    })();
    if (process.env.NODE_ENV === "development") {
      console.log("[EnhancedCascadingMode] canAddShelf:", {
        currentInput,
        trimmed,
        selectedDevice: selectedDevice
          ? { id: selectedDevice.id, name: selectedDevice.name }
          : null,
        isCreatingDevice,
        isCreatingShelf,
        matchesExisting: !!shelves.find(
          (s) => (s.label || s.name)?.toLowerCase() === trimmed.toLowerCase(),
        ),
        result,
      });
    }
    return result;
  }, [shelfInput, shelves, selectedDevice, isCreatingDevice, isCreatingShelf]);

  // Helper: Check if Add button should be enabled for rack
  const canAddRack = useCallback(() => {
    // Use ref for synchronous access to current input value
    const currentInput = rackInputRef.current || rackInput;
    const trimmed = currentInput.trim();
    const result = (() => {
      if (!trimmed) return false;
      // Must have a valid shelf (with id or being created)
      if (!selectedShelf || (!selectedShelf.id && !isCreatingShelf)) {
        return false;
      }
      // Must be in creation mode (isCreatingRack flag is true)
      if (!isCreatingRack) return false;
      const matches = racks.find(
        (r) => (r.label || r.name)?.toLowerCase() === trimmed.toLowerCase(),
      );
      return !matches && trimmed.length > 0;
    })();
    if (process.env.NODE_ENV === "development") {
      console.log("[EnhancedCascadingMode] canAddRack:", {
        currentInput,
        trimmed,
        selectedShelf: selectedShelf
          ? { id: selectedShelf.id, label: selectedShelf.label }
          : null,
        isCreatingShelf,
        isCreatingRack,
        matchesExisting: !!racks.find(
          (r) => (r.label || r.name)?.toLowerCase() === trimmed.toLowerCase(),
        ),
        result,
      });
    }
    return result;
  }, [rackInput, racks, selectedShelf, isCreatingShelf, isCreatingRack]);

  // Create device if input doesn't match existing - FULLY SYNCHRONOUS
  const handleDeviceChange = useCallback(
    (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";

      // Get current values from refs for synchronous access
      const currentRoom = selectedRoomRef.current || selectedRoom;
      const currentShelf = selectedShelfRef.current || selectedShelf;
      const currentRack = selectedRackRef.current || selectedRack;
      const currentPosition = positionInput;

      if (selectedItem) {
        // User selected from dropdown
        selectedDeviceRef.current = selectedItem;
        setSelectedDevice(selectedItem);
        setIsCreatingDevice(false);
        setShowAddDeviceLink(false);
        deviceInputRef.current = selectedItem.name || "";
        setDeviceInput(selectedItem.name || "");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: selectedItem,
            shelf: currentShelf || null,
            rack: currentRack || null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      } else if (
        trimmedValue &&
        currentRoom &&
        (currentRoom.id || isCreatingRoom || pendingRoomCreation)
      ) {
        // User is typing - check if it matches existing or needs creation
        const existing = devices.find(
          (d) => d.name?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          selectedDeviceRef.current = existing;
          setSelectedDevice(existing);
          setIsCreatingDevice(false);
          setShowAddDeviceLink(false);
          deviceInputRef.current = existing.name || "";
          setDeviceInput(existing.name || "");

          // Update parent immediately
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: existing,
              shelf: currentShelf || null,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        } else {
          setIsCreatingDevice(true);
          const newDevice = {
            name: trimmedValue,
            code: trimmedValue.substring(0, 50).toUpperCase(),
            type: "other",
          };
          selectedDeviceRef.current = newDevice;
          setSelectedDevice(newDevice);
          setShowAddDeviceLink(true);

          // Update parent immediately with new device (no id yet)
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: newDevice,
              shelf: currentShelf || null,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        }
      } else {
        // Empty input - clear selection
        selectedDeviceRef.current = null;
        setSelectedDevice(null);
        setIsCreatingDevice(false);
        setShowAddDeviceLink(false);
        deviceInputRef.current = "";
        setDeviceInput("");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: null,
            shelf: currentShelf || null,
            rack: currentRack || null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      }
    },
    [
      devices,
      selectedRoom,
      isCreatingRoom,
      pendingRoomCreation,
      onLocationChange,
      selectedShelf,
      selectedRack,
      positionInput,
      buildLocationWithFlexibleFields,
    ],
  );

  // Handle Enter key for device - show confirmation
  const handleDeviceKeyDown = useCallback(
    (e) => {
      if (e.key === "Enter" && canAddDevice()) {
        e.preventDefault();
        setShowConfirmDevice(true);
      }
    },
    [canAddDevice],
  );

  // Create shelf if input doesn't match existing - FULLY SYNCHRONOUS
  const handleShelfChange = useCallback(
    (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";

      // Get current values from refs for synchronous access
      const currentRoom = selectedRoomRef.current || selectedRoom;
      const currentDevice = selectedDeviceRef.current || selectedDevice;
      const currentRack = selectedRackRef.current || selectedRack;
      const currentPosition = positionInput;

      if (selectedItem) {
        selectedShelfRef.current = selectedItem;
        setSelectedShelf(selectedItem);
        setIsCreatingShelf(false);
        setShowAddShelfLink(false);
        shelfInputRef.current = selectedItem.label || selectedItem.name || "";
        setShelfInput(selectedItem.label || selectedItem.name || "");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: currentDevice || null,
            shelf: selectedItem,
            rack: currentRack || null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      } else if (trimmedValue && currentDevice && currentDevice.id) {
        const existing = shelves.find(
          (s) => s.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          selectedShelfRef.current = existing;
          setSelectedShelf(existing);
          setIsCreatingShelf(false);
          setShowAddShelfLink(false);
          shelfInputRef.current = existing.label || "";
          setShelfInput(existing.label || "");

          // Update parent immediately
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: currentDevice || null,
              shelf: existing,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        } else {
          setIsCreatingShelf(true);
          const newShelf = { label: trimmedValue };
          selectedShelfRef.current = newShelf;
          setSelectedShelf(newShelf);
          setShowAddShelfLink(true);

          // Update parent immediately with new shelf (no id yet)
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: currentDevice || null,
              shelf: newShelf,
              rack: currentRack || null,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        }
      } else {
        selectedShelfRef.current = null;
        setSelectedShelf(null);
        setIsCreatingShelf(false);
        setShowAddShelfLink(false);
        shelfInputRef.current = "";
        setShelfInput("");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: currentDevice || null,
            shelf: null,
            rack: currentRack || null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      }
    },
    [
      shelves,
      selectedDevice,
      onLocationChange,
      selectedRoom,
      selectedRack,
      positionInput,
      buildLocationWithFlexibleFields,
    ],
  );

  // Create rack if input doesn't match existing - FULLY SYNCHRONOUS
  const handleRackChange = useCallback(
    (inputValue, selectedItem) => {
      const trimmedValue = inputValue?.trim() || "";

      // Get current values from refs for synchronous access
      const currentRoom = selectedRoomRef.current || selectedRoom;
      const currentDevice = selectedDeviceRef.current || selectedDevice;
      const currentShelf = selectedShelfRef.current || selectedShelf;
      const currentPosition = positionInput;

      if (selectedItem) {
        selectedRackRef.current = selectedItem;
        setSelectedRack(selectedItem);
        setIsCreatingRack(false);
        setShowAddRackLink(false);
        rackInputRef.current = selectedItem.label || selectedItem.name || "";
        setRackInput(selectedItem.label || selectedItem.name || "");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: currentDevice || null,
            shelf: currentShelf || null,
            rack: selectedItem,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      } else if (trimmedValue && currentShelf && currentShelf.id) {
        const existing = racks.find(
          (r) => r.label?.toLowerCase() === trimmedValue.toLowerCase(),
        );
        if (existing) {
          selectedRackRef.current = existing;
          setSelectedRack(existing);
          setIsCreatingRack(false);
          setShowAddRackLink(false);
          rackInputRef.current = existing.label || "";
          setRackInput(existing.label || "");

          // Update parent immediately
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: currentDevice || null,
              shelf: currentShelf || null,
              rack: existing,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        } else {
          setIsCreatingRack(true);
          const newRack = { label: trimmedValue, rows: 0, columns: 0 };
          selectedRackRef.current = newRack;
          setSelectedRack(newRack);
          setShowAddRackLink(true);

          // Update parent immediately with new rack (no id yet)
          if (onLocationChange) {
            const locationObj = {
              room: currentRoom || null,
              device: currentDevice || null,
              shelf: currentShelf || null,
              rack: newRack,
              position: currentPosition
                ? { coordinate: currentPosition }
                : null,
            };
            onLocationChange(buildLocationWithFlexibleFields(locationObj));
          }
        }
      } else {
        selectedRackRef.current = null;
        setSelectedRack(null);
        setIsCreatingRack(false);
        setShowAddRackLink(false);
        rackInputRef.current = "";
        setRackInput("");

        // Update parent immediately
        if (onLocationChange) {
          const locationObj = {
            room: currentRoom || null,
            device: currentDevice || null,
            shelf: currentShelf || null,
            rack: null,
            position: currentPosition ? { coordinate: currentPosition } : null,
          };
          onLocationChange(buildLocationWithFlexibleFields(locationObj));
        }
      }
    },
    [
      racks,
      selectedShelf,
      onLocationChange,
      selectedRoom,
      selectedDevice,
      positionInput,
      buildLocationWithFlexibleFields,
    ],
  );

  // Create room via API
  const createRoom = useCallback(async () => {
    // Use ref for synchronous access - state might not be updated yet
    // Also check pendingRoomCreation as fallback
    const currentRoom =
      selectedRoomRef.current || selectedRoom || pendingRoomCreation;
    if (!currentRoom || !currentRoom.name || currentRoom.id) {
      return;
    }

    // Code is optional - backend will auto-generate if not provided
    const formData = {
      name: currentRoom.name,
      // code: optional - backend generates unique code automatically
      description: "",
      active: true,
    };

    postToOpenElisServerJsonResponse(
      "/rest/storage/rooms",
      JSON.stringify(formData),
      (response) => {
        // Check if response has error property (400 response)
        if (response.error) {
          console.error("Failed to create room:", response.error);
          // Show error notification
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage(
                { id: "storage.create.room.error" },
                { error: response.error },
              ) || `Failed to create room: ${response.error}`,
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
          // Don't clear state - keep user's input so they can fix it
          // Keep isCreatingRoom true so "Add new" button stays enabled
          return;
        }
        // Success - response is the created room object
        selectedRoomRef.current = response;
        setSelectedRoom(response);
        roomInputRef.current = response.name || "";
        setRoomInput(response.name || "");
        setRooms((prev) => [...prev, response]);
        setIsCreatingRoom(false);
        setPendingRoomCreation(null);
        // Button state managed via canAddRoom() - no link state needed

        // Show success notification
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage(
              { id: "storage.create.room.success" },
              { name: response.name },
            ) || `Room "${response.name}" created successfully`,
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);

        // Update parent immediately with created room
        const currentDevice = selectedDeviceRef.current || selectedDevice;
        const currentShelf = selectedShelfRef.current || selectedShelf;
        const currentRack = selectedRackRef.current || selectedRack;
        const currentPosition = positionInput;
        const locationObj = {
          room: response,
          device: currentDevice || null,
          shelf: currentShelf || null,
          rack: currentRack || null,
          position: currentPosition ? { coordinate: currentPosition } : null,
        };
        onLocationChange(buildLocationWithFlexibleFields(locationObj));
      },
    );
  }, [
    selectedRoom,
    onLocationChange,
    selectedDevice,
    selectedShelf,
    selectedRack,
    positionInput,
    addNotification,
    setNotificationVisible,
    intl,
  ]);

  // Create device via API
  const createDevice = useCallback(async () => {
    if (
      !selectedDevice ||
      !selectedDevice.name ||
      selectedDevice.id ||
      !selectedRoom?.id
    )
      return;

    // Code is optional - backend will auto-generate if not provided
    const formData = {
      name: selectedDevice.name,
      // code: optional - backend generates unique code automatically
      type: selectedDevice.type || "other",
      active: true,
      parentRoomId: String(selectedRoom.id),
    };

    postToOpenElisServerJsonResponse(
      "/rest/storage/devices",
      JSON.stringify(formData),
      (response) => {
        if (response.error) {
          console.error("Failed to create device:", response.error);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage(
                { id: "storage.create.device.error" },
                { error: response.error },
              ) || `Failed to create device: ${response.error}`,
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
          return;
        }
        selectedDeviceRef.current = response;
        setSelectedDevice(response);
        deviceInputRef.current = response.name || "";
        setDeviceInput(response.name || "");
        setDevices((prev) => [...prev, response]);
        setIsCreatingDevice(false);
        setShowAddDeviceLink(false);

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage(
              { id: "storage.create.device.success" },
              { name: response.name },
            ) || `Device "${response.name}" created successfully`,
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);

        // Update parent immediately
        const currentRoom = selectedRoomRef.current || selectedRoom;
        const currentShelf = selectedShelfRef.current || selectedShelf;
        const currentRack = selectedRackRef.current || selectedRack;
        const currentPosition = positionInput;
        const locationObj = {
          room: currentRoom || null,
          device: response,
          shelf: currentShelf || null,
          rack: currentRack || null,
          position: currentPosition ? { coordinate: currentPosition } : null,
        };
        onLocationChange(buildLocationWithFlexibleFields(locationObj));
      },
    );
  }, [
    selectedDevice,
    selectedRoom,
    onLocationChange,
    selectedShelf,
    selectedRack,
    positionInput,
    addNotification,
    setNotificationVisible,
    intl,
  ]);

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

    postToOpenElisServerJsonResponse(
      "/rest/storage/shelves",
      JSON.stringify(formData),
      (response) => {
        if (response.error) {
          console.error("Failed to create shelf:", response.error);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage(
                { id: "storage.create.shelf.error" },
                { error: response.error },
              ) || `Failed to create shelf: ${response.error}`,
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
          return;
        }
        selectedShelfRef.current = response;
        setSelectedShelf(response);
        shelfInputRef.current = response.label || "";
        setShelfInput(response.label || "");
        setShelves((prev) => [...prev, response]);
        setIsCreatingShelf(false);
        setShowAddShelfLink(false);

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage(
              { id: "storage.create.shelf.success" },
              { label: response.label },
            ) || `Shelf "${response.label}" created successfully`,
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);

        // Update parent immediately
        const currentRoom = selectedRoomRef.current || selectedRoom;
        const currentDevice = selectedDeviceRef.current || selectedDevice;
        const currentRack = selectedRackRef.current || selectedRack;
        const currentPosition = positionInput;
        const locationObj = {
          room: currentRoom || null,
          device: currentDevice || null,
          shelf: response,
          rack: currentRack || null,
          position: currentPosition ? { coordinate: currentPosition } : null,
        };
        onLocationChange(buildLocationWithFlexibleFields(locationObj));
      },
    );
  }, [
    selectedShelf,
    selectedDevice,
    onLocationChange,
    selectedRoom,
    selectedRack,
    positionInput,
    addNotification,
    setNotificationVisible,
    intl,
  ]);

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

    postToOpenElisServerJsonResponse(
      "/rest/storage/racks",
      JSON.stringify(formData),
      (response) => {
        if (response.error) {
          console.error("Failed to create rack:", response.error);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage(
                { id: "storage.create.rack.error" },
                { error: response.error },
              ) || `Failed to create rack: ${response.error}`,
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
          return;
        }
        selectedRackRef.current = response;
        setSelectedRack(response);
        rackInputRef.current = response.label || "";
        setRackInput(response.label || "");
        setRacks((prev) => [...prev, response]);
        setIsCreatingRack(false);
        setShowAddRackLink(false);

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage(
              { id: "storage.create.rack.success" },
              { label: response.label },
            ) || `Rack "${response.label}" created successfully`,
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);

        // Update parent immediately
        const currentRoom = selectedRoomRef.current || selectedRoom;
        const currentDevice = selectedDeviceRef.current || selectedDevice;
        const currentShelf = selectedShelfRef.current || selectedShelf;
        const currentPosition = positionInput;
        const locationObj = {
          room: currentRoom || null,
          device: currentDevice || null,
          shelf: currentShelf || null,
          rack: response,
          position: currentPosition ? { coordinate: currentPosition } : null,
        };
        onLocationChange(buildLocationWithFlexibleFields(locationObj));
      },
    );
  }, [
    selectedRack,
    selectedShelf,
    onLocationChange,
    selectedRoom,
    selectedDevice,
    positionInput,
    addNotification,
    setNotificationVisible,
    intl,
  ]);

  // Note: Items are now created manually via "add new" links, not automatically

  // SIMPLIFIED WORKFLOW: Don't notify parent on every change - only when "Add" is clicked
  // This prevents state conflicts and race conditions
  // The parent component (LocationSearchAndCreate) will handle calling onLocationChange
  // when the user clicks "Add" button, after validating at least 2 levels are selected

  return (
    <div className="enhanced-cascading-container">
      {/* Confirmation dialogs */}
      {showConfirmDevice && selectedDevice && (
        <ComposedModal
          open={showConfirmDevice}
          onClose={() => setShowConfirmDevice(false)}
        >
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
            <Button
              kind="secondary"
              onClick={() => setShowConfirmDevice(false)}
            >
              <FormattedMessage id="label.button.cancel" />
            </Button>
            <Button
              kind="primary"
              onClick={() => {
                if (
                  selectedDevice &&
                  !selectedDevice.id &&
                  selectedRoom &&
                  selectedRoom.id
                ) {
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
            itemToString={(item) => {
              if (!item) return "";
              return item.name || "";
            }}
            selectedItem={selectedRoom || null}
            downshiftProps={{
              inputValue: roomInputRef.current || roomInput || "",
              onStateChange: (changes, downshiftState) => {
                // Prevent inputValue from being reset when selectedItem is null (during creation)
                if (
                  changes.inputValue !== undefined &&
                  changes.inputValue === "" &&
                  (isCreatingRoom || pendingRoomCreation) &&
                  roomInputRef.current &&
                  roomInputRef.current.trim()
                ) {
                  // Force the input value to stay as our ref value
                  return { ...changes, inputValue: roomInputRef.current };
                }
              },
            }}
            onInputChange={(event) => {
              // ComboBox onInputChange can pass string directly or { inputValue } object
              const inputValue =
                typeof event === "string"
                  ? event
                  : event?.inputValue !== undefined
                    ? event.inputValue
                    : event?.target?.value || "";
              const newInput = inputValue || "";

              // Update ref and state synchronously
              roomInputRef.current = newInput;
              setRoomInput(newInput);

              // Handle change immediately with new value
              handleRoomChange(newInput, null);
            }}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                // User selected from dropdown - update immediately
                roomInputRef.current = selectedItem.name || "";
                setRoomInput(selectedItem.name || "");
                handleRoomChange(selectedItem.name || "", selectedItem);
              } else {
                // User cleared selection - allow it, but keep input value if it exists
                // Only clear if input is actually empty
                if (!roomInputRef.current || !roomInputRef.current.trim()) {
                  handleRoomChange("", null);
                }
              }
            }}
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
                if (canAddRoom() && (isCreatingRoom || pendingRoomCreation)) {
                  createRoom();
                }
              }}
              data-testid="add-new-room-button"
              disabled={!canAddRoom()}
            >
              <Add size={16} />
              <FormattedMessage id="storage.add.new" defaultMessage="Add new" />
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
            itemToString={(item) => {
              if (!item) return "";
              return item.name || "";
            }}
            selectedItem={selectedDevice || null}
            downshiftProps={{
              inputValue: deviceInputRef.current || deviceInput || "",
            }}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                selectedDeviceRef.current = selectedItem;
                deviceInputRef.current = selectedItem.name || "";
                setDeviceInput(selectedItem.name || "");
                handleDeviceChange(selectedItem.name || "", selectedItem);
              } else {
                // User cleared - only clear if input is actually empty
                if (!deviceInputRef.current || !deviceInputRef.current.trim()) {
                  selectedDeviceRef.current = null;
                  handleDeviceChange("", null);
                }
              }
            }}
            onInputChange={(event) => {
              const inputValue =
                typeof event === "string"
                  ? event
                  : event?.inputValue !== undefined
                    ? event.inputValue
                    : event?.target?.value || "";
              const newInput = inputValue || "";
              deviceInputRef.current = newInput;
              setDeviceInput(newInput);
              handleDeviceChange(newInput, null);
            }}
            onKeyDown={handleDeviceKeyDown}
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
                if (
                  canAddDevice() &&
                  isCreatingDevice &&
                  selectedRoom &&
                  selectedRoom.id
                ) {
                  createDevice();
                }
              }}
              data-testid="add-new-device-button"
              disabled={!canAddDevice()}
            >
              <Add size={16} />
              <FormattedMessage id="storage.add.new" defaultMessage="Add new" />
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
            itemToString={(item) => {
              if (!item) return "";
              // Handle both existing items (with id) and new items being created (without id)
              return item.label || item.name || "";
            }}
            selectedItem={selectedShelf || null}
            downshiftProps={{
              inputValue: shelfInputRef.current || shelfInput || "",
            }}
            onInputChange={(event) => {
              const inputValue =
                typeof event === "string"
                  ? event
                  : event?.inputValue !== undefined
                    ? event.inputValue
                    : event?.target?.value || "";
              const newInput = inputValue || "";
              shelfInputRef.current = newInput;
              setShelfInput(newInput);
              handleShelfChange(newInput, null);
            }}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                selectedShelfRef.current = selectedItem;
                shelfInputRef.current =
                  selectedItem.label || selectedItem.name || "";
                setShelfInput(selectedItem.label || selectedItem.name || "");
                handleShelfChange(
                  selectedItem.label || selectedItem.name || "",
                  selectedItem,
                );
              } else {
                if (!shelfInputRef.current || !shelfInputRef.current.trim()) {
                  selectedShelfRef.current = null;
                  handleShelfChange("", null);
                }
              }
            }}
            disabled={(() => {
              const isDisabled =
                !selectedDevice || (!selectedDevice.id && !isCreatingDevice);
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
                if (
                  canAddShelf() &&
                  isCreatingShelf &&
                  selectedDevice &&
                  selectedDevice.id
                ) {
                  createShelf();
                }
              }}
              data-testid="add-new-shelf-button"
              disabled={!canAddShelf()}
            >
              <Add size={16} />
              <FormattedMessage id="storage.add.new" defaultMessage="Add new" />
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
            itemToString={(item) => {
              if (!item) return "";
              // Handle both existing items (with id) and new items being created (without id)
              return item.label || item.name || "";
            }}
            selectedItem={selectedRack || null}
            downshiftProps={{
              inputValue: rackInputRef.current || rackInput || "",
            }}
            onInputChange={(event) => {
              const inputValue =
                typeof event === "string"
                  ? event
                  : event?.inputValue !== undefined
                    ? event.inputValue
                    : event?.target?.value || "";
              const newInput = inputValue || "";
              rackInputRef.current = newInput;
              setRackInput(newInput);
              handleRackChange(newInput, null);
            }}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                selectedRackRef.current = selectedItem;
                rackInputRef.current =
                  selectedItem.label || selectedItem.name || "";
                setRackInput(selectedItem.label || selectedItem.name || "");
                handleRackChange(
                  selectedItem.label || selectedItem.name || "",
                  selectedItem,
                );
              } else {
                if (!rackInputRef.current || !rackInputRef.current.trim()) {
                  selectedRackRef.current = null;
                  handleRackChange("", null);
                }
              }
            }}
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
                if (
                  canAddRack() &&
                  isCreatingRack &&
                  selectedShelf &&
                  selectedShelf.id
                ) {
                  createRack();
                }
              }}
              data-testid="add-new-rack-button"
              disabled={!canAddRack()}
            >
              <Add size={16} />
              <FormattedMessage id="storage.add.new" defaultMessage="Add new" />
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
                  <FormattedMessage
                    id="label.optional"
                    defaultMessage="optional"
                  />
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
