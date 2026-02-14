import React, {
  useState,
  useMemo,
  useEffect,
  useContext,
  useCallback,
} from "react";
import {
  Button,
  Dropdown,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Pagination,
  Modal,
  Form,
  Stack,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  InlineNotification,
  Link,
  Toggle,
} from "@carbon/react";
import { Add, Edit, WarningAlt } from "@carbon/icons-react";
import {
  fetchCorrectiveActions,
  createCorrectiveAction,
  updateCorrectiveAction,
  completeCorrectiveAction,
  retractCorrectiveAction,
  fetchDevices,
  fetchUsers,
  fetchLocations,
  createDevice,
  createRoom,
} from "./api";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import AddDeviceModal from "./shared/AddDeviceModal";

const COLUMNS = [
  { key: "id", header: "Action ID" },
  { key: "status", header: "Status" },
  { key: "device", header: "Device" },
  { key: "summary", header: "Summary" },
  { key: "performedBy", header: "Performed By" },
  { key: "created", header: "Created" },
  { key: "updated", header: "Last Updated By" },
];

const ACTION_TYPES = [
  {
    id: "TEMPERATURE_ADJUSTMENT",
    label: "Temperature Adjustment",
    description: "Adjusting temperature settings",
  },
  {
    id: "EQUIPMENT_REPAIR",
    label: "Equipment Repair",
    description: "Repairing or replacing equipment",
  },
  {
    id: "SAMPLE_RELOCATION",
    label: "Sample Relocation",
    description: "Moving samples to another location",
  },
  {
    id: "CALIBRATION",
    label: "Calibration",
    description: "Calibrating equipment or sensors",
  },
  {
    id: "ITEM_REORDER",
    label: "Item Reorder",
    description: "Reordering inventory items",
  },
  {
    id: "MAINTENANCE",
    label: "Maintenance",
    description: "Performing maintenance tasks",
  },
  {
    id: "OTHER",
    label: "Other",
    description: "Other or custom corrective actions",
  },
];

const TIME_FILTERS = [
  { id: "last24", label: "Last 24 Hours", hours: 24 },
  { id: "last7", label: "Last 7 Days", hours: 24 * 7 },
  { id: "last14", label: "Last 14 Days", hours: 24 * 14 },
  { id: "current_month", label: "Current Month", hours: null },
  { id: "all", label: "All", hours: null },
];

function statusTag(status, isEdited = false) {
  const tag = (() => {
    switch (status) {
      case "PENDING":
        return <Tag type="red">Pending</Tag>;
      case "IN_PROGRESS":
        return <Tag type="blue">In Progress</Tag>;
      case "COMPLETED":
        return <Tag type="green">Completed</Tag>;
      case "CANCELLED":
        return <Tag type="gray">Cancelled</Tag>;
      case "RETRACTED":
        return <Tag type="magenta">Retracted</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  })();

  return (
    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
      {tag}
      {isEdited && (
        <Tag type="purple" size="sm">
          Edited
        </Tag>
      )}
    </div>
  );
}

export default function CorrectiveActions() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

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

  const normalizeArray = useCallback((payload) => {
    if (Array.isArray(payload)) {
      return payload;
    }
    if (payload && typeof payload === "object") {
      return (
        payload.items ||
        payload.data ||
        payload.results ||
        payload.list ||
        payload.rows ||
        []
      );
    }
    return [];
  }, []);

  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [devices, setDevices] = useState([]);
  const [users, setUsers] = useState([]);
  const [locations, setLocations] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [timeFilter, setTimeFilter] = useState(TIME_FILTERS[4]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isRetractModalOpen, setIsRetractModalOpen] = useState(false);
  const [isDeviceModalOpen, setIsDeviceModalOpen] = useState(false);
  const [isAddRoomModalOpen, setIsAddRoomModalOpen] = useState(false);
  const [selectedAction, setSelectedAction] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState({
    device: null,
    performedBy: "",
    actionType: null,
    summary: "",
  });

  const [editStatus, setEditStatus] = useState(null);
  const [completionNotes, setCompletionNotes] = useState("");
  const [retractionReason, setRetractionReason] = useState("");

  const [roomFormData, setRoomFormData] = useState({
    name: "",
    description: "",
    active: true,
  });

  const getCurrentUserDisplayName = useCallback(() => {
    if (userSessionDetails?.firstName && userSessionDetails?.lastName) {
      return `${userSessionDetails.firstName} ${userSessionDetails.lastName}`;
    }
    return userSessionDetails?.loginName || "";
  }, [userSessionDetails]);

  const getDateRange = useCallback((filter) => {
    const end = new Date();
    let start = new Date();

    if (filter.id === "current_month") {
      start = new Date(end.getFullYear(), end.getMonth(), 1);
    } else if (filter.hours) {
      start = new Date(end.getTime() - filter.hours * 60 * 60 * 1000);
    } else {
      return { startDate: null, endDate: null };
    }

    return {
      startDate: start.toISOString(),
      endDate: end.toISOString(),
    };
  }, []);

  useEffect(() => {
    loadCorrectiveActions();
  }, [timeFilter]);

  useEffect(() => {
    loadDevices();
    loadUsers();
    loadLocations();
  }, []);

  useEffect(() => {
    const currentUserDisplayName = getCurrentUserDisplayName();
    if (currentUserDisplayName) {
      setForm((prev) => {
        if (!prev.performedBy || prev.performedBy === "") {
          return { ...prev, performedBy: currentUserDisplayName };
        }
        return prev;
      });
    }
  }, [userSessionDetails, getCurrentUserDisplayName]);

  const loadCorrectiveActions = async () => {
    setLoading(true);
    try {
      const dateRange = getDateRange(timeFilter);
      const filters = {};

      if (dateRange.startDate && dateRange.endDate) {
        filters.startDate = dateRange.startDate;
        filters.endDate = dateRange.endDate;
      }

      const data = await fetchCorrectiveActions(filters);
      const items = normalizeArray(data);
      const normalizedActions = items.map(normalizeAction);
      setActions(normalizedActions);
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load corrective actions. Please try again.",
      });
    } finally {
      setLoading(false);
    }
  };

  const loadDevices = async () => {
    try {
      const data = await fetchDevices();
      const items = Array.isArray(data) ? data : normalizeArray(data);
      const mappedDevices = items
        .filter((item) => {
          const device = item.device || item;
          return device.active !== false;
        })
        .map((item) => {
          const device = item.device || item;
          const freezerId = (item.id || device.id)?.toString() || "";
          const name =
            device.name ||
            device.unitName ||
            item.name ||
            item.unitName ||
            device.displayName ||
            "Unnamed Device";

          return {
            id: freezerId,
            displayName: name.trim(),
          };
        });
      setDevices(mappedDevices);
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load device list. Please try again later.",
      });
    }
  };

  const loadUsers = async () => {
    try {
      const data = await fetchUsers();
      const items = normalizeArray(data);
      setUsers(items || []);
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load user list. Please try again later.",
      });
    }
  };

  const loadLocations = async () => {
    try {
      const data = await fetchLocations();
      setLocations(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to load locations:", err);
    }
  };

  const normalizeAction = (action) => {
    const parseDate = (dateValue) => {
      if (!dateValue) return new Date();

      if (dateValue instanceof Date) return dateValue;
      if (typeof dateValue === "number") {
        return new Date(dateValue * 1000);
      }

      if (typeof dateValue === "string") {
        const parsed = new Date(dateValue);
        if (!isNaN(parsed.getTime())) {
          return parsed;
        }
      }

      return new Date();
    };

    const createdDate = parseDate(action.createdAt);
    const updatedDate = action.updatedAt
      ? parseDate(action.updatedAt)
      : createdDate;

    const formatDate = (date) => {
      const dateStr = date.toLocaleDateString();
      const timeStr = date.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      });
      return `${dateStr} ${timeStr}`;
    };

    const updatedByText = action.updatedByName
      ? `${action.updatedByName} at ${formatDate(updatedDate)}`
      : formatDate(updatedDate);

    return {
      id: `CA-${String(action.id).padStart(3, "0")}`,
      rawId: action.id,
      status: action.status || "PENDING",
      isEdited: action.isEdited || false,
      device: action.freezerName || `Device ${action.freezerId || "Unknown"}`,
      summary: action.description || "",
      performedBy: action.createdByName || "Unknown",
      created: formatDate(createdDate),
      updated: updatedByText,
      freezerId: action.freezerId,
      actionType: action.actionType,
      completedAt: action.completedAt,
      completionNotes: action.completionNotes,
      retractedAt: action.retractedAt,
      retractionReason: action.retractionReason,
      rawAction: action,
    };
  };

  const performerOptions = users.map(
    (user) => user.value || user.displayName || user.id,
  );

  const deviceOptions = devices.map((device) => ({
    id: device.id,
    label: device.displayName || "Unnamed Device",
  }));

  const filteredActions = useMemo(() => {
    return actions.filter((item) => {
      if (!searchTerm) return true;
      const lc = searchTerm.toLowerCase();
      return (
        item.id.toLowerCase().includes(lc) ||
        item.device.toLowerCase().includes(lc) ||
        item.summary.toLowerCase().includes(lc)
      );
    });
  }, [actions, searchTerm]);

  const paginatedActions = filteredActions.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  const itemToString = (item) => (item ? item.label || item : "");

  const resetForm = () => {
    const currentUserDisplayName = getCurrentUserDisplayName();
    setForm({
      device: null,
      performedBy: currentUserDisplayName || "",
      actionType: null,
      summary: "",
    });
  };

  const handleOpenAddModal = () => {
    resetForm();
    setIsAddModalOpen(true);
  };

  const handleCloseAddModal = () => {
    setIsAddModalOpen(false);
    setSubmitting(false);
  };

  const handleSearch = (event) => {
    setSearchTerm(event.target.value);
    setCurrentPage(1);
  };

  const handleViewAction = (action) => {
    setSelectedAction(action);
    setEditStatus({ id: action.status, label: action.status });
    setCompletionNotes(action.completionNotes || "");
    setIsViewModalOpen(true);
  };

  const handleCloseViewModal = () => {
    setIsViewModalOpen(false);
    setSelectedAction(null);
    setEditStatus(null);
    setCompletionNotes("");
  };

  const handleOpenRetractModal = (action) => {
    setSelectedAction(action);
    setRetractionReason("");
    setIsRetractModalOpen(true);
  };

  const handleCloseRetractModal = () => {
    setIsRetractModalOpen(false);
    setSelectedAction(null);
    setRetractionReason("");
  };

  const handleUpdateStatus = async () => {
    if (!selectedAction || !editStatus) return;

    setSubmitting(true);
    try {
      const actionId = selectedAction.rawId;
      const userId = userSessionDetails?.userId || 1;

      if (editStatus.id === "COMPLETED") {
        await completeCorrectiveAction(actionId, userId, completionNotes);
      } else {
        await updateCorrectiveAction(
          actionId,
          userId,
          selectedAction.summary,
          editStatus.id,
        );
      }

      handleCloseViewModal();

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Corrective action updated successfully",
      });

      loadCorrectiveActions();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to update corrective action. Please try again.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleRetractAction = async () => {
    if (!selectedAction || !retractionReason.trim()) {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        subtitle: "Please provide a reason for retraction",
      });
      return;
    }

    setSubmitting(true);
    try {
      const actionId = selectedAction.rawId;
      const userId = userSessionDetails?.userId || 1;

      await retractCorrectiveAction(actionId, userId, retractionReason);

      handleCloseRetractModal();

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Corrective action retracted successfully",
      });

      loadCorrectiveActions();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to retract corrective action. Please try again.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateDevice = async (formData) => {
    if (!formData.roomId || formData.roomId === "") {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        subtitle: "Please select a room/location for the device",
      });
      return;
    }

    try {
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
        active: true,
      };
      delete deviceData.deviceType;

      const createdDevice = await createDevice(deviceData);

      const newDevice = {
        id: createdDevice.id?.toString() || "",
        label: formData.name,
      };
      setForm((prev) => ({ ...prev, device: newDevice }));

      setIsDeviceModalOpen(false);
      setIsAddModalOpen(true);

      notify({
        kind: NotificationKinds.success,
        title: "Device Created",
        subtitle: `Device "${formData.name}" has been created successfully`,
      });

      loadDevices();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to create device. Please try again later.",
      });
    }
  };

  const handleAddRoom = () => {
    setRoomFormData({ name: "", description: "", active: true });
    setIsDeviceModalOpen(false);
    setIsAddRoomModalOpen(true);
  };

  const handleRoomFormChange = (field, value) => {
    setRoomFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleRoomSubmit = async () => {
    try {
      await createRoom(roomFormData);
      setIsAddRoomModalOpen(false);
      setIsDeviceModalOpen(true);

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Room created successfully",
      });

      loadLocations();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: `Failed to create room: ${err.message}`,
      });
    }
  };

  const handleSubmit = async () => {
    if (!isValid) {
      return;
    }

    setSubmitting(true);

    try {
      const freezerId = form.device?.id;
      if (!freezerId) {
        throw new Error("Please select a device");
      }

      if (!form.actionType) {
        throw new Error("Please select an action type");
      }

      await createCorrectiveAction(
        parseInt(freezerId),
        form.actionType.id,
        form.summary,
        userSessionDetails?.userId || 1,
      );

      setIsAddModalOpen(false);
      resetForm();

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Corrective action created successfully",
      });

      loadCorrectiveActions();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to create corrective action. Please try again.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const isValid =
    form.device &&
    form.performedBy &&
    form.actionType &&
    form.summary.trim().length > 0;

  const rows = paginatedActions.map((action) => ({
    id: action.id,
    status: statusTag(action.status, action.isEdited),
    device: action.device,
    summary: action.summary,
    performedBy: action.performedBy,
    created: action.created,
    updated: action.updated,
    _action: action,
  }));

  return (
    <div style={{ padding: "1rem 0" }}>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading ? (
        <div>Loading corrective actions...</div>
      ) : (
        <DataTable rows={rows} headers={COLUMNS}>
          {({
            rows,
            headers,
            getHeaderProps,
            getRowProps,
            getTableProps,
            getTableContainerProps,
          }) => (
            <TableContainer
              title="Corrective Actions"
              description="Track maintenance and repair actions for cold storage devices"
              {...getTableContainerProps()}
            >
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch
                    placeholder="Search by Action ID, Device, or Summary"
                    onChange={handleSearch}
                    value={searchTerm}
                  />
                  <Dropdown
                    id="time-filter"
                    titleText=""
                    label={timeFilter.label}
                    items={TIME_FILTERS}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={timeFilter}
                    onChange={({ selectedItem }) =>
                      setTimeFilter(selectedItem || TIME_FILTERS[0])
                    }
                    size="md"
                  />
                  <Button
                    kind="primary"
                    renderIcon={Add}
                    onClick={handleOpenAddModal}
                  >
                    Add New Action
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
                    <TableHeader>Actions</TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.length === 0 && (
                    <TableRow>
                      <TableCell
                        colSpan={COLUMNS.length + 1}
                        style={{
                          textAlign: "center",
                          padding: "2rem 0",
                          color: "var(--cds-text-secondary)",
                        }}
                      >
                        No corrective actions found.
                      </TableCell>
                    </TableRow>
                  )}

                  {rows.map((row) => {
                    const action = paginatedActions.find(
                      (a) => a.id === row.id,
                    );
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                        <TableCell>
                          <div style={{ display: "flex", gap: "0.5rem" }}>
                            <Button
                              kind="ghost"
                              size="sm"
                              onClick={() => handleViewAction(action)}
                            >
                              View
                            </Button>
                            {action &&
                              action.status !== "COMPLETED" &&
                              action.status !== "RETRACTED" &&
                              action.status !== "CANCELLED" && (
                                <Button
                                  kind="danger--ghost"
                                  size="sm"
                                  renderIcon={WarningAlt}
                                  onClick={() => handleOpenRetractModal(action)}
                                >
                                  Retract
                                </Button>
                              )}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
              <Pagination
                backwardText="Previous page"
                forwardText="Next page"
                itemsPerPageText="Items per page:"
                page={currentPage}
                pageSize={pageSize}
                pageSizes={[5, 10, 20, 30, 40, 50]}
                totalItems={filteredActions.length}
                onChange={({ page, pageSize }) => {
                  setCurrentPage(page);
                  setPageSize(pageSize);
                }}
              />
            </TableContainer>
          )}
        </DataTable>
      )}

      <Modal
        open={isAddModalOpen}
        onRequestClose={handleCloseAddModal}
        onRequestSubmit={handleSubmit}
        modalHeading="Add Corrective Action"
        primaryButtonText="Add Action"
        secondaryButtonText="Cancel"
        primaryButtonDisabled={!isValid || submitting}
        size="sm"
      >
        <Form>
          <Stack gap={5}>
            {devices.length === 0 && (
              <InlineNotification
                kind="info"
                title="No Devices Available"
                subtitle="You need to create a device first before adding a corrective action."
                lowContrast
                hideCloseButton
              />
            )}

            <div>
              <Dropdown
                id="device-dropdown"
                titleText="Device *"
                label={
                  form.device ? itemToString(form.device) : "Select device"
                }
                items={deviceOptions}
                itemToString={itemToString}
                selectedItem={form.device}
                onChange={({ selectedItem }) =>
                  setForm((prev) => ({ ...prev, device: selectedItem }))
                }
                disabled={devices.length === 0}
              />
              <div style={{ marginTop: "0.5rem" }}>
                <Link
                  onClick={() => {
                    setIsAddModalOpen(false);
                    setIsDeviceModalOpen(true);
                  }}
                  style={{ cursor: "pointer", fontSize: "0.875rem" }}
                >
                  {devices.length === 0
                    ? "Create a new device"
                    : "Don't see your device? Create a new one"}
                </Link>
              </div>
            </div>

            <Dropdown
              id="performed-by-dropdown"
              titleText="Performed By *"
              label={form.performedBy}
              items={performerOptions}
              selectedItem={form.performedBy}
              onChange={({ selectedItem }) =>
                setForm((prev) => ({ ...prev, performedBy: selectedItem }))
              }
            />

            <Dropdown
              id="action-type-dropdown"
              titleText="Action Type *"
              label={
                form.actionType ? itemToString(form.actionType) : "Select type"
              }
              items={ACTION_TYPES}
              itemToString={itemToString}
              selectedItem={form.actionType}
              onChange={({ selectedItem }) =>
                setForm((prev) => ({ ...prev, actionType: selectedItem }))
              }
            />

            <TextArea
              id="action-summary"
              labelText="Description *"
              placeholder="Describe the corrective action taken, findings, and results..."
              rows={4}
              value={form.summary}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, summary: e.target.value }))
              }
              required
            />
          </Stack>
        </Form>
      </Modal>

      <Modal
        open={isAddRoomModalOpen}
        onRequestClose={() => {
          setIsAddRoomModalOpen(false);
          setIsDeviceModalOpen(true);
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

      <AddDeviceModal
        isOpen={isDeviceModalOpen}
        onClose={() => {
          setIsDeviceModalOpen(false);
          setIsAddModalOpen(true);
        }}
        onSubmit={handleCreateDevice}
        locations={locations}
        onAddRoom={handleAddRoom}
        editingDevice={null}
      />

      <Modal
        open={isViewModalOpen}
        onRequestClose={handleCloseViewModal}
        onRequestSubmit={handleUpdateStatus}
        modalHeading={`Corrective Action ${selectedAction?.id || ""}`}
        primaryButtonText="Update Status"
        secondaryButtonText="Close"
        primaryButtonDisabled={!editStatus || submitting}
        size="md"
      >
        {selectedAction && (
          <Stack gap={5}>
            <div>
              <strong>Device:</strong> {selectedAction.device}
            </div>
            <div>
              <strong>Action Type:</strong>{" "}
              {ACTION_TYPES.find((t) => t.id === selectedAction.actionType)
                ?.label || selectedAction.actionType}
            </div>
            <div>
              <strong>Summary:</strong> {selectedAction.summary}
            </div>
            <div>
              <strong>Performed By:</strong> {selectedAction.performedBy}
            </div>
            <div>
              <strong>Created:</strong> {selectedAction.created}
            </div>
            <div>
              <strong>Last Updated:</strong> {selectedAction.updated}
            </div>
            <div>
              <strong>Current Status:</strong>{" "}
              {statusTag(selectedAction.status, selectedAction.isEdited)}
            </div>

            {selectedAction.status !== "COMPLETED" &&
              selectedAction.status !== "CANCELLED" &&
              selectedAction.status !== "RETRACTED" && (
                <>
                  <Dropdown
                    id="status-dropdown"
                    titleText="Update Status"
                    label={editStatus ? editStatus.label : "Select status"}
                    items={[
                      { id: "PENDING", label: "Pending" },
                      { id: "IN_PROGRESS", label: "In Progress" },
                      { id: "COMPLETED", label: "Completed" },
                      { id: "CANCELLED", label: "Cancelled" },
                    ]}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={editStatus}
                    onChange={({ selectedItem }) => setEditStatus(selectedItem)}
                  />

                  {editStatus?.id === "COMPLETED" && (
                    <TextArea
                      id="completion-notes"
                      labelText="Completion Notes *"
                      placeholder="Describe what was done to complete this action..."
                      rows={4}
                      value={completionNotes}
                      onChange={(e) => setCompletionNotes(e.target.value)}
                      required
                    />
                  )}
                </>
              )}

            {selectedAction.completionNotes && (
              <div>
                <strong>Completion Notes:</strong>
                <div style={{ whiteSpace: "pre-wrap", marginTop: "0.5rem" }}>
                  {selectedAction.completionNotes}
                </div>
              </div>
            )}

            {selectedAction.retractionReason && (
              <div>
                <strong>Retraction Reason:</strong>
                <div style={{ whiteSpace: "pre-wrap", marginTop: "0.5rem" }}>
                  {selectedAction.retractionReason}
                </div>
              </div>
            )}
          </Stack>
        )}
      </Modal>

      <Modal
        open={isRetractModalOpen}
        onRequestClose={handleCloseRetractModal}
        onRequestSubmit={handleRetractAction}
        modalHeading={`Retract Corrective Action ${selectedAction?.id || ""}`}
        primaryButtonText="Retract Action"
        secondaryButtonText="Cancel"
        danger
        primaryButtonDisabled={!retractionReason.trim() || submitting}
        size="sm"
      >
        <Stack gap={5}>
          <InlineNotification
            kind="warning"
            title="Warning"
            subtitle="Retracting an action marks it as invalid. This action cannot be undone."
            lowContrast
            hideCloseButton
          />

          <TextArea
            id="retraction-reason"
            labelText="Retraction Reason *"
            placeholder="Provide a reason for retracting this action..."
            rows={4}
            value={retractionReason}
            onChange={(e) => setRetractionReason(e.target.value)}
            required
          />
        </Stack>
      </Modal>
    </div>
  );
}
