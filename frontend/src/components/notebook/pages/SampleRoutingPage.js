import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Tag,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Chemistry, SendAlt, Archive, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import BoxLayoutViewer from "../workflow/BoxLayoutViewer";
import StorageHierarchySelector from "../workflow/StorageHierarchySelector";
import AssayPlateCreator from "../workflow/AssayPlateCreator";
import "../workflow/NotebookWorkflow.css";

/**
 * SampleRoutingPage - Page 5 of the immunology workflow.
 * Handles routing child samples to destinations: internal analysis, external lab, or storage.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID (used for routing API calls)
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function SampleRoutingPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Routing summary state
  const [routingSummary, setRoutingSummary] = useState({
    internalAnalysis: 0,
    externalLab: 0,
    storage: 0,
    unrouted: 0,
    total: 0,
  });

  // Routing modal state
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [routeDestination, setRouteDestination] = useState(null);
  const [routing, setRouting] = useState(false);

  // Destination-specific fields
  const [selectedBox, setSelectedBox] = useState(null);
  const [externalLabName, setExternalLabName] = useState("");
  const [shipmentDate, setShipmentDate] = useState(null);
  // Additional external lab fields
  const [chainOfCustodyNotes, setChainOfCustodyNotes] = useState("");
  const [packagingRequirements, setPackagingRequirements] = useState("");
  const [shipmentStatus, setShipmentStatus] = useState("PENDING");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [externalLabContact, setExternalLabContact] = useState("");

  // Box layout state
  const [selectedBoxForLayout, setSelectedBoxForLayout] = useState(null);

  // Hierarchical storage selection state (for STORAGE destination and Box Layout tab)
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [tabBoxLayout, setTabBoxLayout] = useState({});

  // Storage modal box layout state (for previewing occupancy before routing)
  const [storageModalBoxLayout, setStorageModalBoxLayout] = useState({});
  const [loadingStorageLayout, setLoadingStorageLayout] = useState(false);
  // Well assignments for storage preview (sampleId -> wellCoord)
  const [storageWellAssignments, setStorageWellAssignments] = useState({});

  // Additional storage fields
  const [storagePurpose, setStoragePurpose] = useState("SHORT_TERM");
  const [storageTemperature, setStorageTemperature] = useState("");
  const [retrievalDate, setRetrievalDate] = useState(null);
  const [storageNotes, setStorageNotes] = useState("");

  // Assay plate state (for Internal Analysis - NOT connected to storage hierarchy)
  const [assayPlates, setAssayPlates] = useState([]);
  const [selectedAssayPlateId, setSelectedAssayPlateId] = useState(null);

  // Destination options
  const destinationOptions = [
    {
      id: "INTERNAL_ANALYSIS",
      label: intl.formatMessage({
        id: "notebook.routing.destination.internal",
        defaultMessage: "Internal Analysis",
      }),
      icon: Chemistry,
    },
    {
      id: "EXTERNAL_LAB",
      label: intl.formatMessage({
        id: "notebook.routing.destination.external",
        defaultMessage: "External Lab",
      }),
      icon: SendAlt,
    },
    {
      id: "STORAGE",
      label: intl.formatMessage({
        id: "notebook.routing.destination.storage",
        defaultMessage: "Storage",
      }),
      icon: Archive,
    },
  ];

  // Load samples and routing summary
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadRoutingSummary();

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

    // Load only COMPLETED samples - these are samples that finished aliquoting on ChildSampleCreationPage
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples?status=COMPLETED`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              // Use routing status for display - COMPLETED means routed, PENDING means awaiting routing
              status: sample.destinationType ? "COMPLETED" : "PENDING",
              routingStatus: sample.destinationType ? "ROUTED" : "UNROUTED",
              destinationType: sample.destinationType,
              wellCoordinate: sample.wellCoordinate,
              // Aliquot data from child sample creation (stored in data JSONB)
              volume: sample.data?.volume,
              volumeUnit: sample.data?.volumeUnit || "uL",
              initialVolume: sample.data?.initialVolume,
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

  const loadRoutingSummary = useCallback(() => {
    if (!notebookId) return;

    getFromOpenElisServer(
      `/rest/notebook/${notebookId}/samples/routing`,
      (response) => {
        if (componentMounted.current && response) {
          setRoutingSummary({
            internalAnalysis: response.internalAnalysis || 0,
            externalLab: response.externalLab || 0,
            storage: response.storage || 0,
            unrouted: response.unrouted || 0,
            total: response.total || 0,
          });
        }
      },
    );
  }, [notebookId]);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle route modal open
  const handleOpenRouteModal = useCallback(
    (destination) => {
      if (selectedSampleIds.length === 0) {
        setError("Please select at least one sample to route.");
        return;
      }
      setRouteDestination(destination);
      // Clear any previous storage well assignments when opening modal
      setStorageWellAssignments({});
      // Reset external lab fields
      setExternalLabName("");
      setExternalLabContact("");
      setShipmentDate(null);
      setChainOfCustodyNotes("");
      setPackagingRequirements("");
      setShipmentStatus("PENDING");
      setTrackingNumber("");
      // Reset storage fields
      setStoragePurpose("SHORT_TERM");
      setStorageTemperature("");
      setRetrievalDate(null);
      setStorageNotes("");
      setRouteModalOpen(true);
    },
    [selectedSampleIds],
  );

  // Handle routing
  const handleRouteSamples = useCallback(() => {
    if (selectedSampleIds.length === 0 || !routeDestination || !hasRealPageId)
      return;

    setRouting(true);
    setError(null);

    const routeRequest = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      destinationType: routeDestination.id,
      pageId: pageData?.id, // Include pageId so backend can update status
    };

    // Add destination-specific fields
    if (routeDestination.id === "INTERNAL_ANALYSIS") {
      // Use assay plates (temporary, not connected to storage hierarchy)
      const selectedPlate = assayPlates.find(
        (p) => p.id === selectedAssayPlateId,
      );
      if (!selectedPlate) {
        setError(
          "Please create and select an assay plate for internal analysis.",
        );
        setRouting(false);
        return;
      }
      // Send plate info for well auto-assignment (backend will handle as temporary plate)
      routeRequest.assayPlate = {
        id: selectedPlate.id,
        name: selectedPlate.name,
        rows: selectedPlate.rows,
        columns: selectedPlate.columns,
      };
    } else if (routeDestination.id === "EXTERNAL_LAB") {
      if (!externalLabName.trim()) {
        setError("Please enter the destination laboratory name.");
        setRouting(false);
        return;
      }
      routeRequest.externalLabName = externalLabName;
      if (externalLabContact) {
        routeRequest.externalLabContact = externalLabContact;
      }
      if (shipmentDate) {
        routeRequest.shipmentDate = shipmentDate;
      }
      if (chainOfCustodyNotes) {
        routeRequest.chainOfCustodyNotes = chainOfCustodyNotes;
      }
      if (packagingRequirements) {
        routeRequest.packagingRequirements = packagingRequirements;
      }
      routeRequest.shipmentStatus = shipmentStatus;
      if (trackingNumber) {
        routeRequest.trackingNumber = trackingNumber;
      }
    } else if (routeDestination.id === "STORAGE") {
      // Box is REQUIRED for storage to create proper storage assignment
      if (!selectedBox) {
        setError(
          "Please select a storage box. Storage routing requires a box assignment.",
        );
        setRouting(false);
        return;
      }

      // Check if wells have been assigned (via Auto-Populate button)
      if (Object.keys(storageWellAssignments).length === 0) {
        setError(
          "Please click Auto-Populate to assign samples to wells before routing.",
        );
        setRouting(false);
        return;
      }

      const storagePayload = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        boxId: selectedBox.id,
        wellAssignments: storageWellAssignments,
        condition: storageTemperature || "REFRIGERATED",
        storagePurpose: storagePurpose,
        retrievalDate: retrievalDate,
        storageNotes: storageNotes,
        retentionYears: 5, // Default retention
        pageId: pageData?.id,
      };

      postToOpenElisServerJsonResponse(
        `/rest/notebook/${notebookId}/samples/assign-storage`,
        JSON.stringify(storagePayload),
        (response) => {
          setRouting(false);
          setRouteModalOpen(false);

          if (response && response.success) {
            setSuccess(
              `Successfully assigned ${response.assignedCount || selectedSampleIds.length} samples to storage.`,
            );
            setSelectedSampleIds([]);
            loadPageSamples();
            loadRoutingSummary();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to assign samples to storage.");
          }
        },
      );
      return; // Exit early - don't use the generic route endpoint
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/route`,
      JSON.stringify(routeRequest),
      (response) => {
        setRouting(false);
        setRouteModalOpen(false);

        if (response && response.success) {
          setSuccess(
            `Successfully routed ${response.routedCount} samples to ${routeDestination.label}.`,
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          loadRoutingSummary();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to route samples.");
        }
      },
    );
  }, [
    selectedSampleIds,
    routeDestination,
    hasRealPageId,
    selectedBox,
    externalLabName,
    externalLabContact,
    shipmentDate,
    chainOfCustodyNotes,
    packagingRequirements,
    shipmentStatus,
    trackingNumber,
    storagePurpose,
    storageTemperature,
    retrievalDate,
    storageNotes,
    notebookId,
    pageData?.id,
    assayPlates,
    selectedAssayPlateId,
    loadPageSamples,
    loadRoutingSummary,
    onProgressUpdate,
    storageWellAssignments,
  ]);

  // Auto-populate wells for storage routing (preview before save)
  const handleAutoPopulateStorage = useCallback(() => {
    if (!selectedBox) {
      setError("Please select a storage box first.");
      return;
    }
    if (selectedSampleIds.length === 0) {
      setError("No samples selected for storage assignment.");
      return;
    }

    const rows = selectedBox.rows || 8;
    const columns = selectedBox.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    // Iterate through wells row by row
    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        // Skip if well is already occupied
        if (!storageModalBoxLayout[wellCoord]) {
          newAssignments[parseInt(selectedSampleIds[sampleIndex], 10)] =
            wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setStorageWellAssignments(newAssignments);

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.routing.storage.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccess(
        intl.formatMessage(
          {
            id: "notebook.routing.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  }, [selectedBox, selectedSampleIds, storageModalBoxLayout, intl]);

  // Get combined layout showing existing wells + pending assignments
  const getCombinedStorageLayout = useCallback(() => {
    const combined = { ...storageModalBoxLayout };

    // Add pending assignments
    Object.entries(storageWellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  }, [storageModalBoxLayout, storageWellAssignments, samples]);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError("Cannot update status: Page not properly initialized.");
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate],
  );

  // Render routing status tag (Routed/Unrouted)
  const renderRoutingStatusTag = (value, sample) => {
    const s = sample || value;
    if (!s?.destinationType) {
      return <Tag type="gray">Unrouted</Tag>;
    }
    return <Tag type="green">Routed</Tag>;
  };

  // Render destination tag
  const renderDestinationTag = (value, sample) => {
    const s = sample || value;
    if (!s?.destinationType) {
      return <span style={{ color: "#8d8d8d" }}>-</span>;
    }

    const tagTypes = {
      INTERNAL_ANALYSIS: "blue",
      EXTERNAL_LAB: "purple",
      STORAGE: "cyan",
    };

    const labels = {
      INTERNAL_ANALYSIS: "Internal Analysis",
      EXTERNAL_LAB: "External Lab",
      STORAGE: "Storage",
    };

    return (
      <Tag type={tagTypes[s.destinationType] || "gray"}>
        {labels[s.destinationType] || s.destinationType}
        {s.wellCoordinate && ` (${s.wellCoordinate})`}
      </Tag>
    );
  };

  return (
    <div className="sample-routing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.routing.title"
            defaultMessage="Sample Routing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.routing.description"
            defaultMessage="Route samples to their destinations: internal analysis (with box/well assignment), external lab, or storage."
          />
        </p>
      </div>

      {/* Routing Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.routing.unrouted"
                  defaultMessage="Unrouted"
                />
              </span>
              <span className="progress-value">{routingSummary.unrouted}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.routing.internal"
                  defaultMessage="Internal Analysis"
                />
              </span>
              <span className="progress-value">
                {routingSummary.internalAnalysis}
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.routing.external"
                  defaultMessage="External Lab"
                />
              </span>
              <span className="progress-value">
                {routingSummary.externalLab}
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.routing.storage"
                  defaultMessage="Storage"
                />
              </span>
              <span className="progress-value">{routingSummary.storage}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={() =>
            handleOpenRouteModal(
              destinationOptions.find((d) => d.id === "INTERNAL_ANALYSIS"),
            )
          }
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.routing.routeInternal"
            defaultMessage="Route to Internal Analysis"
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={SendAlt}
          onClick={() =>
            handleOpenRouteModal(
              destinationOptions.find((d) => d.id === "EXTERNAL_LAB"),
            )
          }
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.routing.routeExternal"
            defaultMessage="Route to External Lab"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Archive}
          onClick={() =>
            handleOpenRouteModal(
              destinationOptions.find((d) => d.id === "STORAGE"),
            )
          }
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.routing.routeStorage"
            defaultMessage="Route to Storage"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={() => {
            loadPageSamples();
            loadRoutingSummary();
          }}
        >
          <FormattedMessage
            id="notebook.routing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Tabs for Samples and Box Layout */}
      <Tabs>
        <TabList aria-label="Routing tabs">
          <Tab>
            <FormattedMessage
              id="notebook.routing.tab.samples"
              defaultMessage="Samples"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.routing.tab.boxLayout"
              defaultMessage="Box Layout"
            />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            {/* Sample Grid */}
            <div className="sample-grid-container">
              <SampleGrid
                gridId="sample-routing"
                samples={samples}
                selectedIds={selectedSampleIds}
                onSelectionChange={setSelectedSampleIds}
                onStatusChange={handleStatusChange}
                statusFilter={statusFilter}
                onStatusFilterChange={setStatusFilter}
                showSelection={true}
                loading={loading}
                additionalColumns={[
                  {
                    key: "volume",
                    header: intl.formatMessage({
                      id: "notebook.grid.extractedVolume",
                      defaultMessage: "Extracted Vol.",
                    }),
                    render: (value, sample) =>
                      value ? `${value} ${sample.volumeUnit || "uL"}` : "-",
                  },
                  {
                    key: "initialVolume",
                    header: intl.formatMessage({
                      id: "notebook.grid.remainingVolume",
                      defaultMessage: "Remaining Vol.",
                    }),
                    render: (value, sample) =>
                      value ? `${value} ${sample.volumeUnit || "uL"}` : "-",
                  },
                  {
                    key: "routingStatus",
                    header: intl.formatMessage({
                      id: "notebook.grid.routingStatus",
                      defaultMessage: "Routing Status",
                    }),
                    render: renderRoutingStatusTag,
                  },
                  {
                    key: "destination",
                    header: intl.formatMessage({
                      id: "notebook.grid.destination",
                      defaultMessage: "Destination",
                    }),
                    render: renderDestinationTag,
                  },
                ]}
              />
            </div>
          </TabPanel>
          <TabPanel>
            {/* Box Selection and Layout with Hierarchical Selection */}
            <div style={{ marginTop: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.routing.selectStorageLocation"
                  defaultMessage="Select Storage Location"
                />
              </h5>
              <StorageHierarchySelector
                onSelectionChange={(selection) => {
                  if (selection.box) {
                    setSelectedBoxForLayout(selection.box.id);
                    // Load box layout
                    getFromOpenElisServer(
                      `/rest/notebook/${notebookId}/box/${selection.box.id}/layout`,
                      (response) => {
                        if (response && response.wells) {
                          setTabBoxLayout(response.wells);
                        } else {
                          setTabBoxLayout({});
                        }
                      },
                    );
                  } else {
                    setSelectedBoxForLayout(null);
                    setTabBoxLayout({});
                  }
                }}
                entryId={entryId}
                showPath={true}
              />

              {/* Box Layout Viewer */}
              {selectedBoxForLayout && (
                <div style={{ marginTop: "1rem" }}>
                  <BoxLayoutViewer
                    boxId={selectedBoxForLayout}
                    layout={tabBoxLayout}
                    rows={8}
                    columns={12}
                  />
                </div>
              )}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.routing.empty"
              defaultMessage="No samples available for routing. Please complete the child sample creation step first."
            />
          </p>
        </div>
      )}

      {/* Route Modal */}
      <Modal
        open={routeModalOpen}
        size={routeDestination?.id === "STORAGE" ? "lg" : "md"}
        modalHeading={
          routeDestination
            ? intl.formatMessage(
                {
                  id: "notebook.routing.modal.title",
                  defaultMessage: "Route to {destination}",
                },
                { destination: routeDestination.label },
              )
            : ""
        }
        primaryButtonText={intl.formatMessage({
          id: "notebook.routing.modal.route",
          defaultMessage: "Route Samples",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.routing.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setRouteModalOpen(false)}
        onRequestSubmit={handleRouteSamples}
        primaryButtonDisabled={routing}
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.routing.modal.description"
              defaultMessage="Route {count} selected sample(s) to {destination}."
              values={{
                count: selectedSampleIds.length,
                destination: routeDestination?.label || "",
              }}
            />
          </p>
        </div>

        {/* Internal Analysis Fields - Uses Assay Plates (NOT connected to storage hierarchy) */}
        {routeDestination?.id === "INTERNAL_ANALYSIS" && (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.routing.modal.internalInfo"
                defaultMessage="Create assay plates for internal analysis. These are temporary plates for running assays - not connected to storage."
              />
            </p>
            <AssayPlateCreator
              plates={assayPlates}
              onPlatesChange={setAssayPlates}
              selectedPlateId={selectedAssayPlateId}
              onPlateSelect={setSelectedAssayPlateId}
              sampleCount={selectedSampleIds.length}
            />
            <p
              style={{
                marginTop: "0.5rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              <FormattedMessage
                id="notebook.routing.modal.boxHelp"
                defaultMessage="Wells will be auto-assigned in row-major order (A1, A2, ..., A12, B1, ...)"
              />
            </p>
          </div>
        )}

        {/* External Lab Fields */}
        {routeDestination?.id === "EXTERNAL_LAB" && (
          <div className="external-lab-fields">
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.routing.modal.externalLabInfo"
                defaultMessage="Prepare samples for external shipment. Complete the chain of custody documentation and packaging requirements."
              />
            </p>

            <Accordion>
              {/* Destination Laboratory Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.external.destinationSection",
                  defaultMessage: "Destination Laboratory",
                })}
                open
              >
                <Grid narrow>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="external-lab-name"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.modal.labName",
                        defaultMessage: "Laboratory Name *",
                      })}
                      value={externalLabName}
                      onChange={(e) => setExternalLabName(e.target.value)}
                      required
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="external-lab-contact"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.modal.labContact",
                        defaultMessage: "Contact Person / Email",
                      })}
                      value={externalLabContact}
                      onChange={(e) => setExternalLabContact(e.target.value)}
                      placeholder="e.g., Dr. Smith / lab@example.com"
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* Chain of Custody Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.external.custodySection",
                  defaultMessage: "Chain of Custody Documentation",
                })}
                open
              >
                <TextArea
                  id="chain-of-custody-notes"
                  labelText={intl.formatMessage({
                    id: "notebook.routing.modal.custodyNotes",
                    defaultMessage: "Chain of Custody Notes",
                  })}
                  value={chainOfCustodyNotes}
                  onChange={(e) => setChainOfCustodyNotes(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "notebook.routing.modal.custodyNotesPlaceholder",
                    defaultMessage:
                      "Document sample handling, transfer records, and any special custody requirements...",
                  })}
                  rows={3}
                />
              </AccordionItem>

              {/* Packaging Requirements Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.external.packagingSection",
                  defaultMessage: "Packaging Requirements",
                })}
                open
              >
                <TextArea
                  id="packaging-requirements"
                  labelText={intl.formatMessage({
                    id: "notebook.routing.modal.packagingRequirements",
                    defaultMessage: "Packaging & Shipping Requirements",
                  })}
                  value={packagingRequirements}
                  onChange={(e) => setPackagingRequirements(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "notebook.routing.modal.packagingPlaceholder",
                    defaultMessage:
                      "e.g., Triple packaging, dry ice required, temperature range -20°C to -80°C, UN3373 compliant...",
                  })}
                  rows={3}
                />
              </AccordionItem>

              {/* Shipment Status Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.external.shipmentSection",
                  defaultMessage: "Shipment Status & Tracking",
                })}
                open
              >
                <Grid narrow>
                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={([date]) =>
                        setShipmentDate(date?.toISOString().split("T")[0])
                      }
                    >
                      <DatePickerInput
                        id="shipment-date"
                        labelText={intl.formatMessage({
                          id: "notebook.routing.modal.shipmentDate",
                          defaultMessage: "Planned Shipment Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="shipment-status"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.modal.shipmentStatus",
                        defaultMessage: "Shipment Status",
                      })}
                      value={shipmentStatus}
                      onChange={(e) => setShipmentStatus(e.target.value)}
                    >
                      <SelectItem value="PENDING" text="Pending Preparation" />
                      <SelectItem
                        value="PREPARED"
                        text="Prepared for Shipment"
                      />
                      <SelectItem value="SHIPPED" text="Shipped" />
                      <SelectItem value="IN_TRANSIT" text="In Transit" />
                      <SelectItem value="DELIVERED" text="Delivered" />
                    </Select>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <TextInput
                      id="tracking-number"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.modal.trackingNumber",
                        defaultMessage: "Tracking Number / Reference",
                      })}
                      value={trackingNumber}
                      onChange={(e) => setTrackingNumber(e.target.value)}
                      placeholder="Enter courier tracking number when available"
                    />
                  </Column>
                </Grid>
              </AccordionItem>
            </Accordion>
          </div>
        )}

        {/* Storage Fields */}
        {routeDestination?.id === "STORAGE" && (
          <div className="storage-routing-fields">
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.routing.modal.storageInfo"
                defaultMessage="Store samples for future analysis. Record storage location, temperature, and retrieval schedule."
              />
            </p>

            <Accordion>
              {/* Storage Purpose Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.storage.purposeSection",
                  defaultMessage: "Storage Purpose",
                })}
                open
              >
                <Grid narrow>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="storage-purpose"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.storage.purpose",
                        defaultMessage: "Storage Purpose *",
                      })}
                      value={storagePurpose}
                      onChange={(e) => setStoragePurpose(e.target.value)}
                    >
                      <SelectItem
                        value="SHORT_TERM"
                        text={intl.formatMessage({
                          id: "notebook.routing.storage.shortTerm",
                          defaultMessage: "Designated Short-Term Storage",
                        })}
                      />
                      <SelectItem
                        value="FUTURE_ANALYSIS"
                        text={intl.formatMessage({
                          id: "notebook.routing.storage.futureAnalysis",
                          defaultMessage: "Store for Future Analysis",
                        })}
                      />
                      <SelectItem
                        value="ARCHIVAL"
                        text={intl.formatMessage({
                          id: "notebook.routing.storage.archival",
                          defaultMessage: "Archival / Long-Term Retention",
                        })}
                      />
                      <SelectItem
                        value="QUALITY_CONTROL"
                        text={intl.formatMessage({
                          id: "notebook.routing.storage.qc",
                          defaultMessage: "Quality Control Reference",
                        })}
                      />
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="storage-temperature"
                      labelText={intl.formatMessage({
                        id: "notebook.routing.storage.temperature",
                        defaultMessage: "Storage Temperature *",
                      })}
                      value={storageTemperature}
                      onChange={(e) => setStorageTemperature(e.target.value)}
                    >
                      <SelectItem value="" text="Select temperature..." />
                      <SelectItem
                        value="ROOM_TEMP"
                        text="Room Temperature (15-25°C)"
                      />
                      <SelectItem
                        value="REFRIGERATED"
                        text="Refrigerated (2-8°C)"
                      />
                      <SelectItem
                        value="FROZEN_MINUS20"
                        text="Frozen (-20°C)"
                      />
                      <SelectItem
                        value="FROZEN_MINUS80"
                        text="Ultra-Low (-80°C)"
                      />
                      <SelectItem
                        value="LIQUID_NITROGEN"
                        text="Liquid Nitrogen (-196°C)"
                      />
                    </Select>
                  </Column>
                </Grid>
              </AccordionItem>

              {/* Storage Location Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.storage.locationSection",
                  defaultMessage:
                    "Storage Location (Room → Freezer → Rack → Box → Position)",
                })}
                open
              >
                <StorageHierarchySelector
                  onSelectionChange={(selection) => {
                    setStorageSelection(selection);
                    // Clear pending well assignments when box changes
                    setStorageWellAssignments({});
                    if (selection.box) {
                      setSelectedBox(selection.box);
                      // Load box layout to show current occupancy
                      setLoadingStorageLayout(true);
                      getFromOpenElisServer(
                        `/rest/notebook/${notebookId}/box/${selection.box.id}/layout?includeGlobal=true`,
                        (response) => {
                          setLoadingStorageLayout(false);
                          if (response && response.wells) {
                            setStorageModalBoxLayout(response.wells);
                          } else {
                            setStorageModalBoxLayout({});
                          }
                        },
                      );
                    } else {
                      setSelectedBox(null);
                      setStorageModalBoxLayout({});
                    }
                  }}
                  entryId={entryId}
                  showPath={true}
                />

                {/* Display selected path */}
                {storageSelection.room && (
                  <div
                    style={{
                      marginTop: "0.5rem",
                      padding: "0.5rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                      fontSize: "0.875rem",
                    }}
                  >
                    <strong>
                      <FormattedMessage
                        id="notebook.routing.storage.selectedPath"
                        defaultMessage="Selected Location:"
                      />
                    </strong>{" "}
                    {storageSelection.room?.name || "-"}
                    {storageSelection.device &&
                      ` → ${storageSelection.device.name}`}
                    {storageSelection.shelf &&
                      ` → ${storageSelection.shelf.name}`}
                    {storageSelection.rack &&
                      ` → ${storageSelection.rack.name}`}
                    {storageSelection.box && ` → ${storageSelection.box.name}`}
                  </div>
                )}

                {/* Box Layout Preview with Auto-Populate */}
                {selectedBox && (
                  <div style={{ marginTop: "1rem" }}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <h5>
                        <FormattedMessage
                          id="notebook.routing.modal.boxLayoutPreview"
                          defaultMessage="Box Layout Preview"
                        />
                      </h5>
                      <Button
                        kind="tertiary"
                        size="sm"
                        renderIcon={Renew}
                        onClick={handleAutoPopulateStorage}
                        disabled={selectedSampleIds.length === 0}
                      >
                        <FormattedMessage
                          id="notebook.routing.storage.autoPopulate"
                          defaultMessage="Auto-Populate"
                        />
                      </Button>
                    </div>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.routing.modal.boxLayoutPreviewDescription"
                        defaultMessage="Click Auto-Populate to preview well assignments. Pending assignments shown in yellow."
                      />
                    </p>
                    {loadingStorageLayout ? (
                      <p style={{ fontStyle: "italic", color: "#8d8d8d" }}>
                        <FormattedMessage
                          id="notebook.routing.modal.loadingLayout"
                          defaultMessage="Loading box layout..."
                        />
                      </p>
                    ) : (
                      <BoxLayoutViewer
                        boxId={selectedBox.id}
                        layout={getCombinedStorageLayout()}
                        rows={selectedBox.rows || 8}
                        columns={selectedBox.columns || 12}
                      />
                    )}

                    {/* Assignment Summary */}
                    <div
                      style={{
                        marginTop: "0.5rem",
                        fontSize: "0.875rem",
                        color: "#525252",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.routing.storage.assignmentSummary"
                        defaultMessage="{assigned} of {total} samples assigned to wells"
                        values={{
                          assigned: Object.keys(storageWellAssignments).length,
                          total: selectedSampleIds.length,
                        }}
                      />
                    </div>
                  </div>
                )}
              </AccordionItem>

              {/* Retrieval Schedule Section */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.routing.storage.retrievalSection",
                  defaultMessage: "Retrieval Schedule (Optional)",
                })}
              >
                <Grid narrow>
                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={([date]) =>
                        setRetrievalDate(date?.toISOString().split("T")[0])
                      }
                    >
                      <DatePickerInput
                        id="retrieval-date"
                        labelText={intl.formatMessage({
                          id: "notebook.routing.storage.retrievalDate",
                          defaultMessage: "Scheduled Retrieval Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <p
                      style={{
                        fontSize: "0.75rem",
                        color: "#525252",
                        marginTop: "1.5rem",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.routing.storage.retrievalInfo"
                        defaultMessage="Set a retrieval date if samples are scheduled for future processing or analysis."
                      />
                    </p>
                  </Column>
                </Grid>
                <div style={{ marginTop: "1rem" }}>
                  <TextArea
                    id="storage-notes"
                    labelText={intl.formatMessage({
                      id: "notebook.routing.storage.notes",
                      defaultMessage: "Storage Notes",
                    })}
                    value={storageNotes}
                    onChange={(e) => setStorageNotes(e.target.value)}
                    placeholder={intl.formatMessage({
                      id: "notebook.routing.storage.notesPlaceholder",
                      defaultMessage:
                        "Additional notes about storage requirements, special handling, or retrieval instructions...",
                    })}
                    rows={3}
                  />
                </div>
              </AccordionItem>
            </Accordion>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default SampleRoutingPage;
