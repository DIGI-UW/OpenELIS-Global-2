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
  Select,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableSelectAll,
  TableSelectRow,
  TableBatchActions,
  TableBatchAction,
} from "@carbon/react";
import { Chemistry, SendAlt, Archive, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import AssayPlateCreator from "../../workflow/AssayPlateCreator";
import "../../workflow/NotebookWorkflow.css";

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
function MedLabSampleRoutingPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  console.log(
    "[MedLabSampleRoutingPage] RENDER - entryId:",
    entryId,
    "notebookId:",
    notebookId,
    "pageData?.id:",
    pageData?.id,
  );
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

  // Retrieval request modal state
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [selectedRetrievalSamples, setSelectedRetrievalSamples] = useState([]);
  const [retrievalPurpose, setRetrievalPurpose] = useState("");
  const [retrievalDestinationDetails, setRetrievalDestinationDetails] =
    useState("");
  const [creatingRetrieval, setCreatingRetrieval] = useState(false);
  const [retrievalModalError, setRetrievalModalError] = useState(null);
  const [retrievedSamples, setRetrievedSamples] = useState([]);
  const [loadingRetrieved, setLoadingRetrieved] = useState(false);
  const [selectedRetrievedSampleIds, setSelectedRetrievedSampleIds] = useState(
    [],
  ); // Selected retrieved samples for returning to lab
  const [returningToLab, setReturningToLab] = useState(false); // Track if bulk return is in progress

  console.log({ retrievedSamples, samples });
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

  // Storage modal box layout state (for previewing occupancy before routing)
  const [storageModalBoxLayout, setStorageModalBoxLayout] = useState({});
  const [loadingStorageLayout, setLoadingStorageLayout] = useState(false);
  // Well assignments for storage preview (sampleId -> wellCoord)
  const [storageWellAssignments, setStorageWellAssignments] = useState({});

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

  // Define loadPageSamples before the useEffect that uses it
  const loadPageSamples = useCallback(() => {
    console.log(
      "[MedLabSampleRoutingPage] loadPageSamples called, entryId:",
      entryId,
    );
    if (!entryId) {
      console.log(
        "[MedLabSampleRoutingPage] entryId is falsy, returning early",
      );
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    console.log(
      "[MedLabSampleRoutingPage] Calling API: /rest/medlab/entry/" +
        entryId +
        "/samples-for-routing",
    );
    // Load QC-ACCEPTED samples that are ready for routing decision
    // These are samples that passed Quality Check (page 4)
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-routing`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Patient info is stored in sample.data field from Link to Patient feature
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              labNo: sample.labNo || sample.accessionNumber,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              patientName: sample.data?.patientName || sample.patientName || "",
              patientId: sample.data?.patientId || "",
              patientNationalId: sample.data?.patientNationalId || "",
              // Routing status - samples start as PENDING (unrouted)
              status: sample.destinationType ? "COMPLETED" : "PENDING",
              routingStatus: sample.destinationType ? "ROUTED" : "UNROUTED",
              destinationType: sample.destinationType,
              wellCoordinate: sample.wellCoordinate,
              // QC info from previous page
              qcStatus: sample.qcStatus,
              qcAcceptedDate: sample.qcAcceptedDate,
              data: sample.data, // Preserve full data for other uses
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Define loadRoutingSummary before the useEffect that uses it
  const loadRoutingSummary = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/routing-summary`,
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
  }, [entryId]);

  // Fetch completed retrieval requests from biorepository
  const loadRetrievedSamples = useCallback(() => {
    setLoadingRetrieved(true);
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests?status=COMPLETED`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          if (response.length === 0) {
            setRetrievedSamples([]);
            setLoadingRetrieved(false);
            return;
          }

          // Fetch each request individually to get items
          const allRetrievedItems = [];
          let fetchedCount = 0;

          response.forEach((request) => {
            getFromOpenElisServer(
              `/rest/biorepository/retrieval/requests/${request.id}`,
              (fullRequest) => {
                if (componentMounted.current) {
                  fetchedCount++;

                  // Add items from this request
                  if (fullRequest.items && Array.isArray(fullRequest.items)) {
                    fullRequest.items.forEach((item) => {
                      allRetrievedItems.push({
                        ...item,
                        requestNumber: fullRequest.requestNumber,
                        requestPurpose: fullRequest.requestPurpose,
                        requestedDate: fullRequest.requestedTimestamp,
                      });
                    });
                  }

                  // Once all requests fetched, update state
                  if (fetchedCount === response.length) {
                    setRetrievedSamples(allRetrievedItems);
                    setLoadingRetrieved(false);
                  }
                }
              },
            );
          });
        } else {
          setRetrievedSamples([]);
          setLoadingRetrieved(false);
        }
      },
    );
  }, []);

  // Load samples and routing summary
  useEffect(() => {
    console.log(
      "[MedLabSampleRoutingPage] useEffect triggered, entryId:",
      entryId,
      "pageData?.id:",
      pageData?.id,
    );
    componentMounted.current = true;
    loadPageSamples();
    loadRoutingSummary();
    loadRetrievedSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, loadPageSamples, loadRoutingSummary, loadRetrievedSamples]);

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
        setError("Please select a destination laboratory.");
        setRouting(false);
        return;
      }

      // Special handling for Biorepository - create transfer request + update routing status
      if (externalLabName === "Biorepository") {
        const transferRequest = {
          sourceLab: "MEDICAL_LAB",
          sampleItemIds: selectedSampleIds.map((id) => parseInt(id, 10)),
          requestNotes: `Transfer from medical lab entry ${entryId}`,
        };

        // Step 1: Create biorepository transfer request
        postToOpenElisServerJsonResponse(
          `/rest/biorepository/transfer`,
          JSON.stringify(transferRequest),
          (response) => {
            if (!componentMounted.current) return;

            if (response && response.id) {
              // Step 2: Update medical lab routing status
              const medLabRouteRequest = {
                sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
                destinationType: "EXTERNAL_LAB",
                externalLabName: "Biorepository",
                pageId: pageData?.id,
              };

              postToOpenElisServerJsonResponse(
                `/rest/medlab/route-samples`,
                JSON.stringify(medLabRouteRequest),
                (routeResponse) => {
                  if (!componentMounted.current) return;
                  setRouting(false);
                  setRouteModalOpen(false);

                  if (routeResponse && routeResponse.success) {
                    setSuccess(
                      `Successfully created biorepository transfer request #${response.id} for ${response.itemCount || selectedSampleIds.length} samples. Status: ${response.status}`,
                    );
                    setSelectedSampleIds([]);
                    loadPageSamples();
                    loadRoutingSummary();
                    if (onProgressUpdate) {
                      onProgressUpdate();
                    }
                  } else {
                    setError(
                      routeResponse?.error ||
                        "Transfer created but failed to update routing status.",
                    );
                  }
                },
              );
            } else {
              setRouting(false);
              setRouteModalOpen(false);
              setError(
                response?.error ||
                  "Failed to create biorepository transfer request.",
              );
            }
          },
        );
        return; // Exit early - don't use the generic route endpoint
      }

      // Regular external lab routing
      routeRequest.externalLabName = externalLabName;
      if (shipmentDate) {
        routeRequest.shipmentDate = shipmentDate;
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

      // Add storage metadata to route request
      // For each sample, we send the first well assignment as the primary position
      // The full well assignments can be used for batch processing
      routeRequest.storageBoxId = selectedBox.id;
      routeRequest.locationType = "box"; // storageBoxId refers to storage_box table
      routeRequest.storageWellAssignments = storageWellAssignments;

      // Get first sample's position as primary coordinate (for single-sample case)
      const firstSampleId = selectedSampleIds[0];
      const firstWellPosition = storageWellAssignments[firstSampleId];
      if (firstWellPosition) {
        routeRequest.positionCoordinate = firstWellPosition;
      }
    }

    postToOpenElisServerJsonResponse(
      `/rest/medlab/route-samples`,
      JSON.stringify(routeRequest),
      (response) => {
        if (!componentMounted.current) return;
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
    pageData?.id,
    assayPlates,
    selectedAssayPlateId,
    loadPageSamples,
    loadRoutingSummary,
    onProgressUpdate,
    storageWellAssignments,
    entryId,
  ]);

  // Handle returning retrieved samples to lab for re-routing (bulk operation)
  const handleReturnToLab = useCallback(() => {
    if (!notebookId || !pageData?.id || selectedRetrievedSampleIds.length === 0)
      return;

    setReturningToLab(true);
    setError(null);

    const unrouteRequest = {
      sampleIds: selectedRetrievedSampleIds.map((id) => parseInt(id, 10)),
      pageId: pageData.id,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/unroute`,
      JSON.stringify(unrouteRequest),
      (response) => {
        if (!componentMounted.current) return;
        setReturningToLab(false);

        if (response && response.success) {
          setSuccess(
            `Successfully returned ${response.unroutedCount} sample(s) to routing queue.`,
          );
          setSelectedRetrievedSampleIds([]); // Clear selection
          // Reload both samples and retrieved samples lists
          loadPageSamples();
          loadRetrievedSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to return sample to lab.");
        }
      },
    );
  }, [
    notebookId,
    pageData?.id,
    selectedRetrievedSampleIds,
    loadPageSamples,
    loadRetrievedSamples,
    onProgressUpdate,
  ]);

  // Handle retrieval request creation
  const handleCreateRetrievalRequest = useCallback(() => {
    if (selectedRetrievalSamples.length === 0) {
      setRetrievalModalError("Please select at least one sample to retrieve.");
      return;
    }

    if (!retrievalPurpose.trim()) {
      setRetrievalModalError(
        "Please provide a purpose for the retrieval request.",
      );
      return;
    }

    setCreatingRetrieval(true);
    setRetrievalModalError(null);

    // Directly fetch BioSamples by sample item ID
    const bioSampleIds = [];
    const notFoundSamples = [];
    let fetchCount = 0;

    selectedRetrievalSamples.forEach((sampleId) => {
      getFromOpenElisServer(
        `/rest/biorepository/sample/by-sample-item/${sampleId}`,
        (bioSample) => {
          if (!componentMounted.current) return;

          fetchCount++;

          // Collect BioSample ID if found
          if (bioSample && bioSample.id) {
            bioSampleIds.push(bioSample.id);
          } else {
            notFoundSamples.push(sampleId);
          }

          // Once all samples checked, create retrieval request
          if (fetchCount === selectedRetrievalSamples.length) {
            if (bioSampleIds.length === 0) {
              setRetrievalModalError(
                "None of the selected samples are available in the biorepository. Samples must be accepted and stored before retrieval requests can be created.",
              );
              setCreatingRetrieval(false);
              return;
            }

            // Show warning if some samples weren't found
            if (notFoundSamples.length > 0) {
              console.warn(
                `Warning: ${notFoundSamples.length} of ${selectedRetrievalSamples.length} samples were not found in biorepository. Creating request for ${bioSampleIds.length} available samples.`,
              );
            }

            // Create retrieval request
            const retrievalRequest = {
              requestPurpose: retrievalPurpose,
              bioSampleIds: bioSampleIds,
              destinationType: "ANALYSIS_RETURN",
              destinationDetails:
                retrievalDestinationDetails || `Medical Lab - Entry ${entryId}`,
              priorityLevel: "NORMAL",
            };

            postToOpenElisServerJsonResponse(
              `/rest/biorepository/retrieval/requests`,
              JSON.stringify(retrievalRequest),
              (createResponse) => {
                if (!componentMounted.current) return;

                if (createResponse && createResponse.id) {
                  // Request created in DRAFT status, now submit for approval
                  postToOpenElisServerJsonResponse(
                    `/rest/biorepository/retrieval/requests/${createResponse.id}/submit`,
                    JSON.stringify({}),
                    (submitResponse) => {
                      if (!componentMounted.current) return;
                      setCreatingRetrieval(false);

                      if (submitResponse && submitResponse.status) {
                        setRetrievalModalOpen(false);
                        setSuccess(
                          `Successfully created retrieval request #${createResponse.requestNumber} for ${bioSampleIds.length} samples. Status: ${submitResponse.status}`,
                        );
                        setSelectedRetrievalSamples([]);
                        setRetrievalPurpose("");
                        setRetrievalDestinationDetails("");
                        setRetrievalModalError(null);
                      } else {
                        setRetrievalModalError(
                          submitResponse?.error ||
                            "Failed to submit retrieval request for approval.",
                        );
                      }
                    },
                  );
                } else {
                  setCreatingRetrieval(false);
                  setRetrievalModalError(
                    createResponse?.error ||
                      "Failed to create retrieval request.",
                  );
                }
              },
            );
          }
        },
      );
    });
  }, [
    selectedRetrievalSamples,
    retrievalPurpose,
    retrievalDestinationDetails,
    entryId,
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
          if (!componentMounted.current) return;
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

      {/* Tabs for Samples, Box Layout, and External Lab Transfers */}
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
          <Tab>
            <FormattedMessage
              id="notebook.routing.tab.externalTransfers"
              defaultMessage="External Lab Transfers"
            />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            {/* Sample Grid - Exclude External Lab samples (they have their own tab) */}
            <div className="sample-grid-container">
              <SampleGrid
                gridId="sample-routing"
                samples={samples.filter(
                  (s) => s.destinationType !== "EXTERNAL_LAB",
                )}
                selectedIds={selectedSampleIds}
                onSelectionChange={setSelectedSampleIds}
                onStatusChange={handleStatusChange}
                statusFilter={statusFilter}
                onStatusFilterChange={setStatusFilter}
                showSelection={true}
                showPatient={true}
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
          <TabPanel>
            {/* External Lab Transfers Tab */}
            <div style={{ marginTop: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.routing.externalTransfers.title"
                  defaultMessage="Samples Transferred to External Labs"
                />
              </h5>
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                <FormattedMessage
                  id="notebook.routing.externalTransfers.description"
                  defaultMessage="View samples that have been routed to external laboratories, including biorepository transfers."
                />
              </p>

              {/* Retrieval Request Button */}
              <div style={{ marginBottom: "1rem" }}>
                <Button
                  kind="primary"
                  size="sm"
                  onClick={() => setRetrievalModalOpen(true)}
                  disabled={selectedRetrievalSamples.length === 0}
                >
                  <FormattedMessage
                    id="notebook.routing.requestRetrieval"
                    defaultMessage="Request Retrieval ({count})"
                    values={{ count: selectedRetrievalSamples.length }}
                  />
                </Button>
              </div>
              {(() => {
                const externalLabSamples = samples.filter(
                  (s) => s.destinationType === "EXTERNAL_LAB",
                );

                if (externalLabSamples.length === 0) {
                  return (
                    <div
                      style={{
                        padding: "2rem",
                        textAlign: "center",
                        color: "#8d8d8d",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.routing.externalTransfers.empty"
                        defaultMessage="No samples have been transferred to external labs yet."
                      />
                    </div>
                  );
                }

                return (
                  <SampleGrid
                    gridId="external-lab-transfers"
                    samples={externalLabSamples}
                    selectedIds={selectedRetrievalSamples}
                    onSelectionChange={setSelectedRetrievalSamples}
                    showSelection={true}
                    showPatient={true}
                    loading={loading}
                    additionalColumns={[
                      {
                        key: "externalLab",
                        header: "External Lab",
                        render: (value, sample) => {
                          // For now, hardcoded to Biorepository (only supported external lab)
                          return (
                            <Tag type="purple">
                              Biorepository (Long-term Storage)
                            </Tag>
                          );
                        },
                      },
                      {
                        key: "transferStatus",
                        header: "Transfer Status",
                        render: (value, sample) => {
                          // For biorepository, we could check transfer status
                          // For now, show as "Transferred"
                          return <Tag type="green">Transferred</Tag>;
                        },
                      },
                    ]}
                  />
                );
              })()}

              {/* Retrieved Samples Section */}
              <div
                style={{
                  marginTop: "2rem",
                  paddingTop: "2rem",
                  borderTop: "1px solid #e0e0e0",
                }}
              >
                <h5 style={{ marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="notebook.routing.retrieved.title"
                    defaultMessage="Retrieved Samples from Biorepository"
                  />
                </h5>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    marginBottom: "1rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.routing.retrieved.description"
                    defaultMessage="Samples that have been retrieved from biorepository storage. Select samples and click 'Return to Routing Queue' in the batch actions toolbar to make them available for re-routing."
                  />
                </p>

                {loadingRetrieved ? (
                  <div style={{ padding: "2rem", textAlign: "center" }}>
                    <FormattedMessage
                      id="notebook.routing.retrieved.loading"
                      defaultMessage="Loading retrieved samples..."
                    />
                  </div>
                ) : retrievedSamples.length === 0 ? (
                  <div
                    style={{
                      padding: "2rem",
                      textAlign: "center",
                      color: "#8d8d8d",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.routing.retrieved.empty"
                      defaultMessage="No samples have been retrieved from biorepository yet."
                    />
                  </div>
                ) : (
                  <DataTable
                    rows={retrievedSamples
                      .filter((item) => {
                        // Filter out samples that have been returned to routing queue
                        // Only show samples that still have destinationType = "EXTERNAL_LAB"
                        const sampleId = String(item.sampleItemId || item.id);
                        const currentSample = samples.find(
                          (s) => s.id === sampleId,
                        );
                        return (
                          currentSample?.destinationType === "EXTERNAL_LAB"
                        );
                      })
                      .map((item, idx) => ({
                        id: String(item.sampleItemId || item.id || idx),
                        barcode: item.externalId || "-",
                        sampleType: item.sampleType || "-",
                        requestNumber: item.requestNumber || "-",
                        purpose: item.requestPurpose || "-",
                        retrievedDate: item.retrievedTimestamp
                          ? new Date(
                              item.retrievedTimestamp,
                            ).toLocaleDateString()
                          : item.requestedDate
                            ? new Date(item.requestedDate).toLocaleDateString()
                            : "-",
                        status: item.status || "PENDING",
                      }))}
                    headers={[
                      {
                        key: "barcode",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.externalId",
                          defaultMessage: "External ID",
                        }),
                      },
                      {
                        key: "sampleType",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.sampleType",
                          defaultMessage: "Sample Type",
                        }),
                      },
                      {
                        key: "requestNumber",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.requestNumber",
                          defaultMessage: "Request #",
                        }),
                      },
                      {
                        key: "purpose",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.purpose",
                          defaultMessage: "Retrieval Purpose",
                        }),
                      },
                      {
                        key: "retrievedDate",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.date",
                          defaultMessage: "Retrieved Date",
                        }),
                      },
                      {
                        key: "status",
                        header: intl.formatMessage({
                          id: "notebook.routing.retrieved.status",
                          defaultMessage: "Status",
                        }),
                      },
                    ]}
                  >
                    {({
                      rows,
                      headers,
                      getHeaderProps,
                      getTableProps,
                      getSelectionProps,
                      getBatchActionProps,
                      selectedRows,
                    }) => {
                      // Update selected sample IDs when selection changes
                      const currentSelectedIds = selectedRows.map((row) =>
                        String(row.id),
                      );
                      if (
                        JSON.stringify(currentSelectedIds) !==
                        JSON.stringify(selectedRetrievedSampleIds)
                      ) {
                        setSelectedRetrievedSampleIds(currentSelectedIds);
                      }

                      return (
                        <TableContainer>
                          <TableBatchActions {...getBatchActionProps()}>
                            <TableBatchAction
                              renderIcon={Renew}
                              iconDescription="Return to Routing Queue"
                              onClick={handleReturnToLab}
                              disabled={returningToLab}
                            >
                              <FormattedMessage
                                id="notebook.routing.retrieved.returnToQueue"
                                defaultMessage="Return to Routing Queue"
                              />
                            </TableBatchAction>
                          </TableBatchActions>
                          <Table {...getTableProps()} size="md">
                            <TableHead>
                              <TableRow>
                                <TableSelectAll
                                  {...getSelectionProps()}
                                  disabled={returningToLab}
                                />
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
                              {rows.map((row) => (
                                <TableRow key={row.id}>
                                  <TableSelectRow
                                    {...getSelectionProps({ row })}
                                    disabled={returningToLab}
                                  />
                                  {row.cells.map((cell) => {
                                    if (cell.info.header === "status") {
                                      const statusColors = {
                                        PENDING: "gray",
                                        RETRIEVED: "blue",
                                        IN_ANALYSIS: "purple",
                                        RETURNED: "green",
                                        CONSUMED: "teal",
                                      };
                                      return (
                                        <TableCell key={cell.id}>
                                          <Tag
                                            type={
                                              statusColors[cell.value] || "gray"
                                            }
                                          >
                                            {cell.value}
                                          </Tag>
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
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      );
                    }}
                  </DataTable>
                )}
              </div>
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
          <div>
            {externalLabName === "Biorepository" && (
              <InlineNotification
                kind="info"
                title="Biorepository Transfer"
                subtitle="Routing to Biorepository will create a transfer request that must be reviewed and accepted by biorepository staff. Samples will be assigned biosafety levels, ethics approval references, and long-term storage locations upon acceptance."
                hideCloseButton
                lowContrast
                style={{ marginBottom: "1rem" }}
              />
            )}
            <Select
              id="external-lab-name"
              labelText={intl.formatMessage({
                id: "notebook.routing.modal.labName",
                defaultMessage: "External Lab *",
              })}
              value={externalLabName}
              onChange={(e) => setExternalLabName(e.target.value)}
              style={{ marginBottom: "1rem" }}
            >
              <SelectItem value="" text="Select destination lab..." />
              <SelectItem
                value="Biorepository"
                text="Biorepository (Long-term Storage)"
              />
            </Select>
            {externalLabName !== "Biorepository" && (
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
            )}
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

            <p
              style={{
                marginTop: "0.5rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              <FormattedMessage
                id="notebook.routing.modal.storageBoxHelp"
                defaultMessage="Select a box and click Auto-Populate to preview assignments before routing."
              />
            </p>
          </div>
        )}
      </Modal>

      {/* Retrieval Request Modal */}
      <Modal
        open={retrievalModalOpen}
        size="md"
        modalHeading={intl.formatMessage({
          id: "notebook.routing.retrieval.modal.title",
          defaultMessage: "Request Sample Retrieval from Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.routing.retrieval.modal.create",
          defaultMessage: "Create Retrieval Request",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.routing.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setRetrievalModalOpen(false);
          setRetrievalPurpose("");
          setRetrievalDestinationDetails("");
          setRetrievalModalError(null);
        }}
        onRequestSubmit={handleCreateRetrievalRequest}
        primaryButtonDisabled={creatingRetrieval || !retrievalPurpose.trim()}
      >
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="info"
            title="Biorepository Retrieval Request"
            subtitle={intl.formatMessage(
              {
                id: "notebook.routing.retrieval.modal.info",
                defaultMessage:
                  "You are requesting to retrieve {count} sample(s) from the Biorepository. This request will be reviewed and approved by biorepository staff before samples can be released.",
              },
              { count: selectedRetrievalSamples.length },
            )}
            hideCloseButton
            lowContrast
            style={{ marginBottom: "1rem" }}
          />

          {retrievalModalError && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "notebook.routing.retrieval.modal.error.title",
                defaultMessage: "Unable to Create Retrieval Request",
              })}
              subtitle={retrievalModalError}
              onCloseButtonClick={() => setRetrievalModalError(null)}
              lowContrast
              style={{ marginBottom: "1rem" }}
            />
          )}

          <TextInput
            id="retrieval-purpose"
            labelText={intl.formatMessage({
              id: "notebook.routing.retrieval.purpose",
              defaultMessage: "Purpose of Retrieval *",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.routing.retrieval.purposePlaceholder",
              defaultMessage:
                "e.g., Additional analysis, re-testing, quality control",
            })}
            value={retrievalPurpose}
            onChange={(e) => setRetrievalPurpose(e.target.value)}
            style={{ marginBottom: "1rem" }}
            required
          />

          <TextInput
            id="retrieval-destination"
            labelText={intl.formatMessage({
              id: "notebook.routing.retrieval.destination",
              defaultMessage: "Destination Details (Optional)",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.routing.retrieval.destinationPlaceholder",
              defaultMessage: "Where will samples be used?",
            })}
            value={retrievalDestinationDetails}
            onChange={(e) => setRetrievalDestinationDetails(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  );
}

export default MedLabSampleRoutingPage;
