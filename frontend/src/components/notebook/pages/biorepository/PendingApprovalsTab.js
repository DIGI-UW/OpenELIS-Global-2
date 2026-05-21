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
  TextArea,
  Tag,
  InlineNotification,
  Loading,
  ExpandableSearch,
  Pagination,
} from "@carbon/react";
import { Checkmark, Close, View, Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * PendingApprovalsTab - Supervisor approval queue for retrieval requests
 *
 * Displays requests awaiting approval with ability to:
 * - View request details and samples
 * - Approve (generates work order)
 * - Reject with reason
 */
function PendingApprovalsTab({ onActionComplete }) {
  const intl = useIntl();

  // Data state
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Search state
  const [searchTerm, setSearchTerm] = useState("");

  // Modal state
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [approvalNotes, setApprovalNotes] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  // Load pending requests
  const loadRequests = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      "/rest/biorepository/retrieval/requests/pending?limit=100",
      (data) => {
        setLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        if (data && Array.isArray(data)) {
          setRequests(data);
        } else {
          setRequests([]);
        }
      },
    );
  }, []);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  // Filter requests by search term
  const filteredRequests = requests.filter((r) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (r.requestNumber && r.requestNumber.toLowerCase().includes(term)) ||
      (r.requestPurpose && r.requestPurpose.toLowerCase().includes(term)) ||
      (r.requestedByName && r.requestedByName.toLowerCase().includes(term))
    );
  });

  // Paginated requests
  const paginatedRequests = filteredRequests.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  // Handle approve
  const handleApprove = useCallback(() => {
    if (!selectedRequest) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/requests/${selectedRequest.id}/approve`,
      JSON.stringify({ approvalNotes: approvalNotes || null }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        setApproveModalOpen(false);
        setSelectedRequest(null);
        setApprovalNotes("");
        loadRequests();
        if (onActionComplete) {
          onActionComplete();
        }
      },
    );
  }, [selectedRequest, approvalNotes, loadRequests, onActionComplete]);

  // Handle reject
  const handleReject = useCallback(() => {
    if (!selectedRequest || !rejectionReason.trim()) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/requests/${selectedRequest.id}/reject`,
      JSON.stringify({ reason: rejectionReason }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        setRejectModalOpen(false);
        setSelectedRequest(null);
        setRejectionReason("");
        loadRequests();
        if (onActionComplete) {
          onActionComplete();
        }
      },
    );
  }, [selectedRequest, rejectionReason, loadRequests, onActionComplete]);

  // Get priority tag type
  const getPriorityTagType = (priority) => {
    switch (priority) {
      case "CRITICAL":
        return "red";
      case "URGENT":
        return "orange";
      default:
        return "gray";
    }
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
      key: "requestedBy",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestedBy",
        defaultMessage: "Requested By",
      }),
    },
    {
      key: "sampleCount",
      header: intl.formatMessage({
        id: "biorepository.retrieval.sampleCount",
        defaultMessage: "Samples",
      }),
    },
    {
      key: "priority",
      header: intl.formatMessage({
        id: "biorepository.retrieval.priority",
        defaultMessage: "Priority",
      }),
    },
    {
      key: "requestedAt",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestedAt",
        defaultMessage: "Requested",
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
    <div className="pending-approvals-tab" style={{ padding: "1rem 0" }}>
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

      <DataTable
        rows={paginatedRequests.map((r) => ({
          id: r.id.toString(),
          requestNumber: r.requestNumber || `REQ-${r.id}`,
          requestedBy: r.requestedByName || "Unknown",
          sampleCount: r.totalItemCount || (r.items ? r.items.length : 0),
          priority: r.priorityLevel || "NORMAL",
          requestedAt: r.requestedTimestamp
            ? new Date(r.requestedTimestamp).toLocaleDateString()
            : "N/A",
          _raw: r,
        }))}
        headers={headers}
        size="md"
      >
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <TableContainer>
            <TableToolbar>
              <TableToolbarContent>
                <ExpandableSearch
                  labelText={intl.formatMessage({
                    id: "label.search",
                    defaultMessage: "Search",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.retrieval.search.pending",
                    defaultMessage: "Search by request number or requester...",
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
                  onClick={loadRequests}
                />
              </TableToolbarContent>
            </TableToolbar>
            <Table {...getTableProps()}>
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
                {rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={headers.length}>
                      <div
                        style={{
                          textAlign: "center",
                          padding: "2rem",
                          color: "#525252",
                        }}
                      >
                        <FormattedMessage
                          id="biorepository.retrieval.noPendingRequests"
                          defaultMessage="No requests pending approval"
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row) => {
                    const rawData = paginatedRequests.find(
                      (r) => r.id.toString() === row.id,
                    );
                    return (
                      <TableRow {...getRowProps({ row })} key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "priority" ? (
                              <Tag
                                type={getPriorityTagType(cell.value)}
                                size="sm"
                              >
                                {cell.value}
                              </Tag>
                            ) : cell.info.header === "actions" ? (
                              <div style={{ display: "flex", gap: "0.25rem" }}>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={View}
                                  iconDescription={intl.formatMessage({
                                    id: "label.viewDetails",
                                    defaultMessage: "View Details",
                                  })}
                                  hasIconOnly
                                  onClick={() => {
                                    setSelectedRequest(rawData);
                                    setDetailsModalOpen(true);
                                  }}
                                />
                                <Button
                                  kind="primary"
                                  size="sm"
                                  renderIcon={Checkmark}
                                  iconDescription={intl.formatMessage({
                                    id: "label.approve",
                                    defaultMessage: "Approve",
                                  })}
                                  hasIconOnly
                                  onClick={() => {
                                    setSelectedRequest(rawData);
                                    setApproveModalOpen(true);
                                  }}
                                />
                                <Button
                                  kind="danger--ghost"
                                  size="sm"
                                  renderIcon={Close}
                                  iconDescription={intl.formatMessage({
                                    id: "label.reject",
                                    defaultMessage: "Reject",
                                  })}
                                  hasIconOnly
                                  onClick={() => {
                                    setSelectedRequest(rawData);
                                    setRejectModalOpen(true);
                                  }}
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
        )}
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

      {/* Details Modal */}
      <Modal
        open={detailsModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.requestDetails",
          defaultMessage: "Request Details",
        })}
        passiveModal
        onRequestClose={() => {
          setDetailsModalOpen(false);
          setSelectedRequest(null);
        }}
        size="lg"
      >
        {selectedRequest && (
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
                    id="biorepository.retrieval.requestNumber"
                    defaultMessage="Request #"
                  />
                  :
                </strong>{" "}
                {selectedRequest.requestNumber || `REQ-${selectedRequest.id}`}
              </div>
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.status"
                    defaultMessage="Status"
                  />
                  :
                </strong>{" "}
                <Tag type="blue">{selectedRequest.requestStatus}</Tag>
              </div>
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.requestedBy"
                    defaultMessage="Requested By"
                  />
                  :
                </strong>{" "}
                {selectedRequest.requestedByName || "Unknown"}
              </div>
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.requestedAt"
                    defaultMessage="Requested"
                  />
                  :
                </strong>{" "}
                {selectedRequest.requestedTimestamp
                  ? new Date(
                      selectedRequest.requestedTimestamp,
                    ).toLocaleString()
                  : "N/A"}
              </div>
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.priority"
                    defaultMessage="Priority"
                  />
                  :
                </strong>{" "}
                <Tag
                  type={getPriorityTagType(selectedRequest.priorityLevel)}
                  size="sm"
                >
                  {selectedRequest.priorityLevel || "NORMAL"}
                </Tag>
              </div>
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.destinationType"
                    defaultMessage="Destination Type"
                  />
                  :
                </strong>{" "}
                {selectedRequest.destinationType || "N/A"}
              </div>
            </div>

            <div>
              <strong>
                <FormattedMessage
                  id="biorepository.retrieval.purpose"
                  defaultMessage="Purpose"
                />
                :
              </strong>
              <p style={{ marginTop: "0.25rem", color: "#525252" }}>
                {selectedRequest.requestPurpose || "Not specified"}
              </p>
            </div>

            {selectedRequest.destinationDetails && (
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.destinationDetails"
                    defaultMessage="Destination Details"
                  />
                  :
                </strong>
                <p style={{ marginTop: "0.25rem", color: "#525252" }}>
                  {selectedRequest.destinationDetails}
                </p>
              </div>
            )}

            {selectedRequest.ethicsApprovalRef && (
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.ethicsRef"
                    defaultMessage="Ethics Approval"
                  />
                  :
                </strong>{" "}
                {selectedRequest.ethicsApprovalRef}
              </div>
            )}

            {selectedRequest.items && selectedRequest.items.length > 0 && (
              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.requestedSamples"
                    defaultMessage="Requested Samples"
                  />
                  :
                </strong>
                <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
                  {selectedRequest.items.map((item, idx) => (
                    <li key={idx}>
                      {item.sampleNumber ||
                        item.bioSampleExternalId ||
                        `Sample ${idx + 1}`}
                      {item.storageLocation && ` - ${item.storageLocation}`}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Approve Modal */}
      <Modal
        open={approveModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.approveRequest",
          defaultMessage: "Approve Request",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "label.approve",
                defaultMessage: "Approve",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setApproveModalOpen(false);
          setSelectedRequest(null);
          setApprovalNotes("");
        }}
        onRequestSubmit={handleApprove}
        primaryButtonDisabled={actionLoading}
        danger={false}
      >
        {selectedRequest && (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="biorepository.retrieval.approve.confirmation"
                defaultMessage="Are you sure you want to approve request {requestNumber}? This will generate a work order for sample retrieval."
                values={{
                  requestNumber:
                    selectedRequest.requestNumber ||
                    `REQ-${selectedRequest.id}`,
                }}
              />
            </p>
            <TextArea
              id="approvalNotes"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.approvalNotes",
                defaultMessage: "Approval Notes (Optional)",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.approvalNotes.placeholder",
                defaultMessage: "Add any notes for the requester...",
              })}
              value={approvalNotes}
              onChange={(e) => setApprovalNotes(e.target.value)}
            />
          </div>
        )}
      </Modal>

      {/* Reject Modal */}
      <Modal
        open={rejectModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.rejectRequest",
          defaultMessage: "Reject Request",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "label.reject",
                defaultMessage: "Reject",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setRejectModalOpen(false);
          setSelectedRequest(null);
          setRejectionReason("");
        }}
        onRequestSubmit={handleReject}
        primaryButtonDisabled={actionLoading || !rejectionReason.trim()}
        danger
      >
        {selectedRequest && (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="biorepository.retrieval.reject.confirmation"
                defaultMessage="Are you sure you want to reject request {requestNumber}?"
                values={{
                  requestNumber:
                    selectedRequest.requestNumber ||
                    `REQ-${selectedRequest.id}`,
                }}
              />
            </p>
            <TextArea
              id="rejectionReason"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.rejectionReason",
                defaultMessage: "Rejection Reason (Required)",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.rejectionReason.placeholder",
                defaultMessage:
                  "Provide a reason for rejecting this request...",
              })}
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              required
            />
          </div>
        )}
      </Modal>
    </div>
  );
}

PendingApprovalsTab.propTypes = {
  onActionComplete: PropTypes.func,
};

export default PendingApprovalsTab;
