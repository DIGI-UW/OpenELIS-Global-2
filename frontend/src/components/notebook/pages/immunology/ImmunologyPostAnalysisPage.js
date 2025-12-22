import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Dropdown,
  NumberInput,
  Tag,
  TextInput,
  TextArea,
  Checkbox,
} from "@carbon/react";
import {
  Archive,
  Checkmark,
  Temperature,
  Renew,
  Warning,
  WarningAlt,
  Subtract,
  Location,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyPostAnalysisPage - Stage 7 of the Immunology workflow.
 * Handles post-analysis sample storage with:
 *
 * SAMPLE STORAGE:
 * - Store processed samples under defined conditions:
 *   - Refrigerated (2-8°C for short-term)
 *   - Frozen (-20°C or -80°C for long-term)
 *   - Liquid nitrogen (for extended storage)
 *
 * TRACKING:
 * - Record post-assay storage location
 * - Update sample status (analyzed, partially used, exhausted)
 * - Calculate and record remaining volume
 * - Set retention period based on project requirements
 *
 * QUALITY FLAGS:
 * - Flag samples with:
 *   - Insufficient volume for repeat testing
 *   - Quality issues discovered during assay
 *   - Unexpected results requiring investigation
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ImmunologyPostAnalysisPage({
  entryId,
  notebookId,
  pageData,
  progress: _progress, // eslint-disable-line no-unused-vars
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage hierarchy state
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [wellAssignments, setWellAssignments] = useState({});

  // Quality flag modal state
  const [qualityFlagModalOpen, setQualityFlagModalOpen] = useState(false);
  const [qualityFlagData, setQualityFlagData] = useState({
    flagType: null,
    notes: "",
    requiresInvestigation: false,
  });

  // Volume update modal state
  const [volumeModalOpen, setVolumeModalOpen] = useState(false);
  const [volumeUpdateData, setVolumeUpdateData] = useState({
    remainingVolume: "",
    volumeUnit: "µL",
    sampleStatus: "ANALYZED",
  });

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [storageNotes, setStorageNotes] = useState("");

  // Storage condition options for immunology
  const storageConditionOptions = [
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C) - Short-term",
      }),
      tempRange: "2-8°C",
      duration: "short-term",
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.condition.frozen20",
        defaultMessage: "Frozen (-20°C) - Long-term",
      }),
      tempRange: "-20°C",
      duration: "long-term",
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.condition.frozen80",
        defaultMessage: "Ultra-Low (-80°C) - Long-term",
      }),
      tempRange: "-80°C",
      duration: "long-term",
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.condition.liquidNitrogen",
        defaultMessage: "Liquid Nitrogen (-196°C) - Extended Storage",
      }),
      tempRange: "-196°C",
      duration: "extended",
    },
  ];

  // Sample status options
  const sampleStatusOptions = [
    {
      id: "ANALYZED",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.status.analyzed",
        defaultMessage: "Analyzed - Full Volume Remaining",
      }),
    },
    {
      id: "PARTIALLY_USED",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.status.partiallyUsed",
        defaultMessage: "Partially Used - Reduced Volume",
      }),
    },
    {
      id: "EXHAUSTED",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.status.exhausted",
        defaultMessage: "Exhausted - No Volume Remaining",
      }),
    },
  ];

  // Quality flag type options
  const qualityFlagOptions = [
    {
      id: "INSUFFICIENT_VOLUME",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.insufficientVolume",
        defaultMessage: "Insufficient Volume for Repeat Testing",
      }),
      severity: "warning",
    },
    {
      id: "QUALITY_ISSUE",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.qualityIssue",
        defaultMessage: "Quality Issue Discovered During Assay",
      }),
      severity: "error",
    },
    {
      id: "UNEXPECTED_RESULTS",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.unexpectedResults",
        defaultMessage: "Unexpected Results Requiring Investigation",
      }),
      severity: "warning",
    },
    {
      id: "HEMOLYSIS",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.hemolysis",
        defaultMessage: "Hemolysis Detected",
      }),
      severity: "error",
    },
    {
      id: "LIPEMIA",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.lipemia",
        defaultMessage: "Lipemia Detected",
      }),
      severity: "warning",
    },
    {
      id: "CONTAMINATION",
      label: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.flag.contamination",
        defaultMessage: "Possible Contamination",
      }),
      severity: "error",
    },
  ];

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

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

    let samplesData = [];
    let routingData = [];
    let loadCount = 0;

    const processData = () => {
      loadCount++;
      if (loadCount < 2) return;

      if (componentMounted.current) {
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

        const transformedSamples = samplesData.map((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const routing = routingMap[sampleId];

          const storageBox =
            sample.data?.storageBox || routing?.boxName || null;
          const storageWell =
            sample.data?.storageWell ||
            sample.data?.wellCoordinate ||
            routing?.wellCoordinate ||
            null;

          let storageLocation = sample.data?.storageLocation || null;
          if (!storageLocation && (storageBox || storageWell)) {
            storageLocation = storageBox
              ? `${storageBox} - ${storageWell || ""}`
              : storageWell;
          }
          if (!storageLocation && routing) {
            storageLocation = routing.boxName
              ? `${routing.boxName} - ${routing.wellCoordinate}`
              : routing.wellCoordinate;
          }

          const hasStorageAssignment = !!(
            storageLocation ||
            storageBox ||
            storageWell ||
            routing?.hasRouting
          );

          let status = sample.pageStatus || "PENDING";
          if (hasStorageAssignment && status === "PENDING") {
            status = "IN_PROGRESS";
          }

          return {
            id: sampleId,
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            collectionDate: sample.collectionDate,
            status: status,
            // Storage info
            storageLocation: storageLocation,
            storageBox: storageBox,
            storageWell: storageWell,
            storageCondition: sample.data?.storageCondition || null,
            retentionExpiry: sample.data?.retentionExpiry || null,
            hasStorageAssignment: hasStorageAssignment,
            // Post-analysis tracking
            sampleStatus: sample.data?.sampleStatus || "ANALYZED",
            remainingVolume: sample.data?.remainingVolume || null,
            volumeUnit: sample.data?.volumeUnit || "µL",
            // Quality flags
            qualityFlags: sample.data?.qualityFlags || [],
            requiresInvestigation: sample.data?.requiresInvestigation || false,
            qualityNotes: sample.data?.qualityNotes || "",
            // Original data
            data: sample.data,
          };
        });

        setSamples(transformedSamples);
        setLoading(false);
      }
    };

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          samplesData = response;
        }
        processData();
      },
    );

    const nbId = notebookId || entryId;
    if (nbId) {
      getFromOpenElisServer(
        `/rest/notebook/${nbId}/routing?destinationType=STORAGE`,
        (response) => {
          if (response && Array.isArray(response)) {
            routingData = response;
          }
          processData();
        },
      );
    } else {
      processData();
    }
  }, [pageData?.id, entryId, notebookId]);

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Count samples with quality flags
  const flaggedSamplesCount = useMemo(
    () =>
      samples.filter((s) => s.qualityFlags && s.qualityFlags.length > 0).length,
    [samples],
  );

  // Count samples pending storage
  const pendingStorageCount = useMemo(
    () => samples.filter((s) => !s.hasStorageAssignment).length,
    [samples],
  );

  // Count samples with storage assigned
  const storedCount = useMemo(
    () => samples.filter((s) => s.hasStorageAssignment).length,
    [samples],
  );

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
  }, []);

  // Handle box layout loaded
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle well click
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        setError(
          intl.formatMessage(
            {
              id: "notebook.immunology.postAnalysis.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied. Choose another position.",
            },
            { well: wellCoord },
          ),
        );
        return;
      }

      const unassignedSamples = selectedSampleIds.filter(
        (id) => !wellAssignments[id],
      );
      if (unassignedSamples.length > 0) {
        setWellAssignments((prev) => ({
          ...prev,
          [unassignedSamples[0]]: wellCoord,
        }));
      }
    },
    [selectedSampleIds, wellAssignments, intl],
  );

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.selectBoxFirst",
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
            id: "notebook.immunology.postAnalysis.notEnoughWells",
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
            id: "notebook.immunology.postAnalysis.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Build combined layout for visualization
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

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    setStorageModalOpen(true);
    setError(null);
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setStorageNotes("");
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const wellAssignmentsForBackend = {};
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      wellAssignmentsForBackend[sampleId] = wellCoord;
    });

    const nbId = notebookId || entryId;
    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: storageSelection.box.id,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      retentionYears: retentionYears,
      storageNotes: storageNotes,
      postAnalysisStorage: true,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${nbId}/samples/assign-storage`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.immunology.postAnalysis.assignSuccess",
                defaultMessage:
                  "Successfully assigned {count} samples to post-analysis storage.",
              },
              {
                count:
                  response.assignedCount || Object.keys(wellAssignments).length,
              },
            ),
          );
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.immunology.postAnalysis.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle open quality flag modal
  const handleOpenQualityFlagModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.noSamplesForFlag",
          defaultMessage: "Please select samples to add quality flags.",
        }),
      );
      return;
    }

    setQualityFlagModalOpen(true);
    setError(null);
    setQualityFlagData({
      flagType: null,
      notes: "",
      requiresInvestigation: false,
    });
  };

  // Handle add quality flag
  const handleAddQualityFlag = () => {
    if (!qualityFlagData.flagType) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.selectFlagType",
          defaultMessage: "Please select a quality flag type.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const nbId = notebookId || entryId;
    const payload = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      flagType: qualityFlagData.flagType.id,
      notes: qualityFlagData.notes,
      requiresInvestigation: qualityFlagData.requiresInvestigation,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${nbId}/samples/quality-flag`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.immunology.postAnalysis.flagSuccess",
                defaultMessage:
                  "Successfully added quality flag to {count} samples.",
              },
              { count: response.updatedCount || selectedSampleIds.length },
            ),
          );
          setQualityFlagModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.immunology.postAnalysis.flagError",
                defaultMessage: "Failed to add quality flag.",
              }),
          );
        }
      },
    );
  };

  // Handle open volume update modal
  const handleOpenVolumeModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.noSamplesForVolume",
          defaultMessage: "Please select samples to update volume/status.",
        }),
      );
      return;
    }

    setVolumeModalOpen(true);
    setError(null);
    setVolumeUpdateData({
      remainingVolume: "",
      volumeUnit: "µL",
      sampleStatus: "ANALYZED",
    });
  };

  // Handle update volume/status
  const handleUpdateVolume = () => {
    setAssigning(true);
    setError(null);

    const nbId = notebookId || entryId;
    const payload = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      remainingVolume: volumeUpdateData.remainingVolume
        ? parseFloat(volumeUpdateData.remainingVolume)
        : null,
      volumeUnit: volumeUpdateData.volumeUnit,
      sampleStatus: volumeUpdateData.sampleStatus,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${nbId}/samples/update-volume`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.immunology.postAnalysis.volumeUpdateSuccess",
                defaultMessage:
                  "Successfully updated volume/status for {count} samples.",
              },
              { count: response.updatedCount || selectedSampleIds.length },
            ),
          );
          setVolumeModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.immunology.postAnalysis.volumeUpdateError",
                defaultMessage: "Failed to update volume/status.",
              }),
          );
        }
      },
    );
  };

  // Handle mark complete
  const handleMarkComplete = () => {
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.status !== "COMPLETED" &&
        s.hasStorageAssignment,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.postAnalysis.noEligibleSamples",
          defaultMessage:
            "Selected samples must have storage assigned before completing.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.immunology.postAnalysis.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to mark samples complete.");
        }
      },
    );
  };

  // Render sample status tag (analyzed/partially used/exhausted)
  const renderSampleStatusTag = (sample) => {
    const status = sample.sampleStatus || "ANALYZED";
    switch (status) {
      case "EXHAUSTED":
        return (
          <Tag type="red" size="sm" renderIcon={Subtract}>
            {intl.formatMessage({
              id: "notebook.immunology.postAnalysis.exhausted",
              defaultMessage: "Exhausted",
            })}
          </Tag>
        );
      case "PARTIALLY_USED":
        return (
          <Tag type="purple" size="sm">
            {intl.formatMessage({
              id: "notebook.immunology.postAnalysis.partiallyUsed",
              defaultMessage: "Partial",
            })}
          </Tag>
        );
      case "ANALYZED":
      default:
        return (
          <Tag type="teal" size="sm">
            {intl.formatMessage({
              id: "notebook.immunology.postAnalysis.analyzed",
              defaultMessage: "Analyzed",
            })}
          </Tag>
        );
    }
  };

  // Render storage tag
  const renderStorageTag = (sample) => {
    if (sample.storageLocation) {
      return (
        <Tag type="cyan" size="sm" renderIcon={Archive}>
          {sample.storageLocation}
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        {intl.formatMessage({
          id: "notebook.immunology.postAnalysis.notStored",
          defaultMessage: "Not Stored",
        })}
      </Tag>
    );
  };

  // Render condition tag
  const renderConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    const conditionLabels = {
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      LIQUID_NITROGEN: "LN₂",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Render quality flags - compact display with count
  const renderQualityFlags = (sample) => {
    if (!sample.qualityFlags || sample.qualityFlags.length === 0) {
      return null;
    }

    const flagCount = sample.qualityFlags.length;
    const hasError = sample.qualityFlags.some((flag) => {
      const flagOption = qualityFlagOptions.find((f) => f.id === flag);
      return flagOption?.severity === "error";
    });

    // Show compact tag with count and tooltip-like title
    const flagLabels = sample.qualityFlags
      .map((flag) => {
        const flagOption = qualityFlagOptions.find((f) => f.id === flag);
        return flagOption?.label || flag;
      })
      .join(", ");

    return (
      <Tag
        type={hasError ? "red" : "magenta"}
        size="sm"
        renderIcon={hasError ? Warning : WarningAlt}
        title={flagLabels}
      >
        {flagCount} {flagCount === 1 ? "Flag" : "Flags"}
      </Tag>
    );
  };

  // Grid columns for samples (custom columns without collectionDate)
  const getColumns = () => [
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
        id: "notebook.column.sampleType",
        defaultMessage: "Sample Type",
      }),
      render: (value, sample) => value || sample?.sampleType || "-",
    },
    {
      key: "sampleStatus",
      header: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.sampleStatus",
        defaultMessage: "Sample Status",
      }),
      render: (value, sample) => renderSampleStatusTag(sample),
    },
    {
      key: "remainingVolume",
      header: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.remainingVolume",
        defaultMessage: "Remaining Vol.",
      }),
      render: (value, sample) =>
        sample.remainingVolume
          ? `${sample.remainingVolume} ${sample.volumeUnit || "µL"}`
          : "-",
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "notebook.column.storage",
        defaultMessage: "Storage",
      }),
      render: (value, sample) => (
        <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
          {renderStorageTag(sample)}
          {renderConditionTag(sample)}
        </div>
      ),
    },
    {
      key: "qualityFlags",
      header: intl.formatMessage({
        id: "notebook.immunology.postAnalysis.qualityFlags",
        defaultMessage: "Flags",
      }),
      render: (value, sample) => renderQualityFlags(sample) || "-",
    },
  ];

  return (
    <div className="immunology-post-analysis-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.postAnalysis.title"
            defaultMessage="Post-Analysis Handling"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.postAnalysis.description"
            defaultMessage="Store processed samples under defined conditions, track remaining volume and sample status, and flag samples with quality issues."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.pendingStorage"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{pendingStorageCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storedCount}</span>
            </Tile>
            <Tile
              className={`progress-tile ${flaggedSamplesCount > 0 ? "warning" : ""}`}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.flagged"
                  defaultMessage="Flagged"
                />
              </span>
              <span className="progress-value">{flaggedSamplesCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.immunology.postAnalysis.assignStorage"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Subtract}
          onClick={handleOpenVolumeModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.immunology.postAnalysis.updateVolume"
            defaultMessage="Update Volume/Status"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={WarningAlt}
          onClick={handleOpenQualityFlagModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.immunology.postAnalysis.addQualityFlag"
            defaultMessage="Add Quality Flag"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
            disabled={assigning || !hasRealPageId}
          >
            <FormattedMessage
              id="notebook.immunology.postAnalysis.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.immunology.postAnalysis.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.immunology.postAnalysis.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.immunology.postAnalysis.pendingSamples.description"
              defaultMessage="Samples awaiting post-analysis storage assignment, volume tracking, or quality review."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.pendingSamples.empty"
                  defaultMessage="No pending samples. All samples have been processed."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={handleSelectionChange}
              showSelection={true}
              loading={loading}
              columns={getColumns()}
            />
          )}
        </div>
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.immunology.postAnalysis.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.immunology.postAnalysis.completedSamples.description"
              defaultMessage="Samples that have been stored with defined conditions and retention periods."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.completedSamples.empty"
                  defaultMessage="No completed samples yet. Assign storage and mark samples as complete."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="completed-samples"
              samples={completedSamples}
              showSelection={false}
              loading={loading}
              columns={getColumns()}
            />
          )}
        </div>
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.postAnalysis.storageModal.title",
          defaultMessage: "Assign Post-Analysis Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.postAnalysis.storageModal.assign",
                defaultMessage: "Assign to Storage",
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
        <p className="modal-description">
          <FormattedMessage
            id="notebook.immunology.postAnalysis.storageModal.description"
            defaultMessage="Store {count} processed samples under defined conditions. Select appropriate storage based on sample type and retention requirements."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Storage Location Selection */}
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.immunology.postAnalysis.storageLocation"
                  defaultMessage="Storage Location"
                />
              </h5>
              <StorageHierarchySelector
                onSelectionChange={handleStorageSelectionChange}
                entryId={notebookId || entryId}
                onBoxLayoutLoaded={handleBoxLayoutLoaded}
                boxRequired={true}
                showPath={true}
              />
            </div>
          </Column>

          {/* Box Layout Preview */}
          <Column lg={8} md={4} sm={4}>
            {storageSelection.box ? (
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
                    <Archive size={16} style={{ marginRight: "0.5rem" }} />
                    <FormattedMessage
                      id="notebook.immunology.postAnalysis.boxLayout"
                      defaultMessage="Box Layout"
                    />
                  </h5>
                  <Button
                    kind="tertiary"
                    size="sm"
                    renderIcon={Renew}
                    onClick={handleAutoPopulate}
                    disabled={selectedSampleIds.length === 0}
                  >
                    <FormattedMessage
                      id="notebook.immunology.postAnalysis.autoPopulate"
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

                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.immunology.postAnalysis.assignmentSummary"
                    defaultMessage="{assigned} of {total} samples assigned to wells"
                    values={{
                      assigned: Object.keys(wellAssignments).length,
                      total: selectedSampleIds.length,
                    }}
                  />
                </div>
              </div>
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <Archive size={32} />
                <p style={{ marginTop: "0.5rem", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.immunology.postAnalysis.selectBoxFirst"
                    defaultMessage="Select a storage location to preview box layout"
                  />
                </p>
              </div>
            )}
          </Column>

          {/* Storage Settings */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.immunology.postAnalysis.storageSettings"
                defaultMessage="Storage Settings"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-condition-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.condition",
                defaultMessage: "Storage Condition *",
              })}
              label={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.selectCondition",
                defaultMessage: "Select condition...",
              })}
              items={storageConditionOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedCondition}
              onChange={({ selectedItem }) =>
                setSelectedCondition(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="retention-years"
              label={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.retentionYears",
                defaultMessage: "Retention Period (Years)",
              })}
              value={retentionYears}
              min={1}
              max={30}
              step={1}
              onChange={(e, { value }) => setRetentionYears(value)}
              helperText={intl.formatMessage(
                {
                  id: "notebook.immunology.postAnalysis.expiryDate",
                  defaultMessage: "Expiry date will be: {date}",
                },
                {
                  date: new Date(
                    Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                  ).toLocaleDateString(),
                },
              )}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="storage-notes"
              labelText={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.notes",
                defaultMessage: "Storage Notes",
              })}
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Optional notes about post-analysis storage..."
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Quality Flag Modal */}
      <Modal
        open={qualityFlagModalOpen}
        onRequestClose={() => setQualityFlagModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.postAnalysis.qualityModal.title",
          defaultMessage: "Add Quality Flag",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.postAnalysis.qualityModal.add",
                defaultMessage: "Add Flag",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAddQualityFlag}
        primaryButtonDisabled={!qualityFlagData.flagType || assigning}
        size="md"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.immunology.postAnalysis.qualityModal.description"
            defaultMessage="Flag {count} samples with quality issues discovered during assay or analysis."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Dropdown
          id="quality-flag-dropdown"
          titleText={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.flagType",
            defaultMessage: "Flag Type *",
          })}
          label={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.selectFlagType",
            defaultMessage: "Select flag type...",
          })}
          items={qualityFlagOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={qualityFlagData.flagType}
          onChange={({ selectedItem }) =>
            setQualityFlagData((prev) => ({ ...prev, flagType: selectedItem }))
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="quality-notes"
          labelText={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.qualityNotes",
            defaultMessage: "Notes",
          })}
          value={qualityFlagData.notes}
          onChange={(e) =>
            setQualityFlagData((prev) => ({ ...prev, notes: e.target.value }))
          }
          placeholder="Describe the quality issue..."
          rows={3}
          style={{ marginBottom: "1rem" }}
        />

        <Checkbox
          id="requires-investigation"
          labelText={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.requiresInvestigation",
            defaultMessage: "Requires investigation",
          })}
          checked={qualityFlagData.requiresInvestigation}
          onChange={(e, { checked }) =>
            setQualityFlagData((prev) => ({
              ...prev,
              requiresInvestigation: checked,
            }))
          }
        />
      </Modal>

      {/* Volume Update Modal */}
      <Modal
        open={volumeModalOpen}
        onRequestClose={() => setVolumeModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.postAnalysis.volumeModal.title",
          defaultMessage: "Update Volume & Status",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.postAnalysis.volumeModal.update",
                defaultMessage: "Update",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleUpdateVolume}
        primaryButtonDisabled={assigning}
        size="md"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.immunology.postAnalysis.volumeModal.description"
            defaultMessage="Update remaining volume and sample status for {count} samples after analysis."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Dropdown
          id="sample-status-dropdown"
          titleText={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.sampleStatus",
            defaultMessage: "Sample Status",
          })}
          label={intl.formatMessage({
            id: "notebook.immunology.postAnalysis.selectStatus",
            defaultMessage: "Select status...",
          })}
          items={sampleStatusOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={sampleStatusOptions.find(
            (s) => s.id === volumeUpdateData.sampleStatus,
          )}
          onChange={({ selectedItem }) =>
            setVolumeUpdateData((prev) => ({
              ...prev,
              sampleStatus: selectedItem?.id || "ANALYZED",
            }))
          }
          style={{ marginBottom: "1rem" }}
        />

        <Grid fullWidth narrow>
          <Column lg={10} md={5} sm={3}>
            <TextInput
              id="remaining-volume"
              labelText={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.remainingVolume",
                defaultMessage: "Remaining Volume",
              })}
              value={volumeUpdateData.remainingVolume}
              onChange={(e) =>
                setVolumeUpdateData((prev) => ({
                  ...prev,
                  remainingVolume: e.target.value,
                }))
              }
              placeholder="e.g., 250"
              type="number"
            />
          </Column>
          <Column lg={6} md={3} sm={1}>
            <Dropdown
              id="volume-unit-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.immunology.postAnalysis.unit",
                defaultMessage: "Unit",
              })}
              items={[
                { id: "µL", label: "µL (microliters)" },
                { id: "mL", label: "mL (milliliters)" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={{
                id: volumeUpdateData.volumeUnit,
                label: volumeUpdateData.volumeUnit,
              }}
              onChange={({ selectedItem }) =>
                setVolumeUpdateData((prev) => ({
                  ...prev,
                  volumeUnit: selectedItem?.id || "µL",
                }))
              }
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default ImmunologyPostAnalysisPage;
