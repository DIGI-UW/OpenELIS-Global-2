import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Tag,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import InventoryItemForm from "./InventoryItemForm.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/admin" },
  {
    label: "inventory.manage.title",
    link: "/admin#inventoryManagement",
  },
];

function InventoryManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeFilter, setActiveFilter] = useState("all"); // all, active, inactive
  const [inventoryList, setInventoryList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [kitTypes, setKitTypes] = useState([]);
  const [sources, setSources] = useState([]);
  const [fromRecordCount, setFromRecordCount] = useState("1");
  const [toRecordCount, setToRecordCount] = useState("0");
  const [totalRecordCount, setTotalRecordCount] = useState("0");
  const [modifyButton, setModifyButton] = useState(true);
  const [deactivateButton, setDeactivateButton] = useState(true);

  // Load initial data
  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);

    // Load kit types
    getFromOpenElisServer("/rest/inventory/kit-types", (res) => {
      if (componentMounted.current) {
        if (res && Array.isArray(res)) {
          setKitTypes(res);
        } else {
          // Fallback to default types if API fails
          setKitTypes(["HIV", "SYPHILIS"]);
        }
      }
    });

    // Load sources
    getFromOpenElisServer("/rest/inventory/sources", (res) => {
      if (res) {
        setSources(res);
      }
    });

    // Load inventory items
    loadInventoryItems();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadInventoryItems = () => {
    const activeOnly =
      activeFilter === "active"
        ? true
        : activeFilter === "inactive"
          ? false
          : null;
    const url =
      activeOnly !== null
        ? `/rest/inventory?activeOnly=${activeOnly}`
        : "/rest/inventory";

    getFromOpenElisServer(url, (res) => {
      if (res) {
        setInventoryList(res);
        setFilteredList(res);
        setLoading(false);
      } else {
        setLoading(false);
      }
    });
  };

  useEffect(() => {
    loadInventoryItems();
  }, [activeFilter]);

  // Filter by search term
  useEffect(() => {
    if (!searchTerm) {
      setFilteredList(inventoryList);
    } else {
      const filtered = inventoryList.filter((item) => {
        const searchLower = searchTerm.toLowerCase();
        return (
          item.kitName?.toLowerCase().includes(searchLower) ||
          item.type?.toLowerCase().includes(searchLower) ||
          item.lotNumber?.toLowerCase().includes(searchLower) ||
          item.source?.toLowerCase().includes(searchLower)
        );
      });
      setFilteredList(filtered);
    }
  }, [searchTerm, inventoryList]);

  // Update pagination counts
  useEffect(() => {
    const total = filteredList.length;
    const from = total > 0 ? (page - 1) * pageSize + 1 : 0;
    const to = Math.min(page * pageSize, total);
    setTotalRecordCount(total.toString());
    setFromRecordCount(from.toString());
    setToRecordCount(to.toString());
  }, [filteredList, page, pageSize]);

  // Update button states based on selection
  useEffect(() => {
    setModifyButton(selectedRowIds.length !== 1);
    setDeactivateButton(selectedRowIds.length === 0);
  }, [selectedRowIds]);

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
  };

  const handlePreviousPage = () => {
    if (page > 1) {
      setPage(page - 1);
      setSelectedRowIds([]);
    }
  };

  const handleNextPage = () => {
    const totalPages = Math.ceil(filteredList.length / pageSize);
    if (page < totalPages) {
      setPage(page + 1);
      setSelectedRowIds([]);
    }
  };

  const handleCreate = () => {
    setEditingItem(null);
    setIsModalOpen(true);
  };

  const handleEdit = (item) => {
    setEditingItem(item);
    setIsModalOpen(true);
  };

  const openUpdateModal = (id) => {
    const item = filteredList.find((i) => i.id === id);
    if (item) {
      handleEdit(item);
    }
  };

  const handleDeactivate = () => {
    if (selectedRowIds.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "inventory.select.item.to.deactivate",
        }),
      });
      setNotificationVisible(true);
      return;
    }

    const itemsToUpdate = filteredList.filter((item) =>
      selectedRowIds.includes(item.id),
    );

    itemsToUpdate.forEach((item) => {
      const updatedItem = {
        ...item,
        isActive: false,
      };

      postToOpenElisServerFullResponse(
        `/rest/inventory/${item.id}`,
        JSON.stringify(updatedItem),
        (res) => {
          if (res && res.status === 200) {
            loadInventoryItems();
            addNotification({
              kind: NotificationKinds.success,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "inventory.deactivate.success",
              }),
            });
            setNotificationVisible(true);
          }
        },
      );
    });
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setEditingItem(null);
  };

  const handleModalSave = () => {
    loadInventoryItems();
    handleModalClose();
  };

  // Prepare table headers
  const headers = [
    {
      key: "kitName",
      header: intl.formatMessage({ id: "inventory.testKit.name" }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "inventory.testKit.type" }),
    },
    {
      key: "receiveDate",
      header: intl.formatMessage({ id: "inventory.testKit.receiveDate" }),
    },
    {
      key: "expirationDate",
      header: intl.formatMessage({ id: "inventory.testKit.expiration" }),
    },
    {
      key: "lotNumber",
      header: intl.formatMessage({ id: "inventory.testKit.lot" }),
    },
    {
      key: "source",
      header: intl.formatMessage({ id: "inventory.testKit.source" }),
    },
    {
      key: "isActive",
      header: intl.formatMessage({ id: "inventory.testKit.status" }),
    },
  ];

  // Prepare table rows
  const rows = filteredList.map((item) => ({
    id: item.id,
    kitName: item.kitName || "",
    type: item.type || "",
    receiveDate: item.receiveDate || "",
    expirationDate: item.expirationDate || "",
    lotNumber: item.lotNumber || "",
    source: item.source || "",
    isActive: item.isActive ? (
      <Tag type="green">
        <FormattedMessage id="label.active" />
      </Tag>
    ) : (
      <Tag type="red">
        <FormattedMessage id="label.inactive" />
      </Tag>
    ),
  }));

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="inventory.manage.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <ActionPaginationButtonType
          selectedRowIds={selectedRowIds}
          modifyButton={modifyButton}
          deactivateButton={deactivateButton}
          deleteDeactivate={handleDeactivate}
          openUpdateModal={openUpdateModal}
          openAddModal={handleCreate}
          handlePreviousPage={handlePreviousPage}
          handleNextPage={handleNextPage}
          fromRecordCount={fromRecordCount}
          toRecordCount={toRecordCount}
          totalRecordCount={totalRecordCount}
          type="type1"
        />
        <br />
        <div className="orderLegendBody">
          <Grid>
            <Column lg={8} md={4} sm={2}>
              <Section>
                <Search
                  size="lg"
                  id="inventory-search-bar"
                  labelText={<FormattedMessage id="inventory.search" />}
                  placeholder={intl.formatMessage({
                    id: "inventory.search.placeholder",
                  })}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  value={searchTerm || ""}
                />
              </Section>
            </Column>
            <Column lg={8} md={4} sm={2}>
              <Section>
                <Select
                  id="activeFilter"
                  labelText={intl.formatMessage({
                    id: "inventory.filter.status",
                  })}
                  value={activeFilter}
                  onChange={(e) => setActiveFilter(e.target.value)}
                >
                  <SelectItem
                    value="all"
                    text={intl.formatMessage({ id: "label.all" })}
                  />
                  <SelectItem
                    value="active"
                    text={intl.formatMessage({ id: "label.active" })}
                  />
                  <SelectItem
                    value="inactive"
                    text={intl.formatMessage({ id: "label.inactive" })}
                  />
                </Select>
              </Section>
            </Column>
          </Grid>
          <br />
          <>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                {loading ? (
                  <Loading
                    description={intl.formatMessage({ id: "label.loading" })}
                  />
                ) : (
                  <DataTable
                    rows={filteredList.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )}
                    headers={headers}
                    isSortable
                    useZebraStyles
                    render={({
                      rows,
                      headers,
                      getHeaderProps,
                      getRowProps,
                      getSelectionProps,
                      getTableProps,
                    }) => (
                      <TableContainer>
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
                            {rows.map((row) => (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                />
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  />
                )}
                {!loading && filteredList.length > 0 && (
                  <div
                    className="cds--pagination"
                    style={{ marginTop: "1rem" }}
                  >
                    <Pagination
                      onChange={handlePageChange}
                      page={page}
                      pageSize={pageSize}
                      pageSizes={[10, 20]}
                      totalItems={filteredList.length}
                      forwardText={intl.formatMessage({
                        id: "pagination.forward",
                      })}
                      backwardText={intl.formatMessage({
                        id: "pagination.backward",
                      })}
                      itemRangeText={(min, max, total) =>
                        intl.formatMessage(
                          { id: "pagination.item-range" },
                          { min: min, max: max, total: total },
                        )
                      }
                      itemsPerPageText={intl.formatMessage({
                        id: "pagination.items-per-page",
                      })}
                      itemText={(min, max) =>
                        intl.formatMessage(
                          { id: "pagination.item" },
                          { min: min, max: max },
                        )
                      }
                      pageNumberText={intl.formatMessage({
                        id: "pagination.page-number",
                      })}
                      pageRangeText={(_current, total) =>
                        intl.formatMessage(
                          { id: "pagination.page-range" },
                          { total: total },
                        )
                      }
                      pageText={(page, pagesUnknown) =>
                        intl.formatMessage(
                          { id: "pagination.page" },
                          { page: pagesUnknown ? "" : page },
                        )
                      }
                    />
                  </div>
                )}
              </Column>
            </Grid>
          </>
        </div>
      </div>

      {isModalOpen && (
        <InventoryItemForm
          isOpen={isModalOpen}
          onClose={handleModalClose}
          onSave={handleModalSave}
          item={editingItem}
          kitTypes={kitTypes}
          sources={sources}
        />
      )}
    </>
  );
}

export default InventoryManagement;
