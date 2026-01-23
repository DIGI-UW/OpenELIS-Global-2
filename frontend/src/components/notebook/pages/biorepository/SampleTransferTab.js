import { useState, useCallback, useEffect } from "react";
import {
  Grid,
  Column,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  Tag,
  Button,
  Dropdown,
  TextInput,
  InlineNotification,
  Loading,
  Modal,
  TextArea,
} from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * SampleTransferTab - Sample Transfer Queue management
 * Displays pending transfer requests from origin labs for biorepository review.
 * Supports inline editing of BSL/Ethics and bulk accept/reject operations.
 */
function SampleTransferTab() {
  const intl = useIntl();

  // Data state
  const [transferItems, setTransferItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Filter state
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [sourceLabFilter, setSourceLabFilter] = useState(null);
  const [availableSourceLabs, setAvailableSourceLabs] = useState([]);

  // Accept modal state (bulk entry form)
  const [acceptModalOpen, setAcceptModalOpen] = useState(false);
  const [acceptBsl, setAcceptBsl] = useState("BSL_1");
  const [acceptEthics, setAcceptEthics] = useState("");
  const [itemsToAccept, setItemsToAccept] = useState([]);

  // Reject modal state
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [itemsToReject, setItemsToReject] = useState([]);

  // Selection state (managed by DataTable)
  const [selectedRows, setSelectedRows] = useState([]);

  const biosafetyLevels = [
    { id: "BSL_1", text: "BSL-1" },
    { id: "BSL_2", text: "BSL-2" },
    { id: "BSL_3", text: "BSL-3" },
    { id: "BSL_4", text: "BSL-4" },
  ];

  const statusFilters = [
    {
      id: "PENDING",
      text: intl.formatMessage({
        id: "biorepository.transfer.status.pending",
        defaultMessage: "Pending",
      }),
    },
    {
      id: "ACCEPTED",
      text: intl.formatMessage({
        id: "biorepository.transfer.status.accepted",
        defaultMessage: "Accepted",
      }),
    },
    {
      id: "REJECTED",
      text: intl.formatMessage({
        id: "biorepository.transfer.status.rejected",
        defaultMessage: "Rejected",
      }),
    },
    {
      id: "ALL",
      text: intl.formatMessage({
        id: "biorepository.transfer.status.all",
        defaultMessage: "All",
      }),
    },
  ];

  /**
   * Load transfer requests and flatten into items for display
   */
  const loadTransferData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // Build endpoint based on filters
      let endpoint = "/rest/biorepository/transfer/pending?limit=100";
      if (
        statusFilter &&
        statusFilter !== "PENDING" &&
        statusFilter !== "ALL"
      ) {
        endpoint = `/rest/biorepository/transfer?status=${statusFilter}`;
      } else if (statusFilter === "ALL") {
        endpoint = "/rest/biorepository/transfer";
      }

      // First get the list of requests
      getFromOpenElisServer(endpoint, async (requests) => {
        if (!requests || !Array.isArray(requests)) {
          setTransferItems([]);
          setLoading(false);
          return;
        }

        // For each request, fetch full details to get items
        const allItems = [];
        const labSet = new Set();

        for (const request of requests) {
          labSet.add(request.sourceLab);

          // Fetch full request with items
          await new Promise((resolve) => {
            getFromOpenElisServer(
              `/rest/biorepository/transfer/${request.id}`,
              (fullRequest) => {
                if (fullRequest && fullRequest.items) {
                  for (const item of fullRequest.items) {
                    // Apply source lab filter if set
                    if (
                      sourceLabFilter &&
                      request.sourceLab !== sourceLabFilter
                    ) {
                      continue;
                    }
                    // For pending filter, only show pending items
                    if (
                      statusFilter === "PENDING" &&
                      item.status !== "PENDING"
                    ) {
                      continue;
                    }

                    allItems.push({
                      ...item,
                      requestId: request.id,
                      sourceLab: request.sourceLab,
                      requestNotes: request.requestNotes,
                      requestedTimestamp: request.requestedTimestamp,
                      requestedByName: request.requestedByName,
                    });
                  }
                }
                resolve();
              },
              () => resolve(), // Resolve on error to continue
            );
          });
        }

        setTransferItems(allItems);
        setAvailableSourceLabs(
          [...labSet].map((lab) => ({ id: lab, text: lab })),
        );
        setLoading(false);
      });
    } catch (err) {
      setError(err.message || "Failed to load transfer data");
      setLoading(false);
    }
  }, [statusFilter, sourceLabFilter]);

  // Load data on mount and when filters change
  useEffect(() => {
    loadTransferData();
  }, [loadTransferData]);

  /**
   * Open accept modal for a single item
   */
  const handleAcceptItem = useCallback((item) => {
    setItemsToAccept([item]);
    setAcceptBsl("BSL_1");
    setAcceptEthics("");
    setAcceptModalOpen(true);
  }, []);

  /**
   * Open reject modal for single item
   */
  const handleRejectItemClick = useCallback((item) => {
    setItemsToReject([item]);
    setRejectReason("");
    setRejectModalOpen(true);
  }, []);

  /**
   * Open reject modal for selected items
   */
  const handleRejectSelectedClick = useCallback(() => {
    const items = transferItems.filter((item) =>
      selectedRows.includes(item.id.toString()),
    );
    setItemsToReject(items);
    setRejectReason("");
    setRejectModalOpen(true);
  }, [selectedRows, transferItems]);

  /**
   * Execute rejection with reason
   */
  const executeReject = useCallback(async () => {
    if (!rejectReason.trim()) {
      setError(
        intl.formatMessage({
          id: "biorepository.transfer.rejectReasonRequired",
          defaultMessage: "Rejection reason is required",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);
    setRejectModalOpen(false);

    let successCount = 0;
    let errorCount = 0;

    for (const item of itemsToReject) {
      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/biorepository/transfer/item/${item.id}/reject`,
          JSON.stringify({ reason: rejectReason }),
        );

        if (response.error) {
          errorCount++;
        } else {
          successCount++;
        }
      } catch (err) {
        errorCount++;
      }
    }

    if (errorCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "biorepository.transfer.rejectPartialError",
            defaultMessage: "{errorCount} item(s) failed to reject",
          },
          { errorCount },
        ),
      );
    }

    if (successCount > 0) {
      setSuccess(
        intl.formatMessage(
          {
            id: "biorepository.transfer.rejectSuccess",
            defaultMessage: "{count} item(s) rejected successfully",
          },
          { count: successCount },
        ),
      );
    }

    setItemsToReject([]);
    setRejectReason("");
    loadTransferData();
    setLoading(false);
  }, [itemsToReject, rejectReason, intl, loadTransferData]);

  /**
   * Open accept modal for selected items
   */
  const handleAcceptSelectedClick = useCallback(() => {
    const items = transferItems.filter((item) =>
      selectedRows.includes(item.id.toString()),
    );
    setItemsToAccept(items);
    setAcceptBsl("BSL_1");
    setAcceptEthics("");
    setAcceptModalOpen(true);
  }, [selectedRows, transferItems]);

  /**
   * Execute acceptance with form data from modal
   */
  const executeAccept = useCallback(async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setAcceptModalOpen(false);

    let successCount = 0;
    let errorCount = 0;

    const metadata = {
      biosafetyLevel: acceptBsl,
      ethicsApprovalRef: acceptEthics.trim() || null,
    };

    for (const item of itemsToAccept) {
      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/biorepository/transfer/item/${item.id}/accept`,
          JSON.stringify(metadata),
        );

        if (response.error) {
          errorCount++;
        } else {
          successCount++;
        }
      } catch (err) {
        errorCount++;
      }
    }

    if (errorCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "biorepository.transfer.acceptPartialError",
            defaultMessage: "{errorCount} item(s) failed to accept",
          },
          { errorCount },
        ),
      );
    }

    if (successCount > 0) {
      setSuccess(
        intl.formatMessage(
          {
            id: "biorepository.transfer.acceptBulkSuccess",
            defaultMessage: "{count} item(s) accepted successfully",
          },
          { count: successCount },
        ),
      );
    }

    setItemsToAccept([]);
    setSelectedRows([]);
    loadTransferData();
    setLoading(false);
  }, [itemsToAccept, acceptBsl, acceptEthics, intl, loadTransferData]);

  // Prepare table rows
  const tableRows = transferItems.map((item) => ({
    id: item.id.toString(),
    sampleId:
      item.externalId || item.accessionNumber || `Item-${item.sampleItemId}`,
    sampleType: item.sampleType || "-",
    sourceLab: item.sourceLab,
    status: item.status,
    requestId: item.requestId,
  }));

  const tableHeaders = [
    {
      key: "sampleId",
      header: intl.formatMessage({
        id: "biorepository.transfer.column.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.transfer.column.sampleType",
        defaultMessage: "Type",
      }),
    },
    {
      key: "sourceLab",
      header: intl.formatMessage({
        id: "biorepository.transfer.column.sourceLab",
        defaultMessage: "Source Lab",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "biorepository.transfer.column.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const pendingCount = transferItems.filter(
    (i) => i.status === "PENDING",
  ).length;

  return (
    <div className="sample-transfer-tab">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <h4>
              <FormattedMessage
                id="biorepository.transfer.title"
                defaultMessage="Sample Transfer Queue"
              />
            </h4>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              onClick={loadTransferData}
              disabled={loading}
            >
              <FormattedMessage
                id="biorepository.transfer.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>

          {/* Filters */}
          <div
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1rem",
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            <Dropdown
              id="status-filter"
              titleText={intl.formatMessage({
                id: "biorepository.transfer.filter.status",
                defaultMessage: "Status",
              })}
              items={statusFilters}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={statusFilters.find((s) => s.id === statusFilter)}
              onChange={({ selectedItem }) =>
                setStatusFilter(selectedItem?.id || "PENDING")
              }
              size="sm"
              style={{ minWidth: "150px" }}
            />
            <Dropdown
              id="source-lab-filter"
              titleText={intl.formatMessage({
                id: "biorepository.transfer.filter.sourceLab",
                defaultMessage: "Source Lab",
              })}
              items={[
                {
                  id: null,
                  text: intl.formatMessage({
                    id: "biorepository.transfer.filter.allLabs",
                    defaultMessage: "All Labs",
                  }),
                },
                ...availableSourceLabs,
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                sourceLabFilter
                  ? availableSourceLabs.find((l) => l.id === sourceLabFilter)
                  : {
                      id: null,
                      text: intl.formatMessage({
                        id: "biorepository.transfer.filter.allLabs",
                        defaultMessage: "All Labs",
                      }),
                    }
              }
              onChange={({ selectedItem }) =>
                setSourceLabFilter(selectedItem?.id || null)
              }
              size="sm"
              style={{ minWidth: "150px" }}
            />
            <Tag type="blue" style={{ marginLeft: "auto" }}>
              <FormattedMessage
                id="biorepository.transfer.pendingCount"
                defaultMessage="Pending: {count} items"
                values={{ count: pendingCount }}
              />
            </Tag>
          </div>

          {/* Notifications */}
          {error && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "biorepository.transfer.error",
                defaultMessage: "Error",
              })}
              subtitle={error}
              lowContrast
              onCloseButtonClick={() => setError(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}
          {success && (
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "biorepository.transfer.success",
                defaultMessage: "Success",
              })}
              subtitle={success}
              lowContrast
              onCloseButtonClick={() => setSuccess(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}

          {loading && <Loading withOverlay description="Loading..." />}

          {/* Data Table */}
          {transferItems.length === 0 && !loading ? (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "biorepository.transfer.noItems",
                defaultMessage: "No Items",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.transfer.noItemsMessage",
                defaultMessage:
                  "No transfer requests found matching the current filters.",
              })}
              lowContrast
              hideCloseButton
            />
          ) : (
            <DataTable
              rows={tableRows}
              headers={tableHeaders}
              isSortable
              render={({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
                getSelectionProps,
                selectedRows: dtSelectedRows,
              }) => {
                // Sync selection state
                if (
                  dtSelectedRows &&
                  dtSelectedRows.length !== selectedRows.length
                ) {
                  const newSelected = dtSelectedRows.map((r) => r.id);
                  if (
                    JSON.stringify(newSelected) !== JSON.stringify(selectedRows)
                  ) {
                    setTimeout(() => setSelectedRows(newSelected), 0);
                  }
                }

                return (
                  <TableContainer>
                    <TableToolbar>
                      <TableToolbarContent>
                        <div
                          style={{
                            display: "flex",
                            gap: "1rem",
                            alignItems: "center",
                            padding: "0.5rem 0",
                          }}
                        >
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="biorepository.transfer.selected"
                              defaultMessage="Selected: {count}"
                              values={{ count: selectedRows.length }}
                            />
                          </span>
                          <Button
                            kind="primary"
                            size="sm"
                            onClick={handleAcceptSelectedClick}
                            disabled={selectedRows.length === 0}
                          >
                            <FormattedMessage
                              id="biorepository.transfer.acceptSelected"
                              defaultMessage="Accept Selected"
                            />
                          </Button>
                          <Button
                            kind="danger"
                            size="sm"
                            onClick={handleRejectSelectedClick}
                            disabled={selectedRows.length === 0}
                          >
                            <FormattedMessage
                              id="biorepository.transfer.rejectSelected"
                              defaultMessage="Reject Selected"
                            />
                          </Button>
                        </div>
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
                        {rows.map((row) => {
                          const item = transferItems.find(
                            (i) => i.id.toString() === row.id,
                          );
                          const isPending = item?.status === "PENDING";

                          return (
                            <TableRow {...getRowProps({ row })} key={row.id}>
                              <TableSelectRow {...getSelectionProps({ row })} />
                              {row.cells.map((cell) => {
                                // Actions column
                                if (cell.info.header === "actions") {
                                  return (
                                    <TableCell key={cell.id}>
                                      {isPending ? (
                                        <div
                                          style={{
                                            display: "flex",
                                            gap: "0.25rem",
                                          }}
                                        >
                                          <Button
                                            kind="primary"
                                            size="sm"
                                            onClick={() =>
                                              handleAcceptItem(item)
                                            }
                                          >
                                            <FormattedMessage
                                              id="biorepository.transfer.accept"
                                              defaultMessage="Accept"
                                            />
                                          </Button>
                                          <Button
                                            kind="danger--ghost"
                                            size="sm"
                                            onClick={() =>
                                              handleRejectItemClick(item)
                                            }
                                          >
                                            <FormattedMessage
                                              id="biorepository.transfer.reject"
                                              defaultMessage="Reject"
                                            />
                                          </Button>
                                        </div>
                                      ) : (
                                        <Tag
                                          type={
                                            item.status === "ACCEPTED"
                                              ? "green"
                                              : "red"
                                          }
                                        >
                                          {item.status}
                                        </Tag>
                                      )}
                                    </TableCell>
                                  );
                                }

                                // Default cell rendering
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                );
                              })}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </TableContainer>
                );
              }}
            />
          )}
        </Column>
      </Grid>

      {/* Reject Reason Modal */}
      <Modal
        open={rejectModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.transfer.rejectModal.title",
          defaultMessage: "Reject Transfer Item(s)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "biorepository.transfer.rejectModal.confirm",
          defaultMessage: "Reject",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.transfer.rejectModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={executeReject}
        onRequestClose={() => {
          setRejectModalOpen(false);
          setRejectReason("");
          setItemsToReject([]);
        }}
        danger
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="biorepository.transfer.rejectModal.message"
            defaultMessage="You are about to reject {count} item(s). Please provide a reason for rejection."
            values={{ count: itemsToReject.length }}
          />
        </p>
        <TextArea
          id="reject-reason"
          labelText={intl.formatMessage({
            id: "biorepository.transfer.rejectModal.reasonLabel",
            defaultMessage: "Rejection Reason *",
          })}
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          rows={4}
          required
        />
      </Modal>

      {/* Accept Bulk Entry Modal */}
      <Modal
        open={acceptModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.transfer.acceptModal.title",
          defaultMessage: "Accept Samples into Biorepository",
        })}
        primaryButtonText={intl.formatMessage(
          {
            id: "biorepository.transfer.acceptModal.confirm",
            defaultMessage: "Accept {count} Sample(s)",
          },
          { count: itemsToAccept.length },
        )}
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.transfer.acceptModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={executeAccept}
        onRequestClose={() => {
          setAcceptModalOpen(false);
          setAcceptBsl("BSL_1");
          setAcceptEthics("");
          setItemsToAccept([]);
        }}
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="biorepository.transfer.acceptModal.message"
            defaultMessage="You are about to accept {count} sample(s) into the biorepository. Please provide the required metadata."
            values={{ count: itemsToAccept.length }}
          />
        </p>

        <div style={{ marginBottom: "1rem" }}>
          <Dropdown
            id="accept-bsl"
            titleText={intl.formatMessage({
              id: "biorepository.transfer.acceptModal.bslLabel",
              defaultMessage: "Biosafety Level *",
            })}
            items={biosafetyLevels}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={biosafetyLevels.find((b) => b.id === acceptBsl)}
            onChange={({ selectedItem }) =>
              setAcceptBsl(selectedItem?.id || "BSL_1")
            }
          />
        </div>

        <TextInput
          id="accept-ethics"
          labelText={intl.formatMessage({
            id: "biorepository.transfer.acceptModal.ethicsLabel",
            defaultMessage: "Ethics Approval Reference",
          })}
          value={acceptEthics}
          onChange={(e) => setAcceptEthics(e.target.value)}
          placeholder={intl.formatMessage({
            id: "biorepository.transfer.acceptModal.ethicsPlaceholder",
            defaultMessage: "e.g., IRB-2024-001",
          })}
        />
      </Modal>
    </div>
  );
}

SampleTransferTab.propTypes = {};

export default SampleTransferTab;
