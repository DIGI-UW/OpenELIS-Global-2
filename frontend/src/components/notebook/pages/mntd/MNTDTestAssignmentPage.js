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
  TextInput,
  TextArea,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Modal,
  Tag,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Renew,
  Chemistry,
  Calendar,
  Settings,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDTestAssignmentPage - Page 7 of the MNTD workflow.
 * Handles test assignment and machine scheduling.
 *
 * Purpose: Link samples to experiments and machines.
 *
 * Who uses it:
 * - Lab manager
 * - Technician
 *
 * Data Points:
 * - Test Assignment: Experiment category, Specific assay/protocol
 * - Machine Scheduling: Instrument selected, Date & time slot, Operator name
 *
 * System Actions:
 * - Send test order to machine (if integrated)
 * - Block machine calendar
 * - Log user activity
 *
 * Leads to: Test Execution & Raw Data Page
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 * @param {Array} props.notebookInstruments - Instruments attached to the notebook
 */
function MNTDTestAssignmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
  notebookInstruments,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Instruments from notebook (attached to the notebook instance)
  const [instruments, setInstruments] = useState([]);

  // Test assignment modal state
  const [showAssignmentModal, setShowAssignmentModal] = useState(false);
  const [assignmentData, setAssignmentData] = useState({
    experimentCategory: "",
    subcategory: "",
    specificAssay: "",
    notes: "",
  });

  // Machine scheduling modal state
  const [showSchedulingModal, setShowSchedulingModal] = useState(false);
  const [schedulingData, setSchedulingData] = useState({
    instrument: "",
    instrumentId: "", // Physical instrument ID/serial number
    scheduledDate: new Date().toISOString().split("T")[0],
    timeSlot: "", // Predefined time slot
    startTime: "",
    endTime: "",
  });

  // Time slot options for machine scheduling
  const timeSlotOptions = [
    { id: "SLOT_0800_1000", text: "08:00 - 10:00 (Morning Slot 1)" },
    { id: "SLOT_1000_1200", text: "10:00 - 12:00 (Morning Slot 2)" },
    { id: "SLOT_1200_1400", text: "12:00 - 14:00 (Midday Slot)" },
    { id: "SLOT_1400_1600", text: "14:00 - 16:00 (Afternoon Slot 1)" },
    { id: "SLOT_1600_1800", text: "16:00 - 18:00 (Afternoon Slot 2)" },
    { id: "SLOT_1800_2000", text: "18:00 - 20:00 (Evening Slot)" },
    { id: "SLOT_OVERNIGHT", text: "20:00 - 08:00 (Overnight Run)" },
    { id: "SLOT_CUSTOM", text: "Custom Time Range" },
  ];

  // Main experiment categories
  const experimentCategoryOptions = [
    { id: "PARASITE_MOLECULAR", text: "A. Parasite Molecular Experiments" },
    { id: "VECTOR_MOLECULAR", text: "B. Vector Molecular Experiments" },
    { id: "GENOMICS", text: "C. Genomics" },
    { id: "DIGITAL_PCR", text: "D. Digital PCR" },
    { id: "SEROLOGICAL", text: "E. Serological Assays" },
    { id: "PARASITE_CULTURE", text: "F. Parasite Culture" },
  ];

  // Subcategories based on main category
  const getSubcategoryOptions = (category) => {
    switch (category) {
      case "PARASITE_MOLECULAR":
        return [
          { id: "PARASITE_QPCR", text: "qPCR Assays" },
          { id: "PARASITE_CONVENTIONAL_PCR", text: "Conventional PCR" },
          { id: "PARASITE_ITS1_RFLP", text: "ITS1 PCR-RFLP" },
          { id: "PARASITE_OTHER", text: "Other Parasite Tests" },
        ];
      case "VECTOR_MOLECULAR":
        return [
          { id: "VECTOR_QPCR", text: "qPCR Assays" },
          { id: "VECTOR_CONVENTIONAL_PCR", text: "Conventional PCR" },
        ];
      case "GENOMICS":
        return [
          { id: "GENOMICS_DIAGNOSTIC", text: "Diagnostic Resistance (hrp2/3)" },
          { id: "GENOMICS_DRUG_RESISTANCE", text: "Drug Resistance" },
          { id: "GENOMICS_INSECTICIDE", text: "Insecticide Resistance" },
          { id: "GENOMICS_DIVERSITY", text: "Diversity Studies" },
          { id: "GENOMICS_VACCINE", text: "Vaccine Targets" },
          { id: "GENOMICS_HUMAN", text: "Human Genomics (MAD4HatTeR)" },
          { id: "GENOMICS_HLA", text: "HLA Typing" },
          { id: "GENOMICS_PFAMPSEQ", text: "PfAmpSeq-MARS" },
          { id: "GENOMICS_PF_WGS", text: "Pf WGS (Whole Genome Sequencing)" },
          { id: "GENOMICS_PVAMPSEQ", text: "PvAmpSeq" },
          { id: "GENOMICS_HUMAN_GENO", text: "Human Genotyping" },
          { id: "GENOMICS_TCR", text: "T cell receptor (TCR) clonotypes" },
        ];
      case "DIGITAL_PCR":
        return [
          { id: "DPCR_ABSOLUTE_QUANT", text: "Absolute Quantification" },
          { id: "DPCR_MUTATION", text: "Mutation Detection" },
          { id: "DPCR_GENE_EXPRESSION", text: "Gene Expression" },
          { id: "DPCR_SNP", text: "SNP (Single-nucleotide polymorphism)" },
          { id: "DPCR_CNV", text: "Copy Number Variation (CNV)" },
        ];
      case "SEROLOGICAL":
        return [
          { id: "SERO_ELISA", text: "ELISA" },
          { id: "SERO_BEAD_MULTIPLEX", text: "Bead-Based Multiplex" },
          { id: "SERO_ELISPOT", text: "ELISPOT" },
          { id: "SERO_FLUOROSPOT", text: "FluoroSpot" },
          { id: "SERO_FLOW_CYTOMETRY", text: "Flow Cytometry" },
        ];
      case "PARASITE_CULTURE":
        return [
          { id: "CULTURE_LEISHMANIA", text: "Leishmania Culture" },
          { id: "CULTURE_MALARIA", text: "Malaria Culture" },
        ];
      default:
        return [];
    }
  };

  // Specific assays based on subcategory
  const getSpecificAssayOptions = (subcategory) => {
    switch (subcategory) {
      // Parasite qPCR Assays
      case "PARASITE_QPCR":
        return [
          { id: "QPCR_COX1_PV", text: "Plasmodium species ID - Cox 1 Pv" },
          { id: "QPCR_COX_PF", text: "Plasmodium species ID - Cox Pf" },
          {
            id: "QPCR_MULTIPLEX_COX1",
            text: "Plasmodium species ID - Multiplex Cox 1",
          },
          {
            id: "QPCR_MULTIPLEX_18S",
            text: "Plasmodium species ID - Multiplex 18s",
          },
          { id: "QPCR_VAR18S", text: "Plasmodium species ID - Var18s" },
          { id: "QPCR_HRP2_3_GENO", text: "Pf hrp2/3 Multiplex Genotyping" },
          { id: "QPCR_PFMGET", text: "Gametocyte Detection - Pf: PfMGET" },
          { id: "QPCR_CCP4", text: "Gametocyte Detection - Pf: CCP4" },
          { id: "QPCR_PFS25", text: "Gametocyte Detection - Pf: PfS25" },
          { id: "QPCR_PFGEXP2", text: "Gametocyte Detection - Early: PfGEXP2" },
          { id: "QPCR_PFSIR2A", text: "Gametocyte Detection - Early: PfSir2A" },
          { id: "QPCR_PFAP2G", text: "Gametocyte Detection - Early: PfAP2G" },
          { id: "QPCR_PVS25", text: "Gametocyte Detection - Pv: Pvs25" },
          { id: "QPCR_PVDBP", text: "PvDBP" },
          { id: "QPCR_SBP1", text: "SBP1" },
          { id: "QPCR_CNV", text: "Copy Number Variation (CNV)" },
          {
            id: "QPCR_QUANTITECT",
            text: "QuantiTect SYBR green PCR (mRNA expression)",
          },
          { id: "QPCR_PARASITE_OTHER", text: "Others" },
        ];
      // Parasite Conventional PCR
      case "PARASITE_CONVENTIONAL_PCR":
        return [
          { id: "PCR_18S_NPCR", text: "18s nPCR" },
          { id: "PCR_COX1_GENERIC", text: "Cox 1 Generic PCR" },
          { id: "PCR_PV_COX1", text: "Pv Cox 1 PCR" },
          { id: "PCR_PARASITE_OTHER", text: "Others" },
        ];
      // ITS1 PCR-RFLP
      case "PARASITE_ITS1_RFLP":
        return [
          { id: "ITS1_LEISHMANIA", text: "Leishmania Species Identification" },
        ];
      // Other Parasite Tests
      case "PARASITE_OTHER":
        return [
          { id: "OTHER_MSP1", text: "MSP1" },
          { id: "OTHER_MSP2", text: "MSP2" },
          { id: "OTHER_GLURP_POLYA", text: "Glurp/Polya" },
        ];
      // Vector qPCR Assays
      case "VECTOR_QPCR":
        return [
          { id: "VQPCR_BLOOD_COW", text: "Blood-meal PCR - Cow" },
          { id: "VQPCR_BLOOD_DOG", text: "Blood-meal PCR - Dog" },
          { id: "VQPCR_BLOOD_HUMAN", text: "Blood-meal PCR - Human" },
          { id: "VQPCR_BLOOD_GOAT", text: "Blood-meal PCR - Goat" },
          { id: "VQPCR_BLOOD_CHICKEN", text: "Blood-meal PCR - Chicken" },
          { id: "VQPCR_BLOOD_HORSE", text: "Blood-meal PCR - Horse" },
          { id: "VQPCR_BLOOD_RAT", text: "Blood-meal PCR - Rat" },
          { id: "VQPCR_BLOOD_CAMEL", text: "Blood-meal PCR - Camel" },
          {
            id: "VQPCR_ANOPHELES_ID",
            text: "Anopheles ID PCR (species-specific)",
          },
          { id: "VQPCR_KDR", text: "KDR PCR" },
          { id: "VQPCR_G6PD", text: "G6PD PCR" },
          { id: "VQPCR_HBB", text: "HBB PCR" },
          { id: "VQPCR_DARC", text: "DARC PCR" },
        ];
      // Vector Conventional PCR
      case "VECTOR_CONVENTIONAL_PCR":
        return [
          { id: "VPCR_BLOOD_PANEL", text: "Blood-meal PCR Panels" },
          { id: "VPCR_AN_ARABIENSIS", text: "Anopheles arabiensis ID" },
          { id: "VPCR_AN_GAMBIAE", text: "Anopheles gambiae s.l. ID" },
          { id: "VPCR_AN_COLUZZII", text: "Anopheles coluzzii ID" },
          { id: "VPCR_AN_STEPHENSI", text: "Anopheles stephensi ID" },
          { id: "VPCR_AN_FUNESTUS", text: "Anopheles funestus ID" },
          { id: "VPCR_KDR", text: "KDR" },
          { id: "VPCR_G6PD", text: "G6PD" },
          { id: "VPCR_HBB", text: "HBB" },
          { id: "VPCR_DARC", text: "DARC" },
          { id: "VPCR_PH_LONGIPES", text: "Phlebotomus longipes" },
          { id: "VPCR_PH_CELIAE", text: "Phlebotomus celiae" },
          { id: "VPCR_PH_MARTINI", text: "Phlebotomus martini" },
          { id: "VPCR_PH_ORIENTALIS", text: "Phlebotomus orientalis" },
          { id: "VPCR_PH_PEDIFER", text: "Phlebotomus pedifer" },
          { id: "VPCR_OTHER", text: "Others" },
        ];
      // ELISA
      case "SERO_ELISA":
        return [
          { id: "ELISA_CSP", text: "CSP ELISA Assay" },
          { id: "ELISA_PF_PV_MSP19", text: "Pf/Pv MSP19" },
          { id: "ELISA_PF_PV_AMA", text: "Pf/Pv AMA" },
        ];
      // Bead-Based Multiplex
      case "SERO_BEAD_MULTIPLEX":
        return [
          {
            id: "BEAD_MULTIPLEX_AB_PF_PV",
            text: "Multiplex Antibody (Pf and Pv)",
          },
          {
            id: "BEAD_MULTIPLEX_AG_PF_PV",
            text: "Multiplex Antigen (Pf and Pv)",
          },
          { id: "BEAD_CSP", text: "CSP Bead-based Assay" },
          {
            id: "BEAD_PROCARTAPLEX",
            text: "ProcartaPlex Human Cytokine/Chemokine 45-plex",
          },
          { id: "BEAD_INFLAMMATORY", text: "Inflammatory Biomarkers" },
          { id: "BEAD_G6PD_HBB_DARC", text: "G6PD, HBB, DARC Genotyping" },
          { id: "BEAD_SNP", text: "SNP Assays" },
        ];
      // ELISPOT
      case "SERO_ELISPOT":
        return [
          {
            id: "ELISPOT_LEISH_TCELL",
            text: "Leishmania Antigen-specific T Cell Responses",
          },
        ];
      // FluoroSpot
      case "SERO_FLUOROSPOT":
        return [
          {
            id: "FLUORO_LEISH_TCELL",
            text: "Leishmania Antigen-specific T Cell Responses",
          },
        ];
      // Flow Cytometry
      case "SERO_FLOW_CYTOMETRY":
        return [
          { id: "FLOW_MULTICOLOR", text: "Multi-color Flow Cytometry" },
          { id: "FLOW_TCELL_PAN", text: "T Cell Pan-markers" },
          { id: "FLOW_TCELL_ACTIVATION", text: "T Cell Activation Markers" },
          { id: "FLOW_TCELL_MEMORY", text: "T Cell Memory Markers" },
        ];
      // Leishmania Culture
      case "CULTURE_LEISHMANIA":
        return [
          { id: "CULT_LEISH_MASS", text: "Mass Cultivation of Promastigotes" },
          { id: "CULT_LEISH_DRUG", text: "Drug Susceptibility Assays" },
          { id: "CULT_LEISH_GROWTH", text: "Growth Rate Analysis" },
          {
            id: "CULT_LEISH_VIABILITY",
            text: "Cell Viability and DNA Synthesis Assays",
          },
        ];
      // Malaria Culture
      case "CULTURE_MALARIA":
        return [
          {
            id: "CULT_MAL_RSA",
            text: "Ring-stage Survival Assay (RSA) for Artemisinin",
          },
          {
            id: "CULT_MAL_HYPOXANTHINE",
            text: "Hypoxanthine Incorporation Assay for Non-artemisinins",
          },
        ];
      default:
        return [];
    }
  };

  // Set instruments from notebook when prop changes
  useEffect(() => {
    if (notebookInstruments && Array.isArray(notebookInstruments)) {
      setInstruments(
        notebookInstruments.map((i) => ({
          id: String(i.id),
          text: i.value || i.name,
          physicalId: i.serialNumber || "N/A",
          name: i.value || i.name,
          serialNumber: i.serialNumber,
          ...i,
        })),
      );
    } else {
      setInstruments([]);
    }
  }, [notebookInstruments]);

  // Load samples for this page - only QC Passed samples from page 6
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

    // Skip loading for synthetic page IDs
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
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Previous page data (extraction from page 6)
              extractionMethod: sample.data?.extractionMethod,
              // Test assignment data
              experimentCategory: sample.data?.experimentCategory,
              subcategory: sample.data?.subcategory,
              specificAssay: sample.data?.specificAssay,
              // Machine scheduling data
              instrument: sample.data?.instrument,
              instrumentId: sample.data?.instrumentId,
              scheduledDate: sample.data?.scheduledDate,
              timeSlot: sample.data?.timeSlot,
              startTime: sample.data?.startTime,
              endTime: sample.data?.endTime,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const assigned = samples.filter((s) => s.experimentCategory).length;
    const scheduled = samples.filter((s) => s.instrument).length;
    const fullyReady = samples.filter(
      (s) => s.experimentCategory && s.instrument,
    ).length;
    const pending = samples.filter(
      (s) => !s.experimentCategory && !s.instrument,
    ).length;
    return { total: samples.length, assigned, scheduled, fullyReady, pending };
  }, [samples]);

  // Handle opening test assignment modal
  const handleOpenAssignmentModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowAssignmentModal(true);
  }, [selectedIds, intl]);

  // Handle saving test assignment data
  const handleSaveAssignmentData = useCallback(() => {
    if (!assignmentData.experimentCategory) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.categoryRequired",
          defaultMessage: "Experiment category is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowAssignmentModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      experimentCategory: assignmentData.experimentCategory,
      subcategory: assignmentData.subcategory,
      specificAssay: assignmentData.specificAssay,
      assignmentNotes: assignmentData.notes,
      assignmentDate: new Date().toISOString().split("T")[0],
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.testassignment.assignmentSaved",
                      defaultMessage:
                        "Test assignment saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowAssignmentModal(false);
                setSelectedIds([]);
                // Reset form
                setAssignmentData({
                  experimentCategory: "",
                  subcategory: "",
                  specificAssay: "",
                  notes: "",
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save test assignment.");
          }
        }
      },
    );
  }, [
    assignmentData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle opening machine scheduling modal
  const handleOpenSchedulingModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowSchedulingModal(true);
  }, [selectedIds, intl]);

  // Handle saving machine scheduling data
  const handleSaveSchedulingData = useCallback(() => {
    if (!schedulingData.instrument) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.instrumentRequired",
          defaultMessage: "Instrument selection is required.",
        }),
      );
      return;
    }

    if (!schedulingData.scheduledDate) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.dateRequired",
          defaultMessage: "Scheduled date is required.",
        }),
      );
      return;
    }

    if (!schedulingData.timeSlot) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.timeSlotRequired",
          defaultMessage: "Time slot is required.",
        }),
      );
      return;
    }

    // Validate custom time if selected
    if (
      schedulingData.timeSlot === "SLOT_CUSTOM" &&
      (!schedulingData.startTime || !schedulingData.endTime)
    ) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.customTimeRequired",
          defaultMessage:
            "Start and end times are required for custom time range.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowSchedulingModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Get instrument physical ID
    const selectedInstrument = instruments.find(
      (i) => i.id === schedulingData.instrument,
    );
    const instrumentPhysicalId =
      selectedInstrument?.physicalId || schedulingData.instrumentId;

    // Extract start/end times from time slot or use custom values
    let startTime = schedulingData.startTime;
    let endTime = schedulingData.endTime;
    if (schedulingData.timeSlot !== "SLOT_CUSTOM") {
      const slotTimeMap = {
        SLOT_0800_1000: { start: "08:00", end: "10:00" },
        SLOT_1000_1200: { start: "10:00", end: "12:00" },
        SLOT_1200_1400: { start: "12:00", end: "14:00" },
        SLOT_1400_1600: { start: "14:00", end: "16:00" },
        SLOT_1600_1800: { start: "16:00", end: "18:00" },
        SLOT_1800_2000: { start: "18:00", end: "20:00" },
        SLOT_OVERNIGHT: { start: "20:00", end: "08:00" },
      };
      const slotTimes = slotTimeMap[schedulingData.timeSlot];
      if (slotTimes) {
        startTime = slotTimes.start;
        endTime = slotTimes.end;
      }
    }

    const dataToSave = {
      instrument: schedulingData.instrument,
      instrumentId: instrumentPhysicalId,
      scheduledDate: schedulingData.scheduledDate,
      timeSlot: schedulingData.timeSlot,
      startTime: startTime,
      endTime: endTime,
      // Activity logging timestamp
      scheduledAt: new Date().toISOString(),
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.mntd.testassignment.schedulingSaved",
                  defaultMessage:
                    "Machine scheduling saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setShowSchedulingModal(false);
            setSelectedIds([]);
            // Reset form
            setSchedulingData({
              instrument: "",
              instrumentId: "",
              scheduledDate: new Date().toISOString().split("T")[0],
              timeSlot: "",
              startTime: "",
              endTime: "",
            });
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save machine scheduling.");
          }
        }
      },
    );
  }, [
    schedulingData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.testassignment.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as ready for testing (complete this page)
  const handleBulkMarkReady = useCallback(() => {
    if (selectedIds.length === 0) return;

    // Check that selected samples have both assignment and scheduling
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const incompleteCount = selectedSamples.filter(
      (s) => !s.experimentCategory || !s.instrument,
    ).length;

    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.mntd.testassignment.incompleteWarning",
            defaultMessage:
              "{count} sample(s) are missing test assignment or machine scheduling. Please complete both before marking as ready.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: numericIds,
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.testassignment.markedReady",
                defaultMessage:
                  "Marked {count} samples as ready for test execution.",
              },
              { count: selectedIds.length },
            ),
          );
          loadPageSamples();
          setSelectedIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedIds,
    samples,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Get experiment category label
  const getCategoryLabel = (categoryId) => {
    const category = experimentCategoryOptions.find((c) => c.id === categoryId);
    return category ? category.text : categoryId;
  };

  // Get subcategory label
  const getSubcategoryLabel = (categoryId, subcategoryId) => {
    const subcategories = getSubcategoryOptions(categoryId);
    const subcategory = subcategories.find((s) => s.id === subcategoryId);
    return subcategory ? subcategory.text : subcategoryId;
  };

  // Get specific assay label
  const getAssayLabel = (subcategoryId, assayId) => {
    const assays = getSpecificAssayOptions(subcategoryId);
    const assay = assays.find((a) => a.id === assayId);
    return assay ? assay.text : assayId;
  };

  // Get instrument label
  const getInstrumentLabel = (instrumentId) => {
    const instrument = instruments.find((i) => i.id === instrumentId);
    return instrument ? instrument.text : instrumentId;
  };

  // Render test assignment info column
  const renderAssignmentInfo = (sample) => {
    if (sample.experimentCategory) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            {getCategoryLabel(sample.experimentCategory)}
          </Tag>
          {sample.subcategory && (
            <div style={{ marginTop: "2px", fontWeight: "500" }}>
              {getSubcategoryLabel(
                sample.experimentCategory,
                sample.subcategory,
              )}
            </div>
          )}
          {sample.specificAssay && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              {getAssayLabel(sample.subcategory, sample.specificAssay)}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testassignment.notAssigned"
          defaultMessage="Not assigned"
        />
      </span>
    );
  };

  // Get time slot label
  const getTimeSlotLabel = (timeSlotId) => {
    const slot = timeSlotOptions.find((t) => t.id === timeSlotId);
    return slot ? slot.text : timeSlotId;
  };

  // Render machine scheduling info column
  const renderSchedulingInfo = (sample) => {
    if (sample.instrument) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="teal" size="sm">
            <Settings size={12} style={{ marginRight: "4px" }} />
            {getInstrumentLabel(sample.instrument)}
          </Tag>
          {sample.instrumentId && (
            <div
              style={{ marginTop: "2px", color: "#6f6f6f", fontSize: "11px" }}
            >
              ID: {sample.instrumentId}
            </div>
          )}
          {sample.scheduledDate && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              <Calendar size={12} style={{ marginRight: "4px" }} />
              {sample.scheduledDate}
            </div>
          )}
          {sample.timeSlot && (
            <div style={{ color: "#525252" }}>
              {sample.startTime && sample.endTime
                ? `${sample.startTime} - ${sample.endTime}`
                : getTimeSlotLabel(sample.timeSlot)}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testassignment.notScheduled"
          defaultMessage="Not scheduled"
        />
      </span>
    );
  };

  return (
    <div className="mntd-test-assignment-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.testassignment.title"
            defaultMessage="Test Assignment & Machine Scheduling"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.testassignment.description"
            defaultMessage="Link samples to experiments and schedule machine time. Assign experiment category and specific assay, then schedule instrument and operator for test execution."
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
                  id="notebook.mntd.testassignment.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.testAssigned"
                  defaultMessage="Test Assigned"
                />
              </span>
              <span className="progress-value">{stats.assigned}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.machineScheduled"
                  defaultMessage="Machine Scheduled"
                />
              </span>
              <span className="progress-value">{stats.scheduled}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.fullyReady"
                  defaultMessage="Fully Ready"
                />
              </span>
              <span className="progress-value">{stats.fullyReady}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

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

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenAssignmentModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testassignment.assignTest"
            defaultMessage="Assign Test ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Calendar}
          onClick={handleOpenSchedulingModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testassignment.scheduleMachine"
            defaultMessage="Schedule Machine ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkReady}
          >
            <FormattedMessage
              id="notebook.mntd.testassignment.markReady"
              defaultMessage="Mark Ready for Testing ({count})"
              values={{ count: selectedIds.length }}
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
            id="notebook.mntd.testassignment.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="mntd-test-assignment"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "assignmentInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.testassignment.testAssignment",
                defaultMessage: "Test Assignment",
              }),
              render: renderAssignmentInfo,
            },
            {
              key: "schedulingInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.testassignment.machineScheduling",
                defaultMessage: "Machine Scheduling",
              }),
              render: renderSchedulingInfo,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.mntd.testassignment.empty"
              defaultMessage="No samples available for test assignment. Please complete the Processing & QC step first (samples must pass QC)."
            />
          </p>
        </div>
      )}

      {/* Test Assignment Modal */}
      <Modal
        open={showAssignmentModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.assignmentTitle",
          defaultMessage: "Assign Test",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowAssignmentModal(false)}
        onRequestSubmit={handleSaveAssignmentData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testassignment.modal.assignmentDescription"
              defaultMessage="Assign test experiment category and assay for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Experiment Category Selection */}
          <Dropdown
            id="experiment-category"
            titleText={intl.formatMessage({
              id: "notebook.mntd.testassignment.experimentCategory",
              defaultMessage: "Experiment Category",
            })}
            label={intl.formatMessage({
              id: "notebook.mntd.testassignment.selectCategory",
              defaultMessage: "Select category",
            })}
            items={experimentCategoryOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={experimentCategoryOptions.find(
              (c) => c.id === assignmentData.experimentCategory,
            )}
            onChange={({ selectedItem }) =>
              setAssignmentData({
                ...assignmentData,
                experimentCategory: selectedItem?.id || "",
                subcategory: "", // Reset subcategory when category changes
                specificAssay: "", // Reset assay when category changes
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Subcategory Selection - shown when category is selected */}
          {assignmentData.experimentCategory &&
            getSubcategoryOptions(assignmentData.experimentCategory).length >
              0 && (
              <Dropdown
                id="subcategory"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testassignment.subcategory",
                  defaultMessage: "Test Type / Subcategory",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testassignment.selectSubcategory",
                  defaultMessage: "Select test type",
                })}
                items={getSubcategoryOptions(assignmentData.experimentCategory)}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={getSubcategoryOptions(
                  assignmentData.experimentCategory,
                ).find((s) => s.id === assignmentData.subcategory)}
                onChange={({ selectedItem }) =>
                  setAssignmentData({
                    ...assignmentData,
                    subcategory: selectedItem?.id || "",
                    specificAssay: "", // Reset assay when subcategory changes
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            )}

          {/* Specific Assay Selection - shown when subcategory is selected */}
          {assignmentData.subcategory &&
            getSpecificAssayOptions(assignmentData.subcategory).length > 0 && (
              <Dropdown
                id="specific-assay"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testassignment.specificAssay",
                  defaultMessage: "Specific Assay / Protocol",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testassignment.selectAssay",
                  defaultMessage: "Select assay",
                })}
                items={getSpecificAssayOptions(assignmentData.subcategory)}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={getSpecificAssayOptions(
                  assignmentData.subcategory,
                ).find((a) => a.id === assignmentData.specificAssay)}
                onChange={({ selectedItem }) =>
                  setAssignmentData({
                    ...assignmentData,
                    specificAssay: selectedItem?.id || "",
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            )}

          {/* Notes */}
          <TextArea
            id="assignment-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testassignment.notes",
              defaultMessage: "Notes",
            })}
            value={assignmentData.notes}
            onChange={(e) =>
              setAssignmentData({ ...assignmentData, notes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>

      {/* Machine Scheduling Modal */}
      <Modal
        open={showSchedulingModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.schedulingTitle",
          defaultMessage: "Schedule Machine",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowSchedulingModal(false)}
        onRequestSubmit={handleSaveSchedulingData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testassignment.modal.schedulingDescription"
              defaultMessage="Schedule machine time for {count} selected sample(s). All fields marked with * are required."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Instrument Selection Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#0f62fe" }}>
              <Settings
                size={16}
                style={{ marginRight: "0.5rem", verticalAlign: "middle" }}
              />
              <FormattedMessage
                id="notebook.mntd.testassignment.instrumentSelection"
                defaultMessage="Instrument Selection"
              />
            </h5>

            {instruments.length === 0 ? (
              <div
                style={{
                  padding: "0.5rem",
                  backgroundColor: "#fff3cd",
                  border: "1px solid #ffc107",
                  borderRadius: "4px",
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.mntd.testassignment.noInstrumentsAttached"
                  defaultMessage="No instruments attached to this notebook. Please add instruments in the notebook configuration."
                />
              </div>
            ) : (
              <Dropdown
                id="instrument"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testassignment.instrument",
                  defaultMessage: "Instrument *",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testassignment.selectInstrument",
                  defaultMessage: "Select instrument",
                })}
                items={instruments}
                itemToString={(item) =>
                  item ? `${item.text} (${item.physicalId})` : ""
                }
                selectedItem={instruments.find(
                  (i) => i.id === schedulingData.instrument,
                )}
                onChange={({ selectedItem }) =>
                  setSchedulingData({
                    ...schedulingData,
                    instrument: selectedItem?.id || "",
                    instrumentId: selectedItem?.physicalId || "",
                  })
                }
                style={{ marginBottom: "0.5rem" }}
                disabled={instruments.length === 0}
              />
            )}

            {schedulingData.instrument && (
              <div
                style={{
                  fontSize: "12px",
                  color: "#525252",
                  marginTop: "0.25rem",
                }}
              >
                <strong>Instrument ID:</strong>{" "}
                {instruments.find((i) => i.id === schedulingData.instrument)
                  ?.physicalId || "N/A"}
              </div>
            )}
          </div>

          {/* Scheduled Date & Time Slot Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#defbe6",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #24a148",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#24a148" }}>
              <Calendar
                size={16}
                style={{ marginRight: "0.5rem", verticalAlign: "middle" }}
              />
              <FormattedMessage
                id="notebook.mntd.testassignment.scheduledDateTime"
                defaultMessage="Date & Time Slot"
              />
            </h5>

            <Grid fullWidth style={{ marginBottom: "0.5rem" }}>
              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  value={schedulingData.scheduledDate}
                  onChange={([date]) =>
                    setSchedulingData({
                      ...schedulingData,
                      scheduledDate: date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="scheduled-date"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.testassignment.date",
                      defaultMessage: "Scheduled Date *",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="time-slot"
                  titleText={intl.formatMessage({
                    id: "notebook.mntd.testassignment.timeSlot",
                    defaultMessage: "Time Slot *",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.mntd.testassignment.selectTimeSlot",
                    defaultMessage: "Select time slot",
                  })}
                  items={timeSlotOptions}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={timeSlotOptions.find(
                    (t) => t.id === schedulingData.timeSlot,
                  )}
                  onChange={({ selectedItem }) =>
                    setSchedulingData({
                      ...schedulingData,
                      timeSlot: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
            </Grid>

            {/* Custom Time Range - shown only when Custom is selected */}
            {schedulingData.timeSlot === "SLOT_CUSTOM" && (
              <Grid fullWidth style={{ marginTop: "0.5rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="start-time"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.testassignment.startTime",
                      defaultMessage: "Start Time *",
                    })}
                    value={schedulingData.startTime}
                    onChange={(e) =>
                      setSchedulingData({
                        ...schedulingData,
                        startTime: e.target.value,
                      })
                    }
                    placeholder="HH:MM (e.g., 09:30)"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="end-time"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.testassignment.endTime",
                      defaultMessage: "End Time *",
                    })}
                    value={schedulingData.endTime}
                    onChange={(e) =>
                      setSchedulingData({
                        ...schedulingData,
                        endTime: e.target.value,
                      })
                    }
                    placeholder="HH:MM (e.g., 12:30)"
                  />
                </Column>
              </Grid>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default MNTDTestAssignmentPage;
