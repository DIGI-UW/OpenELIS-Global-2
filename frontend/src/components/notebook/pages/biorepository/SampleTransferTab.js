import { useState, useCallback, useEffect, useContext } from "react";
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
import { NotificationContext } from "../../../layout/Layout";
import { AlertDialog, NotificationKinds } from "../../../common/CustomNotification";

/**
 * SampleTransferTab - Sample Transfer Queue management
 * Displays pending transfer requests from origin labs for biorepository review.
 * Supports inline editing of BSL/Ethics and bulk accept/reject operations.
 */
function SampleTransferTab() {
  const intl = useIntl();

  // Notification context
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Data state
  const [transferItems, setTransferItems] = useState([]);
  const [loading, setLoading] = useState(false);

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

  // Selection is now managed by DataTable directly

  // Notification helper function
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
          await new Promise((resolve, reject) => {
            getFromOpenElisServer(
              `/rest/biorepository/transfer/${request.id}`,
              (fullRequest) => {
                try {
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
                        item?.status !== "PENDING"
                      ) {
                        continue;
                      }

                      // Ensure item has required properties
                      if (item && item.id) {
                        allItems.push({
                          ...item,
                          requestId: request.id,
                          sourceLab: request.sourceLab,
                          requestNotes: request.requestNotes,
                          requestedTimestamp: request.requestedTimestamp,
                          requestedByName: request.requestedByName,
                          status: item.status || "PENDING", // Default to PENDING if no status
                        });
                      }
                    }
                  }
                  // Always resolve to continue processing other requests
                  resolve();
                } catch (error) {
                  // Handle errors but still resolve to continue with other requests
                  console.error("Error processing transfer request:", error);
                  resolve();
                }
              },
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
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "biorepository.transfer.error",
          defaultMessage: "Error",
        }),
        subtitle: err.message || intl.formatMessage({
          id: "biorepository.transfer.loadError",
          defaultMessage: "Failed to load transfer data",
        }),
      });
      setLoading(false);
    }
  }, [statusFilter, sourceLabFilter, intl, notify]);

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
  const handleRejectSelectedClick = useCallback(
    (dtSelectedRows) => {
      if (!dtSelectedRows || dtSelectedRows.length === 0) return;

      const selectedRowIds = dtSelectedRows.map((row) => row.id);
      const items = transferItems.filter((item) =>
        selectedRowIds.includes(item.id.toString()),
      );
      setItemsToReject(items);
      setRejectReason("");
      setRejectModalOpen(true);
    },
    [transferItems],
  );

  /**
   * Execute rejection with reason
   */
  const executeReject = useCallback(async () => {
    if (!rejectReason.trim()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "biorepository.transfer.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "biorepository.transfer.rejectReasonRequired",
          defaultMessage: "Rejection reason is required",
        }),
      });
      return;
    }

    setLoading(true);
    setRejectModalOpen(false);

    let successCount = 0;
    let errorCount = 0;

    const rejectPromises = itemsToReject.map((item) => {
      return new Promise((resolve) => {
        postToOpenElisServerJsonResponse(
          `/rest/biorepository/transfer/item/${item.id}/reject`,
          JSON.stringify({ reason: rejectReason }),
          (response) => {
            if (response && response.error) {
              resolve({ success: false, item, response });
            } else {
              resolve({ success: true, item, response });
            }
          },
        );
      });
    });

    // Wait for all rejections to complete
    const results = await Promise.all(rejectPromises);

    // Count successes and errors
    for (const result of results) {
      if (result.success) {
        successCount++;
      } else {
        errorCount++;
      }
    }

    if (errorCount > 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "biorepository.transfer.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage(
          {
            id: "biorepository.transfer.rejectPartialError",
            defaultMessage: "{errorCount} item(s) failed to reject",
          },
          { errorCount },
        ),
      });
    }

    if (successCount > 0) {
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "biorepository.transfer.success",
          defaultMessage: "Success",
        }),
        subtitle: intl.formatMessage(
          {
            id: "biorepository.transfer.rejectSuccess",
            defaultMessage: "{count} item(s) rejected successfully",
          },
          { count: successCount },
        ),
      });
    }

    setItemsToReject([]);
    setRejectReason("");
    loadTransferData();
    setLoading(false);
  }, [itemsToReject, rejectReason, intl, loadTransferData, notify]);

  /**
   * Open accept modal for selected items
   */
  const handleAcceptSelectedClick = useCallback(
    (dtSelectedRows) => {
      if (!dtSelectedRows || dtSelectedRows.length === 0) return;

      const selectedRowIds = dtSelectedRows.map((row) => row.id);
      const items = transferItems.filter((item) =>
        selectedRowIds.includes(item.id.toString()),
      );
      setItemsToAccept(items);
      setAcceptBsl("BSL_1");
      setAcceptEthics("");
      setAcceptModalOpen(true);
    },
    [transferItems],
  );

  /**
   * Execute acceptance with form data from modal
   */
  const executeAccept = useCallback(async () => {
    setLoading(true);
    setAcceptModalOpen(false);

    let successCount = 0;
    let errorCount = 0;

    const metadata = {
      biosafetyLevel: acceptBsl,
      ethicsApprovalRef: acceptEthics.trim() || null,
    };

    // Process accepts with Promise wrapper for async/await support
    const acceptPromises = itemsToAccept.map((item) => {
      return new Promise((resolve) => {
        postToOpenElisServerJsonResponse(
          `/rest/biorepository/transfer/item/${item.id}/accept`,
          JSON.stringify(metadata),
          (response) => {
            if (response && response.error) {
              resolve({ success: false, item, response });
            } else {
              resolve({ success: true, item, response });
            }
          },
        );
      });
    });

    // Wait for all accepts to complete
    const results = await Promise.all(acceptPromises);

    // Count successes and errors
    for (const result of results) {
      if (result.success) {
        successCount++;
      } else {
        errorCount++;
      }
    }

    if (errorCount > 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "biorepository.transfer.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage(
          {
            id: "biorepository.transfer.acceptPartialError",
            defaultMessage: "{errorCount} item(s) failed to accept",
          },
          { errorCount },
        ),
      });
    }

    if (successCount > 0) {
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "biorepository.transfer.success",
          defaultMessage: "Success",
        }),
        subtitle: intl.formatMessage(
          {
            id: "biorepository.transfer.acceptBulkSuccess",
            defaultMessage: "{count} item(s) accepted successfully",
          },
          { count: successCount },
        ),
      });
    }

    setItemsToAccept([]);
    loadTransferData();
    setLoading(false);
  }, [itemsToAccept, acceptBsl, acceptEthics, intl, loadTransferData, notify]);

  // Prepare table rows
  const tableRows = transferItems.map((item) => ({
    id: item?.id?.toString() || "unknown",
    sampleId:
      item?.externalId ||
      item?.accessionNumber ||
      `Item-${item?.sampleItemId}` ||
      "Unknown",
    sampleType: item?.sampleType || "-",
    sourceLab: item?.sourceLab || "Unknown",
    status: item?.status || "UNKNOWN",
    requestId: item?.requestId,
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
    (i) => i?.status === "PENDING",
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
              label={intl.formatMessage({
                id: "biorepository.transfer.filter.statusLabel",
                defaultMessage: "Select status",
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
              label={intl.formatMessage({
                id: "biorepository.transfer.filter.sourceLabLabel",
                defaultMessage: "Select source lab",
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

          {/* Toast notifications via centralized system */}
          {notificationVisible === true ? <AlertDialog /> : ""}

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
                // Use DataTable's built-in selectedRows directly instead of syncing to external state

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
                              values={{
                                count: dtSelectedRows
                                  ? dtSelectedRows.length
                                  : 0,
                              }}
                            />
                          </span>
                          <Button
                            kind="primary"
                            size="sm"
                            onClick={() =>
                              handleAcceptSelectedClick(dtSelectedRows)
                            }
                            disabled={
                              !dtSelectedRows || dtSelectedRows.length === 0
                            }
                          >
                            <FormattedMessage
                              id="biorepository.transfer.acceptSelected"
                              defaultMessage="Accept Selected"
                            />
                          </Button>
                          <Button
                            kind="danger"
                            size="sm"
                            onClick={() =>
                              handleRejectSelectedClick(dtSelectedRows)
                            }
                            disabled={
                              !dtSelectedRows || dtSelectedRows.length === 0
                            }
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
                                            item?.status === "ACCEPTED"
                                              ? "green"
                                              : "red"
                                          }
                                        >
                                          {item?.status || "UNKNOWN"}
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
            label={intl.formatMessage({
              id: "biorepository.transfer.acceptModal.bslSelectLabel",
              defaultMessage: "Select biosafety level",
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
