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
  Tooltip,
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

// Growth Results for Media Reactions
const GROWTH_RESULTS = [
  { id: "GROWTH", text: "Growth Observed" },
  { id: "NO_GROWTH", text: "No Growth" },
  { id: "CONTAMINATED", text: "Contaminated" },
  { id: "PENDING", text: "Pending/Incubating" },
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

// Biochemical QC Failure Reasons
const BIOCHEM_QC_FAILURE_REASONS = [
  { id: "REAGENT_EXPIRED", text: "Reagent Expired" },
  { id: "REAGENT_CONTAMINATED", text: "Reagent Contaminated" },
  { id: "CONTROL_FAILED", text: "Control Failed" },
  { id: "INCONSISTENT_RESULTS", text: "Inconsistent/Contradictory Results" },
  { id: "POOR_INOCULUM", text: "Poor Inoculum Quality" },
  { id: "WRONG_INCUBATION", text: "Incorrect Incubation Conditions" },
  { id: "EQUIPMENT_MALFUNCTION", text: "Equipment Malfunction" },
  { id: "CONTAMINATION", text: "Sample Contamination" },
  { id: "OTHER", text: "Other (specify in notes)" },
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

// PCR Primer Direction - Forward or Reverse
const PCR_PRIMER_DIRECTION = [
  { id: "FORWARD", text: "Forward" },
  { id: "REVERSE", text: "Reverse" },
];

// PCR Enzymes - DNA Polymerases and Master Mixes
const PCR_ENZYMES = [
  { id: "TAQ_POLYMERASE", text: "Taq DNA Polymerase" },
  { id: "PHUSION", text: "Phusion High-Fidelity DNA Polymerase" },
  { id: "Q5", text: "Q5 High-Fidelity DNA Polymerase" },
  { id: "PLATINUM_TAQ", text: "Platinum Taq DNA Polymerase" },
  { id: "GOTAQ", text: "GoTaq DNA Polymerase" },
  { id: "DREAMTAQ", text: "DreamTaq DNA Polymerase" },
  { id: "AMPLITAQ_GOLD", text: "AmpliTaq Gold DNA Polymerase" },
  { id: "KAPA_HIFI", text: "KAPA HiFi DNA Polymerase" },
  { id: "SYBR_GREEN_MM", text: "SYBR Green Master Mix" },
  { id: "TAQMAN_MM", text: "TaqMan Master Mix" },
  { id: "POWERUP_SYBR", text: "PowerUp SYBR Green Master Mix" },
  { id: "LUNA_MM", text: "Luna Universal qPCR Master Mix" },
  { id: "CUSTOM", text: "Custom Enzyme/Mix" },
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
// Molecular QC Constants
// ==========================================

// Reagent/Kit QC - Types of reagents that need QC verification
const MOLECULAR_REAGENT_TYPES = [
  { id: "EXTRACTION_KIT", text: "Extraction Kit" },
  { id: "PCR_MASTERMIX", text: "PCR Master Mix" },
  { id: "PRIMERS", text: "Primers/Probes" },
  { id: "DNTP_MIX", text: "dNTP Mix" },
  { id: "POLYMERASE", text: "DNA Polymerase" },
  { id: "REVERSE_TRANSCRIPTASE", text: "Reverse Transcriptase" },
  { id: "LIBRARY_PREP_KIT", text: "Library Prep Kit" },
  { id: "SEQUENCING_REAGENTS", text: "Sequencing Reagents" },
  { id: "OTHER", text: "Other (specify)" },
];

// Reagent QC Status
const REAGENT_QC_STATUS = [
  { id: "PASSED", text: "Passed" },
  { id: "FAILED", text: "Failed" },
  { id: "PENDING", text: "Pending" },
];

// Reagent QC Failure Reasons
const REAGENT_QC_FAILURE_REASONS = [
  { id: "EXPIRED", text: "Reagent Expired" },
  { id: "STORAGE_TEMP", text: "Improper Storage Temperature" },
  { id: "CONTROL_FAILED", text: "Control Failed" },
  { id: "CONTAMINATED", text: "Contamination Detected" },
  { id: "DEGRADED", text: "Reagent Degradation" },
  { id: "LOT_RECALL", text: "Lot Recall/Quality Issue" },
  { id: "OTHER", text: "Other (specify)" },
];

// Equipment Types for QC
const MOLECULAR_EQUIPMENT_TYPES = [
  { id: "THERMOCYCLER", text: "Thermocycler/PCR Machine" },
  { id: "REALTIME_PCR", text: "Real-Time PCR System" },
  { id: "SEQUENCER", text: "Sequencer" },
  { id: "CENTRIFUGE", text: "Centrifuge" },
  { id: "SPECTROPHOTOMETER", text: "Spectrophotometer/NanoDrop" },
  { id: "BIOANALYZER", text: "Bioanalyzer/TapeStation" },
  { id: "QUBIT", text: "Qubit Fluorometer" },
  { id: "PIPETTES", text: "Pipettes" },
  { id: "VORTEX", text: "Vortex Mixer" },
  { id: "HEAT_BLOCK", text: "Heat Block/Water Bath" },
  { id: "OTHER", text: "Other (specify)" },
];

// Equipment QC Status
const EQUIPMENT_QC_STATUS = [
  { id: "PASSED", text: "Calibrated/Verified" },
  { id: "FAILED", text: "Out of Calibration/Failed" },
  { id: "DUE", text: "Calibration Due" },
  { id: "MAINTENANCE", text: "Under Maintenance" },
];

// Equipment QC Failure Reasons
const EQUIPMENT_QC_FAILURE_REASONS = [
  { id: "CALIBRATION_EXPIRED", text: "Calibration Expired" },
  { id: "FAILED_VERIFICATION", text: "Failed Verification Check" },
  { id: "MALFUNCTION", text: "Equipment Malfunction" },
  { id: "MAINTENANCE_DUE", text: "Preventive Maintenance Overdue" },
  { id: "TEMPERATURE_ISSUE", text: "Temperature Out of Range" },
  { id: "OTHER", text: "Other (specify)" },
];

// Sample QC Criteria
const SAMPLE_QC_CRITERIA = [
  { id: "CONCENTRATION", text: "Concentration Check" },
  { id: "PURITY_260_280", text: "Purity (A260/A280)" },
  { id: "PURITY_260_230", text: "Purity (A260/A230)" },
  { id: "INTEGRITY", text: "Integrity/RIN Score" },
  { id: "CONTAMINATION", text: "Contamination Check" },
  { id: "VOLUME", text: "Sufficient Volume" },
];

// Sample QC Status
const SAMPLE_QC_STATUS = [
  { id: "PASSED", text: "QC Passed" },
  { id: "FAILED", text: "QC Failed" },
  { id: "CONDITIONAL", text: "Conditional Pass (Proceed with Caution)" },
  { id: "PENDING", text: "Pending" },
];

// Sample QC Failure Reasons
const SAMPLE_QC_FAILURE_REASONS = [
  { id: "LOW_CONCENTRATION", text: "Concentration Below Threshold" },
  { id: "LOW_PURITY", text: "Poor Purity (A260/A280 or A260/A230)" },
  { id: "DEGRADED", text: "Sample Degradation" },
  { id: "CONTAMINATED", text: "Contamination Detected" },
  { id: "INSUFFICIENT_VOLUME", text: "Insufficient Volume" },
  { id: "INHIBITORS", text: "PCR Inhibitors Present" },
  { id: "OTHER", text: "Other (specify)" },
];

// Overall Molecular QC Result
const MOLECULAR_QC_RESULTS = [
  { id: "PASS", text: "QC Passed - Proceed with Testing" },
  { id: "FAIL", text: "QC Failed - Do Not Proceed" },
  { id: "CONDITIONAL", text: "Conditional - Proceed with Caution" },
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

  // Enzymes from inventory
  const [enzymes, setEnzymes] = useState([]);
  const [loadingEnzymes, setLoadingEnzymes] = useState(false);

  // Antibiotics from inventory
  const [antibiotics, setAntibiotics] = useState([]);
  const [loadingAntibiotics, setLoadingAntibiotics] = useState(false);

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
    // QC fields
    qcResult: "", // PASS or FAIL
    qcFailureReason: "",
    qcNotes: "",
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
  // SECTION B.2: Multiple Media Reactions State
  // Supports multiple reactions per sample - each media can have its own culture results and DST
  // ==========================================

  const [mediaReactionsModalOpen, setMediaReactionsModalOpen] = useState(false);
  const [mediaReactions, setMediaReactions] = useState([]);
  // Each media reaction structure:
  // {
  //   id: UUID string,
  //   mediaType: string (from ISOLATION_MEDIA_TYPES),
  //   otherMediaName: string (if mediaType is OTHER),
  //   inoculationDate: string,
  //   inoculationTime: string,
  //   incubationCondition: string,
  //   incubationTemp: string,
  //   incubationDuration: string,
  //   inoculatedBy: string,
  //   colonyCount: string,
  //   colonyMorphology: array,
  //   hemolysis: string,
  //   lactoseFermentation: string,
  //   pigmentation: string,
  //   growthResult: string (GROWTH, NO_GROWTH, CONTAMINATED),
  //   organismIdentified: string,
  //   notes: string,
  //   dstCompleted: boolean,
  //   dstMethod: string,
  //   dstGuidelinesUsed: string,
  //   dstAntibioticPanel: string,
  //   dstAntibioticResults: array of {panelType, antibiotic, zoneDiameter, mic, interpretation},
  //   dstNotes: string
  // }

  // Currently editing media reaction (for the DST sub-modal)
  const [editingMediaReactionIndex, setEditingMediaReactionIndex] =
    useState(null);
  const [mediaReactionDstModalOpen, setMediaReactionDstModalOpen] =
    useState(false);

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
    // Structured template fields (between PCR Type and Target Gene)
    primerDirection: "",
    primerType: "",
    enzyme: "",
    otherEnzyme: "",
    targetGene: "",
    // Primary target gene
    target: "",
    otherTarget: "",
    // Multiple gene detection results (additional targets)
    geneDetectionResults: [],
    // Each gene result: { target, otherTarget, ctValue, result, notes }
    runId: "",
    testDate: new Date().toISOString().split("T")[0],
    performedBy: "",
    instrument: "",
    kitUsed: "",
    kitLot: "",
    controlResults: {
      positive: "",
      negative: "",
      internal: "",
    },
    // Primary result
    result: "",
    ctValue: "",
    // Attachments (images, text files, etc.)
    attachments: [],
    // Each attachment: { id, name, type, size, file, description }
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
  // Molecular QC State
  // ==========================================

  const [molecularQcModalOpen, setMolecularQcModalOpen] = useState(false);
  const [molecularQcData, setMolecularQcData] = useState({
    // QC Date and Performer
    qcDate: new Date().toISOString().split("T")[0],
    qcPerformedBy: "",

    // Reagent/Kit QC Section
    reagentQcChecks: [
      // Array of { reagentType, lotNumber, expiryDate, qcStatus, failureReason, otherReason, notes }
    ],

    // Equipment QC Section
    equipmentQcChecks: [
      // Array of { equipmentType, equipmentId, lastCalibrationDate, qcStatus, failureReason, otherReason, notes }
    ],

    // Sample QC Section
    sampleQcChecks: [
      // Array of { criterion, value, unit, qcStatus, failureReason, otherReason, notes }
    ],

    // Control Results
    positiveControlResult: "", // PASSED, FAILED, NOT_RUN
    negativeControlResult: "", // PASSED, FAILED, NOT_RUN
    internalControlResult: "", // PASSED, FAILED, NOT_RUN

    // Overall QC Result
    overallQcResult: "", // PASS, FAIL, CONDITIONAL
    overallQcNotes: "",
  });

  // ==========================================
  // Data Loading Functions
  // ==========================================

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();
    loadEnzymes();
    loadAntibiotics();

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
              biochemQcResult: sample.data?.biochemQcResult,
              biochemQcFailureReason: sample.data?.biochemQcFailureReason,
              biochemQcPassed: sample.data?.biochemQcPassed,
              presumptiveId: sample.data?.presumptiveId,
              dstCompleted: sample.data?.dstCompleted,
              dstMethod: sample.data?.dstMethod,
              automatedIdCompleted: sample.data?.automatedIdCompleted,
              organismIdentified: sample.data?.organismIdentified,
              molecularCompleted: sample.data?.molecularCompleted,
              pcrCompleted: sample.data?.pcrCompleted,
              pcrAssayType: sample.data?.pcrAssayType,
              pcrTarget: sample.data?.pcrTarget,
              pcrResult: sample.data?.pcrResult,
              pcrCtValue: sample.data?.pcrCtValue,
              pcrPrimer: sample.data?.pcrPrimer,
              pcrEnzyme: sample.data?.pcrEnzyme,
              pcrGeneDetectionResults: sample.data?.pcrGeneDetectionResults,
              pcrAttachments: sample.data?.pcrAttachments,
              pcrRunId: sample.data?.pcrRunId,
              pcrTestDate: sample.data?.pcrTestDate,
              pcrPerformedBy: sample.data?.pcrPerformedBy,
              pcrInstrument: sample.data?.pcrInstrument,
              pcrControlResults: sample.data?.pcrControlResults,
              pcrNotes: sample.data?.pcrNotes,
              wgsCompleted: sample.data?.wgsCompleted,
              extractionCompleted: sample.data?.extractionCompleted,
              // Molecular QC data
              molecularQcCompleted: sample.data?.molecularQcCompleted,
              molecularQcDate: sample.data?.molecularQcDate,
              molecularQcPerformedBy: sample.data?.molecularQcPerformedBy,
              molecularQcOverallResult: sample.data?.molecularQcOverallResult,
              molecularQcReagentChecks: sample.data?.molecularQcReagentChecks,
              molecularQcEquipmentChecks:
                sample.data?.molecularQcEquipmentChecks,
              molecularQcSampleChecks: sample.data?.molecularQcSampleChecks,
              molecularQcNotes: sample.data?.molecularQcNotes,
              // Media reactions data (multiple media per sample)
              mediaReactions: sample.data?.mediaReactions,
              mediaReactionsCompleted: sample.data?.mediaReactionsCompleted,
              mediaReactionsCount: sample.data?.mediaReactionsCount,
              hasDstResults: sample.data?.hasDstResults,
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

  const loadEnzymes = useCallback(() => {
    setLoadingEnzymes(true);
    getFromOpenElisServer(
      "/rest/inventory/item/type/ENZYME",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const enzymeOptions = response.map((enzyme) => ({
              id: enzyme.id,
              text: enzyme.name,
              name: enzyme.name,
              catalogNumber: enzyme.catalogNumber,
              manufacturer: enzyme.manufacturer,
              ...enzyme,
            }));
            setEnzymes(enzymeOptions);
          } else {
            setEnzymes(PCR_ENZYMES); // Fallback to hardcoded list
          }
          setLoadingEnzymes(false);
        }
      },
      () => {
        // On error, use the hardcoded PCR_ENZYMES list as fallback
        if (componentMounted.current) {
          setEnzymes(PCR_ENZYMES);
          setLoadingEnzymes(false);
        }
      },
    );
  }, []);

  const loadAntibiotics = useCallback(() => {
    setLoadingAntibiotics(true);
    getFromOpenElisServer(
      "/rest/inventory/item/type/ANTIBIOTICS",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const antibioticOptions = response.map((antibiotic) => ({
              id: antibiotic.id,
              text: antibiotic.name,
              name: antibiotic.name,
              catalogNumber: antibiotic.catalogNumber,
              manufacturer: antibiotic.manufacturer,
              ...antibiotic,
            }));
            setAntibiotics(antibioticOptions);
          } else {
            setAntibiotics([]); // No fallback hardcoded list needed
          }
          setLoadingAntibiotics(false);
        }
      },
      () => {
        // On error, set empty list
        if (componentMounted.current) {
          setAntibiotics([]);
          setLoadingAntibiotics(false);
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
      qcResult: "",
      qcFailureReason: "",
      qcNotes: "",
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
      // QC data
      biochemQcResult: biochemData.qcResult,
      biochemQcFailureReason: biochemData.qcFailureReason,
      biochemQcNotes: biochemData.qcNotes,
      biochemQcPassed: biochemData.qcResult === "PASS",
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
  // SECTION B.2: Media Reactions Handlers
  // Support for multiple media inoculations per sample with independent results
  // ==========================================

  const handleOpenMediaReactionsModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Initialize with empty array - user will add reactions
    setMediaReactions([]);
    setMediaReactionsModalOpen(true);
  }, [selectedIds, intl]);

  const handleAddMediaReaction = useCallback(() => {
    const newReaction = {
      id: `reaction-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      mediaType: "",
      otherMediaName: "",
      inoculationDate: new Date().toISOString().split("T")[0],
      inoculationTime: "",
      incubationCondition: "",
      incubationTemp: "",
      incubationDuration: "",
      inoculatedBy: "",
      colonyCount: "",
      colonyMorphology: [],
      hemolysis: "",
      lactoseFermentation: "",
      pigmentation: "",
      growthResult: "",
      organismIdentified: "",
      notes: "",
      dstCompleted: false,
      dstMethod: "",
      dstGuidelinesUsed: "",
      dstAntibioticPanel: "",
      dstAntibioticResults: [],
      dstNotes: "",
    };
    setMediaReactions([...mediaReactions, newReaction]);
  }, [mediaReactions]);

  const handleRemoveMediaReaction = useCallback(
    (index) => {
      setMediaReactions(mediaReactions.filter((_, i) => i !== index));
    },
    [mediaReactions],
  );

  const handleUpdateMediaReaction = useCallback(
    (index, field, value) => {
      const updated = [...mediaReactions];
      updated[index] = { ...updated[index], [field]: value };
      setMediaReactions(updated);
    },
    [mediaReactions],
  );

  // Open DST modal for a specific media reaction
  const handleOpenMediaReactionDST = useCallback((index) => {
    setEditingMediaReactionIndex(index);
    setMediaReactionDstModalOpen(true);
  }, []);

  // Add antibiotic to a specific media reaction's DST
  const handleAddMediaReactionAntibiotic = useCallback(() => {
    if (editingMediaReactionIndex === null) return;

    const reaction = mediaReactions[editingMediaReactionIndex];
    const newResult = {
      panelType:
        reaction.dstAntibioticPanel === "FIRST_LINE"
          ? "1ST_LINE"
          : reaction.dstAntibioticPanel === "SECOND_LINE"
            ? "2ND_LINE"
            : "1ST_LINE",
      antibiotic: "",
      zoneDiameter: "",
      mic: "",
      interpretation: "",
    };

    const updated = [...mediaReactions];
    updated[editingMediaReactionIndex] = {
      ...updated[editingMediaReactionIndex],
      dstAntibioticResults: [
        ...updated[editingMediaReactionIndex].dstAntibioticResults,
        newResult,
      ],
    };
    setMediaReactions(updated);
  }, [editingMediaReactionIndex, mediaReactions]);

  // Remove antibiotic from a specific media reaction's DST
  const handleRemoveMediaReactionAntibiotic = useCallback(
    (antibioticIndex) => {
      if (editingMediaReactionIndex === null) return;

      const updated = [...mediaReactions];
      updated[editingMediaReactionIndex] = {
        ...updated[editingMediaReactionIndex],
        dstAntibioticResults: updated[
          editingMediaReactionIndex
        ].dstAntibioticResults.filter((_, i) => i !== antibioticIndex),
      };
      setMediaReactions(updated);
    },
    [editingMediaReactionIndex, mediaReactions],
  );

  // Update antibiotic in a specific media reaction's DST
  const handleUpdateMediaReactionAntibiotic = useCallback(
    (antibioticIndex, field, value) => {
      if (editingMediaReactionIndex === null) return;

      const updated = [...mediaReactions];
      const antibioticResults = [
        ...updated[editingMediaReactionIndex].dstAntibioticResults,
      ];
      antibioticResults[antibioticIndex] = {
        ...antibioticResults[antibioticIndex],
        [field]: value,
      };
      updated[editingMediaReactionIndex] = {
        ...updated[editingMediaReactionIndex],
        dstAntibioticResults: antibioticResults,
      };
      setMediaReactions(updated);
    },
    [editingMediaReactionIndex, mediaReactions],
  );

  // Save DST for a specific media reaction
  const handleSaveMediaReactionDST = useCallback(() => {
    if (editingMediaReactionIndex === null) return;

    const updated = [...mediaReactions];
    updated[editingMediaReactionIndex] = {
      ...updated[editingMediaReactionIndex],
      dstCompleted: true,
    };
    setMediaReactions(updated);
    setMediaReactionDstModalOpen(false);
    setEditingMediaReactionIndex(null);
  }, [editingMediaReactionIndex, mediaReactions]);

  // Save all media reactions to the server
  const handleSaveMediaReactions = useCallback(() => {
    if (!hasRealPageId) {
      setMediaReactionsModalOpen(false);
      return;
    }

    if (mediaReactions.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.noMediaReactions",
          defaultMessage: "Please add at least one media reaction.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      mediaReactionsCompleted: true,
      mediaReactions: mediaReactions,
      mediaReactionsCount: mediaReactions.length,
      // Track if any DST was performed
      hasDstResults: mediaReactions.some(
        (r) => r.dstCompleted && r.dstAntibioticResults.length > 0,
      ),
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
                  id: "notebook.bacteriology.assay.mediaReactionsSaved",
                  defaultMessage:
                    "Media reactions saved for {count} samples ({reactionCount} reactions recorded).",
                },
                {
                  count: selectedIds.length,
                  reactionCount: mediaReactions.length,
                },
              ),
            );
            setMediaReactionsModalOpen(false);
            setSelectedIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save media reactions.");
          }
        }
      },
    );
  }, [
    mediaReactions,
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

  // Add antibiotic result row - always includes zoneDiameter, mic, and interpretation
  const handleAddAntibioticResult = useCallback(() => {
    const newResult = {
      panelType:
        dstData.antibioticPanel === "FIRST_LINE"
          ? "1ST_LINE"
          : dstData.antibioticPanel === "SECOND_LINE"
            ? "2ND_LINE"
            : "1ST_LINE",
      antibiotic: "",
      zoneDiameter: "", // Zone of inhibition (mm) - for Disc Diffusion
      mic: "", // Minimum Inhibitory Concentration (µg/mL) - always available
      interpretation: "", // S/I/R interpretation
    };

    setAntibioticResults([...antibioticResults, newResult]);
  }, [dstData.antibioticPanel, antibioticResults]);

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

  // ==========================================
  // Molecular QC Handlers
  // ==========================================

  const handleOpenMolecularQcModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.assay.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Reset QC data with one default entry for each section
    setMolecularQcData({
      qcDate: new Date().toISOString().split("T")[0],
      qcPerformedBy: "",
      reagentQcChecks: [
        {
          reagentType: "",
          lotNumber: "",
          expiryDate: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
      equipmentQcChecks: [
        {
          equipmentType: "",
          equipmentId: "",
          lastCalibrationDate: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
      sampleQcChecks: [
        {
          criterion: "",
          value: "",
          unit: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
      positiveControlResult: "",
      negativeControlResult: "",
      internalControlResult: "",
      overallQcResult: "",
      overallQcNotes: "",
    });
    setMolecularQcModalOpen(true);
  }, [selectedIds, intl]);

  // Add a new reagent QC check row
  const handleAddReagentQcCheck = useCallback(() => {
    setMolecularQcData((prev) => ({
      ...prev,
      reagentQcChecks: [
        ...prev.reagentQcChecks,
        {
          reagentType: "",
          lotNumber: "",
          expiryDate: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
    }));
  }, []);

  // Remove a reagent QC check row
  const handleRemoveReagentQcCheck = useCallback((index) => {
    setMolecularQcData((prev) => ({
      ...prev,
      reagentQcChecks: prev.reagentQcChecks.filter((_, i) => i !== index),
    }));
  }, []);

  // Update a reagent QC check row
  const handleUpdateReagentQcCheck = useCallback((index, field, value) => {
    setMolecularQcData((prev) => ({
      ...prev,
      reagentQcChecks: prev.reagentQcChecks.map((check, i) =>
        i === index ? { ...check, [field]: value } : check,
      ),
    }));
  }, []);

  // Add a new equipment QC check row
  const handleAddEquipmentQcCheck = useCallback(() => {
    setMolecularQcData((prev) => ({
      ...prev,
      equipmentQcChecks: [
        ...prev.equipmentQcChecks,
        {
          equipmentType: "",
          equipmentId: "",
          lastCalibrationDate: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
    }));
  }, []);

  // Remove an equipment QC check row
  const handleRemoveEquipmentQcCheck = useCallback((index) => {
    setMolecularQcData((prev) => ({
      ...prev,
      equipmentQcChecks: prev.equipmentQcChecks.filter((_, i) => i !== index),
    }));
  }, []);

  // Update an equipment QC check row
  const handleUpdateEquipmentQcCheck = useCallback((index, field, value) => {
    setMolecularQcData((prev) => ({
      ...prev,
      equipmentQcChecks: prev.equipmentQcChecks.map((check, i) =>
        i === index ? { ...check, [field]: value } : check,
      ),
    }));
  }, []);

  // Add a new sample QC check row
  const handleAddSampleQcCheck = useCallback(() => {
    setMolecularQcData((prev) => ({
      ...prev,
      sampleQcChecks: [
        ...prev.sampleQcChecks,
        {
          criterion: "",
          value: "",
          unit: "",
          qcStatus: "",
          failureReason: "",
          otherReason: "",
          notes: "",
        },
      ],
    }));
  }, []);

  // Remove a sample QC check row
  const handleRemoveSampleQcCheck = useCallback((index) => {
    setMolecularQcData((prev) => ({
      ...prev,
      sampleQcChecks: prev.sampleQcChecks.filter((_, i) => i !== index),
    }));
  }, []);

  // Update a sample QC check row
  const handleUpdateSampleQcCheck = useCallback((index, field, value) => {
    setMolecularQcData((prev) => ({
      ...prev,
      sampleQcChecks: prev.sampleQcChecks.map((check, i) =>
        i === index ? { ...check, [field]: value } : check,
      ),
    }));
  }, []);

  // Save Molecular QC Data
  const handleSaveMolecularQcData = useCallback(() => {
    if (!hasRealPageId) {
      setMolecularQcModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Determine if QC passed or failed
    const hasReagentFailure = molecularQcData.reagentQcChecks.some(
      (check) => check.qcStatus === "FAILED",
    );
    const hasEquipmentFailure = molecularQcData.equipmentQcChecks.some(
      (check) => check.qcStatus === "FAILED" || check.qcStatus === "DUE",
    );
    const hasSampleFailure = molecularQcData.sampleQcChecks.some(
      (check) => check.qcStatus === "FAILED",
    );
    const hasControlFailure =
      molecularQcData.positiveControlResult === "FAILED" ||
      molecularQcData.negativeControlResult === "FAILED";

    // Auto-determine overall QC result if not manually set
    let overallResult = molecularQcData.overallQcResult;
    if (!overallResult) {
      if (
        hasReagentFailure ||
        hasEquipmentFailure ||
        hasSampleFailure ||
        hasControlFailure
      ) {
        overallResult = "FAIL";
      } else {
        const hasConditional = molecularQcData.sampleQcChecks.some(
          (check) => check.qcStatus === "CONDITIONAL",
        );
        overallResult = hasConditional ? "CONDITIONAL" : "PASS";
      }
    }

    const dataToSave = {
      molecularQcCompleted: true,
      molecularQcDate: molecularQcData.qcDate,
      molecularQcPerformedBy: molecularQcData.qcPerformedBy,
      molecularQcReagentChecks: molecularQcData.reagentQcChecks,
      molecularQcEquipmentChecks: molecularQcData.equipmentQcChecks,
      molecularQcSampleChecks: molecularQcData.sampleQcChecks,
      molecularQcPositiveControl: molecularQcData.positiveControlResult,
      molecularQcNegativeControl: molecularQcData.negativeControlResult,
      molecularQcInternalControl: molecularQcData.internalControlResult,
      molecularQcOverallResult: overallResult,
      molecularQcNotes: molecularQcData.overallQcNotes,
    };

    // Determine sample status based on QC result
    let sampleStatus = "IN_PROGRESS";
    if (overallResult === "FAIL") {
      sampleStatus = "REJECTED";
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update sample status if QC failed
            if (overallResult === "FAIL") {
              postToOpenElisServer(
                `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                JSON.stringify({
                  sampleIds: numericIds,
                  status: sampleStatus,
                }),
                () => {
                  setError(
                    intl.formatMessage(
                      {
                        id: "notebook.bacteriology.assay.molecularQcFailed",
                        defaultMessage:
                          "Molecular QC FAILED for {count} samples. Samples cannot proceed with molecular testing.",
                      },
                      { count: selectedIds.length },
                    ),
                  );
                  setMolecularQcModalOpen(false);
                  setSelectedIds([]);
                  loadPageSamples();
                },
              );
            } else {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.bacteriology.assay.molecularQcSaved",
                    defaultMessage:
                      "Molecular QC {result} for {count} samples.",
                  },
                  {
                    result: overallResult === "PASS" ? "PASSED" : "CONDITIONAL",
                    count: selectedIds.length,
                  },
                ),
              );
              setMolecularQcModalOpen(false);
              setSelectedIds([]);
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }
          } else {
            setError(response?.error || "Failed to save molecular QC data.");
          }
        }
      },
    );
  }, [
    molecularQcData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Check if selected samples have molecular QC failures (used to disable buttons)
  const selectedMolecularQcStatus = useMemo(() => {
    if (selectedIds.length === 0) {
      return { hasQCFailed: false, qcFailedCount: 0 };
    }

    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const qcFailedSamples = selectedSamples.filter(
      (s) => s.molecularQcOverallResult === "FAIL" || s.status === "REJECTED",
    );

    return {
      hasQCFailed: qcFailedSamples.length > 0,
      qcFailedCount: qcFailedSamples.length,
    };
  }, [selectedIds, samples]);

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
      primerDirection: "",
      primerType: "",
      enzyme: "",
      otherEnzyme: "",
      targetGene: "",
      target: "",
      otherTarget: "",
      geneDetectionResults: [],
      runId: "",
      testDate: new Date().toISOString().split("T")[0],
      performedBy: "",
      instrument: "",
      kitUsed: "",
      kitLot: "",
      controlResults: {
        positive: "",
        negative: "",
        internal: "",
      },
      result: "",
      ctValue: "",
      attachments: [],
      notes: "",
    });
    setPcrModalOpen(true);
  }, [selectedIds, intl]);

  // Add gene detection result
  const handleAddGeneDetectionResult = useCallback(() => {
    const newResult = {
      id: `gene-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      target: "",
      otherTarget: "",
      ctValue: "",
      result: "",
      notes: "",
    };
    setPcrData((prev) => ({
      ...prev,
      geneDetectionResults: [...prev.geneDetectionResults, newResult],
    }));
  }, []);

  // Remove gene detection result
  const handleRemoveGeneDetectionResult = useCallback((index) => {
    setPcrData((prev) => ({
      ...prev,
      geneDetectionResults: prev.geneDetectionResults.filter(
        (_, i) => i !== index,
      ),
    }));
  }, []);

  // Update gene detection result
  const handleUpdateGeneDetectionResult = useCallback((index, field, value) => {
    setPcrData((prev) => {
      const updated = [...prev.geneDetectionResults];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, geneDetectionResults: updated };
    });
  }, []);

  // Handle file attachment upload
  const handlePcrFileUpload = useCallback((event) => {
    const files = Array.from(event.target.files || []);
    const newAttachments = files.map((file) => ({
      id: `attachment-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      name: file.name,
      type: file.type,
      size: file.size,
      file: file,
      description: "",
    }));
    setPcrData((prev) => ({
      ...prev,
      attachments: [...prev.attachments, ...newAttachments],
    }));
  }, []);

  // Remove attachment
  const handleRemovePcrAttachment = useCallback((index) => {
    setPcrData((prev) => ({
      ...prev,
      attachments: prev.attachments.filter((_, i) => i !== index),
    }));
  }, []);

  // Update attachment description
  const handleUpdatePcrAttachmentDescription = useCallback(
    (index, description) => {
      setPcrData((prev) => {
        const updated = [...prev.attachments];
        updated[index] = { ...updated[index], description };
        return { ...prev, attachments: updated };
      });
    },
    [],
  );

  const handleSavePCRData = useCallback(() => {
    if (!hasRealPageId) {
      setPcrModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Process attachments - convert file objects to base64 for storage
    const processedAttachments = pcrData.attachments.map((att) => ({
      id: att.id,
      name: att.name,
      type: att.type,
      size: att.size,
      description: att.description,
      // Note: actual file upload would be handled separately
    }));

    const dataToSave = {
      molecularCompleted: true,
      pcrCompleted: true,
      pcrAssayType: pcrData.assayType,
      // Primer/Enzyme structured template
      pcrPrimerDirection: pcrData.primerDirection,
      pcrPrimerType: pcrData.primerType,
      pcrTargetGene: pcrData.targetGene,
      pcrEnzyme:
        pcrData.enzyme === "OTHER" ? pcrData.otherEnzyme : pcrData.enzyme,
      // Primary target gene
      pcrTarget:
        pcrData.target === "OTHER" ? pcrData.otherTarget : pcrData.target,
      // Primary result
      pcrResult: pcrData.result,
      pcrCtValue: pcrData.ctValue,
      // Multiple gene detection results (additional targets)
      pcrGeneDetectionResults: pcrData.geneDetectionResults.map((result) => ({
        target: result.target === "OTHER" ? result.otherTarget : result.target,
        ctValue: result.ctValue,
        result: result.result,
        notes: result.notes,
      })),
      // Attachments metadata
      pcrAttachments: processedAttachments,
      pcrRunId: pcrData.runId,
      pcrTestDate: pcrData.testDate,
      pcrPerformedBy: pcrData.performedBy,
      pcrInstrument: pcrData.instrument,
      pcrKitUsed: pcrData.kitUsed,
      pcrKitLot: pcrData.kitLot,
      pcrControlResults: pcrData.controlResults,
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
  const renderTestStatus = (value, sample) => {
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
      // Show Biochem with QC status
      if (sample.biochemQcResult === "FAIL") {
        tests.push(
          <Tag
            key="biochem"
            type="red"
            size="sm"
            style={{ marginRight: "4px" }}
            title={
              sample.biochemQcFailureReason
                ? BIOCHEM_QC_FAILURE_REASONS.find(
                    (r) => r.id === sample.biochemQcFailureReason,
                  )?.text || sample.biochemQcFailureReason
                : "QC Failed"
            }
          >
            Biochem QC Failed
          </Tag>,
        );
      } else if (sample.biochemQcResult === "PASS") {
        tests.push(
          <Tag
            key="biochem"
            type="green"
            size="sm"
            style={{ marginRight: "4px" }}
          >
            Biochem QC Passed
          </Tag>,
        );
      } else {
        // No QC result recorded
        tests.push(
          <Tag
            key="biochem"
            type="gray"
            size="sm"
            style={{ marginRight: "4px" }}
          >
            Biochem (No QC)
          </Tag>,
        );
      }
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
    if (sample.molecularCompleted || sample.pcrCompleted) {
      // Show PCR tag
      tests.push(
        <Tag
          key="molecular"
          type="teal"
          size="sm"
          style={{ marginRight: "4px" }}
        >
          PCR
        </Tag>,
      );

      // Show individual gene detection results
      const geneResults = sample.pcrGeneDetectionResults || [];
      geneResults.forEach((gene, index) => {
        const geneTargetText =
          PCR_TARGETS.find((t) => t.id === gene.target)?.text || gene.target;
        const resultColor =
          gene.result === "DETECTED"
            ? "red"
            : gene.result === "NOT_DETECTED"
              ? "green"
              : "gray";
        tests.push(
          <Tag
            key={`gene-${index}`}
            type={resultColor}
            size="sm"
            style={{ marginRight: "4px" }}
            title={`Ct: ${gene.ctValue || "N/A"}, Notes: ${gene.notes || "None"}`}
          >
            {geneTargetText}:{" "}
            {gene.result === "DETECTED"
              ? "+"
              : gene.result === "NOT_DETECTED"
                ? "-"
                : "?"}
          </Tag>,
        );
      });

      // Show attachments count if any
      const attachmentCount = sample.pcrAttachments?.length || 0;
      if (attachmentCount > 0) {
        const attachmentNames = sample.pcrAttachments
          .map((att) => att.name)
          .join(", ");
        tests.push(
          <Tag
            key="attachments"
            type="purple"
            size="sm"
            style={{ marginRight: "4px" }}
            title={attachmentNames}
          >
            {attachmentCount} File{attachmentCount > 1 ? "s" : ""}
          </Tag>,
        );
      }
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

  // Render Molecular QC Status column - shows detailed QC status for Section E
  const renderMolecularQcStatus = (value, sample) => {
    if (!sample) return null;

    // Check if QC has been completed
    if (!sample.molecularQcCompleted) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.bacteriology.assay.qcPending"
              defaultMessage="QC Pending"
            />
          </Tag>
          <div style={{ color: "#8d8d8d", marginTop: "4px", fontSize: "11px" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.qcRequired"
              defaultMessage="Molecular QC required before testing"
            />
          </div>
        </div>
      );
    }

    const qcResult = sample.molecularQcOverallResult;

    // QC Failed
    if (qcResult === "FAIL") {
      // Get failure reasons from the QC checks
      const failureReasons = [];

      // Check reagent failures
      if (sample.molecularQcReagentChecks) {
        sample.molecularQcReagentChecks.forEach((check) => {
          if (check.qcStatus === "FAILED" && check.failureReason) {
            const reasonText =
              REAGENT_QC_FAILURE_REASONS.find(
                (r) => r.id === check.failureReason,
              )?.text || check.failureReason;
            const reagentText =
              MOLECULAR_REAGENT_TYPES.find((r) => r.id === check.reagentType)
                ?.text || check.reagentType;
            failureReasons.push(`${reagentText}: ${reasonText}`);
          }
        });
      }

      // Check equipment failures
      if (sample.molecularQcEquipmentChecks) {
        sample.molecularQcEquipmentChecks.forEach((check) => {
          if (
            (check.qcStatus === "FAILED" || check.qcStatus === "DUE") &&
            check.failureReason
          ) {
            const reasonText =
              EQUIPMENT_QC_FAILURE_REASONS.find(
                (r) => r.id === check.failureReason,
              )?.text || check.failureReason;
            const equipmentText =
              MOLECULAR_EQUIPMENT_TYPES.find(
                (e) => e.id === check.equipmentType,
              )?.text || check.equipmentType;
            failureReasons.push(`${equipmentText}: ${reasonText}`);
          }
        });
      }

      // Check sample failures
      if (sample.molecularQcSampleChecks) {
        sample.molecularQcSampleChecks.forEach((check) => {
          if (check.qcStatus === "FAILED" && check.failureReason) {
            const reasonText =
              SAMPLE_QC_FAILURE_REASONS.find(
                (r) => r.id === check.failureReason,
              )?.text || check.failureReason;
            const criterionText =
              SAMPLE_QC_CRITERIA.find((c) => c.id === check.criterion)?.text ||
              check.criterion;
            failureReasons.push(`${criterionText}: ${reasonText}`);
          }
        });
      }

      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="red" size="sm">
            <FormattedMessage
              id="notebook.bacteriology.assay.qcFailed"
              defaultMessage="QC FAILED"
            />
          </Tag>
          <div style={{ marginTop: "4px", fontSize: "11px" }}>
            {failureReasons.length > 0 && (
              <div style={{ color: "#da1e28", marginBottom: "2px" }}>
                <strong>
                  <FormattedMessage
                    id="notebook.bacteriology.assay.reason"
                    defaultMessage="Reason:"
                  />
                </strong>{" "}
                {failureReasons.slice(0, 2).join("; ")}
                {failureReasons.length > 2 &&
                  ` (+${failureReasons.length - 2} more)`}
              </div>
            )}
            {sample.molecularQcDate && (
              <div style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.date"
                  defaultMessage="Date:"
                />{" "}
                {sample.molecularQcDate}
              </div>
            )}
            {sample.molecularQcPerformedBy && (
              <div style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.performedBy"
                  defaultMessage="By:"
                />{" "}
                {sample.molecularQcPerformedBy}
              </div>
            )}
          </div>
        </div>
      );
    }

    // QC Passed
    if (qcResult === "PASS") {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="green" size="sm">
            <FormattedMessage
              id="notebook.bacteriology.assay.qcPassed"
              defaultMessage="QC PASSED"
            />
          </Tag>
          <div style={{ marginTop: "4px", fontSize: "11px" }}>
            <div style={{ color: "#198038", fontWeight: "500" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.proceedToExtraction"
                defaultMessage="→ Proceed to DNA/RNA Extraction"
              />
            </div>
            {sample.molecularQcDate && (
              <div style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.date"
                  defaultMessage="Date:"
                />{" "}
                {sample.molecularQcDate}
              </div>
            )}
            {sample.molecularQcPerformedBy && (
              <div style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.performedBy"
                  defaultMessage="By:"
                />{" "}
                {sample.molecularQcPerformedBy}
              </div>
            )}
          </div>
        </div>
      );
    }

    // QC Conditional
    if (qcResult === "CONDITIONAL") {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="outline" size="sm" style={{ borderColor: "#f1c21b" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.qcConditional"
              defaultMessage="QC CONDITIONAL"
            />
          </Tag>
          <div style={{ marginTop: "4px", fontSize: "11px" }}>
            <div style={{ color: "#b28600", fontWeight: "500" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.proceedWithCaution"
                defaultMessage="→ Proceed with Caution"
              />
            </div>
            {sample.molecularQcNotes && (
              <div style={{ color: "#525252", fontStyle: "italic" }}>
                {sample.molecularQcNotes}
              </div>
            )}
            {sample.molecularQcDate && (
              <div style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.date"
                  defaultMessage="Date:"
                />{" "}
                {sample.molecularQcDate}
              </div>
            )}
          </div>
        </div>
      );
    }

    // Default - QC completed but no result
    return (
      <div style={{ fontSize: "12px" }}>
        <Tag type="gray" size="sm">
          <FormattedMessage
            id="notebook.bacteriology.assay.qcCompleted"
            defaultMessage="QC Done"
          />
        </Tag>
      </div>
    );
  };

  // Render PCR Details column - shows PCR Type, Target Gene, Primer, Enzyme
  const renderPcrDetails = (value, sample) => {
    if (!sample) return null;

    // Check if PCR has been completed
    if (!sample.pcrCompleted) {
      return (
        <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
          <FormattedMessage
            id="notebook.bacteriology.assay.noPcrData"
            defaultMessage="No PCR data"
          />
        </span>
      );
    }

    // Get display text for PCR Assay Type
    const assayTypeText =
      PCR_ASSAY_TYPES.find((t) => t.id === sample.pcrAssayType)?.text ||
      sample.pcrAssayType ||
      "—";

    // Get display text for Target Gene
    const targetText =
      PCR_TARGETS.find((t) => t.id === sample.pcrTarget)?.text ||
      sample.pcrTarget ||
      "—";

    // Get display text for Primer
    const primerText =
      PCR_PRIMERS.find((p) => p.id === sample.pcrPrimer)?.text ||
      sample.pcrPrimer ||
      "—";

    // Get display text for Enzyme
    const enzymeText =
      PCR_ENZYMES.find((e) => e.id === sample.pcrEnzyme)?.text ||
      sample.pcrEnzyme ||
      "—";

    // Gene detection results count
    const geneCount = sample.pcrGeneDetectionResults?.length || 0;
    const attachmentCount = sample.pcrAttachments?.length || 0;

    return (
      <div style={{ fontSize: "12px" }}>
        {/* PCR Type */}
        <div style={{ marginBottom: "4px" }}>
          <Tag type="blue" size="sm">
            {assayTypeText}
          </Tag>
        </div>

        {/* Target Gene */}
        <div style={{ marginBottom: "2px" }}>
          <span style={{ color: "#525252", fontWeight: "500" }}>Target: </span>
          <span style={{ color: "#161616" }}>{targetText}</span>
        </div>

        {/* Primer */}
        <div style={{ marginBottom: "2px" }}>
          <span style={{ color: "#525252", fontWeight: "500" }}>Primer: </span>
          <span style={{ color: "#161616", fontSize: "11px" }}>
            {primerText}
          </span>
        </div>

        {/* Enzyme */}
        <div style={{ marginBottom: "2px" }}>
          <span style={{ color: "#525252", fontWeight: "500" }}>Enzyme: </span>
          <span style={{ color: "#161616", fontSize: "11px" }}>
            {enzymeText}
          </span>
        </div>

        {/* Gene Detection Results */}
        {geneCount > 0 && (
          <div style={{ marginTop: "4px" }}>
            <Tag type="teal" size="sm">
              {geneCount} Gene Target{geneCount > 1 ? "s" : ""}
            </Tag>
          </div>
        )}

        {/* Attachments */}
        {attachmentCount > 0 && (
          <Tag type="warm-gray" size="sm" style={{ marginLeft: "4px" }}>
            {attachmentCount} Attachment{attachmentCount > 1 ? "s" : ""}
          </Tag>
        )}

        {/* PCR Result */}
        {sample.pcrResult && (
          <div style={{ marginTop: "4px" }}>
            <Tag
              type={
                sample.pcrResult === "DETECTED"
                  ? "red"
                  : sample.pcrResult === "NOT_DETECTED"
                    ? "green"
                    : "gray"
              }
              size="sm"
            >
              {sample.pcrResult === "DETECTED"
                ? "Detected"
                : sample.pcrResult === "NOT_DETECTED"
                  ? "Not Detected"
                  : sample.pcrResult}
            </Tag>
          </div>
        )}
      </div>
    );
  };

  // Render identification column
  const renderIdentification = (value, sample) => {
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

  // Render media reactions column - shows summary of multiple media inoculations per sample
  const renderMediaReactions = (value, sample) => {
    if (!sample) return null;

    // Check if sample has media reactions data
    const reactions = sample.mediaReactions;
    if (!reactions || reactions.length === 0) {
      if (sample.mediaReactionsCompleted) {
        return (
          <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.noReactionsRecorded"
              defaultMessage="No reactions"
            />
          </span>
        );
      }
      return (
        <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
          <FormattedMessage
            id="notebook.bacteriology.assay.mediaReactionsNotRecorded"
            defaultMessage="Not recorded"
          />
        </span>
      );
    }

    // Group by growth result
    const growthCount = reactions.filter(
      (r) => r.growthResult === "GROWTH",
    ).length;
    const noGrowthCount = reactions.filter(
      (r) => r.growthResult === "NO_GROWTH",
    ).length;
    const contaminatedCount = reactions.filter(
      (r) => r.growthResult === "CONTAMINATED",
    ).length;
    const pendingCount = reactions.filter(
      (r) => r.growthResult === "PENDING" || !r.growthResult,
    ).length;
    const dstCount = reactions.filter(
      (r) => r.dstCompleted && r.dstAntibioticResults?.length > 0,
    ).length;

    return (
      <div style={{ fontSize: "12px" }}>
        {/* Media count summary */}
        <div style={{ marginBottom: "4px" }}>
          <Tag type="blue" size="sm">
            {reactions.length}{" "}
            <FormattedMessage
              id="notebook.bacteriology.assay.mediaCount"
              defaultMessage="{count, plural, one {medium} other {media}}"
              values={{ count: reactions.length }}
            />
          </Tag>
        </div>

        {/* Growth results */}
        <div style={{ display: "flex", flexWrap: "wrap", gap: "2px" }}>
          {growthCount > 0 && (
            <Tag type="green" size="sm">
              {growthCount} Growth
            </Tag>
          )}
          {noGrowthCount > 0 && (
            <Tag type="gray" size="sm">
              {noGrowthCount} No Growth
            </Tag>
          )}
          {contaminatedCount > 0 && (
            <Tag type="red" size="sm">
              {contaminatedCount} Contaminated
            </Tag>
          )}
          {pendingCount > 0 && (
            <Tag type="purple" size="sm">
              {pendingCount} Pending
            </Tag>
          )}
        </div>

        {/* DST summary */}
        {dstCount > 0 && (
          <div style={{ marginTop: "4px" }}>
            <Tag type="teal" size="sm">
              {dstCount} DST
            </Tag>
          </div>
        )}

        {/* List individual media types */}
        <div
          style={{
            marginTop: "4px",
            fontSize: "11px",
            color: "#525252",
          }}
        >
          {reactions.slice(0, 3).map((r, idx) => {
            const mediaName =
              ISOLATION_MEDIA_TYPES.find((m) => m.id === r.mediaType)?.text ||
              r.mediaType ||
              "Unknown";
            const shortName =
              mediaName.length > 15
                ? mediaName.substring(0, 12) + "..."
                : mediaName;
            return (
              <div key={r.id || idx}>
                {shortName}
                {r.organismIdentified && (
                  <span style={{ fontWeight: "500" }}>
                    {" "}
                    → {r.organismIdentified}
                  </span>
                )}
              </div>
            );
          })}
          {reactions.length > 3 && (
            <div style={{ fontStyle: "italic" }}>
              +{reactions.length - 3} more...
            </div>
          )}
        </div>
      </div>
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
                  kind="primary"
                  size="sm"
                  renderIcon={Add}
                  onClick={handleOpenMediaReactionsModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.mediaReactions"
                    defaultMessage="Media Reactions ({count})"
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
                      key: "mediaReactions",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.mediaReactions",
                        defaultMessage: "Media Reactions",
                      }),
                      render: renderMediaReactions,
                    },
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
                {/* Molecular QC Button - Must be done first */}
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Activity}
                  onClick={handleOpenMolecularQcModal}
                  disabled={selectedIds.length === 0}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.molecularQc"
                    defaultMessage="Molecular QC ({count})"
                    values={{ count: selectedIds.length }}
                  />
                </Button>

                {/* Extraction - Disabled if QC failed */}
                <Tooltip
                  align="bottom"
                  label={
                    selectedMolecularQcStatus.hasQCFailed
                      ? intl.formatMessage(
                          {
                            id: "notebook.bacteriology.assay.qcFailedTooltip",
                            defaultMessage:
                              "{count} selected sample(s) have failed Molecular QC. Cannot proceed with testing.",
                          },
                          { count: selectedMolecularQcStatus.qcFailedCount },
                        )
                      : ""
                  }
                >
                  <Button
                    kind="secondary"
                    size="sm"
                    renderIcon={DataVis_2}
                    onClick={handleOpenExtractionModal}
                    disabled={
                      selectedIds.length === 0 ||
                      selectedMolecularQcStatus.hasQCFailed
                    }
                  >
                    <FormattedMessage
                      id="notebook.bacteriology.assay.recordExtraction"
                      defaultMessage="DNA/RNA Extraction ({count})"
                      values={{ count: selectedIds.length }}
                    />
                  </Button>
                </Tooltip>

                {/* PCR - Disabled if QC failed */}
                <Tooltip
                  align="bottom"
                  label={
                    selectedMolecularQcStatus.hasQCFailed
                      ? intl.formatMessage(
                          {
                            id: "notebook.bacteriology.assay.qcFailedTooltip",
                            defaultMessage:
                              "{count} selected sample(s) have failed Molecular QC. Cannot proceed with testing.",
                          },
                          { count: selectedMolecularQcStatus.qcFailedCount },
                        )
                      : ""
                  }
                >
                  <Button
                    kind="secondary"
                    size="sm"
                    renderIcon={ListChecked}
                    onClick={handleOpenPCRModal}
                    disabled={
                      selectedIds.length === 0 ||
                      selectedMolecularQcStatus.hasQCFailed
                    }
                  >
                    <FormattedMessage
                      id="notebook.bacteriology.assay.recordPCR"
                      defaultMessage="PCR Assay ({count})"
                      values={{ count: selectedIds.length }}
                    />
                  </Button>
                </Tooltip>

                {/* WGS - Disabled if QC failed */}
                <Tooltip
                  align="bottom"
                  label={
                    selectedMolecularQcStatus.hasQCFailed
                      ? intl.formatMessage(
                          {
                            id: "notebook.bacteriology.assay.qcFailedTooltip",
                            defaultMessage:
                              "{count} selected sample(s) have failed Molecular QC. Cannot proceed with testing.",
                          },
                          { count: selectedMolecularQcStatus.qcFailedCount },
                        )
                      : ""
                  }
                >
                  <Button
                    kind="tertiary"
                    size="sm"
                    renderIcon={DataBase}
                    onClick={handleOpenWGSModal}
                    disabled={
                      selectedIds.length === 0 ||
                      selectedMolecularQcStatus.hasQCFailed
                    }
                  >
                    <FormattedMessage
                      id="notebook.bacteriology.assay.recordWGS"
                      defaultMessage="WGS ({count})"
                      values={{ count: selectedIds.length }}
                    />
                  </Button>
                </Tooltip>

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
                      key: "molecularQcStatus",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.molecularQcStatus",
                        defaultMessage: "Molecular QC Status",
                      }),
                      render: renderMolecularQcStatus,
                    },
                    {
                      key: "pcrDetails",
                      header: intl.formatMessage({
                        id: "notebook.bacteriology.assay.pcrDetails",
                        defaultMessage: "PCR Details",
                      }),
                      render: renderPcrDetails,
                    },
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
          </Grid>

          {/* QC Section */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#e8f4f8",
              borderRadius: "4px",
              border: "1px solid #0072c3",
            }}
          >
            <h6 style={{ marginBottom: "0.75rem", color: "#0072c3" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.biochemQcSection"
                defaultMessage="Quality Control (QC)"
              />
            </h6>

            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.qcResult",
                    defaultMessage: "QC Result",
                  })}
                  name="biochem-qc-result"
                  valueSelected={biochemData.qcResult}
                  onChange={(value) =>
                    setBiochemData({
                      ...biochemData,
                      qcResult: value,
                      // Clear failure reason if passing
                      qcFailureReason:
                        value === "PASS" ? "" : biochemData.qcFailureReason,
                    })
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    id="biochem-qc-pass"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.qcPass",
                      defaultMessage: "Pass",
                    })}
                    value="PASS"
                  />
                  <RadioButton
                    id="biochem-qc-fail"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.qcFail",
                      defaultMessage: "Fail",
                    })}
                    value="FAIL"
                  />
                </RadioButtonGroup>
              </Column>

              {biochemData.qcResult === "FAIL" && (
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="biochem-qc-failure-reason"
                    titleText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.qcFailureReason",
                      defaultMessage: "Failure Reason",
                    })}
                    label="Select reason"
                    items={BIOCHEM_QC_FAILURE_REASONS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={BIOCHEM_QC_FAILURE_REASONS.find(
                      (r) => r.id === biochemData.qcFailureReason,
                    )}
                    onChange={({ selectedItem }) =>
                      setBiochemData({
                        ...biochemData,
                        qcFailureReason: selectedItem?.id || "",
                      })
                    }
                  />
                </Column>
              )}

              {biochemData.qcResult && (
                <Column lg={16} md={8} sm={4}>
                  <TextInput
                    id="biochem-qc-notes"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.qcNotes",
                      defaultMessage: "QC Notes",
                    })}
                    value={biochemData.qcNotes}
                    onChange={(e) =>
                      setBiochemData({
                        ...biochemData,
                        qcNotes: e.target.value,
                      })
                    }
                    placeholder={
                      biochemData.qcResult === "FAIL"
                        ? "Describe the QC failure..."
                        : "Optional notes about QC..."
                    }
                  />
                </Column>
              )}
            </Grid>
          </div>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
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
          MODALS - Section B.2: Media Reactions
          Supports multiple media inoculations per sample with independent results and DST
          ========================================== */}
      <Modal
        open={mediaReactionsModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.mediaReactionsTitle",
          defaultMessage: "Record Media Reactions",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setMediaReactionsModalOpen(false)}
        onRequestSubmit={handleSaveMediaReactions}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.mediaReactionsDescription"
              defaultMessage="Record multiple media inoculations and results for {count} selected sample(s). Each media reaction can have its own culture results and DST."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Add Media Reaction Button */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <h6>
              <FormattedMessage
                id="notebook.bacteriology.assay.mediaReactionsList"
                defaultMessage="Media Reactions ({count})"
                values={{ count: mediaReactions.length }}
              />
            </h6>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Add}
              onClick={handleAddMediaReaction}
            >
              <FormattedMessage
                id="notebook.bacteriology.assay.addMediaReaction"
                defaultMessage="Add Media"
              />
            </Button>
          </div>

          {/* Media Reactions List */}
          {mediaReactions.length === 0 ? (
            <div
              style={{
                padding: "2rem",
                textAlign: "center",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <p style={{ color: "#6f6f6f", fontStyle: "italic" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.noMediaReactionsYet"
                  defaultMessage="No media reactions added yet. Click 'Add Media' to record inoculations on different media types."
                />
              </p>
            </div>
          ) : (
            <div style={{ maxHeight: "500px", overflowY: "auto" }}>
              {mediaReactions.map((reaction, index) => (
                <div
                  key={reaction.id}
                  style={{
                    padding: "1rem",
                    marginBottom: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    border: "1px solid #e0e0e0",
                  }}
                >
                  {/* Media Reaction Header */}
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "0.75rem",
                    }}
                  >
                    <h6 style={{ margin: 0 }}>
                      <FormattedMessage
                        id="notebook.bacteriology.assay.mediaReactionNumber"
                        defaultMessage="Media Reaction #{number}"
                        values={{ number: index + 1 }}
                      />
                      {reaction.mediaType && (
                        <Tag
                          type="blue"
                          size="sm"
                          style={{ marginLeft: "0.5rem" }}
                        >
                          {ISOLATION_MEDIA_TYPES.find(
                            (m) => m.id === reaction.mediaType,
                          )?.text || reaction.mediaType}
                        </Tag>
                      )}
                      {reaction.growthResult && (
                        <Tag
                          type={
                            reaction.growthResult === "GROWTH"
                              ? "green"
                              : reaction.growthResult === "NO_GROWTH"
                                ? "gray"
                                : reaction.growthResult === "CONTAMINATED"
                                  ? "red"
                                  : "purple"
                          }
                          size="sm"
                          style={{ marginLeft: "0.25rem" }}
                        >
                          {GROWTH_RESULTS.find(
                            (g) => g.id === reaction.growthResult,
                          )?.text || reaction.growthResult}
                        </Tag>
                      )}
                      {reaction.dstCompleted && (
                        <Tag
                          type="teal"
                          size="sm"
                          style={{ marginLeft: "0.25rem" }}
                        >
                          DST ({reaction.dstAntibioticResults.length})
                        </Tag>
                      )}
                    </h6>
                    <Button
                      kind="ghost"
                      size="sm"
                      hasIconOnly
                      renderIcon={TrashCan}
                      iconDescription="Remove"
                      onClick={() => handleRemoveMediaReaction(index)}
                    />
                  </div>

                  {/* Media Type Selection */}
                  <Grid fullWidth>
                    <Column lg={6} md={4} sm={4}>
                      <Dropdown
                        id={`media-type-${index}`}
                        titleText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.mediaType",
                          defaultMessage: "Media Type",
                        })}
                        label="Select media"
                        items={ISOLATION_MEDIA_TYPES}
                        itemToString={(item) => (item ? item.text : "")}
                        selectedItem={ISOLATION_MEDIA_TYPES.find(
                          (m) => m.id === reaction.mediaType,
                        )}
                        onChange={({ selectedItem }) =>
                          handleUpdateMediaReaction(
                            index,
                            "mediaType",
                            selectedItem?.id || "",
                          )
                        }
                        size="sm"
                      />
                    </Column>

                    {reaction.mediaType === "OTHER" && (
                      <Column lg={6} md={4} sm={4}>
                        <TextInput
                          id={`other-media-${index}`}
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.assay.otherMediaName",
                            defaultMessage: "Other Media Name",
                          })}
                          value={reaction.otherMediaName}
                          onChange={(e) =>
                            handleUpdateMediaReaction(
                              index,
                              "otherMediaName",
                              e.target.value,
                            )
                          }
                          size="sm"
                        />
                      </Column>
                    )}

                    <Column lg={4} md={2} sm={2}>
                      <TextInput
                        id={`inoculation-date-${index}`}
                        type="date"
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.inoculationDate",
                          defaultMessage: "Inoculation Date",
                        })}
                        value={reaction.inoculationDate}
                        onChange={(e) =>
                          handleUpdateMediaReaction(
                            index,
                            "inoculationDate",
                            e.target.value,
                          )
                        }
                        size="sm"
                      />
                    </Column>
                  </Grid>

                  {/* Growth Result and Colony Info */}
                  <Grid fullWidth style={{ marginTop: "0.5rem" }}>
                    <Column lg={4} md={2} sm={2}>
                      <Dropdown
                        id={`growth-result-${index}`}
                        titleText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.growthResult",
                          defaultMessage: "Growth Result",
                        })}
                        label="Select"
                        items={GROWTH_RESULTS}
                        itemToString={(item) => (item ? item.text : "")}
                        selectedItem={GROWTH_RESULTS.find(
                          (g) => g.id === reaction.growthResult,
                        )}
                        onChange={({ selectedItem }) =>
                          handleUpdateMediaReaction(
                            index,
                            "growthResult",
                            selectedItem?.id || "",
                          )
                        }
                        size="sm"
                      />
                    </Column>

                    {reaction.growthResult === "GROWTH" && (
                      <>
                        <Column lg={4} md={2} sm={2}>
                          <TextInput
                            id={`colony-count-${index}`}
                            labelText={intl.formatMessage({
                              id: "notebook.bacteriology.assay.colonyCount",
                              defaultMessage: "Colony Count",
                            })}
                            value={reaction.colonyCount}
                            onChange={(e) =>
                              handleUpdateMediaReaction(
                                index,
                                "colonyCount",
                                e.target.value,
                              )
                            }
                            placeholder="e.g., >100 CFU"
                            size="sm"
                          />
                        </Column>

                        <Column lg={4} md={2} sm={2}>
                          <TextInput
                            id={`organism-id-${index}`}
                            labelText={intl.formatMessage({
                              id: "notebook.bacteriology.assay.organismIdentified",
                              defaultMessage: "Organism Identified",
                            })}
                            value={reaction.organismIdentified}
                            onChange={(e) =>
                              handleUpdateMediaReaction(
                                index,
                                "organismIdentified",
                                e.target.value,
                              )
                            }
                            placeholder="e.g., E. coli"
                            size="sm"
                          />
                        </Column>

                        <Column lg={4} md={2} sm={2}>
                          <Dropdown
                            id={`hemolysis-${index}`}
                            titleText={intl.formatMessage({
                              id: "notebook.bacteriology.assay.hemolysis",
                              defaultMessage: "Hemolysis",
                            })}
                            label="Select"
                            items={[
                              { id: "ALPHA", text: "Alpha (α)" },
                              { id: "BETA", text: "Beta (β)" },
                              { id: "GAMMA", text: "Gamma (γ) - None" },
                              { id: "NA", text: "N/A" },
                            ]}
                            itemToString={(item) => (item ? item.text : "")}
                            selectedItem={
                              [
                                { id: "ALPHA", text: "Alpha (α)" },
                                { id: "BETA", text: "Beta (β)" },
                                { id: "GAMMA", text: "Gamma (γ) - None" },
                                { id: "NA", text: "N/A" },
                              ].find((h) => h.id === reaction.hemolysis) || null
                            }
                            onChange={({ selectedItem }) =>
                              handleUpdateMediaReaction(
                                index,
                                "hemolysis",
                                selectedItem?.id || "",
                              )
                            }
                            size="sm"
                          />
                        </Column>
                      </>
                    )}
                  </Grid>

                  {/* DST Button - Only show if growth is observed */}
                  {reaction.growthResult === "GROWTH" && (
                    <div style={{ marginTop: "0.75rem" }}>
                      <Button
                        kind={reaction.dstCompleted ? "secondary" : "tertiary"}
                        size="sm"
                        renderIcon={reaction.dstCompleted ? Edit : Add}
                        onClick={() => handleOpenMediaReactionDST(index)}
                      >
                        {reaction.dstCompleted ? (
                          <FormattedMessage
                            id="notebook.bacteriology.assay.editDST"
                            defaultMessage="Edit DST ({count} antibiotics)"
                            values={{
                              count: reaction.dstAntibioticResults.length,
                            }}
                          />
                        ) : (
                          <FormattedMessage
                            id="notebook.bacteriology.assay.addDST"
                            defaultMessage="Add DST Results"
                          />
                        )}
                      </Button>
                    </div>
                  )}

                  {/* Notes */}
                  <Grid fullWidth style={{ marginTop: "0.5rem" }}>
                    <Column lg={16} md={8} sm={4}>
                      <TextInput
                        id={`reaction-notes-${index}`}
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.notes",
                          defaultMessage: "Notes",
                        })}
                        value={reaction.notes}
                        onChange={(e) =>
                          handleUpdateMediaReaction(
                            index,
                            "notes",
                            e.target.value,
                          )
                        }
                        size="sm"
                      />
                    </Column>
                  </Grid>
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>

      {/* ==========================================
          MODALS - Section B.2: Media Reaction DST Sub-Modal
          ========================================== */}
      <Modal
        open={mediaReactionDstModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.mediaReactionDstTitle",
          defaultMessage: "Drug Susceptibility Testing for Media Reaction",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.saveDST",
          defaultMessage: "Save DST",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setMediaReactionDstModalOpen(false);
          setEditingMediaReactionIndex(null);
        }}
        onRequestSubmit={handleSaveMediaReactionDST}
        size="lg"
      >
        {editingMediaReactionIndex !== null &&
          mediaReactions[editingMediaReactionIndex] && (
            <div style={{ marginBottom: "1rem" }}>
              <p style={{ color: "#525252", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.modal.mediaReactionDstDescription"
                  defaultMessage="Record DST results for {mediaType}. Add Zone Diameter and/or MIC values with R/S/I interpretation."
                  values={{
                    mediaType:
                      ISOLATION_MEDIA_TYPES.find(
                        (m) =>
                          m.id ===
                          mediaReactions[editingMediaReactionIndex].mediaType,
                      )?.text || "this media",
                  }}
                />
              </p>

              {/* DST Method and Guidelines */}
              <Grid fullWidth>
                <Column lg={6} md={4} sm={4}>
                  <Dropdown
                    id="media-dst-method"
                    titleText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.dstMethod",
                      defaultMessage: "DST Method",
                    })}
                    label="Select method"
                    items={DST_METHODS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={DST_METHODS.find(
                      (m) =>
                        m.id ===
                        mediaReactions[editingMediaReactionIndex].dstMethod,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateMediaReaction(
                        editingMediaReactionIndex,
                        "dstMethod",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>

                <Column lg={5} md={4} sm={4}>
                  <Dropdown
                    id="media-dst-guidelines"
                    titleText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.guidelinesUsed",
                      defaultMessage: "Interpretation Guidelines",
                    })}
                    label="Select guidelines"
                    items={INTERPRETATION_GUIDELINES}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={INTERPRETATION_GUIDELINES.find(
                      (g) =>
                        g.id ===
                        mediaReactions[editingMediaReactionIndex]
                          .dstGuidelinesUsed,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateMediaReaction(
                        editingMediaReactionIndex,
                        "dstGuidelinesUsed",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>

                <Column lg={5} md={4} sm={4}>
                  <Dropdown
                    id="media-dst-panel"
                    titleText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.antibioticPanel",
                      defaultMessage: "Antibiotic Panel",
                    })}
                    label="Select panel"
                    items={ANTIBIOTIC_PANELS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={ANTIBIOTIC_PANELS.find(
                      (p) =>
                        p.id ===
                        mediaReactions[editingMediaReactionIndex]
                          .dstAntibioticPanel,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateMediaReaction(
                        editingMediaReactionIndex,
                        "dstAntibioticPanel",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
              </Grid>

              {/* Antibiotic Results Section */}
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
                  </h6>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={Add}
                    onClick={handleAddMediaReactionAntibiotic}
                  >
                    <FormattedMessage
                      id="notebook.bacteriology.assay.addAntibiotic"
                      defaultMessage="Add Antibiotic"
                    />
                  </Button>
                </div>

                {/* Column Headers */}
                {mediaReactions[editingMediaReactionIndex].dstAntibioticResults
                  .length > 0 && (
                  <Grid fullWidth style={{ marginBottom: "0.25rem" }}>
                    <Column lg={2} md={1} sm={1}>
                      <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                        Panel
                      </span>
                    </Column>
                    <Column lg={4} md={2} sm={1}>
                      <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                        Antibiotic
                      </span>
                    </Column>
                    <Column lg={2} md={1} sm={1}>
                      <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                        Zone (mm)
                      </span>
                    </Column>
                    <Column lg={3} md={2} sm={1}>
                      <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                        MIC (µg/mL)
                      </span>
                    </Column>
                    <Column lg={3} md={2} sm={1}>
                      <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                        Interpretation
                      </span>
                    </Column>
                    <Column lg={2} md={1} sm={1}></Column>
                  </Grid>
                )}

                {/* Antibiotic Rows */}
                {mediaReactions[
                  editingMediaReactionIndex
                ].dstAntibioticResults.map((result, abIndex) => (
                  <Grid
                    fullWidth
                    key={abIndex}
                    style={{ marginBottom: "0.5rem", alignItems: "flex-end" }}
                  >
                    <Column lg={2} md={1} sm={1}>
                      <Dropdown
                        id={`media-ab-panel-${abIndex}`}
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
                          handleUpdateMediaReactionAntibiotic(
                            abIndex,
                            "panelType",
                            selectedItem?.id || "1ST_LINE",
                          )
                        }
                        size="sm"
                      />
                    </Column>

                    <Column lg={4} md={2} sm={1}>
                      <TextInput
                        id={`media-ab-name-${abIndex}`}
                        value={result.antibiotic}
                        onChange={(e) =>
                          handleUpdateMediaReactionAntibiotic(
                            abIndex,
                            "antibiotic",
                            e.target.value,
                          )
                        }
                        placeholder="e.g., Ampicillin"
                        size="sm"
                      />
                    </Column>

                    <Column lg={2} md={1} sm={1}>
                      <TextInput
                        id={`media-ab-zone-${abIndex}`}
                        value={result.zoneDiameter || ""}
                        onChange={(e) =>
                          handleUpdateMediaReactionAntibiotic(
                            abIndex,
                            "zoneDiameter",
                            e.target.value,
                          )
                        }
                        placeholder="mm"
                        size="sm"
                      />
                    </Column>

                    <Column lg={3} md={2} sm={1}>
                      <TextInput
                        id={`media-ab-mic-${abIndex}`}
                        value={result.mic || ""}
                        onChange={(e) =>
                          handleUpdateMediaReactionAntibiotic(
                            abIndex,
                            "mic",
                            e.target.value,
                          )
                        }
                        placeholder="µg/mL"
                        size="sm"
                      />
                    </Column>

                    <Column lg={3} md={2} sm={1}>
                      <Dropdown
                        id={`media-ab-interp-${abIndex}`}
                        label="Select"
                        items={SUSCEPTIBILITY_INTERPRETATION}
                        itemToString={(item) => (item ? item.text : "")}
                        selectedItem={SUSCEPTIBILITY_INTERPRETATION.find(
                          (i) => i.id === result.interpretation,
                        )}
                        onChange={({ selectedItem }) =>
                          handleUpdateMediaReactionAntibiotic(
                            abIndex,
                            "interpretation",
                            selectedItem?.id || "",
                          )
                        }
                        size="sm"
                      />
                    </Column>

                    <Column lg={2} md={1} sm={1}>
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        iconDescription="Remove"
                        onClick={() =>
                          handleRemoveMediaReactionAntibiotic(abIndex)
                        }
                      />
                    </Column>
                  </Grid>
                ))}

                {mediaReactions[editingMediaReactionIndex].dstAntibioticResults
                  .length === 0 && (
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

              {/* DST Notes */}
              <Grid fullWidth style={{ marginTop: "1rem" }}>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="media-dst-notes"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.notes",
                      defaultMessage: "Notes",
                    })}
                    value={
                      mediaReactions[editingMediaReactionIndex].dstNotes || ""
                    }
                    onChange={(e) =>
                      handleUpdateMediaReaction(
                        editingMediaReactionIndex,
                        "dstNotes",
                        e.target.value,
                      )
                    }
                    rows={2}
                  />
                </Column>
              </Grid>
            </div>
          )}
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

              {/* Column Headers - Always show Zone, MIC, and Interpretation */}
              {antibioticResults.length > 0 && (
                <Grid fullWidth style={{ marginBottom: "0.25rem" }}>
                  <Column lg={2} md={1} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Panel
                    </span>
                  </Column>
                  <Column lg={4} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Antibiotic
                    </span>
                  </Column>
                  <Column lg={2} md={1} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Zone (mm)
                    </span>
                  </Column>
                  <Column lg={3} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      MIC (µg/mL)
                    </span>
                  </Column>
                  <Column lg={3} md={2} sm={1}>
                    <span style={{ fontSize: "0.75rem", fontWeight: "600" }}>
                      Interpretation
                    </span>
                  </Column>
                  <Column lg={2} md={1} sm={1}></Column>
                </Grid>
              )}

              {/* Antibiotic Result Rows - Always show Zone, MIC, and Interpretation */}
              {antibioticResults.map((result, index) => (
                <Grid
                  fullWidth
                  key={index}
                  style={{ marginBottom: "0.5rem", alignItems: "flex-end" }}
                >
                  {/* Panel Type */}
                  <Column lg={2} md={1} sm={1}>
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
                    <Dropdown
                      id={`antibiotic-name-${index}`}
                      items={antibiotics}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={antibiotics.find(
                        (a) => a.id === result.antibioticId,
                      )}
                      onChange={({ selectedItem }) => {
                        handleUpdateAntibioticResult(
                          index,
                          "antibiotic",
                          selectedItem?.text || "",
                        );
                        handleUpdateAntibioticResult(
                          index,
                          "antibioticId",
                          selectedItem?.id || "",
                        );
                      }}
                      disabled={loadingAntibiotics}
                      size="sm"
                    />
                  </Column>

                  {/* Zone Diameter - Always visible */}
                  <Column lg={2} md={1} sm={1}>
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
                      placeholder="mm"
                      size="sm"
                    />
                  </Column>

                  {/* MIC - Always visible */}
                  <Column lg={3} md={2} sm={1}>
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
                      placeholder="µg/mL"
                      size="sm"
                    />
                  </Column>

                  {/* Interpretation - S, I, R per CLSI/EUCAST */}
                  <Column lg={3} md={2} sm={1}>
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
            <Column lg={16} md={8} sm={4}>
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
          </Grid>

          {/* Primer, Enzyme, and Target Gene Structured Template */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#e0f7fa",
              borderRadius: "4px",
              border: "1px solid #00838f",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem", color: "#00695c" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.pcrTemplate"
                defaultMessage="PCR Structured Template"
              />
            </h6>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="pcr-primer-direction"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrPrimerDirection",
                    defaultMessage: "Primer Direction",
                  })}
                  label="Select direction"
                  items={PCR_PRIMER_DIRECTION}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={PCR_PRIMER_DIRECTION.find(
                    (p) => p.id === pcrData.primerDirection,
                  )}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      primerDirection: selectedItem?.id || "",
                    })
                  }
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="pcr-primer-type"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrPrimerType",
                    defaultMessage: "Primer Type",
                  })}
                  value={pcrData.primerType}
                  onChange={(e) =>
                    setPcrData({
                      ...pcrData,
                      primerType: e.target.value,
                    })
                  }
                  placeholder="e.g., 16S rRNA, mecA, vanA"
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="pcr-enzyme"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrEnzyme",
                    defaultMessage: "Enzyme/Master Mix",
                  })}
                  label="Select enzyme"
                  items={enzymes.length > 0 ? enzymes : PCR_ENZYMES}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={(enzymes.length > 0
                    ? enzymes
                    : PCR_ENZYMES
                  ).find((e) => e.id === pcrData.enzyme)}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      enzyme: selectedItem?.id || "",
                    })
                  }
                  disabled={loadingEnzymes}
                />
              </Column>

              {pcrData.enzyme === "OTHER" && (
                <Column lg={16} md={8} sm={4}>
                  <TextInput
                    id="pcr-other-enzyme"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.assay.otherEnzyme",
                      defaultMessage: "Specify Other Enzyme/Mix",
                    })}
                    value={pcrData.otherEnzyme}
                    onChange={(e) =>
                      setPcrData({
                        ...pcrData,
                        otherEnzyme: e.target.value,
                      })
                    }
                  />
                </Column>
              )}

              {/* Target Gene */}
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="pcr-target-gene"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrTargetGene",
                    defaultMessage: "Target Gene",
                  })}
                  value={pcrData.targetGene}
                  onChange={(e) =>
                    setPcrData({
                      ...pcrData,
                      targetGene: e.target.value,
                    })
                  }
                  placeholder="e.g., 16S rRNA, mecA, CTX-M"
                />
              </Column>

              {/* Primary Target Gene (kept for backward compatibility) */}
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="pcr-target"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.pcrTarget",
                    defaultMessage: "Primary Target Gene/Marker",
                  })}
                  label="Select target"
                  items={PCR_TARGETS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={PCR_TARGETS.find(
                    (t) => t.id === pcrData.target,
                  )}
                  onChange={({ selectedItem }) =>
                    setPcrData({
                      ...pcrData,
                      target: selectedItem?.id || "",
                    })
                  }
                />
              </Column>

              {pcrData.target === "OTHER" && (
                <Column lg={8} md={4} sm={4}>
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
            </Grid>
          </div>

          <Grid fullWidth style={{ marginTop: "1rem" }}>
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

          {/* Primary PCR Result */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#e8f5e9",
              borderRadius: "4px",
              border: "1px solid #4caf50",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem", color: "#2e7d32" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.primaryPcrResult"
                defaultMessage="Primary PCR Result"
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
          </div>

          {/* Multiple Gene Detection Results (Additional Targets) */}
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
                  id="notebook.bacteriology.assay.geneDetectionResults"
                  defaultMessage="Gene Detection Results"
                />
              </h6>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Add}
                onClick={handleAddGeneDetectionResult}
              >
                <FormattedMessage
                  id="notebook.bacteriology.assay.addGeneResult"
                  defaultMessage="Add Gene Target"
                />
              </Button>
            </div>

            {pcrData.geneDetectionResults.length === 0 ? (
              <p style={{ color: "#525252", fontStyle: "italic" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.noGeneResults"
                  defaultMessage="No gene targets added. Click 'Add Gene Target' to record detection results."
                />
              </p>
            ) : (
              pcrData.geneDetectionResults.map((geneResult, index) => (
                <div
                  key={geneResult.id}
                  style={{
                    padding: "0.75rem",
                    backgroundColor: "#ffffff",
                    borderRadius: "4px",
                    marginBottom: "0.5rem",
                    border: "1px solid #e0e0e0",
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
                    <span style={{ fontWeight: "500" }}>
                      <FormattedMessage
                        id="notebook.bacteriology.assay.geneTarget"
                        defaultMessage="Gene Target #{num}"
                        values={{ num: index + 1 }}
                      />
                    </span>
                    <Button
                      kind="danger--ghost"
                      size="sm"
                      renderIcon={TrashCan}
                      iconDescription="Remove"
                      hasIconOnly
                      onClick={() => handleRemoveGeneDetectionResult(index)}
                    />
                  </div>
                  <Grid fullWidth>
                    <Column lg={6} md={4} sm={4}>
                      <Dropdown
                        id={`gene-target-${index}`}
                        titleText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.targetGene",
                          defaultMessage: "Target Gene/Marker",
                        })}
                        label="Select target"
                        items={PCR_TARGETS}
                        itemToString={(item) => (item ? item.text : "")}
                        selectedItem={PCR_TARGETS.find(
                          (t) => t.id === geneResult.target,
                        )}
                        onChange={({ selectedItem }) =>
                          handleUpdateGeneDetectionResult(
                            index,
                            "target",
                            selectedItem?.id || "",
                          )
                        }
                        size="sm"
                      />
                    </Column>
                    {geneResult.target === "OTHER" && (
                      <Column lg={6} md={4} sm={4}>
                        <TextInput
                          id={`gene-other-target-${index}`}
                          labelText={intl.formatMessage({
                            id: "notebook.bacteriology.assay.otherTarget",
                            defaultMessage: "Specify Target",
                          })}
                          value={geneResult.otherTarget}
                          onChange={(e) =>
                            handleUpdateGeneDetectionResult(
                              index,
                              "otherTarget",
                              e.target.value,
                            )
                          }
                          size="sm"
                        />
                      </Column>
                    )}
                    <Column lg={4} md={2} sm={2}>
                      <TextInput
                        id={`gene-ct-value-${index}`}
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.ctValue",
                          defaultMessage: "Ct Value",
                        })}
                        value={geneResult.ctValue}
                        onChange={(e) =>
                          handleUpdateGeneDetectionResult(
                            index,
                            "ctValue",
                            e.target.value,
                          )
                        }
                        placeholder="e.g., 25.5"
                        size="sm"
                      />
                    </Column>
                    <Column lg={4} md={2} sm={2}>
                      <Dropdown
                        id={`gene-result-${index}`}
                        titleText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.result",
                          defaultMessage: "Result",
                        })}
                        label="Select"
                        items={PCR_RESULTS}
                        itemToString={(item) => (item ? item.text : "")}
                        selectedItem={PCR_RESULTS.find(
                          (r) => r.id === geneResult.result,
                        )}
                        onChange={({ selectedItem }) =>
                          handleUpdateGeneDetectionResult(
                            index,
                            "result",
                            selectedItem?.id || "",
                          )
                        }
                        size="sm"
                      />
                    </Column>
                    <Column lg={16} md={8} sm={4}>
                      <TextInput
                        id={`gene-notes-${index}`}
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.assay.geneNotes",
                          defaultMessage: "Notes for this target",
                        })}
                        value={geneResult.notes}
                        onChange={(e) =>
                          handleUpdateGeneDetectionResult(
                            index,
                            "notes",
                            e.target.value,
                          )
                        }
                        size="sm"
                      />
                    </Column>
                  </Grid>
                </div>
              ))
            )}

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

          {/* Attachments Section */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#fff8e1",
              borderRadius: "4px",
              border: "1px solid #ffb300",
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
              <h6 style={{ color: "#e65100" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.attachments"
                  defaultMessage="Attachments (Images, Text, Files)"
                />
              </h6>
              <label htmlFor="pcr-file-upload">
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Add}
                  onClick={() =>
                    document.getElementById("pcr-file-upload").click()
                  }
                >
                  <FormattedMessage
                    id="notebook.bacteriology.assay.addAttachment"
                    defaultMessage="Add Attachment"
                  />
                </Button>
              </label>
              <input
                type="file"
                id="pcr-file-upload"
                style={{ display: "none" }}
                multiple
                accept="image/*,.pdf,.txt,.doc,.docx,.xls,.xlsx,.csv"
                onChange={handlePcrFileUpload}
              />
            </div>

            {pcrData.attachments.length === 0 ? (
              <p style={{ color: "#525252", fontStyle: "italic" }}>
                <FormattedMessage
                  id="notebook.bacteriology.assay.noAttachments"
                  defaultMessage="No attachments added. Supported: images, PDFs, text files, documents."
                />
              </p>
            ) : (
              <div>
                {pcrData.attachments.map((attachment, index) => (
                  <div
                    key={attachment.id}
                    style={{
                      padding: "0.5rem",
                      backgroundColor: "#ffffff",
                      borderRadius: "4px",
                      marginBottom: "0.5rem",
                      border: "1px solid #e0e0e0",
                      display: "flex",
                      alignItems: "center",
                      gap: "1rem",
                    }}
                  >
                    <div style={{ flex: "0 0 auto" }}>
                      {attachment.type?.startsWith("image/") ? (
                        <Tag type="cyan" size="sm">
                          Image
                        </Tag>
                      ) : attachment.type === "application/pdf" ? (
                        <Tag type="red" size="sm">
                          PDF
                        </Tag>
                      ) : attachment.type?.startsWith("text/") ? (
                        <Tag type="green" size="sm">
                          Text
                        </Tag>
                      ) : (
                        <Tag type="gray" size="sm">
                          File
                        </Tag>
                      )}
                    </div>
                    <div style={{ flex: "1 1 auto" }}>
                      <div style={{ fontWeight: "500", fontSize: "0.875rem" }}>
                        {attachment.name}
                      </div>
                      <div style={{ color: "#525252", fontSize: "0.75rem" }}>
                        {(attachment.size / 1024).toFixed(1)} KB
                      </div>
                    </div>
                    <div style={{ flex: "1 1 auto" }}>
                      <TextInput
                        id={`attachment-description-${index}`}
                        labelText=""
                        placeholder={intl.formatMessage({
                          id: "notebook.bacteriology.assay.attachmentDescription",
                          defaultMessage: "Description (optional)",
                        })}
                        value={attachment.description}
                        onChange={(e) =>
                          handleUpdatePcrAttachmentDescription(
                            index,
                            e.target.value,
                          )
                        }
                        size="sm"
                      />
                    </div>
                    <div style={{ flex: "0 0 auto" }}>
                      <Button
                        kind="danger--ghost"
                        size="sm"
                        renderIcon={TrashCan}
                        iconDescription="Remove"
                        hasIconOnly
                        onClick={() => handleRemovePcrAttachment(index)}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
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

      {/* ==========================================
          MODAL - Molecular QC
          ========================================== */}
      <Modal
        open={molecularQcModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.molecularQcTitle",
          defaultMessage: "Molecular Quality Control",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.bacteriology.assay.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setMolecularQcModalOpen(false)}
        onRequestSubmit={handleSaveMolecularQcData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.assay.modal.molecularQcDescription"
              defaultMessage="Perform quality control checks for {count} selected sample(s) before molecular testing."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* QC Date and Performer */}
          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                value={molecularQcData.qcDate}
                onChange={([date]) =>
                  setMolecularQcData({
                    ...molecularQcData,
                    qcDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="molecular-qc-date"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.qcDate",
                    defaultMessage: "QC Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="molecular-qc-performed-by"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.assay.qcPerformedBy",
                  defaultMessage: "QC Performed By",
                })}
                value={molecularQcData.qcPerformedBy}
                onChange={(e) =>
                  setMolecularQcData({
                    ...molecularQcData,
                    qcPerformedBy: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          {/* Section 1: Reagent/Kit QC */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.reagentQc"
                defaultMessage="1. Reagent/Kit QC"
              />
            </h6>

            {molecularQcData.reagentQcChecks.map((check, index) => (
              <Grid
                fullWidth
                key={`reagent-qc-${index}`}
                style={{
                  marginBottom: "0.5rem",
                  paddingBottom: "0.5rem",
                  borderBottom:
                    index < molecularQcData.reagentQcChecks.length - 1
                      ? "1px solid #e0e0e0"
                      : "none",
                }}
              >
                <Column lg={4} md={4} sm={4}>
                  <Dropdown
                    id={`reagent-type-${index}`}
                    titleText="Reagent Type"
                    label="Select reagent"
                    size="sm"
                    items={MOLECULAR_REAGENT_TYPES}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={MOLECULAR_REAGENT_TYPES.find(
                      (r) => r.id === check.reagentType,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateReagentQcCheck(
                        index,
                        "reagentType",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id={`reagent-lot-${index}`}
                    labelText="Lot Number"
                    size="sm"
                    value={check.lotNumber}
                    onChange={(e) =>
                      handleUpdateReagentQcCheck(
                        index,
                        "lotNumber",
                        e.target.value,
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id={`reagent-expiry-${index}`}
                    labelText="Expiry Date"
                    size="sm"
                    type="date"
                    value={check.expiryDate}
                    onChange={(e) =>
                      handleUpdateReagentQcCheck(
                        index,
                        "expiryDate",
                        e.target.value,
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <Dropdown
                    id={`reagent-status-${index}`}
                    titleText="QC Status"
                    label="Select status"
                    size="sm"
                    items={REAGENT_QC_STATUS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={REAGENT_QC_STATUS.find(
                      (s) => s.id === check.qcStatus,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateReagentQcCheck(
                        index,
                        "qcStatus",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={2} md={2} sm={2}>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    iconDescription="Remove"
                    hasIconOnly
                    onClick={() => handleRemoveReagentQcCheck(index)}
                    disabled={molecularQcData.reagentQcChecks.length <= 1}
                    style={{ marginTop: "1.5rem" }}
                  />
                </Column>
                {check.qcStatus === "FAILED" && (
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id={`reagent-failure-${index}`}
                      titleText="Failure Reason"
                      label="Select reason"
                      size="sm"
                      items={REAGENT_QC_FAILURE_REASONS}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={REAGENT_QC_FAILURE_REASONS.find(
                        (r) => r.id === check.failureReason,
                      )}
                      onChange={({ selectedItem }) =>
                        handleUpdateReagentQcCheck(
                          index,
                          "failureReason",
                          selectedItem?.id || "",
                        )
                      }
                    />
                  </Column>
                )}
              </Grid>
            ))}

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Add}
              onClick={handleAddReagentQcCheck}
            >
              Add Reagent Check
            </Button>
          </div>

          {/* Section 2: Equipment QC */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.equipmentQc"
                defaultMessage="2. Equipment QC"
              />
            </h6>

            {molecularQcData.equipmentQcChecks.map((check, index) => (
              <Grid
                fullWidth
                key={`equipment-qc-${index}`}
                style={{
                  marginBottom: "0.5rem",
                  paddingBottom: "0.5rem",
                  borderBottom:
                    index < molecularQcData.equipmentQcChecks.length - 1
                      ? "1px solid #e0e0e0"
                      : "none",
                }}
              >
                <Column lg={4} md={4} sm={4}>
                  <Dropdown
                    id={`equipment-type-${index}`}
                    titleText="Equipment Type"
                    label="Select equipment"
                    size="sm"
                    items={MOLECULAR_EQUIPMENT_TYPES}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={MOLECULAR_EQUIPMENT_TYPES.find(
                      (e) => e.id === check.equipmentType,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateEquipmentQcCheck(
                        index,
                        "equipmentType",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id={`equipment-id-${index}`}
                    labelText="Equipment ID"
                    size="sm"
                    value={check.equipmentId}
                    onChange={(e) =>
                      handleUpdateEquipmentQcCheck(
                        index,
                        "equipmentId",
                        e.target.value,
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id={`equipment-cal-date-${index}`}
                    labelText="Last Calibration"
                    size="sm"
                    type="date"
                    value={check.lastCalibrationDate}
                    onChange={(e) =>
                      handleUpdateEquipmentQcCheck(
                        index,
                        "lastCalibrationDate",
                        e.target.value,
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <Dropdown
                    id={`equipment-status-${index}`}
                    titleText="QC Status"
                    label="Select status"
                    size="sm"
                    items={EQUIPMENT_QC_STATUS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={EQUIPMENT_QC_STATUS.find(
                      (s) => s.id === check.qcStatus,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateEquipmentQcCheck(
                        index,
                        "qcStatus",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={2} md={2} sm={2}>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    iconDescription="Remove"
                    hasIconOnly
                    onClick={() => handleRemoveEquipmentQcCheck(index)}
                    disabled={molecularQcData.equipmentQcChecks.length <= 1}
                    style={{ marginTop: "1.5rem" }}
                  />
                </Column>
                {(check.qcStatus === "FAILED" || check.qcStatus === "DUE") && (
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id={`equipment-failure-${index}`}
                      titleText="Failure Reason"
                      label="Select reason"
                      size="sm"
                      items={EQUIPMENT_QC_FAILURE_REASONS}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={EQUIPMENT_QC_FAILURE_REASONS.find(
                        (r) => r.id === check.failureReason,
                      )}
                      onChange={({ selectedItem }) =>
                        handleUpdateEquipmentQcCheck(
                          index,
                          "failureReason",
                          selectedItem?.id || "",
                        )
                      }
                    />
                  </Column>
                )}
              </Grid>
            ))}

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Add}
              onClick={handleAddEquipmentQcCheck}
            >
              Add Equipment Check
            </Button>
          </div>

          {/* Section 3: Sample QC */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.sampleQc"
                defaultMessage="3. Sample QC"
              />
            </h6>

            {molecularQcData.sampleQcChecks.map((check, index) => (
              <Grid
                fullWidth
                key={`sample-qc-${index}`}
                style={{
                  marginBottom: "0.5rem",
                  paddingBottom: "0.5rem",
                  borderBottom:
                    index < molecularQcData.sampleQcChecks.length - 1
                      ? "1px solid #e0e0e0"
                      : "none",
                }}
              >
                <Column lg={4} md={4} sm={4}>
                  <Dropdown
                    id={`sample-criterion-${index}`}
                    titleText="QC Criterion"
                    label="Select criterion"
                    size="sm"
                    items={SAMPLE_QC_CRITERIA}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={SAMPLE_QC_CRITERIA.find(
                      (c) => c.id === check.criterion,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateSampleQcCheck(
                        index,
                        "criterion",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id={`sample-value-${index}`}
                    labelText="Value"
                    size="sm"
                    value={check.value}
                    onChange={(e) =>
                      handleUpdateSampleQcCheck(index, "value", e.target.value)
                    }
                  />
                </Column>
                <Column lg={2} md={2} sm={2}>
                  <TextInput
                    id={`sample-unit-${index}`}
                    labelText="Unit"
                    size="sm"
                    value={check.unit}
                    onChange={(e) =>
                      handleUpdateSampleQcCheck(index, "unit", e.target.value)
                    }
                  />
                </Column>
                <Column lg={3} md={2} sm={2}>
                  <Dropdown
                    id={`sample-status-${index}`}
                    titleText="QC Status"
                    label="Select status"
                    size="sm"
                    items={SAMPLE_QC_STATUS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={SAMPLE_QC_STATUS.find(
                      (s) => s.id === check.qcStatus,
                    )}
                    onChange={({ selectedItem }) =>
                      handleUpdateSampleQcCheck(
                        index,
                        "qcStatus",
                        selectedItem?.id || "",
                      )
                    }
                  />
                </Column>
                <Column lg={2} md={2} sm={2}>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    iconDescription="Remove"
                    hasIconOnly
                    onClick={() => handleRemoveSampleQcCheck(index)}
                    disabled={molecularQcData.sampleQcChecks.length <= 1}
                    style={{ marginTop: "1.5rem" }}
                  />
                </Column>
                {check.qcStatus === "FAILED" && (
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id={`sample-failure-${index}`}
                      titleText="Failure Reason"
                      label="Select reason"
                      size="sm"
                      items={SAMPLE_QC_FAILURE_REASONS}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={SAMPLE_QC_FAILURE_REASONS.find(
                        (r) => r.id === check.failureReason,
                      )}
                      onChange={({ selectedItem }) =>
                        handleUpdateSampleQcCheck(
                          index,
                          "failureReason",
                          selectedItem?.id || "",
                        )
                      }
                    />
                  </Column>
                )}
              </Grid>
            ))}

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Add}
              onClick={handleAddSampleQcCheck}
            >
              Add Sample QC Check
            </Button>
          </div>

          {/* Section 4: Control Results */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h6 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.controlResults"
                defaultMessage="4. Control Results"
              />
            </h6>

            <Grid fullWidth>
              <Column lg={5} md={4} sm={4}>
                <Dropdown
                  id="positive-control-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.positiveControl",
                    defaultMessage: "Positive Control",
                  })}
                  label="Select result"
                  items={[
                    { id: "PASSED", text: "Passed" },
                    { id: "FAILED", text: "Failed" },
                    { id: "NOT_RUN", text: "Not Run" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={
                    [
                      { id: "PASSED", text: "Passed" },
                      { id: "FAILED", text: "Failed" },
                      { id: "NOT_RUN", text: "Not Run" },
                    ].find(
                      (c) => c.id === molecularQcData.positiveControlResult,
                    ) || null
                  }
                  onChange={({ selectedItem }) =>
                    setMolecularQcData({
                      ...molecularQcData,
                      positiveControlResult: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                <Dropdown
                  id="negative-control-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.negativeControl",
                    defaultMessage: "Negative Control",
                  })}
                  label="Select result"
                  items={[
                    { id: "PASSED", text: "Passed" },
                    { id: "FAILED", text: "Failed" },
                    { id: "NOT_RUN", text: "Not Run" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={
                    [
                      { id: "PASSED", text: "Passed" },
                      { id: "FAILED", text: "Failed" },
                      { id: "NOT_RUN", text: "Not Run" },
                    ].find(
                      (c) => c.id === molecularQcData.negativeControlResult,
                    ) || null
                  }
                  onChange={({ selectedItem }) =>
                    setMolecularQcData({
                      ...molecularQcData,
                      negativeControlResult: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                <Dropdown
                  id="internal-control-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.internalControl",
                    defaultMessage: "Internal Control",
                  })}
                  label="Select result"
                  items={[
                    { id: "PASSED", text: "Passed" },
                    { id: "FAILED", text: "Failed" },
                    { id: "NOT_RUN", text: "Not Run" },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={
                    [
                      { id: "PASSED", text: "Passed" },
                      { id: "FAILED", text: "Failed" },
                      { id: "NOT_RUN", text: "Not Run" },
                    ].find(
                      (c) => c.id === molecularQcData.internalControlResult,
                    ) || null
                  }
                  onChange={({ selectedItem }) =>
                    setMolecularQcData({
                      ...molecularQcData,
                      internalControlResult: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
            </Grid>
          </div>

          {/* Section 5: Overall QC Result */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#e8f5e9",
              borderRadius: "4px",
              border: "1px solid #4caf50",
            }}
          >
            <h6 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.assay.overallQcResult"
                defaultMessage="5. Overall QC Result"
              />
            </h6>

            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="overall-qc-result"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.overallResult",
                    defaultMessage: "Overall QC Result",
                  })}
                  label="Select result (auto-determined if left blank)"
                  items={MOLECULAR_QC_RESULTS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={MOLECULAR_QC_RESULTS.find(
                    (r) => r.id === molecularQcData.overallQcResult,
                  )}
                  onChange={({ selectedItem }) =>
                    setMolecularQcData({
                      ...molecularQcData,
                      overallQcResult: selectedItem?.id || "",
                    })
                  }
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="overall-qc-notes"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.assay.qcNotes",
                    defaultMessage: "QC Notes",
                  })}
                  value={molecularQcData.overallQcNotes}
                  onChange={(e) =>
                    setMolecularQcData({
                      ...molecularQcData,
                      overallQcNotes: e.target.value,
                    })
                  }
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

export default BacteriologyAssayTestExecutionPage;
