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
  TextArea,
  Loading,
  NumberInput,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Pending,
  WarningAltFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAnalyticalPage - Page 6 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 6: Analytical Pathways (4 Mandatory Steps for PATH A)
 * STEP 1: Fractionation - Separate extract using chromatography
 * STEP 2: Identification/Isolation - Detect active constituents
 * STEP 3: Purification - Remove impurities, assess purity
 * STEP 4: Characterization - Determine structure (NMR, MS, IR)
 *
 * PATH A samples must complete all 4 steps before advancing to Page 7.
 * PATH B samples skip this page entirely.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineAnalyticalPage({
  entryId,
  pageData,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // Use standard permissions instead of custom TMMRD-specific logic
  // Page-level access control should be handled by usePageAccessControl() in parent workflow component
  // This component focuses on action-level permissions using PermissionGate components around individual actions

  // Core state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isCompleting, setIsCompleting] = useState(false);

  // STEP 1: Fractionation modal state
  const [fractionationModalOpen, setFractionationModalOpen] = useState(false);
  const [isApplyingFractionation, setIsApplyingFractionation] = useState(false);
  const [chromatographyMethod, setChromatographyMethod] = useState(null);
  const [numberOfFractions, setNumberOfFractions] = useState("");
  const [fractionLabels, setFractionLabels] = useState("");
  const [fractionDescription, setFractionDescription] = useState("");
  const [fractionationNotes, setFractionationNotes] = useState("");

  // STEP 2: Identification modal state
  const [identificationModalOpen, setIdentificationModalOpen] = useState(false);
  const [isApplyingIdentification, setIsApplyingIdentification] =
    useState(false);
  const [detectionMethod, setDetectionMethod] = useState(null);
  const [activeConstituentsFound, setActiveConstituentsFound] = useState("");
  const [knownCompoundsIdentified, setKnownCompoundsIdentified] = useState("");
  const [identificationNotes, setIdentificationNotes] = useState("");

  // STEP 3: Purification modal state
  const [purificationModalOpen, setPurificationModalOpen] = useState(false);
  const [isApplyingPurification, setIsApplyingPurification] = useState(false);
  const [purificationMethod, setPurificationMethod] = useState(null);
  const [purityLevel, setPurityLevel] = useState("");
  const [purityAssessmentMethod, setPurityAssessmentMethod] = useState(null);
  const [purificationNotes, setPurificationNotes] = useState("");

  // STEP 4: Characterization modal state
  const [characterizationModalOpen, setCharacterizationModalOpen] =
    useState(false);
  const [isApplyingCharacterization, setIsApplyingCharacterization] =
    useState(false);
  const [spectroscopyTechniques, setSpectroscopyTechniques] = useState([]);
  const [structureDetermination, setStructureDetermination] = useState("");
  const [propertiesIdentified, setPropertiesIdentified] = useState("");
  const [characterizationNotes, setCharacterizationNotes] = useState("");

  // Dropdown options
  const chromatographyOptions = [
    { id: "column_chromatography", label: "Column Chromatography" },
    { id: "hplc_prep", label: "HPLC Prep" },
    { id: "other", label: "Other" },
  ];

  const detectionMethodOptions = [
    { id: "tlc", label: "TLC (Thin Layer Chromatography)" },
    { id: "hplc", label: "HPLC (High Performance Liquid Chromatography)" },
    { id: "gc_ms", label: "GC-MS (Gas Chromatography-Mass Spectrometry)" },
    { id: "lc_ms", label: "LC-MS (Liquid Chromatography-Mass Spectrometry)" },
    { id: "other", label: "Other" },
  ];

  const purificationMethodOptions = [
    { id: "recrystallization", label: "Recrystallization" },
    { id: "prep_hplc", label: "Prep-HPLC" },
    { id: "column_chromatography", label: "Column Chromatography" },
    { id: "other", label: "Other" },
  ];

  const purityAssessmentMethodOptions = [
    { id: "hplc", label: "HPLC" },
    { id: "nmr", label: "NMR" },
    { id: "other", label: "Other" },
  ];

  const spectroscopyTechniqueOptions = [
    { id: "nmr", label: "NMR (Nuclear Magnetic Resonance)" },
    { id: "mass_spectrometry", label: "Mass Spectrometry (MS)" },
    { id: "ir_ftir", label: "IR/FTIR (Infrared Spectroscopy)" },
    { id: "other", label: "Other" },
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
              ? samplesToProcess
                  .filter((s) => s.data?.analyticalPathwayId === "path_a")
                  .map((s) => ({
                    id: String(s.id || s.sampleItemId),
                    externalId: s.externalId,
                    accessionNumber: s.accessionNumber,
                    status: s.pageStatus || s.status || "PENDING",
                    selectedPath: s.data?.analyticalPathwayId,
                    selectedPathLabel: s.data?.analyticalPathwayLabel,
                    // STEP 1: Fractionation
                    chromatographyMethod: s.data?.chromatographyMethod,
                    chromatographyMethodLabel:
                      s.data?.chromatographyMethodLabel,
                    numberOfFractions: s.data?.numberOfFractions,
                    fractionLabels: s.data?.fractionLabels,
                    fractionDescription: s.data?.fractionDescription,
                    fractionationNotes: s.data?.fractionationNotes,
                    fractionationComplete: s.data?.fractionationComplete,
                    // STEP 2: Identification
                    detectionMethod: s.data?.detectionMethod,
                    detectionMethodLabel: s.data?.detectionMethodLabel,
                    activeConstituentsFound: s.data?.activeConstituentsFound,
                    knownCompoundsIdentified: s.data?.knownCompoundsIdentified,
                    identificationNotes: s.data?.identificationNotes,
                    identificationComplete: s.data?.identificationComplete,
                    // STEP 3: Purification
                    purificationMethod: s.data?.purificationMethod,
                    purificationMethodLabel: s.data?.purificationMethodLabel,
                    purityLevel: s.data?.purityLevel,
                    purityAssessmentMethod: s.data?.purityAssessmentMethod,
                    purityAssessmentMethodLabel:
                      s.data?.purityAssessmentMethodLabel,
                    purificationNotes: s.data?.purificationNotes,
                    purificationComplete: s.data?.purificationComplete,
                    // STEP 4: Characterization
                    spectroscopyTechniques: s.data?.spectroscopyTechniques,
                    structureDetermination: s.data?.structureDetermination,
                    propertiesIdentified: s.data?.propertiesIdentified,
                    characterizationNotes: s.data?.characterizationNotes,
                    characterizationComplete: s.data?.characterizationComplete,
                    // Pathway selection
                    selectedPathwayLabel: s.data?.analyticalPathwayLabel,
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

  // Helper: Check if fractionation is complete for selected samples
  const canOpenIdentificationModal = useCallback(() => {
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every((s) => s.fractionationComplete);
  }, [samples, selectedSampleIds]);

  // Helper: Check if identification is complete for selected samples
  const canOpenPurificationModal = useCallback(() => {
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every(
      (s) => s.fractionationComplete && s.identificationComplete,
    );
  }, [samples, selectedSampleIds]);

  // Helper: Check if purification is complete for selected samples
  const canOpenCharacterizationModal = useCallback(() => {
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every(
      (s) =>
        s.fractionationComplete &&
        s.identificationComplete &&
        s.purificationComplete,
    );
  }, [samples, selectedSampleIds]);

  // Helper: Check if all 4 steps are complete for selected samples
  const canMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return false;
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    return selectedSamples.every(
      (s) =>
        s.fractionationComplete &&
        s.identificationComplete &&
        s.purificationComplete &&
        s.characterizationComplete &&
        s.status !== "COMPLETED",
    );
  }, [samples, selectedSampleIds]);

  // STEP 1: Record Fractionation
  const openFractionationModal = useCallback(() => {
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
    setFractionationModalOpen(true);
  }, [selectedSampleIds, intl, notify]);

  const applyFractionation = useCallback(() => {
    if (!chromatographyMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.methodRequired",
          defaultMessage: "Please select a chromatography method.",
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

    setIsApplyingFractionation(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/record-fractionation`,
      JSON.stringify({
        sampleIds,
        chromatographyMethod: chromatographyMethod.id,
        numberOfFractions: numberOfFractions ? parseInt(numberOfFractions) : 0,
        fractionLabels,
        fractionDescription,
        fractionationNotes,
      }),
      (response) => {
        setIsApplyingFractionation(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.analytical.fractionation.success",
                defaultMessage: "Recorded fractionation for {count} sample(s).",
              },
              { count: response.updatedCount },
            ),
          });
          setFractionationModalOpen(false);
          setChromatographyMethod(null);
          setNumberOfFractions("");
          setFractionLabels("");
          setFractionDescription("");
          setFractionationNotes("");
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.error",
                defaultMessage: "Failed to record fractionation.",
              }),
          });
        }
      },
    );
  }, [
    chromatographyMethod,
    numberOfFractions,
    fractionLabels,
    fractionDescription,
    fractionationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // STEP 2: Record Identification
  const openIdentificationModal = useCallback(() => {
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

    if (!canOpenIdentificationModal()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.fractionationRequired",
          defaultMessage:
            "Fractionation must be completed before proceeding to Identification.",
        }),
      });
      return;
    }

    setIdentificationModalOpen(true);
  }, [selectedSampleIds, canOpenIdentificationModal, intl, notify]);

  const applyIdentification = useCallback(() => {
    if (!detectionMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.methodRequired",
          defaultMessage: "Please select a detection method.",
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

    setIsApplyingIdentification(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/record-identification`,
      JSON.stringify({
        sampleIds,
        detectionMethod: detectionMethod.id,
        activeConstituentsFound,
        knownCompoundsIdentified,
        identificationNotes,
      }),
      (response) => {
        setIsApplyingIdentification(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.analytical.identification.success",
                defaultMessage:
                  "Recorded identification for {count} sample(s).",
              },
              { count: response.updatedCount },
            ),
          });
          setIdentificationModalOpen(false);
          setDetectionMethod(null);
          setActiveConstituentsFound("");
          setKnownCompoundsIdentified("");
          setIdentificationNotes("");
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.analytical.identification.error",
                defaultMessage: "Failed to record identification.",
              }),
          });
        }
      },
    );
  }, [
    detectionMethod,
    activeConstituentsFound,
    knownCompoundsIdentified,
    identificationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // STEP 3: Record Purification
  const openPurificationModal = useCallback(() => {
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

    if (!canOpenPurificationModal()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.fractionationAndIdentificationRequired",
          defaultMessage:
            "Fractionation and Identification must be completed before proceeding to Purification.",
        }),
      });
      return;
    }

    setPurificationModalOpen(true);
  }, [selectedSampleIds, canOpenPurificationModal, intl, notify]);

  const applyPurification = useCallback(() => {
    if (!purificationMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.methodRequired",
          defaultMessage: "Please select a purification method.",
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

    setIsApplyingPurification(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/record-purification`,
      JSON.stringify({
        sampleIds,
        purificationMethod: purificationMethod.id,
        purityLevel: purityLevel ? parseFloat(purityLevel) : null,
        purityAssessmentMethod: purityAssessmentMethod?.id,
        purificationNotes,
      }),
      (response) => {
        setIsApplyingPurification(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.analytical.purification.success",
                defaultMessage: "Recorded purification for {count} sample(s).",
              },
              { count: response.updatedCount },
            ),
          });
          setPurificationModalOpen(false);
          setPurificationMethod(null);
          setPurityLevel("");
          setPurityAssessmentMethod(null);
          setPurificationNotes("");
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purification.error",
                defaultMessage: "Failed to record purification.",
              }),
          });
        }
      },
    );
  }, [
    purificationMethod,
    purityLevel,
    purityAssessmentMethod,
    purificationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // STEP 4: Record Characterization
  const openCharacterizationModal = useCallback(() => {
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

    if (!canOpenCharacterizationModal()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.allPriorStepsRequired",
          defaultMessage:
            "Fractionation, Identification, and Purification must be completed before proceeding to Characterization.",
        }),
      });
      return;
    }

    setCharacterizationModalOpen(true);
  }, [selectedSampleIds, canOpenCharacterizationModal, intl, notify]);

  const applyCharacterization = useCallback(() => {
    if (spectroscopyTechniques.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.techniqueRequired",
          defaultMessage: "Please select at least one spectroscopy technique.",
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

    setIsApplyingCharacterization(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/record-characterization`,
      JSON.stringify({
        sampleIds,
        spectroscopyTechniques: spectroscopyTechniques.map((t) => t.id),
        structureDetermination,
        propertiesIdentified,
        characterizationNotes,
      }),
      (response) => {
        setIsApplyingCharacterization(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.analytical.characterization.success",
                defaultMessage:
                  "Recorded characterization for {count} sample(s).",
              },
              { count: response.updatedCount },
            ),
          });
          setCharacterizationModalOpen(false);
          setSpectroscopyTechniques([]);
          setStructureDetermination("");
          setPropertiesIdentified("");
          setCharacterizationNotes("");
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.analytical.characterization.error",
                defaultMessage: "Failed to record characterization.",
              }),
          });
        }
      },
    );
  }, [
    spectroscopyTechniques,
    structureDetermination,
    propertiesIdentified,
    characterizationNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Mark all 4 steps complete
  const handleMarkComplete = useCallback(() => {
    if (!canMarkComplete()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.allStepsRequired",
          defaultMessage:
            "All 4 analytical steps must be completed before marking samples ready for Page 7.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = selectedSampleIds
      .map((id) => parseInt(id, 10))
      .filter((id) => {
        const sample = samples.find((s) => s.id === String(id));
        return (
          sample?.fractionationComplete &&
          sample?.identificationComplete &&
          sample?.purificationComplete &&
          sample?.characterizationComplete &&
          sample?.status !== "COMPLETED"
        );
      });

    // Use /status endpoint with pathway routing to advance samples to Page 7
    const endpoint = `/rest/notebook/bulk/page/${pageData.id}/samples/status?pathwayrouting=true&sourcepage=Analytical&targetpage=Product+Development`;

    postToOpenElisServerJsonResponse(
      endpoint,
      JSON.stringify({ sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.analytical.markComplete.success",
                defaultMessage: "Successfully marked {count} samples complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.analytical.markComplete.error",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    canMarkComplete,
    samples,
    selectedSampleIds,
    pageData?.id,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const pendingSamples = useMemo(
    () => samples.filter((s) => s.status !== "COMPLETED"),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Page-level access control is handled by usePageAccessControl() in parent workflow component
  // This component assumes it's only rendered when user has page access
  // Individual UI elements use PermissionGate for action-level control

  const renderStepStatus = (sample) => {
    const steps = [
      {
        label: "F",
        title: "Fractionation",
        complete: sample.fractionationComplete,
      },
      {
        label: "I",
        title: "Identification",
        complete: sample.identificationComplete,
      },
      {
        label: "P",
        title: "Purification",
        complete: sample.purificationComplete,
      },
      {
        label: "C",
        title: "Characterization",
        complete: sample.characterizationComplete,
      },
    ];

    return (
      <div style={{ display: "flex", gap: "0.25rem" }}>
        {steps.map((step) => (
          <Tag
            key={step.label}
            type={step.complete ? "green" : "gray"}
            size="sm"
            title={step.title}
          >
            {step.label}
          </Tag>
        ))}
      </div>
    );
  };

  return (
    <div className="tradmed-analytical-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.analytical.page6.title"
            defaultMessage="Analytical Pathways - Advanced Analysis (PATH A)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.analytical.page6.description"
            defaultMessage="Complete all 4 mandatory steps for PATH A samples: Fractionation, Identification, Purification, and Characterization."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <PermissionGate
          roles={[
            Permissions.CHEMICAL_ANALYST,
            Permissions.PHARMACIST,
            Permissions.RESEARCHER,
            Permissions.LAB_SUPERVISOR,
          ]}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.fractionationPermission",
            defaultMessage: "Insufficient permissions to perform fractionation",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openFractionationModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
            title={
              selectedSampleIds.length === 0
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.selectSamples",
                    defaultMessage: "Select samples for fractionation",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.analytical.step1"
              defaultMessage="Step 1: Fractionation"
            />
          </Button>
        </PermissionGate>

        <PermissionGate
          roles={[
            Permissions.CHEMICAL_ANALYST,
            Permissions.PHARMACIST,
            Permissions.RESEARCHER,
            Permissions.LAB_SUPERVISOR,
          ]}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.identificationPermission",
            defaultMessage:
              "Insufficient permissions to perform identification",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openIdentificationModal}
            disabled={
              selectedSampleIds.length === 0 ||
              !canOpenIdentificationModal() ||
              !hasRealPageId
            }
            title={
              !canOpenIdentificationModal()
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.fractionationRequired",
                    defaultMessage: "Fractionation must be completed first",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.analytical.step2"
              defaultMessage="Step 2: Identification"
            />
          </Button>
        </PermissionGate>

        <PermissionGate
          roles={[
            Permissions.CHEMICAL_ANALYST,
            Permissions.PHARMACIST,
            Permissions.RESEARCHER,
            Permissions.LAB_SUPERVISOR,
          ]}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.purificationPermission",
            defaultMessage: "Insufficient permissions to perform purification",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openPurificationModal}
            disabled={
              selectedSampleIds.length === 0 ||
              !canOpenPurificationModal() ||
              !hasRealPageId
            }
            title={
              !canOpenPurificationModal()
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.priorStepsRequired",
                    defaultMessage:
                      "Fractionation and Identification must be completed first",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.analytical.step3"
              defaultMessage="Step 3: Purification"
            />
          </Button>
        </PermissionGate>

        <PermissionGate
          roles={[
            Permissions.CHEMICAL_ANALYST,
            Permissions.PHARMACIST,
            Permissions.RESEARCHER,
            Permissions.LAB_SUPERVISOR,
          ]}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.characterizationPermission",
            defaultMessage:
              "Insufficient permissions to perform characterization",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openCharacterizationModal}
            disabled={
              selectedSampleIds.length === 0 ||
              !canOpenCharacterizationModal() ||
              !hasRealPageId
            }
            title={
              !canOpenCharacterizationModal()
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.allPriorStepsRequired",
                    defaultMessage:
                      "All prior analytical steps must be completed first",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.analytical.step4"
              defaultMessage="Step 4: Characterization"
            />
          </Button>
        </PermissionGate>

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

        <PermissionGate
          roles={[Permissions.LAB_SUPERVISOR, Permissions.PHARMACIST]}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.markCompletePermission",
            defaultMessage:
              "Insufficient permissions to mark analytical work complete",
          })}
        >
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={!canMarkComplete() || isCompleting || !hasRealPageId}
            title={
              !canMarkComplete()
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.allStepsCompleteRequired",
                    defaultMessage: "All 4 analytical steps must be completed",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.analytical.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.analytical.inProgress.title"
              defaultMessage="Samples In Progress"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.inProgress.empty"
                  defaultMessage="No samples in progress."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="analytical-in-progress"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                {
                  key: "selectedPathwayLabel",
                  header: "Analysis Pathway",
                  render: (value, sample) => {
                    if (!value) return null;
                    const isPathA =
                      sample?.data?.analyticalPathwayId === "path_a";
                    return (
                      <Tag type={isPathA ? "blue" : "purple"} size="sm">
                        {value.split("(")[0].trim()}
                      </Tag>
                    );
                  },
                },
                {
                  key: "steps",
                  header: "Steps (F/I/P/C)",
                  render: (_value, sample) => renderStepStatus(sample),
                },
                {
                  key: "fractionationComplete",
                  header: "Fractionation",
                  render: (value) => (
                    <Tag
                      type={value ? "green" : "gray"}
                      size="sm"
                      renderIcon={value ? CheckmarkFilled : Pending}
                    >
                      {value ? "Complete" : "Pending"}
                    </Tag>
                  ),
                },
                {
                  key: "identificationComplete",
                  header: "Identification",
                  render: (value) => (
                    <Tag
                      type={value ? "green" : "gray"}
                      size="sm"
                      renderIcon={value ? CheckmarkFilled : Pending}
                    >
                      {value ? "Complete" : "Pending"}
                    </Tag>
                  ),
                },
                {
                  key: "purificationComplete",
                  header: "Purification",
                  render: (value) => (
                    <Tag
                      type={value ? "green" : "gray"}
                      size="sm"
                      renderIcon={value ? CheckmarkFilled : Pending}
                    >
                      {value ? "Complete" : "Pending"}
                    </Tag>
                  ),
                },
                {
                  key: "characterizationComplete",
                  header: "Characterization",
                  render: (value) => (
                    <Tag
                      type={value ? "green" : "gray"}
                      size="sm"
                      renderIcon={value ? CheckmarkFilled : Pending}
                    >
                      {value ? "Complete" : "Pending"}
                    </Tag>
                  ),
                },
              ]}
            />
          )}
        </div>
      </div>

      {completedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.analytical.completed.title"
                defaultMessage="Analysis Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="analytical-completed"
              samples={completedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                {
                  key: "selectedPathwayLabel",
                  header: "Analysis Pathway",
                  render: (value, sample) => {
                    if (!value) return null;
                    const isPathA =
                      sample?.data?.analyticalPathwayId === "path_a";
                    return (
                      <Tag type={isPathA ? "blue" : "purple"} size="sm">
                        {value.split("(")[0].trim()}
                      </Tag>
                    );
                  },
                },
                {
                  key: "steps",
                  header: "Steps (F/I/P/C)",
                  render: (_value, sample) => renderStepStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      {/* STEP 1: Fractionation Modal */}
      <Modal
        open={fractionationModalOpen}
        onRequestClose={() => setFractionationModalOpen(false)}
        onRequestSubmit={applyFractionation}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.fractionation.modal.title",
          defaultMessage: "Step 1: Record Fractionation",
        })}
        primaryButtonText={
          isApplyingFractionation
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingFractionation}
        size="md"
      >
        {isApplyingFractionation && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="chromatography-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.method",
                defaultMessage: "Chromatography Method *",
              })}
              label="Select..."
              items={chromatographyOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={chromatographyMethod}
              onChange={({ selectedItem }) =>
                setChromatographyMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="number-of-fractions"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.fractions",
                defaultMessage: "Number of Fractions",
              })}
              value={numberOfFractions}
              onChange={(e) =>
                setNumberOfFractions(e.imaginaryTarget?.value || "")
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="fraction-labels"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.labels",
                defaultMessage: "Fraction Labels / IDs",
              })}
              value={fractionLabels}
              onChange={(e) => setFractionLabels(e.target.value)}
              rows={2}
              placeholder="e.g., F1, F2, F3..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="fraction-description"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.description",
                defaultMessage: "Fraction Description",
              })}
              value={fractionDescription}
              onChange={(e) => setFractionDescription(e.target.value)}
              rows={2}
              placeholder="Description of each fraction collected..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="fractionation-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionation.notes",
                defaultMessage: "Fractionation Notes",
              })}
              value={fractionationNotes}
              onChange={(e) => setFractionationNotes(e.target.value)}
              rows={3}
              placeholder="Additional notes on fractionation procedure..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* STEP 2: Identification Modal */}
      <Modal
        open={identificationModalOpen}
        onRequestClose={() => setIdentificationModalOpen(false)}
        onRequestSubmit={applyIdentification}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.identification.modal.title",
          defaultMessage: "Step 2: Record Identification",
        })}
        primaryButtonText={
          isApplyingIdentification
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingIdentification}
        size="md"
      >
        {isApplyingIdentification && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="detection-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.identification.method",
                defaultMessage: "Detection Method *",
              })}
              label="Select..."
              items={detectionMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={detectionMethod}
              onChange={({ selectedItem }) => setDetectionMethod(selectedItem)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="active-constituents"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.identification.constituents",
                defaultMessage: "Active Constituents Found",
              })}
              value={activeConstituentsFound}
              onChange={(e) => setActiveConstituentsFound(e.target.value)}
              rows={2}
              placeholder="List of active constituents detected..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="known-compounds"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.identification.compounds",
                defaultMessage: "Known Compounds Identified",
              })}
              value={knownCompoundsIdentified}
              onChange={(e) => setKnownCompoundsIdentified(e.target.value)}
              rows={2}
              placeholder="List of known compounds identified..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="identification-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.identification.notes",
                defaultMessage: "Identification Notes",
              })}
              value={identificationNotes}
              onChange={(e) => setIdentificationNotes(e.target.value)}
              rows={3}
              placeholder="Additional notes on identification process..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* STEP 3: Purification Modal */}
      <Modal
        open={purificationModalOpen}
        onRequestClose={() => setPurificationModalOpen(false)}
        onRequestSubmit={applyPurification}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.purification.modal.title",
          defaultMessage: "Step 3: Record Purification",
        })}
        primaryButtonText={
          isApplyingPurification
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingPurification}
        size="md"
      >
        {isApplyingPurification && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="purification-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purification.method",
                defaultMessage: "Purification Method *",
              })}
              label="Select..."
              items={purificationMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={purificationMethod}
              onChange={({ selectedItem }) =>
                setPurificationMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="purity-level"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purification.purity",
                defaultMessage: "Purity Level (%)",
              })}
              value={purityLevel}
              onChange={(e) => setPurityLevel(e.imaginaryTarget?.value || "")}
              min={0}
              max={100}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="purity-assessment-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purification.assessment",
                defaultMessage: "Purity Assessment Method",
              })}
              label="Select..."
              items={purityAssessmentMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={purityAssessmentMethod}
              onChange={({ selectedItem }) =>
                setPurityAssessmentMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="purification-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purification.notes",
                defaultMessage: "Purification Notes",
              })}
              value={purificationNotes}
              onChange={(e) => setPurificationNotes(e.target.value)}
              rows={3}
              placeholder="Additional notes on purification procedure..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* STEP 4: Characterization Modal */}
      <Modal
        open={characterizationModalOpen}
        onRequestClose={() => setCharacterizationModalOpen(false)}
        onRequestSubmit={applyCharacterization}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.characterization.modal.title",
          defaultMessage: "Step 4: Record Characterization",
        })}
        primaryButtonText={
          isApplyingCharacterization
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingCharacterization}
        size="md"
      >
        {isApplyingCharacterization && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div>
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.characterization.techniques"
                  defaultMessage="Spectroscopy Techniques *"
                />
              </label>
              {spectroscopyTechniqueOptions.map((technique) => (
                <div key={technique.id} style={{ marginBottom: "0.5rem" }}>
                  <input
                    type="checkbox"
                    id={`technique-${technique.id}`}
                    checked={spectroscopyTechniques.some(
                      (t) => t.id === technique.id,
                    )}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSpectroscopyTechniques([
                          ...spectroscopyTechniques,
                          technique,
                        ]);
                      } else {
                        setSpectroscopyTechniques(
                          spectroscopyTechniques.filter(
                            (t) => t.id !== technique.id,
                          ),
                        );
                      }
                    }}
                  />
                  <label htmlFor={`technique-${technique.id}`}>
                    {technique.label}
                  </label>
                </div>
              ))}
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="structure-determination"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.characterization.structure",
                defaultMessage: "Structure Determination",
              })}
              value={structureDetermination}
              onChange={(e) => setStructureDetermination(e.target.value)}
              rows={2}
              placeholder="Describe the structure determined..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="properties-identified"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.characterization.properties",
                defaultMessage: "Properties Identified",
              })}
              value={propertiesIdentified}
              onChange={(e) => setPropertiesIdentified(e.target.value)}
              rows={2}
              placeholder="Physical and chemical properties identified..."
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="characterization-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.characterization.notes",
                defaultMessage: "Characterization Notes",
              })}
              value={characterizationNotes}
              onChange={(e) => setCharacterizationNotes(e.target.value)}
              rows={3}
              placeholder="Additional notes on characterization techniques and results..."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineAnalyticalPage;
