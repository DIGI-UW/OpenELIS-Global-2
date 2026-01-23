import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Modal,
  Tag,
  Loading,
  Checkbox,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import {
  TrashCan,
  Archive,
  Renew,
  Warning,
  Locked,
  DeliveryTruck,
  DocumentExport,
  Checkmark,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologySampleRetrievalDisposalPage - Page 7: Sample Retrieval, Archival & Disposal
 *
 * Final stage of the Bacteriology workflow. Manages:
 * 1. Sample Retrieval - for shipping, retesting, research, QA
 * 2. Biorepository Transfer - long-term archival with chain of custody
 * 3. Sample Disposal - with criteria, methods, and documentation
 * 4. Record Archival - documentation retention
 *
 * Samples are routed here from the Post-Analysis Storage page (Page 6).
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID for fetching storage routing data
 */
function BacteriologySampleRetrievalDisposalPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Modal states
  const [showRetrievalModal, setShowRetrievalModal] = useState(false);
  const [showBiorepositoryModal, setShowBiorepositoryModal] = useState(false);
  const [showDisposalModal, setShowDisposalModal] = useState(false);

  // Retrieval form data
  const [retrievalData, setRetrievalData] = useState({
    retrievalPurpose: "",
    retrievalRequestedBy: "",
    retrievalRequestDate: new Date().toISOString().split("T")[0],
    retrievalApprovalRequired: false,
    retrievalApprovedBy: "",
    retrievalApprovalDate: "",
    retrievalDate: new Date().toISOString().split("T")[0],
    retrievalTime: "",
    retrievedBy: "",
    retrievalDestination: "",
    retrievalNotes: "",
  });

  // Biorepository transfer form data
  const [biorepositoryData, setBiorepositoryData] = useState({
    biorepositoryName: "",
    transferDate: new Date().toISOString().split("T")[0],
    chainOfCustodyNumber: "",
    transferDocuments: [],
    transferConditions: "",
    transferredBy: "",
    receivedBy: "",
    biorepositoryAccessionNumber: "",
    // New storage location for biorepository transfer
    newStorageLocation: null,
  });

  // Disposal form data
  const [disposalData, setDisposalData] = useState({
    disposalCriteria: "",
    disposalMethod: "",
    disposalDate: new Date().toISOString().split("T")[0],
    disposalTime: "",
    quantityDisposed: "",
    disposedBy: "",
    supervisorSignOff: "",
    biosafetyOfficerSignOff: "",
    disposalCertificateNumber: "",
    disposalNotes: "",
    confirmDisposal: false,
  });

  // Summary counts
  const [summary, setSummary] = useState({
    total: 0,
    active: 0,
    retrieved: 0,
    transferred: 0,
    disposed: 0,
    archived: 0,
  });

  // Retrieval purpose options
  const retrievalPurposes = [
    { value: "SHIPPING_REFERENCE_LAB", label: "Shipping to Reference Lab" },
    { value: "SHIPPING_COLLABORATOR", label: "Shipping to Collaborator" },
    { value: "RETESTING_CONFIRMATION", label: "Retesting - Confirmation" },
    {
      value: "RETESTING_INCONCLUSIVE",
      label: "Retesting - Inconclusive Results",
    },
    { value: "RESEARCH", label: "Research" },
    { value: "QUALITY_ASSURANCE", label: "Quality Assurance" },
    { value: "PROFICIENCY_TESTING", label: "Proficiency Testing" },
    { value: "OTHER", label: "Other" },
  ];

  // Disposal criteria options (bacteriology-specific)
  const disposalCriteria = [
    { value: "EXPIRED", label: "Expired - Beyond retention period" },
    { value: "LEAKAGE", label: "Leakage - Container compromised" },
    { value: "MISLABELED", label: "Mislabeled - Cannot be identified" },
    { value: "STORAGE_SHORTAGE", label: "Storage Shortage" },
    { value: "EXHAUSTED", label: "Exhausted - Sample fully consumed" },
    { value: "CONTAMINATION", label: "Contamination" },
    { value: "BIOHAZARD", label: "Biohazard - Safety concerns" },
    { value: "NO_GROWTH", label: "No Growth - Non-viable culture" },
    { value: "STUDY_COMPLETE", label: "Study Complete" },
    { value: "OTHER", label: "Other" },
  ];

  // Disposal methods (bacteriology-specific)
  const disposalMethods = [
    {
      value: "AUTOCLAVE",
      label: "Autoclave - Standard for bacterial cultures",
    },
    { value: "INCINERATION", label: "Incineration - Biohazard materials" },
    { value: "CHEMICAL_TREATMENT", label: "Chemical Treatment - Disinfection" },
    { value: "CERTIFIED_WASTE", label: "Certified Waste Disposal" },
    { value: "OTHER", label: "Other" },
  ];

  // Transfer conditions
  const transferConditions = [
    { value: "DRY_ICE", label: "Dry Ice (-78°C)" },
    { value: "COLD_PACK", label: "Cold Pack (2-8°C)" },
    { value: "AMBIENT", label: "Ambient Temperature" },
    { value: "LIQUID_NITROGEN", label: "Liquid Nitrogen (-196°C)" },
    { value: "FROZEN_MINUS80", label: "Frozen (-80°C)" },
    { value: "FROZEN_MINUS20", label: "Frozen (-20°C)" },
  ];

  // Transfer documents
  const transferDocumentOptions = [
    { value: "SAMPLE_MANIFEST", label: "Sample Manifest" },
    { value: "CHAIN_OF_CUSTODY", label: "Chain of Custody Form" },
    { value: "TEST_RESULTS", label: "Test Results" },
    { value: "ISOLATION_RECORDS", label: "Isolation Records" },
    { value: "IDENTIFICATION_REPORT", label: "Identification Report" },
    { value: "AST_RESULTS", label: "AST Results" },
    { value: "MOLECULAR_DATA", label: "Molecular Data" },
  ];

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples from Post-Analysis Storage page (page 6) and this page
  // Also fetches routing data for storage location information
  const loadSamples = useCallback(() => {
    if (!hasRealPageId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    let pageSamples = [];
    let postAnalysisSamples = [];
    let routingData = [];
    let loadCount = 0;
    const expectedLoads = 3; // page samples, post-analysis samples, routing data

    const finishProcessing = () => {
      loadCount++;
      if (loadCount < expectedLoads) return;

      if (componentMounted.current) {
        // Build routing map from storage routing data
        const routingMap = {};
        routingData.forEach((routing) => {
          if (routing.destinationType === "STORAGE" && routing.sampleItemId) {
            routingMap[String(routing.sampleItemId)] = {
              boxId: routing.boxId,
              boxName: routing.boxName,
              wellCoordinate: routing.wellCoordinate,
              routedAt: routing.routedAt,
              hasRouting: true,
            };
          }
        });

        // Build storage map from Post-Analysis page
        const storageMap = {};
        postAnalysisSamples.forEach((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const routing = routingMap[sampleId];

          // Get storage box/well from sample data or routing
          const storageBox =
            sample.data?.storageBox || routing?.boxName || null;
          const storageWell =
            sample.data?.storageWell ||
            sample.data?.wellCoordinate ||
            routing?.wellCoordinate ||
            null;

          // Build storage location string
          let storageLocation = sample.data?.storageLocation || null;
          if (!storageLocation && (storageBox || storageWell)) {
            storageLocation = storageBox
              ? `${storageBox}${storageWell ? ` - ${storageWell}` : ""}`
              : storageWell;
          }
          if (!storageLocation && routing) {
            storageLocation = routing.boxName
              ? `${routing.boxName}${routing.wellCoordinate ? ` - ${routing.wellCoordinate}` : ""}`
              : routing.wellCoordinate;
          }

          storageMap[sampleId] = {
            aliquotType: sample.data?.aliquotType,
            storageCondition: sample.data?.storageCondition,
            storageMedia: sample.data?.storageMedia,
            containerType: sample.data?.containerType,
            organismIdentified: sample.data?.organismIdentified,
            storagePath: storageLocation,
            storageBox: storageBox,
            storageWell: storageWell,
            storageLocation: storageLocation,
            hasRouting: routing?.hasRouting || false,
          };
        });

        const transformedSamples = pageSamples.map((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const storageData = storageMap[sampleId];
          const routing = routingMap[sampleId];

          // Build storagePath from available data (priority: sample data > storage map > routing)
          let storagePath =
            sample.data?.storagePath ||
            sample.data?.storageLocation ||
            storageData?.storagePath ||
            storageData?.storageLocation;

          // If no storagePath but have routing, build from routing
          if (!storagePath && routing) {
            storagePath = routing.boxName
              ? `${routing.boxName}${routing.wellCoordinate ? ` - ${routing.wellCoordinate}` : ""}`
              : routing.wellCoordinate;
          }

          // If no storagePath but have box/well, build it
          if (!storagePath) {
            const box =
              sample.data?.storageBox || storageData?.storageBox || null;
            const well =
              sample.data?.storageWell || storageData?.storageWell || null;
            if (box) {
              storagePath = well ? `${box} - ${well}` : box;
            }
          }

          return {
            id: sampleId,
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            collectionDate: sample.collectionDate,
            status: sample.pageStatus || "PENDING",
            // From Post-Analysis Storage or Routing
            aliquotType: sample.data?.aliquotType || storageData?.aliquotType,
            storageCondition:
              sample.data?.storageCondition || storageData?.storageCondition,
            storageMedia:
              sample.data?.storageMedia || storageData?.storageMedia,
            organismIdentified:
              sample.data?.organismIdentified ||
              storageData?.organismIdentified,
            storagePath: storagePath,
            storageBox:
              sample.data?.storageBox ||
              storageData?.storageBox ||
              routing?.boxName,
            storageWell:
              sample.data?.storageWell ||
              storageData?.storageWell ||
              routing?.wellCoordinate,
            storageLocation:
              sample.data?.storageLocation ||
              storageData?.storageLocation ||
              storagePath,
            hasStorageAssignment:
              storageData?.hasRouting || routing?.hasRouting || !!storagePath,
            // Retrieval data
            retrievalPurpose: sample.data?.retrievalPurpose || "",
            retrievalDate: sample.data?.retrievalDate || "",
            retrievedBy: sample.data?.retrievedBy || "",
            retrievalDestination: sample.data?.retrievalDestination || "",
            // Biorepository data
            biorepositoryName: sample.data?.biorepositoryName || "",
            transferDate: sample.data?.transferDate || "",
            biorepositoryAccessionNumber:
              sample.data?.biorepositoryAccessionNumber || "",
            newStoragePath: sample.data?.newStoragePath || "",
            // Disposal data
            disposalCriteria: sample.data?.disposalCriteria || "",
            disposalMethod: sample.data?.disposalMethod || "",
            disposalDate: sample.data?.disposalDate || "",
            disposedBy: sample.data?.disposedBy || "",
            // Status flags
            isRetrieved: sample.data?.isRetrieved || false,
            isTransferred: sample.data?.isTransferred || false,
            isDisposed: sample.data?.isDisposed || false,
            isArchived: sample.data?.isArchived || false,
          };
        });
        setSamples(transformedSamples);
        calculateSummary(transformedSamples);
        setLoading(false);
      }
    };

    // Load this page's samples
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          pageSamples = response;
        }
        finishProcessing();
      },
    );

    // Fetch notebook pages to find Post-Analysis Storage page (order 6) and load routing data
    if (notebookId) {
      // Load routing data for storage assignments
      getFromOpenElisServer(
        `/rest/notebook/${notebookId}/routing?destinationType=STORAGE`,
        (response) => {
          if (response && Array.isArray(response)) {
            routingData = response;
          }
          finishProcessing();
        },
      );

      // Load Post-Analysis page samples
      getFromOpenElisServer(
        `/rest/notebook/view/${notebookId}`,
        (nbResponse) => {
          if (nbResponse && nbResponse.pages) {
            const postAnalysisPage = nbResponse.pages.find(
              (p) => (p.pageOrder || p.order) === 6,
            );
            if (postAnalysisPage && postAnalysisPage.id) {
              getFromOpenElisServer(
                `/rest/notebook/page/${postAnalysisPage.id}/samples`,
                (storageResponse) => {
                  if (storageResponse && Array.isArray(storageResponse)) {
                    postAnalysisSamples = storageResponse;
                  }
                  finishProcessing();
                },
              );
            } else {
              finishProcessing();
            }
          } else {
            finishProcessing();
          }
        },
      );
    } else {
      // No notebookId, call finishProcessing twice to match expectedLoads
      finishProcessing();
      finishProcessing();
    }
  }, [pageData?.id, notebookId, hasRealPageId]);

  // Calculate summary
  const calculateSummary = (sampleData) => {
    const total = sampleData.length;
    const retrieved = sampleData.filter((s) => s.isRetrieved).length;
    const transferred = sampleData.filter((s) => s.isTransferred).length;
    const disposed = sampleData.filter((s) => s.isDisposed).length;
    const archived = sampleData.filter((s) => s.isArchived).length;
    const active = total - disposed - transferred;

    setSummary({
      total,
      active: active > 0 ? active : 0,
      retrieved,
      transferred,
      disposed,
      archived,
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples]);

  // Handle sample retrieval - also removes samples from storage location
  const handleApplyRetrievalData = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.retrieval.noSamplesSelected",
          defaultMessage: "Please select samples to retrieve",
        }),
      );
      return;
    }

    if (!retrievalData.retrievalPurpose) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.retrieval.purposeRequired",
          defaultMessage: "Retrieval purpose is required",
        }),
      );
      return;
    }

    if (!retrievalData.retrievedBy) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.retrieval.retrievedByRequired",
          defaultMessage: "Retrieved by field is required",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowRetrievalModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // First, remove samples from storage location by calling the dispose API
    // (disposal API clears storage assignment without marking as disposed when used for retrieval)
    const removeFromStoragePromises = selectedIds.map((sampleId) => {
      return new Promise((resolve) => {
        // Get the sample to check if it has a storage location
        const sample = samples.find((s) => String(s.id) === String(sampleId));
        if (sample && sample.storagePath) {
          // Use move API to clear storage (move to null location tracked as retrieval)
          postToOpenElisServer(
            "/rest/storage/sample-items/move",
            JSON.stringify({
              sampleItemId: sampleId,
              locationId: null,
              locationType: null,
              reason: `Retrieved for ${retrievalData.retrievalPurpose}: ${retrievalData.retrievalDestination || "N/A"}`,
            }),
            () => resolve(),
          );
        } else {
          resolve();
        }
      });
    });

    // Wait for storage removal, then apply retrieval data
    Promise.all(removeFromStoragePromises).then(() => {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: numericIds,
          data: {
            ...retrievalData,
            isRetrieved: true,
            storageCleared: true,
            storageClearedDate: new Date().toISOString(),
          },
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.bacteriology.retrieval.success",
                    defaultMessage:
                      "Retrieved {count} sample(s) successfully. Samples removed from storage.",
                  },
                  { count: selectedIds.length },
                ),
              );
              setShowRetrievalModal(false);
              setSelectedIds([]);
              resetRetrievalForm();
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            } else {
              setError(response?.error || "Failed to apply retrieval data");
            }
          }
        },
      );
    });
  };

  // Handle biorepository transfer - moves samples to new storage location
  const handleApplyBiorepositoryData = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.biorepository.noSamplesSelected",
          defaultMessage: "Please select samples to transfer",
        }),
      );
      return;
    }

    if (!biorepositoryData.biorepositoryName) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.biorepository.nameRequired",
          defaultMessage: "Biorepository name is required",
        }),
      );
      return;
    }

    if (!biorepositoryData.chainOfCustodyNumber) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.biorepository.chainRequired",
          defaultMessage: "Chain of custody number is required",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowBiorepositoryModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // If a new storage location is selected, move samples to that location first
    const moveToStoragePromises = selectedIds.map((sampleId) => {
      return new Promise((resolve) => {
        if (biorepositoryData.newStorageLocation) {
          const loc = biorepositoryData.newStorageLocation;
          postToOpenElisServer(
            "/rest/storage/sample-items/move",
            JSON.stringify({
              sampleItemId: sampleId,
              locationId: loc.id,
              locationType: loc.type,
              positionCoordinate: loc.positionCoordinate || null,
              reason: `Transferred to biorepository: ${biorepositoryData.biorepositoryName}`,
              notes: `Chain of custody: ${biorepositoryData.chainOfCustodyNumber}`,
            }),
            () => resolve(),
          );
        } else {
          resolve();
        }
      });
    });

    // Wait for storage moves, then apply biorepository data
    Promise.all(moveToStoragePromises).then(() => {
      // Build storage path for display
      const newStoragePath = biorepositoryData.newStorageLocation
        ? biorepositoryData.newStorageLocation.path ||
          biorepositoryData.newStorageLocation.label
        : null;

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: numericIds,
          data: {
            ...biorepositoryData,
            isTransferred: true,
            transferToBiorepository: true,
            newStoragePath: newStoragePath,
            transferredToStorageDate: new Date().toISOString(),
          },
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              // Mark samples as COMPLETED
              postToOpenElisServer(
                `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                JSON.stringify({
                  sampleIds: numericIds,
                  status: "COMPLETED",
                }),
                () => {
                  setSuccess(
                    intl.formatMessage(
                      {
                        id: "notebook.bacteriology.biorepository.success",
                        defaultMessage:
                          "Transferred {count} sample(s) to biorepository{location}.",
                      },
                      {
                        count: selectedIds.length,
                        location: newStoragePath ? ` (${newStoragePath})` : "",
                      },
                    ),
                  );
                  setShowBiorepositoryModal(false);
                  setSelectedIds([]);
                  resetBiorepositoryForm();
                  loadSamples();
                  if (onProgressUpdate) {
                    onProgressUpdate();
                  }
                },
              );
            } else {
              setError(response?.error || "Failed to apply transfer data");
            }
          }
        },
      );
    });
  };

  // Handle disposal - removes samples from storage and marks as disposed
  const handleApplyDisposalData = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to dispose",
        }),
      );
      return;
    }

    if (!disposalData.disposalCriteria || !disposalData.disposalMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.disposal.criteriaMethodRequired",
          defaultMessage: "Disposal criteria and method are required",
        }),
      );
      return;
    }

    if (!disposalData.supervisorSignOff) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.disposal.supervisorRequired",
          defaultMessage: "Supervisor sign-off is required",
        }),
      );
      return;
    }

    if (!disposalData.confirmDisposal) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.disposal.confirmRequired",
          defaultMessage: "Please confirm the disposal action",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowDisposalModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // First, remove samples from storage location using the dispose API
    // This API clears storage assignment and marks sample as disposed
    const disposeFromStoragePromises = selectedIds.map((sampleId) => {
      return new Promise((resolve) => {
        const sample = samples.find((s) => String(s.id) === String(sampleId));
        // Call the storage disposal API to clear storage and mark sample as disposed
        postToOpenElisServer(
          "/rest/storage/sample-items/dispose",
          JSON.stringify({
            sampleItemId: sampleId,
            reason: disposalData.disposalCriteria,
            method: disposalData.disposalMethod,
            notes: `${disposalData.disposalNotes || ""} | Supervisor: ${disposalData.supervisorSignOff}${sample?.storagePath ? ` | Previous storage: ${sample.storagePath}` : ""}`,
          }),
          () => resolve(),
        );
      });
    });

    // Wait for storage disposal, then apply notebook disposal data
    Promise.all(disposeFromStoragePromises).then(() => {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: numericIds,
          data: {
            ...disposalData,
            isDisposed: true,
            isArchived: true,
            storageCleared: true,
            storageClearedDate: new Date().toISOString(),
          },
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              // Mark samples as COMPLETED
              postToOpenElisServer(
                `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                JSON.stringify({
                  sampleIds: numericIds,
                  status: "COMPLETED",
                }),
                () => {
                  setSuccess(
                    intl.formatMessage(
                      {
                        id: "notebook.bacteriology.disposal.success",
                        defaultMessage:
                          "Disposed {count} sample(s). Samples removed from storage, records locked and archived.",
                      },
                      { count: selectedIds.length },
                    ),
                  );
                  setShowDisposalModal(false);
                  setSelectedIds([]);
                  resetDisposalForm();
                  loadSamples();
                  if (onProgressUpdate) {
                    onProgressUpdate();
                  }
                },
              );
            } else {
              setError(response?.error || "Failed to apply disposal data");
            }
          }
        },
      );
    });
  };

  // Reset forms
  const resetRetrievalForm = () => {
    setRetrievalData({
      retrievalPurpose: "",
      retrievalRequestedBy: "",
      retrievalRequestDate: new Date().toISOString().split("T")[0],
      retrievalApprovalRequired: false,
      retrievalApprovedBy: "",
      retrievalApprovalDate: "",
      retrievalDate: new Date().toISOString().split("T")[0],
      retrievalTime: "",
      retrievedBy: "",
      retrievalDestination: "",
      retrievalNotes: "",
    });
  };

  const resetBiorepositoryForm = () => {
    setBiorepositoryData({
      biorepositoryName: "",
      transferDate: new Date().toISOString().split("T")[0],
      chainOfCustodyNumber: "",
      transferDocuments: [],
      transferConditions: "",
      transferredBy: "",
      receivedBy: "",
      biorepositoryAccessionNumber: "",
      newStorageLocation: null,
    });
  };

  const resetDisposalForm = () => {
    setDisposalData({
      disposalCriteria: "",
      disposalMethod: "",
      disposalDate: new Date().toISOString().split("T")[0],
      disposalTime: "",
      quantityDisposed: "",
      disposedBy: "",
      supervisorSignOff: "",
      biosafetyOfficerSignOff: "",
      disposalCertificateNumber: "",
      disposalNotes: "",
      confirmDisposal: false,
    });
  };

  // Render status tag
  const renderStatusTag = (sample) => {
    if (sample.isDisposed) {
      return (
        <Tag type="red" renderIcon={TrashCan} size="sm">
          Disposed
        </Tag>
      );
    }
    if (sample.isTransferred) {
      return (
        <Tag type="cyan" renderIcon={Archive} size="sm">
          Transferred
        </Tag>
      );
    }
    if (sample.isRetrieved) {
      return (
        <Tag type="teal" renderIcon={DeliveryTruck} size="sm">
          Retrieved
        </Tag>
      );
    }
    if (sample.isArchived) {
      return (
        <Tag type="purple" renderIcon={Locked} size="sm">
          Archived
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        Active
      </Tag>
    );
  };

  // Render aliquot type
  const renderAliquotType = (sample) => {
    if (!sample.aliquotType) return <span style={{ color: "#8d8d8d" }}>-</span>;

    const typeLabels = {
      ISOLATE: "Isolate",
      DNA: "DNA",
      RNA: "RNA",
      PRIMARY_SAMPLE: "Primary",
      GLYCEROL_STOCK: "Glycerol Stock",
      LYOPHILIZED: "Lyophilized",
    };
    return typeLabels[sample.aliquotType] || sample.aliquotType;
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="bacteriology-retrieval-disposal-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.retrievalDisposal.title"
            defaultMessage="Sample Retrieval, Archival &amp; Disposal"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.retrievalDisposal.description"
            defaultMessage="Final stage of sample lifecycle. Retrieve samples for shipping/retesting, transfer to biorepository, or dispose with full documentation."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {/* Summary Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.retrievalDisposal.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{summary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.retrievalDisposal.active"
                  defaultMessage="Active"
                />
              </span>
              <span className="progress-value">{summary.active}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.retrievalDisposal.retrieved"
                  defaultMessage="Retrieved"
                />
              </span>
              <span className="progress-value">{summary.retrieved}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.retrievalDisposal.transferred"
                  defaultMessage="Transferred"
                />
              </span>
              <span className="progress-value">{summary.transferred}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.retrievalDisposal.disposed"
                  defaultMessage="Disposed"
                />
              </span>
              <span className="progress-value">{summary.disposed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Tabs for different operations */}
      <Tabs>
        <TabList aria-label="Sample operations">
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.retrievalDisposal.tab.retrieval"
              defaultMessage="Sample Retrieval"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.retrievalDisposal.tab.biorepository"
              defaultMessage="Biorepository Transfer"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.retrievalDisposal.tab.disposal"
              defaultMessage="Disposal"
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Tab 1: Sample Retrieval */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.retrieval.title"
                  defaultMessage="Sample Retrieval"
                />
              </h5>
              <p className="page-description" style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.retrieval.description"
                  defaultMessage="Retrieve samples for shipping to reference labs, retesting, research, or quality assurance purposes."
                />
              </p>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={DeliveryTruck}
                  onClick={() => setShowRetrievalModal(true)}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.retrieval.retrieveSelected"
                    defaultMessage="Retrieve Selected ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.retrieval.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-retrieval"
                  samples={samples.filter(
                    (s) => !s.isDisposed && !s.isTransferred,
                  )}
                  selectedIds={selectedIds}
                  onSelectionChange={setSelectedIds}
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                  showSelection={true}
                  loading={loading}
                  additionalColumns={[
                    {
                      key: "status",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.retrieval.status",
                        defaultMessage: "Status",
                      }),
                      render: (value, row) => renderStatusTag(row),
                    },
                    {
                      key: "aliquotType",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.retrieval.aliquotType",
                        defaultMessage: "Aliquot Type",
                      }),
                      render: (value, row) => renderAliquotType(row),
                    },
                    {
                      key: "organismIdentified",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.retrieval.organism",
                        defaultMessage: "Organism",
                      }),
                      render: (value) => value || "-",
                    },
                    {
                      key: "storagePath",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.retrieval.storage",
                        defaultMessage: "Storage Location",
                      }),
                      render: (value, row) => {
                        // Check storagePath first (includes storageLocation mapping)
                        if (row.storagePath) {
                          return (
                            <span title={row.storagePath}>
                              {row.storagePath}
                            </span>
                          );
                        }
                        // Fallback to storageLocation from Post-Analysis page
                        if (row.storageLocation) {
                          return (
                            <span title={row.storageLocation}>
                              {row.storageLocation}
                            </span>
                          );
                        }
                        // Fallback to storageBox/storageWell
                        if (row.storageBox) {
                          const display = row.storageWell
                            ? `${row.storageBox} > ${row.storageWell}`
                            : row.storageBox;
                          return <span title={display}>{display}</span>;
                        }
                        return <span style={{ color: "#8d8d8d" }}>-</span>;
                      },
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* Tab 2: Biorepository Transfer */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.biorepository.title"
                  defaultMessage="Biorepository Transfer"
                />
              </h5>
              <p className="page-description" style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.biorepository.description"
                  defaultMessage="Transfer isolates and DNA to biorepository for long-term archival with chain of custody documentation."
                />
              </p>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="secondary"
                  size="sm"
                  renderIcon={Archive}
                  onClick={() => setShowBiorepositoryModal(true)}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.biorepository.transferSelected"
                    defaultMessage="Transfer to Biorepository ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.biorepository.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-biorepository"
                  samples={samples.filter((s) => !s.isDisposed)}
                  selectedIds={selectedIds}
                  onSelectionChange={setSelectedIds}
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                  showSelection={true}
                  loading={loading}
                  additionalColumns={[
                    {
                      key: "status",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.biorepository.status",
                        defaultMessage: "Status",
                      }),
                      render: (value, row) => renderStatusTag(row),
                    },
                    {
                      key: "aliquotType",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.biorepository.aliquotType",
                        defaultMessage: "Aliquot Type",
                      }),
                      render: (value, row) => renderAliquotType(row),
                    },
                    {
                      key: "organismIdentified",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.biorepository.organism",
                        defaultMessage: "Organism",
                      }),
                      render: (value) => value || "-",
                    },
                    {
                      key: "storagePath",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.biorepository.storage",
                        defaultMessage: "Current Storage",
                      }),
                      render: (value, row) => {
                        // Check storagePath first (includes storageLocation mapping)
                        if (row.storagePath) {
                          return (
                            <span title={row.storagePath}>
                              {row.storagePath}
                            </span>
                          );
                        }
                        // Fallback to storageLocation from Post-Analysis page
                        if (row.storageLocation) {
                          return (
                            <span title={row.storageLocation}>
                              {row.storageLocation}
                            </span>
                          );
                        }
                        // Fallback to storageBox/storageWell
                        if (row.storageBox) {
                          const display = row.storageWell
                            ? `${row.storageBox} > ${row.storageWell}`
                            : row.storageBox;
                          return <span title={display}>{display}</span>;
                        }
                        return <span style={{ color: "#8d8d8d" }}>-</span>;
                      },
                    },
                    {
                      key: "biorepositoryAccessionNumber",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.biorepository.accession",
                        defaultMessage: "Biorepository Accession",
                      }),
                      render: (value) => value || "-",
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* Tab 3: Disposal */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.disposal.title"
                  defaultMessage="Sample Disposal"
                />
              </h5>
              <p className="page-description" style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.disposal.description"
                  defaultMessage="Dispose samples using appropriate methods (autoclave, incineration, chemical treatment) with full documentation and supervisor sign-off."
                />
              </p>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="danger"
                  size="sm"
                  renderIcon={TrashCan}
                  onClick={() => setShowDisposalModal(true)}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.disposal.disposeSelected"
                    defaultMessage="Dispose Selected ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.disposal.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-disposal"
                  samples={samples}
                  selectedIds={selectedIds}
                  onSelectionChange={setSelectedIds}
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                  showSelection={true}
                  loading={loading}
                  additionalColumns={[
                    {
                      key: "status",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.disposal.status",
                        defaultMessage: "Status",
                      }),
                      render: (value, row) => renderStatusTag(row),
                    },
                    {
                      key: "aliquotType",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.disposal.aliquotType",
                        defaultMessage: "Aliquot Type",
                      }),
                      render: (value, row) => renderAliquotType(row),
                    },
                    {
                      key: "storagePath",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.disposal.storage",
                        defaultMessage: "Storage Location",
                      }),
                      render: (value, row) => {
                        if (row.isDisposed) {
                          return (
                            <span
                              style={{ color: "#8d8d8d", fontStyle: "italic" }}
                            >
                              Removed
                            </span>
                          );
                        }
                        // Check storagePath first (includes storageLocation mapping)
                        if (row.storagePath) {
                          return (
                            <span title={row.storagePath}>
                              {row.storagePath}
                            </span>
                          );
                        }
                        // Fallback to storageLocation from Post-Analysis page
                        if (row.storageLocation) {
                          return (
                            <span title={row.storageLocation}>
                              {row.storageLocation}
                            </span>
                          );
                        }
                        // Fallback to storageBox/storageWell
                        if (row.storageBox) {
                          const display = row.storageWell
                            ? `${row.storageBox} > ${row.storageWell}`
                            : row.storageBox;
                          return <span title={display}>{display}</span>;
                        }
                        return <span style={{ color: "#8d8d8d" }}>-</span>;
                      },
                    },
                    {
                      key: "disposalCriteria",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.disposal.criteria",
                        defaultMessage: "Disposal Reason",
                      }),
                      render: (value) => {
                        if (!value) return "-";
                        const criterion = disposalCriteria.find(
                          (c) => c.value === value,
                        );
                        return criterion
                          ? criterion.label.split(" - ")[0]
                          : value;
                      },
                    },
                    {
                      key: "disposalDate",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.disposal.date",
                        defaultMessage: "Disposal Date",
                      }),
                      render: (value) => value || "-",
                    },
                  ]}
                />
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
              id="notebook.page.bacteriology.retrievalDisposal.empty"
              defaultMessage="No samples available. Complete Post-Analysis Storage first."
            />
          </p>
        </div>
      )}

      {/* Retrieval Modal */}
      <Modal
        open={showRetrievalModal}
        onRequestClose={() => setShowRetrievalModal(false)}
        onRequestSubmit={handleApplyRetrievalData}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.retrieval.modalTitle",
          defaultMessage: "Retrieve Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.retrieval.confirm",
          defaultMessage: "Confirm Retrieval",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.retrieval.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage(
              {
                id: "notebook.bacteriology.retrieval.info",
                defaultMessage: "Retrieving {count} sample(s)",
              },
              { count: selectedIds.length },
            )}
            hideCloseButton
            lowContrast
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="retrieval-purpose"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.retrieval.purpose",
                  defaultMessage: "Retrieval Purpose *",
                })}
                value={retrievalData.retrievalPurpose}
                onChange={(e) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievalPurpose: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select purpose..." />
                {retrievalPurposes.map((purpose) => (
                  <SelectItem
                    key={purpose.value}
                    value={purpose.value}
                    text={purpose.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="retrieval-destination"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.retrieval.destination",
                  defaultMessage: "Destination",
                })}
                value={retrievalData.retrievalDestination}
                onChange={(e) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievalDestination: e.target.value,
                  })
                }
                placeholder="e.g., CDC Reference Lab"
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="requested-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.retrieval.requestedBy",
                  defaultMessage: "Requested By",
                })}
                value={retrievalData.retrievalRequestedBy}
                onChange={(e) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievalRequestedBy: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="retrieved-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.retrieval.retrievedBy",
                  defaultMessage: "Retrieved By *",
                })}
                value={retrievalData.retrievedBy}
                onChange={(e) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievedBy: e.target.value,
                  })
                }
                required
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={retrievalData.retrievalDate}
                onChange={([date]) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievalDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="retrieval-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.retrieval.date",
                    defaultMessage: "Retrieval Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TimePicker
                id="retrieval-time"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.retrieval.time",
                  defaultMessage: "Retrieval Time",
                })}
                value={retrievalData.retrievalTime}
                onChange={(e) =>
                  setRetrievalData({
                    ...retrievalData,
                    retrievalTime: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <Checkbox
            id="approval-required"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.retrieval.approvalRequired",
              defaultMessage: "External/Sensitive - Approval Required",
            })}
            checked={retrievalData.retrievalApprovalRequired}
            onChange={(_, { checked }) =>
              setRetrievalData({
                ...retrievalData,
                retrievalApprovalRequired: checked,
              })
            }
          />

          {retrievalData.retrievalApprovalRequired && (
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="approved-by"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.retrieval.approvedBy",
                    defaultMessage: "Approved By",
                  })}
                  value={retrievalData.retrievalApprovedBy}
                  onChange={(e) =>
                    setRetrievalData({
                      ...retrievalData,
                      retrievalApprovedBy: e.target.value,
                    })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    setRetrievalData({
                      ...retrievalData,
                      retrievalApprovalDate:
                        date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="approval-date"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.retrieval.approvalDate",
                      defaultMessage: "Approval Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
            </Grid>
          )}

          <TextArea
            id="retrieval-notes"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.retrieval.notes",
              defaultMessage: "Notes",
            })}
            value={retrievalData.retrievalNotes}
            onChange={(e) =>
              setRetrievalData({
                ...retrievalData,
                retrievalNotes: e.target.value,
              })
            }
            rows={2}
          />
        </div>
      </Modal>

      {/* Biorepository Transfer Modal */}
      <Modal
        open={showBiorepositoryModal}
        onRequestClose={() => setShowBiorepositoryModal(false)}
        onRequestSubmit={handleApplyBiorepositoryData}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.biorepository.modalTitle",
          defaultMessage: "Transfer to Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.biorepository.confirm",
          defaultMessage: "Confirm Transfer",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.biorepository.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage(
              {
                id: "notebook.bacteriology.biorepository.info",
                defaultMessage:
                  "Transferring {count} sample(s) to biorepository",
              },
              { count: selectedIds.length },
            )}
            hideCloseButton
            lowContrast
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="biorepository-name"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.biorepository.name",
                  defaultMessage: "Biorepository Name *",
                })}
                value={biorepositoryData.biorepositoryName}
                onChange={(e) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    biorepositoryName: e.target.value,
                  })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="chain-of-custody"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.biorepository.chainOfCustody",
                  defaultMessage: "Chain of Custody Number *",
                })}
                value={biorepositoryData.chainOfCustodyNumber}
                onChange={(e) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    chainOfCustodyNumber: e.target.value,
                  })
                }
                required
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={biorepositoryData.transferDate}
                onChange={([date]) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    transferDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="transfer-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.biorepository.transferDate",
                    defaultMessage: "Transfer Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="transfer-conditions"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.biorepository.conditions",
                  defaultMessage: "Transfer Conditions",
                })}
                value={biorepositoryData.transferConditions}
                onChange={(e) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    transferConditions: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select conditions..." />
                {transferConditions.map((condition) => (
                  <SelectItem
                    key={condition.value}
                    value={condition.value}
                    text={condition.label}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="transferred-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.biorepository.transferredBy",
                  defaultMessage: "Transferred By",
                })}
                value={biorepositoryData.transferredBy}
                onChange={(e) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    transferredBy: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="received-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.biorepository.receivedBy",
                  defaultMessage: "Received By (Biorepository)",
                })}
                value={biorepositoryData.receivedBy}
                onChange={(e) =>
                  setBiorepositoryData({
                    ...biorepositoryData,
                    receivedBy: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <TextInput
            id="biorepository-accession"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.biorepository.accessionNumber",
              defaultMessage: "Biorepository Accession Number",
            })}
            value={biorepositoryData.biorepositoryAccessionNumber}
            onChange={(e) =>
              setBiorepositoryData({
                ...biorepositoryData,
                biorepositoryAccessionNumber: e.target.value,
              })
            }
          />

          {/* New Storage Location Selector */}
          <div style={{ marginTop: "1rem" }}>
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.biorepository.newStorageLocation"
                defaultMessage="New Storage Location (Optional)"
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
                id="notebook.bacteriology.biorepository.newStorageDescription"
                defaultMessage="Select a new storage location in the biorepository for these samples. Leave empty to keep current storage."
              />
            </p>
            <StorageHierarchySelector
              onLocationSelect={(location) =>
                setBiorepositoryData({
                  ...biorepositoryData,
                  newStorageLocation: location,
                })
              }
              selectedLocation={biorepositoryData.newStorageLocation}
              showPositionCoordinate={true}
              compact={true}
            />
            {biorepositoryData.newStorageLocation && (
              <div style={{ marginTop: "0.5rem" }}>
                <Tag type="teal" size="sm">
                  <FormattedMessage
                    id="notebook.bacteriology.biorepository.newLocation"
                    defaultMessage="New Location: {path}"
                    values={{
                      path:
                        biorepositoryData.newStorageLocation.path ||
                        biorepositoryData.newStorageLocation.label ||
                        "Selected",
                    }}
                  />
                </Tag>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    setBiorepositoryData({
                      ...biorepositoryData,
                      newStorageLocation: null,
                    })
                  }
                  style={{ marginLeft: "0.5rem" }}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.biorepository.clearLocation"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Disposal Modal */}
      <Modal
        open={showDisposalModal}
        onRequestClose={() => setShowDisposalModal(false)}
        onRequestSubmit={handleApplyDisposalData}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.disposal.modalTitle",
          defaultMessage: "Dispose Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.disposal.confirm",
          defaultMessage: "Confirm Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.disposal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "notebook.bacteriology.disposal.warning",
              defaultMessage: "Warning: This action is irreversible",
            })}
            subtitle={intl.formatMessage(
              {
                id: "notebook.bacteriology.disposal.warningSubtitle",
                defaultMessage:
                  "You are about to dispose {count} sample(s). Records will be locked and archived.",
              },
              { count: selectedIds.length },
            )}
            hideCloseButton
            lowContrast
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-criteria"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.criteria",
                  defaultMessage: "Disposal Criteria *",
                })}
                value={disposalData.disposalCriteria}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposalCriteria: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select criteria..." />
                {disposalCriteria.map((criterion) => (
                  <SelectItem
                    key={criterion.value}
                    value={criterion.value}
                    text={criterion.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-method"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.method",
                  defaultMessage: "Disposal Method *",
                })}
                value={disposalData.disposalMethod}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposalMethod: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select method..." />
                {disposalMethods.map((method) => (
                  <SelectItem
                    key={method.value}
                    value={method.value}
                    text={method.label}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={disposalData.disposalDate}
                onChange={([date]) =>
                  setDisposalData({
                    ...disposalData,
                    disposalDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="disposal-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.disposal.disposalDate",
                    defaultMessage: "Disposal Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="disposed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.disposedBy",
                  defaultMessage: "Disposed By",
                })}
                value={disposalData.disposedBy}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposedBy: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="quantity-disposed"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.quantity",
                  defaultMessage: "Quantity Disposed",
                })}
                value={disposalData.quantityDisposed}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    quantityDisposed: e.target.value,
                  })
                }
                placeholder="e.g., 5 tubes, 2 plates"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="disposal-certificate"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.certificate",
                  defaultMessage: "Disposal Certificate Number",
                })}
                value={disposalData.disposalCertificateNumber}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposalCertificateNumber: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <h5>
            <FormattedMessage
              id="notebook.bacteriology.disposal.signOffs"
              defaultMessage="Required Sign-Offs"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="supervisor-signoff"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.supervisor",
                  defaultMessage: "Laboratory Supervisor *",
                })}
                value={disposalData.supervisorSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    supervisorSignOff: e.target.value,
                  })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="biosafety-signoff"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.disposal.biosafetyOfficer",
                  defaultMessage: "Biosafety Officer (if applicable)",
                })}
                value={disposalData.biosafetyOfficerSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    biosafetyOfficerSignOff: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <TextArea
            id="disposal-notes"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.disposal.notes",
              defaultMessage: "Notes / Comments",
            })}
            value={disposalData.disposalNotes}
            onChange={(e) =>
              setDisposalData({
                ...disposalData,
                disposalNotes: e.target.value,
              })
            }
            rows={2}
          />

          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#fff1f1",
              borderRadius: "4px",
            }}
          >
            <Checkbox
              id="confirm-disposal"
              labelText={intl.formatMessage({
                id: "notebook.bacteriology.disposal.confirmCheckbox",
                defaultMessage:
                  "I confirm that all data has been archived and these samples can be permanently disposed. This action cannot be undone.",
              })}
              checked={disposalData.confirmDisposal}
              onChange={(_, { checked }) =>
                setDisposalData({
                  ...disposalData,
                  confirmDisposal: checked,
                })
              }
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologySampleRetrievalDisposalPage;
