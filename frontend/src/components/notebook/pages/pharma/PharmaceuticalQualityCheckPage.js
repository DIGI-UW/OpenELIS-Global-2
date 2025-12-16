import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  Tile,
  Modal,
  RadioButtonGroup,
  RadioButton,
  Tag,
} from "@carbon/react";
import { Checkmark, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalQualityCheckPage - Page 2 of the Pharmaceuticals workflow.
 * Performs initial QC verification similar to MNTDReceptionVerificationPage.
 *
 * QC Data Points (per pharma spec):
 * - Container Integrity (intact/compromised)
 * - Label Readability (readable/illegible)
 * - Appearance Match (yes/no - compared to specification)
 * - Environmental Deviations (yes/no with notes)
 * - QC Result (Pass/Fail/Pass with remarks)
 * - QC Remarks
 * - Action if Fail (Discard/Notify/Proceed with remarks)
 */
function PharmaceuticalQualityCheckPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);
  const [bulkApplyValues, setBulkApplyValues] = useState({
    containerIntegrity: "",
    labelReadability: "",
    appearanceMatch: null,
    environmentalDeviation: null,
    deviationNotes: "",
    qcResult: "",
    qcRemarks: "",
    failAction: "",
  });

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    setSuccessMessage(null);

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
              status: sample.pageStatus || sample.status || "PENDING",
              // QC fields from data
              containerIntegrity: sample.data?.containerIntegrity,
              labelReadability: sample.data?.labelReadability,
              appearanceMatch: sample.data?.appearanceMatch,
              environmentalDeviation: sample.data?.environmentalDeviation,
              deviationNotes: sample.data?.deviationNotes,
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset bulk apply form
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      containerIntegrity: "",
      labelReadability: "",
      appearanceMatch: null,
      environmentalDeviation: null,
      deviationNotes: "",
      qcResult: "",
      qcRemarks: "",
      failAction: "",
    });
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.qc.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Build data object with only non-empty values
    const data = {};
    if (bulkApplyValues.containerIntegrity)
      data.containerIntegrity = bulkApplyValues.containerIntegrity;
    if (bulkApplyValues.labelReadability)
      data.labelReadability = bulkApplyValues.labelReadability;
    if (bulkApplyValues.appearanceMatch !== null)
      data.appearanceMatch = bulkApplyValues.appearanceMatch;
    if (bulkApplyValues.environmentalDeviation !== null)
      data.environmentalDeviation = bulkApplyValues.environmentalDeviation;
    if (bulkApplyValues.deviationNotes)
      data.deviationNotes = bulkApplyValues.deviationNotes;
    if (bulkApplyValues.qcResult) data.qcResult = bulkApplyValues.qcResult;
    if (bulkApplyValues.qcRemarks) data.qcRemarks = bulkApplyValues.qcRemarks;
    if (bulkApplyValues.failAction)
      data.failAction = bulkApplyValues.failAction;

    if (Object.keys(data).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.qc.error.noData",
          defaultMessage: "Please fill in at least one field before applying.",
        }),
      );
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.applied",
                defaultMessage: "Applied QC values to {count} sample(s).",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkApplyModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.qc.error.apply",
              defaultMessage: "Failed to apply QC values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    bulkApplyValues,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark samples as QC complete
  const handleMarkQcComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have QC result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingQC = selectedSamples.filter((s) => !s.qcResult);
    if (missingQC.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.pharma.qc.error.missingQC",
            defaultMessage:
              "{count} sample(s) are missing QC result. Please complete verification first.",
          },
          { count: missingQC.length },
        ),
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
            intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.completed",
                defaultMessage:
                  "Marked {count} sample(s) as QC Complete. They can now proceed to Processing.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Calculate stats
  const qcPassedCount = samples.filter((s) => s.qcResult === "Pass").length;
  const qcFailedCount = samples.filter((s) => s.qcResult === "Fail").length;
  const qcPendingCount = samples.filter((s) => !s.qcResult).length;
  const verifiedCount = samples.filter((s) => s.status === "COMPLETED").length;

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    if (qcResult === "Pass with remarks") return <Tag type="teal">Pass*</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  return (
    <div className="pharma-quality-check-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.qc.title"
            defaultMessage="Raw Sample Quality Check (QC)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.qc.description"
            defaultMessage="Verify container integrity, label readability, appearance vs specification, and environmental deviations. Use Bulk Apply to verify multiple samples at once."
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
                  id="notebook.page.pharma.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.passed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.failed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.pending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.verified"
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
            id="notebook.page.pharma.qc.bulkApply"
            defaultMessage="Bulk Apply QC ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkQcComplete}
          >
            <FormattedMessage
              id="notebook.page.pharma.qc.markComplete"
              defaultMessage="Mark QC Complete ({count})"
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
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            { key: "containerIntegrity", header: "Container" },
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
              id="notebook.page.pharma.qc.empty"
              defaultMessage="No samples available yet. Complete Sample Creation first, then perform QC."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.bulkApply.title",
          defaultMessage: "Bulk Apply QC Values",
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
            id="notebook.pharma.bulkApply.description"
            defaultMessage="Apply the following QC values to {count} selected sample(s). Only non-empty values will be applied."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Physical Inspection */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.pharma.qc.section.physical"
                defaultMessage="Physical Inspection"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="containerIntegrity"
              labelText={intl.formatMessage({
                id: "notebook.pharma.qc.field.container",
                defaultMessage: "Container Integrity",
              })}
              value={bulkApplyValues.containerIntegrity}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  containerIntegrity: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select..." />
              <SelectItem value="Intact" text="Intact" />
              <SelectItem value="Compromised" text="Compromised" />
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="labelReadability"
              labelText={intl.formatMessage({
                id: "notebook.pharma.qc.field.label",
                defaultMessage: "Label Readability",
              })}
              value={bulkApplyValues.labelReadability}
              onChange={(e) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  labelReadability: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select..." />
              <SelectItem value="Readable" text="Readable" />
              <SelectItem value="Illegible" text="Illegible" />
            </Select>
          </Column>

          {/* Verification */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.pharma.qc.section.verification"
                defaultMessage="Verification"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.pharma.qc.field.appearance",
                defaultMessage: "Appearance matches specification",
              })}
              name="appearanceMatch"
              valueSelected={
                bulkApplyValues.appearanceMatch === true
                  ? "yes"
                  : bulkApplyValues.appearanceMatch === false
                    ? "no"
                    : ""
              }
              onChange={(value) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  appearanceMatch: value === "yes",
                }))
              }
            >
              <RadioButton labelText="Yes" value="yes" id="appearance-yes" />
              <RadioButton labelText="No" value="no" id="appearance-no" />
            </RadioButtonGroup>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.pharma.qc.field.deviation",
                defaultMessage: "Environmental deviation detected",
              })}
              name="environmentalDeviation"
              valueSelected={
                bulkApplyValues.environmentalDeviation === true
                  ? "yes"
                  : bulkApplyValues.environmentalDeviation === false
                    ? "no"
                    : ""
              }
              onChange={(value) =>
                setBulkApplyValues((prev) => ({
                  ...prev,
                  environmentalDeviation: value === "yes",
                }))
              }
            >
              <RadioButton labelText="Yes" value="yes" id="deviation-yes" />
              <RadioButton labelText="No" value="no" id="deviation-no" />
            </RadioButtonGroup>
          </Column>

          {bulkApplyValues.environmentalDeviation && (
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="deviationNotes"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.qc.field.deviationNotes",
                  defaultMessage: "Deviation Notes",
                })}
                value={bulkApplyValues.deviationNotes}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    deviationNotes: e.target.value,
                  }))
                }
                placeholder="Describe environmental deviation..."
              />
            </Column>
          )}

          {/* Quality Assessment */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.pharma.qc.section.assessment"
                defaultMessage="Quality Assessment"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="qcResult"
              labelText={intl.formatMessage({
                id: "notebook.pharma.qc.field.result",
                defaultMessage: "QC Result",
              })}
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
                labelText={intl.formatMessage({
                  id: "notebook.pharma.qc.field.failAction",
                  defaultMessage: "Action if Fail",
                })}
                value={bulkApplyValues.failAction}
                onChange={(e) =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    failAction: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select action..." />
                <SelectItem value="Discard" text="Discard sample" />
                <SelectItem value="Notify" text="Notify submitter" />
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
                labelText={intl.formatMessage({
                  id: "notebook.pharma.qc.field.remarks",
                  defaultMessage: "QC Remarks",
                })}
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

export default PharmaceuticalQualityCheckPage;
