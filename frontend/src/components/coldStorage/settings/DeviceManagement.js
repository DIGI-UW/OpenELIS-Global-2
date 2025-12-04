import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Pagination,
  Modal,
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  Dropdown,
  Toggle,
  IconButton,
  FormLabel,
} from "@carbon/react";
import { Add, Edit, Power, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, injectIntl } from "react-intl";
import {
  fetchDevices,
  fetchLocations,
  createDevice,
  updateDevice,
  toggleDeviceStatus,
  deleteDevice,
  createRoom,
} from "../api";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import AddDeviceModal from "../shared/AddDeviceModal";

const DEVICE_COLUMNS = [
  { key: "id", header: "Device ID" },
  { key: "status", header: "Status" },
  { key: "name", header: "Name" },
  { key: "deviceType", header: "Type" },
  { key: "host", header: "IP Address" },
  { key: "port", header: "Port" },
  { key: "protocol", header: "Protocol" },
  { key: "room", header: "Room/Facility" },
  { key: "actions", header: "Actions" },
];

const DEVICE_TYPE_OPTIONS = [
  { value: "freezer", label: "Freezer" },
  { value: "refrigerator", label: "Refrigerator" },
  { value: "cabinet", label: "Cabinet" },
  { value: "other", label: "Other" },
];

const PROTOCOL_OPTIONS = [
  { value: "TCP", label: "Modbus TCP" },
  { value: "RTU", label: "Modbus RTU" },
];

const PARITY_OPTIONS = [
  { value: "NONE", label: "None" },
  { value: "EVEN", label: "Even" },
  { value: "ODD", label: "Odd" },
  { value: "MARK", label: "Mark" },
  { value: "SPACE", label: "Space" },
];

function DeviceManagement({ intl }) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );
  const [devices, setDevices] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isAddRoomModalOpen, setIsAddRoomModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deviceToDelete, setDeviceToDelete] = useState(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [deviceToToggle, setDeviceToToggle] = useState(null);
  const [newStatusValue, setNewStatusValue] = useState(false);
  const [editingDevice, setEditingDevice] = useState(null);
  const [roomFormData, setRoomFormData] = useState({
    name: "",
    description: "",
    active: true,
  });
  const [formData, setFormData] = useState({
    name: "",
    deviceType: "freezer",
    roomId: "",
    protocol: "TCP",
    host: "",
    port: 502,
    serialPort: "",
    baudRate: 9600,
    dataBits: 8,
    stopBits: 1,
    parity: "NONE",
    slaveId: 1,
    temperatureRegister: 0,
    temperatureScale: 1.0,
    temperatureOffset: 0.0,
    humidityRegister: null,
    humidityScale: 1.0,
    humidityOffset: 0.0,
  });

  const loadLocations = useCallback(async () => {
    try {
      const response = await fetchLocations();
      setLocations(response || []);
    } catch (err) {
      console.error("Failed to load locations:", err);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load locations: " + err.message,
      });
    }
  }, [notify]);

  const loadDevices = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetchDevices(searchTerm);
      setDevices(response || []);
    } catch (err) {
      setError("Failed to load devices: " + err.message);
    } finally {
      setLoading(false);
    }
  }, [searchTerm]);

  useEffect(() => {
    loadLocations();
    loadDevices();
  }, [loadLocations, loadDevices]);

  const handleSearch = (event) => {
    setSearchTerm(event.target.value);
    setCurrentPage(1);
  };

  const handleAddDevice = () => {
    setEditingDevice(null);
    setFormData({
      name: "",
      deviceType: "freezer",
      roomId: "",
      protocol: "TCP",
      host: "",
      port: 502,
      serialPort: "",
      baudRate: 9600,
      dataBits: 8,
      stopBits: 1,
      parity: "NONE",
      slaveId: 1,
      temperatureRegister: 0,
      temperatureScale: 1.0,
      temperatureOffset: 0.0,
      humidityRegister: null,
      humidityScale: 1.0,
      humidityOffset: 0.0,
    });
    setIsModalOpen(true);
  };

  const handleEditDevice = (device) => {
    setEditingDevice(device);
    setFormData({
      name: device.name || "",
      deviceType: device.storageDevice?.type?.toLowerCase() || "freezer",
      roomId: device.roomId || "",
      protocol: device.protocol || "TCP",
      host: device.host || "",
      port: device.port || 502,
      serialPort: device.serialPort || "",
      baudRate: device.baudRate || 9600,
      dataBits: device.dataBits || 8,
      stopBits: device.stopBits || 1,
      parity: device.parity || "NONE",
      slaveId: device.slaveId || 1,
      temperatureRegister: device.temperatureRegister || 0,
      temperatureScale: device.temperatureScale || 1.0,
      temperatureOffset: device.temperatureOffset || 0.0,
      humidityRegister: device.humidityRegister || null,
      humidityScale: device.humidityScale || 1.0,
      humidityOffset: device.humidityOffset || 0.0,
    });
    setIsModalOpen(true);
  };

  const handleToggleStatus = (device) => {
    setDeviceToToggle(device);
    setNewStatusValue(!device.active);
    setIsStatusModalOpen(true);
  };

  const confirmToggleStatus = async () => {
    if (!deviceToToggle) return;

    try {
      await toggleDeviceStatus(deviceToToggle.id, newStatusValue);

      // Close modal immediately after successful action
      setIsStatusModalOpen(false);
      setDeviceToToggle(null);

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: `Device "${deviceToToggle.name}" has been ${newStatusValue ? "activated" : "deactivated"} successfully`,
      });

      // Reload data in the background after modal is closed
      loadDevices();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to toggle device status: " + err.message,
      });
    }
  };

  const cancelToggleStatus = () => {
    setIsStatusModalOpen(false);
    setDeviceToToggle(null);
  };

  const handleDeleteDevice = (device) => {
    setDeviceToDelete(device);
    setIsDeleteModalOpen(true);
  };

  const confirmDeleteDevice = async () => {
    if (!deviceToDelete) return;

    try {
      await deleteDevice(deviceToDelete.id);

      // Close modal immediately after successful action
      setIsDeleteModalOpen(false);
      setDeviceToDelete(null);

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: `Device "${deviceToDelete.name}" has been deleted successfully`,
      });

      // Reload data in the background after modal is closed
      loadDevices();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to delete device: " + err.message,
      });
    }
  };

  const cancelDeleteDevice = () => {
    setIsDeleteModalOpen(false);
    setDeviceToDelete(null);
  };

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (formData) => {
    // Validate roomId
    if (!formData.roomId || formData.roomId === "") {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        subtitle: "Please select a room/location for the device",
      });
      return;
    }

    try {
      // Prepare device data with storageDevice type for auto-creation
      const deviceData = {
        ...formData,
        roomId: parseInt(formData.roomId),
        storageDevice: { type: formData.deviceType },
        port: parseInt(formData.port),
        slaveId: parseInt(formData.slaveId),
        temperatureRegister: parseInt(formData.temperatureRegister),
        humidityRegister:
          formData.humidityRegister != null
            ? parseInt(formData.humidityRegister)
            : null,
        dataBits: parseInt(formData.dataBits),
        stopBits: parseInt(formData.stopBits),
        baudRate: parseInt(formData.baudRate),
        temperatureScale: parseFloat(formData.temperatureScale),
        temperatureOffset: parseFloat(formData.temperatureOffset),
        humidityScale: parseFloat(formData.humidityScale),
        humidityOffset: parseFloat(formData.humidityOffset),
      };
      // Remove deviceType from top level (it's in storageDevice object)
      delete deviceData.deviceType;

      if (editingDevice) {
        await updateDevice(editingDevice.id, deviceData);

        // Close modal immediately after successful action
        setIsModalOpen(false);
        setEditingDevice(null);

        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle: "Device updated successfully",
        });
      } else {
        await createDevice(deviceData);

        // Close modal immediately after successful action
        setIsModalOpen(false);
        setEditingDevice(null);

        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle: "Device created successfully",
        });
      }

      // Reload data in the background after modal is closed
      loadDevices();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: `Failed to ${editingDevice ? "update" : "create"} device: ${err.message}`,
      });
    }
  };

  const handleAddRoom = () => {
    setRoomFormData({ name: "", description: "", active: true });
    setIsModalOpen(false);
    setIsAddRoomModalOpen(true);
  };

  const handleRoomFormChange = (field, value) => {
    setRoomFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleRoomSubmit = async () => {
    try {
      const response = await createRoom(roomFormData);

      // Close modal immediately after successful action
      setIsAddRoomModalOpen(false);
      setIsModalOpen(true);

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Room created successfully",
      });

      // Reload data in the background after modal is closed
      loadLocations();

      if (response && response.id) {
        setFormData((prev) => ({
          ...prev,
          roomId: response.id.toString(),
        }));
      }
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: `Failed to create room: ${err.message}`,
      });
    }
  };

  const filteredDevices = devices.filter((device) => {
    if (filterStatus === "active" && !device.active) return false;
    if (filterStatus === "inactive" && device.active) return false;
    return true;
  });

  const paginatedDevices = filteredDevices.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  const rows = paginatedDevices.map((device) => ({
    id: device.id.toString(),
    deviceId: device.id,
    status: device.active ? (
      <Tag type="green">ACTIVE</Tag>
    ) : (
      <Tag type="gray">INACTIVE</Tag>
    ),
    name: device.name,
    deviceType: device.storageDevice?.type || "—",
    host: device.host || "—",
    port: device.port || "—",
    protocol: device.protocol || "—",
    room: device.room || device.locationName || "—",
    actions: (
      <div style={{ display: "flex", gap: "0.5rem" }}>
        <IconButton
          label="Edit device"
          kind="ghost"
          size="sm"
          onClick={() => handleEditDevice(device)}
        >
          <Edit />
        </IconButton>
        <IconButton
          label={device.active ? "Deactivate device" : "Activate device"}
          kind="ghost"
          size="sm"
          onClick={() => handleToggleStatus(device)}
        >
          <Power />
        </IconButton>
        <IconButton
          label="Delete device"
          kind="ghost"
          size="sm"
          onClick={() => handleDeleteDevice(device)}
        >
          <TrashCan />
        </IconButton>
      </div>
    ),
    _device: device,
  }));

  return (
    <div style={{ padding: "1rem 0" }}>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <DataTable rows={rows} headers={DEVICE_COLUMNS}>
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getTableContainerProps,
        }) => (
          <TableContainer
            title="Configured Devices"
            {...getTableContainerProps()}
          >
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  placeholder="Search by Device ID or Name"
                  onChange={handleSearch}
                  value={searchTerm}
                />
                <Dropdown
                  id="filter-status"
                  titleText=""
                  label="All Devices"
                  items={[
                    { id: "all", label: "All Devices" },
                    { id: "active", label: "Active Only" },
                    { id: "inactive", label: "Inactive Only" },
                  ]}
                  itemToString={(item) => (item ? item.label : "")}
                  onChange={({ selectedItem }) =>
                    setFilterStatus(selectedItem?.id || "all")
                  }
                  size="md"
                />
                <Button
                  kind="primary"
                  renderIcon={Add}
                  onClick={handleAddDevice}
                >
                  Add New Device
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Pagination
              backwardText="Previous page"
              forwardText="Next page"
              itemsPerPageText="Items per page:"
              page={currentPage}
              pageSize={pageSize}
              pageSizes={[5, 10, 20, 30, 40, 50]}
              totalItems={filteredDevices.length}
              onChange={({ page, pageSize }) => {
                setCurrentPage(page);
                setPageSize(pageSize);
              }}
            />
          </TableContainer>
        )}
      </DataTable>

      <AddDeviceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        locations={locations}
        onAddRoom={handleAddRoom}
        editingDevice={editingDevice}
      />

      <Modal
        open={isAddRoomModalOpen}
        onRequestClose={() => {
          setIsAddRoomModalOpen(false);
          setIsModalOpen(true);
        }}
        onRequestSubmit={handleRoomSubmit}
        modalHeading="Add New Room"
        primaryButtonText="Create Room"
        secondaryButtonText="Cancel"
        size="sm"
        preventCloseOnClickOutside
      >
        <Stack gap={5}>
          <TextInput
            id="roomName"
            labelText="Room Name *"
            placeholder="Enter room name (e.g., Lab Storage Room A)"
            value={roomFormData.name}
            onChange={(e) => handleRoomFormChange("name", e.target.value)}
            required
          />

          <TextInput
            id="roomDescription"
            labelText="Description"
            placeholder="Enter room description (optional)"
            value={roomFormData.description}
            onChange={(e) =>
              handleRoomFormChange("description", e.target.value)
            }
          />

          <Toggle
            id="roomActive"
            labelText="Active"
            toggled={roomFormData.active}
            onToggle={(checked) => handleRoomFormChange("active", checked)}
          />
        </Stack>
      </Modal>

      <Modal
        open={isStatusModalOpen}
        onRequestClose={cancelToggleStatus}
        onRequestSubmit={confirmToggleStatus}
        modalHeading="Change Device Status"
        primaryButtonText="Confirm"
        secondaryButtonText="Cancel"
        size="sm"
      >
        <Stack gap={5}>
          <p>
            Change the status of <strong>{deviceToToggle?.name}</strong>:
          </p>
          <Toggle
            id="device-status-toggle"
            labelText="Device Status"
            labelA="Inactive"
            labelB="Active"
            toggled={newStatusValue}
            onToggle={(checked) => setNewStatusValue(checked)}
          />
          <p
            style={{ fontSize: "0.875rem", color: "var(--cds-text-secondary)" }}
          >
            {newStatusValue ? (
              <>
                Activating this device will enable monitoring and data
                collection.
              </>
            ) : (
              <>
                Deactivating this device will stop monitoring and data
                collection. Historical data will be preserved.
              </>
            )}
          </p>
        </Stack>
      </Modal>

      <Modal
        open={isDeleteModalOpen}
        onRequestClose={cancelDeleteDevice}
        onRequestSubmit={confirmDeleteDevice}
        modalHeading="Delete Device"
        primaryButtonText="Delete"
        secondaryButtonText="Cancel"
        danger
        size="sm"
      >
        <p>
          Are you sure you want to delete{" "}
          <strong>{deviceToDelete?.name}</strong>?
        </p>
        <p style={{ marginTop: "1rem", color: "var(--cds-text-secondary)" }}>
          This will mark the device as inactive and remove it from active
          monitoring. Historical data will be preserved.
        </p>
      </Modal>
    </div>
  );
}

export default injectIntl(DeviceManagement);
