import React, { useState, useCallback, useEffect } from "react";
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
  TableBatchActions,
  TableBatchAction,
  TableSelectAll,
  TableSelectRow,
  Button,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  Checkbox,
  Tag,
  InlineNotification,
  Loading,
  ExpandableSearch,
  Pagination,
} from "@carbon/react";
import { Renew, Catalog, Checkmark } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import BiorepositoryLifecycleModal from "./BiorepositoryLifecycleModal";
import AttachSamplePanel from "./AttachSamplePanel";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";
import { formatBrf02SamplePath } from "./biorepositorySamplePathHelpers";
import { formatRequestedReferenceSummary } from "../common/biorepoRequestReferenceHelpers";
import {
  countUnresolvedReferenceRows,
  findNextAttachTarget,
  getItemDisplayStatus,
  getRequestCompletionBlockReason,
  getRequestDisplayStatus,
  getRequestLineCount,
  resolveItemStatus,
} from "./biorepoRetrievalStatusHelpers";

/**
 * ActiveRetrievalsTab - Track and manage checked-out samples
 *
 * Features:
 * - View approved requests with work orders
 * - Record physical retrieval from storage
 * - Release samples to requester
 * - Process sample returns
 * - Flag overdue items
 */
function ActiveRetrievalsTab({ onActionComplete, refreshToken }) {
  const intl = useIntl();

  // Data state
  const [activeRequests, setActiveRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  // Modal state
  const [selectedItem, setSelectedItem] = useState(null);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [retrieveModalOpen, setRetrieveModalOpen] = useState(false);
  const [releaseModalOpen, setReleaseModalOpen] = useState(false);
  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [workOrderModalOpen, setWorkOrderModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [workOrderLoading, setWorkOrderLoading] = useState(false);
  const [lifecycleModalOpen, setLifecycleModalOpen] = useState(false);
  const [lifecycleContext, setLifecycleContext] = useState(null);

  // Batch selection state
  const [selectedRows, setSelectedRows] = useState([]);

  const [attachTargetItem, setAttachTargetItem] = useState(null);

  // Retrieve form state
  const [conditionAtRelease, setConditionAtRelease] = useState("Good");
  const [conditionNotes, setConditionNotes] = useState("");
  const [temperatureAtRetrieval, setTemperatureAtRetrieval] = useState("");
  const [quantityReleased, setQuantityReleased] = useState("");
  const [receivedByName, setReceivedByName] = useState("");

  // Return form state
  const [returnedCondition, setReturnedCondition] = useState("Good");
  const [returnNotes, setReturnNotes] = useState("");
  const [fullyConsumed, setFullyConsumed] = useState(false);

  // Load active data
  const loadData = useCallback(() => {
    setLoading(true);
    setError(null);

    // Load approved/in-progress requests
    getFromOpenElisServer(
      "/rest/biorepository/retrieval/requests?status=APPROVED,IN_PROGRESS,PARTIALLY_COMPLETED",
      (data) => {
        if (data && Array.isArray(data)) {
          setActiveRequests(data);
        } else if (data && !data.error) {
          setActiveRequests([]);
        }
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, refreshToken]);

  // Filter data
  const filteredRequests = activeRequests.filter((r) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (r.requestNumber && r.requestNumber.toLowerCase().includes(term)) ||
      (r.requestedByName && r.requestedByName.toLowerCase().includes(term)) ||
      (r.workOrderNumber && r.workOrderNumber.toLowerCase().includes(term))
    );
  });

  // Paginate
  const paginatedData = filteredRequests.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  const resetRetrieveForm = () => {
    setConditionAtRelease("Good");
    setConditionNotes("");
    setTemperatureAtRetrieval("");
    setQuantityReleased("");
  };

  const resetReturnForm = () => {
    setReturnedCondition("Good");
    setReturnNotes("");
    setFullyConsumed(false);
  };

  const refreshSelectedRequest = useCallback((requestId) => {
    if (!requestId) return;
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests/${requestId}`,
      (data) => {
        if (data && !data.error) {
          setSelectedRequest(data);
        }
      },
    );
  }, []);

  const afterItemAction = useCallback(
    (requestId) => {
      loadData();
      if (requestId) {
        refreshSelectedRequest(requestId);
      }
      if (onActionComplete) onActionComplete();
    },
    [loadData, refreshSelectedRequest, onActionComplete],
  );

  const openAttachPanel = useCallback((item) => {
    setAttachTargetItem(item);
  }, []);

  const handleAttachSuccess = useCallback(
    (bioSample, errorMessage) => {
      if (errorMessage) {
        setError(errorMessage);
        return;
      }
      const requestId = selectedRequest?.id;
      const currentItemId = attachTargetItem?.id;
      loadData();
      if (requestId) {
        getFromOpenElisServer(
          `/rest/biorepository/retrieval/requests/${requestId}`,
          (data) => {
            if (data && !data.error) {
              setSelectedRequest(data);
              const next = findNextAttachTarget(data, currentItemId);
              setAttachTargetItem(next);
            }
          },
        );
      } else {
        setAttachTargetItem(null);
      }
      if (onActionComplete) onActionComplete();
    },
    [
      selectedRequest?.id,
      attachTargetItem?.id,
      loadData,
      onActionComplete,
    ],
  );

  // Handle retrieve action
  const handleRetrieve = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/retrieve`,
      JSON.stringify({
        conditionAtRelease,
        conditionNotes: conditionNotes || null,
        temperatureAtRetrieval: temperatureAtRetrieval
          ? parseFloat(temperatureAtRetrieval)
          : null,
        quantityReleased: quantityReleased ? parseFloat(quantityReleased) : null,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setRetrieveModalOpen(false);
        setSelectedItem(null);
        resetRetrieveForm();
        afterItemAction(requestId);
      },
    );
  }, [
    selectedItem,
    conditionAtRelease,
    conditionNotes,
    temperatureAtRetrieval,
    quantityReleased,
    afterItemAction,
  ]);

  // Handle release action
  const handleRelease = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/release`,
      JSON.stringify({
        receivedByName: receivedByName || null,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setReleaseModalOpen(false);
        setSelectedItem(null);
        setReceivedByName("");
        afterItemAction(requestId);
      },
    );
  }, [selectedItem, receivedByName, afterItemAction]);

  // Handle return action
  const handleReturn = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/return`,
      JSON.stringify({
        returnedCondition,
        returnNotes: returnNotes || null,
        fullyConsumed,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setReturnModalOpen(false);
        setSelectedItem(null);
        resetReturnForm();
        afterItemAction(requestId);
      },
    );
  }, [
    selectedItem,
    returnedCondition,
    returnNotes,
    fullyConsumed,
    afterItemAction,
  ]);

  const getItemLabel = (item) =>
    item?.sampleNumber ||
    item?.bioSampleExternalId ||
    (item?.sampleItemId ? `Item-${item.sampleItemId}` : `Item-${item?.id}`);

  const openRetrieveModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setConditionAtRelease("Good");
    setConditionNotes("");
    setTemperatureAtRetrieval("");
    setQuantityReleased(
      item.quantityRequested != null ? String(item.quantityRequested) : "",
    );
    setRetrieveModalOpen(true);
  }, []);

  const openReleaseModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setReceivedByName(item.receivedByName || "");
    setReleaseModalOpen(true);
  }, []);

  const openReturnModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setReturnedCondition("Good");
    setReturnNotes("");
    setFullyConsumed(false);
    setReturnModalOpen(true);
  }, []);

  const openLifecycle = useCallback((item, referenceItem) => {
    if (!item) return;
    setLifecycleContext({
      sampleItemId: item.sampleItemId,
      bioSampleId: item.bioSampleId,
      sampleLabel:
        item.sampleNumber ||
        item.bioSampleExternalId ||
        item.externalId ||
        item.barcode ||
        (item.sampleItemId ? `Item-${item.sampleItemId}` : ""),
      retrievalContext: referenceItem
        ? {
            requestedReference: formatRequestedReferenceSummary(referenceItem),
            requestedQuantity: referenceItem.quantityRequested,
            requestedUnit: referenceItem.unitOfMeasure,
            fulfilledSample:
              item.externalId ||
              item.barcode ||
              item.sampleNumber ||
              item.bioSampleExternalId,
            storagePath: item.sourceStoragePath || item.storageLocation,
            quantityReleased: item.quantityReleased,
          }
        : null,
    });
    setLifecycleModalOpen(true);
  }, []);

  const renderItemActions = useCallback(
    (item, referenceItem) => {
      if (!item) return null;
      const status = resolveItemStatus(item);
      const enrichedItem = {
        ...item,
        retrievalRequestId: selectedRequest?.id,
      };

      return (
        <div style={{ display: "flex", gap: "0.25rem", flexWrap: "wrap" }}>
          {status === "AWAITING_FULFILLMENT" && (
            <Button kind="primary" size="sm" onClick={() => openAttachPanel(item)}>
              <FormattedMessage
                id="biorepository.retrieval.attachSample"
                defaultMessage="Attach Sample"
              />
            </Button>
          )}
          {status === "PENDING" && (
            <Button
              kind="primary"
              size="sm"
              onClick={() => openRetrieveModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.retrieve"
                defaultMessage="Retrieve"
              />
            </Button>
          )}
          {status === "RETRIEVED" && (
            <Button
              kind="primary"
              size="sm"
              onClick={() => openReleaseModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.release"
                defaultMessage="Release"
              />
            </Button>
          )}
          {status === "IN_ANALYSIS" && item.returnExpected !== false && (
            <Button
              kind="secondary"
              size="sm"
              onClick={() => openReturnModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.processReturn"
                defaultMessage="Process Return"
              />
            </Button>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => openLifecycle(item, referenceItem)}
          >
            <FormattedMessage
              id="biorepository.lifecycle.view"
              defaultMessage="Lifecycle"
            />
          </Button>
        </div>
      );
    },
    [
      selectedRequest?.id,
      openAttachPanel,
      openRetrieveModal,
      openReleaseModal,
      openReturnModal,
      openLifecycle,
    ],
  );

  // Load full request details with items
  const loadRequestDetails = useCallback((requestId) => {
    setWorkOrderLoading(true);
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests/${requestId}`,
      (data) => {
        setWorkOrderLoading(false);
        if (data && !data.error) {
          setSelectedRequest(data);
          setWorkOrderModalOpen(true);
        } else {
          setError(data?.error || "Failed to load request details");
        }
      },
    );
  }, []);

  const completeRequestAfterRelease = useCallback(
    (requestId) =>
      new Promise((resolve) => {
        getFromOpenElisServer(
          `/rest/biorepository/retrieval/requests/${requestId}`,
          (data) => {
            if (!data || data.error) {
              resolve({ error: data?.error || "Failed to load request" });
              return;
            }

            const blockReason = getRequestCompletionBlockReason(data.items || []);
            if (blockReason) {
              resolve({ error: blockReason });
              return;
            }

            postToOpenElisServerJsonResponse(
              `/rest/biorepository/retrieval/requests/${requestId}/complete`,
              "{}",
              (completeData) => {
                if (completeData && completeData.error) {
                  resolve({ error: completeData.error });
                  return;
                }
                if (data.notebookId) {
                  postToOpenElisServerJsonResponse(
                    `/rest/biorepository/retrieval/requests/${requestId}/link-to-notebook`,
                    JSON.stringify({ notebookId: data.notebookId }),
                    () => resolve({ success: true }),
                  );
                } else {
                  resolve({ success: true });
                }
              },
            );
          },
        );
      }),
    [],
  );

  // Handle bulk complete
  const handleBulkComplete = useCallback(() => {
    if (selectedRows.length === 0) return;

    setActionLoading(true);
    let completed = 0;
    let failed = 0;

    selectedRows.forEach((rowId) => {
      const request = activeRequests.find((r) => r.id.toString() === rowId);
      if (!request) return;

      completeRequestAfterRelease(request.id).then((result) => {
        if (result.error) {
          failed++;
        } else {
          completed++;
        }

        if (completed + failed === selectedRows.length) {
          setActionLoading(false);
          setSelectedRows([]);
          loadData();
          if (failed > 0) {
            setError(`Completed ${completed} request(s). ${failed} failed.`);
          }
          if (onActionComplete) onActionComplete();
        }
      });
    });
  }, [
    selectedRows,
    activeRequests,
    loadData,
    completeRequestAfterRelease,
    onActionComplete,
  ]);

  // Get item status tag (used in work order modal)
  const getItemStatusTag = (item) => {
    const display = getItemDisplayStatus(item, intl);
    return (
      <Tag type={display.tagType} size="sm">
        {display.label}
      </Tag>
    );
  };

  // Request table headers
  const requestHeaders = [
    {
      key: "requestNumber",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestNumber",
        defaultMessage: "Request #",
      }),
    },
    {
      key: "workOrderNumber",
      header: intl.formatMessage({
        id: "biorepository.retrieval.workOrder",
        defaultMessage: "Work Order",
      }),
    },
    {
      key: "requestedBy",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestedBy",
        defaultMessage: "Requested By",
      }),
    },
    {
      key: "itemCount",
      header: intl.formatMessage({
        id: "biorepository.retrieval.items",
        defaultMessage: "Items",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepository.retrieval.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="active-retrievals-tab" style={{ padding: "1rem 0" }}>
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Data Table */}
      <DataTable
        rows={paginatedData.map((r) => {
          const displayStatus = getRequestDisplayStatus(r, intl);
          return {
            id: r.id.toString(),
            requestNumber: r.requestNumber || `REQ-${r.id}`,
            workOrderNumber: r.workOrderNumber || "N/A",
            requestedBy: r.requestedByName || "Unknown",
            itemCount: getRequestLineCount(r),
            status: displayStatus.label,
            statusTagType: displayStatus.tagType,
            _raw: r,
          };
        })}
        headers={requestHeaders}
        size="md"
        radio={false}
        isSortable={false}
      >
        {({
          rows,
          headers,
          getTableProps,
          getHeaderProps,
          getRowProps,
          getSelectionProps,
          getBatchActionProps,
          selectedRows,
        }) => {
          const selectedRowIds = selectedRows.map((row) => row.id);
          return (
            <TableContainer>
              <TableToolbar>
                <TableBatchActions {...getBatchActionProps()}>
                  <TableBatchAction
                    renderIcon={Checkmark}
                    iconDescription={intl.formatMessage({
                      id: "biorepository.retrieval.markComplete",
                      defaultMessage: "Complete After Release",
                    })}
                    onClick={() => {
                      handleBulkComplete();
                      setSelectedRows(selectedRowIds);
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.markComplete"
                      defaultMessage="Complete After Release"
                    />
                  </TableBatchAction>
                </TableBatchActions>
                <TableToolbarContent>
                  <ExpandableSearch
                    labelText={intl.formatMessage({
                      id: "label.search",
                      defaultMessage: "Search",
                    })}
                    placeholder={intl.formatMessage({
                      id: "biorepository.retrieval.search.active",
                      defaultMessage: "Search...",
                    })}
                    value={searchTerm}
                    onChange={(e) => {
                      setSearchTerm(e.target.value);
                      setPage(1);
                    }}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={Renew}
                    iconDescription={intl.formatMessage({
                      id: "label.refresh",
                      defaultMessage: "Refresh",
                    })}
                    hasIconOnly
                    onClick={loadData}
                  />
                </TableToolbarContent>
              </TableToolbar>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    <TableSelectAll {...getSelectionProps()} />
                    {headers.map((header) => (
                      <TableHeader
                        {...getHeaderProps({ header })}
                        key={header.key}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={headers.length + 1}>
                        <div
                          style={{
                            textAlign: "center",
                            padding: "2rem",
                            color: "#525252",
                          }}
                        >
                          <FormattedMessage
                            id="biorepository.retrieval.noActiveRetrievals"
                            defaultMessage="No active retrievals"
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : (
                    rows.map((row) => {
                      // Get raw data from paginatedData
                      const rawData = paginatedData.find(
                        (r) => r.id.toString() === row.id,
                      );

                      if (!rawData) {
                        console.error("Missing rawData for row:", row.id);
                        console.error(
                          "paginatedData IDs:",
                          paginatedData.map((d) => d.id),
                        );
                        console.error("row:", row);
                        return null;
                      }

                      return (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          <TableSelectRow {...getSelectionProps({ row })} />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "status" ? (
                                <Tag
                                  type={rawData ? getRequestDisplayStatus(rawData, intl).tagType : "gray"}
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                <div
                                  style={{ display: "flex", gap: "0.25rem" }}
                                >
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Catalog}
                                    iconDescription={intl.formatMessage({
                                      id: "biorepository.retrieval.viewWorkOrder",
                                      defaultMessage: "View Work Order",
                                    })}
                                    hasIconOnly
                                    onClick={() =>
                                      loadRequestDetails(rawData.id)
                                    }
                                  />
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    data-testid="view-lifecycle-button"
                                    onClick={() =>
                                      openLifecycle(rawData.items?.[0])
                                    }
                                  >
                                    <FormattedMessage
                                      id="biorepository.lifecycle.view"
                                      defaultMessage="View Lifecycle"
                                    />
                                  </Button>
                                </div>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      );
                    })
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          );
        }}
      </DataTable>

      {filteredRequests.length > pageSize && (
        <Pagination
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 20, 50]}
          totalItems={filteredRequests.length}
          onChange={({ page: newPage, pageSize: newPageSize }) => {
            setPage(newPage);
            setPageSize(newPageSize);
          }}
        />
      )}

      {/* Work Order Modal */}
      <Modal
        open={workOrderModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.workOrderDetails",
          defaultMessage: "Work Order Details",
        })}
        passiveModal
        onRequestClose={() => {
          setWorkOrderModalOpen(false);
          setSelectedRequest(null);
          setAttachTargetItem(null);
        }}
        size="lg"
      >
        {workOrderLoading ? (
          <div style={{ padding: "2rem", textAlign: "center" }}>
            <Loading withOverlay={false} small />
          </div>
        ) : (
          selectedRequest && (
            <div style={{ display: "grid", gap: "1rem" }}>
              <div
                style={{
                  display: "grid",
                  gap: "0.5rem",
                  gridTemplateColumns: "1fr 1fr",
                }}
              >
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.workOrder"
                      defaultMessage="Work Order"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.workOrderNumber || "N/A"}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.requestNumber"
                      defaultMessage="Request #"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.requestNumber}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.approvedBy"
                      defaultMessage="Approved By"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.approvedByName || "N/A"}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.approvedAt"
                      defaultMessage="Approved At"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.approvedTimestamp
                    ? new Date(
                        selectedRequest.approvedTimestamp,
                      ).toLocaleString()
                    : "N/A"}
                </div>
              </div>

              {selectedRequest.items && selectedRequest.items.length > 0 && (
                <InlineNotification
                  kind="info"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage(
                    {
                      id: "biorepository.retrieval.workOrder.unresolvedBanner",
                      defaultMessage:
                        "{unresolvedCount} of {requestLineCount} rows awaiting sample match",
                    },
                    {
                      unresolvedCount: countUnresolvedReferenceRows(
                        selectedRequest.items,
                      ),
                      requestLineCount: getRequestLineCount(selectedRequest),
                    },
                  )}
                />
              )}

              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.workOrder.items"
                    defaultMessage="Request Items"
                  />
                  :
                </strong>
                {selectedRequest.items && selectedRequest.items.length > 0 ? (
                  <Table size="sm" style={{ marginTop: "0.5rem" }}>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workOrder.requestedReference"
                            defaultMessage="Requested Reference"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workOrder.qtyRequested"
                            defaultMessage="Qty Requested"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workOrder.attachedSample"
                            defaultMessage="Attached Sample"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepo.import.field.samplePath"
                            defaultMessage="Sample Path (Storage Location)"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.status"
                            defaultMessage="Status"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="label.actions"
                            defaultMessage="Actions"
                          />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {selectedRequest.items.flatMap((item) => {
                        const rows = [
                          { item, isFulfillment: false, referenceItem: null },
                        ];
                        (item.fulfillments || []).forEach((fulfillment) => {
                          rows.push({
                            item: fulfillment,
                            isFulfillment: true,
                            referenceItem: item,
                          });
                        });
                        return rows;
                      }).map(({ item, isFulfillment, referenceItem }, idx) => (
                        <TableRow key={`${item.id}-${idx}`}>
                          <TableCell>
                            {isFulfillment ? (
                              <span
                                style={{
                                  paddingLeft: "1rem",
                                  color: "#525252",
                                  fontStyle: "italic",
                                }}
                              >
                                <FormattedMessage
                                  id="biorepository.retrieval.workOrder.fulfillsRowAbove"
                                  defaultMessage="(fulfills row above)"
                                />
                              </span>
                            ) : (
                              formatRequestedReferenceSummary(item)
                            )}
                          </TableCell>
                          <TableCell>
                            {!isFulfillment && item.quantityRequested != null
                              ? formatQuantityWithUnit(
                                  item.quantityRequested,
                                  item.unitOfMeasure,
                                )
                              : "—"}
                          </TableCell>
                          <TableCell>
                            {isFulfillment
                              ? item.externalId ||
                                item.accessionNumber ||
                                item.barcode ||
                                item.sampleNumber ||
                                item.bioSampleExternalId ||
                                "—"
                              : item.fulfillments?.length > 0
                                ? item.fulfillments
                                    .map(
                                      (f) =>
                                        f.externalId ||
                                        f.accessionNumber ||
                                        f.barcode ||
                                        f.sampleNumber ||
                                        f.bioSampleExternalId,
                                    )
                                    .filter(Boolean)
                                    .join(", ") || "—"
                                : "—"}
                          </TableCell>
                          <TableCell>
                            {isFulfillment
                              ? formatBrf02SamplePath(item) ||
                                item.sourceStoragePath ||
                                item.storageLocation ||
                                "—"
                              : "—"}
                          </TableCell>
                          <TableCell>{getItemStatusTag(item)}</TableCell>
                          <TableCell>
                            {renderItemActions(item, referenceItem)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                ) : (
                  <p
                    style={{
                      color: "#525252",
                      fontStyle: "italic",
                      marginTop: "0.5rem",
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.workOrder.noItems"
                      defaultMessage="No items available"
                    />
                  </p>
                )}
              </div>

              {attachTargetItem && (
                <AttachSamplePanel
                  referenceItem={attachTargetItem}
                  onAttachSuccess={handleAttachSuccess}
                  onCancel={() => setAttachTargetItem(null)}
                />
              )}
            </div>
          )
        )}
      </Modal>

      {/* Retrieve Modal */}
      <Modal
        open={retrieveModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.retrieveSample",
          defaultMessage: "Record Sample Retrieval",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.retrieve",
                defaultMessage: "Retrieve",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setRetrieveModalOpen(false);
          setSelectedItem(null);
          resetRetrieveForm();
        }}
        onRequestSubmit={handleRetrieve}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.retrieve.description"
                defaultMessage="Record physical retrieval of sample {sampleNumber} from storage."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>

            {(selectedItem.quantityRequested != null ||
              selectedItem.availableQuantity != null) && (
              <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                Requested:{" "}
                {formatQuantityWithUnit(
                  selectedItem.quantityRequested,
                  selectedItem.unitOfMeasure,
                )}
                {selectedItem.availableQuantity != null &&
                  ` — Available: ${formatQuantityWithUnit(
                    selectedItem.availableQuantity,
                    selectedItem.unitOfMeasure,
                  )}`}
              </p>
            )}

            <NumberInput
              id="quantityReleased"
              label={intl.formatMessage({
                id: "biorepository.retrieval.quantityReleased",
                defaultMessage: "Quantity to release",
              })}
              helperText={intl.formatMessage({
                id: "biorepository.retrieval.quantityReleased.helper",
                defaultMessage: "Defaults to requested quantity if left blank",
              })}
              value={quantityReleased}
              onChange={(e, { value }) => setQuantityReleased(value)}
              min={0}
              step={0.0001}
              allowEmpty
            />

            <Select
              id="conditionAtRelease"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.conditionAtRelease",
                defaultMessage: "Condition at Retrieval",
              })}
              value={conditionAtRelease}
              onChange={(e) => setConditionAtRelease(e.target.value)}
            >
              <SelectItem value="Good" text="Good" />
              <SelectItem value="Thawed" text="Thawed" />
              <SelectItem value="Partially Thawed" text="Partially Thawed" />
              <SelectItem value="Damaged" text="Damaged" />
              <SelectItem value="Other" text="Other" />
            </Select>

            <TextArea
              id="conditionNotes"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.conditionNotes",
                defaultMessage: "Condition Notes (Optional)",
              })}
              value={conditionNotes}
              onChange={(e) => setConditionNotes(e.target.value)}
            />

            <NumberInput
              id="temperatureAtRetrieval"
              label={intl.formatMessage({
                id: "biorepository.retrieval.temperature",
                defaultMessage: "Temperature at Retrieval (°C, Optional)",
              })}
              value={temperatureAtRetrieval}
              onChange={(e, { value }) => setTemperatureAtRetrieval(value)}
              min={-200}
              max={50}
              step={0.1}
              allowEmpty
            />
          </div>
        )}
      </Modal>

      {/* Release Modal */}
      <Modal
        open={releaseModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.releaseSample",
          defaultMessage: "Release Sample to Requester",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.release",
                defaultMessage: "Release",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setReleaseModalOpen(false);
          setSelectedItem(null);
          setReceivedByName("");
        }}
        onRequestSubmit={handleRelease}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.release.confirmation"
                defaultMessage="Confirm release of sample {sampleNumber} to requester. The sample will be marked as 'In Analysis'."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>
            <TextInput
              id="receivedByName"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.receivedByName",
                defaultMessage: "Received by",
              })}
              value={receivedByName}
              onChange={(e) => setReceivedByName(e.target.value)}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.receivedByName.placeholder",
                defaultMessage: "Enter receiver name",
              })}
            />
          </div>
        )}
      </Modal>

      {/* Return Modal */}
      <Modal
        open={returnModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.returnSample",
          defaultMessage: "Process Sample Return",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.processReturn",
                defaultMessage: "Process Return",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setReturnModalOpen(false);
          setSelectedItem(null);
          resetReturnForm();
        }}
        onRequestSubmit={handleReturn}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.return.description"
                defaultMessage="Process return of sample {sampleNumber}."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>

            <Select
              id="returnedCondition"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.returnedCondition",
                defaultMessage: "Returned Condition",
              })}
              value={returnedCondition}
              onChange={(e) => setReturnedCondition(e.target.value)}
            >
              <SelectItem value="Good" text="Good" />
              <SelectItem value="Reduced Volume" text="Reduced Volume" />
              <SelectItem value="Thawed" text="Thawed" />
              <SelectItem value="Damaged" text="Damaged" />
              <SelectItem value="Other" text="Other" />
            </Select>

            <TextArea
              id="returnNotes"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.returnNotes",
                defaultMessage: "Return Notes (Optional)",
              })}
              value={returnNotes}
              onChange={(e) => setReturnNotes(e.target.value)}
            />

            <Checkbox
              id="fullyConsumed"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.fullyConsumed",
                defaultMessage:
                  "Sample was fully consumed (will not be returned to storage)",
              })}
              checked={fullyConsumed}
              onChange={(_, { checked }) => setFullyConsumed(checked)}
            />
          </div>
        )}
      </Modal>

      <BiorepositoryLifecycleModal
        open={lifecycleModalOpen}
        onClose={() => {
          setLifecycleModalOpen(false);
          setLifecycleContext(null);
        }}
        sampleItemId={lifecycleContext?.sampleItemId}
        bioSampleId={lifecycleContext?.bioSampleId}
        sampleLabel={lifecycleContext?.sampleLabel}
        retrievalContext={lifecycleContext?.retrievalContext}
      />
    </div>
  );
}

ActiveRetrievalsTab.propTypes = {
  onActionComplete: PropTypes.func,
  refreshToken: PropTypes.number,
};

export default ActiveRetrievalsTab;
