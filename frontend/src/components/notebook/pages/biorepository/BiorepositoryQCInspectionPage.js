import React, { useState, useCallback, useEffect, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  Tag,
  TextInput,
  Checkbox,
  Dropdown,
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
  Loading,
} from "@carbon/react";
import { Checkmark, Edit, Renew, WarningAlt } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import {
  getAllowedCorrectionActions,
  hasInvalidMarkMissingLocationFields,
  isMissingDiscrepancyType,
} from "./qcCorrectionGuardrails";

/**
 * QC Checklist items for Biorepository sample inspection
 * Based on ISO 20387:2018 quality control requirements
 */
const QC_CHECKLIST = [
  {
    id: "samplePresent",
    labelId: "biorepository.qc.samplePresent",
    defaultLabel: "Sample physically present in location",
  },
  {
    id: "labelIntegrity",
    labelId: "biorepository.qc.labelIntegrity",
    defaultLabel: "Label intact and legible",
  },
  {
    id: "containerIntegrity",
    labelId: "biorepository.qc.containerIntegrity",
    defaultLabel: "Container intact (no cracks/leaks)",
  },
  {
    id: "volumeAppearanceAcceptable",
    labelId: "biorepository.qc.volumeAppearance",
    defaultLabel: "Volume and appearance acceptable",
  },
  {
    id: "correctPosition",
    labelId: "biorepository.qc.correctPosition",
    defaultLabel: "Sample in correct storage position",
  },
];

/**
 * Discrepancy types for failed QC
 */
const DISCREPANCY_TYPES = [
  { id: "SAMPLE_MISSING", label: "Sample missing" },
  { id: "WRONG_SAMPLE_IN_POSITION", label: "Wrong sample in position" },
  {
    id: "MISPLACED_SAMPLE_FOUND",
    label: "Misplaced sample (found elsewhere)",
  },
  {
    id: "EMPTY_POSITION_REGISTERED",
    label: "Empty position but registered as occupied",
  },
  { id: "LABELING_ERROR", label: "Labeling error" },
  { id: "BOX_RACK_MISPLACEMENT", label: "Box/rack misplacement" },
];

const CORRECTION_ACTIONS = [
  {
    id: "UPDATE_LOCATION",
    label: "Update correct location (sample found elsewhere)",
  },
  {
    id: "REASSIGN_POSITION",
    label: "Reassign position in selected box",
  },
  {
    id: "MARK_MISSING",
    label: "Mark sample as Missing (not found)",
  },
];

const ALL_OPTION = "__ALL__";

const buildStorageOverviewQuery = (filters) => {
  const params = new URLSearchParams();
  ["freezer", "shelf", "rack", "box"].forEach((key) => {
    const value = filters?.[key];
    if (value && value !== ALL_OPTION) {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
};

/**
 * BiorepositoryQCInspectionPage - QC Inspection workflow for stored samples
 *
 * Allows technicians to perform visual inspection and verification of samples
 * in storage against their expected location and condition.
 *
 * Features:
 * - Load samples with workflowStatus = STORED
 * - Display storage coordinates (freezer/shelf/rack/box/position)
 * - 5-point QC checklist (presence, label, container, volume, position)
 * - Auto-calculate QC result (all pass = VERIFIED, any fail = DISCREPANCY_FOUND)
 * - Record discrepancy details (type + corrective action)
 * - Bulk apply QC to multiple samples
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryQCInspectionPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();

  // State for samples
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [roundInfo, setRoundInfo] = useState(null);
  const [isGeneratingRound, setIsGeneratingRound] = useState(false);
  const [loadingStorageOverview, setLoadingStorageOverview] = useState(false);

  // Storage overview/filter state (before random generation)
  const [storageFilters, setStorageFilters] = useState({
    freezer: ALL_OPTION,
    shelf: ALL_OPTION,
    rack: ALL_OPTION,
    box: ALL_OPTION,
  });
  const [storageOverviewData, setStorageOverviewData] = useState({
    counts: { freezers: 0, shelves: 0, racks: 0, boxes: 0, eligibleSamples: 0 },
    filters: { freezers: [], shelves: [], racks: [], boxes: [] },
    eligibleSamples: [],
  });
  const [roundSettings, setRoundSettings] = useState({
    boxesPerRound: "10",
    samplesPerBox: "3",
  });
  const [availableBoxes, setAvailableBoxes] = useState([]);
  const [loadingBoxes, setLoadingBoxes] = useState(false);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);
  const [selectedForBulkApply, setSelectedForBulkApply] = useState([]); // Capture selection when modal opens

  // Bulk apply form values
  const [bulkApplyValues, setBulkApplyValues] = useState({
    inspectorName: "",
    inspectionDate: new Date().toISOString().slice(0, 16),
    // QC Checklist - 5 boolean criteria
    qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
      acc[item.id] = false;
      return acc;
    }, {}),
    // Auto-calculated QC result
    qcResult: "",
    // Discrepancy details (only for failed QC)
    discrepancyType: "",
    correctiveAction: "",
    remarks: "",
    correctionActionType: "",
    correctionBoxId: "",
    correctionPositionCoordinate: "",
    correctionReason: "",
  });

  // Load stored samples for QC
  const loadStoredSamples = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/biorepository/qc-inspection/samples`,
      (response) => {
        setLoading(false);
        if (response && Array.isArray(response)) {
          // Transform API response to component state
          const transformedSamples = response.map((sample) => ({
            id: sample.bioSampleId,
            sampleItemId: sample.sampleItemId,
            externalId: sample.externalId || "-",
            accessionNumber: sample.accessionNumber || "-",
            sampleType: sample.sampleType || "-",
            locationPath: sample.locationPath || "Not Assigned",
            storageLocation: sample.storageLocation, // Full location object
            biosafetyLevel: sample.biosafetyLevel || "-",
            workflowStatus: sample.workflowStatus,
            lastQCInspection: sample.lastQCInspection, // Most recent inspection record
          }));
          setSamples(transformedSamples);
        } else {
          setSamples([]);
        }
      },
    );
  }, []);

  // Load samples on mount
  useEffect(() => {
    loadStoredSamples();
  }, [loadStoredSamples]);

  const loadStorageOverview = useCallback((filters) => {
    setLoadingStorageOverview(true);
    const query = buildStorageOverviewQuery(filters);
    getFromOpenElisServer(
      `/rest/biorepository/qc-inspection/storage-overview${query}`,
      (response) => {
        setLoadingStorageOverview(false);
        if (!response || response.error) {
          return;
        }
        setStorageOverviewData({
          counts: response.counts || {
            freezers: 0,
            shelves: 0,
            racks: 0,
            boxes: 0,
            eligibleSamples: 0,
          },
          filters: response.filters || {
            freezers: [],
            shelves: [],
            racks: [],
            boxes: [],
          },
          eligibleSamples: Array.isArray(response.eligibleSamples)
            ? response.eligibleSamples
            : [],
        });
      },
    );
  }, []);

  useEffect(() => {
    loadStorageOverview(storageFilters);
  }, [storageFilters, loadStorageOverview]);

  useEffect(() => {
    setLoadingBoxes(true);
    getFromOpenElisServer(`/rest/storage/boxes?active=true`, (response) => {
      setLoadingBoxes(false);
      if (!Array.isArray(response)) {
        setAvailableBoxes([]);
        return;
      }

      const normalized = response
        .filter((box) => box && box.id)
        .map((box) => ({
          id: String(box.id),
          label:
            box.hierarchicalPath || box.label || box.code || `Box ${box.id}`,
        }))
        .sort((a, b) => a.label.localeCompare(b.label));
      setAvailableBoxes(normalized);
    });
  }, []);

  const filterOptionItems = useMemo(() => {
    const toItems = (set, allLabel) => [
      { id: ALL_OPTION, label: allLabel },
      ...Array.from(set || [])
        .sort((a, b) => a.localeCompare(b))
        .map((value) => ({ id: value, label: value })),
    ];

    return {
      freezer: toItems(storageOverviewData.filters.freezers, "All freezers"),
      shelf: toItems(storageOverviewData.filters.shelves, "All shelves"),
      rack: toItems(storageOverviewData.filters.racks, "All racks"),
      box: toItems(storageOverviewData.filters.boxes, "All boxes"),
    };
  }, [storageOverviewData.filters]);

  const eligibleSampleIdSet = useMemo(
    () =>
      new Set(
        (storageOverviewData.eligibleSamples || [])
          .map((sample) => String(sample.bioSampleId))
          .filter(Boolean),
      ),
    [storageOverviewData.eligibleSamples],
  );

  const filteredSamples = useMemo(
    () =>
      samples.filter((sample) => eligibleSampleIdSet.has(String(sample.id))),
    [samples, eligibleSampleIdSet],
  );

  const storageOverview = useMemo(
    () => ({
      freezers: storageOverviewData.counts.freezers || 0,
      shelves: storageOverviewData.counts.shelves || 0,
      racks: storageOverviewData.counts.racks || 0,
      boxes: storageOverviewData.counts.boxes || 0,
      samples: storageOverviewData.counts.eligibleSamples || 0,
    }),
    [storageOverviewData.counts],
  );

  const generateRandomRound = useCallback(() => {
    const boxesPerRound = Math.max(
      parseInt(roundSettings.boxesPerRound, 10) || 0,
      1,
    );
    const samplesPerBox = Math.max(
      parseInt(roundSettings.samplesPerBox, 10) || 0,
      1,
    );

    const eligibleSamples = storageOverviewData.eligibleSamples || [];
    if (eligibleSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noSamplesForFilters",
          defaultMessage:
            "No samples match the current storage filters. Adjust filters before generating.",
        }),
      );
      return;
    }

    setIsGeneratingRound(true);
    setError(null);

    const groupedByBox = new Map();
    eligibleSamples.forEach((sample) => {
      const key = [
        sample.freezer || "Unknown",
        sample.shelf || "Unknown",
        sample.rack || "Unknown",
        sample.box || "Unknown",
      ].join(" > ");
      if (!groupedByBox.has(key)) {
        groupedByBox.set(key, []);
      }
      groupedByBox.get(key).push(sample);
    });

    const shuffle = (arr) => {
      const copy = [...arr];
      for (let i = copy.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [copy[i], copy[j]] = [copy[j], copy[i]];
      }
      return copy;
    };

    const boxDescriptors = Array.from(groupedByBox.keys()).map((boxKey) => {
      const [freezer, shelf, rack] = boxKey.split(" > ");
      return {
        boxKey,
        shelfKey: `${freezer || "Unknown"} > ${shelf || "Unknown"}`,
        rackKey: `${freezer || "Unknown"} > ${shelf || "Unknown"} > ${
          rack || "Unknown"
        }`,
      };
    });

    const targetBoxCount = Math.min(boxesPerRound, boxDescriptors.length);
    const selectedBoxKeys = [];
    const selectedSet = new Set();

    // Pass 1: maximize shelf distribution.
    const boxesByShelf = new Map();
    boxDescriptors.forEach((descriptor) => {
      if (!boxesByShelf.has(descriptor.shelfKey)) {
        boxesByShelf.set(descriptor.shelfKey, []);
      }
      boxesByShelf.get(descriptor.shelfKey).push(descriptor.boxKey);
    });

    shuffle(Array.from(boxesByShelf.keys())).forEach((shelfKey) => {
      if (selectedBoxKeys.length >= targetBoxCount) return;
      const candidate = shuffle(boxesByShelf.get(shelfKey)).find(
        (boxKey) => !selectedSet.has(boxKey),
      );
      if (candidate) {
        selectedBoxKeys.push(candidate);
        selectedSet.add(candidate);
      }
    });

    // Pass 2: maximize rack distribution.
    const boxesByRack = new Map();
    boxDescriptors.forEach((descriptor) => {
      if (!boxesByRack.has(descriptor.rackKey)) {
        boxesByRack.set(descriptor.rackKey, []);
      }
      boxesByRack.get(descriptor.rackKey).push(descriptor.boxKey);
    });

    shuffle(Array.from(boxesByRack.keys())).forEach((rackKey) => {
      if (selectedBoxKeys.length >= targetBoxCount) return;
      const candidate = shuffle(boxesByRack.get(rackKey)).find(
        (boxKey) => !selectedSet.has(boxKey),
      );
      if (candidate) {
        selectedBoxKeys.push(candidate);
        selectedSet.add(candidate);
      }
    });

    // Pass 3: fill remaining slots randomly from any unselected boxes.
    const remainingBoxes = shuffle(boxDescriptors.map((d) => d.boxKey)).filter(
      (boxKey) => !selectedSet.has(boxKey),
    );
    remainingBoxes.forEach((boxKey) => {
      if (selectedBoxKeys.length >= targetBoxCount) return;
      selectedBoxKeys.push(boxKey);
      selectedSet.add(boxKey);
    });

    const selectedSamples = [];
    selectedBoxKeys.forEach((boxKey) => {
      const boxSamples = [...(groupedByBox.get(boxKey) || [])];
      for (let i = boxSamples.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [boxSamples[i], boxSamples[j]] = [boxSamples[j], boxSamples[i]];
      }
      selectedSamples.push(
        ...boxSamples.slice(0, Math.min(samplesPerBox, boxSamples.length)),
      );
    });

    if (selectedSamples.length === 0) {
      setIsGeneratingRound(false);
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.emptyRound",
          defaultMessage:
            "No eligible samples were found to generate a QC round.",
        }),
      );
      return;
    }

    setSelectedForBulkApply(
      selectedSamples.map((sample) => String(sample.bioSampleId)),
    );
    setRoundInfo({
      boxesSelected: selectedBoxKeys.length,
      samplesSelected: selectedSamples.length,
      filteredSamplePool: eligibleSamples.length,
    });
    resetBulkApplyValues();
    setBulkApplyModalOpen(true);
    setIsGeneratingRound(false);
  }, [
    intl,
    roundSettings.boxesPerRound,
    roundSettings.samplesPerBox,
    storageOverviewData.eligibleSamples,
  ]);

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      inspectorName: "",
      inspectionDate: new Date().toISOString().slice(0, 16),
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "",
      discrepancyType: "",
      correctiveAction: "",
      remarks: "",
      correctionActionType: "",
      correctionBoxId: "",
      correctionPositionCoordinate: "",
      correctionReason: "",
    });
  };

  // Calculate QC result based on checklist
  const calculateQCResult = (checklist) => {
    const allPassed = Object.values(checklist).every((v) => v === true);
    return allPassed ? "VERIFIED" : "DISCREPANCY_FOUND";
  };

  // Handle checklist change
  const handleChecklistChange = (criteriaId, checked) => {
    setBulkApplyValues((prev) => {
      const newChecklist = { ...prev.qcChecklist, [criteriaId]: checked };
      const autoResult = calculateQCResult(newChecklist);
      return {
        ...prev,
        qcChecklist: newChecklist,
        qcResult: autoResult,
        // Clear fail-related fields if now passing
        discrepancyType: autoResult === "VERIFIED" ? "" : prev.discrepancyType,
        correctiveAction:
          autoResult === "VERIFIED" ? "" : prev.correctiveAction,
        correctionActionType:
          autoResult === "VERIFIED" ? "" : prev.correctionActionType,
        correctionBoxId: autoResult === "VERIFIED" ? "" : prev.correctionBoxId,
        correctionPositionCoordinate:
          autoResult === "VERIFIED" ? "" : prev.correctionPositionCoordinate,
        correctionReason: autoResult === "VERIFIED" ? "" : prev.correctionReason,
      };
    });
  };

  // Handle "Check All" for QC criteria
  const handleCheckAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = true;
        return acc;
      }, {}),
      qcResult: "VERIFIED",
      discrepancyType: "",
      correctiveAction: "",
      correctionActionType: "",
      correctionBoxId: "",
      correctionPositionCoordinate: "",
      correctionReason: "",
    }));
  };

  // Handle "Clear All" for QC criteria
  const handleClearAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "DISCREPANCY_FOUND",
    }));
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedForBulkApply.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noSelection",
          defaultMessage: "Please select samples to apply QC to.",
        }),
      );
      return;
    }

    // Validate: Inspector name required
    if (!bulkApplyValues.inspectorName.trim()) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noInspector",
          defaultMessage: "Please enter inspector name.",
        }),
      );
      return;
    }

    // Validate: QC result must be set
    if (!bulkApplyValues.qcResult) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noResult",
          defaultMessage:
            "Please complete the QC checklist to determine pass/fail status.",
        }),
      );
      return;
    }

    // Validate: If discrepancy found, must have discrepancy type and corrective action
    if (bulkApplyValues.qcResult === "DISCREPANCY_FOUND") {
      if (!bulkApplyValues.discrepancyType) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noDiscrepancyType",
            defaultMessage: "Please select a discrepancy type.",
          }),
        );
        return;
      }
      if (!bulkApplyValues.correctiveAction.trim()) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectiveAction",
            defaultMessage: "Please describe the corrective action taken.",
          }),
        );
        return;
      }
      if (!bulkApplyValues.remarks.trim()) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noRemarks",
            defaultMessage:
              "Please enter comment/remarks for discrepancy findings.",
          }),
        );
        return;
      }

      if (
        selectedForBulkApply.length === 1 &&
        !bulkApplyValues.correctionActionType
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectionAction",
            defaultMessage:
              "Select a correction workflow action (update location, reassign position, or mark missing).",
          }),
        );
        return;
      }

      if (
        bulkApplyValues.correctionActionType &&
        selectedForBulkApply.length !== 1
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.correctionSingleOnly",
            defaultMessage:
              "Correction workflow currently supports one sample at a time. Select a single sample for correction.",
          }),
        );
        return;
      }

      if (
        bulkApplyValues.correctionActionType === "MARK_MISSING" &&
        !isMissingDiscrepancyType(bulkApplyValues.discrepancyType)
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.markMissingRequiresMissingDiscrepancy",
            defaultMessage:
              "Mark missing is only allowed when discrepancy type is Sample missing.",
          }),
        );
        return;
      }

      if (hasInvalidMarkMissingLocationFields(bulkApplyValues)) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.markMissingLocationFieldsNotAllowed",
            defaultMessage:
              "Location and position inputs are not allowed for Mark missing correction.",
          }),
        );
        return;
      }

      if (
        ["UPDATE_LOCATION", "REASSIGN_POSITION"].includes(
          bulkApplyValues.correctionActionType,
        ) &&
        !bulkApplyValues.correctionBoxId
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectionLocation",
            defaultMessage:
              "Select a target storage box for update/reassign correction.",
          }),
        );
        return;
      }

      if (
        bulkApplyValues.correctionActionType === "REASSIGN_POSITION" &&
        !bulkApplyValues.correctionPositionCoordinate.trim()
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectionPosition",
            defaultMessage:
              "Enter the new position coordinate for reassign position correction.",
          }),
        );
        return;
      }
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare request payload
    const payload = {
      bioSampleIds: selectedForBulkApply.map((id) => parseInt(id, 10)),
      inspectorName: bulkApplyValues.inspectorName.trim(),
      inspectionDate: bulkApplyValues.inspectionDate
        ? new Date(bulkApplyValues.inspectionDate).toISOString()
        : null,
      samplePresent: bulkApplyValues.qcChecklist.samplePresent,
      labelIntegrity: bulkApplyValues.qcChecklist.labelIntegrity,
      containerIntegrity: bulkApplyValues.qcChecklist.containerIntegrity,
      volumeAppearanceAcceptable:
        bulkApplyValues.qcChecklist.volumeAppearanceAcceptable,
      correctPosition: bulkApplyValues.qcChecklist.correctPosition,
      discrepancyType:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
          ? bulkApplyValues.discrepancyType
          : null,
      correctiveAction:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
          ? bulkApplyValues.correctiveAction
          : null,
      remarks: bulkApplyValues.remarks || null,
      correctionActionType:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND" &&
        bulkApplyValues.correctionActionType
          ? bulkApplyValues.correctionActionType
          : null,
      correctionLocationType:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND" &&
        ["UPDATE_LOCATION", "REASSIGN_POSITION"].includes(
          bulkApplyValues.correctionActionType,
        )
          ? "box"
          : null,
      correctionLocationId:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND" &&
        ["UPDATE_LOCATION", "REASSIGN_POSITION"].includes(
          bulkApplyValues.correctionActionType,
        )
          ? bulkApplyValues.correctionBoxId
          : null,
      correctionPositionCoordinate:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND" &&
        bulkApplyValues.correctionActionType === "REASSIGN_POSITION"
          ? bulkApplyValues.correctionPositionCoordinate || null
          : null,
      correctionReason:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
          ? bulkApplyValues.correctionReason || null
          : null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/biorepository/qc-inspection/bulk-apply`,
      JSON.stringify(payload),
      (response) => {
        setIsBulkApplying(false);
        if (response && !response.error) {
          const count = response.count || selectedForBulkApply.length;
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "biorepository.qc.success.applied",
                defaultMessage: "QC inspection applied to {count} sample(s).",
              },
              { count },
            ),
          );
          setBulkApplyModalOpen(false);
          resetBulkApplyValues();
          setSelectedForBulkApply([]); // Clear captured selection
          loadStoredSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "biorepository.qc.error.apply",
                defaultMessage:
                  "Failed to apply QC inspection. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedForBulkApply,
    bulkApplyValues,
    intl,
    loadStoredSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = samples.length;
  const verifiedCount = samples.filter(
    (s) => s.lastQCInspection && s.lastQCInspection.qcResult === "VERIFIED",
  ).length;
  const discrepanciesCount = samples.filter(
    (s) =>
      s.lastQCInspection && s.lastQCInspection.qcResult === "DISCREPANCY_FOUND",
  ).length;
  const pendingCount = samples.filter((s) => !s.lastQCInspection).length;

  // Count checked criteria
  const checkedCount = useMemo(() => {
    return Object.values(bulkApplyValues.qcChecklist).filter((v) => v).length;
  }, [bulkApplyValues.qcChecklist]);

  const correctionActionOptions = useMemo(
    () =>
      getAllowedCorrectionActions(
        CORRECTION_ACTIONS,
        bulkApplyValues.discrepancyType,
      ),
    [bulkApplyValues.discrepancyType],
  );

  useEffect(() => {
    if (
      bulkApplyValues.correctionActionType === "MARK_MISSING" &&
      !isMissingDiscrepancyType(bulkApplyValues.discrepancyType)
    ) {
      setBulkApplyValues((prev) => ({
        ...prev,
        correctionActionType: "",
        correctionBoxId: "",
        correctionPositionCoordinate: "",
      }));
    }
  }, [bulkApplyValues.correctionActionType, bulkApplyValues.discrepancyType]);

  // Get QC result tag
  const getQCTag = (qcResult, qcStatus) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcStatus === "MISSING") return <Tag type="purple">Missing</Tag>;
    if (qcResult === "VERIFIED") return <Tag type="green">Verified</Tag>;
    if (qcResult === "DISCREPANCY_FOUND")
      return <Tag type="red">Discrepancy</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "biorepository.sample.accessionNumber",
        defaultMessage: "Sample Number",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "locationPath",
      header: intl.formatMessage({
        id: "biorepository.sample.storageLocation",
        defaultMessage: "Storage Location",
      }),
    },
    {
      key: "biosafetyLevel",
      header: intl.formatMessage({
        id: "biorepository.sample.bsl",
        defaultMessage: "BSL",
      }),
    },
    {
      key: "lastQCInspection",
      header: intl.formatMessage({
        id: "biorepository.qc.lastInspection",
        defaultMessage: "Last QC Inspection",
      }),
    },
  ];

  return (
    <div className="biorepository-qc-inspection-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="biorepository.qc.title"
            defaultMessage="Quality Control Inspection"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="biorepository.qc.description"
            defaultMessage="Perform visual inspection and verification of stored samples. Use the QC checklist to assess physical presence, label integrity, container condition, volume/appearance, and storage position. Samples with discrepancies require corrective action documentation."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.totalStored"
                  defaultMessage="Total Stored"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.discrepancies"
                  defaultMessage="Discrepancies"
                />
              </span>
              <span className="progress-value">{discrepanciesCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {roundInfo && (
        <InlineNotification
          kind="info"
          title={intl.formatMessage(
            {
              id: "biorepository.qc.round.generated",
              defaultMessage:
                "QC round generated from filtered storage scope: {boxes} box(es), {samples} sample(s) from {pool} eligible sample(s).",
            },
            {
              boxes: roundInfo.boxesSelected,
              samples: roundInfo.samplesSelected,
              pool: roundInfo.filteredSamplePool,
            },
          )}
          lowContrast
        />
      )}

      {/* Storage overview + scope filters (must be correct before random generation) */}
      <Grid
        fullWidth
        className="progress-section"
        style={{ marginTop: "1rem" }}
      >
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">Freezers</span>
              <span className="progress-value">{storageOverview.freezers}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">Shelves</span>
              <span className="progress-value">{storageOverview.shelves}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">Racks</span>
              <span className="progress-value">{storageOverview.racks}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">Boxes</span>
              <span className="progress-value">{storageOverview.boxes}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">Eligible Samples</span>
              <span className="progress-value">{storageOverview.samples}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth style={{ marginTop: "0.75rem", marginBottom: "0.75rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="qc-filter-freezer"
            titleText="Freezer"
            items={filterOptionItems.freezer}
            selectedItem={
              filterOptionItems.freezer.find(
                (item) => item.id === storageFilters.freezer,
              ) || filterOptionItems.freezer[0]
            }
            itemToString={(item) => item?.label || ""}
            onChange={({ selectedItem }) =>
              setStorageFilters((prev) => ({
                ...prev,
                freezer: selectedItem?.id || ALL_OPTION,
                shelf: ALL_OPTION,
                rack: ALL_OPTION,
                box: ALL_OPTION,
              }))
            }
            disabled={loadingStorageOverview}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="qc-filter-shelf"
            titleText="Shelf"
            items={filterOptionItems.shelf}
            selectedItem={
              filterOptionItems.shelf.find(
                (item) => item.id === storageFilters.shelf,
              ) || filterOptionItems.shelf[0]
            }
            itemToString={(item) => item?.label || ""}
            onChange={({ selectedItem }) =>
              setStorageFilters((prev) => ({
                ...prev,
                shelf: selectedItem?.id || ALL_OPTION,
                rack: ALL_OPTION,
                box: ALL_OPTION,
              }))
            }
            disabled={loadingStorageOverview}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="qc-filter-rack"
            titleText="Rack"
            items={filterOptionItems.rack}
            selectedItem={
              filterOptionItems.rack.find(
                (item) => item.id === storageFilters.rack,
              ) || filterOptionItems.rack[0]
            }
            itemToString={(item) => item?.label || ""}
            onChange={({ selectedItem }) =>
              setStorageFilters((prev) => ({
                ...prev,
                rack: selectedItem?.id || ALL_OPTION,
                box: ALL_OPTION,
              }))
            }
            disabled={loadingStorageOverview}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="qc-filter-box"
            titleText="Box"
            items={filterOptionItems.box}
            selectedItem={
              filterOptionItems.box.find(
                (item) => item.id === storageFilters.box,
              ) || filterOptionItems.box[0]
            }
            itemToString={(item) => item?.label || ""}
            onChange={({ selectedItem }) =>
              setStorageFilters((prev) => ({
                ...prev,
                box: selectedItem?.id || ALL_OPTION,
              }))
            }
            disabled={loadingStorageOverview}
          />
        </Column>
      </Grid>

      <Grid fullWidth style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="qc-boxes-per-round"
            labelText="Boxes per round"
            value={roundSettings.boxesPerRound}
            onChange={(e) =>
              setRoundSettings((prev) => ({
                ...prev,
                boxesPerRound: e.target.value.replace(/[^\d]/g, ""),
              }))
            }
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="qc-samples-per-box"
            labelText="Samples per box"
            value={roundSettings.samplesPerBox}
            onChange={(e) =>
              setRoundSettings((prev) => ({
                ...prev,
                samplesPerBox: e.target.value.replace(/[^\d]/g, ""),
              }))
            }
          />
        </Column>
        <Column
          lg={8}
          md={8}
          sm={4}
          style={{ display: "flex", alignItems: "flex-end" }}
        >
          <Button
            kind="secondary"
            size="sm"
            onClick={generateRandomRound}
            disabled={
              isGeneratingRound ||
              loadingStorageOverview ||
              storageOverview.samples === 0
            }
          >
            Generate Random QC Round
          </Button>
        </Column>
      </Grid>

      {/* Samples Table */}
      <div className="sample-table-section" style={{ marginTop: "1rem" }}>
        {loading ? (
          <div style={{ padding: "2rem", textAlign: "center" }}>
            <Loading withOverlay={false} />
          </div>
        ) : filteredSamples.length === 0 ? (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "biorepository.qc.noSamples",
              defaultMessage: "No Samples in Current Storage Scope",
            })}
            subtitle={intl.formatMessage({
              id: "biorepository.qc.noSamples.message",
              defaultMessage:
                "No STORED samples are currently eligible under the selected freezer/shelf/rack/box scope.",
            })}
            lowContrast
            hideCloseButton
          />
        ) : (
          <DataTable
            rows={filteredSamples.map((sample) => ({
              id: sample.id.toString(),
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType,
              locationPath: sample.locationPath,
              biosafetyLevel: sample.biosafetyLevel,
              lastQCInspection: sample.lastQCInspection,
              _raw: sample,
            }))}
            headers={headers}
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
              // Get selected IDs from Carbon's DataTable state (read-only, no setState in render)
              const currentSelectedIds = selectedRows.map((r) => r.id);

              return (
                <TableContainer>
                  <TableToolbar>
                    <TableBatchActions {...getBatchActionProps()}>
                      <TableBatchAction
                        renderIcon={Edit}
                        iconDescription={intl.formatMessage({
                          id: "biorepository.qc.bulkApply",
                          defaultMessage: "Bulk Apply QC",
                        })}
                        onClick={() => {
                          // Capture selection when modal opens
                          setSelectedForBulkApply(currentSelectedIds);
                          resetBulkApplyValues();
                          setBulkApplyModalOpen(true);
                        }}
                      >
                        <FormattedMessage
                          id="biorepository.qc.bulkApply"
                          defaultMessage="Bulk Apply QC"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={Renew}
                        iconDescription={intl.formatMessage({
                          id: "label.refresh",
                          defaultMessage: "Refresh",
                        })}
                        hasIconOnly
                        onClick={loadStoredSamples}
                        disabled={loading}
                      />
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
                        const sample = filteredSamples.find(
                          (s) => s.id.toString() === row.id,
                        );
                        return (
                          <TableRow {...getRowProps({ row })} key={row.id}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => {
                              if (cell.info.header === "biosafetyLevel") {
                                let bslColor = "gray";
                                if (cell.value === "BSL_1") bslColor = "green";
                                else if (cell.value === "BSL_2")
                                  bslColor = "teal";
                                else if (cell.value === "BSL_3")
                                  bslColor = "purple";
                                else if (cell.value === "BSL_4")
                                  bslColor = "red";
                                return (
                                  <TableCell key={cell.id}>
                                    <Tag type={bslColor}>{cell.value}</Tag>
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "lastQCInspection") {
                                if (!sample?.lastQCInspection) {
                                  return (
                                    <TableCell key={cell.id}>
                                      <Tag type="gray">Never Inspected</Tag>
                                    </TableCell>
                                  );
                                }
                                const qc = sample.lastQCInspection;
                                return (
                                  <TableCell key={cell.id}>
                                    <div
                                      style={{
                                        display: "flex",
                                        gap: "0.5rem",
                                        alignItems: "center",
                                      }}
                                    >
                                      {getQCTag(qc.qcResult, qc.qcStatus)}
                                      <span
                                        style={{
                                          fontSize: "0.75rem",
                                          color: "#525252",
                                        }}
                                      >
                                        {new Date(
                                          qc.inspectionDate,
                                        ).toLocaleDateString()}
                                      </span>
                                    </div>
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
        )}
      </div>

      {/* Bulk Apply QC Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.qc.bulkApply.title",
          defaultMessage: "Bulk QC Inspection",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "VERIFIED"
              ? intl.formatMessage({
                  id: "biorepository.qc.action.verify",
                  defaultMessage: "Record Verification",
                })
              : bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
                ? intl.formatMessage({
                    id: "biorepository.qc.action.recordDiscrepancy",
                    defaultMessage: "Record Discrepancy",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="md"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "DISCREPANCY_FOUND"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="biorepository.qc.bulkApply.description"
              defaultMessage="Apply QC inspection to {count} selected sample(s)."
              values={{ count: selectedForBulkApply.length }}
            />
          </p>

          {/* Inspector Information */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.inspector"
                defaultMessage="Inspector Information"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="inspectorName"
                  labelText={intl.formatMessage({
                    id: "biorepository.qc.inspectorName",
                    defaultMessage: "Inspector Name (Required)",
                  })}
                  value={bulkApplyValues.inspectorName}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      inspectorName: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "biorepository.qc.inspectorName.placeholder",
                    defaultMessage: "Enter inspector name",
                  })}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="biorepository.qc.inspectionDate"
                      defaultMessage="Inspection Date & Time"
                    />
                  </label>
                  <input
                    type="datetime-local"
                    className="cds--text-input"
                    value={bulkApplyValues.inspectionDate}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        inspectionDate: e.target.value,
                      }))
                    }
                  />
                </div>
              </Column>
            </Grid>
          </div>

          {/* QC Checklist Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.checklist"
                defaultMessage="QC Checklist"
              />
              <span className="qc-checklist-count">
                ({checkedCount}/{QC_CHECKLIST.length})
              </span>
            </h5>
            <div className="qc-checklist-actions">
              <Button kind="ghost" size="sm" onClick={handleCheckAll}>
                <FormattedMessage
                  id="biorepository.qc.checkAll"
                  defaultMessage="Check All (Verify)"
                />
              </Button>
              <Button kind="ghost" size="sm" onClick={handleClearAll}>
                <FormattedMessage
                  id="biorepository.qc.clearAll"
                  defaultMessage="Clear All"
                />
              </Button>
            </div>
            <div className="qc-checklist-items">
              {QC_CHECKLIST.map((criteria) => (
                <Checkbox
                  key={criteria.id}
                  id={`qc-${criteria.id}`}
                  labelText={intl.formatMessage({
                    id: criteria.labelId,
                    defaultMessage: criteria.defaultLabel,
                  })}
                  checked={bulkApplyValues.qcChecklist[criteria.id]}
                  onChange={(_, { checked }) =>
                    handleChecklistChange(criteria.id, checked)
                  }
                />
              ))}
            </div>
          </div>

          {/* QC Result Indicator */}
          <div className="qc-section qc-decision-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.result"
                defaultMessage="QC Result"
              />
            </h5>
            <div
              className={`qc-result-indicator ${bulkApplyValues.qcResult === "VERIFIED" ? "pass" : bulkApplyValues.qcResult === "DISCREPANCY_FOUND" ? "fail" : ""}`}
            >
              {bulkApplyValues.qcResult === "VERIFIED" && (
                <Tag type="green" size="md">
                  <Checkmark size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="biorepository.qc.result.verified"
                    defaultMessage="QC VERIFIED - All checks passed"
                  />
                </Tag>
              )}
              {bulkApplyValues.qcResult === "DISCREPANCY_FOUND" && (
                <Tag type="red" size="md">
                  <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="biorepository.qc.result.discrepancy"
                    defaultMessage="DISCREPANCY FOUND - Corrective action required"
                  />
                </Tag>
              )}
              {!bulkApplyValues.qcResult && (
                <Tag type="gray" size="md">
                  <FormattedMessage
                    id="biorepository.qc.result.pending"
                    defaultMessage="Complete checklist to determine result"
                  />
                </Tag>
              )}
            </div>
          </div>

          {/* Discrepancy Details - Only show if QC result is DISCREPANCY_FOUND */}
          {bulkApplyValues.qcResult === "DISCREPANCY_FOUND" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="biorepository.qc.section.discrepancy"
                  defaultMessage="Discrepancy Details"
                />
              </h5>
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <Dropdown
                    id="discrepancyType"
                    titleText={intl.formatMessage({
                      id: "biorepository.qc.discrepancyType",
                      defaultMessage: "Discrepancy Type (Required)",
                    })}
                    label={intl.formatMessage({
                      id: "biorepository.qc.discrepancyType.placeholder",
                      defaultMessage: "Select discrepancy type",
                    })}
                    items={DISCREPANCY_TYPES}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={DISCREPANCY_TYPES.find(
                      (d) => d.id === bulkApplyValues.discrepancyType,
                    )}
                    onChange={({ selectedItem }) => {
                      const nextDiscrepancyType = selectedItem?.id || "";
                      setBulkApplyValues((prev) => {
                        const shouldClearMarkMissing =
                          prev.correctionActionType === "MARK_MISSING" &&
                          !isMissingDiscrepancyType(nextDiscrepancyType);
                        return {
                          ...prev,
                          discrepancyType: nextDiscrepancyType,
                          correctionActionType: shouldClearMarkMissing
                            ? ""
                            : prev.correctionActionType,
                          correctionBoxId: shouldClearMarkMissing
                            ? ""
                            : prev.correctionBoxId,
                          correctionPositionCoordinate: shouldClearMarkMissing
                            ? ""
                            : prev.correctionPositionCoordinate,
                        };
                      });
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="correctiveAction"
                    labelText={intl.formatMessage({
                      id: "biorepository.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken (Required)",
                    })}
                    value={bulkApplyValues.correctiveAction}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        correctiveAction: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "biorepository.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe the corrective action taken to resolve the discrepancy...",
                    })}
                    rows={3}
                  />
                </Column>
              </Grid>

              {selectedForBulkApply.length > 1 && (
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "biorepository.qc.correction.multipleSelection",
                    defaultMessage:
                      "Correction workflow actions can be executed for one sample at a time.",
                  })}
                  subtitle={intl.formatMessage({
                    id: "biorepository.qc.correction.multipleSelectionSubtitle",
                    defaultMessage:
                      "For this bulk discrepancy record, correction action fields are optional. Re-open with a single sample to apply location/missing corrections.",
                  })}
                  lowContrast
                />
              )}

              <Grid fullWidth style={{ marginTop: "1rem" }}>
                <Column lg={16} md={8} sm={4}>
                  <Dropdown
                    id="correctionActionType"
                    titleText={intl.formatMessage({
                      id: "biorepository.qc.correctionAction",
                      defaultMessage:
                        selectedForBulkApply.length === 1
                          ? "Correction Workflow Action (Required for single-sample fail)"
                          : "Correction Workflow Action (Optional for bulk fail)",
                    })}
                    label={intl.formatMessage({
                      id: "biorepository.qc.correctionAction.placeholder",
                      defaultMessage: "Select correction action",
                    })}
                    items={correctionActionOptions}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={correctionActionOptions.find(
                      (action) => action.id === bulkApplyValues.correctionActionType,
                    )}
                    onChange={({ selectedItem }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        correctionActionType: selectedItem?.id || "",
                        correctionBoxId: "",
                        correctionPositionCoordinate: "",
                      }))
                    }
                  />
                </Column>

                {["UPDATE_LOCATION", "REASSIGN_POSITION"].includes(
                  bulkApplyValues.correctionActionType,
                ) && (
                  <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                    <Dropdown
                      id="correctionBoxId"
                      titleText={intl.formatMessage({
                        id: "biorepository.qc.correctionTargetBox",
                        defaultMessage: "Target Box",
                      })}
                      label={intl.formatMessage({
                        id: "biorepository.qc.correctionTargetBox.placeholder",
                        defaultMessage: "Select target box",
                      })}
                      items={availableBoxes}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={availableBoxes.find(
                        (box) => box.id === bulkApplyValues.correctionBoxId,
                      )}
                      onChange={({ selectedItem }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          correctionBoxId: selectedItem?.id || "",
                        }))
                      }
                      disabled={loadingBoxes}
                    />
                  </Column>
                )}

                {bulkApplyValues.correctionActionType === "REASSIGN_POSITION" && (
                  <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                    <TextInput
                      id="correctionPositionCoordinate"
                      labelText={intl.formatMessage({
                        id: "biorepository.qc.correctionPositionCoordinate",
                        defaultMessage: "New Position Coordinate",
                      })}
                      value={bulkApplyValues.correctionPositionCoordinate}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          correctionPositionCoordinate: e.target.value,
                        }))
                      }
                      placeholder={intl.formatMessage({
                        id: "biorepository.qc.correctionPositionCoordinate.placeholder",
                        defaultMessage: "e.g., A3",
                      })}
                    />
                  </Column>
                )}

                {bulkApplyValues.correctionActionType && (
                  <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                    <TextArea
                      id="correctionReason"
                      labelText={intl.formatMessage({
                        id: "biorepository.qc.correctionReason",
                        defaultMessage: "Correction Reason (Optional)",
                      })}
                      value={bulkApplyValues.correctionReason}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          correctionReason: e.target.value,
                        }))
                      }
                      rows={2}
                      placeholder={intl.formatMessage({
                        id: "biorepository.qc.correctionReason.placeholder",
                        defaultMessage:
                          "Reason shown in audit trail (falls back to corrective action text if empty)",
                      })}
                    />
                  </Column>
                )}
              </Grid>
            </div>
          )}

          {/* Remarks (Optional) */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="remarks"
                  labelText={intl.formatMessage({
                    id: "biorepository.qc.remarks",
                    defaultMessage:
                      bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
                        ? "Remarks / Comment (Required for discrepancy)"
                        : "Remarks (Optional)",
                  })}
                  value={bulkApplyValues.remarks}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      remarks: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "biorepository.qc.remarks.placeholder",
                    defaultMessage: "Additional observations or notes...",
                  })}
                  rows={2}
                />
              </Column>
            </Grid>
          </div>
        </div>
      </Modal>
    </div>
  );
}

BiorepositoryQCInspectionPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryQCInspectionPage;
