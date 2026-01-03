import React, {
  useState,
  useEffect,
  useContext,
  useCallback,
  useMemo,
} from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  Button,
  Dropdown,
  Search,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Pagination,
  Grid,
  Column,
  Tile,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI, InventoryLotAPI } from "./InventoryService";
import LotEntryModal from "./LotEntryModal";
import RecordUsageModal from "./RecordUsageModal";
import LotAdjustmentModal from "./LotAdjustmentModal";
import DisposeLotModal from "./DisposeLotModal";
import UpdateQCStatusModal from "./UpdateQCStatusModal";
import LotDetailsPanel from "./LotDetailsPanel";
import AuditLogViewer from "./AuditLogViewer";
import "./InventoryList.css";

const InventoryDashboard = () => {
  const intl = useIntl();
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

  const [lots, setLots] = useState([]);
  const [items, setItems] = useState({});
  const [loading, setLoading] = useState(true);
  const [unitMap, setUnitMap] = useState({});
  const [projectMap, setProjectMap] = useState({});

  const [metrics, setMetrics] = useState({
    totalLots: 0,
    lowStock: 0,
    expiringSoon: 0,
    expired: 0,
  });

  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState("CARTRIDGE");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [totalRecords, setTotalRecords] = useState(0);

  const [lotModalOpen, setLotModalOpen] = useState(false);
  const [usageModalOpen, setUsageModalOpen] = useState(false);
  const [adjustmentModalOpen, setAdjustmentModalOpen] = useState(false);
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [qcStatusModalOpen, setQcStatusModalOpen] = useState(false);
  const [detailsPanelOpen, setDetailsPanelOpen] = useState(false);
  const [auditLogOpen, setAuditLogOpen] = useState(false);
  const [selectedLot, setSelectedLot] = useState(null);
  const [selectedItem, setSelectedItem] = useState(null);
  const [selectedLotsForDisposal, setSelectedLotsForDisposal] = useState([]);

  // Utility function to format dates safely
  const formatDate = (dateValue) => {
    if (!dateValue) return "N/A";
    try {
      const date = new Date(dateValue);
      return isNaN(date.getTime()) ? "N/A" : date.toLocaleDateString();
    } catch (error) {
      console.warn("Date formatting error:", error);
      return "N/A";
    }
  };

  const itemTypes = [
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
    { id: "REAGENT", text: "Reagent" },
    { id: "CARTRIDGE", text: "Equipment" },
    { id: "RDT", text: "RDT" },
    { id: "ENZYME", text: "Enzyme" },
    { id: "ANTIBIOTICS", text: "Antibiotics" },
  ];

  const statusOptions = [
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
    { id: "ACTIVE", text: "Active" },
    { id: "IN_USE", text: "In Use" },
    { id: "EXPIRED", text: "Expired" },
  ];

  // Equipment-specific headers (for CARTRIDGE type)
  const equipmentHeaders = [
    {
      key: "name",
      header: "Equipment Name",
    },
    {
      key: "manufacturer",
      header: "Manufacturer",
    },
    {
      key: "modelNumber",
      header: "Model",
    },
    {
      key: "serialNumber",
      header: "Serial Number",
    },
    {
      key: "compatibleAnalyzers",
      header: "Software",
    },
    {
      key: "ahriTag",
      header: "AHRI Tag",
    },
    {
      key: "installationDate",
      header: "Installation Date",
    },
    {
      key: "currentLocation",
      header: "Current Location",
    },
    {
      key: "equipmentCondition",
      header: "Equipment Condition (Functional/Non-functional)",
    },
    {
      key: "lastServiceDate",
      header: "Last Service Date",
    },
    {
      key: "lastMaintenanceDate",
      header: "Last Maintenance Date",
    },
    {
      key: "actions",
      header: "Action",
    },
  ];

  // Reagent-specific headers (for REAGENT type)
  const reagentHeaders = [
    {
      key: "name",
      header: "Reagent Name",
    },
    {
      key: "catalogNumber",
      header: "Catalogue Number",
    },
    {
      key: "manufacturer",
      header: "Reagent Manufacturer",
    },
    {
      key: "category",
      header: "Reagent Category",
    },
    {
      key: "concentration",
      header: "Concentration",
    },
    {
      key: "units",
      header: "Unit of Measurement",
    },
    {
      key: "currentQuantity",
      header: "Quantity",
    },
    {
      key: "dateReceived",
      header: "Date Received",
    },
    {
      key: "receivedBy",
      header: "Received By",
    },
    {
      key: "projectName",
      header: "Project Received For",
    },
    {
      key: "storageTemp",
      header: "Storage Temp",
    },
    {
      key: "storageLocation",
      header: "Storage Location (Freezer)",
    },
    {
      key: "storageBoxNo",
      header: "Storage Location (Box No)",
    },
    {
      key: "actions",
      header: "Action",
    },
  ];

  // Default headers for all types or when no specific filter is selected
  const defaultHeaders = [
    {
      key: "name",
      header: intl.formatMessage({ id: "catalog.item.name" }),
    },
    {
      key: "projectName",
      header: "Project",
    },
    {
      key: "lotNumber",
      header: intl.formatMessage({ id: "lot.number" }),
    },
    {
      key: "itemType",
      header: intl.formatMessage({ id: "catalog.item.type" }),
    },
    {
      key: "currentQuantity",
      header: intl.formatMessage({ id: "lot.currentQuantity" }),
    },
    {
      key: "expirationDate",
      header: intl.formatMessage({ id: "lot.expirationDate" }),
    },
    {
      key: "dateOpened",
      header: intl.formatMessage({ id: "lot.openingDate" }) || "Opening Date",
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "lot.status" }),
    },
    {
      key: "stockStatus",
      header: intl.formatMessage({ id: "stock.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.action" }),
    },
  ];

  // Select headers based on current type filter
  const headers = useMemo(() => {
    if (typeFilter === "CARTRIDGE") {
      return equipmentHeaders;
    } else if (typeFilter === "REAGENT") {
      return reagentHeaders;
    } else {
      return defaultHeaders;
    }
  }, [typeFilter, intl]);

  useEffect(() => {
    fetchUnits();
    fetchProjects();
  }, []);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (page !== 1) {
        setPage(1);
      } else {
        fetchLots();
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchTerm]);

  useEffect(() => {
    fetchLots();
  }, [typeFilter, statusFilter, page, pageSize]);

  useEffect(() => {
    if (page !== 1) {
      setPage(1);
    }
  }, [typeFilter, statusFilter]);

  const fetchUnits = async () => {
    try {
      const unitsData = await InventoryItemAPI.getUnitOptions();
      const unitLookup = {};
      unitsData.forEach((unit) => {
        unitLookup[String(unit.id)] = unit.text;
      });
      setUnitMap(unitLookup);
    } catch (error) {
      console.error("Error fetching unit options:", error);
    }
  };

  const fetchProjects = async () => {
    try {
      const { NotebookDataAPI } = await import("./InventoryService");
      const notebooks = await NotebookDataAPI.getNotebooks();
      const projectLookup = {};
      notebooks.forEach((notebook) => {
        projectLookup[notebook.id] = notebook.title;
        projectLookup[String(notebook.id)] = notebook.title;
      });
      setProjectMap(projectLookup);
    } catch (error) {
      console.error("Error fetching projects:", error);
    }
  };

  const fetchLots = async () => {
    setLoading(true);
    try {
      const offset = (page - 1) * pageSize;

      const response = await InventoryLotAPI.getPaged({
        limit: pageSize,
        offset: offset,
        sortBy: "expirationDate",
        sortOrder: "asc",
        itemType: typeFilter !== "ALL" ? typeFilter : undefined,
        status: statusFilter !== "ALL" ? statusFilter : undefined,
        search: searchTerm || undefined,
      });

      const validLots = Array.isArray(response.lots) ? response.lots : [];
      setLots(validLots);
      setTotalRecords(response.totalRecords || 0);

      const itemsMap = {};
      validLots.forEach((lot) => {
        if (lot.inventoryItem && lot.inventoryItem.id) {
          itemsMap[lot.inventoryItem.id] = lot.inventoryItem;
        }
      });
      setItems(itemsMap);

      calculateMetrics(validLots, itemsMap);
    } catch (error) {
      console.error("Error fetching inventory:", error);
      setLots([]);
      setItems({});
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: "Error loading inventory data",
      });
    } finally {
      setLoading(false);
    }
  };

  const calculateMetrics = (lotsData, itemsData) => {
    let lowStockCount = 0;
    let expiringSoonCount = 0;
    let expiredCount = 0;

    lotsData.forEach((lot) => {
      const item = itemsData[lot.inventoryItem?.id];
      if (!item) return;

      const currentQty = lot.currentQuantity || 0;
      const minStock = item.minimumStockLevel || 0;

      if (lot.expirationDate) {
        const expiryDate = new Date(lot.expirationDate);
        const today = new Date();
        const daysUntilExpiry = Math.floor(
          (expiryDate - today) / (1000 * 60 * 60 * 24),
        );

        if (daysUntilExpiry < 0) {
          expiredCount++;
          return;
        }

        const alertDays = item.expirationAlertDays || 30;
        if (daysUntilExpiry <= alertDays) {
          expiringSoonCount++;
        }
      }

      if (currentQty > 0 && currentQty <= minStock) {
        lowStockCount++;
      }
    });

    setMetrics({
      totalLots: lotsData.length,
      lowStock: lowStockCount,
      expiringSoon: expiringSoonCount,
      expired: expiredCount,
    });
  };

  const getStockStatus = (lot) => {
    if (!lot || !lot.inventoryItem) return null;

    const item = items[lot.inventoryItem.id];
    if (!item) return null;

    const currentQty = lot.currentQuantity || 0;
    const minStock = item.minimumStockLevel || 0;

    if (lot.expirationDate) {
      const expiryDate = new Date(lot.expirationDate);
      const today = new Date();
      const daysUntilExpiry = Math.floor(
        (expiryDate - today) / (1000 * 60 * 60 * 24),
      );

      if (daysUntilExpiry < 0) {
        return { type: "expired", label: "Expired", kind: "red" };
      }

      const alertDays = item.expirationAlertDays || 30;
      if (daysUntilExpiry <= alertDays) {
        return {
          type: "expiring",
          label: `Expiring (${daysUntilExpiry}d)`,
          kind: "warm-gray",
        };
      }
    }

    if (currentQty === 0) {
      return { type: "outOfStock", label: "Out of Stock", kind: "red" };
    }

    if (currentQty < minStock) {
      return { type: "lowStock", label: "Low Stock", kind: "warm-gray" };
    }

    return { type: "inStock", label: "In Stock", kind: "green" };
  };

  const rows = lots.map((lot) => {
    const item = items[lot.inventoryItem?.id];
    const stockStatus = getStockStatus(lot);

    const unitId = item?.units;
    const unitsDisplay = unitMap[unitId] || unitId || "";

    const projectName = item?.projectName;
    let projectDisplay = "N/A";
    if (projectName) {
      if (isNaN(projectName)) {
        projectDisplay = projectName;
      } else {
        projectDisplay = projectMap[projectName] || projectName;
      }
    }

    const baseData = {
      id: String(lot.id),
      name: item?.name || "Unknown",
      manufacturer: item?.manufacturer || "N/A",
      catalogNumber: item?.catalogNumber || "N/A",
      category: item?.category || "N/A",
      units: unitsDisplay,
      currentQuantity: `${lot.currentQuantity || 0}${unitsDisplay ? ` ${unitsDisplay}` : ""}`,
      lotNumber: lot.lotNumber || "N/A",
      projectName: projectDisplay,
      status: lot.status,
      stockStatus: stockStatus,
    };

    // Equipment-specific row data (for CARTRIDGE items)
    if (item?.itemType === "CARTRIDGE") {
      return {
        ...baseData,
        // Map new equipment-specific fields
        modelNumber: item?.modelNumber || "N/A",
        serialNumber: item?.serialNumber || lot.lotNumber || "N/A",
        compatibleAnalyzers: item?.compatibleAnalyzers || "N/A",
        ahriTag: item?.ahriTag || "N/A",
        installationDate: item?.installationDate
          ? formatDate(item.installationDate)
          : "N/A",
        currentLocation: lot?.storagePath || item?.currentLocation || "N/A",
        equipmentCondition: item?.equipmentCondition === "functional" ? "Functional" :
                           item?.equipmentCondition === "non-functional" ? "Non-functional" :
                           item?.equipmentCondition === "under-repair" ? "Under Repair" :
                           item?.equipmentCondition === "decommissioned" ? "Decommissioned" :
                           "Unknown",
        lastServiceDate: item?.lastServiceDate
          ? formatDate(item.lastServiceDate)
          : "N/A",
        lastMaintenanceDate: item?.lastMaintenanceDate
          ? formatDate(item.lastMaintenanceDate)
          : "N/A",
      };
    }

    // Reagent-specific row data (for REAGENT items)
    else if (item?.itemType === "REAGENT") {
      return {
        ...baseData,
        concentration: item?.concentration || "N/A",
        dateReceived: lot.receiptDate
          ? new Date(lot.receiptDate).toLocaleDateString()
          : "N/A",
        receivedBy: lot.receivedBy || "N/A",
        storageTemp: item?.storageRequirements || "N/A",
        storageLocation: lot.specificStorageLocation || "N/A",
        storageBoxNo: lot.storageBoxNumber || "N/A",
      };
    }

    // Default row data for all other types or when viewing all types
    else {
      return {
        ...baseData,
        itemType: item?.itemType || "",
        expirationDate: lot.expirationDate
          ? new Date(lot.expirationDate).toLocaleDateString()
          : "N/A",
        dateOpened: lot.dateOpened
          ? new Date(lot.dateOpened).toLocaleDateString()
          : "Not Opened",
      };
    }
  });

  const handleLotSaved = () => {
    setLotModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "lot.save.success" }),
    });
  };

  const handleUsageSaved = () => {
    setUsageModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "usage.record.success" }),
    });
  };

  const handleAdjustmentSaved = () => {
    setAdjustmentModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "adjustment.success" }),
    });
  };

  const handleDisposalSaved = () => {
    setDisposalModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "disposal.success" }),
    });
  };

  const handleQCStatusSaved = () => {
    setQcStatusModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "qc.status.update.success" }),
    });
  };

  const handleEditLot = (lot) => {
    setSelectedLot(lot);
    setLotModalOpen(true);
  };

  const handleViewDetails = (lot) => {
    setSelectedLot(lot);
    setDetailsPanelOpen(true);
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Grid className="inventory-metrics-grid" fullWidth={false}>
        <Column lg={4} md={2} sm={4} className="inventory-metric-column">
          <Tile className="inventory-metric-tile">
            <div className="metric-value">{metrics.totalLots}</div>
            <div className="metric-label">
              <FormattedMessage id="inventory.metrics.totalLots" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4} className="inventory-metric-column">
          <Tile className="inventory-metric-tile metric-warning">
            <div className="metric-value">{metrics.lowStock}</div>
            <div className="metric-label">
              <FormattedMessage id="inventory.metrics.lowStock" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4} className="inventory-metric-column">
          <Tile className="inventory-metric-tile metric-expiring">
            <div className="metric-value">{metrics.expiringSoon}</div>
            <div className="metric-label">
              <FormattedMessage id="inventory.metrics.expiringSoon" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4} className="inventory-metric-column">
          <Tile className="inventory-metric-tile metric-expired">
            <div className="metric-value">{metrics.expired}</div>
            <div className="metric-label">
              <FormattedMessage id="inventory.metrics.expired" />
            </div>
          </Tile>
        </Column>
      </Grid>

      <DataTable rows={rows} headers={headers} isSortable radio={false}>
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getSelectionProps,
          getTableProps,
          getTableContainerProps,
          selectedRows,
          selectRow,
        }) => {
          const handleBatchDispose = () => {
            const selectedLots = selectedRows.map((row) => {
              const lotIndex = rows.findIndex((r) => r.id === row.id);
              return lots[lotIndex];
            });
            setSelectedLotsForDisposal(selectedLots);
            setSelectedLot(null);
            setDisposalModalOpen(true);
          };

          return (
            <>
              <div className="inventory-filters-section">
                <div className="inventory-filters-container">
                  <div className="filter-group">
                    <Search
                      placeholder={intl.formatMessage({
                        id: "inventory.search.placeholder",
                      })}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      value={searchTerm}
                      size="md"
                    />

                    <Dropdown
                      id="type-filter"
                      titleText=""
                      label={intl.formatMessage({
                        id: "inventory.filter.type",
                      })}
                      items={itemTypes}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={itemTypes.find((t) => t.id === typeFilter)}
                      onChange={({ selectedItem }) =>
                        setTypeFilter(selectedItem.id)
                      }
                      size="md"
                    />

                    <Dropdown
                      id="status-filter"
                      titleText=""
                      label={intl.formatMessage({
                        id: "inventory.filter.status",
                      })}
                      items={statusOptions}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={statusOptions.find(
                        (s) => s.id === statusFilter,
                      )}
                      onChange={({ selectedItem }) =>
                        setStatusFilter(selectedItem.id)
                      }
                      size="md"
                    />
                  </div>

                  <div className="action-buttons-group">
                    <Button
                      renderIcon={Add}
                      onClick={() => {
                        setSelectedLot(null);
                        setLotModalOpen(true);
                      }}
                    >
                      <FormattedMessage id="inventory.add.button" />
                    </Button>
                  </div>
                </div>
              </div>
              <TableContainer
                title=""
                description=""
                {...getTableContainerProps()}
              >
                <TableToolbar>
                  <TableBatchActions
                    onCancel={() => {
                      selectedRows.forEach((rowId) => selectRow(rowId));
                    }}
                    totalSelected={selectedRows.length}
                    shouldShowBatchActions={selectedRows.length > 0}
                  >
                    <TableBatchAction
                      renderIcon={() => null}
                      onClick={handleBatchDispose}
                    >
                      <FormattedMessage
                        id="disposal.batch.button"
                        defaultMessage="Dispose Selected ({count})"
                        values={{ count: selectedRows.length }}
                      />
                    </TableBatchAction>
                  </TableBatchActions>
                  <TableToolbarContent>
                    {/* Empty toolbar content since filters moved outside */}
                  </TableToolbarContent>
                </TableToolbar>

                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll {...getSelectionProps()} />
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
                    {loading ? (
                      <TableRow>
                        <TableCell colSpan={headers.length}>
                          Loading...
                        </TableCell>
                      </TableRow>
                    ) : rows.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={headers.length}>
                          No inventory items found
                        </TableCell>
                      </TableRow>
                    ) : (
                      rows.map((row, rowIndex) => {
                        const lot = lots[rowIndex];
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => {
                              if (cell.info.header === "stockStatus") {
                                const status = cell.value;
                                return (
                                  <TableCell key={cell.id}>
                                    {status && (
                                      <Tag type={status.kind}>
                                        {status.label}
                                      </Tag>
                                    )}
                                  </TableCell>
                                );
                              }

                              if (cell.info.header === "dateOpened") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value === "Not Opened" ? (
                                      <Tag type="outline">{cell.value}</Tag>
                                    ) : (
                                      cell.value
                                    )}
                                  </TableCell>
                                );
                              }

                              if (cell.info.header === "actions") {
                                return (
                                  <TableCell key={cell.id}>
                                    <OverflowMenu
                                      size="sm"
                                      flipped
                                      aria-label={
                                        intl?.formatMessage({
                                          id: "label.button.action",
                                        }) || "Actions"
                                      }
                                    >
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "lot.details.view",
                                        })}
                                        onClick={() => handleViewDetails(lot)}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "button.edit",
                                        })}
                                        onClick={() => handleEditLot(lot)}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "usage.record.button",
                                        })}
                                        onClick={() => {
                                          setSelectedLot(lot);
                                          setSelectedItem(null);
                                          setUsageModalOpen(true);
                                        }}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "usage.record.fefo.button",
                                        })}
                                        onClick={() => {
                                          setSelectedItem(lot.inventoryItem);
                                          setSelectedLot(null);
                                          setUsageModalOpen(true);
                                        }}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "adjustment.button",
                                        })}
                                        onClick={() => {
                                          setSelectedLot(lot);
                                          setAdjustmentModalOpen(true);
                                        }}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "qc.status.update.button",
                                        })}
                                        onClick={() => {
                                          setSelectedLot(lot);
                                          setQcStatusModalOpen(true);
                                        }}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "audit.log.view.button",
                                        })}
                                        onClick={() => {
                                          setSelectedLot(lot);
                                          setAuditLogOpen(true);
                                        }}
                                      />
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "disposal.button",
                                        })}
                                        onClick={() => {
                                          setSelectedLot(lot);
                                          setDisposalModalOpen(true);
                                        }}
                                        isDelete
                                      />
                                    </OverflowMenu>
                                  </TableCell>
                                );
                              }

                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableRow>
                        );
                      })
                    )}
                  </TableBody>
                </Table>

                {!loading && rows.length > 0 && (
                  <Pagination
                    backwardText="Previous page"
                    forwardText="Next page"
                    itemsPerPageText="Items per page:"
                    page={page}
                    pageSize={pageSize}
                    pageSizes={[10, 20, 30, 40, 50]}
                    totalItems={totalRecords}
                    onChange={({ page, pageSize }) => {
                      setPage(page);
                      setPageSize(pageSize);
                    }}
                  />
                )}
              </TableContainer>
            </>
          );
        }}
      </DataTable>

      {/* Lot Entry Modal */}
      {lotModalOpen && (
        <LotEntryModal
          open={lotModalOpen}
          onClose={() => {
            setLotModalOpen(false);
            setSelectedLot(null);
          }}
          onSave={handleLotSaved}
          lot={selectedLot}
        />
      )}

      {/* Record Usage Modal */}
      {usageModalOpen && (
        <RecordUsageModal
          open={usageModalOpen}
          onClose={() => {
            setUsageModalOpen(false);
            setSelectedLot(null);
            setSelectedItem(null);
          }}
          onSave={handleUsageSaved}
          lot={selectedLot}
          item={selectedItem}
        />
      )}

      {/* Lot Adjustment Modal */}
      {adjustmentModalOpen && (
        <LotAdjustmentModal
          open={adjustmentModalOpen}
          onClose={() => {
            setAdjustmentModalOpen(false);
            setSelectedLot(null);
          }}
          onSave={handleAdjustmentSaved}
          lot={selectedLot}
        />
      )}

      {/* Dispose Lot Modal */}
      {disposalModalOpen && (
        <DisposeLotModal
          open={disposalModalOpen}
          onClose={() => {
            setDisposalModalOpen(false);
            setSelectedLot(null);
            setSelectedLotsForDisposal([]);
          }}
          onSave={handleDisposalSaved}
          lot={selectedLot}
          lots={
            selectedLotsForDisposal.length > 0
              ? selectedLotsForDisposal
              : selectedLot
                ? [selectedLot]
                : []
          }
        />
      )}

      {/* Update QC Status Modal */}
      {qcStatusModalOpen && (
        <UpdateQCStatusModal
          open={qcStatusModalOpen}
          onClose={() => {
            setQcStatusModalOpen(false);
            setSelectedLot(null);
          }}
          onSave={handleQCStatusSaved}
          lot={selectedLot}
        />
      )}

      {/* Lot Details Panel (Slide-out) */}
      <LotDetailsPanel
        open={detailsPanelOpen}
        onClose={() => {
          setDetailsPanelOpen(false);
          setSelectedLot(null);
        }}
        lot={selectedLot}
      />

      {/* Audit Log Viewer */}
      <AuditLogViewer
        open={auditLogOpen}
        onClose={() => {
          setAuditLogOpen(false);
          setSelectedLot(null);
        }}
        entityType="LOT"
        entityId={selectedLot?.id}
        entityName={selectedLot?.lotNumber}
      />
    </>
  );
};

export default InventoryDashboard;
