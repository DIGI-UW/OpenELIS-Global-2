import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  DatePicker,
  DatePickerInput,
  Tag,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
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
        setError("Please enter the external lab name.");
        setRouting(false);
        return;
      }
      routeRequest.externalLabName = externalLabName;
      if (shipmentDate) {
        routeRequest.shipmentDate = shipmentDate;
      }
    } else if (routeDestination.id === "STORAGE") {
      // Box is optional for storage - if selected, wells will be auto-assigned
      if (selectedBox) {
        routeRequest.boxId = selectedBox.id;
      }
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
    shipmentDate,
    entryId,
    loadPageSamples,
    loadRoutingSummary,
    onProgressUpdate,
  ]);

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
  const renderRoutingStatusTag = (sample) => {
    if (!sample.destinationType) {
      return <Tag type="gray">Unrouted</Tag>;
    }
    return <Tag type="green">Routed</Tag>;
  };

  // Render destination tag
  const renderDestinationTag = (sample) => {
    if (!sample.destinationType) {
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
      <Tag type={tagTypes[sample.destinationType] || "gray"}>
        {labels[sample.destinationType] || sample.destinationType}
        {sample.wellCoordinate && ` (${sample.wellCoordinate})`}
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
                    key: "routingStatus",
                    header: "Routing Status",
                    render: renderRoutingStatusTag,
                  },
                  {
                    key: "destination",
                    header: "Destination",
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
          <div>
            <TextInput
              id="external-lab-name"
              labelText={intl.formatMessage({
                id: "notebook.routing.modal.labName",
                defaultMessage: "External Lab Name",
              })}
              value={externalLabName}
              onChange={(e) => setExternalLabName(e.target.value)}
              style={{ marginBottom: "1rem" }}
            />
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
                  defaultMessage: "Shipment Date (Optional)",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </div>
        )}

        {/* Storage Fields */}
        {routeDestination?.id === "STORAGE" && (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.routing.modal.storageInfo"
                defaultMessage="Samples will be routed to long-term storage. Select storage location using the hierarchy below."
              />
            </p>
            <StorageHierarchySelector
              onSelectionChange={(selection) => {
                setStorageSelection(selection);
                if (selection.box) {
                  setSelectedBox(selection.box);
                } else {
                  setSelectedBox(null);
                }
              }}
              entryId={entryId}
              showPath={true}
            />
            <p
              style={{
                marginTop: "0.5rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              <FormattedMessage
                id="notebook.routing.modal.storageBoxHelp"
                defaultMessage="Wells will be auto-assigned in row-major order when a box is selected."
              />
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default SampleRoutingPage;
