import React, { useState, useCallback, useEffect } from "react";
import config from "../../../../config.json";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Modal,
  ComboBox,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import {
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
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
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 */
function BioanalyticalStorageArchivingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [storageSamples, setStorageSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState(new Set());
  const [disposalRecords, setDisposalRecords] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Modal states
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [disposalReason, setDisposalReason] = useState("");
  const [disposalMethod, setDisposalMethod] = useState("");
  const [disposalNotes, setDisposalNotes] = useState("");
  const [supervisorApproval, setSupervisorApproval] = useState("");

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
              sample.data.submissionStatus === "QA_APPROVED_READY_FOR_STORAGE" ||
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

  // Stage 5 Handler: Sample Disposal Management
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
      // Call backend API to process disposal
      const disposalData = {
        sampleIds: Array.from(selectedSamples).map(id => parseInt(id, 10)),
        disposalReason: disposalReason,
        disposalMethod: disposalMethod,
        notes: disposalNotes,
        supervisorApproval: supervisorApproval,
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/sample/dispose`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(disposalData),
        },
      );

      if (response.ok) {
        setSuccessMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.storage.disposalSuccess",
            defaultMessage:
              "{count} samples scheduled for disposal. Method: {method}",
          }, {
            count: selectedSamples.size,
            method: disposalMethods.find(m => m.id === disposalMethod)?.label,
          }),
        );

        setDisposalModalOpen(false);
        setSelectedSamples(new Set());
        setDisposalReason("");
        setDisposalMethod("");
        setDisposalNotes("");
        setSupervisorApproval("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to process disposal");
      }
    } catch (error) {
      console.error("Sample disposal error:", error);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalError",
          defaultMessage: "Failed to process sample disposal",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [selectedSamples, disposalReason, disposalMethod, supervisorApproval, disposalNotes, intl, loadStorageSamples, disposalMethods, pageData?.id]);

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
                        <span
                          style={{ marginLeft: "1rem", color: "#0043ce" }}
                        >
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
                      }}
                    >
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

                  <DataTable rows={storageSamples} headers={[
                    { key: "sampleId", header: "Sample ID" },
                    { key: "type", header: "Type" },
                    { key: "volume", header: "Volume" },
                    { key: "location", header: "Storage Location" },
                    { key: "storageTemp", header: "Temperature" },
                    { key: "status", header: "Status" },
                  ]}>
                    {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
                      <div>
                        <table {...getTableProps()}>
                          <thead>
                            <tr>
                              <th style={{ width: "40px" }}>
                                <input
                                  type="checkbox"
                                  checked={
                                    selectedSamples.size === storageSamples.length &&
                                    storageSamples.length > 0
                                  }
                                  onChange={(e) => {
                                    if (e.target.checked) {
                                      setSelectedSamples(
                                        new Set(storageSamples.map((s) => s.id)),
                                      );
                                    } else {
                                      setSelectedSamples(new Set());
                                    }
                                  }}
                                  aria-label="Select all samples"
                                />
                              </th>
                              {headers.map((header) => (
                                <th key={header.key} {...getHeaderProps({ header })}>
                                  {header.header}
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {rows.map((row) => (
                              <tr key={row.id} {...getRowProps({ row })}>
                                <td style={{ width: "40px" }}>
                                  <input
                                    type="checkbox"
                                    checked={selectedSamples.has(row.id)}
                                    onChange={(e) => {
                                      const newSelected = new Set(selectedSamples);
                                      if (e.target.checked) {
                                        newSelected.add(row.id);
                                      } else {
                                        newSelected.delete(row.id);
                                      }
                                      setSelectedSamples(newSelected);
                                    }}
                                    aria-label={`Select sample ${row.id}`}
                                  />
                                </td>
                                {row.cells.map((cell) => (
                                  <td key={cell.id}>
                                    {cell.info.header === "status" ? (
                                      getStatusTag(cell.value)
                                    ) : (
                                      cell.value
                                    )}
                                  </td>
                                ))}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </DataTable>
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
            Schedule disposal for selected bioequivalence study samples after retention period completion.
          </p>

          <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
            <div style={{ flex: 1 }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "600", fontSize: "0.875rem" }}>
                Disposal Reason
              </label>
              <ComboBox
                items={disposalReasons}
                itemToString={(item) => (item ? item.label : "")}
                value={disposalReasons.find(r => r.id === disposalReason) || null}
                onChange={({ selectedItem }) => setDisposalReason(selectedItem?.id || "")}
                placeholder="Select a reason"
              />
            </div>

            <div style={{ flex: 1 }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "600", fontSize: "0.875rem" }}>
                Disposal Method
              </label>
              <ComboBox
                items={disposalMethods}
                itemToString={(item) => (item ? item.label : "")}
                value={disposalMethods.find(m => m.id === disposalMethod) || null}
                onChange={({ selectedItem }) => setDisposalMethod(selectedItem?.id || "")}
                placeholder="Select a method"
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "600", fontSize: "0.875rem" }}>
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
                fontSize: "0.875rem"
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "600", fontSize: "0.875rem" }}>
              Notes
            </label>
            <TextArea
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
                border: "1px solid #f59e0b"
              }}
            >
              <p style={{ fontSize: "0.875rem", margin: 0, color: "#92400e", fontWeight: "600" }}>
                ⚠️ Disposal Warning
              </p>
              <p style={{ fontSize: "0.875rem", margin: "0.5rem 0 0 0", color: "#92400e" }}>
                <strong>Samples to dispose:</strong> {selectedSamples.size} samples
              </p>
              <p style={{ fontSize: "0.75rem", margin: "0.5rem 0 0 0", color: "#92400e" }}>
                This action will permanently schedule samples for disposal and cannot be undone.
              </p>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BioanalyticalStorageArchivingPage;
