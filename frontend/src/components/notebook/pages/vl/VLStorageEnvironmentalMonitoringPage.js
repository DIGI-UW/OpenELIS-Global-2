import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
  InlineNotification,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  Archive,
  Undo,
  TrashCan,
  CheckmarkFilled,
  Renew,
  Automatic,
} from "@carbon/react/icons";
import useVLPermissions from "../../../../hooks/useVLPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * VLStorageEnvironmentalMonitoringPage - Page 10: Storage & Environmental Monitoring
 *
 * Manages physical storage, environmental monitoring, and sample retrieval/disposal.
 * Implements TMMRD design pattern:
 * - Section-based layout
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Hierarchical storage location assignment
 * - Retrieval and disposal tracking
 * - Storage logbook audit trail
 *
 * Data stored in sample.data JSONB:
 * {
 *   stage10: {
 *     storageLocation: "Room A > Freezer-80-A > Shelf 3 > Rack 2 > Box VL-001",
 *     storageCondition: "FROZEN_MINUS80",
 *     dateStored: "2025-01-29",
 *     timeStored: "14:30",
 *     wellCoordinate: "A3",
 *     retentionYears: 5,
 *     retrieval: { dateRetrieved, retrievedBy, recipientSignature, purpose },
 *     disposal: { disposalDate, disposalMethod, disposedBy, disposalNotes },
 *     storageHistory: [ { timestamp, action, location, performedBy, notes } ]
 *   }
 * }
 */
export const VLStorageEnvironmentalMonitoringPage = ({
  entryId,
  notebookId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { getPagePermissionLevel, canSaveData, canAccessSampleStorage } =
    useVLPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VL Lab Technician",
    "VL Manager",
    "VL Principal Investigator",
    "VL Data Manager",
  ];

  const canAccessPage = canAccessSampleStorage() || hasAnyRole(allowedRoles);

  const pagePermissionLevel = getPagePermissionLevel(
    "Storage & Environmental Monitoring",
  );
  const canPerformStorage = canSaveData(pagePermissionLevel);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Modal states
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);

  // Storage hierarchy selection
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [selectedBox, setSelectedBox] = useState(null);
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [dateStored, setDateStored] = useState(
    new Date().toISOString().split("T")[0],
  );

  // Retrieval state
  const [selectedSampleForRetrieval, setSelectedSampleForRetrieval] =
    useState(null);
  const [retrievalData, setRetrievalData] = useState({
    dateRetrieved: new Date().toISOString().split("T")[0],
    retrievedBy: "",
    recipientSignature: "",
    purpose: "",
  });
  const [isBulkRetrieval, setIsBulkRetrieval] = useState(false);

  // Disposal state
  const [selectedSampleForDisposal, setSelectedSampleForDisposal] =
    useState(null);
  const [disposalData, setDisposalData] = useState({
    disposalDate: new Date().toISOString().split("T")[0],
    disposalMethod: "",
    disposedBy: "",
    disposalNotes: "",
  });
  const [isBulkDisposal, setIsBulkDisposal] = useState(false);

  // Storage condition options
  const storageConditionOptions = [
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "notebook.vl.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C) - DNA, PCR products, Libraries",
      }),
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "notebook.vl.storage.condition.frozen80",
        defaultMessage: "Frozen (-80°C) - DNA, RNA (avoid freeze-thaw)",
      }),
    },
  ];

  const disposalMethodOptions = [
    { id: "INCINERATION", label: "Incineration" },
    { id: "AUTOCLAVE", label: "Autoclave" },
    { id: "CHEMICAL", label: "Chemical Disposal" },
    { id: "BIOHAZARD", label: "Biohazard Waste" },
    { id: "OTHER", label: "Other" },
  ];

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  sampleType: s.data?.sampleType,
                  collectionDate: s.data?.collectionDate,
                  storageLocation:
                    s.data?.storagePath || s.data?.storageLocation,
                  wellCoordinate: s.data?.wellCoordinate || s.data?.storageWell,
                  storageCondition: s.data?.storageCondition,
                  dateStored: s.data?.dateStored,
                  retentionExpiry: s.data?.retentionExpiry,
                  retrieval: s.data?.retrieval,
                  disposal: s.data?.disposal,
                }))
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  // Progress calculations
  const storageSummary = useMemo(() => {
    return {
      total: samples.length,
      stored: samples.filter(
        (s) => s.storageLocation && s.storageLocation.length > 0,
      ).length,
      pending: samples.filter(
        (s) => !s.storageLocation || s.storageLocation.length === 0,
      ).length,
    };
  }, [samples]);

  // Helper to render storage condition
  const renderCondition = (condition) => {
    if (!condition) return "-";
    const label =
      storageConditionOptions.find((c) => c.id === condition)?.label ||
      condition;
    return label;
  };

  // Helper to determine sample workflow status
  const getWorkflowStatus = (sample) => {
    // Check status field directly (set by backend)
    if (sample.status === "COMPLETED") return "COMPLETED";
    if (sample.status === "RETRIEVED") return "RETRIEVED";
    if (sample.status === "DISPOSED") return "DISPOSED";
    // Fallback to checking data objects for backward compatibility
    if (sample.disposal) return "DISPOSED";
    if (sample.retrieval) return "RETRIEVED";
    if (sample.storageLocation) return "STORED";
    return "PENDING";
  };

  // Helper to render workflow status
  const renderWorkflowStatus = (sample) => {
    const status = getWorkflowStatus(sample);
    const iconMap = {
      COMPLETED: CheckmarkFilled,
      DISPOSED: TrashCan,
      RETRIEVED: Undo,
      STORED: Archive,
    };

    const typeMap = {
      COMPLETED: "green",
      DISPOSED: "red",
      RETRIEVED: "blue",
      STORED: "cyan",
      PENDING: "gray",
    };

    const labelMap = {
      COMPLETED: "Completed",
      DISPOSED: "Disposed",
      RETRIEVED: "Recovered",
      STORED: "Stored",
      PENDING: "Pending",
    };

    return (
      <Tag type={typeMap[status]} size="sm" renderIcon={iconMap[status]}>
        {labelMap[status]}
      </Tag>
    );
  };

  // Check page access
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Storage & Environmental Monitoring"
        reason="This page requires specific VL laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Handlers for modals
  const handleOpenStorageModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }

    // Check if any selected sample already has storage assigned
    const samplesWithStorage = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.storageLocation &&
        s.storageLocation.length > 0,
    );

    if (samplesWithStorage.length > 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.error.alreadyAssigned",
          defaultMessage:
            "Cannot reassign storage to samples that already have a location assigned.",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.vl.storage.error.alreadyAssignedDetails",
            defaultMessage:
              "{count} of {total} selected samples already have storage assigned. Please unassign storage first before reassigning.",
          },
          { count: samplesWithStorage.length, total: selectedSampleIds.length },
        ),
      });
      return;
    }

    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    setSelectedBox(null);
    setBoxLayout({});
    setWellAssignments({});
    setSelectedCondition(null);
    setStorageModalOpen(true);
  }, [selectedSampleIds, samples, notify, intl]);

  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
    setSelectedBox(selection.box || null);
    if (selection.box?.id) {
      // Load box occupancy if available
      getFromOpenElisServer(
        `/rest/storage/boxes/${selection.box.id}/occupancy`,
        (response) => {
          if (componentMounted.current && response) {
            const occupiedCoordinates = response.occupiedCoordinates || {};
            setBoxLayout(occupiedCoordinates);
          }
        },
      );
    } else {
      setBoxLayout({});
    }
  }, []);

  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Build combined layout (existing + pending assignments)
  const getCombinedLayout = () => {
    const combined = { ...boxLayout };
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
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
  };

  // Auto-populate wells with samples
  const handleAutoPopulate = useCallback(() => {
    if (!selectedBox) return;

    const rows = selectedBox.rows || 8;
    const columns = selectedBox.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        if (!boxLayout[wellCoord]) {
          newAssignments[selectedSampleIds[sampleIndex]] = wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setWellAssignments(newAssignments);
  }, [selectedSampleIds, selectedBox, boxLayout]);

  // Handle well click for manual assignment
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && wellInfo.pending === false) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.vl.storage.wellOccupied",
            defaultMessage: `Well ${wellCoord} is already occupied`,
          }),
        });
        return;
      }

      if (Object.values(wellAssignments).includes(wellCoord)) {
        // Deselect if already selected
        const newAssignments = { ...wellAssignments };
        delete newAssignments[
          Object.keys(newAssignments).find(
            (k) => newAssignments[k] === wellCoord,
          )
        ];
        setWellAssignments(newAssignments);
      } else if (
        Object.keys(wellAssignments).length < selectedSampleIds.length
      ) {
        // Select next unassigned sample
        const assignedSamples = Object.keys(wellAssignments);
        const nextSample = selectedSampleIds.find(
          (id) => !assignedSamples.includes(id),
        );
        if (nextSample) {
          setWellAssignments({
            ...wellAssignments,
            [nextSample]: wellCoord,
          });
        }
      }
    },
    [wellAssignments, selectedSampleIds, notify, intl],
  );

  // Implement storage assignment

  // Handle retrieval (single or bulk)
  const handleSubmitRetrieval = useCallback(() => {
    if (!retrievalData.retrievedBy) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.retrieval.requiredField",
          defaultMessage: "Retrieved By is required",
        }),
      });
      return;
    }

    setSubmitting(true);

    // Get sample IDs (bulk or single)
    const sampleIds = isBulkRetrieval
      ? selectedSampleIds.map((id) => parseInt(id, 10))
      : [parseInt(selectedSampleForRetrieval.id, 10)];

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          retrieval: {
            dateRetrieved: retrievalData.dateRetrieved,
            retrievedBy: retrievalData.retrievedBy,
            recipientSignature: retrievalData.recipientSignature || null,
            purpose: retrievalData.purpose || null,
          },
        },
      }),
      (response) => {
        setSubmitting(false);

        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.vl.storage.retrieval.success",
                defaultMessage: "Recorded retrieval for {count} sample(s)",
              },
              { count: sampleIds.length },
            ),
          });

          // Close modal immediately after retrieval data is recorded
          setRetrievalModalOpen(false);
          setSelectedSampleForRetrieval(null);
          setIsBulkRetrieval(false);
          setRetrievalData({
            dateRetrieved: new Date().toISOString().split("T")[0],
            retrievedBy: "",
            recipientSignature: "",
            purpose: "",
          });

          // Update status to RETRIEVED (non-blocking)
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({ sampleIds, status: "RETRIEVED" }),
            () => {
              // Reload samples to reflect status change (always reload regardless of response)
              loadPageSamples();
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.vl.storage.retrieval.error",
              defaultMessage: "Failed to record sample retrieval",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleForRetrieval,
    retrievalData,
    isBulkRetrieval,
    selectedSampleIds,
    pageData?.id,
    notify,
    intl,
    loadPageSamples,
  ]);

  // Handle disposal (single or bulk)
  const handleSubmitDisposal = useCallback(() => {
    if (!disposalData.disposalMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.disposal.requiredField",
          defaultMessage: "Disposal Method is required",
        }),
      });
      return;
    }

    setSubmitting(true);

    // Get sample IDs (bulk or single)
    const sampleIds = isBulkDisposal
      ? selectedSampleIds.map((id) => parseInt(id, 10))
      : [parseInt(selectedSampleForDisposal.id, 10)];

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          disposal: {
            disposalDate: disposalData.disposalDate,
            disposalMethod: disposalData.disposalMethod,
            disposedBy: disposalData.disposedBy || "System",
            disposalNotes: disposalData.disposalNotes || null,
          },
        },
      }),
      (response) => {
        setSubmitting(false);

        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.vl.storage.disposal.success",
                defaultMessage: "Recorded disposal for {count} sample(s)",
              },
              { count: sampleIds.length },
            ),
          });

          // Close modal immediately after disposal data is recorded
          setDisposalModalOpen(false);
          setSelectedSampleForDisposal(null);
          setIsBulkDisposal(false);
          setDisposalData({
            disposalDate: new Date().toISOString().split("T")[0],
            disposalMethod: "",
            disposedBy: "",
            disposalNotes: "",
          });

          // Update status to DISPOSED (non-blocking)
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({ sampleIds, status: "DISPOSED" }),
            () => {
              // Reload samples to reflect status change (always reload regardless of response)
              loadPageSamples();
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.vl.storage.disposal.error",
              defaultMessage: "Failed to record sample disposal",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleForDisposal,
    disposalData,
    isBulkDisposal,
    selectedSampleIds,
    pageData?.id,
    notify,
    intl,
    loadPageSamples,
  ]);

  // Mark samples as complete
  const handleMarkComplete = useCallback(() => {
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.storageLocation &&
        s.storageLocation.length > 0,
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.markComplete.noEligible",
          defaultMessage:
            "Selected samples must have storage assigned before completing.",
        }),
      });
      return;
    }

    setSubmitting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds, status: "COMPLETED" }),
      (response) => {
        setSubmitting(false);

        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.vl.storage.markComplete.success",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          });

          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.vl.storage.markComplete.error",
              defaultMessage: "Failed to mark samples complete",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    notify,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Count eligible samples for completion
  const eligibleForCompletionCount = useMemo(
    () =>
      samples.filter(
        (s) =>
          selectedSampleIds.includes(s.id) &&
          s.storageLocation &&
          s.storageLocation.length > 0,
      ).length,
    [samples, selectedSampleIds],
  );

  const handleAssignStorage = useCallback(() => {
    // Validation
    if (!selectedBox) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.selectBox",
          defaultMessage: "Please select a storage box",
        }),
      });
      return;
    }

    if (!selectedCondition) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.selectCondition",
          defaultMessage: "Please select a storage condition",
        }),
      });
      return;
    }

    if (Object.keys(wellAssignments).length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.storage.assignWells",
          defaultMessage: "Please assign samples to wells",
        }),
      });
      return;
    }

    setSubmitting(true);

    const sampleIdsString = Object.keys(wellAssignments).map((id) =>
      String(id),
    );

    // Use the storage assignment endpoint
    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/assign-storage`,
      JSON.stringify({
        sampleIdsString,
        boxId: selectedBox.id,
        wellAssignments,
        condition: selectedCondition.id,
        retentionYears,
        reassign: false,
        pageId: pageData.id,
        dateStored,
      }),
      (response) => {
        setSubmitting(false);

        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.vl.storage.assignSuccess",
                defaultMessage: "Assigned {count} samples to storage",
              },
              {
                count:
                  response.assignedCount || Object.keys(wellAssignments).length,
              },
            ),
          });

          // Update sample status to IN_PROGRESS
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: Object.keys(wellAssignments).map((id) =>
                parseInt(id, 10),
              ),
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                setStorageModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.vl.storage.assignError",
                defaultMessage: "Failed to assign storage",
              }),
          });
        }
      },
    );
  }, [
    selectedBox,
    selectedCondition,
    wellAssignments,
    retentionYears,
    dateStored,
    notebookId,
    pageData?.id,
    notify,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  return (
    <div className="vl-storage-monitoring-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.vl.storage.title"
            defaultMessage="Storage & Environmental Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.vl.storage.description"
            defaultMessage="Track physical storage locations and environmental conditions for DNA/RNA samples, PCR products, and sequencing libraries."
          />
        </p>
      </div>

      {/* Progress Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.vl.storage.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.vl.storage.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storageSummary.stored}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.vl.storage.pending"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Bar */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            !canPerformStorage ||
            // Disable if any selected sample is disposed or completed
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .some((s) => s.disposal || s.status === "COMPLETED") ||
            // Disable if any selected sample already has storage assigned
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .some((s) => s.storageLocation && s.storageLocation.length > 0)
          }
          title={
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .some((s) => s.storageLocation && s.storageLocation.length > 0)
              ? "Some selected samples already have storage assigned. Unassign storage first to reassign."
              : ""
          }
        >
          <FormattedMessage
            id="notebook.vl.storage.assignStorage"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Undo}
          onClick={() => {
            setIsBulkRetrieval(true);
            setRetrievalModalOpen(true);
          }}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            !canPerformStorage ||
            // Recover only for DISPOSED samples (to recover them), not if COMPLETED
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .some((s) => s.status === "COMPLETED") ||
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .every((s) => !s.disposal || s.retrieval)
          }
        >
          <FormattedMessage
            id="notebook.vl.storage.retrieveSelected"
            defaultMessage="Recover ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={TrashCan}
          onClick={() => {
            setIsBulkDisposal(true);
            setDisposalModalOpen(true);
          }}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            !canPerformStorage ||
            // Dispose only for STORED samples (not yet disposed), not if COMPLETED
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .some((s) => s.status === "COMPLETED") ||
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .every((s) => !s.storageLocation || s.disposal)
          }
        >
          <FormattedMessage
            id="notebook.vl.storage.disposeSelected"
            defaultMessage="Dispose ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            eligibleForCompletionCount === 0 ||
            submitting ||
            !hasRealPageId ||
            !canPerformStorage
          }
        >
          <FormattedMessage
            id="notebook.vl.storage.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: eligibleForCompletionCount }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.vl.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.vl.storage.sampleList"
              defaultMessage="Sample Storage Tracking"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {samples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && samples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.vl.storage.noSamples"
                  defaultMessage="No samples found for this stage."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="vl-storage-monitoring"
              samples={samples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformStorage}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "storageLocation", header: "Storage Location" },
                { key: "wellCoordinate", header: "Well" },
                {
                  key: "storageCondition",
                  header: "Condition",
                  render: (_value, sample) =>
                    renderCondition(sample.storageCondition),
                },
                { key: "dateStored", header: "Date Stored" },
                { key: "retentionExpiry", header: "Retention Expiry" },
                {
                  key: "workflowStatus",
                  header: intl.formatMessage({
                    id: "notebook.vl.column.workflowStatus",
                    defaultMessage: "Workflow Status",
                  }),
                  render: (_value, sample) => renderWorkflowStatus(sample),
                },
                {
                  key: "retrievalInfo",
                  header: "Recovered",
                  render: (_value, sample) =>
                    sample.retrieval ? (
                      <div style={{ fontSize: "0.875rem" }}>
                        <div>{sample.retrieval.retrievedBy}</div>
                        <div style={{ color: "#525252" }}>
                          {sample.retrieval.dateRetrieved}
                        </div>
                      </div>
                    ) : (
                      "-"
                    ),
                },
                {
                  key: "disposalInfo",
                  header: "Disposed",
                  render: (_value, sample) =>
                    sample.disposal ? (
                      <div style={{ fontSize: "0.875rem" }}>
                        <div>{sample.disposal.disposalMethod}</div>
                        <div style={{ color: "#525252" }}>
                          {sample.disposal.disposalDate}
                        </div>
                      </div>
                    ) : (
                      "-"
                    ),
                },
                {
                  key: "sampleActions",
                  header: "Actions",
                  render: (_value, sample) => (
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={Undo}
                        onClick={() => {
                          setIsBulkRetrieval(false);
                          setSelectedSampleForRetrieval(sample);
                          setRetrievalModalOpen(true);
                        }}
                        disabled={
                          sample.status === "COMPLETED" ||
                          !sample.disposal ||
                          sample.retrieval
                        }
                        title={
                          sample.status === "COMPLETED"
                            ? "Sample is completed"
                            : !sample.disposal
                              ? "Sample not disposed"
                              : sample.retrieval
                                ? "Already recovered"
                                : "Recover disposed sample"
                        }
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={TrashCan}
                        onClick={() => {
                          setIsBulkDisposal(false);
                          setSelectedSampleForDisposal(sample);
                          setDisposalModalOpen(true);
                        }}
                        disabled={
                          sample.status === "COMPLETED" ||
                          !sample.storageLocation ||
                          sample.disposal
                        }
                        title={
                          sample.status === "COMPLETED"
                            ? "Sample is completed"
                            : !sample.storageLocation
                              ? "No storage location"
                              : sample.disposal
                                ? "Already disposed"
                                : "Dispose sample"
                        }
                      />
                    </div>
                  ),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "notebook.vl.storage.assignModal.title",
            defaultMessage: "Assign {count} samples to Storage",
          },
          { count: selectedSampleIds.length },
        )}
        primaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.assignModal.assign",
          defaultMessage: "Assign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.assignModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setStorageModalOpen(false)}
        onRequestSubmit={handleAssignStorage}
        danger={false}
      >
        <div style={{ marginBottom: "2rem" }}>
          <h5 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.vl.storage.selectLocation"
              defaultMessage="Select Storage Location"
            />
          </h5>
          <StorageHierarchySelector
            onSelectionChange={handleStorageSelectionChange}
            initialSelection={storageSelection}
            boxRequired={true}
            showPath={true}
            entryId={entryId}
            onBoxLayoutLoaded={handleBoxLayoutLoaded}
          />
        </div>

        {/* Box Layout Viewer */}
        {selectedBox && (
          <div style={{ marginBottom: "2rem" }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: "1rem",
              }}
            >
              <h5>
                <FormattedMessage
                  id="notebook.vl.storage.boxLayout"
                  defaultMessage="Box Layout (96-well)"
                />
              </h5>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Automatic}
                onClick={handleAutoPopulate}
              >
                <FormattedMessage
                  id="notebook.vl.storage.autoPopulate"
                  defaultMessage="Auto-Populate"
                />
              </Button>
            </div>
            <BoxLayoutViewer
              boxId={selectedBox.id}
              layout={getCombinedLayout()}
              rows={selectedBox.rows || 8}
              columns={selectedBox.columns || 12}
              onWellClick={handleWellClick}
            />
            <p
              style={{
                marginTop: "0.5rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              {Object.keys(wellAssignments).length} of{" "}
              {selectedSampleIds.length} samples assigned to wells
            </p>
          </div>
        )}

        {/* Storage Condition and Retention */}
        <div style={{ marginBottom: "1rem" }}>
          <Dropdown
            id="storage-condition"
            titleText={intl.formatMessage({
              id: "notebook.vl.storage.condition",
              defaultMessage: "Storage Condition",
            })}
            label="Select storage condition"
            items={storageConditionOptions}
            selectedItem={selectedCondition}
            onChange={({ selectedItem }) => setSelectedCondition(selectedItem)}
            itemToString={(item) => item?.label || ""}
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <NumberInput
            id="retention-years"
            label={intl.formatMessage({
              id: "notebook.vl.storage.retentionYears",
              defaultMessage: "Retention Period (years)",
            })}
            value={retentionYears}
            onChange={(e) =>
              setRetentionYears(parseInt(e.target.value, 10) || 5)
            }
            min={1}
            max={50}
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <DatePickerInput
            id="date-stored"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.dateStored",
              defaultMessage: "Date Stored",
            })}
            value={dateStored}
            onChange={([date]) =>
              setDateStored(new Date(date).toISOString().split("T")[0])
            }
          />
        </div>
      </Modal>

      {/* Retrieval Modal */}
      <Modal
        open={retrievalModalOpen}
        modalHeading={
          isBulkRetrieval
            ? intl.formatMessage(
                {
                  id: "notebook.vl.storage.retrievalModal.bulkTitle",
                  defaultMessage: "Recover {count} Disposed Samples",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "notebook.vl.storage.retrievalModal.title",
                  defaultMessage: "Recover Disposed Sample - {id}",
                },
                { id: selectedSampleForRetrieval?.accessionNumber || "" },
              )
        }
        primaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.retrievalModal.recordRetrieval",
          defaultMessage: "Record Recovery",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.retrievalModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setRetrievalModalOpen(false);
          setSelectedSampleForRetrieval(null);
          setIsBulkRetrieval(false);
        }}
        onRequestSubmit={handleSubmitRetrieval}
      >
        <InlineNotification
          kind="info"
          title={intl.formatMessage({
            id: "notebook.vl.storage.retrieval.info",
            defaultMessage: "Recovery",
          })}
          subtitle={intl.formatMessage({
            id: "notebook.vl.storage.retrieval.infoDetails",
            defaultMessage:
              "This action recovers a previously disposed sample back to storage.",
          })}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1.5rem" }}
        />
        <div style={{ marginBottom: "1rem" }}>
          <DatePickerInput
            id="date-retrieved"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.retrieval.dateRecovered",
              defaultMessage: "Date Recovered",
            })}
            value={retrievalData.dateRetrieved}
            onChange={([date]) =>
              setRetrievalData({
                ...retrievalData,
                dateRetrieved: new Date(date).toISOString().split("T")[0],
              })
            }
          />
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="retrieved-by"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.retrieval.recoveredBy",
              defaultMessage: "Recovered By",
            })}
            value={retrievalData.retrievedBy}
            onChange={(e) =>
              setRetrievalData({
                ...retrievalData,
                retrievedBy: e.target.value,
              })
            }
            placeholder="Staff name or initials"
          />
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="recipient-signature"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.retrieval.signature",
              defaultMessage: "Authorized By",
            })}
            value={retrievalData.recipientSignature}
            onChange={(e) =>
              setRetrievalData({
                ...retrievalData,
                recipientSignature: e.target.value,
              })
            }
            placeholder="Initials or name"
          />
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="retrieval-purpose"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.retrieval.reason",
              defaultMessage: "Reason for Recovery",
            })}
            value={retrievalData.purpose}
            onChange={(e) =>
              setRetrievalData({ ...retrievalData, purpose: e.target.value })
            }
            placeholder="e.g., Error in disposal, Additional testing needed"
          />
        </div>
      </Modal>

      {/* Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        modalHeading={
          isBulkDisposal
            ? intl.formatMessage(
                {
                  id: "notebook.vl.storage.disposalModal.bulkTitle",
                  defaultMessage: "Dispose {count} Samples",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "notebook.vl.storage.disposalModal.title",
                  defaultMessage: "Dispose Sample - {id}",
                },
                { id: selectedSampleForDisposal?.accessionNumber || "" },
              )
        }
        primaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.disposalModal.recordDisposal",
          defaultMessage: "Record Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.vl.storage.disposalModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setDisposalModalOpen(false);
          setSelectedSampleForDisposal(null);
          setIsBulkDisposal(false);
        }}
        onRequestSubmit={handleSubmitDisposal}
        danger
      >
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notebook.vl.storage.disposal.warning",
            defaultMessage: "Warning: This action cannot be undone",
          })}
          subtitle={intl.formatMessage({
            id: "notebook.vl.storage.disposal.warningDetails",
            defaultMessage:
              "Please ensure you have the proper authorization before disposing of this sample.",
          })}
          style={{ marginBottom: "1rem" }}
        />

        <div style={{ marginBottom: "1rem" }}>
          <DatePickerInput
            id="disposal-date"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.disposal.disposalDate",
              defaultMessage: "Disposal Date",
            })}
            value={disposalData.disposalDate}
            onChange={([date]) =>
              setDisposalData({
                ...disposalData,
                disposalDate: new Date(date).toISOString().split("T")[0],
              })
            }
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <Select
            id="disposal-method"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.disposal.method",
              defaultMessage: "Disposal Method",
            })}
            value={disposalData.disposalMethod}
            onChange={(e) =>
              setDisposalData({
                ...disposalData,
                disposalMethod: e.target.value,
              })
            }
          >
            <SelectItem value="" text="Select disposal method" />
            {disposalMethodOptions.map((option) => (
              <SelectItem
                key={option.id}
                value={option.id}
                text={option.label}
              />
            ))}
          </Select>
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="disposed-by"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.disposal.disposedBy",
              defaultMessage: "Disposed By",
            })}
            value={disposalData.disposedBy}
            onChange={(e) =>
              setDisposalData({ ...disposalData, disposedBy: e.target.value })
            }
            placeholder="Staff name"
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <TextArea
            id="disposal-notes"
            labelText={intl.formatMessage({
              id: "notebook.vl.storage.disposal.notes",
              defaultMessage: "Notes",
            })}
            value={disposalData.disposalNotes}
            onChange={(e) =>
              setDisposalData({
                ...disposalData,
                disposalNotes: e.target.value,
              })
            }
            rows={2}
            placeholder="Reason for disposal, retention period expiry, etc."
          />
        </div>
      </Modal>

      {loading && (
        <div style={{ padding: "2rem", textAlign: "center" }}>
          <Loading withOverlay={false} description="Loading samples..." />
        </div>
      )}
    </div>
  );
};

export default VLStorageEnvironmentalMonitoringPage;
