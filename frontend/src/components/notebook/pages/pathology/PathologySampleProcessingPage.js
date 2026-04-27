import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextArea,
  Select,
  SelectItem,
  Checkbox,
  Modal,
} from "@carbon/react";
import { Renew, ChevronRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

function PathologySampleProcessingPage({
  entryId,
  pageData,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [processingData, setProcessingData] = useState({
    tissueProcessing: false,
    fluidProcessing: false,
    fluidProcessingMethod: "",
    qcPassed: false,
    qcNotes: "",
  });

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!entryId || !pageData?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/pathology/workflow/samples-ready?entryId=${entryId}&currentStep=blocks`,
      (workflowResponse) => {
        if (!componentMounted.current) return;

        getFromOpenElisServer(
          `/rest/notebook/page/${pageData.id}/samples`,
          (pageResponse) => {
            if (!componentMounted.current) return;

            const pageSampleMap = {};
            if (Array.isArray(pageResponse)) {
              pageResponse.forEach((ps) => {
                const sampleId = String(ps.sampleItemId || ps.id);
                pageSampleMap[sampleId] = ps;
              });
            }

            if (Array.isArray(workflowResponse)) {
              const transformedSamples = workflowResponse.map((sample) => {
                const sampleId = String(sample.id || sample.sampleItemId);
                const pageSample =
                  pageSampleMap[sampleId] ||
                  pageSampleMap[sampleId.split("_")[0]];
                const processingPageData = pageSample?.data || {};

                return {
                  id: sampleId,
                  externalId: sample.externalId,
                  accessionNumber: sample.accessionNumber,
                  sampleType:
                    sample.sampleType || sample.typeOfSample?.description,
                  status:
                    pageSample?.pageStatus || pageSample?.status || "PENDING",
                  tissueProcessing:
                    processingPageData.tissueProcessing === true,
                  fluidProcessing: processingPageData.fluidProcessing === true,
                  fluidProcessingMethod:
                    processingPageData.fluidProcessingMethod || "",
                  qcPassed: processingPageData.qcPassed === true,
                };
              });
              setSamples(transformedSamples);
            } else {
              setSamples([]);
            }

            setLoading(false);
          },
        );
      },
    );
  }, [entryId, pageData?.id]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setProcessingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const openProcessingModal = (sample) => {
    if (!sample) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.sampleMissing",
          defaultMessage:
            "Unable to open sample processing. Please refresh and try again.",
        }),
      );
      return;
    }

    setSelectedSample(sample);
    setProcessingData({
      tissueProcessing: sample.tissueProcessing || false,
      fluidProcessing: sample.fluidProcessing || false,
      fluidProcessingMethod: sample.fluidProcessingMethod || "",
      qcPassed: sample.qcPassed || false,
      qcNotes: "",
    });
    setProcessingModalOpen(true);
  };

  const handleSubmitProcessing = () => {
    if (submitting) return;

    if (!processingData.tissueProcessing && !processingData.fluidProcessing) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.requiredFields",
          defaultMessage:
            "Select at least one option: Tissue processing or Fluid processing.",
        }),
      );
      return;
    }

    if (
      processingData.fluidProcessing &&
      !processingData.fluidProcessingMethod
    ) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.fluidMethodRequired",
          defaultMessage:
            "Please select a fluid processing method when fluid processing is checked.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const payload = {
      sampleId: selectedSample?.id,
      pageId: pageData?.id,
      entryId,
      ...processingData,
    };

    postToOpenElisServer(
      `/rest/notebook/pathology/processing/submit`,
      JSON.stringify(payload),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.processing.success",
              defaultMessage: "Processing data submitted successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.submitFailed",
              defaultMessage:
                "Failed to submit processing data. Please try again.",
            }),
          );
        }
      },
    );
  };

  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(
              intl.formatMessage({
                id: "pathology.processing.error.statusUpdateFailed",
                defaultMessage:
                  "Failed to update sample status. Please try again.",
              }),
            );
          }
        },
      );
    },
    [pageData?.id, loadPageSamples, onProgressUpdate, intl],
  );

  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.processing.bulkCompleted",
                defaultMessage:
                  "Successfully marked {count} samples as completed.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.statusUpdateFailed",
              defaultMessage:
                "Failed to update sample status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  const processedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  return (
    <div className="pathology-processing-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.processing.title"
            defaultMessage="Sample Processing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.processing.description"
            defaultMessage="Record tissue/fluid processing and stage-level quality control."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div
        className="page-actions-bar"
        style={{
          display: "flex",
          justifyContent: "space-between",
          marginBottom: "1rem",
        }}
      >
        <Button
          kind="secondary"
          size="sm"
          onClick={handleBulkMarkCompleted}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="pathology.page.processing.markCompleted"
            defaultMessage="Mark Processing Complete ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="pathology.page.processing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      <SampleGrid
        gridId="pathology-sample-processing"
        samples={samples}
        selectedIds={selectedSampleIds}
        onSelectionChange={setSelectedSampleIds}
        onStatusChange={handleStatusChange}
        statusFilter="ALL"
        onStatusFilterChange={() => {}}
        showSelection={true}
        showHierarchy={true}
        loading={loading}
        additionalColumns={[
          {
            key: "process",
            header: intl.formatMessage({
              id: "pathology.page.processing.processColumn",
              defaultMessage: "Process",
            }),
            render: (_, sample) => (
              <Button
                kind="ghost"
                size="sm"
                renderIcon={ChevronRight}
                onClick={() => openProcessingModal(sample)}
              >
                <FormattedMessage
                  id="pathology.page.processing.process"
                  defaultMessage="Process"
                />
              </Button>
            ),
          },
          {
            key: "stageQc",
            header: intl.formatMessage({
              id: "pathology.page.processing.stageQcColumn",
              defaultMessage: "Stage QC",
            }),
            render: (_, sample) =>
              sample.qcPassed ? (
                <span style={{ color: "#198038", fontWeight: 600 }}>
                  <FormattedMessage
                    id="pathology.page.processing.qc.passed"
                    defaultMessage="Passed"
                  />
                </span>
              ) : (
                <span style={{ color: "#8d8d8d" }}>
                  <FormattedMessage
                    id="pathology.page.processing.qc.pending"
                    defaultMessage="Pending"
                  />
                </span>
              ),
          },
        ]}
      />

      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="pathology.page.processing.empty"
              defaultMessage="No samples available for processing. Samples must pass Cassette Setup first."
            />
          </p>
        </div>
      )}

      <Modal
        open={processingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.processing.title",
            defaultMessage: "Process Sample - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          setError(null);
        }}
        onRequestSubmit={handleSubmitProcessing}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "0.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.stage.title"
                defaultMessage="Sample Processing"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="tissueProcessing"
              name="tissueProcessing"
              labelText={intl.formatMessage({
                id: "pathology.processing.tissueProcessingCheckbox",
                defaultMessage: "Tissue processing",
              })}
              checked={processingData.tissueProcessing}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="fluidProcessing"
              name="fluidProcessing"
              labelText={intl.formatMessage({
                id: "pathology.processing.fluidProcessingCheckbox",
                defaultMessage: "Fluid processing",
              })}
              checked={processingData.fluidProcessing}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="fluidProcessingMethod"
              name="fluidProcessingMethod"
              labelText={intl.formatMessage({
                id: "pathology.processing.fluidProcessingMethod",
                defaultMessage: "Fluid processing method",
              })}
              value={processingData.fluidProcessingMethod}
              onChange={handleInputChange}
              disabled={!processingData.fluidProcessing}
            >
              <SelectItem value="" text="" />
              <SelectItem value="centrifuge" text="Centrifuge" />
              <SelectItem value="shaker" text="Shaker" />
              <SelectItem value="other" text="Other" />
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.qc.title"
                defaultMessage="Sample Processing Quality Control (Stage QC)"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="qcPassed"
              name="qcPassed"
              labelText={intl.formatMessage({
                id: "pathology.processing.qc.completed",
                defaultMessage: "QC passed",
              })}
              checked={processingData.qcPassed}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={12} md={4} sm={4}>
            <TextArea
              id="qcNotes"
              name="qcNotes"
              labelText={intl.formatMessage({
                id: "pathology.processing.qc.notes",
                defaultMessage: "QC notes",
              })}
              value={processingData.qcNotes}
              onChange={handleInputChange}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologySampleProcessingPage;
