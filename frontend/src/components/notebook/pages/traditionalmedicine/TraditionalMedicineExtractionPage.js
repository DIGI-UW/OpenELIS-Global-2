import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
  NumberInput,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Archive,
  Pending,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineExtractionPage - Page 4 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 5: Extraction, Filtration & Concentration
 * - Extraction: Solvents (ethanol, methanol, water, hexane, chloroform)
 * - Techniques: Maceration, Soxhlet, Ultrasonic, Distillation
 * - Filtration: Methods (filter paper, vacuum, centrifugation)
 * - Concentration: Evaporation, distillation
 * - Yield calculation and tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineExtractionPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const { getPagePermissionLevel, canSaveData, canAccessStage3to4 } =
    useTMMRDPermissions();

  // STAGE 4 allowed roles per TMMRD SRS Section 11 - Lab Technicians and Researchers
  const allowedRoles = [
    "TMMRD Lab Technician",
    "TMMRD Researcher",
    "TMMRD Pharmacognosist",
    "TMMRD Lab Manager",
    "TMMRD Principal Investigator",
  ];

  const canAccessPage = canAccessStage3to4();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(
    "Extraction & Concentration",
  );
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Extraction modal and state
  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [isApplyingExtraction, setIsApplyingExtraction] = useState(false);
  const [solventType, setSolventType] = useState(null);
  const [extractionTechnique, setExtractionTechnique] = useState(null);
  const [plantMaterialWeight, setPlantMaterialWeight] = useState("");
  const [solventVolume, setSolventVolume] = useState("");
  const [extractionTemp, setExtractionTemp] = useState("");
  const [extractionDuration, setExtractionDuration] = useState("");
  const [extractionNotes, setExtractionNotes] = useState("");

  // Filtration modal and state
  const [filtrationModalOpen, setFiltrationModalOpen] = useState(false);
  const [isApplyingFiltration, setIsApplyingFiltration] = useState(false);
  const [filtrationMethod, setFiltrationMethod] = useState(null);
  const [filtrationNotes, setFiltrationNotes] = useState("");

  // Concentration modal and state
  const [concentrationModalOpen, setConcentrationModalOpen] = useState(false);
  const [isApplyingConcentration, setIsApplyingConcentration] = useState(false);
  const [concentrationMethod, setConcentrationMethod] = useState(null);
  const [extractWeight, setExtractWeight] = useState("");
  const [concentrationNotes, setConcentrationNotes] = useState("");

  // Pathway selection state (end of Page 5)
  const [pathwaySelectionModalOpen, setPathwaySelectionModalOpen] =
    useState(false);
  const [isSelectingPathway, setIsSelectingPathway] = useState(false);
  const [selectedPathway, setSelectedPathway] = useState(null);

  // General completion state
  const [isCompleting, setIsCompleting] = useState(false);

  const solventOptions = [
    { id: "ethanol", label: "Ethanol" },
    { id: "methanol", label: "Methanol" },
    { id: "water", label: "Water" },
    { id: "hexane", label: "Hexane" },
    { id: "chloroform", label: "Chloroform" },
    { id: "acetone", label: "Acetone" },
    { id: "other", label: "Other Solvent" },
  ];

  const techniqueOptions = [
    { id: "maceration", label: "Maceration" },
    { id: "soxhlet", label: "Soxhlet Extraction" },
    { id: "ultrasonic", label: "Ultrasonic Extraction" },
    { id: "distillation", label: "Distillation" },
    { id: "other", label: "Other Technique" },
  ];

  const filtrationOptions = [
    { id: "filter_paper", label: "Filter Paper" },
    { id: "vacuum", label: "Vacuum Filtration" },
    { id: "centrifugation", label: "Centrifugation" },
  ];

  const concentrationOptions = [
    { id: "rotary_evaporator", label: "Rotary Evaporator" },
    { id: "distillation", label: "Distillation" },
    { id: "no_concentration", label: "No Concentration" },
  ];

  // Analytical pathway options (selected at end of Page 5)
  const pathwayOptions = [
    {
      id: "path_a",
      label: "Advanced Analysis",
      description:
        "Proceed with Fractionation → Identification → Purification → Characterization (Page 6)",
    },
    {
      id: "path_b",
      label: "Direct to Production",
      description:
        "Skip Advanced Analysis and proceed directly to Product Development (Page 7)",
    },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

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

          // Handle both array and object responses from API
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
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  sampleCategory: s.data?.sampleCategory,
                  plantPart: s.data?.plantPart,
                  collectionDate: s.data?.collectionDate,
                  intendedUse: s.data?.intendedUse,
                  solventType: s.data?.solvent,
                  extractionTechnique: s.data?.extractionTechnique,
                  filtrationMethod: s.data?.filtrationMethod,
                  concentrationMethod: s.data?.concentrationMethod,
                  selectedPathway: s.data?.analyticalPathwayId,
                  selectedPathwayLabel: s.data?.analyticalPathwayLabel,
                  yieldPercent: s.data?.extractYieldPercentage,
                  extractWeight: s.data?.extractWeight,
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

  const resetExtractionForm = useCallback(() => {
    setSolventType(null);
    setExtractionTechnique(null);
    setPlantMaterialWeight("");
    setSolventVolume("");
    setExtractionTemp("");
    setExtractionDuration("");
    setExtractionNotes("");
  }, []);

  const resetFiltrationForm = useCallback(() => {
    setFiltrationMethod(null);
    setFiltrationNotes("");
  }, []);

  const resetConcentrationForm = useCallback(() => {
    setConcentrationMethod(null);
    setExtractWeight("");
    setConcentrationNotes("");
  }, []);

  // Helper: Check if any selected sample has already completed extraction
  const hasAnyCompletedExtraction = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.some((s) => s.solventType);
  }, [samples, selectedSampleIds]);

  // Helper: Check if any selected sample has already completed filtration
  const hasAnyCompletedFiltration = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.some((s) => s.filtrationMethod);
  }, [samples, selectedSampleIds]);

  // Helper: Check if any selected sample has already completed concentration
  const hasAnyCompletedConcentration = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.some((s) => s.concentrationMethod);
  }, [samples, selectedSampleIds]);

  // Helper: Check if all selected samples have a pathway selected
  const hasAllSelectedSamplesPathway = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every((s) => s.selectedPathway);
  }, [samples, selectedSampleIds]);

  // Helper: Check if any selected sample has already selected a pathway
  const hasAnySelectedPathway = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.some((s) => s.selectedPathway);
  }, [samples, selectedSampleIds]);

  const openExtractionModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetExtractionForm();
    setExtractionModalOpen(true);
  }, [selectedSampleIds, intl, resetExtractionForm, notify]);

  const openFiltrationModal = useCallback(() => {
    const samplesToFilter = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.solventType && // Has extraction data
        !s.concentrationMethod, // Not yet concentrated
    );

    if (samplesToFilter.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.noExtracted",
          defaultMessage:
            "Selected samples must have extraction data recorded before filtration.",
        }),
      });
      return;
    }
    resetFiltrationForm();
    setFiltrationModalOpen(true);
  }, [selectedSampleIds, samples, intl, resetFiltrationForm, notify]);

  const openConcentrationModal = useCallback(() => {
    const samplesToConcentrate = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.solventType && // Has extraction data
        s.filtrationMethod, // Has filtration data
    );

    if (samplesToConcentrate.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.noFiltered",
          defaultMessage:
            "Selected samples must have extraction and filtration data recorded before concentration.",
        }),
      });
      return;
    }
    resetConcentrationForm();
    setConcentrationModalOpen(true);
  }, [selectedSampleIds, samples, intl, resetConcentrationForm, notify]);

  // Helper: Check if all selected samples have all 3 steps complete
  const canOpenPathwaySelection = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every(
      (s) => s.solventType && s.filtrationMethod && s.concentrationMethod,
    );
  }, [samples, selectedSampleIds]);

  const openPathwaySelectionModal = useCallback(() => {
    const samplesToSelectPathway = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.solventType && // Has extraction data
        s.filtrationMethod && // Has filtration data
        s.concentrationMethod, // Has concentration data
    );

    if (samplesToSelectPathway.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.noConcentrated",
          defaultMessage:
            "Selected samples must have all extraction, filtration, and concentration data recorded before selecting pathway.",
        }),
      });
      return;
    }

    setSelectedPathway(null);
    setPathwaySelectionModalOpen(true);
  }, [selectedSampleIds, samples, intl, notify]);

  const selectPathway = useCallback(() => {
    if (!selectedPathway) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.pathwayRequired",
          defaultMessage: "Please select an analysis pathway.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsSelectingPathway(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          analyticalPathwayId: selectedPathway.id,
          analyticalPathwayLabel: selectedPathway.label,
        },
      }),
      (response) => {
        setIsSelectingPathway(false);

        if (response?.success) {
          const pathwayLabel = selectedPathway.label;
          const message =
            response.message ||
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.extraction.pathway.success",
                defaultMessage:
                  "Pathway selected: {pathway} for {count} sample(s).",
              },
              {
                pathway: pathwayLabel,
                count: response.updatedCount || selectedSampleIds.length,
              },
            );

          notify({
            kind: NotificationKinds.success,
            title: message,
          });
          setPathwaySelectionModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.extraction.pathway.failed",
                defaultMessage: "Failed to select pathway. Please try again.",
              }),
          });
        }
      },
    );
  }, [
    selectedPathway,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const applyExtraction = useCallback(() => {
    if (!solventType || !extractionTechnique) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.required",
          defaultMessage: "Please select solvent and technique.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingExtraction(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          solvent: solventType.id,
          solventLabel: solventType.label,
          extractionTechnique: extractionTechnique.id,
          extractionTechniqueLabel: extractionTechnique.label,
          materialWeight: plantMaterialWeight || null,
          solventVolume: solventVolume || null,
          extractionTemperature: extractionTemp || null,
          extractionDurationMinutes: extractionDuration || null,
          extractionNotes: extractionNotes || null,
        },
      }),
      (response) => {
        setIsApplyingExtraction(false);
        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title:
              response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.extraction.success",
                  defaultMessage: "Extraction recorded for {count} sample(s).",
                },
                {
                  count: response.updatedCount || selectedSampleIds.length,
                },
              ),
          });
          setExtractionModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Failed to record extraction",
          });
        }
      },
    );
  }, [
    solventType,
    extractionTechnique,
    plantMaterialWeight,
    solventVolume,
    extractionTemp,
    extractionDuration,
    extractionNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const applyFiltration = useCallback(() => {
    if (!filtrationMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.filtration.error.required",
          defaultMessage: "Please select a filtration method.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingFiltration(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          filtrationMethod: filtrationMethod.id,
          filtrationMethodLabel: filtrationMethod.label,
          filtrationNotes: filtrationNotes || null,
        },
      }),
      (response) => {
        setIsApplyingFiltration(false);
        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title:
              response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.filtration.success",
                  defaultMessage: "Filtration recorded for {count} sample(s).",
                },
                {
                  count: response.updatedCount || selectedSampleIds.length,
                },
              ),
          });
          setFiltrationModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Failed to record filtration",
          });
        }
      },
    );
  }, [
    filtrationMethod,
    filtrationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const applyConcentration = useCallback(() => {
    if (!concentrationMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.concentration.error.required",
          defaultMessage: "Please select a concentration method.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingConcentration(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));
    const yieldPercent =
      plantMaterialWeight && extractWeight
        ? (
            (parseFloat(extractWeight) / parseFloat(plantMaterialWeight)) *
            100
          ).toFixed(2)
        : null;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          concentrationMethod: concentrationMethod.id,
          concentrationMethodLabel: concentrationMethod.label,
          extractWeight: extractWeight || null,
          extractYieldPercentage: yieldPercent || null,
          concentrationNotes: concentrationNotes || null,
        },
      }),
      (response) => {
        setIsApplyingConcentration(false);
        if (response?.success) {
          notify({
            kind: NotificationKinds.success,
            title:
              response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.concentration.success",
                  defaultMessage:
                    "Concentration recorded for {count} sample(s).",
                },
                {
                  count: response.updatedCount || selectedSampleIds.length,
                },
              ),
          });
          setConcentrationModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Failed to record concentration",
          });
        }
      },
    );
  }, [
    concentrationMethod,
    plantMaterialWeight,
    extractWeight,
    concentrationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const handleMarkComplete = useCallback(() => {
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.extract.noEligibleSamples",
          defaultMessage:
            "Selected samples must have extraction recorded (status: In Progress) before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    // Build URL with pathway routing parameters
    // Page 5 (Extraction) routes to different pages based on pathway selection:
    // - path_a: Next page (Page 6 - Analytical Pathways)
    // - path_b: Page 7 (Product Development & Testing)
    const url = new URL(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      window.location.origin,
    );
    url.searchParams.append("pathwayrouting", "true");
    url.searchParams.append("sourcepage", "Extraction");
    url.searchParams.append("targetpage", "Product Development");

    postToOpenElisServerJsonResponse(
      url.pathname + url.search,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.extract.completeSuccess",
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
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.extract.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const unpreparedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const extractedCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const extractedInProgressSamples = useMemo(
    () => samples.filter((s) => s.status === "IN_PROGRESS"),
    [samples],
  );

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Extraction & Concentration"
        reason="This page requires specific Traditional Medicine extraction roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-extraction-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.extraction.title"
            defaultMessage="Extraction, Filtration & Concentration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.extraction.description"
            defaultMessage="Extract plant material using various solvents and techniques, filter, and concentrate extracts with yield tracking."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.ready"
                  defaultMessage="Ready for Extraction"
                />
              </span>
              <span className="progress-value">{unpreparedSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.extracted"
                  defaultMessage="Extracted"
                />
              </span>
              <span className="progress-value">
                {extractedCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openExtractionModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            hasAnyCompletedExtraction()
          }
        >
          <FormattedMessage
            id="notebook.page.tradmed.extraction.recordExtraction"
            defaultMessage="Record Extraction ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Edit}
          onClick={openFiltrationModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            hasAnyCompletedFiltration()
          }
        >
          <FormattedMessage
            id="notebook.page.tradmed.filtration.recordFiltration"
            defaultMessage="Record Filtration ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Edit}
          onClick={openConcentrationModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            hasAnyCompletedConcentration()
          }
        >
          <FormattedMessage
            id="notebook.page.tradmed.concentration.recordConcentration"
            defaultMessage="Record Concentration ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="danger"
          size="sm"
          renderIcon={Edit}
          onClick={openPathwaySelectionModal}
          disabled={
            !canOpenPathwaySelection() ||
            !hasRealPageId ||
            hasAnySelectedPathway()
          }
        >
          <FormattedMessage
            id="notebook.page.tradmed.extraction.selectPathway"
            defaultMessage="Select Analysis Pathway ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            selectedSampleIds.length === 0 ||
            isCompleting ||
            !hasRealPageId ||
            !hasAllSelectedSamplesPathway()
          }
        >
          <FormattedMessage
            id="notebook.tradmed.extract.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: selectedSampleIds.length }}
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
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.ready.title"
              defaultMessage="Samples Ready for Extraction"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {unpreparedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && unpreparedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.ready.empty"
                  defaultMessage="No samples ready for extraction."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="ready-extraction"
              samples={unpreparedSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "filtrationMethod", header: "Filtration" },
                { key: "concentrationMethod", header: "Concentration" },
                {
                  key: "selectedPathwayLabel",
                  header: "Analysis Pathway",
                  render: (value) => {
                    if (!value) return null;
                    const isPathA = value.includes("Advanced");
                    return (
                      <Tag type={isPathA ? "blue" : "purple"} size="sm">
                        {value.split("(")[0].trim()}
                      </Tag>
                    );
                  },
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Extracted Samples Section - IN PROGRESS (with checkboxes) */}
      {/* Extraction Completed Section - COMPLETED (without checkboxes) */}
      {extractedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.extraction.extracted.completed.title"
                defaultMessage="Extraction Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {extractedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="extracted-completed-samples"
              samples={extractedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "filtrationMethod", header: "Filtration" },
                { key: "concentrationMethod", header: "Concentration" },
                {
                  key: "selectedPathwayLabel",
                  header: "Analysis Pathway",
                  render: (value) => {
                    if (!value) return null;
                    const isPathA = value.includes("Advanced");
                    return (
                      <Tag type={isPathA ? "blue" : "purple"} size="sm">
                        {value.split("(")[0].trim()}
                      </Tag>
                    );
                  },
                },
                { key: "extractWeight", header: "Extract Weight (g)" },
                { key: "yieldPercent", header: "Yield %" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      {/* Extraction Modal */}
      <Modal
        open={extractionModalOpen}
        onRequestClose={() => setExtractionModalOpen(false)}
        onRequestSubmit={applyExtraction}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.extraction.modal.title",
          defaultMessage: "Record Extraction Process",
        })}
        primaryButtonText={
          isApplyingExtraction
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.record",
                defaultMessage: "Record Extraction",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingExtraction}
        size="md"
      >
        {isApplyingExtraction && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="solvent"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.solvent",
                defaultMessage: "Solvent *",
              })}
              label="Select..."
              items={solventOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={solventType}
              onChange={({ selectedItem }) => setSolventType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="technique"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.technique",
                defaultMessage: "Technique *",
              })}
              label="Select..."
              items={techniqueOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={extractionTechnique}
              onChange={({ selectedItem }) =>
                setExtractionTechnique(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="plant-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.plantWeight",
                defaultMessage: "Plant Material Weight (g)",
              })}
              value={plantMaterialWeight}
              onChange={(e) =>
                setPlantMaterialWeight(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="solvent-vol"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.solventVolume",
                defaultMessage: "Solvent Volume (mL)",
              })}
              value={solventVolume}
              onChange={(e) =>
                setSolventVolume(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="ext-temp"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.temp",
                defaultMessage: "Temperature (°C)",
              })}
              value={extractionTemp}
              onChange={(e) =>
                setExtractionTemp(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="ext-duration"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.duration",
                defaultMessage: "Duration (hours)",
              })}
              value={extractionDuration}
              onChange={(e) => setExtractionDuration(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="extraction-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.notes",
                defaultMessage: "Extraction Notes",
              })}
              value={extractionNotes}
              onChange={(e) => setExtractionNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Filtration Modal */}
      <Modal
        open={filtrationModalOpen}
        onRequestClose={() => setFiltrationModalOpen(false)}
        onRequestSubmit={applyFiltration}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.filtration.modal.title",
          defaultMessage: "Record Filtration",
        })}
        primaryButtonText={
          isApplyingFiltration
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.filtration.modal.record",
                defaultMessage: "Record Filtration",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingFiltration}
        size="md"
      >
        {isApplyingFiltration && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="filtration"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.filtration.modal.method",
                defaultMessage: "Filtration Method *",
              })}
              label="Select..."
              items={filtrationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={filtrationMethod}
              onChange={({ selectedItem }) => setFiltrationMethod(selectedItem)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="filtration-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.filtration.modal.notes",
                defaultMessage: "Filtration Notes",
              })}
              value={filtrationNotes}
              onChange={(e) => setFiltrationNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Concentration Modal */}
      <Modal
        open={concentrationModalOpen}
        onRequestClose={() => setConcentrationModalOpen(false)}
        onRequestSubmit={applyConcentration}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.concentration.modal.title",
          defaultMessage: "Record Concentration",
        })}
        primaryButtonText={
          isApplyingConcentration
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.concentration.modal.record",
                defaultMessage: "Record Concentration",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingConcentration}
        size="md"
      >
        {isApplyingConcentration && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="concentration"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.concentration.modal.method",
                defaultMessage: "Concentration Method *",
              })}
              label="Select..."
              items={concentrationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={concentrationMethod}
              onChange={({ selectedItem }) =>
                setConcentrationMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="extract-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.concentration.modal.extractWeight",
                defaultMessage: "Extract Weight (g)",
              })}
              value={extractWeight}
              onChange={(e) =>
                setExtractWeight(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="concentration-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.concentration.modal.notes",
                defaultMessage: "Concentration Notes",
              })}
              value={concentrationNotes}
              onChange={(e) => setConcentrationNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Pathway Selection Modal (End of Page 5) */}
      <Modal
        open={pathwaySelectionModalOpen}
        onRequestClose={() => setPathwaySelectionModalOpen(false)}
        onRequestSubmit={selectPathway}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.extraction.pathway.modal.title",
          defaultMessage: "Select Analysis Pathway",
        })}
        primaryButtonText={
          isSelectingPathway
            ? intl.formatMessage({
                id: "label.selecting",
                defaultMessage: "Selecting...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.extraction.pathway.modal.select",
                defaultMessage: "Select Pathway",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isSelectingPathway}
        size="md"
      >
        {isSelectingPathway && <Loading withOverlay={false} small />}

        <div style={{ marginBottom: "1.5rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.pathway.modal.description"
              defaultMessage="Select the analysis pathway for {count} sample(s). This selection is LOCKED after confirmation and cannot be changed."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          {pathwayOptions.map((pathway) => (
            <div
              key={pathway.id}
              onClick={() => setSelectedPathway(pathway)}
              style={{
                padding: "1rem",
                border:
                  selectedPathway?.id === pathway.id
                    ? "2px solid #0043CE"
                    : "2px solid #e0e0e0",
                borderRadius: "4px",
                cursor: "pointer",
                backgroundColor:
                  selectedPathway?.id === pathway.id ? "#f4f4f4" : "white",
                transition: "all 0.2s ease",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  marginBottom: "0.5rem",
                }}
              >
                <input
                  type="radio"
                  name="pathway"
                  value={pathway.id}
                  checked={selectedPathway?.id === pathway.id}
                  onChange={() => setSelectedPathway(pathway)}
                  style={{ marginRight: "0.5rem" }}
                />
                <label style={{ fontWeight: "bold", cursor: "pointer" }}>
                  {pathway.label}
                </label>
              </div>
              <p
                style={{ margin: "0", color: "#525252", fontSize: "0.875rem" }}
              >
                {pathway.description}
              </p>
            </div>
          ))}
        </div>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineExtractionPage;
