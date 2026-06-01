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
  Button,
  Modal,
  Tag,
  InlineNotification,
  Loading,
  ExpandableSearch,
  Pagination,
} from "@carbon/react";
import { Renew, Catalog, View } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../../utils/Utils";
import BiorepositoryLifecycleModal from "./BiorepositoryLifecycleModal";
import { formatRequestedReferenceSummary } from "../common/biorepoRequestReferenceHelpers";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";
import {
  getItemDisplayStatus,
  getRequestDisplayStatus,
  getRequestLineCount,
  resolveItemStatus,
} from "./biorepoRetrievalStatusHelpers";

/**
 * RetrievalHistoryTab - View completed and cancelled retrieval requests
 *
 * Features:
 * - View historical retrieval requests
 * - Filter by status (completed/cancelled)
 * - View request details
 * - Search functionality
 */
function RetrievalHistoryTab({ onActionComplete, refreshToken }) {
  const intl = useIntl();

  // Data state
  const [historicalRequests, setHistoricalRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  // Modal state
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [lifecycleModalOpen, setLifecycleModalOpen] = useState(false);
  const [lifecycleContext, setLifecycleContext] = useState(null);

  // Load historical data
  const loadData = useCallback(() => {
    setLoading(true);
    setError(null);

    // Load completed and cancelled requests
    getFromOpenElisServer(
      "/rest/biorepository/retrieval/requests?status=COMPLETED,CANCELLED,REJECTED",
      (data) => {
        if (data && Array.isArray(data)) {
          setHistoricalRequests(data);
        } else if (data && !data.error) {
          setError("Invalid data format received");
        } else if (data && data.error) {
          setError(data.error);
        }
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, refreshToken]);

  // Filter requests by search term
  const filteredRequests = historicalRequests.filter((req) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      req.requestNumber?.toLowerCase().includes(search) ||
      req.requestPurpose?.toLowerCase().includes(search) ||
      req.requestedByName?.toLowerCase().includes(search) ||
      req.projectName?.toLowerCase().includes(search)
    );
  });

  // Paginate results
  const totalItems = filteredRequests.length;
  const startIndex = (page - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedRequests = filteredRequests.slice(startIndex, endIndex);

  // View request details
  const handleViewDetails = (requestId) => {
    setDetailsLoading(true);
    setDetailsModalOpen(true);

    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests/${requestId}`,
      (data) => {
        if (data && !data.error) {
          setSelectedRequest(data);
        } else {
          setError(
            data?.error || "Failed to load request details. Please try again.",
          );
          setDetailsModalOpen(false);
        }
        setDetailsLoading(false);
      },
    );
  };

  const openLifecycle = (item) => {
    if (!item) return;
    setLifecycleContext({
      sampleItemId: item.sampleItemId,
      bioSampleId: item.bioSampleId,
      sampleLabel:
        item.sampleNumber ||
        item.bioSampleExternalId ||
        (item.sampleItemId ? `Item-${item.sampleItemId}` : ""),
    });
    setLifecycleModalOpen(true);
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // Get status tag
  const getStatusTag = (status, request) => {
    if (request) {
      const display = getRequestDisplayStatus(request, intl);
      return (
        <Tag type={display.tagType} size="sm">
          {display.label}
        </Tag>
      );
    }

    if (status === "COMPLETED") {
      return (
        <Tag type="green" size="sm">
          {intl.formatMessage({
            id: "biorepository.retrieval.status.completed",
            defaultMessage: "Completed",
          })}
        </Tag>
      );
    }
    if (status === "CANCELLED") {
      return (
        <Tag type="gray" size="sm">
          {intl.formatMessage({
            id: "biorepository.retrieval.status.cancelled",
            defaultMessage: "Cancelled",
          })}
        </Tag>
      );
    }
    if (status === "REJECTED") {
      return (
        <Tag type="red" size="sm">
          {intl.formatMessage({
            id: "biorepository.retrieval.status.rejected",
            defaultMessage: "Rejected",
          })}
        </Tag>
      );
    }
    return <Tag size="sm">{status}</Tag>;
  };

  // Table headers
  const headers = [
    {
      key: "requestNumber",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestNumber",
        defaultMessage: "Request #",
      }),
    },
    {
      key: "requestPurpose",
      header: intl.formatMessage({
        id: "biorepository.retrieval.purpose",
        defaultMessage: "Purpose",
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
      key: "requestedDate",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestedDate",
        defaultMessage: "Requested Date",
      }),
    },
    {
      key: "completedDate",
      header: intl.formatMessage({
        id: "biorepository.retrieval.completedDate",
        defaultMessage: "Completed Date",
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
  ];

  // Map requests to table rows
  const rows = paginatedRequests.map((req) => ({
    id: req.id.toString(),
    requestNumber: req.requestNumber || "-",
    requestPurpose: req.requestPurpose || "-",
    requestedBy: req.requestedByName || "-",
    requestedDate: formatDate(req.requestedTimestamp),
    completedDate: formatDate(req.lastUpdated),
    itemCount: getRequestLineCount(req),
    status: getStatusTag(req.status, req),
  }));

  return (
    <div className="retrieval-history-tab">
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.retrieval.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      <DataTable rows={rows} headers={headers} isSortable>
        {({
          rows,
          headers,
          getHeaderProps,
          getTableProps,
          getRowProps,
          getTableContainerProps,
        }) => (
          <TableContainer
            title={intl.formatMessage({
              id: "biorepository.retrieval.history.title",
              defaultMessage: "Retrieval History",
            })}
            description={intl.formatMessage({
              id: "biorepository.retrieval.history.description",
              defaultMessage: "View completed and cancelled retrieval requests",
            })}
            {...getTableContainerProps()}
          >
            <TableToolbar>
              <TableToolbarContent>
                <ExpandableSearch
                  labelText={intl.formatMessage({
                    id: "biorepository.retrieval.search",
                    defaultMessage: "Search",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.retrieval.search.placeholder",
                    defaultMessage:
                      "Search by request number, purpose, or requester...",
                  })}
                  value={searchTerm}
                  onChange={(e) => {
                    setSearchTerm(e.target.value);
                    setPage(1); // Reset to first page on search
                  }}
                  size="lg"
                />
                <Button
                  kind="ghost"
                  renderIcon={Renew}
                  iconDescription={intl.formatMessage({
                    id: "biorepository.retrieval.refresh",
                    defaultMessage: "Refresh",
                  })}
                  onClick={loadData}
                  size="sm"
                >
                  {intl.formatMessage({
                    id: "biorepository.retrieval.refresh",
                    defaultMessage: "Refresh",
                  })}
                </Button>
              </TableToolbarContent>
            </TableToolbar>

            {loading ? (
              <Loading
                description={intl.formatMessage({
                  id: "biorepository.retrieval.loading",
                  defaultMessage: "Loading...",
                })}
              />
            ) : rows.length === 0 ? (
              <div style={{ padding: "2rem", textAlign: "center" }}>
                <p>
                  {searchTerm
                    ? intl.formatMessage({
                        id: "biorepository.retrieval.noResults",
                        defaultMessage: "No requests match your search",
                      })
                    : intl.formatMessage({
                        id: "biorepository.retrieval.history.empty",
                        defaultMessage: "No historical requests found",
                      })}
                </p>
              </div>
            ) : (
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
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row, idx) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={View}
                          iconDescription={intl.formatMessage({
                            id: "biorepository.retrieval.viewDetails",
                            defaultMessage: "View Details",
                          })}
                          onClick={() =>
                            handleViewDetails(paginatedRequests[idx].id)
                          }
                        >
                          {intl.formatMessage({
                            id: "biorepository.retrieval.viewDetails",
                            defaultMessage: "View Details",
                          })}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}

            {!loading && rows.length > 0 && (
              <Pagination
                totalItems={totalItems}
                page={page}
                pageSize={pageSize}
                pageSizes={[10, 20, 50]}
                onChange={({ page, pageSize }) => {
                  setPage(page);
                  setPageSize(pageSize);
                }}
              />
            )}
          </TableContainer>
        )}
      </DataTable>

      {/* Details Modal */}
      <Modal
        open={detailsModalOpen}
        onRequestClose={() => {
          setDetailsModalOpen(false);
          setSelectedRequest(null);
        }}
        modalHeading={
          selectedRequest
            ? `${intl.formatMessage({
                id: "biorepository.retrieval.requestDetails",
                defaultMessage: "Request Details",
              })} - ${selectedRequest.requestNumber}`
            : intl.formatMessage({
                id: "biorepository.retrieval.requestDetails",
                defaultMessage: "Request Details",
              })
        }
        passiveModal
        size="lg"
      >
        {detailsLoading ? (
          <Loading
            description={intl.formatMessage({
              id: "biorepository.retrieval.loading",
              defaultMessage: "Loading...",
            })}
          />
        ) : selectedRequest ? (
          <div style={{ marginTop: "1rem" }}>
            {/* Request Information */}
            <div
              style={{
                marginBottom: "1.5rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem" }}>
                {intl.formatMessage({
                  id: "biorepository.retrieval.requestInfo",
                  defaultMessage: "Request Information",
                })}
              </h5>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(2, 1fr)",
                  gap: "1rem",
                }}
              >
                <div>
                  <strong>
                    {intl.formatMessage({
                      id: "biorepository.retrieval.status",
                      defaultMessage: "Status",
                    })}
                    :
                  </strong>{" "}
                  {getStatusTag(selectedRequest.status, selectedRequest)}
                </div>
                <div>
                  <strong>
                    {intl.formatMessage({
                      id: "biorepository.retrieval.purpose",
                      defaultMessage: "Purpose",
                    })}
                    :
                  </strong>{" "}
                  {selectedRequest.requestPurpose || "-"}
                </div>
                <div>
                  <strong>
                    {intl.formatMessage({
                      id: "biorepository.retrieval.requestedBy",
                      defaultMessage: "Requested By",
                    })}
                    :
                  </strong>{" "}
                  {selectedRequest.requestedByName || "-"}
                </div>
                <div>
                  <strong>
                    {intl.formatMessage({
                      id: "biorepository.retrieval.requestedDate",
                      defaultMessage: "Requested Date",
                    })}
                    :
                  </strong>{" "}
                  {formatDate(selectedRequest.requestedTimestamp)}
                </div>
                {selectedRequest.approvedTimestamp && (
                  <>
                    <div>
                      <strong>
                        {intl.formatMessage({
                          id: "biorepository.retrieval.approvedBy",
                          defaultMessage: "Approved By",
                        })}
                        :
                      </strong>{" "}
                      {selectedRequest.approvedByUsername || "-"}
                    </div>
                    <div>
                      <strong>
                        {intl.formatMessage({
                          id: "biorepository.retrieval.approvedDate",
                          defaultMessage: "Approved Date",
                        })}
                        :
                      </strong>{" "}
                      {formatDate(selectedRequest.approvedTimestamp)}
                    </div>
                  </>
                )}
                {selectedRequest.lastUpdated && (
                  <div>
                    <strong>
                      {intl.formatMessage({
                        id: "biorepository.retrieval.completedDate",
                        defaultMessage: "Completed Date",
                      })}
                      :
                    </strong>{" "}
                    {formatDate(selectedRequest.lastUpdated)}
                  </div>
                )}
                {selectedRequest.projectName && (
                  <div>
                    <strong>
                      {intl.formatMessage({
                        id: "biorepository.retrieval.project",
                        defaultMessage: "Project",
                      })}
                      :
                    </strong>{" "}
                    {selectedRequest.projectName}
                  </div>
                )}
              </div>
            </div>

            {/* Request lines with reference vs fulfilled layers */}
            <h5 style={{ marginBottom: "0.5rem" }}>
              {intl.formatMessage({
                id: "biorepository.retrieval.requestedSamples",
                defaultMessage: "Requested Samples",
              })}
            </h5>
            {selectedRequest.items && selectedRequest.items.length > 0 ? (
              selectedRequest.items
                .filter(
                  (item) =>
                    item.itemRole !== "FULFILLMENT" && !item.fulfillsItemId,
                )
                .map((item, idx) => (
                  <div
                    key={item.id || idx}
                    style={{
                      marginBottom: "1rem",
                      padding: "0.75rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <div style={{ marginBottom: "0.5rem" }}>
                      <strong>
                        {intl.formatMessage({
                          id: "biorepository.retrieval.lifecycle.requestedReference",
                          defaultMessage: "Requested Reference",
                        })}
                        :
                      </strong>{" "}
                      {formatRequestedReferenceSummary(item)}
                      {item.quantityRequested != null &&
                        ` — ${formatQuantityWithUnit(
                          item.quantityRequested,
                          item.unitOfMeasure,
                        )}`}
                    </div>
                    {(item.fulfillments || []).length > 0 ? (
                      <div>
                        <strong>
                          {intl.formatMessage({
                            id: "biorepository.retrieval.lifecycle.fulfilledSample",
                            defaultMessage: "Fulfilled Sample",
                          })}
                          :
                        </strong>
                        <ul style={{ marginTop: "0.25rem", paddingLeft: "1.25rem" }}>
                          {item.fulfillments.map((fulfillment) => {
                            const itemStatus = getItemDisplayStatus(
                              fulfillment,
                              intl,
                            );
                            return (
                              <li key={fulfillment.id}>
                                {fulfillment.externalId ||
                                  fulfillment.barcode ||
                                  fulfillment.sampleNumber ||
                                  `Sample ${fulfillment.id}`}
                                {fulfillment.sourceStoragePath &&
                                  ` — ${fulfillment.sourceStoragePath}`}
                                {fulfillment.quantityReleased != null &&
                                  ` — released: ${formatQuantityWithUnit(
                                    fulfillment.quantityReleased,
                                    fulfillment.unitOfMeasure,
                                  )}`}
                                {" — "}
                                {itemStatus.label}
                              </li>
                            );
                          })}
                        </ul>
                      </div>
                    ) : item.bioSampleId ? (
                      <div>
                        <strong>
                          {intl.formatMessage({
                            id: "biorepository.retrieval.lifecycle.fulfilledSample",
                            defaultMessage: "Fulfilled Sample",
                          })}
                          :
                        </strong>{" "}
                        {item.externalId ||
                          item.barcode ||
                          item.sampleNumber ||
                          `Sample ${item.id}`}
                        {" — "}
                        {getItemDisplayStatus(item, intl).label}
                      </div>
                    ) : (
                      <div style={{ color: "#525252", fontStyle: "italic" }}>
                        {resolveItemStatus(item) === "AWAITING_FULFILLMENT"
                          ? "Not fulfilled"
                          : getItemDisplayStatus(item, intl).label}
                      </div>
                    )}
                  </div>
                ))
            ) : (
              <p style={{ color: "#525252" }}>No items recorded</p>
            )}
          </div>
        ) : (
          <p>
            {intl.formatMessage({
              id: "biorepository.retrieval.noData",
              defaultMessage: "No data available",
            })}
          </p>
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
      />
    </div>
  );
}

RetrievalHistoryTab.propTypes = {
  onActionComplete: PropTypes.func,
  refreshToken: PropTypes.number,
};

export default RetrievalHistoryTab;
