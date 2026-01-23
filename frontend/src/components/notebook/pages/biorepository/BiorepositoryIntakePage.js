import React, { useState, useCallback, useEffect } from "react";
import {
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  InlineNotification,
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
  TableBatchActions,
  TableBatchAction,
  Tag,
  Button,
  Modal,
} from "@carbon/react";
import { Checkmark, Renew, Barcode, ArrowRight } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import ShipmentReceptionForm from "./ShipmentReceptionForm";
import SampleIntakeForm from "./SampleIntakeForm";
import DocumentationVerificationModal from "./DocumentationVerificationModal";
import ManifestUploadModal from "./ManifestUploadModal";
import SampleTransferTab from "./SampleTransferTab";
import RetentionPolicySection from "./RetentionPolicySection";

/**
 * BiorepositoryIntakePage - Sample Intake & Registration workflow page
 * Stage 1 of the Biorepository workflow with 5 sub-stages:
 *   1a: Shipment Reception
 *   1b: Documentation Verification (7-point checklist, linked to shipment)
 *   1c: Sample Registration (single entry or bulk manifest import)
 *   1d: Sample Transfer
 *   1e: Sample Inventory (includes barcode generation via batch action)
 *
 * Per SRS Section 4.2: Documentation must be verified BEFORE sample registration
 * to prevent entry of samples that cannot be properly tracked.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration from notebook
 * @param {Object} props.progress - Progress tracking data
 * @param {Function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryIntakePage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const [activeSubStage, setActiveSubStage] = useState(0);

  // Workflow state
  const [currentShipment, setCurrentShipment] = useState(null);
  const [registeredSamples, setRegisteredSamples] = useState([]);
  const [allBioSamples, setAllBioSamples] = useState([]);
  const [loadingSamples, setLoadingSamples] = useState(false);
  const [verificationModalOpen, setVerificationModalOpen] = useState(false);
  const [manifestModalOpen, setManifestModalOpen] = useState(false);

  // Barcode generation state
  const [barcodeSource, setBarcodeSource] = useState("about:blank");
  const [renderBarcode, setRenderBarcode] = useState(false);
  const [barcodeLabNo, setBarcodeLabNo] = useState("");

  // Advance to storage state
  const [advancingToStorage, setAdvancingToStorage] = useState(false);
  const [advanceNotification, setAdvanceNotification] = useState(null);

  // Track completion of each sub-stage
  const [subStageComplete, setSubStageComplete] = useState({
    shipment: false,
    registration: false,
    documentation: false,
  });

  const subStages = [
    {
      id: "1a",
      key: "shipment",
      label: intl.formatMessage({
        id: "biorepository.intake.substage.shipment",
        defaultMessage: "Shipment Reception",
      }),
    },
    {
      id: "1b",
      key: "documentation",
      label: intl.formatMessage({
        id: "biorepository.intake.substage.documentation",
        defaultMessage: "Documentation Verification",
      }),
    },
    {
      id: "1c",
      key: "registration",
      label: intl.formatMessage({
        id: "biorepository.intake.substage.registration",
        defaultMessage: "Sample Registration",
      }),
    },
    {
      id: "1d",
      key: "transfer",
      label: intl.formatMessage({
        id: "biorepository.intake.substage.transfer",
        defaultMessage: "Sample Transfer",
      }),
    },
    {
      id: "1e",
      key: "inventory",
      label: intl.formatMessage({
        id: "biorepository.intake.substage.inventory",
        defaultMessage: "Sample Inventory",
      }),
    },
  ];

  const handleShipmentCreated = useCallback((shipment) => {
    setCurrentShipment(shipment);
    setSubStageComplete((prev) => ({ ...prev, shipment: true }));
    // Auto-advance to documentation verification
    setActiveSubStage(1);
  }, []);

  const handleShipmentSelected = useCallback((shipment) => {
    // When user selects an existing shipment, treat it like a newly created one
    setCurrentShipment(shipment);
    setSubStageComplete((prev) => ({ ...prev, shipment: true }));
    // Check if shipment already has documentation verified
    if (
      shipment.documentationStatus === "VERIFIED" ||
      shipment.documentationStatus === "QUARANTINE"
    ) {
      setSubStageComplete((prev) => ({ ...prev, documentation: true }));
      // Auto-advance to sample registration
      setActiveSubStage(2);
    } else {
      // Auto-advance to documentation verification
      setActiveSubStage(1);
    }
  }, []);

  const handleSamplesRegistered = useCallback((samples) => {
    setRegisteredSamples((prev) => [...prev, ...samples]);
    setSubStageComplete((prev) => ({ ...prev, registration: true }));
  }, []);

  const handleBulkImportComplete = useCallback((samples) => {
    setRegisteredSamples((prev) => [...prev, ...samples]);
    setSubStageComplete((prev) => ({ ...prev, registration: true }));
    setManifestModalOpen(false);
  }, []);

  const handleOpenVerification = useCallback(() => {
    // Open verification modal for the current shipment
    setVerificationModalOpen(true);
  }, []);

  const handleVerificationComplete = useCallback((verification) => {
    // Update shipment with new documentation status
    setCurrentShipment((prev) => ({
      ...prev,
      documentationStatus: verification.status,
    }));

    // Mark documentation as complete (verified or quarantined)
    setSubStageComplete((prev) => ({ ...prev, documentation: true }));
    setVerificationModalOpen(false);

    // Auto-advance to sample registration
    setActiveSubStage(2);
  }, []);

  // Load all biorepository samples (for inventory tab) - runs on mount
  // Filter by workflowStatus=REGISTERED to show only samples at Intake stage
  const loadAllBioSamples = useCallback(() => {
    setLoadingSamples(true);
    getFromOpenElisServer(
      `/rest/biorepository/sample?limit=500&workflowStatus=REGISTERED`,
      (data) => {
        setLoadingSamples(false);
        if (data && Array.isArray(data)) {
          setAllBioSamples(data);
        }
      },
    );
  }, []);

  // Load all samples on mount
  useEffect(() => {
    loadAllBioSamples();
  }, [loadAllBioSamples]);

  // Load samples for current shipment when shipment changes
  useEffect(() => {
    if (currentShipment?.id) {
      getFromOpenElisServer(
        `/rest/biorepository/sample?shipmentId=${currentShipment.id}&limit=100`,
        (data) => {
          if (data && Array.isArray(data)) {
            setRegisteredSamples(data);
            if (data.length > 0) {
              setSubStageComplete((prev) => ({ ...prev, registration: true }));
            }
          }
        },
      );
    }
  }, [currentShipment]);

  // Refresh inventory after bulk import
  const handleBulkImportCompleteWithRefresh = useCallback(
    (samples) => {
      handleBulkImportComplete(samples);
      // Refresh all samples list
      loadAllBioSamples();
    },
    [handleBulkImportComplete, loadAllBioSamples],
  );

  // Advance selected samples to Storage Assignment page (page 2)
  const handleAdvanceToStorage = useCallback(
    (selectedRows) => {
      if (!selectedRows || selectedRows.length === 0) {
        setAdvanceNotification({
          kind: "warning",
          title: intl.formatMessage({
            id: "biorepository.inventory.advance.noSelection",
            defaultMessage: "No Samples Selected",
          }),
          subtitle: intl.formatMessage({
            id: "biorepository.inventory.advance.noSelection.message",
            defaultMessage: "Please select samples to advance to storage.",
          }),
        });
        return;
      }

      // Get sampleItemIds from the selected BioSamples
      const sampleItemIds = selectedRows
        .map((row) => {
          const bioSample = allBioSamples.find(
            (s) => s.id.toString() === row.id,
          );
          return bioSample?.sampleItemId;
        })
        .filter((id) => id != null);

      if (sampleItemIds.length === 0) {
        setAdvanceNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "biorepository.inventory.advance.noSampleItems",
            defaultMessage: "Cannot Advance",
          }),
          subtitle: intl.formatMessage({
            id: "biorepository.inventory.advance.noSampleItems.message",
            defaultMessage:
              "Selected samples do not have valid sample item links.",
          }),
        });
        return;
      }

      setAdvancingToStorage(true);
      setAdvanceNotification(null);

      // Get notebook pages to find Storage Assignment page (order 2)
      const nbId = notebookId || entryId;
      if (!nbId) {
        setAdvancingToStorage(false);
        setAdvanceNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "biorepository.inventory.advance.noNotebook",
            defaultMessage: "Notebook Not Found",
          }),
          subtitle: intl.formatMessage({
            id: "biorepository.inventory.advance.noNotebook.message",
            defaultMessage: "Cannot determine notebook for advancing samples.",
          }),
        });
        return;
      }

      getFromOpenElisServer(`/rest/notebook/view/${nbId}`, (nbResponse) => {
        if (nbResponse && nbResponse.pages) {
          // Find Storage Assignment page (order 2)
          const storageAssignmentPage = nbResponse.pages.find(
            (p) => (p.pageOrder || p.order) === 2,
          );

          if (storageAssignmentPage && storageAssignmentPage.id) {
            // Add samples to the Storage Assignment page
            postToOpenElisServerJsonResponse(
              `/rest/notebook/bulk/page/${storageAssignmentPage.id}/samples/add`,
              JSON.stringify({ sampleIds: sampleItemIds }),
              (addResponse) => {
                if (addResponse && addResponse.success) {
                  // Update workflow status to PENDING_STORAGE
                  putToOpenElisServerJsonResponse(
                    `/rest/biorepository/sample/workflow-status`,
                    JSON.stringify({
                      sampleItemIds: sampleItemIds,
                      workflowStatus: "PENDING_STORAGE",
                    }),
                    () => {
                      // Calculate retention expiry dates for the advanced samples
                      postToOpenElisServerJsonResponse(
                        `/rest/biorepository/sample/calculate-retention`,
                        JSON.stringify({ sampleItemIds: sampleItemIds }),
                        (retentionResponse) => {
                          setAdvancingToStorage(false);
                          const addedCount =
                            addResponse.addedCount || sampleItemIds.length;
                          const retentionUpdated =
                            retentionResponse?.updatedCount || 0;

                          let subtitle = intl.formatMessage(
                            {
                              id: "biorepository.inventory.advance.success.message",
                              defaultMessage:
                                "{count} sample(s) advanced to Storage Assignment. Navigate to Stage 2 to assign storage locations.",
                            },
                            { count: addedCount },
                          );

                          // Add retention info to notification if any were updated
                          if (retentionUpdated > 0) {
                            subtitle += ` ${intl.formatMessage(
                              {
                                id: "biorepository.inventory.advance.retentionCalculated",
                                defaultMessage:
                                  "Retention expiry calculated for {retentionCount} sample(s).",
                              },
                              { retentionCount: retentionUpdated },
                            )}`;
                          }

                          setAdvanceNotification({
                            kind: "success",
                            title: intl.formatMessage({
                              id: "biorepository.inventory.advance.success",
                              defaultMessage: "Samples Advanced",
                            }),
                            subtitle: subtitle,
                          });
                          // Refresh samples list
                          loadAllBioSamples();
                          if (onProgressUpdate) {
                            onProgressUpdate();
                          }
                        },
                      );
                    },
                  );
                } else {
                  setAdvancingToStorage(false);
                  setAdvanceNotification({
                    kind: "error",
                    title: intl.formatMessage({
                      id: "biorepository.inventory.advance.error",
                      defaultMessage: "Failed to Advance",
                    }),
                    subtitle:
                      addResponse?.error ||
                      intl.formatMessage({
                        id: "biorepository.inventory.advance.error.message",
                        defaultMessage:
                          "Could not add samples to Storage Assignment page.",
                      }),
                  });
                }
              },
            );
          } else {
            setAdvancingToStorage(false);
            setAdvanceNotification({
              kind: "error",
              title: intl.formatMessage({
                id: "biorepository.inventory.advance.noStoragePage",
                defaultMessage: "Storage Page Not Found",
              }),
              subtitle: intl.formatMessage({
                id: "biorepository.inventory.advance.noStoragePage.message",
                defaultMessage:
                  "Storage Assignment page (Stage 2) not found in notebook.",
              }),
            });
          }
        } else {
          setAdvancingToStorage(false);
          setAdvanceNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "biorepository.inventory.advance.notebookError",
              defaultMessage: "Notebook Error",
            }),
            subtitle: intl.formatMessage({
              id: "biorepository.inventory.advance.notebookError.message",
              defaultMessage: "Could not load notebook pages.",
            }),
          });
        }
      });
    },
    [
      allBioSamples,
      notebookId,
      entryId,
      intl,
      loadAllBioSamples,
      onProgressUpdate,
    ],
  );

  return (
    <div className="biorepository-intake-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tabs
            selectedIndex={activeSubStage}
            onChange={({ selectedIndex }) => setActiveSubStage(selectedIndex)}
          >
            <TabList aria-label="Intake sub-stages">
              {subStages.map((stage) => (
                <Tab key={stage.id}>
                  <span
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                    }}
                  >
                    {subStageComplete[stage.key] && (
                      <Checkmark size={16} style={{ color: "green" }} />
                    )}
                    {stage.label}
                  </span>
                </Tab>
              ))}
            </TabList>
            <TabPanels>
              {/* Sub-stage 1a: Shipment Reception */}
              <TabPanel>
                <div className="substage-content" style={{ padding: "1rem 0" }}>
                  <h4 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="biorepository.intake.shipment.title"
                      defaultMessage="Shipment Reception"
                    />
                  </h4>

                  {subStageComplete.shipment && currentShipment ? (
                    <InlineNotification
                      kind="success"
                      title={intl.formatMessage({
                        id: "biorepository.intake.shipment.received",
                        defaultMessage: "Shipment Received",
                      })}
                      subtitle={intl.formatMessage(
                        {
                          id: "biorepository.intake.shipment.receivedDetails",
                          defaultMessage:
                            "Delivery reference: {ref}. Proceed to sample registration.",
                        },
                        {
                          ref:
                            currentShipment.deliveryReference ||
                            currentShipment.id,
                        },
                      )}
                      lowContrast
                      hideCloseButton
                    />
                  ) : (
                    <ShipmentReceptionForm
                      onShipmentCreated={handleShipmentCreated}
                      onShipmentSelected={handleShipmentSelected}
                      onCancel={() => {}}
                    />
                  )}
                </div>
              </TabPanel>

              {/* Sub-stage 1b: Documentation Verification (linked to shipment) */}
              <TabPanel>
                <div className="substage-content" style={{ padding: "1rem 0" }}>
                  <h4 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="biorepository.intake.documentation.title"
                      defaultMessage="Documentation Verification"
                    />
                  </h4>

                  {!currentShipment ? (
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "biorepository.intake.documentation.noShipment",
                        defaultMessage: "No Shipment Selected",
                      })}
                      subtitle={intl.formatMessage({
                        id: "biorepository.intake.documentation.noShipment.message",
                        defaultMessage:
                          "Receive a shipment first before proceeding to documentation verification.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  ) : subStageComplete.documentation ? (
                    <InlineNotification
                      kind={
                        currentShipment.documentationStatus === "VERIFIED"
                          ? "success"
                          : "warning"
                      }
                      title={intl.formatMessage({
                        id:
                          currentShipment.documentationStatus === "VERIFIED"
                            ? "biorepository.intake.documentation.verified"
                            : "biorepository.intake.documentation.quarantine",
                        defaultMessage:
                          currentShipment.documentationStatus === "VERIFIED"
                            ? "Documentation Verified"
                            : "Documentation Quarantined",
                      })}
                      subtitle={intl.formatMessage({
                        id: "biorepository.intake.documentation.proceedToRegistration",
                        defaultMessage: "Proceed to sample registration.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  ) : (
                    <div>
                      <p style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="biorepository.intake.documentation.shipmentInstructions"
                          defaultMessage="Verify the shipment documentation using the 7-point checklist before registering samples."
                        />
                      </p>
                      <div
                        onClick={handleOpenVerification}
                        style={{
                          padding: "1.5rem",
                          border: "1px solid #e0e0e0",
                          borderRadius: "4px",
                          cursor: "pointer",
                          backgroundColor: "#f4f4f4",
                          maxWidth: "400px",
                        }}
                      >
                        <strong>
                          <FormattedMessage
                            id="biorepository.intake.documentation.shipmentRef"
                            defaultMessage="Shipment: {ref}"
                            values={{
                              ref:
                                currentShipment.deliveryReference ||
                                currentShipment.id,
                            }}
                          />
                        </strong>
                        <div
                          style={{
                            fontSize: "0.875rem",
                            color: "#525252",
                            marginTop: "0.5rem",
                          }}
                        >
                          <FormattedMessage
                            id="biorepository.intake.documentation.clickToVerifyShipment"
                            defaultMessage="Click to complete documentation verification checklist"
                          />
                        </div>
                        <Tag type="gray" style={{ marginTop: "0.5rem" }}>
                          <FormattedMessage
                            id="biorepository.intake.documentation.pending"
                            defaultMessage="Pending Verification"
                          />
                        </Tag>
                      </div>
                    </div>
                  )}
                </div>
              </TabPanel>

              {/* Sub-stage 1c: Sample Registration */}
              <TabPanel>
                <div className="substage-content" style={{ padding: "1rem 0" }}>
                  <h4 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="biorepository.intake.registration.title"
                      defaultMessage="Sample Registration"
                    />
                  </h4>

                  {!subStageComplete.documentation ? (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "biorepository.intake.registration.docsRequired",
                        defaultMessage: "Documentation Verification Required",
                      })}
                      subtitle={intl.formatMessage({
                        id: "biorepository.intake.registration.docsRequired.message",
                        defaultMessage:
                          "Complete documentation verification before registering samples.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  ) : (
                    <>
                      {registeredSamples.length > 0 && (
                        <InlineNotification
                          kind="info"
                          title={intl.formatMessage(
                            {
                              id: "biorepository.intake.registration.count",
                              defaultMessage: "{count} sample(s) registered",
                            },
                            { count: registeredSamples.length },
                          )}
                          lowContrast
                          hideCloseButton
                          style={{ marginBottom: "1rem" }}
                        />
                      )}

                      <SampleIntakeForm
                        shipment={currentShipment}
                        onSamplesRegistered={handleSamplesRegistered}
                        onBulkImport={() => setManifestModalOpen(true)}
                        onCancel={() => {}}
                      />
                    </>
                  )}
                </div>
              </TabPanel>

              {/* Sub-stage 1d: Sample Transfer Queue */}
              <TabPanel>
                <div className="substage-content" style={{ padding: "1rem 0" }}>
                  <SampleTransferTab />
                </div>
              </TabPanel>

              {/* Sub-stage 1e: Sample Inventory */}
              <TabPanel>
                <div className="substage-content" style={{ padding: "1rem 0" }}>
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
                        id="biorepository.intake.inventory.title"
                        defaultMessage="Sample Inventory"
                      />
                    </h4>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Renew}
                      onClick={loadAllBioSamples}
                      disabled={loadingSamples}
                    >
                      <FormattedMessage
                        id="biorepository.intake.inventory.refresh"
                        defaultMessage="Refresh"
                      />
                    </Button>
                  </div>

                  <p style={{ marginBottom: "1rem", color: "#525252" }}>
                    <FormattedMessage
                      id="biorepository.intake.inventory.description"
                      defaultMessage="All samples registered in the biorepository from sample registration and transfers."
                    />
                  </p>

                  {/* Retention Policy Configuration Section */}
                  <RetentionPolicySection />

                  {loadingSamples ? (
                    <p>
                      <FormattedMessage
                        id="biorepository.intake.inventory.loading"
                        defaultMessage="Loading samples..."
                      />
                    </p>
                  ) : allBioSamples.length === 0 ? (
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "biorepository.intake.inventory.empty",
                        defaultMessage: "No Samples",
                      })}
                      subtitle={intl.formatMessage({
                        id: "biorepository.intake.inventory.empty.message",
                        defaultMessage:
                          "No samples have been registered in the biorepository yet.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  ) : (
                    <>
                      <InlineNotification
                        kind="info"
                        title={intl.formatMessage(
                          {
                            id: "biorepository.intake.inventory.count",
                            defaultMessage:
                              "{count} sample(s) in biorepository",
                          },
                          { count: allBioSamples.length },
                        )}
                        lowContrast
                        hideCloseButton
                        style={{ marginBottom: "1rem" }}
                      />
                      {advanceNotification && (
                        <InlineNotification
                          kind={advanceNotification.kind}
                          title={advanceNotification.title}
                          subtitle={advanceNotification.subtitle}
                          lowContrast
                          onCloseButtonClick={() =>
                            setAdvanceNotification(null)
                          }
                          style={{ marginBottom: "1rem" }}
                        />
                      )}
                      <DataTable
                        rows={allBioSamples.map((sample) => ({
                          id: sample.id.toString(),
                          barcode: sample.barcode || "-",
                          sampleType: sample.sampleType?.description || "-",
                          originLab: sample.originLab || "-",
                          receiptDate: sample.receiptDate
                            ? new Date(sample.receiptDate).toLocaleDateString()
                            : "-",
                          receiptTime: sample.receiptDate
                            ? new Date(sample.receiptDate).toLocaleTimeString(
                                "en-US",
                                {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                  hour12: false,
                                },
                              )
                            : "-",
                          biosafetyLevel: sample.biosafetyLevel || "-",
                          status: sample.status || "REGISTERED",
                          documentationStatus:
                            sample.documentationStatus || "PENDING",
                          actions: "",
                        }))}
                        headers={[
                          {
                            key: "barcode",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.barcode",
                              defaultMessage: "Barcode",
                            }),
                          },
                          {
                            key: "sampleType",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.sampleType",
                              defaultMessage: "Sample Type",
                            }),
                          },
                          {
                            key: "originLab",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.originLab",
                              defaultMessage: "Origin Lab",
                            }),
                          },
                          {
                            key: "receiptDate",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.receiptDate",
                              defaultMessage: "Receipt Date",
                            }),
                          },
                          {
                            key: "receiptTime",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.receiptTime",
                              defaultMessage: "Receipt Time",
                            }),
                          },
                          {
                            key: "biosafetyLevel",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.biosafetyLevel",
                              defaultMessage: "BSL",
                            }),
                          },
                          {
                            key: "status",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.status",
                              defaultMessage: "Status",
                            }),
                          },
                          {
                            key: "documentationStatus",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.documentationStatus",
                              defaultMessage: "Documentation",
                            }),
                          },
                          {
                            key: "actions",
                            header: intl.formatMessage({
                              id: "biorepository.sample.field.actions",
                              defaultMessage: "Actions",
                            }),
                          },
                        ]}
                      >
                        {({
                          rows,
                          headers,
                          getTableProps,
                          getHeaderProps,
                          getRowProps,
                          getSelectionProps,
                          getBatchActionProps,
                          selectedRows,
                        }) => {
                          const batchActionProps = getBatchActionProps();
                          return (
                            <TableContainer>
                              <TableToolbar>
                                <TableBatchActions {...batchActionProps}>
                                  <TableBatchAction
                                    tabIndex={
                                      batchActionProps.shouldShowBatchActions
                                        ? 0
                                        : -1
                                    }
                                    renderIcon={Barcode}
                                    onClick={() => {
                                      if (selectedRows.length === 0) return;
                                      const selectedSample = allBioSamples.find(
                                        (s) =>
                                          s.id.toString() ===
                                          selectedRows[0].id,
                                      );
                                      if (!selectedSample) return;
                                      // Use accessionNumber for LabelMakerServlet lookup
                                      const accessionNum =
                                        selectedSample.accessionNumber;
                                      const displayLabel =
                                        selectedSample.barcode ||
                                        selectedSample.id.toString();
                                      setBarcodeLabNo(displayLabel);
                                      setBarcodeSource(
                                        `/LabelMakerServlet?labNo=${accessionNum}&type=generic&sampleType=${encodeURIComponent(selectedSample.sampleType?.description || "")}&from=Biorepository`,
                                      );
                                      setRenderBarcode(true);
                                    }}
                                  >
                                    <FormattedMessage
                                      id="biorepository.inventory.generateBarcode"
                                      defaultMessage="Generate Barcode"
                                    />
                                  </TableBatchAction>
                                  <TableBatchAction
                                    tabIndex={
                                      batchActionProps.shouldShowBatchActions
                                        ? 0
                                        : -1
                                    }
                                    renderIcon={ArrowRight}
                                    onClick={() =>
                                      handleAdvanceToStorage(selectedRows)
                                    }
                                    disabled={advancingToStorage}
                                  >
                                    <FormattedMessage
                                      id="biorepository.inventory.advanceToStorage"
                                      defaultMessage="Advance to Storage"
                                    />
                                  </TableBatchAction>
                                </TableBatchActions>
                                <TableToolbarContent>
                                  <p
                                    style={{
                                      fontSize: "0.875rem",
                                      color: "#525252",
                                    }}
                                  >
                                    <FormattedMessage
                                      id="biorepository.inventory.selectHint"
                                      defaultMessage="Select samples for batch actions"
                                    />
                                  </p>
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
                                    const sample = allBioSamples.find(
                                      (s) => s.id.toString() === row.id,
                                    );
                                    return (
                                      <TableRow
                                        {...getRowProps({ row })}
                                        key={row.id}
                                      >
                                        <TableSelectRow
                                          {...getSelectionProps({ row })}
                                        />
                                        {row.cells.map((cell) => {
                                          if (cell.info.header === "status") {
                                            let statusColor = "gray";
                                            if (
                                              sample?.status === "REGISTERED"
                                            ) {
                                              statusColor = "blue";
                                            } else if (
                                              sample?.status === "STORED"
                                            ) {
                                              statusColor = "green";
                                            } else if (
                                              sample?.status === "QUARANTINE"
                                            ) {
                                              statusColor = "red";
                                            }
                                            return (
                                              <TableCell key={cell.id}>
                                                <Tag type={statusColor}>
                                                  {cell.value}
                                                </Tag>
                                              </TableCell>
                                            );
                                          }
                                          if (
                                            cell.info.header ===
                                            "documentationStatus"
                                          ) {
                                            let docColor = "gray";
                                            if (
                                              sample?.documentationStatus ===
                                              "VERIFIED"
                                            ) {
                                              docColor = "green";
                                            } else if (
                                              sample?.documentationStatus ===
                                              "QUARANTINE"
                                            ) {
                                              docColor = "red";
                                            } else if (
                                              sample?.documentationStatus ===
                                              "PENDING"
                                            ) {
                                              docColor = "purple";
                                            }
                                            return (
                                              <TableCell key={cell.id}>
                                                <Tag type={docColor}>
                                                  {cell.value}
                                                </Tag>
                                              </TableCell>
                                            );
                                          }
                                          if (
                                            cell.info.header ===
                                            "biosafetyLevel"
                                          ) {
                                            let bslColor = "gray";
                                            if (cell.value === "BSL_1") {
                                              bslColor = "green";
                                            } else if (cell.value === "BSL_2") {
                                              bslColor = "teal";
                                            } else if (cell.value === "BSL_3") {
                                              bslColor = "purple";
                                            } else if (cell.value === "BSL_4") {
                                              bslColor = "red";
                                            }
                                            return (
                                              <TableCell key={cell.id}>
                                                <Tag type={bslColor}>
                                                  {cell.value}
                                                </Tag>
                                              </TableCell>
                                            );
                                          }
                                          if (cell.info.header === "actions") {
                                            return (
                                              <TableCell key={cell.id}>
                                                <Button
                                                  kind="ghost"
                                                  size="sm"
                                                  hasIconOnly
                                                  renderIcon={Barcode}
                                                  iconDescription={intl.formatMessage(
                                                    {
                                                      id: "biorepository.inventory.generateBarcode",
                                                      defaultMessage:
                                                        "Generate Barcode",
                                                    },
                                                  )}
                                                  onClick={() => {
                                                    const accessionNum =
                                                      sample?.accessionNumber;
                                                    const displayLabel =
                                                      sample?.barcode ||
                                                      sample?.id?.toString();
                                                    if (accessionNum) {
                                                      setBarcodeLabNo(
                                                        displayLabel,
                                                      );
                                                      setBarcodeSource(
                                                        `/LabelMakerServlet?labNo=${accessionNum}&type=generic&sampleType=${encodeURIComponent(sample?.sampleType?.description || "")}&from=Biorepository`,
                                                      );
                                                      setRenderBarcode(true);
                                                    }
                                                  }}
                                                />
                                              </TableCell>
                                            );
                                          }
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
                      </DataTable>
                    </>
                  )}
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      {/* Documentation Verification Modal */}
      <DocumentationVerificationModal
        open={verificationModalOpen}
        onClose={() => setVerificationModalOpen(false)}
        shipment={currentShipment}
        onVerificationComplete={handleVerificationComplete}
      />

      {/* Manifest Upload Modal */}
      <ManifestUploadModal
        open={manifestModalOpen}
        onClose={() => setManifestModalOpen(false)}
        shipmentId={currentShipment?.id}
        onImportComplete={handleBulkImportCompleteWithRefresh}
      />

      {/* Barcode Display Modal */}
      <Modal
        open={renderBarcode}
        onRequestClose={() => {
          setRenderBarcode(false);
          setBarcodeSource("about:blank");
          setBarcodeLabNo("");
        }}
        modalHeading={
          <FormattedMessage
            id="biorepository.barcode.modal.heading"
            defaultMessage="Barcode - {labNo}"
            values={{ labNo: barcodeLabNo }}
          />
        }
        passiveModal
        size="lg"
      >
        <div style={{ minHeight: "500px" }}>
          <iframe
            src={barcodeSource}
            width="100%"
            height="500px"
            title={intl.formatMessage({
              id: "biorepository.barcode.modal.iframeTitle",
              defaultMessage: "Barcode Preview",
            })}
            style={{ border: "1px solid #e0e0e0" }}
          />
        </div>
      </Modal>
    </div>
  );
}

BiorepositoryIntakePage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryIntakePage;
