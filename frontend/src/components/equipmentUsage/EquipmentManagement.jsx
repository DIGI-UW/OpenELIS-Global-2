import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Grid,
  Column,
  Loading,
  Stack,
  OverflowMenu,
  OverflowMenuItem,
  Modal,
  Search,
  Dropdown,
  Pagination,
  Tile,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import EquipmentModal from "./modals/EquipmentModal";
import { EquipmentAPI } from "./EquipmentUsageService";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

const EquipmentManagement = () => {
  const intl = useIntl();
  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [equipmentToDeactivate, setEquipmentToDeactivate] = useState(null);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Filter and search state
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [departmentFilter, setDepartmentFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Metrics state
  const [metrics, setMetrics] = useState({
    totalEquipment: 0,
    activeEquipment: 0,
    inactiveEquipment: 0,
    needsCalibration: 0,
  });

  useEffect(() => {
    loadEquipment();
  }, [statusFilter, departmentFilter]);

  const calculateMetrics = (data) => {
    const total = data.length;
    const active = data.filter((e) => e.isActive === "Y").length;
    const inactive = data.filter((e) => e.isActive === "N").length;

    // Check if equipment needs calibration (next calibration due date has passed)
    const today = new Date();
    const needsCalib = data.filter((e) => {
      if (e.nextCalibrationDue) {
        const dueDate = new Date(e.nextCalibrationDue);
        return dueDate < today;
      }
      return false;
    }).length;

    setMetrics({
      totalEquipment: total,
      activeEquipment: active,
      inactiveEquipment: inactive,
      needsCalibration: needsCalib,
    });
  };

  const loadEquipment = async () => {
    setLoading(true);
    try {
      const data = await EquipmentAPI.getAll();
      const transformedData = Array.isArray(data)
        ? data.map((item) => ({
            id: String(item.id),
            name: item.name || "—",
            serialNumber: item.serialNumber || "—",
            department: item.department || "—",
            manufacturer: item.manufacturer || "—",
            modelNumber: item.modelNumber || "—",
            isActive: item.isActive || "N",
            nextCalibrationDue: item.nextCalibrationDue,
            // Keep original item for actions but don't spread other complex fields
            _original: item,
          }))
        : [];
      setEquipmentList(transformedData);
      calculateMetrics(transformedData);
      setPage(1); // Reset to first page when loading
    } catch (err) {
      console.error("Error loading equipment:", err);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Error",
        }),
        message:
          err.message ||
          intl.formatMessage({
            id: "equipment.load.error",
            defaultMessage: "Failed to load equipment",
          }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    } finally {
      setLoading(false);
    }
  };

  // Filter equipment based on search and filter criteria
  const getFilteredEquipment = () => {
    let filtered = equipmentList;

    // Apply status filter
    if (statusFilter !== "ALL") {
      filtered = filtered.filter((item) => item.isActive === statusFilter);
    }

    // Apply department filter
    if (departmentFilter !== "ALL") {
      filtered = filtered.filter(
        (item) => item.department === departmentFilter,
      );
    }

    // Apply search term
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (item) =>
          item.name?.toLowerCase().includes(searchLower) ||
          item.serialNumber?.toLowerCase().includes(searchLower) ||
          item.manufacturer?.toLowerCase().includes(searchLower),
      );
    }

    return filtered;
  };

  const filteredEquipment = getFilteredEquipment();
  const paginatedEquipment = filteredEquipment.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  // Get unique departments for filter dropdown
  const departments = [
    {
      id: "ALL",
      text: intl.formatMessage({
        id: "inventory.filter.all",
        defaultMessage: "All",
      }),
    },
    ...Array.from(
      new Map(
        equipmentList
          .filter((item) => item.department && item.department !== "—")
          .map((item) => [
            item.department,
            { id: item.department, text: item.department },
          ]),
      ).values(),
    ),
  ];

  const handleNewEquipment = () => {
    setSelectedEquipment(null);
    setIsNew(true);
    setIsModalOpen(true);
  };

  const handleEditEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setIsNew(false);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedEquipment(null);
  };

  const handleEquipmentSaved = async () => {
    handleModalClose();
    await loadEquipment();
  };

  const handleDeleteEquipment = (equipment) => {
    setEquipmentToDeactivate(equipment);
    setIsConfirmModalOpen(true);
  };

  const confirmDeactivation = async () => {
    if (equipmentToDeactivate) {
      try {
        const action =
          equipmentToDeactivate.isActive === "Y" ? "deactivate" : "activate";

        if (equipmentToDeactivate.isActive === "Y") {
          await EquipmentAPI.deactivate(equipmentToDeactivate.id);
        } else {
          await EquipmentAPI.activate(equipmentToDeactivate.id);
        }

        // Show success notification
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id:
              action === "deactivate"
                ? "equipment.deactivate.success"
                : "equipment.activate.success",
            defaultMessage:
              action === "deactivate"
                ? "Equipment deactivated successfully"
                : "Equipment activated successfully",
          }),
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);

        // Close modal immediately
        setIsConfirmModalOpen(false);
        setEquipmentToDeactivate(null);

        // Reload equipment data to reflect the change
        await loadEquipment();
      } catch (err) {
        const action =
          equipmentToDeactivate.isActive === "Y" ? "deactivate" : "activate";
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message:
            intl.formatMessage({
              id: `equipment.${action}.error`,
              defaultMessage: `Failed to ${action} equipment`,
            }) + `: ${err.message}`,
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        setIsConfirmModalOpen(false);
        setEquipmentToDeactivate(null);
      }
    }
  };

  const cancelDeactivation = () => {
    setIsConfirmModalOpen(false);
    setEquipmentToDeactivate(null);
  };

  if (loading && equipmentList.length === 0) {
    return <Loading />;
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}

      {/* Metrics Tiles */}
      <Grid
        className="equipment-metrics-grid"
        fullWidth={false}
        style={{ marginBottom: "2rem" }}
      >
        <Column lg={4} md={2} sm={4}>
          <Tile className="equipment-metric-tile">
            <div className="metric-value">{metrics.totalEquipment}</div>
            <div className="metric-label">Total Equipment</div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="equipment-metric-tile">
            <div className="metric-value">{metrics.activeEquipment}</div>
            <div className="metric-label">Active</div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="equipment-metric-tile">
            <div className="metric-value">{metrics.inactiveEquipment}</div>
            <div className="metric-label">Inactive</div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="equipment-metric-tile metric-warning">
            <div className="metric-value">{metrics.needsCalibration}</div>
            <div className="metric-label">Needs Calibration</div>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons and Filters */}
      <Grid fullWidth={true} style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Button kind="primary" renderIcon={Add} onClick={handleNewEquipment}>
            <FormattedMessage
              id="equipment.management.button.add"
              defaultMessage="Add Equipment"
            />
          </Button>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Search
            size="lg"
            placeholder="Search by name, serial number, or manufacturer..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onClear={() => setSearchTerm("")}
          />
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Stack orientation="horizontal" gap={2}>
            <Dropdown
              id="status-filter"
              items={[
                { id: "ALL", text: "All Status" },
                { id: "Y", text: "Active" },
                { id: "N", text: "Inactive" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                statusFilter === "ALL"
                  ? { id: "ALL", text: "All Status" }
                  : statusFilter === "Y"
                    ? { id: "Y", text: "Active" }
                    : { id: "N", text: "Inactive" }
              }
              onChange={(e) => setStatusFilter(e.selectedItem.id)}
              label="Status Filter"
            />
            <Dropdown
              id="department-filter"
              items={departments}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                departments.find((d) => d.id === departmentFilter) ||
                departments[0]
              }
              onChange={(e) => setDepartmentFilter(e.selectedItem.id)}
              label="Department Filter"
            />
          </Stack>
        </Column>
      </Grid>

      <DataTable
        rows={paginatedEquipment}
        headers={[
          { key: "name", header: "Equipment Name" },
          { key: "serialNumber", header: "Serial Number" },
          { key: "department", header: "Department" },
          { key: "manufacturer", header: "Manufacturer" },
          { key: "modelNumber", header: "Model" },
          { key: "isActive", header: "Status" },
          { key: "actions", header: "Actions" },
        ]}
      >
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getTableContainerProps,
        }) => (
          <TableContainer {...getTableContainerProps()}>
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
                {rows.map((row, rowIndex) => {
                  const equipment = paginatedEquipment[rowIndex];
                  const originalEquipment = equipment?._original;
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "Status") {
                          return (
                            <TableCell key={cell.id}>
                              {row.original.isActive === "Y"
                                ? "Active"
                                : "Inactive"}
                            </TableCell>
                          );
                        }

                        if (cell.info.header === "actions") {
                          return (
                            <TableCell key={cell.id}>
                              <OverflowMenu
                                flipped
                                ariaLabel="Equipment actions"
                              >
                                <OverflowMenuItem
                                  itemText="Edit"
                                  onClick={() =>
                                    handleEditEquipment(originalEquipment)
                                  }
                                />
                                <OverflowMenuItem
                                  itemText={
                                    originalEquipment?.isActive === "Y"
                                      ? "Deactivate"
                                      : "Activate"
                                  }
                                  onClick={() =>
                                    handleDeleteEquipment(originalEquipment)
                                  }
                                  isDelete={originalEquipment?.isActive === "Y"}
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }

                        if (
                          typeof cell.value === "object" &&
                          cell.value !== null
                        ) {
                          return <TableCell key={cell.id}>—</TableCell>;
                        }

                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>

            {!loading && paginatedEquipment.length > 0 && (
              <Pagination
                backwardText="Previous page"
                forwardText="Next page"
                itemsPerPageText="Items per page:"
                page={page}
                pageSize={pageSize}
                pageSizes={[10, 20, 30, 40, 50]}
                totalItems={filteredEquipment.length}
                onChange={({ page, pageSize }) => {
                  setPage(page);
                  setPageSize(pageSize);
                }}
              />
            )}
          </TableContainer>
        )}
      </DataTable>

      <EquipmentModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        equipment={selectedEquipment}
        isNew={isNew}
        onSubmit={handleEquipmentSaved}
      />

      <Modal
        open={isConfirmModalOpen}
        modalHeading={
          equipmentToDeactivate?.isActive === "Y"
            ? "Deactivate Equipment"
            : "Activate Equipment"
        }
        primaryButtonText={
          equipmentToDeactivate?.isActive === "Y" ? "Deactivate" : "Activate"
        }
        secondaryButtonText="Cancel"
        onRequestSubmit={confirmDeactivation}
        onRequestClose={cancelDeactivation}
        danger={equipmentToDeactivate?.isActive === "Y"}
        size="sm"
      >
        <p>
          {equipmentToDeactivate?.isActive === "Y" ? (
            <>
              Are you sure you want to deactivate{" "}
              <strong>{equipmentToDeactivate?.name}</strong>?
              <br />
              <br />
              This equipment will no longer be available for new usage entries.
            </>
          ) : (
            <>
              Are you sure you want to activate{" "}
              <strong>{equipmentToDeactivate?.name}</strong>?
              <br />
              <br />
              This equipment will be available for new usage entries.
            </>
          )}
        </p>
      </Modal>
    </>
  );
};

export default EquipmentManagement;
