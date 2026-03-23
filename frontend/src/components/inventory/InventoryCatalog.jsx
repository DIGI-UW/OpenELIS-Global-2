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
  OverflowMenu,
  OverflowMenuItem,
  Pagination,
  Tag,
  Modal,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI } from "./InventoryService";
import InventoryItemForm from "./InventoryItemForm";

const InventoryCatalog = () => {
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

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [itemTypes, setItemTypes] = useState([
    { id: "ALL", text: intl.formatMessage({ id: "inventory.filter.all" }) },
  ]);

  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [itemModalOpen, setItemModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [deactivateModalOpen, setDeactivateModalOpen] = useState(false);
  const [itemToDeactivate, setItemToDeactivate] = useState(null);

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
      key: "itemType",
      header: intl.formatMessage({ id: "catalog.item.type" }),
    },
    {
      key: "units",
      header: intl.formatMessage({ id: "catalog.item.units" }),
    },
    {
      key: "lowStockThreshold",
      header: intl.formatMessage({ id: "catalog.item.lowStockThreshold" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "catalog.item.status" }),
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
    fetchItems();
  }, [typeFilter, statusFilter]);

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

  const fetchItems = async () => {
    setLoading(true);
    try {
      const response = await InventoryItemAPI.getAll();
      const processedItems = (response || []).map((item) => ({
        ...item,
        isActive: item.isActive === "Y" || item.isActive === true,
      }));
      setItems(processedItems);
    } catch (error) {
      console.error("Error fetching catalog items:", error);
      setItems([]);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Error loading catalog items",
      });
    } finally {
      setLoading(false);
    }
  };

  const getFilteredItems = () => {
    let filtered = items;

    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      filtered = filtered.filter((item) =>
        item.name?.toLowerCase().includes(searchLower),
      );
    }

    if (typeFilter !== "ALL") {
      filtered = filtered.filter((item) => item.itemType === typeFilter);
    }

    if (statusFilter !== "ALL") {
      filtered = filtered.filter((item) => {
        if (statusFilter === "ACTIVE") return item.isActive;
        if (statusFilter === "INACTIVE") return !item.isActive;
        return true;
      });
    }

    return filtered;
  };

  const filteredItems = getFilteredItems();

  const paginatedItems = filteredItems.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  const rows = paginatedItems.map((item) => ({
    id: String(item.id),
    name: item.name,
    itemType: item.itemType,
    units: item.units,
    lowStockThreshold: item.lowStockThreshold || "-",
    status: item.isActive ? "Active" : "Inactive",
  }));

  const handleItemSaved = () => {
    setItemModalOpen(false);
    setSelectedItem(null);
    fetchItems();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      subtitle: intl.formatMessage({ id: "catalog.item.save.success" }),
    });
  };

  const handleEditItem = (item) => {
    setSelectedItem(item);
    setItemModalOpen(true);
  };

  const handleDeactivateItem = (item) => {
    setItemToDeactivate(item);
    setDeactivateModalOpen(true);
  };

  const handleActivateItem = async (item) => {
    if (!item) return;

    try {
      await InventoryItemAPI.activate(item.id);
      fetchItems();
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: intl.formatMessage({ id: "catalog.item.activate.success" }),
      });
    } catch (error) {
      console.error("Error activating item:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Error activating item",
      });
    }
  };

  const confirmDeactivate = async () => {
    if (!itemToDeactivate) return;

    try {
      await InventoryItemAPI.deactivate(itemToDeactivate.id);
      setDeactivateModalOpen(false);
      setItemToDeactivate(null);
      fetchItems();
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: intl.formatMessage({ id: "catalog.item.deactivate.success" }),
      });
    } catch (error) {
      console.error("Error deactivating item:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Error deactivating item",
      });
    }
  };

  const cancelDeactivate = () => {
    setDeactivateModalOpen(false);
    setItemToDeactivate(null);
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
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
                    id: "catalog.search.placeholder",
                  })}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  value={searchTerm}
                />

                <Dropdown
                  id="type-filter"
                  titleText=""
                  label={intl.formatMessage({ id: "inventory.filter.type" })}
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
                  label={intl.formatMessage({ id: "inventory.filter.status" })}
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
                    setSelectedItem(null);
                    setItemModalOpen(true);
                  }}
                >
                  <FormattedMessage id="inventory.addItem.button" />
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
                      No catalog items found
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row, rowIndex) => {
                    const item = paginatedItems[rowIndex];
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "status") {
                            return (
                              <TableCell key={cell.id}>
                                <Tag
                                  type={
                                    cell.value === "Active" ? "green" : "gray"
                                  }
                                >
                                  {cell.value}
                                </Tag>
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
                                      id: "button.edit",
                                    })}
                                    onClick={() => handleEditItem(item)}
                                  />
                                  {item && item.isActive && (
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "button.deactivate",
                                      })}
                                      onClick={() => handleDeactivateItem(item)}
                                      isDelete
                                    />
                                  )}
                                  {item && !item.isActive && (
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "button.activate",
                                      })}
                                      onClick={() => handleActivateItem(item)}
                                    />
                                  )}
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
                totalItems={filteredItems.length}
                onChange={({ page, pageSize }) => {
                  setPage(page);
                  setPageSize(pageSize);
                }}
              />
            )}
          </TableContainer>
        )}
      </DataTable>

      {itemModalOpen && (
        <InventoryItemForm
          open={itemModalOpen}
          onClose={() => {
            setItemModalOpen(false);
            setSelectedItem(null);
          }}
          onSave={handleItemSaved}
          item={selectedItem}
        />
      )}

      <Modal
        open={deactivateModalOpen}
        danger
        modalHeading={intl.formatMessage({
          id: "catalog.item.deactivate.confirm.title",
        })}
        modalLabel={intl.formatMessage({ id: "catalog.item.deactivate.label" })}
        primaryButtonText={intl.formatMessage({
          id: "catalog.item.deactivate.confirm.button",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestSubmit={confirmDeactivate}
        onSecondarySubmit={cancelDeactivate}
        onRequestClose={cancelDeactivate}
      >
        <p>
          <FormattedMessage
            id="catalog.item.deactivate.confirm.message"
            values={{ itemName: itemToDeactivate?.name || "" }}
          />
        </p>
        <br />
        <p>
          <strong>
            <FormattedMessage id="catalog.item.deactivate.implications.title" />
          </strong>
        </p>
        <ul style={{ marginLeft: "20px", marginTop: "8px" }}>
          <li>
            <FormattedMessage id="catalog.item.deactivate.implications.1" />
          </li>
          <li>
            <FormattedMessage id="catalog.item.deactivate.implications.2" />
          </li>
          <li>
            <FormattedMessage id="catalog.item.deactivate.implications.3" />
          </li>
        </ul>
      </Modal>
    </>
  );
};

export default InventoryCatalog;
