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

  const [lotModalOpen, setLotModalOpen] = useState(false);
  const [usageModalOpen, setUsageModalOpen] = useState(false);
  const [adjustmentModalOpen, setAdjustmentModalOpen] = useState(false);
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [qcStatusModalOpen, setQcStatusModalOpen] = useState(false);
  const [detailsPanelOpen, setDetailsPanelOpen] = useState(false);
  const [selectedLot, setSelectedLot] = useState(null);

  const itemTypes = [
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
    { id: "REAGENT", text: "Reagent" },
    { id: "RDT", text: "RDT" },
    { id: "CARTRIDGE", text: "Cartridge" },
  ];

  const statusOptions = [
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
    { id: "ACTIVE", text: "Active" },
    { id: "IN_USE", text: "In Use" },
    { id: "EXPIRED", text: "Expired" },
    { id: "CONSUMED", text: "Consumed" },
    { id: "QUARANTINED", text: "Quarantined" },
  ];

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "catalog.item.name" }),
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
    fetchLots();
  }, [typeFilter, statusFilter]);

  const fetchLots = async () => {
    setLoading(true);
    try {
      const lotsResponse = await InventoryLotAPI.getAll({
        status: statusFilter !== "ALL" ? statusFilter : undefined,
      });

      const validLots = Array.isArray(lotsResponse) ? lotsResponse : [];
      setLots(validLots);

      const uniqueItemIds = [
        ...new Set(
          validLots.map((lot) => lot.inventoryItem?.id).filter(Boolean),
        ),
      ];

      const itemsMap = {};
      await Promise.all(
        uniqueItemIds.map(async (itemId) => {
          try {
            const item = await InventoryItemAPI.getById(itemId);
            itemsMap[itemId] = item;
          } catch (error) {
            console.error(`Error fetching item ${itemId}:`, error);
          }
        }),
      );

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

  const getFilteredLots = () => {
    let filtered = lots;

    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter((lot) => {
        const item = items[lot.inventoryItem?.id];
        return (
          lot.lotNumber?.toLowerCase().includes(searchLower) ||
          item?.name?.toLowerCase().includes(searchLower)
        );
      });
    }

    if (typeFilter !== "ALL") {
      filtered = filtered.filter((lot) => {
        const item = items[lot.inventoryItem?.id];
        return item?.itemType === typeFilter;
      });
    }

    return filtered;
  };

  const filteredLots = getFilteredLots();

  const paginatedLots = filteredLots.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  const rows = paginatedLots.map((lot) => {
    const item = items[lot.inventoryItem?.id];
    const stockStatus = getStockStatus(lot);

    return {
      id: String(lot.id),
      name: item?.name || "Unknown",
      lotNumber: lot.lotNumber,
      itemType: item?.itemType || "",
      currentQuantity: `${lot.currentQuantity || 0} ${item?.units || ""}`,
      expirationDate: lot.expirationDate
        ? new Date(lot.expirationDate).toLocaleDateString()
        : "N/A",
      status: lot.status,
      stockStatus: stockStatus,
    };
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
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  placeholder={intl.formatMessage({
                    id: "inventory.search.placeholder",
                  })}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  value={searchTerm}
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

                <Button
                  renderIcon={Add}
                  onClick={() => {
                    setSelectedLot(null);
                    setLotModalOpen(true);
                  }}
                >
                  <FormattedMessage id="inventory.add.button" />
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
                  rows.map((row, rowIndex) => {
                    const lot = paginatedLots[rowIndex];
                    return (
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

                          if (cell.info.header === "actions") {
                            return (
                              <TableCell key={cell.id}>
                                <OverflowMenu
                                  size="sm"
                                  flipped
                                  ariaLabel={intl.formatMessage({
                                    id: "label.button.action",
                                  })}
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
                            <TableCell key={cell.id}>{cell.value}</TableCell>
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
                totalItems={filteredLots.length}
                onChange={({ page, pageSize }) => {
                  setPage(page);
                  setPageSize(pageSize);
                }}
              />
            )}
          </TableContainer>
        )}
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
          }}
          onSave={handleUsageSaved}
          lot={selectedLot}
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
          }}
          onSave={handleDisposalSaved}
          lot={selectedLot}
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
    </>
  );
};

export default InventoryDashboard;
