import { useState, useEffect, useRef, useCallback } from "react";
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
  Checkbox,
  TextInput,
  MultiSelect,
} from "@carbon/react";
import { Checkmark, Edit, InventoryManagement } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalQualityCheckPage - Page 2 of the Pharmaceuticals workflow.
 * Performs initial QC verification with sample type-specific criteria.
 *
 * QC RAW SAMPLE CRITERIA:
 * 1. Pharmaceutical Sample Checks:
 *    - Label accuracy (matches requisition)
 *    - Container integrity (no leaks/tampering)
 *    - Quantity/Volume (meets minimum)
 *    - Physical appearance (matches pharmacopeial description)
 *    - Expiry/Validity
 *    - Storage/transport compliance (e.g., 25°C / 60% RH)
 *
 * 2. Biological Sample Checks:
 *    - Label accuracy
 *    - Container integrity
 *    - Volume/Quantity
 *    - Visual integrity (free from hemolysis/contamination)
 *    - Transport/Storage compliance
 *    - Biosafety risk labeling
 *
 * 3. Microbiological/Environmental Checks:
 *    - Container integrity (especially sterility)
 *    - Sample Volume/Quantity
 *    - Holding time (within timeframe)
 *    - Transport/Storage compliance
 *
 * Pass/Fail Actions:
 * - Pass: Proceed to processing
 * - Fail: Reject, log in LMIS, notify submitter, request replacement
 */
function PharmaceuticalQualityCheckPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
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
  const [sampleType, setSampleType] = useState("pharmaceutical");

  // Instruments from inventory
  const [instruments, setInstruments] = useState([]);
  const [loadingInstruments, setLoadingInstruments] = useState(false);

  // Pharmaceutical Sample Checks
  const [bulkApplyValues, setBulkApplyValues] = useState({
    // Pharmaceutical checks
    pharmaLabelAccuracy: null,
    pharmaContainerIntegrity: null,
    pharmaQuantityVolume: null,
    pharmaPhysicalAppearance: null,
    pharmaExpiryValidity: null,
    pharmaStorageCompliance: null,
    pharmaStorageConditions: "",
    // Biological checks
    bioLabelAccuracy: null,
    bioContainerIntegrity: null,
    bioVolumeQuantity: null,
    bioVisualIntegrity: null,
    bioTransportCompliance: null,
    bioBiosafetyLabeling: null,
    // Microbiological/Environmental checks
    microContainerIntegrity: null,
    microSampleVolume: null,
    microHoldingTime: null,
    microTransportCompliance: null,
    // QC Result & Actions
    qcResult: "",
    qcRemarks: "",
    failAction: "",
    rejectionReason: "",
    notifySubmitter: false,
    requestReplacement: false,
    // Instruments used for QC
    selectedInstruments: [],
  });

  // Load instruments from template or inventory
  const loadInstruments = useCallback(() => {
    // If template has configured instruments, use those exclusively
    if (templateInstruments && templateInstruments.length > 0) {
      setInstruments(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          label: analyzer.value,
          name: analyzer.value,
        })),
      );
      setLoadingInstruments(false);
      return;
    }

    // Fallback: load from inventory if no template instruments configured
    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: i.id,
                label: `${i.name} (${i.serialNumber || "N/A"})`,
                name: i.name,
                serialNumber: i.serialNumber,
                ...i,
              })),
            );
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, [templateInstruments]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadInstruments();
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
              sampleTypeCategory: sample.data?.sampleType, // QC category (pharmaceutical/biological/microbiological)
              status: sample.pageStatus || sample.status || "PENDING",
              // QC fields from data
              qcResult: sample.data?.qcResult,
              qcRemarks: sample.data?.qcRemarks,
              failAction: sample.data?.failAction,
              rejectionReason: sample.data?.rejectionReason,
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
    setSampleType("pharmaceutical");
    setBulkApplyValues({
      // Pharmaceutical checks
      pharmaLabelAccuracy: null,
      pharmaContainerIntegrity: null,
      pharmaQuantityVolume: null,
      pharmaPhysicalAppearance: null,
      pharmaExpiryValidity: null,
      pharmaStorageCompliance: null,
      pharmaStorageConditions: "",
      // Biological checks
      bioLabelAccuracy: null,
      bioContainerIntegrity: null,
      bioVolumeQuantity: null,
      bioVisualIntegrity: null,
      bioTransportCompliance: null,
      bioBiosafetyLabeling: null,
      // Microbiological/Environmental checks
      microContainerIntegrity: null,
      microSampleVolume: null,
      microHoldingTime: null,
      microTransportCompliance: null,
      // QC Result & Actions
      qcResult: "",
      qcRemarks: "",
      failAction: "",
      rejectionReason: "",
      notifySubmitter: false,
      requestReplacement: false,
      // Instruments used for QC
      selectedInstruments: [],
    });
  };

  // Auto-calculate QC result based on checklist items
  const calculateQcResult = useCallback(() => {
    let hasAnyFail = false;
    let hasAnyCheck = false;

    if (sampleType === "pharmaceutical") {
      const pharmaChecks = [
        bulkApplyValues.pharmaLabelAccuracy,
        bulkApplyValues.pharmaContainerIntegrity,
        bulkApplyValues.pharmaQuantityVolume,
        bulkApplyValues.pharmaPhysicalAppearance,
        bulkApplyValues.pharmaExpiryValidity,
        bulkApplyValues.pharmaStorageCompliance,
      ];
      pharmaChecks.forEach((check) => {
        if (check !== null) {
          hasAnyCheck = true;
          if (check === false) hasAnyFail = true;
        }
      });
    } else if (sampleType === "biological") {
      const bioChecks = [
        bulkApplyValues.bioLabelAccuracy,
        bulkApplyValues.bioContainerIntegrity,
        bulkApplyValues.bioVolumeQuantity,
        bulkApplyValues.bioVisualIntegrity,
        bulkApplyValues.bioTransportCompliance,
        bulkApplyValues.bioBiosafetyLabeling,
      ];
      bioChecks.forEach((check) => {
        if (check !== null) {
          hasAnyCheck = true;
          if (check === false) hasAnyFail = true;
        }
      });
    } else if (sampleType === "microbiological") {
      const microChecks = [
        bulkApplyValues.microContainerIntegrity,
        bulkApplyValues.microSampleVolume,
        bulkApplyValues.microHoldingTime,
        bulkApplyValues.microTransportCompliance,
      ];
      microChecks.forEach((check) => {
        if (check !== null) {
          hasAnyCheck = true;
          if (check === false) hasAnyFail = true;
        }
      });
    }

    if (!hasAnyCheck) return "";
    return hasAnyFail ? "Fail" : "Pass";
  }, [sampleType, bulkApplyValues]);

  // Update QC result when checks change
  useEffect(() => {
    const calculatedResult = calculateQcResult();
    if (calculatedResult && bulkApplyValues.qcResult !== calculatedResult) {
      setBulkApplyValues((prev) => ({
        ...prev,
        qcResult: calculatedResult,
      }));
    }
  }, [calculateQcResult, bulkApplyValues.qcResult]);

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

    // Build data object with only non-empty/non-null values based on sample type
    const data = {
      sampleType,
    };

    // Add pharmaceutical checks if applicable
    if (sampleType === "pharmaceutical") {
      if (bulkApplyValues.pharmaLabelAccuracy !== null)
        data.pharmaLabelAccuracy = bulkApplyValues.pharmaLabelAccuracy;
      if (bulkApplyValues.pharmaContainerIntegrity !== null)
        data.pharmaContainerIntegrity =
          bulkApplyValues.pharmaContainerIntegrity;
      if (bulkApplyValues.pharmaQuantityVolume !== null)
        data.pharmaQuantityVolume = bulkApplyValues.pharmaQuantityVolume;
      if (bulkApplyValues.pharmaPhysicalAppearance !== null)
        data.pharmaPhysicalAppearance =
          bulkApplyValues.pharmaPhysicalAppearance;
      if (bulkApplyValues.pharmaExpiryValidity !== null)
        data.pharmaExpiryValidity = bulkApplyValues.pharmaExpiryValidity;
      if (bulkApplyValues.pharmaStorageCompliance !== null)
        data.pharmaStorageCompliance = bulkApplyValues.pharmaStorageCompliance;
      if (bulkApplyValues.pharmaStorageConditions)
        data.pharmaStorageConditions = bulkApplyValues.pharmaStorageConditions;
    }

    // Add biological checks if applicable
    if (sampleType === "biological") {
      if (bulkApplyValues.bioLabelAccuracy !== null)
        data.bioLabelAccuracy = bulkApplyValues.bioLabelAccuracy;
      if (bulkApplyValues.bioContainerIntegrity !== null)
        data.bioContainerIntegrity = bulkApplyValues.bioContainerIntegrity;
      if (bulkApplyValues.bioVolumeQuantity !== null)
        data.bioVolumeQuantity = bulkApplyValues.bioVolumeQuantity;
      if (bulkApplyValues.bioVisualIntegrity !== null)
        data.bioVisualIntegrity = bulkApplyValues.bioVisualIntegrity;
      if (bulkApplyValues.bioTransportCompliance !== null)
        data.bioTransportCompliance = bulkApplyValues.bioTransportCompliance;
      if (bulkApplyValues.bioBiosafetyLabeling !== null)
        data.bioBiosafetyLabeling = bulkApplyValues.bioBiosafetyLabeling;
    }

    // Add microbiological/environmental checks if applicable
    if (sampleType === "microbiological") {
      if (bulkApplyValues.microContainerIntegrity !== null)
        data.microContainerIntegrity = bulkApplyValues.microContainerIntegrity;
      if (bulkApplyValues.microSampleVolume !== null)
        data.microSampleVolume = bulkApplyValues.microSampleVolume;
      if (bulkApplyValues.microHoldingTime !== null)
        data.microHoldingTime = bulkApplyValues.microHoldingTime;
      if (bulkApplyValues.microTransportCompliance !== null)
        data.microTransportCompliance =
          bulkApplyValues.microTransportCompliance;
    }

    // Add QC result and actions
    if (bulkApplyValues.qcResult) data.qcResult = bulkApplyValues.qcResult;
    if (bulkApplyValues.qcRemarks) data.qcRemarks = bulkApplyValues.qcRemarks;
    if (bulkApplyValues.failAction)
      data.failAction = bulkApplyValues.failAction;
    if (bulkApplyValues.rejectionReason)
      data.rejectionReason = bulkApplyValues.rejectionReason;
    if (bulkApplyValues.notifySubmitter)
      data.notifySubmitter = bulkApplyValues.notifySubmitter;
    if (bulkApplyValues.requestReplacement)
      data.requestReplacement = bulkApplyValues.requestReplacement;

    // Add selected instruments
    if (bulkApplyValues.selectedInstruments.length > 0)
      data.selectedInstruments = bulkApplyValues.selectedInstruments;

    // Check if any QC checks were actually performed
    const hasAnyCheck =
      Object.keys(data).filter((k) => k !== "sampleType").length > 0;
    if (!hasAnyCheck) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.qc.error.noData",
          defaultMessage:
            "Please complete at least one QC check before applying.",
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
          const resultMessage =
            bulkApplyValues.qcResult === "Pass"
              ? intl.formatMessage(
                  {
                    id: "notebook.page.pharma.qc.applied.pass",
                    defaultMessage:
                      "QC passed for {count} sample(s). Samples can proceed to processing.",
                  },
                  { count: selectedSampleIds.length },
                )
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage(
                    {
                      id: "notebook.page.pharma.qc.applied.fail",
                      defaultMessage:
                        "QC failed for {count} sample(s). Samples rejected and logged.",
                    },
                    { count: selectedSampleIds.length },
                  )
                : intl.formatMessage(
                    {
                      id: "notebook.page.pharma.qc.applied",
                      defaultMessage: "Applied QC values to {count} sample(s).",
                    },
                    { count: selectedSampleIds.length },
                  );
          setSuccessMessage(resultMessage);
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
    sampleType,
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

    // Separate samples by their fail action
    // Rejected/Discarded samples should NOT proceed to next page
    const rejectedSamples = selectedSamples.filter(
      (s) => s.failAction === "reject" || s.failAction === "discard",
    );
    const passingSamples = selectedSamples.filter(
      (s) => s.failAction !== "reject" && s.failAction !== "discard",
    );

    // Track how many requests we need to make
    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (passingSamples.length > 0 ? 1 : 0) +
      (rejectedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          // Build appropriate success message
          let message = "";
          if (passingSamples.length > 0 && rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.completedMixed",
                defaultMessage:
                  "{passCount} sample(s) completed QC and can proceed to Processing. {rejectCount} sample(s) were rejected/discarded and will not proceed.",
              },
              {
                passCount: passingSamples.length,
                rejectCount: rejectedSamples.length,
              },
            );
          } else if (rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.completedRejected",
                defaultMessage:
                  "{count} sample(s) were rejected/discarded. They will not proceed to Processing.",
              },
              { count: rejectedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.completed",
                defaultMessage:
                  "Marked {count} sample(s) as QC Complete. They can now proceed to Processing.",
              },
              { count: passingSamples.length },
            );
          }
          setSuccessMessage(message);
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
      }
    };

    // Update passing samples to COMPLETED (they will proceed to next page)
    if (passingSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: passingSamples.map((s) => parseInt(s.id, 10)),
          status: "COMPLETED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // Update rejected/discarded samples to SKIPPED (they will NOT proceed to next page)
    if (rejectedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: rejectedSamples.map((s) => parseInt(s.id, 10)),
          status: "SKIPPED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // If no samples to process, just return
    if (totalRequests === 0) {
      return;
    }
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

  // Get sample type tag
  const getSampleTypeTag = (sampleTypeCategory) => {
    if (!sampleTypeCategory) return null;
    if (sampleTypeCategory === "pharmaceutical")
      return <Tag type="blue">Pharma</Tag>;
    if (sampleTypeCategory === "biological")
      return <Tag type="purple">Bio</Tag>;
    if (sampleTypeCategory === "microbiological")
      return <Tag type="cyan">Micro</Tag>;
    return <Tag type="gray">{sampleTypeCategory}</Tag>;
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

      {/* Pending QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.pharma.qc.pendingTable.title"
              defaultMessage="Samples Pending QC"
            />
            <Tag type="gray" className="count-tag">
              {
                samples.filter(
                  (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.pharma.qc.pendingTable.description"
              defaultMessage="Select samples and use 'Bulk Apply QC' to perform quality checks. Samples with QC result can be marked as complete."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-qc"
            samples={samples.filter(
              (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
            )}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "sampleTypeCategory",
                header: intl.formatMessage({
                  id: "notebook.grid.qcCategory",
                  defaultMessage: "QC Category",
                }),
                render: (value) => getSampleTypeTag(value),
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.pharma.qc.pendingTable.empty"
                  defaultMessage="No samples pending QC. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.pharma.qc.completedTable.title"
              defaultMessage="QC Completed Samples"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.pharma.qc.completedTable.description"
              defaultMessage="Samples that have completed quality check. Passed samples can proceed to processing."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="completed-qc"
            samples={samples.filter(
              (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
            )}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "sampleTypeCategory",
                header: intl.formatMessage({
                  id: "notebook.grid.qcCategory",
                  defaultMessage: "QC Category",
                }),
                render: (value) => getSampleTypeTag(value),
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "failAction",
                header: intl.formatMessage({
                  id: "notebook.grid.failAction",
                  defaultMessage: "Action Taken",
                }),
                render: (value) => {
                  if (!value) return "-";
                  if (value === "reject") return <Tag type="red">Rejected</Tag>;
                  if (value === "discard")
                    return <Tag type="magenta">Discarded</Tag>;
                  if (value === "quarantine")
                    return <Tag type="purple">Quarantined</Tag>;
                  return value;
                },
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value) => {
                  if (value === "SKIPPED") {
                    return (
                      <Tag type="red">
                        <FormattedMessage
                          id="notebook.disposition.notProceeding"
                          defaultMessage="Not Proceeding"
                        />
                      </Tag>
                    );
                  }
                  if (value === "COMPLETED") {
                    return (
                      <Tag type="green">
                        <FormattedMessage
                          id="notebook.disposition.proceeding"
                          defaultMessage="Proceeding"
                        />
                      </Tag>
                    );
                  }
                  return "-";
                },
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.pharma.qc.completedTable.empty"
                  defaultMessage="No samples have completed QC yet."
                />
              </p>
            </div>
          )}
      </div>

      {/* Global empty state - only show when no samples at all */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
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
          defaultMessage: "Raw Sample Quality Check (QC)",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "Pass"
              ? intl.formatMessage({
                  id: "notebook.pharma.qc.action.pass",
                  defaultMessage: "Pass - Proceed to Processing",
                })
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "notebook.pharma.qc.action.fail",
                    defaultMessage: "Fail - Reject Sample(s)",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply QC",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "Fail"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.pharma.bulkApply.description"
              defaultMessage="Perform quality checks on {count} selected sample(s). Complete the checklist for the appropriate sample type."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Sample Type Selection */}
          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <Select
                id="sampleType"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.qc.sampleType",
                  defaultMessage: "Sample Type Category",
                })}
                value={sampleType}
                onChange={(e) => {
                  setSampleType(e.target.value);
                  // Reset checks when sample type changes
                  resetBulkApplyValues();
                  setSampleType(e.target.value);
                }}
              >
                <SelectItem
                  value="pharmaceutical"
                  text={intl.formatMessage({
                    id: "notebook.pharma.qc.type.pharmaceutical",
                    defaultMessage: "Pharmaceutical Sample",
                  })}
                />
                <SelectItem
                  value="biological"
                  text={intl.formatMessage({
                    id: "notebook.pharma.qc.type.biological",
                    defaultMessage: "Biological Sample",
                  })}
                />
                <SelectItem
                  value="microbiological"
                  text={intl.formatMessage({
                    id: "notebook.pharma.qc.type.microbiological",
                    defaultMessage: "Microbiological/Environmental Sample",
                  })}
                />
              </Select>
            </Column>
          </Grid>

          {/* Pharmaceutical Sample Checks */}
          {sampleType === "pharmaceutical" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.pharma.qc.section.pharmaChecks"
                  defaultMessage="Pharmaceutical Sample Checks"
                />
              </h5>
              <Grid fullWidth className="qc-checklist">
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.labelAccuracy",
                      defaultMessage: "Label accuracy (matches requisition)",
                    })}
                    name="pharmaLabelAccuracy"
                    valueSelected={
                      bulkApplyValues.pharmaLabelAccuracy === true
                        ? "pass"
                        : bulkApplyValues.pharmaLabelAccuracy === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaLabelAccuracy: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-label-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-label-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.containerIntegrity",
                      defaultMessage:
                        "Container integrity (no leaks/tampering)",
                    })}
                    name="pharmaContainerIntegrity"
                    valueSelected={
                      bulkApplyValues.pharmaContainerIntegrity === true
                        ? "pass"
                        : bulkApplyValues.pharmaContainerIntegrity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaContainerIntegrity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-container-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-container-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.quantityVolume",
                      defaultMessage: "Quantity/Volume (meets minimum)",
                    })}
                    name="pharmaQuantityVolume"
                    valueSelected={
                      bulkApplyValues.pharmaQuantityVolume === true
                        ? "pass"
                        : bulkApplyValues.pharmaQuantityVolume === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaQuantityVolume: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-qty-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-qty-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.physicalAppearance",
                      defaultMessage:
                        "Physical appearance (matches pharmacopeial description)",
                    })}
                    name="pharmaPhysicalAppearance"
                    valueSelected={
                      bulkApplyValues.pharmaPhysicalAppearance === true
                        ? "pass"
                        : bulkApplyValues.pharmaPhysicalAppearance === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaPhysicalAppearance: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-appearance-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-appearance-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.expiryValidity",
                      defaultMessage: "Expiry/Validity (not expired)",
                    })}
                    name="pharmaExpiryValidity"
                    valueSelected={
                      bulkApplyValues.pharmaExpiryValidity === true
                        ? "pass"
                        : bulkApplyValues.pharmaExpiryValidity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaExpiryValidity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-expiry-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-expiry-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.storageCompliance",
                      defaultMessage: "Storage/transport compliance",
                    })}
                    name="pharmaStorageCompliance"
                    valueSelected={
                      bulkApplyValues.pharmaStorageCompliance === true
                        ? "pass"
                        : bulkApplyValues.pharmaStorageCompliance === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaStorageCompliance: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="pharma-storage-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="pharma-storage-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextInput
                    id="pharmaStorageConditions"
                    labelText={intl.formatMessage({
                      id: "notebook.pharma.qc.pharma.storageConditions",
                      defaultMessage:
                        "Storage conditions (e.g., 25°C / 60% RH)",
                    })}
                    value={bulkApplyValues.pharmaStorageConditions}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        pharmaStorageConditions: e.target.value,
                      }))
                    }
                    placeholder="e.g., 25°C / 60% RH"
                  />
                </Column>
              </Grid>
            </div>
          )}

          {/* Biological Sample Checks */}
          {sampleType === "biological" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.pharma.qc.section.bioChecks"
                  defaultMessage="Biological Sample Checks"
                />
              </h5>
              <Grid fullWidth className="qc-checklist">
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.labelAccuracy",
                      defaultMessage: "Label accuracy",
                    })}
                    name="bioLabelAccuracy"
                    valueSelected={
                      bulkApplyValues.bioLabelAccuracy === true
                        ? "pass"
                        : bulkApplyValues.bioLabelAccuracy === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioLabelAccuracy: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-label-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-label-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.containerIntegrity",
                      defaultMessage: "Container integrity",
                    })}
                    name="bioContainerIntegrity"
                    valueSelected={
                      bulkApplyValues.bioContainerIntegrity === true
                        ? "pass"
                        : bulkApplyValues.bioContainerIntegrity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioContainerIntegrity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-container-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-container-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.volumeQuantity",
                      defaultMessage: "Volume/Quantity",
                    })}
                    name="bioVolumeQuantity"
                    valueSelected={
                      bulkApplyValues.bioVolumeQuantity === true
                        ? "pass"
                        : bulkApplyValues.bioVolumeQuantity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioVolumeQuantity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-volume-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-volume-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.visualIntegrity",
                      defaultMessage:
                        "Visual integrity (free from hemolysis/contamination)",
                    })}
                    name="bioVisualIntegrity"
                    valueSelected={
                      bulkApplyValues.bioVisualIntegrity === true
                        ? "pass"
                        : bulkApplyValues.bioVisualIntegrity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioVisualIntegrity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-visual-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-visual-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.transportCompliance",
                      defaultMessage: "Transport/Storage compliance",
                    })}
                    name="bioTransportCompliance"
                    valueSelected={
                      bulkApplyValues.bioTransportCompliance === true
                        ? "pass"
                        : bulkApplyValues.bioTransportCompliance === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioTransportCompliance: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-transport-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-transport-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.bio.biosafetyLabeling",
                      defaultMessage: "Biosafety risk labeling",
                    })}
                    name="bioBiosafetyLabeling"
                    valueSelected={
                      bulkApplyValues.bioBiosafetyLabeling === true
                        ? "pass"
                        : bulkApplyValues.bioBiosafetyLabeling === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        bioBiosafetyLabeling: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="bio-biosafety-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="bio-biosafety-fail"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </div>
          )}

          {/* Microbiological/Environmental Sample Checks */}
          {sampleType === "microbiological" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.pharma.qc.section.microChecks"
                  defaultMessage="Microbiological/Environmental Checks"
                />
              </h5>
              <Grid fullWidth className="qc-checklist">
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.micro.containerIntegrity",
                      defaultMessage:
                        "Container integrity (especially sterility)",
                    })}
                    name="microContainerIntegrity"
                    valueSelected={
                      bulkApplyValues.microContainerIntegrity === true
                        ? "pass"
                        : bulkApplyValues.microContainerIntegrity === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        microContainerIntegrity: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="micro-container-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="micro-container-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.micro.sampleVolume",
                      defaultMessage: "Sample Volume/Quantity",
                    })}
                    name="microSampleVolume"
                    valueSelected={
                      bulkApplyValues.microSampleVolume === true
                        ? "pass"
                        : bulkApplyValues.microSampleVolume === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        microSampleVolume: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="micro-volume-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="micro-volume-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.micro.holdingTime",
                      defaultMessage: "Holding time (within timeframe)",
                    })}
                    name="microHoldingTime"
                    valueSelected={
                      bulkApplyValues.microHoldingTime === true
                        ? "pass"
                        : bulkApplyValues.microHoldingTime === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        microHoldingTime: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="micro-holding-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="micro-holding-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.pharma.qc.micro.transportCompliance",
                      defaultMessage: "Transport/Storage compliance",
                    })}
                    name="microTransportCompliance"
                    valueSelected={
                      bulkApplyValues.microTransportCompliance === true
                        ? "pass"
                        : bulkApplyValues.microTransportCompliance === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        microTransportCompliance: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="micro-transport-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="micro-transport-fail"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </div>
          )}

          {/* QC Result Summary */}
          <div className="qc-section qc-result-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.pharma.qc.section.result"
                defaultMessage="QC Result"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <div className="qc-result-display">
                  {bulkApplyValues.qcResult === "Pass" && (
                    <Tag type="green" size="md">
                      <FormattedMessage
                        id="notebook.pharma.qc.result.pass"
                        defaultMessage="PASS - Proceed to Processing"
                      />
                    </Tag>
                  )}
                  {bulkApplyValues.qcResult === "Fail" && (
                    <Tag type="red" size="md">
                      <FormattedMessage
                        id="notebook.pharma.qc.result.fail"
                        defaultMessage="FAIL - Reject Sample"
                      />
                    </Tag>
                  )}
                  {!bulkApplyValues.qcResult && (
                    <Tag type="gray" size="md">
                      <FormattedMessage
                        id="notebook.pharma.qc.result.pending"
                        defaultMessage="Complete checklist above"
                      />
                    </Tag>
                  )}
                </div>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="qcResultOverride"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.qc.resultOverride",
                    defaultMessage: "Override QC Result (optional)",
                  })}
                  value={bulkApplyValues.qcResult}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      qcResult: e.target.value,
                    }))
                  }
                >
                  <SelectItem value="" text="Auto (based on checklist)" />
                  <SelectItem value="Pass" text="Pass" />
                  <SelectItem value="Fail" text="Fail" />
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Fail Actions */}
          {bulkApplyValues.qcResult === "Fail" && (
            <div className="qc-section qc-fail-actions">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.pharma.qc.section.failActions"
                  defaultMessage="Fail Actions"
                />
              </h5>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.pharma.qc.failWarning.title",
                  defaultMessage: "Sample(s) will be rejected",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.pharma.qc.failWarning.subtitle",
                  defaultMessage:
                    "Failed samples will be logged in LMIS and marked for rejection.",
                })}
                hideCloseButton
                lowContrast
              />
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="rejectionReason"
                    labelText={intl.formatMessage({
                      id: "notebook.pharma.qc.rejectionReason",
                      defaultMessage: "Rejection Reason",
                    })}
                    value={bulkApplyValues.rejectionReason}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        rejectionReason: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.pharma.qc.rejectionReason.placeholder",
                      defaultMessage:
                        "Describe the reason for rejection (required for audit trail)...",
                    })}
                    required
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="failAction"
                    labelText={intl.formatMessage({
                      id: "notebook.pharma.qc.failAction",
                      defaultMessage: "Primary Action",
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
                    <SelectItem
                      value="reject"
                      text={intl.formatMessage({
                        id: "notebook.pharma.qc.failAction.reject",
                        defaultMessage: "Reject and log in LMIS",
                      })}
                    />
                    <SelectItem
                      value="discard"
                      text={intl.formatMessage({
                        id: "notebook.pharma.qc.failAction.discard",
                        defaultMessage: "Discard sample",
                      })}
                    />
                    <SelectItem
                      value="quarantine"
                      text={intl.formatMessage({
                        id: "notebook.pharma.qc.failAction.quarantine",
                        defaultMessage: "Quarantine for review",
                      })}
                    />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <div className="qc-fail-checkboxes">
                    <Checkbox
                      id="notifySubmitter"
                      labelText={intl.formatMessage({
                        id: "notebook.pharma.qc.notifySubmitter",
                        defaultMessage: "Notify submitter",
                      })}
                      checked={bulkApplyValues.notifySubmitter}
                      onChange={(_, { checked }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          notifySubmitter: checked,
                        }))
                      }
                    />
                    <Checkbox
                      id="requestReplacement"
                      labelText={intl.formatMessage({
                        id: "notebook.pharma.qc.requestReplacement",
                        defaultMessage: "Request replacement sample",
                      })}
                      checked={bulkApplyValues.requestReplacement}
                      onChange={(_, { checked }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          requestReplacement: checked,
                        }))
                      }
                    />
                  </div>
                </Column>
              </Grid>
            </div>
          )}

          {/* QC Equipment / Instruments */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <InventoryManagement
                size={16}
                style={{ marginRight: "0.5rem" }}
              />
              <FormattedMessage
                id="notebook.pharma.qc.section.instruments"
                defaultMessage="QC Equipment Used"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="selectedInstruments"
                  titleText={intl.formatMessage({
                    id: "notebook.pharma.qc.instruments",
                    defaultMessage: "Instruments",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.pharma.qc.instruments.placeholder",
                    defaultMessage: "Select QC instruments used...",
                  })}
                  items={instruments}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={instruments.filter((i) =>
                    bulkApplyValues.selectedInstruments.includes(i.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      selectedInstruments: selectedItems.map((i) => i.id),
                    }))
                  }
                  disabled={loadingInstruments}
                />
              </Column>
            </Grid>
          </div>

          {/* Remarks (for both Pass and Fail) */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="qcRemarks"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.qc.remarks",
                    defaultMessage: "QC Remarks (optional)",
                  })}
                  value={bulkApplyValues.qcRemarks}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      qcRemarks: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.pharma.qc.remarks.placeholder",
                    defaultMessage:
                      "Add any additional notes or observations...",
                  })}
                />
              </Column>
            </Grid>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default PharmaceuticalQualityCheckPage;
