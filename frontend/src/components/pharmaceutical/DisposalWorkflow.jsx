import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  DataTable,
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
  Modal,
  Form,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Tag,
  Loading,
  InlineNotification,
  Tile,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
} from "@carbon/react";
import {
  Add,
  View,
  Checkmark,
  Close,
  DocumentPdf,
  Time,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../utils/Utils";

const DisposalWorkflow = () => {
  const intl = useIntl();
  const [disposalRecords, setDisposalRecords] = useState([]);
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [isExecuteModalOpen, setIsExecuteModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [formData, setFormData] = useState({
    sampleId: "",
    reason: "EXPIRED",
    method: "INCINERATION",
    justification: "",
  });
  const [executeData, setExecuteData] = useState({
    witnessId: "",
    disposalNotes: "",
  });

  const fetchDisposalRecords = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/pharmaceutical/disposal", (response) => {
      if (response) {
        setDisposalRecords(response);
      }
      setLoading(false);
    });
  }, []);

  const fetchPendingApprovals = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/disposal/pending-approvals",
      (response) => {
        if (response) {
          setPendingApprovals(response);
        }
      },
    );
  }, []);

  const fetchSamples = useCallback(() => {
    getFromOpenElisServer("/rest/pharmaceutical/samples", (response) => {
      if (response) {
        setSamples(response);
      }
    });
  }, []);

  useEffect(() => {
    fetchDisposalRecords();
    fetchPendingApprovals();
    fetchSamples();
  }, [fetchDisposalRecords, fetchPendingApprovals, fetchSamples]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleExecuteInputChange = (e) => {
    const { name, value } = e.target;
    setExecuteData((prev) => ({ ...prev, [name]: value }));
  };

  const handleRequestDisposal = () => {
    postToOpenElisServer(
      "/rest/pharmaceutical/disposal/request",
      JSON.stringify(formData),
      (response) => {
        if (response) {
          setIsRequestModalOpen(false);
          setFormData({
            sampleId: "",
            reason: "EXPIRED",
            method: "INCINERATION",
            justification: "",
          });
          fetchDisposalRecords();
          fetchPendingApprovals();
        } else {
          setError("Failed to request disposal");
        }
      },
    );
  };

  const handleApprove = (recordId) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/disposal/${recordId}/approve`,
      null,
      (response) => {
        if (response) {
          fetchDisposalRecords();
          fetchPendingApprovals();
        } else {
          setError("Failed to approve disposal");
        }
      },
    );
  };

  const handleReject = (recordId, reason) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/disposal/${recordId}/reject?rejectionReason=${encodeURIComponent(
        reason,
      )}`,
      null,
      (response) => {
        if (response) {
          fetchDisposalRecords();
          fetchPendingApprovals();
        } else {
          setError("Failed to reject disposal");
        }
      },
    );
  };

  const handleExecuteDisposal = () => {
    if (!selectedRecord) return;

    postToOpenElisServer(
      `/rest/pharmaceutical/disposal/${selectedRecord.id}/execute`,
      JSON.stringify(executeData),
      (response) => {
        if (response) {
          setIsExecuteModalOpen(false);
          setSelectedRecord(null);
          setExecuteData({
            witnessId: "",
            disposalNotes: "",
          });
          fetchDisposalRecords();
        } else {
          setError("Failed to execute disposal");
        }
      },
    );
  };

  const handleGenerateCertificate = (recordId) => {
    getFromOpenElisServer(
      `/rest/pharmaceutical/disposal/${recordId}/certificate`,
      (response) => {
        if (response) {
          const blob = new Blob([response], { type: "text/plain" });
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          a.download = `disposal-certificate-${recordId}.txt`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }
      },
    );
  };

  const openExecuteModal = (record) => {
    setSelectedRecord(record);
    setIsExecuteModalOpen(true);
  };

  const openViewModal = (record) => {
    setSelectedRecord(record);
    setIsViewModalOpen(true);
  };

  const getStatusTag = (status) => {
    const statusColors = {
      PENDING_APPROVAL: "purple",
      APPROVED: "blue",
      SCHEDULED: "cyan",
      COMPLETED: "green",
      REJECTED: "red",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "pharmaceutical.disposal.id" }),
    },
    {
      key: "sampleId",
      header: intl.formatMessage({ id: "pharmaceutical.disposal.sample" }),
    },
    {
      key: "reason",
      header: intl.formatMessage({ id: "pharmaceutical.disposal.reason" }),
    },
    {
      key: "method",
      header: intl.formatMessage({ id: "pharmaceutical.disposal.method" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.disposal.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = disposalRecords.map((record) => ({
    id: String(record.id),
    sampleId: record.sampleId,
    reason: record.reason,
    method: record.method,
    status: record.status,
    actions: record,
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            className="pharmaceutical-alert"
          />
        )}

        {pendingApprovals.length > 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--warning">
            <strong>
              <FormattedMessage id="pharmaceutical.disposal.pendingApprovals" />
            </strong>
            <p>
              {pendingApprovals.length}{" "}
              <FormattedMessage id="pharmaceutical.disposal.pendingApprovals.count" />
            </p>
          </Tile>
        )}

        <DataTable rows={rows} headers={headers}>
          {({
            rows,
            headers,
            getTableProps,
            getHeaderProps,
            getRowProps,
            onInputChange,
          }) => (
            <>
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch onChange={onInputChange} />
                  <Button
                    renderIcon={Add}
                    onClick={() => setIsRequestModalOpen(true)}
                  >
                    <FormattedMessage id="pharmaceutical.disposal.request" />
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            getStatusTag(cell.value)
                          ) : cell.info.header === "actions" ? (
                            <div className="pharmaceutical-actions">
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={View}
                                iconDescription="View"
                                hasIconOnly
                                onClick={() => openViewModal(cell.value)}
                              />
                              {cell.value.status === "PENDING_APPROVAL" && (
                                <>
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Checkmark}
                                    iconDescription="Approve"
                                    hasIconOnly
                                    onClick={() => handleApprove(cell.value.id)}
                                  />
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Close}
                                    iconDescription="Reject"
                                    hasIconOnly
                                    onClick={() =>
                                      handleReject(cell.value.id, "Rejected")
                                    }
                                  />
                                </>
                              )}
                              {(cell.value.status === "APPROVED" ||
                                cell.value.status === "SCHEDULED") && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() => openExecuteModal(cell.value)}
                                >
                                  <FormattedMessage id="pharmaceutical.disposal.execute" />
                                </Button>
                              )}
                              {cell.value.status === "COMPLETED" && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={DocumentPdf}
                                  iconDescription="Certificate"
                                  hasIconOnly
                                  onClick={() =>
                                    handleGenerateCertificate(cell.value.id)
                                  }
                                />
                              )}
                            </div>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </DataTable>

        {/* Request Disposal Modal */}
        <Modal
          open={isRequestModalOpen}
          onRequestClose={() => setIsRequestModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.disposal.request.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.submit" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleRequestDisposal}
        >
          <Form className="pharmaceutical-form">
            <Select
              id="sampleId"
              name="sampleId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.sample",
              })}
              value={formData.sampleId}
              onChange={handleInputChange}
              required
            >
              <SelectItem value="" text="Select a sample" />
              {samples.map((sample) => (
                <SelectItem
                  key={sample.id}
                  value={sample.id}
                  text={`${sample.uniqueSampleId} - ${sample.sampleName}`}
                />
              ))}
            </Select>
            <Select
              id="reason"
              name="reason"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.reason",
              })}
              value={formData.reason}
              onChange={handleInputChange}
            >
              <SelectItem value="EXPIRED" text="Expired" />
              <SelectItem value="FAILED_QC" text="Failed QC" />
              <SelectItem value="CONTAMINATED" text="Contaminated" />
              <SelectItem value="TESTING_COMPLETE" text="Testing Complete" />
              <SelectItem value="STABILITY_FAILURE" text="Stability Failure" />
              <SelectItem value="CUSTOMER_REQUEST" text="Customer Request" />
            </Select>
            <Select
              id="method"
              name="method"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.method",
              })}
              value={formData.method}
              onChange={handleInputChange}
            >
              <SelectItem value="INCINERATION" text="Incineration" />
              <SelectItem
                value="CHEMICAL_TREATMENT"
                text="Chemical Treatment"
              />
              <SelectItem
                value="RETURN_TO_MANUFACTURER"
                text="Return to Manufacturer"
              />
              <SelectItem value="LANDFILL" text="Landfill" />
              <SelectItem value="AUTOCLAVE" text="Autoclave" />
            </Select>
            <TextArea
              id="justification"
              name="justification"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.justification",
              })}
              value={formData.justification}
              onChange={handleInputChange}
              rows={4}
              required
            />
          </Form>
        </Modal>

        {/* Execute Disposal Modal */}
        <Modal
          open={isExecuteModalOpen}
          onRequestClose={() => setIsExecuteModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.disposal.execute.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleExecuteDisposal}
          danger
        >
          <Form className="pharmaceutical-form">
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "pharmaceutical.disposal.execute.warning",
              })}
              subtitle={intl.formatMessage({
                id: "pharmaceutical.disposal.execute.warning.message",
              })}
              hideCloseButton
              lowContrast
            />
            <TextInput
              id="witnessId"
              name="witnessId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.witness",
              })}
              value={executeData.witnessId}
              onChange={handleExecuteInputChange}
              required
            />
            <TextArea
              id="disposalNotes"
              name="disposalNotes"
              labelText={intl.formatMessage({
                id: "pharmaceutical.disposal.notes",
              })}
              value={executeData.disposalNotes}
              onChange={handleExecuteInputChange}
              rows={4}
            />
          </Form>
        </Modal>

        {/* View Disposal Record Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.disposal.details.title",
          })}
          passiveModal
        >
          {selectedRecord && (
            <div>
              <StructuredListWrapper>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>
                      <FormattedMessage id="label.field" />
                    </StructuredListCell>
                    <StructuredListCell head>
                      <FormattedMessage id="label.value" />
                    </StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.id" />
                    </StructuredListCell>
                    <StructuredListCell>{selectedRecord.id}</StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.sample" />
                    </StructuredListCell>
                    <StructuredListCell>
                      {selectedRecord.sampleId}
                    </StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.reason" />
                    </StructuredListCell>
                    <StructuredListCell>
                      {selectedRecord.reason}
                    </StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.method" />
                    </StructuredListCell>
                    <StructuredListCell>
                      {selectedRecord.method}
                    </StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.status" />
                    </StructuredListCell>
                    <StructuredListCell>
                      {getStatusTag(selectedRecord.status)}
                    </StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="pharmaceutical.disposal.requestedBy" />
                    </StructuredListCell>
                    <StructuredListCell>
                      {selectedRecord.requestedBy}
                    </StructuredListCell>
                  </StructuredListRow>
                  {selectedRecord.approvedBy && (
                    <StructuredListRow>
                      <StructuredListCell>
                        <FormattedMessage id="pharmaceutical.disposal.approvedBy" />
                      </StructuredListCell>
                      <StructuredListCell>
                        {selectedRecord.approvedBy}
                      </StructuredListCell>
                    </StructuredListRow>
                  )}
                  {selectedRecord.executedBy && (
                    <StructuredListRow>
                      <StructuredListCell>
                        <FormattedMessage id="pharmaceutical.disposal.executedBy" />
                      </StructuredListCell>
                      <StructuredListCell>
                        {selectedRecord.executedBy}
                      </StructuredListCell>
                    </StructuredListRow>
                  )}
                  {selectedRecord.witnessId && (
                    <StructuredListRow>
                      <StructuredListCell>
                        <FormattedMessage id="pharmaceutical.disposal.witness" />
                      </StructuredListCell>
                      <StructuredListCell>
                        {selectedRecord.witnessId}
                      </StructuredListCell>
                    </StructuredListRow>
                  )}
                </StructuredListBody>
              </StructuredListWrapper>
              {selectedRecord.justification && (
                <div style={{ marginTop: "1rem" }}>
                  <h5>
                    <FormattedMessage id="pharmaceutical.disposal.justification" />
                  </h5>
                  <p>{selectedRecord.justification}</p>
                </div>
              )}
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default DisposalWorkflow;
