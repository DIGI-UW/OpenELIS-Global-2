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
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
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
  Add,
  TrashCan,
  Chemistry,
  Microscope,
  DataVis_2,
  View,
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
 * BacteriologyPostAnalysisPage - Page 6 of the Bacteriology workflow.
 * Handles post-analysis sample storage with bacteriology-specific tracking:
 *
 * SAMPLE TRACKING/STORAGE:
 * 1. Remaining Volume Tracking - Record remaining sample volume after processing
 * 2. Type of Sample Aliquots:
 *    - Isolates: Pure bacterial cultures (agar slants, glycerol stocks)
 *    - DNA: Extracted genomic DNA
 *    - RNA: If applicable (rare in bacteriology)
 *    - Primary Sample: Residual original specimen
 * 3. Storage Media for Isolates:
 *    - STGG (Skim Milk-Tryptone-Glucose-Glycerol): Long-term storage
 *    - Glycerol stocks: 15-20% glycerol in broth, stored at -80°C
 *    - Bead systems, lyophilized cultures
 * 4. Storage Containers: Tube, Vial (cryovial), Slide, Plate, Other
 * 5. Storage Hierarchy: Room → Freezer/Refrigerator → Drawer → Rack → Sample Box
 * 6. Storage Methods:
 *    - Freezer (-20°C, -80°C for isolates, DNA)
 *    - Refrigerator (2-8°C for short-term cultures)
 *    - Slide cabinet (room temp for stained slides)
 * 7. Environmental Monitoring:
 *    - Temperature checks (twice daily or automated)
 *    - Logged with date, time, temp, staff initials
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */

// Sample Aliquot Types for Bacteriology
const ALIQUOT_TYPES = [
  { id: "ISOLATE", text: "Isolate (Pure Culture)" },
  { id: "DNA", text: "DNA (Extracted Genomic DNA)" },
  { id: "RNA", text: "RNA (if applicable)" },
  { id: "PRIMARY_SAMPLE", text: "Primary Sample (Residual Specimen)" },
  { id: "GLYCEROL_STOCK", text: "Glycerol Stock" },
  { id: "LYOPHILIZED", text: "Lyophilized Culture" },
];

// Storage Media Types for Isolates
const STORAGE_MEDIA_TYPES = [
  { id: "STGG", text: "STGG (Skim Milk-Tryptone-Glucose-Glycerol)" },
  { id: "GLYCEROL_15", text: "Glycerol Stock (15%)" },
  { id: "GLYCEROL_20", text: "Glycerol Stock (20%)" },
  { id: "BEAD_SYSTEM", text: "Bead System (Microbank/Cryobeads)" },
  { id: "AGAR_SLANT", text: "Agar Slant" },
  { id: "BROTH_CULTURE", text: "Broth Culture" },
  { id: "LYOPHILIZED", text: "Lyophilized" },
  { id: "TE_BUFFER", text: "TE Buffer (for DNA)" },
  { id: "RNASE_FREE_WATER", text: "RNase-free Water (for RNA)" },
  { id: "NONE", text: "None / Not Applicable" },
];

// Storage Container Types
const CONTAINER_TYPES = [
  { id: "CRYOVIAL", text: "Cryovial (1.5-2.0 mL)" },
  { id: "TUBE_1_5ML", text: "Microcentrifuge Tube (1.5 mL)" },
  { id: "TUBE_2ML", text: "Microcentrifuge Tube (2.0 mL)" },
  { id: "SCREW_CAP_TUBE", text: "Screw Cap Tube" },
  { id: "AGAR_PLATE", text: "Agar Plate" },
  { id: "AGAR_SLANT_TUBE", text: "Agar Slant Tube" },
  { id: "SLIDE", text: "Microscope Slide" },
  { id: "PCR_STRIP", text: "PCR Strip Tube" },
  { id: "OTHER", text: "Other" },
];

// Storage Conditions for Bacteriology
const STORAGE_CONDITIONS = [
  { id: "ROOM_TEMP", text: "Room Temperature (15-25°C)", tempRange: "15-25°C" },
  {
    id: "REFRIGERATED",
    text: "Refrigerated (2-8°C)",
    tempRange: "2-8°C",
    duration: "short-term",
  },
  {
    id: "FROZEN_MINUS20",
    text: "Frozen (-20°C)",
    tempRange: "-20°C",
    duration: "medium-term",
  },
  {
    id: "FROZEN_MINUS80",
    text: "Ultra-Low (-80°C)",
    tempRange: "-80°C",
    duration: "long-term",
  },
  {
    id: "LIQUID_NITROGEN",
    text: "Liquid Nitrogen (-196°C)",
    tempRange: "-196°C",
    duration: "extended",
  },
];

// Storage Methods
const STORAGE_METHODS = [
  { id: "FREEZER_MINUS20", text: "Freezer (-20°C)" },
  { id: "FREEZER_MINUS80", text: "Ultra-Low Freezer (-80°C)" },
  { id: "REFRIGERATOR", text: "Refrigerator (2-8°C)" },
  { id: "SLIDE_CABINET", text: "Slide Cabinet (Room Temp)" },
  { id: "INCUBATOR", text: "Incubator" },
  { id: "LIQUID_NITROGEN_TANK", text: "Liquid Nitrogen Tank" },
];

// Sample Status Options
const SAMPLE_STATUS_OPTIONS = [
  { id: "ANALYZED", text: "Analyzed - Full Volume Remaining" },
  { id: "PARTIALLY_USED", text: "Partially Used - Reduced Volume" },
  { id: "EXHAUSTED", text: "Exhausted - No Volume Remaining" },
  { id: "STORED_ISOLATE", text: "Stored as Isolate" },
  { id: "STORED_DNA", text: "Stored as DNA Extract" },
];

// Quality Flag Types for Bacteriology
const QUALITY_FLAG_OPTIONS = [
  {
    id: "INSUFFICIENT_VOLUME",
    text: "Insufficient Volume for Repeat Testing",
    severity: "warning",
  },
  { id: "CONTAMINATION", text: "Possible Contamination", severity: "error" },
  { id: "MIXED_CULTURE", text: "Mixed Culture Detected", severity: "warning" },
  { id: "NO_GROWTH", text: "No Growth on Subculture", severity: "error" },
  {
    id: "ATYPICAL_MORPHOLOGY",
    text: "Atypical Colony Morphology",
    severity: "warning",
  },
  { id: "VIABILITY_CONCERN", text: "Viability Concern", severity: "warning" },
  {
    id: "STORAGE_TEMP_DEVIATION",
    text: "Storage Temperature Deviation",
    severity: "error",
  },
  {
    id: "UNEXPECTED_RESULTS",
    text: "Unexpected Results Requiring Investigation",
    severity: "warning",
  },
];

function BacteriologyPostAnalysisPage({
  entryId,
  notebookId,
  pageData,
  progress: _progress,
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

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [selectedAliquotType, setSelectedAliquotType] = useState(null);
  const [selectedStorageMedia, setSelectedStorageMedia] = useState(null);
  const [selectedContainerType, setSelectedContainerType] = useState(null);
  const [selectedStorageMethod, setSelectedStorageMethod] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [storageNotes, setStorageNotes] = useState("");

  // Volume update modal state
  const [volumeModalOpen, setVolumeModalOpen] = useState(false);
  const [volumeUpdateData, setVolumeUpdateData] = useState({
    remainingVolume: "",
    volumeUnit: "µL",
    sampleStatus: "ANALYZED",
  });

  // Quality flag modal state
  const [qualityFlagModalOpen, setQualityFlagModalOpen] = useState(false);
  const [qualityFlagData, setQualityFlagData] = useState({
    flagType: null,
    notes: "",
    requiresInvestigation: false,
  });

  // Temperature log modal state
  const [tempLogModalOpen, setTempLogModalOpen] = useState(false);
  const [temperatureLogs, setTemperatureLogs] = useState([]);
  const [newTempLog, setNewTempLog] = useState({
    date: new Date().toISOString().split("T")[0],
    time: new Date().toTimeString().slice(0, 5),
    temperature: "",
    unit: "C",
    staffInitials: "",
    storageLocation: "",
    notes: "",
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadTemperatureLogs();

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
            // Bacteriology-specific fields
            aliquotType: sample.data?.aliquotType || null,
            storageMedia: sample.data?.storageMedia || null,
            containerType: sample.data?.containerType || null,
            storageMethod: sample.data?.storageMethod || null,
            organismIdentified: sample.data?.organismIdentified || null,
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

  const loadTemperatureLogs = useCallback(() => {
    const nbId = notebookId || entryId;
    if (!nbId) return;

    getFromOpenElisServer(
      `/rest/notebook-entry/${nbId}/temperature-logs`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setTemperatureLogs(response);
        }
      },
    );
  }, [notebookId, entryId]);

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

  // Count samples by aliquot type
  const aliquotCounts = useMemo(() => {
    const counts = {};
    samples.forEach((s) => {
      if (s.aliquotType) {
        counts[s.aliquotType] = (counts[s.aliquotType] || 0) + 1;
      }
    });
    return counts;
  }, [samples]);

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
              id: "notebook.bacteriology.postAnalysis.wellOccupied",
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
          id: "notebook.bacteriology.postAnalysis.selectBoxFirst",
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
            id: "notebook.bacteriology.postAnalysis.notEnoughWells",
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
            id: "notebook.bacteriology.postAnalysis.autoPopulateSuccess",
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
          id: "notebook.bacteriology.postAnalysis.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    setStorageModalOpen(true);
    setError(null);
    setWellAssignments({});
    setSelectedCondition(null);
    setSelectedAliquotType(null);
    setSelectedStorageMedia(null);
    setSelectedContainerType(null);
    setSelectedStorageMethod(null);
    setRetentionYears(5);
    setStorageNotes("");
  };

  // Get the most specific location selected in the hierarchy
  const getLocationInfo = () => {
    if (storageSelection.box && storageSelection.box.id) {
      return { locationId: storageSelection.box.id, locationType: "box" };
    }
    if (storageSelection.rack && storageSelection.rack.id) {
      return { locationId: storageSelection.rack.id, locationType: "rack" };
    }
    if (storageSelection.shelf && storageSelection.shelf.id) {
      return { locationId: storageSelection.shelf.id, locationType: "shelf" };
    }
    if (storageSelection.device && storageSelection.device.id) {
      return { locationId: storageSelection.device.id, locationType: "device" };
    }
    if (storageSelection.room && storageSelection.room.id) {
      return { locationId: storageSelection.room.id, locationType: "room" };
    }
    return { locationId: null, locationType: null };
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    // Check if any hierarchy level is selected
    if (
      !storageSelection.room &&
      !storageSelection.device &&
      !storageSelection.shelf &&
      !storageSelection.rack &&
      !storageSelection.box
    ) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.selectStorage",
          defaultMessage: "Please select a storage location.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (!selectedAliquotType) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.selectAliquotType",
          defaultMessage: "Please select an aliquot type.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.noWellAssignments",
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
    const locationInfo = getLocationInfo();
    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      locationId: locationInfo.locationId,
      locationType: locationInfo.locationType,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      aliquotType: selectedAliquotType.id,
      storageMedia: selectedStorageMedia?.id || null,
      containerType: selectedContainerType?.id || null,
      storageMethod: selectedStorageMethod?.id || null,
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
                id: "notebook.bacteriology.postAnalysis.assignSuccess",
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
                id: "notebook.bacteriology.postAnalysis.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle open volume modal
  const handleOpenVolumeModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.noSamplesForVolume",
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
                id: "notebook.bacteriology.postAnalysis.volumeUpdateSuccess",
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
                id: "notebook.bacteriology.postAnalysis.volumeUpdateError",
                defaultMessage: "Failed to update volume/status.",
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
          id: "notebook.bacteriology.postAnalysis.noSamplesForFlag",
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
          id: "notebook.bacteriology.postAnalysis.selectFlagType",
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
                id: "notebook.bacteriology.postAnalysis.flagSuccess",
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
                id: "notebook.bacteriology.postAnalysis.flagError",
                defaultMessage: "Failed to add quality flag.",
              }),
          );
        }
      },
    );
  };

  // Handle add temperature log
  const handleAddTemperatureLog = () => {
    if (!newTempLog.temperature || !newTempLog.staffInitials) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.tempLogRequired",
          defaultMessage: "Temperature and staff initials are required.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const nbId = notebookId || entryId;
    const payload = {
      date: newTempLog.date,
      time: newTempLog.time,
      temperature: parseFloat(newTempLog.temperature),
      unit: newTempLog.unit,
      staffInitials: newTempLog.staffInitials,
      storageLocation: newTempLog.storageLocation,
      notes: newTempLog.notes,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook-entry/${nbId}/temperature-logs`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage({
              id: "notebook.bacteriology.postAnalysis.tempLogSuccess",
              defaultMessage: "Temperature log recorded successfully.",
            }),
          );
          setNewTempLog({
            date: new Date().toISOString().split("T")[0],
            time: new Date().toTimeString().slice(0, 5),
            temperature: "",
            unit: "C",
            staffInitials: "",
            storageLocation: "",
            notes: "",
          });
          loadTemperatureLogs();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.tempLogError",
                defaultMessage: "Failed to record temperature log.",
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
          id: "notebook.bacteriology.postAnalysis.noEligibleSamples",
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
                id: "notebook.bacteriology.postAnalysis.completeSuccess",
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

  // Render aliquot type tag
  const renderAliquotTypeTag = (sample) => {
    if (!sample || !sample.aliquotType) return null;

    const typeLabels = {
      ISOLATE: { text: "Isolate", icon: Microscope, type: "purple" },
      DNA: { text: "DNA", icon: DataVis_2, type: "blue" },
      RNA: { text: "RNA", icon: DataVis_2, type: "teal" },
      PRIMARY_SAMPLE: { text: "Primary", icon: Chemistry, type: "gray" },
      GLYCEROL_STOCK: { text: "Glycerol", icon: Chemistry, type: "cyan" },
      LYOPHILIZED: { text: "Lyophilized", icon: Chemistry, type: "magenta" },
    };

    const config = typeLabels[sample.aliquotType] || {
      text: sample.aliquotType,
      type: "gray",
    };

    return (
      <Tag type={config.type} size="sm" renderIcon={config.icon}>
        {config.text}
      </Tag>
    );
  };

  // Render sample status tag
  const renderSampleStatusTag = (sample) => {
    if (!sample) return null;
    const status = sample.sampleStatus || "ANALYZED";
    const statusConfig = {
      ANALYZED: { text: "Analyzed", type: "teal" },
      PARTIALLY_USED: { text: "Partial", type: "purple" },
      EXHAUSTED: { text: "Exhausted", type: "red", icon: Subtract },
      STORED_ISOLATE: { text: "Isolate Stored", type: "blue" },
      STORED_DNA: { text: "DNA Stored", type: "cyan" },
    };

    const config = statusConfig[status] || { text: status, type: "gray" };

    return (
      <Tag type={config.type} size="sm" renderIcon={config.icon}>
        {config.text}
      </Tag>
    );
  };

  // Render storage tag
  const renderStorageTag = (sample) => {
    if (!sample) return null;
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
          id: "notebook.bacteriology.postAnalysis.notStored",
          defaultMessage: "Not Stored",
        })}
      </Tag>
    );
  };

  // Render condition tag
  const renderConditionTag = (sample) => {
    if (!sample || !sample.storageCondition) return null;

    const conditionLabels = {
      ROOM_TEMP: "RT",
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

  // Render quality flags
  const renderQualityFlags = (sample) => {
    if (!sample || !sample.qualityFlags || sample.qualityFlags.length === 0) {
      return null;
    }

    const flagCount = sample.qualityFlags.length;
    const hasError = sample.qualityFlags.some((flag) => {
      const flagOption = QUALITY_FLAG_OPTIONS.find((f) => f.id === flag);
      return flagOption?.severity === "error";
    });

    const flagLabels = sample.qualityFlags
      .map((flag) => {
        const flagOption = QUALITY_FLAG_OPTIONS.find((f) => f.id === flag);
        return flagOption?.text || flag;
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

  // Grid columns for samples
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
      key: "aliquotType",
      header: intl.formatMessage({
        id: "notebook.bacteriology.postAnalysis.aliquotType",
        defaultMessage: "Aliquot Type",
      }),
      render: (value, sample) => renderAliquotTypeTag(sample) || "-",
    },
    {
      key: "sampleStatus",
      header: intl.formatMessage({
        id: "notebook.bacteriology.postAnalysis.sampleStatus",
        defaultMessage: "Status",
      }),
      render: (value, sample) => renderSampleStatusTag(sample),
    },
    {
      key: "remainingVolume",
      header: intl.formatMessage({
        id: "notebook.bacteriology.postAnalysis.remainingVolume",
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
        id: "notebook.bacteriology.postAnalysis.qualityFlags",
        defaultMessage: "Flags",
      }),
      render: (value, sample) => renderQualityFlags(sample) || "-",
    },
  ];

  return (
    <div className="bacteriology-post-analysis-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.postAnalysis.title"
            defaultMessage="Post-Analysis Storage"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.postAnalysis.description"
            defaultMessage="Store processed samples (isolates, DNA, primary samples) with appropriate storage conditions. Track remaining volumes and environmental monitoring."
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

      {/* Tabs for different sections */}
      <Tabs>
        <TabList aria-label="Post-analysis sections">
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.postAnalysis.tab.storage"
              defaultMessage="Sample Storage"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.postAnalysis.tab.monitoring"
              defaultMessage="Environmental Monitoring"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Sample Storage Tab */}
          <TabPanel>
            {/* Progress Summary */}
            <Grid fullWidth className="progress-section">
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.totalSamples"
                        defaultMessage="Total Samples"
                      />
                    </span>
                    <span className="progress-value">{samples.length}</span>
                  </Tile>
                  <Tile className="progress-tile pending">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.pendingStorage"
                        defaultMessage="Pending Storage"
                      />
                    </span>
                    <span className="progress-value">
                      {pendingStorageCount}
                    </span>
                  </Tile>
                  <Tile className="progress-tile verified">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.stored"
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
                        id="notebook.bacteriology.postAnalysis.flagged"
                        defaultMessage="Flagged"
                      />
                    </span>
                    <span className="progress-value">
                      {flaggedSamplesCount}
                    </span>
                  </Tile>
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.completed"
                        defaultMessage="Completed"
                      />
                    </span>
                    <span className="progress-value">
                      {completedSamples.length}
                    </span>
                  </Tile>
                </div>
              </Column>
            </Grid>

            {/* Aliquot Type Summary */}
            {Object.keys(aliquotCounts).length > 0 && (
              <Grid
                fullWidth
                style={{ marginTop: "0.5rem", marginBottom: "1rem" }}
              >
                <Column lg={16} md={8} sm={4}>
                  <div
                    style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}
                  >
                    <span
                      style={{
                        fontSize: "12px",
                        color: "#525252",
                        marginRight: "8px",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.aliquotSummary"
                        defaultMessage="Aliquot Types:"
                      />
                    </span>
                    {Object.entries(aliquotCounts).map(([type, count]) => {
                      const typeInfo = ALIQUOT_TYPES.find((t) => t.id === type);
                      return (
                        <Tag key={type} type="outline" size="sm">
                          {typeInfo?.text || type}: {count}
                        </Tag>
                      );
                    })}
                  </div>
                </Column>
              </Grid>
            )}

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
                  id="notebook.bacteriology.postAnalysis.assignStorage"
                  defaultMessage="Assign to Storage ({count})"
                  values={{ count: selectedSampleIds.length }}
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
                    id="notebook.bacteriology.postAnalysis.markComplete"
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
                  id="notebook.bacteriology.postAnalysis.refresh"
                  defaultMessage="Refresh"
                />
              </Button>
            </div>

            {/* Pending / In Progress Samples Table */}
            <div className="sample-table-section">
              <div className="table-section-header">
                <h5>
                  <FormattedMessage
                    id="notebook.bacteriology.postAnalysis.pendingSamples.title"
                    defaultMessage="Pending / In Progress"
                  />
                  <Tag type="gray" size="sm" className="count-tag">
                    {pendingSamples.length}
                  </Tag>
                </h5>
                <p className="table-section-description">
                  <FormattedMessage
                    id="notebook.bacteriology.postAnalysis.pendingSamples.description"
                    defaultMessage="Samples awaiting post-analysis storage assignment with isolate/DNA/primary sample tracking."
                  />
                </p>
              </div>
              <div className="sample-grid-container">
                {!loading && pendingSamples.length === 0 ? (
                  <div className="empty-table-state">
                    <p>
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.pendingSamples.empty"
                        defaultMessage="No pending samples. All samples have been processed and stored."
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
                    id="notebook.bacteriology.postAnalysis.completedSamples.title"
                    defaultMessage="Completed"
                  />
                  <Tag type="green" size="sm" className="count-tag">
                    {completedSamples.length}
                  </Tag>
                </h5>
                <p className="table-section-description">
                  <FormattedMessage
                    id="notebook.bacteriology.postAnalysis.completedSamples.description"
                    defaultMessage="Samples stored with defined conditions and retention periods."
                  />
                </p>
              </div>
              <div className="sample-grid-container">
                {!loading && completedSamples.length === 0 ? (
                  <div className="empty-table-state">
                    <p>
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.completedSamples.empty"
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
          </TabPanel>

          {/* Environmental Monitoring Tab */}
          <TabPanel>
            <Grid fullWidth style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <Tile>
                  <h5
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                      marginBottom: "1rem",
                    }}
                  >
                    <Temperature size={20} />
                    <FormattedMessage
                      id="notebook.bacteriology.postAnalysis.tempMonitoring.title"
                      defaultMessage="Temperature Monitoring Log"
                    />
                  </h5>
                  <p
                    style={{
                      fontSize: "12px",
                      color: "#525252",
                      marginBottom: "1rem",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.bacteriology.postAnalysis.tempMonitoring.description"
                      defaultMessage="Record temperature checks twice daily or from automated monitoring systems. Log date, time, temperature, and staff initials."
                    />
                  </p>

                  {/* Add New Temperature Log */}
                  <div
                    style={{
                      backgroundColor: "#f4f4f4",
                      padding: "1rem",
                      borderRadius: "4px",
                      marginBottom: "1rem",
                    }}
                  >
                    <h6 style={{ marginBottom: "0.5rem" }}>
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.addTempLog"
                        defaultMessage="Add Temperature Reading"
                      />
                    </h6>
                    <Grid fullWidth narrow>
                      <Column lg={4} md={2} sm={2}>
                        <DatePicker
                          datePickerType="single"
                          dateFormat="Y-m-d"
                          value={newTempLog.date}
                          onChange={(dates) => {
                            if (dates && dates[0]) {
                              setNewTempLog((prev) => ({
                                ...prev,
                                date: dates[0].toISOString().split("T")[0],
                              }));
                            }
                          }}
                        >
                          <DatePickerInput
                            id="temp-log-date"
                            labelText={intl.formatMessage({
                              id: "notebook.bacteriology.postAnalysis.date",
                              defaultMessage: "Date",
                            })}
                            placeholder="yyyy-mm-dd"
                          />
                        </DatePicker>
                      </Column>
                      <Column lg={3} md={2} sm={2}>
                        <TextInput
                          id="temp-log-time"
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.time",
                            defaultMessage: "Time",
                          })}
                          value={newTempLog.time}
                          onChange={(e) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              time: e.target.value,
                            }))
                          }
                          placeholder="HH:MM"
                        />
                      </Column>
                      <Column lg={3} md={2} sm={2}>
                        <TextInput
                          id="temp-log-temperature"
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.temperature",
                            defaultMessage: "Temperature",
                          })}
                          value={newTempLog.temperature}
                          onChange={(e) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              temperature: e.target.value,
                            }))
                          }
                          placeholder="-80"
                          type="number"
                        />
                      </Column>
                      <Column lg={2} md={1} sm={1}>
                        <Dropdown
                          id="temp-log-unit"
                          titleText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.unit",
                            defaultMessage: "Unit",
                          })}
                          items={[
                            { id: "C", text: "°C" },
                            { id: "F", text: "°F" },
                          ]}
                          itemToString={(item) => (item ? item.text : "")}
                          selectedItem={{
                            id: newTempLog.unit,
                            text: newTempLog.unit === "C" ? "°C" : "°F",
                          }}
                          onChange={({ selectedItem }) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              unit: selectedItem?.id || "C",
                            }))
                          }
                        />
                      </Column>
                      <Column lg={4} md={3} sm={2}>
                        <TextInput
                          id="temp-log-location"
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.storageLocation",
                            defaultMessage: "Storage Location",
                          })}
                          value={newTempLog.storageLocation}
                          onChange={(e) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              storageLocation: e.target.value,
                            }))
                          }
                          placeholder="e.g., Freezer A1"
                        />
                      </Column>
                      <Column lg={3} md={2} sm={2}>
                        <TextInput
                          id="temp-log-initials"
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.staffInitials",
                            defaultMessage: "Staff Initials",
                          })}
                          value={newTempLog.staffInitials}
                          onChange={(e) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              staffInitials: e.target.value,
                            }))
                          }
                          placeholder="ABC"
                          maxLength={5}
                        />
                      </Column>
                      <Column lg={5} md={4} sm={4}>
                        <TextInput
                          id="temp-log-notes"
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.postAnalysis.notes",
                            defaultMessage: "Notes",
                          })}
                          value={newTempLog.notes}
                          onChange={(e) =>
                            setNewTempLog((prev) => ({
                              ...prev,
                              notes: e.target.value,
                            }))
                          }
                          placeholder="Optional notes..."
                        />
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <Button
                          kind="primary"
                          size="sm"
                          renderIcon={Add}
                          onClick={handleAddTemperatureLog}
                          disabled={assigning}
                          style={{ marginTop: "24px" }}
                        >
                          <FormattedMessage
                            id="notebook.bacteriology.postAnalysis.recordReading"
                            defaultMessage="Record Reading"
                          />
                        </Button>
                      </Column>
                    </Grid>
                  </div>

                  {/* Temperature Log History */}
                  <div>
                    <h6 style={{ marginBottom: "0.5rem" }}>
                      <FormattedMessage
                        id="notebook.bacteriology.postAnalysis.logHistory"
                        defaultMessage="Recent Readings"
                      />
                    </h6>
                    {temperatureLogs.length === 0 ? (
                      <div
                        style={{
                          textAlign: "center",
                          padding: "2rem",
                          color: "#525252",
                        }}
                      >
                        <Temperature size={32} />
                        <p style={{ marginTop: "0.5rem" }}>
                          <FormattedMessage
                            id="notebook.bacteriology.postAnalysis.noLogs"
                            defaultMessage="No temperature logs recorded yet."
                          />
                        </p>
                      </div>
                    ) : (
                      <DataTable
                        rows={temperatureLogs.slice(0, 20).map((log, idx) => ({
                          id: String(log.id || idx),
                          date: log.date,
                          time: log.time,
                          temperature: `${log.temperature}°${log.unit || "C"}`,
                          location: log.storageLocation || "-",
                          initials: log.staffInitials,
                          notes: log.notes || "-",
                        }))}
                        headers={[
                          { key: "date", header: "Date" },
                          { key: "time", header: "Time" },
                          { key: "temperature", header: "Temperature" },
                          { key: "location", header: "Location" },
                          { key: "initials", header: "Staff" },
                          { key: "notes", header: "Notes" },
                        ]}
                        size="sm"
                      >
                        {({
                          rows,
                          headers,
                          getTableProps,
                          getHeaderProps,
                          getRowProps,
                        }) => (
                          <Table {...getTableProps()} size="sm">
                            <TableHead>
                              <TableRow>
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
                                <TableRow
                                  {...getRowProps({ row })}
                                  key={row.id}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        )}
                      </DataTable>
                    )}
                  </div>
                </Tile>
              </Column>
            </Grid>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.storageModal.title",
          defaultMessage: "Assign Post-Analysis Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.storageModal.assign",
                defaultMessage: "Assign to Storage",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !(
            storageSelection.room ||
            storageSelection.device ||
            storageSelection.shelf ||
            storageSelection.rack ||
            storageSelection.box
          ) ||
          !selectedCondition ||
          !selectedAliquotType ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.bacteriology.postAnalysis.storageModal.description"
            defaultMessage="Store {count} processed samples (isolates, DNA, or primary samples) with appropriate storage conditions."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Sample Type and Storage Media */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="aliquot-type-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.aliquotType",
                defaultMessage: "Aliquot Type *",
              })}
              label={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.selectAliquotType",
                defaultMessage: "Select aliquot type...",
              })}
              items={ALIQUOT_TYPES}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={selectedAliquotType}
              onChange={({ selectedItem }) =>
                setSelectedAliquotType(selectedItem)
              }
              style={{ marginBottom: "1rem" }}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-media-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.storageMedia",
                defaultMessage: "Storage Media",
              })}
              label={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.selectStorageMedia",
                defaultMessage: "Select storage media...",
              })}
              items={STORAGE_MEDIA_TYPES}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={selectedStorageMedia}
              onChange={({ selectedItem }) =>
                setSelectedStorageMedia(selectedItem)
              }
              style={{ marginBottom: "1rem" }}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="container-type-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.containerType",
                defaultMessage: "Container Type",
              })}
              label={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.selectContainerType",
                defaultMessage: "Select container type...",
              })}
              items={CONTAINER_TYPES}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={selectedContainerType}
              onChange={({ selectedItem }) =>
                setSelectedContainerType(selectedItem)
              }
              style={{ marginBottom: "1rem" }}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-method-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.storageMethod",
                defaultMessage: "Storage Method",
              })}
              label={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.selectStorageMethod",
                defaultMessage: "Select storage method...",
              })}
              items={STORAGE_METHODS}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={selectedStorageMethod}
              onChange={({ selectedItem }) =>
                setSelectedStorageMethod(selectedItem)
              }
              style={{ marginBottom: "1rem" }}
            />
          </Column>

          {/* Storage Location Selection */}
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.bacteriology.postAnalysis.storageLocation"
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
                      id="notebook.bacteriology.postAnalysis.boxLayout"
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
                      id="notebook.bacteriology.postAnalysis.autoPopulate"
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
                    id="notebook.bacteriology.postAnalysis.assignmentSummary"
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
                    id="notebook.bacteriology.postAnalysis.selectBoxFirst"
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
                id="notebook.bacteriology.postAnalysis.storageSettings"
                defaultMessage="Storage Settings"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-condition-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.condition",
                defaultMessage: "Storage Condition *",
              })}
              label={intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.selectCondition",
                defaultMessage: "Select condition...",
              })}
              items={STORAGE_CONDITIONS}
              itemToString={(item) => (item ? item.text : "")}
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
                id: "notebook.bacteriology.postAnalysis.retentionYears",
                defaultMessage: "Retention Period (Years)",
              })}
              value={retentionYears}
              min={1}
              max={30}
              step={1}
              onChange={(e, { value }) => setRetentionYears(value)}
              helperText={intl.formatMessage(
                {
                  id: "notebook.bacteriology.postAnalysis.expiryDate",
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
                id: "notebook.bacteriology.postAnalysis.notes",
                defaultMessage: "Storage Notes",
              })}
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Optional notes about post-analysis storage (organism identified, special handling, etc.)..."
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Volume Update Modal */}
      <Modal
        open={volumeModalOpen}
        onRequestClose={() => setVolumeModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.volumeModal.title",
          defaultMessage: "Update Volume & Status",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.volumeModal.update",
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
            id="notebook.bacteriology.postAnalysis.volumeModal.description"
            defaultMessage="Update remaining volume and sample status for {count} samples after processing."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Dropdown
          id="sample-status-dropdown"
          titleText={intl.formatMessage({
            id: "notebook.bacteriology.postAnalysis.sampleStatus",
            defaultMessage: "Sample Status",
          })}
          label={intl.formatMessage({
            id: "notebook.bacteriology.postAnalysis.selectStatus",
            defaultMessage: "Select status...",
          })}
          items={SAMPLE_STATUS_OPTIONS}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={SAMPLE_STATUS_OPTIONS.find(
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
                id: "notebook.bacteriology.postAnalysis.remainingVolume",
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
                id: "notebook.bacteriology.postAnalysis.unit",
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

      {/* Quality Flag Modal */}
      <Modal
        open={qualityFlagModalOpen}
        onRequestClose={() => setQualityFlagModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.postAnalysis.qualityModal.title",
          defaultMessage: "Add Quality Flag",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "notebook.bacteriology.postAnalysis.qualityModal.add",
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
            id="notebook.bacteriology.postAnalysis.qualityModal.description"
            defaultMessage="Flag {count} samples with quality issues discovered during analysis or storage."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Dropdown
          id="quality-flag-dropdown"
          titleText={intl.formatMessage({
            id: "notebook.bacteriology.postAnalysis.flagType",
            defaultMessage: "Flag Type *",
          })}
          label={intl.formatMessage({
            id: "notebook.bacteriology.postAnalysis.selectFlagType",
            defaultMessage: "Select flag type...",
          })}
          items={QUALITY_FLAG_OPTIONS}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={qualityFlagData.flagType}
          onChange={({ selectedItem }) =>
            setQualityFlagData((prev) => ({ ...prev, flagType: selectedItem }))
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="quality-notes"
          labelText={intl.formatMessage({
            id: "notebook.bacteriology.postAnalysis.qualityNotes",
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
            id: "notebook.bacteriology.postAnalysis.requiresInvestigation",
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
    </div>
  );
}

export default BacteriologyPostAnalysisPage;
