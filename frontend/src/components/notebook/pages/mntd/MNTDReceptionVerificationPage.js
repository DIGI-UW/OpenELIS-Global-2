import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  TextArea,
  Checkbox,
  RadioButtonGroup,
  RadioButton,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Tag,
} from "@carbon/react";
import { Checkmark, WarningAlt, Close, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import Questionnaire from "../../../common/Questionnaire";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDReceptionVerificationPage - Page 2 of the MNTD workflow.
 * Handles laboratory reception and verification of samples.
 * Uses bulk value entry similar to Immunology workflow.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID for questionnaire loading
 */
function MNTDReceptionVerificationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // State for FHIR questionnaire
  const [questionnaire, setQuestionnaire] = useState(null);
  const [questionnaireLoading, setQuestionnaireLoading] = useState(false);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [bulkApplyValues, setBulkApplyValues] = useState({
    receivedDateTime: new Date().toISOString().slice(0, 16),
    receivedBy: "",
    conditionOnArrival: "",
    sampleMatchesRegistration: null,
    labelVerified: null,
    qcResult: "",
    qcRemarks: "",
    failAction: "",
  });
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Load FHIR questionnaire for verification
  useEffect(() => {
    if (notebookId) {
      loadQuestionnaire();
    }
  }, [notebookId]);

  const loadQuestionnaire = useCallback(() => {
    setQuestionnaireLoading(true);
    // Load reception verification questionnaire
    getFromOpenElisServer(
      `/rest/fhir/Questionnaire?name=MNTDReceptionVerification`,
      (res) => {
        if (res && res.entry && res.entry.length > 0) {
          setQuestionnaire(res.entry[0].resource);
        } else {
          setQuestionnaire(null);
        }
        setQuestionnaireLoading(false);
      },
    );
  }, [notebookId]);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              // Reception verification fields from data
              receivedDateTime: sample.data?.receivedDateTime,
              receivedBy: sample.data?.receivedBy,
              conditionOnArrival: sample.data?.conditionOnArrival,
              sampleMatchesRegistration: sample.data?.sampleMatchesRegistration,
              labelVerified: sample.data?.labelVerified,
              qcResult: sample.data?.qcResult,
              qcRemarks: sample.data?.qcRemarks,
              failAction: sample.data?.failAction,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select samples to apply values to.");
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare the data to apply
    const applyData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        receivedDateTime: bulkApplyValues.receivedDateTime,
        receivedBy: bulkApplyValues.receivedBy,
        conditionOnArrival: bulkApplyValues.conditionOnArrival,
        sampleMatchesRegistration: bulkApplyValues.sampleMatchesRegistration,
        labelVerified: bulkApplyValues.labelVerified,
        qcResult: bulkApplyValues.qcResult,
        qcRemarks: bulkApplyValues.qcRemarks,
        failAction: bulkApplyValues.failAction,
      },
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            `Applied values to ${selectedSampleIds.length} samples.`,
          );
          setBulkApplyModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to apply values. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    bulkApplyValues,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle marking samples as verified (QC complete)
  const handleMarkVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    // Check if all selected samples have QC result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingQC = selectedSamples.filter((s) => !s.qcResult);
    if (missingQC.length > 0) {
      setError(
        `${missingQC.length} sample(s) are missing QC result. Please complete verification first.`,
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            `Marked ${selectedSampleIds.length} samples as verified.`,
          );
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const qcPassedCount = samples.filter((s) => s.qcResult === "Pass").length;
  const qcFailedCount = samples.filter((s) => s.qcResult === "Fail").length;
  const qcPendingCount = samples.filter((s) => !s.qcResult).length;
  const verifiedCount = samples.filter((s) => s.status === "COMPLETED").length;

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      receivedDateTime: new Date().toISOString().slice(0, 16),
      receivedBy: "",
      conditionOnArrival: "",
      sampleMatchesRegistration: null,
      labelVerified: null,
      qcResult: "",
      qcRemarks: "",
      failAction: "",
    });
  };

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    if (qcResult === "Pass with remarks") return <Tag type="teal">Pass*</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  return (
    <div className="mntd-reception-verification-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.receptionVerification.title"
            defaultMessage="Laboratory Reception & Verification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.receptionVerification.description"
            defaultMessage="Confirm physical receipt and validate sample quality. Use bulk value entry to verify multiple samples at once. Record receipt information, verification status, and QC assessment."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcPending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => {
            resetBulkApplyValues();
            setBulkApplyModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.mntd.bulkApply"
            defaultMessage="Bulk Apply Values ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkVerified}
          >
            <FormattedMessage
              id="notebook.page.mntd.markVerified"
              defaultMessage="Mark as Verified ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            { key: "receivedDateTime", header: "Received" },
            { key: "receivedBy", header: "Received By" },
            {
              key: "qcResult",
              header: "QC Result",
              render: (value) => getQCTag(value),
            },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.mntd.receptionVerification.empty"
              defaultMessage="No samples available for verification. Samples must be created in the Sample Intake page first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.bulkApply.title",
          defaultMessage: "Bulk Apply Reception & Verification Values",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({ id: "label.apply", defaultMessage: "Apply" })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.mntd.bulkApply.description"
            defaultMessage="Apply the following values to {count} selected sample(s). Only non-empty values will be applied."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Receipt Information */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              Receipt Information
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">Received date & time</label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={bulkApplyValues.receivedDateTime}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    receivedDateTime: e.target.value,
                  }))
                }
              />
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">Received by (staff name)</label>
              <input
                type="text"
                className="cds--text-input"
                value={bulkApplyValues.receivedBy}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    receivedBy: e.target.value,
                  }))
                }
                placeholder="Enter staff name"
              />
            </div>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="conditionOnArrival"
              labelText="Condition on arrival"
              value={bulkApplyValues.conditionOnArrival}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  conditionOnArrival: e.target.value,
                }))
              }
              placeholder="Describe sample condition..."
            />
          </Column>

          {/* Verification */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              Verification
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText="Sample matches registration"
              name="sampleMatchesRegistration"
              valueSelected={
                bulkApplyValues.sampleMatchesRegistration === true
                  ? "yes"
                  : bulkApplyValues.sampleMatchesRegistration === false
                    ? "no"
                    : ""
              }
              onChange={(value) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  sampleMatchesRegistration: value === "yes",
                }))
              }
            >
              <RadioButton labelText="Yes" value="yes" id="match-yes" />
              <RadioButton labelText="No" value="no" id="match-no" />
            </RadioButtonGroup>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText="Label verified"
              name="labelVerified"
              valueSelected={
                bulkApplyValues.labelVerified === true
                  ? "yes"
                  : bulkApplyValues.labelVerified === false
                    ? "no"
                    : ""
              }
              onChange={(value) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  labelVerified: value === "yes",
                }))
              }
            >
              <RadioButton labelText="Yes" value="yes" id="label-yes" />
              <RadioButton labelText="No" value="no" id="label-no" />
            </RadioButtonGroup>
          </Column>

          {/* Quality Assessment */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              Quality Assessment
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="qcResult"
              labelText="QC Result"
              value={bulkApplyValues.qcResult}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  qcResult: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select QC result..." />
              <SelectItem value="Pass" text="Pass" />
              <SelectItem value="Fail" text="Fail" />
              <SelectItem value="Pass with remarks" text="Pass with remarks" />
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            {bulkApplyValues.qcResult === "Fail" && (
              <Select
                id="failAction"
                labelText="Action if Fail"
                value={bulkApplyValues.failAction}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    failAction: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select action..." />
                <SelectItem
                  value="Notify researcher"
                  text="Notify researcher"
                />
                <SelectItem value="Discarded" text="Discarded" />
                <SelectItem
                  value="Proceed with remarks"
                  text="Proceed with remarks"
                />
              </Select>
            )}
          </Column>
          <Column lg={16} md={8} sm={4}>
            {(bulkApplyValues.qcResult === "Fail" ||
              bulkApplyValues.qcResult === "Pass with remarks") && (
              <TextArea
                id="qcRemarks"
                labelText="QC Remarks"
                value={bulkApplyValues.qcRemarks}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    qcRemarks: e.target.value,
                  }))
                }
                placeholder="Enter QC remarks..."
              />
            )}
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default MNTDReceptionVerificationPage;
