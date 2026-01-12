import React, { useState, useCallback, useMemo } from "react";
import config from "../../../../config.json";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  Tag,
  Modal,
  ComboBox,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import SampleGrid from "../../workflow/SampleGrid";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalStorageArchivingPage - Stage 5 of bioanalytical workflow.
 *
 * Features:
 * - Sample storage location and condition tracking
 * - Retention period management per regulatory requirements
 * - Long-term archival and retrieval planning
 * - Disposal scheduling and compliance documentation
 * - Final sample/data disposition tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 */
function BioanalyticalStorageArchivingPage({ entryId, pageData }) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [storageSamples, setStorageSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState(new Set());
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Modal states
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [disposalReason, setDisposalReason] = useState("");
  const [disposalMethod, setDisposalMethod] = useState("");
  const [disposalNotes, setDisposalNotes] = useState("");
  const [supervisorApproval, setSupervisorApproval] = useState("");

  // View details modal states
  const [viewDetailsModalOpen, setViewDetailsModalOpen] = useState(false);
  const [selectedSampleDetail, setSelectedSampleDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Biorepository and retention storage modal states
  const [biorepositoryModalOpen, setBiorepositoryModalOpen] = useState(false);
  const [retentionStorageModalOpen, setRetentionStorageModalOpen] =
    useState(false);
  const [storageLocation, setStorageLocation] = useState("");
  const [storageTemperature, setStorageTemperature] = useState("");
  const [retentionPeriod, setRetentionPeriod] = useState("");
  const [storageNotes, setStorageNotes] = useState("");

  const disposalMethods = [
    {
      id: "autoclave_incineration",
      label: "Autoclaving + Incineration (Biological Samples)",
    },
    {
      id: "chemical_incineration",
      label: "Chemical Treatment + Incineration (Pharmaceutical)",
    },
    {
      id: "licensed_facility",
      label: "Licensed/Accredited Disposal Facility",
    },
    { id: "return_sponsor", label: "Return to Sponsor" },
    { id: "research_transfer", label: "Transfer to Research (with approval)" },
  ];

  const disposalReasons = [
    { id: "exhausted", label: "Sample Exhausted" },
    { id: "retention_expired", label: "Retention Period Expired" },
    { id: "failed_qc", label: "Failed QC (Unusable)" },
    { id: "safety_concerns", label: "Safety Concerns" },
    { id: "legal_hold_completed", label: "Legal Hold Period Completed" },
    { id: "study_terminated", label: "Study Terminated" },
  ];

  const loadStorageSamples = useCallback(async () => {
    if (!entryId || String(entryId).startsWith("default-")) {
      setStorageSamples([]);
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        const allSamples = Array.isArray(data) ? data : data.samples || [];

        let stage4CompletedSamples = allSamples.filter((sample) => {
          return (
            sample.data &&
            sample.data.executionStatus === "EXECUTED" &&
            sample.data.resultsApproved &&
            (sample.data.submissionStatus === "SUBMITTED" ||
              sample.data.submissionStatus ===
                "QA_APPROVED_READY_FOR_STORAGE" ||
              sample.data.exportStatus === "EXPORTED")
          );
        });

        if (stage4CompletedSamples.length === 0 && allSamples.length > 0) {
          console.log(
            "No QA-approved samples found, showing all samples in Stage 5",
          );
          stage4CompletedSamples = allSamples;
        }

        const transformedSamples = stage4CompletedSamples.map((sample) => ({
          id: sample.id,
          sampleId:
            sample.accessionNumber || sample.externalId || `S${sample.id}`,
          type: sample.sampleType || "Bioanalytical Sample",
          volume: sample.data?.sampleVolume || "5.0 mL",
          location: sample.data?.storageLocation || "Not Assigned",
          storageTemp: sample.data?.storageTemperature || "Pending Assignment",
          status: sample.data?.storageStatus || "READY_FOR_STORAGE",
          disposalStatus: sample.data?.disposalStatus || "-",
          disposalReason: sample.data?.disposalReason || "-",
          disposalMethod: sample.data?.disposalMethod || "-",
          disposalDate: sample.data?.disposalDate || "-",
          disposalApprovedBy: sample.data?.disposalApprovedBy || "-",
          retentionPeriod: sample.data?.retentionPeriod || "-",
          retentionExpiryDate: sample.data?.retentionExpiryDate || "-",
        }));

        setStorageSamples(transformedSamples);
      } else {
        console.error("Failed to load samples:", response.status);
        setStorageSamples([]);
      }
    } catch (error) {
      console.error("Error loading storage samples:", error);
      setStorageSamples([]);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.loadError",
          defaultMessage: "Failed to load samples. Please refresh the page.",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [entryId, intl, pageData.id]);

  React.useEffect(() => {
    loadStorageSamples();
  }, [loadStorageSamples]);

  // Stage 5 Handler: Sample Disposal Management (via bulk apply endpoint)
  const handleSampleDisposal = useCallback(async () => {
    if (selectedSamples.size === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for disposal",
        }),
      );
      return;
    }

    if (!disposalReason) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectDisposalReason",
          defaultMessage: "Please select disposal reason",
        }),
      );
      return;
    }

    if (!disposalMethod) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectDisposalMethod",
          defaultMessage: "Please select disposal method",
        }),
      );
      return;
    }

    if (!supervisorApproval) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.supervisorApprovalRequired",
          defaultMessage: "Supervisor approval is required for sample disposal",
        }),
      );
      return;
    }

    setIsLoading(true);
    try {
      // Use bulk apply endpoint with disposal fields in JSONB data
      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          disposalStatus: "SCHEDULED",
          disposalReason: disposalReason,
          disposalMethod: disposalMethod,
          disposalDate: new Date().toISOString().split("T")[0],
          disposalApprovedBy: supervisorApproval,
          disposalNotes: disposalNotes,
          storageStatus: "DISPOSED",
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (response.ok) {
        const result = await response.json();
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "notebook.bioanalytical.storage.disposalSuccess",
              defaultMessage:
                "{count} samples scheduled for disposal. Method: {method}. Supervisor: {supervisor}",
            },
            {
              count: result.updatedCount || selectedSamples.size,
              method: disposalMethods.find((m) => m.id === disposalMethod)
                ?.label,
              supervisor: supervisorApproval,
            },
          ),
        );

        setDisposalModalOpen(false);
        setSelectedSamples(new Set());
        setDisposalReason("");
        setDisposalMethod("");
        setDisposalNotes("");
        setSupervisorApproval("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to schedule disposal");
      }
    } catch (error) {
      console.error("Sample disposal error:", error);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalError",
          defaultMessage: "Failed to schedule sample disposal",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    disposalReason,
    disposalMethod,
    supervisorApproval,
    disposalNotes,
    intl,
    loadStorageSamples,
    disposalMethods,
    pageData?.id,
  ]);

  const handleSampleDetailsClick = useCallback(async (sample) => {
    setViewDetailsModalOpen(true);
    setSelectedSampleDetail(sample);
    setDetailLoading(true);

    try {
      // Fetch the full sample record from the backend
      const response = await fetch(
        `${config.serverBaseUrl}/rest/sample/${sample.id}`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const fullSample = await response.json();
        setSelectedSampleDetail(fullSample);
      } else {
        console.error("Failed to fetch sample details:", response.status);
        // Keep the basic sample data if full fetch fails
      }
    } catch (error) {
      console.error("Error fetching sample details:", error);
      // Keep the basic sample data if fetch fails
    } finally {
      setDetailLoading(false);
    }
  }, []);

  // Handle biorepository transfer
  const handleBiorepositoryTransfer = useCallback(async () => {
    if (selectedSamples.size === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for biorepository transfer",
        }),
      );
      return;
    }

    setIsLoading(true);
    try {
      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          storageStatus: "BIOREPOSITORY_TRANSFER",
          storageLocation: storageLocation || "Biorepository",
          storageTemperature: storageTemperature || "-80°C",
          storageNotes: storageNotes,
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (response.ok) {
        setSuccessMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.storage.biorepositorySuccess",
            defaultMessage:
              "{count} samples transferred to biorepository at {location}",
            values: { count: selectedSamples.size, location: storageLocation },
          }),
        );
        setBiorepositoryModalOpen(false);
        setSelectedSamples(new Set());
        setStorageLocation("");
        setStorageTemperature("");
        setStorageNotes("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to transfer samples to biorepository");
      }
    } catch (error) {
      console.error("Biorepository transfer error:", error);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.biorepositoryError",
          defaultMessage: "Failed to transfer samples to biorepository",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    storageLocation,
    storageTemperature,
    storageNotes,
    intl,
    loadStorageSamples,
    pageData?.id,
  ]);

  // Handle retention storage
  const handleRetentionStorage = useCallback(async () => {
    if (selectedSamples.size === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for retention storage",
        }),
      );
      return;
    }

    if (!retentionPeriod) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectRetentionPeriod",
          defaultMessage: "Please select retention period",
        }),
      );
      return;
    }

    setIsLoading(true);
    try {
      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          storageStatus: "RETENTION_STORAGE",
          storageLocation: storageLocation || "Retention Freezer",
          storageTemperature: storageTemperature || "-80°C",
          retentionPeriod: retentionPeriod,
          retentionExpiryDate: calculateRetentionExpiryDate(retentionPeriod),
          storageNotes: storageNotes,
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (response.ok) {
        setSuccessMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.storage.retentionSuccess",
            defaultMessage:
              "{count} samples placed in retention storage for {period}",
            values: { count: selectedSamples.size, period: retentionPeriod },
          }),
        );
        setRetentionStorageModalOpen(false);
        setSelectedSamples(new Set());
        setStorageLocation("");
        setStorageTemperature("");
        setRetentionPeriod("");
        setStorageNotes("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to place samples in retention storage");
      }
    } catch (error) {
      console.error("Retention storage error:", error);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.retentionError",
          defaultMessage: "Failed to place samples in retention storage",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    storageLocation,
    storageTemperature,
    retentionPeriod,
    storageNotes,
    intl,
    loadStorageSamples,
    pageData?.id,
  ]);

  const calculateRetentionExpiryDate = (period) => {
    const now = new Date();
    const months = parseInt(period.split(" ")[0]) || 24;
    now.setMonth(now.getMonth() + months);
    return now.toISOString().split("T")[0];
  };

  const getStatusTag = (status) => {
    let type = "blue";
    let label = status;

    switch (status) {
      case "READY_FOR_STORAGE":
        type = "blue";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.ready",
          defaultMessage: "Ready for Storage",
        });
        break;
      case "BIOREPOSITORY_TRANSFER":
        type = "green";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.biorepository",
          defaultMessage: "Biorepository Transfer",
        });
        break;
      case "RETENTION_STORAGE":
        type = "purple";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.retention",
          defaultMessage: "Retention Storage",
        });
        break;
      case "DISPOSED":
        type = "red";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.disposed",
          defaultMessage: "Disposed",
        });
        break;
      case "ARCHIVED":
        type = "cyan";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.archived",
          defaultMessage: "Archived",
        });
        break;
      default:
        type = "gray";
    }

    return <Tag type={type}>{label}</Tag>;
  };

  const getDisposalStatusTag = (status) => {
    let type = "gray";
    let label = status;

    switch (status) {
      case "SCHEDULED":
        type = "green";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.scheduled",
          defaultMessage: "Scheduled",
        });
        break;
      case "PENDING":
        type = "blue";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.pending",
          defaultMessage: "Pending",
        });
        break;
      case "COMPLETED":
        type = "teal";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.completed",
          defaultMessage: "Completed",
        });
        break;
      case "CANCELLED":
        type = "red";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.cancelled",
          defaultMessage: "Cancelled",
        });
        break;
      case "-":
        return "-";
      default:
        return <Tag type={type}>{status}</Tag>;
    }

    return <Tag type={type}>{label}</Tag>;
  };

  const headers = [
    { key: "sampleId", header: "Sample ID" },
    { key: "type", header: "Type" },
    { key: "location", header: "Storage Location" },
    { key: "storageTemp", header: "Temperature" },
    { key: "retentionPeriod", header: "Retention Period" },
    { key: "retentionExpiryDate", header: "Expiry Date" },
    { key: "disposalStatus", header: "Disposal Status" },
    { key: "disposalReason", header: "Disposal Reason" },
    { key: "disposalMethod", header: "Disposal Method" },
    { key: "disposalDate", header: "Disposal Date" },
    { key: "disposalApprovedBy", header: "Approved By" },
  ];

  const additionalColumns = useMemo(
    () => [
      {
        key: "status",
        header: "Storage Status",
        render: (value) => getStatusTag(value),
      },
      {
        key: "disposalStatus",
        header: "Disposal Status",
        render: (value) => getDisposalStatusTag(value),
      },
    ],
    [],
  );

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.storage.title"
            defaultMessage="Sample Storage & Archival"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.storage.description"
            defaultMessage="Document sample storage locations and conditions, establish retention periods per regulatory requirements, plan long-term archival, and schedule final disposal or archival transfers."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.storage.error",
              defaultMessage: "Error",
            })}
            subtitle={errorMessage}
            lowContrast
            onCloseButtonClick={() => setErrorMessage("")}
          />
        </div>
      )}

      {successMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.storage.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <div style={{ paddingTop: "1.5rem" }}>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <div className="section-header">
              <h4>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.inventorySection"
                  defaultMessage="Stage 4 Completed Samples - Ready for Storage & Archival"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.inventoryHelp"
                  defaultMessage="Samples that have completed Stage 4 (QA approved and submitted/exported) are ready for biorepository transfer, retention storage at -80°C (2 years for bioequivalence), and data archival."
                />
              </p>

              {isLoading ? (
                <Loading description="Loading sample inventory..." />
              ) : storageSamples.length > 0 ? (
                <div style={{ marginTop: "1.5rem" }}>
                  <div
                    style={{
                      marginBottom: "1rem",
                      padding: "0.75rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <p style={{ fontSize: "0.875rem", margin: 0 }}>
                      <strong>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.totalSamples"
                          defaultMessage="Total Samples in Storage:"
                        />
                      </strong>{" "}
                      {storageSamples.length}
                      {selectedSamples.size > 0 && (
                        <span style={{ marginLeft: "1rem", color: "#0043ce" }}>
                          (
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.samplesSelected"
                            defaultMessage="{count} selected"
                            values={{ count: selectedSamples.size }}
                          />
                          )
                        </span>
                      )}
                    </p>
                  </div>

                  {selectedSamples.size > 0 && (
                    <div
                      style={{
                        marginBottom: "1rem",
                        display: "flex",
                        gap: "1rem",
                        flexWrap: "wrap",
                      }}
                    >
                      <Button
                        kind="primary"
                        onClick={() => setBiorepositoryModalOpen(true)}
                        disabled={isLoading}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.transferBiorepository"
                          defaultMessage="Transfer to Biorepository"
                        />
                      </Button>
                      <Button
                        kind="secondary"
                        onClick={() => setRetentionStorageModalOpen(true)}
                        disabled={isLoading}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.retentionStorage"
                          defaultMessage="Retention Storage"
                        />
                      </Button>
                      <Button
                        kind="danger--tertiary"
                        onClick={() => setDisposalModalOpen(true)}
                        disabled={isLoading}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.manageSampleDisposal"
                          defaultMessage="Manage Sample Disposal"
                        />
                      </Button>
                    </div>
                  )}

                  <SampleGrid
                    gridId="storage-samples"
                    samples={storageSamples}
                    selectedIds={Array.from(selectedSamples)}
                    onSelectionChange={(ids) => {
                      // Only allow selection of non-disposed samples
                      const selectableSamples = ids.filter((id) => {
                        const sample = storageSamples.find((s) => s.id === id);
                        return sample && sample.status !== "DISPOSED";
                      });
                      setSelectedSamples(new Set(selectableSamples));
                    }}
                    onSampleClick={handleSampleDetailsClick}
                    showSelection={true}
                    loading={isLoading}
                    columns={headers}
                    additionalColumns={additionalColumns}
                  />
                </div>
              ) : (
                <div
                  style={{
                    marginTop: "1.5rem",
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    textAlign: "center",
                  }}
                >
                  <p style={{ color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.noSamples"
                      defaultMessage="No samples in storage"
                    />
                  </p>
                </div>
              )}
            </div>
          </Column>
        </Grid>
      </div>

      {/* Sample Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        onRequestClose={() => setDisposalModalOpen(false)}
        modalHeading="Sample Disposal Management"
        primaryButtonText="Schedule Disposal"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleSampleDisposal}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Schedule disposal for selected bioequivalence study samples after
            retention period completion.
          </p>

          <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
            <div style={{ flex: 1 }}>
              <ComboBox
                id="disposal-reason"
                items={disposalReasons}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  disposalReasons.find((r) => r.id === disposalReason) || null
                }
                onChange={({ selectedItem }) =>
                  setDisposalReason(selectedItem?.id || "")
                }
                placeholder="Select a reason"
                titleText="Disposal Reason"
              />
            </div>

            <div style={{ flex: 1 }}>
              <ComboBox
                id="disposal-method"
                items={disposalMethods}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  disposalMethods.find((m) => m.id === disposalMethod) || null
                }
                onChange={({ selectedItem }) =>
                  setDisposalMethod(selectedItem?.id || "")
                }
                placeholder="Select a method"
                titleText="Disposal Method"
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Supervisor Approval (Name)
            </label>
            <input
              type="text"
              value={supervisorApproval}
              onChange={(e) => setSupervisorApproval(e.target.value)}
              placeholder="Enter supervisor name for approval"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="disposal-notes"
              labelText="Notes"
              value={disposalNotes}
              onChange={(e) => setDisposalNotes(e.target.value)}
              placeholder="Add any additional notes about this disposal"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#fef3c7",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #f59e0b",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#92400e",
                  fontWeight: "600",
                }}
              >
                ⚠️ Disposal Warning
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#92400e",
                }}
              >
                <strong>Samples to dispose:</strong> {selectedSamples.size}{" "}
                samples
              </p>
              <p
                style={{
                  fontSize: "0.75rem",
                  margin: "0.5rem 0 0 0",
                  color: "#92400e",
                }}
              >
                This action will permanently schedule samples for disposal and
                cannot be undone.
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Sample Details View Modal */}
      <Modal
        open={viewDetailsModalOpen}
        onRequestClose={() => setViewDetailsModalOpen(false)}
        modalHeading={
          selectedSampleDetail
            ? `Sample Details - ${selectedSampleDetail.sampleId || selectedSampleDetail.accessionNumber || `S${selectedSampleDetail.id}`}`
            : "Sample Details"
        }
        secondaryButtonText="Close"
        primaryButtonText=""
        size="lg"
      >
        {detailLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading sample details..." />
          </div>
        ) : selectedSampleDetail ? (
          <div style={{ marginBottom: "1.5rem" }}>
            <Grid>
              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.sampleId"
                      defaultMessage="Sample ID"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.sampleId ||
                      selectedSampleDetail.accessionNumber ||
                      `S${selectedSampleDetail.id}`}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.sampleType"
                      defaultMessage="Sample Type"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.type ||
                      selectedSampleDetail.sampleType ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.volume"
                      defaultMessage="Volume"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.volume ||
                      selectedSampleDetail.data?.sampleVolume ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.storageLocation"
                      defaultMessage="Storage Location"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.location ||
                      selectedSampleDetail.data?.storageLocation ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.temperature"
                      defaultMessage="Storage Temperature"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.storageTemp ||
                      selectedSampleDetail.data?.storageTemperature ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.status"
                      defaultMessage="Status"
                    />
                  </h5>
                  <div style={{ margin: 0 }}>
                    {getStatusTag(
                      selectedSampleDetail.status ||
                        selectedSampleDetail.data?.storageStatus,
                    )}
                  </div>
                </div>
              </Column>

              {selectedSampleDetail.data?.disposalStatus && (
                <>
                  <Column
                    lg={16}
                    md={8}
                    sm={4}
                    style={{
                      marginBottom: "1rem",
                      marginTop: "1rem",
                      paddingTop: "1rem",
                      borderTop: "1px solid #e0e0e0",
                    }}
                  >
                    <h5 style={{ color: "#161616", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.disposalInfo"
                        defaultMessage="Disposal Information"
                      />
                    </h5>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalReason"
                          defaultMessage="Disposal Reason"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalReason || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalMethod"
                          defaultMessage="Disposal Method"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalMethod || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalDate"
                          defaultMessage="Disposal Date"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalDate || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.approvedBy"
                          defaultMessage="Approved By"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalApprovedBy || "-"}
                      </p>
                    </div>
                  </Column>

                  {selectedSampleDetail.data?.disposalNotes && (
                    <Column
                      lg={16}
                      md={8}
                      sm={4}
                      style={{ marginBottom: "1.5rem" }}
                    >
                      <div>
                        <h5
                          style={{ marginBottom: "0.5rem", color: "#161616" }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.disposalNotes"
                            defaultMessage="Disposal Notes"
                          />
                        </h5>
                        <p
                          style={{
                            margin: 0,
                            color: "#525252",
                            whiteSpace: "pre-wrap",
                          }}
                        >
                          {selectedSampleDetail.data.disposalNotes}
                        </p>
                      </div>
                    </Column>
                  )}
                </>
              )}
            </Grid>
          </div>
        ) : (
          <p>
            <FormattedMessage
              id="notebook.bioanalytical.storage.noDetailsAvailable"
              defaultMessage="No sample details available"
            />
          </p>
        )}
      </Modal>

      {/* Biorepository Transfer Modal */}
      <Modal
        open={biorepositoryModalOpen}
        onRequestClose={() => setBiorepositoryModalOpen(false)}
        modalHeading="Transfer to Biorepository"
        primaryButtonText="Confirm Transfer"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleBiorepositoryTransfer}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Transfer selected samples to biorepository storage for long-term
            preservation and future research use.
          </p>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Storage Location
            </label>
            <input
              type="text"
              value={storageLocation}
              onChange={(e) => setStorageLocation(e.target.value)}
              placeholder="e.g., Biorepository Building A, Room 201"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Storage Temperature
            </label>
            <input
              type="text"
              value={storageTemperature}
              onChange={(e) => setStorageTemperature(e.target.value)}
              placeholder="e.g., -80°C"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="biorepository-notes"
              labelText="Notes"
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Add any additional notes about biorepository transfer"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#d0f0ff",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #0f62fe",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#0043ce",
                  fontWeight: "600",
                }}
              >
                ℹ️ Transfer Information
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#0043ce",
                }}
              >
                <strong>Samples to transfer:</strong> {selectedSamples.size}{" "}
                samples
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Retention Storage Modal */}
      <Modal
        open={retentionStorageModalOpen}
        onRequestClose={() => setRetentionStorageModalOpen(false)}
        modalHeading="Place in Retention Storage"
        primaryButtonText="Confirm Storage"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleRetentionStorage}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Place selected samples in retention storage per regulatory
            requirements (typically -80°C for 2 years in bioequivalence
            studies).
          </p>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Storage Location
            </label>
            <input
              type="text"
              value={storageLocation}
              onChange={(e) => setStorageLocation(e.target.value)}
              placeholder="e.g., Ultra-low Freezer -1"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Storage Temperature
            </label>
            <input
              type="text"
              value={storageTemperature}
              onChange={(e) => setStorageTemperature(e.target.value)}
              placeholder="e.g., -80°C"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Retention Period
            </label>
            <select
              value={retentionPeriod}
              onChange={(e) => setRetentionPeriod(e.target.value)}
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            >
              <option value="">Select retention period...</option>
              <option value="6 months">6 months</option>
              <option value="1 year">1 year</option>
              <option value="2 years">2 years (Bioequivalence Standard)</option>
              <option value="3 years">3 years</option>
              <option value="5 years">5 years</option>
              <option value="10 years">10 years</option>
            </select>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="retention-notes"
              labelText="Notes"
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Add any additional notes about retention storage"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#f0f3ff",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #7f10f0",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#7f10f0",
                  fontWeight: "600",
                }}
              >
                ℹ️ Retention Information
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#7f10f0",
                }}
              >
                <strong>Samples to store:</strong> {selectedSamples.size}{" "}
                samples
              </p>
              {retentionPeriod && (
                <p
                  style={{
                    fontSize: "0.875rem",
                    margin: "0.5rem 0 0 0",
                    color: "#7f10f0",
                  }}
                >
                  <strong>Expiry Date:</strong>{" "}
                  {calculateRetentionExpiryDate(retentionPeriod)}
                </p>
              )}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BioanalyticalStorageArchivingPage;
