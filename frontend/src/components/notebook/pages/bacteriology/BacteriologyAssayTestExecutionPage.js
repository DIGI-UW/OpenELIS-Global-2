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
  MultiSelect,
  DatePicker,
  DatePickerInput,
  NumberInput,
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
  InlineLoading,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Accordion,
  AccordionItem,
  Checkbox,
  FileUploader,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  ExpandableTile,
  TileAboveTheFoldContent,
  TileBelowTheFoldContent,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Renew,
  Chemistry,
  Microscope,
  WarningAlt,
  View,
  Add,
  TrashCan,
  DocumentExport,
  DocumentImport,
  Upload,
  Run,
  Report,
  DataBase,
  Edit,
  ChartLine,
  DataVis_2,
  Activity,
  ListChecked,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologyAssayTestExecutionPage - Page 5 of the Bacteriology workflow.
 * Handles comprehensive test execution including:
 *
 * SECTION A: Primary Culture & Microscopy
 * - Gram Staining
 * - Microscopy Examination
 *
 * SECTION B: Isolation & Identification
 * - Culture on selective/differential media
 * - Biochemical Tests (Catalase, Coagulase, Oxidase, TSI/KIA, Motility, Indole, Citrate, Urease, Lysine Decarboxylase, MR-VP, Nitrate reduction)
 *
 * SECTION C: Drug Susceptibility Testing (DST)
 * - Disc Diffusion (Kirby-Bauer)
 * - Broth Microdilution (MIC)
 * - AST Strip (E-test)
 * - Automated AST
 *
 * SECTION D: Automated Identification and AST
 * - BD BACTEC
 * - VITEK/Phoenix
 * - MALDI-TOF
 *
 * SECTION E: Molecular Techniques
 * - Nucleic Acid Extraction
 * - PCR Assays (Conventional, Real-Time qPCR)
 * - Whole Genome Sequencing (WGS)
 *
 * Who uses it:
 * - Lab technician
 * - Microbiologist
 * - Molecular biologist
 * - Supervisor
 *
 * System Actions:
 * - Test results recorded
 * - Raw data stored
 * - QC validation performed
 *
 * Leads to: Results Interpretation & Reporting
 */

// ==========================================
// SECTION A: Primary Culture & Microscopy Constants
// ==========================================

// Gram Stain Results
const GRAM_STAIN_RESULTS = [
  { id: "GRAM_POS_COCCI_CLUSTERS", text: "Gram-positive cocci in clusters" },
  { id: "GRAM_POS_COCCI_CHAINS", text: "Gram-positive cocci in chains" },
  { id: "GRAM_POS_COCCI_PAIRS", text: "Gram-positive cocci in pairs" },
  { id: "GRAM_POS_BACILLI", text: "Gram-positive bacilli" },
  { id: "GRAM_POS_BACILLI_SPORE", text: "Gram-positive bacilli with spores" },
  { id: "GRAM_NEG_COCCI", text: "Gram-negative cocci" },
  { id: "GRAM_NEG_BACILLI", text: "Gram-negative bacilli" },
  { id: "GRAM_NEG_COCCOBACILLI", text: "Gram-negative coccobacilli" },
  { id: "MIXED_FLORA", text: "Mixed flora" },
  { id: "NO_ORGANISMS", text: "No organisms seen" },
  { id: "YEAST", text: "Yeast cells" },
  { id: "YEAST_PSEUDOHYPHAE", text: "Yeast with pseudohyphae" },
  { id: "OTHER", text: "Other (specify)" },
];

// Microscopy Stain Types (limited to common bacteriology stains)
const MICROSCOPY_STAIN_TYPES = [
  { id: "GRAM", text: "Gram Stain" },
  { id: "ZN", text: "Ziehl-Neelsen (AFB)" },
  { id: "INDIA_INK", text: "India Ink" },
  { id: "WET_MOUNT", text: "Wet Mount (Saline)" },
  { id: "OTHER", text: "Other (specify)" },
];

// ==========================================
// SECTION B: Isolation & Identification Constants
// ==========================================

// Culture Media Types for Isolation
const ISOLATION_MEDIA_TYPES = [
  { id: "BLOOD_AGAR", text: "Blood Agar (BAP)" },
  { id: "CHOCOLATE_AGAR", text: "Chocolate Agar" },
  { id: "MACCONKEY", text: "MacConkey Agar" },
  { id: "EMB", text: "Eosin Methylene Blue (EMB)" },
  { id: "XLD", text: "XLD Agar" },
  { id: "SS_AGAR", text: "Salmonella-Shigella (SS) Agar" },
  { id: "TCBS", text: "TCBS Agar" },
  { id: "MANNITOL_SALT", text: "Mannitol Salt Agar (MSA)" },
  { id: "MUELLER_HINTON", text: "Mueller-Hinton Agar" },
  { id: "CLED", text: "CLED Agar" },
  { id: "CHROMAGAR", text: "CHROMagar" },
  { id: "SABOURAUD", text: "Sabouraud Dextrose Agar" },
  { id: "OTHER", text: "Other (specify)" },
];

// Colony Morphology Options
const COLONY_MORPHOLOGY = [
  { id: "LARGE", text: "Large (>2mm)" },
  { id: "MEDIUM", text: "Medium (1-2mm)" },
  { id: "SMALL", text: "Small (<1mm)" },
  { id: "PINPOINT", text: "Pinpoint" },
  { id: "MUCOID", text: "Mucoid" },
  { id: "SMOOTH", text: "Smooth" },
  { id: "ROUGH", text: "Rough" },
  { id: "BETA_HEMOLYTIC", text: "Beta-hemolytic" },
  { id: "ALPHA_HEMOLYTIC", text: "Alpha-hemolytic" },
  { id: "GAMMA_HEMOLYTIC", text: "Gamma (non-hemolytic)" },
  { id: "LACTOSE_FERMENTER", text: "Lactose fermenter" },
  { id: "NON_LACTOSE_FERMENTER", text: "Non-lactose fermenter" },
  { id: "PIGMENTED", text: "Pigmented" },
  { id: "SWARMING", text: "Swarming" },
];

// Biochemical Test Types - Limited to user specification:
// Catalase, Coagulase, Oxidase, TSI/KIA, Motility, Indole, Citrate, Urease, LDC, MR-VP, Nitrate reduction
const BIOCHEMICAL_TESTS = [
  {
    id: "CATALASE",
    text: "Catalase Test",
    positiveResult: "Bubbles/effervescence",
    negativeResult: "No reaction",
  },
  {
    id: "COAGULASE",
    text: "Coagulase Test",
    positiveResult: "Clot formation",
    negativeResult: "No clot",
  },
  {
    id: "OXIDASE",
    text: "Oxidase Test",
    positiveResult: "Purple/blue color",
    negativeResult: "No color change",
  },
  {
    id: "TSI",
    text: "TSI (Triple Sugar Iron)",
    positiveResult: "Variable (A/A, K/A, etc.)",
    negativeResult: "K/K",
  },
  {
    id: "KIA",
    text: "KIA (Kligler Iron Agar)",
    positiveResult: "Variable",
    negativeResult: "K/K",
  },
  {
    id: "MOTILITY",
    text: "Motility Test",
    positiveResult: "Diffuse growth",
    negativeResult: "Growth along stab line",
  },
  {
    id: "INDOLE",
    text: "Indole Test",
    positiveResult: "Red ring",
    negativeResult: "No color change",
  },
  {
    id: "CITRATE",
    text: "Citrate Utilization",
    positiveResult: "Blue color",
    negativeResult: "Green (no change)",
  },
  {
    id: "UREASE",
    text: "Urease Test",
    positiveResult: "Pink/red color",
    negativeResult: "Yellow (no change)",
  },
  {
    id: "LDC",
    text: "Lysine Decarboxylase (LDC)",
    positiveResult: "Purple color",
    negativeResult: "Yellow color",
  },
  {
    id: "MR",
    text: "Methyl Red (MR)",
    positiveResult: "Red color",
    negativeResult: "Yellow color",
  },
  {
    id: "VP",
    text: "Voges-Proskauer (VP)",
    positiveResult: "Red color",
    negativeResult: "No color change",
  },
  {
    id: "NITRATE",
    text: "Nitrate Reduction",
    positiveResult: "Red color (after reagents)",
    negativeResult: "No color/zinc positive",
  },
];

// Biochemical Test Results
const BIOCHEMICAL_RESULTS = [
  { id: "POSITIVE", text: "Positive (+)" },
  { id: "NEGATIVE", text: "Negative (-)" },
  { id: "WEAKLY_POSITIVE", text: "Weakly Positive (+/-)" },
  { id: "VARIABLE", text: "Variable" },
  { id: "NOT_DONE", text: "Not Done" },
];

// TSI/KIA Specific Results
const TSI_KIA_RESULTS = [
  { id: "A_A", text: "A/A (Acid/Acid) - Glucose & Lactose/Sucrose fermented" },
  { id: "K_A", text: "K/A (Alkaline/Acid) - Glucose only fermented" },
  { id: "K_K", text: "K/K (Alkaline/Alkaline) - No fermentation" },
  { id: "A_A_H2S", text: "A/A with H2S" },
  { id: "K_A_H2S", text: "K/A with H2S" },
  { id: "A_A_GAS", text: "A/A with Gas" },
  { id: "K_A_GAS", text: "K/A with Gas" },
];

// ==========================================
// SECTION C: Drug Susceptibility Testing Constants
// ==========================================

// DST Methods - per user specification
const DST_METHODS = [
  {
    id: "DISC_DIFFUSION",
    text: "Disc Diffusion (Kirby-Bauer)",
    description: "Standard method - Measure zone of inhibition",
  },
  {
    id: "BROTH_MICRODILUTION",
    text: "Broth Microdilution (MIC)",
    description:
      "Determine Minimum Inhibitory Concentration - Quantitative result",
  },
  {
    id: "AST_STRIP",
    text: "AST Strip (E-test)",
    description: "Gradient method for MIC determination",
  },
  {
    id: "AUTOMATED_AST",
    text: "Automated AST",
    description: "Automated AST systems",
  },
];

// Interpretation Guidelines - per CLSI/EUCAST
const INTERPRETATION_GUIDELINES = [
  { id: "CLSI", text: "CLSI" },
  { id: "EUCAST", text: "EUCAST" },
  { id: "OTHER", text: "Other" },
];

// Antibiotic Panel Selection - per user specification
// Panel depends on organism and clinical scenario
// 2nd line if resistance detected or clinically indicated
const ANTIBIOTIC_PANELS = [
  {
    id: "FIRST_LINE",
    text: "1st Line Antibiotics",
    description: "Standard panel for initial testing",
  },
  {
    id: "SECOND_LINE",
    text: "2nd Line Antibiotics",
    description: "If resistance detected or clinically indicated",
  },
  {
    id: "BOTH",
    text: "Both Panels",
    description: "Full 1st and 2nd line testing",
  },
];

// Susceptibility Interpretation - S, I, R per CLSI/EUCAST guidelines
const SUSCEPTIBILITY_INTERPRETATION = [
  { id: "S", text: "Susceptible (S)" },
  { id: "I", text: "Intermediate (I)" },
  { id: "R", text: "Resistant (R)" },
];

// ==========================================
// SECTION D: Automated Identification Constants
// ==========================================

// Confidence Levels for automated identification - machine-agnostic
const CONFIDENCE_LEVELS = [
  { id: "HIGH", text: "High Confidence" },
  { id: "MODERATE", text: "Moderate Confidence" },
  { id: "LOW", text: "Low Confidence" },
];

// ==========================================
// SECTION E: Molecular Techniques Constants
// ==========================================

// Extraction Methods - Limited to user specification: kit-based, manual
const EXTRACTION_METHODS = [
  { id: "KIT_BASED", text: "Kit-based Extraction" },
  { id: "MANUAL", text: "Manual Extraction" },
  { id: "OTHER", text: "Other (specify)" },
];

// Nucleic Acid Types
const NUCLEIC_ACID_TYPES = [
  { id: "DNA", text: "Genomic DNA" },
  { id: "RNA", text: "Total RNA" },
  { id: "PLASMID", text: "Plasmid DNA" },
  { id: "CDNA", text: "cDNA" },
];

// PCR Assay Types - Limited to user specification: conventional, qPCR
const PCR_ASSAY_TYPES = [
  { id: "CONVENTIONAL", text: "Conventional PCR" },
  { id: "REALTIME", text: "Real-Time PCR (qPCR)" },
  { id: "OTHER", text: "Other (specify)" },
];

// Common PCR Targets for Bacteriology
const PCR_TARGETS = [
  { id: "16S_RRNA", text: "16S rRNA (Universal bacteria)" },
  { id: "23S_RRNA", text: "23S rRNA" },
  { id: "MEC_A", text: "mecA (MRSA)" },
  { id: "VAN_A", text: "vanA (VRE)" },
  { id: "VAN_B", text: "vanB (VRE)" },
  { id: "CTX_M", text: "CTX-M (ESBL)" },
  { id: "KPC", text: "KPC (Carbapenemase)" },
  { id: "NDM", text: "NDM (Carbapenemase)" },
  { id: "OXA_48", text: "OXA-48 (Carbapenemase)" },
  { id: "VIM", text: "VIM (Carbapenemase)" },
  { id: "IMP", text: "IMP (Carbapenemase)" },
  { id: "MCR_1", text: "mcr-1 (Colistin resistance)" },
  { id: "TET_M", text: "tetM (Tetracycline resistance)" },
  { id: "ERM_B", text: "ermB (Macrolide resistance)" },
  { id: "TOX_A", text: "tcdA (C. difficile toxin A)" },
  { id: "TOX_B", text: "tcdB (C. difficile toxin B)" },
  { id: "SPA", text: "spa (S. aureus typing)" },
  { id: "PVL", text: "PVL (Panton-Valentine Leukocidin)" },
  { id: "SPECIES_SPECIFIC", text: "Species-specific target" },
  { id: "OTHER", text: "Other (specify)" },
];

// PCR Result Interpretation
const PCR_RESULTS = [
  { id: "DETECTED", text: "Detected (Positive)" },
  { id: "NOT_DETECTED", text: "Not Detected (Negative)" },
  { id: "INDETERMINATE", text: "Indeterminate" },
  { id: "INVALID", text: "Invalid (repeat required)" },
];

// Sequencing Platforms
const SEQUENCING_PLATFORMS = [
  { id: "ILLUMINA_MISEQ", text: "Illumina MiSeq" },
  { id: "ILLUMINA_NEXTSEQ", text: "Illumina NextSeq" },
  { id: "ILLUMINA_NOVASEQ", text: "Illumina NovaSeq" },
  { id: "ONT_MINION", text: "Oxford Nanopore MinION" },
  { id: "ONT_GRIDION", text: "Oxford Nanopore GridION" },
  { id: "PACBIO_SEQUEL", text: "PacBio Sequel" },
  { id: "ION_TORRENT", text: "Ion Torrent" },
  { id: "SANGER", text: "Sanger Sequencing" },
  { id: "OTHER", text: "Other (specify)" },
];

// WGS Analysis Types - Limited to user specification: epidemiology, resistance, virulence
const WGS_ANALYSIS_TYPES = [
  { id: "EPIDEMIOLOGY", text: "Epidemiological Analysis" },
  { id: "RESISTANCE", text: "Antimicrobial Resistance Detection" },
  { id: "VIRULENCE", text: "Virulence Factor Detection" },
  { id: "OTHER", text: "Other (specify)" },
];

// Quality Metrics for WGS
const WGS_QUALITY_METRICS = [
  { id: "COVERAGE", text: "Coverage Depth", unit: "X" },
  { id: "Q30", text: "Q30 Score", unit: "%" },
  { id: "N50", text: "N50", unit: "bp" },
  { id: "CONTIGS", text: "Number of Contigs", unit: "" },
  { id: "GENOME_SIZE", text: "Genome Size", unit: "Mb" },
];

// ==========================================
// Main Component
// ==========================================

function BacteriologyAssayTestExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
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

  // Active tab state (0-4 for sections A-E)
  const [activeTab, setActiveTab] = useState(0);

  // Reagents from inventory
  const [reagents, setReagents] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);

  // ==========================================
  // SECTION A: Primary Culture & Microscopy State
  // ==========================================

  const [microscopyModalOpen, setMicroscopyModalOpen] = useState(false);
  const [microscopyData, setMicroscopyData] = useState({
    stainType: "",
    otherStainType: "",
    gramStainResult: "",
    otherGramResult: "",
    otherFindings: "",
    microscopyDate: new Date().toISOString().split("T")[0],
    microscopyTime: "",
    performedBy: "",
    preliminaryId: "",
    qualityAssessment: "",
    notes: "",
  });

  // ==========================================
  // SECTION B: Isolation & Identification State
  // ==========================================

  const [cultureModalOpen, setCultureModalOpen] = useState(false);
  const [cultureData, setCultureData] = useState({
    mediaUsed: [],
    otherMedia: "",
    inoculationDate: new Date().toISOString().split("T")[0],
    inoculationTime: "",
    incubationCondition: "",
    incubationTemp: "",
    incubationDuration: "",
    inoculatedBy: "",
    notes: "",
  });

  const [biochemModalOpen, setBiochemModalOpen] = useState(false);
  const [biochemData, setBiochemData] = useState({
    testDate: new Date().toISOString().split("T")[0],
    performedBy: "",
    testResults: {}, // Map of testId -> result
    tsiResult: "",
    presumptiveId: "",
    notes: "",
  });

  const [colonyModalOpen, setColonyModalOpen] = useState(false);
  const [colonyData, setColonyData] = useState({
    mediaType: "",
    colonyCount: "",
    colonyMorphology: [],
    hemolysis: "",
    lactoseFermentation: "",
    pigmentation: "",
    odor: "",
    readingDate: new Date().toISOString().split("T")[0],
    readBy: "",
    isolateId: "",
    notes: "",
  });

  // ==========================================
  // SECTION C: Drug Susceptibility Testing State
  // ==========================================

  const [dstModalOpen, setDstModalOpen] = useState(false);
  const [dstData, setDstData] = useState({
    method: "", // DISC_DIFFUSION, BROTH_MICRODILUTION, AST_STRIP, AUTOMATED_AST
    guidelinesUsed: "", // CLSI, EUCAST, OTHER
    antibioticPanel: "", // FIRST_LINE, SECOND_LINE, BOTH
    antibioticResults: [], // Array of {panelType, antibiotic, zoneDiameter, mic, interpretation}
    notes: "",
  });

  // Antibiotic results for different methods
  // Each result: {panelType: "1ST_LINE"|"2ND_LINE", antibiotic: string, zoneDiameter?: string, mic?: string, interpretation: "S"|"I"|"R"}
  const [antibioticResults, setAntibioticResults] = useState([]);

  // ==========================================
  // SECTION D: Automated Identification State
  // Machine-agnostic interface for entering results from any automated system
  // ==========================================

  const [automatedIdModalOpen, setAutomatedIdModalOpen] = useState(false);
  const [automatedIdData, setAutomatedIdData] = useState({
    testDate: new Date().toISOString().split("T")[0],
    performedBy: "",
    organismIdentified: "",
    confidenceLevel: "",
    alternativeIds: "",
    notes: "",
  });

  // ==========================================
  // SECTION E: Molecular Techniques State
  // ==========================================

  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [extractionData, setExtractionData] = useState({
    method: "",
    otherMethod: "",
    nucleicAcidType: "",
    extractionDate: new Date().toISOString().split("T")[0],
    extractionTime: "",
    performedBy: "",
    kitUsed: "",
    kitLot: "",
    sampleInputVolume: "",
    elutionVolume: "",
    concentration: "",
    concentrationUnit: "ng/uL",
    purity260280: "",
    purity260230: "",
    qualityAssessment: "",
    storageLocation: "",
    notes: "",
  });

  const [pcrModalOpen, setPcrModalOpen] = useState(false);
  const [pcrData, setPcrData] = useState({
    assayType: "",
    otherAssayType: "",
    target: "",
    otherTarget: "",
    runId: "",
    testDate: new Date().toISOString().split("T")[0],
    performedBy: "",
    instrument: "",
    kitUsed: "",
    kitLot: "",
    primerSet: "",
    controlResults: {
      positive: "",
      negative: "",
      internal: "",
    },
    ctValue: "",
    result: "",
    rawDataFile: null,
    notes: "",
  });

  const [wgsModalOpen, setWgsModalOpen] = useState(false);
  const [wgsData, setWgsData] = useState({
    platform: "",
    otherPlatform: "",
    runId: "",
    sequencingDate: new Date().toISOString().split("T")[0],
    performedBy: "",
    libraryPrepKit: "",
    libraryPrepKitLot: "",
    indexUsed: "",
    loadingConcentration: "",
    analysisType: [],
    coverage: "",
    q30Score: "",
    n50: "",
    contigCount: "",
    genomeSize: "",
    speciesIdentified: "",
    mlstType: "",
    amrGenes: "",
    virulenceFactors: "",
    rawDataLocation: "",
    analysisNotes: "",
  });

  // File upload state
  const [uploadedFile, setUploadedFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  // ==========================================
  // Data Loading Functions
  // ==========================================

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();

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
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Test execution data
              microscopyCompleted: sample.data?.microscopyCompleted,
              gramStainResult: sample.data?.gramStainResult,
              cultureCompleted: sample.data?.cultureCompleted,
              biochemCompleted: sample.data?.biochemCompleted,
              presumptiveId: sample.data?.presumptiveId,
              dstCompleted: sample.data?.dstCompleted,
              dstMethod: sample.data?.dstMethod,
              automatedIdCompleted: sample.data?.automatedIdCompleted,
              organismIdentified: sample.data?.organismIdentified,
              molecularCompleted: sample.data?.molecularCompleted,
              pcrResult: sample.data?.pcrResult,
              wgsCompleted: sample.data?.wgsCompleted,
              // From previous pages
              processingStatus: sample.data?.processingStatus,
              sampleOrigin: sample.data?.sampleOrigin,
              projectName: sample.data?.projectName,
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

  // Load reagents from inventory
  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: r.id,
                label: `${r.name} (Lot: ${r.lotNumber || "N/A"})`,
                name: r.name,
                lotNumber: r.lotNumber,
                expiryDate: r.expiryDate,
                ...r,
              })),
            );
          } else {
            setReagents([]);
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const microscopyDone = samples.filter((s) => s.microscopyCompleted).length;
    const cultureDone = samples.filter((s) => s.cultureCompleted).length;
    const biochemDone = samples.filter((s) => s.biochemCompleted).length;
    const dstDone = samples.filter((s) => s.dstCompleted).length;
    const automatedDone = samples.filter((s) => s.automatedIdCompleted).length;
    const molecularDone = samples.filter((s) => s.molecularCompleted).length;
    const identified = samples.filter((s) => s.organismIdentified).length;
    const pending = samples.filter((s) => s.status === "PENDING").length;
    const completed = samples.filter((s) => s.status === "COMPLETED").length;

    return {
      total: samples.length,
      microscopyDone,
      cultureDone,
      biochemDone,
      dstDone,
      automatedDone,
      molecularDone,
      identified,
      pending,
      completed,
    };
  }, [samples]);

  // ==========================================
  // SECTION A: Microscopy Handlers
  // ==========================================

  const handleOpenMicroscopyModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setMicroscopyData({
      stainType: "",
      otherStainType: "",
      gramStainResult: "",
      otherGramResult: "",
      otherFindings: "",
      microscopyDate: new Date().toISOString().split("T")[0],
      microscopyTime: "",
      performedBy: "",
      preliminaryId: "",
      qualityAssessment: "",
      notes: "",
    });
    setMicroscopyModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveMicroscopyData = useCallback(() => {
    if (!hasRealPageId) {
      setMicroscopyModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      microscopyCompleted: true,
      stainType: microscopyData.stainType,
      gramStainResult:
        microscopyData.gramStainResult === "OTHER"
          ? microscopyData.otherGramResult
          : microscopyData.gramStainResult,
      otherFindings: microscopyData.otherFindings,
      microscopyDate: microscopyData.microscopyDate,
      microscopyTime: microscopyData.microscopyTime,
      microscopyPerformedBy: microscopyData.performedBy,
      preliminaryId: microscopyData.preliminaryId,
      microscopyQuality: microscopyData.qualityAssessment,
      microscopyNotes: microscopyData.notes,
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
                      id: "notebook.bacteriology.assay.microscopySaved",
                      defaultMessage:
                        "Microscopy data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setMicroscopyModalOpen(false);
                setSelectedIds([]);
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save microscopy data.");
          }
        }
      },
    );
  }, [
    microscopyData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // SECTION B: Culture & Biochemical Handlers
  // ==========================================

  const handleOpenCultureModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setCultureData({
      mediaUsed: [],
      otherMedia: "",
      inoculationDate: new Date().toISOString().split("T")[0],
      inoculationTime: "",
      incubationCondition: "",
      incubationTemp: "",
      incubationDuration: "",
      inoculatedBy: "",
      notes: "",
    });
    setCultureModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveCultureData = useCallback(() => {
    if (!hasRealPageId) {
      setCultureModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      cultureCompleted: true,
      cultureMediaUsed: cultureData.mediaUsed,
      otherCultureMedia: cultureData.otherMedia,
      inoculationDate: cultureData.inoculationDate,
      inoculationTime: cultureData.inoculationTime,
      incubationCondition: cultureData.incubationCondition,
      incubationTemp: cultureData.incubationTemp,
      incubationDuration: cultureData.incubationDuration,
      inoculatedBy: cultureData.inoculatedBy,
      cultureNotes: cultureData.notes,
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
                  id: "notebook.bacteriology.assay.cultureSaved",
                  defaultMessage: "Culture data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setCultureModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save culture data.");
          }
        }
      },
    );
  }, [
    cultureData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  const handleOpenBiochemModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setBiochemData({
      testDate: new Date().toISOString().split("T")[0],
      performedBy: "",
      testResults: {},
      tsiResult: "",
      presumptiveId: "",
      notes: "",
    });
    setBiochemModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveBiochemData = useCallback(() => {
    if (!hasRealPageId) {
      setBiochemModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      biochemCompleted: true,
      biochemTestDate: biochemData.testDate,
      biochemPerformedBy: biochemData.performedBy,
      biochemTestResults: biochemData.testResults,
      tsiResult: biochemData.tsiResult,
      presumptiveId: biochemData.presumptiveId,
      biochemNotes: biochemData.notes,
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
                  id: "notebook.bacteriology.assay.biochemSaved",
                  defaultMessage:
                    "Biochemical test data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setBiochemModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save biochemical data.");
          }
        }
      },
    );
  }, [
    biochemData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  const handleOpenColonyModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setColonyData({
      mediaType: "",
      colonyCount: "",
      colonyMorphology: [],
      hemolysis: "",
      lactoseFermentation: "",
      pigmentation: "",
      odor: "",
      readingDate: new Date().toISOString().split("T")[0],
      readBy: "",
      isolateId: "",
      notes: "",
    });
    setColonyModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveColonyData = useCallback(() => {
    if (!hasRealPageId) {
      setColonyModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      colonyReadingCompleted: true,
      colonyMediaType: colonyData.mediaType,
      colonyCount: colonyData.colonyCount,
      colonyMorphology: colonyData.colonyMorphology,
      hemolysis: colonyData.hemolysis,
      lactoseFermentation: colonyData.lactoseFermentation,
      pigmentation: colonyData.pigmentation,
      odor: colonyData.odor,
      colonyReadingDate: colonyData.readingDate,
      colonyReadBy: colonyData.readBy,
      isolateId: colonyData.isolateId,
      colonyNotes: colonyData.notes,
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
                  id: "notebook.bacteriology.assay.colonySaved",
                  defaultMessage:
                    "Colony reading data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setColonyModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save colony data.");
          }
        }
      },
    );
  }, [
    colonyData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // SECTION C: DST Handlers
  // ==========================================

  const handleOpenDSTModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setDstData({
      method: "",
      guidelinesUsed: "",
      antibioticPanel: "",
      antibioticResults: [],
      notes: "",
    });
    setAntibioticResults([]);
    setDstModalOpen(true);
  }, [selectedIds, intl]);

  // Add antibiotic result row based on method and panel selection
  const handleAddAntibioticResult = useCallback(() => {
    const newResult = {
      panelType:
        dstData.antibioticPanel === "FIRST_LINE"
          ? "1ST_LINE"
          : dstData.antibioticPanel === "SECOND_LINE"
            ? "2ND_LINE"
            : "1ST_LINE",
      antibiotic: "",
      interpretation: "",
    };

    // Add method-specific fields
    // Disc Diffusion: Zone of inhibition (mm) - Standard Kirby-Bauer method
    if (dstData.method === "DISC_DIFFUSION") {
      newResult.zoneDiameter = "";
    }
    // Broth Microdilution: MIC (µg/mL) - Quantitative result
    else if (dstData.method === "BROTH_MICRODILUTION") {
      newResult.mic = "";
    }
    // AST Strip (E-test): MIC (µg/mL) - Gradient method
    else if (dstData.method === "AST_STRIP") {
      newResult.mic = "";
    }
    // Automated AST: System result from automated systems
    else if (dstData.method === "AUTOMATED_AST") {
      newResult.automatedResult = "";
    }

    setAntibioticResults([...antibioticResults, newResult]);
  }, [dstData.method, dstData.antibioticPanel, antibioticResults]);

  const handleRemoveAntibioticResult = useCallback(
    (index) => {
      setAntibioticResults(antibioticResults.filter((_, i) => i !== index));
    },
    [antibioticResults],
  );

  const handleUpdateAntibioticResult = useCallback(
    (index, field, value) => {
      const updated = [...antibioticResults];
      updated[index] = { ...updated[index], [field]: value };
      setAntibioticResults(updated);
    },
    [antibioticResults],
  );

  const handleSaveDSTData = useCallback(() => {
    if (!hasRealPageId) {
      setDstModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      dstCompleted: true,
      dstMethod: dstData.method,
      dstGuidelinesUsed: dstData.guidelinesUsed,
      dstAntibioticPanel: dstData.antibioticPanel,
      dstAntibioticResults: antibioticResults,
      dstNotes: dstData.notes,
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
                  id: "notebook.bacteriology.assay.dstSaved",
                  defaultMessage: "DST data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setDstModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save DST data.");
          }
        }
      },
    );
  }, [
    dstData,
    antibioticResults,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // SECTION D: Automated Identification Handlers
  // ==========================================

  const handleOpenAutomatedIdModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setAutomatedIdData({
      testDate: new Date().toISOString().split("T")[0],
      performedBy: "",
      organismIdentified: "",
      confidenceLevel: "",
      alternativeIds: "",
      notes: "",
    });
    setAutomatedIdModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveAutomatedIdData = useCallback(() => {
    if (!hasRealPageId) {
      setAutomatedIdModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      automatedIdCompleted: true,
      automatedTestDate: automatedIdData.testDate,
      automatedPerformedBy: automatedIdData.performedBy,
      organismIdentified: automatedIdData.organismIdentified,
      automatedConfidenceLevel: automatedIdData.confidenceLevel,
      automatedAlternativeIds: automatedIdData.alternativeIds,
      automatedNotes: automatedIdData.notes,
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
                  id: "notebook.bacteriology.assay.automatedIdSaved",
                  defaultMessage:
                    "Automated ID data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setAutomatedIdModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save automated ID data.");
          }
        }
      },
    );
  }, [
    automatedIdData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // SECTION E: Molecular Techniques Handlers
  // ==========================================

  const handleOpenExtractionModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setExtractionData({
      method: "",
      otherMethod: "",
      nucleicAcidType: "",
      extractionDate: new Date().toISOString().split("T")[0],
      extractionTime: "",
      performedBy: "",
      kitUsed: "",
      kitLot: "",
      sampleInputVolume: "",
      elutionVolume: "",
      concentration: "",
      concentrationUnit: "ng/uL",
      purity260280: "",
      purity260230: "",
      qualityAssessment: "",
      storageLocation: "",
      notes: "",
    });
    setExtractionModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveExtractionData = useCallback(() => {
    if (!hasRealPageId) {
      setExtractionModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      extractionCompleted: true,
      extractionMethod: extractionData.method,
      extractionNucleicAcidType: extractionData.nucleicAcidType,
      extractionDate: extractionData.extractionDate,
      extractionTime: extractionData.extractionTime,
      extractionPerformedBy: extractionData.performedBy,
      extractionKitUsed: extractionData.kitUsed,
      extractionKitLot: extractionData.kitLot,
      extractionSampleInputVolume: extractionData.sampleInputVolume,
      extractionElutionVolume: extractionData.elutionVolume,
      extractionConcentration: extractionData.concentration,
      extractionConcentrationUnit: extractionData.concentrationUnit,
      extractionPurity260280: extractionData.purity260280,
      extractionPurity260230: extractionData.purity260230,
      extractionQualityAssessment: extractionData.qualityAssessment,
      extractionStorageLocation: extractionData.storageLocation,
      extractionNotes: extractionData.notes,
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
                  id: "notebook.bacteriology.assay.extractionSaved",
                  defaultMessage: "Extraction data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setExtractionModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save extraction data.");
          }
        }
      },
    );
  }, [
    extractionData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  const handleOpenPCRModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setPcrData({
      assayType: "",
      otherAssayType: "",
      target: "",
      otherTarget: "",
      runId: "",
      testDate: new Date().toISOString().split("T")[0],
      performedBy: "",
      instrument: "",
      kitUsed: "",
      kitLot: "",
      primerSet: "",
      controlResults: {
        positive: "",
        negative: "",
        internal: "",
      },
      ctValue: "",
      result: "",
      rawDataFile: null,
      notes: "",
    });
    setPcrModalOpen(true);
  }, [selectedIds, intl]);

  const handleSavePCRData = useCallback(() => {
    if (!hasRealPageId) {
      setPcrModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      molecularCompleted: true,
      pcrCompleted: true,
      pcrAssayType: pcrData.assayType,
      pcrTarget:
        pcrData.target === "OTHER" ? pcrData.otherTarget : pcrData.target,
      pcrRunId: pcrData.runId,
      pcrTestDate: pcrData.testDate,
      pcrPerformedBy: pcrData.performedBy,
      pcrInstrument: pcrData.instrument,
      pcrKitUsed: pcrData.kitUsed,
      pcrKitLot: pcrData.kitLot,
      pcrPrimerSet: pcrData.primerSet,
      pcrControlResults: pcrData.controlResults,
      pcrCtValue: pcrData.ctValue,
      pcrResult: pcrData.result,
      pcrNotes: pcrData.notes,
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
                  id: "notebook.bacteriology.assay.pcrSaved",
                  defaultMessage: "PCR data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setPcrModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save PCR data.");
          }
        }
      },
    );
  }, [
    pcrData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  const handleOpenWGSModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setWgsData({
      platform: "",
      otherPlatform: "",
      runId: "",
      sequencingDate: new Date().toISOString().split("T")[0],
      performedBy: "",
      libraryPrepKit: "",
      libraryPrepKitLot: "",
      indexUsed: "",
      loadingConcentration: "",
      analysisType: [],
      coverage: "",
      q30Score: "",
      n50: "",
      contigCount: "",
      genomeSize: "",
      speciesIdentified: "",
      mlstType: "",
      amrGenes: "",
      virulenceFactors: "",
      rawDataLocation: "",
      analysisNotes: "",
    });
    setWgsModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveWGSData = useCallback(() => {
    if (!hasRealPageId) {
      setWgsModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      molecularCompleted: true,
      wgsCompleted: true,
      wgsPlatform: wgsData.platform,
      wgsRunId: wgsData.runId,
      wgsSequencingDate: wgsData.sequencingDate,
      wgsPerformedBy: wgsData.performedBy,
      wgsLibraryPrepKit: wgsData.libraryPrepKit,
      wgsLibraryPrepKitLot: wgsData.libraryPrepKitLot,
      wgsIndexUsed: wgsData.indexUsed,
      wgsLoadingConcentration: wgsData.loadingConcentration,
      wgsAnalysisType: wgsData.analysisType,
      wgsCoverage: wgsData.coverage,
      wgsQ30Score: wgsData.q30Score,
      wgsN50: wgsData.n50,
      wgsContigCount: wgsData.contigCount,
      wgsGenomeSize: wgsData.genomeSize,
      organismIdentified: wgsData.speciesIdentified,
      wgsMlstType: wgsData.mlstType,
      wgsAmrGenes: wgsData.amrGenes,
      wgsVirulenceFactors: wgsData.virulenceFactors,
      wgsRawDataLocation: wgsData.rawDataLocation,
      wgsAnalysisNotes: wgsData.analysisNotes,
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
                  id: "notebook.bacteriology.assay.wgsSaved",
                  defaultMessage: "WGS data saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setWgsModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save WGS data.");
          }
        }
      },
    );
  }, [
    wgsData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // Status Change Handler
  // ==========================================

  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.bacteriology.assay.pageNotInitialized",
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

  // ==========================================
  // Bulk Mark Complete Handler
  // ==========================================

  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.noSamplesSelected",
          defaultMessage: "Please select samples to mark as completed.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.bacteriology.assay.samplesMarkedCompleted",
                defaultMessage:
                  "{count} sample(s) marked as completed successfully.",
              },
              { count: selectedIds.length },
            ),
          );
          setSelectedIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to mark samples as completed. Please try again.");
        }
      },
    );
  }, [
    selectedIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // Render Helper Functions
  // ==========================================

  // Render test status column
  const renderTestStatus = (sample) => {
    if (!sample) return null;
    const tests = [];

    if (sample.microscopyCompleted) {
      tests.push(
        <Tag
          key="microscopy"
          type="green"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          Microscopy
        </Tag>,
      );
    }
    if (sample.cultureCompleted) {
      tests.push(
        <Tag
          key="culture"
          type="green"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          Culture
        </Tag>,
      );
    }
    if (sample.biochemCompleted) {
      tests.push(
        <Tag
          key="biochem"
          type="green"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          Biochem
        </Tag>,
      );
    }
    if (sample.dstCompleted) {
      tests.push(
        <Tag key="dst" type="blue" size="sm" style={{ marginRight: "4px" }}>
          DST
        </Tag>,
      );
    }
    if (sample.automatedIdCompleted) {
      tests.push(
        <Tag
          key="automated"
          type="purple"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          Auto-ID
        </Tag>,
      );
    }
    if (sample.molecularCompleted) {
      tests.push(
        <Tag
          key="molecular"
          type="teal"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          Molecular
        </Tag>,
      );
    }

    if (tests.length === 0) {
      return (
        <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
          <FormattedMessage
            id="notebook.bacteriology.assay.noTests"
            defaultMessage="No tests completed"
          />
        </span>
      );
    }

    return (
      <div style={{ display: "flex", flexWrap: "wrap", gap: "2px" }}>
        {tests}
      </div>
    );
  };

  // Render identification column
  const renderIdentification = (sample) => {
    if (!sample) return null;
    if (sample.organismIdentified) {
      return (
        <div style={{ fontSize: "12px" }}>
          <strong>{sample.organismIdentified}</strong>
          {sample.presumptiveId &&
            sample.presumptiveId !== sample.organismIdentified && (
              <div style={{ color: "#525252", fontSize: "11px" }}>
                Presumptive: {sample.presumptiveId}
              </div>
            )}
        </div>
      );
    }
    if (sample.presumptiveId) {
      return (
        <div style={{ fontSize: "12px" }}>
          <span style={{ color: "#525252" }}>Presumptive: </span>
          {sample.presumptiveId}
        </div>
      );
    }
    if (sample.gramStainResult) {
      return (
        <div style={{ fontSize: "12px", color: "#525252" }}>
          Gram: {sample.gramStainResult}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.bacteriology.assay.notIdentified"
          defaultMessage="Not identified"
        />
      </span>
    );
  };

  // ==========================================
  // RENDER - First Half (Sections A-C)
  // ==========================================

  return (
    <div className="bacteriology-assay-test-execution-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.assay.title"
            defaultMessage="Assay/Test Execution"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.assay.description"
            defaultMessage="Comprehensive test execution including microscopy, culture, biochemical tests, drug susceptibility testing, automated identification, and molecular techniques."
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
                  id="notebook.bacteriology.assay.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.assay.microscopyDone"
                  defaultMessage="Microscopy"
                />
              </span>
              <span className="progress-value">{stats.microscopyDone}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.assay.cultureDone"
                  defaultMessage="Culture"
                />
              </span>
              <span className="progress-value">{stats.cultureDone}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.assay.dstDone"
                  defaultMessage="DST"
                />
              </span>
              <span className="progress-value">{stats.dstDone}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.assay.identified"
                  defaultMessage="Identified"
                />
              </span>
              <span className="progress-value">{stats.identified}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.assay.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{stats.completed}</span>
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

      {/* Main Tabs for Test Sections */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Test execution sections">
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.assay.tab.microscopy"
              defaultMessage="A. Microscopy"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.assay.tab.isolation"
              defaultMessage="B. Isolation & ID"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.assay.tab.dst"
              defaultMessage="C. DST"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.assay.tab.automated"
              defaultMessage="D. Automated ID"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bacteriology.assay.tab.molecular"
              defaultMessage="E. Molecular"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* ==========================================
              TAB A: Primary Culture & Microscopy
              ========================================== */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.sectionA.title"
                  defaultMessage="Primary Culture & Microscopy"
                />
              </h5>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Microscope}
                  onClick={handleOpenMicroscopyModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordMicroscopy"
                    defaultMessage="Record Microscopy ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={CheckmarkFilled}
                  onClick={handleBulkMarkCompleted}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.markCompleted"
                    defaultMessage="Mark Completed ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadPageSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-assay-microscopy"
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
                      key: "testStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.testStatus",
                        defaultMessage: "Tests Completed",
                      }),
                      render: renderTestStatus,
                    },
                    {
                      key: "identification",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.identification",
                        defaultMessage: "Identification",
                      }),
                      render: renderIdentification,
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* ==========================================
              TAB B: Isolation & Identification
              ========================================== */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.sectionB.title"
                  defaultMessage="Isolation & Identification"
                />
              </h5>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Microscope}
                  onClick={handleOpenCultureModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordCulture"
                    defaultMessage="Record Culture ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="secondary"
                  size="sm"
                  renderIcon={View}
                  onClick={handleOpenColonyModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.readColonies"
                    defaultMessage="Read Colonies ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Chemistry}
                  onClick={handleOpenBiochemModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.biochemTests"
                    defaultMessage="Biochemical Tests ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={CheckmarkFilled}
                  onClick={handleBulkMarkCompleted}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.markCompleted"
                    defaultMessage="Mark Completed ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadPageSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-assay-isolation"
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
                      key: "testStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.testStatus",
                        defaultMessage: "Tests Completed",
                      }),
                      render: renderTestStatus,
                    },
                    {
                      key: "identification",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.identification",
                        defaultMessage: "Identification",
                      }),
                      render: renderIdentification,
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* ==========================================
              TAB C: Drug Susceptibility Testing
              ========================================== */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.sectionC.title"
                  defaultMessage="Drug Susceptibility Testing (DST)"
                />
              </h5>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={ChartLine}
                  onClick={handleOpenDSTModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordDST"
                    defaultMessage="Record DST ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={CheckmarkFilled}
                  onClick={handleBulkMarkCompleted}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.markCompleted"
                    defaultMessage="Mark Completed ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadPageSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-assay-dst"
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
                      key: "testStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.testStatus",
                        defaultMessage: "Tests Completed",
                      }),
                      render: renderTestStatus,
                    },
                    {
                      key: "identification",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.identification",
                        defaultMessage: "Identification",
                      }),
                      render: renderIdentification,
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* ==========================================
              TAB D: Automated Identification and AST
              ========================================== */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.sectionD.title"
                  defaultMessage="Automated Identification and AST"
                />
              </h5>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Activity}
                  onClick={handleOpenAutomatedIdModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordAutomatedId"
                    defaultMessage="Automated ID ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={CheckmarkFilled}
                  onClick={handleBulkMarkCompleted}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.markCompleted"
                    defaultMessage="Mark Completed ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadPageSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-assay-automated"
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
                      key: "testStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.testStatus",
                        defaultMessage: "Tests Completed",
                      }),
                      render: renderTestStatus,
                    },
                    {
                      key: "identification",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.identification",
                        defaultMessage: "Identification",
                      }),
                      render: renderIdentification,
                    },
                  ]}
                />
              </div>
            </div>
          </TabPanel>

          {/* ==========================================
              TAB E: Molecular Techniques
              ========================================== */}
          <TabPanel>
            <div className="tab-content">
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.sectionE.title"
                  defaultMessage="Molecular Techniques"
                />
              </h5>

              {/* Action Buttons */}
              <div className="page-actions-bar">
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={DataVis_2}
                  onClick={handleOpenExtractionModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordExtraction"
                    defaultMessage="DNA/RNA Extraction ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="secondary"
                  size="sm"
                  renderIcon={ListChecked}
                  onClick={handleOpenPCRModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordPCR"
                    defaultMessage="PCR Assay ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={DataBase}
                  onClick={handleOpenWGSModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.recordWGS"
                    defaultMessage="WGS ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={CheckmarkFilled}
                  onClick={handleBulkMarkCompleted}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.markCompleted"
                    defaultMessage="Mark Completed ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={loadPageSamples}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </div>

              {/* Sample Grid */}
              <div className="sample-grid-container">
                <SampleGrid
                  gridId="bacteriology-assay-molecular"
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
                      key: "testStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.testStatus",
                        defaultMessage: "Tests Completed",
                      }),
                      render: renderTestStatus,
                    },
                    {
                      key: "identification",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.identification",
                        defaultMessage: "Identification",
                      }),
                      render: renderIdentification,
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
              id="notebook.bacteriology.assay.empty"
              defaultMessage="No samples available for test execution. Please complete the processing step first."
            />
          </p>
        </div>
      )}

      {/* ==========================================
          MODALS - Section A: Microscopy
          ========================================== */}
      <Modal
        open={microscopyModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.microscopyTitle",
          defaultMessage: "Record Microscopy Examination",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setMicroscopyModalOpen(false)}
        onRequestSubmit={handleSaveMicroscopyData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.microscopyDescription"
              defaultMessage="Record microscopy examination results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* Stain Type */}
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="stain-type"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.stainType",
                  defaultMessage: "Stain Type",
                })}
                label={intl.formatMessage({
                  id: "notebook.bacteriology.assay.selectStain",
                  defaultMessage: "Select stain type",
                })}
                items={MICROSCOPY_STAIN_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={MICROSCOPY_STAIN_TYPES.find(
                  (s) => s.id === microscopyData.stainType,
                )}
                onChange={({ selectedItem }) =>
                  setMicroscopyData({
                    ...microscopyData,
                    stainType: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            {microscopyData.stainType === "OTHER" && (
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="other-stain-type"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.otherStainType",
                    defaultMessage: "Specify Other Stain",
                  })}
                  value={microscopyData.otherStainType}
                  onChange={(e) =>
                    setMicroscopyData({
                      ...microscopyData,
                      otherStainType: e.target.value,
                    })
                  }
                />
              </Column>
            )}

            {/* Gram Stain Result */}
            {(microscopyData.stainType === "GRAM" ||
              !microscopyData.stainType) && (
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="gram-stain-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.gramStainResult",
                    defaultMessage: "Gram Stain Result",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.bacteriology.assay.selectResult",
                    defaultMessage: "Select result",
                  })}
                  items={GRAM_STAIN_RESULTS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={GRAM_STAIN_RESULTS.find(
                    (r) => r.id === microscopyData.gramStainResult,
                  )}
                  onChange={({ selectedItem }) =>
                    setMicroscopyData({
                      ...microscopyData,
                      gramStainResult: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
            )}

            {microscopyData.gramStainResult === "OTHER" && (
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="other-gram-result"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.otherGramResult",
                    defaultMessage: "Specify Other Finding",
                  })}
                  value={microscopyData.otherGramResult}
                  onChange={(e) =>
                    setMicroscopyData({
                      ...microscopyData,
                      otherGramResult: e.target.value,
                    })
                  }
                />
              </Column>
            )}
          </Grid>

          {/* Date/Time and Performer */}
          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={microscopyData.microscopyDate}
                onChange={([date]) =>
                  setMicroscopyData({
                    ...microscopyData,
                    microscopyDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="microscopy-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.microscopyDate",
                    defaultMessage: "Examination Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={microscopyData.performedBy}
                onChange={(e) =>
                  setMicroscopyData({
                    ...microscopyData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="preliminary-id"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.preliminaryId",
                  defaultMessage: "Preliminary Identification",
                })}
                value={microscopyData.preliminaryId}
                onChange={(e) =>
                  setMicroscopyData({
                    ...microscopyData,
                    preliminaryId: e.target.value,
                  })
                }
                placeholder="e.g., Gram-positive cocci, likely Staphylococcus"
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="microscopy-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={microscopyData.notes}
                onChange={(e) =>
                  setMicroscopyData({
                    ...microscopyData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section B: Culture
          ========================================== */}
      <Modal
        open={cultureModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.cultureTitle",
          defaultMessage: "Record Culture Inoculation",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCultureModalOpen(false)}
        onRequestSubmit={handleSaveCultureData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.cultureDescription"
              defaultMessage="Record culture inoculation for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <MultiSelect
                id="media-used"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.mediaUsed",
                  defaultMessage: "Media Used",
                })}
                label={intl.formatMessage({
                  id: "notebook.bacteriology.assay.selectMedia",
                  defaultMessage: "Select media types",
                })}
                items={ISOLATION_MEDIA_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItems={ISOLATION_MEDIA_TYPES.filter((m) =>
                  cultureData.mediaUsed.includes(m.id),
                )}
                onChange={({ selectedItems }) =>
                  setCultureData({
                    ...cultureData,
                    mediaUsed: selectedItems.map((item) => item.id),
                  })
                }
              />
            </Column>

            {cultureData.mediaUsed.includes("OTHER") && (
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="other-media"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.otherMedia",
                    defaultMessage: "Specify Other Media",
                  })}
                  value={cultureData.otherMedia}
                  onChange={(e) =>
                    setCultureData({
                      ...cultureData,
                      otherMedia: e.target.value,
                    })
                  }
                />
              </Column>
            )}

            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={cultureData.inoculationDate}
                onChange={([date]) =>
                  setCultureData({
                    ...cultureData,
                    inoculationDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="inoculation-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.inoculationDate",
                    defaultMessage: "Inoculation Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="inoculation-time"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.inoculationTime",
                  defaultMessage: "Inoculation Time",
                })}
                value={cultureData.inoculationTime}
                onChange={(e) =>
                  setCultureData({
                    ...cultureData,
                    inoculationTime: e.target.value,
                  })
                }
                placeholder="HH:MM"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="incubation-temp"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.incubationTemp",
                  defaultMessage: "Incubation Temperature",
                })}
                value={cultureData.incubationTemp}
                onChange={(e) =>
                  setCultureData({
                    ...cultureData,
                    incubationTemp: e.target.value,
                  })
                }
                placeholder="e.g., 37°C"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="incubation-duration"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.incubationDuration",
                  defaultMessage: "Incubation Duration",
                })}
                value={cultureData.incubationDuration}
                onChange={(e) =>
                  setCultureData({
                    ...cultureData,
                    incubationDuration: e.target.value,
                  })
                }
                placeholder="e.g., 18-24 hours"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="inoculated-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.inoculatedBy",
                  defaultMessage: "Inoculated By",
                })}
                value={cultureData.inoculatedBy}
                onChange={(e) =>
                  setCultureData({
                    ...cultureData,
                    inoculatedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="culture-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={cultureData.notes}
                onChange={(e) =>
                  setCultureData({
                    ...cultureData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section B: Colony Reading
          ========================================== */}
      <Modal
        open={colonyModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.colonyTitle",
          defaultMessage: "Record Colony Reading",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setColonyModalOpen(false)}
        onRequestSubmit={handleSaveColonyData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.colonyDescription"
              defaultMessage="Record colony reading for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="colony-media-type"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.mediaType",
                  defaultMessage: "Media Type",
                })}
                label="Select media"
                items={ISOLATION_MEDIA_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={ISOLATION_MEDIA_TYPES.find(
                  (m) => m.id === colonyData.mediaType,
                )}
                onChange={({ selectedItem }) =>
                  setColonyData({
                    ...colonyData,
                    mediaType: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="colony-count"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.colonyCount",
                  defaultMessage: "Colony Count",
                })}
                value={colonyData.colonyCount}
                onChange={(e) =>
                  setColonyData({
                    ...colonyData,
                    colonyCount: e.target.value,
                  })
                }
                placeholder="e.g., >10^5 CFU/mL, TNTC, 50 CFU"
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <MultiSelect
                id="colony-morphology"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.colonyMorphology",
                  defaultMessage: "Colony Morphology",
                })}
                label="Select characteristics"
                items={COLONY_MORPHOLOGY}
                itemToString={(item) => (item ? item.text : "")}
                selectedItems={COLONY_MORPHOLOGY.filter((m) =>
                  colonyData.colonyMorphology.includes(m.id),
                )}
                onChange={({ selectedItems }) =>
                  setColonyData({
                    ...colonyData,
                    colonyMorphology: selectedItems.map((item) => item.id),
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="isolate-id"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.isolateId",
                  defaultMessage: "Isolate ID",
                })}
                value={colonyData.isolateId}
                onChange={(e) =>
                  setColonyData({
                    ...colonyData,
                    isolateId: e.target.value,
                  })
                }
                placeholder="e.g., ISO-001"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="colony-read-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.readBy",
                  defaultMessage: "Read By",
                })}
                value={colonyData.readBy}
                onChange={(e) =>
                  setColonyData({
                    ...colonyData,
                    readBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="colony-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={colonyData.notes}
                onChange={(e) =>
                  setColonyData({
                    ...colonyData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section B: Biochemical Tests
          ========================================== */}
      <Modal
        open={biochemModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.biochemTitle",
          defaultMessage: "Record Biochemical Tests",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBiochemModalOpen(false)}
        onRequestSubmit={handleSaveBiochemData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.biochemDescription"
              defaultMessage="Record biochemical test results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Quick Entry for Common Tests */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.commonTests"
                defaultMessage="Common Tests"
              />
            </h6>
            <Grid fullWidth>
              {BIOCHEMICAL_TESTS.slice(0, 12).map((test) => (
                <Column key={test.id} lg={4} md={4} sm={4}>
                  <Dropdown
                    id={`biochem-${test.id}`}
                    titleText={test.text}
                    label="Select"
                    items={BIOCHEMICAL_RESULTS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={BIOCHEMICAL_RESULTS.find(
                      (r) => r.id === biochemData.testResults[test.id],
                    )}
                    onChange={({ selectedItem }) =>
                      setBiochemData({
                        ...biochemData,
                        testResults: {
                          ...biochemData.testResults,
                          [test.id]: selectedItem?.id || "",
                        },
                      })
                    }
                    size="sm"
                  />
                </Column>
              ))}
            </Grid>
          </div>

          {/* TSI/KIA Specific */}
          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="tsi-result"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.tsiResult",
                  defaultMessage: "TSI/KIA Result",
                })}
                label="Select result"
                items={TSI_KIA_RESULTS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={TSI_KIA_RESULTS.find(
                  (r) => r.id === biochemData.tsiResult,
                )}
                onChange={({ selectedItem }) =>
                  setBiochemData({
                    ...biochemData,
                    tsiResult: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="presumptive-id"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.presumptiveId",
                  defaultMessage: "Presumptive Identification",
                })}
                value={biochemData.presumptiveId}
                onChange={(e) =>
                  setBiochemData({
                    ...biochemData,
                    presumptiveId: e.target.value,
                  })
                }
                placeholder="e.g., E. coli, S. aureus"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={biochemData.testDate}
                onChange={([date]) =>
                  setBiochemData({
                    ...biochemData,
                    testDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="biochem-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.testDate",
                    defaultMessage: "Test Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="biochem-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={biochemData.performedBy}
                onChange={(e) =>
                  setBiochemData({
                    ...biochemData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="biochem-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={biochemData.notes}
                onChange={(e) =>
                  setBiochemData({
                    ...biochemData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section C: DST
          ========================================== */}
      <Modal
        open={dstModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.dstTitle",
          defaultMessage: "Record Drug Susceptibility Testing",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setDstModalOpen(false)}
        onRequestSubmit={handleSaveDSTData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.dstDescription"
              defaultMessage="Record DST results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* DST Method Selection */}
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="dst-method"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.dstMethod",
                  defaultMessage: "DST Method",
                })}
                label="Select method"
                items={DST_METHODS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={DST_METHODS.find((m) => m.id === dstData.method)}
                onChange={({ selectedItem }) =>
                  setDstData({
                    ...dstData,
                    method: selectedItem?.id || "",
                  })
                }
              />
              {dstData.method && (
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    marginTop: "0.25rem",
                  }}
                >
                  {
                    DST_METHODS.find((m) => m.id === dstData.method)
                      ?.description
                  }
                </p>
              )}
            </Column>

            {/* Interpretation Guidelines - CLSI/EUCAST */}
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="dst-guidelines"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.guidelinesUsed",
                  defaultMessage: "Interpretation Guidelines",
                })}
                label="Select guidelines"
                items={INTERPRETATION_GUIDELINES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={INTERPRETATION_GUIDELINES.find(
                  (g) => g.id === dstData.guidelinesUsed,
                )}
                onChange={({ selectedItem }) =>
                  setDstData({
                    ...dstData,
                    guidelinesUsed: selectedItem?.id || "",
                  })
                }
              />
            </Column>
          </Grid>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
            {/* Antibiotic Panel Selection */}
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="dst-panel"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.antibioticPanel",
                  defaultMessage: "Antibiotic Panel",
                })}
                label="Select panel"
                items={ANTIBIOTIC_PANELS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={ANTIBIOTIC_PANELS.find(
                  (p) => p.id === dstData.antibioticPanel,
                )}
                onChange={({ selectedItem }) =>
                  setDstData({
                    ...dstData,
                    antibioticPanel: selectedItem?.id || "",
                  })
                }
              />
              {dstData.antibioticPanel && (
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    marginTop: "0.25rem",
                  }}
                >
                  {
                    ANTIBIOTIC_PANELS.find(
                      (p) => p.id === dstData.antibioticPanel,
                    )?.description
                  }
                </p>
              )}
            </Column>
          </Grid>

          {/* Antibiotic Results Section - shown when method and panel are selected */}
          {dstData.method && dstData.antibioticPanel && (
            <div
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "0.5rem",
                }}
              >
                <h6>
                  <FormattedMessage
                    id="notebook.bacteriology.assay.antibioticResults"
                    defaultMessage="Antibiotic Results"
                  />
                  <span
                    style={{
                      fontSize: "0.75rem",
                      color: "#6f6f6f",
                      marginLeft: "0.5rem",
                    }}
                  >
                    (
                    {dstData.method === "DISC_DIFFUSION" &&
                      "Measure zone of inhibition - Interpret as S/I/R per guidelines"}
                    {dstData.method === "BROTH_MICRODILUTION" &&
                      "Determine MIC - Quantitative result"}
                    {dstData.method === "AST_STRIP" &&
                      "E-test gradient - MIC determination"}
                    {dstData.method === "AUTOMATED_AST" &&
                      "Automated AST system results"}
                    )
                  </span>
                </h6>
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Add}
                  onClick={handleAddAntibioticResult}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.addAntibiotic"
                    defaultMessage="Add Antibiotic"
                  />
                </Button>
              </div>

              {/* Column Headers - Method Specific */}
              {antibioticResults.length > 0 && (
                <Grid fullWidth style={{ marginBottom: "0.25rem" }}>
                  <Column lg={3} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Panel
                    </span>
                  </Column>
                  <Column lg={4} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Antibiotic
                    </span>
                  </Column>
                  <Column lg={3} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      {dstData.method === "DISC_DIFFUSION" && "Zone (mm)"}
                      {dstData.method === "BROTH_MICRODILUTION" &&
                        "MIC (µg/mL)"}
                      {dstData.method === "AST_STRIP" && "MIC (µg/mL)"}
                      {dstData.method === "AUTOMATED_AST" && "Result"}
                    </span>
                  </Column>
                  <Column lg={4} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Interpretation (S/I/R)
                    </span>
                  </Column>
                  <Column lg={2} md={1} sm={1}></Column>
                </Grid>
              )}

              {/* Antibiotic Result Rows */}
              {antibioticResults.map((result, index) => (
                <Grid
                  fullWidth
                  key={index}
                  style={{ marginBottom: "0.5rem", alignItems: "flex-end" }}
                >
                  {/* Panel Type */}
                  <Column lg={3} md={2} sm={1}>
                    <Dropdown
                      id={`antibiotic-panel-${index}`}
                      label="Select"
                      items={[
                        { id: "1ST_LINE", text: "1st Line" },
                        { id: "2ND_LINE", text: "2nd Line" },
                      ]}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={
                        result.panelType === "1ST_LINE"
                          ? { id: "1ST_LINE", text: "1st Line" }
                          : { id: "2ND_LINE", text: "2nd Line" }
                      }
                      onChange={({ selectedItem }) =>
                        handleUpdateAntibioticResult(
                          index,
                          "panelType",
                          selectedItem?.id || "1ST_LINE",
                        )
                      }
                      size="sm"
                    />
                  </Column>

                  {/* Antibiotic Name */}
                  <Column lg={4} md={2} sm={1}>
                    <TextInput
                      id={`antibiotic-name-${index}`}
                      value={result.antibiotic}
                      onChange={(e) =>
                        handleUpdateAntibioticResult(
                          index,
                          "antibiotic",
                          e.target.value,
                        )
                      }
                      placeholder="e.g., Ampicillin"
                      size="sm"
                    />
                  </Column>

                  {/* Method-specific result field */}
                  <Column lg={3} md={2} sm={1}>
                    {/* Disc Diffusion: Zone of inhibition */}
                    {dstData.method === "DISC_DIFFUSION" && (
                      <TextInput
                        id={`zone-diameter-${index}`}
                        value={result.zoneDiameter || ""}
                        onChange={(e) =>
                          handleUpdateAntibioticResult(
                            index,
                            "zoneDiameter",
                            e.target.value,
                          )
                        }
                        placeholder="Zone (mm)"
                        size="sm"
                      />
                    )}
                    {/* Broth Microdilution: MIC - Quantitative */}
                    {dstData.method === "BROTH_MICRODILUTION" && (
                      <TextInput
                        id={`mic-value-${index}`}
                        value={result.mic || ""}
                        onChange={(e) =>
                          handleUpdateAntibioticResult(
                            index,
                            "mic",
                            e.target.value,
                          )
                        }
                        placeholder="MIC (µg/mL)"
                        size="sm"
                      />
                    )}
                    {/* AST Strip (E-test): MIC - Gradient */}
                    {dstData.method === "AST_STRIP" && (
                      <TextInput
                        id={`etest-mic-${index}`}
                        value={result.mic || ""}
                        onChange={(e) =>
                          handleUpdateAntibioticResult(
                            index,
                            "mic",
                            e.target.value,
                          )
                        }
                        placeholder="MIC (µg/mL)"
                        size="sm"
                      />
                    )}
                    {/* Automated AST: System result */}
                    {dstData.method === "AUTOMATED_AST" && (
                      <TextInput
                        id={`automated-result-${index}`}
                        value={result.automatedResult || ""}
                        onChange={(e) =>
                          handleUpdateAntibioticResult(
                            index,
                            "automatedResult",
                            e.target.value,
                          )
                        }
                        placeholder="System result"
                        size="sm"
                      />
                    )}
                  </Column>

                  {/* Interpretation - S, I, R per CLSI/EUCAST */}
                  <Column lg={4} md={2} sm={1}>
                    <Dropdown
                      id={`interpretation-${index}`}
                      label="Select"
                      items={SUSCEPTIBILITY_INTERPRETATION}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={SUSCEPTIBILITY_INTERPRETATION.find(
                        (i) => i.id === result.interpretation,
                      )}
                      onChange={({ selectedItem }) =>
                        handleUpdateAntibioticResult(
                          index,
                          "interpretation",
                          selectedItem?.id || "",
                        )
                      }
                      size="sm"
                    />
                  </Column>

                  {/* Remove Button */}
                  <Column lg={2} md={1} sm={1}>
                    <Button
                      kind="ghost"
                      size="sm"
                      hasIconOnly
                      renderIcon={TrashCan}
                      iconDescription="Remove"
                      onClick={() => handleRemoveAntibioticResult(index)}
                    />
                  </Column>
                </Grid>
              ))}

              {antibioticResults.length === 0 && (
                <p
                  style={{
                    color: "#6f6f6f",
                    fontStyle: "italic",
                    textAlign: "center",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.noAntibioticsAdded"
                    defaultMessage="No antibiotics added. Click 'Add Antibiotic' to record results."
                  />
                </p>
              )}
            </div>
          )}

          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="dst-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={dstData.notes}
                onChange={(e) =>
                  setDstData({
                    ...dstData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section D: Automated Identification
          Machine-agnostic interface for entering results from any automated system
          ========================================== */}
      <Modal
        open={automatedIdModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.automatedIdTitle",
          defaultMessage: "Record Automated Identification",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setAutomatedIdModalOpen(false)}
        onRequestSubmit={handleSaveAutomatedIdData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.automatedIdDescription"
              defaultMessage="Record automated identification results for {count} selected sample(s). Enter results from any automated system."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={automatedIdData.testDate}
                onChange={([date]) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    testDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="automated-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.testDate",
                    defaultMessage: "Test Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="automated-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={automatedIdData.performedBy}
                onChange={(e) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="organism-identified"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.organismIdentified",
                  defaultMessage: "Organism Identified",
                })}
                value={automatedIdData.organismIdentified}
                onChange={(e) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    organismIdentified: e.target.value,
                  })
                }
                placeholder="e.g., Escherichia coli"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="confidence-level"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.confidenceLevel",
                  defaultMessage: "Confidence Level",
                })}
                label="Select"
                items={CONFIDENCE_LEVELS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={CONFIDENCE_LEVELS.find(
                  (l) => l.id === automatedIdData.confidenceLevel,
                )}
                onChange={({ selectedItem }) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    confidenceLevel: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="alternative-ids"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.alternativeIds",
                  defaultMessage: "Alternative Identifications (optional)",
                })}
                value={automatedIdData.alternativeIds}
                onChange={(e) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    alternativeIds: e.target.value,
                  })
                }
                placeholder="e.g., Shigella spp., Klebsiella oxytoca"
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="automated-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={automatedIdData.notes}
                onChange={(e) =>
                  setAutomatedIdData({
                    ...automatedIdData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section E: Nucleic Acid Extraction
          ========================================== */}
      <Modal
        open={extractionModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.extractionTitle",
          defaultMessage: "Record Nucleic Acid Extraction",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setExtractionModalOpen(false)}
        onRequestSubmit={handleSaveExtractionData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.extractionDescription"
              defaultMessage="Record nucleic acid extraction results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="extraction-method"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.extractionMethod",
                  defaultMessage: "Extraction Method",
                })}
                label="Select method"
                items={EXTRACTION_METHODS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={EXTRACTION_METHODS.find(
                  (m) => m.id === extractionData.method,
                )}
                onChange={({ selectedItem }) =>
                  setExtractionData({
                    ...extractionData,
                    method: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="nucleic-acid-type"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.nucleicAcidType",
                  defaultMessage: "Nucleic Acid Type",
                })}
                label="Select type"
                items={NUCLEIC_ACID_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={NUCLEIC_ACID_TYPES.find(
                  (t) => t.id === extractionData.nucleicAcidType,
                )}
                onChange={({ selectedItem }) =>
                  setExtractionData({
                    ...extractionData,
                    nucleicAcidType: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={extractionData.extractionDate}
                onChange={([date]) =>
                  setExtractionData({
                    ...extractionData,
                    extractionDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="extraction-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.extractionDate",
                    defaultMessage: "Extraction Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="extraction-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={extractionData.performedBy}
                onChange={(e) =>
                  setExtractionData({
                    ...extractionData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="extraction-kit"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.kitUsed",
                  defaultMessage: "Kit Used",
                })}
                value={extractionData.kitUsed}
                onChange={(e) =>
                  setExtractionData({
                    ...extractionData,
                    kitUsed: e.target.value,
                  })
                }
                placeholder="e.g., QIAamp DNA Mini Kit"
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="extraction-kit-lot"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.kitLot",
                  defaultMessage: "Kit Lot Number",
                })}
                value={extractionData.kitLot}
                onChange={(e) =>
                  setExtractionData({
                    ...extractionData,
                    kitLot: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          {/* Quantity and Quality */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.quantityQuality"
                defaultMessage="Quantity and Quality"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="extraction-concentration"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.concentration",
                    defaultMessage: "Concentration",
                  })}
                  value={extractionData.concentration}
                  onChange={(e) =>
                    setExtractionData({
                      ...extractionData,
                      concentration: e.target.value,
                    })
                  }
                  placeholder="e.g., 50"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Dropdown
                  id="concentration-unit"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.unit",
                    defaultMessage: "Unit",
                  })}
                  label="Select"
                  items={[
                    { id: "ng/uL", text: "ng/µL" },
                    { id: "ug/mL", text: "µg/mL" },
                    { id: "pg/uL", text: "pg/µL" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={[
                    { id: "ng/uL", text: "ng/µL" },
                    { id: "ug/mL", text: "µg/mL" },
                    { id: "pg/uL", text: "pg/µL" },
                  ].find((u) => u.id === extractionData.concentrationUnit)}
                  onChange={({ selectedItem }) =>
                    setExtractionData({
                      ...extractionData,
                      concentrationUnit: selectedItem?.id || "ng/uL",
                    })
                  }
                  size="sm"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="purity-260-280"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.purity260280",
                    defaultMessage: "A260/A280",
                  })}
                  value={extractionData.purity260280}
                  onChange={(e) =>
                    setExtractionData({
                      ...extractionData,
                      purity260280: e.target.value,
                    })
                  }
                  placeholder="e.g., 1.85"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="purity-260-230"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.purity260230",
                    defaultMessage: "A260/A230",
                  })}
                  value={extractionData.purity260230}
                  onChange={(e) =>
                    setExtractionData({
                      ...extractionData,
                      purity260230: e.target.value,
                    })
                  }
                  placeholder="e.g., 2.0"
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="quality-assessment"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.qualityAssessment",
                    defaultMessage: "Quality Assessment",
                  })}
                  label="Select"
                  items={[
                    { id: "EXCELLENT", text: "Excellent (suitable for WGS)" },
                    { id: "GOOD", text: "Good (suitable for PCR)" },
                    {
                      id: "ACCEPTABLE",
                      text: "Acceptable (may need re-extraction)",
                    },
                    { id: "POOR", text: "Poor (re-extraction required)" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={[
                    { id: "EXCELLENT", text: "Excellent (suitable for WGS)" },
                    { id: "GOOD", text: "Good (suitable for PCR)" },
                    {
                      id: "ACCEPTABLE",
                      text: "Acceptable (may need re-extraction)",
                    },
                    { id: "POOR", text: "Poor (re-extraction required)" },
                  ].find((q) => q.id === extractionData.qualityAssessment)}
                  onChange={({ selectedItem }) =>
                    setExtractionData({
                      ...extractionData,
                      qualityAssessment: selectedItem?.id || "",
                    })
                  }
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="storage-location"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.storageLocation",
                    defaultMessage: "Storage Location",
                  })}
                  value={extractionData.storageLocation}
                  onChange={(e) =>
                    setExtractionData({
                      ...extractionData,
                      storageLocation: e.target.value,
                    })
                  }
                  placeholder="e.g., Freezer A, Box 5, Position C3"
                />
              </Column>
            </Grid>
          </div>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="extraction-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={extractionData.notes}
                onChange={(e) =>
                  setExtractionData({
                    ...extractionData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section E: PCR Assay
          ========================================== */}
      <Modal
        open={pcrModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.pcrTitle",
          defaultMessage: "Record PCR Assay Results",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setPcrModalOpen(false)}
        onRequestSubmit={handleSavePCRData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.pcrDescription"
              defaultMessage="Record PCR assay results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="pcr-assay-type"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.pcrAssayType",
                  defaultMessage: "PCR Assay Type",
                })}
                label="Select type"
                items={PCR_ASSAY_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={PCR_ASSAY_TYPES.find(
                  (t) => t.id === pcrData.assayType,
                )}
                onChange={({ selectedItem }) =>
                  setPcrData({
                    ...pcrData,
                    assayType: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="pcr-target"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.pcrTarget",
                  defaultMessage: "Target Gene/Marker",
                })}
                label="Select target"
                items={PCR_TARGETS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={PCR_TARGETS.find((t) => t.id === pcrData.target)}
                onChange={({ selectedItem }) =>
                  setPcrData({
                    ...pcrData,
                    target: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            {pcrData.target === "OTHER" && (
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="pcr-other-target"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.otherTarget",
                    defaultMessage: "Specify Target",
                  })}
                  value={pcrData.otherTarget}
                  onChange={(e) =>
                    setPcrData({
                      ...pcrData,
                      otherTarget: e.target.value,
                    })
                  }
                />
              </Column>
            )}

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="pcr-run-id"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.runId",
                  defaultMessage: "Run ID",
                })}
                value={pcrData.runId}
                onChange={(e) =>
                  setPcrData({
                    ...pcrData,
                    runId: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={pcrData.testDate}
                onChange={([date]) =>
                  setPcrData({
                    ...pcrData,
                    testDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="pcr-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.testDate",
                    defaultMessage: "Test Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="pcr-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={pcrData.performedBy}
                onChange={(e) =>
                  setPcrData({
                    ...pcrData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="pcr-instrument"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.pcrInstrument",
                  defaultMessage: "Instrument",
                })}
                value={pcrData.instrument}
                onChange={(e) =>
                  setPcrData({
                    ...pcrData,
                    instrument: e.target.value,
                  })
                }
                placeholder="e.g., ABI 7500, Bio-Rad CFX96"
              />
            </Column>
          </Grid>

          {/* PCR Results */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.pcrResults"
                defaultMessage="PCR Results"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="pcr-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrResult",
                    defaultMessage: "Result",
                  })}
                  label="Select result"
                  items={PCR_RESULTS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={PCR_RESULTS.find(
                    (r) => r.id === pcrData.result,
                  )}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      result: selectedItem?.id || "",
                    })
                  }
                />
              </Column>

              {(pcrData.assayType === "REALTIME" ||
                pcrData.assayType === "DIGITAL") && (
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="pcr-ct-value"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.ctValue",
                      defaultMessage: "Ct Value",
                    })}
                    value={pcrData.ctValue}
                    onChange={(e) =>
                      setPcrData({
                        ...pcrData,
                        ctValue: e.target.value,
                      })
                    }
                    placeholder="e.g., 25.5"
                  />
                </Column>
              )}
            </Grid>

            {/* Control Results */}
            <h6 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.controlResults"
                defaultMessage="Control Results"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={5} md={4} sm={4}>
                <Dropdown
                  id="pcr-positive-control"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.positiveControl",
                    defaultMessage: "Positive Control",
                  })}
                  label="Select"
                  items={[
                    { id: "VALID", text: "Valid (Detected)" },
                    { id: "INVALID", text: "Invalid (Not Detected)" },
                    { id: "NA", text: "N/A" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={[
                    { id: "VALID", text: "Valid (Detected)" },
                    { id: "INVALID", text: "Invalid (Not Detected)" },
                    { id: "NA", text: "N/A" },
                  ].find((c) => c.id === pcrData.controlResults.positive)}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      controlResults: {
                        ...pcrData.controlResults,
                        positive: selectedItem?.id || "",
                      },
                    })
                  }
                  size="sm"
                />
              </Column>

              <Column lg={5} md={4} sm={4}>
                <Dropdown
                  id="pcr-negative-control"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.negativeControl",
                    defaultMessage: "Negative Control",
                  })}
                  label="Select"
                  items={[
                    { id: "VALID", text: "Valid (Not Detected)" },
                    { id: "INVALID", text: "Invalid (Detected)" },
                    { id: "NA", text: "N/A" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={[
                    { id: "VALID", text: "Valid (Not Detected)" },
                    { id: "INVALID", text: "Invalid (Detected)" },
                    { id: "NA", text: "N/A" },
                  ].find((c) => c.id === pcrData.controlResults.negative)}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      controlResults: {
                        ...pcrData.controlResults,
                        negative: selectedItem?.id || "",
                      },
                    })
                  }
                  size="sm"
                />
              </Column>

              <Column lg={6} md={4} sm={4}>
                <Dropdown
                  id="pcr-internal-control"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.internalControl",
                    defaultMessage: "Internal Control",
                  })}
                  label="Select"
                  items={[
                    { id: "VALID", text: "Valid" },
                    { id: "INVALID", text: "Invalid" },
                    { id: "NA", text: "N/A" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={[
                    { id: "VALID", text: "Valid" },
                    { id: "INVALID", text: "Invalid" },
                    { id: "NA", text: "N/A" },
                  ].find((c) => c.id === pcrData.controlResults.internal)}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      controlResults: {
                        ...pcrData.controlResults,
                        internal: selectedItem?.id || "",
                      },
                    })
                  }
                  size="sm"
                />
              </Column>
            </Grid>
          </div>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="pcr-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.notes",
                  defaultMessage: "Notes",
                })}
                value={pcrData.notes}
                onChange={(e) =>
                  setPcrData({
                    ...pcrData,
                    notes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section E: Whole Genome Sequencing
          ========================================== */}
      <Modal
        open={wgsModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.wgsTitle",
          defaultMessage: "Record Whole Genome Sequencing",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setWgsModalOpen(false)}
        onRequestSubmit={handleSaveWGSData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.wgsDescription"
              defaultMessage="Record whole genome sequencing results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="wgs-platform"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.wgsPlatform",
                  defaultMessage: "Sequencing Platform",
                })}
                label="Select platform"
                items={SEQUENCING_PLATFORMS}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={SEQUENCING_PLATFORMS.find(
                  (p) => p.id === wgsData.platform,
                )}
                onChange={({ selectedItem }) =>
                  setWgsData({
                    ...wgsData,
                    platform: selectedItem?.id || "",
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="wgs-run-id"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.runId",
                  defaultMessage: "Run ID",
                })}
                value={wgsData.runId}
                onChange={(e) =>
                  setWgsData({
                    ...wgsData,
                    runId: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={wgsData.sequencingDate}
                onChange={([date]) =>
                  setWgsData({
                    ...wgsData,
                    sequencingDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="wgs-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.sequencingDate",
                    defaultMessage: "Sequencing Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="wgs-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.performedBy",
                  defaultMessage: "Performed By",
                })}
                value={wgsData.performedBy}
                onChange={(e) =>
                  setWgsData({
                    ...wgsData,
                    performedBy: e.target.value,
                  })
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <MultiSelect
                id="wgs-analysis-type"
                titleText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.analysisType",
                  defaultMessage: "Analysis Type",
                })}
                label="Select analyses performed"
                items={WGS_ANALYSIS_TYPES}
                itemToString={(item) => (item ? item.text : "")}
                selectedItems={WGS_ANALYSIS_TYPES.filter((t) =>
                  wgsData.analysisType.includes(t.id),
                )}
                onChange={({ selectedItems }) =>
                  setWgsData({
                    ...wgsData,
                    analysisType: selectedItems.map((item) => item.id),
                  })
                }
              />
            </Column>
          </Grid>

          {/* Quality Metrics */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.qualityMetrics"
                defaultMessage="Quality Metrics"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="wgs-coverage"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.coverage",
                    defaultMessage: "Coverage (X)",
                  })}
                  value={wgsData.coverage}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      coverage: e.target.value,
                    })
                  }
                  placeholder="e.g., 100"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="wgs-q30"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.q30Score",
                    defaultMessage: "Q30 Score (%)",
                  })}
                  value={wgsData.q30Score}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      q30Score: e.target.value,
                    })
                  }
                  placeholder="e.g., 90"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="wgs-n50"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.n50",
                    defaultMessage: "N50 (bp)",
                  })}
                  value={wgsData.n50}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      n50: e.target.value,
                    })
                  }
                  placeholder="e.g., 150000"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="wgs-contigs"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.contigCount",
                    defaultMessage: "Contigs",
                  })}
                  value={wgsData.contigCount}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      contigCount: e.target.value,
                    })
                  }
                  placeholder="e.g., 45"
                />
              </Column>
            </Grid>
          </div>

          {/* Analysis Results */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#e0f0e0",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.analysisResults"
                defaultMessage="Analysis Results"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="wgs-species"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.speciesIdentified",
                    defaultMessage: "Species Identified",
                  })}
                  value={wgsData.speciesIdentified}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      speciesIdentified: e.target.value,
                    })
                  }
                  placeholder="e.g., Escherichia coli"
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="wgs-mlst"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.mlstType",
                    defaultMessage: "MLST Type",
                  })}
                  value={wgsData.mlstType}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      mlstType: e.target.value,
                    })
                  }
                  placeholder="e.g., ST131"
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="wgs-amr-genes"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.amrGenes",
                    defaultMessage: "AMR Genes Detected",
                  })}
                  value={wgsData.amrGenes}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      amrGenes: e.target.value,
                    })
                  }
                  placeholder="e.g., blaCTX-M-15, blaTEM-1, aac(6')-Ib-cr"
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="wgs-virulence"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.virulenceFactors",
                    defaultMessage: "Virulence Factors",
                  })}
                  value={wgsData.virulenceFactors}
                  onChange={(e) =>
                    setWgsData({
                      ...wgsData,
                      virulenceFactors: e.target.value,
                    })
                  }
                  placeholder="e.g., stx1, stx2, eae"
                />
              </Column>
            </Grid>
          </div>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="wgs-notes"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.analysisNotes",
                  defaultMessage: "Analysis Notes",
                })}
                value={wgsData.analysisNotes}
                onChange={(e) =>
                  setWgsData({
                    ...wgsData,
                    analysisNotes: e.target.value,
                  })
                }
                rows={2}
              />
            </Column>
          </Grid>
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyAssayTestExecutionPage;
