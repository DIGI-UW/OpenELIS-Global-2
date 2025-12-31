import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Modal,
  NumberInput,
  Dropdown,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Undo,
  Checkmark,
  Renew,
  Location,
  Automatic,
  Warning,
  TrashCan,
  Time,
  Calendar,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyStorageInventoryPage - Page 5 of the pathology workflow.
 * Purpose: Track physical storage and retrieval of pathology materials.
 * Who uses it: Store manager / lab staff
 *
 * Features:
 * - Hierarchical storage selection (Room → Device → Shelf → Rack → Box)
 * - 96-well grid visualization with auto-populate functionality
 * - Storage condition and retention period tracking
 * - Temperature monitoring (AM/PM checks)
 * - Sample retrieval with signature tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyStorageInventoryPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);
  const [isReassignment, setIsReassignment] = useState(false);

  // Auto-assign modal state
  const [autoAssignModalOpen, setAutoAssignModalOpen] = useState(false);
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [autoAssignValues, setAutoAssignValues] = useState({
    storageType: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });

  // Storage hierarchy using StorageHierarchySelector
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });

  // Temperature logs state
  const [temperatureLogs, setTemperatureLogs] = useState([]);

  // Box layout state
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [expectedDuration, setExpectedDuration] = useState("");
  const [dateStored, setDateStored] = useState(
    new Date().toISOString().split("T")[0],
  );

  // Temperature log modal state
  const [tempLogModalOpen, setTempLogModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [tempLogData, setTempLogData] = useState({
    storageUnit: "",
    deviceType: "",
    temperature: 0,
    checkTime: new Date().toTimeString().slice(0, 5),
    checkedBy: "",
    checkDate: new Date().toISOString().split("T")[0],
    notes: "",
  });

  // Retrieval modal state
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [selectedSampleForRetrieval, setSelectedSampleForRetrieval] =
    useState(null);
  const [retrievalData, setRetrievalData] = useState({
    dateRetrieved: "",
    retrievedBy: "",
    recipientSignature: "",
    purpose: "",
  });

  // Storage Logbook state
  const [storageLogbook, setStorageLogbook] = useState([]);

  // Disposal modal state
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [selectedSampleForDisposal, setSelectedSampleForDisposal] =
    useState(null);
  const [disposalData, setDisposalData] = useState({
    disposalDate: "",
    disposalMethod: "",
    disposedBy: "",
    disposalNotes: "",
  });

  // Storage condition options matching backend StorageCondition enum
  const storageConditionOptions = [
    {
      id: "ROOM_TEMP",
      label: intl.formatMessage({
        id: "pathology.storage.condition.roomTemp",
        defaultMessage: "Room Temperature Cabinet (15-25°C)",
      }),
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "pathology.storage.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen80",
        defaultMessage: "Frozen (-80°C)",
      }),
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "pathology.storage.condition.liquidNitrogen",
        defaultMessage: "LN2 Vapor (-196°C)",
      }),
    },
    {
      id: "SLIDE_BOX",
      label: intl.formatMessage({
        id: "pathology.storage.condition.slideBox",
        defaultMessage: "Slide Box",
      }),
    },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    stored: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples, temperature logs, and storage logbook
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadTemperatureLogs();
    loadStorageLogbook();

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

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              const sampleId = String(sample.id || sample.sampleItemId);

              // Get storage fields from sample data
              const storageLocation = sample.data?.storageLocation || null;
              const storageCondition = sample.data?.storageCondition || null;
              const boxId = sample.data?.boxId || null;
              const wellCoordinate = sample.data?.wellCoordinate || null;
              const storagePath = sample.data?.storagePath || null;
              const storageBox = sample.data?.storageBox || null;
              const storageRoom = sample.data?.storageRoom || null;
              const storageRack = sample.data?.storageRack || null;
              const storageShelf = sample.data?.storageShelf || null;

              // Determine if sample has storage assignment
              // Check multiple sources for storage info
              const hasStorageAssignment = !!(
                storageLocation ||
                storagePath ||
                storageBox ||
                boxId ||
                wellCoordinate
              );

              // Determine status
              let status = sample.pageStatus || "PENDING";
              if (hasStorageAssignment && status === "PENDING") {
                status = "IN_PROGRESS";
              }

              return {
                id: sampleId,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: status,
                storageLocation: storageLocation,
                storageCondition: storageCondition,
                storagePath: storagePath,
                storageBox: storageBox,
                storageRoom: storageRoom,
                storageRack: storageRack,
                storageShelf: storageShelf,
                retentionExpiry: sample.data?.retentionExpiry || null,
                boxId: boxId,
                wellCoordinate: wellCoordinate,
                hasStorageAssignment: hasStorageAssignment,
                data: sample.data,
              };
            });

            setSamples(transformedSamples);

            // Calculate summary
            const stored = transformedSamples.filter(
              (s) => s.hasStorageAssignment || s.status === "COMPLETED",
            ).length;
            setStorageSummary({
              pending: transformedSamples.length - stored,
              stored: stored,
              total: transformedSamples.length,
            });
          } else {
            setSamples([]);
            setStorageSummary({ pending: 0, stored: 0, total: 0 });
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load temperature logs - uses entry ID for notebook-entry endpoint
  const loadTemperatureLogs = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setTemperatureLogs(response);
        }
      },
    );
  }, [entryId]);

  // Load storage logbook entries
  const loadStorageLogbook = useCallback(() => {
    if (!pageData?.id) return;

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/storage-logbook`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setStorageLogbook(response);
        }
      },
    );
  }, [pageData?.id]);

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
  }, []);

  // Handle box layout loaded from StorageHierarchySelector
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    // Check if any selected samples already have storage assignments
    const alreadyAssigned = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.storageLocation,
    );

    if (alreadyAssigned.length > 0) {
      setSamplesToReassign(alreadyAssigned);
      setConfirmReassignModalOpen(true);
      return;
    }

    openStorageAssignmentModal(false);
  };

  // Open the storage assignment modal
  const openStorageAssignmentModal = (reassigning) => {
    setIsReassignment(reassigning);
    setStorageModalOpen(true);
    setError(null);
    // Keep storage selection from main page
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setExpectedDuration("");
    setDateStored(new Date().toISOString().split("T")[0]);
  };

  // Handle confirmation of reassignment
  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    const rows = storageSelection.box.rows || 8;
    const columns = storageSelection.box.columns || 12;
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

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "pathology.storage.notEnoughWells",
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
            id: "pathology.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        // Well is occupied by existing sample
        setError(
          intl.formatMessage(
            {
              id: "pathology.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied by {sample}. Choose another position.",
            },
            { well: wellCoord, sample: wellInfo.externalId || "a sample" },
          ),
        );
        return;
      }

      if (storageModalOpen) {
        // Single well assignment during modal
        const unassignedSamples = selectedSampleIds.filter(
          (id) => !wellAssignments[id],
        );
        if (unassignedSamples.length > 0) {
          setWellAssignments((prev) => ({
            ...prev,
            [unassignedSamples[0]]: wellCoord,
          }));
        }
      } else {
        // Quick assignment outside modal - open modal if samples selected
        if (selectedSampleIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "pathology.storage.selectSamplesFirst",
              defaultMessage:
                "Please select samples to assign to storage first.",
            }),
          );
          return;
        }
        setStorageModalOpen(true);
      }
    },
    [selectedSampleIds, wellAssignments, storageModalOpen, intl],
  );

  // Build combined layout for visualization
  const getCombinedLayout = () => {
    const combined = { ...boxLayout };

    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sample?.accessionNumber || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    // Build well assignments with string keys (backend expects Map<String, String>)
    const wellAssignmentsForBackend = {};
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      wellAssignmentsForBackend[sampleId] = wellCoord;
    });

    // notebookId is the actual notebook ID - do not use entryId as fallback
    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.error.noNotebookId",
          defaultMessage: "Notebook ID not available. Cannot assign storage.",
        }),
      );
      setAssigning(false);
      return;
    }
    // Use sampleIdsString to support composite sample IDs (e.g., "4_cassette_0_block_0")
    const payload = {
      sampleIdsString: Object.keys(wellAssignments).map((id) => String(id)),
      boxId: storageSelection.box.id,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      retentionYears: retentionYears,
      reassign: isReassignment,
      pageId: pageData?.id,
      dateStored: dateStored,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/assign-storage`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          const messageId = isReassignment
            ? "pathology.storage.reassignSuccess"
            : "pathology.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccess(
            intl.formatMessage(
              {
                id: messageId,
                defaultMessage: defaultMessage,
              },
              {
                count:
                  response.assignedCount || Object.keys(wellAssignments).length,
              },
            ),
          );
          setIsReassignment(false);
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          // Reload box layout
          if (storageSelection.box && notebookId) {
            getFromOpenElisServer(
              `/rest/notebook/${notebookId}/box/${storageSelection.box.id}/layout`,
              (layoutResponse) => {
                if (componentMounted.current && layoutResponse) {
                  setBoxLayout(layoutResponse.wells || {});
                }
              },
            );
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle mark complete - marks samples complete and advances them to Disposal & Archiving page
  const handleMarkComplete = () => {
    // Find samples with storage that aren't already completed
    const pendingSamples = samples.filter(
      (s) => s.status !== "COMPLETED" && s.hasStorageAssignment,
    );

    if (pendingSamples.length === 0) {
      // Provide more specific feedback
      const samplesWithStorage = samples.filter((s) => s.hasStorageAssignment);
      if (samplesWithStorage.length === 0) {
        setError(
          intl.formatMessage({
            id: "pathology.storage.noStoredSamples",
            defaultMessage:
              "No stored samples to mark complete. Assign storage first.",
          }),
        );
      } else {
        setError(
          intl.formatMessage({
            id: "pathology.storage.allAlreadyComplete",
            defaultMessage: "All stored samples are already marked complete.",
          }),
        );
      }
      return;
    }

    setAssigning(true);
    setError(null);

    // Use string IDs for composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
    const sampleIds = pendingSamples.map((s) => String(s.id));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status-string`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        if (response && response.success) {
          // Now advance samples to Disposal & Archiving page (page 11)
          // Storage is page 9, Disposal is page 11
          // Note: notebookId is the actual notebook ID, entryId is the notebook entry ID
          // We must use notebookId for the advance API, not entryId
          if (!notebookId) {
            console.warn(
              "notebookId not available, cannot advance samples to Disposal page",
            );
            setSuccess(
              intl.formatMessage(
                {
                  id: "pathology.storage.completeSuccess",
                  defaultMessage:
                    "Successfully marked {count} samples as complete.",
                },
                { count: response.updatedCount || pendingSamples.length },
              ),
            );
            loadPageSamples();
            setAssigning(false);
            return;
          }
          // Use string endpoint to preserve composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
          postToOpenElisServerJsonResponse(
            `/rest/notebook/${notebookId}/samples/advance-string`,
            JSON.stringify({
              sampleIds: sampleIds, // Keep full composite IDs as strings
              fromPageId: pageData.id,
              toPageIndex: 11, // Disposal & Archiving page
            }),
            (advanceResponse) => {
              setAssigning(false);

              if (advanceResponse && advanceResponse.success) {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "pathology.storage.completeAndAdvanceSuccess",
                      defaultMessage:
                        "Successfully completed {count} samples and advanced to Disposal & Archiving.",
                    },
                    { count: response.updatedCount || pendingSamples.length },
                  ),
                );
              } else {
                // Samples were marked complete but advance failed - still show partial success
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "pathology.storage.completeSuccess",
                      defaultMessage:
                        "Successfully marked {count} samples as complete.",
                    },
                    { count: response.updatedCount || pendingSamples.length },
                  ),
                );
                console.warn(
                  "Failed to advance samples to Disposal page:",
                  advanceResponse?.error,
                );
              }

              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            },
          );
        } else {
          setAssigning(false);
          setError(response?.error || "Failed to mark samples complete.");
        }
      },
    );
  };

  // Temperature log handlers
  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const handleSubmitTempLog = () => {
    if (submitting) return;

    // Validate required fields
    if (!tempLogData.storageUnit || !tempLogData.checkDate) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.tempLog.error.required",
          defaultMessage: "Please enter Storage Unit and Check Date",
        }),
      );
      return;
    }

    // Temperature is required (0 is a valid reading)
    if (tempLogData.temperature === null || tempLogData.temperature === "") {
      setError(
        intl.formatMessage({
          id: "pathology.storage.tempLog.error.noTemp",
          defaultMessage: "Please enter a temperature reading",
        }),
      );
      return;
    }

    // Temperature logs use the entry ID for notebook-entry endpoint
    if (!entryId) {
      setError("Notebook entry not found");
      return;
    }

    setSubmitting(true);

    // Build checkedDateTime from checkDate and checkTime
    const checkedDateTime = `${tempLogData.checkDate}T${tempLogData.checkTime || "00:00"}`;

    // Use the common temperature log endpoint (like Pharma)
    postToOpenElisServerJsonResponse(
      `/rest/notebook-entry/${entryId}/temperature-logs`,
      JSON.stringify({
        freezerId: tempLogData.storageUnit, // API expects freezerId
        checkTime: tempLogData.checkTime || "AM",
        temperatureValue: parseFloat(tempLogData.temperature),
        temperatureUnit: "C",
        checkedBy: tempLogData.checkedBy,
        checkedDateTime: checkedDateTime,
        notes: tempLogData.notes,
      }),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setTempLogModalOpen(false);
          // Reset form
          setTempLogData({
            storageUnit: "",
            deviceType: "",
            temperature: 0,
            checkTime: new Date().toTimeString().slice(0, 5),
            checkedBy: "",
            checkDate: new Date().toISOString().split("T")[0],
            notes: "",
          });
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
          loadTemperatureLogs();
        } else {
          setError(
            response?.error || "Failed to log temperature. Please try again.",
          );
        }
      },
    );
  };

  // Retrieval handlers
  const openRetrievalModal = (sample) => {
    setSelectedSampleForRetrieval(sample);
    setRetrievalData({
      dateRetrieved: new Date().toISOString().split("T")[0],
      retrievedBy: "",
      recipientSignature: "",
      purpose: "",
    });
    setRetrievalModalOpen(true);
  };

  const handleSubmitRetrieval = () => {
    if (submitting) return;
    if (!retrievalData.retrievedBy || !retrievalData.recipientSignature) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.retrieval.error.required",
          defaultMessage: "Please fill in Retrieved By and Recipient Signature",
        }),
      );
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/retrieve`,
      JSON.stringify({
        sampleId: selectedSampleForRetrieval?.id,
        pageId: pageData?.id,
        dateRetrieved: retrievalData.dateRetrieved,
        retrievedBy: retrievalData.retrievedBy,
        recipientSignature: retrievalData.recipientSignature,
        purpose: retrievalData.purpose,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setRetrievalModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.retrievalSuccess",
              defaultMessage: "Sample retrieval recorded successfully.",
            }),
          );
          loadPageSamples();
          loadStorageLogbook();
          onProgressUpdate?.();
        } else {
          setError("Failed to record retrieval. Please try again.");
        }
      },
    );
  };

  // Disposal handlers
  const openDisposalModal = (sample) => {
    setSelectedSampleForDisposal(sample);
    setDisposalData({
      disposalDate: new Date().toISOString().split("T")[0],
      disposalMethod: "",
      disposedBy: "",
      disposalNotes: "",
    });
    setDisposalModalOpen(true);
  };

  const handleSubmitDisposal = () => {
    if (submitting) return;
    if (!disposalData.disposedBy || !disposalData.disposalMethod) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.disposal.error.required",
          defaultMessage: "Please fill in Disposed By and Disposal Method",
        }),
      );
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/dispose`,
      JSON.stringify({
        sampleId: selectedSampleForDisposal?.id,
        pageId: pageData?.id,
        ...disposalData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setDisposalModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.disposalSuccess",
              defaultMessage:
                "Sample disposal recorded successfully. Logbook entry crossed out.",
            }),
          );
          loadPageSamples();
          loadStorageLogbook();
          onProgressUpdate?.();
        } else {
          setError("Failed to record disposal. Please try again.");
        }
      },
    );
  };

  // Format exact storage location string
  const formatExactLocation = (sample) => {
    if (!sample) return "-";

    const parts = [];
    if (sample.data?.storageRoom) parts.push(sample.data.storageRoom);
    if (sample.data?.storageDevice) parts.push(sample.data.storageDevice);
    if (sample.data?.storageShelf)
      parts.push(`Shelf ${sample.data.storageShelf}`);
    if (sample.data?.storageRack) parts.push(`Rack ${sample.data.storageRack}`);
    if (sample.data?.storageBox) parts.push(`Box ${sample.data.storageBox}`);
    if (sample.wellCoordinate) parts.push(`Position ${sample.wellCoordinate}`);

    return parts.length > 0 ? parts.join(", ") : sample.storageLocation || "-";
  };

  // Render storage status tag
  const renderStorageTag = (sample) => {
    if (!sample) return null;
    if (sample.status === "COMPLETED" && sample.storageLocation) {
      return (
        <Tag type="green" renderIcon={Checkmark}>
          {sample.storageLocation}
        </Tag>
      );
    }
    if (sample.storageLocation) {
      return (
        <Tag type="cyan" renderIcon={Archive}>
          {sample.storageLocation}
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="notebook.status.pending"
          defaultMessage="Pending"
        />
      </Tag>
    );
  };

  // Render condition tag
  const renderConditionTag = (sample) => {
    if (!sample || !sample.storageCondition) return null;

    const conditionLabels = {
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      ROOM_TEMP: "15-25°C",
      LIQUID_NITROGEN: "-196°C",
      SLIDE_BOX: "Slide Box",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Handle auto-assign
  const handleAutoAssign = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to auto-assign.",
        }),
      );
      return;
    }

    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.pageNotInit",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setIsAutoAssigning(true);
    setError(null);

    // Build storage path
    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    // Get list of occupied wells from current box layout
    const occupiedWells = Object.keys(boxLayout);

    // Use sampleIdsString for composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
    const autoAssignData = {
      sampleIdsString: selectedSampleIds.map((id) => String(id)),
      data: {
        storageRoom: storageSelection.room?.label,
        storageDevice: storageSelection.device?.label,
        storageType: autoAssignValues.storageType,
        storageRack: storageSelection.rack?.label,
        storageBox: storageSelection.box?.label,
        storagePath: storagePath,
        assignedBy: autoAssignValues.assignedBy,
        assignedDateTime: autoAssignValues.assignedDateTime,
        notes: autoAssignValues.notes,
      },
      boxId: storageSelection.box?.id,
      rows: storageSelection.box?.rows || 8,
      columns: storageSelection.box?.columns || 12,
      occupiedWells: occupiedWells,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage/auto-assign`,
      JSON.stringify(autoAssignData),
      (status, response) => {
        setIsAutoAssigning(false);
        if (status === 200) {
          const responseData =
            typeof response === "string" ? JSON.parse(response) : response;
          const assignmentCount = responseData?.updatedCount || 0;

          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.storage.autoAssignSuccess",
                defaultMessage:
                  "Auto-assigned {count} sample(s) to storage in {box}.",
              },
              {
                count: assignmentCount,
                box: storageSelection.box?.label,
              },
            ),
          );
          setAutoAssignModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          // Reload box layout - uses notebook ID for notebook endpoint
          if (storageSelection.box && notebookId) {
            getFromOpenElisServer(
              `/rest/notebook/${notebookId}/box/${storageSelection.box.id}/layout`,
              (layoutResponse) => {
                if (componentMounted.current && layoutResponse) {
                  setBoxLayout(layoutResponse.wells || {});
                }
              },
            );
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorData =
            typeof response === "string" ? JSON.parse(response) : response;
          setError(
            errorData?.error ||
              intl.formatMessage({
                id: "pathology.storage.autoAssignError",
                defaultMessage:
                  "Failed to auto-assign storage. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    storageSelection,
    autoAssignValues,
    boxLayout,
    entryId,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Get temperature status tag for logs
  const getTemperatureStatusTag = (log) => {
    // Define acceptable ranges per device type
    const ranges = {
      Refrigerator: { min: 2, max: 8 },
      "Freezer-20": { min: -25, max: -15 },
      "Freezer-80": { min: -85, max: -75 },
      LN2Tank: { min: -200, max: -180 },
      Incubator: { min: 35, max: 38 },
      StabilityRoom: { min: 20, max: 25 },
      ColdRoom: { min: 2, max: 8 },
      ROOM_TEMP: { min: 15, max: 25 },
      REFRIGERATED: { min: 2, max: 8 },
      FROZEN_MINUS20: { min: -25, max: -15 },
      FROZEN_MINUS80: { min: -85, max: -75 },
      LIQUID_NITROGEN: { min: -200, max: -180 },
    };

    const range = ranges[log.deviceType] ||
      ranges[log.storageCondition] || { min: -999, max: 999 };
    const temp =
      log.temperatureValue || log.temperatureCheckAM || log.temperatureCheckPM;
    const inRange = temp >= range.min && temp <= range.max;

    return inRange ? (
      <Tag type="green" size="sm">
        {temp}°{log.temperatureUnit || "C"}
      </Tag>
    ) : (
      <Tag type="red" size="sm" renderIcon={Warning}>
        {temp}°{log.temperatureUnit || "C"} - OUT OF RANGE
      </Tag>
    );
  };

  // Build hierarchical path
  const getHierarchicalPath = () => {
    const parts = [];
    if (storageSelection.room) parts.push(storageSelection.room.label);
    if (storageSelection.device) parts.push(storageSelection.device.label);
    if (storageSelection.shelf) parts.push(storageSelection.shelf.label);
    if (storageSelection.rack) parts.push(storageSelection.rack.label);
    if (storageSelection.box) parts.push(storageSelection.box.label);
    return parts.join(" > ");
  };

  // Grid columns
  // Note: render function signature is (value, sample) where value is sample[key]
  const columns = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.column.accessionNumber",
        defaultMessage: "Sample ID",
      }),
      render: (value, sample) =>
        !sample ? null : (
          <div>
            <strong>{sample.accessionNumber || sample.externalId}</strong>
            {sample.externalId && sample.accessionNumber && (
              <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                {sample.externalId}
              </div>
            )}
          </div>
        ),
    },
    {
      key: "exactLocation",
      header: intl.formatMessage({
        id: "pathology.column.exactLocation",
        defaultMessage: "Exact Location",
      }),
      render: (value, sample) =>
        !sample ? (
          <span className="text-muted">-</span>
        ) : sample.storageLocation ? (
          <div style={{ fontSize: "0.875rem" }}>
            <div>{formatExactLocation(sample)}</div>
            {sample.wellCoordinate && (
              <Tag type="cyan" size="sm" style={{ marginTop: "0.25rem" }}>
                Position {sample.wellCoordinate}
              </Tag>
            )}
          </div>
        ) : (
          <Tag type="gray">
            <FormattedMessage
              id="notebook.status.notAssigned"
              defaultMessage="Not Assigned"
            />
          </Tag>
        ),
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "pathology.column.storage",
        defaultMessage: "Storage Status",
      }),
      render: (value, sample) =>
        !sample ? null : (
          <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
            {renderStorageTag(sample)}
            {renderConditionTag(sample)}
          </div>
        ),
    },
    {
      key: "dateStored",
      header: intl.formatMessage({
        id: "pathology.column.dateStored",
        defaultMessage: "Date Stored",
      }),
      render: (value, sample) =>
        !sample ? (
          <span className="text-muted">-</span>
        ) : sample.data?.dateStored ? (
          <span>{sample.data.dateStored}</span>
        ) : (
          <span className="text-muted">-</span>
        ),
    },
    {
      key: "retentionExpiry",
      header: intl.formatMessage({
        id: "pathology.column.expiry",
        defaultMessage: "Retention Expiry",
      }),
      render: (value, sample) =>
        !sample ? (
          <span className="text-muted">-</span>
        ) : sample.retentionExpiry ? (
          <span>{sample.retentionExpiry}</span>
        ) : (
          <span className="text-muted">-</span>
        ),
    },
    {
      key: "storageActions",
      header: intl.formatMessage({
        id: "pathology.column.actions",
        defaultMessage: "Actions",
      }),
      render: (value, sample) =>
        !sample ? null : sample.status === "COMPLETED" &&
          sample.storageLocation ? (
          <div style={{ display: "flex", gap: "0.25rem" }}>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Undo}
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "pathology.storage.retrieve",
                defaultMessage: "Retrieve",
              })}
              onClick={(e) => {
                e.stopPropagation();
                openRetrievalModal(sample);
              }}
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={TrashCan}
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "pathology.storage.dispose",
                defaultMessage: "Dispose",
              })}
              onClick={(e) => {
                e.stopPropagation();
                openDisposalModal(sample);
              }}
            />
          </div>
        ) : null,
    },
  ];

  return (
    <div className="pathology-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.storage.title"
            defaultMessage="Storage & Inventory Management"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.storage.description"
            defaultMessage="Track physical storage and retrieval of pathology materials. Assign locations and log environmental monitoring."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Storage Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storageSummary.stored}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.pending"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Temperature}
          onClick={() => setTempLogModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.storage.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>

        <Button
          kind="primary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="pathology.storage.assignSelected"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={storageSummary.stored === 0 || assigning || !hasRealPageId}
        >
          <FormattedMessage
            id="pathology.storage.markComplete"
            defaultMessage="Mark Complete"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="pathology.storage.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Daily Temperature Logs */}
      <div style={{ marginTop: "1.5rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <Temperature size={16} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="pathology.storage.dailyTempLogs"
            defaultMessage="Daily Temperature Logs"
          />
        </h5>
        {temperatureLogs.length > 0 ? (
          <div
            style={{
              display: "flex",
              gap: "0.5rem",
              flexWrap: "wrap",
            }}
          >
            {temperatureLogs.map((log, index) => {
              // Get temperature value - API returns temperatureValue
              const tempValue = log.temperatureValue ?? log.temperature ?? null;
              // Get device name - API returns freezerId
              const deviceName =
                log.freezerId || log.storageUnit || log.deviceId || "Unknown";
              // Format date from checkedDateTime
              const displayDate = log.checkedDateTime
                ? new Date(log.checkedDateTime).toLocaleDateString()
                : log.checkDate || "";

              return (
                <Tile
                  key={log.id || index}
                  style={{
                    padding: "0.5rem 0.75rem",
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.25rem",
                    minWidth: "160px",
                  }}
                >
                  <strong style={{ fontSize: "0.875rem" }}>{deviceName}</strong>
                  <div
                    style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}
                  >
                    {tempValue != null && (
                      <Tag type="blue" size="sm">
                        {log.checkTime || "—"}: {tempValue}°
                        {log.temperatureUnit || "C"}
                      </Tag>
                    )}
                  </div>
                  <span style={{ fontSize: "0.7rem", color: "#8d8d8d" }}>
                    {displayDate} | {log.checkedBy || "-"}
                  </span>
                </Tile>
              );
            })}
          </div>
        ) : (
          <Tile style={{ padding: "1rem", color: "#525252" }}>
            <FormattedMessage
              id="pathology.storage.noTempLogs"
              defaultMessage="No temperature logs recorded. Use the 'Log Temperature' button to add entries."
            />
          </Tile>
        )}
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container" style={{ marginTop: "1.5rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="pathology.storage.sampleList"
            defaultMessage="Sample List"
          />
        </h5>
        <SampleGrid
          samples={samples}
          loading={loading}
          columns={columns}
          onSelectionChange={handleSelectionChange}
          selectedIds={selectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          emptyStateMessage={intl.formatMessage({
            id: "pathology.storage.noSamples",
            defaultMessage: "No samples available for storage assignment.",
          })}
        />
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "pathology.storage.modal.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !storageSelection.box ||
          !selectedCondition ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.storage.modal.description"
              defaultMessage="Assign {count} selected samples to storage."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Storage Location Selector */}
          <div>
            <h5 style={{ marginBottom: "0.5rem" }}>
              <Location size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="pathology.storage.selectLocation"
                defaultMessage="Select Storage Location"
              />
            </h5>
            <StorageHierarchySelector
              onSelectionChange={handleStorageSelectionChange}
              entryId={notebookId}
              onBoxLayoutLoaded={handleBoxLayoutLoaded}
              boxRequired={true}
              showPath={true}
            />
          </div>

          {/* Hierarchical Path Display */}
          {getHierarchicalPath() && (
            <div
              style={{
                backgroundColor: "#f4f4f4",
                padding: "0.75rem 1rem",
                borderRadius: "4px",
              }}
            >
              <strong>
                <FormattedMessage
                  id="pathology.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
            </div>
          )}

          {/* Box Layout Viewer */}
          {storageSelection.box && (
            <div>
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
                    id="pathology.storage.boxLayout"
                    defaultMessage="Box Layout"
                  />
                </h5>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Automatic}
                  onClick={handleAutoPopulate}
                  disabled={selectedSampleIds.length === 0}
                >
                  <FormattedMessage
                    id="pathology.storage.autoPopulate"
                    defaultMessage="Auto-Populate"
                  />
                </Button>
              </div>

              <BoxLayoutViewer
                boxId={storageSelection.box.id}
                layout={getCombinedLayout()}
                rows={storageSelection.box.rows || 8}
                columns={storageSelection.box.columns || 12}
                onWellClick={handleWellClick}
              />

              {/* Assignment Summary */}
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="pathology.storage.assignmentSummary"
                  defaultMessage="{assigned} of {total} samples assigned to wells"
                  values={{
                    assigned: Object.keys(wellAssignments).length,
                    total: selectedSampleIds.length,
                  }}
                />
              </div>
            </div>
          )}

          {/* Date Stored */}
          <DatePicker
            datePickerType="single"
            value={dateStored}
            onChange={(dates) => {
              if (dates?.[0]) {
                setDateStored(dates[0].toISOString().split("T")[0]);
              }
            }}
          >
            <DatePickerInput
              id="dateStored"
              labelText={intl.formatMessage({
                id: "pathology.storage.dateStored",
                defaultMessage: "Date Stored *",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          {/* Storage Condition Selector */}
          <Dropdown
            id="storage-condition-dropdown"
            titleText={intl.formatMessage({
              id: "pathology.storage.condition",
              defaultMessage: "Storage Condition",
            })}
            label={intl.formatMessage({
              id: "pathology.storage.selectCondition",
              defaultMessage: "Select condition...",
            })}
            items={storageConditionOptions}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedCondition}
            onChange={({ selectedItem }) => setSelectedCondition(selectedItem)}
          />

          {/* Retention Period */}
          <NumberInput
            id="retention-years"
            label={intl.formatMessage({
              id: "pathology.storage.retentionYears",
              defaultMessage: "Retention Period (Years)",
            })}
            value={retentionYears}
            min={1}
            max={30}
            step={1}
            onChange={(e, { value }) => setRetentionYears(value)}
            helperText={intl.formatMessage(
              {
                id: "pathology.storage.expiryDate",
                defaultMessage: "Expiry date will be: {date}",
              },
              {
                date: new Date(
                  Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                ).toLocaleDateString(),
              },
            )}
          />

          {/* Expected Duration (pathology-specific) */}
          <TextInput
            id="expectedDuration"
            labelText={intl.formatMessage({
              id: "pathology.storage.expectedDuration",
              defaultMessage: "Expected Duration (Optional)",
            })}
            value={expectedDuration}
            onChange={(e) => setExpectedDuration(e.target.value)}
            placeholder={intl.formatMessage({
              id: "pathology.storage.expectedDurationPlaceholder",
              defaultMessage: "e.g., 6 months, 1 year",
            })}
          />
        </div>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.confirm",
          defaultMessage: "Reassign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleConfirmReassignment}
        danger
        size="sm"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.storage.reassignConfirm.warning"
              defaultMessage="{count} of the selected samples already have storage assignments. Reassigning will remove them from their current locations."
              values={{ count: samplesToReassign.length }}
            />
          </p>

          <div
            style={{
              backgroundColor: "#fff1f1",
              border: "1px solid #da1e28",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <strong style={{ color: "#da1e28" }}>
              <FormattedMessage
                id="pathology.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>{sample.accessionNumber || sample.externalId}</strong>
                  : {sample.storageLocation}
                  {sample.storageCondition && ` (${sample.storageCondition})`}
                </li>
              ))}
            </ul>
          </div>

          <p style={{ fontStyle: "italic", color: "#525252" }}>
            <FormattedMessage
              id="pathology.storage.reassignConfirm.proceed"
              defaultMessage="Do you want to proceed with reassigning these samples to a new storage location?"
            />
          </p>
        </div>
      </Modal>

      {/* Temperature Log Modal */}
      <Modal
        open={tempLogModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.tempLog.title",
          defaultMessage: "Log Temperature Check",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setTempLogModalOpen(false)}
        onRequestSubmit={handleSubmitTempLog}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <p style={{ marginBottom: "1rem", color: "#525252" }}>
          <FormattedMessage
            id="pathology.storage.tempLog.description"
            defaultMessage="Record manual temperature check for storage units."
          />
        </p>

        <Grid fullWidth>
          {/* Storage Unit & Device Type */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tempStorageUnit"
              name="storageUnit"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.storageUnit",
                defaultMessage: "Storage Unit *",
              })}
              value={tempLogData.storageUnit}
              onChange={(e) => handleInputChange(e, setTempLogData)}
              placeholder={intl.formatMessage({
                id: "pathology.storage.tempLog.storageUnitPlaceholder",
                defaultMessage: "e.g., Freezer 3, Refrigerator A",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Select
              id="tempDeviceType"
              name="deviceType"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.deviceType",
                defaultMessage: "Device Type",
              })}
              value={tempLogData.deviceType}
              onChange={(e) =>
                setTempLogData((prev) => ({
                  ...prev,
                  deviceType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select..." />
              <SelectItem value="Refrigerator" text="Refrigerator (2-8°C)" />
              <SelectItem value="Freezer-20" text="Freezer (-20°C)" />
              <SelectItem value="Freezer-80" text="Freezer (-80°C)" />
              <SelectItem value="LN2Tank" text="LN2 Tank (-196°C)" />
              <SelectItem value="Incubator" text="Incubator (35-38°C)" />
              <SelectItem value="ColdRoom" text="Cold Room (2-8°C)" />
            </Select>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              value={tempLogData.checkDate}
              onChange={(dates) =>
                handleDateChange(dates, "checkDate", setTempLogData)
              }
            >
              <DatePickerInput
                id="checkDate"
                labelText={intl.formatMessage({
                  id: "pathology.storage.tempLog.checkDate",
                  defaultMessage: "Check Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          {/* Temperature Reading */}
          <Column lg={4} md={4} sm={4}>
            <NumberInput
              id="temperature"
              label={intl.formatMessage({
                id: "pathology.storage.tempLog.temperature",
                defaultMessage: "Temperature (°C) *",
              })}
              value={tempLogData.temperature ?? 0}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperature: typeof value === "number" ? value : 0,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="checkTime"
              name="checkTime"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.checkTime",
                defaultMessage: "Check Time",
              })}
              value={tempLogData.checkTime}
              onChange={(e) => handleInputChange(e, setTempLogData)}
              placeholder="HH:MM"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="checkedBy"
              name="checkedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.staffInitials",
                defaultMessage: "Staff Initials",
              })}
              value={tempLogData.checkedBy}
              onChange={(e) => handleInputChange(e, setTempLogData)}
              placeholder={intl.formatMessage({
                id: "pathology.storage.tempLog.initialsPlaceholder",
                defaultMessage: "e.g., JD",
              })}
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="tempNotes"
              name="notes"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.notes",
                defaultMessage: "Notes",
              })}
              value={tempLogData.notes}
              onChange={(e) => handleInputChange(e, setTempLogData)}
              rows={2}
              placeholder={intl.formatMessage({
                id: "pathology.storage.tempLog.notesPlaceholder",
                defaultMessage:
                  "Any observations, anomalies, or corrective actions...",
              })}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Auto-Assign Modal */}
      <Modal
        open={autoAssignModalOpen}
        onRequestClose={() => setAutoAssignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.autoAssign.title",
          defaultMessage: "Auto-Assign Storage Locations",
        })}
        primaryButtonText={
          isAutoAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "label.autoAssign",
                defaultMessage: "Auto-Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAutoAssign}
        onSecondarySubmit={() => setAutoAssignModalOpen(false)}
        size="md"
        primaryButtonDisabled={isAutoAssigning}
      >
        <p className="modal-description">
          <FormattedMessage
            id="pathology.storage.autoAssign.description"
            defaultMessage="Automatically assign {count} sample(s) to the next available wells in {box}, starting from position A1."
            values={{
              count: selectedSampleIds.length,
              box: storageSelection.box?.label || "selected box",
            }}
          />
        </p>

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                backgroundColor: "#f4f4f4",
                padding: "1rem",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <strong>
                <FormattedMessage
                  id="pathology.storage.path"
                  defaultMessage="Storage Path:"
                />
              </strong>{" "}
              {getHierarchicalPath()}
              <div style={{ marginTop: "0.5rem" }}>
                <Tag type="blue">
                  <FormattedMessage
                    id="pathology.storage.availableWells"
                    defaultMessage="{available} wells available"
                    values={{
                      available:
                        (storageSelection.box?.rows || 8) *
                          (storageSelection.box?.columns || 12) -
                        Object.keys(boxLayout).length,
                    }}
                  />
                </Tag>
              </div>
            </div>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="autoAssignStorageType"
              labelText={intl.formatMessage({
                id: "pathology.storage.storageType",
                defaultMessage: "Storage Type",
              })}
              value={autoAssignValues.storageType}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  storageType: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select type..." />
              <SelectItem value="Block" text="Tissue Block" />
              <SelectItem value="Slide" text="Slide" />
              <SelectItem value="Cassette" text="Cassette" />
              <SelectItem value="Sample" text="Sample" />
              <SelectItem value="Archive" text="Archive" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="autoAssignAssignedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.assignedBy",
                defaultMessage: "Assigned By",
              })}
              value={autoAssignValues.assignedBy}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  assignedBy: e.target.value,
                }))
              }
              placeholder="Enter staff name"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="autoAssignNotes"
              labelText={intl.formatMessage({
                id: "pathology.storage.notes",
                defaultMessage: "Notes",
              })}
              value={autoAssignValues.notes}
              onChange={(e) =>
                setAutoAssignValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Optional notes..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Retrieval Modal */}
      <Modal
        open={retrievalModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.storage.retrieval.title",
            defaultMessage: "Retrieve Sample - {accession}",
          },
          {
            accession:
              selectedSampleForRetrieval?.accessionNumber ||
              selectedSampleForRetrieval?.externalId ||
              "",
          },
        )}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.retrieval.submit",
          defaultMessage: "Record Retrieval",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setRetrievalModalOpen(false)}
        onRequestSubmit={handleSubmitRetrieval}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "dateRetrieved", setRetrievalData)
              }
            >
              <DatePickerInput
                id="dateRetrieved"
                labelText={intl.formatMessage({
                  id: "pathology.storage.retrieval.dateRetrieved",
                  defaultMessage: "Date Retrieved *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="retrievedBy"
              name="retrievedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.retrievedBy",
                defaultMessage: "Retrieved By *",
              })}
              value={retrievalData.retrievedBy}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="recipientSignature"
              name="recipientSignature"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.recipientSignature",
                defaultMessage: "Recipient Signature *",
              })}
              value={retrievalData.recipientSignature}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="retrievalPurpose"
              name="purpose"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.purpose",
                defaultMessage: "Purpose of Retrieval",
              })}
              value={retrievalData.purpose}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
              placeholder={intl.formatMessage({
                id: "pathology.storage.retrieval.purposePlaceholder",
                defaultMessage:
                  "e.g., Testing, Additional analysis, External review",
              })}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.storage.disposal.title",
            defaultMessage: "Dispose Sample - {accession}",
          },
          {
            accession:
              selectedSampleForDisposal?.accessionNumber ||
              selectedSampleForDisposal?.externalId ||
              "",
          },
        )}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.disposal.submit",
          defaultMessage: "Record Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setDisposalModalOpen(false)}
        onRequestSubmit={handleSubmitDisposal}
        primaryButtonDisabled={submitting}
        danger
      >
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "pathology.storage.disposal.warning",
            defaultMessage:
              "This action will mark the sample as disposed and cross out the logbook entry.",
          })}
          hideCloseButton
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "disposalDate", setDisposalData)
              }
            >
              <DatePickerInput
                id="disposalDate"
                labelText={intl.formatMessage({
                  id: "pathology.storage.disposal.date",
                  defaultMessage: "Disposal Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="disposalMethod"
              name="disposalMethod"
              labelText={intl.formatMessage({
                id: "pathology.storage.disposal.method",
                defaultMessage: "Disposal Method *",
              })}
              value={disposalData.disposalMethod}
              onChange={(e) =>
                setDisposalData((prev) => ({
                  ...prev,
                  disposalMethod: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="" />
              <SelectItem value="INCINERATION" text="Incineration" />
              <SelectItem value="AUTOCLAVE" text="Autoclave" />
              <SelectItem value="CHEMICAL" text="Chemical Treatment" />
              <SelectItem value="BIOHAZARD" text="Biohazard Waste" />
              <SelectItem value="OTHER" text="Other" />
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="disposedBy"
              name="disposedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.disposal.disposedBy",
                defaultMessage: "Disposed By *",
              })}
              value={disposalData.disposedBy}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="disposalNotes"
              name="disposalNotes"
              labelText={intl.formatMessage({
                id: "pathology.storage.disposal.notes",
                defaultMessage: "Disposal Notes",
              })}
              value={disposalData.disposalNotes}
              onChange={(e) => handleInputChange(e, setDisposalData)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyStorageInventoryPage;
