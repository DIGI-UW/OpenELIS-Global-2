import React, { useState, useEffect, useContext, useCallback } from "react";
import { useHistory } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
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
  Button,
  Tag,
  Checkbox,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Pagination,
  ProgressBar,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { Add, Scan, Renew } from "@carbon/icons-react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";
import BarcodeScannerBar from "./BarcodeScannerBar";
import { useOrderContext } from "./OrderContext";
import "./order-workflow.scss";

/**
 * OrderDashboard - Default landing page for "Add Order" menu (DSH-1 to DSH-9)
 *
 * Features:
 * - DSH-1: Shows current user's in-progress orders by default
 * - DSH-2: Search by patient name, lab number, national ID, referring lab number
 * - DSH-3/4: "Include external sources" toggle for EMR/referral orders
 * - DSH-5/6: "+ New Order" button and barcode scan bar
 * - DSH-7/8: Filter dropdowns (Status, date range, Priority)
 * - DSH-9: Pagination (25/50/100 items, default 100)
 */

const ORDER_STEPS = [
  { key: "enter", label: "Enter" },
  { key: "collect", label: "Collect" },
  { key: "label", label: "Label" },
  { key: "qa", label: "QA" },
];

const STATUS_OPTIONS = [
  { id: "all", label: "All Statuses" },
  { id: "in_progress", label: "In Progress" },
  { id: "pending_qa", label: "Pending QA" },
  { id: "completed", label: "Completed" },
  { id: "rejected", label: "Rejected" },
];

const PRIORITY_OPTIONS = [
  { id: "all", label: "All Priorities" },
  { id: "stat", label: "STAT" },
  { id: "routine", label: "Routine" },
];

const PAGE_SIZES = [25, 50, 100];

const OrderDashboardContent = () => {
  const intl = useIntl();
  const history = useHistory();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { loadOrder } = useOrderContext();

  // State
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [includeExternal, setIncludeExternal] = useState(false);
  const [externalCount, setExternalCount] = useState(0);
  const [statusFilter, setStatusFilter] = useState("all");
  const [priorityFilter, setPriorityFilter] = useState("all");
  const [dateRange, setDateRange] = useState({ start: null, end: null });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [totalItems, setTotalItems] = useState(0);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "sidenav.label.addorder", link: "/order" },
  ];

  // Fetch orders
  const fetchOrders = useCallback(() => {
    setIsLoading(true);

    const params = new URLSearchParams({
      page: page.toString(),
      pageSize: pageSize.toString(),
      includeExternal: includeExternal.toString(),
    });

    if (searchQuery) params.append("search", searchQuery);
    if (statusFilter !== "all") params.append("status", statusFilter);
    if (priorityFilter !== "all") params.append("priority", priorityFilter);
    if (dateRange.start) params.append("startDate", dateRange.start);
    if (dateRange.end) params.append("endDate", dateRange.end);

    getFromOpenElisServer(`/rest/order/dashboard?${params}`, (response) => {
      setIsLoading(false);
      if (response) {
        setOrders(response.orders || []);
        setTotalItems(response.totalCount || 0);
        setExternalCount(response.externalCount || 0);
      }
    });
  }, [
    page,
    pageSize,
    searchQuery,
    statusFilter,
    priorityFilter,
    dateRange,
    includeExternal,
  ]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Handlers
  const handleNewOrder = () => {
    history.push("/order/enter");
  };

  const handleContinueOrder = async (order) => {
    // Load the order into context, then navigate to the appropriate step
    try {
      console.log("handleContinueOrder: Loading order", order.labNumber);
      const result = await loadOrder(order.labNumber, false); // false = editable
      console.log("handleContinueOrder: loadOrder result", result);
      const nextStep = getNextStep(order);
      console.log("handleContinueOrder: navigating to", `/order/${nextStep}`);
      history.push(`/order/${nextStep}`);
    } catch (error) {
      console.error("handleContinueOrder: Error loading order", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.load.error",
          defaultMessage: "Failed to load order",
        }),
      });
      setNotificationVisible(true);
    }
  };

  const handleAcceptExternal = async (order) => {
    try {
      await loadOrder(order.labNumber, false);
      history.push("/order/enter");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.accept.error",
          defaultMessage: "Failed to accept order",
        }),
      });
      setNotificationVisible(true);
    }
  };

  const handleFixIssue = async (order) => {
    // Load the order into context, then navigate to the step that needs fixing
    try {
      await loadOrder(order.labNumber, false); // false = editable
      const returnedStep = order.returnedToStep || "enter";
      history.push(`/order/${returnedStep}`);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.load.error",
          defaultMessage: "Failed to load order",
        }),
      });
      setNotificationVisible(true);
    }
  };

  const handleBarcodeOrderLoaded = (order) => {
    history.push(`/order/enter?labNumber=${order.labNumber}`);
  };

  const getNextStep = (order) => {
    if (!order.stepProgress) return "enter";
    if (!order.stepProgress.enter) return "enter";
    if (!order.stepProgress.collect) return "collect";
    if (!order.stepProgress.label) return "label";
    if (!order.stepProgress.qa) return "qa";
    return "qa";
  };

  const getStepProgressValue = (order) => {
    if (!order.stepProgress) return 0;
    let completed = 0;
    if (order.stepProgress.enter) completed++;
    if (order.stepProgress.collect) completed++;
    if (order.stepProgress.label) completed++;
    if (order.stepProgress.qa) completed++;
    return (completed / 4) * 100;
  };

  // Table headers
  const headers = [
    {
      key: "labNumber",
      header: intl.formatMessage({
        id: "order.labNumber",
        defaultMessage: "Lab Number",
      }),
    },
    {
      key: "patient",
      header: intl.formatMessage({
        id: "patient.label",
        defaultMessage: "Patient/Subject",
      }),
    },
    {
      key: "facility",
      header: intl.formatMessage({
        id: "order.facility",
        defaultMessage: "Facility",
      }),
    },
    {
      key: "priority",
      header: intl.formatMessage({
        id: "order.priority",
        defaultMessage: "Priority",
      }),
    },
    {
      key: "progress",
      header: intl.formatMessage({
        id: "order.progress",
        defaultMessage: "Progress",
      }),
    },
    {
      key: "lastUpdated",
      header: intl.formatMessage({
        id: "order.lastUpdated",
        defaultMessage: "Last Updated",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.action",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Transform orders to table rows
  const rows = orders.map((order) => ({
    id: order.id || order.labNumber,
    labNumber: (
      <div className="order-lab-number">
        {order.labNumber}
        {order.isExternal && (
          <Tag type="purple" size="sm" className="external-badge">
            <FormattedMessage id="order.external" defaultMessage="External" />
          </Tag>
        )}
      </div>
    ),
    patient: order.patientName || order.subjectName || "---",
    facility: order.facilityName || "---",
    priority:
      order.priority === "stat" ? (
        <Tag type="red" size="sm">
          STAT
        </Tag>
      ) : (
        <Tag type="gray" size="sm">
          Routine
        </Tag>
      ),
    progress: (
      <div className="order-progress">
        <ProgressBar
          value={getStepProgressValue(order)}
          size="small"
          status={order.status === "rejected" ? "error" : "active"}
          hideLabel
        />
        <span className="progress-label">
          {
            ORDER_STEPS.filter(
              (_, i) => order.stepProgress?.[ORDER_STEPS[i]?.key],
            ).length
          }
          /4
        </span>
      </div>
    ),
    lastUpdated: order.lastUpdated || "---",
    actions: (
      <div className="order-actions">
        {order.returnedFromQA ? (
          <Button
            kind="danger--tertiary"
            size="sm"
            onClick={() => handleFixIssue(order)}
          >
            <FormattedMessage id="order.fixIssue" defaultMessage="Fix Issue" />
          </Button>
        ) : order.isExternal ? (
          <Button
            kind="primary"
            size="sm"
            onClick={() => handleAcceptExternal(order)}
          >
            <FormattedMessage id="order.accept" defaultMessage="Accept" />
          </Button>
        ) : (
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleContinueOrder(order)}
          >
            <FormattedMessage id="order.continue" defaultMessage="Continue" />
          </Button>
        )}
      </div>
    ),
    className: order.returnedFromQA
      ? "returned-from-qa"
      : order.isExternal
        ? "external-order"
        : "",
  }));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {notificationVisible && <AlertDialog />}

      <div className="order-dashboard">
        <Stack gap={5}>
          {/* Header with title and New Order button */}
          <div className="dashboard-header">
            <h2>
              <FormattedMessage
                id="order.dashboard.title"
                defaultMessage="Orders"
              />
            </h2>
            <Button
              kind="primary"
              renderIcon={Add}
              onClick={handleNewOrder}
              className="new-order-btn"
            >
              <FormattedMessage id="order.new" defaultMessage="New Order" />
            </Button>
          </div>

          {/* Barcode Scanner Bar (DSH-5) */}
          <BarcodeScannerBar
            onOrderLoaded={handleBarcodeOrderLoaded}
            className="dashboard-barcode-section"
          />

          {/* Filters Row */}
          <div className="dashboard-filters">
            <Dropdown
              id="status-filter"
              titleText=""
              label={intl.formatMessage({
                id: "order.filter.status",
                defaultMessage: "Status",
              })}
              items={STATUS_OPTIONS}
              itemToString={(item) => item?.label || ""}
              selectedItem={STATUS_OPTIONS.find((s) => s.id === statusFilter)}
              onChange={({ selectedItem }) =>
                setStatusFilter(selectedItem?.id || "all")
              }
            />
            <Dropdown
              id="priority-filter"
              titleText=""
              label={intl.formatMessage({
                id: "order.filter.priority",
                defaultMessage: "Priority",
              })}
              items={PRIORITY_OPTIONS}
              itemToString={(item) => item?.label || ""}
              selectedItem={PRIORITY_OPTIONS.find(
                (p) => p.id === priorityFilter,
              )}
              onChange={({ selectedItem }) =>
                setPriorityFilter(selectedItem?.id || "all")
              }
            />
            <DatePicker
              datePickerType="range"
              onChange={(dates) =>
                setDateRange({ start: dates[0], end: dates[1] })
              }
            >
              <DatePickerInput
                id="date-start"
                placeholder="mm/dd/yyyy"
                labelText=""
                size="md"
              />
              <DatePickerInput
                id="date-end"
                placeholder="mm/dd/yyyy"
                labelText=""
                size="md"
              />
            </DatePicker>
            <Checkbox
              id="include-external"
              labelText={
                externalCount > 0
                  ? intl.formatMessage(
                      {
                        id: "order.includeExternal.count",
                        defaultMessage: "Include external sources ({count})",
                      },
                      { count: externalCount },
                    )
                  : intl.formatMessage({
                      id: "order.includeExternal",
                      defaultMessage: "Include external sources",
                    })
              }
              checked={includeExternal}
              onChange={(_, { checked }) => setIncludeExternal(checked)}
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              onClick={fetchOrders}
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "button.refresh",
                defaultMessage: "Refresh",
              })}
            />
          </div>

          {/* Orders Table */}
          <DataTable rows={rows} headers={headers} isSortable>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getToolbarProps,
              onInputChange,
            }) => (
              <TableContainer>
                <TableToolbar {...getToolbarProps()}>
                  <TableToolbarContent>
                    <TableToolbarSearch
                      placeholder={intl.formatMessage({
                        id: "order.search.placeholder",
                        defaultMessage:
                          "Search by patient, lab number, or ID...",
                      })}
                      onChange={(e) => {
                        onInputChange(e);
                        setSearchQuery(e.target.value);
                      }}
                    />
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
                    {rows.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={headers.length}
                          className="empty-table"
                        >
                          {isLoading ? (
                            <FormattedMessage
                              id="loading"
                              defaultMessage="Loading..."
                            />
                          ) : (
                            <FormattedMessage
                              id="order.dashboard.empty"
                              defaultMessage="No orders found. Click 'New Order' to create one."
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    ) : (
                      rows.map((row) => (
                        <TableRow
                          key={row.id}
                          {...getRowProps({ row })}
                          className={
                            orders.find(
                              (o) => o.id === row.id || o.labNumber === row.id,
                            )?.returnedFromQA
                              ? "returned-from-qa"
                              : ""
                          }
                        >
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>

          {/* Pagination (DSH-9) */}
          <Pagination
            totalItems={totalItems}
            pageSize={pageSize}
            pageSizes={PAGE_SIZES}
            page={page}
            onChange={({ page: newPage, pageSize: newPageSize }) => {
              setPage(newPage);
              setPageSize(newPageSize);
            }}
          />
        </Stack>
      </div>
    </>
  );
};

// OrderDashboard uses the shared OrderProvider from App.js
// Do NOT wrap in OrderProvider here - it would create a separate context
const OrderDashboard = () => <OrderDashboardContent />;

export default OrderDashboard;
