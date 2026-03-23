import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  TextArea,
  Dropdown,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  getFromOpenElisServerV2,
} from "../utils/Utils";
import "./StorageLocationModal.css";

/**
 * Shared modal for creating and editing storage location entities (Room, Device, Shelf, Rack)
 * Supports both create and edit modes with dynamic field rendering based on entity type
 * Includes device connectivity fields (IP Address, Port, Communication Protocol) for devices
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - locationType: string - "room" | "device" | "shelf" | "rack"
 * - mode: string - "create" | "edit"
 * - location: object - Location entity data (required for edit mode)
 * - parentRoom: object - Parent room (required for device create/edit)
 * - parentDevice: object - Parent device (required for shelf create/edit)
 * - parentShelf: object - Parent shelf (required for rack create/edit)
 * - onClose: function - Callback when modal closes
 * - onSave: function - Callback when save is successful with location data
 */
const StorageLocationModal = ({
  open,
  locationType,
  mode = "create",
  location = null,
  parentRoom = null,
  parentDevice = null,
  parentShelf = null,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    name: "",
    code: "",
    label: "",
    description: "",
    active: true,
    type: "",
    temperatureSetting: "",
    capacityLimit: "",
    rows: "",
    columns: "",
    positionSchemaHint: "",
    ipAddress: "",
    port: "",
    communicationProtocol: "BACnet",
  });
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [availableRooms, setAvailableRooms] = useState([]);
  const [availableDevices, setAvailableDevices] = useState([]);
  const [availableShelves, setAvailableShelves] = useState([]);
  const [selectedParentRoomId, setSelectedParentRoomId] = useState(null);
  const [selectedParentDeviceId, setSelectedParentDeviceId] = useState(null);
  const [selectedParentShelfId, setSelectedParentShelfId] = useState(null);

  // Helper to get plural form for API endpoints
  const getPluralType = (type) => {
    const pluralMap = {
      room: "rooms",
      device: "devices",
      shelf: "shelves",
      rack: "racks",
    };
    return pluralMap[type] || `${type}s`;
  };

  // Initialize form data when modal opens or location changes
  useEffect(() => {
    if (open) {
      if (mode === "edit" && location) {
        // Edit mode: populate from location
        setFormData({
          name: location.name || "",
          code: location.code || "",
          label: location.label || "",
          description: location.description || "",
          active: location.active !== false,
          type: location.type || "",
          temperatureSetting: location.temperatureSetting || "",
          capacityLimit: location.capacityLimit || "",
          rows: location.rows || "",
          columns: location.columns || "",
          positionSchemaHint: location.positionSchemaHint || "",
          ipAddress: location.ipAddress || "",
          port: location.port || "",
          communicationProtocol: location.communicationProtocol || "BACnet",
        });
      } else {
        // Create mode: initialize with defaults
        setFormData({
          name: "",
          code: "",
          label: "",
          description: "",
          active: true,
          type: "",
          temperatureSetting: "",
          capacityLimit: "",
          rows: "",
          columns: "",
          positionSchemaHint: "",
          ipAddress: "",
          port: "",
          communicationProtocol: "BACnet",
        });
      }
      setErrors({});
      setSubmitError(null);
    }
  }, [open, mode, location]);

  // Load parent options when modal opens
  useEffect(() => {
    if (!open) return;

    let isMounted = true;

    // Load rooms for device parent selection
    if (locationType === "device") {
      const promise = getFromOpenElisServerV2("/rest/storage/rooms");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeRooms = response.filter(
                (room) => room.active !== false,
              );
              setAvailableRooms(activeRooms);
              // Initialize selected parent from prop or first room
              if (parentRoom) {
                setSelectedParentRoomId(String(parentRoom.id));
              } else if (activeRooms.length > 0) {
                setSelectedParentRoomId(String(activeRooms[0].id));
              }
            }
          })
          .catch(() => {
            // Silently fail - parent options are optional
          });
      }
    }
    // Load devices for shelf parent selection
    if (locationType === "shelf") {
      const promise = getFromOpenElisServerV2("/rest/storage/devices");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeDevices = response.filter(
                (device) => device.active !== false,
              );
              setAvailableDevices(activeDevices);
              // Initialize selected parent from prop or first device
              if (parentDevice) {
                setSelectedParentDeviceId(String(parentDevice.id));
              } else if (activeDevices.length > 0) {
                setSelectedParentDeviceId(String(activeDevices[0].id));
              }
            }
          })
          .catch(() => {
            // Silently fail - parent options are optional
          });
      }
    }
    // Load shelves for rack parent selection
    if (locationType === "rack") {
      const promise = getFromOpenElisServerV2("/rest/storage/shelves");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeShelves = response.filter(
                (shelf) => shelf.active !== false,
              );
              setAvailableShelves(activeShelves);
              // Initialize selected parent from prop or first shelf
              if (parentShelf) {
                setSelectedParentShelfId(String(parentShelf.id));
              } else if (activeShelves.length > 0) {
                setSelectedParentShelfId(String(activeShelves[0].id));
              }
            }
          })
          .catch(() => {
            // Silently fail - parent options are optional
          });
      }
    }

    return () => {
      isMounted = false;
    };
  }, [open, locationType, parentRoom, parentDevice, parentShelf]);

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setFormData({});
      setErrors({});
      setSubmitError(null);
      setIsSubmitting(false);
      setAvailableRooms([]);
      setAvailableDevices([]);
      setAvailableShelves([]);
      setSelectedParentRoomId(null);
      setSelectedParentDeviceId(null);
      setSelectedParentShelfId(null);
    }
  }, [open]);

  // IP address validation (IPv4 or IPv6)
  const validateIPAddress = (ip) => {
    if (!ip || ip.trim() === "") return true; // Optional field
    const ipv4Regex =
      /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    const ipv6Regex = /^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;
    return ipv4Regex.test(ip) || ipv6Regex.test(ip);
  };

  // Port validation (1-65535)
  const validatePort = (port) => {
    if (!port || port === "") return true; // Optional field
    const portNum = parseInt(port, 10);
    return !isNaN(portNum) && portNum >= 1 && portNum <= 65535;
  };

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear field-specific error
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
    setSubmitError(null);
  };

  const validateForm = () => {
    const newErrors = {};

    // Required field validation
    if (locationType === "room" || locationType === "device") {
      if (!formData.name || formData.name.trim() === "") {
        newErrors.name = intl.formatMessage({
          id: "validation.required",
          defaultMessage: "This field is required",
        });
      }
    }
    if (locationType === "shelf" || locationType === "rack") {
      if (!formData.label || formData.label.trim() === "") {
        newErrors.label = intl.formatMessage({
          id: "validation.required",
          defaultMessage: "This field is required",
        });
      }
    }

    // Device-specific validations
    if (locationType === "device") {
      if (formData.ipAddress && !validateIPAddress(formData.ipAddress)) {
        newErrors.ipAddress = intl.formatMessage({
          id: "validation.invalid.ip",
          defaultMessage: "Invalid IP address format (IPv4 or IPv6)",
        });
      }
      if (formData.port && !validatePort(formData.port)) {
        newErrors.port = intl.formatMessage({
          id: "validation.invalid.port",
          defaultMessage: "Port must be between 1 and 65535",
        });
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = async () => {
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const endpoint =
        mode === "edit"
          ? `/rest/storage/${getPluralType(locationType)}/${location.id}`
          : `/rest/storage/${getPluralType(locationType)}`;

      // Build payload based on location type
      const payload = {};
      if (locationType === "room") {
        payload.name = formData.name.trim();
        payload.code = formData.code?.trim() || null;
        payload.description = formData.description?.trim() || null;
        payload.active = formData.active;
      } else if (locationType === "device") {
        payload.name = formData.name.trim();
        payload.code = formData.code?.trim() || null;
        payload.type = formData.type || "other";
        payload.temperatureSetting = formData.temperatureSetting
          ? parseFloat(formData.temperatureSetting)
          : null;
        payload.capacityLimit = formData.capacityLimit
          ? parseInt(formData.capacityLimit, 10)
          : null;
        payload.active = formData.active;
        payload.ipAddress = formData.ipAddress?.trim() || null;
        payload.port = formData.port ? parseInt(formData.port, 10) : null;
        payload.communicationProtocol =
          formData.communicationProtocol?.trim() || "BACnet";
        if (mode === "create" && selectedParentRoomId) {
          payload.parentRoomId = selectedParentRoomId;
        }
      } else if (locationType === "shelf") {
        payload.label = formData.label.trim();
        payload.code = formData.code?.trim() || null;
        payload.capacityLimit = formData.capacityLimit
          ? parseInt(formData.capacityLimit, 10)
          : null;
        payload.active = formData.active;
        if (mode === "create" && selectedParentDeviceId) {
          payload.parentDeviceId = selectedParentDeviceId;
        }
      } else if (locationType === "rack") {
        payload.label = formData.label.trim();
        payload.code = formData.code?.trim() || null;
        payload.active = formData.active;
        if (mode === "create" && selectedParentShelfId) {
          payload.parentShelfId = selectedParentShelfId;
        }
      }

      if (mode === "create") {
        postToOpenElisServerJsonResponse(
          endpoint,
          JSON.stringify(payload),
          (json) => {
            setIsSubmitting(false);

            // postToOpenElisServerJsonResponse returns an error-shaped JSON with status/statusCode on failure
            const status = json?.status ?? json?.statusCode;
            if (status === 409) {
              const errorMessage =
                json?.error ||
                json?.message ||
                intl.formatMessage(
                  {
                    id: "storage.error.name.duplicate",
                    defaultMessage:
                      "A {entityType} with this name already exists in the selected parent location",
                  },
                  { entityType: locationType },
                );
              setSubmitError(errorMessage);
              return;
            }

            if (status && status >= 400) {
              setSubmitError(
                json?.message ||
                  json?.error ||
                  intl.formatMessage({
                    id: "storage.error.save",
                    defaultMessage: "Failed to save location",
                  }),
              );
              return;
            }

            if (onSave) {
              onSave(json);
            }
            handleClose();
          },
        );
      } else {
        // PUT request for edit
        putToOpenElisServer(
          endpoint,
          JSON.stringify(payload),
          (status) => {
            setIsSubmitting(false);
            if (status >= 200 && status < 300) {
              // Success - fetch updated location
              fetch(`${window.location.origin}${endpoint}`)
                .then((res) => res.json())
                .then((data) => {
                  if (onSave) {
                    onSave(data);
                  }
                  handleClose();
                })
                .catch(() => {
                  // Even if fetch fails, consider update successful if status is OK
                  if (onSave) {
                    onSave(payload);
                  }
                  handleClose();
                });
            } else {
              setSubmitError(
                intl.formatMessage({
                  id: "storage.error.save",
                  defaultMessage: "Failed to save location",
                }),
              );
            }
          },
          (error) => {
            setIsSubmitting(false);
            setSubmitError(
              error?.message ||
                intl.formatMessage({
                  id: "storage.error.save",
                  defaultMessage: "Failed to save location",
                }),
            );
          },
        );
      }
    } catch (error) {
      setIsSubmitting(false);
      setSubmitError(
        error.message ||
          intl.formatMessage({
            id: "storage.error.save",
            defaultMessage: "Failed to save location",
          }),
      );
    }
  };

  const handleClose = () => {
    setFormData({});
    setErrors({});
    setSubmitError(null);
    setIsSubmitting(false);
    onClose();
  };

  const deviceTypes = [
    { id: "freezer", label: "Freezer" },
    { id: "refrigerator", label: "Refrigerator" },
    { id: "cabinet", label: "Cabinet" },
    { id: "other", label: "Other" },
  ];

  const getModalTitle = () => {
    const action = mode === "create" ? "add" : "edit";
    const typeKey = `storage.${action}.${locationType}`;
    return intl.formatMessage(
      {
        id: typeKey,
        defaultMessage: `${mode === "create" ? "Add" : "Edit"} ${locationType}`,
      },
      { type: locationType },
    );
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="md"
      data-testid="storage-location-modal"
    >
      <ModalHeader title={getModalTitle()} />
      <ModalBody>
        {submitError && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "storage.error",
              defaultMessage: "Error",
            })}
            subtitle={submitError}
            lowContrast
            onClose={() => setSubmitError(null)}
            data-testid="storage-location-error"
          />
        )}

        <div className="storage-location-form">
          {/* Room fields */}
          {locationType === "room" && (
            <>
              <TextInput
                id="room-name"
                labelText={intl.formatMessage({
                  id: "storage.location.name",
                  defaultMessage: "Name",
                })}
                value={formData.name || ""}
                onChange={(e) => handleFieldChange("name", e.target.value)}
                invalid={!!errors.name}
                invalidText={errors.name}
                required
              />
              <TextInput
                id="room-code"
                labelText={intl.formatMessage({
                  id: "storage.location.code",
                  defaultMessage: "Code",
                })}
                value={formData.code || ""}
                onChange={(e) => {
                  const value = e.target.value.toUpperCase().slice(0, 10);
                  handleFieldChange("code", value);
                }}
                maxLength={10}
                helperText={intl.formatMessage({
                  id: "storage.location.code.helper",
                  defaultMessage:
                    "Max 10 characters, alphanumeric with hyphens/underscores",
                })}
              />
              <TextArea
                id="room-description"
                labelText={intl.formatMessage({
                  id: "storage.location.description",
                  defaultMessage: "Description",
                })}
                value={formData.description || ""}
                onChange={(e) =>
                  handleFieldChange("description", e.target.value)
                }
                rows={3}
              />
              <Toggle
                id="room-active"
                labelText={intl.formatMessage({
                  id: "storage.location.active",
                  defaultMessage: "Active",
                })}
                toggled={formData.active === true}
                onToggle={(checked) => handleFieldChange("active", checked)}
              />
            </>
          )}

          {/* Device fields */}
          {locationType === "device" && (
            <>
              <TextInput
                id="device-name"
                labelText={intl.formatMessage({
                  id: "storage.location.name",
                  defaultMessage: "Name",
                })}
                value={formData.name || ""}
                onChange={(e) => handleFieldChange("name", e.target.value)}
                invalid={!!errors.name}
                invalidText={errors.name}
                required
              />
              <TextInput
                id="device-code"
                labelText={intl.formatMessage({
                  id: "storage.location.code",
                  defaultMessage: "Code",
                })}
                value={formData.code || ""}
                onChange={(e) => {
                  const value = e.target.value.toUpperCase().slice(0, 10);
                  handleFieldChange("code", value);
                }}
                maxLength={10}
                helperText={intl.formatMessage({
                  id: "storage.location.code.helper",
                  defaultMessage:
                    "Max 10 characters, alphanumeric with hyphens/underscores",
                })}
              />
              {mode === "create" && (
                <Dropdown
                  id="device-parent-room"
                  titleText={intl.formatMessage({
                    id: "storage.device.room",
                    defaultMessage: "Room",
                  })}
                  label={intl.formatMessage({
                    id: "storage.device.room",
                    defaultMessage: "Room",
                  })}
                  items={availableRooms}
                  selectedItem={
                    availableRooms.find(
                      (r) => String(r.id) === selectedParentRoomId,
                    ) || null
                  }
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      setSelectedParentRoomId(String(selectedItem.id));
                    }
                  }}
                  itemToString={(item) => (item ? item.name : "")}
                  required
                />
              )}
              <Dropdown
                id="device-type"
                titleText={intl.formatMessage({
                  id: "storage.device.type",
                  defaultMessage: "Type",
                })}
                label={intl.formatMessage({
                  id: "storage.device.type",
                  defaultMessage: "Type",
                })}
                items={deviceTypes}
                itemToString={(item) => (item ? item.label : "")}
                onChange={({ selectedItem }) =>
                  handleFieldChange("type", selectedItem ? selectedItem.id : "")
                }
                selectedItem={
                  deviceTypes.find((t) => t.id === formData.type) || null
                }
              />
              <TextInput
                id="device-temperature"
                labelText={intl.formatMessage({
                  id: "storage.expanded.temperatureSetting",
                  defaultMessage: "Temperature Setting",
                })}
                value={formData.temperatureSetting || ""}
                onChange={(e) =>
                  handleFieldChange("temperatureSetting", e.target.value)
                }
                type="number"
              />
              <TextInput
                id="device-capacity"
                labelText={intl.formatMessage({
                  id: "storage.expanded.capacityLimit",
                  defaultMessage: "Capacity Limit",
                })}
                value={formData.capacityLimit || ""}
                onChange={(e) =>
                  handleFieldChange("capacityLimit", e.target.value)
                }
                type="number"
              />
              {/* Device connectivity fields */}
              <TextInput
                id="device-ip-address"
                labelText={intl.formatMessage({
                  id: "storage.device.ipAddress",
                  defaultMessage: "IP Address",
                })}
                value={formData.ipAddress || ""}
                onChange={(e) => handleFieldChange("ipAddress", e.target.value)}
                invalid={!!errors.ipAddress}
                invalidText={errors.ipAddress}
                helperText={intl.formatMessage({
                  id: "storage.device.ipAddress.helper",
                  defaultMessage:
                    "IPv4 or IPv6 address for network-connected equipment",
                })}
              />
              <TextInput
                id="device-port"
                labelText={intl.formatMessage({
                  id: "storage.device.port",
                  defaultMessage: "Port",
                })}
                value={formData.port || ""}
                onChange={(e) => handleFieldChange("port", e.target.value)}
                invalid={!!errors.port}
                invalidText={errors.port}
                type="number"
                min="1"
                max="65535"
                helperText={intl.formatMessage({
                  id: "storage.device.port.helper",
                  defaultMessage: "Port number (1-65535) for communication",
                })}
              />
              <TextInput
                id="device-communication-protocol"
                labelText={intl.formatMessage({
                  id: "storage.device.communicationProtocol",
                  defaultMessage: "Communication Protocol",
                })}
                value={formData.communicationProtocol || "BACnet"}
                onChange={(e) =>
                  handleFieldChange("communicationProtocol", e.target.value)
                }
                helperText={intl.formatMessage({
                  id: "storage.device.communicationProtocol.helper",
                  defaultMessage:
                    "Protocol used for device communication (e.g., BACnet)",
                })}
              />
              <Toggle
                id="device-active"
                labelText={intl.formatMessage({
                  id: "storage.location.active",
                  defaultMessage: "Active",
                })}
                toggled={formData.active === true}
                onToggle={(checked) => handleFieldChange("active", checked)}
              />
            </>
          )}

          {/* Shelf fields */}
          {locationType === "shelf" && (
            <>
              <TextInput
                id="shelf-label"
                labelText={intl.formatMessage({
                  id: "storage.shelf.label",
                  defaultMessage: "Label",
                })}
                value={formData.label || ""}
                onChange={(e) => handleFieldChange("label", e.target.value)}
                invalid={!!errors.label}
                invalidText={errors.label}
                required
              />
              {mode === "create" && (
                <Dropdown
                  id="shelf-parent-device"
                  titleText={intl.formatMessage({
                    id: "storage.shelf.device",
                    defaultMessage: "Device",
                  })}
                  label={intl.formatMessage({
                    id: "storage.shelf.device",
                    defaultMessage: "Device",
                  })}
                  items={availableDevices}
                  selectedItem={
                    availableDevices.find(
                      (d) => String(d.id) === selectedParentDeviceId,
                    ) || null
                  }
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      setSelectedParentDeviceId(String(selectedItem.id));
                    }
                  }}
                  itemToString={(item) => (item ? item.name : "")}
                  required
                />
              )}
              <TextInput
                id="shelf-capacity"
                labelText={intl.formatMessage({
                  id: "storage.expanded.capacityLimit",
                  defaultMessage: "Capacity Limit",
                })}
                value={formData.capacityLimit || ""}
                onChange={(e) =>
                  handleFieldChange("capacityLimit", e.target.value)
                }
                type="number"
              />
              <TextInput
                id="shelf-code"
                labelText={intl.formatMessage({
                  id: "storage.location.code",
                  defaultMessage: "Code",
                })}
                value={formData.code || ""}
                onChange={(e) => {
                  const value = e.target.value.toUpperCase().slice(0, 10);
                  handleFieldChange("code", value);
                }}
                maxLength={10}
                helperText={intl.formatMessage({
                  id: "storage.location.code.helper",
                  defaultMessage:
                    "Max 10 characters, alphanumeric with hyphens/underscores",
                })}
              />
              <Toggle
                id="shelf-active"
                labelText={intl.formatMessage({
                  id: "storage.location.active",
                  defaultMessage: "Active",
                })}
                toggled={formData.active === true}
                onToggle={(checked) => handleFieldChange("active", checked)}
              />
            </>
          )}

          {/* Rack fields */}
          {locationType === "rack" && (
            <>
              <TextInput
                id="rack-label"
                labelText={intl.formatMessage({
                  id: "storage.rack.label",
                  defaultMessage: "Label",
                })}
                value={formData.label || ""}
                onChange={(e) => handleFieldChange("label", e.target.value)}
                invalid={!!errors.label}
                invalidText={errors.label}
                required
              />
              {mode === "create" && (
                <Dropdown
                  id="rack-parent-shelf"
                  titleText={intl.formatMessage({
                    id: "storage.rack.shelf",
                    defaultMessage: "Shelf",
                  })}
                  label={intl.formatMessage({
                    id: "storage.rack.shelf",
                    defaultMessage: "Shelf",
                  })}
                  items={availableShelves}
                  selectedItem={
                    availableShelves.find(
                      (s) => String(s.id) === selectedParentShelfId,
                    ) || null
                  }
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      setSelectedParentShelfId(String(selectedItem.id));
                    }
                  }}
                  itemToString={(item) => (item ? item.label : "")}
                  required
                />
              )}
              {/* Rack grid details live on StorageBox (Box/Plate), not on Rack */}
              <TextInput
                id="rack-code"
                labelText={intl.formatMessage({
                  id: "storage.location.code",
                  defaultMessage: "Code",
                })}
                value={formData.code || ""}
                onChange={(e) => {
                  const value = e.target.value.toUpperCase().slice(0, 10);
                  handleFieldChange("code", value);
                }}
                maxLength={10}
                helperText={intl.formatMessage({
                  id: "storage.location.code.helper",
                  defaultMessage:
                    "Max 10 characters, alphanumeric with hyphens/underscores",
                })}
              />
              <Toggle
                id="rack-active"
                labelText={intl.formatMessage({
                  id: "storage.location.active",
                  defaultMessage: "Active",
                })}
                toggled={formData.active === true}
                onToggle={(checked) => handleFieldChange("active", checked)}
              />
            </>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          disabled={isSubmitting}
          data-testid="storage-location-cancel-button"
        >
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={isSubmitting || Object.keys(errors).length > 0}
          data-testid="storage-location-save-button"
        >
          <FormattedMessage id="label.button.save" defaultMessage="Save" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default StorageLocationModal;
