import React, {
  useMemo,
  useState,
  useEffect,
  useContext,
  useCallback,
  useRef,
} from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useWorkflowPrefix } from "./OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import {
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextInput,
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
  Dropdown,
  DatePicker,
  DatePickerInput,
  Pagination,
  ProgressBar,
  Stack,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { localizeServerMessage } from "./SaveFailureNotice";
import {
  serverPageArrowsProps,
  serverPageSizeOf,
  serverPaginationProps,
} from "../utils/serverPaging";
import ServerPageArrows from "../common/ServerPageArrows";
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
 * - DSH-9: Pagination, one server page at a time (paging.results.pageSize)
 */

// The filter lists are built per render so their labels come from the
// message bundle (no hard-coded text), like every other label on the page.
const statusOptions = (intl) => [
  {
    id: "all",
    label: intl.formatMessage({ id: "order.dashboard.status.all" }),
  },
  {
    id: "in_progress",
    label: intl.formatMessage({ id: "order.dashboard.status.inProgress" }),
  },
  {
    id: "pending_qa",
    label: intl.formatMessage({ id: "order.dashboard.status.pendingQa" }),
  },
  {
    id: "completed",
    label: intl.formatMessage({ id: "order.dashboard.status.completed" }),
  },
  // Driven by the order's referrals rather than a sample status column: the
  // FHIR-aligned ReferralStatus already models the lifecycle (OGC-1201 U).
  // "Has referred tests" is any open referral; "Referred Out" is every test.
  {
    id: "has_referred",
    label: intl.formatMessage({ id: "order.dashboard.status.hasReferred" }),
  },
  {
    id: "referred_out",
    label: intl.formatMessage({ id: "order.dashboard.status.referredOut" }),
  },
  // Cancelled orders are hidden from every other filter (FR-A4).
  {
    id: "cancelled",
    label: intl.formatMessage({ id: "order.dashboard.filter.cancelled" }),
  },
];

const PROGRESS_STATUS_TAG = {
  ENTERED: { type: "blue", labelId: "order.status.entered" },
  SAMPLES_PREPARED: { type: "teal", labelId: "order.status.samplesPrepared" },
  READY_FOR_TESTING: {
    type: "green",
    labelId: "order.status.readyForTesting",
  },
  CANCELLED: { type: "gray", labelId: "order.status.cancelled" },
};

const CANCEL_REASON_CATEGORY = "Order cancel reasons";

const priorityOptions = (intl) => [
  {
    id: "all",
    label: intl.formatMessage({ id: "order.dashboard.priority.all" }),
  },
  {
    id: "stat",
    label: intl.formatMessage({ id: "order.dashboard.priority.stat" }),
  },
  {
    id: "asap",
    label: intl.formatMessage({ id: "order.dashboard.priority.asap" }),
  },
  {
    id: "timed",
    label: intl.formatMessage({ id: "order.dashboard.priority.timed" }),
  },
  {
    id: "routine",
    label: intl.formatMessage({ id: "order.dashboard.priority.routine" }),
  },
];

const OrderDashboardContent = () => {
  const intl = useIntl();
  const STATUS_OPTIONS = useMemo(() => statusOptions(intl), [intl]);
  const PRIORITY_OPTIONS = useMemo(() => priorityOptions(intl), [intl]);
  const history = useHistory();
  const location = useLocation();
  const workflowPrefix = useWorkflowPrefix();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { loadOrder, resetOrder } = useOrderContext();

  // State
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [priorityFilter, setPriorityFilter] = useState("all");
  const [dateRange, setDateRange] = useState({ start: null, end: null });
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState();
  const [serverPageSize, setServerPageSize] = useState();

  const workflow = workflowPrefix.split("/").pop(); // "clinical" | "environmental" | "vector"
  const isEnvOrVector = workflow === "environmental" || workflow === "vector";

  const workflowLabel = {
    vector: "sidenav.label.vector.order",
    environmental: "sidenav.label.environmental.order",
    clinical: "sidenav.label.clinical.order",
  }[workflow];

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: workflowLabel, link: workflowPrefix },
  ];

  // Identifies the load in flight, so a superseded response is dropped.
  const latestRequest = useRef(0);

  const applyPage = useCallback((requestId, response) => {
    if (requestId !== latestRequest.current) {
      return;
    }
    setIsLoading(false);
    if (response) {
      const pageOrders = response.orders || [];
      setOrders(pageOrders);
      setPaging(response.paging);
      setServerPageSize((previous) =>
        serverPageSizeOf(response.paging, pageOrders.length, previous),
      );
    }
  }, []);

  // A new search: the server matches every order against the filters, caches
  // the list and answers with its first page.
  const fetchOrders = useCallback(() => {
    const requestId = ++latestRequest.current;
    setIsLoading(true);

    const params = new URLSearchParams({
      workflowType: workflow,
    });

    if (searchQuery) params.append("search", searchQuery);
    if (statusFilter !== "all") params.append("status", statusFilter);
    if (priorityFilter !== "all") params.append("priority", priorityFilter);
    // Format dates as YYYY-MM-DD using local date parts to avoid UTC timezone shift
    const toLocalIso = (d) => {
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    };
    if (dateRange.start)
      params.append("startDate", toLocalIso(new Date(dateRange.start)));
    if (dateRange.end)
      params.append("endDate", toLocalIso(new Date(dateRange.end)));

    getFromOpenElisServer(`/rest/order/dashboard?${params}`, (response) =>
      applyPage(requestId, response),
    );
  }, [
    workflow,
    searchQuery,
    statusFilter,
    priorityFilter,
    dateRange,
    applyPage,
  ]);

  /** One server page of the last search, the same request for the arrows and for Carbon. */
  const loadPage = useCallback(
    (pageNumber) => {
      const requestId = ++latestRequest.current;
      setIsLoading(true);
      getFromOpenElisServer(
        `/rest/order/dashboard?page=${pageNumber}`,
        (response) => applyPage(requestId, response),
      );
    },
    [applyPage],
  );

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  /** A filter change is a new search, which the server answers from page 1. */
  const applyFilter = (setFilter) => (value) => {
    setFilter(value);
  };

  const arrows = serverPageArrowsProps({ paging, onPageRequest: loadPage });

  // Handlers
  const handleNewOrder = () => {
    resetOrder();
    history.push(`${workflowPrefix}/enter`);
  };

  const handleContinueOrder = async (order) => {
    // Load the order into context, then navigate to the appropriate step.
    // Include ?order= so a refresh reloads the order automatically.
    try {
      await loadOrder(order.labNumber, false); // false = editable
      const nextStep = getNextStep(order);
      history.push(
        `${workflowPrefix}/${nextStep}?order=${encodeURIComponent(order.labNumber)}`,
      );
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
      history.push(
        `${workflowPrefix}/enter?order=${encodeURIComponent(order.labNumber)}`,
      );
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
    // Load the order into context, then navigate to the step that needs fixing.
    try {
      await loadOrder(order.labNumber, false); // false = editable
      const returnedStep = order.returnedToStep || "enter";
      history.push(
        `${workflowPrefix}/${returnedStep}?order=${encodeURIComponent(order.labNumber)}`,
      );
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
    history.push(`${workflowPrefix}/enter?labNumber=${order.labNumber}`);
  };

  // A clinical order goes Enter Order, Prepare Samples and, when the
  // laboratory uses it, Sample check; a row without a stored workflow type is
  // a clinical order saved before the types were recorded.
  const isClinicalOrder = (order) =>
    !order.workflowType || order.workflowType === "clinical";

  const getNextStep = (order) => {
    if (!order.stepProgress) return "enter";
    if (!order.stepProgress.enter) return "enter";
    if (isClinicalOrder(order)) {
      if (!(order.stepProgress.collect && order.stepProgress.label)) {
        return "collect";
      }
      return order.sampleCheckEnabled === false ? "collect" : "qa";
    }
    if (!isLabelStepComplete(order)) return "label";
    return "qa";
  };

  // Check if label step is complete based on storage or storageSkipped
  const isLabelStepComplete = (order) => {
    // Check if storageSkipped is set from backend
    const storageSkipped = order.storageSkipped === true;

    // Check if all samples have storage assigned
    const allHaveStorage =
      order.samples?.length > 0 &&
      order.samples.every((s) => s.storageLocationId);

    return allHaveStorage || storageSkipped || order.stepProgress?.label;
  };

  const getTotalSteps = (order) => {
    if (isClinicalOrder(order)) {
      return order.sampleCheckEnabled === false ? 2 : 3;
    }
    return order.workflowType === "vector" ? 4 : 3;
  };

  const getCompletedStepsCount = (order) => {
    if (!order.stepProgress) return 0;
    let completed = 0;
    if (order.stepProgress.enter) completed++;
    if (isClinicalOrder(order)) {
      if (order.stepProgress.collect && order.stepProgress.label) completed++;
      if (order.sampleCheckEnabled !== false && order.stepProgress.qa)
        completed++;
      return completed;
    }
    if (isLabelStepComplete(order)) completed++;
    if (order.stepProgress.qa) completed++;
    if (order.workflowType === "vector" && order.stepProgress.qa) completed++;
    return completed;
  };

  const getStepProgressValue = (order) =>
    (getCompletedStepsCount(order) / getTotalSteps(order)) * 100;

  const orderIsOpen = (order) =>
    order.progressStatus !== "CANCELLED" && !order.complete;

  // Cancel order (FR-A4): a reason from the laboratory's list, or Other with
  // free text; nothing is deleted.
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReasons, setCancelReasons] = useState([]);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelOther, setCancelOther] = useState("");
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/dictionary/categories/${encodeURIComponent(CANCEL_REASON_CATEGORY)}/entries`,
      (entries) => setCancelReasons(Array.isArray(entries) ? entries : []),
    );
  }, []);

  const openCancel = (order) => {
    setCancelTarget(order);
    setCancelReason("");
    setCancelOther("");
  };

  const cancelReasonText =
    cancelReason === "__other__" ? cancelOther.trim() : cancelReason;

  const confirmCancel = async () => {
    if (!cancelTarget || !cancelReasonText) return;
    setCancelling(true);
    postToOpenElisServerJsonResponse(
      "/rest/order/cancel",
      JSON.stringify({
        labNumber: cancelTarget.labNumber,
        reason: cancelReasonText,
      }),
      (response) => {
        setCancelling(false);
        if (response && !response.error && response.progressStatus) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "order.dashboard.cancelled" },
              { labNo: cancelTarget.labNumber },
            ),
          });
          setNotificationVisible(true);
          setCancelTarget(null);
          fetchOrders();
          return;
        }
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: response?.error
            ? localizeServerMessage(intl, response.error)
            : intl.formatMessage({ id: "server.error.msg" }),
        });
        setNotificationVisible(true);
      },
    );
  };

  // Arriving from Save and exit highlights the order; from Save and finish it
  // names the completed order (FR-A2, FR-K15).
  const arrival = new URLSearchParams(location.search);
  const highlightLabNo = arrival.get("highlight") || arrival.get("done") || "";
  const finishedLabNo = arrival.get("done") || "";

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
      header: isEnvOrVector
        ? intl.formatMessage({
            id: "order.dashboard.samplingSite",
            defaultMessage: "Sampling Site",
          })
        : intl.formatMessage({
            id: "patient.label",
            defaultMessage: "Patient/Subject",
          }),
    },
    ...(!isEnvOrVector
      ? [
          {
            key: "facility",
            header: intl.formatMessage({
              id: "order.facility",
              defaultMessage: "Facility",
            }),
          },
        ]
      : []),
    {
      key: "priority",
      header: intl.formatMessage({
        id: "order.priority",
        defaultMessage: "Priority",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "status",
        defaultMessage: "Status",
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
    patient: isEnvOrVector
      ? order.samplingSiteName || "---"
      : order.patientName || order.subjectName || "---",
    ...(!isEnvOrVector ? { facility: order.facilityName || "---" } : {}),
    priority: (() => {
      const p = order.priority?.toLowerCase();
      if (p === "stat") {
        return (
          <Tag type="red" size="sm">
            STAT
          </Tag>
        );
      } else if (p === "asap") {
        return (
          <Tag type="magenta" size="sm">
            ASAP
          </Tag>
        );
      } else if (p === "timed") {
        return (
          <Tag type="blue" size="sm">
            Timed
          </Tag>
        );
      } else {
        return (
          <Tag type="gray" size="sm">
            Routine
          </Tag>
        );
      }
    })(),
    status: (() => {
      const tag = PROGRESS_STATUS_TAG[order.progressStatus];
      if (!tag) {
        return "---";
      }
      const referredOut = order.status === "referred_out";
      const completeLabel =
        order.complete && order.progressStatus !== "CANCELLED"
          ? intl.formatMessage({ id: "order.status.complete" })
          : null;
      const label = referredOut
        ? intl.formatMessage({ id: "order.status.referredOut" })
        : completeLabel && order.progressStatus === "SAMPLES_PREPARED"
          ? completeLabel
          : intl.formatMessage({ id: tag.labelId });
      const summary = order.referralSummary;
      const referralLine =
        summary && summary.referredTests > 0
          ? intl.formatMessage(
              {
                id:
                  summary.referredTests >= summary.totalTests
                    ? "order.referral.fully"
                    : "order.referral.partial",
              },
              {
                referred: summary.referredTests,
                total: summary.totalTests,
                lab: summary.referredTo,
              },
            )
          : null;
      return (
        <div className="order-status-cell">
          <Tag type={referredOut ? "purple" : tag.type} size="sm">
            {label}
          </Tag>
          {referralLine ? (
            <div
              className="order-referral-line"
              data-testid="order-referral-line"
            >
              {referralLine}
            </div>
          ) : null}
        </div>
      );
    })(),
    progress: (
      <div className="order-progress">
        <ProgressBar
          value={getStepProgressValue(order)}
          size="small"
          status={order.status === "rejected" ? "error" : "active"}
          label={intl.formatMessage({ id: "order.progress" })}
          hideLabel
        />
        <span className="progress-label">
          {getCompletedStepsCount(order)}/{getTotalSteps(order)}
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
        ) : orderIsOpen(order) ? (
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleContinueOrder(order)}
          >
            <FormattedMessage id="order.continue" defaultMessage="Continue" />
          </Button>
        ) : (
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleAcceptExternal(order)}
          >
            <FormattedMessage id="order.dashboard.open" defaultMessage="Open" />
          </Button>
        )}
        {orderIsOpen(order) && !order.isExternal && (
          <Button
            kind="danger--ghost"
            size="sm"
            onClick={() => openCancel(order)}
          >
            <FormattedMessage id="order.dashboard.cancelOrder" />
          </Button>
        )}
      </div>
    ),
  }));

  // Carbon's DataTable hands back only the cells, so the row classes (a
  // returned order, an external one, the order the user just left) are looked
  // up by row id when the row is drawn.
  const rowClassNames = Object.fromEntries(
    orders.map((order) => [
      order.id || order.labNumber,
      [
        order.returnedFromQA
          ? "returned-from-qa"
          : order.isExternal
            ? "external-order"
            : "",
        highlightLabNo && order.labNumber === highlightLabNo
          ? "order-highlighted"
          : "",
      ]
        .filter(Boolean)
        .join(" "),
    ]),
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {notificationVisible && <AlertDialog />}

      <Modal
        open={Boolean(cancelTarget)}
        danger
        size="sm"
        modalHeading={intl.formatMessage(
          { id: "order.dashboard.cancel.confirm" },
          { labNo: cancelTarget?.labNumber || "" },
        )}
        primaryButtonText={intl.formatMessage({
          id: "order.dashboard.cancelOrder",
        })}
        primaryButtonDisabled={!cancelReasonText || cancelling}
        secondaryButtonText={intl.formatMessage({ id: "common.stay" })}
        onRequestClose={() => setCancelTarget(null)}
        onRequestSubmit={confirmCancel}
      >
        <p>{intl.formatMessage({ id: "order.dashboard.cancel.body" })}</p>
        <Select
          id="cancel-order-reason"
          labelText={intl.formatMessage({
            id: "order.dashboard.cancel.reason",
          })}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
        >
          <SelectItem value="" text="" />
          {cancelReasons
            .filter((entry) => entry.label !== "Other")
            .map((entry) => (
              <SelectItem
                key={entry.code}
                value={entry.label}
                text={entry.label}
              />
            ))}
          <SelectItem
            value="__other__"
            text={intl.formatMessage({ id: "order.dashboard.cancel.other" })}
          />
        </Select>
        {cancelReason === "__other__" && (
          <TextInput
            id="cancel-order-reason-other"
            labelText={intl.formatMessage({
              id: "order.dashboard.cancel.reasonOther",
            })}
            value={cancelOther}
            onChange={(e) => setCancelOther(e.target.value)}
            maxLength={255}
          />
        )}
      </Modal>

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

          {finishedLabNo && (
            <InlineNotification
              kind="success"
              lowContrast
              hideCloseButton
              data-testid="order-finished-notice"
              title={intl.formatMessage(
                { id: "order.finish.title" },
                { labNo: finishedLabNo },
              )}
              subtitle={intl.formatMessage({ id: "order.finish.subtitle" })}
            />
          )}

          {/* Barcode Scanner Bar (DSH-5) */}
          <BarcodeScannerBar
            onOrderLoaded={handleBarcodeOrderLoaded}
            className="dashboard-barcode-section"
          />

          {/* Filters Row */}
          <div className="dashboard-filters">
            <div className="dashboard-filter-item">
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
                  applyFilter(setStatusFilter)(selectedItem?.id || "all")
                }
              />
            </div>
            <div className="dashboard-filter-item">
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
                  applyFilter(setPriorityFilter)(selectedItem?.id || "all")
                }
              />
            </div>
            <div className="dashboard-filter-item">
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  applyFilter(setDateRange)((prev) => ({
                    ...prev,
                    start: dates[0],
                  }))
                }
              >
                <DatePickerInput
                  id="date-start"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({
                    id: "order.filter.dateFrom",
                    defaultMessage: "From",
                  })}
                  size="md"
                />
              </DatePicker>
            </div>
            <div className="dashboard-filter-item">
              <DatePicker
                datePickerType="single"
                onChange={(dates) =>
                  applyFilter(setDateRange)((prev) => ({
                    ...prev,
                    end: dates[0],
                  }))
                }
              >
                <DatePickerInput
                  id="date-end"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({
                    id: "order.filter.dateTo",
                    defaultMessage: "To",
                  })}
                  size="md"
                />
              </DatePicker>
            </div>
          </div>

          {/* Orders Table */}
          {arrows.show && <ServerPageArrows {...arrows} />}
          <DataTable rows={rows} headers={headers} isSortable>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getToolbarProps,
            }) => (
              <TableContainer>
                <TableToolbar {...getToolbarProps()}>
                  <TableToolbarContent>
                    <TableToolbarSearch
                      placeholder={intl.formatMessage(
                        isEnvOrVector
                          ? {
                              id: "order.search.placeholder.env",
                              defaultMessage:
                                "Search by site name or lab number...",
                            }
                          : {
                              id: "order.search.placeholder",
                              defaultMessage:
                                "Search by patient, lab number, or ID...",
                            },
                      )}
                      // The server answers the search (patient, lab number,
                      // identifiers); Carbon's own text filter is not used
                      // because the cells are rendered elements it cannot
                      // read, which hid every matching row.
                      onChange={(e) =>
                        applyFilter(setSearchQuery)(e.target.value)
                      }
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
                          className={rowClassNames[row.id] || ""}
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

          {/* Pagination (DSH-9): Carbon's page is the server's page */}
          <Pagination
            {...serverPaginationProps({
              paging,
              rowsOnPage: orders.length,
              pageSize: serverPageSize,
              onPageRequest: loadPage,
              intl,
            })}
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
