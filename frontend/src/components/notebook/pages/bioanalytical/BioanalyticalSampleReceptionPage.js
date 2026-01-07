import React, { useState, useCallback, useContext, useEffect } from "react";
import { Grid, Column, Button, Tile, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import BioanalyticalManifestImportModal from "../../modals/BioanalyticalManifestImportModal";
import BulkApplyForm from "../../workflow/BulkApplyForm";
import SampleGrid from "../../workflow/SampleGrid";
import { Upload, Checkmark, Edit } from "@carbon/react/icons";
import { postToOpenElisServer } from "../../../utils/Utils";
import config from "../../../../config.json";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalSampleReceptionPage - STAGE 1 of bioanalytical workflow.
 *
 * Following established OpenELIS laboratory workflow patterns:
 * - Import samples via manifest CSV
 * - Display samples in SampleGrid with checkboxes for bulk selection
 * - Action buttons for bulk operations (Edit Metadata, Mark Complete)
 * - Two-section layout: Pending samples (with selection) + Completed samples (read-only)
 * - Progress tracking tiles showing counts and status
 *
 * STAGE 1 Requirements:
 * ● Receive processed biological samples from Medical Laboratory at clinical site
 * ● Receive pharmaceutical samples directly from researchers or external clients
 * ● Register metadata: sample type, requested tests, storage condition, source laboratory/client
 * ● Link to project or bioequivalence study
 * ● Mark samples as verified to proceed to Test Assignment & Preparation
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after sample changes
 */
function BioanalyticalSampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Core state following established patterns
  const [isLoading, setIsLoading] = useState(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Modal states
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isBulkApplyModalOpen, setIsBulkApplyModalOpen] = useState(false);

  // Progress tracking
  // Include both PENDING and IN_PROGRESS statuses as "pending" for the workflow
  // IN_PROGRESS means samples have been opened/started but not yet completed
  const pendingSamples = samples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  );
  const completedSamples = samples.filter((s) => s.status === "COMPLETED");
  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  // Check if we have a real page ID (not a default placeholder)
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Notification helper following established patterns
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Modal handlers following established patterns
  const handleImportModalOpen = () => {
    setIsImportModalOpen(true);
  };

  const handleImportModalClose = () => {
    setIsImportModalOpen(false);
  };

  const handleImportSuccess = useCallback(
    (results) => {
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.success",
          defaultMessage: "Success",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.reception.importSuccess",
            defaultMessage:
              "{count} samples imported successfully for Stage 1 reception",
          },
          { count: results.totalCreated || 0 },
        ),
      });

      // Refresh sample list and close modal
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
      setIsImportModalOpen(false);
    },
    [intl, onProgressUpdate, notify],
  );

  // Bulk operation: Mark samples as verified/completed following established patterns
  const markAsVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    // Use established bulk status update pattern
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notebook.bioanalytical.reception.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage(
              {
                id: "notebook.bioanalytical.reception.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as verified. They are ready for Test Assignment & Preparation.",
              },
              { count: selectedSampleIds.length },
            ),
          });
          setSelectedSampleIds([]); // Clear selection
          loadPageSamples(); // Refresh list
          if (onProgressUpdate) {
            onProgressUpdate(); // Notify parent
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.bioanalytical.reception.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "notebook.bioanalytical.reception.error.status",
              defaultMessage: "Failed to verify samples. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    notify,
    pageData?.id,
    onProgressUpdate,
  ]);

  // Load Stage 1 samples from backend API
  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setIsLoading(false);
      setSamples([]);
      return;
    }

    setIsLoading(true);

    // Load samples for this bioanalytical workflow page
    fetch(`${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`, {
      method: "GET",
      credentials: "include",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Failed to load samples: HTTP ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        console.debug(
          "Loaded samples for bioanalytical page:",
          pageData.id,
          "Count:",
          Array.isArray(data) ? data.length : 0,
          "Data:",
          data,
        );

        if (Array.isArray(data)) {
          const transformedSamples = data.map((sample) => {
            // Combine base sample properties with all data JSONB fields
            // This ensures any bulk-applied values are captured
            const sampleDataFields = sample.data || {};
            const sampleStatus =
              sample.pageStatus || sample.status || "PENDING";

            console.debug(
              "Sample transform debug:",
              sample.externalId,
              "pageStatus:",
              sample.pageStatus,
              "status:",
              sample.status,
              "final status:",
              sampleStatus,
            );

            return {
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              status: sampleStatus,

              // Map bioanalytical-specific fields to SampleGrid default columns
              // to avoid duplicates while providing the data
              sourceFacility: sampleDataFields.sourceOrigin, // Maps to "Source" column
              receivedDate: sampleDataFields.dateTimeOfReceipt, // Maps to "Received Date" column
              // collectionDate is intentionally NOT mapped (will show as "-")

              // Stage 1 bioanalytical-specific metadata from JSONB data
              // Spread all JSONB data fields to capture bulk-applied values
              ...sampleDataFields,

              // Ensure these core fields have fallbacks
              uniqueSampleId:
                sampleDataFields.uniqueSampleId || sample.externalId,
              projectStudyAssociation: sampleDataFields.projectStudyAssociation,
              storageConditionPrior: sampleDataFields.storageConditionPrior,
              manifestVerificationStatus:
                sampleDataFields.manifestVerificationStatus,
              timepoint: sampleDataFields.timepoint,
              sourceOrigin: sampleDataFields.sourceOrigin,
              requestedTests: sampleDataFields.requestedTests,
              dateTimeOfReceipt: sampleDataFields.dateTimeOfReceipt,
              receivingPersonnel: sampleDataFields.receivingPersonnel,
              sampleVolume: sampleDataFields.sampleVolume,
              transportTemperature: sampleDataFields.transportTemperature,
              subjectId: sampleDataFields.subjectId,
              notes: sampleDataFields.notes,
            };
          });
          console.debug(
            "Transformed samples:",
            transformedSamples.length,
            transformedSamples,
          );
          setSamples(transformedSamples);
        } else {
          console.warn("Response data is not an array:", data);
          setSamples([]);
        }
        setIsLoading(false);
      })
      .catch((error) => {
        console.error("Failed to load Stage 1 samples:", error);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.reception.error",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "notebook.bioanalytical.stage1.error.loadSamples",
            defaultMessage: "Failed to load samples. Please refresh the page.",
          }),
        });
        setSamples([]);
        setIsLoading(false);
      });
  }, [pageData?.id, intl, notify]);

  // Load samples when component mounts or page changes
  useEffect(() => {
    loadPageSamples();
  }, [loadPageSamples]);

  return (
    <div className="bioanalytical-page">
      {/* Stage 1 Header - Following established patterns */}
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.stage1.title"
            defaultMessage="STAGE 1: Sample Reception & Registration"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.stage1.description"
            defaultMessage="Import samples via manifest CSV, review and verify metadata, then mark as complete to proceed to Test Assignment & Preparation."
          />
        </p>
      </div>

      {/* Progress Tiles Following Established Pattern */}
      <Grid
        fullWidth
        className="progress-section"
        style={{ marginBottom: "1.5rem" }}
      >
        <Column lg={16} md={8} sm={4}>
          <div
            className="progress-tiles"
            style={{ display: "flex", gap: "1rem" }}
          >
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.awaitingVerification"
                  defaultMessage="Awaiting Verification"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.biological"
                  defaultMessage="Biological"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["Plasma", "Serum", "Urine", "Whole Blood"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.pharmaceutical"
                  defaultMessage="Pharmaceutical"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["API", "Tablet", "Capsule", "Suspension"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Bar Following Established Pattern */}
      <Grid fullWidth style={{ marginBottom: "1.5rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div
            className="page-actions-bar"
            style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}
          >
            <Button
              kind="primary"
              size="sm"
              renderIcon={Upload}
              onClick={handleImportModalOpen}
            >
              <FormattedMessage
                id="notebook.bioanalytical.stage1.importManifest"
                defaultMessage="Import from Manifest"
              />
            </Button>

            {/* Conditional buttons that appear when samples are selected */}
            {selectedSampleIds.length > 0 && (
              <>
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Edit}
                  onClick={() => setIsBulkApplyModalOpen(true)}
                >
                  Edit Metadata ({selectedSampleIds.length})
                </Button>

                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Checkmark}
                  onClick={markAsVerified}
                >
                  Mark as Verified ({selectedSampleIds.length})
                </Button>
              </>
            )}
          </div>
        </Column>
      </Grid>

      {/* Two-Section Layout Following Established Pattern */}

      {/* Section 1: Pending Samples (with checkboxes and selection) */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              marginBottom: "0.5rem",
            }}
          >
            <FormattedMessage
              id="notebook.bioanalytical.stage1.pending.title"
              defaultMessage="Pending Verification"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.pending.description"
              defaultMessage="Select samples to edit metadata or mark as verified. Verified samples will proceed to Test Assignment & Preparation."
            />
          </p>
        </div>

        <div className="sample-grid-container">
          {!isLoading && pendingSamples.length === 0 ? (
            <div
              className="empty-table-state"
              style={{ textAlign: "center", padding: "2rem", color: "#525252" }}
            >
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.pending.empty"
                  defaultMessage="No pending samples. Import a CSV manifest to add samples for verification."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-bioanalytical-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={isLoading}
              columns={[
                {
                  key: "externalId",
                  header: intl.formatMessage({
                    id: "notebook.sample.externalId",
                    defaultMessage: "External ID",
                  }),
                },
                {
                  key: "accessionNumber",
                  header: intl.formatMessage({
                    id: "notebook.sample.accessionNumber",
                    defaultMessage: "Accession #",
                  }),
                },
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.type",
                    defaultMessage: "Sample Type",
                  }),
                },
                {
                  key: "sampleCategory",
                  header: intl.formatMessage({
                    id: "notebook.sample.category",
                    defaultMessage: "Category",
                  }),
                },
                {
                  key: "sourceFacility",
                  header: intl.formatMessage({
                    id: "notebook.sample.sourceFacility",
                    defaultMessage: "Source",
                  }),
                },
                // Collection Date intentionally removed (not used in bioanalytical workflow)
                {
                  key: "receivedDate",
                  header: intl.formatMessage({
                    id: "notebook.sample.receivedDate",
                    defaultMessage: "Received Date",
                  }),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.sample.status",
                    defaultMessage: "Status",
                  }),
                },
              ]}
              additionalColumns={[
                {
                  key: "pending-accessionNumber",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.accessionNumber",
                    defaultMessage: "Accession Number",
                  }),
                  render: (value, sample) => {
                    const accession = sample?.accessionNumber || value;
                    return accession ? (
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontFamily: "monospace",
                        }}
                      >
                        {accession}
                      </div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-uniqueSampleId",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.uniqueSampleId",
                    defaultMessage: "Sample ID",
                  }),
                  render: (value, sample) => {
                    const sampleId =
                      sample?.uniqueSampleId || sample?.externalId || value;
                    return sampleId ? (
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontFamily: "monospace",
                        }}
                      >
                        {sampleId}
                      </div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-projectStudyAssociation",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.projectStudyAssociation",
                    defaultMessage: "Project/Study",
                  }),
                  render: (value, sample) => {
                    const project = sample?.projectStudyAssociation || value;
                    return project ? (
                      <div style={{ fontSize: "0.875rem" }}>🔗 {project}</div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-storageConditionPrior",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.storageCondition",
                    defaultMessage: "Storage Condition",
                  }),
                  render: (value, sample) => {
                    const condition = sample?.storageConditionPrior || value;
                    if (!condition) return "-";

                    // Extract temperature info for quick visualization
                    const tempMatch = condition.match(/\((.*?)\)/);
                    const displayText = tempMatch ? tempMatch[1] : condition;
                    return (
                      <Tag
                        size="sm"
                        type={
                          condition.toLowerCase().includes("frozen")
                            ? "purple"
                            : condition.toLowerCase().includes("refrigerat")
                              ? "cyan"
                              : "gray"
                        }
                      >
                        {displayText}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pending-manifestVerificationStatus",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.verificationStatus",
                    defaultMessage: "Verification Status",
                  }),
                  render: (value, sample) => {
                    const status =
                      sample?.manifestVerificationStatus || value || "Pending";
                    return (
                      <Tag
                        size="sm"
                        type={
                          status === "Verified"
                            ? "green"
                            : status === "Discrepancy"
                              ? "red"
                              : "gray"
                        }
                      >
                        {status}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pending-timepoint",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.timepoint",
                    defaultMessage: "Timepoint",
                  }),
                  render: (value, sample) => {
                    const timepoint = sample?.timepoint || value;
                    return timepoint ? (
                      <div style={{ fontSize: "0.875rem" }}>⏱️ {timepoint}</div>
                    ) : (
                      "-"
                    );
                  },
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Section 2: Verified Samples (read-only, no checkboxes) */}
      {completedSamples.length > 0 && (
        <div className="sample-table-section" style={{ marginTop: "2rem" }}>
          <div className="table-section-header">
            <h5
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                marginBottom: "0.5rem",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.stage1.verified.title"
                defaultMessage="Verified Samples"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
            <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.verified.description"
                defaultMessage="Samples verified and ready for Test Assignment & Preparation."
              />
            </p>
          </div>

          <SampleGrid
            gridId="verified-bioanalytical-samples"
            samples={completedSamples}
            showSelection={false}
            loading={isLoading}
            columns={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.sample.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.sample.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.sample.type",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "sampleCategory",
                header: intl.formatMessage({
                  id: "notebook.sample.category",
                  defaultMessage: "Category",
                }),
              },
              {
                key: "sourceFacility",
                header: intl.formatMessage({
                  id: "notebook.sample.sourceFacility",
                  defaultMessage: "Source",
                }),
              },
              // Collection Date intentionally removed (not used in bioanalytical workflow)
              {
                key: "receivedDate",
                header: intl.formatMessage({
                  id: "notebook.sample.receivedDate",
                  defaultMessage: "Received Date",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.sample.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
            additionalColumns={[
              {
                key: "verified-accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
                render: (value, sample) => {
                  const accession = sample?.accessionNumber || value;
                  return accession ? (
                    <div
                      style={{ fontSize: "0.875rem", fontFamily: "monospace" }}
                    >
                      {accession}
                    </div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-uniqueSampleId",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.uniqueSampleId",
                  defaultMessage: "Sample ID",
                }),
                render: (value, sample) => {
                  const sampleId =
                    sample?.uniqueSampleId || sample?.externalId || value;
                  return sampleId ? (
                    <div
                      style={{ fontSize: "0.875rem", fontFamily: "monospace" }}
                    >
                      {sampleId}
                    </div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-sampleType",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.sampleType",
                  defaultMessage: "Sample Type",
                }),
                render: (value, sample) => {
                  const sampleType = sample?.sampleType || value;
                  return sampleType ? (
                    <Tag
                      type={
                        // Color coding: Blue for biological samples, Green for pharmaceutical samples
                        [
                          "Plasma",
                          "Serum",
                          "Urine",
                          "Whole Blood",
                          "Saliva",
                          "CSF",
                        ].includes(sampleType)
                          ? "blue"
                          : "green"
                      }
                      size="sm"
                    >
                      {sampleType}
                    </Tag>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-projectStudyAssociation",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.projectStudyAssociation",
                  defaultMessage: "Project/Study",
                }),
                render: (value, sample) => {
                  const project = sample?.projectStudyAssociation || value;
                  return project ? (
                    <div style={{ fontSize: "0.875rem" }}>🔗 {project}</div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-requestedTests",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.requestedTests",
                  defaultMessage: "Requested Tests",
                }),
                render: (value, sample) => {
                  const tests = sample?.requestedTests || value;
                  if (!tests) return "-";

                  // Handle both string (single test) and array (multiple tests) formats
                  const testArray = Array.isArray(tests)
                    ? tests
                    : typeof tests === "string"
                      ? tests.split(",").map((t) => t.trim())
                      : [tests];
                  return (
                    <div style={{ fontSize: "0.875rem" }}>
                      {testArray.slice(0, 2).map((test, index) => (
                        <Tag
                          key={index}
                          size="sm"
                          type="outline"
                          style={{
                            marginRight: "0.25rem",
                            marginBottom: "0.125rem",
                          }}
                        >
                          {test}
                        </Tag>
                      ))}
                      {testArray.length > 2 && (
                        <span style={{ color: "#6f6f6f", fontSize: "0.75rem" }}>
                          +{testArray.length - 2} more
                        </span>
                      )}
                    </div>
                  );
                },
              },
              {
                key: "verified-timepoint",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.timepoint",
                  defaultMessage: "Timepoint",
                }),
                render: (value, sample) => {
                  const timepoint = sample?.timepoint || value;
                  return timepoint ? (
                    <div style={{ fontSize: "0.875rem" }}>⏱️ {timepoint}</div>
                  ) : (
                    "-"
                  );
                },
              },
            ]}
          />
        </div>
      )}

      {/* Modals Following Established Patterns */}

      {/* Import Modal */}
      <BioanalyticalManifestImportModal
        open={isImportModalOpen}
        onClose={handleImportModalClose}
        entryId={entryId}
        onSuccess={handleImportSuccess}
      />

      {/* Bulk Apply Metadata Modal */}
      {isBulkApplyModalOpen && hasRealPageId && (
        <BulkApplyForm
          open={isBulkApplyModalOpen}
          onClose={() => setIsBulkApplyModalOpen(false)}
          pageId={pageData.id}
          selectedSampleIds={selectedSampleIds}
          formFields={[
            // Core Sample Information (Stage 1 Requirements)
            {
              key: "sampleType",
              label: "Sample Type",
              type: "dropdown",
              required: true,
              options: [
                // Biological Samples (from Medical Laboratory)
                { id: "Plasma", text: "Plasma (Biological)" },
                { id: "Serum", text: "Serum (Biological)" },
                { id: "Urine", text: "Urine (Biological)" },
                { id: "Whole Blood", text: "Whole Blood (Biological)" },
                { id: "Saliva", text: "Saliva (Biological)" },
                { id: "CSF", text: "Cerebrospinal Fluid (Biological)" },
                // Pharmaceutical Samples (from Researchers/Clients)
                { id: "API", text: "API - Active Pharmaceutical Ingredient" },
                { id: "Tablet", text: "Tablet (Solid Dosage)" },
                { id: "Capsule", text: "Capsule (Solid Dosage)" },
                { id: "Suspension", text: "Suspension (Liquid Dosage)" },
                { id: "Solution", text: "Solution (Liquid Dosage)" },
                { id: "Emulsion", text: "Emulsion (Liquid Dosage)" },
                { id: "Cream", text: "Cream (Topical)" },
                { id: "Ointment", text: "Ointment (Topical)" },
              ],
            },
            {
              key: "requestedTests",
              label: "Requested Test(s)",
              type: "text",
              required: true,
              placeholder:
                "e.g., LC-MS/MS, HPLC, Dissolution (comma-separated for multiple tests)",
            },
            {
              key: "sourceOrigin",
              label: "Source Origin/Laboratory",
              type: "dropdown",
              required: true,
              options: [
                {
                  id: "AHRI Medical Lab",
                  text: "AHRI Medical Laboratory (Clinical Site)",
                },
                {
                  id: "External Medical Lab",
                  text: "External Medical Laboratory",
                },
                {
                  id: "Research Institution",
                  text: "Research Institution/University",
                },
                {
                  id: "Pharmaceutical Company",
                  text: "Pharmaceutical Company",
                },
                { id: "CRO", text: "Contract Research Organization (CRO)" },
                { id: "Government Lab", text: "Government Laboratory" },
                { id: "Private Clinic", text: "Private Clinical Site" },
                { id: "Other", text: "Other (Specify in Notes)" },
              ],
            },
            {
              key: "storageConditionPrior",
              label: "Storage Condition Prior to Testing",
              type: "dropdown",
              required: true,
              options: [
                {
                  id: "Room Temperature (15-25°C)",
                  text: "Room Temperature (15-25°C)",
                },
                { id: "Refrigerated (2-8°C)", text: "Refrigerated (2-8°C)" },
                { id: "Frozen (-20°C)", text: "Frozen (-20°C)" },
                { id: "Ultra-frozen (-80°C)", text: "Ultra-frozen (-80°C)" },
                {
                  id: "Controlled Room Temp",
                  text: "Controlled Room Temperature",
                },
                { id: "Do not freeze", text: "Do not freeze" },
                { id: "Unknown", text: "Unknown" },
              ],
            },

            // Study/Project Linkage
            {
              key: "projectStudyAssociation",
              label: "Project/Study/Protocol ID",
              type: "text",
              placeholder: "Link to bioequivalence study or research project",
            },

            // Sample Details
            {
              key: "sampleVolume",
              label: "Sample Volume/Quantity",
              type: "text",
              placeholder: "e.g., 2.5 mL, 10 tablets, 5g",
            },
            {
              key: "subjectId",
              label: "Subject/Patient ID",
              type: "text",
              placeholder: "Anonymous subject identifier",
            },
            {
              key: "timepoint",
              label: "Collection Timepoint",
              type: "dropdown",
              options: [
                { id: "Pre-dose", text: "Pre-dose" },
                { id: "1h", text: "1h post-dose" },
                { id: "2h", text: "2h post-dose" },
                { id: "4h", text: "4h post-dose" },
                { id: "8h", text: "8h post-dose" },
                { id: "24h", text: "24h post-dose" },
                { id: "Other", text: "Other (specify in notes)" },
              ],
            },

            // Administrative
            {
              key: "receivingPersonnel",
              label: "Receiving Personnel",
              type: "text",
              placeholder: "Staff member who received the sample",
            },
            {
              key: "manifestVerificationStatus",
              label: "Manifest Verification Status",
              type: "dropdown",
              options: [
                { id: "Verified", text: "Verified - All information matches" },
                { id: "Pending", text: "Pending - Awaiting verification" },
                { id: "Discrepancy", text: "Discrepancy - Issues identified" },
              ],
            },
            {
              key: "notes",
              label: "Notes/Comments",
              type: "text",
              placeholder:
                "Special handling instructions, observations, or additional details",
            },
          ]}
          onApplySuccess={(response) => {
            notify({
              kind: NotificationKinds.success,
              title: intl.formatMessage({
                id: "notebook.bioanalytical.reception.success",
                defaultMessage: "Success",
              }),
              message: intl.formatMessage(
                {
                  id: "notebook.bioanalytical.reception.bulkApplySuccess",
                  defaultMessage:
                    "Metadata applied to {count} sample(s) successfully.",
                },
                { count: selectedSampleIds.length },
              ),
            });
            setSelectedSampleIds([]); // Clear selection

            // Refresh data with a small delay to ensure modal closes and state updates
            setTimeout(() => {
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }, 300);
          }}
        />
      )}
    </div>
  );
}

export default BioanalyticalSampleReceptionPage;
