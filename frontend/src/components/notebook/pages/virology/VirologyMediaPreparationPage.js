import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
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
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  FileUploader,
  Checkbox,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Chemistry,
  Settings,
  WarningAlt,
  Add,
  TrashCan,
  Document,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyMediaPreparationPage - Page 2 of the Virology & Vaccine Unit workflow
 *
 * Purpose: Ensure full traceability of materials and equipment for media preparation
 *
 * Requirements:
 * 1. Media name and type (controlled vocabulary)
 * 2. Reagents used (each as a line item):
 *    - Supplier
 *    - Lot number
 *    - Expiration date
 * 3. Equipment used (select from registered equipment list)
 * 4. System Requirements:
 *    - Mandatory fields for lot number and expiry
 *    - Automatic expiry validation (warning if expired)
 *    - Ability to attach supporting documents (COA, spec sheet)
 *    - Time-stamped user signature
 * 5. Outputs:
 *    - Material usage log
 *    - Media preparation record ID (linked forward)
 */

// Media Types for Virology
const MEDIA_TYPES = [
  { id: "DMEM_HIGH_GLUCOSE", text: "DMEM (High Glucose)" },
  { id: "RPMI_1640", text: "RPMI-1640" },
  { id: "MEM", text: "Minimum Essential Medium (MEM)" },
  { id: "OPTI_MEM", text: "Opti-MEM" },
  { id: "EMEM", text: "Eagle's MEM (EMEM)" },
  { id: "F12", text: "Ham's F-12" },
  { id: "DMEM_F12", text: "DMEM/F-12 (1:1)" },
  { id: "IMDM", text: "Iscove's Modified Dulbecco's Medium (IMDM)" },
  { id: "CUSTOM", text: "Custom Formulation" },
];

// Reagent Categories
const REAGENT_CATEGORIES = [
  { id: "SERUM", text: "Serum (FBS, Horse, etc.)" },
  { id: "ANTIBIOTIC", text: "Antibiotics (Pen/Strep, etc.)" },
  { id: "SUPPLEMENT", text: "Supplements (L-Glutamine, NEAA, etc.)" },
  { id: "BUFFER", text: "Buffers (HEPES, Sodium Bicarbonate, etc.)" },
  { id: "GROWTH_FACTOR", text: "Growth Factors" },
  { id: "OTHER", text: "Other Reagents" },
];

// Common Reagents for Virology Media
const COMMON_REAGENTS = [
  { id: "FBS", text: "Fetal Bovine Serum (FBS)", category: "SERUM" },
  { id: "HORSE_SERUM", text: "Horse Serum", category: "SERUM" },
  {
    id: "PEN_STREP",
    text: "Penicillin/Streptomycin (P/S)",
    category: "ANTIBIOTIC",
  },
  { id: "GENTAMICIN", text: "Gentamicin", category: "ANTIBIOTIC" },
  { id: "AMPHOTERICIN_B", text: "Amphotericin B", category: "ANTIBIOTIC" },
  { id: "L_GLUTAMINE", text: "L-Glutamine (200mM)", category: "SUPPLEMENT" },
  { id: "HEPES", text: "HEPES Buffer (1M)", category: "BUFFER" },
  { id: "SODIUM_BICARB", text: "Sodium Bicarbonate", category: "BUFFER" },
  {
    id: "NEAA",
    text: "Non-Essential Amino Acids (NEAA)",
    category: "SUPPLEMENT",
  },
  { id: "SODIUM_PYRUVATE", text: "Sodium Pyruvate", category: "SUPPLEMENT" },
  {
    id: "2_MERCAPTOETHANOL",
    text: "2-Mercaptoethanol",
    category: "SUPPLEMENT",
  },
  { id: "TRYPSIN", text: "Trypsin-EDTA", category: "OTHER" },
];

// Common Suppliers
const SUPPLIERS = [
  "Gibco/Thermo Fisher",
  "Sigma-Aldrich",
  "Lonza",
  "Corning",
  "HyClone",
  "ATCC",
  "Invitrogen",
  "Millipore",
  "Biological Industries",
  "Other",
];

// Sterilization Methods
const STERILIZATION_METHODS = [
  { id: "STEAM_AUTOCLAVE", text: "Steam Autoclave (121°C, 15 PSI)" },
  { id: "DRY_HEAT", text: "Dry Heat Sterilization" },
  { id: "FILTER_022", text: "Filter Sterilization (0.22μm)" },
  { id: "FILTER_010", text: "Filter Sterilization (0.1μm)" },
  { id: "GAMMA_RADIATION", text: "Gamma Irradiation" },
  { id: "UV_STERILIZATION", text: "UV Sterilization (254nm)" },
  { id: "OTHER", text: "Other (specify in notes)" },
];

// SOP Parameter Ranges for Sterilization Validation
const STERILIZATION_SOP_RANGES = {
  STEAM_AUTOCLAVE: {
    temperature: { min: 119, max: 123, unit: "°C" },
    time: { min: 12, max: 18, unit: "minutes" },
    pressure: { min: 13, max: 17, unit: "PSI" },
  },
  DRY_HEAT: {
    temperature: { min: 158, max: 162, unit: "°C" },
    time: { min: 110, max: 130, unit: "minutes" },
    pressure: { min: null, max: null, unit: null },
  },
  FILTER_022: {
    temperature: { min: 20, max: 25, unit: "°C" },
    time: { min: null, max: null, unit: "minutes" },
    pressure: { min: 0, max: 50, unit: "PSI" },
  },
  FILTER_010: {
    temperature: { min: 20, max: 25, unit: "°C" },
    time: { min: null, max: null, unit: "minutes" },
    pressure: { min: 0, max: 50, unit: "PSI" },
  },
  GAMMA_RADIATION: {
    temperature: { min: null, max: null, unit: null },
    time: { min: null, max: null, unit: null },
    pressure: { min: null, max: null, unit: null },
  },
  UV_STERILIZATION: {
    temperature: { min: 20, max: 30, unit: "°C" },
    time: { min: 25, max: 65, unit: "minutes" },
    pressure: { min: null, max: null, unit: null },
  },
};

// Biological Indicators
const BIOLOGICAL_INDICATORS = [
  { id: "GEOBACILLUS", text: "Geobacillus stearothermophilus spores (Steam)" },
  { id: "BACILLUS_ATROPHAEUS", text: "Bacillus atrophaeus spores (Dry heat)" },
  { id: "NONE", text: "Not Applicable" },
];

function VirologyMediaPreparationPage({
  entryId,
  pageData,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // State for media preparation (sterilization fields removed - now separate)
  const [preparationModalOpen, setPreparationModalOpen] = useState(false);
  const [mediaPreparationData, setMediaPreparationData] = useState({
    // Linked samples (optional - can be empty)
    linkedSamples: [],
    // Media preparation fields
    mediaType: "",
    mediaName: "",
    batchId: "",
    preparationDate: new Date().toISOString().split("T")[0],
    expirationDate: "",
    volume: "",
    volumeUnit: "mL",
    reagents: [], // Array of reagent objects
    equipment: [], // Array of equipment IDs
    pH: "",
    osmolality: "",
    sterilityTest: "PENDING",
    notes: "",
    documents: [], // Array of attached documents
    preparedBy: "",
  });

  // State for sterilization (separate modal)
  const [sterilizationModalOpen, setSterilizationModalOpen] = useState(false);
  const [sterilizationData, setSterilizationData] = useState({
    linkedSamples: [],
    method: "",
    equipmentId: "",
    temperature: "",
    temperatureUnit: "°C",
    time: "",
    timeUnit: "minutes",
    pressure: "",
    pressureUnit: "PSI",
    sterilizationDate: new Date().toISOString().split("T")[0],
    operator: "",
    biologicalIndicator: "",
    biResult: "PENDING",
    notes: "",
    passFailStatus: "PENDING",
    outOfRangeFlag: false,
    outOfRangeReason: "",
  });

  // State for reagent modal
  const [reagentModalOpen, setReagentModalOpen] = useState(false);
  const [currentReagent, setCurrentReagent] = useState({
    reagentName: "",
    reagentCategory: "",
    supplier: "",
    catalogNumber: "",
    lotNumber: "",
    expirationDate: "",
    concentration: "",
    volume: "",
    volumeUnit: "mL",
    notes: "",
  });
  const [editingReagentIndex, setEditingReagentIndex] = useState(null);

  // State for equipment and inventory lists
  const [availableEquipment, setAvailableEquipment] = useState([]);
  const [availableInventory, setAvailableInventory] = useState([]);

  // Load samples for this page (samples that completed previous page)
  const loadSamples = useCallback(() => {
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
              status: sample.pageStatus || sample.status || "PENDING",
              // Reception metadata from Page 1
              sampleId: sample.data?.sampleId,
              source: sample.data?.source,
              testType: sample.data?.testType,
              projectStudyAssociation: sample.data?.projectStudyAssociation,
              receptionDateTime: sample.data?.receptionDateTime,
              // Media preparation data (this page)
              mediaPreparationId: sample.data?.mediaPreparationId,
              mediaType: sample.data?.mediaType,
              batchId: sample.data?.batchId,
              preparationDate: sample.data?.preparationDate,
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

  // Load equipment list from notebook template configuration
  const loadEquipment = useCallback(() => {
    if (templateInstruments && templateInstruments.length > 0) {
      // Use instruments from notebook template configuration
      setAvailableEquipment(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          name: analyzer.value,
          serialNumber: analyzer.serialNumber || "N/A",
        })),
      );
      return;
    }

    // Fall back to active inventory if template has no instruments configured
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setAvailableEquipment(
              response.map((i) => ({
                id: i.id,
                name: i.name,
                serialNumber: i.serialNumber || "N/A",
                ...i,
              })),
            );
          }
        }
      },
    );
  }, [templateInstruments]);

  // Load inventory items (media and reagents) from active inventory
  const loadInventory = useCallback(() => {
    // Load all active inventory items (reagents and media)
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setAvailableInventory(
              response.map((item) => ({
                id: item.id,
                name: item.name,
                description: item.description || item.name,
                category: item.category || "REAGENT",
                subcategory: item.subcategory,
                lotNumber: item.lotNumber,
                expirationDate: item.expirationDate,
                ...item,
              })),
            );
          }
        }
      },
    );
  }, []);

  // Load samples, equipment, and inventory on component mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamples();
    loadEquipment();
    loadInventory();

    return () => {
      componentMounted.current = false;
    };
  }, [loadSamples]);

  // Check if date is expired
  const isExpired = (expirationDate) => {
    if (!expirationDate) return false;
    const today = new Date();
    const expDate = new Date(expirationDate);
    return expDate < today;
  };

  // Check if date is expiring soon (within 30 days)
  const isExpiringSoon = (expirationDate) => {
    if (!expirationDate) return false;
    const today = new Date();
    const expDate = new Date(expirationDate);
    const daysUntilExpiry = Math.ceil(
      (expDate - today) / (1000 * 60 * 60 * 24),
    );
    return daysUntilExpiry > 0 && daysUntilExpiry <= 30;
  };

  // Validate sterilization parameters against SOP ranges
  const validateSterilizationParameters = (sterilization) => {
    const { method, temperature, time, pressure } = sterilization;

    if (!method || !STERILIZATION_SOP_RANGES[method]) {
      return { valid: true, errors: [] };
    }

    const ranges = STERILIZATION_SOP_RANGES[method];
    const errors = [];

    // Validate temperature
    if (ranges.temperature.min !== null && temperature) {
      const temp = parseFloat(temperature);
      if (temp < ranges.temperature.min || temp > ranges.temperature.max) {
        errors.push(
          `Temperature must be between ${ranges.temperature.min}-${ranges.temperature.max}${ranges.temperature.unit}`,
        );
      }
    }

    // Validate time
    if (ranges.time.min !== null && time) {
      const timeVal = parseFloat(time);
      if (timeVal < ranges.time.min || timeVal > ranges.time.max) {
        errors.push(
          `Time must be between ${ranges.time.min}-${ranges.time.max} ${ranges.time.unit}`,
        );
      }
    }

    // Validate pressure
    if (ranges.pressure.min !== null && pressure) {
      const pressureVal = parseFloat(pressure);
      if (
        pressureVal < ranges.pressure.min ||
        pressureVal > ranges.pressure.max
      ) {
        errors.push(
          `Pressure must be between ${ranges.pressure.min}-${ranges.pressure.max} ${ranges.pressure.unit}`,
        );
      }
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  };

  // Open preparation modal (sample selection is optional)
  const handleOpenPreparationModal = () => {
    // Initialize linked samples with selected samples (if any)
    setMediaPreparationData((prev) => ({
      ...prev,
      linkedSamples: selectedIds,
    }));
    setPreparationModalOpen(true);
  };

  // Open sterilization modal (sample selection is optional)
  const handleOpenSterilizationModal = () => {
    // Initialize linked samples with selected samples (if any)
    setSterilizationData((prev) => ({
      ...prev,
      linkedSamples: selectedIds,
    }));
    setSterilizationModalOpen(true);
  };

  // Add reagent
  const handleAddReagent = () => {
    setCurrentReagent({
      reagentName: "",
      reagentCategory: "",
      supplier: "",
      catalogNumber: "",
      lotNumber: "",
      expirationDate: "",
      concentration: "",
      volume: "",
      volumeUnit: "mL",
      notes: "",
    });
    setEditingReagentIndex(null);
    setReagentModalOpen(true);
  };

  // Edit reagent
  const handleEditReagent = (index) => {
    setCurrentReagent(mediaPreparationData.reagents[index]);
    setEditingReagentIndex(index);
    setReagentModalOpen(true);
  };

  // Save reagent
  const handleSaveReagent = () => {
    // Validation
    if (
      !currentReagent.reagentName ||
      !currentReagent.lotNumber ||
      !currentReagent.expirationDate
    ) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.media.reagent.missingRequired",
          defaultMessage:
            "Please fill in all required fields (Reagent Name, Lot Number, Expiration Date)",
        }),
        kind: "error",
      });
      return;
    }

    // Check if expired
    if (isExpired(currentReagent.expirationDate)) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.warning",
          defaultMessage: "Warning",
        }),
        message: intl.formatMessage({
          id: "virology.media.reagent.expired",
          defaultMessage:
            "This reagent is expired. Please use a different lot or confirm usage.",
        }),
        kind: "warning",
      });
      return;
    }

    const updatedReagents = [...mediaPreparationData.reagents];
    if (editingReagentIndex !== null) {
      updatedReagents[editingReagentIndex] = currentReagent;
    } else {
      updatedReagents.push(currentReagent);
    }

    setMediaPreparationData((prev) => ({
      ...prev,
      reagents: updatedReagents,
    }));

    setReagentModalOpen(false);
  };

  // Delete reagent
  const handleDeleteReagent = (index) => {
    const updatedReagents = mediaPreparationData.reagents.filter(
      (_, i) => i !== index,
    );
    setMediaPreparationData((prev) => ({
      ...prev,
      reagents: updatedReagents,
    }));
  };

  // Save media preparation
  const handleSaveMediaPreparation = async () => {
    // Validation
    if (!mediaPreparationData.mediaType || !mediaPreparationData.batchId) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.media.preparation.missingRequired",
          defaultMessage:
            "Please fill in all required fields (Media Type, Batch ID)",
        }),
        kind: "error",
      });
      return;
    }

    if (mediaPreparationData.reagents.length === 0) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.media.preparation.noReagents",
          defaultMessage: "Please add at least one reagent",
        }),
        kind: "error",
      });
      return;
    }

    if (mediaPreparationData.equipment.length === 0) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.media.preparation.noEquipment",
          defaultMessage: "Please select at least one piece of equipment",
        }),
        kind: "error",
      });
      return;
    }

    try {
      setLoading(true);

      const payload = {
        entryId,
        pageId: pageData.id,
        sampleIds: mediaPreparationData.linkedSamples,
        mediaPreparation: mediaPreparationData,
      };

      const response = await postToOpenElisServer(
        `/rest/notebook/virology/media-preparation`,
        payload,
      );

      if (response) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "virology.media.preparation.saved",
            defaultMessage: "Media preparation record saved successfully",
          }),
          kind: "success",
        });

        // Update progress
        if (onProgressUpdate) {
          onProgressUpdate(pageData.id, {
            completed: true,
            mediaPreparationId: response.id,
          });
        }

        setPreparationModalOpen(false);
        loadSamples();
      }
    } catch (err) {
      console.error("Error saving media preparation:", err);
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.media.preparation.saveFailed",
          defaultMessage: "Failed to save media preparation record",
        }),
        kind: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  // Save sterilization record (separate from media preparation)
  const handleSaveSterilization = async () => {
    // Validation
    if (!sterilizationData.method || !sterilizationData.equipmentId) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.sterilization.missingRequired",
          defaultMessage:
            "Please fill in all required fields (Method, Equipment ID)",
        }),
        kind: "error",
      });
      return;
    }

    if (!sterilizationData.sterilizationDate || !sterilizationData.operator) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.sterilization.missingDateTime",
          defaultMessage: "Please provide sterilization date and operator name",
        }),
        kind: "error",
      });
      return;
    }

    // Validate sterilization parameters against SOP ranges
    const sterilizationValidation =
      validateSterilizationParameters(sterilizationData);

    if (!sterilizationValidation.valid) {
      // Set out-of-range flag
      setSterilizationData((prev) => ({
        ...prev,
        outOfRangeFlag: true,
        outOfRangeReason: sterilizationValidation.errors.join("; "),
      }));

      addNotification({
        title: intl.formatMessage({
          id: "notification.warning",
          defaultMessage: "Warning",
        }),
        message: intl.formatMessage(
          {
            id: "virology.sterilization.outOfRange",
            defaultMessage:
              "Sterilization parameters are out of SOP range: {errors}. An exception flag has been added.",
          },
          { errors: sterilizationValidation.errors.join(", ") },
        ),
        kind: "warning",
      });
    }

    try {
      setLoading(true);

      const payload = {
        entryId,
        pageId: pageData.id,
        sampleIds: sterilizationData.linkedSamples,
        sterilization: sterilizationData,
      };

      const response = await postToOpenElisServer(
        `/rest/notebook/virology/sterilization`,
        payload,
      );

      if (response) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "virology.sterilization.saved",
            defaultMessage: "Sterilization record saved successfully",
          }),
          kind: "success",
        });

        // Update progress
        if (onProgressUpdate) {
          onProgressUpdate(pageData.id, {
            completed: true,
            sterilizationId: response.id,
          });
        }

        setSterilizationModalOpen(false);
        loadSamples();
      }
    } catch (err) {
      console.error("Error saving sterilization:", err);
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "virology.sterilization.saveFailed",
          defaultMessage: "Failed to save sterilization record",
        }),
        kind: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  // Reagent table headers
  const reagentHeaders = [
    { key: "reagentName", header: "Reagent Name" },
    { key: "supplier", header: "Supplier" },
    { key: "lotNumber", header: "Lot Number" },
    { key: "expirationDate", header: "Expiration Date" },
    { key: "concentration", header: "Concentration" },
    { key: "volume", header: "Volume" },
    { key: "status", header: "Status" },
    { key: "actions", header: "Actions" },
  ];

  // Prepare reagent rows for table
  const reagentRows = mediaPreparationData.reagents.map((reagent, index) => ({
    id: `${index}`,
    reagentName: reagent.reagentName,
    supplier: reagent.supplier,
    lotNumber: reagent.lotNumber,
    expirationDate: reagent.expirationDate,
    concentration: reagent.concentration || "N/A",
    volume: reagent.volume ? `${reagent.volume} ${reagent.volumeUnit}` : "N/A",
    status: isExpired(reagent.expirationDate) ? (
      <Tag type="red">Expired</Tag>
    ) : isExpiringSoon(reagent.expirationDate) ? (
      <Tag type="yellow">Expiring Soon</Tag>
    ) : (
      <Tag type="green">Valid</Tag>
    ),
    actions: (
      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button
          kind="ghost"
          size="sm"
          onClick={() => handleEditReagent(index)}
          iconDescription="Edit"
        >
          Edit
        </Button>
        <Button
          kind="danger--ghost"
          size="sm"
          onClick={() => handleDeleteReagent(index)}
          iconDescription="Delete"
          renderIcon={TrashCan}
        >
          Delete
        </Button>
      </div>
    ),
  }));

  return (
    <div className="virology-media-preparation-page">
      <h3>
        <FormattedMessage
          id="virology.media.preparation.title"
          defaultMessage="Media Preparation - Processing & Quality Control"
        />
      </h3>
      <p>
        <FormattedMessage
          id="virology.media.preparation.description"
          defaultMessage="Ensure full traceability of materials and equipment for media preparation"
        />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {/* Action Buttons - ABOVE Sample Grid */}
      <div
        style={{
          marginTop: "1rem",
          marginBottom: "1rem",
          display: "flex",
          gap: "1rem",
          alignItems: "center",
        }}
      >
        <Button onClick={handleOpenPreparationModal} renderIcon={Chemistry}>
          <FormattedMessage
            id="virology.media.preparation.prepare"
            defaultMessage="Prepare Media"
          />
        </Button>
        <Button onClick={handleOpenSterilizationModal} renderIcon={Settings}>
          <FormattedMessage
            id="virology.sterilization.record"
            defaultMessage="Record Sterilization"
          />
        </Button>
        {selectedIds.length > 0 && (
          <Tag type="blue" size="sm">
            {selectedIds.length} sample(s) selected
          </Tag>
        )}
      </div>

      {/* Sample Grid - BELOW Buttons */}
      <Tile style={{ marginTop: "1rem" }}>
        <h4>
          <FormattedMessage
            id="virology.media.preparation.samples"
            defaultMessage="Samples"
          />
        </h4>
        <SampleGrid
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          loading={loading}
        />
      </Tile>

      {/* Media Preparation Modal */}
      <Modal
        open={preparationModalOpen}
        onRequestClose={() => setPreparationModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "virology.media.preparation.modal.title",
          defaultMessage: "Media Preparation Record",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.media.preparation.save",
          defaultMessage: "Save Media Preparation",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveMediaPreparation}
        size="lg"
      >
        <Grid fullWidth>
          {/* Linked Samples (Optional) */}
          <Column lg={16}>
            <MultiSelect
              id="linked-samples"
              titleText={
                <FormattedMessage
                  id="virology.media.preparation.linkedSamples"
                  defaultMessage="Link Samples (Optional)"
                />
              }
              label="Select samples to link to this media preparation..."
              items={samples.map((s) => ({
                id: s.id,
                text: `${s.accessionNumber || s.externalId} - ${s.sampleType || "N/A"}`,
              }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={samples
                .filter((s) =>
                  mediaPreparationData.linkedSamples.includes(s.id),
                )
                .map((s) => ({
                  id: s.id,
                  text: `${s.accessionNumber || s.externalId} - ${s.sampleType || "N/A"}`,
                }))}
              onChange={({ selectedItems }) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  linkedSamples: selectedItems.map((item) => item.id),
                }))
              }
              helperText={
                <FormattedMessage
                  id="virology.media.preparation.linkedSamples.helper"
                  defaultMessage="You can prepare media without linking samples, or link samples later during the workflow"
                />
              }
            />
          </Column>

          <Column lg={16}>
            <hr
              style={{ margin: "1.5rem 0", borderTop: "1px solid #e0e0e0" }}
            />
          </Column>

          {/* Media Type and Name */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="media-type"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.media.preparation.mediaType"
                    defaultMessage="Media Type"
                  />
                  {" *"}
                </span>
              }
              label="Select media type..."
              items={availableInventory
                .filter((item) => item.category === "MEDIA")
                .map((item) => ({
                  id: item.id || item.name,
                  text: item.name || item.description,
                }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={availableInventory
                .filter((item) => item.category === "MEDIA")
                .map((item) => ({
                  id: item.id || item.name,
                  text: item.name || item.description,
                }))
                .find((m) => m.id === mediaPreparationData.mediaType)}
              onChange={({ selectedItem }) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  mediaType: selectedItem?.id || "",
                }))
              }
              helperText={
                <FormattedMessage
                  id="virology.media.preparation.mediaType.helper"
                  defaultMessage="Select media type from active inventory"
                />
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="media-name"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.mediaName"
                  defaultMessage="Media Name/Description"
                />
              }
              placeholder="e.g., DMEM + 10% FBS + P/S"
              value={mediaPreparationData.mediaName}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  mediaName: e.target.value,
                }))
              }
            />
          </Column>

          {/* Batch ID and Dates */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="batch-id"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.media.preparation.batchId"
                    defaultMessage="Batch ID"
                  />
                  {" *"}
                </span>
              }
              placeholder="e.g., MEDIA-2026-001"
              value={mediaPreparationData.batchId}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  batchId: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={4} md={2} sm={2}>
            <DatePicker dateFormat="Y-m-d" datePickerType="single">
              <DatePickerInput
                id="preparation-date"
                labelText={
                  <FormattedMessage
                    id="virology.media.preparation.date"
                    defaultMessage="Preparation Date"
                  />
                }
                value={mediaPreparationData.preparationDate}
                onChange={(e) =>
                  setMediaPreparationData((prev) => ({
                    ...prev,
                    preparationDate: e.target.value,
                  }))
                }
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={2} sm={2}>
            <DatePicker dateFormat="Y-m-d" datePickerType="single">
              <DatePickerInput
                id="expiration-date"
                labelText={
                  <FormattedMessage
                    id="virology.media.preparation.expirationDate"
                    defaultMessage="Expiration Date"
                  />
                }
                value={mediaPreparationData.expirationDate}
                onChange={(e) =>
                  setMediaPreparationData((prev) => ({
                    ...prev,
                    expirationDate: e.target.value,
                  }))
                }
              />
            </DatePicker>
          </Column>

          {/* Volume */}
          <Column lg={6} md={3} sm={2}>
            <TextInput
              id="volume"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.volume"
                  defaultMessage="Volume Prepared"
                />
              }
              placeholder="e.g., 500"
              value={mediaPreparationData.volume}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  volume: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={2} md={1} sm={2}>
            <Dropdown
              id="volume-unit"
              titleText=" "
              label="Unit"
              items={[
                { id: "mL", text: "mL" },
                { id: "L", text: "L" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: mediaPreparationData.volumeUnit,
                text: mediaPreparationData.volumeUnit,
              }}
              onChange={({ selectedItem }) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  volumeUnit: selectedItem?.id || "mL",
                }))
              }
            />
          </Column>

          {/* Reagents Section */}
          <Column lg={16}>
            <div style={{ marginTop: "1rem", marginBottom: "1rem" }}>
              <h5>
                <FormattedMessage
                  id="virology.media.preparation.reagents"
                  defaultMessage="Reagents Used"
                />
                {" *"}
              </h5>
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={Add}
                onClick={handleAddReagent}
                style={{ marginTop: "0.5rem" }}
              >
                <FormattedMessage
                  id="virology.media.preparation.addReagent"
                  defaultMessage="Add Reagent"
                />
              </Button>
            </div>

            {mediaPreparationData.reagents.length > 0 && (
              <DataTable rows={reagentRows} headers={reagentHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader {...getHeaderProps({ header })}>
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow {...getRowProps({ row })}>
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
            )}
          </Column>

          {/* Equipment Selection */}
          <Column lg={16}>
            <MultiSelect
              id="equipment"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.media.preparation.equipment"
                    defaultMessage="Equipment Used"
                  />
                  {" *"}
                </span>
              }
              label="Select equipment..."
              items={availableEquipment.map((eq) => ({
                id: eq.id,
                text: `${eq.name} (${eq.serialNumber})`,
              }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={availableEquipment
                .filter((eq) => mediaPreparationData.equipment.includes(eq.id))
                .map((eq) => ({
                  id: eq.id,
                  text: `${eq.name} (${eq.serialNumber})`,
                }))}
              onChange={({ selectedItems }) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  equipment: selectedItems.map((item) => item.id),
                }))
              }
            />
          </Column>

          {/* pH and Osmolality */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="ph"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.ph"
                  defaultMessage="pH (Target: 7.2-7.4)"
                />
              }
              placeholder="e.g., 7.3"
              value={mediaPreparationData.pH}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  pH: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="osmolality"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.osmolality"
                  defaultMessage="Osmolality (mOsm/kg)"
                />
              }
              placeholder="e.g., 300"
              value={mediaPreparationData.osmolality}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  osmolality: e.target.value,
                }))
              }
            />
          </Column>

          {/* Sterility Test */}
          <Column lg={16}>
            <RadioButtonGroup
              legendText={
                <FormattedMessage
                  id="virology.media.preparation.sterilityTest"
                  defaultMessage="Sterility Test Status"
                />
              }
              name="sterility-test"
              value={mediaPreparationData.sterilityTest}
              onChange={(value) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  sterilityTest: value,
                }))
              }
            >
              <RadioButton labelText="Pending" value="PENDING" />
              <RadioButton labelText="Pass" value="PASS" />
              <RadioButton labelText="Fail" value="FAIL" />
            </RadioButtonGroup>
          </Column>

          {/* Notes */}
          <Column lg={16}>
            <TextArea
              id="notes"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Additional notes about media preparation..."
              value={mediaPreparationData.notes}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              rows={3}
            />
          </Column>

          {/* Prepared By */}
          <Column lg={16}>
            <TextInput
              id="prepared-by"
              labelText={
                <FormattedMessage
                  id="virology.media.preparation.preparedBy"
                  defaultMessage="Prepared By (Time-stamped signature)"
                />
              }
              placeholder="Enter your name"
              value={mediaPreparationData.preparedBy}
              onChange={(e) =>
                setMediaPreparationData((prev) => ({
                  ...prev,
                  preparedBy: e.target.value,
                }))
              }
            />
          </Column>

          {/* Document Attachments */}
          <Column lg={16}>
            <FileUploader
              labelTitle={
                <FormattedMessage
                  id="virology.media.preparation.documents"
                  defaultMessage="Supporting Documents (COA, Spec Sheets)"
                />
              }
              labelDescription={
                <FormattedMessage
                  id="virology.media.preparation.documents.description"
                  defaultMessage="Upload certificates of analysis, specification sheets, or other supporting documents"
                />
              }
              buttonLabel={
                <FormattedMessage
                  id="virology.media.preparation.documents.button"
                  defaultMessage="Add files"
                />
              }
              filenameStatus="edit"
              accept={[".pdf", ".doc", ".docx", ".xlsx", ".jpg", ".png"]}
              multiple
              iconDescription={
                <FormattedMessage
                  id="virology.media.preparation.documents.clear"
                  defaultMessage="Clear file"
                />
              }
              onChange={(e) => {
                if (e.target.files) {
                  const files = Array.from(e.target.files);
                  setMediaPreparationData((prev) => ({
                    ...prev,
                    documents: [...prev.documents, ...files],
                  }));
                }
              }}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Sterilization Modal (Separate) */}
      <Modal
        open={sterilizationModalOpen}
        onRequestClose={() => setSterilizationModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "virology.sterilization.modal.title",
          defaultMessage: "Sterilization Record - Compliance & QC",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.sterilization.save",
          defaultMessage: "Save Sterilization",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveSterilization}
        size="lg"
      >
        <Grid fullWidth>
          {/* Linked Samples (Optional) */}
          <Column lg={16}>
            <MultiSelect
              id="sterilization-linked-samples"
              titleText={
                <FormattedMessage
                  id="virology.sterilization.linkedSamples"
                  defaultMessage="Link Samples (Optional)"
                />
              }
              label="Select samples to link to this sterilization record..."
              items={samples.map((s) => ({
                id: s.id,
                text: `${s.accessionNumber || s.externalId} - ${s.sampleType || "N/A"}`,
              }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={samples
                .filter((s) => sterilizationData.linkedSamples.includes(s.id))
                .map((s) => ({
                  id: s.id,
                  text: `${s.accessionNumber || s.externalId} - ${s.sampleType || "N/A"}`,
                }))}
              onChange={({ selectedItems }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  linkedSamples: selectedItems.map((item) => item.id),
                }))
              }
              helperText={
                <FormattedMessage
                  id="virology.sterilization.linkedSamples.helper"
                  defaultMessage="You can record sterilization without linking samples, or link samples later during the workflow"
                />
              }
            />
          </Column>

          <Column lg={16}>
            <hr
              style={{ margin: "1.5rem 0", borderTop: "1px solid #e0e0e0" }}
            />
          </Column>

          {/* Sterilization Method */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="sterilization-method"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.method"
                    defaultMessage="Sterilization Method"
                  />
                  {" *"}
                </span>
              }
              label="Select method..."
              items={STERILIZATION_METHODS}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                STERILIZATION_METHODS.find(
                  (m) => m.id === sterilizationData.method,
                ) || null
              }
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  method: selectedItem?.id || "",
                }))
              }
            />
          </Column>

          {/* Sterilization Equipment */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="sterilization-equipment"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.equipment"
                    defaultMessage="Equipment ID"
                  />
                  {" *"}
                </span>
              }
              label="Select equipment..."
              items={availableEquipment.map((eq) => ({
                id: eq.id,
                text: `${eq.name} - ${eq.serialNumber}`,
                calibrationStatus: eq.calibrationStatus,
                lastCalibrationDate: eq.lastCalibrationDate,
              }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                availableEquipment
                  .map((eq) => ({
                    id: eq.id,
                    text: `${eq.name} - ${eq.serialNumber}`,
                  }))
                  .find((eq) => eq.id === sterilizationData.equipmentId) || null
              }
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  equipmentId: selectedItem?.id || "",
                }))
              }
            />
            {sterilizationData.equipmentId && (
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="virology.sterilization.equipment.linked"
                  defaultMessage="Auto-linked to equipment calibration records"
                />
              </div>
            )}
          </Column>

          {/* Temperature */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="sterilization-temperature"
              labelText={
                <FormattedMessage
                  id="virology.sterilization.temperature"
                  defaultMessage="Temperature"
                />
              }
              placeholder="e.g., 121"
              value={sterilizationData.temperature}
              onChange={(e) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  temperature: e.target.value,
                }))
              }
              helperText={
                sterilizationData.method &&
                STERILIZATION_SOP_RANGES[sterilizationData.method]?.temperature
                  .min !== null
                  ? `SOP Range: ${STERILIZATION_SOP_RANGES[sterilizationData.method].temperature.min}-${STERILIZATION_SOP_RANGES[sterilizationData.method].temperature.max}${STERILIZATION_SOP_RANGES[sterilizationData.method].temperature.unit}`
                  : ""
              }
            />
          </Column>

          <Column lg={3} md={2} sm={2}>
            <Dropdown
              id="temperature-unit"
              titleText=" "
              label="Unit"
              items={[
                { id: "°C", text: "°C" },
                { id: "°F", text: "°F" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: sterilizationData.temperatureUnit,
                text: sterilizationData.temperatureUnit,
              }}
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  temperatureUnit: selectedItem?.id || "°C",
                }))
              }
            />
          </Column>

          {/* Time */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="sterilization-time"
              labelText={
                <FormattedMessage
                  id="virology.sterilization.time"
                  defaultMessage="Time"
                />
              }
              placeholder="e.g., 15"
              value={sterilizationData.time}
              onChange={(e) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  time: e.target.value,
                }))
              }
              helperText={
                sterilizationData.method &&
                STERILIZATION_SOP_RANGES[sterilizationData.method]?.time.min !==
                  null
                  ? `SOP Range: ${STERILIZATION_SOP_RANGES[sterilizationData.method].time.min}-${STERILIZATION_SOP_RANGES[sterilizationData.method].time.max} ${STERILIZATION_SOP_RANGES[sterilizationData.method].time.unit}`
                  : ""
              }
            />
          </Column>

          <Column lg={3} md={2} sm={2}>
            <Dropdown
              id="time-unit"
              titleText=" "
              label="Unit"
              items={[
                { id: "minutes", text: "minutes" },
                { id: "hours", text: "hours" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: sterilizationData.timeUnit,
                text: sterilizationData.timeUnit,
              }}
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  timeUnit: selectedItem?.id || "minutes",
                }))
              }
            />
          </Column>

          {/* Pressure */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="sterilization-pressure"
              labelText={
                <FormattedMessage
                  id="virology.sterilization.pressure"
                  defaultMessage="Pressure"
                />
              }
              placeholder="e.g., 15"
              value={sterilizationData.pressure}
              onChange={(e) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  pressure: e.target.value,
                }))
              }
              helperText={
                sterilizationData.method &&
                STERILIZATION_SOP_RANGES[sterilizationData.method]?.pressure
                  .min !== null
                  ? `SOP Range: ${STERILIZATION_SOP_RANGES[sterilizationData.method].pressure.min}-${STERILIZATION_SOP_RANGES[sterilizationData.method].pressure.max} ${STERILIZATION_SOP_RANGES[sterilizationData.method].pressure.unit}`
                  : ""
              }
            />
          </Column>

          <Column lg={3} md={2} sm={2}>
            <Dropdown
              id="pressure-unit"
              titleText=" "
              label="Unit"
              items={[
                { id: "PSI", text: "PSI" },
                { id: "kPa", text: "kPa" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: sterilizationData.pressureUnit,
                text: sterilizationData.pressureUnit,
              }}
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  pressureUnit: selectedItem?.id || "PSI",
                }))
              }
            />
          </Column>

          {/* Sterilization Date */}
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={sterilizationData.sterilizationDate}
              onChange={(dates) => {
                const date = dates[0];
                setSterilizationData((prev) => ({
                  ...prev,
                  sterilizationDate: date
                    ? new Date(date).toISOString().split("T")[0]
                    : "",
                }));
              }}
            >
              <DatePickerInput
                id="sterilization-date"
                labelText={
                  <span>
                    <FormattedMessage
                      id="virology.sterilization.date"
                      defaultMessage="Sterilization Date"
                    />
                    {" *"}
                  </span>
                }
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          {/* Operator */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="sterilization-operator"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.operator"
                    defaultMessage="Operator Name"
                  />
                  {" *"}
                </span>
              }
              placeholder="Enter operator name"
              value={sterilizationData.operator}
              onChange={(e) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  operator: e.target.value,
                }))
              }
            />
          </Column>

          {/* Biological Indicator */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="biological-indicator"
              titleText={
                <FormattedMessage
                  id="virology.sterilization.biologicalIndicator"
                  defaultMessage="Biological Indicator"
                />
              }
              label="Select indicator..."
              items={BIOLOGICAL_INDICATORS}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                BIOLOGICAL_INDICATORS.find(
                  (bi) => bi.id === sterilizationData.biologicalIndicator,
                ) || null
              }
              onChange={({ selectedItem }) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  biologicalIndicator: selectedItem?.id || "",
                }))
              }
            />
          </Column>

          {/* BI Result */}
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={
                <FormattedMessage
                  id="virology.sterilization.biResult"
                  defaultMessage="BI Test Result"
                />
              }
              name="bi-result"
              value={sterilizationData.biResult}
              onChange={(value) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  biResult: value,
                }))
              }
              orientation="horizontal"
            >
              <RadioButton labelText="Pending" value="PENDING" />
              <RadioButton labelText="Pass" value="PASS" />
              <RadioButton labelText="Fail" value="FAIL" />
            </RadioButtonGroup>
          </Column>

          {/* Pass/Fail Status & Out-of-Range Warning */}
          {sterilizationData.outOfRangeFlag && (
            <Column lg={16}>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "virology.sterilization.outOfRange.title",
                  defaultMessage: "Parameters Out of SOP Range",
                })}
                subtitle={sterilizationData.outOfRangeReason}
                lowContrast
                hideCloseButton
              />
            </Column>
          )}

          {/* Sterilization Notes */}
          <Column lg={16}>
            <TextArea
              id="sterilization-notes"
              labelText={
                <FormattedMessage
                  id="virology.sterilization.notes"
                  defaultMessage="Sterilization Notes"
                />
              }
              placeholder="Additional notes about sterilization process, exceptions, or observations..."
              value={sterilizationData.notes}
              onChange={(e) =>
                setSterilizationData((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Reagent Modal */}
      <Modal
        open={reagentModalOpen}
        onRequestClose={() => setReagentModalOpen(false)}
        modalHeading={
          editingReagentIndex !== null
            ? intl.formatMessage({
                id: "virology.media.reagent.edit",
                defaultMessage: "Edit Reagent",
              })
            : intl.formatMessage({
                id: "virology.media.reagent.add",
                defaultMessage: "Add Reagent",
              })
        }
        primaryButtonText={intl.formatMessage({
          id: "button.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveReagent}
      >
        <Grid fullWidth>
          {/* Reagent Name */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="reagent-name"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.media.reagent.name"
                    defaultMessage="Reagent Name"
                  />
                  {" *"}
                </span>
              }
              label="Select reagent..."
              items={availableInventory
                .filter((item) => item.category === "REAGENT")
                .map((item) => ({
                  id: item.id || item.name,
                  text: item.name || item.description,
                  category: item.subcategory || "OTHER",
                }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={availableInventory
                .filter((item) => item.category === "REAGENT")
                .map((item) => ({
                  id: item.id || item.name,
                  text: item.name || item.description,
                  category: item.subcategory || "OTHER",
                }))
                .find((r) => r.text === currentReagent.reagentName)}
              onChange={({ selectedItem }) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  reagentName: selectedItem?.text || "",
                  reagentCategory: selectedItem?.category || "",
                }))
              }
              helperText={
                <FormattedMessage
                  id="virology.media.reagent.name.helper"
                  defaultMessage="Select reagent from active inventory"
                />
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="reagent-category"
              titleText={
                <FormattedMessage
                  id="virology.media.reagent.category"
                  defaultMessage="Category"
                />
              }
              label="Select category..."
              items={REAGENT_CATEGORIES}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={REAGENT_CATEGORIES.find(
                (c) => c.id === currentReagent.reagentCategory,
              )}
              onChange={({ selectedItem }) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  reagentCategory: selectedItem?.id || "",
                }))
              }
            />
          </Column>

          {/* Supplier */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="supplier"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.media.reagent.supplier"
                    defaultMessage="Supplier"
                  />
                  {" *"}
                </span>
              }
              label="Select supplier..."
              items={SUPPLIERS.map((s) => ({ id: s, text: s }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: currentReagent.supplier,
                text: currentReagent.supplier,
              }}
              onChange={({ selectedItem }) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  supplier: selectedItem?.text || "",
                }))
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="catalog-number"
              labelText={
                <FormattedMessage
                  id="virology.media.reagent.catalogNumber"
                  defaultMessage="Catalog Number"
                />
              }
              placeholder="e.g., 25200-056"
              value={currentReagent.catalogNumber}
              onChange={(e) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  catalogNumber: e.target.value,
                }))
              }
            />
          </Column>

          {/* Lot Number and Expiration */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="lot-number"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.media.reagent.lotNumber"
                    defaultMessage="Lot Number"
                  />
                  {" *"}
                </span>
              }
              placeholder="e.g., 2345678"
              value={currentReagent.lotNumber}
              onChange={(e) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  lotNumber: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker dateFormat="Y-m-d" datePickerType="single">
              <DatePickerInput
                id="reagent-expiration"
                labelText={
                  <span>
                    <FormattedMessage
                      id="virology.media.reagent.expirationDate"
                      defaultMessage="Expiration Date"
                    />
                    {" *"}
                  </span>
                }
                value={currentReagent.expirationDate}
                onChange={(e) =>
                  setCurrentReagent((prev) => ({
                    ...prev,
                    expirationDate: e.target.value,
                  }))
                }
              />
            </DatePicker>
            {isExpired(currentReagent.expirationDate) && (
              <InlineNotification
                kind="error"
                title="Expired"
                subtitle="This reagent is expired and cannot be used"
                lowContrast
                hideCloseButton
                style={{ marginTop: "0.5rem" }}
              />
            )}
            {!isExpired(currentReagent.expirationDate) &&
              isExpiringSoon(currentReagent.expirationDate) && (
                <InlineNotification
                  kind="warning"
                  title="Expiring Soon"
                  subtitle="This reagent will expire within 30 days"
                  lowContrast
                  hideCloseButton
                  style={{ marginTop: "0.5rem" }}
                />
              )}
          </Column>

          {/* Concentration and Volume */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="concentration"
              labelText={
                <FormattedMessage
                  id="virology.media.reagent.concentration"
                  defaultMessage="Concentration"
                />
              }
              placeholder="e.g., 10,000 U/mL"
              value={currentReagent.concentration}
              onChange={(e) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  concentration: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={6} md={3} sm={2}>
            <TextInput
              id="reagent-volume"
              labelText={
                <FormattedMessage
                  id="virology.media.reagent.volume"
                  defaultMessage="Volume Used"
                />
              }
              placeholder="e.g., 5"
              value={currentReagent.volume}
              onChange={(e) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  volume: e.target.value,
                }))
              }
            />
          </Column>

          <Column lg={2} md={1} sm={2}>
            <Dropdown
              id="reagent-volume-unit"
              titleText=" "
              label="Unit"
              items={[
                { id: "mL", text: "mL" },
                { id: "µL", text: "µL" },
                { id: "L", text: "L" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={{
                id: currentReagent.volumeUnit,
                text: currentReagent.volumeUnit,
              }}
              onChange={({ selectedItem }) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  volumeUnit: selectedItem?.id || "mL",
                }))
              }
            />
          </Column>

          {/* Notes */}
          <Column lg={16}>
            <TextArea
              id="reagent-notes"
              labelText={
                <FormattedMessage
                  id="virology.media.reagent.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Additional notes about this reagent..."
              value={currentReagent.notes}
              onChange={(e) =>
                setCurrentReagent((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default VirologyMediaPreparationPage;
