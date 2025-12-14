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
  Checkbox,
  Tag,
  Loading,
  InlineNotification,
  Tile,
} from "@carbon/react";
import {
  Add,
  View,
  Checkmark,
  Close,
  Link as LinkIcon,
  WarningAlt,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../utils/Utils";

const AssayRunList = () => {
  const intl = useIntl();
  const [assayRuns, setAssayRuns] = useState([]);
  const [samples, setSamples] = useState([]);
  const [pendingReview, setPendingReview] = useState([]);
  const [oosAssays, setOosAssays] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isResultsModalOpen, setIsResultsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedAssay, setSelectedAssay] = useState(null);
  const [formData, setFormData] = useState({
    sampleId: "",
    assayType: "HPLC_POTENCY",
    instrumentId: "",
    positiveControlId: "",
    negativeControlId: "",
    numberOfReplicates: 3,
  });
  const [resultsData, setResultsData] = useState({
    rawResults: "",
    calculatedResults: "",
    oosFlag: false,
  });

  const fetchAssayRuns = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/pharmaceutical/assay-runs", (response) => {
      if (response) {
        setAssayRuns(response);
      }
      setLoading(false);
    });
  }, []);

  const fetchSamples = useCallback(() => {
    getFromOpenElisServer("/rest/pharmaceutical/samples", (response) => {
      if (response) {
        setSamples(response);
      }
    });
  }, []);

  const fetchPendingReview = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/assay-runs/pending-review",
      (response) => {
        if (response) {
          setPendingReview(response);
        }
      },
    );
  }, []);

  const fetchOosAssays = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/assay-runs/oos?oosFlag=true",
      (response) => {
        if (response) {
          setOosAssays(response);
        }
      },
    );
  }, []);

  useEffect(() => {
    fetchAssayRuns();
    fetchSamples();
    fetchPendingReview();
    fetchOosAssays();
  }, [fetchAssayRuns, fetchSamples, fetchPendingReview, fetchOosAssays]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleResultsInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setResultsData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleInitiateAssay = () => {
    postToOpenElisServer(
      `/rest/pharmaceutical/assay-runs/sample/${formData.sampleId}`,
      JSON.stringify({
        assayType: formData.assayType,
        instrumentId: formData.instrumentId,
        positiveControlId: formData.positiveControlId,
        negativeControlId: formData.negativeControlId,
        numberOfReplicates: parseInt(formData.numberOfReplicates),
      }),
      (response) => {
        if (response) {
          setIsCreateModalOpen(false);
          setFormData({
            sampleId: "",
            assayType: "HPLC_POTENCY",
            instrumentId: "",
            positiveControlId: "",
            negativeControlId: "",
            numberOfReplicates: 3,
          });
          fetchAssayRuns();
        } else {
          setError("Failed to initiate assay run");
        }
      },
    );
  };

  const handleRecordResults = () => {
    if (!selectedAssay) return;

    putToOpenElisServer(
      `/rest/pharmaceutical/assay-runs/${selectedAssay.id}/results`,
      JSON.stringify(resultsData),
      (response) => {
        if (response) {
          setIsResultsModalOpen(false);
          setSelectedAssay(null);
          setResultsData({
            rawResults: "",
            calculatedResults: "",
            oosFlag: false,
          });
          fetchAssayRuns();
          fetchOosAssays();
        } else {
          setError("Failed to record results");
        }
      },
    );
  };

  const handleSubmitForReview = (assayId) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/assay-runs/${assayId}/submit-review`,
      null,
      (response) => {
        if (response) {
          fetchAssayRuns();
          fetchPendingReview();
        } else {
          setError("Failed to submit for review");
        }
      },
    );
  };

  const handleApprove = (assayId) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/assay-runs/${assayId}/approve`,
      null,
      (response) => {
        if (response) {
          fetchAssayRuns();
          fetchPendingReview();
        } else {
          setError("Failed to approve assay run");
        }
      },
    );
  };

  const handleReject = (assayId, reason) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/assay-runs/${assayId}/reject?rejectionReason=${encodeURIComponent(
        reason,
      )}`,
      null,
      (response) => {
        if (response) {
          fetchAssayRuns();
          fetchPendingReview();
        } else {
          setError("Failed to reject assay run");
        }
      },
    );
  };

  const openResultsModal = (assay) => {
    setSelectedAssay(assay);
    setResultsData({
      rawResults: assay.rawResults || "",
      calculatedResults: assay.calculatedResults || "",
      oosFlag: assay.oosFlag || false,
    });
    setIsResultsModalOpen(true);
  };

  const openViewModal = (assay) => {
    setSelectedAssay(assay);
    setIsViewModalOpen(true);
  };

  const getStatusTag = (status) => {
    const statusColors = {
      INITIATED: "blue",
      DATA_ENTRY: "cyan",
      PENDING_REVIEW: "purple",
      APPROVED: "green",
      REJECTED: "red",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "pharmaceutical.assay.id" }),
    },
    {
      key: "sampleId",
      header: intl.formatMessage({ id: "pharmaceutical.assay.sample" }),
    },
    {
      key: "assayType",
      header: intl.formatMessage({ id: "pharmaceutical.assay.type" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.assay.status" }),
    },
    {
      key: "oosFlag",
      header: intl.formatMessage({ id: "pharmaceutical.assay.oos" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = assayRuns.map((assay) => ({
    id: String(assay.id),
    sampleId: assay.sampleId,
    assayType: assay.assayType,
    status: assay.status,
    oosFlag: assay.oosFlag,
    actions: assay,
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

        {oosAssays.length > 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--critical">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <WarningAlt size={20} />
              <strong>
                <FormattedMessage id="pharmaceutical.assay.oos.warning" />
              </strong>
            </div>
            <p>
              {oosAssays.length}{" "}
              <FormattedMessage id="pharmaceutical.assay.oos.count" />
            </p>
          </Tile>
        )}

        {pendingReview.length > 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--info">
            <strong>
              <FormattedMessage id="pharmaceutical.assay.pendingReview" />
            </strong>
            <p>
              {pendingReview.length}{" "}
              <FormattedMessage id="pharmaceutical.assay.pendingReview.count" />
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
                    onClick={() => setIsCreateModalOpen(true)}
                  >
                    <FormattedMessage id="pharmaceutical.assay.initiate" />
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
                          ) : cell.info.header === "oosFlag" ? (
                            cell.value ? (
                              <Tag type="red">
                                <FormattedMessage id="pharmaceutical.assay.oos.yes" />
                              </Tag>
                            ) : (
                              <Tag type="green">
                                <FormattedMessage id="pharmaceutical.assay.oos.no" />
                              </Tag>
                            )
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
                              {cell.value.status === "INITIATED" && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() => openResultsModal(cell.value)}
                                >
                                  <FormattedMessage id="pharmaceutical.assay.enterResults" />
                                </Button>
                              )}
                              {cell.value.status === "DATA_ENTRY" && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() =>
                                    handleSubmitForReview(cell.value.id)
                                  }
                                >
                                  <FormattedMessage id="pharmaceutical.assay.submitReview" />
                                </Button>
                              )}
                              {cell.value.status === "PENDING_REVIEW" && (
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

        {/* Initiate Assay Modal */}
        <Modal
          open={isCreateModalOpen}
          onRequestClose={() => setIsCreateModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.assay.initiate.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.save" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleInitiateAssay}
        >
          <Form className="pharmaceutical-form">
            <Select
              id="sampleId"
              name="sampleId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.sample",
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
              id="assayType"
              name="assayType"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.type",
              })}
              value={formData.assayType}
              onChange={handleInputChange}
            >
              <SelectItem value="HPLC_POTENCY" text="HPLC Potency" />
              <SelectItem
                value="TLC_IDENTIFICATION"
                text="TLC Identification"
              />
              <SelectItem value="DISSOLUTION" text="Dissolution" />
              <SelectItem
                value="CONTENT_UNIFORMITY"
                text="Content Uniformity"
              />
              <SelectItem
                value="RELATED_SUBSTANCES"
                text="Related Substances"
              />
              <SelectItem value="WATER_CONTENT" text="Water Content" />
              <SelectItem value="MICROBIAL" text="Microbial" />
            </Select>
            <TextInput
              id="instrumentId"
              name="instrumentId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.instrument",
              })}
              value={formData.instrumentId}
              onChange={handleInputChange}
            />
            <TextInput
              id="numberOfReplicates"
              name="numberOfReplicates"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.replicates",
              })}
              value={formData.numberOfReplicates}
              onChange={handleInputChange}
              type="number"
            />
          </Form>
        </Modal>

        {/* Record Results Modal */}
        <Modal
          open={isResultsModalOpen}
          onRequestClose={() => setIsResultsModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.assay.results.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.save" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleRecordResults}
        >
          <Form className="pharmaceutical-form">
            <TextArea
              id="rawResults"
              name="rawResults"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.rawResults",
              })}
              value={resultsData.rawResults}
              onChange={handleResultsInputChange}
              rows={4}
            />
            <TextArea
              id="calculatedResults"
              name="calculatedResults"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.calculatedResults",
              })}
              value={resultsData.calculatedResults}
              onChange={handleResultsInputChange}
              rows={4}
            />
            <Checkbox
              id="oosFlag"
              name="oosFlag"
              labelText={intl.formatMessage({
                id: "pharmaceutical.assay.oos.flag",
              })}
              checked={resultsData.oosFlag}
              onChange={handleResultsInputChange}
            />
            {resultsData.oosFlag && (
              <div className="oos-warning">
                <WarningAlt size={16} />
                <span>
                  <FormattedMessage id="pharmaceutical.assay.oos.warning.message" />
                </span>
              </div>
            )}
          </Form>
        </Modal>

        {/* View Assay Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.assay.details.title",
          })}
          passiveModal
        >
          {selectedAssay && (
            <div>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.assay.id" />:
                </strong>{" "}
                {selectedAssay.id}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.assay.type" />:
                </strong>{" "}
                {selectedAssay.assayType}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.assay.status" />:
                </strong>{" "}
                {getStatusTag(selectedAssay.status)}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.assay.analyst" />:
                </strong>{" "}
                {selectedAssay.analystId}
              </p>
              {selectedAssay.rawResults && (
                <div className="assay-results-section">
                  <h5>
                    <FormattedMessage id="pharmaceutical.assay.rawResults" />
                  </h5>
                  <pre>{selectedAssay.rawResults}</pre>
                </div>
              )}
              {selectedAssay.calculatedResults && (
                <div className="assay-results-section">
                  <h5>
                    <FormattedMessage id="pharmaceutical.assay.calculatedResults" />
                  </h5>
                  <pre>{selectedAssay.calculatedResults}</pre>
                </div>
              )}
              {selectedAssay.oosFlag && (
                <div className="oos-warning">
                  <WarningAlt size={16} />
                  <span>
                    <FormattedMessage id="pharmaceutical.assay.oos" />
                  </span>
                </div>
              )}
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default AssayRunList;
