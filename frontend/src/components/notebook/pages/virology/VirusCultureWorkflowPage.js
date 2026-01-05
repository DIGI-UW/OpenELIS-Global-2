import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  ProgressIndicator,
  ProgressStep,
  Modal,
  Form,
  FormGroup,
  TextInput,
  Select,
  SelectItem,
  TextArea,
  Tag,
  Loading,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  OverflowMenu,
  OverflowMenuItem,
  IconButton,
} from "@carbon/react";
import {
  Add,
  View,
  Play,
  Checkmark,
  Warning,
  Error,
  Time,
  Microscope,
  Chemistry,
  Temperature,
  CheckmarkFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import "../../workflow/NotebookWorkflow.css";

/**
 * Stage 2: Virus Culture Growth Workflow Page
 * Implements the 9-step virus culture process from media preparation to packaging
 */
const VirusCultureWorkflowPage = ({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookEntry,
}) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [loading, setLoading] = useState(false);
  const [cultureBatches, setCultureBatches] = useState([]);
  const [selectedBatch, setSelectedBatch] = useState(null);
  const [workflowStatus, setWorkflowStatus] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showStepModal, setShowStepModal] = useState(false);
  const [currentStep, setCurrentStep] = useState(null);
  const [dashboardData, setDashboardData] = useState({});
  const [activeTab, setActiveTab] = useState(0);

  // Form states
  const [createBatchForm, setCreateBatchForm] = useState({
    notebookPageSampleId: null,
    virusStrain: "",
    cellLine: "",
    notes: "",
  });

  const [stepForm, setStepForm] = useState({
    qualityResult: "NOT_APPLICABLE",
    notes: "",
    // LMIS tracking fields
    materialsUsed: [],
    equipmentUsed: [],
    lotNumbers: {},
    measurements: {},
    conditions: {},
    qcResults: {},
    personnelInvolved: "",
    startTime: "",
    endTime: "",
    temperature: "",
    co2Level: "",
    phLevel: "",
    sterilizationParams: {},
    imageData: {},
  });

  // Available options (would normally come from API)
  const virusStrains = [
    "SARS-CoV-2",
    "Influenza A (H1N1)",
    "Influenza A (H3N2)",
    "Influenza B",
    "Respiratory Syncytial Virus (RSV)",
    "Adenovirus",
    "Human Metapneumovirus",
    "Parainfluenza Virus",
  ];

  const cellLines = [
    "Vero E6",
    "Vero CCL-81",
    "MDCK",
    "A549",
    "HEp-2",
    "LLC-MK2",
    "HELA",
    "BHK-21",
  ];

  // Enhanced 9-step workflow configuration based on PDF Stage 2 requirements
  const workflowSteps = [
    {
      name: "MEDIA_PREPARATION",
      label: "Media Preparation",
      description:
        "Select media, reagents, equipment | LMIS: Log materials used (type, lot number, expiry)",
      icon: Chemistry,
      order: 1,
      qcRequired: true,
      pdfRequirement: "Select media, reagents, equipment",
      lmisRequirement: "Log materials used (type, lot number, expiry)",
      // Core culture media components
      mediaTypes: [
        "DMEM (High Glucose)",
        "RPMI-1640",
        "MEM",
        "Opti-MEM",
        "EMEM",
      ],
      reagents: [
        "Fetal Bovine Serum (FBS)",
        "Penicillin/Streptomycin (P/S)",
        "L-Glutamine (200mM)",
        "HEPES Buffer (1M)",
        "Sodium Bicarbonate",
        "Non-Essential Amino Acids (NEAA)",
        "Sodium Pyruvate",
        "2-Mercaptoethanol",
      ],
      equipment: [
        "pH Meter",
        "Osmometer",
        "Sterile Bottles",
        "Graduated Cylinders",
        "Magnetic Stirrer",
      ],
      criticalParams: [
        "pH (7.2-7.4)",
        "Osmolality (280-320 mOsm/kg)",
        "Sterility",
        "Endotoxin <0.1 EU/mL",
      ],
      viabilityChecks: [
        "Visual clarity",
        "pH verification",
        "Osmolality measurement",
        "Endotoxin testing",
      ],
    },
    {
      name: "STERILIZATION",
      label: "Sterilization",
      description:
        "Autoclaving, filtration | LMIS: Record sterilization parameters (temp, time, pressure)",
      icon: Temperature,
      order: 2,
      qcRequired: true,
      pdfRequirement: "Autoclaving, filtration",
      lmisRequirement: "Record sterilization parameters (temp, time, pressure)",
      // Sterilization methods based on material type
      methods: [
        "Steam Autoclaving (121°C, 15 min, 15 PSI)",
        "Dry Heat Sterilization (160°C, 2 hours)",
        "Filter Sterilization (0.22μm, 0.1μm)",
        "Gamma Irradiation (25-50 kGy)",
        "UV Sterilization (254nm, 30-60 min)",
      ],
      equipment: [
        "Steam Autoclave",
        "Dry Heat Oven",
        "Filter Units (0.22μm)",
        "Laminar Flow Hood",
        "UV Chamber",
      ],
      // Critical validation parameters
      validationParams: [
        "Temperature Profile (±2°C tolerance)",
        "Time Duration (±5% tolerance)",
        "Pressure Monitoring (±10% tolerance)",
        "Steam Quality (dry saturated steam)",
        "Load Configuration (proper spacing)",
        "Biological Indicators (spore strips)",
        "Chemical Indicators (heat-sensitive tape)",
        "Physical Monitoring (charts/data loggers)",
      ],
      biologicalIndicators: [
        "Geobacillus stearothermophilus spores",
        "Bacillus atrophaeus spores",
      ],
      criticalParams: [
        "Temperature",
        "Time",
        "Pressure",
        "Biological Indicator Results",
        "Filter Integrity",
      ],
      acceptanceCriteria: [
        "BI: No growth after 48h incubation",
        "CI: Proper color change",
        "Physical: Parameters within limits",
      ],
    },
    {
      name: "CELL_CULTURE",
      label: "Cell Culture",
      description:
        "Grow host cells | LMIS: Track cell line, passage number, growth conditions",
      icon: Microscope,
      order: 3,
      qcRequired: true,
      pdfRequirement: "Grow host cells",
      lmisRequirement: "Track cell line, passage number, growth conditions",
      // Host cell lines commonly used in virology
      hostCellLines: [
        "Vero E6 (African Green Monkey Kidney)",
        "Vero CCL-81 (Cercopithecus aethiops)",
        "MDCK (Madin-Darby Canine Kidney)",
        "A549 (Human Lung Carcinoma)",
        "HEp-2 (Human Laryngeal Carcinoma)",
        "LLC-MK2 (Rhesus Monkey Kidney)",
        "BHK-21 (Baby Hamster Kidney)",
        "HELA (Human Cervical Carcinoma)",
        "293T (Human Embryonic Kidney)",
        "CHO (Chinese Hamster Ovary)",
      ],
      // Optimal growth conditions for virus culture
      growthConditions: [
        "Temperature: 37°C ±0.5°C",
        "CO₂: 5% ±0.2% (for bicarbonate buffered media)",
        "Humidity: 95% ±5%",
        "pH: 7.2-7.4 (monitored continuously)",
        "Atmospheric Pressure: Standard",
        "Incubator Calibration: Monthly verification",
      ],
      // Cell culture monitoring parameters
      monitoringParams: [
        "Cell Confluence (70-90% for infection)",
        "Cell Viability (>95% for infection)",
        "Doubling Time (species-specific)",
        "Cell Morphology (adherent vs. suspension)",
        "Passage Number (low passage preferred)",
        "Contamination Check (bacterial, fungal, mycoplasma)",
        "Growth Rate (exponential phase preferred)",
        "Media pH and Color Changes",
      ],
      qualityChecks: [
        "Mycoplasma Testing (PCR/Culture)",
        "Sterility Testing (14-day incubation)",
        "Cell Line Authentication (STR profiling)",
        "Viability Assessment (Trypan Blue)",
        "Morphology Evaluation (Light microscopy)",
        "Growth Curve Analysis",
        "Karyotype Analysis (if required)",
      ],
      criticalParams: [
        "Cell Viability >95%",
        "Confluence 70-90%",
        "Mycoplasma Negative",
        "Contamination Free",
      ],
      acceptanceCriteria: [
        "Viability ≥95%",
        "Confluence 70-90%",
        "Normal morphology",
        "Sterile",
        "Mycoplasma negative",
      ],
    },
    {
      name: "QUALITY_CONTROL",
      label: "Quality Control",
      description:
        "Validate cell viability and sterility | LMIS: Log QC results (viability %, sterility pass/fail)",
      icon: CheckmarkFilled,
      order: 4,
      qcRequired: true,
      pdfRequirement: "Validate cell viability and sterility",
      lmisRequirement: "Log QC results (viability %, sterility pass/fail)",
      // Comprehensive QC testing panel for virus culture
      qcTests: [
        "Cell Viability Assay (Trypan Blue/MTT/ATP)",
        "Sterility Testing (USP <71> 14-day incubation)",
        "Mycoplasma Testing (PCR + Culture)",
        "Endotoxin Testing (LAL/Kinetic Chromogenic)",
        "Osmolality Measurement",
        "pH Verification",
        "Particulate Matter Testing (USP <788>)",
        "Container Closure Integrity (if applicable)",
        "Bioburden Testing (pre-sterilization)",
        "Growth Promotion Testing (media validation)",
      ],
      // Critical viability thresholds for virus culture
      viabilityMethods: [
        "Trypan Blue Exclusion (manual/automated)",
        "MTT/MTS Metabolic Assay",
        "ATP Bioluminescence",
        "Flow Cytometry (7-AAD/PI staining)",
        "Resazurin Reduction Assay",
        "Calcein-AM/EthD-1 Live/Dead Staining",
      ],
      // Sterility testing protocols
      sterilityProtocols: [
        "USP <71> Sterility Testing",
        "Direct Inoculation Method",
        "Membrane Filtration Method",
        "Thioglycolate Medium (anaerobic bacteria)",
        "Soybean-Casein Digest Medium (aerobic bacteria/fungi)",
        "14-day incubation at 30-35°C and 20-25°C",
        "Growth Promotion Testing (GPT) validation",
        "Negative Control Verification",
      ],
      // Mycoplasma detection methods
      mycoplasmaDetection: [
        "Direct PCR (16S rRNA gene)",
        "Culture Method (PPLO broth/agar)",
        "DNA Staining (Hoechst 33258)",
        "Biochemical Tests (arginine/glucose)",
        "Commercial Kits (MycoAlert, PlasmoTest)",
        "Real-time PCR (quantitative)",
      ],
      // Critical acceptance criteria
      acceptanceCriteria: [
        "Cell Viability ≥95% (pre-infection)",
        "Cell Viability ≥85% (post-infection acceptable)",
        "Sterility: No growth after 14 days",
        "Mycoplasma: Negative by PCR and culture",
        "Endotoxin: <0.1 EU/mL (for sensitive applications)",
        "pH: 7.2-7.4",
        "Osmolality: 280-320 mOsm/kg",
        "Particulates: Within USP limits",
      ],
      criticalParams: [
        "Cell Viability %",
        "Sterility Result",
        "Mycoplasma Status",
        "Endotoxin Level",
      ],
    },
    {
      name: "VIRUS_CULTURE",
      label: "Virus Inoculation",
      description:
        "Inoculate cells with virus | LMIS: Record virus strain, culture conditions (temp, CO₂, duration)",
      icon: Chemistry,
      order: 5,
      qcRequired: false,
      pdfRequirement: "Inoculate cells with virus",
      lmisRequirement:
        "Record virus strain, culture conditions (temp, CO₂, duration)",
      // Virus inoculation protocols and parameters
      inoculationMethods: [
        "Direct Cell Inoculation (adherent cells)",
        "Suspension Culture Infection",
        "Microcarrier-based Infection",
        "Perfusion System Infection",
        "Fed-batch Infection Protocol",
      ],
      // Critical inoculation parameters
      inoculationParams: [
        "Multiplicity of Infection (MOI): 0.001-10",
        "Inoculum Volume: 1-10% of culture volume",
        "Adsorption Time: 30-120 minutes",
        "Adsorption Temperature: 37°C or RT",
        "Virus Stock Titer: Pre-determined TCID50/mL",
        "Cell Density at Infection: 1-5 × 10⁶ cells/mL",
        "Cell Confluence: 70-90% for adherent cells",
      ],
      // Virus strain tracking and documentation
      virusStrainData: [
        "Virus Type/Species (e.g., SARS-CoV-2, Influenza A)",
        "Strain Designation (e.g., D614G, Delta, Omicron)",
        "Passage History (original isolation source)",
        "Passage Number (P1, P2, etc.)",
        "Storage Conditions (-80°C, liquid N₂)",
        "Titer at Time of Use (TCID50/mL, PFU/mL)",
        "Lot Number/Batch ID",
        "Expiration Date",
        "Genetic Stability (sequencing data if available)",
      ],
      // Culture conditions during infection
      cultureConditions: [
        "Temperature: Virus-specific (33-37°C)",
        "CO₂: 5% ±0.2% (bicarbonate buffered)",
        "Humidity: 95% ±5%",
        "pH: 7.2-7.4 (may shift during infection)",
        "Serum Concentration: Reduced (2-5% FBS)",
        "Incubation Duration: 1-7 days (virus-dependent)",
        "Agitation: Minimal (for adherent cells)",
        "Light Exposure: Minimize (some viruses photosensitive)",
      ],
      // CPE monitoring and scoring
      cpeMonitoring: [
        "CPE Score 0: No visible changes",
        "CPE Score 1+: <25% cells affected",
        "CPE Score 2+: 25-50% cells affected",
        "CPE Score 3+: 50-75% cells affected",
        "CPE Score 4+: >75% cells affected",
        "Morphological Changes: Rounding, detachment",
        "Syncytia Formation: Multinucleated giant cells",
        "Inclusion Bodies: Intracellular viral factories",
        "Cell Lysis: Complete cell destruction",
        "Plaque Formation: Localized cell death",
      ],
      criticalParams: [
        "MOI",
        "Virus Titer",
        "CPE Score",
        "Incubation Temperature",
        "Time Post-Infection",
      ],
      monitoringSchedule: [
        "2 hours",
        "6 hours",
        "24 hours",
        "48 hours",
        "72 hours",
        "Daily thereafter",
      ],
    },
    {
      name: "DARK_ROOM_IMAGING",
      label: "Dark Room Imaging",
      description:
        "Imaging or fluorescence analysis | LMIS: Store image data, analysis results (CPE observation, fluorescence intensity)",
      icon: View,
      order: 6,
      qcRequired: false,
      pdfRequirement: "Imaging or fluorescence analysis",
      lmisRequirement:
        "Store image data, analysis results (CPE observation, fluorescence intensity)",
      // Advanced imaging techniques for virus culture
      imagingMethods: [
        "Bright-field Microscopy (CPE observation)",
        "Phase-contrast Microscopy (cell morphology)",
        "Fluorescence Microscopy (viral proteins/nucleic acids)",
        "Confocal Microscopy (3D viral localization)",
        "Live Cell Imaging (real-time infection)",
        "Time-lapse Photography (CPE progression)",
        "High-Content Imaging (automated analysis)",
        "Transmission Electron Microscopy (viral morphology)",
      ],
      // Imaging equipment and setup
      imagingEquipment: [
        "Inverted Fluorescence Microscope",
        "Digital Camera Systems (CCD/CMOS)",
        "LED/Mercury/Xenon Light Sources",
        "Filter Sets (DAPI, FITC, TRITC, Cy5)",
        "Objective Lenses (10x, 20x, 40x, 63x)",
        "Environmental Chamber (37°C, 5% CO₂)",
        "Image Analysis Software (ImageJ, CellProfiler)",
        "Data Storage Systems (RAID arrays)",
      ],
      // CPE analysis parameters
      cpeAnalysis: [
        "Cell Rounding (morphological change)",
        "Cell Detachment (loss of adhesion)",
        "Syncytia Formation (cell-cell fusion)",
        "Granulation (cytoplasmic changes)",
        "Nuclear Changes (chromatin condensation)",
        "Inclusion Bodies (viral replication sites)",
        "Apoptotic Bodies (programmed cell death)",
        "Necrotic Areas (uncontrolled cell death)",
        "Plaque Size Distribution",
        "Infection Spread Patterns",
      ],
      // Fluorescence analysis protocols
      fluorescenceAnalysis: [
        "Viral Antigen Staining (immunofluorescence)",
        "Nucleic Acid Staining (DAPI, Hoechst)",
        "Live/Dead Cell Staining (calcein-AM/PI)",
        "Viral RNA Detection (FISH probes)",
        "Protein Localization (GFP/RFP tagged)",
        "Membrane Integrity (propidium iodide)",
        "Mitochondrial Function (TMRM, JC-1)",
        "Caspase Activity (fluorogenic substrates)",
      ],
      // Quantitative measurements
      quantitativeMeasurements: [
        "Fluorescence Intensity (relative units)",
        "Signal-to-Noise Ratio",
        "Colocalization Coefficients",
        "Cell Count and Viability",
        "Infected Cell Percentage",
        "Viral Protein Expression Levels",
        "Nuclear/Cytoplasmic Ratios",
        "Time to First CPE",
        "CPE Progression Rate",
        "Plaque Forming Units (PFU) per field",
      ],
      // Data storage and analysis requirements
      dataRequirements: [
        "Raw Image Files (TIFF/ND2 format)",
        "Metadata (timestamp, exposure, filters)",
        "Analysis Parameters (thresholds, ROIs)",
        "Quantitative Results (CSV/Excel files)",
        "Image Processing Workflows",
        "Statistical Analysis (n≥3 replicates)",
        "Quality Control Images",
        "Backup and Archival (long-term storage)",
      ],
      criticalParams: [
        "Image Quality",
        "CPE Score",
        "Fluorescence Intensity",
        "Cell Count",
        "Viral Spread",
      ],
    },
    {
      name: "FORMULATION",
      label: "Formulation",
      description:
        "Prepare viral product | LMIS: Document formulation details (stabilizers, preservatives, concentrations)",
      icon: Chemistry,
      order: 7,
      qcRequired: true,
      pdfRequirement: "Prepare viral product",
      lmisRequirement:
        "Document formulation details (stabilizers, preservatives, concentrations)",
      // Viral product formulation components
      formulationComponents: [
        "Buffer Systems (PBS, Tris-HCl, HEPES)",
        "Stabilizing Agents (Sucrose, Trehalose, Mannitol)",
        "Cryoprotectants (Glycerol, DMSO, Ethylene glycol)",
        "Preservatives (Thimerosal, 2-Phenoxyethanol)",
        "Antioxidants (Ascorbic acid, Tocopherol)",
        "Chelating Agents (EDTA, EGTA)",
        "Surfactants (Polysorbate 80, Polysorbate 20)",
        "Protein Stabilizers (HSA, BSA, Gelatin)",
      ],
      // Buffer systems for viral formulations
      bufferSystems: [
        "Phosphate Buffered Saline (PBS) pH 7.4",
        "Tris-HCl Buffer (10-50 mM) pH 7.4-8.0",
        "HEPES Buffer (10-25 mM) pH 7.2-7.4",
        "Citrate Buffer (10-50 mM) pH 6.0-7.0",
        "Bicarbonate Buffer (physiological)",
        "Acetate Buffer (low pH applications)",
        "Glycine Buffer (alkaline conditions)",
      ],
      // Stabilizing agents and concentrations
      stabilizers: [
        "Sucrose (5-20% w/v) - osmotic protection",
        "Trehalose (5-15% w/v) - glass transition",
        "Mannitol (2-10% w/v) - bulking agent",
        "Sorbitol (5-15% w/v) - osmolyte",
        "Glycerol (10-50% v/v) - cryoprotectant",
        "PEG 400/600 (5-20% w/v) - protein stabilizer",
        "Dextran (5-10% w/v) - viscosity modifier",
        "L-Arginine (50-200 mM) - anti-aggregation",
      ],
      // Preservative systems
      preservatives: [
        "Thimerosal (0.01-0.1% w/v) - mercury-based",
        "2-Phenoxyethanol (0.5-1.0% v/v) - phenol derivative",
        "Benzyl Alcohol (0.9-2.0% v/v) - antimicrobial",
        "Phenol (0.25-0.5% w/v) - traditional preservative",
        "m-Cresol (0.1-0.3% w/v) - phenolic compound",
        "Chlorobutanol (0.5% w/v) - halogenated alcohol",
        "Parabens (0.1-0.2% w/v) - ester preservatives",
      ],
      // Formulation process steps
      formulationProcess: [
        "Component Preparation (weighing, dissolution)",
        "pH Adjustment (target ±0.1 units)",
        "Osmolality Adjustment (280-320 mOsm/kg)",
        "Sterile Filtration (0.22μm)",
        "Virus Addition (maintain cold chain)",
        "Gentle Mixing (avoid foaming/shearing)",
        "Final pH/Osmolality Check",
        "Sterility Testing Sample Collection",
        "Fill into Final Containers",
        "Freeze/Refrigerate as Required",
      ],
      // Critical quality parameters
      qualityParameters: [
        "Viral Titer (TCID50/mL, PFU/mL)",
        "Infectivity Ratio (infectious:total particles)",
        "pH (±0.2 units from target)",
        "Osmolality (±20 mOsm/kg from target)",
        "Sterility (no growth, 14 days)",
        "Endotoxin (<5 EU/mL for injectables)",
        "Particulate Matter (USP <788>)",
        "Container Closure Integrity",
        "Stability Indicators (visual, chemical)",
        "Preservative Effectiveness",
      ],
      // Stability testing requirements
      stabilityTesting: [
        "Real-time Stability (storage conditions)",
        "Accelerated Stability (elevated temperature)",
        "Freeze-thaw Stability (3-5 cycles)",
        "Light Stability (ICH Q1B)",
        "Mechanical Stress (shaking/vibration)",
        "pH Drift Monitoring",
        "Viral Titer Retention",
        "Aggregation/Precipitation",
        "Container Compatibility",
        "Preservative Potency",
      ],
      criticalParams: [
        "Viral Titer",
        "pH",
        "Osmolality",
        "Sterility",
        "Stability",
      ],
    },
    {
      name: "FEEDING",
      label: "Culture Feeding",
      description:
        "Maintain culture | LMIS: Log feeding schedule, reagents used",
      icon: Time,
      order: 8,
      qcRequired: false,
      pdfRequirement: "Maintain culture",
      lmisRequirement: "Log feeding schedule, reagents used",
      // Culture feeding and maintenance protocols
      feedingProtocols: [
        "Media Exchange (partial or complete)",
        "Nutrient Supplementation (glucose, glutamine)",
        "pH Adjustment (sodium bicarbonate)",
        "Volume Adjustment (concentration/dilution)",
        "Fresh Media Addition (fed-batch mode)",
        "Perfusion Feeding (continuous)",
        "Bolus Feeding (intermittent additions)",
        "Waste Removal (spent media)",
      ],
      // Feeding schedule optimization
      feedingSchedule: [
        "Day 0: Initial Inoculation",
        "Day 1: Monitor, no feeding typically",
        "Day 2: Partial media exchange (50%)",
        "Day 3: Nutrient supplementation",
        "Day 4: Fresh media addition",
        "Day 5-7: Daily monitoring, feed as needed",
        "Beyond Day 7: Harvest or continue",
        "Critical: Virus-dependent timing",
      ],
      // Nutrient monitoring and management
      nutrientManagement: [
        "Glucose Monitoring (2-4 g/L optimal)",
        "Lactate Accumulation (<2 g/L preferred)",
        "Glutamine Depletion (supplement as needed)",
        "pH Drift (7.2-7.4 optimal range)",
        "Osmolality Changes (280-320 mOsm/kg)",
        "Dissolved Oxygen (virus-specific)",
        "Ammonia Accumulation (toxic levels)",
        "Amino Acid Depletion",
      ],
      // Cell density and viability monitoring
      cellMonitoring: [
        "Cell Density (viable cell count)",
        "Viability Percentage (>85% preferred)",
        "Cell Size Distribution",
        "Metabolic Activity (glucose consumption)",
        "Growth Phase (exponential vs. stationary)",
        "Cell Cycle Distribution",
        "Apoptosis Markers",
        "Necrosis Indicators",
      ],
      // Reagents and supplements
      feedingReagents: [
        "Fresh Complete Media (DMEM + supplements)",
        "Glucose Solution (45% w/v stock)",
        "L-Glutamine (200 mM stock)",
        "Sodium Bicarbonate (7.5% w/v)",
        "Amino Acid Solutions (concentrated)",
        "Vitamin Solutions (100x stocks)",
        "Growth Factors (virus-specific)",
        "Serum or Serum Replacements",
      ],
      // Critical feeding parameters
      feedingParameters: [
        "Media Exchange Volume (25-75%)",
        "Feeding Frequency (daily or as needed)",
        "Temperature Maintenance (37°C)",
        "Sterile Technique (laminar flow)",
        "pH Monitoring (continuous or periodic)",
        "Cell Density Targets",
        "Viability Thresholds",
        "Contamination Prevention",
      ],
      // Quality checks during feeding
      qualityChecks: [
        "Visual Inspection (clarity, color)",
        "pH Measurement (immediate)",
        "Osmolality Check (if available)",
        "Cell Count and Viability",
        "Contamination Assessment",
        "Temperature Verification",
        "Volume Accuracy",
        "Documentation Completeness",
      ],
      criticalParams: [
        "Feeding Frequency",
        "Glucose Levels",
        "Lactate Levels",
        "pH",
        "Cell Viability",
      ],
    },
    {
      name: "PACKAGING",
      label: "Final Packaging",
      description:
        "Final product packaging | LMIS: Track batch ID, packaging specs (vial type, fill volume, labeling)",
      icon: Checkmark,
      order: 9,
      qcRequired: true,
      pdfRequirement: "Final product packaging",
      lmisRequirement:
        "Track batch ID, packaging specs (vial type, fill volume, labeling)",
      // Container types for viral products
      containerTypes: [
        "Glass Vials (Type I borosilicate)",
        "Plastic Vials (COP/COC)",
        "Pre-filled Syringes (glass/plastic)",
        "Ampoules (break-open glass)",
        "Bottles (larger volumes)",
        "Cartridges (pen injectors)",
        "Bags (flexible containers)",
        "Tubes (sample/aliquot storage)",
      ],
      // Packaging specifications
      packagingSpecs: [
        "Container Material (USP Type I glass preferred)",
        "Container Volume (0.5-50 mL typical)",
        "Fill Volume (80-90% of container volume)",
        "Headspace (nitrogen/argon gas purging)",
        "Closure System (rubber stoppers/caps)",
        "Crimping/Capping (proper seal integrity)",
        "Secondary Packaging (cartons, trays)",
        "Cold Chain Requirements (-80°C to +8°C)",
      ],
      // Labeling requirements
      labelingRequirements: [
        "Product Name (virus type/strain)",
        "Batch/Lot Number (manufacturing date code)",
        "Manufacturing Date",
        "Expiration Date",
        "Storage Conditions (-80°C, -20°C, +4°C)",
        "Volume/Concentration",
        "Sterility Statement",
        "Manufacturer Information",
        "Regulatory Information (if applicable)",
        "Barcode/QR Code (traceability)",
      ],
      // Batch documentation
      batchDocumentation: [
        "Batch Manufacturing Record (BMR)",
        "Certificate of Analysis (CoA)",
        "Viral Titer Results",
        "Sterility Test Results",
        "Endotoxin Test Results",
        "Container Closure Integrity",
        "Environmental Monitoring Data",
        "Personnel Training Records",
        "Equipment Calibration Records",
        "Material Lot Traceability",
      ],
      // Quality control release testing
      releaseTesting: [
        "Viral Identity (PCR/sequencing)",
        "Viral Titer (TCID50/PFU/mL)",
        "Sterility (USP <71>)",
        "Endotoxin (USP <85>)",
        "Mycoplasma (PCR/culture)",
        "Particulate Matter (USP <788>)",
        "Container Closure Integrity",
        "pH and Osmolality",
        "Appearance (visual inspection)",
        "Volume/Fill Weight",
      ],
      // Packaging process steps
      packagingProcess: [
        "Container Preparation (washing/sterilization)",
        "Environmental Control (cleanroom/isolator)",
        "Fill Volume Verification",
        "Closure Application",
        "Seal Integrity Testing",
        "Visual Inspection",
        "Labeling Application",
        "Secondary Packaging",
        "Final QC Release",
        "Cold Storage Transfer",
      ],
      // Release approval criteria
      releaseApproval: [
        "All QC Tests Pass Specifications",
        "Batch Record Review Complete",
        "Deviation Investigations Closed",
        "Environmental Monitoring Acceptable",
        "Personnel Qualifications Verified",
        "Equipment Functionality Confirmed",
        "Material Traceability Complete",
        "Stability Data Supports Release",
        "Regulatory Compliance Verified",
        "Quality Assurance Approval",
      ],
      // Storage and distribution
      storageDistribution: [
        "Temperature-Controlled Storage",
        "Inventory Management System",
        "First-In-First-Out (FIFO)",
        "Temperature Monitoring",
        "Alarm Systems",
        "Access Control",
        "Shipping Qualification",
        "Chain of Custody",
        "Customer Notification",
        "Product Recall Capability",
      ],
      criticalParams: [
        "Batch ID",
        "Fill Volume",
        "Titer",
        "Sterility",
        "Release Approval",
      ],
    },
  ];

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load data on component mount
  useEffect(() => {
    loadDashboardData();
    loadCultureBatches();
  }, [entryId, notebookEntry]);

  // Load notebook page samples and auto-create virus culture batches
  useEffect(() => {
    if (pageData?.id && !String(pageData.id).startsWith("default-")) {
      loadNotebookPageSamples();
    }
  }, [pageData?.id]);

  const loadNotebookPageSamples = useCallback(() => {
    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && response.samples) {
          console.log(
            `Loaded ${response.samples.length} samples for virus culture workflow page`,
          );

          // Auto-create virus culture batches for samples that don't have them
          response.samples.forEach((sample) => {
            if (
              sample.status === "PENDING" ||
              sample.status === "IN_PROGRESS"
            ) {
              autoCreateVirusCultureBatch(sample);
            }
          });
        }
        setLoading(false);
      },
      (error) => {
        console.error("Failed to load notebook page samples:", error);
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  const autoCreateVirusCultureBatch = useCallback((sample) => {
    // Check if a batch already exists for this sample
    getFromOpenElisServer(
      `/rest/notebook/virology/culture/batches/sample/${sample.sampleItemId}`,
      (response) => {
        if (response && response.batches && response.batches.length === 0) {
          // No existing batch, create one automatically
          const batchRequest = {
            notebookPageSampleId: sample.id,
            virusStrain: "Pending Assignment",
            cellLine: "Vero E6",
            notes: `Auto-created batch for sample ${sample.accessionNumber || sample.sampleItemId}`,
          };

          postToOpenElisServer(
            "/rest/notebook/virology/culture/batch",
            batchRequest,
            (createResponse) => {
              if (createResponse && createResponse.success) {
                notify({
                  kind: NotificationKinds.success,
                  title: intl.formatMessage({
                    id: "notification.title",
                    defaultMessage: "Success",
                  }),
                  subtitle: intl.formatMessage(
                    {
                      id: "virology.culture.batch.created.success",
                      defaultMessage:
                        "Auto-created virus culture batch {batchNumber} for sample {sampleId}",
                    },
                    {
                      batchNumber: createResponse.batchNumber,
                      sampleId: sample.sampleItemId,
                    },
                  ),
                });
                // Reload batches to show the new one
                loadCultureBatches();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notification.error",
                    defaultMessage: "Error",
                  }),
                  subtitle: intl.formatMessage({
                    id: "virology.culture.batch.create.error",
                    defaultMessage: "Failed to auto-create virus culture batch",
                  }),
                });
              }
            },
            (error) => {
              notify({
                kind: NotificationKinds.error,
                title: intl.formatMessage({
                  id: "notification.error",
                  defaultMessage: "Error",
                }),
                subtitle: intl.formatMessage({
                  id: "virology.culture.batch.create.network.error",
                  defaultMessage:
                    "Network error while creating virus culture batch",
                }),
              });
            },
          );
        }
      },
      (error) => {
        console.error("Error checking existing batches for sample:", error);
      },
    );
  }, []);

  const loadDashboardData = useCallback(() => {
    getFromOpenElisServer(
      "/rest/notebook/virology/culture/dashboard",
      (response) => {
        if (response && response.success) {
          setDashboardData(response);
        }
      },
    );
  }, []);

  const loadCultureBatches = useCallback(() => {
    if (!entryId) return;

    setLoading(true);
    getFromOpenElisServer(
      "/rest/notebook/virology/culture/batches/active",
      (response) => {
        setLoading(false);
        if (response && response.success) {
          setCultureBatches(response.batches || []);
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.culture.batches.load.error",
              defaultMessage: "Failed to load virus culture batches",
            }),
          });
        }
      },
    );
  }, [entryId]);

  const loadBatchDetails = useCallback((batchId) => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/virology/culture/batch/${batchId}`,
      (response) => {
        setLoading(false);
        if (response && response.success) {
          setSelectedBatch(response.batch);
          setWorkflowStatus(response.workflowStatus || []);
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.culture.batch.details.load.error",
              defaultMessage: "Failed to load batch details",
            }),
          });
        }
      },
    );
  }, []);

  const handleCreateBatch = () => {
    setLoading(true);
    postToOpenElisServer(
      "/rest/notebook/virology/culture/batch",
      JSON.stringify(createBatchForm),
      (response) => {
        setLoading(false);
        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.culture.batch.create.success",
                defaultMessage:
                  "Virus culture batch {batchNumber} created successfully",
              },
              { batchNumber: response.batchNumber },
            ),
          });
          setShowCreateModal(false);
          setCreateBatchForm({
            notebookPageSampleId: null,
            virusStrain: "",
            cellLine: "",
            notes: "",
          });
          loadCultureBatches();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle:
              response?.message ||
              intl.formatMessage({
                id: "virology.culture.batch.create.manual.error",
                defaultMessage: "Failed to create virus culture batch",
              }),
          });
        }
      },
    );
  };

  const handleStartStep = (batchId, stepName) => {
    setLoading(true);
    postToOpenElisServer(
      `/rest/notebook/virology/culture/batch/${batchId}/step/${stepName}/start`,
      "{}",
      (response) => {
        setLoading(false);
        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.culture.step.start.success",
                defaultMessage: "{stepLabel} started successfully",
              },
              { stepLabel: getStepLabel(stepName) },
            ),
          });
          loadBatchDetails(batchId);
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle:
              response?.message ||
              intl.formatMessage({
                id: "virology.culture.step.start.error",
                defaultMessage: "Failed to start step",
              }),
          });
        }
      },
    );
  };

  const handleCompleteStep = () => {
    if (!currentStep || !selectedBatch) return;

    setLoading(true);
    postToOpenElisServer(
      `/rest/notebook/virology/culture/batch/${selectedBatch.id}/step/${currentStep.stepName}/complete`,
      JSON.stringify(stepForm),
      (response) => {
        setLoading(false);
        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.culture.step.complete.success",
                defaultMessage: "{stepLabel} completed successfully",
              },
              { stepLabel: getStepLabel(currentStep.stepName) },
            ),
          });
          setShowStepModal(false);
          setStepForm({
            qualityResult: "NOT_APPLICABLE",
            notes: "",
            materialsUsed: [],
            equipmentUsed: [],
            lotNumbers: {},
            measurements: {},
            conditions: {},
            qcResults: {},
            personnelInvolved: "",
            startTime: "",
            endTime: "",
            temperature: "",
            co2Level: "",
            phLevel: "",
            sterilizationParams: {},
            imageData: {},
          });
          loadBatchDetails(selectedBatch.id);

          // If this was the final step (PACKAGING), mark the notebook page sample as completed
          if (currentStep.stepName === "PACKAGING") {
            updateNotebookPageSampleStatus(
              selectedBatch.notebookPageSampleId,
              "COMPLETED",
            );
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle:
              response?.message ||
              intl.formatMessage({
                id: "virology.culture.step.complete.error",
                defaultMessage: "Failed to complete step",
              }),
          });
        }
      },
    );
  };

  const updateNotebookPageSampleStatus = useCallback(
    (notebookPageSampleId, status) => {
      if (!notebookPageSampleId || !pageData?.id) return;

      console.log(
        `Updating notebook page sample ${notebookPageSampleId} status to ${status}`,
      );

      // Update the notebook page sample status to sync with virus culture workflow progress
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [notebookPageSampleId],
          status: status,
        }),
        (response) => {
          if (response && response.success) {
            console.log(
              `Successfully updated notebook page sample status to ${status}`,
            );
            // Notify parent component about progress update
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            console.error(
              "Failed to update notebook page sample status:",
              response,
            );
          }
        },
        (error) => {
          console.error("Error updating notebook page sample status:", error);
        },
      );
    },
    [pageData?.id, onProgressUpdate],
  );

  const getStepLabel = (stepName) => {
    const step = workflowSteps.find((s) => s.name === stepName);
    return step ? step.label : stepName;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "—";
    try {
      return new Date(dateString).toLocaleDateString();
    } catch (error) {
      return "—";
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "COMPLETED":
        return <Checkmark size={16} className="status-completed" />;
      case "IN_PROGRESS":
        return <Time size={16} className="status-in-progress" />;
      case "FAILED":
        return <Error size={16} className="status-failed" />;
      case "ON_HOLD":
        return <Warning size={16} className="status-warning" />;
      default:
        return <Time size={16} className="status-pending" />;
    }
  };

  const getStatusTag = (status) => {
    const statusMap = {
      PENDING: { type: "gray", text: "Pending" },
      IN_PROGRESS: { type: "blue", text: "In Progress" },
      COMPLETED: { type: "green", text: "Completed" },
      FAILED: { type: "red", text: "Failed" },
      ON_HOLD: { type: "yellow", text: "On Hold" },
      SKIPPED: { type: "gray", text: "Skipped" },
    };

    const config = statusMap[status] || statusMap["PENDING"];
    return <Tag type={config.type}>{config.text}</Tag>;
  };

  // Render step-specific LMIS tracking fields
  const renderStepSpecificFields = () => {
    if (!currentStep) return null;

    const stepConfig = workflowSteps.find(
      (s) => s.name === currentStep.stepName,
    );
    if (!stepConfig) return null;

    switch (currentStep.stepName) {
      case "MEDIA_PREPARATION":
        return (
          <>
            <FormGroup legendText="Media Types">
              <Select
                id="media-type-select"
                value={stepForm.conditions.mediaType || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    conditions: {
                      ...stepForm.conditions,
                      mediaType: e.target.value,
                    },
                  })
                }
              >
                <SelectItem value="" text="Select media type..." />
                {stepConfig.mediaTypes?.map((media) => (
                  <SelectItem key={media} value={media} text={media} />
                ))}
              </Select>
            </FormGroup>
            <FormGroup legendText="Reagents Used (LMIS Tracking)">
              {stepConfig.reagents?.slice(0, 4).map((material, index) => (
                <div
                  key={material}
                  style={{
                    display: "flex",
                    gap: "1rem",
                    marginBottom: "0.5rem",
                  }}
                >
                  <TextInput
                    id={`material-${index}`}
                    labelText={material}
                    placeholder="Lot Number"
                    value={stepForm.lotNumbers[material] || ""}
                    onChange={(e) =>
                      setStepForm({
                        ...stepForm,
                        lotNumbers: {
                          ...stepForm.lotNumbers,
                          [material]: e.target.value,
                        },
                      })
                    }
                  />
                  <TextInput
                    id={`expiry-${index}`}
                    placeholder="Expiry Date (DD/MM/YYYY)"
                    value={stepForm.measurements[`${material}_expiry`] || ""}
                    onChange={(e) =>
                      setStepForm({
                        ...stepForm,
                        measurements: {
                          ...stepForm.measurements,
                          [`${material}_expiry`]: e.target.value,
                        },
                      })
                    }
                  />
                </div>
              ))}
            </FormGroup>
            <FormGroup legendText="Critical Parameters">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="ph-level"
                  labelText="pH Level (7.2-7.4)"
                  type="number"
                  step="0.1"
                  min="6.5"
                  max="8.0"
                  value={stepForm.phLevel}
                  onChange={(e) =>
                    setStepForm({ ...stepForm, phLevel: e.target.value })
                  }
                />
                <TextInput
                  id="osmolality"
                  labelText="Osmolality (mOsm/kg)"
                  type="number"
                  value={stepForm.measurements.osmolality || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        osmolality: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
          </>
        );

      case "STERILIZATION":
        return (
          <>
            <FormGroup legendText="Sterilization Parameters">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="steril-temp"
                  labelText="Temperature (°C)"
                  type="number"
                  value={stepForm.sterilizationParams.temperature || "121"}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      sterilizationParams: {
                        ...stepForm.sterilizationParams,
                        temperature: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="steril-time"
                  labelText="Time (minutes)"
                  type="number"
                  value={stepForm.sterilizationParams.time || "15"}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      sterilizationParams: {
                        ...stepForm.sterilizationParams,
                        time: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="steril-pressure"
                  labelText="Pressure (PSI)"
                  type="number"
                  value={stepForm.sterilizationParams.pressure || "15"}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      sterilizationParams: {
                        ...stepForm.sterilizationParams,
                        pressure: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
            <FormGroup legendText="Equipment Used">
              <Select
                id="steril-equipment"
                value={stepForm.equipmentUsed[0] || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    equipmentUsed: [e.target.value],
                  })
                }
              >
                <SelectItem value="" text="Select equipment..." />
                {stepConfig.equipment.map((eq) => (
                  <SelectItem key={eq} value={eq} text={eq} />
                ))}
              </Select>
            </FormGroup>
            <FormGroup legendText="Spore Test Result">
              <Select
                id="spore-test"
                value={stepForm.qcResults.sporeTest || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    qcResults: {
                      ...stepForm.qcResults,
                      sporeTest: e.target.value,
                    },
                  })
                }
              >
                <SelectItem value="" text="Select result..." />
                <SelectItem value="PASSED" text="Passed - No Growth" />
                <SelectItem value="FAILED" text="Failed - Growth Detected" />
                <SelectItem value="PENDING" text="Pending - Incubating" />
              </Select>
            </FormGroup>
          </>
        );

      case "CELL_CULTURE":
        return (
          <>
            <FormGroup legendText="Culture Conditions">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="culture-temp"
                  labelText="Temperature (°C)"
                  type="number"
                  step="0.1"
                  value={stepForm.temperature || "37.0"}
                  onChange={(e) =>
                    setStepForm({ ...stepForm, temperature: e.target.value })
                  }
                />
                <TextInput
                  id="co2-level"
                  labelText="CO₂ Level (%)"
                  type="number"
                  step="0.1"
                  value={stepForm.co2Level || "5.0"}
                  onChange={(e) =>
                    setStepForm({ ...stepForm, co2Level: e.target.value })
                  }
                />
              </div>
            </FormGroup>
            <FormGroup legendText="Cell Monitoring">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="confluence"
                  labelText="Confluence (%)"
                  type="number"
                  min="0"
                  max="100"
                  value={stepForm.measurements.confluence || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        confluence: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="viability"
                  labelText="Viability (%)"
                  type="number"
                  min="0"
                  max="100"
                  value={stepForm.measurements.viability || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        viability: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
          </>
        );

      case "QUALITY_CONTROL":
        return (
          <>
            <FormGroup legendText="QC Test Results">
              {stepConfig.tests.map((test, index) => (
                <div key={test} style={{ marginBottom: "1rem" }}>
                  <Select
                    id={`qc-${index}`}
                    labelText={test}
                    value={stepForm.qcResults[test] || ""}
                    onChange={(e) =>
                      setStepForm({
                        ...stepForm,
                        qcResults: {
                          ...stepForm.qcResults,
                          [test]: e.target.value,
                        },
                      })
                    }
                  >
                    <SelectItem value="" text="Select result..." />
                    <SelectItem value="PASSED" text="Passed" />
                    <SelectItem value="FAILED" text="Failed" />
                    <SelectItem value="PENDING" text="Pending" />
                    {test === "Viability Assay" && (
                      <SelectItem value="MARGINAL" text="Marginal (70-85%)" />
                    )}
                  </Select>
                </div>
              ))}
            </FormGroup>
            <FormGroup legendText="Acceptance Criteria Check">
              <TextArea
                id="acceptance-notes"
                labelText="Criteria Verification Notes"
                placeholder="Document how results meet acceptance criteria..."
                rows={2}
                value={stepForm.measurements.acceptanceNotes || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    measurements: {
                      ...stepForm.measurements,
                      acceptanceNotes: e.target.value,
                    },
                  })
                }
              />
            </FormGroup>
          </>
        );

      case "VIRUS_CULTURE":
        return (
          <>
            <FormGroup legendText="Virus Inoculation Parameters">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="moi"
                  labelText="MOI (Multiplicity of Infection)"
                  type="number"
                  step="0.001"
                  value={stepForm.measurements.moi || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        moi: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="virus-titer"
                  labelText="Virus Titer (TCID50/mL)"
                  value={stepForm.measurements.virusTiter || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        virusTiter: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
            <FormGroup legendText="CPE Monitoring">
              <Select
                id="cpe-development"
                labelText="CPE Development"
                value={stepForm.measurements.cpeScore || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    measurements: {
                      ...stepForm.measurements,
                      cpeScore: e.target.value,
                    },
                  })
                }
              >
                <SelectItem value="" text="Select CPE score..." />
                <SelectItem value="0" text="0 - No CPE observed" />
                <SelectItem value="1" text="1+ - Minimal CPE (<25%)" />
                <SelectItem value="2" text="2+ - Moderate CPE (25-50%)" />
                <SelectItem value="3" text="3+ - Extensive CPE (50-75%)" />
                <SelectItem value="4" text="4+ - Complete CPE (>75%)" />
              </Select>
            </FormGroup>
          </>
        );

      case "DARK_ROOM_IMAGING":
        return (
          <>
            <FormGroup legendText="Imaging Parameters">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="fluorescence-units"
                  labelText="Fluorescence Intensity (RFU)"
                  type="number"
                  value={stepForm.measurements.fluorescenceUnits || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        fluorescenceUnits: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="image-count"
                  labelText="Images Captured"
                  type="number"
                  value={stepForm.imageData.imageCount || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      imageData: {
                        ...stepForm.imageData,
                        imageCount: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
            <FormGroup legendText="Morphology Assessment">
              <TextArea
                id="morphology-notes"
                labelText="Cell Morphology Observations"
                placeholder="Document cell shape, viral plaques, cytotoxicity..."
                rows={2}
                value={stepForm.measurements.morphologyNotes || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    measurements: {
                      ...stepForm.measurements,
                      morphologyNotes: e.target.value,
                    },
                  })
                }
              />
            </FormGroup>
          </>
        );

      case "FORMULATION":
        return (
          <>
            <FormGroup legendText="Formulation Components">
              {stepConfig.components.map((component, index) => (
                <div
                  key={component}
                  style={{
                    display: "flex",
                    gap: "1rem",
                    marginBottom: "0.5rem",
                  }}
                >
                  <TextInput
                    id={`component-${index}`}
                    labelText={component}
                    placeholder="Concentration/Amount"
                    value={stepForm.measurements[`${component}_conc`] || ""}
                    onChange={(e) =>
                      setStepForm({
                        ...stepForm,
                        measurements: {
                          ...stepForm.measurements,
                          [`${component}_conc`]: e.target.value,
                        },
                      })
                    }
                  />
                  <TextInput
                    id={`component-lot-${index}`}
                    placeholder="Lot Number"
                    value={stepForm.lotNumbers[component] || ""}
                    onChange={(e) =>
                      setStepForm({
                        ...stepForm,
                        lotNumbers: {
                          ...stepForm.lotNumbers,
                          [component]: e.target.value,
                        },
                      })
                    }
                  />
                </div>
              ))}
            </FormGroup>
            <FormGroup legendText="Final Product QC">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="final-titer"
                  labelText="Final Titer (TCID50/mL)"
                  value={stepForm.measurements.finalTiter || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        finalTiter: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="protein-conc"
                  labelText="Protein Concentration (mg/mL)"
                  type="number"
                  step="0.01"
                  value={stepForm.measurements.proteinConcentration || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        proteinConcentration: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
          </>
        );

      case "FEEDING":
        return (
          <>
            <FormGroup legendText="Feeding Schedule">
              <Select
                id="feeding-type"
                labelText="Feeding Type"
                value={stepForm.conditions.feedingType || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    conditions: {
                      ...stepForm.conditions,
                      feedingType: e.target.value,
                    },
                  })
                }
              >
                <SelectItem value="" text="Select feeding type..." />
                <SelectItem
                  value="FULL_MEDIA_CHANGE"
                  text="Full Media Change"
                />
                <SelectItem
                  value="PARTIAL_MEDIA_CHANGE"
                  text="Partial Media Change (50%)"
                />
                <SelectItem
                  value="SUPPLEMENT_ADDITION"
                  text="Supplement Addition"
                />
                <SelectItem value="GLUCOSE_FEEDING" text="Glucose Feeding" />
                <SelectItem
                  value="MAINTENANCE_FEEDING"
                  text="Maintenance Feeding"
                />
              </Select>
            </FormGroup>
            <FormGroup legendText="Metabolic Monitoring">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr 1fr",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="glucose-level"
                  labelText="Glucose (mg/dL)"
                  type="number"
                  value={stepForm.measurements.glucose || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        glucose: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="lactate-level"
                  labelText="Lactate (mmol/L)"
                  type="number"
                  step="0.1"
                  value={stepForm.measurements.lactate || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        lactate: e.target.value,
                      },
                    })
                  }
                />
                <TextInput
                  id="cell-density"
                  labelText="Cell Density (cells/mL)"
                  value={stepForm.measurements.cellDensity || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        cellDensity: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
          </>
        );

      case "PACKAGING":
        return (
          <>
            <FormGroup legendText="Packaging Specifications">
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <Select
                  id="vial-type"
                  labelText="Vial Type"
                  value={stepForm.conditions.vialType || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      conditions: {
                        ...stepForm.conditions,
                        vialType: e.target.value,
                      },
                    })
                  }
                >
                  <SelectItem value="" text="Select vial type..." />
                  <SelectItem value="GLASS_10ML" text="Glass 10mL" />
                  <SelectItem value="GLASS_5ML" text="Glass 5mL" />
                  <SelectItem value="PLASTIC_15ML" text="Plastic 15mL" />
                  <SelectItem value="PLASTIC_50ML" text="Plastic 50mL" />
                  <SelectItem value="CRYOVIAL_2ML" text="Cryovial 2mL" />
                </Select>
                <TextInput
                  id="fill-volume"
                  labelText="Fill Volume (mL)"
                  type="number"
                  step="0.1"
                  value={stepForm.measurements.fillVolume || ""}
                  onChange={(e) =>
                    setStepForm({
                      ...stepForm,
                      measurements: {
                        ...stepForm.measurements,
                        fillVolume: e.target.value,
                      },
                    })
                  }
                />
              </div>
            </FormGroup>
            <FormGroup legendText="Batch Documentation">
              <TextInput
                id="batch-id"
                labelText="Final Batch ID"
                value={stepForm.conditions.finalBatchId || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    conditions: {
                      ...stepForm.conditions,
                      finalBatchId: e.target.value,
                    },
                  })
                }
              />
            </FormGroup>
            <FormGroup legendText="Release Approval">
              <Select
                id="release-status"
                labelText="Release Status"
                value={stepForm.qcResults.releaseStatus || ""}
                onChange={(e) =>
                  setStepForm({
                    ...stepForm,
                    qcResults: {
                      ...stepForm.qcResults,
                      releaseStatus: e.target.value,
                    },
                  })
                }
              >
                <SelectItem value="" text="Select release status..." />
                <SelectItem
                  value="APPROVED_FOR_RELEASE"
                  text="Approved for Release"
                />
                <SelectItem
                  value="CONDITIONAL_RELEASE"
                  text="Conditional Release"
                />
                <SelectItem
                  value="HOLD_FOR_FURTHER_TESTING"
                  text="Hold for Further Testing"
                />
                <SelectItem value="REJECTED" text="Rejected" />
              </Select>
            </FormGroup>
          </>
        );

      default:
        return null;
    }
  };

  const batchTableHeaders = [
    { key: "batchId", header: "Batch ID" },
    { key: "virusStrain", header: "Virus Strain" },
    { key: "cellLine", header: "Cell Line" },
    { key: "status", header: "Status" },
    { key: "createdDate", header: "Created" },
    { key: "progress", header: "Progress" },
    { key: "actions", header: "Actions" },
  ];

  const renderBatchRow = (batch) => ({
    id: batch.id,
    batchId: batch.batchId,
    virusStrain: batch.virusStrain || "—",
    cellLine: batch.cellLineUsed || "—",
    status: getStatusTag(batch.status),
    createdDate: formatDate(batch.createdDate),
    progress: `${batch.completedSteps || 0}/9 steps`,
    actions: (
      <OverflowMenu>
        <OverflowMenuItem
          itemText="View Details"
          onClick={() => loadBatchDetails(batch.id)}
        />
        <OverflowMenuItem
          itemText="Continue Workflow"
          onClick={() => loadBatchDetails(batch.id)}
        />
      </OverflowMenu>
    ),
  });

  const renderWorkflowProgress = () => {
    if (!workflowStatus.length) return null;

    const currentStepIndex = workflowStatus.findIndex(
      (step) => step.status === "IN_PROGRESS",
    );
    const completedSteps = workflowStatus.filter(
      (step) => step.status === "COMPLETED",
    ).length;

    return (
      <div className="workflow-progress">
        <ProgressIndicator
          currentIndex={
            currentStepIndex >= 0 ? currentStepIndex : completedSteps
          }
        >
          {workflowSteps.map((step, index) => {
            const stepStatus = workflowStatus.find(
              (ws) => ws.stepName === step.name,
            );
            const status = stepStatus ? stepStatus.status : "PENDING";

            return (
              <ProgressStep
                key={step.name}
                label={step.label}
                description={step.description}
                complete={status === "COMPLETED"}
                current={status === "IN_PROGRESS"}
                invalid={status === "FAILED"}
                disabled={status === "PENDING"}
                onClick={() => {
                  if (status === "IN_PROGRESS") {
                    const stepStatus = workflowStatus.find(
                      (ws) => ws.stepName === step.name,
                    );
                    if (stepStatus) {
                      setCurrentStep(stepStatus);
                      setShowStepModal(true);
                    }
                  }
                }}
              />
            );
          })}
        </ProgressIndicator>
      </div>
    );
  };

  const renderWorkflowSteps = () => {
    if (!selectedBatch || !workflowStatus.length) return null;

    return (
      <div className="workflow-steps">
        <h4>Workflow Steps</h4>
        <DataTable
          rows={workflowStatus.map((step) => ({
            id: step.id,
            stepName: getStepLabel(step.stepName),
            status: getStatusTag(step.status),
            assignedTo: step.assignedTo ? step.assignedTo.login : "—",
            startedDate: step.startedDate ? formatDate(step.startedDate) : "—",
            completedDate: step.completedDate
              ? formatDate(step.completedDate)
              : "—",
            actions:
              step.status === "PENDING" ? (
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Play}
                  onClick={() =>
                    handleStartStep(selectedBatch.id, step.stepName)
                  }
                >
                  Start
                </Button>
              ) : step.status === "IN_PROGRESS" ? (
                <Button
                  kind="secondary"
                  size="sm"
                  renderIcon={Checkmark}
                  onClick={() => {
                    setCurrentStep(step);
                    setShowStepModal(true);
                  }}
                >
                  Complete
                </Button>
              ) : null,
          }))}
          headers={[
            { key: "stepName", header: "Step" },
            { key: "status", header: "Status" },
            { key: "assignedTo", header: "Assigned To" },
            { key: "startedDate", header: "Started" },
            { key: "completedDate", header: "Completed" },
            { key: "actions", header: "Actions" },
          ]}
        >
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer title="Workflow Steps">
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
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
      </div>
    );
  };

  const renderDashboard = () => (
    <div className="virology-dashboard">
      {/* Key Metrics */}
      <Grid>
        <Column lg={3} md={2} sm={2}>
          <Tile className="dashboard-tile progress-tile">
            <div className="tile-header">
              <Microscope size={24} />
              <h4>Active Batches</h4>
            </div>
            <div className="metric-value">
              {dashboardData.activeBatches || 0}
            </div>
            <div className="metric-label">Currently Processing</div>
          </Tile>
        </Column>
        <Column lg={3} md={2} sm={2}>
          <Tile className="dashboard-tile verified">
            <div className="tile-header">
              <CheckmarkFilled size={24} />
              <h4>Completed Today</h4>
            </div>
            <div className="metric-value">
              {dashboardData.completedToday || 0}
            </div>
            <div className="metric-label">Steps Finished</div>
          </Tile>
        </Column>
        <Column lg={3} md={2} sm={2}>
          <Tile className="dashboard-tile pending">
            <div className="tile-header">
              <Warning size={24} />
              <h4>Requiring Attention</h4>
            </div>
            <div className="metric-value">
              {dashboardData.requiresAttention || 0}
            </div>
            <div className="metric-label">QC/Equipment Issues</div>
          </Tile>
        </Column>
        <Column lg={3} md={2} sm={2}>
          <Tile className="dashboard-tile">
            <div className="tile-header">
              <Chemistry size={24} />
              <h4>Success Rate</h4>
            </div>
            <div className="metric-value">
              {dashboardData.successRate || 95}%
            </div>
            <div className="metric-label">Batch Success Rate</div>
          </Tile>
        </Column>
      </Grid>

      {/* LMIS Alerts Section */}
      <div style={{ marginTop: "2rem" }}>
        <h4>LMIS Alerts & Equipment Status</h4>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Tile className="alert-tile">
              <h5>Inventory Alerts</h5>
              <div className="alert-list">
                <div className="alert-item">
                  <Warning size={16} style={{ color: "#f1c21b" }} />
                  <span>DMEM Media: 2 lots expiring within 7 days</span>
                </div>
                <div className="alert-item">
                  <Error size={16} style={{ color: "#da1e28" }} />
                  <span>FBS: Low stock (3 lots remaining)</span>
                </div>
                <div className="alert-item">
                  <Checkmark size={16} style={{ color: "#198038" }} />
                  <span>P/S Antibiotics: Stock levels normal</span>
                </div>
              </div>
            </Tile>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Tile className="alert-tile">
              <h5>Equipment Status</h5>
              <div className="alert-list">
                <div className="alert-item">
                  <Checkmark size={16} style={{ color: "#198038" }} />
                  <span>Incubator #1: 37.0°C, 5.0% CO₂ ✓</span>
                </div>
                <div className="alert-item">
                  <Warning size={16} style={{ color: "#f1c21b" }} />
                  <span>Autoclave #2: Service due in 3 days</span>
                </div>
                <div className="alert-item">
                  <Checkmark size={16} style={{ color: "#198038" }} />
                  <span>Laminar Flow Hood: Filter replaced recently</span>
                </div>
              </div>
            </Tile>
          </Column>
        </Grid>
      </div>

      {/* Environmental Monitoring */}
      <div style={{ marginTop: "1.5rem" }}>
        <h5>Environmental Monitoring (Last Reading)</h5>
        <Grid>
          <Column lg={4} md={2} sm={2}>
            <Tile className="env-tile">
              <Temperature size={20} />
              <div>
                <strong>Culture Room</strong>
                <div>22.5°C | 45% RH</div>
                <small style={{ color: "#198038" }}>Normal</small>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="env-tile">
              <Temperature size={20} />
              <div>
                <strong>Incubator #1</strong>
                <div>37.0°C | 95% RH</div>
                <small style={{ color: "#198038" }}>Normal</small>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="env-tile">
              <Temperature size={20} />
              <div>
                <strong>Refrigerator</strong>
                <div>4.2°C</div>
                <small style={{ color: "#198038" }}>Normal</small>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="env-tile">
              <Temperature size={20} />
              <div>
                <strong>Freezer -20°C</strong>
                <div>-19.8°C</div>
                <small style={{ color: "#198038" }}>Normal</small>
              </div>
            </Tile>
          </Column>
        </Grid>
      </div>

      {/* Quick Actions */}
      <div style={{ marginTop: "1.5rem" }}>
        <h5>Quick Actions</h5>
        <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
          <Button kind="secondary" size="sm" renderIcon={Add}>
            Record Environmental Reading
          </Button>
          <Button kind="secondary" size="sm" renderIcon={View}>
            Check Inventory Status
          </Button>
          <Button kind="secondary" size="sm" renderIcon={Chemistry}>
            Equipment Maintenance Log
          </Button>
          <Button kind="secondary" size="sm" renderIcon={Checkmark}>
            QC Test Results
          </Button>
        </div>
      </div>
    </div>
  );

  const renderBatchDetails = () => {
    if (!selectedBatch) return null;

    return (
      <div className="batch-details">
        <h3>Batch Details: {selectedBatch.batchId}</h3>

        <Grid>
          <Column lg={8} md={4} sm={4}>
            <div className="batch-info">
              <h4>Batch Information</h4>
              <p>
                <strong>Virus Strain:</strong> {selectedBatch.virusStrain}
              </p>
              <p>
                <strong>Cell Line:</strong> {selectedBatch.cellLineUsed}
              </p>
              <p>
                <strong>Passage Number:</strong> {selectedBatch.passageNumber}
              </p>
              <p>
                <strong>Status:</strong> {getStatusTag(selectedBatch.status)}
              </p>
              <p>
                <strong>Created:</strong>{" "}
                {formatDate(selectedBatch.createdDate)}
              </p>
              {selectedBatch.cultureStartDate && (
                <p>
                  <strong>Culture Started:</strong>{" "}
                  {formatDate(selectedBatch.cultureStartDate)}
                </p>
              )}
              {selectedBatch.temperatureCelsius && (
                <p>
                  <strong>Temperature:</strong>{" "}
                  {selectedBatch.temperatureCelsius}°C
                </p>
              )}
              {selectedBatch.co2Percentage && (
                <p>
                  <strong>CO₂:</strong> {selectedBatch.co2Percentage}%
                </p>
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            {renderWorkflowProgress()}
          </Column>
        </Grid>

        {renderWorkflowSteps()}

        {/* Step Information Panel */}
        {selectedBatch && renderStepInfoPanel()}
      </div>
    );
  };

  // Render detailed step information panel with LMIS requirements
  const renderStepInfoPanel = () => {
    return (
      <div className="step-info-panel" style={{ marginTop: "2rem" }}>
        <h4>Workflow Step Guidelines & LMIS Requirements</h4>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))",
            gap: "1rem",
          }}
        >
          {workflowSteps.map((step) => {
            const stepStatus = workflowStatus.find(
              (ws) => ws.stepName === step.name,
            );
            const status = stepStatus ? stepStatus.status : "PENDING";

            return (
              <Tile
                key={step.name}
                className={`step-info-tile ${status.toLowerCase()}`}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    marginBottom: "1rem",
                  }}
                >
                  <step.icon size={20} style={{ marginRight: "0.5rem" }} />
                  <h5 style={{ margin: 0 }}>{step.label}</h5>
                  {getStatusTag(status)}
                </div>

                <p style={{ fontSize: "0.875rem", marginBottom: "1rem" }}>
                  {step.description}
                </p>

                {/* Show PDF and LMIS requirements */}
                <div
                  style={{
                    marginBottom: "1rem",
                    padding: "0.5rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                  }}
                >
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>PDF Requirement:</strong> {step.pdfRequirement}
                  </div>
                  <div>
                    <strong>LMIS Requirement:</strong> {step.lmisRequirement}
                  </div>
                </div>

                {/* Show step-specific requirements */}
                {step.mediaTypes && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Media Types:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.mediaTypes.slice(0, 3).map((media) => (
                        <li key={media}>{media}</li>
                      ))}
                      {step.mediaTypes.length > 3 && (
                        <li>... and {step.mediaTypes.length - 3} more</li>
                      )}
                    </ul>
                  </div>
                )}

                {step.reagents && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Key Reagents:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.reagents.slice(0, 4).map((reagent) => (
                        <li key={reagent}>{reagent}</li>
                      ))}
                      {step.reagents.length > 4 && (
                        <li>... and {step.reagents.length - 4} more</li>
                      )}
                    </ul>
                  </div>
                )}

                {step.hostCellLines && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Host Cell Lines:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.hostCellLines.slice(0, 3).map((cellLine) => (
                        <li key={cellLine}>{cellLine}</li>
                      ))}
                      {step.hostCellLines.length > 3 && (
                        <li>... and {step.hostCellLines.length - 3} more</li>
                      )}
                    </ul>
                  </div>
                )}

                {step.qcTests && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>QC Tests:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.qcTests.slice(0, 4).map((test) => (
                        <li key={test}>{test}</li>
                      ))}
                      {step.qcTests.length > 4 && (
                        <li>... and {step.qcTests.length - 4} more</li>
                      )}
                    </ul>
                  </div>
                )}

                {step.methods && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Methods/Protocols:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.methods.slice(0, 3).map((method) => (
                        <li key={method}>{method}</li>
                      ))}
                      {step.methods.length > 3 && (
                        <li>... and {step.methods.length - 3} more</li>
                      )}
                    </ul>
                  </div>
                )}

                {step.equipment && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Equipment Required:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.equipment.map((equipment) => (
                        <li key={equipment}>{equipment}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.conditions && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Critical Conditions:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.conditions.map((condition) => (
                        <li key={condition}>{condition}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.criticalParams && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Critical Parameters:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.criticalParams.map((param) => (
                        <li key={param}>{param}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.tests && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>QC Tests Required:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.tests.map((test) => (
                        <li key={test}>{test}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.acceptanceCriteria && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Acceptance Criteria:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.acceptanceCriteria.map((criteria) => (
                        <li key={criteria}>{criteria}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.measurements && (
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Key Measurements:</strong>
                    <ul style={{ fontSize: "0.75rem", marginLeft: "1rem" }}>
                      {step.measurements.map((measurement) => (
                        <li key={measurement}>{measurement}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {step.qcRequired && (
                  <div
                    style={{
                      padding: "0.5rem",
                      backgroundColor: "#e8f5e8",
                      border: "1px solid #4caf50",
                      borderRadius: "4px",
                      marginTop: "0.5rem",
                    }}
                  >
                    <strong style={{ color: "#2e7d32" }}>✓ QC Required</strong>
                  </div>
                )}

                {stepStatus && (
                  <div
                    style={{
                      marginTop: "1rem",
                      padding: "0.5rem",
                      backgroundColor: "#f5f5f5",
                      borderRadius: "4px",
                    }}
                  >
                    <small>
                      {stepStatus.startedDate &&
                        `Started: ${formatDate(stepStatus.startedDate)}`}
                      {stepStatus.completedDate &&
                        ` | Completed: ${formatDate(stepStatus.completedDate)}`}
                      {stepStatus.assignedTo &&
                        ` | Assigned: ${stepStatus.assignedTo.login}`}
                    </small>
                  </div>
                )}
              </Tile>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="virus-culture-workflow-page">
      <div className="page-header">
        <h2>
          <FormattedMessage
            id="virology.culture.workflow.title"
            defaultMessage="Virus Culture Growth Workflow"
          />
        </h2>
        <Button
          kind="primary"
          renderIcon={Add}
          onClick={() => setShowCreateModal(true)}
        >
          <FormattedMessage
            id="virology.culture.workflow.createBatch"
            defaultMessage="Create Culture Batch"
          />
        </Button>
      </div>

      {loading && <Loading />}

      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Virus Culture Workflow Tabs">
          <Tab>Dashboard</Tab>
          <Tab>Active Batches</Tab>
          <Tab>Batch Details</Tab>
        </TabList>
        <TabPanels>
          <TabPanel>{renderDashboard()}</TabPanel>
          <TabPanel>
            <DataTable
              rows={cultureBatches.map(renderBatchRow)}
              headers={batchTableHeaders}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer title="Active Culture Batches">
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
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
          </TabPanel>
          <TabPanel>
            {selectedBatch ? (
              renderBatchDetails()
            ) : (
              <Tile>
                <p>
                  Select a batch from the Active Batches tab to view details.
                </p>
              </Tile>
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Create Batch Modal */}
      <Modal
        open={showCreateModal}
        onRequestClose={() => setShowCreateModal(false)}
        modalHeading="Create New Virus Culture Batch"
        primaryButtonText="Create Batch"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCreateBatch}
      >
        <Form>
          <FormGroup legendText="Virus Strain">
            <Select
              id="virus-strain-select"
              value={createBatchForm.virusStrain}
              onChange={(e) =>
                setCreateBatchForm({
                  ...createBatchForm,
                  virusStrain: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select virus strain..." />
              {virusStrains.map((strain) => (
                <SelectItem key={strain} value={strain} text={strain} />
              ))}
            </Select>
          </FormGroup>

          <FormGroup legendText="Cell Line">
            <Select
              id="cell-line-select"
              value={createBatchForm.cellLine}
              onChange={(e) =>
                setCreateBatchForm({
                  ...createBatchForm,
                  cellLine: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select cell line..." />
              {cellLines.map((cellLine) => (
                <SelectItem key={cellLine} value={cellLine} text={cellLine} />
              ))}
            </Select>
          </FormGroup>

          <FormGroup legendText="Notes">
            <TextArea
              id="batch-notes"
              labelText="Batch Notes"
              rows={3}
              placeholder="Enter any notes about this batch..."
              value={createBatchForm.notes}
              onChange={(e) =>
                setCreateBatchForm({
                  ...createBatchForm,
                  notes: e.target.value,
                })
              }
            />
          </FormGroup>
        </Form>
      </Modal>

      {/* Complete Step Modal */}
      <Modal
        open={showStepModal}
        onRequestClose={() => setShowStepModal(false)}
        modalHeading={`Complete Step: ${currentStep ? getStepLabel(currentStep.stepName) : ""}`}
        primaryButtonText="Complete Step"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCompleteStep}
      >
        <Form>
          {/* Step-specific LMIS tracking fields */}
          {renderStepSpecificFields()}

          {/* Common fields for all steps */}
          <FormGroup legendText="Time Tracking">
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
              }}
            >
              <TextInput
                id="start-time"
                labelText="Start Time"
                type="datetime-local"
                value={stepForm.startTime}
                onChange={(e) =>
                  setStepForm({ ...stepForm, startTime: e.target.value })
                }
              />
              <TextInput
                id="end-time"
                labelText="End Time"
                type="datetime-local"
                value={stepForm.endTime}
                onChange={(e) =>
                  setStepForm({ ...stepForm, endTime: e.target.value })
                }
              />
            </div>
          </FormGroup>

          <FormGroup legendText="Personnel">
            <TextInput
              id="personnel-involved"
              labelText="Personnel Involved"
              placeholder="Enter names/IDs of personnel"
              value={stepForm.personnelInvolved}
              onChange={(e) =>
                setStepForm({ ...stepForm, personnelInvolved: e.target.value })
              }
            />
          </FormGroup>

          <FormGroup legendText="Quality Check Result">
            <Select
              id="quality-result-select"
              value={stepForm.qualityResult}
              onChange={(e) =>
                setStepForm({
                  ...stepForm,
                  qualityResult: e.target.value,
                })
              }
            >
              <SelectItem value="NOT_APPLICABLE" text="Not Applicable" />
              <SelectItem value="PASSED" text="Passed" />
              <SelectItem value="FAILED" text="Failed" />
              <SelectItem value="CONDITIONAL_PASS" text="Conditional Pass" />
              <SelectItem value="REQUIRES_RETEST" text="Requires Retest" />
            </Select>
          </FormGroup>

          <FormGroup legendText="Step Completion Notes">
            <TextArea
              id="step-notes"
              labelText="Completion Notes & Observations"
              rows={3}
              placeholder="Enter detailed notes about completing this step, any observations, deviations, or issues..."
              value={stepForm.notes}
              onChange={(e) =>
                setStepForm({
                  ...stepForm,
                  notes: e.target.value,
                })
              }
            />
          </FormGroup>
        </Form>
      </Modal>
    </div>
  );
};

export default VirusCultureWorkflowPage;
