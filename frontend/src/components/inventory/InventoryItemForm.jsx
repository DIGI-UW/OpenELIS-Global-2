import React, { useState, useEffect, useContext, useCallback } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  TextArea,
  FormLabel,
  Stack,
  RadioButtonGroup,
  RadioButton,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI, NotebookDataAPI } from "./InventoryService";

/**
 * Convert date string from DatePickerInput (mm/dd/yyyy) to ISO format for backend
 */
const convertToISODateTime = (dateString) => {
  if (!dateString || !dateString.trim()) {
    return null;
  }

  try {
    // If already in ISO format, return as-is
    if (dateString.includes('T')) {
      return dateString;
    }

    // Parse mm/dd/yyyy format
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      console.warn("Invalid date format:", dateString);
      return null;
    }

    // Convert to ISO format expected by backend: yyyy-MM-dd'T'HH:mm:ss
    return date.toISOString().slice(0, 19); // Remove milliseconds and Z
  } catch (error) {
    console.error("Error converting date:", dateString, error);
    return null;
  }
};

/**
 * Convert ISO date format from backend to format suitable for DatePickerInput
 */
const convertFromISODateTime = (isoString) => {
  if (!isoString || !isoString.trim()) {
    return "";
  }

  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) {
      return "";
    }

    // Return in MM/DD/YYYY format for DatePickerInput
    return date.toLocaleDateString('en-US');
  } catch (error) {
    console.error("Error converting ISO date:", isoString, error);
    return "";
  }
};

const InventoryItemForm = ({ open, onClose, onSave, item = null }) => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
      });
    },
    [addNotification, setNotificationVisible],
  );
  const isEdit = !!item;

  // Form state
  const [formData, setFormData] = useState({
    name: "",
    itemType: "REAGENT",
    category: "",
    manufacturer: "",
    units: "",
    lowStockThreshold: 0,
    projectName: "", // New field for project selection
    // Reagent-specific
    stabilityAfterOpening: 0,
    dilutionNotes: "",
    storageRequirements: "",
    concentration: "",
    // Cartridge-specific (Equipment)
    compatibleAnalyzers: "",
    calibrationRequired: "N",
    equipmentCondition: "functional",
    modelNumber: "",
    serialNumber: "",
    ahriTag: "",
    installationDate: "",
    lastServiceDate: "",
    lastMaintenanceDate: "",
    // RDT-specific
    testsPerKit: 0,
    individualTracking: "N",
    // HIV_KIT/SYPHILIS_KIT-specific
    sourceOrganization: "",
    kitTestType: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [itemTypes, setItemTypes] = useState([]);
  const [unitOptions, setUnitOptions] = useState([]);
  const [projects, setProjects] = useState([]); // New state for projects
  const [showNewUnitModal, setShowNewUnitModal] = useState(false);
  const [newUnitName, setNewUnitName] = useState("");

  // Load item types and unit options from backend
  useEffect(() => {
    const loadItemTypes = async () => {
      try {
        const types = await InventoryItemAPI.getItemTypes();
        const formattedTypes = types.map((type) => ({
          id: type,
          text: getItemTypeLabel(type),
        }));
        setItemTypes(formattedTypes);
      } catch (err) {
        console.error("Error loading item types:", err);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Failed to load item types",
        });
      }
    };

    const loadUnitOptions = async () => {
      try {
        const units = await InventoryItemAPI.getUnitOptions();
        // Add "Add new unit..." option at the end
        const unitsWithAddOption = [
          ...units,
          { id: "__add_new__", text: "Add new unit..." },
        ];
        setUnitOptions(unitsWithAddOption);
      } catch (err) {
        console.error("Error loading unit options:", err);
        // Fallback to basic units if API fails
        setUnitOptions([
          { id: "mL", text: "mL" },
          { id: "tests", text: "tests" },
          { id: "kits", text: "kits" },
          { id: "cartridges", text: "cartridges" },
          { id: "tubes", text: "tubes" },
          { id: "bottles", text: "bottles" },
          { id: "units", text: "units" },
          { id: "__add_new__", text: "Add new unit..." },
        ]);
      }
    };

    const loadProjects = async () => {
      try {
        const notebooks = await NotebookDataAPI.getNotebooks();
        const formattedProjects = notebooks.map((notebook) => ({
          id: notebook.id,
          text: notebook.title || "Unknown",
        }));
        setProjects(formattedProjects);
      } catch (err) {
        console.error("Error loading projects:", err);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Failed to load projects",
        });
      }
    };

    loadItemTypes();
    loadUnitOptions();
    loadProjects();
  }, [notify, intl]);

  const getItemTypeLabel = (type) => {
    const labels = {
      REAGENT: "Reagent",
      RDT: "RDT (Rapid Diagnostic Test)",
      CARTRIDGE: "Analyzer Cartridge",
      HIV_KIT: "HIV Test Kit",
      SYPHILIS_KIT: "Syphilis Test Kit",
    };
    return labels[type] || type;
  };

  const handleCreateNewUnit = async () => {
    if (!newUnitName.trim()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Unit name cannot be empty",
      });
      return;
    }

    try {
      await InventoryItemAPI.createUnitOfMeasure(newUnitName.trim());

      // Refresh the unit options list
      const units = await InventoryItemAPI.getUnitOptions();
      const unitsWithAddOption = [
        ...units,
        { id: "__add_new__", text: "Add new unit..." },
      ];
      setUnitOptions(unitsWithAddOption);

      // Set the newly created unit as selected
      handleChange("units", newUnitName.trim());

      // Close modal and reset form
      setShowNewUnitModal(false);
      setNewUnitName("");

      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: `Unit "${newUnitName.trim()}" created successfully`,
      });
    } catch (err) {
      console.error("Error creating new unit:", err);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: err.message || "Failed to create new unit",
      });
    }
  };

  // Load item data if editing, reset if adding new
  useEffect(() => {
    if (item) {
      setFormData({
        name: item.name || "",
        itemType: item.itemType || "REAGENT",
        category: item.category || "",
        manufacturer: item.manufacturer || "",
        units: item.units || "",
        lowStockThreshold: item.lowStockThreshold || 0,
        projectName: item.projectName || "", // New field for project
        // Reagent-specific
        stabilityAfterOpening: item.stabilityAfterOpening || 0,
        dilutionNotes: item.dilutionNotes || "",
        storageRequirements: item.storageRequirements || "",
        // Cartridge-specific
        compatibleAnalyzers: item.compatibleAnalyzers || "",
        calibrationRequired: item.calibrationRequired || "N",
        // Equipment-specific fields
        equipmentCondition: item.equipmentCondition || "functional",
        modelNumber: item.modelNumber || "",
        serialNumber: item.serialNumber || "",
        ahriTag: item.ahriTag || "",
        installationDate: convertFromISODateTime(item.installationDate) || "",
        lastServiceDate: convertFromISODateTime(item.lastServiceDate) || "",
        lastMaintenanceDate: convertFromISODateTime(item.lastMaintenanceDate) || "",
        // Reagent-specific additional fields
        concentration: item.concentration || "",
        // RDT-specific
        testsPerKit: item.testsPerKit || 0,
        individualTracking: item.individualTracking || "N",
        // HIV_KIT/SYPHILIS_KIT-specific
        sourceOrganization: item.sourceOrganization || "",
        kitTestType: item.kitTestType || "",
      });
    } else {
      // Reset to initial state when adding new item
      setFormData({
        name: "",
        itemType: "REAGENT",
        category: "",
        manufacturer: "",
        units: "",
        lowStockThreshold: 0,
        projectName: "", // New field for project
        // Reagent-specific
        stabilityAfterOpening: 0,
        dilutionNotes: "",
        storageRequirements: "",
        // Cartridge-specific
        compatibleAnalyzers: "",
        calibrationRequired: "N",
        // Equipment-specific fields
        equipmentCondition: "functional",
        modelNumber: "",
        serialNumber: "",
        ahriTag: "",
        installationDate: "",
        lastServiceDate: "",
        lastMaintenanceDate: "",
        // Reagent-specific additional fields
        concentration: "",
        // RDT-specific
        testsPerKit: 0,
        individualTracking: "N",
        // HIV_KIT/SYPHILIS_KIT-specific
        sourceOrganization: "",
        kitTestType: "",
      });
    }
  }, [item, open]);

  // Handle input changes
  const handleChange = (field, value) => {
    // Convert empty string or NaN to 0 for numeric fields
    const numericFields = [
      "lowStockThreshold",
      "stabilityAfterOpening",
      "testsPerKit",
    ];

    let processedValue = value;
    if (numericFields.includes(field)) {
      if (value === "" || value === null || value === undefined) {
        processedValue = 0;
      } else if (isNaN(value)) {
        processedValue = 0;
      }
    }

    setFormData((prev) => {
      // Prevent unnecessary state updates if value hasn't changed
      if (prev[field] === processedValue) {
        return prev;
      }
      return { ...prev, [field]: processedValue };
    });
    setError(null);
  };

  // Validate form
  const validate = () => {
    if (!formData.name?.trim()) {
      setError("Item name is required");
      return false;
    }

    if (!formData.itemType) {
      setError("Item type is required");
      return false;
    }

    if (!formData.units?.trim()) {
      setError("Units are required");
      return false;
    }

    // Type-specific validation
    if (formData.itemType === "REAGENT") {
      if (
        !formData.stabilityAfterOpening ||
        formData.stabilityAfterOpening <= 0
      ) {
        setError(
          "Stability after opening is required for reagents and must be greater than 0",
        );
        return false;
      }
    }

    if (formData.itemType === "CARTRIDGE") {
      if (!formData.compatibleAnalyzers?.trim()) {
        setError("Compatible analyzers are required for equipment");
        return false;
      }
      if (
        formData.calibrationRequired &&
        formData.calibrationRequired !== "Y" &&
        formData.calibrationRequired !== "N"
      ) {
        setError("Calibration required must be Y or N");
        return false;
      }
      if (!formData.modelNumber?.trim()) {
        setError("Model number is required for equipment");
        return false;
      }
      const validConditions = ['functional', 'non-functional', 'under-repair', 'decommissioned'];
      if (!validConditions.includes(formData.equipmentCondition)) {
        setError("Invalid equipment condition selected");
        return false;
      }
    }

    if (formData.itemType === "RDT") {
      if (!formData.testsPerKit || formData.testsPerKit <= 0) {
        setError(
          "Tests per kit is required for RDTs and must be greater than 0",
        );
        return false;
      }
      if (
        formData.individualTracking &&
        formData.individualTracking !== "Y" &&
        formData.individualTracking !== "N"
      ) {
        setError("Individual tracking must be Y or N");
        return false;
      }
    }

    if (
      formData.itemType === "HIV_KIT" ||
      formData.itemType === "SYPHILIS_KIT"
    ) {
      if (!formData.sourceOrganization?.trim()) {
        setError("Source organization is required for HIV/Syphilis kits");
        return false;
      }
      if (!formData.kitTestType?.trim()) {
        setError("Kit test type is required for HIV/Syphilis kits");
        return false;
      }
      if (!formData.testsPerKit || formData.testsPerKit <= 0) {
        setError(
          "Tests per kit is required for HIV/Syphilis kits and must be greater than 0",
        );
        return false;
      }
    }

    return true;
  };

  // Handle save
  const handleSave = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      // Build sanitized data with only type-relevant fields
      const sanitizedData = {
        name: formData.name,
        itemType: formData.itemType,
        category: formData.category,
        manufacturer: formData.manufacturer,
        units: formData.units,
        lowStockThreshold: Number(formData.lowStockThreshold) || 0,
        projectName: formData.projectName || null,
      };

      // Add type-specific fields only for relevant item types
      if (formData.itemType === "REAGENT") {
        sanitizedData.stabilityAfterOpening =
          Number(formData.stabilityAfterOpening) || 0;
        sanitizedData.dilutionNotes = formData.dilutionNotes;
        sanitizedData.storageRequirements = formData.storageRequirements;
        sanitizedData.concentration = formData.concentration;
      } else if (formData.itemType === "CARTRIDGE") {
        sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers;
        sanitizedData.calibrationRequired = formData.calibrationRequired;
        sanitizedData.equipmentCondition = formData.equipmentCondition;
        sanitizedData.modelNumber = formData.modelNumber;
        sanitizedData.serialNumber = formData.serialNumber;
        sanitizedData.ahriTag = formData.ahriTag;
        // Convert date strings to proper ISO format if provided
        if (formData.installationDate) {
          sanitizedData.installationDate = convertToISODateTime(formData.installationDate);
        }
        if (formData.lastServiceDate) {
          sanitizedData.lastServiceDate = convertToISODateTime(formData.lastServiceDate);
        }
        if (formData.lastMaintenanceDate) {
          sanitizedData.lastMaintenanceDate = convertToISODateTime(formData.lastMaintenanceDate);
        }
      } else if (formData.itemType === "RDT") {
        sanitizedData.testsPerKit = Number(formData.testsPerKit) || 0;
        sanitizedData.individualTracking = formData.individualTracking;
      } else if (
        formData.itemType === "HIV_KIT" ||
        formData.itemType === "SYPHILIS_KIT"
      ) {
        sanitizedData.sourceOrganization = formData.sourceOrganization;
        sanitizedData.kitTestType = formData.kitTestType;
        sanitizedData.testsPerKit = Number(formData.testsPerKit) || 0;
      }

      if (isEdit) {
        await InventoryItemAPI.update(item.id, sanitizedData);
      } else {
        await InventoryItemAPI.create(sanitizedData);
      }
      setSaving(false);
      onSave();
    } catch (err) {
      console.error("Error saving item:", err);
      const errorMessage = err.message || "Error saving catalog item";
      setError(errorMessage);
      setSaving(false);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: errorMessage,
      });
    }
  };

  return (
    <>
      <Modal
        open={open}
        onRequestClose={onClose}
        onRequestSubmit={handleSave}
        modalHeading={intl.formatMessage({
          id: isEdit
            ? "catalog.item.form.title.edit"
            : "catalog.item.form.title.add",
        })}
        primaryButtonText={intl.formatMessage({ id: "button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={saving}
        size="md"
      >
        <Stack gap={5}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
          )}

          <TextInput
            id="name"
            labelText={<FormattedMessage id="catalog.item.name" />}
            value={formData.name}
            onChange={(e) => handleChange("name", e.target.value)}
            required
          />

          <Dropdown
            id="itemType"
            titleText={<FormattedMessage id="catalog.item.type" />}
            label="Select item type"
            items={itemTypes}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={itemTypes.find((t) => t.id === formData.itemType)}
            onChange={({ selectedItem }) =>
              handleChange("itemType", selectedItem.id)
            }
            required
          />

          <TextInput
            id="category"
            labelText={<FormattedMessage id="catalog.item.category" />}
            value={formData.category}
            onChange={(e) => handleChange("category", e.target.value)}
          />

          <TextInput
            id="manufacturer"
            labelText={<FormattedMessage id="catalog.item.manufacturer" />}
            value={formData.manufacturer}
            onChange={(e) => handleChange("manufacturer", e.target.value)}
          />

          <Dropdown
            id="projectName"
            titleText="Project Name"
            label="Select a project"
            items={projects}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={projects.find((p) => p.text === formData.projectName)}
            onChange={({ selectedItem }) =>
              handleChange("projectName", selectedItem?.text || "")
            }
          />

          <Dropdown
            id="units"
            titleText={intl.formatMessage({ id: "catalog.item.units" })}
            label="Select a unit"
            items={unitOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={unitOptions.find((u) => u.id === formData.units)}
            onChange={({ selectedItem }) => {
              if (selectedItem?.id === "__add_new__") {
                setShowNewUnitModal(true);
              } else {
                handleChange("units", selectedItem?.id || "");
              }
            }}
          />

          <NumberInput
            id="lowStockThreshold"
            label={<FormattedMessage id="catalog.item.lowStockThreshold" />}
            value={formData.lowStockThreshold ?? 0}
            onChange={(e, { value }) =>
              handleChange("lowStockThreshold", value ?? 0)
            }
            min={0}
            max={999999}
          />

          {/* Type-specific fields */}
          {formData.itemType === "REAGENT" && (
            <>
              <NumberInput
                id="stabilityAfterOpening"
                label={
                  <FormattedMessage id="catalog.item.stabilityAfterOpening" />
                }
                helperText="Days until reagent expires after opening"
                value={formData.stabilityAfterOpening ?? 0}
                onChange={(e, { value }) =>
                  handleChange("stabilityAfterOpening", value ?? 0)
                }
                min={1}
                max={365}
                required
              />

              <TextInput
                id="concentration"
                labelText="Concentration"
                value={formData.concentration}
                onChange={(e) => handleChange("concentration", e.target.value)}
                placeholder="e.g., 1 M, 5 mg/mL, 10x"
              />

              <TextArea
                id="dilutionNotes"
                labelText={<FormattedMessage id="catalog.item.dilutionNotes" />}
                value={formData.dilutionNotes}
                onChange={(e) => handleChange("dilutionNotes", e.target.value)}
                placeholder="e.g., Dilute 1:10 with distilled water"
                rows={2}
              />

              <TextArea
                id="storageRequirements"
                labelText={
                  <FormattedMessage id="catalog.item.storageRequirements" />
                }
                value={formData.storageRequirements}
                onChange={(e) =>
                  handleChange("storageRequirements", e.target.value)
                }
                placeholder="e.g., Store at 2-8°C, protect from light"
                rows={2}
              />
            </>
          )}

          {formData.itemType === "CARTRIDGE" && (
            <>
              <TextInput
                id="compatibleAnalyzers"
                labelText={
                  <FormattedMessage id="catalog.item.compatibleAnalyzers" />
                }
                value={formData.compatibleAnalyzers}
                onChange={(e) =>
                  handleChange("compatibleAnalyzers", e.target.value)
                }
                placeholder="e.g., GeneXpert, Cobas 6800"
                required
              />

              <RadioButtonGroup
                legendText={
                  <FormattedMessage id="catalog.item.calibrationRequired" />
                }
                name="calibrationRequired"
                valueSelected={formData.calibrationRequired}
                onChange={(value) => handleChange("calibrationRequired", value)}
              >
                <RadioButton labelText="Yes" value="Y" id="calibration-yes" />
                <RadioButton labelText="No" value="N" id="calibration-no" />
              </RadioButtonGroup>

              <TextInput
                id="modelNumber"
                labelText="Model Number"
                value={formData.modelNumber}
                onChange={(e) => handleChange("modelNumber", e.target.value)}
                placeholder="e.g., QuantStudio-3, BX43"
                required
              />

              <TextInput
                id="serialNumber"
                labelText="Serial Number"
                value={formData.serialNumber}
                onChange={(e) => handleChange("serialNumber", e.target.value)}
                placeholder="e.g., QS3-2024-001"
              />

              <TextInput
                id="ahriTag"
                labelText="AHRI Tag"
                value={formData.ahriTag}
                onChange={(e) => handleChange("ahriTag", e.target.value)}
                placeholder="e.g., AHRI-PCR-001"
              />


              {(() => {
                const conditionOptions = [
                  { id: 'functional', text: 'Functional' },
                  { id: 'non-functional', text: 'Non-functional' },
                  { id: 'under-repair', text: 'Under Repair' },
                  { id: 'decommissioned', text: 'Decommissioned' }
                ];
                return (
                  <Dropdown
                    id="equipmentCondition"
                    titleText="Equipment Condition"
                    label="Select condition"
                    items={conditionOptions}
                    selectedItem={
                      conditionOptions.find(item => item.id === formData.equipmentCondition) ||
                      conditionOptions[0]
                    }
                    itemToString={(item) => (item ? item.text : "")}
                    onChange={({ selectedItem }) =>
                      handleChange("equipmentCondition", selectedItem?.id || "functional")
                    }
                    required
                  />
                );
              })()}

              <DatePicker datePickerType="single">
                <DatePickerInput
                  id="installationDate"
                  placeholder="mm/dd/yyyy"
                  labelText="Installation Date"
                  value={formData.installationDate}
                  onChange={(e) => handleChange("installationDate", e.target.value)}
                />
              </DatePicker>

              <DatePicker datePickerType="single">
                <DatePickerInput
                  id="lastServiceDate"
                  placeholder="mm/dd/yyyy"
                  labelText="Last Service Date"
                  value={formData.lastServiceDate}
                  onChange={(e) => handleChange("lastServiceDate", e.target.value)}
                />
              </DatePicker>

              <DatePicker datePickerType="single">
                <DatePickerInput
                  id="lastMaintenanceDate"
                  placeholder="mm/dd/yyyy"
                  labelText="Last Maintenance Date"
                  value={formData.lastMaintenanceDate}
                  onChange={(e) => handleChange("lastMaintenanceDate", e.target.value)}
                />
              </DatePicker>
            </>
          )}

          {formData.itemType === "RDT" && (
            <>
              <NumberInput
                id="testsPerKit"
                label={<FormattedMessage id="catalog.item.testsPerKit" />}
                helperText="Number of individual tests in this kit"
                value={formData.testsPerKit ?? 0}
                onChange={(e, { value }) =>
                  handleChange("testsPerKit", value ?? 0)
                }
                min={1}
                max={1000}
                required
              />

              <RadioButtonGroup
                legendText={
                  <FormattedMessage id="catalog.item.individualTracking" />
                }
                name="individualTracking"
                valueSelected={formData.individualTracking}
                onChange={(value) => handleChange("individualTracking", value)}
              >
                <RadioButton
                  labelText="Yes - Track each test individually"
                  value="Y"
                  id="tracking-yes"
                />
                <RadioButton
                  labelText="No - Track kit as whole"
                  value="N"
                  id="tracking-no"
                />
              </RadioButtonGroup>
            </>
          )}

          {(formData.itemType === "HIV_KIT" ||
            formData.itemType === "SYPHILIS_KIT") && (
            <>
              <TextInput
                id="sourceOrganization"
                labelText={
                  <FormattedMessage id="catalog.item.sourceOrganization" />
                }
                value={formData.sourceOrganization}
                onChange={(e) =>
                  handleChange("sourceOrganization", e.target.value)
                }
                placeholder="e.g., WHO, CDC, PEPFAR"
                required
              />

              <TextInput
                id="kitTestType"
                labelText={<FormattedMessage id="catalog.item.kitTestType" />}
                value={formData.kitTestType}
                onChange={(e) => handleChange("kitTestType", e.target.value)}
                placeholder={
                  formData.itemType === "HIV_KIT"
                    ? "e.g., HIV-1/2"
                    : "e.g., RPR, TPHA"
                }
                required
              />

              <NumberInput
                id="testsPerKit"
                label={<FormattedMessage id="catalog.item.testsPerKit" />}
                helperText="Number of individual tests in this kit"
                value={formData.testsPerKit ?? 0}
                onChange={(e, { value }) =>
                  handleChange("testsPerKit", value ?? 0)
                }
                min={1}
                max={1000}
                required
              />
            </>
          )}
        </Stack>
      </Modal>

      {/* New Unit Creation Modal */}
      <Modal
        open={showNewUnitModal}
        onRequestClose={() => {
          setShowNewUnitModal(false);
          setNewUnitName("");
        }}
        modalHeading="Add New Unit of Measure"
        primaryButtonText="Create Unit"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCreateNewUnit}
        size="sm"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ marginBottom: "1rem" }}>
            Enter a new unit of measure to add to the standardized list.
          </p>
          <TextInput
            id="newUnitName"
            labelText="Unit Name"
            placeholder="e.g., mg, liters, vials"
            value={newUnitName}
            onChange={(e) => setNewUnitName(e.target.value)}
            helperText="Enter the unit abbreviation or name (e.g., mL, tests, kits)"
          />
        </div>
      </Modal>
    </>
  );
};

export default InventoryItemForm;
