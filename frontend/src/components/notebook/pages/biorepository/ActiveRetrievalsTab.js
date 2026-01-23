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
function ActiveRetrievalsTab({ onActionComplete }) {
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

  // Batch selection state
  const [selectedRows, setSelectedRows] = useState([]);

  // Retrieve form state
  const [conditionAtRelease, setConditionAtRelease] = useState("Good");
  const [conditionNotes, setConditionNotes] = useState("");
  const [temperatureAtRetrieval, setTemperatureAtRetrieval] = useState("");

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
      "/rest/biorepository/retrieval/requests?status=APPROVED,IN_PROGRESS",
      (data) => {
        if (data && Array.isArray(data)) {
          setActiveRequests(data);
        } else if (data && !data.error) {
          setActiveRequests([]);
        }
      },
    );

    setLoading(false);
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

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
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        setRetrieveModalOpen(false);
        setSelectedItem(null);
        resetRetrieveForm();
        loadData();
        if (onActionComplete) onActionComplete();
      },
    );
  }, [
    selectedItem,
    conditionAtRelease,
    conditionNotes,
    temperatureAtRetrieval,
    loadData,
    onActionComplete,
  ]);

  // Handle release action
  const handleRelease = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/release`,
      "{}",
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        setReleaseModalOpen(false);
        setSelectedItem(null);
        loadData();
        if (onActionComplete) onActionComplete();
      },
    );
  }, [selectedItem, loadData, onActionComplete]);

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
        setReturnModalOpen(false);
        setSelectedItem(null);
        resetReturnForm();
        loadData();
        if (onActionComplete) onActionComplete();
      },
    );
  }, [
    selectedItem,
    returnedCondition,
    returnNotes,
    fullyConsumed,
    loadData,
    onActionComplete,
  ]);

  const resetRetrieveForm = () => {
    setConditionAtRelease("Good");
    setConditionNotes("");
    setTemperatureAtRetrieval("");
  };

  const resetReturnForm = () => {
    setReturnedCondition("Good");
    setReturnNotes("");
    setFullyConsumed(false);
  };

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

  // Handle bulk complete
  const handleBulkComplete = useCallback(() => {
    if (selectedRows.length === 0) return;

    setActionLoading(true);
    let completed = 0;
    let failed = 0;

    selectedRows.forEach((rowId) => {
      const request = activeRequests.find((r) => r.id.toString() === rowId);
      if (!request) return;

      postToOpenElisServerJsonResponse(
        `/rest/biorepository/retrieval/requests/${request.id}/complete`,
        "{}",
        (data) => {
          if (data && data.error) {
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
          }
        },
      );
    });
  }, [selectedRows, activeRequests, loadData]);

  // Get item status tag (used in work order modal)
  const getItemStatusTag = (status) => {
    switch (status) {
      case "PENDING":
        return (
          <Tag type="gray" size="sm">
            Pending
          </Tag>
        );
      case "RETRIEVED":
        return (
          <Tag type="blue" size="sm">
            Retrieved
          </Tag>
        );
      case "IN_ANALYSIS":
        return (
          <Tag type="purple" size="sm">
            In Analysis
          </Tag>
        );
      case "RETURNED":
        return (
          <Tag type="green" size="sm">
            Returned
          </Tag>
        );
      case "CONSUMED":
        return (
          <Tag type="teal" size="sm">
            Consumed
          </Tag>
        );
      default:
        return <Tag size="sm">{status}</Tag>;
    }
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
        rows={paginatedData.map((r) => ({
          id: r.id.toString(),
          requestNumber: r.requestNumber || `REQ-${r.id}`,
          workOrderNumber: r.workOrderNumber || "N/A",
          requestedBy: r.requestedByName || "Unknown",
          itemCount: r.totalItemCount || (r.items ? r.items.length : 0),
          status: r.requestStatus,
          _raw: r,
        }))}
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
                      defaultMessage: "Mark as Completed",
                    })}
                    onClick={() => {
                      handleBulkComplete();
                      setSelectedRows(selectedRowIds);
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.markComplete"
                      defaultMessage="Mark as Completed"
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
                                  type={
                                    cell.value === "IN_PROGRESS"
                                      ? "blue"
                                      : "green"
                                  }
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

              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.storageLocations"
                    defaultMessage="Storage Locations"
                  />
                  :
                </strong>
                {selectedRequest.items && selectedRequest.items.length > 0 ? (
                  <DataTable
                    rows={selectedRequest.items.map((item, idx) => ({
                      id: item.id?.toString() || idx.toString(),
                      sample:
                        item.sampleNumber ||
                        item.bioSampleExternalId ||
                        `Sample ${idx + 1}`,
                      location: item.storageLocation || "N/A",
                      coordinates: item.storageCoordinates || "N/A",
                      status: item.itemStatus || "PENDING",
                    }))}
                    headers={[
                      { key: "sample", header: "Sample" },
                      { key: "location", header: "Location" },
                      { key: "coordinates", header: "Coordinates" },
                      { key: "status", header: "Status" },
                    ]}
                    size="sm"
                  >
                    {({
                      rows,
                      headers,
                      getTableProps,
                      getHeaderProps,
                      getRowProps,
                    }) => (
                      <Table
                        {...getTableProps()}
                        style={{ marginTop: "0.5rem" }}
                      >
                        <TableHead>
                          <TableRow>
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
                          {rows.map((row) => (
                            <TableRow {...getRowProps({ row })} key={row.id}>
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "status"
                                    ? getItemStatusTag(cell.value)
                                    : cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    )}
                  </DataTable>
                ) : (
                  <p
                    style={{
                      color: "#525252",
                      fontStyle: "italic",
                      marginTop: "0.5rem",
                    }}
                  >
                    No items available
                  </p>
                )}
              </div>
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
        }}
        onRequestSubmit={handleRelease}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
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
    </div>
  );
}

ActiveRetrievalsTab.propTypes = {
  onActionComplete: PropTypes.func,
};

export default ActiveRetrievalsTab;
