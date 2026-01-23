import React, { useState, useEffect, useContext, useCallback } from "react";
import {
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
import { InventoryItemAPI, InventoryLotAPI } from "./InventoryService";
import LotEntryModal from "./LotEntryModal";
import InventoryItemForm from "./InventoryItemForm";
import RecordUsageModal from "./RecordUsageModal";
import LotAdjustmentModal from "./LotAdjustmentModal";
import DisposeLotModal from "./DisposeLotModal";
import UpdateQCStatusModal from "./UpdateQCStatusModal";
import LotDetailsPanel from "./LotDetailsPanel";
import InventoryReportsModal from "./InventoryReportsModal";
import PageBreadCrumb from "../common/PageBreadCrumb";
import "./InventoryList.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.inventory.management",
    link: "/inventory",
    defaultMessage: "Inventory Management",
  },
];

const InventoryList = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

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
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [totalRecords, setTotalRecords] = useState(0);

  const [lotModalOpen, setLotModalOpen] = useState(false);
  const [itemModalOpen, setItemModalOpen] = useState(false);
  const [usageModalOpen, setUsageModalOpen] = useState(false);
  const [adjustmentModalOpen, setAdjustmentModalOpen] = useState(false);
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [qcStatusModalOpen, setQcStatusModalOpen] = useState(false);
  const [detailsPanelOpen, setDetailsPanelOpen] = useState(false);
  const [reportsModalOpen, setReportsModalOpen] = useState(false);
  const [selectedLot, setSelectedLot] = useState(null);

  const [itemTypes, setItemTypes] = useState([
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
  ]);

  const statusOptions = [
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
    { id: "ACTIVE", text: "Active" },
    { id: "INACTIVE", text: "Inactive" },
  ];

  const headers = [
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
      key: "quantity",
      header: intl.formatMessage({ id: "inventory.quantity" }) || "Quantity",
    },
    {
      key: "uom",
      header: intl.formatMessage({ id: "inventory.uom" }) || "UOM",
    },
    {
      key: "unitSize",
      header: intl.formatMessage({ id: "inventory.unitSize" }) || "Unit Size",
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

  useEffect(() => {
    const loadItemTypes = async () => {
      try {
        const types = await InventoryItemAPI.getItemTypes();
        const formattedTypes = [
          {
            id: "ALL",
            text: intl.formatMessage({ id: "inventory.filter.all" }),
          },
          ...types.map((type) => ({
            id: type,
            text: getItemTypeLabel(type),
          })),
        ];
        setItemTypes(formattedTypes);
      } catch (err) {
        console.error("Error loading item types:", err);
      }
    };
    loadItemTypes();
  }, [intl]);

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
        unitLookup[unit.id] = unit.text;
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

  const getItemTypeLabel = (type) => {
    const labels = {
      REAGENT: "Reagent",
      RDT: "RDT (Rapid Diagnostic Test)",
      CARTRIDGE: "Analyzer Cartridge",
      HIV_KIT: "HIV Test Kit",
      SYPHILIS_KIT: "Syphilis Test Kit",
    };
    return labels[type] || type;
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
      setTotalRecords(0);
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
          kind: "yellow",
        };
      }
    }

    if (currentQty === 0) {
      return { type: "outOfStock", label: "Out of Stock", kind: "red" };
    }

    if (currentQty < minStock) {
      return { type: "lowStock", label: "Low Stock", kind: "yellow" };
    }

    return { type: "inStock", label: "In Stock", kind: "green" };
  };

  const rows = lots.map((lot) => {
    const item = items[lot.inventoryItem?.id];
    const stockStatus = getStockStatus(lot);

    const unitId = item?.units;
    const unitsDisplay = unitMap[unitId] || unitId || "N/A";

    const projectName = item?.projectName;
    let projectDisplay = "N/A";
    if (projectName) {
      if (isNaN(projectName)) {
        projectDisplay = projectName;
      } else {
        projectDisplay = projectMap[projectName] || projectName;
      }
    }

    return {
      id: lot.id,
      name: item?.name || "Unknown",
      projectName: projectDisplay,
      lotNumber: lot.lotNumber,
      itemType: item?.itemType || "",
      quantity: lot.currentQuantity || 0,
      uom: unitsDisplay,
      unitSize: lot.unitSize || "N/A",
      expirationDate: lot.expirationDate
        ? new Date(lot.expirationDate).toLocaleDateString()
        : "N/A",
      dateOpened: lot.dateOpened
        ? new Date(lot.dateOpened).toLocaleDateString()
        : "Not Opened",
      status: lot.status,
      stockStatus: stockStatus,
      lot: lot,
      item: item,
    };
  });

  const handleLotSaved = () => {
    setLotModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "lot.save.success" }),
    });
  };

  const handleItemSaved = () => {
    setItemModalOpen(false);
    fetchLots();
    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "catalog.item.save.success" }),
    });
  };

  const handleUsageSaved = () => {
    setUsageModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "usage.record.success" }),
    });
  };

  const handleAdjustmentSaved = () => {
    setAdjustmentModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "adjustment.success" }),
    });
  };

  const handleDisposalSaved = () => {
    setDisposalModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: "disposal.success" }),
    });
  };

  const handleQCStatusSaved = () => {
    setQcStatusModalOpen(false);
    setSelectedLot(null);
    fetchLots();
    addNotification({
      kind: "success",
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

  const handleOpenLot = async (lot) => {
    try {
      await InventoryLotAPI.open(lot.id);
      fetchLots();
      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({ id: "lot.open.success" }),
      });
    } catch (error) {
      console.error("Error opening lot:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: error.message || "Failed to open lot",
      });
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h2>
              <FormattedMessage id="inventory.list.title" />
            </h2>

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
          </div>
        </Column>
      </Grid>

      {/* Filters Section - Outside DataTable to prevent dropdown overlap */}
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
              label={intl.formatMessage({ id: "inventory.filter.type" })}
              items={itemTypes}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={itemTypes.find((t) => t.id === typeFilter)}
              onChange={({ selectedItem }) => setTypeFilter(selectedItem.id)}
              size="md"
            />

            <Dropdown
              id="status-filter"
              titleText=""
              label={intl.formatMessage({ id: "inventory.filter.status" })}
              items={statusOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={statusOptions.find((s) => s.id === statusFilter)}
              onChange={({ selectedItem }) => setStatusFilter(selectedItem.id)}
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

            <Button kind="secondary" onClick={() => setItemModalOpen(true)}>
              <FormattedMessage id="inventory.addItem.button" />
            </Button>

            <Button kind="tertiary" onClick={() => setReportsModalOpen(true)}>
              <FormattedMessage id="reports.button" />
            </Button>
          </div>
        </div>
      </div>

      <DataTable rows={rows} headers={headers} isSortable>
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getTableContainerProps,
        }) => (
          <TableContainer title="" description="" {...getTableContainerProps()}>
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
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={headers.length}>Loading...</TableCell>
                  </TableRow>
                ) : rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={headers.length}>
                      No inventory items found
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "stockStatus") {
                          const status = cell.value;
                          return (
                            <TableCell key={cell.id}>
                              {status && (
                                <Tag type={status.kind}>{status.label}</Tag>
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
                                  onClick={() => handleViewDetails(row.lot)}
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "button.edit",
                                  })}
                                  onClick={() => handleEditLot(row.lot)}
                                />
                                {!row.lot.dateOpened && (
                                  <OverflowMenuItem
                                    itemText={intl.formatMessage({
                                      id: "lot.open.button",
                                    })}
                                    onClick={() => handleOpenLot(row.lot)}
                                  />
                                )}
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "usage.record.button",
                                  })}
                                  onClick={() => {
                                    setSelectedLot(row.lot);
                                    setUsageModalOpen(true);
                                  }}
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "adjustment.button",
                                  })}
                                  onClick={() => {
                                    setSelectedLot(row.lot);
                                    setAdjustmentModalOpen(true);
                                  }}
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "qc.status.update.button",
                                  })}
                                  onClick={() => {
                                    setSelectedLot(row.lot);
                                    setQcStatusModalOpen(true);
                                  }}
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "disposal.button",
                                  })}
                                  onClick={() => {
                                    setSelectedLot(row.lot);
                                    setDisposalModalOpen(true);
                                  }}
                                  isDelete
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }

                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        );
                      })}
                    </TableRow>
                  ))
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
        )}
      </DataTable>

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

      {itemModalOpen && (
        <InventoryItemForm
          open={itemModalOpen}
          onClose={() => setItemModalOpen(false)}
          onSave={handleItemSaved}
        />
      )}

      {usageModalOpen && (
        <RecordUsageModal
          open={usageModalOpen}
          onClose={() => {
            setUsageModalOpen(false);
            setSelectedLot(null);
          }}
          onSave={handleUsageSaved}
          lot={selectedLot}
        />
      )}

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

      {disposalModalOpen && (
        <DisposeLotModal
          open={disposalModalOpen}
          onClose={() => {
            setDisposalModalOpen(false);
            setSelectedLot(null);
          }}
          onSave={handleDisposalSaved}
          lot={selectedLot}
        />
      )}

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

      <LotDetailsPanel
        open={detailsPanelOpen}
        onClose={() => {
          setDetailsPanelOpen(false);
          setSelectedLot(null);
        }}
        lot={selectedLot}
      />

      {reportsModalOpen && (
        <InventoryReportsModal
          open={reportsModalOpen}
          onClose={() => setReportsModalOpen(false)}
        />
      )}
    </>
  );
};

export default InventoryList;
