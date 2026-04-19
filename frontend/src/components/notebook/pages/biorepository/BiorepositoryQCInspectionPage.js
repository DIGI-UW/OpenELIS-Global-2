import React, { useState, useCallback, useEffect, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  ButtonSet,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  Tag,
  TextInput,
  Checkbox,
  Dropdown,
  Toggle,
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

const DISCREPANCY_TYPES = [
  { id: "MISSING_SAMPLE", label: "Missing Sample" },
  { id: "WRONG_SAMPLE_IN_POSITION", label: "Wrong Sample In Position" },
  { id: "DAMAGED_LABEL", label: "Damaged/Illegible Label" },
  { id: "MISPLACED_ITEM", label: "Misplaced Item (Wrong Position)" },
  {
    id: "EMPTY_POSITION_REGISTERED_OCCUPIED",
    label: "Empty Position But Registered Occupied",
  },
  { id: "LABELING_ERROR", label: "Labeling Error" },
  { id: "BOX_RACK_MISPLACEMENT", label: "Box/Rack Misplacement" },
  { id: "CONTAINER_DAMAGE", label: "Container Damage" },
  { id: "VOLUME_DISCREPANCY", label: "Volume Discrepancy" },
  { id: "OTHER", label: "Other" },
];

const ALL_FREEZERS = "All freezers";
const ALL_SHELVES = "All shelves";
const ALL_RACKS = "All racks";

const createBlankChecklist = () =>
  QC_CHECKLIST.reduce((acc, item) => {
    acc[item.id] = false;
    return acc;
  }, {});

const createInitialInspectionForm = () => ({
  inspectorName: "",
  inspectionDate: new Date().toISOString().slice(0, 16),
  qcChecklist: createBlankChecklist(),
  qcResult: "",
  discrepancyType: "",
  correctiveAction: "",
  remarks: "",
});

const toSafeString = (value, fallback = "") => {
  if (value === null || value === undefined) {
    return fallback;
  }
  const asString = String(value).trim();
  return asString || fallback;
};

const parseLocationPath = (
  locationPath,
  positionCoordinate,
  locationDetails = {},
) => {
  const path = toSafeString(locationPath);
  const coordinate = toSafeString(positionCoordinate);
  const explicitFreezer = toSafeString(locationDetails.deviceName);
  const explicitShelf = toSafeString(locationDetails.shelfLabel);
  const explicitRack = toSafeString(locationDetails.rackLabel);
  const explicitBox = toSafeString(locationDetails.boxLabel);

  const segments = path
    .split(/\s*>\s*/)
    .map((part) => part.trim())
    .filter(Boolean);

  let freezer;
  let shelf;
  let rack;
  let box;

  segments.forEach((segment) => {
    const lower = segment.toLowerCase();
    if (!freezer && lower.includes("freezer")) {
      freezer = segment;
    } else if (!shelf && lower.includes("shelf")) {
      shelf = segment;
    } else if (!rack && lower.includes("rack")) {
      rack = segment;
    } else if (!box && (lower.includes("box") || lower.startsWith("bx"))) {
      box = segment;
    }
  });

  if (!freezer && segments[0]) {
    freezer = segments[0];
  }
  if (!shelf && segments[1]) {
    shelf = segments[1];
  }
  if (!rack && segments[2]) {
    rack = segments[2];
  }
  if (!box && segments[3]) {
    box = segments[3];
  }
  if (!box && segments.length > 0) {
    box = segments[segments.length - 1];
  }

  const normalizedFreezer = explicitFreezer || freezer || "Unknown Freezer";
  const normalizedShelf = explicitShelf || shelf || "Unknown Shelf";
  const normalizedRack = explicitRack || rack || "Unknown Rack";
  const normalizedBox = explicitBox || box || "Unknown Box";

  return {
    freezer: normalizedFreezer,
    shelf: normalizedShelf,
    rack: normalizedRack,
    box: normalizedBox,
    positionCoordinate: coordinate || "-",
    shelfKey: `${normalizedFreezer} > ${normalizedShelf}`,
    rackKey: `${normalizedFreezer} > ${normalizedShelf} > ${normalizedRack}`,
    boxKey: `${normalizedFreezer} > ${normalizedShelf} > ${normalizedRack} > ${normalizedBox}`,
  };
};

const computeOverview = (samples) => {
  const freezers = new Set();
  const shelves = new Set();
  const racks = new Set();
  const boxes = new Set();
  const freezerSummaryMap = new Map();

  samples.forEach((sample) => {
    freezers.add(sample.freezer);
    shelves.add(sample.shelfKey);
    racks.add(sample.rackKey);
    boxes.add(sample.boxKey);

    if (!freezerSummaryMap.has(sample.freezer)) {
      freezerSummaryMap.set(sample.freezer, {
        freezer: sample.freezer,
        sampleCount: 0,
        shelfSet: new Set(),
        rackSet: new Set(),
        boxSet: new Set(),
      });
    }

    const row = freezerSummaryMap.get(sample.freezer);
    row.sampleCount += 1;
    row.shelfSet.add(sample.shelfKey);
    row.rackSet.add(sample.rackKey);
    row.boxSet.add(sample.boxKey);
  });

  const freezerSummary = Array.from(freezerSummaryMap.values())
    .map((row) => ({
      freezer: row.freezer,
      sampleCount: row.sampleCount,
      shelfCount: row.shelfSet.size,
      rackCount: row.rackSet.size,
      boxCount: row.boxSet.size,
    }))
    .sort((a, b) => a.freezer.localeCompare(b.freezer));

  return {
    totalStoredSamples: samples.length,
    freezerCount: freezers.size,
    shelfCount: shelves.size,
    rackCount: racks.size,
    boxCount: boxes.size,
    freezerSummary,
  };
};

const buildSnapshotText = (sample) => {
  if (!sample) {
    return null;
  }
  if (!sample.locationPath || sample.locationPath === "Not Assigned") {
    return sample.positionCoordinate && sample.positionCoordinate !== "-"
      ? sample.positionCoordinate
      : null;
  }
  if (!sample.positionCoordinate || sample.positionCoordinate === "-") {
    return sample.locationPath;
  }
  return `${sample.locationPath} @ ${sample.positionCoordinate}`;
};

function BiorepositoryQCInspectionPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();

  const [samples, setSamples] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [overviewLoading, setOverviewLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [filters, setFilters] = useState({
    freezer: "",
    shelf: "",
    rack: "",
    box: "",
  });

  const [roundSettings, setRoundSettings] = useState({
    boxesPerRound: "12",
    samplesPerBox: "4",
    seed: "",
  });

  const [qcRoundInfo, setQcRoundInfo] = useState(null);
  const [showSelectedOnly, setShowSelectedOnly] = useState(false);

  const [inspectionModalOpen, setInspectionModalOpen] = useState(false);
  const [isSubmittingInspection, setIsSubmittingInspection] = useState(false);
  const [inspectionContext, setInspectionContext] = useState({
    mode: "bulk",
    sampleIds: [],
    samplePreview: [],
  });
  const [inspectionValues, setInspectionValues] = useState(
    createInitialInspectionForm(),
  );

  const resetInspectionForm = useCallback(() => {
    setInspectionValues(createInitialInspectionForm());
  }, []);

  const calculateQCResult = useCallback((checklist) => {
    const allPassed = Object.values(checklist).every((value) => value === true);
    return allPassed ? "VERIFIED" : "DISCREPANCY_FOUND";
  }, []);

  const loadStoredSamples = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer(`/rest/biorepository/qc-inspection/samples`, (response) => {
      setLoading(false);
      if (Array.isArray(response)) {
        const transformed = response.map((sample) => {
          const storageLocation = sample.storageLocation || {};
          const rawCoordinate = toSafeString(
            storageLocation.positionCoordinate,
            "-",
          );

          return {
            id: sample.bioSampleId,
            sampleItemId: sample.sampleItemId,
            externalId: sample.externalId || "-",
            accessionNumber: sample.accessionNumber || "-",
            sampleType: sample.sampleType || "-",
            locationPath:
              sample.locationPath ||
              storageLocation.hierarchicalPath ||
              "Not Assigned",
            positionCoordinate: rawCoordinate,
            storageDetails: storageLocation,
            biosafetyLevel: sample.biosafetyLevel || "-",
            workflowStatus: sample.workflowStatus,
            lastQCInspection: sample.lastQCInspection,
          };
        });

        setSamples(transformed);
      } else {
        setSamples([]);
      }
    });
  }, []);

  const loadLocationOverview = useCallback(() => {
    setOverviewLoading(true);
    getFromOpenElisServer(
      `/rest/biorepository/qc-inspection/location-overview`,
      (response) => {
        setOverviewLoading(false);
        if (response && typeof response === "object" && !response.error) {
          setOverview(response);
        } else {
          setOverview(null);
        }
      },
    );
  }, []);

  useEffect(() => {
    loadStoredSamples();
    loadLocationOverview();
  }, [loadStoredSamples, loadLocationOverview]);

  const enrichedSamples = useMemo(
    () =>
      samples.map((sample) => ({
        ...sample,
        ...parseLocationPath(
          sample.locationPath,
          sample.positionCoordinate,
          sample.storageDetails,
        ),
      })),
    [samples],
  );

  const fullOverviewFallback = useMemo(
    () => computeOverview(enrichedSamples),
    [enrichedSamples],
  );

  const freezerScopeOptions = useMemo(() => {
    if (Array.isArray(overview?.freezerOptions) && overview.freezerOptions.length) {
      return overview.freezerOptions;
    }
    return Array.from(
      new Set(enrichedSamples.map((sample) => sample.freezer)),
    )
      .sort((a, b) => a.localeCompare(b))
      .map((value) => ({ value, label: value }));
  }, [overview, enrichedSamples]);

  const availableFreezers = useMemo(
    () =>
      freezerScopeOptions
        .map((option) => option?.value || option?.label)
        .filter(Boolean),
    [freezerScopeOptions],
  );

  const availableShelves = useMemo(
    () => {
      if (Array.isArray(overview?.shelfOptions) && overview.shelfOptions.length) {
        return Array.from(
          new Set(
            overview.shelfOptions
              .filter(
                (option) =>
                  !filters.freezer || option.freezer === filters.freezer,
              )
              .map((option) => option?.value || option?.label)
              .filter(Boolean),
          ),
        ).sort((a, b) => a.localeCompare(b));
      }

      return Array.from(
        new Set(
          enrichedSamples
            .filter(
              (sample) => !filters.freezer || sample.freezer === filters.freezer,
            )
            .map((sample) => sample.shelf),
        ),
      ).sort((a, b) => a.localeCompare(b));
    },
    [overview, enrichedSamples, filters.freezer],
  );

  const availableRacks = useMemo(
    () => {
      if (Array.isArray(overview?.rackOptions) && overview.rackOptions.length) {
        return Array.from(
          new Set(
            overview.rackOptions
              .filter(
                (option) =>
                  (!filters.freezer || option.freezer === filters.freezer) &&
                  (!filters.shelf || option.shelf === filters.shelf),
              )
              .map((option) => option?.value || option?.label)
              .filter(Boolean),
          ),
        ).sort((a, b) => a.localeCompare(b));
      }

      return Array.from(
        new Set(
          enrichedSamples
            .filter(
              (sample) =>
                (!filters.freezer || sample.freezer === filters.freezer) &&
                (!filters.shelf || sample.shelf === filters.shelf),
            )
            .map((sample) => sample.rack),
        ),
      ).sort((a, b) => a.localeCompare(b));
    },
    [overview, enrichedSamples, filters.freezer, filters.shelf],
  );

  const roundSelectedIds = useMemo(
    () =>
      new Set(
        (qcRoundInfo?.samples || [])
          .map((sample) => String(sample.bioSampleId))
          .filter(Boolean),
      ),
    [qcRoundInfo],
  );

  const displayedSamples = useMemo(
    () =>
      enrichedSamples.filter((sample) => {
        if (filters.freezer && sample.freezer !== filters.freezer) {
          return false;
        }
        if (filters.shelf && sample.shelf !== filters.shelf) {
          return false;
        }
        if (filters.rack && sample.rack !== filters.rack) {
          return false;
        }
        if (
          filters.box &&
          !sample.box.toLowerCase().includes(filters.box.trim().toLowerCase())
        ) {
          return false;
        }
        if (showSelectedOnly && qcRoundInfo) {
          return roundSelectedIds.has(String(sample.id));
        }
        return true;
      }),
    [enrichedSamples, filters, showSelectedOnly, qcRoundInfo, roundSelectedIds],
  );

  const filteredOverview = useMemo(
    () => computeOverview(displayedSamples),
    [displayedSamples],
  );

  const activeOverview = useMemo(() => {
    if (overview && !filters.freezer && !filters.shelf && !filters.rack && !filters.box) {
      return overview;
    }
    return filteredOverview;
  }, [overview, filters, filteredOverview]);

  const verifiedCount = useMemo(
    () =>
      displayedSamples.filter(
        (sample) => sample.lastQCInspection?.qcResult === "VERIFIED",
      ).length,
    [displayedSamples],
  );

  const discrepancyCount = useMemo(
    () =>
      displayedSamples.filter(
        (sample) => sample.lastQCInspection?.qcResult === "DISCREPANCY_FOUND",
      ).length,
    [displayedSamples],
  );

  const pendingCount = useMemo(
    () => displayedSamples.filter((sample) => !sample.lastQCInspection).length,
    [displayedSamples],
  );

  const checkedCount = useMemo(
    () =>
      Object.values(inspectionValues.qcChecklist).filter((checked) => checked)
        .length,
    [inspectionValues.qcChecklist],
  );

  const openInspectionModal = useCallback(
    (mode, sampleIds, samplePreview = []) => {
      setInspectionContext({
        mode,
        sampleIds,
        samplePreview,
      });
      resetInspectionForm();
      setInspectionModalOpen(true);
      setError(null);
    },
    [resetInspectionForm],
  );

  const closeInspectionModal = useCallback(() => {
    setInspectionModalOpen(false);
    setInspectionContext({ mode: "bulk", sampleIds: [], samplePreview: [] });
    resetInspectionForm();
  }, [resetInspectionForm]);

  const handleChecklistChange = useCallback(
    (criteriaId, checked) => {
      setInspectionValues((previous) => {
        const qcChecklist = {
          ...previous.qcChecklist,
          [criteriaId]: checked,
        };
        const qcResult = calculateQCResult(qcChecklist);
        return {
          ...previous,
          qcChecklist,
          qcResult,
          discrepancyType: qcResult === "VERIFIED" ? "" : previous.discrepancyType,
          correctiveAction:
            qcResult === "VERIFIED" ? "" : previous.correctiveAction,
        };
      });
    },
    [calculateQCResult],
  );

  const handleSetAllChecklist = useCallback(
    (isChecked) => {
      const checklist = QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = isChecked;
        return acc;
      }, {});

      setInspectionValues((previous) => ({
        ...previous,
        qcChecklist: checklist,
        qcResult: calculateQCResult(checklist),
        discrepancyType: isChecked ? "" : previous.discrepancyType,
        correctiveAction: isChecked ? "" : previous.correctiveAction,
      }));
    },
    [calculateQCResult],
  );

  const handleGenerateQCRound = useCallback(() => {
    setError(null);

    const boxesPerRound = Math.max(
      1,
      Number.parseInt(roundSettings.boxesPerRound || "10", 10) || 10,
    );
    const samplesPerBox = Math.max(
      1,
      Number.parseInt(roundSettings.samplesPerBox || "3", 10) || 3,
    );

    const seedValue = roundSettings.seed.trim();
    const parsedSeed = seedValue ? Number.parseInt(seedValue, 10) : null;

    if (seedValue && Number.isNaN(parsedSeed)) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.invalidSeed",
          defaultMessage: "Seed must be a valid whole number.",
        }),
      );
      return;
    }

    const payload = {
      boxesPerRound,
      samplesPerBox,
      seed: parsedSeed,
      freezer: filters.freezer || null,
      shelf: filters.shelf || null,
      rack: filters.rack || null,
      box: filters.box || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/biorepository/qc-inspection/generate-round`,
      JSON.stringify(payload),
      (response) => {
        if (response?.error) {
          setError(response.error);
          return;
        }

        const selectedIds = (response?.samples || [])
          .map((sample) => String(sample.bioSampleId))
          .filter(Boolean);

        if (selectedIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "biorepository.qc.error.emptyRound",
              defaultMessage:
                "No eligible samples were found for the selected location and round settings.",
            }),
          );
          return;
        }

        setQcRoundInfo(response);
        setShowSelectedOnly(true);

        setSuccessMessage(
          intl.formatMessage(
            {
              id: "biorepository.qc.round.success",
              defaultMessage:
                "QC round generated successfully: {boxes} box(es), {samples} sample(s).",
            },
            {
              boxes: response.boxesSelected || 0,
              samples: response.samplesSelected || 0,
            },
          ),
        );
      },
    );
  }, [filters, intl, roundSettings]);

  const handleSubmitInspection = useCallback(() => {
    if (!inspectionContext.sampleIds.length) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noSelection",
          defaultMessage: "Please choose at least one sample.",
        }),
      );
      return;
    }

    if (!inspectionValues.inspectorName.trim()) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noInspector",
          defaultMessage: "Inspector name is required.",
        }),
      );
      return;
    }

    if (!inspectionValues.qcResult) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noResult",
          defaultMessage:
            "Complete the checklist first so the system can determine pass/fail.",
        }),
      );
      return;
    }

    if (inspectionValues.qcResult === "DISCREPANCY_FOUND") {
      if (!inspectionValues.discrepancyType) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noDiscrepancyType",
            defaultMessage: "Discrepancy type is required for failed QC.",
          }),
        );
        return;
      }
      if (!inspectionValues.correctiveAction.trim()) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectiveAction",
            defaultMessage: "Corrective action details are required for failed QC.",
          }),
        );
        return;
      }
      if (!inspectionValues.remarks.trim()) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noRemarks",
            defaultMessage: "Comment/remarks are required for failed QC.",
          }),
        );
        return;
      }
    }

    setIsSubmittingInspection(true);
    setError(null);

    const singleSampleSnapshot =
      inspectionContext.mode === "single" && inspectionContext.samplePreview.length
        ? buildSnapshotText(inspectionContext.samplePreview[0])
        : null;

    const payload = {
      bioSampleIds: inspectionContext.sampleIds.map((id) => Number(id)),
      inspectorName: inspectionValues.inspectorName.trim(),
      inspectionDate: inspectionValues.inspectionDate
        ? new Date(inspectionValues.inspectionDate).toISOString()
        : null,
      samplePresent: inspectionValues.qcChecklist.samplePresent,
      labelIntegrity: inspectionValues.qcChecklist.labelIntegrity,
      containerIntegrity: inspectionValues.qcChecklist.containerIntegrity,
      volumeAppearanceAcceptable:
        inspectionValues.qcChecklist.volumeAppearanceAcceptable,
      correctPosition: inspectionValues.qcChecklist.correctPosition,
      discrepancyType:
        inspectionValues.qcResult === "DISCREPANCY_FOUND"
          ? inspectionValues.discrepancyType
          : null,
      correctiveAction:
        inspectionValues.qcResult === "DISCREPANCY_FOUND"
          ? inspectionValues.correctiveAction.trim()
          : null,
      remarks: inspectionValues.remarks.trim() || null,
      qcBatchId: qcRoundInfo?.qcBatchId || null,
      expectedCoordinateSnapshot: singleSampleSnapshot,
    };

    postToOpenElisServerJsonResponse(
      `/rest/biorepository/qc-inspection/bulk-apply`,
      JSON.stringify(payload),
      (response) => {
        setIsSubmittingInspection(false);
        if (response?.error) {
          setError(response.error);
          return;
        }

        const count = response?.count || inspectionContext.sampleIds.length;
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "biorepository.qc.success.applied",
              defaultMessage: "QC inspection recorded for {count} sample(s).",
            },
            { count },
          ),
        );

        closeInspectionModal();
        loadStoredSamples();
        loadLocationOverview();

        if (onProgressUpdate) {
          onProgressUpdate();
        }
      },
    );
  }, [
    closeInspectionModal,
    inspectionContext,
    inspectionValues,
    intl,
    loadLocationOverview,
    loadStoredSamples,
    onProgressUpdate,
    qcRoundInfo?.qcBatchId,
  ]);

  const handleDownloadRoundCSV = useCallback(() => {
    if (!qcRoundInfo?.samples?.length) {
      return;
    }

    const headers = [
      "qcBatchId",
      "freezer",
      "shelf",
      "rack",
      "box",
      "position",
      "locationPath",
      "sampleNumber",
      "sampleId",
    ];

    const escapeCSV = (value) => {
      const text = value === null || value === undefined ? "" : String(value);
      if (text.includes(",") || text.includes('"') || text.includes("\n")) {
        return `"${text.replace(/"/g, '""')}"`;
      }
      return text;
    };

    const rows = qcRoundInfo.samples.map((sample) => {
      const parsed = parseLocationPath(
        sample.locationPath,
        sample.expectedCoordinate || sample.positionCoordinate,
      );
      return [
        qcRoundInfo.qcBatchId || "",
        sample.freezer || parsed.freezer,
        sample.shelf || parsed.shelf,
        sample.rack || parsed.rack,
        sample.box || parsed.box,
        sample.expectedCoordinate || sample.positionCoordinate || "",
        sample.locationPath || "",
        sample.accessionNumber || "",
        sample.externalId || "",
      ];
    });

    const csv = `${headers.join(",")}\n${rows
      .map((row) => row.map(escapeCSV).join(","))
      .join("\n")}\n`;

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `biorepository-qc-round-${qcRoundInfo.qcBatchId || "batch"}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }, [qcRoundInfo]);

  const roundRows = useMemo(
    () =>
      (qcRoundInfo?.samples || []).map((sample, index) => {
        const parsed = parseLocationPath(
          sample.locationPath,
          sample.expectedCoordinate || sample.positionCoordinate,
        );

        return {
          id: `${sample.bioSampleId || "sample"}-${index}`,
          freezer: sample.freezer || parsed.freezer,
          shelf: sample.shelf || parsed.shelf,
          rack: sample.rack || parsed.rack,
          box: sample.box || parsed.box,
          position:
            sample.expectedCoordinate || sample.positionCoordinate || parsed.positionCoordinate,
          accessionNumber: sample.accessionNumber || "-",
          externalId: sample.externalId || "-",
        };
      }),
    [qcRoundInfo],
  );

  const roundHeaders = useMemo(
    () => [
      { key: "freezer", header: "Freezer" },
      { key: "shelf", header: "Shelf" },
      { key: "rack", header: "Rack" },
      { key: "box", header: "Box" },
      { key: "position", header: "Position" },
      { key: "accessionNumber", header: "Sample Number" },
      { key: "externalId", header: "Sample ID" },
    ],
    [],
  );

  const tableHeaders = useMemo(
    () => [
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
      { key: "freezer", header: "Freezer" },
      { key: "shelf", header: "Shelf" },
      { key: "rack", header: "Rack" },
      { key: "box", header: "Box" },
      { key: "positionCoordinate", header: "Position" },
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
          defaultMessage: "Last QC",
        }),
      },
      { key: "actions", header: "Actions" },
    ],
    [intl],
  );

  const tableRows = useMemo(
    () =>
      displayedSamples.map((sample) => ({
        id: String(sample.id),
        accessionNumber: sample.accessionNumber,
        sampleType: sample.sampleType,
        freezer: sample.freezer,
        shelf: sample.shelf,
        rack: sample.rack,
        box: sample.box,
        positionCoordinate: sample.positionCoordinate,
        biosafetyLevel: sample.biosafetyLevel,
        lastQCInspection: sample.lastQCInspection,
        actions: "inspect",
        _raw: sample,
      })),
    [displayedSamples],
  );

  const freezerDropdownItems = useMemo(
    () => [ALL_FREEZERS, ...availableFreezers],
    [availableFreezers],
  );

  const shelfDropdownItems = useMemo(
    () => [ALL_SHELVES, ...availableShelves],
    [availableShelves],
  );

  const rackDropdownItems = useMemo(
    () => [ALL_RACKS, ...availableRacks],
    [availableRacks],
  );

  const getQCTag = useCallback((qcResult) => {
    if (!qcResult) {
      return <Tag type="gray">Pending</Tag>;
    }
    if (qcResult === "VERIFIED") {
      return <Tag type="green">Verified</Tag>;
    }
    if (qcResult === "DISCREPANCY_FOUND") {
      return <Tag type="red">Discrepancy</Tag>;
    }
    return <Tag type="gray">{qcResult}</Tag>;
  }, []);

  return (
    <div className="biorepository-qc-inspection-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="biorepository.qc.title"
            defaultMessage="Biorepository Periodic QC"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="biorepository.qc.description"
            defaultMessage="Start with a room overview, scope the location to check, generate a randomized QC round, then record outcomes individually or in bulk with required failure documentation."
          />
        </p>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          lowContrast
          onClose={() => setSuccessMessage(null)}
        />
      )}

      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <h5 style={{ marginTop: 0, marginBottom: "0.75rem" }}>
              Room Overview
            </h5>
            <p style={{ marginTop: 0, marginBottom: "1rem", color: "#525252" }}>
              Review storage scale before starting QC. Filters below let you plan a
              focused QC round.
            </p>

            {overviewLoading && (
              <div style={{ marginBottom: "1rem" }}>
                <Loading small withOverlay={false} />
              </div>
            )}

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
                gap: "0.75rem",
                marginBottom: "1rem",
              }}
            >
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Total Stored Samples
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {fullOverviewFallback.totalStoredSamples}
                </div>
              </Tile>
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Matching Current Filters
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {filteredOverview.totalStoredSamples}
                </div>
              </Tile>
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Freezers
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {activeOverview.freezerCount || 0}
                </div>
              </Tile>
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Shelves
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {activeOverview.shelfCount || 0}
                </div>
              </Tile>
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Racks
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {activeOverview.rackCount || 0}
                </div>
              </Tile>
              <Tile>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  Boxes
                </div>
                <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                  {activeOverview.boxCount || 0}
                </div>
              </Tile>
            </div>

            <Grid condensed fullWidth>
              <Column lg={4} md={4} sm={4}>
                <Dropdown
                  id="qc-filter-freezer"
                  titleText="Freezer"
                  label={ALL_FREEZERS}
                  items={freezerDropdownItems}
                  selectedItem={filters.freezer || ALL_FREEZERS}
                  itemToString={(item) => (item ? String(item) : "")}
                  onChange={({ selectedItem }) => {
                    const value = selectedItem === ALL_FREEZERS ? "" : selectedItem;
                    setFilters((previous) => ({
                      ...previous,
                      freezer: value,
                      shelf: "",
                      rack: "",
                    }));
                  }}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Dropdown
                  id="qc-filter-shelf"
                  titleText="Shelf"
                  label={ALL_SHELVES}
                  items={shelfDropdownItems}
                  selectedItem={filters.shelf || ALL_SHELVES}
                  itemToString={(item) => (item ? String(item) : "")}
                  onChange={({ selectedItem }) => {
                    const value = selectedItem === ALL_SHELVES ? "" : selectedItem;
                    setFilters((previous) => ({
                      ...previous,
                      shelf: value,
                      rack: "",
                    }));
                  }}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Dropdown
                  id="qc-filter-rack"
                  titleText="Rack"
                  label={ALL_RACKS}
                  items={rackDropdownItems}
                  selectedItem={filters.rack || ALL_RACKS}
                  itemToString={(item) => (item ? String(item) : "")}
                  onChange={({ selectedItem }) => {
                    const value = selectedItem === ALL_RACKS ? "" : selectedItem;
                    setFilters((previous) => ({
                      ...previous,
                      rack: value,
                    }));
                  }}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qc-filter-box"
                  labelText="Box (optional text filter)"
                  placeholder="e.g. BX078"
                  value={filters.box}
                  onChange={(event) =>
                    setFilters((previous) => ({
                      ...previous,
                      box: event.target.value,
                    }))
                  }
                />
              </Column>
            </Grid>

            <div style={{ marginTop: "0.75rem", display: "flex", gap: "0.5rem" }}>
              <Button
                kind="ghost"
                size="sm"
                onClick={() =>
                  setFilters({ freezer: "", shelf: "", rack: "", box: "" })
                }
              >
                Clear location filters
              </Button>
              <Button kind="ghost" size="sm" renderIcon={Renew} onClick={loadStoredSamples}>
                Refresh samples
              </Button>
              <Button kind="ghost" size="sm" renderIcon={Renew} onClick={loadLocationOverview}>
                Refresh overview
              </Button>
            </div>
          </Tile>
        </Column>
      </Grid>

      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <h5 style={{ marginTop: 0, marginBottom: "0.75rem" }}>
              QC Round Planning
            </h5>
            <p style={{ marginTop: 0, marginBottom: "1rem", color: "#525252" }}>
              Choose how many boxes and positions to sample for this periodic QC
              round. The system will generate a randomized list from the selected
              location scope.
            </p>

            <Grid condensed fullWidth>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qc-round-boxes"
                  type="number"
                  min="1"
                  max="200"
                  labelText="Boxes per round"
                  value={roundSettings.boxesPerRound}
                  onChange={(event) =>
                    setRoundSettings((previous) => ({
                      ...previous,
                      boxesPerRound: event.target.value,
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qc-round-samples-per-box"
                  type="number"
                  min="1"
                  max="50"
                  labelText="Samples per box"
                  value={roundSettings.samplesPerBox}
                  onChange={(event) =>
                    setRoundSettings((previous) => ({
                      ...previous,
                      samplesPerBox: event.target.value,
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qc-round-seed"
                  type="number"
                  labelText="Random seed (optional)"
                  placeholder="Leave empty for true random"
                  value={roundSettings.seed}
                  onChange={(event) =>
                    setRoundSettings((previous) => ({
                      ...previous,
                      seed: event.target.value,
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={4} sm={4} style={{ display: "flex", alignItems: "end" }}>
                <Button size="sm" onClick={handleGenerateQCRound}>
                  Generate random QC round
                </Button>
              </Column>
            </Grid>

            {qcRoundInfo && (
              <div style={{ marginTop: "1rem" }}>
                <InlineNotification
                  kind="info"
                  lowContrast
                  title={intl.formatMessage(
                    {
                      id: "biorepository.qc.round.generated",
                      defaultMessage:
                        "Round {batch}: {boxes} box(es), {samples} sample(s) selected.",
                    },
                    {
                      batch: qcRoundInfo.qcBatchId,
                      boxes: qcRoundInfo.boxesSelected || 0,
                      samples: qcRoundInfo.samplesSelected || 0,
                    },
                  )}
                />

                <div style={{ marginTop: "0.75rem" }}>
                  <ButtonSet>
                    <Button
                      kind="primary"
                      size="sm"
                      onClick={() => {
                        const selectedIds = (qcRoundInfo.samples || [])
                          .map((sample) => String(sample.bioSampleId))
                          .filter(Boolean);
                        const preview = displayedSamples
                          .filter((sample) => selectedIds.includes(String(sample.id)))
                          .slice(0, 6);
                        openInspectionModal("bulk", selectedIds, preview);
                      }}
                    >
                      Bulk inspect generated round
                    </Button>
                    <Button kind="tertiary" size="sm" onClick={handleDownloadRoundCSV}>
                      Download round list (CSV)
                    </Button>
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() => {
                        setQcRoundInfo(null);
                        setShowSelectedOnly(false);
                      }}
                    >
                      Clear round
                    </Button>
                  </ButtonSet>
                </div>

                <div style={{ marginTop: "0.75rem" }}>
                  <Toggle
                    id="show-selected-round-only"
                    labelText="Show only generated round samples in main table"
                    toggled={showSelectedOnly}
                    onToggle={(nextValue) => setShowSelectedOnly(Boolean(nextValue))}
                  />
                </div>
              </div>
            )}
          </Tile>
        </Column>
      </Grid>

      {qcRoundInfo && roundRows.length > 0 && (
        <Grid fullWidth style={{ marginTop: "1rem" }}>
          <Column lg={16} md={8} sm={4}>
            <DataTable rows={roundRows} headers={roundHeaders}>
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer
                  title="Generated QC Checklist"
                  description="Technician path guidance: Freezer > Shelf > Rack > Box > Position"
                >
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader key={header.key} {...getHeaderProps({ header })}>
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </Column>
        </Grid>
      )}

      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
              gap: "0.75rem",
              marginBottom: "1rem",
            }}
          >
            <Tile>
              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                In View
              </div>
              <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>
                {displayedSamples.length}
              </div>
            </Tile>
            <Tile>
              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                Verified
              </div>
              <div style={{ fontSize: "1.5rem", fontWeight: 600, color: "#198038" }}>
                {verifiedCount}
              </div>
            </Tile>
            <Tile>
              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                Discrepancies
              </div>
              <div style={{ fontSize: "1.5rem", fontWeight: 600, color: "#da1e28" }}>
                {discrepancyCount}
              </div>
            </Tile>
            <Tile>
              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                Pending
              </div>
              <div style={{ fontSize: "1.5rem", fontWeight: 600 }}>{pendingCount}</div>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="sample-table-section" style={{ marginTop: "0.5rem" }}>
        {loading ? (
          <div style={{ padding: "2rem", textAlign: "center" }}>
            <Loading withOverlay={false} />
          </div>
        ) : tableRows.length === 0 ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title="No samples found for current view"
            subtitle="Adjust location filters or generate a new QC round."
          />
        ) : (
          <DataTable rows={tableRows} headers={tableHeaders}>
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
              const selectedIds = selectedRows.map((row) => row.id);

              return (
                <TableContainer title="QC Execution Table">
                  <TableToolbar>
                    <TableBatchActions {...getBatchActionProps()}>
                      <TableBatchAction
                        renderIcon={Edit}
                        iconDescription="Bulk inspect selected"
                        onClick={() => {
                          const preview = displayedSamples
                            .filter((sample) => selectedIds.includes(String(sample.id)))
                            .slice(0, 6);
                          openInspectionModal("bulk", selectedIds, preview);
                        }}
                      >
                        Bulk inspect selected
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <Button
                        kind="ghost"
                        hasIconOnly
                        size="sm"
                        iconDescription="Refresh"
                        renderIcon={Renew}
                        onClick={loadStoredSamples}
                      />
                    </TableToolbarContent>
                  </TableToolbar>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader key={header.key} {...getHeaderProps({ header })}>
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const raw = row.cells.find((cell) => cell.info.header === "actions")
                          ? displayedSamples.find(
                              (sample) => String(sample.id) === String(row.id),
                            )
                          : null;

                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => {
                              if (cell.info.header === "biosafetyLevel") {
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
                                    <Tag type={bslColor}>{cell.value}</Tag>
                                  </TableCell>
                                );
                              }

                              if (cell.info.header === "lastQCInspection") {
                                const inspection = raw?.lastQCInspection;
                                if (!inspection) {
                                  return (
                                    <TableCell key={cell.id}>
                                      <Tag type="gray">Never inspected</Tag>
                                    </TableCell>
                                  );
                                }
                                return (
                                  <TableCell key={cell.id}>
                                    <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                                      {getQCTag(inspection.qcResult)}
                                      <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                                        {new Date(inspection.inspectionDate).toLocaleDateString()}
                                      </span>
                                    </div>
                                  </TableCell>
                                );
                              }

                              if (cell.info.header === "actions") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      onClick={() =>
                                        openInspectionModal("single", [row.id], [raw])
                                      }
                                    >
                                      Inspect
                                    </Button>
                                  </TableCell>
                                );
                              }

                              return <TableCell key={cell.id}>{cell.value}</TableCell>;
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

      <Modal
        open={inspectionModalOpen}
        onRequestClose={closeInspectionModal}
        modalHeading={
          inspectionContext.mode === "single"
            ? "Individual QC Inspection"
            : "Bulk QC Inspection"
        }
        primaryButtonText={
          isSubmittingInspection
            ? "Saving..."
            : inspectionValues.qcResult === "DISCREPANCY_FOUND"
              ? "Record Failed QC"
              : "Record QC"
        }
        secondaryButtonText="Cancel"
        onRequestSubmit={handleSubmitInspection}
        onSecondarySubmit={closeInspectionModal}
        primaryButtonDisabled={isSubmittingInspection || !inspectionValues.qcResult}
        danger={inspectionValues.qcResult === "DISCREPANCY_FOUND"}
        size="md"
      >
        <p style={{ marginTop: 0 }}>
          {inspectionContext.mode === "single"
            ? "Record QC for one selected sample."
            : `Record QC for ${inspectionContext.sampleIds.length} selected sample(s).`}
        </p>

        {inspectionContext.samplePreview.length > 0 && (
          <Tile style={{ marginBottom: "1rem" }}>
            <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
              Selected preview
            </div>
            {inspectionContext.samplePreview.slice(0, 4).map((sample) => (
              <div key={`preview-${sample.id}`} style={{ marginTop: "0.35rem" }}>
                {sample.accessionNumber} - {sample.freezer} / {sample.shelf} / {sample.rack} / {sample.box} / {sample.positionCoordinate}
              </div>
            ))}
            {inspectionContext.samplePreview.length > 4 && (
              <div style={{ marginTop: "0.35rem", color: "#6f6f6f" }}>
                +{inspectionContext.samplePreview.length - 4} more sample(s)
              </div>
            )}
          </Tile>
        )}

        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="qc-inspector-name"
              labelText="Inspector name (required)"
              value={inspectionValues.inspectorName}
              onChange={(event) =>
                setInspectionValues((previous) => ({
                  ...previous,
                  inspectorName: event.target.value,
                }))
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">Inspection date and time</label>
              <input
                type="datetime-local"
                className="cds--text-input"
                value={inspectionValues.inspectionDate}
                onChange={(event) =>
                  setInspectionValues((previous) => ({
                    ...previous,
                    inspectionDate: event.target.value,
                  }))
                }
              />
            </div>
          </Column>
        </Grid>

        <div style={{ marginTop: "1rem" }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              marginBottom: "0.5rem",
            }}
          >
            <strong>
              QC Checklist ({checkedCount}/{QC_CHECKLIST.length})
            </strong>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button kind="ghost" size="sm" onClick={() => handleSetAllChecklist(true)}>
                Check all
              </Button>
              <Button kind="ghost" size="sm" onClick={() => handleSetAllChecklist(false)}>
                Clear all
              </Button>
            </div>
          </div>

          {QC_CHECKLIST.map((item) => (
            <Checkbox
              key={item.id}
              id={`qc-check-${item.id}`}
              labelText={intl.formatMessage({
                id: item.labelId,
                defaultMessage: item.defaultLabel,
              })}
              checked={inspectionValues.qcChecklist[item.id]}
              onChange={(_, { checked }) => handleChecklistChange(item.id, checked)}
            />
          ))}
        </div>

        <div style={{ marginTop: "1rem" }}>
          {inspectionValues.qcResult === "VERIFIED" && (
            <Tag type="green">
              <Checkmark size={16} style={{ marginRight: "0.25rem" }} />
              QC VERIFIED
            </Tag>
          )}
          {inspectionValues.qcResult === "DISCREPANCY_FOUND" && (
            <Tag type="red">
              <WarningAlt size={16} style={{ marginRight: "0.25rem" }} />
              DISCREPANCY FOUND
            </Tag>
          )}
          {!inspectionValues.qcResult && <Tag type="gray">Complete checklist</Tag>}
        </div>

        {inspectionValues.qcResult === "DISCREPANCY_FOUND" && (
          <div style={{ marginTop: "1rem" }}>
            <Dropdown
              id="qc-discrepancy-type"
              titleText="Discrepancy type (required)"
              label="Choose discrepancy"
              items={DISCREPANCY_TYPES}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={DISCREPANCY_TYPES.find(
                (item) => item.id === inspectionValues.discrepancyType,
              )}
              onChange={({ selectedItem }) =>
                setInspectionValues((previous) => ({
                  ...previous,
                  discrepancyType: selectedItem?.id || "",
                }))
              }
            />

            <TextArea
              id="qc-corrective-action"
              labelText="Corrective action (required)"
              rows={3}
              value={inspectionValues.correctiveAction}
              onChange={(event) =>
                setInspectionValues((previous) => ({
                  ...previous,
                  correctiveAction: event.target.value,
                }))
              }
            />
          </div>
        )}

        <div style={{ marginTop: "1rem" }}>
          <TextArea
            id="qc-remarks"
            labelText={
              inspectionValues.qcResult === "DISCREPANCY_FOUND"
                ? "Comment/remarks (required for failed QC)"
                : "Comment/remarks (optional)"
            }
            rows={2}
            value={inspectionValues.remarks}
            onChange={(event) =>
              setInspectionValues((previous) => ({
                ...previous,
                remarks: event.target.value,
              }))
            }
          />
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
